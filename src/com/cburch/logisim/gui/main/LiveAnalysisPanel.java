package com.cburch.logisim.gui.main;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.LocaleManager;

/**
 * Compact, continuously refreshed truth-table/Karnaugh preview for the current circuit.
 * Full analysis remains available through the classic analyzer button.
 */
public class LiveAnalysisPanel extends JPanel
        implements CircuitListener, ProjectListener, PropertyChangeListener {
    private static final int MAX_LIVE_INPUTS = 6;

    private final Project project;
    private AnalyzerModel model = new AnalyzerModel();
    private final LiveTableModel tableModel = new LiveTableModel();
    private final JTable table = new JTable(tableModel);
    private final KarnaughPreview kmap = new KarnaughPreview();
    private final JComboBox<String> outputCombo = new JComboBox<String>();
    private final JLabel title = new JLabel();
    private final JLabel status = new JLabel();
    private final JLabel hint = new JLabel();
    private final JButton fullButton = new JButton();
    private final JTabbedPane tabs = new JTabbedPane();
    private final Timer debounce;
    private final Timer pulseTimer;
    private float pulse = 0f;
    private boolean pulseUp = true;
    private boolean refreshing = false;

    public LiveAnalysisPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        setOpaque(false);
        setPreferredSize(new Dimension(DarkTheme.scale(300), DarkTheme.scale(500)));
        setMinimumSize(new Dimension(DarkTheme.scale(235), DarkTheme.scale(300)));
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BORDER_SOFT));

        JPanel header = new JPanel(new BorderLayout(DarkTheme.scale(8), DarkTheme.scale(4))) {
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
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10),
                DarkTheme.scale(7), DarkTheme.scale(8)));

        title.setText(isRussian() ? "Live-анализ" : "Live Analysis");
        title.setFont(title.getFont().deriveFont(Font.BOLD, DarkTheme.scale(14.0f)));
        status.setText("● LIVE");
        status.setHorizontalAlignment(SwingConstants.RIGHT);
        status.setFont(status.getFont().deriveFont(Font.BOLD, DarkTheme.scale(10.5f)));
        header.add(title, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);

        JPanel controls = new JPanel(new BorderLayout(DarkTheme.scale(6), 0));
        controls.setOpaque(false);
        outputCombo.setToolTipText(isRussian() ? "Выход для карты Карно" : "Output for Karnaugh map");
        fullButton.setText(isRussian() ? "Полный анализ" : "Full analyzer");
        fullButton.setToolTipText(isRussian() ? "Открыть классическое окно анализа Logisim" : "Open the classic Logisim analyzer");
        fullButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Circuit c = project.getCurrentCircuit();
                if (c != null) ProjectCircuitActions.doAnalyze(project, c);
            }
        });
        outputCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { kmap.repaint(); }
        });
        controls.add(outputCombo, BorderLayout.CENTER);
        controls.add(fullButton, BorderLayout.EAST);
        controls.setBorder(BorderFactory.createEmptyBorder(0, DarkTheme.scale(10), DarkTheme.scale(8), DarkTheme.scale(10)));

        table.setRowHeight(DarkTheme.scale(22));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public java.awt.Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createEmptyBorder(0, DarkTheme.scale(5), 0, DarkTheme.scale(5)));
                if (selected) {
                    l.setBackground(DarkTheme.SELECTION);
                    l.setForeground(DarkTheme.FOREGROUND);
                } else {
                    l.setBackground((row & 1) == 0 ? DarkTheme.INPUT_BACKGROUND : DarkTheme.PANEL_BACKGROUND);
                    l.setForeground(DarkTheme.FOREGROUND);
                }
                return l;
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel kmapWrap = new JPanel(new BorderLayout());
        kmapWrap.setOpaque(false);
        kmapWrap.add(kmap, BorderLayout.CENTER);

        tabs.addTab(isRussian() ? "Таблица" : "Table", tableScroll);
        tabs.addTab(isRussian() ? "Карно" : "K-map", kmapWrap);

        hint.setHorizontalAlignment(SwingConstants.CENTER);
        hint.setForeground(DarkTheme.FOREGROUND_MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(6), DarkTheme.scale(8),
                DarkTheme.scale(8), DarkTheme.scale(8)));
        hint.setText(isRussian() ? "Изменения схемы анализируются автоматически" : "Circuit changes are analyzed automatically");

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(controls, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(hint, BorderLayout.SOUTH);

        debounce = new Timer(320, new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshNow(); }
        });
        debounce.setRepeats(false);

        pulseTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!AppPreferences.UI_ANIMATIONS.getBoolean() || !isShowing()) return;
                pulse += pulseUp ? 0.075f : -0.075f;
                if (pulse >= 1f) { pulse = 1f; pulseUp = false; }
                if (pulse <= 0f) { pulse = 0f; pulseUp = true; }
                updateStatusColor();
            }
        });
        pulseTimer.start();

        project.addCircuitListener(this);
        project.addProjectListener(this);
        AppPreferences.UI_THEME.addPropertyChangeListener(this);
        AppPreferences.UI_ANIMATIONS.addPropertyChangeListener(this);
        scheduleRefresh();
    }

    public void close() {
        debounce.stop();
        pulseTimer.stop();
        project.removeCircuitListener(this);
        project.removeProjectListener(this);
        AppPreferences.UI_THEME.removePropertyChangeListener(this);
        AppPreferences.UI_ANIMATIONS.removePropertyChangeListener(this);
    }

    public void scheduleRefresh() {
        if (!AppPreferences.LIVE_ANALYSIS.getBoolean()) return;
        debounce.restart();
    }

    private void refreshNow() {
        if (refreshing || !AppPreferences.LIVE_ANALYSIS.getBoolean()) return;
        Circuit circuit = project.getCurrentCircuit();
        if (circuit == null) {
            showMessage(isRussian() ? "Нет активной схемы" : "No active circuit");
            return;
        }
        try {
            Map<Instance, String> pins = Analyze.getPinLabels(circuit);
            int in = 0;
            int out = 0;
            for (Map.Entry<Instance, String> e : pins.entrySet()) {
                Instance pin = e.getKey();
                if (pin.getAttributeValue(StdAttr.WIDTH).getWidth() != 1) {
                    showMessage(isRussian() ? "Live-анализ: только 1-битовые контакты" : "Live analysis supports 1-bit pins only");
                    return;
                }
                if (Pin.FACTORY.isInputPin(pin)) in++; else out++;
            }
            if (in == 0 || out == 0) {
                showMessage(isRussian() ? "Добавьте входы и выходы для анализа" : "Add inputs and outputs to analyze the circuit");
                return;
            }
            if (in > MAX_LIVE_INPUTS) {
                showMessage((isRussian() ? "Live-превью ограничено " : "Live preview is limited to ")
                        + MAX_LIVE_INPUTS + (isRussian() ? " входами" : " inputs"));
                return;
            }

            refreshing = true;
            AnalyzerModel next = new AnalyzerModel();
            next.setCurrentCircuit(project, circuit);
            Analyze.computeTable(next, project, circuit, pins);
            model = next;
            tableModel.fireTableStructureChanged();
            configureColumns();
            rebuildOutputs();
            kmap.repaint();
            hint.setText(isRussian() ? "Обновлено автоматически • до 6 входов" : "Updated automatically • up to 6 inputs");
            status.setText("● LIVE");
            updateStatusColor();
        } catch (Throwable ex) {
            showMessage((isRussian() ? "Не удалось проанализировать: " : "Unable to analyze: ") + ex.getClass().getSimpleName());
        } finally {
            refreshing = false;
        }
    }

    private void configureColumns() {
        int cols = tableModel.getColumnCount();
        for (int i = 0; i < cols; i++) {
            int w = DarkTheme.scale(i < model.getInputs().size() ? 42 : 50);
            table.getColumnModel().getColumn(i).setPreferredWidth(w);
            table.getColumnModel().getColumn(i).setMinWidth(DarkTheme.scale(34));
        }
    }

    private void rebuildOutputs() {
        Object selected = outputCombo.getSelectedItem();
        outputCombo.removeAllItems();
        for (int i = 0; i < model.getOutputs().size(); i++) outputCombo.addItem(model.getOutputs().get(i));
        if (selected != null) outputCombo.setSelectedItem(selected);
        if (outputCombo.getSelectedIndex() < 0 && outputCombo.getItemCount() > 0) outputCombo.setSelectedIndex(0);
    }

    private void showMessage(String text) {
        model = new AnalyzerModel();
        tableModel.fireTableStructureChanged();
        outputCombo.removeAllItems();
        hint.setText(text);
        status.setText("●");
        status.setForeground(DarkTheme.FOREGROUND_MUTED);
        kmap.repaint();
    }

    private void updateStatusColor() {
        Color a = DarkTheme.ACCENT;
        Color b = DarkTheme.FOREGROUND;
        float t = AppPreferences.UI_ANIMATIONS.getBoolean() ? 0.15f + pulse * 0.35f : 0.15f;
        int r = Math.round(a.getRed() * (1f - t) + b.getRed() * t);
        int g = Math.round(a.getGreen() * (1f - t) + b.getGreen() * t);
        int bl = Math.round(a.getBlue() * (1f - t) + b.getBlue() * t);
        status.setForeground(new Color(r, g, bl));
    }

    public void circuitChanged(CircuitEvent event) {
        if (event.getCircuit() == project.getCurrentCircuit()) scheduleRefresh();
    }

    public void projectChanged(ProjectEvent event) {
        int a = event.getAction();
        if (a == ProjectEvent.ACTION_SET_CURRENT || a == ProjectEvent.ACTION_SET_FILE || a == ProjectEvent.ACTION_SET_STATE) scheduleRefresh();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BORDER_SOFT));
        updateStatusColor();
        repaint();
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

    private final class LiveTableModel extends AbstractTableModel {
        public int getRowCount() { return model.getTruthTable().getRowCount(); }
        public int getColumnCount() { return model.getInputs().size() + model.getOutputs().size(); }
        public String getColumnName(int col) {
            if (col < model.getInputs().size()) return model.getInputs().get(col);
            return model.getOutputs().get(col - model.getInputs().size());
        }
        public Object getValueAt(int row, int col) {
            TruthTable t = model.getTruthTable();
            Entry e = col < model.getInputs().size()
                    ? t.getInputEntry(row, col)
                    : t.getOutputEntry(row, col - model.getInputs().size());
            return e.getDescription();
        }
        public boolean isCellEditable(int r, int c) { return false; }
    }

    private final class KarnaughPreview extends JPanel {
        KarnaughPreview() {
            setOpaque(false);
            setPreferredSize(new Dimension(DarkTheme.scale(300), DarkTheme.scale(270)));
            setToolTipText(isRussian() ? "Мини-карта Карно для выбранного выхода" : "Mini Karnaugh map for selected output");
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int inputs = model.getInputs().size();
            int output = outputCombo.getSelectedIndex();
            if (inputs < 2 || inputs > 4 || output < 0 || model.getOutputs().size() == 0) {
                paintCentered(g2, isRussian() ? "Карта доступна для 2–4 входов" : "K-map is available for 2–4 inputs");
                g2.dispose();
                return;
            }

            int rowBits = inputs <= 2 ? 1 : (inputs == 3 ? 1 : 2);
            int colBits = inputs - rowBits;
            int rows = 1 << rowBits;
            int cols = 1 << colBits;
            int[] rowGray = grayOrder(rowBits);
            int[] colGray = grayOrder(colBits);

            int cellW = Math.max(DarkTheme.scale(42), Math.min(DarkTheme.scale(60), (getWidth() - DarkTheme.scale(48)) / cols));
            int cellH = DarkTheme.scale(40);
            int mapW = cols * cellW;
            int mapH = rows * cellH;
            int x0 = Math.max(DarkTheme.scale(38), (getWidth() - mapW) / 2 + DarkTheme.scale(12));
            int y0 = Math.max(DarkTheme.scale(58), (getHeight() - mapH) / 2 + DarkTheme.scale(12));

            g2.setFont(getFont().deriveFont(Font.BOLD, DarkTheme.scale(11.0f)));
            g2.setColor(DarkTheme.FOREGROUND_MUTED);
            String rowVars = joinVars(0, rowBits);
            String colVars = joinVars(rowBits, inputs);
            g2.drawString(rowVars + " \\ " + colVars, DarkTheme.scale(10), DarkTheme.scale(22));
            g2.drawString(model.getOutputs().get(output), DarkTheme.scale(10), DarkTheme.scale(42));

            for (int c = 0; c < cols; c++) {
                String code = bitString(colGray[c], colBits);
                int sw = g2.getFontMetrics().stringWidth(code);
                g2.drawString(code, x0 + c * cellW + (cellW - sw) / 2, y0 - DarkTheme.scale(8));
            }
            for (int r = 0; r < rows; r++) {
                String code = bitString(rowGray[r], rowBits);
                int sw = g2.getFontMetrics().stringWidth(code);
                g2.drawString(code, x0 - DarkTheme.scale(8) - sw, y0 + r * cellH + cellH / 2 + DarkTheme.scale(4));
            }

            TruthTable tt = model.getTruthTable();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int truthRow = (rowGray[r] << colBits) | colGray[c];
                    Entry value = tt.getOutputEntry(truthRow, output);
                    int x = x0 + c * cellW;
                    int y = y0 + r * cellH;
                    if (value == Entry.ONE) {
                        Color a = DarkTheme.ACCENT;
                        g2.setColor(new Color(a.getRed(), a.getGreen(), a.getBlue(), 62));
                    } else {
                        g2.setColor(new Color(DarkTheme.INPUT_BACKGROUND.getRed(), DarkTheme.INPUT_BACKGROUND.getGreen(),
                                DarkTheme.INPUT_BACKGROUND.getBlue(), 190));
                    }
                    g2.fillRoundRect(x + 1, y + 1, cellW - 2, cellH - 2, DarkTheme.scale(7), DarkTheme.scale(7));
                    g2.setStroke(new BasicStroke(Math.max(1f, DarkTheme.getUiScale() * 0.8f)));
                    g2.setColor(value == Entry.ONE ? DarkTheme.ACCENT : DarkTheme.BORDER);
                    g2.drawRoundRect(x + 1, y + 1, cellW - 3, cellH - 3, DarkTheme.scale(7), DarkTheme.scale(7));
                    String v = value.getDescription();
                    g2.setFont(getFont().deriveFont(Font.BOLD, DarkTheme.scale(13.0f)));
                    g2.setColor(DarkTheme.FOREGROUND);
                    int sw = g2.getFontMetrics().stringWidth(v);
                    g2.drawString(v, x + (cellW - sw) / 2, y + cellH / 2 + DarkTheme.scale(5));
                }
            }
            g2.dispose();
        }

        private void paintCentered(Graphics2D g2, String text) {
            g2.setColor(DarkTheme.FOREGROUND_MUTED);
            g2.setFont(getFont().deriveFont(Font.PLAIN, DarkTheme.scale(11.5f)));
            int sw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, Math.max(DarkTheme.scale(8), (getWidth() - sw) / 2), getHeight() / 2);
        }

        private int[] grayOrder(int bits) {
            int n = 1 << bits;
            int[] ret = new int[n];
            for (int i = 0; i < n; i++) ret[i] = i ^ (i >> 1);
            return ret;
        }

        private String bitString(int value, int bits) {
            String s = Integer.toBinaryString(value);
            StringBuilder b = new StringBuilder();
            for (int i = s.length(); i < bits; i++) b.append('0');
            b.append(s);
            return b.toString();
        }

        private String joinVars(int from, int to) {
            StringBuilder b = new StringBuilder();
            for (int i = from; i < to; i++) b.append(model.getInputs().get(i));
            return b.toString();
        }
    }

    private static boolean isRussian() {
        java.util.Locale loc = LocaleManager.getLocale();
        return loc != null && "ru".equalsIgnoreCase(loc.getLanguage());
    }
}
