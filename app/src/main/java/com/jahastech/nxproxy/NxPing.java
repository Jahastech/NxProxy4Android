/* Written by Jahastech (devel@jahastech.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package com.jahastech.nxproxy;

import android.content.Context;
import android.content.Intent;

import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.LibCtx;
import com.jahastech.nxproxy.lib.NxLog;

import com.jahastech.nxproxy.vpn.AdVpnService;
import com.jahastech.nxproxy.vpn.Command;

//-----------------------------------------------
public class NxPing extends Thread {

    private static NxPing instance = null;

    private Context ctx = null;
    private boolean startedFlag = false;

    private boolean startVpnFlag = false;
    private boolean stopVpnFlag = false;

    //-----------------------------------------------
    private NxPing() {
    }

    //-----------------------------------------------
    public static synchronized NxPing getInstance() {
        if (instance == null) {
            instance = new NxPing();
        }
        return instance;
    }

    //-----------------------------------------------
    public void setStartVpnFlag() {
        startVpnFlag = true;
    }

    //-----------------------------------------------
    public void setStopVpnFlag() {
        stopVpnFlag = true;
    }

    //-----------------------------------------------
    public void setContext(Context _ctx) {
        if (ctx == null) {
            ctx = _ctx.getApplicationContext();
        }
    }

    //-----------------------------------------------
    @Override
    public synchronized void start() {
        if (startedFlag) {
            return;
        }
        super.start();
        startedFlag = true;
    }

    //-----------------------------------------------
    private void restartVpn() {
        NxLog.info("Restarting VPN.");

        // Stop the VPN service if it is running
        if (AdVpnService.vpnStatus != AdVpnService.VPN_STATUS_STOPPED) {
            Intent intent = new Intent(ctx, AdVpnService.class);
            intent.putExtra("COMMAND", Command.STOP.ordinal());
            ctx.startService(intent);
        }

        Lib.sleep(1000 * 2);

        // Start the VPN service if it is not running
        if (AdVpnService.vpnStatus == AdVpnService.VPN_STATUS_STOPPED) {
            Intent intent = AdVpnService.getStartIntent(ctx);
            ctx.startForegroundService(intent);
        }
    }

    //-----------------------------------------------
    @Override
    public void run() {
        NxTalkie nxTalkie = NxTalkie.getInstance();
        NxPolicy nxPolicy = NxPolicy.getInstance();
        Config cfg = Config.getInstance();

        // Try to find username.
        /*
        cfg.setUname(LibCtx.findUsername(ctx));

        NxLog.info("LibCtx.findUsername(ctx) = " + LibCtx.findUsername(ctx));
         */

        // Send start signal.
        nxTalkie.sendSignalStart();

        // First policy update.
        if (nxPolicy.updatePolicy(true)) {
            restartVpn();
        }

        while (true) {
            for (int i = 0; i < 60; i += 5) {
                Lib.sleep(1000 * 5);

                // Deal with start/stop signals.
                if (stopVpnFlag) {
                    nxTalkie.sendSignalStop();
                    stopVpnFlag = false;
                }
                if (startVpnFlag) {
                    nxTalkie.sendSignalStart();
                    startVpnFlag = false;
                }

                /*
                if (Lib.isEmpty(cfg.getUname())) {
                    cfg.setUname(LibCtx.findUsername(ctx));
                }
                 */
            }

            nxTalkie.sendSignalPing();

            if (nxPolicy.updatePolicy(false)) {
                NxLog.info("Restarting VPN by policy update.");
                restartVpn();
            }
        }
    }
}

