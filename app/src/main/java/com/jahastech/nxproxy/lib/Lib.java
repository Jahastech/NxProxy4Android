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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.net.*;
import java.io.*;
import java.util.regex.*;

//-----------------------------------------------
public class Lib {

    private static Pattern PAT_IPV4 = null;
    private static final String _PAT_IPV4 = "^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}$";

    //-----------------------------------------------
    static {
        try {
            PAT_IPV4 = Pattern.compile(_PAT_IPV4, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
        }
    }

    //-----------------------------------------------
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    //-----------------------------------------------
    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    //-----------------------------------------------
    public static boolean isValidIp(String ip) {
        return isNotEmpty(ip) && PAT_IPV4.matcher(ip).matches();
    }

    //-----------------------------------------------
    public static String httpGetText(String url, int connTimeout, int readTimeout, String charset, int maxLine) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(1000 * connTimeout);
            conn.setReadTimeout(1000 * readTimeout);
        } catch (Exception e) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            if (isNotEmpty(charset)) {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
            } else {
                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }

            int cnt = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");

                if (maxLine > 0 && ++cnt >= maxLine) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
            }
        }
        return sb.toString().trim();
    }

    //-----------------------------------------------
    public static String httpGetText(String url, int connTimeout, int readTimeout) {
        return httpGetText(url, connTimeout, readTimeout, null, 0);
    }

    //-----------------------------------------------
    public static boolean isValidToken(String token) {
        return isNotEmpty(token) && token.matches("[a-zA-Z0-9]{8}");
    }

    //-----------------------------------------------
    public static int unixTimestamp() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

    //-----------------------------------------------
    public static Message getRediMsg(Message query, String rediIp, int ttl) {
        Message msg = query;
        try {
            Record question = msg.getQuestion();
            Record answer = Record.fromString(question.getName(), Type.A, DClass.IN, ttl, rediIp, question.getName());

            msg.addRecord(answer, Section.ANSWER);

            msg.getHeader().setRcode(Rcode.NOERROR);
            msg.getHeader().setFlag(Flags.QR);
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }

        return msg;
    }

    //-----------------------------------------------
    public static Message getRediMsgIpv6(Message query, String rediIp, int ttl) {
        Message msg = query;
        try {
            Record question = msg.getQuestion();
            Record answer = Record.fromString(question.getName(), Type.AAAA, DClass.IN, ttl, rediIp, question.getName());

            msg.addRecord(answer, Section.ANSWER);

            msg.getHeader().setRcode(Rcode.NOERROR);
            msg.getHeader().setFlag(Flags.QR);
        } catch (Exception e) {
//            e.printStackTrace();
            return null;
        }

        return msg;
    }

    //-----------------------------------------------
    public static Message getRefusedMsg(Message query) {
        Message msg = query;
        try {
            msg.getHeader().setRcode(Rcode.REFUSED);
            msg.getHeader().setFlag(Flags.QR);
        } catch (Exception e) {
            //e.printStackTrace();
            return null;
        }

        return msg;
    }

    //-----------------------------------------------
    public static void sleep(int msec) {
        try {
            Thread.sleep(msec);
        } catch (Exception e) {
        }
    }

    //-----------------------------------------------
    public static String[] getJsonArray(JSONObject jsonObject, String key) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray(key);
        int length = jsonArray.length();
        String[] array = new String[length];
        for (int i = 0; i < length; i++) {
            array[i] = jsonArray.getString(i);
        }
        return array;
    }
}
