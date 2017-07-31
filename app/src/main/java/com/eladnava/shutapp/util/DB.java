package com.eladnava.shutapp.util;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.eladnava.shutapp.R;
import com.eladnava.shutapp.config.Busybox;
import com.eladnava.shutapp.config.Logging;
import com.eladnava.shutapp.config.SQLite3;
import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DB {
    Context mContext;

    public DB(Context context) {
        // Save context instance
        this.mContext = context;
    }

    public String getSQLCommand(String sql, String db) {
        // Build SQL command
        return getPathToSQLite3() + " " + SQLite3.SEPARATOR_PARAM + " " + db + " \"" + sql + "\"";
    }

    public void update(String tableName, HashMap<String, String> row, String whereClause, String dbName) throws Exception {
        // Make sure we installed binaries!
        installBinaries();

        // Prepare set array
        List<String> set = new ArrayList<>();

        // Add items, escape single-quotes
        for (Map.Entry<String, String> column : row.entrySet()) {
            // Apparently, SQLite3 escapes single-quotes with another quote
            set.add(column.getKey() + " = '" + column.getValue().replace("'", "''") + "'");
        }

        // Convert to CSV string
        String setList = TextUtils.join(", ", set);

        // Generate SQL update statement
        String sql = "UPDATE " + tableName + " SET " + setList + " WHERE " + whereClause;

        // Prepare the reboot command
        Command sqlCommand = new Command(0, getSQLCommand(sql, dbName));

        try {
            // Execute the command
            RootShell.getShell(true).add(sqlCommand);
        }
        catch (Exception exc) {
            // Log error to logcat
            Log.d(Logging.TAG, "Update command failed", exc);
        }

        // Wait for it...
        RootHelper.waitForFinish(sqlCommand);
    }

    public List<HashMap<String, String>> select(final String[] columns, String tableName, String whereClause, String dbName) throws Exception {
        // Make sure we installed binaries!
        installBinaries();

        // Create a list of rows
        final List<HashMap<String, String>> rows = new ArrayList<>();

        // Convert to CSV
        String columnsList = TextUtils.join(", ", columns);

        // Generate SQL statement
        String sql = "SELECT " + columnsList + " FROM " + tableName + " WHERE " + whereClause;

        // Execute the query
        Command sqlCommand = new Command(0, false, getSQLCommand(sql, dbName)) {
            @Override
            public void commandOutput(int id, String line) {
                // Add row to rows list
                rows.add(convertRowToHashMap(columns, line));

                // Must call the super method when overriding!
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String error) {
                // Log error
                Log.e(Logging.TAG, "Select command failed: " + error);
            }
        };

        // Execute the command
        RootShell.getShell(true).add(sqlCommand);

        // Wait for it...
        RootHelper.waitForFinish(sqlCommand);

        // Return rows
        return rows;
    }

    public HashMap<String, String> convertRowToHashMap(String[] columns, String line) {
        // Split row into columns
        List<String> values = new ArrayList<>(Arrays.asList(line.split(SQLite3.SEPARATOR_CHAR)));

        // Not enough values?
        if (values.size() < columns.length) {
            // Add empty values
            for (int i = values.size(); i < columns.length; i++) {
                values.add("");
            }
        }

        // Create a new hash map
        HashMap<String, String> row = new HashMap();

        // Add all columns to hash map
        for (int i = 0; i < values.size(); i++) {
            row.put(columns[i], values.get(i));
        }

        // Return row
        return row;
    }

    public void installBinaries() throws Exception {
        // Make sure we have root!
        if (!RootShell.isRootAvailable()) {
            throw new Exception(mContext.getString(R.string.no_root_desc));
        }

        // Make sure we have permission
        if (!RootShell.isAccessGiven()) {
            throw new Exception(mContext.getString(R.string.no_root_access_desc));
        }

        // Get default SQLite3 binary
        int resource = R.raw.sqlite3_binary_4x;

        // Add support for post-5.x devices (PIE enforcement)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            resource = R.raw.sqlite3_binary_5x;
        }

        // Install SQLite3 binary
        installBinary(getPathToSQLite3(), resource);

        // Install Busybox binary
        installBinary(getPathToBusybox(), R.raw.busybox_binary);
    }

    public String getPathToSQLite3() {
        // Use "ctx.getFilesDir()" for better compatibility
        return mContext.getFilesDir().getAbsolutePath() + "/" + SQLite3.SQLITE3_BINARY_NAME;
    }

    public String getPathToBusybox() {
        // Use "ctx.getFilesDir()" for better compatibility
        return mContext.getFilesDir().getAbsolutePath() + "/" + Busybox.BUSYBOX_BINARY_NAME;
    }

    void installBinary(String path, int resource) throws Exception {
        // Get path to file
        File installationPath = new File(path);

        // File exists?
        if (installationPath.exists()) {
            return;
        }

        // Open file from raw assets
        InputStream rawResource = mContext.getResources().openRawResource(resource);

        // Create output stream
        OutputStream internalStorage = new FileOutputStream(installationPath);

        // Define temp length variable
        int length;

        // Define temporary byte buffer
        byte[] buffer = new byte[1024];

        // Read from raw resource until we reach EOF
        while ((length = rawResource.read(buffer)) > 0) {
            // Write to internal storage
            internalStorage.write(buffer, 0, length);
        }

        // Close both streams
        rawResource.close();
        internalStorage.close();

        // CHMOD 777 (so we can exec)
        String chmodCommand = "chmod 777 " + installationPath.getAbsolutePath();

        // Execute the command
        Command rootCommand = new Command(0, chmodCommand);

        // Execute the command
        RootShell.getShell(true).add(rootCommand);

        // Wait for it...
        RootHelper.waitForFinish(rootCommand);
    }
}