package cornerstone.chargerfaultcheck;

import java.util.HashMap;

import cornerstone.IECCharger;
import victorho.device.PowerMeter;
import victorho.util.Logger;

public class OverCurrentCheck {
	
	private final int IEC_ALLOWED_DELAY = 5000;
	private boolean overCurrentTriggeredPhaseA=false;
	private boolean overCurrentTriggeredPhaseB=false;
	private boolean overCurrentTriggeredPhaseC=false;
	private boolean overCurrentTriggered=false;
	private final int MAX_LIMIT = 34;
	private static HashMap<Long, Double> limitHistory = new HashMap<>();
	
	public OverCurrentCheck() {
	}
	
	public OverCurrentCheck(long first, double limit) {
		this.limitHistory = new HashMap<>();
		this.limitHistory.put(first, limit);		//regLength.put(AIGAIN,3); 
	}
	
	public boolean isOverCurrent(double measuredValue, double limit, int phase) {
	 
		boolean result = false;
	 			
		if(measuredValue>limit) {
			if(phase==PowerMeter.PHASE_A) {
				Logger.writeln("Phase A over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseA = true;
				overCurrentTriggered=true;
			}else if(phase==PowerMeter.PHASE_B) {
				Logger.writeln("Phase B over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseB = true;
				overCurrentTriggered=true;
			}else {
				Logger.writeln("Phase C over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseC = true;
				overCurrentTriggered=true;
			}
			result = true;
		}else {
			result =  false;
		}
		
		return result;
	}	 

//	
//	public boolean isOverCurrent(IECCharger charger, double measuredValue, int phase) {
//	 
//		double limit;
//		boolean result = false;
//	 			
//		if( (charger.getChargeCurrent()<charger.getLastChargeCurrent()) && ((System.currentTimeMillis() -  charger.getLastSetChargeCurrentTime()) < IEC_ALLOWED_DELAY) ) {		
//			limit =charger.getLastChargeCurrent();
//		} else {
//			limit = charger.getChargeCurrent();
//		}
//
//		if(measuredValue>limit) {
//			if(phase==PowerMeter.PHASE_A) {
//				Logger.writeln("Phase A over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseA = true;
//				overCurrentTriggered=true;
//			}else if(phase==PowerMeter.PHASE_B) {
//				Logger.writeln("Phase B over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseB = true;
//				overCurrentTriggered=true;
//			}else {
//				Logger.writeln("Phase C over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseC = true;
//				overCurrentTriggered=true;
//			}
//			result = true;
//		}else {
//			result =  false;
//		}
//		
//		return result;
//	}	 
//
//	public boolean isOverCurrent(IECCharger charger, double measuredValue, double chargerConsumption, int phase) {
//		 
//		double limit;
//		boolean result = false;
//	 			
//		if( (charger.getChargeCurrent()<charger.getLastChargeCurrent()) && ((System.currentTimeMillis() -  charger.getLastSetChargeCurrentTime()) < IEC_ALLOWED_DELAY) ) {		
//			limit =charger.getLastChargeCurrent();
//		} else {
//			limit = charger.getChargeCurrent();
//		}
//
//		if(limit<0.01) {
//			limit = chargerConsumption;
//		}
//		
//		if(measuredValue>limit) {
//			if(phase==PowerMeter.PHASE_A) {
//				Logger.writeln("Phase A over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseA = true;
//				overCurrentTriggered=true;
//			}else if(phase==PowerMeter.PHASE_B) {
//				Logger.writeln("Phase B over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseB = true;
//				overCurrentTriggered=true;
//			}else {
//				Logger.writeln("Phase C over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
//				overCurrentTriggeredPhaseC = true;
//				overCurrentTriggered=true;
//			}
//			result = true;
//		}else {
//			result =  false;
//		}
//		
//		return result;
//	}	
	
	public boolean isOverCurrent(IECCharger charger, double measuredValue, double chargerConsumption, int margin, int phase) {
		 
		double limit;
		boolean result = false;
		double ocMargin = ((double)margin)/100;
	 			
//		if( (charger.getChargeCurrentRating()<charger.getLastChargeCurrentRating()) && ((System.currentTimeMillis() -  charger.getLastSetChargeCurrentTime()) < IEC_ALLOWED_DELAY) ) {		
//			limit =charger.getLastChargeCurrentRating()*(1+ocMargin);
//		} else {
//			limit = charger.getChargeCurrentRating()*(1+ocMargin);
//		}

		if( (charger.getChargeCurrentRating()<charger.getLastHigherChargeCurrentRating()) && ((System.currentTimeMillis() - charger.getSetChargeCurrentTime()) < IEC_ALLOWED_DELAY) ) {		
			limit =charger.getLastHigherChargeCurrentRating()*(1+ocMargin);
			Logger.writeln("Latest rating: "+ charger.getChargeCurrentRating()               +" at time: "+ charger.getSetChargeCurrentTime());
			Logger.writeln("Last higher rating: "+ charger.getLastHigherChargeCurrentRating()+" at time: "+ charger.getLastHigherSetChargeCurrentTime() );
			Logger.writeln("Last rating: "+ charger.getChargeCurrentRating()                 +" at time: "+ charger.getLastSetChargeCurrentTime() );
			Logger.writeln("Result limit with(using last higher): "+ limit                   +" at time: "+ System.currentTimeMillis() + " with margin(%): "+ocMargin );
		} else {
			limit = charger.getChargeCurrentRating()*(1+ocMargin);
		}		

		if(limit<0.01) {
			limit = chargerConsumption;
		}
		
		if(limit>MAX_LIMIT) {
			limit = MAX_LIMIT;
			Logger.writeln("Limit over ceiling clipped to 32A");
		}
		
		Logger.writeln("Effective over-current limit: "+limit);
		
		if(measuredValue>limit) {
			Logger.writeln("Over-current detected " +"at margin:" + margin+"%  "+" charger consumption:"+ chargerConsumption);
			Logger.writeln("Latest rating: "+ charger.getChargeCurrentRating()               +" at time: "+ charger.getSetChargeCurrentTime());
			Logger.writeln("Last higher rating: "+ charger.getLastHigherChargeCurrentRating()+" at time: "+ charger.getLastHigherSetChargeCurrentTime() );
			Logger.writeln("Last rating: "+ charger.getChargeCurrentRating()                 +" at time: "+ charger.getLastSetChargeCurrentTime() );
			if(phase==PowerMeter.PHASE_A) {
				Logger.writeln("Phase A over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseA = true;
				overCurrentTriggered=true;
			}else if(phase==PowerMeter.PHASE_B) {
				Logger.writeln("Phase B over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseB = true;
				overCurrentTriggered=true;
			}else {
				Logger.writeln("Phase C over-current detected! "+ "limit: "+limit+ "measured value: "+measuredValue);
				overCurrentTriggeredPhaseC = true;
				overCurrentTriggered=true;
			}
			result = true;
		}else {
			result =  false;
		}
		
		return result;
	}	
	
	
	public Object overCurrent() {
		return false;
	}

	public Object overCurrentChk(PowerMeter meter, String meterType, int phase) {
		
		 boolean result = false;
		 
		 try {
			 
		 }catch (Exception e) {
			 
		 } 

		 return result;
	}	
	
//	public Object overCurrentChk(IECCharger charger, double measuredValue, int phase) {
//		boolean result = false;
//		if(isOverCurrent(charger, measuredValue, phase)) {
//			result = true;
//		}
//		return result;
//	}
	
	public Object overCurrentChk(IECCharger charger, double measuredValue, double chargerConsumption, int margin, int phase) {
		boolean result = false;
		if(isOverCurrent(charger, measuredValue, chargerConsumption, margin, phase)) {
			result = true;
		}
		return result;
	}

//	public Object overCurrentChk(IECCharger charger, double measuredValue, double chargerConsumption, int phase) {
//		boolean result = false;
//		if(isOverCurrent(charger, measuredValue, chargerConsumption, phase)) {
//			result = true;
//		}
//		return result;
//	}	
//	
	public Object overCurrentChk(double measuredValue, double limit, int phase) {
		boolean result = false;
		if(isOverCurrent(measuredValue, limit, phase)) {
		 result = true;
		}
		return result;
		}	 
	
	public boolean isOverCurrentTriggerred() {
		return overCurrentTriggered;
	}
	
	public boolean isOverCurrentTriggerred(int phase) {
		boolean result = false;
		if(phase==PowerMeter.PHASE_A) {
			result = overCurrentTriggeredPhaseA;
		}
		if(phase==PowerMeter.PHASE_B) {
			result = overCurrentTriggeredPhaseB;
		}
		if(phase==PowerMeter.PHASE_C) {
			result = overCurrentTriggeredPhaseC;
		}
		return result;
	}
	
	public void resetAllOverCurrentFlag() {
		overCurrentTriggeredPhaseA=false;
		overCurrentTriggeredPhaseB=false;
		overCurrentTriggeredPhaseC=false;
		overCurrentTriggered=false;		 
	}
	
	public void resetOverCurrentFlag(int phase) {
		if(phase==PowerMeter.PHASE_A) {
			overCurrentTriggeredPhaseA = false;
		}
		if(phase==PowerMeter.PHASE_B) {
			overCurrentTriggeredPhaseB = false;
		}
		if(phase==PowerMeter.PHASE_C) {
			overCurrentTriggeredPhaseC = false;
		}		 
	}
	
	public void setLimitHistory(long time, double limit) {
		this.limitHistory.put(time, limit);
	}
	
}
