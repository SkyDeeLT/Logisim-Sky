/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import java.awt.event.ActionEvent;

import com.cburch.logisim.DarkTheme;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

class ToolbarButton extends JComponent implements MouseListener {
    private Toolbar toolbar;
    private ToolbarItem item;
    private boolean hovered;

    ToolbarButton(Toolbar toolbar, ToolbarItem item) {
        this.toolbar = toolbar;
        this.item = item;
        this.hovered = false;
        addMouseListener(this);
        setFocusable(true);
        setToolTipText("");
        setOpaque(false);
        String accessibleName = item == null ? null : item.getToolTip();
        if (accessibleName != null) {
            getAccessibleContext().setAccessibleName(accessibleName);
            getAccessibleContext().setAccessibleDescription(accessibleName);
        }
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "activateToolbarItem");
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "activateToolbarItem");
        getActionMap().put("activateToolbarItem", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { activate(); }
        });
    }

    public ToolbarItem getItem() {
        return item;
    }

    private int border() {
        return Math.max(3, DarkTheme.scale(3));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim = item.getDimension(toolbar.getOrientation());
        int border = border();
        return new Dimension(dim.width + 2 * border, dim.height + 2 * border);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public void paintComponent(Graphics g) {
        int border = border();
        Dimension dim = item.getDimension(toolbar.getOrientation());
        boolean pressed = toolbar.getPressed() == this;
        boolean selected = toolbar.getToolbarModel().isSelected(item);

        if (g instanceof Graphics2D) {
            Graphics2D bg = (Graphics2D) g.create();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = Math.max(5, DarkTheme.scale(5));
            if (pressed) {
                bg.setPaint(new GradientPaint(0, 0, DarkTheme.PRESSED.brighter(), 0, getHeight(), DarkTheme.PRESSED));
                bg.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            } else if (selected) {
                bg.setPaint(new GradientPaint(0, 0, DarkTheme.SELECTION.brighter(), 0, getHeight(), DarkTheme.SELECTION));
                bg.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            } else if (hovered && item.isSelectable()) {
                bg.setPaint(new GradientPaint(0, 0, DarkTheme.HOVER.brighter(), 0, getHeight(), DarkTheme.HOVER));
                bg.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, arc, arc);
            }

            if (selected) {
                bg.setColor(DarkTheme.ACCENT);
                bg.setStroke(new BasicStroke(Math.max(1.0f, DarkTheme.getUiScale())));
                bg.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, arc, arc);
            }
            if (isFocusOwner()) {
                int inset = Math.max(2, DarkTheme.scale(2));
                bg.setColor(DarkTheme.ACCENT);
                bg.setStroke(new BasicStroke(Math.max(1.2f, DarkTheme.getUiScale())));
                bg.drawRoundRect(inset, inset, getWidth() - inset * 2 - 1,
                        getHeight() - inset * 2 - 1, arc, arc);
            }
            bg.dispose();
        }

        Graphics g2 = g.create();
        g2.translate(border, border);
        item.paintIcon(ToolbarButton.this, g2);
        g2.dispose();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return item.getToolTip();
    }

    public void mousePressed(MouseEvent e) {
        if (item != null && item.isSelectable()) {
            toolbar.setPressed(this);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (toolbar.getPressed() == this) {
            activate();
            toolbar.setPressed(null);
        }
    }

    public void mouseClicked(MouseEvent e) { }

    public void mouseEntered(MouseEvent e) {
        hovered = true;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        hovered = false;
        toolbar.setPressed(null);
        repaint();
    }

    private void activate() {
        if (item != null && item.isSelectable() && toolbar.getToolbarModel() != null) {
            toolbar.getToolbarModel().itemSelected(item);
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleToolbarButton();
        }
        return accessibleContext;
    }

    protected class AccessibleToolbarButton extends AccessibleJComponent {
        private static final long serialVersionUID = 1L;

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.PUSH_BUTTON;
        }
    }
}
