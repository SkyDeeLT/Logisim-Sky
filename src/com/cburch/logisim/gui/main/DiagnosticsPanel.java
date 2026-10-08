package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.WidthIncompatibilityData;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public final class DiagnosticsPanel extends JPanel implements CircuitListener, ProjectListener {
    private static final class Issue {
        final String kind;
        final String text;
        final Location location;
        Issue(String kind, String text, Location location) {
            this.kind = kind;
            this.text = text;
            this.location = location;
        }
        public String toString() { return kind + " " + text; }
    }

    private final Project project;
    private final DefaultListModel<Issue> model = new DefaultListModel<Issue>();
    private final JList<Issue> list = new JList<Issue>(model);
    private final JLabel header = new JLabel();
    private final Timer debounce;

    public DiagnosticsPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10), DarkTheme.scale(6), DarkTheme.scale(10)));
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(DarkTheme.scale(34));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, selected, focus);
                Issue issue = (Issue) value;
                c.setText("<html><b>" + issue.kind + "</b> &nbsp;" + escape(issue.text) + "</html>");
                c.setBorder(BorderFactory.createEmptyBorder(0, DarkTheme.scale(8), 0, DarkTheme.scale(8)));
                return c;
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 1) {
                    Issue issue = list.getSelectedValue();
                    if (issue != null && issue.location != null && project.getFrame() != null) project.getFrame().centerOn(issue.location);
                }
            }
        });
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        debounce = new Timer(250, e -> refreshNow());
        debounce.setRepeats(false);
        project.addCircuitListener(this);
        project.addProjectListener(this);
        refreshNow();
    }

    public void close() {
        debounce.stop();
        project.removeCircuitListener(this);
        project.removeProjectListener(this);
    }

    public void scheduleRefresh() { debounce.restart(); }

    private void refreshNow() {
        model.clear();
        Circuit circuit = project.getCurrentCircuit();
        if (circuit == null) {
            header.setText("Ошибки схемы");
            return;
        }
        Set<WidthIncompatibilityData> widths = circuit.getWidthIncompatibilityData();
        if (widths != null) {
            for (WidthIncompatibilityData data : widths) {
                Location loc = data.size() > 0 ? data.getPoint(0) : null;
                StringBuilder msg = new StringBuilder("Несовместимая ширина: ");
                for (int i = 0; i < data.size(); i++) {
                    if (i > 0) msg.append(" / ");
                    msg.append(data.getBitWidth(i).getWidth());
                }
                model.addElement(new Issue("⛔", msg.toString(), loc));
            }
        }
        for (com.cburch.logisim.comp.Component comp : circuit.getNonWires()) {
            List<EndData> ends = comp.getEnds();
            for (EndData end : ends) {
                Location loc = end.getLocation();
                if (circuit.isConnected(loc, comp)) continue;
                String name = comp.getFactory().getDisplayName();
                if (end.isInput() && !end.isOutput()) {
                    model.addElement(new Issue("⚠", "Неподключённый вход: " + name, loc));
                } else if (end.isOutput() && !end.isInput()) {
                    model.addElement(new Issue("⚠", "Неподключённый выход: " + name, loc));
                }
            }
        }
        header.setText(model.isEmpty() ? "Ошибки схемы • нет проблем" : "Ошибки схемы • " + model.size());
    }

    public void circuitChanged(CircuitEvent event) { scheduleRefresh(); }
    public void projectChanged(ProjectEvent event) { scheduleRefresh(); }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
