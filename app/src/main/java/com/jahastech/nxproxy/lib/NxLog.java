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

import android.util.Log;

//-----------------------------------------------
public class NxLog {
    private static final String TAG = "NxProxy";

    //-----------------------------------------------
    public static void info(String format, Object... args) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement callerStackTraceElement = stackTraceElements[3];

        String className = callerStackTraceElement.getClassName();
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1) {
            className = className.substring(lastDotIndex + 1);
        }

        String methodName = callerStackTraceElement.getMethodName();

        try {
            String message = String.format(format, args);
            Log.i(TAG, className + "." + methodName + ": " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //-----------------------------------------------
    public static void debug(String format, Object... args) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement callerStackTraceElement = stackTraceElements[3];

        String className = callerStackTraceElement.getClassName();
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1) {
            className = className.substring(lastDotIndex + 1);
        }

        String methodName = callerStackTraceElement.getMethodName();

        try {
            String message = String.format(format, args);
            Log.d(TAG, className + "." + methodName + ": " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //-----------------------------------------------
    public static void error(String format, Object... args) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement callerStackTraceElement = stackTraceElements[3];

        String className = callerStackTraceElement.getClassName();
        int lastDotIndex = className.lastIndexOf(".");
        if (lastDotIndex != -1) {
            className = className.substring(lastDotIndex + 1);
        }

        String methodName = callerStackTraceElement.getMethodName();

        try {
            String message = String.format(format, args);
            Log.e(TAG, className + "." + methodName + ": " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
