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

import org.json.JSONObject;

import java.util.Arrays;

//-----------------------------------------------
public class NxPolicy {

    private static NxPolicy instance = null;

    public static final int UPDATE_INTERVAL = 300;  // seconds.

    private boolean enableFilter = true;  // Enable it at default.
    private String[] bypassedPackages = new String[0];

    private String curPolicyText = "";
    private int updateTime = 0;

    //-----------------------------------------------
    private NxPolicy() {
        SharedPreferences preferences = NxProxy.getPreferences();
        if (preferences != null) {
            curPolicyText = preferences.getString("curPolicyText", "").trim();
            if (Lib.isNotEmpty(curPolicyText) && parsePolicyText(curPolicyText)) {
                NxLog.info("Policy loaded from preferences");
            }
        }
    }

    //-----------------------------------------------
    public static NxPolicy getInstance() {
        if (instance == null) {
            instance = new NxPolicy();
        }
        return instance;
    }

    //-----------------------------------------------
    private void savePreferences() {
        SharedPreferences preferences = NxProxy.getPreferences();
        if (preferences == null) {
            NxLog.error("preferences is null!");
            return;
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("curPolicyText", curPolicyText);
        editor.apply();
        NxLog.info("Policy saved into preferences");
    }

    //-----------------------------------------------
    private boolean parsePolicyText(String policyText) {
        try {
            JSONObject jsonObject = new JSONObject(policyText);
            enableFilter = jsonObject.getBoolean("ea");
            bypassedPackages = Lib.getJsonArray(jsonObject, "bp");
            return true;
        } catch (Exception e) {
            NxLog.error("Parsing error, policyText = " + policyText);
        }
        return false;
    }

    //-----------------------------------------------
    public synchronized boolean updatePolicy(boolean forceFlag) {
        if (!forceFlag && Lib.unixTimestamp() - updateTime < UPDATE_INTERVAL) {
            NxLog.info("Still fresh policy.");
            return false;
        }

        String newPolicyText = NxTalkie.getInstance().getPolicyText();
        if (Lib.isEmpty(newPolicyText)) {
            NxLog.info("Couldn't get policy from server.");
            return false;
        }

        if (!forceFlag && newPolicyText.equals(curPolicyText)) {
            NxLog.info("Nothing different from the current policy.");
            return false;
        }

        if (parsePolicyText(newPolicyText)) {
            curPolicyText = newPolicyText;
            updateTime = Lib.unixTimestamp();
            savePreferences();

            NxLog.info("Policy updated = " + this);
            NxLog.info(" from curPolicyText = " + curPolicyText);

            return true;
        }

        return false;
    }

    //-----------------------------------------------
    public synchronized boolean isEnableFilter() {
        return enableFilter;
    }

    //-----------------------------------------------
    public synchronized String[] getBypassedPackages() {
        return bypassedPackages;
    }

    @Override
    //-----------------------------------------------
    public String toString() {
        return "NxPolicy{" +
                "enableFilter=" + enableFilter +
                ", bypassedPackages=" + Arrays.toString(bypassedPackages) +
                ", curPolicyText='" + curPolicyText + '\'' +
                ", updateTime=" + updateTime +
                '}';
    }
}
