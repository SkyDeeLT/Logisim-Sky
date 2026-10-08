/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.cburch.logisim.DarkTheme;
import com.cburch.draw.toolbar.AbstractToolbarModel;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.draw.toolbar.ToolbarSeparator;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.ToolbarData;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.ModernIcons;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class LayoutToolbarModel extends AbstractToolbarModel {
	private class ToolItem implements ToolbarItem {
		private Tool tool;
		
		ToolItem(Tool tool) {
			this.tool = tool;
		}
		
		public boolean isSelectable() {
			return true;
		}
		
		public void paintIcon(Component destination, Graphics g) {
			int box = DarkTheme.scale(28);
			int icon = DarkTheme.scale(22);
			int off = (box - icon) / 2;

			if (tool == haloedTool && AppPreferences.ATTRIBUTE_HALO.getBoolean()) {
				g.setColor(DarkTheme.HALO);
				g.fillRoundRect(DarkTheme.scale(2), DarkTheme.scale(2),
					box - DarkTheme.scale(4), box - DarkTheme.scale(4),
					DarkTheme.scale(7), DarkTheme.scale(7));
			}

			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

			if (!ModernIcons.paintToolIcon(tool, g2, off, off, icon)) {
				// Fallback for less common third-party/built-in tools: keep the original
				// renderer, but center it inside the same stable square.
				float scale = icon / 20.0f;
				g2.translate(off, off);
				g2.scale(scale, scale);
				g2.setColor(DarkTheme.FOREGROUND);
				Graphics gCopy = g2.create();
				ComponentDrawContext c = new ComponentDrawContext(destination, null, null, g2, gCopy);
				tool.paintIcon(c, 0, 0);
				gCopy.dispose();
			}
			g2.dispose();
		}
		
		public String getToolTip() {
			String ret = tool.getDescription();
			int index = 1;
			for (ToolbarItem item : items) {
				if (item == this) break;
				if (item instanceof ToolItem) ++index;
			}
			if (index <= 10) {
				if (index == 10) index = 0;
				int mask = frame.getToolkit().getMenuShortcutKeyMask();
				ret += " (" + InputEventUtil.toKeyDisplayString(mask)
					+ "-" + index + ")";
			}
			return ret;
		}
		
		public Dimension getDimension(Object orientation) {
			return new Dimension(DarkTheme.scale(28), DarkTheme.scale(28));
		}
	}

	private class MyListener implements ProjectListener, AttributeListener,
				ToolbarData.ToolbarListener, PropertyChangeListener {
		//
		// ProjectListener methods
		//
		public void projectChanged(ProjectEvent e) {
			int act = e.getAction();
			if (act == ProjectEvent.ACTION_SET_TOOL) {
				fireToolbarAppearanceChanged();
			} else if (act == ProjectEvent.ACTION_SET_FILE) {
				LogisimFile old = (LogisimFile) e.getOldData();
				if (old != null) {
					ToolbarData data = old.getOptions().getToolbarData();
					data.removeToolbarListener(this);
					data.removeToolAttributeListener(this);
				}
				LogisimFile file = (LogisimFile) e.getData();
				if (file != null) {
					ToolbarData data = file.getOptions().getToolbarData();
					data.addToolbarListener(this);
					data.addToolAttributeListener(this);
				}
				buildContents();
			}
		}

		//
		// ToolbarListener methods
		//
		public void toolbarChanged() {
			buildContents();
		}
		
		//
		// AttributeListener methods
		//
		public void attributeListChanged(AttributeEvent e) { }
		public void attributeValueChanged(AttributeEvent e) {
			fireToolbarAppearanceChanged();
		}
		
		//
		// PropertyChangeListener method
		//
		public void propertyChange(PropertyChangeEvent event) {
			if (AppPreferences.GATE_SHAPE.isSource(event)) {
				fireToolbarAppearanceChanged();
			}
		}
	}
		
	private Frame frame;
	private Project proj;
	private MyListener myListener;
	private List<ToolbarItem> items;
	private Tool haloedTool;

	public LayoutToolbarModel(Frame frame, Project proj) {
		this.frame = frame;
		this.proj = proj;
		myListener = new MyListener();
		items = Collections.emptyList();
		haloedTool = null;
		buildContents();

		// set up listeners
		ToolbarData data = proj.getOptions().getToolbarData();
		data.addToolbarListener(myListener);
		data.addToolAttributeListener(myListener);
		AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
		proj.addProjectListener(myListener);
	}

	@Override
	public List<ToolbarItem> getItems() {
		return items;
	}
	
	@Override
	public boolean isSelected(ToolbarItem item) {
		if (item instanceof ToolItem) {
			Tool tool = ((ToolItem) item).tool;
			return tool == proj.getTool();
		} else {
			return false;
		}
	}

	@Override
	public void itemSelected(ToolbarItem item) {
		if (item instanceof ToolItem) {
			Tool tool = ((ToolItem) item).tool;
			proj.setTool(tool);
		}
	}

	public void setHaloedTool(Tool t) {
		if (haloedTool != t) {
			haloedTool = t;
			fireToolbarAppearanceChanged();
		}
	}
	
	private void buildContents() {
		List<ToolbarItem> oldItems = items;
		List<ToolbarItem> newItems = new ArrayList<ToolbarItem>();
		int pos = -1;
		ToolbarData data = proj.getLogisimFile().getOptions().getToolbarData();
		for (Tool tool : data.getContents()) {
			++pos;
			if (tool == null) {
				newItems.add(new ToolbarSeparator(4));
			} else {
				ToolbarItem i = findItem(oldItems, tool);
				if (i == null) {
					newItems.add(new ToolItem(tool));
				} else {
					newItems.add(i);
				}
			}
		}
		items = Collections.unmodifiableList(newItems);
		fireToolbarContentsChanged();
	}
	
	private static ToolbarItem findItem(List<ToolbarItem> items, Tool tool) {
		for (ToolbarItem item : items) {
			if (item instanceof ToolItem) {
				if (tool == ((ToolItem) item).tool) {
					return item;
				}
			}
		}
		return null;
	}
}
