/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.data.Direction;

public class Icons {
    private static final String path = "resources/logisim/icons";
    private static final Map<String, Icon> cache = new HashMap<String, Icon>();

    private Icons() { }

    public static Icon getIcon(String name) {
        int target = Math.max(18, DarkTheme.scale(18));
        String key = name + "@" + target;
        Icon found = cache.get(key);
        if (found != null) return found;

        Icon modern = ModernIcons.getActionIcon(name, target);
        if (modern != null) {
            cache.put(key, modern);
            return modern;
        }

        java.net.URL url = Icons.class.getClassLoader().getResource(path + "/" + name);
        if (url == null) return null;
        ImageIcon original = new ImageIcon(url);
        if (target == original.getIconWidth() && target == original.getIconHeight()) {
            cache.put(key, original);
            return original;
        }

        Icon scaled = new SmoothScaledIcon(original.getImage(), target, target);
        cache.put(key, scaled);
        return scaled;
    }

    private static class SmoothScaledIcon implements Icon {
        private final Image image;
        private final int width;
        private final int height;

        SmoothScaledIcon(Image image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }

        public int getIconWidth() { return width; }
        public int getIconHeight() { return height; }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(image, x, y, width, height, c);
                g2.dispose();
            } else {
                g.drawImage(image, x, y, width, height, c);
            }
        }
    }

    public static void paintRotated(Graphics g, int x, int y, Direction dir, Icon icon, Component dest) {
        if (!(g instanceof Graphics2D) || dir == Direction.EAST) {
            icon.paintIcon(dest, g, x, y);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        double cx = x + icon.getIconWidth() / 2.0;
        double cy = y + icon.getIconHeight() / 2.0;
        if (dir == Direction.WEST) {
            g2.rotate( Math.PI, cx, cy);
        } else if (dir == Direction.NORTH) {
            g2.rotate(-Math.PI / 2.0, cx, cy);
        } else if (dir == Direction.SOUTH) {
            g2.rotate( Math.PI / 2.0, cx, cy);
        } else {
            g2.translate(-x, -y);
        }
        icon.paintIcon(dest, g2, x, y);
        g2.dispose();
    }
}
