/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

class SimulatorTicker extends Thread {
	private Simulator.PropagationManager manager;
	private int ticksPerTickPhase;
	private int millisPerTickPhase;

	private boolean shouldTick;
	private boolean complete;
	
	public SimulatorTicker(Simulator.PropagationManager manager) {
		this.manager = manager;
		ticksPerTickPhase = 1;
		millisPerTickPhase = 1000;
		shouldTick = false;
		complete = false;
	}
	
	public synchronized void setTickFrequency(int millis, int ticks) {
		millisPerTickPhase = millis;
		ticksPerTickPhase = ticks;
	}

	synchronized void setAwake(boolean value) {
		shouldTick = value;
		if (shouldTick) notifyAll();
	}
	
	public synchronized void shutDown() {
		complete = true;
		notifyAll();
	}
	
	@Override
	public void run() {
		long lastTick = System.currentTimeMillis();
		while (true) {
			int millis;
			int ticks;
			synchronized (this) {
				while (!shouldTick && !complete) {
					try {
						wait();
					} catch (InterruptedException e) { }
				}
				if (complete) return;
				millis = millisPerTickPhase;
				ticks = ticksPerTickPhase;
			}

			long now = System.currentTimeMillis();
			if (now - lastTick >= millis) {
				lastTick = now;
				manager.requestAutomaticTicks(ticks);
			}

			try {
				long nextTick = lastTick + millis;
				int wait = (int) (nextTick - System.currentTimeMillis());
				if (wait < 1) wait = 1;
				if (wait > 100) wait = 100;
				Thread.sleep(wait);
			} catch (InterruptedException e) { }
		}
	}
}
