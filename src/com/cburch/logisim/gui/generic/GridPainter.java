/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.DarkTheme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

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
	private Image gridImage;
	private int gridImageWidth;
	private int themeRevisionSeen;
	
	public GridPainter(Component destination) {
		this.destination = destination;
		support = new PropertyChangeSupport(this);
		showGrid = true;
		gridSize = 10;
		zoomFactor = 1.0;
		themeRevisionSeen = DarkTheme.getThemeRevision();
		updateGridImage(gridSize, zoomFactor);
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
			updateGridImage(gridSize, value);
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
			}
			setShowGrid(model.getShowGrid());
			setZoomFactor(model.getZoomFactor());
			destination.repaint();
		}
	}

	public void paintGrid(Graphics g) {
		Rectangle clip = g.getClipBounds();
		double zoom = zoomFactor;
		int size = gridSize;

		if (!showGrid || clip == null) return;

		/*
		 * Sky UI deliberately paints grid dots directly instead of using the old
		 * cached MemoryImageSource tile. On several Java/Swing pipelines that
		 * image cache flattens transparent pixels against the canvas colour that
		 * existed when the tile was created. After a live theme switch the cached
		 * tile can therefore repaint the entire workspace with the previous theme
		 * (most visibly: Light UI + Midnight canvas). Direct dots are cheap for a
		 * viewport-sized grid and keep the background genuinely transparent.
		 */
		themeRevisionSeen = DarkTheme.getThemeRevision();
		paintGridOld(g, size, zoom, clip);
	}
	
	private void paintGridOld(Graphics g, int size, double f, Rectangle clip) {
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
			if (f <= 0.5) g.setColor(DarkTheme.GRID);
			for (double x = x0; x < x1; x += size) {
				for (double y = y0; y < y1; y += size) {
					int sx = (int) Math.round(f * x);
					int sy = (int) Math.round(f * y);
					g.fillRect(sx, sy, 1, 1);
				}
			}
			if (f <= 0.5) { // make every 5th pixel darker
				int size5 = 5 * size;
				g.setColor(DarkTheme.GRID);
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
			
			/* Original code by Carl Burch
			int x0 = 10 * (int) Math.ceil(clip.x / f / 10);
			int x1 = x0 + (int)(clip.width / f);
			int y0 = 10 * (int) Math.ceil(clip.y / f / 10);
			int y1 = y0 + (int) (clip.height / f);
			int s = f > 0.5 ? 1 : f > 0.25 ? 2 : 3;
			int i0 = s - ((x0 + 10*s - 1) % (s * 10)) / 10 - 1;
			int j0 = s - ((y1 + 10*s - 1) % (s * 10)) / 10 - 1;
			for (int i = 0; i < s; i++) {
				for (int x = x0+i*10; x < x1; x += s*10) {
					for (int j = 0; j < s; j++) {
						g.setColor(i == i0 && j == j0 ? Color.gray : GRID_ZOOMED_OUT_COLOR);
						for (int y = y0+j*10; y < y1; y += s*10) {
							int sx = (int) Math.round(f * x);
							int sy = (int) Math.round(f * y);
							g.fillRect(sx, sy, 1, 1);
						}
					}
				}
			}
			*/
		}
	}

	//
	// creating the grid image
	//
	private void updateGridImage(int size, double f) {
		double ww = f * size * 5;
		while (2 * ww < 150) ww *= 2;
		int w = (int) Math.round(ww);
		int[] pix = new int[w * w];
		Arrays.fill(pix, 0x00000000);

		if (f == 1.0) {
			int lineStep = size * w;
			for (int j = 0; j < pix.length; j += lineStep) {
				for (int i = 0; i < w; i += size) {
					pix[i + j] = DarkTheme.GRID.getRGB();
				}
			}
		} else {
			int off0 = 0;
			int off1 = 1;
			if (f >= 2.0) { // we'll draw several pixels for each grid point
				int num = (int) (f + 0.001);
				off0 = -(num / 2);
				off1 = off0 + num; 
			}
			
			int dotColor = f <= 0.5 ? DarkTheme.GRID_STRONG.getRGB() : DarkTheme.GRID.getRGB();
			for (int j = 0; true; j += size) {
				int y = (int) Math.round(f * j);
				if (y + off0 >= w) break;
				
				for (int yo = y + off0; yo < y + off1; yo++) {
					if (yo >= 0 && yo < w) {
						int base = yo * w;
						for (int i = 0; true; i += size) {
							int x = (int) Math.round(f * i);
							if (x + off0 >= w) break;
							for (int xo = x + off0; xo < x + off1; xo++) {
								if (xo >= 0 && xo < w) {
									pix[base + xo] = dotColor;
								}
							}
						}
					}
				}
			}
			if (f <= 0.5) { // repaint over every 5th pixel so it is darker
				int size5 = size * 5;
				for (int j = 0; true; j += size5) {
					int y = (int) Math.round(f * j);
					if (y >= w) break;
					y *= w;
					
					for (int i = 0; true; i += size5) {
						int x = (int) Math.round(f * i);
						if (x >= w) break;
						pix[y + x] = DarkTheme.GRID.getRGB();
					}
				}
			}
		}
		gridImage = destination.createImage(new MemoryImageSource(w, w, pix, 0, w));
		gridImageWidth = w;
	}
}
