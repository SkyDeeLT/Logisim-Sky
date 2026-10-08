package com.cburch.logisim.util;

import java.io.File;
import java.io.IOException;
import javax.swing.JMenuBar;

public class MacCompatibility {
    private MacCompatibility() { }

    public static final double mrjVersion = 0.0;

    public static boolean isAboutAutomaticallyPresent() { return false; }
    public static boolean isPreferencesAutomaticallyPresent() { return false; }
    public static boolean isQuitAutomaticallyPresent() { return false; }

    public static boolean isSwingUsingScreenMenuBar() {
        String value = System.getProperty("apple.laf.useScreenMenuBar", "false");
        return "true".equalsIgnoreCase(value);
    }

    public static void setFramelessJMenuBar(JMenuBar menubar) { }

    public static void setFileCreatorAndType(File dest, String app, String type)
            throws IOException { }
}
