package com.eladnava.shutapp.config;

public class SQLite3 {
    // Separate columns using this char
    public static String SEPARATOR_CHAR = (char) 006 + "";

    // Add separator param to query
    public static String SEPARATOR_PARAM = "-separator '" + SEPARATOR_CHAR + "'";

    // SQLite3 binary path
    public static String SQLITE3_BINARY_NAME = "sqlite3";
}
