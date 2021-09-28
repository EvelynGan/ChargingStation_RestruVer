/**
 * @author Ho Wai Fung
 * @date 10/3/2017
 *
 */
package cornerstone;


import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import victorho.device.PowerMeter;

public abstract class Charger {

	public static final int STATE_EExt = -2;
	public static final int STATE_E = -1;
	public static final int INITIALIZE = 0;
	public static final int STATE_CABLE_DISCONNECT = 1;
	public static final int STATE_CABLE_CONNECT = 2;
	public static final int STATE_N = 3;
	public static final int STATE_A = 4;
	public static final int STATE_B = 5;
	public static final int STATE_C = 6;
	public static final int STATE_D = 7;
	public static final int STATE_FAULT_CRITICAL = 9900;

    protected int state;
    protected PowerMeter meter;
    protected EventListenerList listenerlist;
        
    public Charger(PowerMeter meter) {
    	this.meter = meter;
    	listenerlist = new EventListenerList();
    }
    
    public void addChangeListener(ChangeListener l) {
    	listenerlist.add(ChangeListener.class, l);
    }
    
	void fireChangeEvent(ChangeEvent evt) {
		Object[] listeners = listenerlist.getListenerList();
		for(int i = 0; i < listeners.length; i += 2) {
			if(listeners[i] == ChangeListener.class) {
				((ChangeListener)listeners[i + 1]).stateChanged(evt);
			}
		}
		
	}

    abstract public int getState();
    
    void setState(int state) {
    	this.state = state;
    	fireChangeEvent(new ChangeEvent(new Integer(state)));
    }
    
	abstract public void setCableLock(boolean isLock);

	abstract public float getChargeCurrent();
	
	abstract public void setChargeCurrent(float ampere);
    
	abstract public void setPilotEnable(boolean isEnable);

}
