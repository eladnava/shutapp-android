package com.eladnava.shutapp.config;

public class Dumpsys {
    // Returns information about foreground app
    public static String GET_FOREGROUND_APP_COMMAND = "dumpsys window windows | grep -E 'mFocusedApp'";
}
