package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.cburch.logisim.DarkTheme;

/**
 * Wraps a side panel in a narrow always-visible rail. When collapsed only the
 * rail remains, so the canvas can use nearly the entire window while the panel
 * is still one click away.
 */
public class CollapsibleSidePanel extends JPanel {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;

    private final JComponent content;
    private final int side;
    private final Rail rail;
    private boolean collapsed;
    private Runnable toggleListener;

    public CollapsibleSidePanel(JComponent content, int side) {
        super(new BorderLayout());
        this.content = content;
        this.side = side;
        this.rail = new Rail();
        setOpaque(false);
        if (side == LEFT) {
            add(content, BorderLayout.CENTER);
            add(rail, BorderLayout.EAST);
        } else {
            add(rail, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
        }
        setCollapsed(false);
    }

    public void setToggleListener(Runnable listener) {
        this.toggleListener = listener;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public int getCollapsedWidth() {
        return DarkTheme.scale(28);
    }

    public void toggle() {
        setCollapsed(!collapsed);
        if (toggleListener != null) toggleListener.run();
    }

    public void setCollapsed(boolean value) {
        if (collapsed == value && content.isVisible() == !value) {
            rail.updateArrow();
            return;
        }
        collapsed = value;
        content.setVisible(!value);
        rail.updateArrow();
        revalidate();
        repaint();
    }

    @Override
    public Dimension getMinimumSize() {
        if (collapsed) return new Dimension(getCollapsedWidth(), DarkTheme.scale(120));
        Dimension c = content.getMinimumSize();
        return new Dimension(Math.max(getCollapsedWidth() + DarkTheme.scale(150), c.width + getCollapsedWidth()), c.height);
    }

    @Override
    public Dimension getPreferredSize() {
        if (collapsed) return new Dimension(getCollapsedWidth(), DarkTheme.scale(420));
        Dimension c = content.getPreferredSize();
        return new Dimension(c.width + getCollapsedWidth(), c.height);
    }

    private final class Rail extends JPanel {
        private final RailButton button = new RailButton();

        Rail() {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(getCollapsedWidth(), DarkTheme.scale(70)));
            setMinimumSize(new Dimension(getCollapsedWidth(), DarkTheme.scale(40)));
            setBorder(side == LEFT
                    ? BorderFactory.createMatteBorder(0, 0, 0, 1, DarkTheme.BORDER_SOFT)
                    : BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BORDER_SOFT));

            button.setFocusable(false);
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setPreferredSize(new Dimension(getCollapsedWidth(), DarkTheme.scale(54)));
            button.setToolTipText(side == LEFT
                    ? "Скрыть/показать панель проекта • Ctrl+Alt+L"
                    : "Скрыть/показать Live-анализ • Ctrl+Alt+R");
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { toggle(); }
            });
            add(button, BorderLayout.NORTH);
            updateArrow();
        }

        void updateArrow() {
            button.repaint();
            repaint();
        }

        private final class RailButton extends JButton {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover() || getModel().isPressed()) {
                    Color a = DarkTheme.ACCENT;
                    int alpha = getModel().isPressed() ? 52 : 30;
                    g2.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), alpha));
                    int pad = DarkTheme.scale(4);
                    g2.fillRoundRect(pad, DarkTheme.scale(7), getWidth() - pad * 2,
                            Math.max(DarkTheme.scale(28), getHeight() - DarkTheme.scale(14)),
                            DarkTheme.scale(8), DarkTheme.scale(8));
                }
                g2.setColor(getModel().isRollover() ? DarkTheme.ACCENT : DarkTheme.FOREGROUND_MUTED);
                g2.setStroke(new java.awt.BasicStroke(Math.max(1.5f, DarkTheme.scale(1.7f)),
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = Math.min(getHeight() / 2, DarkTheme.scale(27));
                int dx = DarkTheme.scale(4);
                int dy = DarkTheme.scale(6);
                boolean pointRight = side == LEFT; // Rails always face inward: left >, right <.
                if (pointRight) {
                    g2.drawLine(cx - dx, cy - dy, cx + dx, cy);
                    g2.drawLine(cx + dx, cy, cx - dx, cy + dy);
                } else {
                    g2.drawLine(cx + dx, cy - dy, cx - dx, cy);
                    g2.drawLine(cx - dx, cy, cx + dx, cy + dy);
                }
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                DarkTheme.paintSurface(g2, this);
                Color accent = new Color(DarkTheme.ACCENT.getRed(), DarkTheme.ACCENT.getGreen(),
                        DarkTheme.ACCENT.getBlue(), 38);
                g2.setColor(accent);
                if (side == LEFT) g2.fillRect(Math.max(0, getWidth() - DarkTheme.scale(2)), 0, DarkTheme.scale(2), getHeight());
                else g2.fillRect(0, 0, DarkTheme.scale(2), getHeight());
                g2.dispose();
            } else {
                g.setColor(DarkTheme.PANEL_BACKGROUND);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }
}
