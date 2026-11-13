package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.CircuitTemplates;
import com.cburch.logisim.file.CircuitTemplates.Entry;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;

final class TemplatesPanel extends JPanel implements ProjectListener {
    private final Project project;
    private final JComboBox<String> categories = new JComboBox<>();
    private final DefaultListModel<Entry> model = new DefaultListModel<>();
    private final JList<Entry> templates = new JList<Entry>(model) {
        @Override
        public String getToolTipText(MouseEvent event) {
            int row = locationToIndex(event.getPoint());
            if (row < 0 || !getCellBounds(row, row).contains(event.getPoint())) return null;
            return model.get(row).getName();
        }
    };
    private final JScrollPane templateList = new JScrollPane(templates) {
        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    };
    private final JTextArea details = new JTextArea(3, 1);
    private final JTextArea status = new JTextArea(2, 1);
    private final JButton insert = new JButton();
    private final JButton save = new JButton();
    private final JButton delete = new JButton();
    private List<Entry> entries = Collections.emptyList();
    private boolean refreshing;
    private String catalogError;

    TemplatesPanel(Project project) {
        super(new BorderLayout(DarkTheme.scale(6), DarkTheme.scale(7)));
        this.project = project;
        setOpaque(false);
        int padding = DarkTheme.scale(8);
        setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        add(categories, BorderLayout.NORTH);

        JPanel footer = new JPanel() {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        configureText(details);
        configureText(status);
        JScrollPane description = new JScrollPane(details,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        description.setBorder(BorderFactory.createEmptyBorder());
        description.setOpaque(false);
        description.getViewport().setOpaque(false);
        footer.add(description);
        footer.add(status);

        JPanel insertRow = new JPanel(new BorderLayout(DarkTheme.scale(5), 0));
        insertRow.setOpaque(false);
        insertRow.add(insert, BorderLayout.CENTER);
        insertRow.add(delete, BorderLayout.EAST);
        footer.add(insertRow);
        JPanel saveRow = new JPanel(new BorderLayout());
        saveRow.setOpaque(false);
        saveRow.setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(5), 0, 0, 0));
        saveRow.add(save, BorderLayout.CENTER);
        footer.add(saveRow);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        templateList.setAlignmentX(LEFT_ALIGNMENT);
        footer.setAlignmentX(LEFT_ALIGNMENT);
        body.add(templateList);
        body.add(Box.createVerticalStrut(DarkTheme.scale(7)));
        body.add(footer);
        body.add(Box.createVerticalGlue());
        add(body, BorderLayout.CENTER);

        templates.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templates.setFixedCellHeight(DarkTheme.scale(25));
        templates.setToolTipText("");
        templates.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focused) {
                Entry entry = (Entry) value;
                super.getListCellRendererComponent(list, entry.getName(), index, selected, focused);
                setBorder(BorderFactory.createCompoundBorder(getBorder(),
                        BorderFactory.createEmptyBorder(0, DarkTheme.scale(6), 0, DarkTheme.scale(4))));
                return this;
            }
        });
        templates.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !refreshing) updateDetails();
        });
        categories.addActionListener(event -> {
            if (!refreshing) refreshRows(selectedId());
        });
        templates.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton(event)) return;
                int row = templates.locationToIndex(event.getPoint());
                if (row >= 0 && templates.getCellBounds(row, row).contains(event.getPoint())) placeTemplate();
            }
        });
        templates.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "placeTemplate");
        templates.getActionMap().put("placeTemplate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                placeTemplate();
            }
        });
        insert.addActionListener(event -> placeTemplate());
        save.addActionListener(event -> saveTemplate());
        delete.addActionListener(event -> deleteTemplate());
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                updateControls();
            }
        });
        project.addProjectListener(this);
        updateLabels();
    }

    private static void configureText(JTextArea text) {
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, DarkTheme.scale(5), 0));
    }

    void close() {
        project.removeProjectListener(this);
    }

    void updateLabels() {
        insert.setText(Strings.get("templatesInsert"));
        delete.setText(Strings.get("templatesDelete"));
        categories.getAccessibleContext().setAccessibleName(Strings.get("templatesCategory"));
        templates.getAccessibleContext().setAccessibleName(Strings.get("templatesTab"));
        updateDetails();
    }

    void refreshCatalog() {
        refreshCatalog(selectedId(), currentCategory());
    }

    private void refreshCatalog(String preferredId, String preferredCategory) {
        catalogError = null;
        try {
            entries = CircuitTemplates.list();
        } catch (IOException error) {
            catalogError = error.getLocalizedMessage();
            if (catalogError == null) catalogError = "";
            try {
                entries = CircuitTemplates.listBuiltins();
            } catch (IOException builtinsError) {
                entries = Collections.emptyList();
            }
        }
        Set<String> available = new LinkedHashSet<>();
        for (Entry entry : entries) available.add(entry.getCategory());
        refreshing = true;
        categories.removeAllItems();
        categories.addItem(Strings.get("templatesAllCategories"));
        for (String category : available) categories.addItem(category);
        if (available.contains(preferredCategory)) categories.setSelectedItem(preferredCategory);
        refreshing = false;
        refreshRows(preferredId);
    }

    private void refreshRows(String preferredId) {
        String category = currentCategory();
        refreshing = true;
        model.clear();
        int preferredRow = -1;
        for (Entry entry : entries) {
            if (category == null || category.equals(entry.getCategory())) {
                if (entry.getId().equals(preferredId)) preferredRow = model.size();
                model.addElement(entry);
            }
        }
        templates.setVisibleRowCount(Math.min(12, Math.max(1, model.size())));
        templateList.revalidate();
        if (!model.isEmpty()) {
            templates.setSelectedIndex(preferredRow >= 0 ? preferredRow : 0);
            templates.ensureIndexIsVisible(templates.getSelectedIndex());
        }
        refreshing = false;
        updateDetails();
    }

    private String currentCategory() {
        return categories.getSelectedIndex() > 0 ? (String) categories.getSelectedItem() : null;
    }

    private String selectedId() {
        Entry selected = templates.getSelectedValue();
        return selected == null ? null : selected.getId();
    }

    private Circuit editableCircuit() {
        Circuit current = project.getCurrentCircuit();
        return current != null && project.getLogisimFile().contains(current) ? current : null;
    }

    private Selection canvasSelection() {
        Frame frame = project.getFrame();
        return frame == null ? null : frame.getCanvas().getSelection();
    }

    private void updateDetails() {
        Entry entry = templates.getSelectedValue();
        if (entry != null) {
            details.setText(entry.getDescription());
        } else {
            details.setText(Strings.get(catalogError == null ? "templatesEmpty" : "templatesCatalogError"));
        }
        details.setCaretPosition(0);
        updateControls();
    }

    private void updateControls() {
        Entry entry = templates.getSelectedValue();
        Circuit current = editableCircuit();
        Selection selection = canvasSelection();
        boolean hasSelection = selection != null && !selection.isEmpty();
        boolean canSave = current != null
                && (hasSelection || !current.getNonWires().isEmpty() || !current.getWires().isEmpty());
        save.setText(Strings.get(hasSelection ? "templatesSaveSelection" : "templatesSaveCircuit"));
        save.setEnabled(canSave);
        save.setToolTipText(Strings.get(canSave ? "templatesSaveTip" : "templatesSaveUnavailable"));
        insert.setEnabled(current != null && entry != null);
        delete.setEnabled(entry != null && !entry.isBuiltin());
        delete.setToolTipText(Strings.get("templatesDeleteTip"));
        status.setToolTipText(catalogError);
        if (current == null) {
            status.setText(Strings.get("templatesReadOnly"));
        } else if (project.getTool() instanceof TemplatePlacementTool) {
            status.setText(Strings.get("templatesClickToPlace"));
        } else if (catalogError != null) {
            status.setText(Strings.get("templatesCatalogError"));
        } else {
            status.setText(entry == null ? "" : Strings.get("templatesInsertHint"));
        }
    }

    private void placeTemplate() {
        Entry entry = templates.getSelectedValue();
        if (!insert.isEnabled() || entry == null) return;
        try {
            LogisimFile template = CircuitTemplates.load(entry, project.getLogisimFile().getLoader());
            TemplatePlacementTool.start(project, template, entry.getName());
        } catch (IOException error) {
            showError("templatesInsertError", error);
        }
    }

    private void saveTemplate() {
        Circuit current = editableCircuit();
        if (!save.isEnabled() || current == null) return;
        Selection selection = canvasSelection();
        List<com.cburch.logisim.comp.Component> components = new ArrayList<>();
        boolean saveSelection = selection != null && !selection.isEmpty();
        if (saveSelection) {
            components.addAll(selection.getComponents());
        } else {
            components.addAll(current.getNonWires());
            components.addAll(current.getWires());
        }
        String name = (String) JOptionPane.showInputDialog(this,
                Strings.get(saveSelection ? "templatesNameSelection" : "templatesNameCircuit"),
                save.getText(), JOptionPane.PLAIN_MESSAGE, null, null, saveSelection ? "" : current.getName());
        if (name == null) return;
        if (name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, Strings.get("templatesNameRequired"),
                    Strings.get("templatesSaveError"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Entry saved = CircuitTemplates.save(name.trim(), null, project, components);
            refreshCatalog(saved.getId(), saved.getCategory());
        } catch (IOException error) {
            showError("templatesSaveError", error);
        }
    }

    private void deleteTemplate() {
        Entry selected = templates.getSelectedValue();
        if (selected == null || selected.isBuiltin()) return;
        int choice = JOptionPane.showConfirmDialog(this, Strings.get("templatesDeleteConfirm", selected.getName()),
                Strings.get("templatesDelete"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        try {
            CircuitTemplates.remove(selected);
            refreshCatalog(null, currentCategory());
        } catch (IOException error) {
            showError("templatesDeleteError", error);
        }
    }

    private void showError(String title, IOException error) {
        JOptionPane.showMessageDialog(this, error.getLocalizedMessage(), Strings.get(title), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void projectChanged(ProjectEvent event) {
        switch (event.getAction()) {
            case ProjectEvent.ACTION_SET_FILE:
            case ProjectEvent.ACTION_SET_CURRENT:
            case ProjectEvent.ACTION_SET_TOOL:
            case ProjectEvent.ACTION_SELECTION:
            case ProjectEvent.ACTION_COMPLETE:
            case ProjectEvent.UNDO_COMPLETE:
                if (isShowing()) updateControls();
                break;
            default:
                break;
        }
    }
}
