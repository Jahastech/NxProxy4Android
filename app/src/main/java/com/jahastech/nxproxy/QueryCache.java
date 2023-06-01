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

import java.util.*;

//-----------------------------------------------
public class QueryCache {

    private static QueryCache instance = null;
    private Map<String, QueryCacheData> cacheMap = null;

    private final int MAX_SIZE = 20000;
    private final int MAX_TTL = 60 * 5;

    //-----------------------------------------------
    private QueryCache() {
        cacheMap = new HashMap<>();
    }

    //-----------------------------------------------
    public static QueryCache getInstance() {
        if (instance == null) {
            instance = new QueryCache();
        }
        return instance;
    }

    //-----------------------------------------------
    public synchronized QueryCacheData find(String domain) {
        QueryCacheData qcd = cacheMap.get(domain);

        // It's expired.
        if (qcd != null && (Lib.unixTimestamp() - qcd.stime > MAX_TTL)) {
            NxLog.debug("Cache expired, " + qcd);
            return null;
        }

        return qcd;
    }

    //-----------------------------------------------
    public synchronized void add(String domain, boolean blockFlag, boolean dropFlag) {
        if (cacheMap.size() > MAX_SIZE) {
            cacheMap.clear();
        }

        cacheMap.put(domain, new QueryCacheData(domain, blockFlag, dropFlag));
    }
}
