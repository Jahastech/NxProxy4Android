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

import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.NxLog;

//-----------------------------------------------
public class NxTalkie {

    private static NxTalkie instance = null;
    private Config cfg = null;

    public static final int SUCCESS = 0;
    public static final int LOGIN_ERR = -1;
    public static final int CONN_ERR = -2;
    public static final int INVALID_CONFIG = -3;
    private static final int CONN_TIMEOUT = 2;
    private static final int READ_TIMEOUT = 2;

    private final int MAX_CONN_ERR_CNT = 3;
    private int connErrCnt = 0;

    //-----------------------------------------------
    private NxTalkie() {
        cfg = Config.getInstance();
    }

    //-----------------------------------------------
    public static NxTalkie getInstance() {
        if (instance == null) {
            instance = new NxTalkie();
        }
        return instance;
    }

    //-----------------------------------------------
    private String makeQueryUrl(String domain) {
        if (!cfg.isValid()) {
            return "";
        }

        if(cfg.isSendUname()){
            return String.format("%s?domain=%s&token=%s&uname=%s",
                    cfg.getServerUrl(), domain, cfg.getToken(), cfg.getUname());
        }

        return String.format("%s?domain=%s&token=%s&uname=%s",
                cfg.getServerUrl(), domain, cfg.getToken(), cfg.getUname());
    }

    //-----------------------------------------------
    public int sendSignalTest() {
        String query = makeQueryUrl(Global.SIGNAL_PING);
        NxLog.debug("query = %s.", query);
        if (Lib.isEmpty(query)) {
            return INVALID_CONFIG;
        }

        String resText = Lib.httpGetText(query, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.info("resText = %s.", resText);
        if (Lib.isEmpty(resText)) {
            return CONN_ERR;
        }

        if (resText.equals(Global.ERR_KW)) {
            return LOGIN_ERR;
        }

        return SUCCESS;
    }

    //-----------------------------------------------
    public boolean sendSignalPing() {
        String query = makeQueryUrl(Global.SIGNAL_PING);
        NxLog.debug("query = %s.", query);
        if (Lib.isEmpty(query)) {
            return false;
        }

        String resText = Lib.httpGetText(query, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.debug("resText = %s.", resText);
        if (Lib.isEmpty(resText)) {
            return false;
        }

        // Reset connErrCnt when the connection restored.
        if (hasMaxConnErrCnt()) {
            resetConnErrCnt();
        }

        return resText.equals(Global.SUCC_KW);
    }

    //-----------------------------------------------
    public boolean sendSignalStart() {
        String query = makeQueryUrl(Global.SIGNAL_START);
        NxLog.debug("query = %s.", query);
        if (Lib.isEmpty(query)) {
            return false;
        }

        String resText = Lib.httpGetText(query, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.debug("resText = %s.", resText);
        if (Lib.isEmpty(resText)) {
            return false;
        }

        return resText.equals(Global.SUCC_KW);
    }

    //-----------------------------------------------
    public boolean sendSignalStop() {
        String query = makeQueryUrl(Global.SIGNAL_STOP);
        NxLog.debug("query = %s.", query);
        if (Lib.isEmpty(query)) {
            return false;
        }

        String resText = Lib.httpGetText(query, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.debug("resText = %s.", resText);
        if (Lib.isEmpty(resText)) {
            return false;
        }

        return resText.equals(Global.SUCC_KW);
    }

    //-----------------------------------------------
    private String sendQuery(String domain) {
        String query = makeQueryUrl(domain);
        NxLog.debug("query url = %s.", query);
        if (Lib.isEmpty(query)) {
            return "";
        }

        String resText = Lib.httpGetText(query, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.debug("resText = %s.", resText);

        if (Lib.isEmpty(resText)) {
            return "";
        }

        return resText;
    }

    //-----------------------------------------------
    public boolean isBlocked(String domain) {
        // Find cache.
        QueryCache qc = QueryCache.getInstance();
        QueryCacheData qcd = qc.find(domain);

        if (qcd != null) {
            NxLog.debug("Cache found, " + qcd);
            if (qcd.blockFlag || qcd.dropFlag) {
                return true;
            } else {
                return false;
            }
        }

        // Bypass query
        if (hasMaxConnErrCnt()) {
            NxLog.debug("Bypassing query for %s as we can't reach %s.", domain, cfg.getServer());
            return false;
        }

        // Talk to server.
        String resText = sendQuery(domain);
        if (Lib.isEmpty(resText)) {
            addConnErrCnt();
            return false;
        }

        boolean blockFlag = false;
        boolean dropFlag = false;

        if (resText.equals("/BLOCK")) {
            blockFlag = true;
        } else if (resText.equals("/DROP")) {
            blockFlag = true;
        }

        // Add cache.
        qc.add(domain, blockFlag, dropFlag);

        return blockFlag || dropFlag;
    }

    //-----------------------------------------------
    private synchronized void addConnErrCnt() {
        connErrCnt++;

        if (connErrCnt == MAX_CONN_ERR_CNT) {
            NxLog.info("ServerState.addConnErrCnt, Max error count reached. We bypass filtering.");
        }
    }

    //-----------------------------------------------
    private synchronized void resetConnErrCnt() {
        if (connErrCnt >= MAX_CONN_ERR_CNT) {
            NxLog.info("ServerState.resetConnErrCnt, Connection restored. We resume filtering.");
        }

        connErrCnt = 0;
    }

    //-----------------------------------------------
    private synchronized boolean hasMaxConnErrCnt() {
        return connErrCnt >= MAX_CONN_ERR_CNT;
    }

    //-----------------------------------------------
    public String getPolicyText() {
        if (!cfg.isValid()) {
            return "";
        }

        String url = String.format("%s?action=/NXPA", cfg.getServerUrl());
        String resText = Lib.httpGetText(url, CONN_TIMEOUT, READ_TIMEOUT);
        NxLog.debug("url = %s, resText = %s.", url, resText);

        return resText;
    }
}
