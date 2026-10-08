package com.cburch.logisim.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import com.cburch.logisim.DarkTheme;

/** Creates a clean HiDPI application icon without relying on tiny legacy PNGs. */
public final class AppIconFactory {
    private AppIconFactory() { }

    public static BufferedImage create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double s = size / 64.0;
        g.scale(s, s);

        g.setColor(new Color(30, 37, 46, 245));
        g.fill(new RoundRectangle2D.Double(3, 3, 58, 58, 15, 15));
        g.setColor(new Color(101, 177, 255, 230));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new RoundRectangle2D.Double(4.5, 4.5, 55, 55, 14, 14));

        // stylized AND gate with input/output nodes
        Path2D p = new Path2D.Double();
        p.moveTo(18, 18); p.lineTo(31, 18);
        p.curveTo(44, 18, 49, 24, 49, 32);
        p.curveTo(49, 40, 44, 46, 31, 46);
        p.lineTo(18, 46); p.closePath();
        g.setColor(new Color(235, 240, 247));
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
        g.setColor(new Color(101, 177, 255));
        g.drawLine(10, 25, 18, 25); g.drawLine(10, 39, 18, 39); g.drawLine(49, 32, 56, 32);
        g.setColor(new Color(63, 211, 140));
        g.fillOval(8, 23, 4, 4); g.fillOval(8, 37, 4, 4); g.fillOval(54, 30, 4, 4);

        g.dispose();
        return img;
    }
}
