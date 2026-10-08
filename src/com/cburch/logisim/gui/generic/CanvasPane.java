/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.cburch.logisim.util.MacCompatibility;

public class CanvasPane extends JScrollPane {
    private class Listener implements ComponentListener, PropertyChangeListener {
        public void componentResized(ComponentEvent e) { contents.recomputeSize(); }
        public void componentMoved(ComponentEvent e) { }
        public void componentShown(ComponentEvent e) { }
        public void componentHidden(ComponentEvent e) { }

        public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            if (prop.equals(ZoomModel.ZOOM)) {
                double oldZoom = ((Double) e.getOldValue()).doubleValue();
                Rectangle r = getViewport().getViewRect();
                double cx = (r.x + r.width / 2) / oldZoom;
                double cy = (r.y + r.height / 2) / oldZoom;

                double newZoom = ((Double) e.getNewValue()).doubleValue();
                contents.recomputeSize();
                r = getViewport().getViewRect();
                int hv = (int) (cx * newZoom) - r.width / 2;
                int vv = (int) (cy * newZoom) - r.height / 2;
                getHorizontalScrollBar().setValue(hv);
                getVerticalScrollBar().setValue(vv);
            }
        }
    }

    private CanvasPaneContents contents;
    private Listener listener;
    private ZoomModel zoomModel;

    public CanvasPane(CanvasPaneContents contents) {
        super((Component) contents);
        this.contents = contents;
        this.listener = new Listener();
        this.zoomModel = null;
        if (MacCompatibility.mrjVersion >= 0.0) {
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        }

        addComponentListener(listener);

        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown() && zoomModel != null) {
                    stepZoom(e.getWheelRotation() < 0 ? 1 : -1);
                    e.consume();
                }
            }
        });

        // Keep keyboard zoom shortcuts available regardless of selected tool.
        int shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, shortcutMask), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, shortcutMask), "zoomIn");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, shortcutMask), "zoomOut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, shortcutMask), "zoomOut");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, shortcutMask), "zoomReset");
        am.put("zoomIn", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { stepZoom(1); }
        });
        am.put("zoomOut", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { stepZoom(-1); }
        });
        am.put("zoomReset", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (zoomModel != null) zoomModel.setZoomFactor(1.0);
            }
        });

        contents.setCanvasPane(this);
    }

    private void stepZoom(int direction) {
        if (zoomModel == null) return;
        double zoom = zoomModel.getZoomFactor() * 100.0;
        double[] opts = zoomModel.getZoomOptions();
        if (direction > 0) {
            for (int i = 0; i < opts.length; i++) {
                if (opts[i] > zoom * 1.001) {
                    zoomModel.setZoomFactor(opts[i] / 100.0);
                    return;
                }
            }
        } else {
            for (int i = opts.length - 1; i >= 0; i--) {
                if (opts[i] < zoom * 0.999) {
                    zoomModel.setZoomFactor(opts[i] / 100.0);
                    return;
                }
            }
        }
    }

    public void setZoomModel(ZoomModel model) {
        ZoomModel oldModel = zoomModel;
        if (oldModel != null) oldModel.removePropertyChangeListener(ZoomModel.ZOOM, listener);
        zoomModel = model;
        if (model != null) model.addPropertyChangeListener(ZoomModel.ZOOM, listener);
    }

    public double getZoomFactor() {
        ZoomModel model = zoomModel;
        return model == null ? 1.0 : model.getZoomFactor();
    }

    public Dimension getViewportSize() {
        Dimension size = new Dimension();
        getViewport().getSize(size);
        return size;
    }

    public int supportScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        int unit = supportScrollableUnitIncrement(visibleRect, orientation, direction);
        if (direction == SwingConstants.VERTICAL) return visibleRect.height / unit * unit;
        else return visibleRect.width / unit * unit;
    }

    public int supportScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        double zoom = getZoomFactor();
        return (int) Math.round(10 * zoom);
    }

    public Dimension supportPreferredSize(int width, int height) {
        double zoom = getZoomFactor();
        if (zoom != 1.0) {
            width = (int) Math.ceil(width * zoom);
            height = (int) Math.ceil(height * zoom);
        }
        Dimension minSize = getViewportSize();
        if (minSize.width > width) width = minSize.width;
        if (minSize.height > height) height = minSize.height;

        // Give middle-button panning room even for a tiny circuit.
        // This changes only the scrollable workspace, never circuit coordinates or saved files.
        int roomyWidth = Math.max(2400, minSize.width * 3);
        int roomyHeight = Math.max(1800, minSize.height * 3);
        if (roomyWidth > width) width = roomyWidth;
        if (roomyHeight > height) height = roomyHeight;
        return new Dimension(width, height);
    }
}
