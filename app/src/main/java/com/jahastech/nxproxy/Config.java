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

import android.content.SharedPreferences;

import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.NxLog;

//-----------------------------------------------
public class Config {
    private static Config instance = null;

    private String server = "";
    private String token = "";
    private String uname = "";
    private boolean sendUname = true;  // Always true since versionCode 984.

    private boolean loginAlreadyFlag = false;
    private boolean passwdProtection = false;

    //-----------------------------------------------
    private Config() {
        readPreferences();
    }

    //-----------------------------------------------
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    //-----------------------------------------------
    public synchronized boolean isValid() {
        return Lib.isValidIp(server) && Lib.isValidToken(token);
    }

    //-----------------------------------------------
    public synchronized String getServer() {
        return server;
    }

    //-----------------------------------------------
    public synchronized void setServer(String server) {
        this.server = server;
    }

    //-----------------------------------------------
    public synchronized String getToken() {
        return token;
    }

    //-----------------------------------------------
    public synchronized void setToken(String token) {
        this.token = token;
    }

    //-----------------------------------------------
    public synchronized void readPreferences() {
        SharedPreferences preferences = NxProxy.getPreferences();
        if (preferences != null) {
            server = preferences.getString("server", "").trim();
            token = preferences.getString("token", "").trim();
            //sendUname = preferences.getBoolean("sendUname", false);
            passwdProtection = preferences.getBoolean("passwdProtection", false);
        }
    }

    //-----------------------------------------------
    public synchronized void savePreferences() {
        SharedPreferences preferences = NxProxy.getPreferences();
        if (preferences == null) {
            NxLog.error("preferences is null!");
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("server", server);
        editor.putString("token", token);
        //editor.putBoolean("sendUname", sendUname);
        editor.putBoolean("passwdProtection", passwdProtection);
        editor.apply();
    }

    //-----------------------------------------------
    public synchronized void setUname(String _uname) {
        uname = _uname;
    }

    //-----------------------------------------------
    public synchronized String getUname() {
        return uname;
    }

    //-----------------------------------------------
    public synchronized String getServerUrl() {
        return "https://" + server + "/hxlistener";
    }

    //-----------------------------------------------
    public synchronized void setLoginAlreadyFlag(boolean flag) {
        loginAlreadyFlag = flag;
    }

    //-----------------------------------------------
    public synchronized boolean getLoginAlreadyFlag() {
        return loginAlreadyFlag;
    }

    //-----------------------------------------------
    public boolean isSendUname() {
        return sendUname;
    }

    //-----------------------------------------------
    public void setSendUname(boolean sendUname) {
        this.sendUname = sendUname;
    }

    //-----------------------------------------------
    public synchronized boolean isPasswdProtection() {
        return passwdProtection;
    }

    //-----------------------------------------------
    public synchronized void setPasswdProtection(boolean passwdProtection) {
        this.passwdProtection = passwdProtection;
    }
}