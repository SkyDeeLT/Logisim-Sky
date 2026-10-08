package com.cburch.logisim.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.SelectTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.WiringTool;

/**
 * HiDPI-safe vector icons used by the modern skin. They are drawn at runtime,
 * so no bitmap gets stretched on high-DPI displays. UI-only: no project data
 * or .circ serialization is touched.
 */
public final class ModernIcons {
    private ModernIcons() { }
    private static final Color GREEN = new Color(63, 211, 140);
    private static final Color RED = new Color(244, 96, 108);
    private static final Color YELLOW = new Color(241, 190, 72);

    public static Icon getActionIcon(String name, int size) {
        String kind = null;
        if ("projadd.gif".equals(name)) kind = "plus";
        else if ("projup.gif".equals(name)) kind = "up";
        else if ("projdown.gif".equals(name)) kind = "down";
        else if ("projdel.gif".equals(name)) kind = "delete";
        else if ("projlayo.gif".equals(name)) kind = "layout";
        else if ("projapp.gif".equals(name)) kind = "appearance";
        else if ("projtool.gif".equals(name)) kind = "toolbox";
        else if ("projsim.gif".equals(name)) kind = "simulation";
        else if ("simplay.png".equals(name)) kind = "play";
        else if ("simstop.png".equals(name)) kind = "stop";
        else if ("simstep.png".equals(name)) kind = "step";
        else if ("simtplay.png".equals(name)) kind = "tickplay";
        else if ("simtstop.png".equals(name)) kind = "tickstop";
        else if ("simtstep.png".equals(name)) kind = "tickstep";
        else if ("select.gif".equals(name)) kind = "select";
        else if ("poke.gif".equals(name)) kind = "poke";
        else if ("wiring.gif".equals(name)) kind = "wire";
        else if ("text.gif".equals(name)) kind = "text";
        return kind == null ? null : new VectorIcon(kind, size);
    }

    /** Small semantic icons for menus; action names come from LogisimMenuItem. */
    public static Icon getMenuIcon(String action) {
        if (action == null) return null;
        String kind = null;
        if ("Print".equals(action)) kind = "print";
        else if ("ExportImage".equals(action)) kind = "export";
        else if ("Cut".equals(action)) kind = "cut";
        else if ("Copy".equals(action)) kind = "copy";
        else if ("Paste".equals(action)) kind = "paste";
        else if ("Delete".equals(action) || "RemoveCircuit".equals(action)) kind = "delete";
        else if ("Duplicate".equals(action)) kind = "duplicate";
        else if ("SelectAll".equals(action)) kind = "selectall";
        else if ("AddCircuit".equals(action)) kind = "plus";
        else if ("MoveCircuitUp".equals(action) || "Raise".equals(action)
                || "RaiseTop".equals(action)) kind = "up";
        else if ("MoveCircuitDown".equals(action) || "Lower".equals(action)
                || "LowerBottom".equals(action)) kind = "down";
        else if ("EditLayout".equals(action)) kind = "layout";
        else if ("EditAppearance".equals(action)) kind = "appearance";
        else if ("ViewToolbox".equals(action)) kind = "toolbox";
        else if ("ViewSimulation".equals(action)) kind = "simulation";
        else if ("AnalyzeCircuit".equals(action)) kind = "analyze";
        else if ("GetCircuitStatistics".equals(action)) kind = "stats";
        else if ("SimulateEnable".equals(action)) kind = "play";
        else if ("SimulateStep".equals(action)) kind = "step";
        else if ("TickEnable".equals(action)) kind = "tickplay";
        else if ("TickStep".equals(action)) kind = "tickstep";
        return kind == null ? null : new VectorIcon(kind, Math.max(16, DarkTheme.scale(14)));
    }

    public static Icon getUiIcon(String kind) {
        return kind == null ? null : new VectorIcon(kind, Math.max(16, DarkTheme.scale(14)));
    }

    private static class VectorIcon implements Icon {
        private final String kind;
        private final int size;
        VectorIcon(String kind, int size) { this.kind = kind; this.size = size; }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (!(g instanceof Graphics2D)) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            configure(g2, size);
            g2.scale(size / 24.0, size / 24.0);
            paintKind(g2, kind);
            g2.dispose();
        }
    }

    public static boolean paintToolIcon(Tool tool, Graphics2D target, int x, int y, int size) {
        if (tool == null || target == null) return false;
        Graphics2D g = (Graphics2D) target.create();
        g.translate(x, y);
        configure(g, size);
        g.scale(size / 24.0, size / 24.0);

        boolean handled = true;
        if (tool instanceof PokeTool) {
            paintKind(g, "poke");
        } else if (tool instanceof WiringTool) {
            paintKind(g, "wire");
        } else if (tool instanceof SelectTool || tool instanceof EditTool) {
            paintKind(g, "select");
        } else if (tool instanceof TextTool) {
            paintKind(g, "text");
        } else if (tool instanceof AddTool) {
            AddTool add = (AddTool) tool;
            ComponentFactory f = add.getFactory(true);
            if (f == null) {
                handled = false;
            } else {
                String n = f.getClass().getSimpleName();
                if ("Pin".equals(n)) paintPin(g);
                else if ("Probe".equals(n)) paintProbe(g);
                else if ("NotGate".equals(n)) paintNot(g);
                else if ("Buffer".equals(n)) paintBuffer(g);
                else if ("AndGate".equals(n)) paintAnd(g, false);
                else if ("NandGate".equals(n)) paintAnd(g, true);
                else if ("OrGate".equals(n)) paintOr(g, false, false);
                else if ("NorGate".equals(n)) paintOr(g, true, false);
                else if ("XorGate".equals(n) || "OddParityGate".equals(n)) paintOr(g, false, true);
                else if ("Clock".equals(n)) paintClock(g);
                else if ("Constant".equals(n)) paintConstant(g);
                else if ("Tunnel".equals(n)) paintTunnel(g);
                else if ("Splitter".equals(n)) paintSplitter(g);
                else if ("Text".equals(n)) paintKind(g, "text");
                else if ("Led".equals(n)) paintLed(g);
                else if ("Button".equals(n)) paintButton(g);
                else if ("SubcircuitFactory".equals(n)) paintCircuit(g);
                else handled = false;
            }
        } else {
            handled = false;
        }
        g.dispose();
        return handled;
    }

    private static void configure(Graphics2D g, int size) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(DarkTheme.FOREGROUND);
    }

    private static void paintKind(Graphics2D g, String kind) {
        if ("plus".equals(kind)) {
            g.setColor(GREEN); g.setStroke(stroke(2.2f));
            g.draw(new Line2D.Double(12, 5, 12, 19)); g.draw(new Line2D.Double(5, 12, 19, 12));
        } else if ("up".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(2.2f));
            g.draw(new Line2D.Double(12, 19, 12, 6));
            Path2D p = new Path2D.Double(); p.moveTo(6.5, 11); p.lineTo(12, 5.5); p.lineTo(17.5, 11); g.draw(p);
        } else if ("down".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(2.2f));
            g.draw(new Line2D.Double(12, 5, 12, 18));
            Path2D p = new Path2D.Double(); p.moveTo(6.5, 13); p.lineTo(12, 18.5); p.lineTo(17.5, 13); g.draw(p);
        } else if ("delete".equals(kind)) {
            g.setColor(RED); g.setStroke(stroke(2.3f));
            g.draw(new Line2D.Double(6, 6, 18, 18)); g.draw(new Line2D.Double(18, 6, 6, 18));
        } else if ("new".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            Path2D p = new Path2D.Double(); p.moveTo(6, 3.5); p.lineTo(15, 3.5); p.lineTo(19, 7.5);
            p.lineTo(19, 20.5); p.lineTo(6, 20.5); p.closePath(); g.draw(p);
            g.setColor(GREEN); g.setStroke(stroke(2.0f));
            g.draw(new Line2D.Double(12.5, 10, 12.5, 17)); g.draw(new Line2D.Double(9, 13.5, 16, 13.5));
        } else if ("open".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            Path2D p = new Path2D.Double(); p.moveTo(3.5, 7); p.lineTo(10, 7); p.lineTo(12, 9);
            p.lineTo(20.5, 9); p.lineTo(18, 19); p.lineTo(4.5, 19); p.closePath(); g.draw(p);
            g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(5, 11, 19, 11));
        } else if ("save".equals(kind) || "saveas".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 2, 2));
            g.draw(new RoundRectangle2D.Double(7, 5, 9, 5, 1, 1));
            g.setColor(DarkTheme.ACCENT); g.draw(new RoundRectangle2D.Double(7, 13, 10, 6, 1.5, 1.5));
            if ("saveas".equals(kind)) { g.setColor(YELLOW); g.draw(new Line2D.Double(15, 17, 21, 11)); }
        } else if ("export".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            g.draw(new RoundRectangle2D.Double(4, 8, 12, 12, 2, 2));
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(2.0f));
            g.draw(new Line2D.Double(10, 14, 20, 4)); g.draw(new Line2D.Double(14, 4, 20, 4));
            g.draw(new Line2D.Double(20, 4, 20, 10));
        } else if ("print".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            g.draw(new RoundRectangle2D.Double(4, 9, 16, 8, 2, 2));
            g.draw(new RoundRectangle2D.Double(7, 3.5, 10, 7, 1, 1));
            g.setColor(DarkTheme.ACCENT); g.draw(new RoundRectangle2D.Double(7, 14, 10, 6.5, 1, 1));
        } else if ("undo".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(2.0f));
            Path2D p = new Path2D.Double(); p.moveTo(9, 6); p.lineTo(4, 10.5); p.lineTo(9, 15); g.draw(p);
            g.draw(new java.awt.geom.Arc2D.Double(5, 6, 15, 13, -110, 220, java.awt.geom.Arc2D.OPEN));
        } else if ("cut".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.8f));
            g.draw(new Ellipse2D.Double(3, 4, 6, 6)); g.draw(new Ellipse2D.Double(3, 14, 6, 6));
            g.draw(new Line2D.Double(8, 9, 20, 19)); g.draw(new Line2D.Double(8, 15, 20, 5));
            g.setColor(DarkTheme.ACCENT); g.fill(new Ellipse2D.Double(10.5, 10.5, 3, 3));
        } else if ("copy".equals(kind) || "duplicate".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            g.draw(new RoundRectangle2D.Double(4, 4, 11, 13, 2, 2));
            g.setColor(DarkTheme.ACCENT); g.draw(new RoundRectangle2D.Double(9, 7, 11, 13, 2, 2));
            if ("duplicate".equals(kind)) {
                g.setColor(GREEN); g.setStroke(stroke(1.7f));
                g.draw(new Line2D.Double(14.5, 11, 14.5, 17)); g.draw(new Line2D.Double(11.5, 14, 17.5, 14));
            }
        } else if ("paste".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f));
            g.draw(new RoundRectangle2D.Double(5, 6, 14, 15, 2, 2));
            g.setColor(DarkTheme.ACCENT); g.draw(new RoundRectangle2D.Double(8, 3, 8, 5, 2, 2));
            g.draw(new Line2D.Double(8, 12, 16, 12)); g.draw(new Line2D.Double(8, 16, 14, 16));
        } else if ("selectall".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND_MUTED); g.setStroke(stroke(1.4f));
            g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 2, 2));
            g.setColor(GREEN); g.setStroke(stroke(2.0f));
            Path2D p = new Path2D.Double(); p.moveTo(8, 12); p.lineTo(11, 15); p.lineTo(17, 8); g.draw(p);
        } else if ("settings".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(2.0f));
            g.draw(new Ellipse2D.Double(7, 7, 10, 10)); g.draw(new Ellipse2D.Double(10, 10, 4, 4));
            g.setColor(DarkTheme.ACCENT);
            for (int i = 0; i < 8; i++) { double a = Math.PI * i / 4.0; g.draw(new Line2D.Double(12 + 6 * Math.cos(a), 12 + 6 * Math.sin(a), 12 + 9 * Math.cos(a), 12 + 9 * Math.sin(a))); }
        } else if ("quit".equals(kind)) {
            g.setColor(RED); g.setStroke(stroke(2.1f));
            g.draw(new java.awt.geom.Arc2D.Double(5, 5, 14, 15, -55, -250, java.awt.geom.Arc2D.OPEN));
            g.draw(new Line2D.Double(12, 3, 12, 12));
        } else if ("analyze".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f));
            g.draw(new Line2D.Double(4, 17, 9, 11)); g.draw(new Line2D.Double(9, 11, 14, 14)); g.draw(new Line2D.Double(14, 14, 20, 6));
            g.setColor(GREEN); g.fill(new Ellipse2D.Double(2, 15, 4, 4)); g.fill(new Ellipse2D.Double(7, 9, 4, 4));
            g.fill(new Ellipse2D.Double(12, 12, 4, 4)); g.fill(new Ellipse2D.Double(18, 4, 4, 4));
        } else if ("stats".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND_MUTED); g.setStroke(stroke(1.5f));
            g.draw(new Line2D.Double(4, 20, 21, 20)); g.draw(new Line2D.Double(4, 20, 4, 4));
            g.setColor(DarkTheme.ACCENT); g.fill(new RoundRectangle2D.Double(7, 13, 3.5, 7, 1, 1));
            g.setColor(GREEN); g.fill(new RoundRectangle2D.Double(12, 9, 3.5, 11, 1, 1));
            g.setColor(YELLOW); g.fill(new RoundRectangle2D.Double(17, 5, 3.5, 15, 1, 1));
        } else if ("layout".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f));
            g.draw(new RoundRectangle2D.Double(4, 5, 5, 5, 1.5, 1.5));
            g.draw(new RoundRectangle2D.Double(15, 5, 5, 5, 1.5, 1.5));
            g.draw(new RoundRectangle2D.Double(9.5, 15, 5, 5, 1.5, 1.5));
            g.draw(new Line2D.Double(9, 7.5, 15, 7.5)); g.draw(new Line2D.Double(12, 10, 12, 15));
        } else if ("appearance".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f));
            g.draw(new Ellipse2D.Double(4, 7, 16, 10)); g.draw(new Ellipse2D.Double(9, 9.5, 6, 6));
            g.setColor(GREEN); g.fill(new Ellipse2D.Double(11, 11.5, 2, 2));
        } else if ("toolbox".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND_MUTED); g.setStroke(stroke(1.8f));
            g.draw(new RoundRectangle2D.Double(4, 8, 16, 11, 2, 2));
            g.draw(new RoundRectangle2D.Double(8, 5, 8, 5, 2, 2));
            g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(4, 12, 20, 12));
        } else if ("simulation".equals(kind)) {
            paintPlay(g); g.setColor(GREEN); g.setStroke(stroke(1.8f));
            g.draw(new Line2D.Double(4, 17, 8, 17)); g.draw(new Line2D.Double(8, 17, 10, 13)); g.draw(new Line2D.Double(10, 13, 13, 19));
        } else if ("play".equals(kind) || "tickplay".equals(kind)) {
            paintPlay(g);
            if ("tickplay".equals(kind)) { g.setColor(DarkTheme.FOREGROUND_MUTED); g.draw(new Line2D.Double(5, 19, 19, 19)); }
        } else if ("stop".equals(kind) || "tickstop".equals(kind)) {
            g.setColor(RED); g.fill(new RoundRectangle2D.Double(7, 7, 10, 10, 2, 2));
            if ("tickstop".equals(kind)) { g.setColor(DarkTheme.FOREGROUND_MUTED); g.draw(new Line2D.Double(5, 20, 19, 20)); }
        } else if ("step".equals(kind) || "tickstep".equals(kind)) {
            g.setColor(DarkTheme.ACCENT); Path2D p = new Path2D.Double(); p.moveTo(6, 6); p.lineTo(15, 12); p.lineTo(6, 18); p.closePath(); g.fill(p);
            g.setStroke(stroke(2.0f)); g.draw(new Line2D.Double(17.5, 6, 17.5, 18));
            if ("tickstep".equals(kind)) { g.setColor(DarkTheme.FOREGROUND_MUTED); g.draw(new Line2D.Double(5, 20, 19, 20)); }
        } else if ("select".equals(kind)) {
            paintCursor(g);
        } else if ("poke".equals(kind)) {
            paintPoke(g);
        } else if ("wire".equals(kind)) {
            paintWire(g);
        } else if ("text".equals(kind)) {
            g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(2.0f));
            g.draw(new Line2D.Double(6, 5.5, 18, 5.5)); g.draw(new Line2D.Double(12, 5.5, 12, 19));
            g.draw(new Line2D.Double(8.5, 19, 15.5, 19));
        }
    }

    private static void paintPlay(Graphics2D g) {
        g.setColor(GREEN);
        Path2D p = new Path2D.Double(); p.moveTo(7, 5); p.lineTo(19, 12); p.lineTo(7, 19); p.closePath(); g.fill(p);
    }

    private static void paintCursor(Graphics2D g) {
        Path2D p = new Path2D.Double();
        p.moveTo(5, 3.5); p.lineTo(5.7, 19.2); p.lineTo(10.1, 15.0); p.lineTo(13.2, 21);
        p.lineTo(16.6, 19.2); p.lineTo(13.6, 13.6); p.lineTo(19.4, 13.0); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.fill(p); g.setColor(new Color(18, 22, 28)); g.setStroke(stroke(1.2f)); g.draw(p);
    }

    private static void paintPoke(Graphics2D g) {
        // clean hand/finger interaction icon
        Path2D p = new Path2D.Double();
        p.moveTo(8.5, 20); p.curveTo(6.4, 18.2, 5.3, 15.5, 5.9, 13.8);
        p.curveTo(6.3, 12.7, 7.7, 12.5, 8.6, 13.2); p.lineTo(8.6, 7.0);
        p.curveTo(8.6, 5.8, 9.5, 5.0, 10.6, 5.0); p.curveTo(11.8, 5.0, 12.5, 5.9, 12.5, 7.0);
        p.lineTo(12.5, 11.5); p.lineTo(13.2, 9.6); p.curveTo(13.6, 8.7, 14.8, 8.4, 15.6, 9.0);
        p.curveTo(16.1, 9.4, 16.2, 10.0, 16.1, 10.7); p.curveTo(16.7, 9.8, 17.9, 9.7, 18.6, 10.4);
        p.curveTo(20.0, 11.8, 19.6, 16.6, 17.8, 19.9); p.closePath();
        g.setColor(new Color(218, 163, 124)); g.fill(p); g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.25f)); g.draw(p);
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.6f));
        g.draw(new Line2D.Double(6.0, 5.2, 4.5, 3.7)); g.draw(new Line2D.Double(8.3, 3.4, 8.0, 1.5));
        g.draw(new Line2D.Double(11.0, 3.2, 11.7, 1.5));
    }

    private static void paintWire(Graphics2D g) {
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(2.0f));
        g.draw(new Line2D.Double(5, 17, 18, 7));
        g.setColor(GREEN); g.fill(new Ellipse2D.Double(2.5, 14.5, 5, 5)); g.fill(new Ellipse2D.Double(15.5, 4.5, 5, 5));
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.0f)); g.draw(new Ellipse2D.Double(2.5, 14.5, 5, 5)); g.draw(new Ellipse2D.Double(15.5, 4.5, 5, 5));
    }

    private static void paintPin(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.6f)); g.draw(new RoundRectangle2D.Double(4, 7, 12, 10, 2, 2));
        g.setColor(GREEN); g.fill(new Ellipse2D.Double(7, 10, 4, 4));
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(16, 12, 21, 12));
    }

    private static void paintProbe(Graphics2D g) {
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(3, 12, 8, 12));
        g.setColor(DarkTheme.FOREGROUND); g.draw(new Ellipse2D.Double(8, 6, 12, 12));
        g.setColor(GREEN); g.fill(new Ellipse2D.Double(12, 10, 4, 4));
    }

    private static void paintBuffer(Graphics2D g) {
        Path2D p = new Path2D.Double(); p.moveTo(5, 5); p.lineTo(18, 12); p.lineTo(5, 19); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.8f)); g.draw(p); g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(2, 12, 5, 12)); g.draw(new Line2D.Double(18, 12, 22, 12));
    }

    private static void paintNot(Graphics2D g) {
        Path2D p = new Path2D.Double(); p.moveTo(5, 5); p.lineTo(16, 12); p.lineTo(5, 19); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.8f)); g.draw(p); g.draw(new Ellipse2D.Double(16, 10, 4, 4));
        g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(2, 12, 5, 12)); g.draw(new Line2D.Double(20, 12, 22, 12));
    }

    private static void paintAnd(Graphics2D g, boolean bubble) {
        Path2D p = new Path2D.Double();
        p.moveTo(5, 5); p.lineTo(11.5, 5); p.curveTo(17.3, 5, 19, 8.0, 19, 12); p.curveTo(19, 16, 17.3, 19, 11.5, 19); p.lineTo(5, 19); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.8f)); g.draw(p);
        g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(2, 9, 5, 9)); g.draw(new Line2D.Double(2, 15, 5, 15));
        if (bubble) { g.setColor(DarkTheme.FOREGROUND); g.draw(new Ellipse2D.Double(19, 10, 4, 4)); }
        else { g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(19, 12, 22, 12)); }
    }

    private static void paintOr(Graphics2D g, boolean bubble, boolean xor) {
        Path2D p = new Path2D.Double();
        p.moveTo(5, 5); p.curveTo(9, 8, 9, 16, 5, 19); p.curveTo(12, 19, 17, 17, 20, 12); p.curveTo(17, 7, 12, 5, 5, 5); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.8f)); g.draw(p);
        if (xor) {
            Path2D q = new Path2D.Double(); q.moveTo(3, 5); q.curveTo(7, 9, 7, 15, 3, 19); g.draw(q);
        }
        g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(2, 9, 7, 9)); g.draw(new Line2D.Double(2, 15, 7, 15));
        if (bubble) { g.setColor(DarkTheme.FOREGROUND); g.draw(new Ellipse2D.Double(20, 10, 4, 4)); }
        else { g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(20, 12, 23, 12)); }
    }

    private static void paintClock(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f)); g.draw(new Ellipse2D.Double(5, 5, 14, 14));
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.8f)); g.draw(new Line2D.Double(12, 8, 12, 12)); g.draw(new Line2D.Double(12, 12, 15.5, 14));
    }

    private static void paintConstant(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.6f)); g.draw(new RoundRectangle2D.Double(4, 6, 16, 12, 3, 3));
        drawCentered(g, "1", 12, 13.8f, DarkTheme.ACCENT, 10.5f, Font.BOLD);
    }

    private static void paintTunnel(Graphics2D g) {
        Path2D p = new Path2D.Double(); p.moveTo(4, 7); p.lineTo(17, 7); p.lineTo(21, 12); p.lineTo(17, 17); p.lineTo(4, 17); p.closePath();
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.7f)); g.draw(p); g.setColor(DarkTheme.ACCENT); g.draw(new Line2D.Double(1.5, 12, 4, 12));
    }

    private static void paintSplitter(Graphics2D g) {
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.9f)); g.draw(new Line2D.Double(4, 12, 11, 12));
        g.draw(new Line2D.Double(11, 12, 18, 6)); g.draw(new Line2D.Double(11, 12, 18, 12)); g.draw(new Line2D.Double(11, 12, 18, 18));
        g.setColor(DarkTheme.FOREGROUND); g.fill(new Ellipse2D.Double(9, 10, 4, 4));
    }

    private static void paintLed(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.6f)); g.draw(new Ellipse2D.Double(5, 5, 14, 14));
        g.setColor(GREEN); g.fill(new Ellipse2D.Double(8, 8, 8, 8));
    }

    private static void paintButton(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.6f)); g.draw(new RoundRectangle2D.Double(5, 5, 14, 14, 4, 4));
        g.setColor(DarkTheme.ACCENT); g.fill(new Ellipse2D.Double(9, 9, 6, 6));
    }


    private static void paintCircuit(Graphics2D g) {
        g.setColor(DarkTheme.FOREGROUND); g.setStroke(stroke(1.6f));
        g.draw(new RoundRectangle2D.Double(6, 5, 12, 14, 2.5, 2.5));
        g.setColor(DarkTheme.ACCENT); g.setStroke(stroke(1.7f));
        g.draw(new Line2D.Double(2.5, 9, 6, 9)); g.draw(new Line2D.Double(2.5, 15, 6, 15));
        g.draw(new Line2D.Double(18, 12, 21.5, 12));
        g.setColor(GREEN);
        g.fill(new Ellipse2D.Double(9.5, 8.5, 3, 3));
        g.fill(new Ellipse2D.Double(9.5, 13.0, 3, 3));
        g.setColor(DarkTheme.FOREGROUND_MUTED); g.setStroke(stroke(1.2f));
        g.draw(new Line2D.Double(12.5, 10, 15.5, 12)); g.draw(new Line2D.Double(12.5, 14.5, 15.5, 12));
    }

    private static BasicStroke stroke(float w) {
        return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static void drawCentered(Graphics2D g, String text, float x, float baseline, Color color, float size, int style) {
        Font old = g.getFont();
        Font f = old.deriveFont(style, size);
        g.setFont(f); g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x - fm.stringWidth(text) / 2.0f, baseline);
        g.setFont(old);
    }
}
