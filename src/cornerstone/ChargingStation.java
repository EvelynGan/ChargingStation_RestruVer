/**
  * @author Ho Wai Fung
 * @date 10/3/2017
 *
 */
package cornerstone;

/**********************************************************************************************************************/
/********************************************* LIBRARY IMPORTED   *****************************************************/
/**********************************************************************************************************************/
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.text.*;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import com.ckzone.util.ClientEncryptUtil;
import com.google.gson.*;
import com.pi4j.io.gpio.exception.GpioPinExistsException;

import cornerstone.ChargingStationConfig.State;
import cornerstone.chargerfaultcheck.EStop;
import cornerstone.chargerfaultcheck.FaultState;
import cornerstone.chargerfaultcheck.OverCurrentCheck;
import cornerstone.chargerfaultcheck.PowerSupplyCheck;
//import apple.laf.JRSUIConstants.State;
import victorho.comm.ConnectionEvent;
import victorho.comm.ConnectionListener;
import victorho.comm.EvseLprsAckMsg;
import victorho.comm.EvseLprsMsg;
import victorho.comm.WebSocketClient;
import victorho.configuration.Configurable;
import victorho.crypto.EncryptionFactory;
import victorho.device.*;
import victorho.marco.GmailClient;
import victorho.ocpp.OCPP;
import victorho.ocpp.OCPP.ChargingProfile;
import victorho.ocpp.OCPP.MeterValues;
import victorho.ocpp.OCPP.StopTransaction;
import victorho.ocpp.OcppClient;
import victorho.platform.adapter.GPIO;
import victorho.platform.adapter.PWMPort;
import victorho.platform.adapter.SPI;
import victorho.platform.adapter.SerialAdapter;
import victorho.usb.DeviceManager;
import victorho.usb.USBListener;
import victorho.util.*;
import victorho.util.NetworkInterface;


public class ChargingStation implements ChangeListener, Configurable, DataListener, ConnectionListener, USBListener {

/**********************************************************************************************************************/
/************************************************ VARIABLES  **********************************************************/
/**********************************************************************************************************************/
	
	ChargingStationConfig configinfo = new ChargingStationConfig();	
	private static String encryptKey = "CsT403.403";
	private static File usbKey = new File("./rcr.key");				
	private static final String OCPP_TEXT = "CSTEV2020";
	private static final int TIME_SELECT_FULL = 99 * 60;
	private File record = new File("./.record");
	private File session = new File("./.session");
	private File meterReading = new File("./.meter");
	private File meterReadingBak = new File("./.meterBak");
 
	private static final int LOCKVER = IECCharger.LOCKVER_HBRIDGE_SOLEN;
	private OverCurrentCheck overCurrentChk;
	
	private byte[][] adminCard = { HexFactory.toBytes("7E C5 00 3C", ' '), HexFactory.toBytes("10 28 04 02", ' '), HexFactory.toBytes("8B D6 08 B9", ' '),	HexFactory.toBytes("8B D6 08 B9", ' ') };

	//EVSE Component
	private NFCReader nfcReader;
	private BarcodeReader barcode;
	private SoftButton unlockButton;				//2020-05-26 for re-enable "STOP" screen button; 2020-06-30 function added for plug & charge mode as well
//	private VideoCapture cam;
//	private Webcam webcam;
	private Camera camera;
//	private FsWebcamDevice webcam;
	private ChargingStationUI view;
	private Contactor contactor;
	private DataListener barcodeListener;
	private DataListener nfcListener;
	private IECCharger charger;
	private GPIO keyLed;
	private GPIO lockKey;
	private GPIO startKey;
	private GPIO eStopHardButton;				// Emergency Stop Hard Button
//	private PowerMeter powerMeter;
	private PowerMeter powerMeter2;
	private NTCThermister JT103;
	private AsiaVisionLPRS lprs;
	private List<BufferedImage> imgsTx;
	
	//Settings
//	private JsonObject config;
	private JSONObject config;
	private JSONObject lmsConfig;
	private StringBuffer configSecret;
	private int idleUnlockInterval;
	private int timeout;

	//Timer
	private Timer chargingTimer;
	private Timer heartbeatTimer;
	private Thread qrReadThread;
	private ScheduledExecutorService powerTimer;
//	private ScheduledExecutorService powerMeterTimer;
	private ScheduledExecutorService messageTimer;
	private List<ScheduledExecutorService> scheduleTimer;
	private ScheduledExecutorService sessionTimer;
	private Timer unlockTimer;
	private Timer plugUnlockTimer;
	private Timer stopTimer;
	private Timer countDownTimer;

	//Transaction Varables
	private int cid;
	private String idTag;
	private Date startTime;
	private int extid;
	private int ttid;
	private int tid;
	private int unitCount;
	private int unitLength;
	private double unitPrice;
	private double chargingFee;
	
	private int timeSelectRemain;
	private String timeRemainDesc;

	//Configuration
	//private OCPP.InsufficientBlanceAction insufficientBalanceAction = OCPP.InsufficientBlanceAction.Reject;
	private OCPP.InsufficientBlanceAction insufficientBalanceAction = OCPP.InsufficientBlanceAction.StopOnly;
	
	private OCPP.IdToken id;
	private Socket housingSocietyLprsSocket;
	private Thread housingSocietyLprsListener;
	private InputStream housingSocietyLprsIS;
	private ScheduledExecutorService chargingProfileService;
	private Thread LEDTimer;
	private Thread cameraCaptureThread;
	private Thread lprsTimeSyncThread;
	private Thread lprsSendMsgThread;	
	private Thread barcodeReadThread;
	private Thread imgsTxClient;
	private int msgSerialLprs;
	private int msgSerialLprsTimeSync;
	private ArrayList<String> lprsMsgBuf;
	private long tMark;
	private final long TIMESYNC_PERIOD = 120*1000;
	final boolean LPRS_MSGBUF_INCR = true;
	final boolean LPRS_MSGBUF_DECR = false;
	private int msgSerial;
	private int msgSerialBD;
	private boolean needRestart;
	private OcppServer ocppServer;
	private volatile boolean serverDown;
	private volatile boolean backdoorDown;
	private volatile boolean isLMSSuspended = false;
	private boolean isOperative;
	private OCPP.ReserveNow reservation;
//	private JsonObject serverReply;
	private JSONObject serverReply;
	private JSONObject backdoorReply;
//	private String socket;
//	private WebSocketClient socket;
//	private String backdoorSocket;
	private State status=State.Startup;			
	private State previousActiveState;
	private boolean criticalFaultTriggered=false;
	
	private LED statusLED;
	private Color ledColor = LEDConfig.UIOrange;	
	private ImgSend imgTx ; 
//	private JsonArray stopTransaction;
	private volatile JSONArray stopTransaction;
	private final ReadWriteLock stLock = new ReentrantReadWriteLock();
	private Map<String, Image> uiImage;
//	private JButton tsBtn1;
//	private JButton tsBtn2;
//	private JButton tsBtn3;
//	private JButton tsBtn4;
//	private JButton tsBtn5;
//	private JButton tsBtn6;
	
	private JButton addHour;
	private JButton minusHour;
	private JButton addMin;
	private JButton minusMin;
	private JButton startCharge;
	private JButton stopQueueing;
	private JButton fullyCharge;
	private JButton resetCharge;
	private JButton stopTimeCharge;
	
	private int selectTime = 0;
	
	private boolean tsBtnEna;
	
	private ChargingProfile maxProfile;
	private ChargingProfile defaultProfile;
	private ChargingProfile currentProfile;
	private ChargingProfile tempProfile;
	
	private DataListener configListener;	
//	private UnlockAndStop plugAndplayUnlock;
//	private int statusTextID;
	boolean plugAndChargeUnlock=false;
	boolean buttonUnlock=false;	
	private final ReadWriteLock remoteCall = new ReentrantReadWriteLock();
	private PeriodTimer lprsImgCapTimer;
//	private ImageTx imgSend;
	private int imgcnt=0;
	private String encryptedOCPPText = "";	
	private String encryptedConfig = "";
	private boolean isNewConfig = false;
	private String hashMd5;	
    // Webcam image;
	BufferedImage cameraImage;
	BufferedImage cameraFrame;
	// Powermeter
	private double lastEnergy;	
	private long lastMaterRead;
	private Long lastMaterValue;
	private double chargerVoltageA = 0;
	private double chargerVoltageB = 0;
	private double chargerVoltageC = 0;
	private double chargerCurrentA = 0;
	private double chargerCurrentB = 0;
	private double chargerCurrentC = 0;
	private double chargerEnergy = 0;
	private double chargerCurrentConsumption = 0.2;			// estimated current consumption of charger itself, not from power meter reading; can be adjusted in configuration menu  
	private double chargerTotalPowerFactor = 0;		
	
	private boolean keepOnCamCapture = true; // Update by cam capture thread, set to false if can't open cam number of times.	
	private int queueingNum = 0;
	private Date queueingTime = new Date();
	private boolean queueingMode = false;	
	private int selectInterval = 15;	
	private Calendar calendar = Calendar.getInstance();	
	private boolean isShowConfig = false;	
	private List<JSONArray> tempMeterValues = new ArrayList<>(); ;	
	private double maxDemand = 0;
	private Thread lockTestThread;
	private EStop estop = null;
	private GPIO estopStatus;
	private int fault=FaultState.NO_FAULT;
	private PowerSupplyCheck powerChk;
	private Thread cpErrorCheckThread;
	private PowerCheckListener powerCheckListener;	
	private ADE7854A meterIC;
	
/**********************************************************************************************************************/
/*********************************************  FUNCTION  *************************************************************/
/**********************************************************************************************************************/	
	// This is for commit test.
	/**
	 * @Function main()
	 * @param args
	 * @throws XPathExpressionException 
	 */
	public static void main(String[] args) {
		Logger.writeln("CS start");
		ChargingStation cs = new ChargingStation(new File(args[0]), 1);
		cs.start();
	}
	
	/**
	 * @Function ChargingStation()
	 * @param cfg,cid 
	 */
	
	public ChargingStation(File cfg, int cid){
		
		checkAlreadyRunning();
		lprsMsgBuf = new ArrayList<String>();
		boolean isConfigLatest = true;
		try {
			FileInputStream fis = new FileInputStream(new File("./config.default"));
			byte[] b = new byte[fis.available()];
			fis.read(b);
			fis.close();
			encryptedConfig = Base64.getEncoder().encodeToString(b);
			setHashMd5(new String(EncryptionFactory.decrypt(encryptKey, b)));
			if(!configinfo.defaultConfig.equals(new String(EncryptionFactory.decrypt(encryptKey, b)))) {
				isConfigLatest = false;
			}
		} catch (IOException e) {
			isConfigLatest = false;
		}
	
		try {
			if(!isConfigLatest) {
				FileOutputStream fos = new FileOutputStream(new File("./config.default"));
				fos.write(EncryptionFactory.encrypt(encryptKey, configinfo.defaultConfig.getBytes()));
				fos.close();
				JSONObject newConfig = new JSONObject(configinfo.defaultConfig);
				try {
					if(cfg.exists()) {
						FileInputStream fis = new FileInputStream(cfg);
						byte[] b = new byte[fis.available()];
						fis.read(b);
						fis.close();
						encryptedConfig = Base64.getEncoder().encodeToString(b);
						b = EncryptionFactory.decrypt(encryptKey, b);
						setHashMd5(new String(b));
						config = new JSONObject(new String(b));
						java.util.Set<String> keyset = config.keySet();
						
						for(String key : config.keySet()) {
							if(newConfig.has(key)) {
								JSONObject json = newConfig.getJSONObject(key);
								json.put("Value", config.getJSONObject(key).get("Value"));
								System.out.println(key + " updated to " + config.getJSONObject(key).get("Value"));
								newConfig.put(key, json);
							}
						}
					}

				} catch (JSONException e) {		
					e.printStackTrace();
				}
				fos = new FileOutputStream(cfg);
				fos.write(EncryptionFactory.encrypt(encryptKey, newConfig.toString().getBytes()));
				fos.close();
				Runtime.getRuntime().exec("sync");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			if(cfg.exists()) {
				FileInputStream fis = new FileInputStream(cfg);
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				encryptedConfig = Base64.getEncoder().encodeToString(b);
				b = EncryptionFactory.decrypt(encryptKey, b);
				setHashMd5(new String(b));
				config = new JSONObject(new String(b));
			}
		} catch (JSONException | IOException e) {
			config = new JSONObject(configinfo.defaultConfig);
		}
		
		//V0.44 add cipher
		// init cipher here
		try {
			String plainText = config.getJSONObject("Station Name").getString("Value") + "_" + OCPP_TEXT + System.currentTimeMillis();
			ClientEncryptUtil.setKEY_PUBLIC("./rsa_512.pub");
			encryptedOCPPText = ClientEncryptUtil.encryptWithBase64URLSafeString(plainText);
			encryptedOCPPText = URLEncoder.encode(encryptedOCPPText, "UTF-8");
			Logger.writeln("encryptedOCPPText"+ encryptedOCPPText);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			Logger.writeln("Error in Initialize encryptedOCPPText"+ e2);
		};		
		
		Logger.writeln("CST Charger: version "+ configinfo.sVersion+" + "+configinfo.sVersionSim);
		JSONObject ioSet = new JSONObject(configinfo.ioMap).getJSONObject(config.getJSONObject("Hardware Version").getString("Value"));
		
//		GPIO rcd = new GPIO(ioSet.getInt("RCD"), GPIO.PullMode.PULL_UP);
//		GPIO rcdRst = new GPIO(ioSet.getInt("RCDRST"), GPIO.State.HIGH);
//
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				while(true) {
//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//					}
//					if(rcd.isHigh()) {
//						System.out.println("RCD leak detected");
//						try {
//							Thread.sleep(5000);
//						} catch (InterruptedException e) {
//						}
//						rcdRst.setState(GPIO.LOW);
//						System.out.println("RCD resetted");
//						rcdRst.setState(GPIO.HIGH);
//					}
//				}
//			}
//			
//		}).start();
		Point p;
		needRestart = false;
		rebootTask();
			
		if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {

			meterIC = new ADE7854A(new SPI(new GPIO(ioSet.getInt("ADE7854A_CS"), GPIO.State.HIGH), 50000, SPI.MODE3), new GPIO(ioSet.getInt("ADE7854A_IRQ0"), GPIO.PullMode.PULL_UP), new GPIO(ioSet.getInt("ADE7854A_IRQ1"), GPIO.PullMode.PULL_UP), new GPIO(ioSet.getInt("ADE7854A_RST"), GPIO.State.HIGH));		
			meterIC.calibrate();
			Logger.writeln("ADE7854A contructed");				
			
			powerChk = new PowerSupplyCheck(new GPIO(ioSet.getInt("AC_OK"), GPIO.PullMode.NO_PULL), new GPIO(ioSet.getInt("12V_OK"), GPIO.PullMode.NO_PULL), contactor, charger, "2_5");
//			while(!powerChk.acReady()) {
//				try {
//					Thread.sleep(1000);
//					Logger.writeln("Input AC power is not ready at startup, system is waiting...");		
//				} catch (InterruptedException e) { 
//					
//				}
//			}
		
			
			while(!powerChk.dcOk()) {
				try {
					Thread.sleep(1000);
					Logger.writeln("DC +12V is not ok at startup, system is waiting...");		
				} catch (InterruptedException e) { 
					
				}
			}		
			
				
			
		}		
		
//		if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
//			meterIC = new ADE7854A(new SPI(new GPIO(ioSet.getInt("ADE7854A_CS"), GPIO.State.HIGH), 50000, SPI.MODE3), new GPIO(ioSet.getInt("ADE7854A_IRQ0"), GPIO.PullMode.PULL_UP), new GPIO(ioSet.getInt("ADE7854A_IRQ1"), GPIO.PullMode.PULL_UP), new GPIO(ioSet.getInt("ADE7854A_RST"), GPIO.State.HIGH));		
//			Logger.writeln("ADE7854A contructed");					
//		}			
		
		
		if(config.getJSONObject("Over-Current Protection").getString("Value").equals("Yes")) {
			//overCurrentChk = new OverCurrentCheck();
			overCurrentChk = new OverCurrentCheck(System.currentTimeMillis(), config.getJSONObject("Default Capacity (A)").getInt("Value"));
		}
		
		this.cid = cid;
		idleUnlockInterval = config.getJSONObject("Auto Unlock Interval (ms)").getInt("Value");
		unitPrice = 1.00;
		unitLength = 15;

		msgSerialBD = 10000;
		msgSerial = 0;
		msgSerialLprs = 0;
		serverReply = new JSONObject();
		backdoorReply = new JSONObject();
		timeout = config.getJSONObject("Network Timeout (ms)").getInt("Value");
		stopTransaction = new JSONArray();

//		haveSession();
		
		if(config.getJSONObject("CardReader Type").getString("Value").equals("CLRC663")) {
			if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {
				nfcReader = new CLRC663(new SPI(new GPIO(ioSet.getInt("CLRC663_CS"), GPIO.State.HIGH), 1000000, SPI.MODE0));
			} else {
				nfcReader = new CLRC663(new SPI(new GPIO(7, GPIO.State.HIGH), 1000000, SPI.MODE0));
			}
			
		} else {
			//nfcReader = new ACR122U();
			nfcReader = new ACM1252U(1,1350,200,50);
		}
		nfcListener = new DataListener() {

			@Override
			public void dataReceived(DataEvent evt) {
				final String cid = new String(evt.getData());
				Logger.writeln("NFC read " + cid);
				if(cid.equals("")) {
					return;
				}
				if(cid.equalsIgnoreCase(config.getJSONObject("Admin Card").getString("Value")) && status == State.Ready) {
					showConfig("Admin");
				} else {
					if(status == State.Authorize || isChargingSession()) {
						if(status == State.Authorize) {
							setState(State.Authorizing);
						}
						
						JSONArray reply;
						if(serverDown) {
							Logger.writeln("NFC read serverDown");
							reply = null;
//							return;
						} else {
							Logger.writeln("NFC try to send");
							reply = send(authorize(cid, OcppClient.NFC));
						}
						boolean selfAuth = false;
						
						if(reply == null) {
							selfAuth = true;
						} else {
							try {
								JSONObject json = reply.getJSONObject(2);
								if(json == null) {
									selfAuth = true;
								}
							} catch (JSONException e) {
								selfAuth = true;
							}
						}
						Logger.writeln("selfAuth: " + selfAuth);
						if(selfAuth) {
							if(isChargingSession()) {
								for (int i = 0; i < adminCard.length; ++i) {
									if(cid.equals(HexFactory.toString(adminCard[i], ""))) {
										new Thread(new Runnable() {
		
											@Override
											public void run() {
												stopTransaction(cid, OCPP.Reason.Other);
											}
		
										}).start();
		
										break;
									}
								}
							} else {
								authorizeConf(null);
							}
						} else {
							if(status == State.Authorizing || isChargingSession()) {
								authorizeConf(reply);
							} else {
								authorizeConf(null);
							}
						}
					}
				}
			}
		};
		nfcReader.addDataListener(nfcListener);

		if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_0")) {
			statusLED = new LED(new WS2812B(new SPI(0, 6400000, SPI.MODE3)), 16);
		} else if (config.getJSONObject("Hardware Version").getString("Value").equals("V1_6") ||config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") ){			
			statusLED = new LED(new WS2812B(new SPI(new GPIO(23, GPIO.State.HIGH),0, 3200000, SPI.MODE3)), config.getJSONObject("Total LEDs number").getInt("Value"));
		} else {
			statusLED = new LED(new WS2811(new SPI(0, 6400000, SPI.MODE3)), 115);
		}

		statusLED.setColor(ChargingStationUI.Orange);
		
		Logger.writeln("Setting up image");
		Dimension size = new Dimension(1024, 600);
		try {
			if(config.getJSONObject("Window").getString("Value").equals("Shrink")) {
				size = new Dimension(985, 560);
			}
		} catch (JSONException e) {
		}
		
		
		uiImage = new HashMap<String, Image>();
		File[] images = new File("./resources/" + config.getJSONObject("Authentication").getString("Value")).listFiles();
		if(images != null) {
			for(int i = 0; i < images.length; ++i) {
				if(images[i].isFile()) {
					String key = images[i].getName();
					key = key.substring(0,  key.lastIndexOf("."));
					uiImage.put(key, Toolkit.getDefaultToolkit().getImage(images[i].getPath()));
				}
			}
		}
		images = new File("./resources/CST").listFiles();
		for(int i = 0; i < images.length; ++i) {
			if(images[i].isFile()) {
				String key = images[i].getName();
				key = key.substring(0,  key.lastIndexOf("."));
				if(!uiImage.containsKey(key)) {
					uiImage.put(key, Toolkit.getDefaultToolkit().getImage(images[i].getPath()));
				}
			}
		}
		
		Logger.writeln("Setting up view");
		try {
			if(config.getJSONObject("Window").getString("Value").equals("Shrink")) {
				view = new ChargingStationUI(new Point(8, 25), size);
			} else {
				view = new ChargingStationUI(new Point(0, 0), size);
			}
		} catch (JSONException e) {
			view = new ChargingStationUI(new Point(0, 0), size);
		}
		
		//view.showInfo(sVersionSim, Color.BLACK, new Font("Arial", Font.PLAIN, 36));
		view.showInfo(configinfo.sVersion + "(" + configinfo.sVersionSim + ")", Color.BLACK, new Font("Arial", Font.PLAIN, 72));

		if(config.getJSONObject("Clock").getString("Value").equals("Show"))
		new Thread(new Runnable() {
			DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			@Override
			public void run() {
				while(true) {
					int offset = config.getJSONObject("Time Zone").getInt("Value");
					dateTime.setTimeZone(TimeZone.getTimeZone(ZoneId.of((offset >= 0 ? "+" : "") + offset)));
					view.showTime(dateTime.format(new Date()));
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Logger.writeln("dateTime: " + e.getMessage());
					}
				}
			}

			
		}).start();
		configSecret = new StringBuffer();
		
		configListener = new DataListener() {

			@Override
			public void dataReceived(DataEvent evt) {
				JSONObject event = new JSONObject(new String(evt.getData()));
				Logger.writeln("configListener: " + event.toString(2));
				String[] names = JSONObject.getNames(event);
				for(int i = 0; i < names.length; ++i) {
					if(config.has(names[i])) {
						JSONObject json = config.getJSONObject(names[i]);
						json.put("Value", event.get(names[i]));
						config.put(names[i], json);
					} else if(names[i].equals("Add")) {	
						ocppServer.addCard(event.getString(names[i]));
						view.updateConfig(getConfiguration());
					} else if(names[i].equals("Remove")) {	
						ocppServer.removeCard(event.getString(names[i]));
						view.updateConfig(getConfiguration());
					} else if(names[i].equals("Remove All")) {	
						ocppServer.setAuthorizedList(new JSONObject());
						view.updateConfig(getConfiguration());
					} else if(names[i].equals("Restart")) {
						needRestart = true;
					} else if(names[i].equals("Reset")) {	
						record.delete();
						session.delete();
//						meterReading.delete();
//						meterReadingBak.delete();
						if(ocppServer != null) {
							ocppServer.reset();
						}
					} else if(names[i].equals("Clear Log")) {	
						ocppServer.clearRecord();
					} else if(names[i].equals("Register")) {
						registerDevice();
					} else if(names[i].equals("DateTime")) {
						setSystemTime(event.getString("DateTime"));
					} else {
						NetworkInterface[] networkIface = NetworkManager.getPhysicalInterface();
						for(int j = 0; j < networkIface.length; ++j) {
							if(names[i].equals(networkIface[j].getName())) {
								JSONObject param = event.getJSONObject(names[i]);
								networkIface[j].setIP(param.getBoolean("DHCP"), param.getString("IP"), param.getString("Netmask"), param.getString("Gateway"));
								if(networkIface[j].isWireless()) {
									networkIface[j].setSSID(param.getString("SSID"), param.getString("Password"));
								}
							}
						}
					}
				}
				try {
					FileOutputStream fos = new FileOutputStream(cfg);
					fos.write(EncryptionFactory.encrypt(encryptKey, config.toString().getBytes()));
					fos.close();
					Runtime.getRuntime().exec("sync");  
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		};
		
		view.addMouseListener(new MouseAdapter() {
			
			private Point lastMousePressed = new Point(0, 0);

			@Override
			public void mousePressed(MouseEvent arg0) {
				double deltaX = arg0.getX() - lastMousePressed.getX();
				double deltaY = arg0.getY() - lastMousePressed.getY();
				lastMousePressed = new Point(arg0.getX(), arg0.getY());
				if(deltaX == 0) {
					deltaX = 1;
				}
				double theta = Math.toDegrees(Math.atan(deltaY / deltaX));
				if(deltaX < 0) {
					theta -= 180;
				} else if(deltaX > 0 && deltaY > 0) {
					theta -= 360;
				}
				theta *= -1;
				int quadrant = ((int)(theta + 22.5) / 45) % 8;
				configSecret.append(quadrant);
				try {
					if(configSecret.length() >= 13 && configSecret.substring(configSecret.length() - 13, configSecret.length() - 10).equals("460") && configSecret.subSequence(configSecret.length() - 9, configSecret.length() - 4).equals("46064") && configSecret.substring(configSecret.length() - 3, configSecret.length()).equals("046")) {						
						showConfig("Admin");
					} else if(configSecret.length() >= 9 && configSecret.substring(configSecret.length() - 9, configSecret.length() - 6).equals("460") && configSecret.subSequence(configSecret.length() - 5, configSecret.length()).equals("05036")) {
						showConfig("Everyone");
					}
				} catch (IndexOutOfBoundsException e) {	
				}
  				while(configSecret.length() >= 20) {
					configSecret.deleteCharAt(0);
				}
			}	
		});
		
		//lprsImgCapTimer = new PeriodTimer(0, 200);
		//lprsImgCapTimer = new PeriodTimer(0, 1000);
		lprsImgCapTimer = new PeriodTimer(0, 33);
		
		view.showImage(uiImage.get("Initialize"));
		if(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/")) {
			Logger.writeln("Setting up OCPP server");		
			p = new Point(100, 100);	
			ocppServer = new OcppServer(p, false, new File("."), config);
			
			if(config.getJSONObject("Authentication").getString("Value").equals("Key")) {
				startKey = new GPIO(ioSet.getInt("Input4"), GPIO.PullMode.PULL_UP);
				startKey.addActionListener(new ActionListener() {
		
					@Override
					public void actionPerformed(ActionEvent evt) {
						if(((GPIO)evt.getSource()).isLow()) {
							ocppServer.remoteStartTransaction();
						} else {
							ocppServer.remoteStopTransaction();
						}
					}
					
				});
			}
		}
		
		try{
		selectInterval = config.getJSONObject("Time Select Interval Min").getInt("Value");
	    }catch(JSONException ex){
	        Logger.writeln(ex);
	        config.put("Time Select Interval Min", new JSONObject("{\"Type\":\"Range\",\"Authority\":\"Admin\",\"Value\":15,\"Option\":[1,30,5]}"));
	    }
		try {
			config.getJSONObject("Custom Fields").getString("Value");
		}catch(JSONException ex) {
	        Logger.writeln(ex);
	        config.put("Custom Fields", new JSONObject("{\"Type\":\"Selection\",\"Authority\":\"Admin\",\"Value\":\"Yes\",\"Option\":[\"Yes\",\"No\"]}"));
		}
		try {
			config.getJSONObject("Enable CPError").getString("Value");
		}catch(JSONException ex) {
	        Logger.writeln(ex);
	        config.put("Enable CPError", new JSONObject("{\"Type\":\"Selection\",\"Authority\":\"Admin\",\"Value\":\"Yes\",\"Option\":[\"Yes\",\"No\"]}"));
		}
		
//		keyLed = new GPIO(ioSet.getInt("Output3"), GPIO.State.LOW);
//		if(config.getJSONObject("Type").getString("Value").equals("Cable")) {
//			lockKey = new GPIO(ioSet.getInt("Input3"), GPIO.PullMode.PULL_UP);
//			lockKey.addActionListener(new ActionListener() {
//	
//				@Override
//				public void actionPerformed(ActionEvent evt) {
//					boolean keyState = ((GPIO)evt.getSource()).isLow();
//					Logger.writeln("Lock key switched");
//					charger.setCableLock(keyState);
//				}
//				
//			});
//			keyLed.setState(lockKey.isHigh() ? GPIO.HIGH : GPIO.LOW);
//			keyLed.setState(GPIO.HIGH);
//		}
//		
		
		//2020-05-26 for re-enable "STOP" screen button start here
	//	if(config.getJSONObject("Image").getString("Value").equals("SmartCharge")) {

//20200728 remove unlock button for LPRS		
		if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {

			try{

//				plugAndplayUnlock = new UnlockAndStop();
//				Thread checkPlugAndPlayUnlock = new Thread(plugAndplayUnlock);
//				try {
//					checkPlugAndPlayUnlock.start();
//				}catch(Exception e) {
//					Logger.writeln("Error in Initialize plugAndplayUnlock");
//				}				
				
				unlockButton = new SoftButton();
				JButton unlockJButton = new JButton(new ImageIcon("./resources/CST/UnlockButton.png"));
				unlockJButton.setPressedIcon(new ImageIcon("./resources/CST/UnlockButtonPressed.png"));	
				unlockJButton.setContentAreaFilled(false);
				unlockJButton.setBorderPainted(false);
				unlockJButton.setBounds(view.getUIConfig("ui_config/unlockJButton/x"), view.getUIConfig("ui_config/unlockJButton/y"), view.getUIConfig("ui_config/unlockJButton/w"), view.getUIConfig("ui_config/unlockJButton/h"));
					
				unlockButton.setSoftButton(unlockJButton);						
				view.add(unlockButton.getSoftButton(),0);
				view.revalidate();
		
				unlockButton.getSoftButton().addActionListener(new ActionListener() {
				
					@Override
					public void actionPerformed(ActionEvent evt) {
						//Logger.writeln("StopTransaction by unlock button");
						//stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Local)));
						
						//plugAndplayUnlock.setNewAction(true);
						if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
							contactor.setState(false);
						}else{
							if(contactor.getState()) {
								contactor.setState(false);
							}							
						}
						plugAndChargeUnlock = true;
						charger.setCableLock(false);
					}
	
				});
				unlockButton.setVisible(false);		
			
			}catch (Exception e) {
				Logger.writeln("Error init STOP button");	
			}
						
			
		}else {
			try{
		
				unlockButton = new SoftButton();
				JButton unlockJButton = new JButton(new ImageIcon("./resources/CST/UnlockButton.png"));
				unlockJButton.setPressedIcon(new ImageIcon("./resources/CST/UnlockButtonPressed.png"));
				unlockJButton.setContentAreaFilled(false);
				unlockJButton.setBorderPainted(false);
				unlockJButton.setBounds(view.getUIConfig("ui_config/unlockJButton/x"), view.getUIConfig("ui_config/unlockJButton/y"), view.getUIConfig("ui_config/unlockJButton/w"), view.getUIConfig("ui_config/unlockJButton/h"));
					
				unlockButton.setSoftButton(unlockJButton);						
				view.add(unlockButton.getSoftButton(),0);
				view.revalidate();
		
				unlockButton.getSoftButton().addActionListener(new ActionListener() {
				
					@Override
					public void actionPerformed(ActionEvent evt) {
						//Logger.writeln("Unlock by STOP button");
						buttonUnlock=true;
						charger.setCableLock(false);
					}
	
				});
				unlockButton.setVisible(false);		


			
			}catch (Exception e) {
				Logger.writeln("Error init STOP button");	
			}
		//2020-05-26 for re-enable "STOP" screen button	end here
	}
		
		
		p = new Point(0, 0);
		if(config.getJSONObject("UART Port").getString("Value").equals("RS232")) {
			SerialAdapter barcodeSerial = new SerialAdapter("/dev/ttyS0", 9600, p, false);
			barcodeSerial.setName("barcode");
			barcode = new DS9208(barcodeSerial);
			barcodeListener = new DataListener() {
	
				@Override
				public void dataReceived(DataEvent evt) {
					Logger.writeln("Barcode read " + new String(evt.getData()));
					if(new String(evt.getData()).equals("")) {
						return;
					}
					if(status == State.Authorize) {
						setState(State.Authorizing);
					}
					if(serverDown) {
						Logger.writeln("Self authorize @ no reply");
						authorizeConf(null);
					} else {
						authorizeConf(send(authorize(new String(evt.getData()), OcppClient.QRCODE)));
					}
				}
	
			};
		}
		
		Logger.writeln("Setting up EVSE");
		try {
			chargerCurrentConsumption = ((double)config.getJSONObject("Charger current consumption(mA)").getInt("Value"))/1000;
			Logger.writeln("Charger current consupmtion setting is :" +chargerCurrentConsumption);
			int overcurrentMargin = config.getJSONObject("Over-Current Margin(%)").getInt("Value");
			Logger.writeln("Over current margin is :" +overcurrentMargin);
		}catch (Exception e) {
			Logger.writeln("Error in getting Charger current consupmtion & overcurrent margin in configuration!");
		}		
		
		PWMPort cp = null;
		GPIO lock = null, lock2 = null, lockState = null;		
		try {
			GPIO[] contactorControl;
			if(config.getJSONObject("Hardware Version").getString("Value").equals("V1_5") || config.getJSONObject("Hardware Version").getString("Value").equals("V1_6")) {
				GPIO enableL1 = new GPIO(ioSet.getInt("EnableL1"), GPIO.State.LOW);
				GPIO enableN = new GPIO(ioSet.getInt("EnableN"), GPIO.State.LOW);
				GPIO enableL2L3 = new GPIO(ioSet.getInt("EnableL2L3"), GPIO.State.LOW);
			//	GPIO enable31 = new GPIO(ioSet.getInt("Enable31"), GPIO.State.LOW);				

				contactorControl= new GPIO[3];
				contactorControl[0] = enableN;
				contactorControl[1] = enableL1;
				contactorControl[2] = enableL2L3;
				//contactorControl[3] = enable31;
				
				boolean[] contactorMode = new boolean[3];
				switch(config.getJSONObject("Phase").getString("Value")) {
				case "3 Phase":
					Logger.writeln("Setting up 3 phase contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					contactorMode[2] = true;
				//	contactorMode[3] = false;
					break;
				case "1 Phase (L3)":
					Logger.writeln("Setting up 1 phase (L3) contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					contactorMode[2] = false;
					//contactorMode[3] = false;
					break;
				case "1 Phase (L2)":
					Logger.writeln("Setting up 1 phase (L2) contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					contactorMode[2] = false;
					//contactorMode[3] = false;
					break;
				case "1 Phase (L1)":
				default:
					Logger.writeln("Setting up 1 phase (L1) contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					contactorMode[2] = false;
					//contactorMode[3] = false;
					break;
				}
				contactor = new Contactor(contactorControl, contactorMode, 1000);
				
				new Thread(new Runnable() {

					@Override
					public void run() {
						contactor.setState(true);
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							Logger.writeln("contactor: " + e.getMessage());
						}
						contactor.setState(false);
					}
					
				}).start();				
				
			}else if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {
				GPIO rl1_en = new GPIO(ioSet.getInt("RL1_EN"), GPIO.State.LOW);
				GPIO rl2_en = new GPIO(ioSet.getInt("RL2_EN"), GPIO.State.LOW);		

				contactorControl= new GPIO[2];
				contactorControl[0] = rl1_en;				// RL1_EN must be at index 0 for maintaining on/off sequence
				contactorControl[1] = rl2_en;				// RL2_EN must be at index 1 for maintaining on/off sequence
				
				boolean[] contactorMode = new boolean[2];
				switch(config.getJSONObject("Phase").getString("Value")) {
				case "3 Phase":
					Logger.writeln("Setting up 3 phase contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					break;
				case "1 Phase (L3)":
					Logger.writeln("Setting up 1 phase (L3) contactor");
					contactorMode[0] = true;
					contactorMode[1] = false;
					break;
				case "1 Phase (L2)":
					Logger.writeln("Setting up 1 phase (L2) contactor");
					contactorMode[0] = true;
					contactorMode[1] = true;
					break;
				case "1 Phase (L1)":
				default:
					Logger.writeln("Setting up 1 phase (L1) contactor");
					contactorMode[0] = true;
					contactorMode[1] = false;
					break;
				}
				//contactor = new Contactor(contactorControl, contactorMode, 1000, Contactor.CONTROL_SCHEME_V2);				
				GPIO[] contReadIO = new GPIO[2];
				contReadIO[0] = new GPIO(ioSet.getInt("RL1_STATUS"), GPIO.PullMode.NO_PULL);
				contReadIO[1] = new GPIO(ioSet.getInt("RL2_STATUS"), GPIO.PullMode.NO_PULL);
				contactor = new Contactor(contactorControl, contReadIO, contactorMode, 1000, Contactor.CONTROL_SCHEME_V2);

/*	//remove power on on-off test for aligning IEC 61851 requirement			
				new Thread(new Runnable() {

					@Override
					public void run() {
						int numOfPhase = 1;
						int retrial=0;
						int maxRetrial = 2;
						if(config.getJSONObject("Phase").getString("Value").equals("3 Phase")) {
							numOfPhase = 3;
						}else {
							numOfPhase = 1;
						}
						
						contactor.setState(true);	
						Logger.writeln("Power up turn ON contactor") ;
//						retrial = 2;
//						while(!contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase) && (retrial<maxRetrial)) {
//							try {
//								Thread.sleep(1000);
//							} catch (InterruptedException e) {
//							}	
//							contactor.setState(true);
//							retrial++;
//							Logger.writeln("Power up closing contactor fail, retrial no. "+retrial);
//							
//						}
//						if(!contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase)) {
//								Logger.writeln("Power up closing contactor failure..");
//								contactor.setOperationNormal(false);
//						}					
						
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						contactor.setState(false);
						Logger.writeln("Power up turn OFF contactor") ;
						
//						retrial = 2;
//						while(contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase) && (retrial<maxRetrial)) {
//							try {
//								Thread.sleep(1000);
//							} catch (InterruptedException e) {
//							}	
//							contactor.setState(false);
//							retrial++;
//							Logger.writeln("Power up open contactor fail, retrial no. "+retrial);
//						}
//						if(contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase)) {
//								Logger.writeln("Power up open contactor failure..");
//								contactor.setOperationNormal(false);
//						}					
					}
					
				}).start();				
*/				
			} else {
				
				GPIO enable1 = new GPIO(ioSet.getInt("Enable1"), GPIO.State.LOW);
				GPIO enable2 = new GPIO(ioSet.getInt("Enable2"), GPIO.State.LOW);
				GPIO enable21 = new GPIO(ioSet.getInt("Enable21"), GPIO.State.LOW);
				GPIO enable31 = new GPIO(ioSet.getInt("Enable31"), GPIO.State.LOW);
				Logger.writeln("Hardware version: "+config.getJSONObject("Hardware Version").getString("Value"));
				Logger.writeln("IO pin enable1 : "+ioSet.getInt("Enable1"));
				Logger.writeln("IO pin enable2 : "+ioSet.getInt("Enable2"));
				Logger.writeln("IO pin enable21 : "+ioSet.getInt("Enable21"));
				Logger.writeln("IO pin enable31 : "+ioSet.getInt("Enable31"));
				
				if(config.getJSONObject("Hardware Version").getString("Value").equals("V0_3")) {
					GPIO enable3 = new GPIO(ioSet.getInt("Enable3"), GPIO.State.LOW);
					contactorControl= new GPIO[5];
					contactorControl[0] = enable21;
					contactorControl[1] = enable31;
					contactorControl[2] = enable1;
					contactorControl[3] = enable2;
					contactorControl[4] = enable3;
					
					boolean[] contactorMode = new boolean[5];
					switch(config.getJSONObject("Phase").getString("Value")) {
					case "3 Phase":
						Logger.writeln("Setting up 3 phase contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = true;
						contactorMode[4] = true;
						break;
					case "1 Phase (L3)":
						Logger.writeln("Setting up 1 phase (L3) contactor");
						contactorMode[0] = false;
						contactorMode[1] = true;
						contactorMode[2] = false;
						contactorMode[3] = false;
						contactorMode[4] = false;
						break;
					case "1 Phase (L2)":
						Logger.writeln("Setting up 1 phase (L2) contactor");
						contactorMode[0] = true;
						contactorMode[1] = false;
						contactorMode[2] = false;
						contactorMode[3] = false;
						contactorMode[4] = false;
						break;
					case "1 Phase (L1)":
					default:
						Logger.writeln("Setting up 1 phase (L1) contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = false;
						contactorMode[4] = false;
						break;
					}
					contactor = new Contactor(contactorControl, contactorMode, 1000);
	
					new Thread(new Runnable() {
	
						@Override
						public void run() {
							contactor.setState(true);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Logger.writeln("contactor: " + e.getMessage());
							}
							contactor.setState(false);
						}
						
					}).start();
				} else if(config.getJSONObject("Hardware Version").getString("Value").equals("V1_0")) {
					contactorControl= new GPIO[4];
					contactorControl[0] = enable21;
					contactorControl[1] = enable31;
					contactorControl[2] = enable1;
					contactorControl[3] = enable2;
					
					boolean[] contactorMode = new boolean[4];
					switch(config.getJSONObject("Phase").getString("Value")) {
					case "3 Phase":
						Logger.writeln("Setting up 3 phase contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = true;
						break;
					case "1 Phase (L3)":
						Logger.writeln("Setting up 1 phase (L3) contactor");
						contactorMode[0] = false;
						contactorMode[1] = true;
						contactorMode[2] = false;
						contactorMode[3] = false;
						break;
					case "1 Phase (L2)":
						Logger.writeln("Setting up 1 phase (L2) contactor");
						contactorMode[0] = true;
						contactorMode[1] = false;
						contactorMode[2] = false;
						contactorMode[3] = false;
						break;
					case "1 Phase (L1)":
					default:
						Logger.writeln("Setting up 1 phase (L1) contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = false;
						break;
					}
					contactor = new Contactor(contactorControl, contactorMode, 1000);
					
					new Thread(new Runnable() {
	
						@Override
						public void run() {
							contactor.setState(true);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								Logger.writeln("contactor: " + e.getMessage());
							}
							contactor.setState(false);
						}
						
					}).start();
				}else {
					contactorControl= new GPIO[4];
					contactorControl[0] = enable21;
					contactorControl[1] = enable31;
					contactorControl[2] = enable1;
					contactorControl[3] = enable2;
					
					boolean[] contactorMode = new boolean[4];
					switch(config.getJSONObject("Phase").getString("Value")) {
					case "3 Phase":
						Logger.writeln("Setting up 3 phase contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = true;
						break;
					case "1 Phase (L3)":
						Logger.writeln("Setting up 1 phase (L3) contactor");
						contactorMode[0] = false;
						contactorMode[1] = true;
						contactorMode[2] = false;
						contactorMode[3] = false;
						break;
					case "1 Phase (L2)":
						Logger.writeln("Setting up 1 phase (L2) contactor");
						contactorMode[0] = true;
						contactorMode[1] = false;
						contactorMode[2] = false;
						contactorMode[3] = false;
						break;
					case "1 Phase (L1)":
					default:
						Logger.writeln("Setting up 1 phase (L1) contactor");
						contactorMode[0] = false;
						contactorMode[1] = false;
						contactorMode[2] = true;
						contactorMode[3] = false;
						break;
					}
					contactor = new Contactor(contactorControl, contactorMode, 0);
				}
				
			}
			
//			if(config.getJSONObject("Type").getString("Value").equals("Socket")) {
				lock = new GPIO(ioSet.getInt("Lock"), GPIO.State.LOW);
//			} else {
//				lock = new GPIO(ioSet.getInt("Lock"), lockKey.isLow() ? GPIO.State.HIGH : GPIO.State.LOW);
//			}
			if((config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )){
				lock2 = new GPIO(ioSet.getInt("Lock2"), GPIO.State.LOW);
				lockState = new GPIO(ioSet.getInt("LockState"), GPIO.PullMode.NO_PULL);
				Logger.writeln("Lock2 GPIO initialized");
			}else {
				lockState = new GPIO(ioSet.getInt("LockState"), GPIO.PullMode.PULL_UP);
			}

			if((config.getJSONObject("Hardware Version").getString("Value").equals("V1_0") )|| (config.getJSONObject("Hardware Version").getString("Value").equals("V1_5")) || config.getJSONObject("Hardware Version").getString("Value").equals("V1_6") || (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) ) {
				lockState.setMode(GPIO.INVERTED);
				Logger.writeln("Lock state GPIO logic set inverted");
			}

			cp = new PWMPort(ioSet.getInt("CP"));
//			PWMPort pwm1 = new PWMPort(1);
//	    	pwm1.setFrequency(1000);
//			pwm1.setDuty(75);
		} catch(GpioPinExistsException e) {
			Logger.writeln("Contactor GPIO error");
		}
		
		OCPP.ChargingSchedulePeriod[] schedulePeriods = new OCPP.ChargingSchedulePeriod[1];
		switch(config.getJSONObject("Phase").getString("Value")) {
		case "3 Phase":
			schedulePeriods[0] = new OCPP.ChargingSchedulePeriod(0, ((float)config.getJSONObject("Default Capacity (A)").getInt("Value")), 3);
			break;
		case "1 Phase (L3)":
			schedulePeriods[0] = new OCPP.ChargingSchedulePeriod(0, ((float)config.getJSONObject("Default Capacity (A)").getInt("Value")), 1);
			break;
		case "1 Phase (L2)":
			schedulePeriods[0] = new OCPP.ChargingSchedulePeriod(0, ((float)config.getJSONObject("Default Capacity (A)").getInt("Value")), 1);
			break;
		case "1 Phase (L1)":
		default:
			schedulePeriods[0] = new OCPP.ChargingSchedulePeriod(0, ((float)config.getJSONObject("Default Capacity (A)").getInt("Value")), 1);
			break;
		}
		defaultProfile = new OCPP.ChargingProfile(1, 0, OCPP.ChargingProfilePurposeType.TxDefaultProfile, OCPP.ChargingProfileKindType.Absolute, new OCPP.ChargingSchedule(OCPP.ChargingRateUnitType.W, schedulePeriods));
		currentProfile = defaultProfile;

		Logger.writeln("Setting up ADC");
		ADCDevice adc;	
//		Operation[] cpScaler = new Operation[2];
		if(config.getJSONObject("Hardware Version").getString("Value").equals("V0_3")) {
			Operation[] cpScaler = new Operation[2];
			adc = new AD7888(new SPI(new GPIO(ioSet.getInt("ADC"), GPIO.State.HIGH), 2000000, SPI.MODE3), 3400);
			cpScaler[0] = new Operation("*", 8);
			cpScaler[1] = new Operation("-", 12000);
			adc.setScaler(ioSet.getInt("ADC_CP"), new Scaler(cpScaler));			
		} else if((config.getJSONObject("Hardware Version").getString("Value").equals("V1_0")) || (config.getJSONObject("Hardware Version").getString("Value").equals("V1_5") || config.getJSONObject("Hardware Version").getString("Value").equals("V1_6"))) {
			Operation[] cpScaler = new Operation[2];
			adc = new MCP3208(new SPI(new GPIO(ioSet.getInt("ADC"), GPIO.State.HIGH), 2000000, SPI.MODE3), 5000);
			cpScaler[0] = new Operation("*", -8);
			cpScaler[1] = new Operation("+", 12000);
			adc.setScaler(ioSet.getInt("ADC_CP"), new Scaler(cpScaler));
		} else if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {
			//Operation[] cpScaler = new Operation[3];
			Operation[] cpScaler = new Operation[4];		// To be resumed to array size of 3 for final hardware!!
			adc = new MCP3208(new SPI(new GPIO(ioSet.getInt("ADC"), GPIO.State.HIGH), 2000000, SPI.MODE3), 5000);
			/*
			cpScaler[0] = new Operation("*", 60);
			cpScaler[1] = new Operation("-", 150000);
			cpScaler[2] = new Operation("/", 11);
			cpScaler[3] = new Operation("+", 1010);		// add for first PCBA sample only!! to be removed for final hardware!!
*/
			/*
			cpScaler[0] = new Operation("*", 5058);
			cpScaler[1] = new Operation("/", 1000);
			cpScaler[2] = new Operation("-", 12484);
			cpScaler[3] = new Operation("+", 0);	
			*/
			cpScaler[0] = new Operation("*", 1);
			cpScaler[1] = new Operation("*", 1);
			cpScaler[2] = new Operation("*", 1);
			cpScaler[3] = new Operation("*", 1);			
			
			
			adc.setScaler(ioSet.getInt("ADC_CP"), new Scaler(cpScaler));

		} else {
			Operation[] cpScaler = new Operation[2];			
			adc = new MCP3208(new SPI(new GPIO(ioSet.getInt("ADC"), GPIO.State.HIGH), 2000000, SPI.MODE3), 5000);
			cpScaler[0] = new Operation("*", -5.10638);
			cpScaler[1] = new Operation("+", 12804);
			adc.setScaler(ioSet.getInt("ADC_CP"), new Scaler(cpScaler));
		}
//		adc.setScaler(ioSet.getInt("ADC_CP"), new Scaler(cpScaler));

		Operation[] ppScaler = new Operation[4];
		ppScaler[0] = new Operation("max", 1);
		ppScaler[1] = new Operation("1/x", ioSet.getInt("ADC_REFERENCE"));
		ppScaler[2] = new Operation("-", 1);
		ppScaler[3] = new Operation("1/x", ioSet.getInt("PROXIMITY_PULLUP"));
		adc.setScaler(ioSet.getInt("ADC_PP"), new Scaler(ppScaler));

		Operation[] tempScaler = new Operation[4];
		tempScaler[0] = new Operation("max", 1);
		tempScaler[1] = new Operation("1/x", 5000);
		tempScaler[2] = new Operation("-", 1);
		tempScaler[3] = new Operation("1/x", 3000);
		adc.setScaler(ioSet.getInt("ADC_TEMP"), new Scaler(tempScaler));		
		JT103 = new NTCThermister(adc.getADCChannel(ioSet.getInt("ADC_TEMP")), 10000, 3435);

		Logger.writeln("Setting up power meter");
		double meterInitial = 0;
		if(meterReading.exists()) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(meterReading);
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				meterInitial = Double.parseDouble(new String(b));
			} catch (IOException e) {
			} catch (NumberFormatException e) {
				try {
					fis = new FileInputStream(meterReadingBak);
					byte[] b = new byte[fis.available()];
					fis.read(b);
					fis.close();
					meterInitial = Double.parseDouble(new String(b));
				} catch(IOException e1) {
				}
			}
		}
		PowerMeter powerMeter = null;
		if(config.getJSONObject("Powermeter Type").getString("Value").equals("ADE7754")) {
			powerMeter = new ADE7754(new SPI(new GPIO(ioSet.getInt("PowerMeter"), GPIO.State.HIGH), 1000000, SPI.MODE1), JT103, (1000000 + 1000) / 1000, (100000 + 24000) / 24000, meterInitial);
			powerMeter.calibrate();
		}else if (config.getJSONObject("Powermeter Type").getString("Value").equals("ADE7854A")) {
			 powerMeter = meterIC.getMeter();
		} else {
			SerialAdapter serial;
			if( config.getJSONObject("Hardware Version").getString("Value").equals("V1_6") || config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {
				if(config.getJSONObject("Offboard Powermeter Link").getString("Value").equals("RS485")) {
					serial = new SerialAdapter("/dev/ttyS0", 9600, new Point(0, 0), false, true, new GPIO(ioSet.getInt("RS485_DIR"), GPIO.State.LOW), true);
				}else {
					serial = new SerialAdapter("/dev/ttyUSB0", 9600);
					Logger.write("USB to serial adaptor enabled");
				}
			} else if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_0")) {
				if(config.getJSONObject("Offboard Powermeter Link").getString("Value").equals("RS485")) {
					serial = new SerialAdapter("/dev/ttyS0", 9600);
				}else {
					serial = new SerialAdapter("/dev/ttyUSB0", 9600);
				}
			} else {
				serial = new SerialAdapter("/dev/ttyUSB0", 9600);
			}
			if(config.getJSONObject("Powermeter Type").getString("Value").equals("SPM91")) {
				powerMeter = new SPM91(serial, Integer.parseInt(config.getJSONObject("Powermeter ID").getString("Value")));
			} else if(config.getJSONObject("Powermeter Type").getString("Value").equals("SPM93")) {
				powerMeter = new SPM93(serial, Integer.parseInt(config.getJSONObject("Powermeter ID").getString("Value")));
			}
//			powerMeter2 = new ADE7754(new SPI(new GPIO(ioSet.getInt("PowerMeter"), GPIO.State.HIGH), 1000000, SPI.MODE1), JT103, (1000000 + 1000) / 1000, (100000 + 24000) / 24000, meterInitial);
//			new Timer().schedule(new TimerTask() {
//			
//				@Override
//				public void run() {
//					powerMeter2.calibrate();
//				}
//			
//			}, 2000);
		}
		
		if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {	
			charger = new IECCharger(powerMeter, config.getJSONObject("Type").getString("Value").equals("Socket") ? IECCharger.SOCKET : IECCharger.CABLE,
					cp,	adc.getADCChannel(ioSet.getInt("ADC_CP"), ADCChannel.ALGO_FILTER1), adc.getADCChannel(ioSet.getInt("ADC_PP"), ADCChannel.ALGO_ADDSERIAL),
					lock, lock2, lockState, LOCKVER, config.getJSONObject("Maximum Capacity (A)").getInt("Value"), IECCharger.CP_IDLE_TYPE2, 10);
			Logger.writeln("V2.5 charger initialized");
			
		}else {		
				charger = new IECCharger(powerMeter, config.getJSONObject("Type").getString("Value").equals("Socket") ? IECCharger.SOCKET : IECCharger.CABLE,
						cp,	adc.getADCChannel(ioSet.getInt("ADC_CP")), adc.getADCChannel(ioSet.getInt("ADC_PP")),
						lock, lockState, config.getJSONObject("Maximum Capacity (A)").getInt("Value"));
		}


		if((config.getJSONObject("Hardware Version").getString("Value").equals("V2_5"))) {
			GPIO estopGpio[] = new GPIO[1];
			estopGpio[0] = new GPIO(ioSet.getInt("ESTOP"), GPIO.PullMode.NO_PULL);
			estop = new EStop(estopGpio);
			//estopStatus = new GPIO(ioSet.getInt("ESTOP"), GPIO.State.HIGH);
			estopStatus = estopGpio[0];
			Logger.writeln("ESTOP GPIO initialized");
			
			
			if(estopStatus.isLow()) {
				Logger.writeln("ESTOP triggered detected at started up");
				int numOfPhase=1;
				fault = FaultState.ESTOP_TRIGGERED;
				estop.setTriggerState(true);
				if(config.getJSONObject("Phase").getString("Value").equals("3 Phase")) {
					numOfPhase = 3;
				}else {
					numOfPhase = 1;
				}
				
				contactor.setState(false);
//				if(contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase)) {
//					Logger.writeln("ESTOP triggered while contactors detected still closed, try re-open them again on more time..");
//					contactor.setState(false);
//					contactor.setOperationNormal(false);
//				}
				
				if(charger.isLocked()) {
					Logger.writeln("ESTOP triggered at startup and cable locked, open IEC lock");
					view.showImage(uiImage.get("Emergency_button_pressed_halt"));
					charger.setCableLock(false);
				}else {
					criticalFaultTriggered = true;
					Logger.writeln("ESTOP triggered, originally unlocked and no unlock action");
					view.showImage(uiImage.get("EStopHalt"));
				}
				//view.showImage(uiImage.get("Emergency_button_pressed_halt"));						
			}			
			
			estopStatus.addActionListener(new ActionListener() {
	    		
				@Override
				public void actionPerformed(ActionEvent evt) {
					Logger.writeln("ESTOP triggered detected");
					if(estopStatus.isLow()) {
						int numOfPhase=1;
						fault = FaultState.ESTOP_TRIGGERED;
						estop.setTriggerState(true);
						if(config.getJSONObject("Phase").getString("Value").equals("3 Phase")) {
							numOfPhase = 3;
						}else {
							numOfPhase = 1;
						}
						
						contactor.setState(false);
//						if(contactor.getState(Contactor.CONTROL_SCHEME_V2, numOfPhase)) {
//							Logger.writeln("ESTOP triggered while contactors detected still closed, try re-open them again on more time..");
//							contactor.setState(false);
//							contactor.setOperationNormal(false);
//						}
						
						if(charger.isLocked()) {
							view.showImage(uiImage.get("Emergency_button_pressed_halt"));
							charger.setCableLock(false);
							Logger.writeln("ESTOP triggered, open IEC lock");
						}else {
							criticalFaultTriggered = true;
							Logger.writeln("ESTOP triggered, originally unlocked and no unlock action");
							view.showImage(uiImage.get("EStopHalt"));
						}
						//view.showImage(uiImage.get("Emergency_button_pressed_halt"));						
					}
				}
	    		
	    	});	
		}		
				


		if(config.getJSONObject("Type").getString("Value").equals("Socket")) {
			charger.addLockListener(new LockStateListener() {
	
				@Override
				public void locked(ChangeEvent evt) {
					if(config.getJSONObject("Type").getString("Value").equals("Socket")) {
						if(charger.getState() == IECCharger.STATE_B) {
							////unlockButton.setVisible(true);							//2020-05-26 for re-enable "STOP" screen button	end here
							setState(State.Locked);
						} else {
							charger.setCableLock(false);
						}
					}
					if(keyLed != null) {
						keyLed.setState(GPIO.HIGH);
					}
				}
	
				@Override
				public void lockFailed(ChangeEvent evt) {
					Logger.writeln("lockFailed event handling");
					setState(State.Replug);
				}
	
				@Override
				public void unlocked(ChangeEvent evt) {
					Logger.writeln("Unlock event handling");
					
					if(plugUnlockTimer != null) {
						plugUnlockTimer.cancel();
					}
					
					if(config.getJSONObject("Type").getString("Value").equals("Socket")) {
						if(isChargingSession()) {
							if(plugAndChargeUnlock || buttonUnlock) {
								plugAndChargeUnlock = false;
								buttonUnlock = false;
								stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Local)));		// add for unlock button at plug & charge
							}else {
								stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.EVDisconnected)));
							}
						}
		
						unlockButton.setVisible(false);						//2020-05-26 for re-enable "STOP" screen button	end here
						// TODO
						if(status != State.Initialize && status != State.Unavailable && status != State.NetworkDown) {
							switch(charger.getState()) {
							case Charger.STATE_CABLE_DISCONNECT:
								setState(State.Ready);
								break;
							case Charger.STATE_CABLE_CONNECT:
//								setState(State.Plugging);
//								break;
							case Charger.STATE_B:
							case Charger.STATE_C:
							case Charger.STATE_D:
								setState(State.Unlocked);
								break;
							case Charger.STATE_N:
								break;
							default:
								break;
							}
						}
					}
					if(keyLed != null) {
						keyLed.setState(GPIO.LOW);
					}
				}
	
				
				@Override
				public void unlockFailed(ChangeEvent evt) {
					if(status != State.Initialize && status != State.Unavailable && status != State.NetworkDown) {
						setState(State.Unlock);
					}
				}
	
			});
		}
		if(config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {
			int[] channels = new int[1];
			channels[0] = ioSet.getInt("ADC_PP");
			//adc.startConversion(1010, channels);
			adc.startConversion(510, channels);		//20210505 
		}else {
			adc.startConversion(1010);
		}
		
		WebSocketClient.addListener(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText, this);
		WebSocketClient.addListener(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value") + "?cipher=" + encryptedOCPPText, this);

		if(!config.getJSONObject("LPRS").getString("Value").equals("0.0.0.0")) {
			housingSocietyLprsListener = new Thread(new Runnable() {
				
				@Override
				public void run() {
					String reply = "";
					while(true) {
						try {
							if(housingSocietyLprsIS.available() > 0) {
								byte[] b = new byte[housingSocietyLprsIS.available()];
								housingSocietyLprsIS.read(b);
								reply = new String(b);
								System.out.println(reply);
								EvseLprsAckMsg msg = new Gson().fromJson(reply, EvseLprsAckMsg.class);
								//setSystemTime(msg.getTimestamp());
								setSystemTimeLprs(msg.getTimestamp());		//2020-09-26 slightly modified for sending start up time sync request
							}
						} catch(JsonSyntaxException e1 ) {
							Logger.writeln("Bad LPRS Acknowledge message : " + reply);
						} catch (NullPointerException | IOException e) {
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
							Logger.writeln("config: " + e.getMessage());
						}
					}
				}
				
			});
			housingSocietyLprsListener.start();
			
		/// 20200624 remove to adapter NEC LPRS 
//			if(!config.getJSONObject("LPRS Engine").getString("Value").equals("0.0.0.0")) {
//				lprs = new AsiaVisionLPRS(config.getJSONObject("LPRS Engine").getString("Value"));
//				lprs.addDataListener(new DataListener() {
//	
//					@Override
//					public void dataReceived(DataEvent evt) {
//							EvseLprsMsg msg = new EvseLprsMsg(++msgSerialLprs, config.getJSONObject("Station Name").getString("Value"), "LPData", new String(evt.getData()), OCPP.dateTime.format(new Date()));
//							sendLprsMessage(new Gson().toJson(msg));
//					}
//					
//				});
//			}
//				
			
		}
		
		
		
		boolean runLPRS = true;
		//String ipc = config.getJSONObject("LPRS Engine").getString("Value").toString();
		if( (config.getJSONObject("LPRS Engine").getString("Value").equals("0.0.0.0") && (config.getJSONObject("LPRS").getString("Value").equals("0.0.0.0")))){
			runLPRS = false;
		}
		
		if(runLPRS || config.getJSONObject("Authentication").getString("Value").equals("Subscription")) {		
		//if(runLPRS || (!config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge"))) {
		//if(runLPRS ) {	
			try {
				camera = new Camera();
				// webcam = Webcam.getDefault();
				// webcam.setViewSize(new Dimension(640, 480));
				//webcam.setViewSize(new Dimension(800, 600));
				//webcam.setCustomViewSizes(new Dimension(1280, 720));
				//webcam.setCustomViewSizes  (new Dimension[] { WebcamResolution.HD.getSize() });
				//((WebCamDevice) webcam)).setResolution(new Dimension[] { WebcamResolution.VGA.getSize() }); // register custom size
				//	webcam.setParameters(parameters);
			}catch (Throwable e) {
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
	
				Logger.writeln("Problem init webacam "+errors.toString());			
			}
			
			Logger.writeln("camera captured enabled");
			
			
			String lprsRxHost = config.getJSONObject("LPRS Engine").getString("Value");
			int Lprsport = 4445;
			
			if(runLPRS) {
				
				try {
					Lprsport = config.getJSONObject("LPRS Engine Port").getInt("Value");
					Logger.writeln("LPRS engine port "+Lprsport);
				} catch (Exception e) {
					Logger.writeln("Error in getting LPRS engine port");
				}


				lprsTimeSyncThread = new Thread(new Runnable() {
					int repeatCnt = 0;
					@Override
					public void run() {

		        		try {
		        			Thread.sleep(5000);
		        		}catch(Exception e ) {
		        		}	
						EvseLprsMsg msg0 = new EvseLprsMsg(++msgSerialLprsTimeSync, config.getJSONObject("Station Name").getString("Value"), "SyncTime","", OCPP.dateTime.format(new Date()));
						//sendLprsMessage(new Gson().toJson(msg0));
						incrDecrLprsMessageBuf(new Gson().toJson(msg0), LPRS_MSGBUF_INCR);
						Logger.writeln("add power up lprs time sync request");
						
		        		try {
		        			Thread.sleep(2000);
		        		}catch(Exception e ) {
		        		}				

		        		tMark = System.currentTimeMillis();
			        	while(true) {
			        		
			    			if(System.currentTimeMillis()<tMark) {				// to handle any problem due to incorrect system time reset/re-sync
			    				tMark = System.currentTimeMillis();
			    				Logger.writeln("lprs process time mark manullay reset to current time");
			    			}
			        		
			        		if((System.currentTimeMillis() - tMark) >= TIMESYNC_PERIOD) {
			        			tMark = System.currentTimeMillis();
								EvseLprsMsg msg = new EvseLprsMsg(++msgSerialLprsTimeSync, config.getJSONObject("Station Name").getString("Value"), "SyncTime","", OCPP.dateTime.format(new Date()));
								//sendLprsMessage(new Gson().toJson(msg));
								incrDecrLprsMessageBuf(new Gson().toJson(msg), LPRS_MSGBUF_INCR);
			        			Logger.writeln("add periodic lprs time sync request");
			        		}
			        		try {
			        			Thread.sleep(2000);
			        		}catch(Exception e ) {
			        		}
			        	}
			        }
				});
				lprsTimeSyncThread.start();
				imgTx =  new ImgSend(lprsRxHost, Lprsport, config.getJSONObject("Station Name").getString("Value"));
				
			}
			
						
			


			barcodeReadThread = new Thread(new Runnable() {
				private long readtime = 0;
				private String code;
				private int DEBOUNCE = 2000;
				
				@Override
				public void run() {
					while(keepOnCamCapture) {
						try {
							if(status == State.Authorize || status == State.Charging || status == State.Pause) {
								
								if(cameraImage != null) {
//									BufferedImage tempImg = cameraImage;
//									resize(cameraFrame, 336 ,252)
//									BufferedImage tempImg = resize(convertToType(cameraImage, BufferedImage.TYPE_BYTE_GRAY), 336 ,252);
									BufferedImage tempImg = convertToType(cameraImage, BufferedImage.TYPE_BYTE_GRAY);
//									String newcode = null;
									String newcode = BarcodeFactory.decode(tempImg);
									tempImg = null;
									
									if(newcode != null) {
										Logger.writeln("Camera read " + newcode);
										if(!newcode.equals(code)) {
											code = newcode;
											readtime = System.currentTimeMillis();
											if(status == State.Authorize || isChargingSession()) {
												if(status == State.Authorize) {
													setState(State.Authorizing);
												}
												authorizeConf(send(authorize(code, OcppClient.QRCODE)));
											}
										} else {
											readtime = System.currentTimeMillis();
										}
										System.out.println("readtime: " + readtime);
									} else {
//										Logger.writeln("Camera read not hit");
										if(System.currentTimeMillis() - readtime > DEBOUNCE) {
											code = null;
										}
									}
								}else {
									Logger.writeln("No buffer image for QR Code Scan, go to sleep");
									Thread.sleep(5000);
								}
							} else {
								Thread.sleep(500);
							}
						} catch (InterruptedException e) {
							Logger.writeln("barcodeReadThread InterruptedException: " + e.getMessage());
						} catch (Exception e) {
							Logger.writeln("barcodeReadThread Exception: " + e.getMessage());
						}
					}
				}	
			});		
			
			cameraCaptureThread = new Thread(new Runnable() {
				@Override
				public void run() {
					long t = System.currentTimeMillis();
					int barcodeCount = 0;
					int camResetCounter = 0;
					while(keepOnCamCapture) {		
						if(lprsImgCapTimer.isTimesUp()) {
							lprsImgCapTimer.resetTimer();
							barcodeCount = barcodeCount >= 30 ? 1 : barcodeCount + 1;
							
							
							
							if(!camera.isOpen()) {
								camResetCounter += 1;
								Logger.writeln("cam closed and open again:" + camResetCounter);
								camera.open();
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
								}													
							}
							
							
							if(camResetCounter > 10) {
								Logger.writeln("Cam Capture Disabled Due To Connection Issue.");
								keepOnCamCapture = false;
							}
								
							
							if(camera.isOpen()) {
								try {
//									System.out.println(System.currentTimeMillis() - t);
									t = System.currentTimeMillis();	
								            
									if (config.getJSONObject("Authentication").getString("Value").equals("Subscription") && status == State.Authorize)	
									{
										cameraImage = camera.getImage();
										cameraFrame = convertToType(cameraImage, BufferedImage.TYPE_3BYTE_BGR);
										AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
										tx.translate(-cameraImage.getWidth(null), 0);
										AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
										BufferedImage pip = op.filter(cameraFrame, null);
										pip = resize(pip, 336 ,252);
										
										if(status == State.Authorize) {	// Authorize state may be changed here after reading the cam image, e.g. after unlock button action 
											view.showPIP(pip);
										}		
									}else if(( status != State.Authorize) &&(status != State.Authorizing)){
										try {
											cameraImage = camera.getImage();
											BufferedImage frameBW = convertToType(cameraImage, BufferedImage.TYPE_BYTE_GRAY);
											if(( status != State.Authorize) &&(status != State.Authorizing)) {	
									        	if(imgTx != null) {
									        		imgTx.process(frameBW);
									        	}
											}
											}catch (Exception e) {
												Logger.writeln("error in adding capture image");
												camResetCounter += 1;
											}
										
										if ( (config.getJSONObject("Authentication").getString("Value").equals("Subscription") && status == State.Charging) ||
											 (config.getJSONObject("Authentication").getString("Value").equals("Subscription") && status == State.Pause)
										) {
											cameraImage = camera.getImage();
											cameraFrame = convertToType(cameraImage, BufferedImage.TYPE_3BYTE_BGR);
											AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
											tx.translate(-cameraImage.getWidth(null), 0);
											AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
											BufferedImage pip = op.filter(cameraFrame, null);
											pip = ChargingStation.resize(pip, 224 ,168);
											
											if(status == State.Charging || status == State.Pause) {				
												view.showPIP(pip, 500, 430);
											}
										}
									}			
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								try {
									Logger.writeln("delay 1s for closed cam");
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									Logger.writeln("!camera.isOpen()" + e.getMessage());
								}
							}	
						}				
					}		
				}		
			});
			
			cameraCaptureThread.start();
			barcodeReadThread.start();
		}else {
			
			Logger.writeln("camera capture disabled");
		}

		if(runLPRS) {
			lprsSendMsgThread = new Thread(new Runnable() {
				
				public void run() {
					Logger.writeln("run lprs msg tx thread");
					while(true) {
						
						try {
							if(lprsMsgBuf.size()>0) {
								
								try {
									
									sendOutLprsMessage(lprsMsgBuf.get(0));
									incrDecrLprsMessageBuf(lprsMsgBuf.get(0), LPRS_MSGBUF_DECR);
									Logger.writeln("send out lprs time sync request or startTransaction/StopTransaction message");
								}catch (Exception e) {
									
									if(lprsMsgBuf.size()>20) {
										incrDecrLprsMessageBuf(lprsMsgBuf.get(0), LPRS_MSGBUF_DECR);
										Logger.writeln("error in sending out lprs msg, remove unsent msgs in buffer to avoid overflow!!");
									}
								}
								
							}
							
							try {
						 		Thread.sleep(10);
							}catch (Exception e){
								
							}
						 		
						}catch (Exception e) {
								e.printStackTrace();
						}
					}
				}
				
			});
			lprsSendMsgThread.start();	
		}
		
		stopQueueing = new JButton("");
		stopQueueing.setContentAreaFilled(false);
		stopQueueing.setBorderPainted(false);
		stopQueueing.setBounds(view.getUIConfig("ui_config/stopQueueing/x"), view.getUIConfig("ui_config/stopQueueing/y"), view.getUIConfig("ui_config/stopQueueing/w"), view.getUIConfig("ui_config/stopQueueing/h"));	
		try {
			stopQueueing.setIcon(new ImageIcon(uiImage.get("StopQueueingBtn").getScaledInstance(152, 72, Image.SCALE_SMOOTH)));
		} catch(NullPointerException e) {
			stopQueueing.setIcon(new ImageIcon(uiImage.get("StopBtn").getScaledInstance(152, 72, Image.SCALE_SMOOTH)));
		}
		stopQueueing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				Logger.writeln("Unlock by STOP queueing button");
				stopTransactionConf(send(stopTransaction("TS", OCPP.Reason.DeAuthorized)));
			}

		});

		
		
		startCharge = new JButton("");
		startCharge.setContentAreaFilled(false);
		startCharge.setBorderPainted(false);
		startCharge.setBounds(view.getUIConfig("ui_config/startCharge/x"), view.getUIConfig("ui_config/startCharge/y"), view.getUIConfig("ui_config/startCharge/w"), view.getUIConfig("ui_config/startCharge/h"));
		startCharge.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectTime > 0) {
					TimeSlotSelect("TS", selectTime);
				}
			}
		});
		
		
		fullyCharge = new JButton("");
		fullyCharge.setContentAreaFilled(false);
		fullyCharge.setBorderPainted(false);
		fullyCharge.setBounds(view.getUIConfig("ui_config/fullyCharge/x"), view.getUIConfig("ui_config/fullyCharge/y"), view.getUIConfig("ui_config/fullyCharge/w"), view.getUIConfig("ui_config/fullyCharge/h"));
		fullyCharge.setIcon(new ImageIcon(uiImage.get("FullChargeBtn").getScaledInstance(195, 91, Image.SCALE_SMOOTH)));
		fullyCharge.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTime = TIME_SELECT_FULL;
				TimeSlotSelect("TS", selectTime);
//				updateTimeBtn(false, true, true);
//				view.showTimeSlot();
			}
		});
		
		stopTimeCharge = new JButton("");
		stopTimeCharge.setContentAreaFilled(false);
		stopTimeCharge.setBorderPainted(false);
		stopTimeCharge.setBounds(view.getUIConfig("ui_config/stopTimeCharge/selecting/x"), view.getUIConfig("ui_config/stopTimeCharge/selecting/y"), view.getUIConfig("ui_config/stopTimeCharge/selecting/w"), view.getUIConfig("ui_config/stopTimeCharge/selecting/h"));
		stopTimeCharge.setIcon(new ImageIcon(uiImage.get("StopBtn").getScaledInstance(195, 91, Image.SCALE_SMOOTH)));	
		stopTimeCharge.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent evt) {
				//Logger.writeln("Unlock by STOP button");
//				buttonUnlock=true;
//				charger.setCableLock(false);
				if(status == State.Authorize) {
					if(contactor.getState()) {
						contactor.setState(false);
					}
					plugAndChargeUnlock = true;
					charger.setCableLock(false);
				} else {
					if(status == State.NetworkDown) {
						new Timer().schedule(new TimerTask() {
							@Override
							public void run() {
								Logger.writeln("stopTimeCharge remoteStopTransaction.");
								stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Remote)));
							}
							
						}, 100);
					} else {
						Logger.writeln("stopTimeCharge deAuthorizedStopTransaction.");
						stopTransactionConf(send(stopTransaction("TS", OCPP.Reason.DeAuthorized)));
						if(isLMSSuspended) {
							view.showImage(uiImage.get("LMS-Suspended"));
						}
					}
				}
			}

		});
		
		resetCharge = new JButton("");
		resetCharge.setContentAreaFilled(false);
		resetCharge.setBorderPainted(false);
		resetCharge.setBounds(view.getUIConfig("ui_config/resetCharge/x"), view.getUIConfig("ui_config/resetCharge/y"), view.getUIConfig("ui_config/resetCharge/w"), view.getUIConfig("ui_config/resetCharge/h"));
		resetCharge.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectTime = 0;
				view.showTimeSlot(selectTime);
				updateTimeBtn(true, false, false);
//				view.timeSelectBtnUpdate(uiImage.get("TimeSelectBtn"));
//				view.repaint();
			}
		});
		
		addHour = new JButton("");
		addHour.setFont(new Font("Arial", Font.PLAIN, 26));
		addHour.setForeground(Color.WHITE);
		addHour.setContentAreaFilled(false);
		addHour.setBorderPainted(false);
		addHour.setBounds(view.getUIConfig("ui_config/addHour/x"), view.getUIConfig("ui_config/addHour/y"), view.getUIConfig("ui_config/addHour/w"), view.getUIConfig("ui_config/addHour/h"));
		addHour.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectTime != TIME_SELECT_FULL) {
					int maxTime = config.getJSONObject("Time Slot Maximum (Min)").getInt("Value"); 
					if(selectTime <= 0) {
//						view.showImage(uiImage.get("TimeSelect-time"));
						updateTimeBtn(true, true, true);
//						view.repaint();
					}
					selectTime += 60;
					selectTime = selectTime > maxTime ? maxTime : selectTime;
					view.showTimeSlot(selectTime);
				}
			}
		});
		
		minusHour = new JButton("");
		minusHour.setFont(new Font("Arial", Font.PLAIN, 26));
		minusHour.setForeground(Color.WHITE);
		minusHour.setContentAreaFilled(false);
		minusHour.setBorderPainted(false);
		minusHour.setBounds(view.getUIConfig("ui_config/minusHour/x"), view.getUIConfig("ui_config/minusHour/y"), view.getUIConfig("ui_config/minusHour/w"), view.getUIConfig("ui_config/minusHour/h"));
		minusHour.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectTime != TIME_SELECT_FULL) {
					if(selectTime - 60 <= 0) {
						updateTimeBtn(true, false, false);
					}
					
					selectTime -= 60;
					selectTime = selectTime <= 0 ? 0 : selectTime;
					view.showTimeSlot(selectTime);
				}
			}
		});
		
		addMin = new JButton("");
		addMin.setFont(new Font("Arial", Font.PLAIN, 26));
		addMin.setForeground(Color.WHITE);
		addMin.setContentAreaFilled(false);
		addMin.setBorderPainted(false);
		addMin.setBounds(view.getUIConfig("ui_config/addMin/x"), view.getUIConfig("ui_config/addMin/y"), view.getUIConfig("ui_config/addMin/w"), view.getUIConfig("ui_config/addMin/h"));
		addMin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectTime != TIME_SELECT_FULL) {
					if(selectTime <= 0) {
						updateTimeBtn(true, true, true);
//						view.repaint();
					}
					
					int maxTime = config.getJSONObject("Time Slot Maximum (Min)").getInt("Value"); 
					selectTime += selectInterval;
					selectTime = selectTime > maxTime ? maxTime : selectTime;
					view.showTimeSlot(selectTime);
				}
			}
		});
		
		minusMin = new JButton("");
		minusMin.setFont(new Font("Arial", Font.PLAIN, 26));
		minusMin.setForeground(Color.WHITE);
		minusMin.setContentAreaFilled(false);
		minusMin.setBorderPainted(false);
		minusMin.setBounds(view.getUIConfig("ui_config/minusMin/x"), view.getUIConfig("ui_config/minusMin/y"), view.getUIConfig("ui_config/minusMin/w"), view.getUIConfig("ui_config/minusMin/h"));
		minusMin.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectTime != TIME_SELECT_FULL) {
					if(selectTime - selectInterval <= 0) {
						updateTimeBtn(true, false, false);
//						view.repaint();
					}
					
					selectTime -= selectInterval;
					selectTime = selectTime <= 0 ? 0 : selectTime;
					view.showTimeSlot(selectTime);
				}
			}
		});
		
		DeviceManager.addUSBListener(this);
		
		imgsTx = new ArrayList<BufferedImage>();

		
		if((config.getJSONObject("Hardware Version").getString("Value").equals("V2_5"))) {
			
			
			try {
			ADCChannel[] adcPwrChk = new ADCChannel[2];
			
			adcPwrChk[PowerSupplyCheck.ADC_P12VSENSE] = adc.getADCChannel(ioSet.getInt("ADC_P12VSENSE"));
			adcPwrChk[PowerSupplyCheck.ADC_N12VSENSE] = adc.getADCChannel(ioSet.getInt("ADC_N12VSENSE"));
			powerChk.setADCHardware(adcPwrChk);
			
			powerChk.dcPwerFailCheckStart();
			}catch (Exception e) {
				Logger.writeln("Error in initi AC power check");
			}
		}

		int cpErrorCntThreshold =  20;
		cpErrorCheckThread = new Thread(new Runnable() {
			int cpErrorCnt = 0;
			@Override
			public void run() {
				
				while(true) {

					if(status==State.CPError) {
						cpErrorCnt++;
						//Logger.writeln("CPError detected");
					}else {
						cpErrorCnt=0;
						//Logger.writeln("CPError reset");
					}	
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					
					}
					if(cpErrorCnt>(cpErrorCntThreshold)) {
						Logger.writeln("CPError triggered & unlock");
						if(contactor.getState()) {
							contactor.setState(false);
						}
						charger.setCableLock(false);						
					}
				}		
			}		
		});
		cpErrorCheckThread.start();
		
		//refreshLedColor(20);   //need debug
		refreshLedColor0(20);
		
		if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
			
			//meterIC.irq1CheckStart();
			meterIC.irqAddListener();
			//meterIC.initialization();
			meterIC.initialization();
			
			
		//	meterIC.irq1CheckStart();
			meterIC.addPwrChkListener(new PowerCheckListener() {
				
				@Override
				public int acLoss(ChangeEvent evt) {
					Logger.writeln("acloss triggered in cs");
					contactor.setState(false);
					if(charger.isLocked()) {
						charger.setCableLock(false);
					}
					return 0;
				}
	
				@Override
				public int overCurrent(ChangeEvent evt) {
					return 0;
				}
	
	
			});
		}			
		
		//testCheckADE7854(5);
		
		Logger.writeln("Setup finish");
		
//		System.out.println("Width: " + view.getWidth() + ", Heinght: " + view.getHeight());
	}

/**************************************************************************************************************************************/
/************************************************   BASIC FUNCTIONS   ****************************************************************/
/**************************************************************************************************************************************/
	
	private void checkAlreadyRunning() {
		int count = 0;
		String line;
		//Executable file name of the application to check. 
		final String applicationToCheck = "ChargingStation.jar";
		//Running command that will get all the working processes.
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec("ps -ef");
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		InputStream stream = proc.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		//Parsing the input stream.
		try {
			while ((line = reader.readLine()) != null) {
			    Pattern pattern = Pattern.compile(applicationToCheck);
			    Matcher matcher = pattern.matcher(line);
			    if (matcher.find()) {
					Logger.writeln("line: " + line);
					count ++;

			    }
			}
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		Logger.writeln("count: " + count);
		if(count > 3) {
			Logger.writeln("ChargingStation.jar is runnung!");
	    	System.exit(0);
	        return;
		}
	}
	private boolean haveSession() {
		Logger.writeln("haveRecordAndSession: " + "record.exists: " + record.exists() + " , session.exists: " + session.exists());

		boolean have = false;
		if(session.exists()) {
			try {
				FileInputStream fis = new FileInputStream(session);
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				StringTokenizer st = new StringTokenizer(new String(b), "\n");
				JSONObject lastRecord = null;
				while (st.hasMoreTokens()) {
					try {
						JSONObject json = new JSONObject(st.nextToken());
						lastRecord = json;
					} catch (JSONException e) {
					}
				}
				if(lastRecord != null) {
					stopTransaction.put(lastRecord);
					if(!record.exists()) {
						record.createNewFile();
					}
					FileOutputStream fos = new FileOutputStream(record);
					fos.write(stopTransaction.toString().getBytes());
					fos.close();
					Runtime.getRuntime().exec("sync");
					have = true;
				} else {
					Logger.writeln("session record error");
				}
				session.delete();
			} catch (IOException e) {
			}
		}
		return have;
	}

	private void setHashMd5(String cpConfig) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
		    byte[] messageDigest = md.digest(cpConfig.getBytes());
		    BigInteger number = new BigInteger(1, messageDigest);
		    hashMd5 = number.toString(16);
		} catch (NoSuchAlgorithmException e) {
			Logger.writeln("setHashMd5: " + e.getMessage());
			hashMd5 = "";
		}
	}
	
	private void rebootTask() {
		new Timer().schedule(new TimerTask() {

			@Override
			public void run() {
				if(needRestart && !isChargingSession()) {
					try {
						Logger.writeln("Need Restart");
						Runtime.getRuntime().exec("sudo reboot");
//						Runtime.getRuntime().exec("sudo java -Dsun.security.smartcardio.library=/usr/lib/arm-linux-gnueabihf/libpcsclite.so.1 -jar ChargingStation.jar config.txt &");
						//Runtime.getRuntime().exec("sudo java -Djava.library.path=/home/pi/opencv-4.1.2/build/lib -Dsun.security.smartcardio.library=/usr/lib/arm-linux-gnueabihf/libpcsclite.so.1 -jar ChargingStation.jar config.txt &");
						Logger.writeln("Restarting...");
						System.exit(0);
					} catch(IOException e) {
						
					}
				}
			}

		}, 0, 5000);
	}
	
	private void updateTimeBtn(boolean enableTimeSelect, boolean enableStartBtn, boolean enableResetBtn) {
		Image upwardBtn;
		Image downwardBtn;
		Image startBtn;
		Image resetBtn;
		
		if(enableTimeSelect) {
			upwardBtn 	= uiImage.get("UpwardBtn").getScaledInstance(72, 77, Image.SCALE_SMOOTH);
			downwardBtn	= uiImage.get("DownwardBtn").getScaledInstance(72, 77, Image.SCALE_SMOOTH);
		} else {
			upwardBtn 	= uiImage.get("UpwardBtn-dim").getScaledInstance(72, 77, Image.SCALE_SMOOTH);
			downwardBtn	= uiImage.get("DownwardBtn-dim").getScaledInstance(72, 77, Image.SCALE_SMOOTH);
		}
		
		startBtn = enableStartBtn ? uiImage.get("StartBtn").getScaledInstance(178, 91, Image.SCALE_SMOOTH) : uiImage.get("StartBtn-dim").getScaledInstance(178, 91, Image.SCALE_SMOOTH);
		resetBtn = enableResetBtn ? uiImage.get("ResetBtn").getScaledInstance(178, 91, Image.SCALE_SMOOTH) : uiImage.get("ResetBtn-dim").getScaledInstance(178, 91, Image.SCALE_SMOOTH);
		
		addHour.setIcon(new ImageIcon(upwardBtn));
		addMin.setIcon(new ImageIcon(upwardBtn));
	
		minusHour.setIcon(new ImageIcon(downwardBtn));
		minusMin.setIcon(new ImageIcon(downwardBtn));
		
		startCharge.setIcon(new ImageIcon(startBtn));
		resetCharge.setIcon(new ImageIcon(resetBtn));
		
	}
	
	private void TimeSlotSelect(String timeSlotType, int time) {
		if(tsBtnEna) {
			tsBtnEna 			= false;
			idTag 				= timeSlotType;
			timeSelectRemain 	= time * 60;
			lastEnergy 			= Math.round(getMaterValue("chargerEnergy"));
			setState(State.Authorized);
			if(queueingNum == 0) {
				startCountDownTimer(timeSlotType, time);
			}
		}
	}
	
	private void startCountDownTimer (String timeSlotType,int time) {
		Logger.writeln("startCountDownTimer timeSlotType: " + timeSlotType + ",  time: " + time);
		if(countDownTimer != null) {
			countDownTimer.cancel();
		}
		if(stopTimer != null) {
			stopTimer.cancel();
		}
		stopTimer 			= new Timer();
		stopTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				Logger.writeln("Time up!");
				stopTransactionConf(send(stopTransaction(timeSlotType, OCPP.Reason.DeAuthorized)));
//				setState(State.Unlock);
			}
		}, time * 60 * 1000);
		String totalTime = time / 60 + "h " +  time % 60 + "m";
		countDownTimer = new Timer();
		
		if(time < TIME_SELECT_FULL) {
			
			countDownTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
		        public void run() {
					double currentEnergy = Math.round(getMaterValue("chargerEnergy"));
					double usedEnergy = currentEnergy - lastEnergy;
					if(usedEnergy < 0) {
						usedEnergy = (currentEnergy + 10000000000D) - lastEnergy;
					}

//					Logger.writeln("(TimeSlotSelect)getVoltage: " + charger.getVoltage(PowerMeter.PHASE_A));
//					view.setAllTag(
//						"" + Math.round(charger.getVoltage(PowerMeter.PHASE_A)),
//						String.format("%.1f", charger.getCurrent(PowerMeter.PHASE_A)),
//						String.format("%.1f", usedEnergy/1000)
//					);
				
					timeSelectRemain -= 3;
					int minRemain = timeSelectRemain / 60 + 1;
					timeRemainDesc = minRemain / 60 + "h " + String.format("%02d", minRemain % 60) + "m / " + totalTime;
					
					if(status == State.Charging ) {
						view.showTag();
						view.showRemaingTime(timeRemainDesc);
					}
				}
		    }, 0, 3000);
		} 
	}
	
	public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
	    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

	    Graphics g = dimg.createGraphics();
	    g.drawImage(img, 0, 0, newW, newH, null);
	    g.dispose();
	    
//	    Graphics2D g2d = dimg.createGraphics();
//	    g2d.drawImage(tmp, 0, 0, null);
//	    g2d.dispose();

	    return dimg;
	} 	

	public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
		BufferedImage image;

		// if the source image is already the target type, return the source image

		if (sourceImage.getType() == targetType)
			image = sourceImage;

		// otherwise create a new image of the target type and draw the new
		// image

		else {
			image = new BufferedImage(
				sourceImage.getWidth(),
				sourceImage.getHeight(), targetType);
			image.getGraphics().drawImage(sourceImage, 0, 0, null);
		}

		return image;
	}	
	
	public InputStream captureImgToFile() {
		try {
			String cmd = "sudo fswebcam -r 1280x720 -d /dev/video0 /home/pi/cam1.jpg";
			
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			InputStream is = p.getErrorStream();
			byte[] b = new byte[is.available()];
			is.read(b);
			is.close();
			is = p.getInputStream();
			b = new byte[is.available()];
			is.read(b);
			is.close();
			
			return is;
		} catch (IOException | InterruptedException e) {
			Logger.writeln("captureImgToFile: " + e.getMessage());
			return null;
		}
	}
		
	private void showQueueing() {
		view.remove(addHour);
		view.remove(minusHour);
		view.remove(addMin);
		view.remove(minusMin);
		view.remove(startCharge);
		view.remove(fullyCharge);
		view.remove(resetCharge);
		view.hideTag();
		view.hideTimeSlot();
		view.remove(stopTimeCharge);
		view.showImage(uiImage.get("Queueing"));
		view.add(stopQueueing, new Integer(2));
		view.updateQueueing(queueingTime, queueingNum);
		queueingMode = true;
	}
	
	private void hideQueueing() {
		view.remove(stopQueueing);
		view.hideQueueing();
		queueingMode = false;
	}

	
	private void showConfig(String permission) {
		isShowConfig = true;
		if(status == State.Authorize && config.getJSONObject("Authentication").getString("Value").equals("TimeSelect")) {
			view.remove(addHour);
			view.remove(minusHour);
			view.remove(addMin);
			view.remove(minusMin);
			view.remove(startCharge);
			view.remove(fullyCharge);
			view.remove(resetCharge);
			view.remove(stopTimeCharge);
			view.remove(stopQueueing);
			view.hideTimeSlot();
			view.hideQueueing();
			view.updateSelectedMsg("");
			if(config.getJSONObject("Time Select Fully Charge").getString("Value").equals("Show")) {
				view.remove(fullyCharge);
			}
		}
	
		
		String[] buttons = null;
		if(permission.equals("Admin")) {						
			buttons = new String[3];
			buttons[0] = "Restart";
			buttons[1] = "Reset";
			buttons[2] = "Register";						
		} else {
			buttons = new String[2];
			buttons[0] = "Restart";
			buttons[1] = "Clear Log";
		}
		view.showImage(uiImage.get("Configure"));
		nfcReader.removeAllListeners();
		nfcReader.addDataListener(new DataListener() {
	
			@Override
			public void dataReceived(DataEvent evt) {
				String cid = new String(evt.getData()).toLowerCase();
				if(ocppServer.hasCard(cid)) {
					ocppServer.removeCard(cid);
				} else {
					ocppServer.addCard(cid);
				}
				view.updateConfig(getConfiguration());
			}
				
		});
		view.showConfig(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/"), getConfiguration(), buttons, permission, configListener, configinfo.sVersion + "(" + configinfo.sVersionSim + ")");
//			view.showImage(uiImage.get("Configure"));
//			
//			int ConfigInset = 30;
//			SettingPanel configPage;
//			
//			if(ocppServer != null) {
//				configPage = new SettingPanel(permission, ChargingStation.this, ocppServer, NetworkManager.getInterface("eth0"), NetworkManager.getInterface("wlan0"));
//			} else {
//				configPage = new SettingPanel(permission, ChargingStation.this);
//			}
//			configPage.setBounds(ConfigInset, ConfigInset, 1024 - 2 * ConfigInset, 600 - 2 * ConfigInset);
//			view.add(configPage);
//			nfcReader.removeAllListeners();
//			nfcReader.addDataListener(new DataListener() {
//
//				@Override
//				public void dataReceived(DataEvent evt) {
//					String cid = new String(evt.getData()).toLowerCase();
//					if(ocppServer.hasCard(cid)) {
//						ocppServer.removeCard(cid);
//					} else {
//						ocppServer.addCard(cid);
//					}
//					view.updateConfig(getConfiguration());
//				}
//				
//			});
//			view.showConfig(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/"), getConfiguration(), buttons, permission, configListener);
	}
	
	private void showReady() {
		if(isChargingSession()) {
			if(sessionTimer == null && config.getJSONObject("End upon state A").getString("Value").equals("Yes")) {
				stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Local)));
			}
		}
		if(isNewConfig) {
			needRestart = true;
			view.showImage(uiImage.get("Initialize"));
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				Logger.writeln("isNewConfig: " + e.getMessage());
			}
			rebootTask();
			return;
		}
		if(charger.isLocked()) {
			charger.setCableLock(false);
		}
		if(queueingNum == 0) {
			view.showImage(uiImage.get("Ready"));
		} else {
			view.showImage(uiImage.get("Ready-Queueing"));
		}
		statusLED.setColor(ChargingStationUI.Green);
		charger.setPilotEnable(false);
		//	disable unlock btn
		if(unlockButton.getIsVisible()) {
			unlockButton.setVisible(false);
		}
	}
	
	public JSONArray authorize(String id, String type) {
		idTag = id;
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put("id");
		payload.put("Authorize");
		JSONObject json = new JSONObject().put("idTag", idTag);
		if(config.getJSONObject("Authentication").getString("Value").equals("Autotoll")) {
			json.put("type", type);
		}
		payload.put(json);
		return payload;
	}

	private void authorizeConf(JSONArray a) {
		Logger.writeln("authorizeConf: " +  a);
		OCPP.Authorize.conf conf;
		if(a == null) {
			conf = new OCPP.Authorize.conf(new OCPP.IdTagInfo(OCPP.AuthorizationStatus.Invalid));
		} else {
			try {
			Logger.writeln("JSON: " + a.getJSONObject(2).toString());
				conf = new Gson().fromJson(a.getJSONObject(2).toString(), OCPP.Authorize.conf.class);
			} catch (JSONException e) {
				Logger.writeln(e.getMessage());
				conf = new OCPP.Authorize.conf(new OCPP.IdTagInfo(OCPP.AuthorizationStatus.Invalid));
			}
//			conf = new Gson().fromJson("{\n"
//					+ "   \"idTagInfo\":{\n"
//					+ "      \"parentIdTag\":{\n"
//					+ "         \"IdToken\":\"citats202103310001\"\n"
//					+ "      },\n"
//					+ "      \"status\":\"Accepted\"\n"
//					+ "   }\n"
//					+ "}", OCPP.Authorize.conf.class);
		}
//		String status = json.getJSONObject("idTagInfo").getString("status").toLowerCase();
		boolean isAuthorize = false;
		Logger.writeln("authorizeConf isChargingSession:" + isChargingSession());
		if(isChargingSession()) {;
			switch(conf.getIdTagInfo().getStatus()) {
			case Accepted:
				isAuthorize = true;
				break;
			case InsufficientBalance:
				if(insufficientBalanceAction == OCPP.InsufficientBlanceAction.Accept || insufficientBalanceAction == OCPP.InsufficientBlanceAction.StopOnly) {
					isAuthorize = true;
				}
				break;
			case Blocked:
			case ConcurrentTx:
			case Expired:
			case Invalid:
			default:
				break;
				
			}
			if(isAuthorize) {
				view.showImage(uiImage.get("Valid"));
				stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Local)));
			} else {
				if(messageTimer != null) {
					messageTimer.shutdown();
					int count = 0;
					while(!messageTimer.isTerminated() || count < 1000) {
						count++;
						Logger.writeln("messageTimer.isTerminated(): " + messageTimer.isTerminated());
					}
				}
				Logger.writeln("idTagInfo.getStatus:" + conf.getIdTagInfo().getStatus());
				
				if( conf.getIdTagInfo().getStatus() == OCPP.AuthorizationStatus.InsufficientBalance) {			//2020-08-25 added for insufficient balance case
					view.showImage(uiImage.get("InsufficientBalance"));
				}
				else if(conf.getIdTagInfo().getStatus() == OCPP.AuthorizationStatus.Expired) {
					view.showImage(uiImage.get("QRcodeExpired"));
				}else {
					view.showImage(uiImage.get("Invalid"));
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(charger.getState() == Charger.STATE_A) {
					view.showImage(uiImage.get("Replug"));
				} else if(charger.getState() == Charger.STATE_C) {
					messageTimer = Executors.newScheduledThreadPool(1);
					messageTimer.scheduleAtFixedRate(new Runnable() {
	
						@Override
						public void run() {
						if(config.getJSONObject("Authentication").getString("Value").equals("Autotoll")) {
								Dimension d = view.getSize();
								BufferedImage img = new BufferedImage(d.width, 4000, BufferedImage.TYPE_4BYTE_ABGR);
								Graphics g = img.getGraphics();
								g.setColor(Color.BLACK);
			
								Font f = new Font("Arial", Font.PLAIN, 36);
								g.setFont(f);
								int minute = (int) (new Date().getTime() - startTime.getTime()) / 60000;
								int hour = minute / 60;
								minute %= 60;
			
								String ctime = (hour < 10 ? " " + hour : hour) + "H " + (minute < 10 ? "0" + minute : minute) + "Min";
								String cfee = " " + (serverDown ? ((hour * 60 + minute) / unitLength + 1) * unitPrice : chargingFee);
								String cno = " " + config.getJSONObject("Station Name").getString("Value");
			
								g.drawString(ctime, d.width / 2 + 100, d.height / 2 - 40);
								g.drawString(cfee, d.width / 2 + 100, d.height / 2 + 30);
								g.drawString(cno, d.width / 2 + 100, d.height / 2 + 100);
								
								g.dispose();
			
								view.showOverlay(img);
							}
						}	
					}, 0, 1, TimeUnit.SECONDS);
					view.showImage(uiImage.get("Charging"));
					if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {
						unlockButton.setVisible(true);					// 2020-06-30 add for plug & charge mode
						Logger.writeln("Show unlockButton: charging");
					}	
				} else if(charger.getState() == Charger.STATE_B || charger.getState() == Charger.STATE_D) {
					view.showImage(uiImage.get("Pause"));
					view.hideTag();
					if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {
						unlockButton.setVisible(true);					// 2020-06-30 add for plug & charge mode
						Logger.writeln("Show unlockButton: pause");
					}
				}
			}
		} else {
			switch(conf.getIdTagInfo().getStatus()) {
			case Accepted:
				isAuthorize = true;
				break;
			case InsufficientBalance:
				if(insufficientBalanceAction == OCPP.InsufficientBlanceAction.Accept || insufficientBalanceAction == OCPP.InsufficientBlanceAction.StartOnly) {
					isAuthorize = true;
				}
				break;
			case Blocked:
			case ConcurrentTx:
			case Expired:
			case Invalid:
			default:
				break;
				
			}
			Logger.writeln("isAuthorize: " + isAuthorize);
			if(isAuthorize) {
				currentProfile = defaultProfile;
				setState(State.Authorized);
			} else {
				if( conf.getIdTagInfo().getStatus() == OCPP.AuthorizationStatus.InsufficientBalance) {			//2020-08-25 added for insufficient balance case
					view.showImage(uiImage.get("InsufficientBalance"));
				}
				else if(conf.getIdTagInfo().getStatus() == OCPP.AuthorizationStatus.Expired) {
					view.showImage(uiImage.get("QRcodeExpired"));
				} else {
					view.showImage(uiImage.get("Invalid"));
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setState(State.Authorize);				
			}
		}
	}

	private JSONArray changeAvailability(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		JSONObject json = new JSONObject();
		
		switch (a.getJSONObject(3).getEnum(OCPP.AvailabilityType.class, "type")) {
		case Inoperative:
			isOperative = false;
			if(isChargingSession()) {
				json.put("status", OCPP.AvailabilityStatus.Scheduled);
			} else {
				setEnable(false);
				json.put("status", OCPP.AvailabilityStatus.Accepted);
			}
			break;
		case Operative:
			if(status == State.Unavailable) {
				isOperative = true;
				setEnable(true);
				json.put("status", OCPP.AvailabilityStatus.Accepted);
			} else {
				json.put("status", OCPP.AvailabilityStatus.Rejected);
				if("Yes".equals(config.getJSONObject("Custom Fields").getString("Value"))) { 
					json.put("reason", "status: " + status);
				}
			}
			break;
		}
		payload.put(json);
		return payload;
	}

	private JSONArray changeConfiguration(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		JSONObject json = new JSONObject();

		switch (a.getJSONObject(3).getString("key").toLowerCase()) {
		case "insufficientbalanceaction":
			insufficientBalanceAction = OCPP.InsufficientBlanceAction.valueOf(a.getJSONObject(3).getString("value"));
			json.put("status", "Accepted");
			break;
		case "admincard":
			StringTokenizer cards = new StringTokenizer(a.getJSONObject(3).getString("value"), "\n");
			adminCard = new byte[cards.countTokens()][];
			for(int i = 0; cards.hasMoreElements(); ++i) {
				adminCard[i] = HexFactory.toBytes(cards.nextToken(), ' ');
			}
			break;
		case "admincard1":
			adminCard[0] = HexFactory.toBytes(a.getJSONObject(3).getString("value"), ' ');
			json.put("status", "Accepted");
			break;
		case "admincard2":
			adminCard[1] = HexFactory.toBytes(a.getJSONObject(3).getString("value"), ' ');
			json.put("status", "Accepted");
			break;
		case "admincard3":
			adminCard[2] = HexFactory.toBytes(a.getJSONObject(3).getString("value"), ' ');
			json.put("status", "Accepted");
			break;
		case "admincard4":
			adminCard[3] = HexFactory.toBytes(a.getJSONObject(3).getString("value"), ' ');
			json.put("status", "Accepted");
			break;
		case "heartbeatinterval":
			setHeartbeat(a.getJSONObject(3).getInt("value"));
			json.put("status", "Accepted");
			break;
		case "idleunlockinterval":
			idleUnlockInterval = a.getJSONObject(3).getInt("value");
			json.put("status", "Accepted");
			break;
		case "minutes_per_session":
			unitLength = Integer.parseInt(a.getJSONObject(3).getString("value"));
			json.put("status", "Accepted");
			break;
		case "session_fee":
			unitPrice = Double.parseDouble(a.getJSONObject(3).getString("value"));
			json.put("status", "Accepted");
			break;
		default:
			json.put("status", "Rejected");
			if("Yes".equals(config.getJSONObject("Custom Fields").getString("Value"))) { 
				json.put("reason", "changeAvailability: " + a);
			}
			break;
		}

		payload.put(json);
		return payload;
	}

	@Override
	public void dataReceived(DataEvent evt) {
	}
	
	@Override
	public void received(ConnectionEvent evt) {
//		serverDown = false;
		WebSocketClient client = (WebSocketClient) evt.getSource();

		if(client.getURL().equals(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText)) {
			final JSONArray a = new JSONArray(WebSocketClient.getMessage(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText));
			Logger.writeln("ocppMsgRes: " + a.toString());
			System.out.println("ocpp response: " + a.toString());
			if(a.getInt(0) == 3 || a.getInt(0) == 4) {
//				Logger.writeln("Receive an reply");
				//synchronized(ChargingStation.class) {
					serverReply.put(a.getString(1), a);
					//notify();
				//}
			} else if(a.getInt(0) == 2) {
//				Logger.writeln("Receive an request");
	//			remoteCall.writeLock().lock();
				
				Thread t = new Thread(new Runnable() {
	
					@Override
					public void run() {
						switch (a.getString(2).toLowerCase()) {
						case "changeavailability":
							send(changeAvailability(a));
							break;
						case "remotestarttransaction":
							JSONArray array = remoteStartTransaction(a);
							send(array);
							OCPP.RemoteStartTransaction.conf conf = new Gson().fromJson(array.getJSONObject(2).toString(), OCPP.RemoteStartTransaction.conf.class);
							if(conf.getStatus() == OCPP.RemoteStartStopStatus.Accepted) {
								setState(State.Authorized);
							}
							break;
						case "remotestoptransaction":
							remoteStopTransaction(a);
							break;
						case "setchargingprofile":
							if(serverDown) {
								setChargingProfile(a);
							} else {
								send(setChargingProfile(a));
							}
							break;
						case "triggermessage":
							triggerMessage(a);
							break;
						case "changeconfiguration":
							send(changeConfiguration(a));
							break;
						case "datatransfer":
							dataTransfer(a);
							break;
						case "GetConfiguration":
							getConfiguration(a);
							break;
						default:
							JSONArray reply = new JSONArray();
							reply.put(3);
							reply.put(a.getString(1));
							reply.put("Unsupported Operation");
							send(reply);
							break;
						}
					}
				});
				t.start();
			}
		} else if(client.getURL().equals(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value") + "?cipher=" + encryptedOCPPText)) {
			try {
				System.out.println("received Backdoor data!");
				final JSONArray a = new JSONArray(WebSocketClient.getMessage(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value") + "?cipher=" + encryptedOCPPText));
				Logger.writeln(a.toString());
				if(a.getInt(0) == 3 || a.getInt(0) == 4) {
					backdoorReply.put(a.getString(1), a);
				} else if(a.getInt(0) == 2) {
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							switch (a.getString(2).toLowerCase()) {
							case "updatefirmware":
								sendBackdoor(updateFirmware(a));
								break;
							case "datatransfer":
								dataTransfer(a);
								break;
							default:
								JSONArray reply = new JSONArray();
								reply.put(3);
								reply.put(a.getString(1));
								reply.put("Unsupported Operation");
//								sendBackdoor(reply);
								break;
							}
						}
					});
					t.start();
				}
			} catch (JSONException e) {
				
			}
		}
	}

	private void dataTransfer(JSONArray a) {
		JSONObject json = new JSONObject();
		JSONArray payload = new JSONArray();
		payload.put(3);	
		payload.put(a.getString(1));
		System.out.println("dataTransfer messageId: " + a.getJSONObject(3).getString("messageId"));
		switch (a.getJSONObject(3).getString("messageId")) {
		case "ChargingDetails":
			try {
				JSONObject data = a.getJSONObject(3).getJSONObject("data");
				startTime = OCPP.dateTime.parse(data.getString("start_time"));
				unitLength = Integer.parseInt(data.getString("minutes_per_session"));
				unitPrice = Double.parseDouble(data.getString("session_fee"));
				unitCount = Integer.parseInt(data.getString("session_count"));
				chargingFee = Double.parseDouble(data.getString("charging_fee"));
				json.put("status", "Accepted");
			} catch (JSONException e) {
				json.put("status", "Rejected");
				json.put("reason", e.getMessage());
			} catch (ParseException e) {
				json.put("status", "Rejected");
				json.put("reason", e.getMessage());
			}
			break;
		//TODO: Add New Data Transfer Message QueueingDetails 
		case "queueingDetails":
			if(isShowConfig) {
				break;
			}
			try {
				
				JSONObject data = a.getJSONObject(3).getJSONObject("data");
//				System.out.println("QueueingDetails: " + data.toString());
				if(data.getString("estStart") == null || "".equals(data.getString("estStart"))) {
					queueingTime = null;
				} else {
					int maxTime = config.getJSONObject("Time Slot Maximum (Min)").getInt("Value");
					Date now = new Date();
					calendar.setTime(now);
					calendar.add(Calendar.MINUTE, maxTime);
					queueingTime = OCPP.dateTime.parse(data.getString("estStart"));
					System.out.println("Max waiting time: " + calendar.getTime() + " , queueingTime: " + queueingTime);
					if(calendar.getTime().before(queueingTime)) {  
						queueingTime = null;
						System.out.println("Set queueingTime to null");
					}
				}
				queueingNum = data.getInt("queueingPos");
			} catch (JSONException e) {
				queueingNum = 99;
			} catch (ParseException e) {
				queueingNum = 99;
			}
//			System.out.println("queueingNum: " + queueingNum + " queueingTime: " + queueingTime + " status: " + status);
			if(status == State.Ready) {
				showReady();
			} else if (queueingMode && isChargingSession()) {
				if(queueingNum == 0) {
					startCountDownTimer("TS", selectTime);
					hideQueueing();
					showTimeSelectMode();
					if(status == State.Pause) {
						view.showImage(uiImage.get("Pause"));
						view.hideTag();
					}
				}
				else {
					if (queueingNum == 1) {
						charger.setPilotEnable(false);
						charger.setPilotEnable(true);
					}
					view.updateQueueing(queueingTime, queueingNum);
					}
				
			} else if (!queueingMode && isChargingSession() && queueingNum > 0) {
				if(countDownTimer != null) {
					countDownTimer.cancel();
				}
				if(stopTimer != null) {
					stopTimer.cancel();
				}
			}
			
			break;
		case "retrieveConfig":
			NetworkInterface[] networkIface = NetworkManager.getPhysicalInterface();
			for(int i = 0; i < networkIface.length; ++i) {
				json.put("macAddress", networkIface[i].getMACAddress());
				if(!"0.0.0.0".equals(networkIface[i].getIPAddress())) {
					json.put("ipAddress", networkIface[i].getIPAddress());
				}
			}
			json.put("config", encryptedConfig);
			json.put("fwVer", configinfo.sVersion);
			break;
		case "updateConfig":
			try {
				String data = a.getJSONObject(3).getString("data");
				byte[] con = Base64.getDecoder().decode(data);
				Config c = new Gson().fromJson(new String(EncryptionFactory.decrypt(encryptKey, con)),Config.class);
				if(c == null) {
					json.put("status", "JsonParseException: " + new String(con));
					break;
				}
				FileOutputStream fos = new FileOutputStream(new File("./config.txt"));
				fos.write(con);
				fos.close();
				Logger.writeln("config file updated, Charger status: " + status );
				if(status == State.Ready) {
					view.showImage(uiImage.get("Initialize"));
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						json.put("status", "InterruptedException: " + e.getMessage());
					}
					needRestart = true;
					rebootTask();
				} else {
					isNewConfig = true;
				}
			} catch (FileNotFoundException e) {
				json.put("status", "FileNotFoundException: " + e.getMessage());
			} catch (IOException e) {
				json.put("status", "IOException: " + e.getMessage());
			} catch (IllegalArgumentException e) {
				json.put("status", "IllegalArgumentException: " + e.getMessage());
			} catch (JsonSyntaxException e) {
				json.put("status", "JsonSyntaxException: " + e.getMessage());
			}
			break;
		case "LmsStatusUpdate":
			setLMSState(a.getJSONObject(3).getJSONObject("data"));
			break;
		default:
			json.put("status", "UnknownMessageId: " + a.getJSONObject(3).getString("messageId"));
			break;
		}
		payload.put(json);
//		send(payload);
		if("retrieveConfig".equals(a.getJSONObject(3).getString("messageId"))) {
			sendBackdoor(payload);
		} else {
			if(serverDown && (a.getJSONObject(3).getString("messageId").equals("LmsStatusUpdate") || a.getJSONObject(3).getString("messageId").equals("queueingDetails") )) {
				//no send
			} else {
				send(payload);
			}
		}

	}
	
	private void setLMSState(JSONObject data) {
		System.out.println(data);
		switch (data.getString("LmsChargingStatus")) {
			case "Suspended":
				isLMSSuspended = true;
				if(status == State.Authorize || isChargingSession()) {
					stopTimeCharge.doClick();
				} else {
					view.showImage(uiImage.get("LMS-Suspended"));
				}
			break;
			case "Available":
				if(isLMSSuspended) {
					isLMSSuspended = false;
					Logger.writeln("charger.getState(): " + charger.getState());
					if(	charger.getState() == IECCharger.STATE_CABLE_DISCONNECT) {
						setState(State.Ready);
					} else {
						setState(status);
					}

				}
			break;
		}
		
	}

	protected JSONObject getConfiguration() {
		JSONObject networkSetting = new JSONObject();
		
		NetworkInterface[] networkIface = NetworkManager.getPhysicalInterface();
		for(int i = 0; i < networkIface.length; ++i) {
			JSONObject network = new JSONObject();
			network.put("MAC", networkIface[i].getMACAddress());
			network.put("IP", networkIface[i].getIPAddress());
			network.put("Netmask", networkIface[i].getNetmask());
			network.put("Gateway", networkIface[i].getGateway());
			network.put("DHCP", networkIface[i].getDHCP());
			if(networkIface[i].isWireless()) {
				network.put("SSID", (networkIface[i].getSSID() == null ? "" : networkIface[i].getSSID()));
				network.put("Password", (networkIface[i].getKey() == null ? "" : networkIface[i].getKey()));
			}
			JSONObject option = new JSONObject();
			option.put("Type", "Network");
			option.put("Authority", "Everyone");
			option.put("Value", network);
			networkSetting.put(networkIface[i].getName(), option);
		}
		JSONObject configuration = new JSONObject();
		configuration.put("EVSE", config);
		configuration.put("Network", networkSetting);
		if(ocppServer != null) {
			configuration.put("LMS", lmsConfig);
			
			JSONObject authenticate = new JSONObject();
			JSONObject nfcSetting = new JSONObject();
			nfcSetting.put("Type", "List");
			nfcSetting.put("Authority", "Everyone");
			nfcSetting.put("Value", ocppServer.getAuthorizedList());
			authenticate.put("NFC", nfcSetting);
			configuration.put("Authenticate", authenticate);
		}
		return configuration;
	}

	public void setConfig(String config) {
		System.out.println(this.getClass().getSimpleName() + " Set : " + config);
	}
	
	public String getConfig() {
////		JSONObject networkSetting = new JSONObject();
////		
////		NetworkInterface[] networkIface = NetworkManager.getPhysicalInterface();
////		for(int i = 0; i < networkIface.length; ++i) {
////			JSONObject network = new JSONObject();
////			network.put("MAC", networkIface[i].getMACAddress());
////			network.put("IP", networkIface[i].getIPAddress());
////			network.put("Netmask", networkIface[i].getNetmask());
////			network.put("Gateway", networkIface[i].getGateway());
////			network.put("DHCP", networkIface[i].getDHCP());
////			if(networkIface[i].isWireless()) {
////				network.put("SSID", (networkIface[i].getSSID() == null ? "" : networkIface[i].getSSID()));
////				network.put("Password", (networkIface[i].getKey() == null ? "" : networkIface[i].getKey()));
////			}
////			JSONObject option = new JSONObject();
////			option.put("Type", "Network");
////			option.put("Authority", "Everyone");
////			option.put("Value", network);
////			networkSetting.put(networkIface[i].getName(), option);
////		}
////		JSONObject configuration = new JSONObject();
////		configuration.put("EVSE", config);
////		configuration.put("Network", networkSetting);
//		if(ocppServer != null) {
//			configuration.put("LMS", lmsConfig);
//			
//			if(config.getJSONObject("Authentication").getString("Value").equals("NFC")) {
//				JSONObject authenticate = new JSONObject();
//				JSONObject nfcSetting = new JSONObject();
//				nfcSetting.put("Type", "List");
//				nfcSetting.put("Authority", "Everyone");
//				nfcSetting.put("Value", ocppServer.getAuthorizedList());
//				authenticate.put("NFC", nfcSetting);
//				configuration.put("Authenticate", authenticate);
//			}
//		}
//		return configuration.toString();
		return config.toString();
	}

	private OCPP.ChargePointStatus getOCPPState(State status) {
		switch (status) {
		case Ready:
		case Plugging:
		case Replug:
		case NetworkDown:
			return OCPP.ChargePointStatus.Available;
		case Locked:
		case Authorize:
		case Authorizing:
			return OCPP.ChargePointStatus.Preparing;
		case Charging:
			return OCPP.ChargePointStatus.Charging;
		case Authorized:
		case Pause:
		case Ventilation:
			return OCPP.ChargePointStatus.SuspendedEV;
		case Expire:
			return OCPP.ChargePointStatus.SuspendedEVSE;
		case Unplug:
			if(config.getJSONObject("Authentication").getString("Value").equals("Autotoll")) {
				return OCPP.ChargePointStatus.Replug;
			} else {
				return OCPP.ChargePointStatus.SuspendedEV;
			}
		case Unlock:
		case Unlocked:
			return OCPP.ChargePointStatus.Finishing;
		default:
			return OCPP.ChargePointStatus.Unavailable;
		}
	}

	public void heartbeat() {
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put("id");
		payload.put("Heartbeat");
		JSONObject json = new JSONObject();
		payload.put(json);
		Logger.writeln("send heartbeat");
		send(payload);
	}

	public void heartbeatConf(JSONArray a) {
		OCPP.Heartbeat.conf conf = new Gson().fromJson(a.getJSONObject(2).toString(), OCPP.Heartbeat.conf.class);
		setSystemTime(conf.getCurrentTime());
	}

	public void  heartbeatBackdoor() {
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put("id");
		payload.put("Heartbeat");
		JSONObject json = new JSONObject();
		payload.put(json);
		sendBackdoor(payload);
	}

	private boolean isChargingSession() {				
		return session.exists();
	}

	private JSONArray meterValues(OCPP.MeterValue[] power) {
		JsonArray payload = new JsonArray();
		payload.add(2);
		payload.add("id");
		payload.add("MeterValues");
		payload.add(new Gson().toJsonTree(new MeterValues.req(1, tid, power)).getAsJsonObject());
		return new JSONArray(new Gson().toJson(payload));

	}

	private JSONArray remoteStartTransaction(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		OCPP.RemoteStartTransaction.req req = new Gson().fromJson(a.getJSONObject(3).toString(), OCPP.RemoteStartTransaction.req.class);
		idTag = req.getIdTag();
		if(req.getChargingProfile() != null) {// && req.getChargingProfile().getChargingProfilePurpose() == OCPP.ChargingProfilePurposeType.TxProfile) {
			currentProfile = req.getChargingProfile();
		}
		
		if(status == State.Authorize) {
			payload.put(new JSONObject(new Gson().toJson(new OCPP.RemoteStartTransaction.conf(OCPP.RemoteStartStopStatus.Accepted))));
		} else {
			payload.put(new JSONObject(new Gson().toJson(new OCPP.RemoteStartTransaction.conf(OCPP.RemoteStartStopStatus.Rejected))));
			payload.put(new JSONObject().put("reason", "remoteStartTransaction status: " + status));
		}

		if (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {		// added in V2_5
			charger.setChargerCurrentLimitStartFlag(true);
		}
		
		return payload;
	}

	private JSONArray remoteStopTransaction(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		JSONObject json = new JSONObject();
		Logger.writeln("isChargingSession: " + isChargingSession() + ", tid: " + tid);
		if(isChargingSession() && tid == a.getJSONObject(3).getInt("transactionId")) {
			json.put("status", "Accepted");
			payload.put(json);
			Logger.writeln("remoteStopTransaction.");
			stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Remote)));
//			new Timer().schedule(new TimerTask() {
//
//				@Override
//				public void run() {
//					Logger.writeln("remoteStopTransaction.");
//					stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Remote)));
//				}
//				
//			}, 100);
		} else {
			json.put("status", "Rejected");
			json.put("reason", "remoteStopTransaction isChargingSession:" + isChargingSession()  + " ,tid: " + tid);
			payload.put(json);
			send(payload);
		}
		return payload;
	}
	
 	private void sendMeterValue(JSONArray msg) {
 		if(status == State.NetworkDown) {
 			tempMeterValues.add(msg);
			Logger.writeln("send to tempMeterValues");
 		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
				send(msg);
			}
			}).start();
 		}
 	}


	
	private JSONArray send(JSONArray msg) {
		if(msg == null) {
			return null;
		}
		if(msg.getInt(0) == 2) {
			synchronized(ChargingStation.class) {
				msg.put(1, Integer.toString(++msgSerial));
				Logger.writeln("ocppMsgSend: " + msg.toString());
				boolean isSend = WebSocketClient.sendText(msg.toString(), config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText);
				long time = System.currentTimeMillis();
				if(!isSend) {
					Logger.writeln("Not sent msg");
				}
				try {
					while(isSend && !serverReply.has(msg.getString(1)) && (System.currentTimeMillis() - time) < timeout) {
						//wait(timeout);
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					Logger.writeln(e.getMessage());
				}
			
				if(serverReply.has(msg.getString(1))) {
					serverDown = false;
					Logger.writeln("previousActiveState: " + previousActiveState);
					Logger.writeln("status: " + status);
					Logger.writeln("charger.isCablePlugged: " + charger.isCablePlugged());
					Logger.writeln("charger.isLocked: " + charger.isLocked());
					Logger.writeln("isChargingSession: " + isChargingSession());
					if(status == State.NetworkDown && isOperative) {
						State nextState = null;
						switch(previousActiveState) {
						case Charging:
							if (charger.isCablePlugged() && isChargingSession()) {
								nextState = previousActiveState;
							} else {
								if(powerTimer != null) {
									Logger.writeln("powerTimer shutdown by not charging.");
									powerTimer.shutdownNow();
								}
								nextState = State.Ready;
							}
							break;
							
						case Initialize:
						case Unlocked:
							nextState = State.Ready;
							break;
						default:
							nextState = previousActiveState;
							break;
						}
						Logger.writeln("Network come back, set state to: " + nextState.name());
						setState(nextState);
						setEnable(true);
						
						if(tempMeterValues != null && tempMeterValues.size() > 0) {
							for(int a = 0 ; a < tempMeterValues.size() ; a++) {
								try {
									Logger.writeln("send MeterValue history: " + tempMeterValues.get(a).toString());
									sendMeterValue(tempMeterValues.get(a));
									tempMeterValues.remove(a);
									Thread.sleep(100);
								} catch (InterruptedException e) {
									Logger.writeln(e.getMessage());
								}
							}
						}
					}
				} else {
					try {
						Logger.writeln("No resply from server @ " + msg.get(1));
						Logger.writeln("ServerReply is " + serverReply.toString());
						Logger.writeln("serverDown: " + serverDown);
						if(!serverDown) {
							serverDown = true;
							WebSocketClient.close(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText);
						}
						serverDown = true;
						setState(State.NetworkDown);
						if(!isChargingSession()) {
							setEnable(false);
						} else {
//							charger.setChargeCurrent(0);
						}
						if(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/")) {
							Logger.writeln("local ocpp down setting up new OCPP server");		
							Point p = new Point(100, 100);	
							if(ocppServer != null) {
								ocppServer.stopServer();
								ocppServer = null;
							}
							ocppServer = new OcppServer(p, false, new File("."), config);
						}
					} catch(Exception e) {
						Logger.writeln("ServerReply Fail, Exception:" + e.getMessage());
					}
					if("MeterValues".equals(msg.get(2))) {
						Logger.writeln("save MeterValue: " + msg.toString());
						tempMeterValues.add(msg);
					}
					
				}
			}			
		} else {
			Logger.writeln("ocppMsgSend: " + msg.toString());
			WebSocketClient.sendText(msg.toString(), config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value") + "?cipher=" + encryptedOCPPText);
		}

		return (JSONArray)serverReply.remove(msg.getString(1));
	}

	private JSONArray sendBackdoor(JSONArray msg) {
		if(config.getJSONObject("Serial No").getString("Value").equals("0")) {
			Logger.writeln("sendBackdoor: " + msg);
			return null;
		}
		
		synchronized(ChargingStation.class) {
			if(msg.getInt(0) == 2) {
				msg.put(1, Integer.toString(++msgSerialBD));
			}
			boolean isSend = WebSocketClient.sendText(msg.toString(), config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value") + "?cipher=" + encryptedOCPPText);
			if(msg.getInt(0) == 2) {
				long time = System.currentTimeMillis();
				try {
					while(isSend && !backdoorReply.has(msg.getString(1)) && (System.currentTimeMillis() - time) < timeout) {
						//wait(timeout);
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
				}
	
				if(backdoorReply.has(msg.getString(1))) {
					backdoorDown = false;
				} else {
					Logger.writeln("No reply from backdoor @ " + msg.get(1));
					backdoorDown = true;
					WebSocketClient.close(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value") + "?cipher=" + encryptedOCPPText);
				}
			}
			return (JSONArray)backdoorReply.remove(msg.getString(1));
		}		
	}

//	private void sendLprsMessage(String msg) {
//		if(housingSocietyLprsSocket == null) {
//			try {
//				housingSocietyLprsSocket = new Socket(config.getJSONObject("LPRS").getString("Value"), 5000);
//				housingSocietyLprsIS = housingSocietyLprsSocket.getInputStream();
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		if(housingSocietyLprsSocket != null) {
//			try {
//				housingSocietyLprsSocket.getOutputStream().write(msg.getBytes());
//				Logger.writeln("Send hs send ok"+msg.toString());
//			} catch (IOException e) {
//				housingSocietyLprsSocket = null;
//				Logger.writeln("Send hs msg fail    "+msg.toString());
//			}
//		}
//
//	}
	
	private synchronized void incrDecrLprsMessageBuf(String msg, boolean indicator) {			
		if(indicator) {
			lprsMsgBuf.add(msg);
		}else {
			lprsMsgBuf.remove(0);
		}
	}
		
	private JSONArray setChargingProfile(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		
		OCPP.ChargingProfile profile = new Gson().fromJson(a.getJSONObject(3).getJSONObject("csChargingProfiles").toString(), OCPP.ChargingProfile.class);
		Logger.writeln("setChargingProfile: " + profile.getChargingProfilePurpose());
		switch(profile.getChargingProfilePurpose()) {
		case TxProfile:
			System.out.println("isChargingSession: " + isChargingSession());
			if(isChargingSession()) {
				Logger.writeln("SCP 1");
				payload.put(new JSONObject().put("status", updateChargingProfile(profile)));
			} else {
				Logger.writeln("SCP 2");
				tempProfile = profile;
				System.out.println("save TxProfile to tempProfile");
				payload.put(new JSONObject().put("status", OCPP.ChargingProfileStatus.Rejected).put("reason", "isChargingSession: " + isChargingSession()));
			}
			break;
		case TxDefaultProfile:
			if(profile.getStackLevel() >= defaultProfile.getStackLevel()) {
				Logger.writeln("SCP 3");
				defaultProfile = profile;
			}
			Logger.writeln("SCP 4");
			payload.put(new JSONObject().put("status", OCPP.ChargingProfileStatus.Accepted));
			break;
		case ChargePointMaxProfile:
			if(maxProfile == null) {
				maxProfile = profile;
			} else {
				if(profile.getStackLevel() >= maxProfile.getStackLevel()) {
					Logger.writeln("SCP 5");
					maxProfile = profile;
				}
			}
			Logger.writeln("SCP 6");
			payload.put(new JSONObject().put("status", OCPP.ChargingProfileStatus.Accepted));
			break;
		default:
			Logger.writeln("SCP 7");
			payload.put(new JSONObject().put("status", OCPP.ChargingProfileStatus.NotSupported));
			break;
		}
		
		return payload;
	}

//	public void setContactor(String name, boolean e) {
//		switch(name) {
//		case "L1":
//			enable1.setState(e ? GPIO.HIGH : GPIO.LOW);
//			break;
//		case "L2":
//			enable2.setState(e ? GPIO.HIGH : GPIO.LOW);
//			break;
//		case "L3":
//			enable3.setState(e ? GPIO.HIGH : GPIO.LOW);
//			break;
//		case "L2-1":
//			enable21.setState(e ? GPIO.HIGH : GPIO.LOW);
//			break;
//		case "L3-1":
//			enable31.setState(e ? GPIO.HIGH : GPIO.LOW);
//			break;
//		default:
//			break;
//		}
//	}

	private void setEnable(boolean enable) {
		if(enable) {
			charger.start();
		} else {
			if(!isChargingSession()) {
				charger.stop();
			}
		}		
	}

	private void setHeartbeat(int interval) {
		if(heartbeatTimer != null) {
			heartbeatTimer.cancel();
		}
		heartbeatTimer = new Timer();
		heartbeatTimer.schedule(new TimerTask() {
			private int count = 0;
			
			@Override
			public void run() {
				if(status != State.NetworkDown) {
					if(++count >= interval) {
						count = 0;
						heartbeat();
					}
				} else {
					int beat = 5 <= timeout/1000 ? timeout/1000+1 : 5;
					if(++count >= beat) {
						count = 0;
						heartbeat();
					}
				}
			}

		}, 1000, 1000);
		
		if(!(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value")).equals(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value"))) {
			if(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/")) {
			}
			new Timer().schedule(new TimerTask() {
				private int count = 0;
				int beat = config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/") ? 2 : 7;
				@Override
				public void run() {
					if(status != State.NetworkDown && !backdoorDown) {
						heartbeatBackdoor();
						count = 0;
					} else {
						if(++count >= beat) {
							count = 0;
							heartbeatBackdoor();
						}
					}
				}
				
			}, 300000, 300000);
		}
	}

	public synchronized void setState(State newState) {
		Logger.writeln("newState: " + newState + ", oldState: " + status);
		boolean isPlugNChargeUnlockbutton = false;
		boolean timeSelectMode = config.getJSONObject("Authentication").getString("Value").equals("TimeSelect");
		if(criticalFaultTriggered) {
			newState = State.CriticalFault;
			status = State.CriticalFault;
		}
		//TODO get queueing single
		
		// On exit
		switch (status) {
		case Authorize:
			if(barcode != null) {
				barcode.removeAllListeners();
			}
//			nfcReader.removeAllListeners();
			
			if(unlockTimer != null) {
				unlockTimer.cancel();
			}
			
			if(timeSelectMode) {
				view.remove(addHour);
				view.remove(minusHour);
				view.remove(addMin);
				view.remove(minusMin);
				view.remove(startCharge);
				view.remove(fullyCharge);
				view.remove(resetCharge);
				view.remove(stopQueueing);
				view.hideTimeSlot();
				view.hideQueueing();
				view.hideSelectedMsg();
			}
			
			if(config.getJSONObject("Time Select Fully Charge").getString("Value").equals("Show")) {
				view.remove(fullyCharge);
			}
			Logger.writeln("try on Authorize get Mater Value: " + getMaterValue("chargerEnergy"));
			break;
		case Charging:
			if(newState != State.NetworkDown) {
				contactor.setState(false);
			}
			LEDTimer.interrupt();
			if(messageTimer != null) {
				messageTimer.shutdown();
				while(!messageTimer.isTerminated()) {
				}
			}
			
			if(newState != State.Charging && config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
				view.remove(stopTimeCharge);

			}
			break;
		case CPError:
			if(newState != State.CPError) {
				unlockButton.setVisible(false);
			}
			break;
		case Unplug:
			charger.setPilotEnable(false);
			break;
		case NetworkDown:
			if(newState != State.NetworkDown) {
				unlockButton.setVisible(false);
			}
		case Unavailable:
		case Unregister:
			setEnable(true);
			break;
		default:
			break;
		}
		if(newState == State.NetworkDown && status != State.NetworkDown) {
			previousActiveState = status;
		}
		if(timeSelectMode && newState != State.Charging && newState != State.Authorize) {
			view.hideTag();
			view.hideRemaingTime();
			view.remove(stopTimeCharge);
		}
		
		State previousState = status;
		

		
		status = newState;
		Logger.writeln(previousState + " to " + newState);
		System.out.println("status: " + status);
		
		// On enter
		if(queueingMode) {
			view.remove(stopQueueing);
			view.hideQueueing();
		}
		switch (status) {
		case Authorize:
			if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {
				idTag = "Plug & Charge";
				new Thread(new Runnable() {
					@Override
					public void run() {
						setState(State.Authorized);
					}
				}).start();
			} else if(timeSelectMode) {
				selectTime = 0;
				tsBtnEna = true;

				if(queueingNum == 0) {
					view.showImage(uiImage.get("TimeSelect"));
					view.updateSelectedMsg("Estimated Start Time ��������: Immediately 疇嚙蝓傢色��");
				} else {
					view.showImage(uiImage.get("TimeSelectQueueing"));
					if(queueingTime == null) {
						view.updateSelectedMsg("Estimated Start Time ��������:\nUnable to estimate 疆�秉事衛怏蕭癡穡��");
					} else {
					view.updateSelectedMsg("Estimated Start Time ��������: " + view.queueingTimeFt.format(queueingTime));
					}
				}
				if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
					view.add(stopTimeCharge, new Integer(2));
					stopTimeCharge.setBounds(view.getUIConfig("ui_config/stopTimeCharge/selecting/x"), view.getUIConfig("ui_config/stopTimeCharge/selecting/y"), view.getUIConfig("ui_config/stopTimeCharge/selecting/w"), view.getUIConfig("ui_config/stopTimeCharge/selecting/h"));
				}
				updateTimeBtn(true, false, false);
				view.add(addHour, new Integer(2));
				view.add(minusHour, new Integer(2));
				view.add(addMin, new Integer(2));
				view.add(minusMin, new Integer(2));
				view.add(startCharge, new Integer(2));
				if(config.getJSONObject("Time Select Fully Charge").getString("Value").equals("Show")) {
					view.add(fullyCharge, new Integer(2));
				}
				view.add(resetCharge, new Integer(2));
				view.showTimeSlot(selectTime);

				double phaseAVoltage = getMaterValue("chargerVoltageA");
				Logger.writeln("(Authorize)getVoltage: " + phaseAVoltage);
//				view.showVoltageTag("" + Math.round(phaseAVoltage));
				
				statusLED.setColor(ChargingStationUI.Green);
				view.repaint();
//				unlockButton.setVisible(true);					//2020-05-29 for adding STOP button
				if(isChargingSession() && config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/") && 
						config.getJSONObject("Authentication").getString("Value").equals("TimeSelect") && previousState == State.Locked) {
					Logger.writeln("session exist so resume charging!");
					selectTime = TIME_SELECT_FULL;
					TimeSlotSelect("TS", selectTime);
				}
			} else {
				checkNonSentStopTransaction();
				view.showImage(uiImage.get("Authorize"));
				statusLED.setColor(ChargingStationUI.Green);
				if(barcode != null) {
					barcode.addEventListener(barcodeListener);
				}
				Logger.writeln("Add Listener - Authorize");
				if(idleUnlockInterval > 0) {
					unlockTimer = new Timer();
					unlockTimer.schedule(new TimerTask() {
	
						@Override
						public void run() {
							Logger.writeln("Auto unlock");
							charger.setCableLock(false);
						}
	
					}, idleUnlockInterval * 1000);
				}
				unlockButton.setVisible(true);					//2020-05-29 for adding STOP button
			
			}			
			break;
		case Authorized:
			if( (config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/") || previousState == State.Authorizing) && charger.isLocked() || config.getJSONObject("Authentication").getString("Value").equals("Kiosk") ) {
				new Thread(new Runnable() {
					@Override
					public void run() {
	//					charger.setChargeCurrent((float)config.getJSONObject("Default Capacity (A)").getInt("Value"));
						System.out.println("startTransactionConf");
						if(config.getJSONObject("Charging detail").getString("Value").equals("Show")) {
							view.showInfo("-----");
						}
						if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
							stopTimeCharge.setBounds(view.getUIConfig("ui_config/stopTimeCharge/selecting/x"), view.getUIConfig("ui_config/stopTimeCharge/selecting/y"), view.getUIConfig("ui_config/stopTimeCharge/selecting/w"), view.getUIConfig("ui_config/stopTimeCharge/selecting/h"));
						}
	
						charger.setChargeCurrent(config.getJSONObject("Default Capacity (A)").getInt("Value"));
						currentProfile = defaultProfile;
						startTransactionConf(send(startTransaction(idTag)));
						Logger.writeln("on Authorized: previousState: " + previousState + ", charger.isLocked: " + charger.isLocked());
						if(!charger.isLocked()) {
							lockOnCharging(previousState);
						}
						if(barcode != null) {
							barcode.addEventListener(barcodeListener);
						}
	//					qrReadThread = new Thread(new Runnable() {
	//
	//						@Override
	//						public void run() {
	//							while(isChargingSession()) {
	//								String code;
	//								
	//						        do {
	//						        	code = cam.readCode();
	//						        } while(code == null);
	//						        
	//								authorizeConf(send(authorize(code, OcppClient.QRCODE)));
	//							}
	//						}
	//						
	//					});
	//					qrReadThread.start();
					}
				}).start();
	
				view.showImage(uiImage.get("Valid"));
				statusLED.setColor(ChargingStationUI.Blue);
				unlockButton.setVisible(false);						//2020-05-26 for re-enable "STOP" screen button	end here
			} else {
				stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Other)));
			}
			break;
		case Charging:
			contactor.setState(true);
			unlockButton.setVisible(false);	
			if(timeSelectMode) {
				if(queueingNum > 0) {
					showQueueing();
				} else {
					showTimeSelectMode();
//					startCountDownTimer("TS", selectTime);
				}
			} else {
				view.showImage(uiImage.get("Charging"));
			}
			
			if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {
				unlockButton.setVisible(true);					// 2020-06-30 add for plug & charge mode
				Logger.writeln("Show unlockButton: charging 2");
			}	
//			LEDTimer = new Thread(new Runnable() {
//
//				@Override
//				public void run() {
//					try {
//						statusLED.setColor(Color.BLUE);
//						int i = 0;
//						while(status == State.Charging) {
//							long delay = 5;
//							switch(++i) {
//							case 100:
//								i = 0;
//								break;
//							case 0:
//							case 1:
//							case 2:
//							case 3:
//							case 4:
//							case 5:
//							case 6:
//							case 7:
//							case 8:
//							case 9:
//								statusLED.shift(LED.BACKWARD, new Color(255, 255, 255));
//								break;
//							default:
//								if(i < 50) {
//									statusLED.shift(LED.BACKWARD, new Color(0, 0, 255));
//								}
//								break;
//							}
//							Thread.sleep(delay);
//							
//						}
//					} catch (InterruptedException e) {
//					}
//				}
//				
//			});
//			LEDTimer.start();
			
			LEDTimer = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
				//		statusLED.setColor(Color.BLUE);
				//		int i = 0;
						while(status == State.Charging) {
							long delay = 5;
							Thread.sleep(delay);
						}
					} catch (InterruptedException e) {
					}
				}
				
			});
			LEDTimer.start();			
			
			messageTimer = Executors.newScheduledThreadPool(1);
			messageTimer.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					if(config.getJSONObject("Authentication").getString("Value").equals("Autotoll")) {
						Dimension d = view.getSize();
						BufferedImage img = new BufferedImage(d.width, 4000, BufferedImage.TYPE_4BYTE_ABGR);
						Graphics g = img.getGraphics();
						g.setColor(Color.BLACK);
	
						Font f = new Font("Arial", Font.PLAIN, 36);
						g.setFont(f);
						int minute = (int) (new Date().getTime() - startTime.getTime()) / 60000;
						int hour = minute / 60;
						minute %= 60;
	
						String ctime = (hour < 10 ? " " + hour : hour) + "H " + (minute < 10 ? "0" + minute : minute) + "Min";
						String cfee = " " + (serverDown ? ((hour * 60 + minute) / unitLength + 1) * unitPrice : chargingFee);
						String cno = " " + config.getJSONObject("Station Name").getString("Value");
	
						g.drawString(ctime, d.width / 2 + 100, d.height / 2 - 40);
						g.drawString(cfee, d.width / 2 + 100, d.height / 2 + 30);
						g.drawString(cno, d.width / 2 + 100, d.height / 2 + 100);
						
						g.dispose();
	
						view.showOverlay(img);
					}
				}	
			}, 0, 1, TimeUnit.SECONDS);
			if(!charger.isLocked()) {
				lockOnCharging(previousState);
			}
			break;
		case CPError:
			view.showImage(uiImage.get("CPError"));
			if(previousState == State.Charging || previousState == State.Pause) {
				unlockButton.setVisible(true);	
			}
			if(!isChargingSession()) {
				charger.setCableLock(false);
			}
			break;
		case Expire:
			view.showImage(uiImage.get("Finish"));
			statusLED.setColor(ChargingStationUI.Green);
			break;
		case Fault:
			view.showImage(uiImage.get("Fault"));
			break;
		case Pause:
		case Ventilation:
			unlockButton.setVisible(false);	
			if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
				stopTimeCharge.setBounds(view.getUIConfig("ui_config/stopTimeCharge/charging/x"), view.getUIConfig("ui_config/stopTimeCharge/charging/y"), view.getUIConfig("ui_config/stopTimeCharge/charging/w"), view.getUIConfig("ui_config/stopTimeCharge/charging/h"));
			}
			if(queueingNum > 0) {
				showQueueing();
			} else {
				view.showImage(uiImage.get("Pause"));
				view.hideTag();
				if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
					view.add(stopTimeCharge, new Integer(2));
				}
				if(config.getJSONObject("Authentication").getString("Value").equals("Plug & Charge")) {
					unlockButton.setVisible(true);					// 2020-06-30 add for plug & charge mode
					Logger.writeln("Show unlockButton: pause 2");
				}
			}
			statusLED.setColor(ChargingStationUI.Orange);
			if(!charger.isLocked()) {
				lockOnCharging(previousState);
			}
			break;
		case Ready:
			showReady();
			checkNonSentStopTransaction();
			break;
		case Plugging:
			view.showImage(uiImage.get("Plugging"));
			statusLED.setColor(ChargingStationUI.Green);
			charger.setCableLock(false);
			break;
		case Locked:
			view.showImage(uiImage.get("Locked"));
			statusLED.setColor(ChargingStationUI.Green);

			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					if(status == State.Locked) {
						setState(State.Authorize);
					}
				}

			}, 1000);
			break;
		case Replug:
			view.showImage(uiImage.get("Replug"));
			statusLED.setColor(ChargingStationUI.Red);
			if(config.getJSONObject("Type").getString("Value").equals("Socket"))
			charger.setCableLock(false);
			break;
		case Unplug:
			view.showImage(uiImage.get("Replug"));
			statusLED.setColor(ChargingStationUI.Red);
			charger.setPilotEnable(false);
			break;

		case Unavailable:
		case Unregister:
			statusLED.setColor(ChargingStationUI.Red);
			setEnable(false);	
		case NetworkDown:
			view.showImage(uiImage.get("Fault"));
			unlockButton.setVisible(true);	//2020-08-28 adding unlock button when network down
			break;
			
		case Unlock:
			unlockButton.setVisible(false);					//2020-05-28 change to remove  STOP" screen button for new UI				
			switch(fault) {
			case FaultState.OVERCURRENT:
				view.showImage(uiImage.get("OvercurrentUnlock"));
				statusLED.setColor(ChargingStationUI.Red);	
				break;
			case FaultState.ESTOP_TRIGGERED:
				view.showImage(uiImage.get("EStopUnlock"));
				Logger.writeln("unlock happens");
				statusLED.setColor(ChargingStationUI.Red);	
				break;
			case FaultState.NO_FAULT:
			default:
				if(isLMSSuspended) {
					view.showImage(uiImage.get("LMS-Suspended"));
				} else {
					view.showImage(uiImage.get("Unlock"));
				}
				statusLED.setColor(ChargingStationUI.Green);	
				break;
			}			
			
			break;	

		case Unlocked:
			switch(fault) {
			case FaultState.OVERCURRENT:
				view.showImage(uiImage.get("OvercurrentHalt"));
				statusLED.setColor(ChargingStationUI.Red);
				criticalFaultTriggered = true;
				break;
			case FaultState.ESTOP_TRIGGERED:
				view.showImage(uiImage.get("EStopHalt"));
				statusLED.setColor(ChargingStationUI.Red);	
				criticalFaultTriggered = true;
				break;
			case FaultState.NO_FAULT:
			default:
				if(charger.getType() == IECCharger.SOCKET) {
				view.showImage(uiImage.get("Unlocked"));
				} else {
					view.showImage(uiImage.get("Finish"));
				}	
				statusLED.setColor(ChargingStationUI.Green);
				break;
			}	

			break;
		default:
			break;
		}
		Logger.writeln("status: " + status + ", previousState: " + previousState);
		Logger.writeln("getOCPPState(status): " + getOCPPState(status) + ", getOCPPState(previousState): " + getOCPPState(previousState));
		if(!getOCPPState(status).equals(getOCPPState(previousState))) {
			JSONArray a = statusNotification(status);
			Executors.newSingleThreadExecutor().execute(new Runnable() {
	
				@Override
				public void run() {
					send(a);
					if(!(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value")).equals(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value")) && !backdoorDown) {
						sendBackdoor(a);
					}
				}
				
			});
		}
	}
	
	private void checkNonSentStopTransaction() {
		if(config.getJSONObject("Server Path").getString("Value").equals("ws://127.0.0.1:8086/ocpp/") && 
				config.getJSONObject("Authentication").getString("Value").equals("TimeSelect")) {
			Logger.writeln("local occp server & TimeSelect no check session");
			return;
		}
		if(haveSession()) {
			JSONArray j = new JSONArray();
			j.put(2);
			j.put("id");
			j.put("TriggerMessage");
			j.put(new JSONObject().put("requestedMessage", "StopTransaction"));
			triggerMessage(j);
		}
	}
	
	private void lockOnCharging(State previousState) {
		Logger.writeln("The charging is unlock status: " + previousState + " and isCablePlugged: " + charger.isCablePlugged());
		if(charger.isCablePlugged()) {
			charger.setCableLock(true);
			Logger.writeln("setCableLock");
		}
//		try {
//			Thread.sleep(500);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if(tid != -1) {
//			stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.EVDisconnected)));
//		} else {
//			charger.setCableLock(false);
//		}
	}

	private void showTimeSelectMode() {
		if(selectTime < TIME_SELECT_FULL) {
			view.showImage(uiImage.get("Charging-time"));
		} else {
			view.showImage(uiImage.get("Charging-time-full"));
		}
		view.showTag();
		if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
			view.add(stopTimeCharge, new Integer(2));
		}
	}

	private void start() {
		String auth = config.getJSONObject("Authentication").getString("Value");
		if(auth.equals("SmartPad")) {
			try {
				String timeSync = "";
				view.showInfo("Time synchronizing...");
				do {
					Process p = Runtime.getRuntime().exec("timedatectl");
					p.waitFor();
					InputStream is = p.getInputStream();
					byte[] b = new byte[is.available()];
					is.read(b);
					is.close();
					StringTokenizer st = new StringTokenizer(new String(b), "\n");
					while(st.hasMoreTokens()) {
						timeSync = st.nextToken();
						if(timeSync.indexOf("synchronized") != -1) {
							break;
						}
					}
					Thread.sleep(100);
				} while(timeSync.indexOf("yes", timeSync.indexOf("synchronized")) == -1);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				Logger.writeln("start: " + e.getMessage());
			}
		}		
		view.showInfo("");
		
		JSONArray payload;
		String macAddress = "";
		String ipAddress = "";
		NetworkInterface[] networkIface = NetworkManager.getPhysicalInterface();
		for(int i = 0; i < networkIface.length; ++i) {
			macAddress = networkIface[i].getMACAddress();
			if(!"0.0.0.0".equals(networkIface[i].getIPAddress())) {
				ipAddress = networkIface[i].getIPAddress();
			}
		}
		JSONArray bootNotification = new JSONArray();
		bootNotification.put(2);
		bootNotification.put("id");
		bootNotification.put("BootNotification");
		if(!config.has("Custom Fields")|| "Yes".equals(config.getJSONObject("Custom Fields").getString("Value"))) {
			bootNotification.put(new JSONObject().put("chargePointSerialNumber", config.getJSONObject("Serial No").getString("Value"))
					.put("chargePointModel", "SLATE 2")
					.put("chargePointVendor", "Cornerstone")
					.put("hashMd5", hashMd5)
					.put("ipAddress", ipAddress)
					.put("macAddress", macAddress)
					.put("firmwareVersion", configinfo.sVersion));
		} else {
			bootNotification.put(new JSONObject().put("chargePointSerialNumber", config.getJSONObject("Serial No").getString("Value"))
					.put("chargePointModel", "SLATE 2")
					.put("chargePointVendor", "Cornerstone")
					.put("firmwareVersion", configinfo.sVersion));
		}

		do {
			payload = send(bootNotification);
			if(payload != null) {
				try {
					OCPP.BootNotification.conf conf = new Gson().fromJson(payload.getJSONObject(2).toString(), OCPP.BootNotification.conf.class);
					setSystemTime(conf.getCurrentTime());

					if(conf.getStatus() == OCPP.RegistrationStatus.Accepted) {
						isOperative = true;
						charger.addChangeListener(this);
						setEnable(true);
						setHeartbeat(conf.getInterval());
						Logger.writeln("Charger started");
						break;

//						StringTokenizer cards = new StringTokenizer(payload.getJSONObject(2).getString("adminCardList"), "\n");
//						adminCard = new byte[cards.countTokens()][];
//						for(int i = 0; cards.hasMoreElements(); ++i) {
//							adminCard[i] = HexFactory.toBytes(cards.nextToken(), ' ');
//						}
					} else {
						setState(State.Unregister);
					}
				} catch (JSONException e) {
					payload = null;
				}
			}
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				Logger.writeln("start: " + e.getMessage());
			}
		} while (payload == null || !charger.isStart());
		
		System.out.println((config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value")).equals(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value")));
		if(!(config.getJSONObject("Server Path").getString("Value") + config.getJSONObject("Station Name").getString("Value")).equals(config.getJSONObject("Backdoor Server").getString("Value") + config.getJSONObject("Serial No").getString("Value"))) {
			sendBackdoor(bootNotification);
		}
	}
	
	
	private synchronized JSONArray startTransaction(String id) {
		this.id = new OCPP.IdToken(id);
		JSONArray payload = new JSONArray();
		payload.put(2)
			   .put("id")
			   .put("StartTransaction");
		JSONObject json = new JSONObject();
		json.put("connectorId", 1)
			.put("idTag", id)
			.put("meterStart", (int)(getMaterValue("chargerEnergy")))
			.put("timestamp", OCPP.dateTime.format(new Date()));
		if(!config.has("Custom Fields") || "Yes".equals(config.getJSONObject("Custom Fields").getString("Value"))) {
			json.put("selectTime", selectTime);
		}
		if(reservation != null) {
			json.put("reservationId", reservation.getReservationId());
		}
		payload.put(json);
		
		if (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")) {		// added in V2_5
			charger.setChargerCurrentLimitStartFlag(true);
		}


		return payload;
	}
	
	private double getMaterValue(String type) {
		boolean canRead = false;
		if(System.currentTimeMillis() - lastMaterRead >= 100) {
			canRead = true;
			lastMaterRead = System.currentTimeMillis();
		}
		Logger.writeln("getMaterValue: " + type + " , can read = " + canRead + ", CP PWM compensation: "+config.getJSONObject("CP PWM offset(mA)").getInt("Value"));
		switch(type) {
			case "chargerEnergy":
				chargerEnergy = canRead ? charger.getEnergy() : chargerEnergy;
				return chargerEnergy;
				
			case "chargerVoltageA":
				double temp = 0;
				if(canRead) {
					temp = charger.getVoltage(PowerMeter.PHASE_A);
					Logger.writeln("VALUE = " + temp );
				}
				chargerVoltageA = temp == chargerEnergy || !canRead ? chargerVoltageA : temp;
				return chargerVoltageA;
				
			case "chargerVoltageB":
				chargerVoltageB = canRead ? charger.getVoltage(PowerMeter.PHASE_B) : chargerVoltageB;
				return chargerVoltageB;
				
			case "chargerVoltageC":
				chargerVoltageC = canRead ? charger.getVoltage(PowerMeter.PHASE_C) : chargerVoltageC;
				return chargerVoltageC;
				
			case "chargerCurrentA":
				chargerCurrentA = canRead ? charger.getCurrent(PowerMeter.PHASE_A) : chargerCurrentA;
				if((canRead) && (config.getJSONObject("Over-Current Protection").getString("Value").equals("Yes"))) {
					Logger.writeln("Phase A current: "+ chargerCurrentA+ " time: "+lastMaterRead);
					boolean over;
					int margin = config.getJSONObject("Over-Current Margin(%)").getInt("Value");
					double chargerConsumption = ((double)config.getJSONObject("Charger current consumption(mA)").getInt("Value"))/1000;

					over = (boolean)overCurrentChk.overCurrentChk(charger, chargerCurrentA, chargerConsumption, margin, PowerMeter.PHASE_A);
					if(over) {
						fault = FaultState.OVERCURRENT;
						Logger.writeln("Overcurrent detected! Stop any charging!");
						contactor.setState(false);
						plugAndChargeUnlock = true;
						charger.setCableLock(false);
					}
				}				

				return chargerCurrentA;
				
			case "chargerCurrentB":
				chargerCurrentB = canRead ? charger.getCurrent(PowerMeter.PHASE_B) : chargerCurrentB;
				if((canRead) && (config.getJSONObject("Over-Current Protection").getString("Value").equals("Yes"))) {
					Logger.writeln("Phase B current: "+ chargerCurrentB+ " time: "+lastMaterRead);
					boolean over;
					int margin = config.getJSONObject("Over-Current Margin(%)").getInt("Value");
					double chargerConsumption = ((double)config.getJSONObject("Charger current consumption(mA)").getInt("Value"))/1000;
					
					over = (boolean)overCurrentChk.overCurrentChk(charger, chargerCurrentB, chargerConsumption, margin, PowerMeter.PHASE_B);
					if(over) {
						fault = FaultState.OVERCURRENT;
						Logger.writeln("Overcurrent detected! Stop any charging!");
						contactor.setState(false);
						plugAndChargeUnlock = true;
						charger.setCableLock(false);
					}
				}
				return chargerCurrentB;
				
			case "chargerCurrentC":
				chargerCurrentC = canRead ? charger.getCurrent(PowerMeter.PHASE_C) : chargerCurrentC;
				if((canRead) && (config.getJSONObject("Over-Current Protection").getString("Value").equals("Yes"))) {
					Logger.writeln("Phase C current: "+ chargerCurrentC+ " time: "+lastMaterRead);
					boolean over;
					int margin = config.getJSONObject("Over-Current Margin(%)").getInt("Value");
					double chargerConsumption = ((double)config.getJSONObject("Charger current consumption(mA)").getInt("Value"))/1000;					
					over = (boolean)overCurrentChk.overCurrentChk(charger, chargerCurrentC, chargerConsumption, margin, PowerMeter.PHASE_C);
					if(over) {
						fault = FaultState.OVERCURRENT;
						Logger.writeln("Overcurrent detected! Stop any charging!");
						contactor.setState(false);
						plugAndChargeUnlock = true;
						charger.setCableLock(false);
					}
				}
				return chargerCurrentC;
				
			case "chargerTotalPowerFactor":
				chargerTotalPowerFactor = canRead ? charger.getPowerFactor(PowerMeter.PHASE_ABC) : chargerTotalPowerFactor;
				return chargerTotalPowerFactor;				
		}
		return 0;
	}
	
	private double getChargerEnergy() {
		try {
			Thread.sleep(100);
			return getMaterValue("chargerEnergy");
		} catch (InterruptedException e) {
			Logger.writeln("InterruptedException at 'startTransactionConf' between get phase A voltage and current ");
			return 0;
		}
	}
	

	private void startTransactionConf(JSONArray a) {
		Logger.writeln("startTransactionConf JSONArray:" + a);
		if(a == null || a.length() == 0) {
			Logger.writeln("server no response");
			stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Other)));
			return;
		}
		if(status != State.Authorized) {
			Logger.writeln("startTransactionConf state: " + status + " , stopTransaction");
			stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Other)));
		}
		if(config.getJSONObject("Time Select Stop Button").getString("Value").equals("Show")) {
			stopTimeCharge.setBounds(view.getUIConfig("ui_config/stopTimeCharge/charging/x"), view.getUIConfig("ui_config/stopTimeCharge/charging/y"), view.getUIConfig("ui_config/stopTimeCharge/charging/w"), view.getUIConfig("ui_config/stopTimeCharge/charging/h"));
		}
		JSONObject json = a.getJSONObject(2);
		JSONObject idTagInfo = json.getJSONObject("idTagInfo");
		System.out.println("idTagInfo status: " + idTagInfo.getString("status"));
		if(idTagInfo.getString("status").toLowerCase().equals("accepted")) {
			if(!session.exists()) {
				try {
					session.createNewFile();
					Logger.writeln("session created");
				} catch (IOException e) {
					Logger.writeln("chargingTimer IOException1: " + e.getMessage());
				}
			}
			System.out.println("created session");

			tid = json.getInt("transactionId");
			try {
				if(tempProfile != null && tid == tempProfile.getTransactionId() ) {
					System.out.println("using tempProfile to updateChargingProfile, tid: " + tid);
					updateChargingProfile(tempProfile);
					tempProfile = null;
				}
			} catch (NullPointerException ex) {
				
			}
			
			sessionTimer = null;
			effectChargingProfile();
			
			if(chargingTimer != null) {
				Logger.writeln("chargingTimer cancel by start Transaction");
				chargingTimer.cancel();
			}
			chargingTimer = new Timer();
			
		

			chargingTimer.schedule(new TimerTask() {

				@Override
				public void run() {

					try {
						JSONObject json = new JSONObject();
						json.put("idTag", id.getIdToken());
						json.put("meterStop", (int)(getMaterValue("chargerEnergy")));
						json.put("timestamp", OCPP.dateTime.format(new Date()));
						json.put("transactionId", tid);
						json.put("reason", "Reboot");

						FileWriter fw = new FileWriter(session, true);
						fw.write(json.toString() + "\n");
						fw.close();
						Runtime.getRuntime().exec("sync");
					} catch (FileNotFoundException e) {
						Logger.writeln("chargingTimer FileNotFoundException: " + e.getMessage());
					} catch (IOException e) {
						Logger.writeln("chargingTimer IOException2: " + e.getMessage());
					}
				}

			}, 0, 30 * 1000);
			
			if(powerTimer != null) {
				Logger.writeln("powerTimer shutdown by start Transaction");
				powerTimer.shutdownNow();
			}
			powerTimer = Executors.newScheduledThreadPool(1);
			powerTimer.scheduleAtFixedRate(new TimerTask() {
				final int reportInterval = 15;
				DecimalFormat df = new DecimalFormat("#.##");
				DecimalFormat df2 = new DecimalFormat("#.###");
				int count = 0;

				@Override
				public void run() {
					Logger.writeln("PowerTimer Working");
					if(!isChargingSession()) {
						Logger.writeln("powerTimer shutdown by no ChargingSession");
						powerTimer.shutdown();
						return;
					}
					
					double chargerPhaseAVoltage = 0;
					double chargerPhaseACurrent = 0;
					double chargerEnergy = 0;
					double chargerReadTotalPowerFactor=0;
					
					for (int a = 0; a <= 5 ; a++) {
						chargerEnergy = getChargerEnergy();
						if(chargerEnergy != 0) {
							break;
						}
					}
					
					
					try {
						Thread.sleep(50);
						chargerPhaseAVoltage = getMaterValue("chargerVoltageA");
						Thread.sleep(50);
						chargerPhaseACurrent = getMaterValue("chargerCurrentA");
						Thread.sleep(50);
						chargerReadTotalPowerFactor = getMaterValue("chargerTotalPowerFactor");
						Thread.sleep(50);						
						Logger.writeln("scheduleAtFixedRate read PHASE A Power Meter, currentTimeMillis: " + System.currentTimeMillis());
						
					} catch (InterruptedException e) {
						Logger.writeln("InterruptedException at 'startTransactionConf' between get phase A voltage and current ");

								if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
									//  TODO : check & remove this exception in this case....
									if(fault==FaultState.OVERCURRENT) {
										view.showImage(uiImage.get("OvercurrentHalt"));
										statusLED.setColor(ChargingStationUI.Red);
										criticalFaultTriggered = true;										
									}else if(fault==FaultState.ESTOP_TRIGGERED) {
										view.showImage(uiImage.get("EStopHalt"));
										statusLED.setColor(ChargingStationUI.Red);	
										criticalFaultTriggered = true;										
									}
								}	

					}

					String e = "" + Math.round(chargerEnergy);
					String l1v = df.format(chargerPhaseAVoltage);
//					String l2v = df.format(getMaterValue("chargerVoltageB"));
//					String l3v = df.format(getMaterValue("chargerVoltageC"));
					String l1i = df.format(chargerPhaseACurrent);
//					String l2i = df.format(getMaterValue("chargerCurrentB"));
//					String l3i = df.format(charger.getCurrent(PowerMeter.PHASE_C));
					String l3pf = df2.format(chargerReadTotalPowerFactor);
					
					if(maxDemand < chargerPhaseAVoltage * chargerPhaseACurrent) {
						maxDemand = chargerPhaseAVoltage * chargerPhaseACurrent;
						Logger.writeln("new max power demand : " + (int)(JT103.getTemperature() - 273) + "C L1 : " + l1i + "/" + Math.round(charger.getChargeCurrent()) + "A@" + l1v + "V"+" PF="+l3pf);
					}
					if(config.getJSONObject("Charging detail").getString("Value").equals("Show")) {
//						if(config.getJSONObject("Phase").getString("Value").equals("3 Phase")) {
//							view.showInfo((int)(JT103.getTemperature() - 273) + "C L1 : " + l2i + "/" + Math.round(charger.getChargeCurrent()) + "A@" + l2v + "V");
//						} else {
							view.showInfo((int)(JT103.getTemperature() - 273) + "C L1 : " + l1i + "/" + Math.round(charger.getChargeCurrent()) + "A@" + l1v + "V"+" PF="+l3pf);
							Logger.writeln("currentProfile: " + new Gson().toJson(currentProfile));
							//Logger.write("Power factor "+chargerReadTotalPowerFactor);
//						}
					}
					
					if(config.getJSONObject("Authentication").getString("Value").equals("TimeSelect")) {
						double currentEnergy	= Math.round(chargerEnergy);
						double usedEnergy 		= currentEnergy - lastEnergy;
						if(usedEnergy < 0) {
							usedEnergy = (currentEnergy + 10000000000D) - lastEnergy;
						}
	
//						Logger.writeln("getVoltage: " + chargerPhaseAVoltage);
						view.setAllTag(
							"" + Math.round(chargerPhaseAVoltage),
							String.format("%.1f", chargerPhaseACurrent),
							String.format("%.1f", usedEnergy/1000)
						);
					}
				
					if(count++ % reportInterval == 0) {
						FileOutputStream backup = null;
						FileOutputStream fos = null;
						try {
							backup = new FileOutputStream(meterReadingBak);
							backup.write(e.getBytes());
							Process p = Runtime.getRuntime().exec("sync");
							p.waitFor();
							
							fos = new FileOutputStream(meterReading);
							fos.write(e.getBytes());
							p = Runtime.getRuntime().exec("sync");
							p.waitFor();
						} catch (IOException ex) {
							Logger.writeln("powerTimer IOException: " + ex.getMessage());
						} catch (InterruptedException ex) {
							Logger.writeln("powerTimer InterruptedException: " + ex.getMessage());
						} finally{
							try {
								backup.close();
								fos.close();
							} catch (IOException ex) {
								Logger.writeln("close powerTimer file IOException: " + ex.getMessage());
							}

						}
												
						OCPP.SampledValue[] sv;
						if(config.getJSONObject("Phase").getString("Value").equals("3 Phase")) {
							sv = new OCPP.SampledValue[7];
							sv[0] = new OCPP.SampledValue(e, OCPP.Measurand.Energy_Active_Import_Register, OCPP.UnitOfMeasure.Wh);
							sv[1] = new OCPP.SampledValue(l1v, OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L1_N);
							sv[2] = new OCPP.SampledValue(l1i, OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L1);
							try {
								sv[3] = new OCPP.SampledValue(df.format(getMaterValue("chargerVoltageB")), OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L2_N);
								Thread.sleep(50);
								sv[4] = new OCPP.SampledValue(df.format(getMaterValue("chargerCurrentB")), OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L2);
								Thread.sleep(50);
								sv[5] = new OCPP.SampledValue(df.format(getMaterValue("chargerVoltageC")), OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L3_N);
								Thread.sleep(50);
								sv[6] = new OCPP.SampledValue(df.format(getMaterValue("chargerCurrentC")), OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L3);
							} catch (InterruptedException ex) {
								Logger.writeln("InterruptedException at 'startTransactionConf' between get pahse BC voltage and current");
								try {
									sv[3] = new OCPP.SampledValue(df.format(0), OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L2_N);
									sv[4] = new OCPP.SampledValue(df.format(0), OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L2);
									sv[5] = new OCPP.SampledValue(df.format(0), OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L3_N);
									sv[6] = new OCPP.SampledValue(df.format(0), OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L3);	
								}catch (Exception e1) {
									Logger.writeln("InterruptedException at 'startTransactionConf' between get pahse BC voltage and current assume zero error ");
								}
								
								if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
									//  TODO : check & remove this exception in this case....
									if(fault==FaultState.OVERCURRENT) {
										view.showImage(uiImage.get("OvercurrentHalt"));
										statusLED.setColor(ChargingStationUI.Red);
										criticalFaultTriggered = true;										
									}else if(fault==FaultState.ESTOP_TRIGGERED) {
										view.showImage(uiImage.get("EStopHalt"));
										statusLED.setColor(ChargingStationUI.Red);	
										criticalFaultTriggered = true;										
									}
								}			

							}
						} else {
//							File socFile = new File("./soc.txt");
//							if(socFile.exists()) {
//								sv = new OCPP.SampledValue[4];
//							} else {
//								sv = new OCPP.SampledValue[3];
//							}
							sv = new OCPP.SampledValue[3];
							sv[0] = new OCPP.SampledValue(e, OCPP.Measurand.Energy_Active_Import_Register, OCPP.UnitOfMeasure.Wh);
							sv[1] = new OCPP.SampledValue(l1v, OCPP.Measurand.Voltage, OCPP.UnitOfMeasure.V, OCPP.Phase.L1_N);
							sv[2] = new OCPP.SampledValue(l1i, OCPP.Measurand.Current_Import, OCPP.UnitOfMeasure.A, OCPP.Phase.L1);
						}
						if(lastMaterValue != null && lastMaterValue - Long.parseLong(e) > 500) {
							Logger.write("Last Mater Value: " + lastMaterValue + " , Current Mater Value: " + e + " so not send msg");
						} else {
//							File socFile = new File("./soc.txt");
//							if(socFile.exists()) {
//								try {
//									int soc = 0; 
//									Scanner myReader = new Scanner(socFile);
//							      while (myReader.hasNextLine()) {
//							          soc = Integer.parseInt(myReader.nextLine());
//							      }
//							      sv[3] = new OCPP.SampledValue(soc+"", OCPP.Measurand.Soc, OCPP.UnitOfMeasure.Percent);
//								} catch (FileNotFoundException | NumberFormatException e1) {
//									e1.printStackTrace();
//								}
//							}
							lastMaterValue = Long.parseLong(e);
							OCPP.MeterValue[] mv = new OCPP.MeterValue[1];
							mv[0] = new OCPP.MeterValue(sv);
							JSONArray a = meterValues(mv);
							Logger.writeln("PowerTimer Send Meter Value:" + a.toString());
							sendMeterValue(a);
							if(queueingMode) {
								if(new Date().after(queueingTime)) {  
									queueingTime = null;
									Logger.write("Set queueingTime to null");
									view.updateQueueing(queueingTime, queueingNum);
								}
							}
						}
						
	
//						if(powerMeter2 != null) {
//							try {
//								File f = new File("logs/" + config.getJSONObject("Station Name").getString("Value") + "_" + tid + ".csv");
//								FileOutputStream fos;
//								fos = new FileOutputStream(f, true);
//								DecimalFormat df2 = new DecimalFormat("#.##");
//								String record = new String(
//										OCPP.dateTime.format(new Date())+","+
//										df2.format(getMaterValue("chargerVoltageB")) + "," + df2.format(powerMeter2.getVoltage(PowerMeter.PHASE_A)) + "," +
//										df2.format(getMaterValue("chargerVoltageC")) + "," + df2.format(powerMeter2.getVoltage(PowerMeter.PHASE_B)) + "," +
//										df2.format(charger.getVoltage(PowerMeter.PHASE_A)) + "," + df2.format(powerMeter2.getVoltage(PowerMeter.PHASE_C)) + "," +
//										df2.format(charger.getCurrent(PowerMeter.PHASE_B)) + "," + df2.format(powerMeter2.getCurrent(PowerMeter.PHASE_A)) + "," +
//										df2.format(charger.getCurrent(PowerMeter.PHASE_C)) + "," + df2.format(powerMeter2.getCurrent(PowerMeter.PHASE_B)) + "," +
//										df2.format(getMaterValue("chargerCurrentA")) + "," + df2.format(powerMeter2.getCurrent(PowerMeter.PHASE_C)) + "," +
//										df2.format(charger.getEnergy()) + "," + df2.format(powerMeter2.getEnergy()) + "," + df2.format(JT103.getTemperature()) + "\r\n"
//										);
//								fos.write(record.getBytes());
//								fos.close();
//							} catch (IOException ex) {
//							}
//						}					
					}
				}
			}, 0, 2, TimeUnit.SECONDS);
			
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {
					if(charger.getState() == IECCharger.STATE_B && isChargingSession()) {
						setState(State.Pause);
					}
				}
				
			}, 5000);

			while(!session.exists());
			charger.setPilotEnable(true);
			startTime = new Date();

			if(!config.getJSONObject("LPRS").getString("Value").equals("0.0.0.0")) {
				EvseLprsMsg msg = new EvseLprsMsg(++msgSerialLprs, config.getJSONObject("Station Name").getString("Value"), "StartCharge", OCPP.dateTime.format(new Date()));
				//sendLprsMessage(new Gson().toJson(msg));
				incrDecrLprsMessageBuf(new Gson().toJson(msg), LPRS_MSGBUF_INCR);
			}
		} else if(idTagInfo.getString("status").equals("ConcurrentTx")) {
			Logger.writeln("startTransactionConf: status: ConcurrentTx");
			stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.DeAuthorized)));
//			for(int b = 0 ; b <= a.getJSONObject(2).getInt("transactionId") ; b++) {
//				JSONArray resentStop = new JSONArray();
//				resentStop.put(2);
//				resentStop.put("id");
//				resentStop.put("StopTransaction");
//				resentStop.put(new JSONObject("{\"reason\":\"EVDisconnected\",\"idTag\":" + idTag + ",\"transactionId\":" + b + ",\"meterStop\":" + (int)(getMaterValue("chargerEnergy")) + ",\"timestamp\": " + OCPP.dateTime.format(new Date()) + "}"));
//				resendStopTransactionConf(send(resentStop));
//			}	
		}
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		int state = ((Integer)evt.getSource()).intValue();
		Logger.writeln("Charge new state: " + state);
		
		
		if(criticalFaultTriggered) {
			Logger.writeln("Critical error triggerred, activities halted");			
			state = IECCharger.STATE_FAULT_CRITICAL;
		}		
		
		if(!serverDown && status != State.NetworkDown && !isLMSSuspended) {
			switch(state) {
			case IECCharger.STATE_CABLE_CONNECT:
				if(isChargingSession()) {
					if(sessionTimer == null && config.getJSONObject("End upon state A").getString("Value").equals("Yes")) {
						stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.EVDisconnected)));
					} else {
						setState(State.Unplug);
					}
				} else {
					if(charger.isLocked()) {
						charger.setCableLock(false);
					} else {
						setState(State.Plugging);
					}
				}
				break;
			case IECCharger.STATE_CABLE_DISCONNECT:
				if(isChargingSession()) {
					stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Other)));				
				}
//				if(!serverDown && status != State.NetworkDown) {
//					Logger.writeln("stateChanged function: STATE_CABLE_DISCONNECT and set to READY status when status is: " + status.name());
					setState(State.Ready);
//				}
				break;
			case IECCharger.STATE_B:
				if(isChargingSession()) {
					setState(State.Pause);
				} else {
					if(charger.getType() == IECCharger.SOCKET) {
						charger.setCableLock(true);
					} else {
						setState(State.Authorize);
					}
				}
				break;
			case IECCharger.STATE_C:
				if(isChargingSession()) {
					setState(State.Charging);
				} else {
					if(!(status == State.Authorizing || status == State.Authorized)) {
						setState(State.Replug);
					} else {
						Logger.writeln("IECCharger.STATE_C but status: " + status + " so no setState to Replug!");
					}
				}
				break;
			case IECCharger.STATE_D:
				if(isChargingSession()) {
					setState(State.Ventilation);
				} else {
					setState(State.Replug);
				}
				break;
			case IECCharger.STATE_N:
				if(isChargingSession()) {
					setState(State.Expire);
				} else {
					if(isOperative) {
//						if(!serverDown && status != State.NetworkDown) {
//							Logger.writeln("stateChanged function: STATE_N and set status to NetworkDown when status is: " + status.name());
							setState(State.NetworkDown);			
//						}
					} else {
						setState(State.Unavailable);
					}
				}
				break;
			case IECCharger.STATE_E:
				setState(State.Fault);
				break;
			case IECCharger.STATE_EExt:
				Logger.writeln("IECCharger.STATE_EExt");
//				if(!serverDown && status != State.NetworkDown) {
//					Logger.writeln("stateChanged function: STATE_N and set status to NetworkDown when status is: " + status.name());
				if(status == State.Pause || status == State.Charging || "Yes".equals(config.getJSONObject("Enable CPError").getString("Value"))) {
					Logger.writeln("now status: " + status);
					setState(State.CPError);
				}
//				}
				break;
			default:
				break;
			}
		}
	}

	private JSONArray statusNotification(State state) {
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put("id");
		payload.put("StatusNotification");
		JSONObject json = new JSONObject();
		json.put("connectorId", cid);
		json.put("errorCode", OCPP.ChargePointErrorCode.NoError);
		json.put("status", getOCPPState(state));
		json.put("timestamp", OCPP.dateTime.format(new Date()));
		json.put("vendorId", "Cornerstone");
		json.put("vendorErrorCode", "NoError");
		payload.put(json);
		return payload;
	}

	private synchronized JSONArray stopTransaction(String idTag, OCPP.Reason reason) {
		contactor.setState(false);		// 2021-04-19 For IEC 61851-1 cert.: put here to reduce AC cut off delay time after CP or GND disconnected 

		view.showInfo("");

		charger.setEnable(true);
		charger.setPilotEnable(false);
		view.hideRemaingTime();
		view.hideTag();
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put("id");
		payload.put("StopTransaction");
		
		
		JSONObject json = new JSONObject();
		if(idTag != null) {
			json.put("idTag", idTag);
		}
		
		selectTime = 0;
		json.put("meterStop", (int)(getMaterValue("chargerEnergy")));
		json.put("timestamp", OCPP.dateTime.format(new Date()));
		json.put("transactionId", tid);
		json.put("reason", reason.name());
	
		payload.put(json);
		stLock.writeLock().lock();
		stopTransaction.put(json);
		ttid = tid;
		tid = -1;
		Logger.writeln("Transaction length " + stopTransaction.length());
		stLock.writeLock().unlock();
		Logger.writeln("stLock.writeLock().unlock()");
		if(!record.exists()) {
			try {
				record.createNewFile();
			} catch (IOException e) {
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(record);
			fos.write(stopTransaction.toString().getBytes());
			fos.close();
			Runtime.getRuntime().exec("sync");
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		if(barcode != null) {
			barcode.removeAllListeners();
		}

		if(chargingTimer != null) {
			chargingTimer.cancel();
			Logger.writeln("schargingTimer.cancel");
			chargingTimer = null;
		}
		if(!session.exists()) {
			Logger.writeln("session not exists");
			return null;
		}
		session.delete();
		Logger.writeln("session deleted");
		if(stopTimer != null) {
			stopTimer.cancel();
			Logger.writeln("stopTimer.cancel");
			stopTimer = null;
		}
		if(countDownTimer != null) {
			countDownTimer.cancel();
			Logger.writeln("countDownTimer.cancel");
			countDownTimer = null;
		}
		if(powerTimer != null) {
			Logger.writeln("powerTimer shutdown by stopTransaction");
			powerTimer.shutdownNow();
//			while(!powerTimer.isTerminated()) {
//				powerTimer.shutdownNow();
//				Logger.writeln("powerTimer shutdownNow");
//				try {
//					Thread.sleep(10);
//				} catch (InterruptedException e) {
//					Logger.writeln("powerTimer: " + e.getMessage());
//				}
//			}
		}
		if(scheduleTimer != null) {
			for(ScheduledExecutorService service : scheduleTimer) {
				while(!service.isTerminated()) {
					service.shutdownNow();
					Logger.writeln("scheduleTimer shutdownNow");
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Logger.writeln("scheduleTimer: " + e.getMessage());
					}
				}
			}
		}
		if(sessionTimer != null) {
			while(!sessionTimer.isTerminated()) {
				sessionTimer.shutdownNow();
				Logger.writeln("sessionTimer shutdownNow");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					Logger.writeln("sessionTimer: " + e.getMessage());
				}
			}
		}
		if(chargingProfileService != null) {
			chargingProfileService.shutdownNow();
			Logger.writeln("chargingProfileService shutdownNow");
		}
	
		if(charger.getType() == IECCharger.SOCKET) {
			charger.setCableLock(false);			
		}
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Logger.writeln("Thread.sleep(100): " + e.getMessage());
		}
// 2021-04-19 For IEC 61851-1 cert.: AC cut off action were moved at the top of this method to reduce delay after CP or GND disconnected 
//		contactor.setState(false);	

		Logger.writeln("serverDown: " + serverDown);
		if(serverDown) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Logger.writeln("serverDown: " + e.getMessage());
			}
			if(serverDown) {
				setEnable(false);
			}
		}
		return payload;
	}

	private void resendStopTransactionConf(JSONArray reply) {
		if(reply == null) {
			Logger.writeln("resendStopTransactionConf no replay!");
			return;
		}
		
		for(int i = 0; i < stopTransaction.length(); ++i) {
			OCPP.StopTransaction.req stReq = new Gson().fromJson(stopTransaction.get(i).toString(), OCPP.StopTransaction.req.class);
			if(stReq.getTransactionId() == extid) {
				stopTransaction.remove(i);
			}
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(record);
			fos.write(stopTransaction.toString().getBytes());
			fos.close();
			Runtime.getRuntime().exec("sync");
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}

	private void stopTransactionConf(JSONArray a) {
		if(a == null) {
			return;
		}
		view.remove(stopTimeCharge);
		OCPP.StopTransaction.conf conf = new Gson().fromJson(a.getJSONObject(2).toString(), OCPP.StopTransaction.conf.class);
		if(conf.getIdTagInfo().getStatus() == OCPP.AuthorizationStatus.Accepted) {
			stLock.writeLock().lock();
			for(int i = 0; i < stopTransaction.length(); ++i) {
				OCPP.StopTransaction.req stReq = new Gson().fromJson(stopTransaction.get(i).toString(), OCPP.StopTransaction.req.class);
				if(stReq.getTransactionId() == ttid) {
					stopTransaction.remove(i);
					Logger.writeln("Transcation " + i + " deleted");
				}
			}
			Logger.writeln("Transcation length " + stopTransaction.length());
			stLock.writeLock().unlock();
			try {
				FileOutputStream fos = new FileOutputStream(record);
				fos.write(stopTransaction.toString().getBytes());
				fos.close();
				Runtime.getRuntime().exec("sync");
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
			
			if(!config.getJSONObject("LPRS").getString("Value").equals("0.0.0.0")) {
				EvseLprsMsg msg = new EvseLprsMsg(++msgSerialLprs, config.getJSONObject("Station Name").getString("Value"), "StopCharge", OCPP.dateTime.format(new Date()));
				//sendLprsMessage(new Gson().toJson(msg));
				incrDecrLprsMessageBuf(new Gson().toJson(msg), LPRS_MSGBUF_INCR);
			}
			if(powerMeter2 != null) {
				File attachment = new File("logs/" + config.getJSONObject("Station Name").getString("Value") + "_" + ttid + ".csv");
				String[] to = new String[1];
				to[0] = "victorhowf@gmail.com";
				GmailClient.send("victorhowf", "ycriymteaysuhdvh", to, "Meter Usage " + ttid, new Date().toString(), attachment);
				attachment.delete();
			}
		} else {
			Logger.writeln("stopTransactionConf error: " + a);
		}
	}
	
	private void getConfiguration(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		JSONArray payload1 = new JSONArray();
		JSONObject json = new JSONObject();
		json.put("key", "Default Capacity (A)");
		json.put("value", config.getJSONObject("Default Capacity (A)").getInt("Value") + "");
		json.put("readonly", false);
		payload1.put(json);
		json = new JSONObject();
		json.put("key", "Hardware Version");
		json.put("value", config.getJSONObject("Hardware Version").getString("Value"));
		json.put("readonly", true);
		json = new JSONObject();
		json.put("key", "Maximum Capacity (A)");
		json.put("value", config.getJSONObject("Maximum Capacity (A)").getInt("Value") + "");
		json.put("readonly", false);
		json = new JSONObject();
		json.put("key", "Powermeter Type");
		json.put("value", config.getJSONObject("Powermeter Type").getString("Value"));
		json.put("readonly", true);
		payload1.put(json);
		payload.put(new JSONObject().put("configurationKey", payload1));
		Logger.writeln("getConfiguration: " + payload);
		send(payload);
	}

	private void triggerMessage(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		JSONObject json = new JSONObject();
		switch (a.getJSONObject(3).getString("requestedMessage").toLowerCase()) {
		case "getexceptionalstop":
		case "stoptransaction":
//			boolean hasException = false;
//			for(int i = 0; i < stopTransaction.length(); ++i) {
//				ClearLog.writeln("TM Checking " + tid + " with " + stopTransaction.get(i).toString());
//				OCPP.StopTransaction.req stReq = new Gson().fromJson(stopTransaction.get(i).toString(), OCPP.StopTransaction.req.class);
//				if(stReq.getTransactionId() != tid) {
//					hasException = true;
//					break;
//				}
//			}
//			if(hasException) {
			if(stopTransaction.length() > 0) {
				json.put("status", "Accepted");
			} else {
				json.put("status", "Rejected");
				json.put("reason", "stopTransaction.length: " + stopTransaction.length());
			}
			break;
		case "statusnotification":
			json.put("status", "Accepted");
			break;
		case "metervalues":
			json.put("status", "Accepted");
			break;
		default:
			json.put("status", "NotImplemented");
			break;
		}
		payload.put(json);
		send(payload);

		switch (a.getJSONObject(3).getString("requestedMessage").toLowerCase()) {
		case "getexceptionalstop":
		case "stoptransaction":
			while(stopTransaction.length() > 0) {
				OCPP.StopTransaction.req stReq = new Gson().fromJson(stopTransaction.get(0).toString(), OCPP.StopTransaction.req.class);
				if(stReq.getTransactionId() != tid) {
					extid = stReq.getTransactionId();
					JSONArray resentStop = new JSONArray();
					resentStop.put(2);
					resentStop.put("id");
					resentStop.put("StopTransaction");
					resentStop.put(stopTransaction.getJSONObject(0));
					resendStopTransactionConf(send(resentStop));
				}
			}
			break;
		case "statusnotification":
			send(statusNotification(status));
			break;
		case "metervalues":
			
			break;
		default:
			break;
		}
	}

	private void effectChargingProfile() {
		if(sessionTimer != null) {
			sessionTimer.shutdownNow();
		}
		Logger.writeln(new Gson().toJson(currentProfile));

		if(currentProfile.getChargingSchedule().getDuration() != 0) {
			sessionTimer = Executors.newScheduledThreadPool(1);
			sessionTimer.schedule(new Runnable() {

				@Override
				public void run() {
					if(isChargingSession()) {
						charger.setEnable(false);
					}
				}

			}, currentProfile.getChargingSchedule().getDuration(), TimeUnit.SECONDS);
		}
		
		if(chargingProfileService != null) {
			chargingProfileService.shutdownNow();
		}
		
//		charger.setChargeCurrent(config.getJSONObject("Default Capacity (A)").getInt("Value"));
//		boolean[] newMode = new boolean[5];
//		if(config.getJSONObject("Phase").getString("Value").equals("3 Phases")) {
//			newMode[0] = false;
//			newMode[1] = false;
//			newMode[2] = true;
//			newMode[3] = true;
//			newMode[4] = true;
//		} else {
//			newMode[0] = false;
//			newMode[1] = false;
//			newMode[2] = true;
//			newMode[3] = false;
//			newMode[4] = false;
//		}
//		contactor.setMode(newMode);
		chargingProfileService = Executors.newSingleThreadScheduledExecutor();
		chargingProfileService.execute(new Runnable() {

			@Override
			public void run() {
				Logger.writeln("CP2");
				OCPP.ChargingSchedulePeriod[] schedules = currentProfile.getChargingSchedule().getChargingSchedulePeriod();
				try {
					for(int i = 0; i < schedules.length; ++i) {
						OCPP.ChargingSchedulePeriod s = ((OCPP.ChargingSchedulePeriod)schedules[i]);
						if(i == 0) {
							Thread.sleep(s.getStartPeriod() * 1000);
						} else {
							Thread.sleep((s.getStartPeriod() - ((OCPP.ChargingSchedulePeriod)schedules[i - 1]).getStartPeriod()) * 1000);
						}
						charger.setChargeCurrent(s.getLimit());
						
						if(config.getJSONObject("Hardware Version").getString("Value").equals("V1_5") || config.getJSONObject("Hardware Version").getString("Value").equals("V1_6")) {
							boolean[] newMode = new boolean[3];
							if(s.getNumberPhases() == 1) {
								newMode[0] = true;
								newMode[1] = true;
								newMode[2] = false;
							} else {
								newMode[0] = true;
								newMode[1] = true;
								newMode[2] = true;
							}
							contactor.setMode(newMode);								
						}else if (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5")){
							boolean[] newMode = new boolean[2];
							if(s.getNumberPhases() == 1) {
								newMode[0] = true;
								newMode[1] = false;
							} else {
								newMode[0] = true;
								newMode[1] = true;
							}							
							contactor.setMode(newMode);	
							
						}else {
							boolean[] newMode = new boolean[5];
							if(s.getNumberPhases() == 1) {
								newMode[0] = false;
								newMode[1] = false;
								newMode[2] = true;
								newMode[3] = false;
								newMode[4] = false;
							} else {
								newMode[0] = false;
								newMode[1] = false;
								newMode[2] = true;
								newMode[3] = true;
								newMode[4] = true;
							}
							contactor.setMode(newMode);
						}
					}
				} catch (InterruptedException e) {
					Logger.writeln("chargingProfileService: " + e.getMessage());
				}
			}
		});
	}
		
	
	private OCPP.ChargingProfileStatus updateChargingProfile(ChargingProfile profile) {
		if(profile.getChargingProfilePurpose() == OCPP.ChargingProfilePurposeType.TxProfile) {
			Logger.writeln("UCP 1");
			Logger.writeln("updateChargingProfile: " + new Gson().toJson(profile));
			currentProfile = profile;
			effectChargingProfile();
			return OCPP.ChargingProfileStatus.Accepted;
		} else if(profile.getChargingProfilePurpose() == OCPP.ChargingProfilePurposeType.TxDefaultProfile) {
			Logger.writeln("UCP 2");
			defaultProfile = profile;
			return OCPP.ChargingProfileStatus.Accepted;
		}
		Logger.writeln("UCP 3");
		return OCPP.ChargingProfileStatus.NotSupported;
	}
	
	private JSONArray updateFirmware(JSONArray a) {
		JSONArray payload = new JSONArray();
		payload.put(3);
		payload.put(a.getString(1));
		payload.put(new JSONObject());
		OCPP.UpdateFirmware req = new Gson().fromJson(a.getJSONObject(3).toString(), OCPP.UpdateFirmware.class);
		String uri = req.getLocation().replaceFirst("REPLACE_CP_SERIAL_NO_HERE", config.getJSONObject("Serial No").getString("Value"));
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					String[] cmd = {"wget", "-O", "update.tmp", uri};
					Process p = Runtime.getRuntime().exec(cmd);
					p.waitFor();
					cmd[2] = "update.md5";
					cmd[3] = uri + ".md5";
					p = Runtime.getRuntime().exec(cmd);
					p.waitFor();

					MessageDigest md = MessageDigest.getInstance("MD5");
					DigestInputStream dis = new DigestInputStream(new FileInputStream("update.tmp"), md);
					dis.read(new byte[dis.available()]);
					dis.close();
					byte[] md5Digests = md.digest();
					int attemp = req.getRetries();
					byte[] b;
					do {
						FileInputStream fis = new FileInputStream("update.md5");
						b = new byte[fis.available()];
						fis.read(b);
						fis.close();
					} while(!new String(b).toLowerCase().equals(HexFactory.toString(md5Digests, "").toLowerCase()) && --attemp > 0);
					if(attemp > 0) {
						File oldFile = new File("ChargingStation.jar");
						File newFile = new File("update.tmp");
						oldFile.delete();
						newFile.renameTo(oldFile);
						p = Runtime.getRuntime().exec("sync");
						p.waitFor();
						needRestart = true;
					}
					
				} catch (IOException | InterruptedException | NoSuchAlgorithmException e) {
					Logger.writeln("updateFirmware: " + e.getMessage());
				}
			}
		}).start();
		return payload;
	}

	@Override
	public void connected(ConnectionEvent evt) {
	}

	@Override
	public void disconnected(ConnectionEvent evt) {
		serverDown = true;
		setEnable(false);
	}
	
	public void registerDevice() {
		List<File> usbDrive = DeviceManager.list();
		String[] drives= new String[usbDrive.size()];
		for(int j = 0; j < drives.length; ++j) {
			drives[j] = usbDrive.get(j).getName();
		}
		int i = JOptionPane.showOptionDialog(view, "Select destination", "Retrieve Charging Record Device Register", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, drives, drives[0]);
		File selected = usbDrive.get(i);
		try {
			FileOutputStream fos = new FileOutputStream(selected.getAbsolutePath() + "/" + usbKey.getName());
			fos.write(EncryptionFactory.encrypt(encryptKey, DeviceManager.serialLookup(DeviceManager.deviceLookup(selected.getAbsolutePath())).getBytes()));
			fos.close();
			JOptionPane.showConfirmDialog(view, "Register Success");
		} catch (IOException e) {
			JOptionPane.showConfirmDialog(view, "Register Fail");
		}
	}
	
	@Override
	public void deviceInserted(File f) {
		Logger.writeln("USB device inserted");
		String serial = DeviceManager.serialLookup(DeviceManager.deviceLookup(f.getAbsolutePath()));				
		File key = new File(f.getAbsolutePath() + "/" + usbKey);
		if(key.exists()) {
			Logger.writeln("USB device contain key file");
			try {
				FileInputStream fis = new FileInputStream(key);
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				if(serial.equals(new String(EncryptionFactory.decrypt(encryptKey, b)))) {
					Logger.writeln("Key File Correct");
					try {
						FileOutputStream fos = new FileOutputStream(f.getAbsolutePath() + "/" + "Record" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".csv");
						fos.write(("Sequence No.,"
								+ "Date (dd/mm/yyyy),"
								+ "Charger Location,"
								+ "NFC Card ID,"
								+ "Odometer reading at recharge (km),"
								+ "Electricity meter reading before recharge (kWh),"
								+ "Electricity meter reading after recharge (kWh),"
								+ "kWh charged (kWh),"
								+ "Battery state of charge before recharge (%),"
								+ "Battery state of charge after recharge (%),"
								+ "Recharge start date (dd/mm/yyyy),"
								+ "Recharge start time (hh:mm:ss),"
								+ "Recharge end date (dd/mm/yyyy),"
								+ "Recharge end time (hh:mm:ss),"
								+ "Downtime due to recharge (hours),"
								+ "Recharge cost other than electicty (with explanation) (HK$)\r\n").getBytes());
						DateFormat df = new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss'Z'");
						String str;
						double d;
						JSONArray transactions = ocppServer.getTransactionHistory();
						for(int i = 0; i < transactions.length(); ++i) {
							try {
								Date startTime = new Date(OCPP.dateTime.parse(transactions.getJSONObject(i).getString("StartTime")).getTime() + config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000);
								Date stopTime = new Date(OCPP.dateTime.parse(transactions.getJSONObject(i).getString("StopTime")).getTime() + config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000);
								Date chargingStartTime = new Date(OCPP.dateTime.parse(transactions.getJSONObject(i).getString("StartCharge")).getTime() + config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000);
								Date chargingStopTime = new Date(OCPP.dateTime.parse(transactions.getJSONObject(i).getString("StopCharge")).getTime() + config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000);
								for(int j = 0; j < 15; ++j) {
									switch(j) {
									case 0:
										fos.write(transactions.getJSONObject(i).getString("tId").getBytes());
										fos.write(',');
										break;
									case 1:
										fos.write(df.format(startTime).substring(0, 10).getBytes());
										fos.write(',');
										break;
									case 2:
										fos.write(config.getJSONObject("Station Name").getString("Value").getBytes());
										fos.write(',');
										break;
									case 3:
										str = transactions.getJSONObject(i).getString("CardId");
										fos.write(str.substring(0, str.indexOf("-")).getBytes());
										fos.write(',');
										break;
									case 4:
										fos.write(',');
										break;
									case 5:
										d = transactions.getJSONObject(i).getInt("meterStart");
										fos.write(String.format("%.2f", d / 1000).getBytes());
										fos.write(',');
										break;
									case 6:
										d = transactions.getJSONObject(i).getInt("meterStop");
										fos.write(String.format("%.2f", d / 1000).getBytes());
										fos.write(',');
										break;
									case 7:
										d = transactions.getJSONObject(i).getInt("meterStop") - transactions.getJSONObject(i).getInt("meterStart");
										fos.write(String.format("%.2f", d / 1000).getBytes());
										fos.write(',');
										break;
									case 8:
										fos.write(',');
										break;
									case 9:
										fos.write(',');
										break;
									case 10:
										if(chargingStartTime.getTime() == config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000) {
											fos.write("N/A".getBytes());
										} else {
											fos.write(df.format(chargingStartTime).substring(0, 10).getBytes());
										}
										fos.write(',');
										break;
									case 11:
										if(chargingStartTime.getTime() == config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000) {
											fos.write("N/A".getBytes());
										} else {
											fos.write(df.format(chargingStartTime).substring(11, 19).getBytes());
										}
										fos.write(',');
										break;
									case 12:
										if(chargingStopTime.getTime() == config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000) {
											fos.write("N/A".getBytes());
										} else {
											fos.write(df.format(chargingStopTime).substring(0, 10).getBytes());
										}
										fos.write(',');
										break;
									case 13:
										if(chargingStopTime.getTime() == config.getJSONObject("Time Zone").getInt("Value") * 60 * 60 * 1000) {
											fos.write("N/A".getBytes());
										} else {
											fos.write(df.format(chargingStopTime).substring(11, 19).getBytes());
										}
										fos.write(',');
										break;
									case 14:
										fos.write('\r');
										fos.write('\n');
										break;
									
									}
								}
							} catch (JSONException | ParseException e) {
							}
						}
						fos.close();
						Runtime.getRuntime().exec("umount " + f.getAbsolutePath());
						JOptionPane.showMessageDialog(view, "ADD");
					} catch (IOException e) {
					}
				}
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void deviceRemoved(File f) {
	}

	static void setSystemTime(String time) {
		try {
			System.out.println("time: " + time);
			Date newDate = new Date();
			if (time != null) {
				newDate = OCPP.dateTime.parse(time);
			}
			Logger.writeln("System time: " + System.currentTimeMillis() + ", newDate: " + newDate.getTime());
			if(Math.abs(System.currentTimeMillis() - newDate.getTime()) > 1000) {
				try {
					String[] cmd = new String[5];
					cmd[0] = "sudo";
					cmd[1] = "date";
					cmd[2] = "-s";
					cmd[3] = time;
					cmd[4] = "+%Y-%m-%dT%H:%M:%SZ";
					Logger.writeln(cmd[0] + " " + cmd[1] + " " + cmd[2] + " " + cmd[3] + " " + cmd[4]);
					Process p = Runtime.getRuntime().exec(cmd);
					p.waitFor();
					InputStream is = p.getErrorStream();
					byte[] b = new byte[is.available()];
					is.read(b);
					is.close();
					is = p.getInputStream();
					b = new byte[is.available()];
					is.read(b);
					is.close();
					Logger.writeln("updated time: " + new Date() );
				} catch (IOException | InterruptedException e) {
					Logger.writeln("setSystemTime get IOException | InterruptedException: " + e.getMessage());
				}
			}
		} catch (ParseException e1) {
			Logger.writeln("setSystemTime get ParseException: " + e1.getMessage());
		}
	}
	
//	static BufferedImage convertMat2BufferedImage(Mat matrix)throws Exception {        
//	    MatOfByte mob=new MatOfByte();
//	    Imgcodecs.imencode(".png", matrix, mob);
//	    byte ba[]=mob.toArray();
//
//	    BufferedImage bi=ImageIO.read(new ByteArrayInputStream(ba));
//	    return bi;
//	}
	
	public List<BufferedImage> getImgsTx(){
		return imgsTx;
	}
	
	public class PeriodTimer {
	    private Timer timer;
	    boolean end = false;
	    boolean timesUp;
	    
	    public PeriodTimer(int delay, long period) {
	        timer = new Timer();
	        timer.schedule(new TxTimer(), delay, period);
	        timesUp = false;
		}

	    class TxTimer extends TimerTask {
	        public void run() {
	        	timesUp = true;
	            if(end) {
	            	timer.cancel();
	           }
	        }
	    }
	    
	    public void setEnd(boolean in) {
	    	end = in;
	    }
	    
	    public Timer getTimer() {
	    	return this.timer;
	    }
	    
	    public void resetTimer() {
	    	timesUp = false;
	    }
	    
	    public boolean isTimesUp() {
	    	return timesUp;
	    }
	}

	static void setSystemTimeLprs(String time) {
		try {
			Date newDate = OCPP.dateTime.parse(time);
			if(Math.abs(System.currentTimeMillis() - newDate.getTime()) > 1000) {
				try {
					String[] cmd = new String[4];
					cmd[0] = "date";
					cmd[1] = "-s";
					cmd[2] = time;
					cmd[3] = "+%Y-%m-%dT%H:%M:%SZ";
					Logger.writeln(cmd[0] + " " + cmd[1] + " " + cmd[2] + " " + cmd[3]);
					Process p = Runtime.getRuntime().exec(cmd);
					p.waitFor();
					InputStream is = p.getErrorStream();
					byte[] b = new byte[is.available()];
					is.read(b);
					is.close();
					is = p.getInputStream();
					b = new byte[is.available()];
					is.read(b);
					is.close();
					//lprsStartupTimeSync = true;
				} catch (IOException | InterruptedException e) {
					Logger.writeln("setSystemTimeLprs: " + e.getMessage());
				}
			}else {
				//lprsStartupTimeSync = true;
			}
		} catch (ParseException e1) {
		}
	}	
	


	private void sendOutLprsMessage(String msg) {	
		if(housingSocietyLprsSocket == null) {
			try {
				housingSocietyLprsSocket = new Socket(config.getJSONObject("LPRS").getString("Value"), 5000);
				housingSocietyLprsIS = housingSocietyLprsSocket.getInputStream();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(housingSocietyLprsSocket != null) {
			try {
				housingSocietyLprsSocket.getOutputStream().write(msg.getBytes());
//				firstLprsMsgSent = true;
			} catch (IOException e) {
				//Logger.writeln("start null hs scoket "+ System.currentTimeMillis());
				housingSocietyLprsSocket = null;
				//Logger.writeln("end null hs scoket "+ System.currentTimeMillis());
			}catch (Exception e) {
				//Logger.writeln("hs scoket error");
			}
			
		}

	}	
	
	public void refreshLedColor(int period) {
		statusLED.setColorUpdate(LEDConfig.UIOrange);
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		
		scheduler.scheduleAtFixedRate(new Runnable() {
			int idxCharging = 0;
			int idxPause = 0;
			int i=0;
			boolean incCharge = true;
			boolean incPause = true;
			@Override
			public void run() {

				try {
					
					if(status == State.Charging) {
						
//						switch(++i) {
//
//						case 48:	
//							i = 0;
//							break;
//						case 0:
//						case 1:
//						case 2:
//						case 3:
//						case 4:
//						case 5:
//						case 6:
//						case 7:
//						case 8:
//						case 9:
//						case 10:
//						case 11:
//						//case 12:
//													
//							statusLED.shift(LED.BACKWARD, new Color(255, 255, 255));
//							break;
//						default:
//							//if(i < 50) {
//							if(i < 24) {	
//								statusLED.shift(LED.BACKWARD, new Color(0, 0, 255));
//							}
//							break;
//						}
						i++;
						if(i== (config.getJSONObject("Total LEDs number").getInt("Value")*config.getJSONObject("LED strips").getInt("Value")*2)) {
							i=0;
						}else if (i< (config.getJSONObject("Total LEDs number").getInt("Value")) ) {
							statusLED.shift(LED.BACKWARD, new Color(255, 255, 255));
						}else if(i< (config.getJSONObject("Total LEDs number").getInt("Value")*2 )){
							statusLED.shift(LED.BACKWARD, new Color(0, 0, 255));
						}
					
					}else if(status == State.Pause) {
	
						
						statusLED.setColorUpdate(new Color(idxPause,idxPause/2,0));
						idxPause++;
						if(idxPause>254) {
							idxPause = 0;
						}	
						
					}else {
						idxCharging = 0;
						idxPause = 0;
						incCharge = true;
						incPause = true;
						if(LEDConfig.stateSteadyColor.containsKey(status)) {
							Color c1 = LEDConfig.stateSteadyColor.get(status);
							if(statusLED.getColor() != c1) {
							//	Logger.writeln("Update LED color "+ c1);
								ledColor = c1;
								statusLED.setColorUpdate(c1);
							}							
						}else {
							ledColor = LEDConfig.Black;
							statusLED.setColorUpdate(ledColor);							
						}
					
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000, period, TimeUnit.MILLISECONDS);
	}	

	public void refreshLedColor0(int period) {
		statusLED.setColorUpdate(LEDConfig.UIOrange);
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		
		scheduler.scheduleAtFixedRate(new Runnable() {
			int idxCharging = 0;
			int idxPause = 0;
			int i=0;
			boolean incCharge = true;
			boolean incPause = true;
			@Override
			public void run() {

				try {
					
					if(status == State.Charging) {
					
						
//						switch(++i) {
//						//case 100:
//						case 48:	
//							i = 0;
//							break;
//						case 0:
//						case 1:
//						case 2:
//						case 3:
//						case 4:
//						case 5:
//						case 6:
//						case 7:
//						case 8:
//						case 9:
//						case 10:
//						case 11:
//						//case 12:
//													
//							statusLED.shift(LED.BACKWARD, new Color(255, 255, 255));
//							break;
//						default:
//							//if(i < 50) {
//							if(i < 24) {	
//								statusLED.shift(LED.BACKWARD, new Color(0, 0, 255));
//							}
//							break;
//						}
					
						i++;
						if(i== (config.getJSONObject("Total LEDs number").getInt("Value")*config.getJSONObject("LED strips").getInt("Value")*2)) {
							i=0;
						}else if (i< (config.getJSONObject("Total LEDs number").getInt("Value")) ) {
							statusLED.shift(LED.BACKWARD, new Color(255, 255, 255));
						}else if(i< (config.getJSONObject("Total LEDs number").getInt("Value")*2 )){
							statusLED.shift(LED.BACKWARD, new Color(0, 0, 255));
						}						
						
					}else if(status == State.Pause) {
	
						
						statusLED.setColorUpdate(new Color(idxPause,idxPause/2,0));
						idxPause++;
						if(idxPause>254) {
							idxPause = 0;
						}	
						
					}else {
						idxCharging = 0;
						idxPause = 0;
						incCharge = true;
						incPause = true;
						Color c1 = LEDConfig.mapNonflashStateColor(status);
						if(statusLED.getColor() != c1) {
						//	Logger.writeln("Update LED color "+ c1);
							ledColor = c1;
							statusLED.setColorUpdate(c1);
						}					
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 1000, period, TimeUnit.MILLISECONDS);
	}	
	
	public void testCheckADE7854(int period) {

		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		
		scheduler.scheduleAtFixedRate(new Runnable() {

		
			@Override
			public void run() {
				if( (config.getJSONObject("Hardware Version").getString("Value").equals("V2_5") )) {
					
					try {
						Logger.writeln("ADE7854A L1 Voltage  "+meterIC.getVoltage(PowerMeter.PHASE_A));
						Logger.writeln("ADE7854A L1 Current  "+meterIC.getCurrent(PowerMeter.PHASE_A));
					} catch (Exception e) {
						e.printStackTrace();
					}					
				}


			}
		}, 5, period, TimeUnit.SECONDS);
	}
	
	
//	public class UnlockAndStop implements Runnable{
//		
//		private boolean newAction = false;
//		
//	    
//		public UnlockAndStop() {
//
//		}
//		
//		public void setNewAction(boolean input) {
//			newAction = input;
//		}
//		 
//	    public void run() {
//	    	while(true) {
//		    	if(newAction) {
//		    		Logger.writeln("StopTransaction by unlock button");
//		    		stopTransactionConf(send(stopTransaction(idTag, OCPP.Reason.Local)));
//		    		newAction = false;
//		    	}
//		    	try {
//		    		Thread.sleep(100);
//		    	}catch (Exception e) {
//		    		
//		    	
//		    	}
//		    }
//	    }
//		
//	}
	
}
