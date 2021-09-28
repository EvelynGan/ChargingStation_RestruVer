/**
 * @author Ho Wai Fung
 * @date 10/3/2017
 *
 */
package cornerstone;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

import victorho.device.ADCChannel;
import victorho.device.PowerMeter;
import victorho.platform.adapter.GPIO;
import victorho.platform.adapter.PWMPort;
import victorho.util.Logger;
import victorho.util.LockStateListener;
import victorho.util.ValueChangeEvent;
import victorho.util.ValueChangeListener;

public class IECCharger extends Charger {
	public static final int CABLE = 1;
	public static final int SOCKET = 2;

	public static final int LOCKVER_OLD = 1;
    public static final int LOCKVER_HBRIDGE_SOLEN = 2;
    public static final int LOCKVER_HBRIDGE_AMPHENOL= 3;
    public static final int CP_IDLE_TYPE1 = 1;		
    public static final int CP_IDLE_TYPE2 = 2;
    private static final int LOCK_DELAY = 2000;
    private static final int UNLOCK_DELAY = 2000;

    private static final int LOCK_CTRL_SOLEN_DELAY = 25;
    private static final int LOCK_CTRL_MOTOR_DELAY = 100;
    private static final int IEC_ALLOWED_DEALAY = 5000;
    
    /**
     * Constructor for objects of class IECCharger
     */
	private EventListenerList listenerList;
    
    private PWMPort pwmCP;
    private GPIO enableLock;
    private GPIO enableLock2;		// for new lock hardware from V2.5 
    private GPIO isLocked;
    private int lockVer = LOCKVER_OLD;
    private boolean isEnable;
    private boolean isPilotEnable;
    private boolean isCableConnect;
    private boolean lockState;
    private long lockEventTime;
    private ADCChannel proximity;
    private ADCChannel pilot;
    private int codingResistance;
    private int type;
    private int cpVoltage;
    private int cpMinus;
    
    private float currentRating;
    private int currentAllowed;
    private int currentLimit;
    private ValueChangeListener proximityListener;
    private ValueChangeListener maxListener;
    private ValueChangeListener minListener;
    private long offTime[];
    private long onTime[];
    private float lastCurrentRating;
    private float lastHigherCurrentRating;
    private long currentRatingUpdateTime;
    private long lastCurrentRatingUpdateTime;
    private long lastHigherCurrentRatingUpdateTime;
    private boolean isCurrentLimitSetAfterChargingSectionStart = false;
    private int cpIdleType = CP_IDLE_TYPE1;
    private int ppSerialResistor = 0;
    private long ppLastValueSerial;
    private long ppValueSerial;  
	private static HashMap<Long, Double> currentLimitHistory = new HashMap<>();
	
    public IECCharger(PowerMeter meter, int type, PWMPort pwm, final ADCChannel pilot, ADCChannel proximity, GPIO enableLock, GPIO isLocked, int maxCurrent) {
    	super(meter);
    	
    	listenerList = new EventListenerList();
    	
    	state = INITIALIZE;
    	onTime = new long[5];
    	offTime = new long[5];
    	for(int i = 0; i < onTime.length; ++i) {
    		onTime[i] = offTime[i] = System.currentTimeMillis();
    	}
    	
    	this.type = type;
    	if(type == SOCKET) {
    		currentAllowed = 6;
    	} else {
    		currentAllowed = maxCurrent;
    	}
    	currentLimit = maxCurrent;
    	isEnable = false;
    	isPilotEnable = false;
    	
    	pwmCP = pwm;
    	pwmCP.setFrequency(1000);
    	pwmCP.setDuty(0);
    	
    	this.enableLock = enableLock;
    	this.enableLock.setState(GPIO.LOW);
    	try {
    		Thread.sleep(250);
    	} catch (InterruptedException e) {
    		
    	}
    	this.isLocked = isLocked;
    	lockState = isLocked();
    	lockEventTime = System.currentTimeMillis();
    	if(type == SOCKET) {
	    	isLocked.addActionListener(new ActionListener() {
	
				@Override
				public void actionPerformed(ActionEvent evt) {
					if(System.currentTimeMillis() - lockEventTime > 250) {
						lockEventTime = System.currentTimeMillis();
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
						}
						fireLockEvent();
						lockState = isLocked();
					}
				}
	    		
	    	});
    	}
    	this.proximity = proximity;
    	this.pilot = pilot;
   		proximity.setWindowSize(20);		
    	pilot.setWindowSize(250);
    	isCableConnect = false;
    	
    	proximityListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				int newRating = 6;
				
				if(evt.getValue() <= 3000) {
					if(codingResistance > 3000 || state == INITIALIZE) {
						isCableConnect = true;
//						System.out.println("CS1");
						checkState();
//				    	pilot.addMaxValueChangeListener(maxListener, 500);
//				    	pilot.addMinValueChangeListener(minListener, 500);
					}
			    	
				} else {
					if(codingResistance <= 3000 || state == INITIALIZE) {
//				    	pilot.removeValueChangeListener(maxListener);
//				    	pilot.removeValueChangeListener(minListener);
						isCableConnect = false;
//						System.out.println("CS2");
						checkState();
					}
				}
				codingResistance = evt.getValue();
				
				if(codingResistance < 110) {
					newRating = 63; 
				} else if(codingResistance < 242) {
					newRating = 32;
				} else if(codingResistance < 748) {
					newRating = 20;
				} else if(codingResistance < 1650) {
					newRating = 13;
				}
				
				if(newRating > currentLimit) {
					newRating = currentLimit;
				}
				
				if(newRating == currentAllowed) {
					return;
				}
				currentAllowed = newRating;

				Logger.writeln("PP " + codingResistance + ", Current = " + currentAllowed);
//				System.out.println("PP " + codingResistance + " " + currentAllowed + "");
			}
    		
    	};
    	
    	maxListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				cpVoltage = evt.getValue();
//				System.out.println("CP+ " + cpVoltage);
				Logger.writeln(" CP+ " + cpVoltage);
				checkState();
			}
    		
    	};
    	minListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				cpMinus = evt.getValue();
				Logger.writeln(" CP- " + cpMinus);
				checkState();
			}
    		
    	};
    	if(type == SOCKET) {
			this.proximity.addMinValueChangeListener(proximityListener, 10);
		}
    	this.pilot.addMaxValueChangeListener(maxListener, 500);
	    this.pilot.addMinValueChangeListener(minListener, 500);
    }

    public IECCharger(PowerMeter meter, int type, PWMPort pwm, final ADCChannel pilot, ADCChannel proximity, GPIO enableLock, GPIO enableLock2, GPIO isLocked, int lockVerIn, int maxCurrent, int cpIdleTypeIn, int ppSerialRes) {
    	
    	super(meter);
    	
    	this.lockVer = lockVerIn;
    	this.cpIdleType = cpIdleTypeIn;
    	this.ppSerialResistor = ppSerialRes;
    	this.ppLastValueSerial = 0;
    	this.ppValueSerial = 0;
    	listenerList = new EventListenerList();
    	this.currentLimitHistory = new HashMap<>();
    	
    	state = INITIALIZE;
    	onTime = new long[5];
    	offTime = new long[5];
    	for(int i = 0; i < onTime.length; ++i) {
    		onTime[i] = offTime[i] = System.currentTimeMillis();
    	}
    	
    	this.type = type;
    	if(type == SOCKET) {
    		currentAllowed = 6;
    	} else {
    		currentAllowed = maxCurrent;
    	}
    	currentLimit = maxCurrent;

    	lastCurrentRating = currentAllowed;
    	lastCurrentRatingUpdateTime = System.currentTimeMillis();   	
    	lastHigherCurrentRating = currentAllowed;
    	lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();  
    	
    	isEnable = false;
    	isPilotEnable = false;
    	
    	pwmCP = pwm;
    	pwmCP.setFrequency(1000);
    	pwmCP.setDuty(0);
    	
//    	this.enableLock = enableLock;
//    	this.enableLock.setState(GPIO.LOW);
//    	this.enableLock2 = enableLock2;
//    	this.enableLock2.setState(GPIO.HIGH);
//    	
//    	try {
//    		Thread.sleep(25);
//    	} catch (InterruptedException e) {
//    		
//    	}
//    	this.enableLock.setState(GPIO.HIGH);
//    	try {
//    		Thread.sleep(25);
//    	} catch (InterruptedException e) {
//    		
//    	}   	
//    	this.enableLock.setState(GPIO.LOW);
//    	this.enableLock2.setState(GPIO.LOW);  
    	this.enableLock = enableLock;
    	this.enableLock2 = enableLock2;    	
    	unlockControlAction();
    	
    	try {
    		Thread.sleep(200);
    	} catch (InterruptedException e) {
    		
    	}    	
    	this.isLocked = isLocked;
    	lockState = isLocked();
    	lockEventTime = System.currentTimeMillis();
    	if(type == SOCKET) {
	    	isLocked.addActionListener(new ActionListener() {
	
				@Override
				public void actionPerformed(ActionEvent evt) {
					if(System.currentTimeMillis() - lockEventTime > 250) {
						lockEventTime = System.currentTimeMillis();
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
						}
						fireLockEvent();
						lockState = isLocked();
					}
				}
	    		
	    	});
    	}
    	this.proximity = proximity;
    	this.pilot = pilot;
   		proximity.setWindowSize(40);
    	pilot.setWindowSize(250);
    	isCableConnect = false;
    	
    	proximityListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				int newRating = 6;
				boolean rollOver = false;
				int measuredRes = 0;
				
				ppLastValueSerial = ppValueSerial;
				ppValueSerial = evt.getSerial();
				
				if(ppValueSerial==Long.MAX_VALUE) {
					rollOver = true;		//suppose rollover will not happen practical in charger's life time!
				}
				
				
				if(ppValueSerial>ppLastValueSerial) {
					
					
					if(evt.getValue() <= 3000) {
						if(codingResistance > 3000 || state == INITIALIZE) {
							isCableConnect = true;
//							System.out.println("CS1");
							checkState();
//					    	pilot.addMaxValueChangeListener(maxListener, 500);
//					    	pilot.addMinValueChangeListener(minListener, 500);
						}
				    	
					} else {
						if(codingResistance <= 3000 || state == INITIALIZE) {
//					    	pilot.removeValueChangeListener(maxListener);
//					    	pilot.removeValueChangeListener(minListener);
							isCableConnect = false;
//							System.out.println("CS2");
							checkState();
						}
					}
					codingResistance = evt.getValue();
					
					if( (codingResistance-ppSerialResistor) < 110) {
						newRating = 63; 
					} else if((codingResistance-ppSerialResistor) < 242) {
						newRating = 32;
					} else if((codingResistance-ppSerialResistor) < 748) {
						newRating = 20;
					} else if((codingResistance-ppSerialResistor) < 1650) {
						newRating = 13;
					}
					
					if(rollOver) {
						newRating = 13;		//play safe only, suppose roll over will never happen practically in charger's life time!
						Logger.writeln("PP sample rollover, set to 13A");
					}
					
					if(newRating > currentLimit) {
						newRating = currentLimit;
					}
					
					if(newRating == currentAllowed) {
						return;
					}
					currentAllowed = newRating;

					Logger.writeln("PP " + codingResistance + ", Current = " + currentAllowed+" at time "+System.currentTimeMillis()+ " serial "+ppValueSerial);
					System.out.println("PP " + codingResistance + " " + currentAllowed + "");					
				}else {
					//Logger.writeln("pp value with earlier serial, value ignored "+ evt.getValue());
					//System.out.println("pp value with earlier serial, value ignored "+ evt.getValue());
				}

			}
    		
    	};
    	
    	maxListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				cpVoltage = evt.getValue();
//				System.out.println("CP+ " + cpVoltage);
				Logger.writeln(" CP+ " + cpVoltage);
				checkState();
			}
    		
    	};
    	minListener = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				cpMinus = evt.getValue();
				Logger.writeln(" CP- " + cpMinus);
				checkState();
			}
    		
    	};
    	if(type == SOCKET) {
			this.proximity.addMinValueChangeListener(proximityListener, 10);
			//this.proximity.addMinValueChangeListener(proximityListener, 5);
		}
    	this.pilot.addMaxValueChangeListener(maxListener, 500);
	    this.pilot.addMinValueChangeListener(minListener, 500);
    }   
    
    public void addLockListener(LockStateListener l) {
    	listenerList.add(LockStateListener.class, l);    	
    }
    
    public void removeLockListener(LockStateListener l) {
    	listenerList.remove(LockStateListener.class, l);
    }


    private void checkState() {
    	Logger.writeln("IEC state " + state + " " + cpVoltage + " " + Math.abs(cpVoltage - 12000));
		if(Math.abs(cpMinus - 0) <= 1000 || (cpMinus < 0 && Math.abs(cpMinus + 12000) > 2000)) {
			if(state != STATE_E) {
				if(isCableConnect) {
					setState(STATE_EExt);
				} else {
					setState(STATE_E);
				}
			}
		}

		System.out.println("CP voltage detected: "+cpVoltage);
		//if(Math.abs(cpVoltage - 3000) <= 1100) {
		if( ((cpVoltage - 3000) <= 1000) && ((3000 - cpVoltage) <= 200) ){
			Logger.writeln("IEC 1");
			if(state != STATE_C) {
				setState(STATE_C);
			}
		//} else if(Math.abs(cpVoltage - 6000) <= 1000) {
		} else if(Math.abs(cpVoltage +200 - 6000) <= 1250) {			
			Logger.writeln("IEC 2");
			if(state != STATE_C) {
				setState(STATE_C);
//			} else {
//				ClearLog.writeln("Skip set STATE_C");
			}
		//} else if(Math.abs(cpVoltage - 9000) <= 1000) {
		} else if(Math.abs(cpVoltage +200 - 9000) <= 1250) {		
			Logger.writeln("IEC 3");
			if(state != STATE_B) {
				setState(STATE_B);
//			} else {
//				ClearLog.writeln("Skip set STATE_B");
			}
		//} else if(Math.abs(cpVoltage - 12000) <= 1000) {
		} else if(Math.abs(cpVoltage - 12000) <= 1250) {	
			Logger.writeln("IEC 4");
			if(isCableConnect) {
				if(state != STATE_CABLE_CONNECT) {
					setState(STATE_CABLE_CONNECT);
				}
			} else {
				if(state != STATE_CABLE_DISCONNECT) {
					setState(STATE_CABLE_DISCONNECT);
				}
			}
		} else if(cpVoltage < 0) {
			Logger.writeln("IEC 5");
			if(state != STATE_N) {
				setState(STATE_N);
			}
		}
    }

      
	private void fireLockEvent() {
		Object[] listeners = listenerList.getListenerList();

		if(isLocked()) {
			Logger.writeln("LOCK");
			if(lockState == false) {
				for(int i = 0; 2 * i < listeners.length; ++i) {
					((LockStateListener)listeners[2* i + 1]).locked(new ChangeEvent(this));
				}
			}
		} else {
			Logger.writeln("UNLOCK");
			if(lockState == true) {
				for(int i = 0; 2 * i < listeners.length; ++i) {
					((LockStateListener)listeners[2* i + 1]).unlocked(new ChangeEvent(this));
				}
			}
		}
	}

	public int getType() {
		return type;
	}
	
    public int getState() {
        return state;
    }
		    
    public boolean isCablePlugged() {
    	return ((codingResistance < 3000 || (cpVoltage > 0 && cpVoltage < 11000)) && type == SOCKET);
    }

	public boolean isLocked() {
		return isLocked.isLow();
	}

	public boolean isStart() {
    	return pwmCP.getDuty() > 5;
    }
		
	public void lockControlAction() {
		int delay = LOCK_CTRL_SOLEN_DELAY;
		enableLock.setState(GPIO.HIGH);
		enableLock2.setState(GPIO.LOW);
		if(lockVer==LOCKVER_HBRIDGE_AMPHENOL) {
			delay = LOCK_CTRL_MOTOR_DELAY;
		}
		try {
			Thread.sleep(delay);
		}catch (InterruptedException e) {
		}
		enableLock.setState(GPIO.HIGH);
		enableLock2.setState(GPIO.HIGH);
	}
	
	public void unlockControlAction() {
		int delay = LOCK_CTRL_SOLEN_DELAY;
		enableLock.setState(GPIO.LOW);
		enableLock2.setState(GPIO.HIGH);
		if(lockVer==LOCKVER_HBRIDGE_AMPHENOL) {
			delay = LOCK_CTRL_MOTOR_DELAY;
		}
		try {
			Thread.sleep(delay);
		}catch (InterruptedException e) {
		}
		enableLock.setState(GPIO.HIGH);
		enableLock2.setState(GPIO.HIGH);		
	}	
	
    @Override
    public synchronized void setCableLock(boolean isLock) {
 	
    	if(lockVer==LOCKVER_OLD) {

    	final Object[] listeners = listenerList.getListenerList();
		Logger.writeln("Cable " + (isLock ? "Locking" : "Unlocking"));
		if(isLock && !isLocked()) {
    		new Thread(new Runnable() {

				@Override
				public void run() {
		    		try {
		    			if(enableLock.isHigh()) {
				    		enableLock.setState(GPIO.LOW);
				    		Thread.sleep(UNLOCK_DELAY);
		    			}
			    		enableLock.setState(GPIO.HIGH);
			    		Thread.sleep(LOCK_DELAY);
			    		if(!isLocked()) {
							for(int i = 0; 2 * i < listeners.length; ++i) {
								((LockStateListener)listeners[2* i + 1]).lockFailed(new ChangeEvent(this));
							}
			    		}
					} catch (InterruptedException e) {
					}
				}
    		}).start();
    	} else if(!isLock && isLocked()){
    		new Thread(new Runnable() {

				@Override
				public void run() {
		    		try {
		    			if(enableLock.isLow()) {
		    				enableLock.setState(GPIO.HIGH);
		    				Thread.sleep(LOCK_DELAY);
		    			}
			    		enableLock.setState(GPIO.LOW);
			    		Thread.sleep(UNLOCK_DELAY);
			    		if(isLocked()) {
							for(int i = 0; 2 * i < listeners.length; ++i) {
								((LockStateListener)listeners[2* i + 1]).unlockFailed(new ChangeEvent(this));
							}
			    		}
			    		while(isLocked()) {
				    		enableLock.setState(GPIO.HIGH);
							Thread.sleep(LOCK_DELAY);
				    		enableLock.setState(GPIO.LOW);
				    		Thread.sleep(UNLOCK_DELAY);
			    		}
					} catch (InterruptedException e) {
					}
				}
    		}).start();
    	}
    	
    	}else {
    		

    		final Object[] listeners = listenerList.getListenerList();
    		Logger.writeln("Cable " + (isLock ? "Locking" : "Unlocking"));
    		if(isLock && !isLocked()) {
        		new Thread(new Runnable() {

    				@Override
    				public void run() {
    		    		try {
    		    			if(enableLock.isHigh()) {
    		    				Logger.writeln("enableLock.isHigh action 1");
    		    				unlockControlAction();
    				    		Thread.sleep(UNLOCK_DELAY);
    		    			}
				    		lockControlAction();
    			    		Thread.sleep(LOCK_DELAY);
    			    		if(!isLocked()) {
    							for(int i = 0; 2 * i < listeners.length; ++i) {
    								((LockStateListener)listeners[2* i + 1]).lockFailed(new ChangeEvent(this));
    							}
    			    		}
    					} catch (InterruptedException e) {
    					}
    				}
        		}).start();
        	} else if(!isLock && isLocked()){
        		new Thread(new Runnable() {

    				@Override
    				public void run() {
    		    		try {
    		    			if(enableLock.isLow()) {

    				    		lockControlAction();
    		    				Thread.sleep(LOCK_DELAY);
    		    			}

    		    			unlockControlAction();
    			    		Thread.sleep(UNLOCK_DELAY);
    			    		if(isLocked()) {
    							for(int i = 0; 2 * i < listeners.length; ++i) {
    								((LockStateListener)listeners[2* i + 1]).unlockFailed(new ChangeEvent(this));
    							}
    			    		}
    			    		while(isLocked()) {

    			    			lockControlAction();
    		    				Thread.sleep(LOCK_DELAY);   			    		
    			    			unlockControlAction();
    		    				Thread.sleep(UNLOCK_DELAY);    			    		
    			    		}
    					} catch (InterruptedException e) {
    					}
    				}
        		}).start();
        	}
      	   			  		
    	}
    
    }

    public void setChargerCurrentLimitStartFlag(boolean input) {
    	this.isCurrentLimitSetAfterChargingSectionStart = input;
    }
    
    public boolean getChargerCurrentLimitStartFlag() {
    	return this.isCurrentLimitSetAfterChargingSectionStart;
    }    
    
    @Override
	public float getChargeCurrent() {
    	return currentRating;
	}

    
	public float getChargeCurrentRating() {
    	return currentRating;
	}
	
	public float getLastChargeCurrentRating() {
    	return lastCurrentRating;
	}
	
	public float getLastHigherChargeCurrentRating() {
    	return lastHigherCurrentRating;
	}
	
	public long getSetChargeCurrentTime() {
    	return currentRatingUpdateTime;
	}
	
	public long getLastSetChargeCurrentTime() {
    	return lastCurrentRatingUpdateTime;
	}
	
	public long getLastHigherSetChargeCurrentTime() {
    	return lastHigherCurrentRatingUpdateTime;
	}		
	
	@Override
    public void setChargeCurrent(float ampere) {
		System.out.println("Set charging current " + ampere);
		if(ampere < 6) {
			ampere = 0;
		} else if(ampere > currentAllowed){
			ampere = currentAllowed;
		}

		if(isCurrentLimitSetAfterChargingSectionStart) {
			lastHigherCurrentRating = ampere;
			lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();
			lastCurrentRating = ampere;
			lastCurrentRatingUpdateTime = lastHigherCurrentRatingUpdateTime;
			Logger.writeln("Update limits at startTransaction: "+ "last Higher & last limit :"+ ampere +" at time "+ lastHigherCurrentRatingUpdateTime);
			isCurrentLimitSetAfterChargingSectionStart = false;
		}else {
			lastCurrentRating = currentRating;
			lastCurrentRatingUpdateTime = currentRatingUpdateTime;
//			if( (System.currentTimeMillis() - lastHigherCurrentRatingUpdateTime) > IEC_ALLOWED_DEALAY){
//				lastHigherCurrentRating = ampere;
//				lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();
//				Logger.writeln("Old last higher limit expired & updated: "+ ampere +" at time "+ lastHigherCurrentRatingUpdateTime);			
//			}
			if(ampere>lastHigherCurrentRating) {
				lastHigherCurrentRating = ampere;
				lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();
				Logger.writeln("New higher limit: "+ ampere +" at time "+ lastHigherCurrentRatingUpdateTime);	
			}			
			
		}
		
		
//		lastCurrentRating = currentRating;
//		lastCurrentRatingUpdateTime = currentRatingUpdateTime;
//		if( (System.currentTimeMillis() - lastHigherCurrentRatingUpdateTime) > IEC_ALLOWED_DEALAY){
//			lastHigherCurrentRating = ampere;
//			lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();
//			Logger.writeln("Old last higher limit expire: "+ ampere +" at time "+ lastHigherCurrentRatingUpdateTime);			
//		}
//		if(ampere>lastHigherCurrentRating) {
//			lastHigherCurrentRating = ampere;
//			lastHigherCurrentRatingUpdateTime = System.currentTimeMillis();
//			Logger.writeln("New higher limit: "+ ampere +" at time "+ lastHigherCurrentRatingUpdateTime);	
//		}
		
		
		currentRating = ampere;
		currentRatingUpdateTime = System.currentTimeMillis();
		Logger.writeln("Charging Current limit updated: "+ currentRating +" at time "+ currentRatingUpdateTime);
		
		if(pwmCP.getDuty() > 5 && pwmCP.getDuty() <= 100) {
			setPilotEnable(true);
		}
    }
    
    public boolean start() {
    	if(!isStart()) {    		
	    	if(type == SOCKET) {
	    		setCableLock(false);
	    	}
			
	    	setEnable(true);
			
			return true;
    	}

		return false;
     }

    public void stop() {
		setEnable(false);
		
		setCableLock(false);

    }

	public void setEnable(boolean isEnable) {
		this.isEnable = isEnable;
		setPilot();
	}

	@Override
	public void setPilotEnable(boolean isEnable) {
		isPilotEnable = isEnable;
		setPilot();
	}


	private void setPilot() {
		if(isEnable) {
			if(isPilotEnable) {
				double duty;
				if(currentRating > 51) {
					duty = currentRating / 2.5 + 64;
				} else {
					duty = currentRating / 0.6;
				}
				if(duty < 10) {
					//duty = 7.5;
					duty = 100;			// 2021-02-01: EV not is not allowed charging for DC(100% PWM) Control Pilot signal, need to be verified on real EV 
				}
				pwmCP.setDuty(duty);
			} else {
				pwmCP.setDuty(100);
			}
		} else {
			if(cpIdleType!=CP_IDLE_TYPE2) {
				pwmCP.setDuty(0);
			}else {
				pwmCP.setDuty(100);
			}
		}
		Logger.writeln("CP Duty is " + (int)pwmCP.getDuty());
	}


//For test CP only	
//	private void setPilot() {
//		if(isEnable) {
//			if(isPilotEnable) {
////				double duty;
////				if(currentRating > 51) {
////					duty = currentRating / 2.5 + 64;
////				} else {
////					duty = currentRating / 0.6;
////				}
////				if(duty < 10) {
////					//duty = 7.5;
////					duty = 100;			// 2021-02-01: EV not is not allowed charging for DC(100% PWM) Control Pilot signal, need to be verified on real EV 
////				}
////				pwmCP.setDuty(duty);
//				
//				pwmCP.setDuty(100);
//			} else {
//				pwmCP.setDuty(100);
//			}
//		} else {
//			if(cpIdleType!=CP_IDLE_TYPE2) {
//				pwmCP.setDuty(0);
//			}else {
//				pwmCP.setDuty(100);
//			}
//		}
//		Logger.writeln("CP Duty is " + (int)pwmCP.getDuty());
//	}	
	
	public double getCurrent(int phase) {
		return meter.getCurrent(phase);
	}	

	public double getEnergy() {
		return meter.getEnergy();
	}

	public double getVoltage(int phase) {
		return meter.getVoltage(phase);
	}

	public double getPowerFactor(int phase) {
		return meter.getPowerFactor(phase);
	}	

	public void set50Pilot() {
		pwmCP.setDuty(50);
	}

}
