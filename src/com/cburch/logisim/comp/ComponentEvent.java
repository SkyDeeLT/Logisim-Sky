/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.comp;

public class ComponentEvent {
	private Component source;
	private Object oldData;
	private Object newData;
	private final boolean simulationChange;

	public ComponentEvent(Component source) {
		this(source, false);
	}

	public ComponentEvent(Component source, boolean simulationChange) {
		this(source, null, null, simulationChange);
	}
	
	public ComponentEvent(Component source, Object oldData, Object newData) {
		this(source, oldData, newData, false);
	}

	private ComponentEvent(Component source, Object oldData, Object newData, boolean simulationChange) {
		this.source = source;
		this.oldData = oldData;
		this.newData = newData;
		this.simulationChange = simulationChange;
	}

	public boolean isSimulationChange() {
		return simulationChange;
	}

	public Component getSource() {
		return source;
	}

	public Object getData() {
		return newData;
	}

	public Object getOldData() {
		return oldData;
	}
}
