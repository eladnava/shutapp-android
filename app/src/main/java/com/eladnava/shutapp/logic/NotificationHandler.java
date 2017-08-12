package com.eladnava.shutapp.logic;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.eladnava.shutapp.config.Dumpsys;
import com.eladnava.shutapp.config.Logging;
import com.eladnava.shutapp.config.WhatsApp;
import com.eladnava.shutapp.util.DB;
import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.execution.Command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NotificationHandler {
    private DB mDB;
    private NotificationListenerService mNotificationListener;

    public NotificationHandler(NotificationListenerService notificationListener) {
        // Instantiate SQLite3 DB accessor
        mDB = new DB(notificationListener);

        // Save for later
        mNotificationListener = notificationListener;
    }

    public void handleWhatsAppNotification(StatusBarNotification notification) throws Exception {
        // Ignore non-WhatsApp notifications
        if (!notification.getPackageName().equals(WhatsApp.PACKAGE)) {
            return;
        }

        // Log the notification
        Log.d(Logging.TAG, "WhatsApp notification detected");

        // List of unread chat jids
        List<String> jids = new ArrayList<>();

        // Get message jid (from tag or extras)
        String notificationJid = getNotificationJid(notification);

        // Got one?
        if (notificationJid != null){
            // Add it
            jids.add(notificationJid);
        }
        else {
            // Null JID, probably more than 1 unread chat
            jids = getUnreadChatJids();
        }

        // Keep track of whether we found a muted JID or not
        boolean foundMutedJid = false;

        // Traverse unread JIDs
        for (String jid : jids) {
            // Verify the jid is currently muted
            if (!isJidMuted(jid)) {
                return;
            }

            // First muted JID we encounter?
            if (!foundMutedJid) {
                // Log what we're doing
                Log.d(Logging.TAG, "Hiding notification containing jid " + jid);

                // Hide notification ASAP
                mNotificationListener.cancelNotification(notification.getKey());
            }

            // Prevent hiding the notification twice
            foundMutedJid = true;

            // Log what we're doing
            Log.d(Logging.TAG, "Marking jid " + jid + " as read");

            // Set badge count to 0 for this jid
            updateChatUnreadCount(jid);

            // Set sort timestamp to group creation date
            updateChatSortTimestamp(jid);
        }

        // Did we silence any muted chats?
        if (foundMutedJid) {
            // Restart WhatsApp interface - it will regenerate the notification if there are any unread chats left
            restartWhatsAppIfNotFocused();
        }
    }

    private List<String> getUnreadChatJids() throws Exception {
        // List of unread JIDs
        List<String> unreadJids = new ArrayList<>();

        // Execute SQL query
        List<HashMap<String, String>> rows = mDB.select(new String[]{"jid"}, "wa_contacts", "unseen_msg_count > 0", WhatsApp.CONTACTS_DB);

        // Traverse rows
        for (HashMap<String, String> row : rows) {
            // Add JID
            unreadJids.add(row.get("jid"));
        }

        // All done
        return unreadJids;
    }

    private String getNotificationJid(StatusBarNotification notification) throws Exception {
        // Attempt to fetch it from tag
        String jid = notification.getTag();

        // Got it?
        if (jid != null) {
            return jid;
        }

        // Attempt to retrieve group name from notification extras
        String subject = notification.getNotification().extras.get("android.title").toString();

        // Failed?
        if (subject != null) {
            // Attempt to fetch jid by group name
            jid = getJidBySubject(subject);
        }

        // Return jid (might be null)
        return jid;
    }

    private void restartWhatsAppIfNotFocused() throws Exception {
        // First check if WhatsApp is focused using dumpsys
        Command command = new Command(0, Dumpsys.GET_FOREGROUND_APP_COMMAND) {
            @Override
            public void commandOutput(int id, String line) {
                // Focused app is not WhatsApp?
                if (!line.contains(WhatsApp.PACKAGE)) {
                    try {
                        // Try to restart WhatsApp
                        restartWhatsApp();
                    }
                    catch (Exception exc) {
                        // Log exception
                        Log.e(Logging.TAG, "Restarting WhatsApp failed", exc);
                    }
                }
                else {
                    // Log WhatsApp state
                    Log.d(Logging.TAG, "WhatsApp is currently focused");
                }

                // Must call the super method when overriding!
                super.commandOutput(id, line);
            }

            @Override
            public void commandTerminated(int id, String reason) {
            }

            @Override
            public void commandCompleted(int id, int exitCode) {
            }

        };

        // Run the dumpsys command
        RootShell.getShell(true).add(command);
    }

    private void restartWhatsApp() throws Exception {
        // Log restart
        Log.d(Logging.TAG, "Restarting " + WhatsApp.PACKAGE + " for changes to take effect");

        // Stop WhatsApp services
        RootShell.getShell(true).add(new Command(0, mDB.getPathToBusybox() + " " + WhatsApp.STOP_WHATSAPP_COMMAND));

        // Wait 200ms
        Thread.sleep(200);

        // Start messaging service
        RootShell.getShell(true).add(new Command(0, WhatsApp.START_MESSAGING_SERVICE_COMMAND));
    }

    private void updateChatUnreadCount(String jid) throws Exception {
        // Initialize contact update object
        HashMap<String, String> contact = new HashMap<>();

        // Set unseen msg count to 0
        contact.put("unseen_msg_count", "0");

        // Update contacts table
        mDB.update("wa_contacts", contact, "jid = '" + jid + "'", WhatsApp.CONTACTS_DB);

        // Initialize chat update object
        HashMap<String, String> chat = new HashMap<>();

        // Set unseen msg count to 0
        chat.put("unseen_message_count", "0");

        // Update contacts table
        mDB.update("chat_list", chat, "key_remote_jid = '" + jid + "'", WhatsApp.MESSAGE_DB);
    }

    private void updateChatSortTimestamp(String jid) throws Exception {
        // Set last message timestamp to 48 hours ago
        String sortTimestamp = (System.currentTimeMillis() - (1000 * 60 * 60 * 48)) + "";

        // Initialize chat list update object
        HashMap<String, String> chat = new HashMap<>();

        // Set the sort timestamp of this group to the creation date
        chat.put("sort_timestamp", sortTimestamp);

        // Update chat list table
        mDB.update("chat_list", chat, "key_remote_jid = '" + jid + "'", WhatsApp.MESSAGE_DB);
    }

    private String getJidBySubject(String subject) throws Exception {
        // Execute SQL query
        List<HashMap<String, String>> rows = mDB.select(new String[]{"key_remote_jid"}, "chat_list", "subject = '" + subject + "'", WhatsApp.MESSAGE_DB);

        // Couldn't find jid for some reason?
        if (rows.size() == 0) {
            return getJidByContactName(subject);
        }

        // Get chat jid
        return rows.get(0).get("key_remote_jid");
    }

    private String getJidByContactName(String name) throws Exception {
        // Execute SQL query
        List<HashMap<String, String>> rows = mDB.select(new String[]{"jid"}, "wa_contacts", "display_name = '" + name+ "'", WhatsApp.CONTACTS_DB);

        // Couldn't find jid for some reason?
        if (rows.size() == 0) {
            return null;
        }

        // Return contact jid
        return rows.get(0).get("jid");
    }

    private boolean isJidMuted(String jid) throws Exception {
        // Execute SQL query
        List<HashMap<String, String>> rows = mDB.select(new String[]{"mute_end"}, "settings", "jid = '" + jid + "'", WhatsApp.SETTINGS_DB);

        // No settings configured for this jid?
        if (rows.size() == 0) {
            return false;
        }

        // Get first row
        HashMap<String, String> row = rows.get(0);

        // Get mute end timestamp as string
        String muteEnd = row.get("mute_end");

        // Empty value?
        if (muteEnd == null || muteEnd.equals("")) {
            return false;
        }

        // Extract mute_end timestamp
        long muteEndTimestamp = Long.parseLong(muteEnd);

        // Mute ended?
        if (muteEndTimestamp < System.currentTimeMillis()) {
            return false;
        }

        // The jid is still muted!
        return true;
    }
}
