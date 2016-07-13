package com.eladnava.shutapp.listeners;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.eladnava.shutapp.config.Logging;
import com.eladnava.shutapp.logic.NotificationHandler;

public class NotificationListener extends NotificationListenerService {
    private NotificationHandler mNotificationHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        // Instantiate notification handler class
        mNotificationHandler = new NotificationHandler(this);

        // Log startup
        Log.d(Logging.TAG, "Notification listener started");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        try {
            // Handle the WhatsApp notification carefully
            mNotificationHandler.handleWhatsAppNotification(notification);
        }
        catch (Exception exc) {
            // Log exception
            Log.e(Logging.TAG, "Handle notification failed", exc);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }
}