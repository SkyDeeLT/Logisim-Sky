/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import javax.swing.JMenu;

abstract class Menu extends JMenu {
	@Override
	public void setText(String text) {
		super.setText(text);
		if (text != null && text.length() > 0) setMnemonic(text.charAt(0));
	}

	abstract void computeEnabled();
}
