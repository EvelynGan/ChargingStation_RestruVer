package cornerstone;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.glassfish.tyrus.server.Server;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import victorho.configuration.Configurable;
import victorho.ocpp.OCPP;
import victorho.ocpp.OCPP.ChargingProfile;
import victorho.ocpp.OCPP.ChargingProfileKindType;
import victorho.ocpp.OCPP.ChargingProfilePurposeType;
import victorho.ocpp.OCPP.ChargingRateUnitType;
import victorho.ocpp.OCPP.ChargingSchedule;
import victorho.ocpp.OCPP.ChargingSchedulePeriod;
import victorho.util.Logger;
import victorho.util.PerpetualInteger;


@ServerEndpoint(value = "/{client-id}")
public class OcppServer implements Configurable {
	
	public static final String NFC = "NFC";
	public static final String QRCODE = "QRCode";

	private static String keysName = ".keys";
	private static String sessionName = ".session_server";
	private static String transactionsFileName = ".transactions";
	private static String remoteOcppNonSendFileName = ".remote_ocpp.temp";
	private static String transactionIdName = ".tid";

	private static String remoteOcppServerAddress;
	private static String remoteOcppServerUsername;
	private static String remoteOcppServerPassword;
	private static String remoteOcppServerAuthToken = "";
	private static Server server;
	private static List<Session> clients;
	private static int msgSerial;
	private static boolean isOnline;
	private static File keys;
	private static JSONObject validID;
	private static JSONObject transaction;
	private static JSONArray transactions;
	private static JSONArray remoteOcppNonSend;
	private static File transactionsFile;
	private static File remoteOcppNonSendFile;
	private static File session;
	private static int count;
	private static PerpetualInteger tid;
	private static OCPP.StartTransaction.req startTransaction;
	private static long chargingStartTime;
	private static long chargingStopTime;
	
	private static int timeZone;
	private static String stationName;
	private static long meterReading;
	
	
	public OcppServer() {
		super();
	}	
	
	public OcppServer(Point p, boolean isVisible, File tempPath, JSONObject config) {
		this.timeZone = config.getJSONObject("Time Zone").getInt("Value");
		remoteOcppServerAddress = config.getJSONObject("Remote OCPP Server Path").getString("Value");
		remoteOcppServerUsername = config.getJSONObject("Remote OCPP Server Username").getString("Value");
		remoteOcppServerPassword = config.getJSONObject("Remote OCPP Server Password").getString("Value");
		stationName = config.getJSONObject("Station Name").getString("Value");
		Logger.writeln("remoteOcppServerAddress:" + remoteOcppServerAddress);
		clients = new ArrayList<Session>();

		tid = new PerpetualInteger(new File(tempPath.getPath() + "/" + transactionIdName));
		msgSerial = 10001;
		count = 0;
		server = new Server("127.0.0.1", 8086, "/ocpp", null, OcppServer.class);
		try {
			server.start();
		} catch (DeploymentException e) {
			e.printStackTrace();
		}
		
		isOnline = true;

		if(isVisible) {
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			JCheckBox cb;
			JButton btn;
			
			cb = new JCheckBox("Enable");
			cb.setSelected(isOnline);
			cb.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					isOnline = ((JCheckBox)arg0.getSource()).isSelected();
				}
				
			});
			panel.add(cb);
			
			Font f = new Font("Arial", Font.PLAIN, 24);
			
			btn = new JButton("Remote Start Transaction");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					remoteStartTransaction();
				}
				
			});
			panel.add(btn);
			
			btn = new JButton("Remote Stop Transaction");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					remoteStopTransaction();
				}
				
			});
			panel.add(btn);
			
			btn = new JButton("Change Configuration");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					changeConfiguration();
				}
				
			});
			panel.add(btn);
			
			btn = new JButton("Data Transfer");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					dataTransfer();
				}
				
			});
			panel.add(btn);
			
			btn = new JButton("Set Charging Profile");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setChargingProfile();
				}
				
			});
			panel.add(btn);
			
			btn = new JButton("Trigger Messgae");
			btn.setFont(f);
			btn.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					triggerMessage();
				}
				
			});
			panel.add(btn);
			
			JFrame view = new JFrame();
			view.setContentPane(panel);
			view.pack();
			view.setLocation(p);
			view.setVisible(true);
		}
		    	
    	keys = new File(tempPath.getPath() + "/" + keysName);
    	session = new File(tempPath.getPath() + "/" + sessionName);
    	transactionsFile = new File(tempPath.getPath() + "/" + transactionsFileName);
    	remoteOcppNonSendFile = new File(tempPath.getPath() + "/" + remoteOcppNonSendFileName);
    	
    	if(keys.exists()) {
			try {
				FileInputStream fis = new FileInputStream(keys);
	    		byte[] b = new byte[fis.available()];
	    		fis.read(b);
	    		fis.close();
	    		validID = new JSONObject(new String(b));
			} catch (IOException e) {
			}
    	} else {
    		validID = new JSONObject();
    	}
    	
    	if(transactionsFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(transactionsFile);
	    		byte[] b = new byte[fis.available()];
	    		fis.read(b);
	    		fis.close();
	    		transactions = new JSONArray(new String(b));
			} catch (IOException e) {
			}
    	} else {
    		transactions = new JSONArray();
    	}
    	
    	if(remoteOcppNonSendFile.exists()) {
			try {
				FileInputStream fis = new FileInputStream(remoteOcppNonSendFile);
	    		byte[] b = new byte[fis.available()];
	    		fis.read(b);
	    		fis.close();
	    		remoteOcppNonSend = new JSONArray(new String(b));
			} catch (IOException e) {
			}
    	} else {
    		remoteOcppNonSend = new JSONArray();
    	}
    	
	    try {
			HttpServer nfcServer = HttpServer.create(new InetSocketAddress(80), 0);
			nfcServer.createContext("/", new HttpHandler() {

				@Override
				public void handle(HttpExchange arg0) throws IOException {
					byte b[] = new byte[arg0.getRequestBody().available()];
					arg0.getRequestBody().read(b);
					JSONObject req = new JSONObject(new String(b));
					Logger.writeln(req.toString());
					
					JSONObject json = new JSONObject();
					
					String reply = "";
					try {
						reply = req.getString("Command").toLowerCase();
						
						if(reply.equals("ping")) {
							json.put("Status", "Accepted");
							reply = json.toString();
						} else if(reply.equals("update")) {
							setAuthorizedList(req.getJSONArray("List"));
							json.put("Status", "Accepted");
							reply = json.toString();
						} else if(reply.equals("retrieverecord")) {
							reply = transactions.toString();
							
							for(int i = 0; i < transactions.length(); ++i) {
								JSONObject record = (JSONObject)transactions.get(i);
								record.put("Resend", 1);
								transactions.put(i, record);
							}
						} else if(reply.equals("flushrecord")) {
							for(int i = 0; i < transactions.length(); ++i) {
								JSONObject record = (JSONObject)transactions.get(i);
								if(record.getInt("Resend") ==  1) {
									transactions.remove(i--);								
								}
							}
	
							try {
								FileOutputStream fos = new FileOutputStream(transactionsFile);
								fos.write(transactions.toString(2).getBytes());
								fos.close();
							} catch (IOException e) {
							}
							
							json.put("Status", "Accepted");
							reply = json.toString();						
						} else {
							json.put("Status", "Rejected");
							json.put("Reason", "Unknown Command");
							reply = json.toString();
						}
					} catch (JSONException e) {
						json.put("Status", "Rejected");
						json.put("Reason", "Command missing");
						reply = json.toString();
					}
					
					arg0.sendResponseHeaders(200, reply.length());
					
					OutputStream os = arg0.getResponseBody();
					os.write(reply.getBytes());
					
					Logger.writeln(reply.toString());
					
					arg0.close();
				}	
			});
			nfcServer.start();
			
			new Thread(new Runnable() {

				private long date = (System.currentTimeMillis() + timeZone * 60 * 60 * 1000) / (24 * 60 *60 * 1000);
				
				@Override
				public void run() {
					while(true) {
						long l = (System.currentTimeMillis() + timeZone * 60 * 60 * 1000) / (24 * 60 *60 * 1000);
						if(l > date) {
							dateChanged(date);
							date = l;
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							Logger.writeln("ocpp: " + e.getMessage());
						}
					}
				}
				
			}).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}
	
	public void addCard(String cid) {
		validID.put(cid, "local");
		
		try {
			FileOutputStream fos = new FileOutputStream(keys);
    		fos.write(validID.toString().getBytes());
    		fos.close();
    		Runtime.getRuntime().exec("sync");
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	private JSONObject authorize(JSONObject json) {
		JSONObject reply = new JSONObject();
		JSONObject idTag = new JSONObject();
		idTag.put("status", "Invalid");

		if(session.exists()) {
			if(transaction.getString("CardId").indexOf(json.getString("idTag").toLowerCase()) == 0) {
				idTag.put("status", "Accepted");				
			}
		} else {
			if(validID != null) {
				for(int i = 0; i < validID.length(); ++i) {
					if(validID.has(json.getString("idTag").toLowerCase())) {
						idTag.put("status", "Accepted");
						break;
					}
				}
			}
		}
		
		reply.put("idTagInfo", idTag);
		
		return reply;	
	}
	
	private void boardcast(String cmd, String msg) {
		JSONArray payload = new JSONArray();
		payload.put(2);
		payload.put(Integer.toString(msgSerial++));
		payload.put(cmd);
		payload.put(new JSONObject(msg));
		
		for(int i = 0; i < clients.size(); ++i) {
			clients.get(i).getAsyncRemote().sendText(payload.toString());
		}
	}
	
	private JSONObject bootNotification(JSONObject json) {
		JSONObject reply = new JSONObject();
		reply.put("status", "Accepted");
		reply.put("currentTime", OCPP.dateTime.format(new Date()));
		reply.put("interval", 300);
		return reply;
	}
	 	
	private void changeConfiguration() {
		JSONObject request = new JSONObject();
		request.put("key", "IdleUnlockInterval");
		request.put("value", 10);
		boardcast("ChangeConfiguration", request.toString());
	}
	
	@OnClose
	public void closedConnection(Session session) { 
		Logger.writeln("OcppServer closedConnection session: " + session);
		clients.remove(session);
	}

	private void dataTransfer() {
		JSONObject request = new JSONObject();
		request.put("vendorId", "Autotoll");
		request.put("messageId", "ChargingDetails");
		JSONObject data = new JSONObject();
		data.put("start_time", transaction.getString("StartTime"));
		data.put("session_fee", "6.00");
		data.put("minutes_per_session", "15");
		data.put("session_count", Integer.toString(count++));
		data.put("charging_fee", 6 * count + ".00");
		request.put("data", data);
		boardcast("DataTransfer", request.toString());
	}
		
	private void dateChanged(long did) {
		if(session.exists()) {
			long dayStart = ((System.currentTimeMillis() + timeZone * 60 * 60 * 1000 - 24 * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000) - timeZone * 60 * 60 * 1000;
			long dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1;
			transaction.put("StopTime", OCPP.dateTime.format(new Date(dayEnd)));
			if(chargingStopTime == Long.MIN_VALUE) {
				if(chargingStartTime == Long.MAX_VALUE) {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(0)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(0)));
				} else {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(chargingStartTime < dayStart ? dayStart : chargingStartTime)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(dayEnd)));
				}
			} else {
				if(chargingStopTime < dayStart) {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(0)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(0)));
				} else {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(chargingStartTime < dayStart ? dayStart : chargingStartTime)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(chargingStopTime)));
				}
			}
			transaction.put("meterStop", meterReading);
			transaction.put("Resend", 0);
			transactions.put(transaction);

			try {
				FileOutputStream fos = new FileOutputStream(transactionsFile);
				fos.write(transactions.toString(2).getBytes());
				fos.close();
				session.delete();
			} catch (IOException e) {
			}
			
			String cid = transaction.getString("CardId");
			
			transaction = new JSONObject();
			transaction.put("CardId", cid);
			transaction.put("StartTime", OCPP.dateTime.format(new Date(dayEnd + 1)));
			transaction.put("meterStart", meterReading);
			try {
				session.createNewFile();
				FileOutputStream fos = new FileOutputStream(session);
				fos.write(transaction.toString().getBytes());
				fos.close();
			} catch (IOException e) {
			}
			
			tid.setValue(tid.getValue() + 1);
			transaction.put("tId", Integer.toString(tid.getValue()));
		}
	}
	
	@OnError
	public void error(Session session, Throwable t) {
		Logger.writeln("OcppServer error: " + t.getMessage());
	}
		
	public JSONArray getAuthorizedList() {
		JSONArray a = new JSONArray();
		for(String id : validID.keySet()) {
			a.put(id);
		}
		return a;
	}
	
	public JSONArray getTransactionHistory() {
		return transactions;
	}
		
	public boolean hasCard(String cid) {
		return validID.has(cid);
	}
		
	public JSONObject heartbeat(JSONObject json) {
		JSONObject reply = new JSONObject();
		reply.put("currentTime", OCPP.dateTime.format(new Date()));
		return reply;
	}	
	
	private JSONObject meterValues(JSONObject json) {
		OCPP.MeterValues.req req = new Gson().fromJson(json.toString(), OCPP.MeterValues.req.class);
		meterReading = Long.parseLong(req.getMeterValue()[0].getSampledValue()[0].getValue());
		JSONObject reply = new JSONObject();
		return reply;
	}
	
	private void getRemoteServerAuthToken() {
		if(!remoteOcppServerAddress.isEmpty()) {
			String url = remoteOcppServerAddress + "/api/authenticate";
			Logger.writeln("Try login remote OCPP server: " + url);
			try {
				JSONObject loginInfo 	= new JSONObject();
				StringBuilder sb 		= new StringBuilder(); 
				loginInfo.put("username", remoteOcppServerUsername);
				loginInfo.put("password", remoteOcppServerPassword);
				
				HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
		        con.setRequestMethod("POST");
		        con.setDoOutput(true);
		        con.setDoInput(true);
		 
		        con.setRequestProperty("Content-Type", "application/json;");
		        con.setRequestProperty("Accept", "application/json,text/plain");
		        
		        OutputStream os = con.getOutputStream();
		        os.write(loginInfo.toString().getBytes());
		        os.close();
		        
		        int HttpResult = con.getResponseCode();
		        
		        if(HttpResult == HttpsURLConnection.HTTP_OK){
		        	BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));  
		            String line = null;
		            while ((line = br.readLine()) != null) {  
		            	sb.append(line + "\n");  
		            }
		            br.close();
		            
		            JSONObject resultObject = new JSONObject(sb.toString());
		            remoteOcppServerAuthToken = resultObject.getJSONObject("response").getString("token");
					Logger.writeln("Remote OCPP server login Success, token: " + remoteOcppServerAuthToken);
		        }else{
		        	String errorResult = new BufferedReader(new InputStreamReader(con.getErrorStream())).lines().collect(Collectors.joining("\n"));
					Logger.writeln("Remote OCPP server login fail: " + errorResult);
		        }
			        
			} catch (MalformedURLException e) {
				Logger.writeln("Get remote OCPP server:" + url + " auth token fail with MalformedURLException: " + e.getMessage());
			} catch (IOException e) {
				Logger.writeln("Get remote OCPP server:" + url + " auth token fail with IOException: " + e.getMessage());
			} catch (Exception e){
				Logger.writeln("Get remote OCPP server:" + url + " auth token fail with Exception: " + e.getMessage());
			}
		}
	}
	
	private synchronized void sendMessageToRemoteServer(String msg, JSONArray reply) throws IOException  {
		if(!remoteOcppServerAddress.isEmpty()) {
			String url = remoteOcppServerAddress + "/api/cp_requests";
			Logger.writeln("Try connect remote server: " + url);
			if(remoteOcppServerAuthToken.isEmpty()) {
				getRemoteServerAuthToken();
			}
			
			try {
				JSONObject newInfo = new JSONObject();
				JSONArray msgObject = new JSONArray(msg);
				newInfo.put("message", msgObject);
				newInfo.put("reply", reply);
				newInfo.put("station_name", stationName);
				remoteOcppNonSend.put(newInfo);
		        HttpURLConnection con = (HttpURLConnection)(new URL(url).openConnection());
		        con.setRequestMethod("POST");
		        con.setDoOutput(true);
		        con.setDoInput(true);
		 
		        con.setRequestProperty("Content-Type", "application/json;");
		        con.setRequestProperty("Accept", "application/json,text/plain");
		        con.setRequestProperty("Authorization", remoteOcppServerAuthToken);
		        OutputStream os = con.getOutputStream();
//		        Logger.writeln("remoteOcppNonSend length: " + remoteOcppNonSend.length() );
		        if("heartbeat".equals(msgObject.getString(2).toLowerCase())) {
		        	remoteOcppNonSend.remove(remoteOcppNonSend.length() - 1);
//		        	Logger.writeln("remove heartbeat from remoteOcppNonSend list");
		        }
		        if(remoteOcppNonSend.length() > 500) {
		        	Logger.writeln("remoteOcppNonSend length: " + remoteOcppNonSend.length() +  " is > 500 so split to call!");
		        	JSONArray tempJSONArray = new JSONArray();
		        	int count = 0;
		        	for(Object o : remoteOcppNonSend) {
		        		if(count == 500) {
		        			break;
		        		}
		        		count++;
		        		tempJSONArray.put(o);
		        	}
//		        	Logger.writeln("first 500 remoteOcppNonSend: " + tempJSONArray.toString());
		        	os.write(tempJSONArray.toString().getBytes());
		        } else {
		        	os.write(remoteOcppNonSend.toString().getBytes());
		        }
		        os.close();
		        int HttpResult = con.getResponseCode();
		        
		        if(HttpResult == HttpsURLConnection.HTTP_OK){
					Logger.writeln("Connect remote server Success: " + HttpResult);
					
					if(remoteOcppNonSend.length() > 500) {
			        	Logger.write("HTTP_OK remoteOcppNonSend length: " + remoteOcppNonSend.length() +  " is > 500 so delete frist 500.");
						JSONArray tempJSONArray = new JSONArray();
						for(int a = 500 ; a < remoteOcppNonSend.length() ; a++) {
							tempJSONArray.put(remoteOcppNonSend.get(a));
						}
						remoteOcppNonSend =  new JSONArray(tempJSONArray);
						Logger.write("new remoteOcppNonSend: " + remoteOcppNonSend.toString());
		    			FileOutputStream fos = new FileOutputStream(remoteOcppNonSendFile);
		    			fos.write(remoteOcppNonSend.toString().getBytes());
		    			fos.close();
					} else {
			        	remoteOcppNonSend = new JSONArray();
			        	if(remoteOcppNonSendFile.exists()) {
			        		remoteOcppNonSendFile.delete();
			        	}
					}
		        } else{
	        		if (HttpResult == 403) {
	        			Logger.writeln("Connect remote server Fail with AccessToken");
			        	getRemoteServerAuthToken();
	        		} else {
			        	String errorResult = new BufferedReader(new InputStreamReader(con.getErrorStream())).lines().collect(Collectors.joining("\n"));  
						Logger.writeln("Connect remote server Fail: " + HttpResult + ":" + errorResult);
	        		}
		    		FileOutputStream fos = new FileOutputStream(remoteOcppNonSendFile);
	    			fos.write(remoteOcppNonSend.toString().getBytes());
	    			fos.close();
		        }
			} catch (MalformedURLException e) {
				Logger.writeln("Send message to remote OCPP server: " + url + " fail with MalformedURLException: " + e.getMessage());
			} catch (Exception e){
				Logger.writeln("Get remote OCPP server:" + url + " auth token fail with Exception: " + e.getMessage());
			}
		}
	}
	
	@OnMessage
	public void onMessage(Session s, String msg) {
//		Logger.writeln("Server received " + msg);
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				Logger.writeln("msg: " + msg );
				JSONArray a = new JSONArray(msg);
				if(a.getInt(0) == 2) {
					JSONArray reply = new JSONArray();
					reply.put(3);
					reply.put(a.getString(1));
					switch(a.getString(2).toLowerCase()) {
					case "authorize":
						reply.put(authorize(a.getJSONObject(3)));
						break;
					case "bootnotification":
						reply.put(bootNotification(a.getJSONObject(3)));
						break;
					case "heartbeat":
						reply.put(heartbeat(a.getJSONObject(3)));
						break;
					case "metervalues":
						reply.put(meterValues(a.getJSONObject(3)));
						break;
					case "starttransaction":
						reply.put(startTransaction(a.getJSONObject(3)));
						break;
					case "statusnotification":
						reply.put(statusNotification(a.getJSONObject(3)));
						break;
					case "stoptransaction":
						reply.put(stopTransaction(a.getJSONObject(3)));
						break;
					default:
						break;
					}
					
//					Logger.writeln("reply: " + reply.toString() );
					
					if(isOnline) {
						s.getAsyncRemote().sendText(reply.toString());
					}
					switch(a.getString(2).toLowerCase()) {
					case "authorize":
						break;
					case "bootnotification":
						if(session.exists()) {
							try {
								FileInputStream fis = new FileInputStream(session);
								byte[] b = new byte[fis.available()];
								fis.read(b);
								fis.close();
								transaction = new JSONObject(new String(b));
								Logger.writeln("transaction msg: " + transaction.toString() );
								triggerMessage();
							} catch (IOException e) {
							}
						}
						break;
					case "heartbeat":
						break;
					case "starttransaction":
						break;
					case "statusnotification":
						break;
					case "stoptransaction":
						break;
					default:
						break;
					}
					
					new Thread(() -> {
						try {
							sendMessageToRemoteServer(msg, reply);
						}catch(IOException e) {
							Logger.writeln("IOException when connect to remote OCPP server: ");
						}
					}).start();
				} else {
					
				}
//			}
//			
//		}).start();
//
	}
	
	@OnOpen
	public void open(Session session) {
		clients.add(session);
	}
	
	public void remoteStartTransaction() {
		OCPP.RemoteStartTransaction.req request = new OCPP.RemoteStartTransaction.req("12345678", 1, null);
		request.setChargingProfile(new ChargingProfile(20695, 0, ChargingProfilePurposeType.TxProfile, ChargingProfileKindType.Absolute, new ChargingSchedule(21600, 0, ChargingRateUnitType.W, new ChargingSchedulePeriod(21600, (float)999.9, 0))));
		boardcast("RemoteStartTransaction", new Gson().toJson(request));
		count = 1;
	}
		
	public void remoteStopTransaction() {
		JSONObject request = new JSONObject();
		request.put("transactionId", tid.getValue());
		boardcast("RemoteStopTransaction", request.toString());
	}

	public void removeCard(String cid) {
		validID.remove(cid);
		try {
			FileOutputStream fos = new FileOutputStream(keys);
    		fos.write(validID.toString().getBytes());
    		fos.close();
    		Runtime.getRuntime().exec("sync");
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
	}
	
	public void reset() {
    	keys.delete();
    	session.delete();
    	transactionsFile.delete();
	}
	
	private void setAuthorizedList(JSONArray nfcList) {
		validID = new JSONObject();

		for(int i = 0; i < nfcList.length(); ++i) {
			StringTokenizer st = new StringTokenizer(nfcList.getString(i), "-");
			String cid = st.nextToken();
			String uid = st.nextToken();
			validID.put(cid, uid);
		}
		
		try {
			FileOutputStream fos = new FileOutputStream(keys);
    		fos.write(validID.toString().getBytes());
    		fos.close();
    		Runtime.getRuntime().exec("sync");
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		
	}
	
	public void setAuthorizedList(JSONObject list) {
		validID = list;
	}

	public void setChargingProfile() {
		OCPP.ChargingSchedulePeriod[] schedulePeriods = {new OCPP.ChargingSchedulePeriod(0, (float)16.0, 3), new OCPP.ChargingSchedulePeriod(10, (float)8.0, 1)};
		OCPP.ChargingSchedule schedule = new OCPP.ChargingSchedule(OCPP.ChargingRateUnitType.W, schedulePeriods);
		OCPP.ChargingProfile profile = new OCPP.ChargingProfile(1, 0, OCPP.ChargingProfilePurposeType.TxProfile, OCPP.ChargingProfileKindType.Absolute, schedule);
		boardcast("SetChargingProfile", new JSONObject().put("connectorId", 1).put("csChargingProfiles", new JSONObject(profile)).toString());		
	}
	
	public JSONObject startTransaction(JSONObject json) {
		transaction = new JSONObject();
		startTransaction = new Gson().fromJson(json.toString(), OCPP.StartTransaction.req.class);
		meterReading = startTransaction.getMeterStart();
		transaction.put("CardId", startTransaction.getIdTag().toLowerCase() + "-" + validID.optString(startTransaction.getIdTag()));
		transaction.put("StartTime", startTransaction.getTimestamp());
		transaction.put("meterStart", startTransaction.getMeterStart());
		try {
			session.createNewFile();
			FileOutputStream fos = new FileOutputStream(session);
			fos.write(transaction.toString().getBytes());
			fos.close();
			
		} catch (IOException e) {
		}
		
		transaction.put("tId", Integer.toString(tid.getValue()));

		chargingStartTime = Long.MAX_VALUE;
		chargingStopTime = Long.MIN_VALUE;
		return new JSONObject(new Gson().toJson(new OCPP.StartTransaction.conf(new OCPP.IdTagInfo(OCPP.AuthorizationStatus.Accepted), tid.getValue())));
	}
	
	public JSONObject statusNotification(JSONObject json) {
		OCPP.StatusNotification.req req = new Gson().fromJson(json.toString(), OCPP.StatusNotification.req.class);
		if(req.getStatus() == OCPP.ChargePointStatus.Charging) {
			chargingStopTime = Long.MIN_VALUE;
			if(System.currentTimeMillis() < chargingStartTime) {
				chargingStartTime = System.currentTimeMillis();
			}
		} else {
			if(System.currentTimeMillis() > chargingStopTime && chargingStartTime != Long.MAX_VALUE) {
				chargingStopTime = System.currentTimeMillis();
			}
		}
		JSONObject reply = new JSONObject();
		return reply;
	}

	public JSONObject stopTransaction(JSONObject json) {
		OCPP.StopTransaction.req stopTransaction = new Gson().fromJson(json.toString(), OCPP.StopTransaction.req.class);
		
		if(stopTransaction.getTransactionId() != tid.getValue()) {
			JSONObject reply = new JSONObject();
			JSONObject idTag = new JSONObject();
			idTag.put("status", "Invalid");
			reply.put("idTagInfo", idTag);
			return reply;			
		}

		tid.setValue(tid.getValue() + 1);
		
		try {
			long dayStart = ((System.currentTimeMillis() + timeZone * 60 * 60 * 1000) / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000) - timeZone * 60 * 60 * 1000;

			transaction.put("StopTime", stopTransaction.getTimestamp());
			if(chargingStopTime == Long.MIN_VALUE) {
				if(chargingStartTime == Long.MAX_VALUE) {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(0)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(0)));
				} else {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(chargingStartTime < dayStart ? dayStart : chargingStartTime)));
					transaction.put("StopCharge", stopTransaction.getTimestamp());
				}
			} else {
				if(chargingStopTime < dayStart) {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(0)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(0)));
				} else {
					transaction.put("StartCharge", OCPP.dateTime.format(new Date(chargingStartTime < dayStart ? dayStart : chargingStartTime)));
					transaction.put("StopCharge", OCPP.dateTime.format(new Date(chargingStopTime)));
				}
			}
			transaction.put("meterStop", stopTransaction.getMeterStop());
			transaction.put("Resend", 0);
			transactions.put(transaction);

		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		try {
			FileOutputStream fos = new FileOutputStream(transactionsFile);
			fos.write(transactions.toString(2).getBytes());
			fos.close();
			session.delete();
		} catch (IOException e) {
		}
		
		JSONObject reply = new JSONObject();
		JSONObject idTag = new JSONObject();
		idTag.put("status", "Accepted");
		reply.put("idTagInfo", idTag);
		return reply;
	}
	
	public void triggerMessage() {
		JSONObject request = new JSONObject();
		request.put("requestedMessage", "GetExceptionalStop");
		request.put("connectorID", 1);
		boardcast("TriggerMessage", request.toString());
	}
	
	public void clearRecord() {
		transactionsFile.delete();
		transactions = new JSONArray();
	}

	@Override
	public String getConfig() {
		JSONArray a = new JSONArray();
		for(String id : validID.keySet()) {
			a.put(id);
		}

		JSONObject config = new JSONObject();
		JSONObject json = new JSONObject();
		json.put("Type", "Text");
		json.put("Authority", "Everyone");
		json.put("Value", "32");
		config.put("Capacity", json);
		json = new JSONObject();
		json.put("Type", "List");
		json.put("Authority", "Everyone");
		json.put("Value", a);
		config.put("NFC", json);

		return config.toString();
	}

	@Override
	public void setConfig(String config) {
		System.out.println(this.getClass().getSimpleName() + " Set : " + config);
	}
	
	public void stopServer() {
		server.stop();
	}
}
