package com.cburch.logisim.gui.main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public final class CommandPalette extends JDialog {
    private static final class Item {
        final Tool tool;
        final String path;
        final String search;

        Item(Tool tool, String path) {
            this.tool = tool;
            this.path = path;
            String aliases = aliasesFor(tool.getName());
            this.search = (tool.getName() + " " + tool.getDisplayName() + " " + path + " " + aliases).toLowerCase(Locale.ROOT);
        }

        public String toString() { return tool.getDisplayName(); }
    }

    private final Project project;
    private final JTextField query = new JTextField();
    private final DefaultListModel<Item> model = new DefaultListModel<Item>();
    private final JList<Item> list = new JList<Item>(model);
    private final List<Item> all = new ArrayList<Item>();

    public CommandPalette(Frame owner, Project project) {
        super(owner, false);
        this.project = project;
        setUndecorated(true);
        setAlwaysOnTop(true);
        JPanel root = new JPanel(new BorderLayout(DarkTheme.scale(8), DarkTheme.scale(8)));
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.ACCENT, Math.max(1, DarkTheme.scale(1))),
                BorderFactory.createEmptyBorder(DarkTheme.scale(10), DarkTheme.scale(10), DarkTheme.scale(10), DarkTheme.scale(10))));
        root.setBackground(DarkTheme.PANEL_BACKGROUND);
        query.setFont(query.getFont().deriveFont(Font.PLAIN, DarkTheme.scale(15.0f)));
        query.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BORDER_SOFT),
                BorderFactory.createEmptyBorder(DarkTheme.scale(8), DarkTheme.scale(10), DarkTheme.scale(8), DarkTheme.scale(10))));
        query.putClientProperty("JTextField.placeholderText", "Компонент или команда...");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        list.setFixedCellHeight(DarkTheme.scale(34));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index, boolean selected, boolean focus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, selected, focus);
                Item item = (Item) value;
                c.setText("<html><b>" + escape(item.tool.getDisplayName()) + "</b> <span style='color:#8a96a8'>" + escape(item.path) + "</span></html>");
                c.setBorder(BorderFactory.createEmptyBorder(0, DarkTheme.scale(8), 0, DarkTheme.scale(8)));
                return c;
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(DarkTheme.BORDER_SOFT));
        root.add(query, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        setContentPane(root);
        setPreferredSize(new Dimension(DarkTheme.scale(560), DarkTheme.scale(390)));
        pack();
        collect(project.getLogisimFile(), "");
        filter();
        query.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        KeyAdapter keys = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    setVisible(false);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int i = Math.min(model.size() - 1, list.getSelectedIndex() + 1);
                    if (i >= 0) list.setSelectedIndex(i);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    int i = Math.max(0, list.getSelectedIndex() - 1);
                    if (model.size() > 0) list.setSelectedIndex(i);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    activate();
                    e.consume();
                }
            }
        };
        query.addKeyListener(keys);
        list.addKeyListener(keys);
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) activate();
            }
        });
    }

    public void showPalette() {
        if (!isDisplayable()) return;
        query.setText("");
        filter();
        setLocationRelativeTo(getOwner());
        setVisible(true);
        SwingUtilities.invokeLater(new Runnable() { public void run() { query.requestFocusInWindow(); } });
    }

    private void collect(Library lib, String prefix) {
        String libName = lib.getDisplayName();
        String path = prefix.length() == 0 ? libName : prefix + " / " + libName;
        for (Tool tool : lib.getTools()) all.add(new Item(tool, path));
        for (Library child : lib.getLibraries()) collect(child, path);
    }

    private void filter() {
        String q = query.getText().trim().toLowerCase(Locale.ROOT);
        String[] parts = q.length() == 0 ? new String[0] : q.split("\\s+");
        model.clear();
        for (Item item : all) {
            boolean ok = true;
            for (String part : parts) if (!item.search.contains(part)) { ok = false; break; }
            if (ok) model.addElement(item);
            if (model.size() >= 80) break;
        }
        if (!model.isEmpty()) list.setSelectedIndex(0);
    }

    private void activate() {
        Item item = list.getSelectedValue();
        if (item == null) return;
        Tool tool = item.tool.cloneTool();
        project.setTool(tool);
        setVisible(false);
        project.getFrame().getCanvas().requestFocusInWindow();
    }

    private static String aliasesFor(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.contains("xor")) return "исключающее или xor";
        if (n.contains("xnor")) return "эквивалентность xnor";
        if (n.equals("and")) return "и and конъюнкция";
        if (n.equals("or")) return "или or дизъюнкция";
        if (n.equals("not")) return "не not инвертор";
        if (n.contains("counter")) return "счетчик счётчик counter";
        if (n.contains("register")) return "регистр register";
        if (n.contains("decoder")) return "декодер decoder";
        if (n.contains("encoder")) return "кодер encoder";
        if (n.contains("multiplexer")) return "мультиплексор mux";
        if (n.contains("demultiplexer")) return "демультиплексор demux";
        if (n.contains("splitter")) return "разветвитель splitter";
        if (n.contains("clock")) return "тактовый генератор clock";
        if (n.contains("pin")) return "контакт вход выход pin";
        return "";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
