package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.proj.Project;

public final class InspectorDock extends JPanel {
    private final LiveAnalysisPanel live;
    private final DiagnosticsPanel diagnostics;
    private final CircuitTestPanel tests;
    private final UndoHistoryPanel history;
    private final JTabbedPane tabs = new JTabbedPane();

    public InspectorDock(Project project) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, DarkTheme.BORDER_SOFT));
        live = new LiveAnalysisPanel(project);
        diagnostics = new DiagnosticsPanel(project);
        tests = new CircuitTestPanel(project);
        history = new UndoHistoryPanel(project);
        tabs.addTab("Анализ", live);
        tabs.addTab("Ошибки", diagnostics);
        tabs.addTab("Тесты", tests);
        tabs.addTab("История", history);
        add(tabs, BorderLayout.CENTER);
    }

    public void close() {
        live.close();
        diagnostics.close();
        history.close();
    }

    public void scheduleRefresh() {
        live.scheduleRefresh();
        diagnostics.scheduleRefresh();
    }

    public LiveAnalysisPanel getLiveAnalysis() { return live; }
    public DiagnosticsPanel getDiagnostics() { return diagnostics; }
}
