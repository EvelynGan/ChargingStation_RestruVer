package cornerstone.chargerfaultcheck;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.EventListenerList;

import victorho.platform.adapter.GPIO;
import victorho.util.LockStateListener;
import victorho.util.Logger;

public class EStop extends EmergencyStop {
	
	public static final int E_STOPSTATUS = 0;
	private GPIO[] gpio;
	private boolean triggeredState=false;

	private EventListenerList listenerList;
	
	public EStop(GPIO[] gpio) {
		this.gpio = gpio;
//		gpio[E_STOPSTATUS].addActionListener(new ActionListener() {
//    		
//			@Override
//			public void actionPerformed(ActionEvent evt) {
//				fireEStopEvent();
//			}
//    		
//    	});		
	}
	
	public boolean isTriggered(){		
		return gpio[E_STOPSTATUS].isLow();		
	}
	
	public void setTriggerState(boolean input){		
		this.triggeredState = input;		
	}
	
	public GPIO getStatusGpio() {
		return this.gpio[E_STOPSTATUS];
	}
	
	private void fireEStopEvent() {
		
	}

}
