package com.cburch.logisim.gui.main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public final class LogicAnalyzerPanel extends JPanel {
    private static final int MAX_SAMPLES = 180;

    private static final class Trace {
        final Location location;
        final String name;
        final LinkedList<Value> values = new LinkedList<Value>();

        Trace(Wire wire, int index) {
            this.location = wire.getEndLocation(0);
            this.name = "NET " + index + "  (" + location.getX() + ", " + location.getY() + ")";
        }
    }

    private final Project project;
    private final List<Trace> traces = new ArrayList<Trace>();
    private final WaveView waveView = new WaveView();
    private final JPanel body = new JPanel(new BorderLayout());
    private final JLabel title = new JLabel("Logic Analyzer");
    private final JButton toggle = new JButton("⌄");
    private final Timer sampleTimer;
    private boolean expanded;

    public LogicAnalyzerPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        setOpaque(false);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(4), DarkTheme.scale(8), DarkTheme.scale(4), DarkTheme.scale(6)));
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD));
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton clear = new JButton("Очистить");
        clear.setFocusable(false);
        clear.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clearSamples(); } });
        JButton remove = new JButton("Удалить все");
        remove.setFocusable(false);
        remove.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { traces.clear(); waveView.repaint(); updateTitle(); } });
        toggle.setFocusable(false);
        toggle.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setExpanded(!expanded); } });
        buttons.add(clear);
        buttons.add(remove);
        buttons.add(toggle);
        head.add(title, BorderLayout.WEST);
        head.add(buttons, BorderLayout.EAST);
        body.setOpaque(false);
        JScrollPane scroll = new JScrollPane(waveView);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        body.add(scroll, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        sampleTimer = new Timer(100, new ActionListener() { public void actionPerformed(ActionEvent e) { sample(); } });
        sampleTimer.start();
        setExpanded(false);
        updateTitle();
    }

    public void close() { sampleTimer.stop(); }

    public void addWire(Wire wire) {
        if (wire == null) return;
        Location loc = wire.getEndLocation(0);
        for (Trace trace : traces) if (trace.location.equals(loc)) { setExpanded(true); return; }
        traces.add(new Trace(wire, traces.size() + 1));
        setExpanded(true);
        updateTitle();
        waveView.revalidate();
        waveView.repaint();
    }

    public void addWires(Iterable<Wire> wires) {
        if (wires == null) return;
        for (Wire wire : wires) addWire(wire);
    }

    public boolean isExpanded() { return expanded; }

    public void setExpanded(boolean value) {
        expanded = value;
        body.setVisible(value);
        toggle.setText(value ? "⌃" : "⌄");
        int h = value ? DarkTheme.scale(210) : DarkTheme.scale(34);
        setPreferredSize(new Dimension(100, h));
        setMinimumSize(new Dimension(100, DarkTheme.scale(34)));
        revalidate();
    }

    private void clearSamples() {
        for (Trace trace : traces) trace.values.clear();
        waveView.repaint();
    }

    private void sample() {
        if (traces.isEmpty()) return;
        CircuitState state = project.getCircuitState();
        if (state == null) return;
        for (Trace trace : traces) {
            trace.values.add(state.getValue(trace.location));
            while (trace.values.size() > MAX_SAMPLES) trace.values.removeFirst();
        }
        if (expanded) waveView.repaint();
    }

    private void updateTitle() {
        title.setText("Logic Analyzer" + (traces.isEmpty() ? "" : "  •  " + traces.size() + " сигналов"));
    }

    @Override protected void paintComponent(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g.create();
            DarkTheme.paintSurface(g2, this);
            g2.dispose();
        } else {
            g.setColor(DarkTheme.PANEL_BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private final class WaveView extends JPanel {
        WaveView() {
            setOpaque(false);
            setPreferredSize(new Dimension(DarkTheme.scale(900), DarkTheme.scale(170)));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = DarkTheme.scale(145);
            int rowH = DarkTheme.scale(38);
            int top = DarkTheme.scale(10);
            int width = Math.max(1, getWidth() - left - DarkTheme.scale(12));
            g2.setFont(getFont().deriveFont((float) DarkTheme.scale(11)));
            FontMetrics fm = g2.getFontMetrics();
            for (int t = 0; t < traces.size(); t++) {
                Trace trace = traces.get(t);
                int cy = top + t * rowH + rowH / 2;
                g2.setColor(DarkTheme.FOREGROUND);
                g2.drawString(trace.name, DarkTheme.scale(8), cy + fm.getAscent() / 2 - 2);
                g2.setColor(DarkTheme.BORDER_SOFT);
                g2.drawLine(left, cy + rowH / 2 - 2, getWidth(), cy + rowH / 2 - 2);
                int count = trace.values.size();
                if (count == 0) continue;
                double step = Math.max(2.0, width / (double) Math.max(1, MAX_SAMPLES - 1));
                int start = Math.max(0, count - MAX_SAMPLES);
                Value first = trace.values.get(start);
                if (first.getWidth() == 1) {
                    g2.setStroke(new BasicStroke(Math.max(1f, DarkTheme.scale(1.5f))));
                    g2.setColor(DarkTheme.ACCENT);
                    int px = left;
                    int py = logicY(first, cy, rowH);
                    for (int i = start + 1; i < count; i++) {
                        Value v = trace.values.get(i);
                        int nx = left + (int) Math.round((i - start) * step);
                        int ny = logicY(v, cy, rowH);
                        if (ny != py) {
                            g2.drawLine(px, py, nx, py);
                            g2.drawLine(nx, py, nx, ny);
                        } else {
                            g2.drawLine(px, py, nx, ny);
                        }
                        px = nx;
                        py = ny;
                    }
                } else {
                    g2.setColor(DarkTheme.ACCENT);
                    g2.drawRoundRect(left, cy - DarkTheme.scale(10), width - 2, DarkTheme.scale(20), DarkTheme.scale(8), DarkTheme.scale(8));
                    Value last = trace.values.get(count - 1);
                    String text = "0x" + last.toHexString() + "  [" + last.getWidth() + " bit]";
                    g2.drawString(text, left + DarkTheme.scale(8), cy + fm.getAscent() / 2 - 2);
                }
            }
            int height = Math.max(DarkTheme.scale(100), top + traces.size() * rowH + DarkTheme.scale(10));
            if (getPreferredSize().height != height) {
                setPreferredSize(new Dimension(Math.max(getWidth(), DarkTheme.scale(900)), height));
                revalidate();
            }
            g2.dispose();
        }

        private int logicY(Value value, int cy, int rowH) {
            String s = value.toString();
            if ("1".equals(s)) return cy - rowH / 4;
            if ("0".equals(s)) return cy + rowH / 4;
            return cy;
        }
    }
}
