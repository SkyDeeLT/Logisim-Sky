package com.cburch.logisim.gui.prefs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleManager;

class AppearanceOptions extends OptionsPanel {
    private static final class ThemeDef {
        final String id;
        String name;
        final Color a;
        final Color b;
        final Color accent;
        ThemeDef(String id, String name, Color a, Color b, Color accent) {
            this.id = id;
            this.name = name;
            this.a = a;
            this.b = b;
            this.accent = accent;
        }
    }

    private static final class ThemeIcon implements Icon {
        private final ThemeDef def;
        ThemeIcon(ThemeDef def) { this.def = def; }
        public int getIconWidth() { return DarkTheme.scale(92); }
        public int getIconHeight() { return DarkTheme.scale(46); }
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getIconWidth();
            int h = getIconHeight();
            g2.setPaint(new LinearGradientPaint(
                    new Point2D.Float(x, y), new Point2D.Float(x + w, y + h),
                    new float[] { 0.0f, 1.0f }, new Color[] { def.a, def.b }));
            g2.fillRoundRect(x, y, w, h, DarkTheme.scale(10), DarkTheme.scale(10));
            g2.setColor(new Color(255, 255, 255, 45));
            g2.drawRoundRect(x, y, w - 1, h - 1, DarkTheme.scale(10), DarkTheme.scale(10));
            int cy = y + h / 2;
            g2.setStroke(new BasicStroke(Math.max(1.4f, DarkTheme.getUiScale())));
            g2.setColor(def.accent);
            g2.drawLine(x + DarkTheme.scale(13), cy, x + w - DarkTheme.scale(13), cy);
            g2.fillOval(x + w - DarkTheme.scale(17), cy - DarkTheme.scale(3),
                    DarkTheme.scale(6), DarkTheme.scale(6));
            g2.dispose();
        }
    }

    private static final class ThemeButton extends JToggleButton {
        private final ThemeDef def;
        ThemeButton(ThemeDef def) {
            super(def.name, new ThemeIcon(def));
            this.def = def;
            setVerticalTextPosition(BOTTOM);
            setHorizontalTextPosition(CENTER);
            setFocusPainted(false);
            setMargin(new Insets(DarkTheme.scale(8), DarkTheme.scale(8),
                    DarkTheme.scale(7), DarkTheme.scale(8)));
            setPreferredSize(new Dimension(DarkTheme.scale(112), DarkTheme.scale(82)));
        }
        String getThemeId() { return def.id; }
        void refreshText() { setText(def.name); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (isSelected() && g instanceof Graphics2D) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DarkTheme.ACCENT);
                g2.setStroke(new BasicStroke(Math.max(1.5f, DarkTheme.getUiScale())));
                int inset = Math.max(2, DarkTheme.scale(2));
                int arc = Math.max(10, DarkTheme.scale(10));
                g2.drawRoundRect(inset, inset, getWidth() - inset * 2 - 1,
                        getHeight() - inset * 2 - 1, arc, arc);
                g2.dispose();
            }
        }
    }

    private final JLabel heading = new JLabel();
    private final JLabel description = new JLabel();
    private final JLabel themeLabel = new JLabel();
    private final JLabel scaleLabel = new JLabel();
    private final JLabel scaleNote = new JLabel();
    private final JLabel navigation = new JLabel();
    private final JLabel previewLabel = new JLabel();
    private final JCheckBox animationsToggle = new JCheckBox();
    private final JCheckBox liveAnalysisToggle = new JCheckBox();
    private final JComboBox scaleCombo;
    private final ThemePreview preview = new ThemePreview();
    private final ThemeDef[] themes;
    private final ThemeButton[] themeButtons;
    private boolean updating = false;

    AppearanceOptions(PreferencesFrame frame) {
        super(frame);
        setLayout(new BorderLayout(DarkTheme.scale(14), DarkTheme.scale(14)));
        setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(18), DarkTheme.scale(20),
                DarkTheme.scale(18), DarkTheme.scale(20)));

        themes = new ThemeDef[] {
                new ThemeDef(DarkTheme.THEME_DARK, "Midnight",
                        new Color(17, 24, 34), new Color(28, 39, 50), new Color(101, 177, 255)),
                new ThemeDef(DarkTheme.THEME_LIGHT, "Light",
                        new Color(251, 253, 255), new Color(232, 240, 250), new Color(37, 99, 235)),
                new ThemeDef(DarkTheme.THEME_AURORA, "Aurora",
                        new Color(12, 19, 35), new Color(67, 32, 76), new Color(167, 139, 250)),
                new ThemeDef(DarkTheme.THEME_OCEAN, "Ocean",
                        new Color(7, 22, 34), new Color(11, 73, 79), new Color(56, 189, 248)),
                new ThemeDef(DarkTheme.THEME_EMBER, "Ember",
                        new Color(28, 17, 25), new Color(79, 38, 43), new Color(251, 113, 133))
        };
        themeButtons = new ThemeButton[themes.length];
        scaleCombo = new JComboBox(new String[] { "100%", "110%", "125%", "135%", "150%", "175%", "200%", "250%" });

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, DarkTheme.scale(20.0f)));
        description.setForeground(DarkTheme.FOREGROUND_MUTED);
        top.add(heading);
        top.add(Box.createVerticalStrut(DarkTheme.scale(4)));
        top.add(description);
        add(top, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        themeLabel.setFont(themeLabel.getFont().deriveFont(Font.BOLD));
        themeLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(themeLabel);
        body.add(Box.createVerticalStrut(DarkTheme.scale(7)));

        JPanel themeGrid = new JPanel(new GridLayout(1, themes.length, DarkTheme.scale(8), 0));
        themeGrid.setOpaque(false);
        themeGrid.setAlignmentX(LEFT_ALIGNMENT);
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < themes.length; i++) {
            final ThemeButton button = new ThemeButton(themes[i]);
            themeButtons[i] = button;
            group.add(button);
            themeGrid.add(button);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (updating || !button.isSelected()) return;
                    DarkTheme.applyTheme(button.getThemeId());
                    AppPreferences.UI_THEME.set(button.getThemeId());
                    refreshThemeSelection();
                    preview.repaint();
                }
            });
        }
        body.add(themeGrid);
        body.add(Box.createVerticalStrut(DarkTheme.scale(16)));

        JPanel scaleRow = new JPanel(new BorderLayout(DarkTheme.scale(12), 0));
        scaleRow.setOpaque(false);
        scaleRow.setAlignmentX(LEFT_ALIGNMENT);
        scaleLabel.setPreferredSize(new Dimension(DarkTheme.scale(125), DarkTheme.scale(28)));
        scaleRow.add(scaleLabel, BorderLayout.WEST);
        scaleRow.add(scaleCombo, BorderLayout.CENTER);
        body.add(scaleRow);
        body.add(Box.createVerticalStrut(DarkTheme.scale(4)));
        scaleNote.setForeground(DarkTheme.FOREGROUND_MUTED);
        scaleNote.setAlignmentX(LEFT_ALIGNMENT);
        body.add(scaleNote);
        body.add(Box.createVerticalStrut(DarkTheme.scale(12)));

        navigation.setForeground(DarkTheme.FOREGROUND_MUTED);
        navigation.setAlignmentX(LEFT_ALIGNMENT);
        body.add(navigation);
        body.add(Box.createVerticalStrut(DarkTheme.scale(10)));

        animationsToggle.setOpaque(false);
        liveAnalysisToggle.setOpaque(false);
        animationsToggle.setAlignmentX(LEFT_ALIGNMENT);
        liveAnalysisToggle.setAlignmentX(LEFT_ALIGNMENT);
        animationsToggle.setSelected(AppPreferences.UI_ANIMATIONS.getBoolean());
        liveAnalysisToggle.setSelected(AppPreferences.LIVE_ANALYSIS.getBoolean());
        body.add(animationsToggle);
        body.add(Box.createVerticalStrut(DarkTheme.scale(4)));
        body.add(liveAnalysisToggle);
        body.add(Box.createVerticalStrut(DarkTheme.scale(16)));

        previewLabel.setFont(previewLabel.getFont().deriveFont(Font.BOLD));
        previewLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(previewLabel);
        body.add(Box.createVerticalStrut(DarkTheme.scale(6)));
        preview.setAlignmentX(LEFT_ALIGNMENT);
        preview.setPreferredSize(new Dimension(DarkTheme.scale(560), DarkTheme.scale(126)));
        preview.setMinimumSize(new Dimension(DarkTheme.scale(360), DarkTheme.scale(100)));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, DarkTheme.scale(140)));
        body.add(preview);

        add(body, BorderLayout.CENTER);

        selectCurrentTheme();
        selectCurrentScale();

        scaleCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (updating) return;
                String item = (String) scaleCombo.getSelectedItem();
                if (item == null) return;
                String value = item.substring(0, item.length() - 1);
                try {
                    double d = Double.parseDouble(value) / 100.0;
                    AppPreferences.UI_SCALE.set(Double.toString(d));
                } catch (NumberFormatException ignored) { }
            }
        });
        animationsToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!updating) AppPreferences.UI_ANIMATIONS.set(Boolean.valueOf(animationsToggle.isSelected()));
            }
        });
        liveAnalysisToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!updating) AppPreferences.LIVE_ANALYSIS.set(Boolean.valueOf(liveAnalysisToggle.isSelected()));
            }
        });

        localeChanged();
    }

    private void refreshThemeSelection() {
        String id = DarkTheme.getCurrentTheme();
        for (int i = 0; i < themeButtons.length; i++) {
            themeButtons[i].setSelected(themeButtons[i].getThemeId().equals(id));
            themeButtons[i].repaint();
        }
    }

    private void selectCurrentTheme() {
        refreshThemeSelection();
    }

    private void selectCurrentScale() {
        try {
            int pct = (int) Math.round(Double.parseDouble(AppPreferences.UI_SCALE.get()) * 100.0);
            scaleCombo.setSelectedItem(Integer.toString(pct) + "%");
        } catch (Throwable ignored) {
            scaleCombo.setSelectedItem("125%");
        }
    }

    public String getTitle() { return isRussian() ? "Интерфейс" : "Appearance"; }
    public String getHelpText() { return isRussian() ? "Темы, масштаб и удобство интерфейса" : "Themes, scaling and interface comfort"; }

    public void localeChanged() {
        updating = true;
        boolean ru = isRussian();
        heading.setText("Logisim Sky");
        description.setText(ru ? "Внешний вид и навигация. Формат .circ и логика схем не изменяются." :
                "Appearance and navigation. Circuit logic and .circ files stay unchanged.");
        themeLabel.setText(ru ? "Тема оформления" : "Theme");
        scaleLabel.setText(ru ? "Масштаб UI" : "UI scale");
        scaleNote.setText(ru ? "Масштаб интерфейса применяется после перезапуска." : "UI scale is applied after restart.");
        navigation.setText(ru ? "Навигация: Ctrl + колесо — масштаб; зажатое колесо — свободное перемещение." :
                "Navigation: Ctrl + wheel zooms; hold the middle button to pan freely.");
        animationsToggle.setText(ru ? "Плавная анимация градиента рабочей области" : "Animate workspace gradient");
        liveAnalysisToggle.setText(ru ? "Показывать Live-анализ справа" : "Show Live Analysis on the right");
        previewLabel.setText(ru ? "Предпросмотр рабочей области" : "Workspace preview");
        themes[0].name = ru ? "Midnight" : "Midnight";
        themes[1].name = ru ? "Светлая" : "Light";
        themes[2].name = "Aurora";
        themes[3].name = "Ocean";
        themes[4].name = "Ember";
        for (int i = 0; i < themeButtons.length; i++) themeButtons[i].refreshText();
        updating = false;
    }

    private static boolean isRussian() {
        Locale loc = LocaleManager.getLocale();
        return loc != null && "ru".equalsIgnoreCase(loc.getLanguage());
    }

    private static class ThemePreview extends JPanel {
        ThemePreview() {
            setOpaque(false);
            setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER));
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(DarkTheme.createCanvasGradient(0, 0, Math.max(1, w), Math.max(1, h)));
            g2.fillRoundRect(0, 0, w, h, DarkTheme.scale(12), DarkTheme.scale(12));

            g2.setColor(DarkTheme.GRID);
            int step = Math.max(8, DarkTheme.scale(9));
            for (int x = step; x < w; x += step) {
                for (int y = step; y < h; y += step) g2.fillRect(x, y, 1, 1);
            }

            int cy = h / 2;
            g2.setStroke(new BasicStroke(Math.max(1.3f, DarkTheme.scale(1.2f))));
            g2.setColor(DarkTheme.ACCENT);
            g2.drawLine(DarkTheme.scale(25), cy, DarkTheme.scale(145), cy);
            g2.drawLine(DarkTheme.scale(235), cy, w - DarkTheme.scale(24), cy);
            g2.setColor(DarkTheme.FOREGROUND);
            int gx = DarkTheme.scale(145);
            int gw = DarkTheme.scale(90);
            int gh = DarkTheme.scale(48);
            g2.drawRoundRect(gx, cy - gh / 2, gw, gh, DarkTheme.scale(12), DarkTheme.scale(12));
            g2.setColor(DarkTheme.ACCENT);
            g2.fillOval(w - DarkTheme.scale(28), cy - DarkTheme.scale(4), DarkTheme.scale(8), DarkTheme.scale(8));
            g2.dispose();
        }
    }
}
