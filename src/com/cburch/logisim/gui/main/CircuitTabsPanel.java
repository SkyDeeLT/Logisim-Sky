package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public final class CircuitTabsPanel extends JPanel implements ProjectListener, LibraryListener, CircuitListener {
    private final Project project;
    private final JTabbedPane tabs = new JTabbedPane();
    private boolean updating;

    public CircuitTabsPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DarkTheme.BORDER_SOFT));
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setPreferredSize(new Dimension(100, DarkTheme.scale(34)));
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (updating) return;
                int index = tabs.getSelectedIndex();
                List<Circuit> circuits = CircuitTabsPanel.this.project.getLogisimFile().getCircuits();
                if (index >= 0 && index < circuits.size()) {
                    Circuit c = circuits.get(index);
                    if (c != CircuitTabsPanel.this.project.getCurrentCircuit()) CircuitTabsPanel.this.project.setCurrentCircuit(c);
                }
            }
        });
        JButton add = new JButton("+");
        add.setToolTipText("Новая схема");
        add.setFocusable(false);
        add.setPreferredSize(new Dimension(DarkTheme.scale(40), DarkTheme.scale(32)));
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ProjectCircuitActions.doAddCircuit(CircuitTabsPanel.this.project); }
        });
        add(tabs, BorderLayout.CENTER);
        add(add, BorderLayout.EAST);
        project.addProjectListener(this);
        project.addLibraryListener(this);
        project.addCircuitListener(this);
        rebuild();
    }

    public void close() {
        project.removeProjectListener(this);
        project.removeLibraryListener(this);
        project.removeCircuitListener(this);
    }

    private void rebuild() {
        updating = true;
        tabs.removeAll();
        List<Circuit> circuits = project.getLogisimFile().getCircuits();
        Circuit current = project.getCurrentCircuit();
        int selected = -1;
        for (int i = 0; i < circuits.size(); i++) {
            Circuit c = circuits.get(i);
            tabs.addTab(c.getName(), null);
            tabs.setToolTipTextAt(i, c == project.getLogisimFile().getMainCircuit() ? "Главная схема" : c.getName());
            if (c == current) selected = i;
        }
        if (selected >= 0) tabs.setSelectedIndex(selected);
        updating = false;
    }

    public void projectChanged(ProjectEvent event) {
        int a = event.getAction();
        if (a == ProjectEvent.ACTION_SET_CURRENT || a == ProjectEvent.ACTION_SET_FILE) rebuild();
    }

    public void libraryChanged(LibraryEvent event) { rebuild(); }
    public void circuitChanged(CircuitEvent event) { rebuild(); }
}
