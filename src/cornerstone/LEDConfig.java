package cornerstone;
import victorho.device.LEDMode;
import java.awt.Color;
import java.util.HashMap;

public final class LEDConfig {
	private static Color Red = new Color(255, 0, 0);
	private static Color Orange = new Color(255, 63, 0);
	private static Color Green = new Color(0, 100,  0);
	private static Color Blue = new Color(0, 0, 255);
	private static Color White = new Color(255, 255, 255);
	public static Color UIOrange = ChargingStationUI.Orange;
	public static Color UIGreen = Green;
	public static Color UIBlue = ChargingStationUI.Blue;
	public static Color UIRed = ChargingStationUI.Red;
	public static Color Black = new Color(0, 0, 0);
	/**
	public static Color UIOrange0 = new Color(Color.HSBtoRGB(30, 100, 0));
	public static Color UIOrange1 = new Color(Color.HSBtoRGB(30, 100, 10));
	public static Color UIOrange2 = new Color(Color.HSBtoRGB(30, 100, 20));
	public static Color UIOrange3 = new Color(Color.HSBtoRGB(30, 100, 30));
	public static Color UIOrange4 = new Color(Color.HSBtoRGB(30, 100, 40));
	public static Color UIOrange5 = new Color(Color.HSBtoRGB(30, 100, 50));
**/
	public static Color UIOrange0 = new Color(230,92,0);
	public static Color UIOrange1 = new Color(204,82,0);
	public static Color UIOrange2 = new Color(179,71,0);
	public static Color UIOrange3 = new Color(153,61,0);
	public static Color UIOrange4 = new Color(102,41,0);
	public static Color UIOrange5 = new Color(77,31,0);
	public static Color UIOrange6 = new Color(51,20,0);
	public static Color UIOrange7 = new Color(26,10,0);
	public static Color UIOrange8 = new Color(0,0,0);
	
	public static Color UIBlue0 = new Color(0,0,60);
	public static Color UIBlue1 = new Color(0,0,80);
	public static Color UIBlue2 = new Color(0,0,100);
	public static Color UIBlue3 = new Color(0,0,120);
	public static Color UIBlue4 = new Color(0,0,140);
	public static Color UIBlue5 = new Color(0,0,160);
	public static Color UIBlue6 = new Color(0,0,180);
	public static Color UIBlue7 = new Color(0,0,200);
	public static Color UIBlue8 = new Color(0,0,220);
	
	public static Color UIGreen0 = new Color(0,60,0);
	public static Color UIGreen1 = new Color(0,80,0);
	public static Color UIGreen2 = new Color(0,100,0);
	public static Color UIGreen3 = new Color(0,120,0);
	public static Color UIGreen4 = new Color(0,140,0);
	public static Color UIGreen5 = new Color(0,160,0);
	public static Color UIGreen6 = new Color(0,180,0);
	public static Color UIGreen7 = new Color(0,200,0);
	public static Color UIGreen8 = new Color(0,220,0);
	
	public static Color Charge0 = UIBlue;
	public static Color Charge1 = White;
	public static Color Charge2 = UIBlue;
	
	
	//Predefine mode - Blue stay 2 Sec, Breathing orange
	public static LEDMode ModeSBlue2BOrange = new LEDMode(LEDMode.LEDEffect.Breathing,
			new Color[] {UIBlue,UIOrange0,UIOrange1,UIOrange2,UIOrange3,UIOrange4,UIOrange5,UIOrange6,UIOrange7,UIOrange8},2000, 15);
	
	//Predefine mode - Blue stay 0.001 Sec, Breathing orange
	public static LEDMode ModeBlueBreath = new LEDMode(LEDMode.LEDEffect.Breathing,
			new Color[] {UIBlue0,UIBlue0,UIBlue1,UIBlue2,UIBlue3,UIBlue4,UIBlue5,UIBlue6,UIBlue7,UIBlue8},15, 15);
	
	//Predefine mode - 
	public static LEDMode ModeGreenBreath = new LEDMode(LEDMode.LEDEffect.Breathing,
			new Color[] {UIGreen0,UIGreen0,UIGreen1,UIGreen2,UIGreen3,UIGreen4,UIGreen5,UIGreen6,UIGreen7,UIGreen8},2000, 15);
	
	//Predefine mode - Pumping blue
	public static LEDMode ModePBlue = new LEDMode(LEDMode.LEDEffect.Running,
			new Color[] {Charge0,Charge1,Charge2},50);
	
	
	public static LEDMode StatusDefault = new LEDMode(UIOrange);
	public static LEDMode StatusReady = new LEDMode(UIGreen);;	
	
	public static LEDMode StatusAuthorize = new LEDMode(UIBlue);
	public static LEDMode StatusAuthorized = new LEDMode(White);

	public static LEDMode StatusCharging = ModePBlue;
	
	public static LEDMode StatusExpire = new LEDMode(UIGreen);
	public static LEDMode StatusFault = new LEDMode(UIRed);
	public static LEDMode StatusVentilation = ModeSBlue2BOrange;
	public static LEDMode StatusPlugging = new LEDMode(UIOrange);
	public static LEDMode StatusLocked = ModeBlueBreath;
	public static LEDMode StatusReplug = new LEDMode(UIRed);
	public static LEDMode StatusUnplug = new LEDMode(UIRed);
	public static LEDMode StatusUnregister = new LEDMode(UIRed);
	public static LEDMode StatusNetworkDown = new LEDMode(UIRed);
	public static LEDMode StatusUnlock = new LEDMode(UIGreen); 
	public static LEDMode StatusUnlocked = ModeGreenBreath;
	public static LEDMode StatusFinish = new LEDMode(UIOrange);
	public static LEDMode StatusCPError = new LEDMode(UIRed);
	
	public static LEDMode MsgValid = new LEDMode(UIGreen);
	public static LEDMode MsgInsufficent = new LEDMode(UIRed);
	public static LEDMode MsgInvalid = new LEDMode(UIRed); //Change Message Display Time!
	public static LEDMode MsgReplug = new LEDMode(UIRed);
	public static LEDMode MsgCharging = StatusCharging;
	public static LEDMode MsgPause = ModeSBlue2BOrange;
	public static LEDMode MsgAuthorize = new LEDMode(UIGreen);
	
	public static HashMap<ChargingStation.State, Color> stateSteadyColor = new HashMap<>();
	
	public LEDConfig(){
		stateSteadyColor.put(ChargingStation.State.Authorize, UIBlue);
		stateSteadyColor.put(ChargingStation.State.Authorized, White);
	//	stateSteadyColor.put(ChargingStation.State.Authorizing, ??);
		//Charging is flashing
		stateSteadyColor.put(ChargingStation.State.CPError, UIRed);		
		stateSteadyColor.put(ChargingStation.State.Expire, UIGreen);
		stateSteadyColor.put(ChargingStation.State.Fault, UIRed);
		stateSteadyColor.put(ChargingStation.State.Initialize, UIOrange);
	//	stateSteadyColor.put(ChargingStation.State.Locked, ??);	
		stateSteadyColor.put(ChargingStation.State.NetworkDown, UIRed);
		stateSteadyColor.put(ChargingStation.State.Pause, UIOrange);	
		stateSteadyColor.put(ChargingStation.State.Plugging, UIOrange);
		stateSteadyColor.put(ChargingStation.State.Ready, UIGreen);
		stateSteadyColor.put(ChargingStation.State.Replug, UIRed);
	//	stateSteadyColor.put(ChargingStation.State.Unavailable, ??);		
		stateSteadyColor.put(ChargingStation.State.Unlock, UIGreen);		
		stateSteadyColor.put(ChargingStation.State.Unlocked, UIGreen); //flashing green?
	//	stateSteadyColor.put(ChargingStation.State.Unplug, ?? );
		stateSteadyColor.put(ChargingStation.State.Unregister, UIRed);	
		stateSteadyColor.put(ChargingStation.State.Ventilation, UIOrange);
		stateSteadyColor.put(ChargingStation.State.Startup, UIOrange);
		stateSteadyColor.put(ChargingStation.State.CriticalFault, UIRed);	
		
	}
	
	
	
	// 2021-07-05 Temporary added for Aloft demo
	public static Color mapNonflashStateColor(ChargingStation.State state) {
		Color c = Color.BLACK;
		switch (state) {
		case Authorize:
			c = UIBlue;
			break;
		case Authorized:
			c= White;
			break;
		case Authorizing:
			
			break;
//		case Charging:
//			
//			break;
		case CPError:
			c = UIRed;
			break;
		case Expire:
			c = UIGreen;
			break;			
		case Fault:
			c = UIRed;
			break;			
		case Initialize:
			c = UIOrange;
			break;
//		case Locked:
//
//			break;			
		case NetworkDown:
			c = UIRed;
			break;
		case Pause:
			c = UIOrange;
			break;			
		case Plugging:
			c = UIOrange;
			break;				
		case Ready:
			c = UIGreen;
			break;				
		case Replug:
			c = UIRed;
			break;				
//		case Unavailable:
//			c = UIOrange;
//			break;				
//		case Unlock:
//			c = UIOrange;
//			break;				
		case Unlocked:
			c = UIGreen;
			break;				
		case Unplug:
			c = UIRed;
			break;				
		case Unregister:
			c = UIRed;
			break;				
		case Ventilation:
			c = UIOrange;
			break;				
		default:
			c = Color.BLACK;
			break;
				
		}
		return c;
	}
	

}
