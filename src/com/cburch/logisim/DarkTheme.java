package com.cburch.logisim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.UIManager;
import javax.swing.UIDefaults;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.cburch.logisim.prefs.AppPreferences;

public final class DarkTheme {
    public static final String THEME_DARK = "midnight";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_AURORA = "aurora";
    public static final String THEME_OCEAN = "ocean";
    public static final String THEME_EMBER = "ember";

    public static Color WINDOW_BACKGROUND;
    public static Color PANEL_BACKGROUND;
    public static Color PANEL_BACKGROUND_ALT;
    public static Color CANVAS_BACKGROUND;
    public static Color CANVAS_GRADIENT_START;
    public static Color CANVAS_GRADIENT_MID;
    public static Color CANVAS_GRADIENT_END;
    public static Color SURFACE_GRADIENT_START;
    public static Color SURFACE_GRADIENT_MID;
    public static Color SURFACE_GRADIENT_END;
    public static Color GRID;
    public static Color GRID_STRONG;
    public static Color FOREGROUND;
    public static Color FOREGROUND_MUTED;
    public static Color BORDER;
    public static Color ACCENT;
    public static Color ACCENT_HOVER;
    public static Color SELECTION;
    public static Color HOVER;
    public static Color PRESSED;
    public static Color HALO;
    public static Color CANVAS_TEXT;
    public static Color TOOLBAR_TOP;
    public static Color TOOLBAR_BOTTOM;
    public static Color MENUBAR_TOP;
    public static Color MENUBAR_BOTTOM;
    public static Color BORDER_SOFT;
    public static Color INPUT_BACKGROUND;

    private static boolean gradientCanvas = false;
    private static boolean lightTheme = false;
    private static boolean modernLookAndFeel = false;
    private static float sessionScale = 1.25f;
    private static String currentTheme = THEME_DARK;
    private static int themeRevision = 0;

    static {
        loadPalette(THEME_DARK);
    }

    private DarkTheme() { }

    private static class LogisimMetalTheme extends DefaultMetalTheme {
        protected ColorUIResource getPrimary1() { return ui(darken(ACCENT, 0.50f)); }
        protected ColorUIResource getPrimary2() { return ui(darken(ACCENT, 0.25f)); }
        protected ColorUIResource getPrimary3() { return ui(lightTheme ? lighten(ACCENT, 0.55f) : darken(ACCENT, 0.05f)); }
        protected ColorUIResource getSecondary1() { return ui(lightTheme ? new Color(170, 180, 192) : darken(PANEL_BACKGROUND, 0.12f)); }
        protected ColorUIResource getSecondary2() { return ui(lightTheme ? new Color(214, 220, 228) : PANEL_BACKGROUND_ALT); }
        protected ColorUIResource getSecondary3() { return ui(PANEL_BACKGROUND); }
        protected ColorUIResource getBlack() { return ui(FOREGROUND); }
        protected ColorUIResource getWhite() { return ui(INPUT_BACKGROUND); }
        public ColorUIResource getControlTextColor() { return ui(FOREGROUND); }
        public ColorUIResource getSystemTextColor() { return ui(FOREGROUND); }
        public ColorUIResource getUserTextColor() { return ui(FOREGROUND); }
        public ColorUIResource getMenuTextColor() { return ui(FOREGROUND); }
        public ColorUIResource getWindowTitleForeground() { return ui(FOREGROUND); }
        public ColorUIResource getWindowTitleInactiveForeground() { return ui(FOREGROUND_MUTED); }
        public ColorUIResource getControlDisabled() { return ui(FOREGROUND_MUTED); }
        public ColorUIResource getControlHighlight() { return ui(PANEL_BACKGROUND_ALT); }
        public ColorUIResource getPrimaryControlHighlight() { return ui(BORDER); }
    }

    public static void apply() {
        String id = THEME_DARK;
        try { id = AppPreferences.UI_THEME.get(); } catch (Throwable ignored) { }
        applyTheme(id, false);
    }

    public static void applyTheme(String id) {
        applyTheme(id, true);
    }

    private static void applyTheme(String id, boolean refreshWindows) {
        // Geometry is created from this value throughout Swing. Keep it stable
        // for the lifetime of the process; the preference page correctly tells
        // users that scale changes take effect after restart.
        if (!refreshWindows) sessionScale = readConfiguredUiScale();
        loadPalette(id);

        try { System.setProperty("awt.useSystemAAFontSettings", "lcd"); } catch (Throwable ignored) { }
        try { System.setProperty("swing.aatext", "true"); } catch (Throwable ignored) { }

        modernLookAndFeel = installModernLookAndFeel();
        if (!modernLookAndFeel) {
            try {
                MetalLookAndFeel.setCurrentTheme(new LogisimMetalTheme());
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (Exception ignored) { }
        }

        final ColorUIResource bg = ui(PANEL_BACKGROUND);
        final ColorUIResource bgAlt = ui(PANEL_BACKGROUND_ALT);
        final ColorUIResource field = ui(INPUT_BACKGROUND);
        final ColorUIResource fg = ui(FOREGROUND);
        final ColorUIResource muted = ui(FOREGROUND_MUTED);
        final ColorUIResource border = ui(BORDER);
        final ColorUIResource accent = ui(ACCENT);
        final ColorUIResource selection = ui(SELECTION);

        put("control", bg);
        put("info", bgAlt);
        put("nimbusBase", bgAlt);
        put("nimbusBlueGrey", bgAlt);
        put("nimbusLightBackground", bg);
        put("text", fg);

        put("Panel.background", bg);
        put("Panel.foreground", fg);
        put("Label.foreground", fg);
        put("Label.background", bg);
        put("Button.background", bgAlt);
        put("Button.foreground", fg);
        put("ToggleButton.background", bgAlt);
        put("ToggleButton.foreground", fg);
        put("CheckBox.background", bg);
        put("CheckBox.foreground", fg);
        put("RadioButton.background", bg);
        put("RadioButton.foreground", fg);
        put("ComboBox.background", bgAlt);
        put("ComboBox.foreground", fg);
        put("ComboBox.selectionBackground", selection);
        put("ComboBox.selectionForeground", fg);
        put("TextField.background", field);
        put("TextField.foreground", fg);
        put("TextField.caretForeground", fg);
        put("TextField.selectionBackground", selection);
        put("TextField.selectionForeground", fg);
        put("FormattedTextField.background", field);
        put("FormattedTextField.foreground", fg);
        put("TextArea.background", field);
        put("TextArea.foreground", fg);
        put("TextPane.background", field);
        put("TextPane.foreground", fg);
        put("EditorPane.background", field);
        put("EditorPane.foreground", fg);
        put("List.background", field);
        put("List.foreground", fg);
        put("List.selectionBackground", selection);
        put("List.selectionForeground", fg);
        put("Tree.background", field);
        put("Tree.foreground", fg);
        put("Tree.textForeground", fg);
        put("Tree.selectionBackground", selection);
        put("Tree.selectionForeground", fg);
        put("Table.background", field);
        put("Table.foreground", fg);
        put("Table.gridColor", border);
        put("Table.selectionBackground", selection);
        put("Table.selectionForeground", fg);
        put("TableHeader.background", bgAlt);
        put("TableHeader.foreground", fg);
        put("ScrollPane.background", bg);
        put("Viewport.background", field);
        put("MenuBar.background", bg);
        put("MenuBar.foreground", fg);
        put("Menu.background", bg);
        put("Menu.foreground", fg);
        put("Menu.selectionBackground", selection);
        put("Menu.selectionForeground", fg);
        put("MenuItem.background", bg);
        put("MenuItem.foreground", fg);
        put("MenuItem.acceleratorForeground", muted);
        put("MenuItem.selectionBackground", selection);
        put("MenuItem.selectionForeground", fg);
        put("CheckBoxMenuItem.background", bg);
        put("CheckBoxMenuItem.foreground", fg);
        put("RadioButtonMenuItem.background", bg);
        put("RadioButtonMenuItem.foreground", fg);
        put("PopupMenu.background", bg);
        put("ToolBar.background", bg);
        put("ToolBar.foreground", fg);
        put("ToolTip.background", bgAlt);
        put("ToolTip.foreground", fg);
        put("TabbedPane.background", bg);
        put("TabbedPane.foreground", fg);
        put("TabbedPane.selected", bgAlt);
        put("SplitPane.background", bg);
        put("Separator.background", bg);
        put("Separator.foreground", border);
        put("OptionPane.background", bg);
        put("OptionPane.foreground", fg);
        put("OptionPane.messageForeground", fg);
        put("ProgressBar.background", field);
        put("ProgressBar.foreground", accent);
        put("Slider.background", bg);
        put("Slider.foreground", accent);
        put("Spinner.background", field);
        put("Spinner.foreground", fg);
        put("RootPane.background", bg);
        put("DesktopPane.background", bg);
        // FlatLaf supplies modern delegates for ordinary panels. The fallback
        // Metal path keeps the lightweight Sky painter used by earlier builds.
        if (!modernLookAndFeel) {
            put("PanelUI", "com.cburch.logisim.gui.generic.SkyGradientPanelUI");
        }

        float uiScale = getUiScale();
        useModernUIFont(uiScale);

        installModernMetrics();
        put("ScrollBar.width", Integer.valueOf(scale(10)));
        put("SplitPane.dividerSize", Integer.valueOf(Math.max(5, scale(5))));
        put("Tree.rowHeight", Integer.valueOf(Math.max(0, scale(22))));
        put("Table.rowHeight", Integer.valueOf(Math.max(18, scale(21))));
        put("MenuBar.height", Integer.valueOf(Math.max(22, scale(23))));
        put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(scale(4), scale(7), scale(4), scale(7))));
        if (!modernLookAndFeel) {
            Border textBorder = BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    BorderFactory.createEmptyBorder(scale(4), scale(7), scale(4), scale(7)));
            put("TextField.border", textBorder);
            put("FormattedTextField.border", textBorder);
        }

        themeRevision++;
        if (refreshWindows) refreshOpenWindows();
    }

    private static void loadPalette(String id) {
        if (id == null) id = THEME_DARK;
        currentTheme = id;
        gradientCanvas = true;
        lightTheme = false;

        if (THEME_LIGHT.equals(id)) {
            lightTheme = true;
            WINDOW_BACKGROUND = new Color(246, 248, 251);
            PANEL_BACKGROUND = new Color(241, 244, 248);
            PANEL_BACKGROUND_ALT = new Color(230, 235, 242);
            CANVAS_BACKGROUND = new Color(250, 251, 253);
            CANVAS_GRADIENT_START = new Color(251, 253, 255);
            CANVAS_GRADIENT_MID = new Color(244, 248, 253);
            CANVAS_GRADIENT_END = new Color(232, 240, 250);
            SURFACE_GRADIENT_START = new Color(250, 252, 255);
            SURFACE_GRADIENT_MID = new Color(242, 246, 251);
            SURFACE_GRADIENT_END = new Color(232, 238, 247);
            GRID = new Color(210, 218, 228);
            GRID_STRONG = new Color(184, 195, 209);
            FOREGROUND = new Color(31, 41, 55);
            FOREGROUND_MUTED = new Color(101, 116, 139);
            BORDER = new Color(198, 207, 219);
            ACCENT = new Color(37, 99, 235);
            ACCENT_HOVER = new Color(29, 78, 216);
            SELECTION = new Color(205, 222, 252);
            HOVER = new Color(225, 231, 239);
            PRESSED = new Color(210, 219, 231);
            HALO = new Color(37, 99, 235);
            CANVAS_TEXT = new Color(17, 24, 39);
            TOOLBAR_TOP = new Color(248, 250, 252);
            TOOLBAR_BOTTOM = new Color(232, 237, 244);
            MENUBAR_TOP = new Color(250, 251, 253);
            MENUBAR_BOTTOM = new Color(236, 240, 245);
            BORDER_SOFT = new Color(218, 225, 233);
            INPUT_BACKGROUND = new Color(255, 255, 255);
            return;
        }

        if (THEME_AURORA.equals(id)) {
            gradientCanvas = true;
            WINDOW_BACKGROUND = new Color(20, 21, 31);
            PANEL_BACKGROUND = new Color(25, 27, 39);
            PANEL_BACKGROUND_ALT = new Color(34, 37, 52);
            CANVAS_BACKGROUND = new Color(19, 23, 35);
            CANVAS_GRADIENT_START = new Color(15, 19, 32);
            CANVAS_GRADIENT_MID = new Color(24, 24, 42);
            CANVAS_GRADIENT_END = new Color(34, 29, 51);
            SURFACE_GRADIENT_START = new Color(30, 31, 47);
            SURFACE_GRADIENT_MID = new Color(30, 29, 44);
            SURFACE_GRADIENT_END = new Color(34, 28, 43);
            GRID = new Color(63, 71, 92);
            GRID_STRONG = new Color(83, 93, 119);
            FOREGROUND = new Color(238, 239, 248);
            FOREGROUND_MUTED = new Color(165, 170, 194);
            BORDER = new Color(68, 72, 95);
            ACCENT = new Color(167, 139, 250);
            ACCENT_HOVER = new Color(139, 92, 246);
            SELECTION = new Color(70, 58, 105);
            HOVER = new Color(44, 44, 64);
            PRESSED = new Color(54, 52, 78);
            HALO = new Color(196, 181, 253);
            CANVAS_TEXT = new Color(244, 243, 255);
            TOOLBAR_TOP = new Color(37, 39, 55);
            TOOLBAR_BOTTOM = new Color(26, 28, 40);
            MENUBAR_TOP = new Color(31, 33, 46);
            MENUBAR_BOTTOM = new Color(23, 25, 35);
            BORDER_SOFT = new Color(51, 54, 72);
            INPUT_BACKGROUND = new Color(21, 23, 34);
            return;
        }

        if (THEME_OCEAN.equals(id)) {
            gradientCanvas = true;
            WINDOW_BACKGROUND = new Color(13, 24, 31);
            PANEL_BACKGROUND = new Color(17, 31, 40);
            PANEL_BACKGROUND_ALT = new Color(24, 43, 54);
            CANVAS_BACKGROUND = new Color(12, 27, 36);
            CANVAS_GRADIENT_START = new Color(9, 22, 31);
            CANVAS_GRADIENT_MID = new Color(11, 34, 44);
            CANVAS_GRADIENT_END = new Color(14, 47, 54);
            SURFACE_GRADIENT_START = new Color(20, 36, 47);
            SURFACE_GRADIENT_MID = new Color(20, 39, 49);
            SURFACE_GRADIENT_END = new Color(19, 44, 51);
            GRID = new Color(44, 76, 88);
            GRID_STRONG = new Color(62, 99, 113);
            FOREGROUND = new Color(231, 245, 248);
            FOREGROUND_MUTED = new Color(143, 178, 185);
            BORDER = new Color(50, 82, 93);
            ACCENT = new Color(56, 189, 248);
            ACCENT_HOVER = new Color(14, 165, 233);
            SELECTION = new Color(30, 76, 96);
            HOVER = new Color(26, 50, 61);
            PRESSED = new Color(31, 62, 75);
            HALO = new Color(103, 232, 249);
            CANVAS_TEXT = new Color(236, 254, 255);
            TOOLBAR_TOP = new Color(26, 45, 56);
            TOOLBAR_BOTTOM = new Color(17, 31, 40);
            MENUBAR_TOP = new Color(22, 39, 49);
            MENUBAR_BOTTOM = new Color(15, 28, 36);
            BORDER_SOFT = new Color(37, 64, 75);
            INPUT_BACKGROUND = new Color(12, 26, 34);
            return;
        }

        if (THEME_EMBER.equals(id)) {
            gradientCanvas = true;
            WINDOW_BACKGROUND = new Color(29, 21, 25);
            PANEL_BACKGROUND = new Color(36, 25, 30);
            PANEL_BACKGROUND_ALT = new Color(50, 34, 41);
            CANVAS_BACKGROUND = new Color(28, 20, 25);
            CANVAS_GRADIENT_START = new Color(27, 19, 24);
            CANVAS_GRADIENT_MID = new Color(39, 24, 32);
            CANVAS_GRADIENT_END = new Color(50, 28, 34);
            SURFACE_GRADIENT_START = new Color(41, 28, 35);
            SURFACE_GRADIENT_MID = new Color(44, 29, 36);
            SURFACE_GRADIENT_END = new Color(48, 30, 35);
            GRID = new Color(81, 58, 65);
            GRID_STRONG = new Color(105, 73, 81);
            FOREGROUND = new Color(248, 238, 241);
            FOREGROUND_MUTED = new Color(194, 158, 168);
            BORDER = new Color(83, 57, 65);
            ACCENT = new Color(251, 113, 133);
            ACCENT_HOVER = new Color(244, 63, 94);
            SELECTION = new Color(91, 49, 62);
            HOVER = new Color(57, 39, 46);
            PRESSED = new Color(68, 45, 53);
            HALO = new Color(253, 164, 175);
            CANVAS_TEXT = new Color(255, 241, 242);
            TOOLBAR_TOP = new Color(50, 34, 41);
            TOOLBAR_BOTTOM = new Color(36, 25, 30);
            MENUBAR_TOP = new Color(43, 30, 36);
            MENUBAR_BOTTOM = new Color(31, 22, 27);
            BORDER_SOFT = new Color(65, 44, 51);
            INPUT_BACKGROUND = new Color(31, 22, 27);
            return;
        }

        currentTheme = THEME_DARK;
        WINDOW_BACKGROUND = new Color(23, 27, 33);
        PANEL_BACKGROUND = new Color(28, 33, 40);
        PANEL_BACKGROUND_ALT = new Color(36, 42, 51);
        CANVAS_BACKGROUND = new Color(23, 28, 34);
        CANVAS_GRADIENT_START = new Color(17, 23, 32);
        CANVAS_GRADIENT_MID = new Color(20, 29, 39);
        CANVAS_GRADIENT_END = new Color(23, 35, 47);
        SURFACE_GRADIENT_START = new Color(34, 40, 49);
        SURFACE_GRADIENT_MID = new Color(30, 36, 45);
        SURFACE_GRADIENT_END = new Color(27, 32, 40);
        GRID = new Color(47, 56, 68);
        GRID_STRONG = new Color(61, 73, 88);
        FOREGROUND = new Color(232, 236, 242);
        FOREGROUND_MUTED = new Color(158, 170, 185);
        BORDER = new Color(62, 74, 89);
        ACCENT = new Color(101, 177, 255);
        ACCENT_HOVER = new Color(83, 140, 198);
        SELECTION = new Color(43, 67, 94);
        HOVER = new Color(45, 54, 66);
        PRESSED = new Color(54, 66, 81);
        HALO = new Color(85, 163, 241);
        CANVAS_TEXT = new Color(235, 240, 247);
        TOOLBAR_TOP = new Color(38, 45, 55);
        TOOLBAR_BOTTOM = new Color(29, 35, 43);
        MENUBAR_TOP = new Color(34, 40, 49);
        MENUBAR_BOTTOM = new Color(27, 32, 39);
        BORDER_SOFT = new Color(48, 58, 70);
        INPUT_BACKGROUND = new Color(23, 27, 33);
    }

    public static boolean isGradientCanvas() { return gradientCanvas; }
    public static boolean isLightTheme() { return lightTheme; }
    public static String getCurrentTheme() { return currentTheme; }
    public static int getThemeRevision() { return themeRevision; }
    public static boolean isModernLookAndFeel() { return modernLookAndFeel; }

    /** Screen diagrams follow the theme; print/export diagrams stay document-black. */
    public static Color getCircuitForeground(boolean printView) {
        return printView ? Color.BLACK : CANVAS_TEXT;
    }

    /**
     * The live palette is updated only through apply/applyTheme. Preferences
     * are read at startup, not during painting, so a freshly selected theme
     * cannot be overwritten by a stale asynchronous preference value.
     */
    public static float getUiScale() {
        return sessionScale;
    }

    private static float readConfiguredUiScale() {
        try {
            float value = Float.parseFloat(AppPreferences.UI_SCALE.get());
            return Math.max(1.0f, Math.min(2.5f, value));
        } catch (Throwable ignored) {
            return 1.25f;
        }
    }

    public static int scale(int px) { return Math.max(1, Math.round(px * getUiScale())); }
    public static float scale(float px) { return Math.max(1.0f, px * getUiScale()); }

    private static void refreshOpenWindows() {
        Runnable run = new Runnable() {
            public void run() {
                final Window[] windows = Window.getWindows();
                for (int i = 0; i < windows.length; i++) {
                    Window w = windows[i];
                    try {
                        w.setBackground(WINDOW_BACKGROUND);
                        SwingUtilities.updateComponentTreeUI(w);
                        refreshThemeSensitiveComponents(w);
                        w.invalidate();
                        w.validate();
                        w.repaint();
                    } catch (Throwable ignored) { }
                }

                // Metal may finish reinstalling delegates after updateComponentTreeUI.
                // Run one more recolor pass on the next EDT turn. This prevents
                // stale Light-theme viewports/panels from surviving in dark themes.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        for (int i = 0; i < windows.length; i++) {
                            Window w = windows[i];
                            try {
                                refreshThemeSensitiveComponents(w);
                                w.invalidate();
                                w.validate();
                                w.repaint();
                            } catch (Throwable ignored) { }
                        }
                    }
                });
            }
        };
        if (SwingUtilities.isEventDispatchThread()) run.run();
        else SwingUtilities.invokeLater(run);
    }

    /**
     * Hard-sync colors for components whose UI delegates/cache can retain the
     * previous theme. In particular an empty JTable viewport used by the
     * attribute pane used to remain pure white after Light -> dark switching.
     */
    private static void refreshThemeSensitiveComponents(Component c) {
        if (c == null) return;
        try {
            String name = c.getClass().getName();
            boolean isCanvas = "com.cburch.logisim.gui.main.Canvas".equals(name);

            if (isCanvas) {
                c.setBackground(CANVAS_BACKGROUND);
                c.setForeground(CANVAS_TEXT);
            } else if (c instanceof JTable) {
                JTable t = (JTable) c;
                t.setBackground(INPUT_BACKGROUND);
                t.setForeground(FOREGROUND);
                t.setGridColor(BORDER);
                t.setSelectionBackground(SELECTION);
                t.setSelectionForeground(FOREGROUND);
                t.setFillsViewportHeight(true);
            } else if (c instanceof JTree) {
                c.setBackground(INPUT_BACKGROUND);
                c.setForeground(FOREGROUND);
            } else if (c instanceof JList) {
                c.setBackground(INPUT_BACKGROUND);
                c.setForeground(FOREGROUND);
            } else if (c instanceof JTextComponent) {
                c.setBackground(INPUT_BACKGROUND);
                c.setForeground(FOREGROUND);
            } else if (c instanceof JScrollPane) {
                c.setBackground(PANEL_BACKGROUND);
                c.setForeground(FOREGROUND);
            } else if (c instanceof JViewport) {
                JViewport vp = (JViewport) c;
                Component view = vp.getView();
                Color viewportColor = PANEL_BACKGROUND;
                if (view != null) {
                    String viewName = view.getClass().getName();
                    if ("com.cburch.logisim.gui.main.Canvas".equals(viewName)) {
                        viewportColor = CANVAS_BACKGROUND;
                        view.setBackground(CANVAS_BACKGROUND);
                    } else if (view instanceof JTable || view instanceof JTree
                            || view instanceof JList || view instanceof JTextComponent) {
                        viewportColor = INPUT_BACKGROUND;
                        view.setBackground(INPUT_BACKGROUND);
                    }
                }
                vp.setOpaque(true);
                vp.setBackground(viewportColor);
            }

            if (c instanceof JComponent) {
                ((JComponent) c).revalidate();
                ((JComponent) c).repaint();
            }
            if (c instanceof Container) {
                Component[] children = ((Container) c).getComponents();
                for (int i = 0; i < children.length; i++) {
                    refreshThemeSensitiveComponents(children[i]);
                }
            }
        } catch (Throwable ignored) { }
    }

    private static void useModernUIFont(float scale) {
        // IMPORTANT: sizes are always derived from fixed logical values.
        // Never multiply the currently-installed Swing font: doing that on each
        // live theme change caused the old "growing text" bug.
        String family = chooseUIFontFamily();
        String[] regular125 = {
            "Label.font", "Button.font", "ToggleButton.font", "RadioButton.font",
            "CheckBox.font", "ComboBox.font", "List.font", "Table.font",
            "TextField.font", "FormattedTextField.font", "PasswordField.font",
            "TextArea.font", "TextPane.font", "EditorPane.font", "Spinner.font",
            "OptionPane.font", "Panel.font", "ProgressBar.font", "TabbedPane.font"
        };
        for (int i = 0; i < regular125.length; i++)
            setFont(regular125[i], family, Font.PLAIN, 12.5f * scale);

        setFont("MenuBar.font", family, Font.PLAIN, 13.0f * scale);
        setFont("Menu.font", family, Font.PLAIN, 13.0f * scale);
        setFont("MenuItem.font", family, Font.PLAIN, 12.5f * scale);
        setFont("CheckBoxMenuItem.font", family, Font.PLAIN, 12.5f * scale);
        setFont("RadioButtonMenuItem.font", family, Font.PLAIN, 12.5f * scale);
        setFont("PopupMenu.font", family, Font.PLAIN, 12.5f * scale);
        setFont("Tree.font", family, Font.PLAIN, 13.0f * scale);
        setFont("TableHeader.font", family, Font.BOLD, 12.5f * scale);
        setFont("ToolTip.font", family, Font.PLAIN, 12.0f * scale);
        setFont("TitledBorder.font", family, Font.BOLD, 12.0f * scale);
        setFont("ToolBar.font", family, Font.PLAIN, 12.5f * scale);
    }

    /**
     * Uses FlatLaf when it is bundled in the release and retains a Metal
     * fallback for development builds. Reflection keeps the original source
     * tree buildable without making the theme engine a compile-time dependency.
     */
    private static boolean installModernLookAndFeel() {
        try {
            System.setProperty("flatlaf.useWindowDecorations", "false");
            String className = lightTheme
                    ? "com.formdev.flatlaf.FlatLightLaf"
                    : "com.formdev.flatlaf.FlatDarkLaf";
            Class<?> type = Class.forName(className);
            Object instance = type.newInstance();
            if (!(instance instanceof LookAndFeel)) return false;
            UIManager.setLookAndFeel((LookAndFeel) instance);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Installs spacing, radii and state colors shared by all application windows. */
    private static void installModernMetrics() {
        int arc = Math.max(8, scale(8));
        int smallArc = Math.max(6, scale(6));
        int focus = Math.max(1, scale(1));

        put("Component.arc", Integer.valueOf(arc));
        put("Component.focusWidth", Integer.valueOf(focus));
        put("Component.innerFocusWidth", Integer.valueOf(0));
        put("Component.focusColor", ui(ACCENT));
        put("Component.focusedBorderColor", ui(ACCENT));
        put("Component.borderColor", ui(BORDER));
        put("Component.disabledBorderColor", ui(BORDER_SOFT));
        put("Component.minimumHeight", Integer.valueOf(Math.max(26, scale(25))));

        put("Button.arc", Integer.valueOf(arc));
        put("Button.margin", new Insets(scale(5), scale(11), scale(5), scale(11)));
        put("Button.hoverBackground", ui(HOVER));
        put("Button.pressedBackground", ui(PRESSED));
        put("Button.selectedBackground", ui(SELECTION));
        put("Button.default.background", ui(ACCENT));
        put("Button.default.foreground", ui(lightTheme ? Color.WHITE : new Color(12, 20, 30)));
        put("Button.default.focusedBorderColor", ui(ACCENT_HOVER));
        put("Button.paintShadow", Boolean.FALSE);
        put("ToggleButton.arc", Integer.valueOf(arc));
        put("ToggleButton.selectedBackground", ui(SELECTION));
        put("ToggleButton.toolbar.selectedBackground", ui(SELECTION));

        put("TextComponent.arc", Integer.valueOf(smallArc));
        put("TextField.margin", new Insets(scale(4), scale(7), scale(4), scale(7)));
        put("FormattedTextField.margin", new Insets(scale(4), scale(7), scale(4), scale(7)));
        put("ComboBox.padding", new Insets(scale(3), scale(7), scale(3), scale(7)));
        put("Spinner.padding", new Insets(scale(3), scale(6), scale(3), scale(6)));

        put("ScrollBar.showButtons", Boolean.FALSE);
        put("ScrollBar.track", ui(PANEL_BACKGROUND));
        put("ScrollBar.thumb", ui(BORDER));
        put("ScrollBar.hoverThumbColor", ui(FOREGROUND_MUTED));
        put("ScrollBar.pressedThumbColor", ui(ACCENT));
        put("ScrollBar.thumbArc", Integer.valueOf(999));
        put("ScrollBar.trackArc", Integer.valueOf(999));
        put("ScrollBar.thumbInsets", new Insets(scale(2), scale(2), scale(2), scale(2)));
        put("ScrollBar.minimumThumbSize", new Dimension(scale(20), scale(20)));

        put("MenuItem.margin", new Insets(scale(5), scale(8), scale(5), scale(8)));
        put("MenuItem.selectionArc", Integer.valueOf(smallArc));
        put("MenuItem.selectionInsets", new Insets(scale(1), scale(4), scale(1), scale(4)));
        put("MenuItem.iconTextGap", Integer.valueOf(scale(8)));
        put("PopupMenu.borderInsets", new Insets(scale(5), scale(5), scale(5), scale(5)));

        put("TabbedPane.tabArc", Integer.valueOf(arc));
        put("TabbedPane.tabHeight", Integer.valueOf(Math.max(28, scale(28))));
        put("TabbedPane.tabInsets", new Insets(scale(6), scale(12), scale(6), scale(12)));
        put("TabbedPane.selectedBackground", ui(PANEL_BACKGROUND_ALT));
        put("TabbedPane.hoverColor", ui(HOVER));
        put("TabbedPane.underlineColor", ui(ACCENT));
        put("TabbedPane.focusColor", ui(ACCENT));
        put("TabbedPane.showTabSeparators", Boolean.FALSE);

        put("ProgressBar.arc", Integer.valueOf(999));
        put("ProgressBar.horizontalSize", new Dimension(scale(146), scale(10)));
        put("Separator.height", Integer.valueOf(1));
        put("Separator.stripeWidth", Integer.valueOf(1));
        put("Table.showHorizontalLines", Boolean.TRUE);
        put("Table.showVerticalLines", Boolean.FALSE);
        put("Table.intercellSpacing", new Dimension(0, 1));
    }

    private static void setFont(String key, String family, int style, float size) {
        UIManager.put(key, new FontUIResource(new Font(family, style, Math.max(10, Math.round(size)))));
    }

    private static String chooseUIFontFamily() {
        Set<String> families = new HashSet<String>();
        try {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for (int i = 0; i < names.length; i++) families.add(names[i]);
        } catch (Throwable ignored) { }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.indexOf("win") >= 0 && families.contains("Segoe UI Variable Text")) return "Segoe UI Variable Text";
        if (os.indexOf("win") >= 0 && families.contains("Segoe UI")) return "Segoe UI";
        if (os.indexOf("mac") >= 0 && families.contains("SF Pro Text")) return "SF Pro Text";
        if (families.contains("Inter")) return "Inter";
        if (families.contains("Noto Sans")) return "Noto Sans";
        if (families.contains("DejaVu Sans")) return "DejaVu Sans";
        return "SansSerif";
    }

    /** Paints a seamless app-wide gradient behind ordinary Swing panels. */
    public static void paintSurface(Graphics2D g2, JComponent c) {
        if (g2 == null || c == null) return;
        Component root = SwingUtilities.getRoot(c);
        if (root == null) root = c;
        Point p;
        try { p = SwingUtilities.convertPoint(c, 0, 0, root); }
        catch (Throwable t) { p = new Point(0, 0); }

        int rw = Math.max(1, root.getWidth());
        int rh = Math.max(1, root.getHeight());
        float x1 = -p.x;
        float y1 = -p.y;
        float x2 = rw - p.x;
        float y2 = rh - p.y;
        if (Math.abs(x2 - x1) < 1f && Math.abs(y2 - y1) < 1f) y2 = y1 + 1f;

        LinearGradientPaint paint = new LinearGradientPaint(
                new Point2D.Float(x1, y1), new Point2D.Float(x2, y2),
                new float[] { 0.0f, 0.52f, 1.0f },
                new Color[] { SURFACE_GRADIENT_START, SURFACE_GRADIENT_MID, SURFACE_GRADIENT_END });
        g2.setPaint(paint);
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());
    }

    /** Builds a richer three-stop gradient for the visible circuit viewport.
     * Sky UI 9 adds a deliberately subtle drift when UI animations are enabled.
     * It changes only paint coordinates, never circuit state or serialization. */
    public static LinearGradientPaint createCanvasGradient(float x1, float y1, float x2, float y2) {
        if (Math.abs(x2 - x1) < 1f && Math.abs(y2 - y1) < 1f) x2 = x1 + 1f;
        try {
            if (AppPreferences.UI_ANIMATIONS.getBoolean() && !lightTheme) {
                double t = (System.currentTimeMillis() % 18000L) / 18000.0 * Math.PI * 2.0;
                float w = Math.max(1f, Math.abs(x2 - x1));
                float h = Math.max(1f, Math.abs(y2 - y1));
                float dx = (float) (Math.sin(t) * w * 0.055);
                float dy = (float) (Math.cos(t * 0.83) * h * 0.045);
                x1 += dx;
                x2 -= dx * 0.45f;
                y1 += dy;
                y2 -= dy * 0.35f;
            }
        } catch (Throwable ignored) { }
        return new LinearGradientPaint(
                new Point2D.Float(x1, y1), new Point2D.Float(x2, y2),
                new float[] { 0.0f, 0.48f, 1.0f },
                new Color[] { CANVAS_GRADIENT_START, CANVAS_GRADIENT_MID, CANVAS_GRADIENT_END });
    }

    private static ColorUIResource ui(Color c) { return new ColorUIResource(c); }
    private static void put(String key, Object value) { UIManager.put(key, value); }

    private static Color darken(Color c, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        return new Color(Math.max(0, Math.round(c.getRed() * (1f - amount))),
                Math.max(0, Math.round(c.getGreen() * (1f - amount))),
                Math.max(0, Math.round(c.getBlue() * (1f - amount))));
    }

    private static Color lighten(Color c, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        return new Color(Math.min(255, Math.round(c.getRed() + (255 - c.getRed()) * amount)),
                Math.min(255, Math.round(c.getGreen() + (255 - c.getGreen()) * amount)),
                Math.min(255, Math.round(c.getBlue() + (255 - c.getBlue()) * amount)));
    }
}
