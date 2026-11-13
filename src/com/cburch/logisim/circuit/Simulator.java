/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import java.util.concurrent.CopyOnWriteArrayList;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.prefs.AppPreferences;

public class Simulator {
	/*begin DEBUGGING
	private static PrintWriter debug_log;
	
	static {
		try {
			debug_log = new PrintWriter(new BufferedWriter(new FileWriter("DEBUG")));
		} catch (IOException e) {
			System.err.println("Could not open debug log"); //OK
		}
	}
	
	public static void log(String msg) {
		debug_log.println(msg);
	}
	
	public static void flushLog() {
		debug_log.flush();
	}
	//end DEBUGGING*/
	
	class PropagationManager extends Thread {
		private volatile Propagator propagator = null;
		private PropagationPoints stepPoints = new PropagationPoints();
		private int ticksRequested = 0;
		private int automaticTicksRequested = 0;
		private int stepsRequested = 0;
		private boolean resetRequested = false;
		private boolean propagateRequested = false;
		private boolean complete = false;

		public Propagator getPropagator() {
			return propagator;
		}
		
		public void setPropagator(Propagator value) {
			propagator = value;
		}
		
		public synchronized void requestPropagate() {
			if (!propagateRequested) {
				propagateRequested = true;
				notifyAll();
			}
		}
		
		public synchronized void requestReset() {
			if (!resetRequested) {
				resetRequested = true;
				notifyAll();
			}
		}
		
		public synchronized void requestTick() {
			if (complete) return;
			ticksRequested++;
			notifyAll();
		}

		public synchronized void requestAutomaticTicks(int count) {
			if (isRunning && isTicking && !complete) {
				automaticTicksRequested = Math.min(16, automaticTicksRequested + count);
				notifyAll();
			}
		}

		public synchronized void cancelAutomaticTicks() {
			automaticTicksRequested = 0;
		}
		
		public synchronized void shutDown() {
			complete = true;
			notifyAll();
		}
		
		@Override
		public void run() {
			while (true) {
				boolean reset;
				boolean tick;
				boolean step;
				boolean propagate;
				boolean running;
				Propagator current;
				synchronized (this) {
					while (!complete && !propagateRequested
							&& !resetRequested && ticksRequested == 0 && automaticTicksRequested == 0
							&& stepsRequested == 0) {
						try {
							wait();
						} catch (InterruptedException e) { }
					}
					if (complete) return;

					current = propagator;
					running = isRunning;
					reset = resetRequested;
					tick = ticksRequested > 0 || (running && isTicking && automaticTicksRequested > 0);
					step = !running && stepsRequested > 0;
					propagate = propagateRequested;
					resetRequested = false;
					propagateRequested = false;
					if (ticksRequested > 0) ticksRequested--;
					else if (tick) automaticTicksRequested--;
					else automaticTicksRequested = 0;
					if (running) stepsRequested = 0;
					else if (step) stepsRequested--;
				}

				if (reset) {
					if (current != null) current.reset();
					firePropagationCompleted();
				}
				if (!tick && !step && !propagate && !(reset && running)) continue;

				if (current != null) {
					try {
						exceptionEncountered = false;
						if (tick) current.tick();
						if (running) {
							stepPoints.clear();
							do {
								current.propagate();
							} while (consumePropagationRequest(current));
							if (propagator == current && current.isOscillating()) {
								setIsRunning(false);
								synchronized (this) {
									ticksRequested = 0;
									propagateRequested = false;
								}
							}
						} else if (step) {
							stepPoints.clear();
							current.step(stepPoints);
						}
					} catch (Throwable thr) {
						thr.printStackTrace();
						exceptionEncountered = true;
						if (running) setIsRunning(false);
					}
					if (tick) fireTickCompleted();
				}
				firePropagationCompleted();
			}
		}

		private synchronized boolean consumePropagationRequest(Propagator current) {
			if (complete || !isRunning || propagator != current
					|| current.isOscillating() || !propagateRequested) return false;
			propagateRequested = false;
			return true;
		}
	}

	private volatile boolean isRunning = true;
	private volatile boolean isTicking = false;
	private volatile boolean exceptionEncountered = false;
	private double tickFrequency = 1.0;

	private PropagationManager manager;
	private SimulatorTicker ticker;
	private final CopyOnWriteArrayList<SimulatorListener> listeners
		= new CopyOnWriteArrayList<SimulatorListener>();
	private final SimulatorEvent event = new SimulatorEvent(this);

	public Simulator() {
		manager = new PropagationManager();
		ticker = new SimulatorTicker(manager);
		try {
			manager.setPriority(manager.getPriority() - 1);
			ticker.setPriority(ticker.getPriority() - 1);
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) { }
		manager.start();
		ticker.start();
		
		tickFrequency = 0.0;
		setTickFrequency(AppPreferences.TICK_FREQUENCY.get().doubleValue());
	}
	
	public void shutDown() {
		ticker.shutDown();
		manager.shutDown();
	}

	public void setCircuitState(CircuitState state) {
		manager.setPropagator(state.getPropagator());
		renewTickerAwake();
	}
	
	public CircuitState getCircuitState() {
		Propagator prop = manager.getPropagator();
		return prop == null ? null : prop.getRootState();
	}
	
	public void requestReset() {
		manager.requestReset();
	}
	
	public void tick() {
		manager.requestTick();
	}
	
	public void step() {
		synchronized(manager) {
			manager.stepsRequested++;
			manager.notifyAll();
		}
	}
	
	public void drawStepPoints(ComponentDrawContext context) {
		manager.stepPoints.draw(context);
	}
	
	public boolean isExceptionEncountered() {
		return exceptionEncountered;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setIsRunning(boolean value) {
		if (isRunning != value) {
			isRunning = value;
			renewTickerAwake();
			if (value) manager.requestPropagate();
			/*DEBUGGING - comment out: 
			if (!value) flushLog(); //*/
			fireSimulatorStateChanged();
		}
	}

	public boolean isTicking() {
		return isTicking;
	}

	public void setIsTicking(boolean value) {
		if (isTicking != value) {
			isTicking = value;
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}
	
	private void renewTickerAwake() {
		boolean awake = isRunning && isTicking && tickFrequency > 0;
		ticker.setAwake(awake);
		if (!awake) manager.cancelAutomaticTicks();
	}

	public double getTickFrequency() {
		return tickFrequency;
	}

	public void setTickFrequency(double freq) {
		if (tickFrequency != freq) {
			int millis = (int) Math.round(1000 / freq);
			int ticks;
			if (millis > 0) {
				ticks = 1;
			} else {
				millis = 1;
				ticks = (int) Math.round(freq / 1000);
			}
			
			tickFrequency = freq;
			ticker.setTickFrequency(millis, ticks);
			renewTickerAwake();
			fireSimulatorStateChanged();
		}
	}

	public void requestPropagate() {
		manager.requestPropagate();
	}

	public boolean isOscillating() {
		Propagator prop = manager.getPropagator();
		return prop != null && prop.isOscillating();
	}

	public void addSimulatorListener(SimulatorListener l) { listeners.add(l); }
	public void removeSimulatorListener(SimulatorListener l) { listeners.remove(l); }
	void firePropagationCompleted() {
		for (SimulatorListener l : listeners) {
			l.propagationCompleted(event);
		}
	}
	void fireTickCompleted() {
		for (SimulatorListener l : listeners) {
			l.tickCompleted(event);
		}
	}
	void fireSimulatorStateChanged() {
		for (SimulatorListener l : listeners) {
			l.simulatorStateChanged(event);
		}
	}
}
