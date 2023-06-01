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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.jahastech.nxproxy.Config;
import com.jahastech.nxproxy.MainActivity;
import com.jahastech.nxproxy.NxPing;
import com.jahastech.nxproxy.NxTalkie;
import com.jahastech.nxproxy.R;
import com.jahastech.nxproxy.lib.NxLog;

import java.lang.ref.WeakReference;

//-----------------------------------------------
public class AdVpnService extends VpnService implements Handler.Callback {

    public static final int NOTIFICATION_ID_STATE = 10;

    public static final String SERVICE_RUNNING = "com.jahastech.nxproxy.service";
    public static final int VPN_STATUS_STARTING = 0;
    public static final int VPN_STATUS_RUNNING = 1;
    public static final int VPN_STATUS_STOPPING = 2;
    public static final int VPN_STATUS_RECONNECTING_NETWORK_ERROR = 5;
    public static final int VPN_STATUS_STOPPED = 6;
    private static final int VPN_MSG_STATUS_UPDATE = 0;
    private static final int VPN_MSG_NETWORK_CHANGED = 1;

    // TODO: Temporary Hack til refactor is done
    public static int vpnStatus = VPN_STATUS_STOPPED;
    private Handler handler = null;
    private AdVpnThread vpnThread = null;
    private BroadcastReceiver connectivityChangedReceiver = null;
    private NotificationCompat.Builder notificationBuilder = null;

    //-----------------------------------------------
    @Override
    public void onCreate() {
        super.onCreate();

        handler = new MyHandler(this);

        vpnThread = new AdVpnThread(this, new AdVpnThread.Notify() {
            @Override
            public void run(int value) {
                handler.sendMessage(handler.obtainMessage(VPN_MSG_STATUS_UPDATE, value, 0));
            }
        });

        connectivityChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NxLog.info("intent = " + intent);
                handler.sendMessage(handler.obtainMessage(VPN_MSG_NETWORK_CHANGED, intent));
            }
        };

        notificationBuilder = new NotificationCompat.Builder(this, AdVpnService.SERVICE_RUNNING)
                .setSmallIcon(R.drawable.ic_state_nx)
                .setPriority(NotificationManager.IMPORTANCE_MIN);

        AdVpnService.createNotificationChannel(this);
    }

    //-----------------------------------------------
    public static void checkStartVpnOnBoot(Context context) {
        Log.i("BOOT", "Checking whether to start ad buster on boot");
        NxLog.info("Starting checkStartVpnOnBoot");

        if (!Config.getInstance().isValid()) {
            return;
        }

        if (VpnService.prepare(context) != null) {
            Log.i("BOOT", "VPN preparation not confirmed by user, changing enabled to false");
        }

        Log.i("BOOT", "Starting ad buster from boot");
        AdVpnService.createNotificationChannel(context);

        Intent intent = AdVpnService.getStartIntent(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    //-----------------------------------------------
    @NonNull
    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, AdVpnService.class);
        intent.putExtra("COMMAND", Command.START.ordinal());

        intent.putExtra("NOTIFICATION_INTENT",
                PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));

        return intent;
    }

    //-----------------------------------------------
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        NxLog.debug("intent = " + intent);
        switch (intent == null ? Command.START : Command.values()[intent.getIntExtra("COMMAND", Command.START.ordinal())]) {
            case START:
                startVpn(intent == null ? null : (PendingIntent) intent.getParcelableExtra("NOTIFICATION_INTENT"));
                break;
            case STOP:
                stopVpn();
                break;
        }

        return Service.START_STICKY;
    }

    //-----------------------------------------------
    private void updateVpnStatus(int status) {
        this.vpnStatus = status;

        if (AdVpnService.vpnStatus == AdVpnService.VPN_STATUS_STOPPED) {
            NxTalkie.getInstance().sendSignalStop();
        }

        notificationBuilder.setContentTitle(getString(R.string.notification_running));
        startForeground(NOTIFICATION_ID_STATE, notificationBuilder.build());
    }

    //-----------------------------------------------
    private void startVpn(PendingIntent notificationIntent) {
        NxLog.info("Starting VPN service.");
        notificationBuilder.setContentTitle(getString(R.string.notification_title));
        if (notificationIntent != null)
            notificationBuilder.setContentIntent(notificationIntent);

        registerReceiver(connectivityChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        restartVpnThread();
        NxPing.getInstance().setStartVpnFlag();
    }

    //-----------------------------------------------
    private void restartVpnThread() {
        NxLog.info("Trying to restart vpnThread.");
        if (vpnThread == null) {
            NxLog.error("Can't restart vpnThread as it is null.");
            return;
        }

        vpnThread.stopThread();
        vpnThread.startThread();
    }

    //-----------------------------------------------
    private void stopVpnThread() {
        NxLog.info("Trying to stop vpnThread.");
        if (vpnThread == null) {
            NxLog.error("Can't stop vpnThread as it is null.");
            return;
        }

        vpnThread.stopThread();
        //vpnThread = null;
        NxPing.getInstance().setStopVpnFlag();
    }

    //-----------------------------------------------
    private void stopVpn() {
        NxLog.info("Stopping Service");
        stopVpnThread();

        try {
            unregisterReceiver(connectivityChangedReceiver);
        } catch (IllegalArgumentException e) {
            NxLog.info("Ignoring exception on unregistering receiver");
        }
        stopSelf();
    }

    //-----------------------------------------------
    @Override
    public void onDestroy() {
        NxLog.info("Destroyed, shutting down");
        stopVpn();
    }

    //-----------------------------------------------
    @Override
    public boolean handleMessage(Message message) {
        if (message == null) {
            return true;
        }

        switch (message.what) {
            case VPN_MSG_STATUS_UPDATE:
                updateVpnStatus(message.arg1);
                break;
            case VPN_MSG_NETWORK_CHANGED:
                connectivityChanged((Intent) message.obj);
                break;
            default:
                throw new IllegalArgumentException("Invalid message with what = " + message.what);
        }
        return true;
    }

    //-----------------------------------------------
    private void connectivityChanged(Intent intent) {
        if (intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, 0) == ConnectivityManager.TYPE_VPN) {
            NxLog.info("Ignoring connectivity changed for our own network");
            return;
        }

        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            NxLog.error("Got bad intent on connectivity changed " + intent.getAction());
        }
        if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            NxLog.info("Connectivity changed to no connectivity, wait for a network");
            stopVpnThread();
        } else {
            NxLog.info("Network changed, try to reconnect");
            restartVpnThread();
        }
    }

    //-----------------------------------------------
    private static void createNotificationChannel(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        NotificationChannel runningChannel = new NotificationChannel(SERVICE_RUNNING, ctx.getString(R.string.notifications_running), NotificationManager.IMPORTANCE_MIN);
        runningChannel.setDescription(ctx.getString(R.string.notifications_running_desc));
        runningChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(runningChannel);
    }

    //-----------------------------------------------
    /* The handler may only keep a weak reference around, otherwise it leaks */
    private static class MyHandler extends Handler {
        private final WeakReference<Handler.Callback> callback;

        public MyHandler(Handler.Callback callback) {
            this.callback = new WeakReference<Callback>(callback);
        }

        //-----------------------------------------------
        @Override
        public void handleMessage(Message msg) {
            Handler.Callback callback = this.callback.get();
            if (callback != null) {
                callback.handleMessage(msg);
            }
            super.handleMessage(msg);
        }
    }
}
