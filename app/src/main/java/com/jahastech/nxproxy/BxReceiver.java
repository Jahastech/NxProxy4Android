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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jahastech.nxproxy.lib.NxLog;

import com.jahastech.nxproxy.vpn.AdVpnService;

//-----------------------------------------------
public class BxReceiver extends BroadcastReceiver {

    //-----------------------------------------------
    @Override
    public void onReceive(Context ctx, Intent intent) {
        // Modified by Jahastech.
        NxLog.info("BxReceiver.onReceive, Starting NxPing.");
        NxPing nxPing = NxPing.getInstance();
        nxPing.setContext(ctx);
        nxPing.start();

        NxLog.info("BxReceiver.onReceive, AdVpnService.checkStartVpnOnBoot.");
        AdVpnService.checkStartVpnOnBoot(ctx);
    }
}
