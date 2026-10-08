/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import com.cburch.logisim.DarkTheme;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class ToolbarSeparator implements ToolbarItem {
	private int size;
	
	public ToolbarSeparator(int size) {
		this.size = size;
	}
	
	public boolean isSelectable() {
		return false;
	}
	
	public void paintIcon(Component destination, Graphics g) {
		Dimension dim = destination.getSize();
		g.setColor(DarkTheme.BORDER_SOFT);
		int x = 0;
		int y = 0;
		int w = dim.width;
		int h = dim.height;
		if (h >= w) { // separator is a vertical line in horizontal toolbar
			h -= DarkTheme.scale(8);
			y = DarkTheme.scale(2);
			x = (w - 2) / 2;
			w = Math.max(1, DarkTheme.scale(1));
		} else { // separator is a horizontal line in vertical toolbar
			w -= DarkTheme.scale(8);
			x = DarkTheme.scale(2);
			y = (h - 2) / 2;
			h = Math.max(1, DarkTheme.scale(1));
		}
		g.fillRect(x, y, w, h);
	}
	
	public String getToolTip() {
		return null;
	}
	
	public Dimension getDimension(Object orientation) {
		return new Dimension(DarkTheme.scale(size), DarkTheme.scale(size));
	}
}
