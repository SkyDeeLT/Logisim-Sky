/* Copyright (c) 2026. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

/** Keeps every application window usable across DPI and monitor changes. */
public final class WindowGeometry {
    private static final int OUTER_MARGIN = 16;

    private WindowGeometry() { }

    public static void center(Window window, Component relativeTo) {
        if (window == null) return;
        try { window.setLocationRelativeTo(relativeTo); }
        catch (Throwable ignored) { }
        ensureUsable(window, false);
    }

    public static void ensureUsable(Window window, boolean centerIfUnpositioned) {
        if (window == null) return;
        Rectangle usable = usableBounds(window.getGraphicsConfiguration());

        Dimension minimum = window.getMinimumSize();
        int maxWidth = Math.max(320, usable.width - OUTER_MARGIN * 2);
        int maxHeight = Math.max(240, usable.height - OUTER_MARGIN * 2);
        int width = Math.max(Math.min(window.getWidth(), maxWidth),
                Math.min(Math.max(1, minimum.width), maxWidth));
        int height = Math.max(Math.min(window.getHeight(), maxHeight),
                Math.min(Math.max(1, minimum.height), maxHeight));
        if (width != window.getWidth() || height != window.getHeight()) {
            window.setSize(width, height);
        }

        Point location = window.getLocation();
        Rectangle current = new Rectangle(location.x, location.y, width, height);
        boolean outside = !intersectsAnyUsableScreen(current);
        boolean unpositioned = centerIfUnpositioned && location.x == 0 && location.y == 0;
        if (outside || unpositioned) {
            location = new Point(
                    usable.x + Math.max(0, (usable.width - width) / 2),
                    usable.y + Math.max(0, (usable.height - height) / 2));
        } else {
            int maxX = usable.x + Math.max(0, usable.width - width);
            int maxY = usable.y + Math.max(0, usable.height - height);
            location.x = Math.max(usable.x, Math.min(location.x, maxX));
            location.y = Math.max(usable.y, Math.min(location.y, maxY));
        }
        window.setLocation(location);
    }

    private static boolean intersectsAnyUsableScreen(Rectangle rectangle) {
        try {
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            for (int i = 0; i < devices.length; i++) {
                GraphicsConfiguration[] configs = devices[i].getConfigurations();
                for (int j = 0; j < configs.length; j++) {
                    if (usableBounds(configs[j]).intersects(rectangle)) return true;
                }
            }
        } catch (Throwable ignored) { }
        return false;
    }

    private static Rectangle usableBounds(GraphicsConfiguration configuration) {
        GraphicsConfiguration config = configuration;
        if (config == null) {
            try {
                config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration();
            } catch (Throwable ignored) { }
        }

        Rectangle bounds;
        if (config != null) bounds = new Rectangle(config.getBounds());
        else bounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        if (config != null) {
            try {
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
                bounds.x += insets.left;
                bounds.y += insets.top;
                bounds.width -= insets.left + insets.right;
                bounds.height -= insets.top + insets.bottom;
            } catch (Throwable ignored) { }
        }
        return bounds;
    }
}
