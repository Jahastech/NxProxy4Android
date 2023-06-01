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

//-----------------------------------------------
public class QueryCacheData {

    public String domain = "";
    public boolean blockFlag = false;
    public boolean dropFlag = false;
    public int stime = 0;

    //-----------------------------------------------
    public QueryCacheData(String _domain, boolean _blockFlag, boolean _dropFlag) {
        domain = _domain;
        blockFlag = _blockFlag;
        dropFlag = _dropFlag;
        stime = Lib.unixTimestamp();
    }

    //-----------------------------------------------
    @Override
    public String toString() {
        return "QueryCacheData{" +
                "domain='" + domain + '\'' +
                ", blockFlag=" + blockFlag +
                ", dropFlag=" + dropFlag +
                ", stime=" + stime +
                '}';
    }
}
