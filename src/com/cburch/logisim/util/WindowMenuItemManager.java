/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JRadioButtonMenuItem;

public abstract class WindowMenuItemManager {
	private class MyListener implements WindowListener {
		public void windowOpened(WindowEvent event) { }
		public void windowClosing(WindowEvent event) {
			JFrame frame = (JFrame) event.getWindow();
			if (frame.getDefaultCloseOperation() == JFrame.HIDE_ON_CLOSE) {
				WindowMenuManager.unsetCurrentManager(WindowMenuItemManager.this);
				removeFromManager();
			}
		}
		
		public void windowClosed(WindowEvent event) {
			frameClosed((JFrame) event.getWindow());
		}
		
		public void windowDeiconified(WindowEvent event) { }

		public void windowIconified(WindowEvent event) {
			if (!event.getWindow().isDisplayable()) return;
			addToManager();
			WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
		}

		public void windowActivated(WindowEvent event) {
			if (!event.getWindow().isDisplayable()) return;
			addToManager();
			WindowMenuManager.setCurrentManager(WindowMenuItemManager.this);
		}

		public void windowDeactivated(WindowEvent event) {
			WindowMenuManager.unsetCurrentManager(WindowMenuItemManager.this);
		}
	}
	
	private MyListener myListener = new MyListener();
	private String text;
	private boolean persistent;
	private boolean listenerAdded = false;
	private boolean inManager = false;
	private HashMap<WindowMenu,JRadioButtonMenuItem> menuItems
		= new HashMap<WindowMenu,JRadioButtonMenuItem>();
	
	public WindowMenuItemManager(String text, boolean persistent) {
		this.text = text;
		this.persistent = persistent;
		if (persistent) {
			WindowMenuManager.addManager(this);
		}
	}
	
	public abstract JFrame getJFrame(boolean create);
	
	public void frameOpened(JFrame frame) {
		if (!listenerAdded) {
			frame.addWindowListener(myListener);
			listenerAdded = true;
		}
		addToManager();
		WindowMenuManager.setCurrentManager(this);
	}

	public void frameClosed(JFrame frame) {
		WindowMenuManager.unsetCurrentManager(this);
		if (listenerAdded) {
			frame.removeWindowListener(myListener);
			listenerAdded = false;
		}
		if (!persistent) {
			removeFromManager();
		}
	}
	
	private void addToManager() {
		if (!persistent && !inManager) {
			WindowMenuManager.addManager(this);
			inManager = true;
		}
	}
	
	private void removeFromManager() {
		if (!persistent && inManager) {
			inManager = false;
			WindowMenuManager.removeManager(this);
		}
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String value) {
		text = value;
		for (JRadioButtonMenuItem menuItem : menuItems.values()) {
			menuItem.setText(text);
		}
	}
	
	JRadioButtonMenuItem getMenuItem(WindowMenu key) {
		return menuItems.get(key);
	}
	
	void createMenuItem(WindowMenu menu) {
		WindowMenuItem ret = new WindowMenuItem(this);
		menuItems.put(menu, ret);
		menu.addMenuItem(this, ret, persistent);
	}
	
	void removeMenuItem(WindowMenu menu) {
		JRadioButtonMenuItem item = menuItems.remove(menu);
		if (item != null) menu.removeMenuItem(this, item);
	}

	void setSelected(boolean selected) {
		for (JRadioButtonMenuItem item : menuItems.values()) {
			item.setSelected(selected);
		}
	}
}
