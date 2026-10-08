/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

import com.cburch.logisim.DarkTheme;
import com.cburch.draw.toolbar.ToolbarItem;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;

class LogisimToolbarItem implements ToolbarItem {
	private MenuListener menu;
	private Icon icon;
	private LogisimMenuItem action;
	private StringGetter toolTip;
	
	public LogisimToolbarItem(MenuListener menu, String iconName,
			LogisimMenuItem action, StringGetter toolTip) {
		this.menu = menu;
		this.icon = Icons.getIcon(iconName);
		this.action = action;
		this.toolTip = toolTip;
	}
	
	public void setIcon(String iconName) {
		this.icon = Icons.getIcon(iconName);
	}
	
	public void setToolTip(StringGetter toolTip) {
		this.toolTip = toolTip;
	}
	
	public void doAction() {
		if (menu != null && menu.isEnabled(action)) {
			menu.doAction(action);
		}
	}
	
	public boolean isSelectable() {
		return menu != null && menu.isEnabled(action);
	}
	
	public void paintIcon(Component destination, Graphics g) {
		Graphics2D g2 = g instanceof Graphics2D ? (Graphics2D) g.create() : null;
		Graphics use = g2 == null ? g : g2;
		if (!isSelectable() && g2 != null) {
			Composite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.32f);
			g2.setComposite(c);
		}

		int box = DarkTheme.scale(28);
		if (icon == null) {
			int s = DarkTheme.scale(10);
			int o = (box - s) / 2;
			use.setColor(new Color(255, 128, 128));
			use.drawRect(o, o, s, s);
			use.drawLine(o, o, o + s, o + s);
			use.drawLine(o, o + s, o + s, o);
		} else {
			int x = (box - icon.getIconWidth()) / 2;
			int y = (box - icon.getIconHeight()) / 2;
			icon.paintIcon(destination, use, x, y);
		}
		if (g2 != null) g2.dispose();
	}
	
	public String getToolTip() {
		if (toolTip != null) {
			return toolTip.get();
		} else {
			return null;
		}
	}
	
	public Dimension getDimension(Object orientation) {
		int box = DarkTheme.scale(28);
		return new Dimension(box, box);
	}
}
