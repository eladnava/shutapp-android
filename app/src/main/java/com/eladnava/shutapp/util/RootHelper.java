package com.eladnava.shutapp.util;

import com.stericson.RootShell.execution.Command;

public class RootHelper {
    public static void waitForFinish(Command command) throws Exception {
        // Command not finished?
        while (!command.isFinished()) {
            // Need to synchronize in order to use wait()
            synchronized (command) {
                // Check again
                if (!command.isFinished()) {
                    // Wait for notifyAll or until the specified interval
                    command.wait(5000);
                }
            }
        }
    }
}
