/* Written by Jahastech (devel@jahastech.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package com.jahastech.nxproxy.lib;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.widget.Toast;

//-----------------------------------------------
public class LibCtx {

    //-----------------------------------------------
    public static void toastText(Context ctx, String msg) {
        Toast toast = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    //-----------------------------------------------
    public static String findUsername(Context ctx) {
        Account[] accounts = AccountManager.get(ctx).getAccountsByType("com.google");
        if (accounts.length > 0) {
            return accounts[0].name.replaceFirst("@.*$", "");
        }
        return "";
    }
}
