/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.AbstractSpinnerModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;

import com.cburch.logisim.DarkTheme;

public class ZoomControl extends JPanel {
	private class SpinnerModel extends AbstractSpinnerModel
			implements PropertyChangeListener {
		public Object getNextValue() {
			double zoom = model.getZoomFactor();
			double[] choices = model.getZoomOptions();
			double factor = zoom * 100.0 * 1.001;
			for (int i = 0; i < choices.length; i++) {
				if (choices[i] > factor) return toString(choices[i]);
			}
			return null;
		}

		public Object getPreviousValue() {
			double zoom = model.getZoomFactor();
			double[] choices = model.getZoomOptions();
			double factor = zoom * 100.0 * 0.999;
			for (int i = choices.length - 1; i >= 0; i--) {
				if (choices[i] < factor) return toString(choices[i]);
			}
			return null;
		}

		public Object getValue() {
			double zoom = model.getZoomFactor();
			return toString(zoom * 100.0);
		}
		
		private String toString(double factor) {
			if (factor > 10) {
				return (int) (factor + 0.5) + "%";
			} else if (factor > 0.1) {
				return (int) (factor * 100 + 0.5) / 100.0 + "%";
			} else {
				return factor + "%";
			}
		}

		public void setValue(Object value) {
			if (value instanceof String) {
				String s = (String) value;
				if (s.endsWith("%")) s = s.substring(0, s.length() - 1);
				s = s.trim();
				try {
					double zoom = Double.parseDouble(s) / 100.0;
					model.setZoomFactor(zoom);
				} catch (NumberFormatException e) { }
			}
		}

		public void propertyChange(PropertyChangeEvent evt) {
			fireStateChanged();
		}
	}
	
	private class GridIcon extends JComponent
			implements MouseListener, PropertyChangeListener {
		boolean state = true;
		boolean hovered = false;
		
		public GridIcon() {
			addMouseListener(this);
			int size = DarkTheme.scale(28);
			setPreferredSize(new Dimension(size, size));
			setMinimumSize(new Dimension(size, size));
			setToolTipText("");
			setFocusable(true);
			getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("SPACE"), "toggleGrid");
			getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "toggleGrid");
			getActionMap().put("toggleGrid", new AbstractAction() {
				public void actionPerformed(ActionEvent e) { toggle(); }
			});
		}
		
		@Override
		public String getToolTipText(MouseEvent e) {
			return Strings.get("zoomShowGrid");
		}

		private void update() {
			boolean grid = model.getShowGrid();
			if (grid != state) {
				state = grid;
				repaint();
			}
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			if (!(g instanceof Graphics2D)) return;
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int inset = DarkTheme.scale(2);
			int arc = DarkTheme.scale(7);
			if (hovered || isFocusOwner()) {
				g2.setColor(hovered ? DarkTheme.HOVER : DarkTheme.SELECTION);
				g2.fillRoundRect(inset, inset, getWidth() - inset * 2, getHeight() - inset * 2, arc, arc);
			}

			int cell = Math.max(3, DarkTheme.scale(4));
			int dot = Math.max(2, DarkTheme.scale(2));
			int gridSize = cell * 3;
			int x0 = (getWidth() - gridSize) / 2;
			int y0 = (getHeight() - gridSize) / 2;
			g2.setColor(state ? DarkTheme.ACCENT : DarkTheme.FOREGROUND_MUTED);
			for (int x = 0; x < 4; x++) {
				for (int y = 0; y < 4; y++) {
					g2.fillOval(x0 + x * cell - dot / 2, y0 + y * cell - dot / 2, dot, dot);
				}
			}
			if (isFocusOwner()) {
				g2.setColor(DarkTheme.ACCENT);
				g2.setStroke(new BasicStroke(Math.max(1.0f, DarkTheme.getUiScale())));
				g2.drawRoundRect(inset, inset, getWidth() - inset * 2 - 1,
						getHeight() - inset * 2 - 1, arc, arc);
			}
			g2.dispose();
		}

		public void mouseClicked(MouseEvent e) { }
		public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
		public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
		public void mouseReleased(MouseEvent e) { }

		public void mousePressed(MouseEvent e) {
			requestFocusInWindow();
			toggle();
		}

		private void toggle() {
			if (model != null) model.setShowGrid(!state);
		}

		public void propertyChange(PropertyChangeEvent evt) {
			update();
		}
	}
	
	private ZoomModel model;
	private JSpinner spinner;
	private SpinnerModel spinnerModel;
	private GridIcon grid;
	
	public ZoomControl(ZoomModel model) {
		super(new BorderLayout());
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(DarkTheme.scale(4), DarkTheme.scale(6),
				DarkTheme.scale(4), DarkTheme.scale(6)));
		this.model = model;
		
		spinnerModel = new SpinnerModel();
		spinner = new JSpinner();
		spinner.setModel(spinnerModel);
		this.add(spinner, BorderLayout.CENTER);
		
		grid = new GridIcon();
		this.add(grid, BorderLayout.EAST);
		grid.update();
		
		model.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
		model.addPropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
	}
	
	public void setZoomModel(ZoomModel value) {
		ZoomModel oldModel = model;
		if (oldModel != value) {
			if (oldModel != null) {
				oldModel.removePropertyChangeListener(ZoomModel.SHOW_GRID, grid);
				oldModel.removePropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
			}
			model = value;
			spinnerModel = new SpinnerModel();
			spinner.setModel(spinnerModel);
			grid.update();
			if (value != null) {
				value.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
				value.addPropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
			}
		}
	}
}
