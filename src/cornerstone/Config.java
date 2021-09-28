package cornerstone;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Config {
	@SerializedName("Time Zone") private TimeZone timeZone;
	@SerializedName("Time Slot Maximum (Min)") private TimeSlotMaximumMin timeSlotMaximumMin;
	@SerializedName("Phase") private Phase phase;
	@SerializedName("Admin Card") private AdminCard adminCard;
	@SerializedName("Server Path") private ServerPath serverPath;
	@SerializedName("Time Select Stop Button") private TimeSelectStopButton timeSelectStopButton;
    @SerializedName("Clock") private Clock clock;
    @SerializedName("Custom Fields") private CustomFields customFields;
    @SerializedName("Network Timeout (ms)") private NetworkTimeoutMs networkTimeoutMs;
    @SerializedName("Remote OCPP Server Path") private RemoteOCPPServerPath remoteOCPPServerPath;
    @SerializedName("Powermeter Type") private PowermeterType powermeterType;
    @SerializedName("Powermeter ID") private PowermeterID powermeterID;
    @SerializedName("Auto Unlock Interval (ms)") private AutoUnlockIntervalMs autoUnlockIntervalMs;
    @SerializedName("LPRS Engine") private LPRSEngine lPRSEngine;
    @SerializedName("Maximum Capacity (A)") private MaximumCapacityA maximumCapacityA;
    @SerializedName("UART Port") private UARTPort uARTPort;
    @SerializedName("Time Select Fully Charge") private TimeSelectFullyCharge timeSelectFullyCharge;
    @SerializedName("Remote OCPP Server Username") private RemoteOCPPServerUsername remoteOCPPServerUsername;
    @SerializedName("Charging detail") private Chargingdetail chargingdetail;
    @SerializedName("Window")private Window window;
    @SerializedName("LPRS Engine Port") private LPRSEnginePort lPRSEnginePort;
    @SerializedName("LPRS") private LPRS lPRS;
    @SerializedName("station Name") private StationName stationName;
    @SerializedName("Serial No") private SerialNo serialNo;
    @SerializedName("Type") private Type type;
    @SerializedName("Authentication") private Authentication authentication;
    @SerializedName("Remote OCPP Server Password") private RemoteOCPPServerPassword remoteOCPPServerPassword;
    @SerializedName("Default Capacity (A)") private DefaultCapacityA defaultCapacityA;
    @SerializedName("Backdoor Server") private BackdoorServer backdoorServer;
    @SerializedName("End upon state A") private EnduponstateA enduponstateA;
    @SerializedName("Hardware Version") private HardwareVersion hardwareVersion;
	public TimeZone getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}
	public TimeSlotMaximumMin getTimeSlotMaximumMin() {
		return timeSlotMaximumMin;
	}
	public void setTimeSlotMaximumMin(TimeSlotMaximumMin timeSlotMaximumMin) {
		this.timeSlotMaximumMin = timeSlotMaximumMin;
	}
	public Phase getPhase() {
		return phase;
	}
	public void setPhase(Phase phase) {
		this.phase = phase;
	}
	public AdminCard getAdminCard() {
		return adminCard;
	}
	public void setAdminCard(AdminCard adminCard) {
		this.adminCard = adminCard;
	}
	public ServerPath getServerPath() {
		return serverPath;
	}
	public void setServerPath(ServerPath serverPath) {
		this.serverPath = serverPath;
	}
	public TimeSelectStopButton getTimeSelectStopButton() {
		return timeSelectStopButton;
	}
	public void setTimeSelectStopButton(TimeSelectStopButton timeSelectStopButton) {
		this.timeSelectStopButton = timeSelectStopButton;
	}
	public Clock getClock() {
		return clock;
	}
	public void setClock(Clock clock) {
		this.clock = clock;
	}
	public NetworkTimeoutMs getNetworkTimeoutMs() {
		return networkTimeoutMs;
	}
	public void setNetworkTimeoutMs(NetworkTimeoutMs networkTimeoutMs) {
		this.networkTimeoutMs = networkTimeoutMs;
	}
	public RemoteOCPPServerPath getRemoteOCPPServerPath() {
		return remoteOCPPServerPath;
	}
	public void setRemoteOCPPServerPath(RemoteOCPPServerPath remoteOCPPServerPath) {
		this.remoteOCPPServerPath = remoteOCPPServerPath;
	}
	public PowermeterType getPowermeterType() {
		return powermeterType;
	}
	public void setPowermeterType(PowermeterType powermeterType) {
		this.powermeterType = powermeterType;
	}
	public PowermeterID getPowermeterID() {
		return powermeterID;
	}
	public void setPowermeterID(PowermeterID powermeterID) {
		this.powermeterID = powermeterID;
	}
	public AutoUnlockIntervalMs getAutoUnlockIntervalMs() {
		return autoUnlockIntervalMs;
	}
	public void setAutoUnlockIntervalMs(AutoUnlockIntervalMs autoUnlockIntervalMs) {
		this.autoUnlockIntervalMs = autoUnlockIntervalMs;
	}
	public LPRSEngine getlPRSEngine() {
		return lPRSEngine;
	}
	public void setlPRSEngine(LPRSEngine lPRSEngine) {
		this.lPRSEngine = lPRSEngine;
	}
	public MaximumCapacityA getMaximumCapacityA() {
		return maximumCapacityA;
	}
	public void setMaximumCapacityA(MaximumCapacityA maximumCapacityA) {
		this.maximumCapacityA = maximumCapacityA;
	}
	public UARTPort getuARTPort() {
		return uARTPort;
	}
	public void setuARTPort(UARTPort uARTPort) {
		this.uARTPort = uARTPort;
	}
	public TimeSelectFullyCharge getTimeSelectFullyCharge() {
		return timeSelectFullyCharge;
	}
	public void setTimeSelectFullyCharge(TimeSelectFullyCharge timeSelectFullyCharge) {
		this.timeSelectFullyCharge = timeSelectFullyCharge;
	}
	public RemoteOCPPServerUsername getRemoteOCPPServerUsername() {
		return remoteOCPPServerUsername;
	}
	public void setRemoteOCPPServerUsername(RemoteOCPPServerUsername remoteOCPPServerUsername) {
		this.remoteOCPPServerUsername = remoteOCPPServerUsername;
	}
	public Chargingdetail getChargingdetail() {
		return chargingdetail;
	}
	public void setChargingdetail(Chargingdetail chargingdetail) {
		this.chargingdetail = chargingdetail;
	}
	public Window getWindow() {
		return window;
	}
	public void setWindow(Window window) {
		this.window = window;
	}
	public LPRSEnginePort getlPRSEnginePort() {
		return lPRSEnginePort;
	}
	public void setlPRSEnginePort(LPRSEnginePort lPRSEnginePort) {
		this.lPRSEnginePort = lPRSEnginePort;
	}
	public LPRS getlPRS() {
		return lPRS;
	}
	public void setlPRS(LPRS lPRS) {
		this.lPRS = lPRS;
	}
	public StationName getStationName() {
		return stationName;
	}
	public void setStationName(StationName stationName) {
		this.stationName = stationName;
	}
	public SerialNo getSerialNo() {
		return serialNo;
	}
	public void setSerialNo(SerialNo serialNo) {
		this.serialNo = serialNo;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public Authentication getAuthentication() {
		return authentication;
	}
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}
	public RemoteOCPPServerPassword getRemoteOCPPServerPassword() {
		return remoteOCPPServerPassword;
	}
	public void setRemoteOCPPServerPassword(RemoteOCPPServerPassword remoteOCPPServerPassword) {
		this.remoteOCPPServerPassword = remoteOCPPServerPassword;
	}
	public DefaultCapacityA getDefaultCapacityA() {
		return defaultCapacityA;
	}
	public void setDefaultCapacityA(DefaultCapacityA defaultCapacityA) {
		this.defaultCapacityA = defaultCapacityA;
	}
	public BackdoorServer getBackdoorServer() {
		return backdoorServer;
	}
	public void setBackdoorServer(BackdoorServer backdoorServer) {
		this.backdoorServer = backdoorServer;
	}
	public EnduponstateA getEnduponstateA() {
		return enduponstateA;
	}
	public void setEnduponstateA(EnduponstateA enduponstateA) {
		this.enduponstateA = enduponstateA;
	}
	public HardwareVersion getHardwareVersion() {
		return hardwareVersion;
	}
	public void setHardwareVersion(HardwareVersion hardwareVersion) {
		this.hardwareVersion = hardwareVersion;
	}
	
	
	public class TimeZone{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private Integer value;
		@SerializedName("Option") private List<Integer> option;
	    private String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public Integer getValue() {
			return value;
		}
		public void setValue(Integer value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	    
	}

	public class TimeSlotMaximumMin{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Phase") private Phase phase;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class Phase{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
		
		
	}

	public class AdminCard{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class ServerPath{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
	    private String getType() {
			return type;
		}
		private void setType(String type) {
			this.type = type;
		}
		private String getAuthority() {
			return authority;
		}
		private void setAuthority(String authority) {
			this.authority = authority;
		}
		private String getValue() {
			return value;
		}
		private void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class TimeSelectStopButton{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
		
		
	}

	public class Clock{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
		
	}
	
	public class CustomFields{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
		
	}

	public class NetworkTimeoutMs{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private int value;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	    
	}

	public class RemoteOCPPServerPath{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
		
	}

	public class PowermeterType{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
		
		
	}

	public class PowermeterID{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class AutoUnlockIntervalMs{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private int value;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	    
	}

	public class LPRSEngine{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
		
	}

	public class MaximumCapacityA{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private int value;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	}
	
	public class TimeSelectIntervalMin{
		@SerializedName("Type") private String type = "Range";
		@SerializedName("Authority") private String authority = "Admin";
		@SerializedName("Value") private int value = 15;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	}

	public class UARTPort{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}

	public class TimeSelectFullyCharge{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
	    public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	}

	public class RemoteOCPPServerUsername{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class Chargingdetail{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}

	public class Window{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}

	public class LPRSEnginePort{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private int value;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	    
	}

	public class LPRS{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class StationName{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class SerialNo{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class Type{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}

	public class Authentication{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}
	

	public class RemoteOCPPServerPassword{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
		
	}

	public class DefaultCapacityA{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private int value;
		@SerializedName("Option") private List<Integer> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public List<Integer> getOption() {
			return option;
		}
		public void setOption(List<Integer> option) {
			this.option = option;
		}
	    
	    
	}

	public class BackdoorServer{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	    
	    
	}

	public class EnduponstateA{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}

	public class HardwareVersion{
		@SerializedName("Type") private String type;
		@SerializedName("Authority") private String authority;
		@SerializedName("Value") private String value;
		@SerializedName("Option") private List<String> option;
		
		private String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getAuthority() {
			return authority;
		}
		public void setAuthority(String authority) {
			this.authority = authority;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public List<String> getOption() {
			return option;
		}
		public void setOption(List<String> option) {
			this.option = option;
		}
	    
	    
	}
}
