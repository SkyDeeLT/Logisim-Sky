/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;

public class Options {
	public static final AttributeOption GATE_UNDEFINED_IGNORE
	= new AttributeOption("ignore", Strings.getter("gateUndefinedIgnore"));
	public static final AttributeOption GATE_UNDEFINED_ERROR
		= new AttributeOption("error", Strings.getter("gateUndefinedError"));
		
	public static final Attribute<Integer> sim_limit_attr
		= Attributes.forInteger("simlimit", Strings.getter("simLimitOption"));
	public static final Attribute<Integer> sim_rand_attr
		= Attributes.forInteger("simrand", Strings.getter("simRandomOption"));
	public static final Attribute<AttributeOption> ATTR_GATE_UNDEFINED
		= Attributes.forOption("gateUndefined", Strings.getter("gateUndefinedOption"),
				new AttributeOption[] { GATE_UNDEFINED_IGNORE, GATE_UNDEFINED_ERROR });
	
	public static final Integer sim_rand_dflt = Integer.valueOf(32);

	private static final Attribute<?>[] ATTRIBUTES = {
			ATTR_GATE_UNDEFINED, sim_limit_attr, sim_rand_attr,
	};
	private static final Object[] DEFAULTS = {
			GATE_UNDEFINED_IGNORE, Integer.valueOf(1000), Integer.valueOf(0),
	};
	
	private AttributeSet attrs;
	private MouseMappings mmappings;
	private ToolbarData toolbar;
	private volatile int simulationRandomShift;

	public Options() {
		attrs = AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
		attrs.addAttributeListener(new AttributeListener() {
			public void attributeListChanged(AttributeEvent event) { }

			public void attributeValueChanged(AttributeEvent event) {
				if (event.getAttribute() == sim_rand_attr) {
					int randomness = (Integer) event.getValue();
					int shift = 32 - Integer.numberOfLeadingZeros(Math.max(1, randomness) - 1);
					simulationRandomShift = Math.min(30, shift);
				}
			}
		});
		mmappings = new MouseMappings();
		toolbar = new ToolbarData();
	}

	public AttributeSet getAttributeSet() {
		return attrs;
	}

	public int getSimulationRandomShift() {
		return simulationRandomShift;
	}

	public MouseMappings getMouseMappings() {
		return mmappings;
	}

	public ToolbarData getToolbarData() {
		return toolbar;
	}

	public void copyFrom(Options other, LogisimFile dest) {
		AttributeSets.copy(other.attrs, this.attrs);
		this.toolbar.copyFrom(other.toolbar, dest);
		this.mmappings.copyFrom(other.mmappings, dest);
	}
}
