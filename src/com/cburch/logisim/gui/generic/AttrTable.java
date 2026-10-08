/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.table.JTableHeader;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.cburch.logisim.DarkTheme;
import com.cburch.logisim.util.JDialogOk;
import com.cburch.logisim.util.JInputComponent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.LinkedList;

public class AttrTable extends JPanel implements LocaleListener {
	private static final AttrTableModel NULL_ATTR_MODEL = new NullAttrModel();
	
	private static class NullAttrModel implements AttrTableModel {
		public void addAttrTableModelListener(AttrTableModelListener listener) { }
		public void removeAttrTableModelListener(AttrTableModelListener listener) { }
		
		public String getTitle() { return null; }
		public int getRowCount() { return 0; }
		public AttrTableModelRow getRow(int rowIndex) { return null; }
	}
	
	private static class TitleLabel extends JLabel {
		TitleLabel() {
			setOpaque(false);
			setForeground(DarkTheme.FOREGROUND);
			setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(5), DarkTheme.scale(6),
					DarkTheme.scale(5), DarkTheme.scale(6)));
		}

		@Override
		protected void paintComponent(Graphics g) {
			// Theme can be switched while this label is already alive.
			// Keep its foreground synchronized instead of retaining the old theme color.
			setForeground(DarkTheme.FOREGROUND);
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2.setPaint(new GradientPaint(0, 0, DarkTheme.PANEL_BACKGROUND_ALT,
						0, Math.max(1, getHeight()), DarkTheme.PANEL_BACKGROUND));
				g2.fillRect(0, 0, getWidth(), getHeight());
				g2.setColor(DarkTheme.BORDER_SOFT);
				g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
				g2.dispose();
			}
			super.paintComponent(g);
		}

		@Override
		public Dimension getMinimumSize() {
			Dimension ret = super.getMinimumSize();
			return new Dimension(1, ret.height);
		}
	}

	/** Viewport that always paints from the live Sky UI palette. */
	private static class ThemeViewport extends JViewport {
		ThemeViewport() { setOpaque(true); }
		@Override
		protected void paintComponent(Graphics g) {
			setBackground(DarkTheme.INPUT_BACKGROUND);
			g.setColor(DarkTheme.INPUT_BACKGROUND);
			g.fillRect(0, 0, getWidth(), getHeight());
			super.paintComponent(g);
		}
	}

	/** JTable can retain a Metal background across live LAF changes; repair it at paint time. */
	private static class ThemeTable extends JTable {
		ThemeTable(TableModel model) { super(model); setFillsViewportHeight(true); }
		@Override
		protected void paintComponent(Graphics g) {
			setBackground(DarkTheme.INPUT_BACKGROUND);
			setForeground(DarkTheme.FOREGROUND);
			setGridColor(DarkTheme.BORDER);
			setSelectionBackground(DarkTheme.SELECTION);
			setSelectionForeground(DarkTheme.FOREGROUND);
			super.paintComponent(g);
		}
	}

	private static class MyDialog extends JDialogOk {
		JInputComponent input;
		Object value;

		public MyDialog(Dialog parent, JInputComponent input) {
			super(parent, Strings.get("attributeDialogTitle"), true);
			configure(input);
		}

		public MyDialog(Frame parent, JInputComponent input) {
			super(parent, Strings.get("attributeDialogTitle"), true);
			configure(input);
		}

		private void configure(JInputComponent input) {
			this.input = input;
			this.value = input.getValue();
			
			// Thanks to Christophe Jacquet, who contributed a fix to this
			// so that when the dialog is resized, the component within it
			// is resized as well. (Tracker #2024479)
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			p.add((JComponent) input, BorderLayout.CENTER);
			getContentPane().add(p, BorderLayout.CENTER);

			pack();
		}

		@Override
		public void okClicked() {
			value = input.getValue();
		}

		public Object getValue() {
			return value;
		}
	}

	private class TableModelAdapter
			implements TableModel, AttrTableModelListener {
		Window parent;
		LinkedList<TableModelListener> listeners;
		AttrTableModel attrModel;
		
		TableModelAdapter(Window parent, AttrTableModel attrModel) {
			this.parent = parent;
			this.listeners = new LinkedList<TableModelListener>();
			this.attrModel = attrModel;
		}
		
		void setAttrTableModel(AttrTableModel value) {
			if (attrModel != value) {
				TableCellEditor editor = table.getCellEditor();
				if (editor != null) editor.cancelCellEditing();
				attrModel.removeAttrTableModelListener(this);
				attrModel = value;
				attrModel.addAttrTableModelListener(this);
				fireTableChanged();
			}
		}

		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		public void removeTableModelListener(TableModelListener l) {
			listeners.remove(l);
		}

		void fireTableChanged() {
			TableModelEvent e = new TableModelEvent(this);
			for (TableModelListener l : new ArrayList<TableModelListener>(listeners)) {
				l.tableChanged(e);
			}
		}

		public int getColumnCount() {
			return 2;
		}

		public String getColumnName(int columnIndex) {
			java.util.Locale loc = LocaleManager.getLocale();
			boolean ru = loc != null && "ru".equalsIgnoreCase(loc.getLanguage());
			if (columnIndex == 0) return ru ? "Свойство" : "Property";
			else                  return ru ? "Значение" : "Value";
		}

		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		public int getRowCount() {
			return attrModel.getRowCount();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				return attrModel.getRow(rowIndex).getLabel();
			} else {
				return attrModel.getRow(rowIndex).getValue();
			}
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex > 0 && attrModel.getRow(rowIndex).isValueEditable();
		}

		public void setValueAt(Object value, int rowIndex,
				int columnIndex) {
			if (columnIndex > 0) {
				try {
					attrModel.getRow(rowIndex).setValue(value);
				} catch (AttrTableSetException e) {
					JOptionPane.showMessageDialog(parent, e.getMessage(),
							Strings.get("attributeChangeInvalidTitle"),
							JOptionPane.WARNING_MESSAGE);
				}
			}
		}
		
		//
		// AttrTableModelListener methods
		//
		public void attrTitleChanged(AttrTableModelEvent e) {
			if (e.getSource() != attrModel) {
				attrModel.removeAttrTableModelListener(this);
				return;
			}
			updateTitle();
		}
		
		public void attrStructureChanged(AttrTableModelEvent e) {
			if (e.getSource() != attrModel) {
				attrModel.removeAttrTableModelListener(this);
				return;
			}
			fireTableChanged();
		}
		
		public void attrValueChanged(AttrTableModelEvent e) {
			if (e.getSource() != attrModel) {
				attrModel.removeAttrTableModelListener(this);
				return;
			}
			fireTableChanged();
		}
	}

	private class CellEditor
			implements TableCellEditor, FocusListener, ActionListener {
		LinkedList<CellEditorListener> listeners = new LinkedList<CellEditorListener>();
		AttrTableModelRow currentRow;
		Component currentEditor;

		//
		// TableCellListener management
		//
		public void addCellEditorListener(CellEditorListener l) {
			// Adds a listener to the list that's notified when the
			// editor stops, or cancels editing. 
			listeners.add(l);
		}

		public void removeCellEditorListener(CellEditorListener l) {
			// Removes a listener from the list that's notified 
			listeners.remove(l);
		}

		public void fireEditingCanceled() {
			ChangeEvent e = new ChangeEvent(AttrTable.this);
			for (CellEditorListener l : new ArrayList<CellEditorListener>(listeners)) {
				l.editingCanceled(e);
			}
		}

		public void fireEditingStopped() {
			ChangeEvent e = new ChangeEvent(AttrTable.this);
			for (CellEditorListener l : new ArrayList<CellEditorListener>(listeners)) {
				l.editingStopped(e);
			}
		}

		//
		// other TableCellEditor methods
		//
		public void cancelCellEditing() {
			// Tells the editor to cancel editing and not accept any
			// partially edited value. 
			fireEditingCanceled();
		}

		public boolean stopCellEditing() {
			// Tells the editor to stop editing and accept any partially
			// edited value as the value of the editor. 
			fireEditingStopped();
			return true;
		}

		public Object getCellEditorValue() {
			// Returns the value contained in the editor. 
			Component comp = currentEditor;
			if (comp instanceof JTextField) {
				return ((JTextField) comp).getText();
			} else if (comp instanceof JComboBox) {
				return ((JComboBox) comp).getSelectedItem();
			} else {
				return null;
			}
		}

		public boolean isCellEditable(EventObject anEvent) {
			// Asks the editor if it can start editing using anEvent. 
			return true;
		}

		public boolean shouldSelectCell(EventObject anEvent) {
			// Returns true if the editing cell should be selected,
			// false otherwise. 
			return true;
		}

		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int rowIndex, int columnIndex) {
			AttrTableModel attrModel = tableModel.attrModel;
			int modelRow = table.convertRowIndexToModel(rowIndex);
			AttrTableModelRow row = attrModel.getRow(modelRow);
			
			if (columnIndex == 0) {
				return new JLabel(row.getLabel());
			} else {
				if (currentEditor != null) currentEditor.transferFocus();

				Component editor = row.getEditor(parent);
				if (editor instanceof JComboBox) {
					((JComboBox) editor).addActionListener(this);
					editor.addFocusListener(this);
				} else if (editor instanceof JInputComponent) {
					JInputComponent input = (JInputComponent) editor;
					MyDialog dlog;
					Window parent = AttrTable.this.parent;
					if (parent instanceof Frame) {
						dlog = new MyDialog((Frame) parent, input);
					} else {
						dlog = new MyDialog((Dialog) parent, input);
					}
					dlog.setVisible(true);
					Object retval = dlog.getValue();
					try {
						row.setValue(retval);
					} catch (AttrTableSetException e) {
						JOptionPane.showMessageDialog(parent, e.getMessage(),
								Strings.get("attributeChangeInvalidTitle"),
								JOptionPane.WARNING_MESSAGE);
					}
					editor = new JLabel(row.getValue());
				} else {
					editor.addFocusListener(this);
				}
				currentRow = row;
				currentEditor = editor;
				return editor;
			}
		}

		//
		// FocusListener methods
		//
		public void focusLost(FocusEvent e) {
			Object dst = e.getOppositeComponent();
			if (dst instanceof Component) {
				Component p = (Component) dst;
				while (p != null && !(p instanceof Window)) {
					if (p == AttrTable.this) {
						// switch to another place in this table,
						// no problem
						return;
					}
					p = p.getParent();
				}
				// focus transferred outside table; stop editing
				editor.stopCellEditing();
			}
		}

		public void focusGained(FocusEvent e) { }

		//
		// ActionListener methods
		//
		public void actionPerformed(ActionEvent e) {
			stopCellEditing();
		}

	}

	private Window parent;
	private boolean titleEnabled;
	private JLabel title;
	private JTextField searchField;
	private JPanel headerPanel;
	private JTable table;
	private JScrollPane tableScroll;
	private TableModelAdapter tableModel;
	private TableRowSorter<TableModelAdapter> sorter;
	private CellEditor editor = new CellEditor();
	private int themeRevisionSeen = -1;

	public AttrTable(Window parent) {
		super(new BorderLayout());
		this.parent = parent;
		
		titleEnabled = true;
		title = new TitleLabel();
		title.setHorizontalAlignment(SwingConstants.LEFT);
		title.setVerticalAlignment(SwingConstants.CENTER);
		searchField = new JTextField();
		searchField.putClientProperty("JTextField.placeholderText", "Поиск свойств…");
		searchField.setToolTipText("Поиск по названию или значению свойства");
		searchField.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(DarkTheme.BORDER_SOFT),
				BorderFactory.createEmptyBorder(DarkTheme.scale(4), DarkTheme.scale(8),
						DarkTheme.scale(4), DarkTheme.scale(8))));
		headerPanel = new JPanel(new BorderLayout(0, DarkTheme.scale(5)));
		headerPanel.setOpaque(false);
		headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, DarkTheme.scale(6), 0));
		headerPanel.add(title, BorderLayout.NORTH);
		headerPanel.add(searchField, BorderLayout.CENTER);

		tableModel = new TableModelAdapter(parent, NULL_ATTR_MODEL);
		table = new ThemeTable(tableModel);
		table.setDefaultEditor(Object.class, editor);
		table.setRowHeight(DarkTheme.scale(29));
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(true);
		table.setIntercellSpacing(new Dimension(0, 1));
		sorter = new TableRowSorter<TableModelAdapter>(tableModel);
		table.setRowSorter(sorter);
		JTableHeader th = table.getTableHeader();
		th.setReorderingAllowed(false);
		th.setPreferredSize(new Dimension(10, DarkTheme.scale(27)));

		Font baseFont = title.getFont();
		int titleSize = Math.max(baseFont.getSize(), DarkTheme.scale(13));
		Font titleFont = baseFont.deriveFont(Font.BOLD, (float) titleSize);
		title.setFont(titleFont);
		table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable t, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				JLabel c = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
				c.setBorder(BorderFactory.createEmptyBorder(0, DarkTheme.scale(8), 0, DarkTheme.scale(8)));
				if (isSelected) {
					c.setBackground(DarkTheme.SELECTION);
					c.setForeground(DarkTheme.FOREGROUND);
				} else {
					boolean even = (row & 1) == 0;
					c.setBackground(column == 0
							? (even ? DarkTheme.PANEL_BACKGROUND_ALT : DarkTheme.PANEL_BACKGROUND)
							: (even ? DarkTheme.INPUT_BACKGROUND : DarkTheme.PANEL_BACKGROUND));
					c.setForeground(column == 0 ? DarkTheme.FOREGROUND_MUTED : DarkTheme.FOREGROUND);
				}
				c.setFont(c.getFont().deriveFont(column == 0 ? Font.BOLD : Font.PLAIN));
				return c;
			}
		});

		searchField.getDocument().addDocumentListener(new DocumentListener() {
			private void updateFilter() {
				final String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
				if (q.length() == 0) {
					sorter.setRowFilter(null);
				} else {
					sorter.setRowFilter(new RowFilter<TableModelAdapter, Integer>() {
						@Override public boolean include(Entry<? extends TableModelAdapter, ? extends Integer> entry) {
							for (int i = 0; i < entry.getValueCount(); i++) {
								String v = entry.getStringValue(i);
								if (v != null && v.toLowerCase().contains(q)) return true;
							}
							return false;
						}
					});
				}
			}
			public void insertUpdate(DocumentEvent e) { updateFilter(); }
			public void removeUpdate(DocumentEvent e) { updateFilter(); }
			public void changedUpdate(DocumentEvent e) { updateFilter(); }
		});

		tableScroll = new JScrollPane();
		ThemeViewport themeViewport = new ThemeViewport();
		themeViewport.setView(table);
		tableScroll.setViewport(themeViewport);
		if (table.getColumnModel().getColumnCount() >= 2) {
			table.getColumnModel().getColumn(0).setPreferredWidth(DarkTheme.scale(150));
			table.getColumnModel().getColumn(1).setPreferredWidth(DarkTheme.scale(170));
		}
		syncThemeColors();
		
		this.add(headerPanel, BorderLayout.PAGE_START);
		this.add(tableScroll, BorderLayout.CENTER);
		LocaleManager.addLocaleListener(this);
		localeChanged();
	}
	
	private void syncThemeColors() {
		themeRevisionSeen = DarkTheme.getThemeRevision();
		setBackground(DarkTheme.PANEL_BACKGROUND);
		setForeground(DarkTheme.FOREGROUND);
		title.setForeground(DarkTheme.FOREGROUND);
		if (searchField != null) {
			searchField.setBackground(DarkTheme.INPUT_BACKGROUND);
			searchField.setForeground(DarkTheme.FOREGROUND);
			searchField.setCaretColor(DarkTheme.FOREGROUND);
		}
		table.setBackground(DarkTheme.INPUT_BACKGROUND);
		table.setForeground(DarkTheme.FOREGROUND);
		table.setGridColor(DarkTheme.BORDER);
		table.setSelectionBackground(DarkTheme.SELECTION);
		table.setSelectionForeground(DarkTheme.FOREGROUND);
		if (tableScroll != null) {
			tableScroll.setBackground(DarkTheme.PANEL_BACKGROUND);
			if (tableScroll.getViewport() != null) {
				tableScroll.getViewport().setOpaque(true);
				tableScroll.getViewport().setBackground(DarkTheme.INPUT_BACKGROUND);
			}
		}
		table.setFillsViewportHeight(true);
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (themeRevisionSeen != DarkTheme.getThemeRevision()) syncThemeColors();
		super.paintComponent(g);
	}

	public void setTitleEnabled(boolean value) {
		titleEnabled = value;
		updateTitle();
	}
	
	public boolean getTitleEnabled() {
		return titleEnabled;
	}
	
	public void setAttrTableModel(AttrTableModel value) {
		tableModel.setAttrTableModel(value == null ? NULL_ATTR_MODEL : value);
		updateTitle();
	}
	
	public AttrTableModel getAttrTableModel() {
		return tableModel.attrModel;
	}

	public void localeChanged() {
		updateTitle();
		tableModel.fireTableChanged();
	}
	
	private void updateTitle() {
		if (titleEnabled) {
			String text = tableModel.attrModel.getTitle();
			if (text == null) {
				title.setVisible(false);
			} else {
				title.setText(text);
				title.setVisible(true);
			}
		} else {
			title.setVisible(false);
		}
	}
}
