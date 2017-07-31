package com.eladnava.shutapp.config;

public class WhatsApp {
    // WhatsApp package name
    public static final String PACKAGE = "com.whatsapp";

    // WhatsApp various internal database paths
    public static String CONTACTS_DB = "/data/data/" + PACKAGE + "/databases/wa.db";
    public static String MESSAGE_DB = "/data/data/" + PACKAGE + "/databases/msgstore.db";
    public static String SETTINGS_DB = "/data/data/" + PACKAGE + "/databases/chatsettings.db";

    // WhatsApp restart commands
    public static String STOP_WHATSAPP_COMMAND = "killall " + PACKAGE;
    public static String START_MESSAGING_SERVICE_COMMAND = "am startservice -n com.whatsapp/com.whatsapp.messaging.MessageService -a com.whatsapp.MessageService.START";
}
