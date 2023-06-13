package com.jahastech.nxproxy.lib;

import java.util.*;

//-----------------------------------------------
public class ParamEncoder {

    private final int PREFIX_LENGTH = 4;
    private final int MIDFIX_LENGTH = 2;
    private final int LAST_CHARS_LENGTH = 2;
    private final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private Base64.Encoder b64enc = null;

    //-----------------------------------------------
    public ParamEncoder() {
        b64enc = Base64.getEncoder();
    }

    //-----------------------------------------------
    private String addMidFix(String line) {
        int midIndex = line.length() - LAST_CHARS_LENGTH;
        String leftPart = line.substring(0, midIndex);
        String rightPart = line.substring(midIndex);
        String midFix = generateRandomString(MIDFIX_LENGTH);
        return leftPart + midFix + rightPart;
    }

    //-----------------------------------------------
    public synchronized String encode(String line) {
        byte[] encodedBytes = b64enc.encode(line.getBytes());
        String encodedString = new String(encodedBytes);
        String encodedWithPrefix = generateRandomString(PREFIX_LENGTH) + encodedString;
        return addMidFix(encodedWithPrefix);
    }

    //-----------------------------------------------
    private String generateRandomString(int len) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < len; i++) {
            char randomChar = CHARACTERS.charAt(random.nextInt(CHARACTERS.length()));
            sb.append(randomChar);
        }
        return sb.toString();
    }
}
