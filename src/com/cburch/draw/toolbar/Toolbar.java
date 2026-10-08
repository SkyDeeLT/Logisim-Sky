/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.awt.BorderLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.BorderFactory;

import com.cburch.logisim.DarkTheme;

public class Toolbar extends JPanel {
	public static final Object VERTICAL = new Object();
	public static final Object HORIZONTAL = new Object();
	
	private class MyListener implements ToolbarModelListener {
		public void toolbarAppearanceChanged(ToolbarModelEvent event) {
			repaint();
		}

		public void toolbarContentsChanged(ToolbarModelEvent event) {
			computeContents();
		}
	}

	private ToolbarModel model;
	private JPanel subpanel;
	private Object orientation;
	private MyListener myListener;
	private ToolbarButton curPressed;

	public Toolbar(ToolbarModel model) {
		super(new BorderLayout());
		this.subpanel = new JPanel();
		this.subpanel.setOpaque(false);
		setOpaque(true);
		setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(2), DarkTheme.scale(4),
				DarkTheme.scale(2), DarkTheme.scale(4)));
		this.model = model;
		this.orientation = HORIZONTAL;
		this.myListener = new MyListener();
		this.curPressed = null;

		JPanel filler = new JPanel();
		filler.setOpaque(false);
		this.add(filler, BorderLayout.CENTER);
		setOrientation(HORIZONTAL);
		
		computeContents();
		if (model != null) model.addToolbarModelListener(myListener);
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			java.awt.Color top = DarkTheme.TOOLBAR_TOP;
			java.awt.Color bottom = DarkTheme.TOOLBAR_BOTTOM;
			g2.setPaint(new GradientPaint(0, 0, top, 0, Math.max(1, getHeight()), bottom));
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.setColor(DarkTheme.BORDER_SOFT);
			if (orientation == HORIZONTAL) {
				g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
			} else {
				g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
			}
			g2.dispose();
		} else {
			super.paintComponent(g);
		}
	}

	public ToolbarModel getToolbarModel() {
		return model;
	}
	
	public void setToolbarModel(ToolbarModel value) {
		ToolbarModel oldValue = model;
		if (value != oldValue) {
			if (oldValue != null) oldValue.removeToolbarModelListener(myListener);
			if (value != null) value.addToolbarModelListener(myListener);
			model = value;
			computeContents();
		}
	}
	
	public void setOrientation(Object value) {
		int axis;
		String position;
		if (value == HORIZONTAL) {
			axis = BoxLayout.X_AXIS;
			position = BorderLayout.LINE_START;
		} else if (value == VERTICAL) {
			axis = BoxLayout.Y_AXIS;
			position = BorderLayout.NORTH;
		} else {
			throw new IllegalArgumentException();
		}
		this.remove(subpanel);
		subpanel.setLayout(new BoxLayout(subpanel, axis));
		this.add(subpanel, position);
		this.orientation = value;
	}
	
	private void computeContents() {
		subpanel.removeAll();
		ToolbarModel m = model;
		if (m != null) {
			for (ToolbarItem item : m.getItems()) {
				subpanel.add(new ToolbarButton(this, item));
				subpanel.add(Box.createRigidArea(new java.awt.Dimension(DarkTheme.scale(1), DarkTheme.scale(1))));
			}
			subpanel.add(Box.createGlue());
		}
		revalidate();
	}
	
	ToolbarButton getPressed() {
		return curPressed;
	}
	
	void setPressed(ToolbarButton value) {
		ToolbarButton oldValue = curPressed;
		if (oldValue != value) {
			curPressed = value;
			if (oldValue != null) oldValue.repaint();
			if (value != null) value.repaint();		
		}
	}
	
	Object getOrientation() {
		return orientation;
	}
}
