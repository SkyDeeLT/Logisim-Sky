package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.Timer;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

public final class UndoHistoryPanel extends JPanel implements ProjectListener {
    private final Project project;
    private final DefaultListModel<String> undoModel = new DefaultListModel<String>();
    private final DefaultListModel<String> redoModel = new DefaultListModel<String>();
    private final JList<String> undoList = new JList<String>(undoModel);
    private final JList<String> redoList = new JList<String>(redoModel);
    private final Timer refreshTimer;

    public UndoHistoryPanel(Project project) {
        super(new BorderLayout(DarkTheme.scale(6), DarkTheme.scale(6)));
        this.project = project;
        setOpaque(false);
        JLabel title = new JLabel("История действий");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10), DarkTheme.scale(4), DarkTheme.scale(10)));
        add(title, BorderLayout.NORTH);

        JPanel undoPanel = titled("Undo", undoList);
        JPanel redoPanel = titled("Redo", redoList);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, undoPanel, redoPanel);
        split.setResizeWeight(0.7);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        JButton undo = new JButton("Undo");
        JButton redo = new JButton("Redo");
        undo.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { project.undoAction(); refresh(); } });
        redo.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { project.redoAction(); refresh(); } });
        buttons.add(undo);
        buttons.add(redo);
        add(buttons, BorderLayout.SOUTH);

        undoList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = undoList.getSelectedIndex();
                    if (row >= 0) {
                        project.undoSteps(row + 1);
                        refresh();
                    }
                }
            }
        });

        project.addProjectListener(this);
        refreshTimer = new Timer(120, new ActionListener() { public void actionPerformed(ActionEvent e) { refresh(); } });
        refreshTimer.setRepeats(false);
        refresh();
    }

    private JPanel titled(String title, JList<String> list) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel label = new JLabel(title);
        label.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(4), DarkTheme.scale(8), DarkTheme.scale(4), DarkTheme.scale(8)));
        p.add(label, BorderLayout.NORTH);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    public void close() {
        refreshTimer.stop();
        project.removeProjectListener(this);
    }

    private void refresh() {
        undoModel.clear();
        redoModel.clear();
        List<String> undo = project.getUndoHistory();
        List<String> redo = project.getRedoHistory();
        for (String s : undo) undoModel.addElement(s);
        for (String s : redo) redoModel.addElement(s);
    }

    public void projectChanged(ProjectEvent event) { refreshTimer.restart(); }
}
