package com.cburch.logisim.gui.generic;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPanelUI;

import com.cburch.logisim.DarkTheme;

/**
 * Lightweight panel painter used by Sky UI.
 * It only changes how ordinary Swing panels are painted; it does not affect
 * circuit state, simulation, coordinates or .circ serialization.
 */
public class SkyGradientPanelUI extends BasicPanelUI {
    private static final SkyGradientPanelUI INSTANCE = new SkyGradientPanelUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    public void update(Graphics g, JComponent c) {
        if (c.isOpaque() && g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            DarkTheme.paintSurface(g2, c);
            g2.dispose();
            paint(g, c);
        } else {
            super.update(g, c);
        }
    }
}
