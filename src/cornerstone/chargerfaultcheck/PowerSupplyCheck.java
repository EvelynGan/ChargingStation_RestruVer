package cornerstone.chargerfaultcheck;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import cornerstone.IECCharger;
import victorho.device.ADCChannel;
import victorho.device.Contactor;
import victorho.device.LED;
import victorho.platform.adapter.GPIO;
import victorho.util.Logger;
import victorho.util.ValueChangeEvent;
import victorho.util.ValueChangeListener;

public class PowerSupplyCheck {
	private int ver = 0; 
	public static final int ADC_P12VSENSE = 0;
	public static final int ADC_N12VSENSE = 1;
	private String version = "2_5";
	private GPIO acOk;
	private GPIO p12VOk;
	private IECCharger charger;
	private Contactor contactor;
	private ADCChannel[] adc;
	private ValueChangeListener[] adcVclistener;
//	private ADCChannel p12VSense;
//	private ValueChangeListener p12SenseListener;
//	private ADCChannel n12VSense;
//	private ValueChangeListener n12SenseListener;
	private int cnt = 0;
	private int cnt2 = 0;
	public PowerSupplyCheck(GPIO acOk, GPIO p12VOk, Contactor contactorIn,  IECCharger chargerIn, String ver){
		this.acOk = acOk;
		this.p12VOk = p12VOk;
		this.contactor = contactorIn;
		this.charger = chargerIn;
		this.version = ver;
	}
	
	public void setADCHardware(ADCChannel[] adcIn) {
		this.adc = adcIn;
	}

	public void startADCListener() {
		this.adcVclistener = new ValueChangeListener[2];
		
		this.adc[ADC_P12VSENSE].setWindowSize(10);
		this.adc[ADC_P12VSENSE].addMinValueChangeListener(this.adcVclistener[ADC_P12VSENSE], 100);
	
		this.adcVclistener[ADC_P12VSENSE] = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				int value = evt.getValue();
				Logger.writeln("+12VSense value is :" +value);
				
				cnt++;
				if(cnt>1000) {
					int valueP12VSense = evt.getValue();
					Logger.writeln("12VSense value is :" +valueP12VSense);
					cnt=0;
				} 
			}
    		
    	};	
    	
		this.adc[ADC_N12VSENSE].setWindowSize(10);
		this.adc[ADC_N12VSENSE].addMinValueChangeListener(this.adcVclistener[ADC_N12VSENSE], 100);
	
		this.adcVclistener[ADC_N12VSENSE] = new ValueChangeListener() {

			@Override
			public synchronized void valueChanged(ValueChangeEvent evt) {
				int value = evt.getValue();
				Logger.writeln("-12VSense value is :" +value);
				
				cnt2++;
				if(cnt2>1000) {
					int valueN12VSense = evt.getValue();
					Logger.writeln("-12VSense value is :" +valueN12VSense);
					cnt2=0;
				} 
			}
    		
    	};   	  	
	}	
	
	public boolean acReady() {
		return this.acOk.isHigh();
	}	
	
	public boolean acOk() {
		return this.acOk.isHigh();
	}
	
	public boolean dcOk() {
		return this.p12VOk.isHigh();
	}

	public boolean isP12V_OK() {
		return this.p12VOk.isHigh();
	}	
	
	public int acFailHandle(){
		this.contactor.setState(false);
		this.charger.setCableLock(false);
		return 0;
	}

	public int p12VOkFailHandle(){
		return 0;
	}	
	
	public void dcPwerFailCheckStart() {
		
		startADCListener(); 
		
		this.p12VOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				if(p12VOk.isLow()) {
					p12VOkFailHandle();
					Logger.writeln("+12V sense fail: +12SENSE pin low");
				}
			}
    		
    	});		
	}
	
	public void acPowerFailCheckStart() {
		
	}	

	
}
