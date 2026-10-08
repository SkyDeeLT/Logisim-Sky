package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.circuit.Analyze;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;

public final class CircuitTestPanel extends JPanel {
    private static final class TestCase {
        String inputs;
        String outputs;
        TestCase(String inputs, String outputs) { this.inputs = inputs; this.outputs = outputs; }
    }

    private final Project project;
    private final ArrayList<TestCase> cases = new ArrayList<TestCase>();
    private final JTextField inputField = new JTextField(8);
    private final JTextField outputField = new JTextField(8);
    private final DefaultTableModel tableModel = new DefaultTableModel(new Object[] { "Входы", "Ожидается", "Результат" }, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel summary = new JLabel("Добавьте тесты для текущей схемы");

    public CircuitTestPanel(Project project) {
        super(new BorderLayout(DarkTheme.scale(6), DarkTheme.scale(6)));
        this.project = project;
        setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Тесты схемы");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10), DarkTheme.scale(4), DarkTheme.scale(10)));
        top.add(title, BorderLayout.NORTH);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, DarkTheme.scale(6), DarkTheme.scale(4)));
        form.setOpaque(false);
        form.add(new JLabel("Входы"));
        form.add(inputField);
        form.add(new JLabel("→"));
        form.add(new JLabel("Выходы"));
        form.add(outputField);
        JButton add = new JButton("Добавить");
        add.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addCase(); } });
        form.add(add);
        top.add(form, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        table.setFillsViewportHeight(true);
        table.setRowHeight(DarkTheme.scale(26));
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        summary.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(6), DarkTheme.scale(8), DarkTheme.scale(6), DarkTheme.scale(8)));
        bottom.add(summary, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, DarkTheme.scale(6), DarkTheme.scale(4)));
        buttons.setOpaque(false);
        JButton remove = new JButton("Удалить");
        remove.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { removeSelected(); } });
        JButton run = new JButton("Run Tests");
        run.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { runTests(); } });
        buttons.add(remove);
        buttons.add(run);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addCase() {
        String in = cleanBits(inputField.getText());
        String out = cleanBits(outputField.getText());
        if (in.length() == 0 || out.length() == 0) {
            summary.setText("Введите двоичные входы и ожидаемые выходы");
            return;
        }
        cases.add(new TestCase(in, out));
        tableModel.addRow(new Object[] { in, out, "—" });
        inputField.setText("");
        outputField.setText("");
        summary.setText("Тестов: " + cases.size());
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= cases.size()) return;
        cases.remove(row);
        tableModel.removeRow(row);
        summary.setText(cases.isEmpty() ? "Добавьте тесты для текущей схемы" : "Тестов: " + cases.size());
    }

    private void runTests() {
        Circuit circuit = project.getCurrentCircuit();
        if (circuit == null || cases.isEmpty()) return;
        try {
            Map<Instance, String> pins = Analyze.getPinLabels(circuit);
            int inputs = 0;
            int outputs = 0;
            for (Map.Entry<Instance, String> e : pins.entrySet()) {
                Instance pin = e.getKey();
                if (pin.getAttributeValue(StdAttr.WIDTH).getWidth() != 1) {
                    summary.setText("Тесты пока поддерживают 1-битовые контакты");
                    return;
                }
                if (Pin.FACTORY.isInputPin(pin)) inputs++; else outputs++;
            }
            if (inputs == 0 || outputs == 0 || inputs > 12) {
                summary.setText("Нужны входы/выходы; максимум 12 входов");
                return;
            }
            AnalyzerModel model = new AnalyzerModel();
            model.setCurrentCircuit(project, circuit);
            Analyze.computeTable(model, project, circuit, pins);
            TruthTable truth = model.getTruthTable();
            int passed = 0;
            for (int i = 0; i < cases.size(); i++) {
                TestCase tc = cases.get(i);
                String result;
                if (tc.inputs.length() != inputs || tc.outputs.length() != outputs) {
                    result = "⚠ размер";
                } else {
                    int row = Integer.parseInt(tc.inputs, 2);
                    StringBuilder actual = new StringBuilder();
                    for (int o = 0; o < outputs; o++) {
                        Entry entry = truth.getOutputEntry(row, o);
                        String d = entry.getDescription();
                        actual.append(d.length() == 0 ? "?" : d.charAt(0));
                    }
                    if (actual.toString().equals(tc.outputs)) {
                        result = "✓ " + actual;
                        passed++;
                    } else {
                        result = "✕ " + actual;
                    }
                }
                tableModel.setValueAt(result, i, 2);
            }
            summary.setText(passed + "/" + cases.size() + " passed");
        } catch (Throwable ex) {
            summary.setText("Не удалось выполнить тесты: " + ex.getClass().getSimpleName());
        }
    }

    private static String cleanBits(String s) {
        if (s == null) return "";
        return s.replace(" ", "").replace("_", "").trim();
    }
}
