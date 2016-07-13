package com.eladnava.shutapp.activities;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.eladnava.shutapp.R;
import com.eladnava.shutapp.config.Logging;
import com.eladnava.shutapp.listeners.NotificationListener;
import com.stericson.RootShell.RootShell;

public class Main extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Main layout
        setContentView(R.layout.activity_main);

        // Make sure we have root access (display dialog otherwise)
        if (verifyRootAccessAvailable()) {
            // User hasn't enabled notification access yet?
            if (!isNotificationListenerActive()) {
                // Ask user to grant notification access permission to app
                launchNotificationAccess();
            }
        }
    }

    void launchNotificationAccess() {
        // Try to launch the notification access page
        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    boolean isNotificationListenerActive() {
        // Not using Android 4.3+? (notification access not supported before then)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }

        // Get activity manager instance for retrieving running services
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        // Traverse running services
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            // Is our listener running?
            if (NotificationListener.class.getName().equals(service.service.getClassName())) {
                // Log this
                Log.d(Logging.TAG, "Notification listener is running");

                // Return true
                return true;
            }
        }

        // No, it is not running
        return false;
    }

    private boolean verifyRootAccessAvailable() {
        // No "su" binary?
        if (!RootShell.isRootAvailable()) {
            // Show fatal error dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_root)
                    .setMessage(R.string.no_root_desc)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // End the activity
                            finish();
                        }
                    })
                    .create().show();

            // No root
            return false;
        }

        // No root access given?
        if (!RootShell.isAccessGiven()) {
            // Show fatal error dialog
            new AlertDialog.Builder(this)
                    .setTitle(R.string.no_root_access)
                    .setMessage(R.string.no_root_access_desc)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            // End the activity
                            finish();
                        }
                    })
                    .create().show();

            // No root access
            return false;
        }

        // We're good!
        return true;
    }
}
