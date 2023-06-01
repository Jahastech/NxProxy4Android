/* Copyright (C) 2016-2019 Julian Andres Klode <jak@jak-linux.org>
 *
 * Derived from AdBuster:
 * Copyright (C) 2016 Daniel Brodie <dbrodie@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package com.jahastech.nxproxy.vpn;

import android.content.Context;
import android.util.Log;

import com.jahastech.nxproxy.NxPolicy;
import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.NxLog;
import com.jahastech.nxproxy.NxTalkie;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.UnknownPacket;
import org.xbill.DNS.Message;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Creates and parses packets, and sends packets to a remote socket or the device using
 * {@link AdVpnThread}.
 */
public class DnsPacketProxy {

    private static final String TAG = "DnsPacketProxy";

    private final EventLoop eventLoop;
    ArrayList<InetAddress> upstreamDnsServers = new ArrayList<>();

    public DnsPacketProxy(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }

    /**
     * Initializes the rules database and the list of upstream servers.
     *
     * @param context            The context we are operating in (for the database)
     * @param upstreamDnsServers The upstream DNS servers to use; or an empty list if no
     *                           rewriting of ip addresses takes place
     * @throws InterruptedException If the database initialization was interrupted
     */
    void initialize(Context context, ArrayList<InetAddress> upstreamDnsServers) throws InterruptedException {
        this.upstreamDnsServers = upstreamDnsServers;
    }

    /**
     * Handles a responsePayload from an upstream DNS server
     *
     * @param requestPacket   The original request packet
     * @param responsePayload The payload of the response
     */
    void handleDnsResponse(IpPacket requestPacket, byte[] responsePayload) {
        UdpPacket udpOutPacket = (UdpPacket) requestPacket.getPayload();
        UdpPacket.Builder payLoadBuilder = new UdpPacket.Builder(udpOutPacket)
                .srcPort(udpOutPacket.getHeader().getDstPort())
                .dstPort(udpOutPacket.getHeader().getSrcPort())
                .srcAddr(requestPacket.getHeader().getDstAddr())
                .dstAddr(requestPacket.getHeader().getSrcAddr())
                .correctChecksumAtBuild(true)
                .correctLengthAtBuild(true)
                .payloadBuilder(
                        new UnknownPacket.Builder()
                                .rawData(responsePayload)
                );


        IpPacket ipOutPacket;
        if (requestPacket instanceof IpV4Packet) {
            ipOutPacket = new IpV4Packet.Builder((IpV4Packet) requestPacket)
                    .srcAddr((Inet4Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet4Address) requestPacket.getHeader().getSrcAddr())
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();

        } else {
            ipOutPacket = new IpV6Packet.Builder((IpV6Packet) requestPacket)
                    .srcAddr((Inet6Address) requestPacket.getHeader().getDstAddr())
                    .dstAddr((Inet6Address) requestPacket.getHeader().getSrcAddr())
                    .correctLengthAtBuild(true)
                    .payloadBuilder(payLoadBuilder)
                    .build();
        }

        eventLoop.queueDeviceWrite(ipOutPacket);
    }

    /**
     * Handles a DNS request, by either blocking it or forwarding it to the remote location.
     *
     * @param packetData The packet data to read
     * @throws AdVpnThread.VpnNetworkException If some network error occurred
     */
    void handleDnsRequest(byte[] packetData) throws AdVpnThread.VpnNetworkException {

        IpPacket parsedPacket = null;
        try {
            parsedPacket = (IpPacket) IpSelector.newPacket(packetData, 0, packetData.length);
            if (!(parsedPacket.getPayload() instanceof UdpPacket)) {
                Log.i(TAG, "handleDnsRequest: Found IpV6ExtHopByHopOptionsPacket.");
                return;
            }
        } catch (Exception e) {
            Log.i(TAG, "handleDnsRequest: Discarding invalid IP packet", e);
            return;
        }

        UdpPacket parsedUdp;
        Packet udpPayload;

        try {
            parsedUdp = (UdpPacket) parsedPacket.getPayload();
            udpPayload = parsedUdp.getPayload();
        } catch (Exception e) {
            try {
                Log.i(TAG, "handleDnsRequest: Discarding unknown packet type " + parsedPacket.getHeader(), e);
            } catch (Exception e1) {
                Log.i(TAG, "handleDnsRequest: Discarding unknown packet type, could not log packet info", e1);
            }
            return;
        }

        InetAddress destAddr = translateDestinationAdress(parsedPacket);
        if (destAddr == null)
            return;

        if (udpPayload == null) {
            try {
                Log.i(TAG, "handleDnsRequest: Sending UDP packet without payload: " + parsedUdp);
            } catch (Exception e1) {
                Log.i(TAG, "handleDnsRequest: Sending UDP packet without payload");
            }

            // Let's be nice to Firefox. Firefox uses an empty UDP packet to
            // the gateway to reduce the RTT. For further details, please see
            // https://bugzilla.mozilla.org/show_bug.cgi?id=888268
            try {
                DatagramPacket outPacket = new DatagramPacket(new byte[0], 0, 0 /* length */, destAddr, parsedUdp.getHeader().getDstPort().valueAsInt());
                eventLoop.forwardPacket(outPacket, null);
            } catch (Exception e) {
                Log.i(TAG, "handleDnsRequest: Could not send empty UDP packet", e);
            }
            return;
        }

        byte[] dnsRawData = udpPayload.getRawData();
        Message dnsMsg;
        try {
            dnsMsg = new Message(dnsRawData);
        } catch (IOException e) {
            Log.i(TAG, "handleDnsRequest: Discarding non-DNS or invalid packet", e);
            return;
        }
        if (dnsMsg.getQuestion() == null) {
            Log.i(TAG, "handleDnsRequest: Discarding DNS packet with no query " + dnsMsg);
            return;
        }

        //-----------------------------------------------
        // Filtering by NxProxy starts from here.
        String dnsQueryName = dnsMsg.getQuestion().getName().toString(true);
        int dnsQueryType = dnsMsg.getQuestion().getType();
        Message respMsg = null;

        NxLog.debug("dnsQueryName = %s, dnsQueryType = %s.", dnsQueryName, dnsQueryType);

        // 'Refused' to type 65 as we don't want to bypass iOS devices.
        if (dnsQueryType == 65) {
            respMsg = Lib.getRefusedMsg(dnsMsg);

            if (respMsg != null) {
                handleDnsResponse(parsedPacket, respMsg.toWire());
                return;
            }
        }

        // Bypass it if we are not interested in the request.
        boolean bypassFlag = false;

        // We are only interested in A, AAAA queries.
        if (dnsQueryType != Type.A && dnsQueryType != Type.AAAA) {
            bypassFlag = true;
        }

        // Not interested in local domains.
        if (dnsQueryName.endsWith(".local")
                || dnsQueryName.endsWith(".localdomain")
                || dnsQueryName.indexOf('.') == -1) {
            bypassFlag = true;
        }

        // Filtering domains.
        if (!bypassFlag && NxPolicy.getInstance().isEnableFilter()
                && NxTalkie.getInstance().isBlocked(dnsQueryName)) {

            NxLog.info("%s has been blocked.", dnsQueryName);

            if (dnsQueryType == Type.A) {
                respMsg = Lib.getRediMsg(dnsMsg, "127.0.0.1", 60);
            } else if (dnsQueryType == Type.AAAA) {
                respMsg = Lib.getRediMsgIpv6(dnsMsg, "::1", 60);
            }

            if (respMsg != null) {
                handleDnsResponse(parsedPacket, respMsg.toWire());
                return;
            }
        }

        //Log.i(TAG, "handleDnsRequest: DNS Name " + dnsQueryName + " Allowed, sending to " + destAddr);
        NxLog.debug("handleDnsRequest: DNS Name " + dnsQueryName + " Allowed, sending to " + destAddr);
        DatagramPacket outPacket = new DatagramPacket(dnsRawData, 0, dnsRawData.length, destAddr, parsedUdp.getHeader().getDstPort().valueAsInt());
        eventLoop.forwardPacket(outPacket, parsedPacket);
    }

    /**
     * Translates the destination address in the packet to the real one. In
     * case address translation is not used, this just returns the original one.
     *
     * @param parsedPacket Packet to get destination address for.
     * @return The translated address or null on failure.
     */
    private InetAddress translateDestinationAdress(IpPacket parsedPacket) {
        InetAddress destAddr = null;
        if (upstreamDnsServers.size() > 0) {
            byte[] addr = parsedPacket.getHeader().getDstAddr().getAddress();
            int index = addr[addr.length - 1] - 2;

            try {
                destAddr = upstreamDnsServers.get(index);
            } catch (Exception e) {
                Log.e(TAG, "handleDnsRequest: Cannot handle packets to " + parsedPacket.getHeader().getDstAddr().getHostAddress() + " - not a valid address for this network", e);
                return null;
            }
            Log.d(TAG, String.format("handleDnsRequest: Incoming packet to %s AKA %d AKA %s", parsedPacket.getHeader().getDstAddr().getHostAddress(), index, destAddr));
        } else {
            destAddr = parsedPacket.getHeader().getDstAddr();
            Log.d(TAG, String.format("handleDnsRequest: Incoming packet to %s - is upstream", parsedPacket.getHeader().getDstAddr().getHostAddress()));
        }
        return destAddr;
    }

    /**
     * Interface abstracting away {@link AdVpnThread}.
     */
    interface EventLoop {
        /**
         * Called to send a packet to a remote location
         *
         * @param packet        The packet to send
         * @param requestPacket If specified, the event loop must wait for a response, and then
         *                      call {@link #handleDnsResponse(IpPacket, byte[])} for the data
         *                      of the response, with this packet as the first argument.
         */
        void forwardPacket(DatagramPacket packet, IpPacket requestPacket) throws AdVpnThread.VpnNetworkException;

        /**
         * Write an IP packet to the local TUN device
         *
         * @param packet The packet to write (a response to a DNS request)
         */
        void queueDeviceWrite(IpPacket packet);
    }
}
