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

//-----------------------------------------------
public class Global {
    public static final String SIGNAL_START = "start.signal.nxfilter.org";
    public static final String SIGNAL_STOP = "stop.signal.nxfilter.org";
    public static final String SIGNAL_PING = "ping.signal.nxfilter.org";

    public static final String SUCC_KW = "127.100.100.100";
    public static final String ERR_KW = "127.100.100.1";

    public static final int UPSTREAM_DNS_TIMEOUT = 6; // seconds
}
