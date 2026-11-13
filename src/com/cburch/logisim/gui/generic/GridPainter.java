/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.DarkTheme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class GridPainter {
	public static final String ZOOM_PROPERTY = "zoom";
	public static final String SHOW_GRID_PROPERTY = "showgrid";
	
	private class Listener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			String prop = event.getPropertyName();
			Object val = event.getNewValue();
			if (prop.equals(ZoomModel.ZOOM)) {
				setZoomFactor(((Double) val).doubleValue());
				destination.repaint();
			} else if (prop.equals(ZoomModel.SHOW_GRID)) {
				setShowGrid(((Boolean) val).booleanValue());
				destination.repaint();
			}
		}
	}

	private Component destination;
	private PropertyChangeSupport support;
	private Listener listener;
	private ZoomModel zoomModel;
	private boolean showGrid;
	private int gridSize;
	private double zoomFactor;
	private BufferedImage gridTile;
	private int tileSpacing;
	private Color tileColor;
	
	public GridPainter(Component destination) {
		this.destination = destination;
		support = new PropertyChangeSupport(this);
		showGrid = true;
		gridSize = 10;
		zoomFactor = 1.0;
	}
	
	public void addPropertyChangeListener(String prop,
			PropertyChangeListener listener) {
		support.addPropertyChangeListener(prop, listener);
	}
	
	public void removePropertyChangeListener(String prop,
			PropertyChangeListener listener) {
		support.removePropertyChangeListener(prop, listener);
	}
	
	public boolean getShowGrid() {
		return showGrid;
	}
	
	public void setShowGrid(boolean value) {
		if (showGrid != value) {
			showGrid = value;
			support.firePropertyChange(SHOW_GRID_PROPERTY, !value, value);
		}
	}
	
	public double getZoomFactor() {
		return zoomFactor;
	}
	
	public void setZoomFactor(double value) {
		double oldValue = zoomFactor;
		if (oldValue != value) {
			zoomFactor = value;
			support.firePropertyChange(ZOOM_PROPERTY, Double.valueOf(oldValue),
					Double.valueOf(value));
		}
	}
	
	public ZoomModel getZoomModel() {
		return zoomModel;
	}
	
	public void setZoomModel(ZoomModel model) {
		ZoomModel old = zoomModel;
		if (model != old) {
			if (listener == null) {
				listener = new Listener();
			}
			if (old != null) {
				old.removePropertyChangeListener(ZoomModel.ZOOM, listener);
				old.removePropertyChangeListener(ZoomModel.SHOW_GRID, listener);
			}
			zoomModel = model;
			if (model != null) {
				model.addPropertyChangeListener(ZoomModel.ZOOM, listener);
				model.addPropertyChangeListener(ZoomModel.SHOW_GRID, listener);
				setShowGrid(model.getShowGrid());
				setZoomFactor(model.getZoomFactor());
			}
			destination.repaint();
		}
	}

	public void paintGrid(Graphics g) {
		Rectangle clip = g.getClipBounds();
		double zoom = zoomFactor;
		int size = gridSize;

		if (!showGrid || clip == null) return;

		double spacing = size * zoom;
		// A repeating tile preserves dot positions for integral spacing and pixel-aligned output.
		if (spacing >= 1 && spacing <= 64 && spacing == (int) spacing
				&& clip.x >= 0 && clip.y >= 0 && isPixelAligned(g)) {
			paintTiledGrid(g, clip, (int) spacing);
		} else {
			paintGridDots(g, size, zoom, clip);
		}
	}

	private static boolean isPixelAligned(Graphics g) {
		if (!(g instanceof Graphics2D)) return false;
		AffineTransform transform = ((Graphics2D) g).getTransform();
		return (transform.getType() & ~(AffineTransform.TYPE_TRANSLATION
				| AffineTransform.TYPE_UNIFORM_SCALE | AffineTransform.TYPE_GENERAL_SCALE)) == 0
				&& transform.getScaleX() == Math.rint(transform.getScaleX())
				&& transform.getScaleY() == Math.rint(transform.getScaleY())
				&& transform.getTranslateX() == Math.rint(transform.getTranslateX())
				&& transform.getTranslateY() == Math.rint(transform.getTranslateY());
	}

	private void paintTiledGrid(Graphics g, Rectangle clip, int spacing) {
		Color color = DarkTheme.GRID;
		if (gridTile == null || tileSpacing != spacing || !color.equals(tileColor)) {
			int side = Math.max(1, 192 / spacing) * spacing;
			gridTile = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < side; y += spacing) {
				for (int x = 0; x < side; x += spacing) gridTile.setRGB(x, y, color.getRGB());
			}
			tileSpacing = spacing;
			tileColor = color;
		}
		int side = gridTile.getWidth();
		int right = clip.x + clip.width;
		int bottom = clip.y + clip.height;
		for (int y = clip.y / side * side; y < bottom; y += side) {
			for (int x = clip.x / side * side; x < right; x += side) {
				g.drawImage(gridTile, x, y, null);
			}
		}
		g.setColor(color);
	}

	private void paintGridDots(Graphics g, int size, double f, Rectangle clip) {
		g.setColor(DarkTheme.GRID);
		if (f == 1.0) {
			int start_x = ((clip.x + 9) / size) * size;
			int start_y = ((clip.y + 9) / size) * size;
			for (int x = 0; x < clip.width; x += size) {
				for (int y = 0; y < clip.height; y += size) {
					g.fillRect(start_x + x, start_y + y, 1, 1);
				}
			}
		} else {
			/* Kevin Walsh of Cornell suggested the code below instead. */
			int x0 = size * (int) Math.ceil(clip.x / f / size);
			int x1 = x0 + (int) (clip.width / f);
			int y0 = size * (int) Math.ceil(clip.y / f / size);
			int y1 = y0 + (int) (clip.height / f);
			for (double x = x0; x < x1; x += size) {
				for (double y = y0; y < y1; y += size) {
					int sx = (int) Math.round(f * x);
					int sy = (int) Math.round(f * y);
					g.fillRect(sx, sy, 1, 1);
				}
			}
			if (f <= 0.5) {
				int size5 = 5 * size;
				x0 = size5 * (int) Math.ceil(clip.x / f / size5);
				y0 = size5 * (int) Math.ceil(clip.y / f / size5);
				for (double x = x0; x < x1; x += size5) {
					for (double y = y0; y < y1; y += size5) {
						int sx = (int) Math.round(f * x);
						int sy = (int) Math.round(f * y);
						g.fillRect(sx, sy, 1, 1);
					}
				}
			}               
			
		}
	}

}
