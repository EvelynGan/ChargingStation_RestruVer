package cornerstone;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;

//import java.io.BufferedReader;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.PrintWriter;
//import java.net.Socket;
//import java.net.UnknownHostException;
//
//public class RemoteRxClient {
//	 
//    public static void main(String[] args) {
//        if (args.length < 1) return;
// 
//        String domainName = args[0];
// 
//        //String hostname = "whois.internic.net";
//        String hostname = "192.168.1.85";
//        
//        int port = 743;
// 
//        try (Socket socket = new Socket(hostname, port)) {
// 
//            OutputStream output = socket.getOutputStream();
//            PrintWriter writer = new PrintWriter(output, true);
//            //writer.println(domainName);
// 
//            InputStream input = socket.getInputStream();
// 
//            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
// 
//            String line;
// 
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//        } catch (UnknownHostException ex) {
// 
//            System.out.println("Server not found: " + ex.getMessage());
// 
//        } catch (IOException ex) {
// 
//            System.out.println("I/O error: " + ex.getMessage());
//        }
//    }
//}



//public class RemoteRxClient {
//	public static void main(String[] args) {  
//		try{      
//			Socket s=new Socket("localhost",6666);  
//			DataOutputStream dout=new DataOutputStream(s.getOutputStream());  
//			dout.writeUTF("Hello Server");  
//			dout.flush();  
//			dout.close();  
//			s.close();  
//		}catch(Exception e){System.out.println(e);}  
//		}  
//}

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

//import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;
//import net.sf.json.JSONObject;
import com.google.gson.Gson;

import victorho.ocpp.OCPP;
import victorho.util.DataListener;
import victorho.util.Logger;

import jdk.nashorn.internal.parser.JSONParser;


public class ImageTxClient implements Runnable{

	static final int port = 4445;
	//static final String serverIP = "192.168.1.66";
	//static final String serverIP = "192.168.43.66";
	//static final String serverIP = "192.168.1.155";
	//static String serverIP = "192.168.1.76";
	static String serverIP = "127.0.0.1";
	
	private int serialno=0;
	private Socket socket;
    private DataOutputStream outputStream; 
    private DataInputStream inputStream;
    private int cntHb;
    private static final int TIME_HEARTBEAT = 30*1000;
    private static final long TIMER_HEARTBEAT = 5000; 
    private boolean hbFlag=false;
    private HeartBeatTimer hbt;
    private ChargingStation cs;
    private String cpName;
    private	int logcnt=0;
    Vector<DataListener> listenerlist;
    private int cnt=0;
    private int errCntSocketIO = 0;
    private int errCntSocketUnknownHost = 0;
    private int errCntJson = 0;
    private int errCntSocketSend = 0;
    boolean socketError = false;

	public void addDataListener(DataListener listener) {
		listenerlist.add(listener);
	}
    
	public ImageTxClient(ChargingStation csIn, String cpNameIn, String host) {
		cs = csIn;
		cpName = cpNameIn;
		serverIP = host;
	}
	
	 
    public void run() {

    	Logger.writeln("start img Tx thread");
    	hbt = new HeartBeatTimer(1, 30);
    	while(true) {	
    		
//   		sendImage();
    		try {
    			if(cs.getImgsTx().size()>0) {
    				
    				if(socketError) {
    		    		try {
    		    			Thread.sleep(60000);
    		    			Logger.writeln("network error");
    		    		}catch (InterruptedException e1) {
    		    			
    		    		}   				
    				}
    				
    				sendLprsImage(cs.getImgsTx().get(0));
    				cs.getImgsTx().remove(0);
    			//	writeErrLog();
    			}
    		}catch (Exception e) {
    			
    		}
    		
    		try {
    			Thread.sleep(100);

    		}catch (InterruptedException e1) {
    			
    		}

    	}
    }
   
  
    private void sendImage() {
    	
		try 
		{
			logcnt++;
			if(logcnt>120) {
				logcnt=0;
				Logger.writeln("Lprs Img Tx alive");
			}
			
			socket = new Socket(serverIP, port);
	        outputStream = new DataOutputStream(socket.getOutputStream());
	        inputStream = new DataInputStream(socket.getInputStream());
	       	
	       while (!socket.isClosed()) { 
	       		if(cs.getImgsTx().size()>0) {
	       			outputStream.writeUTF(cpName);	             
	            	ImageIO.write(cs.getImgsTx().get(0), "jpg", outputStream);
	            	outputStream.close();
	            	inputStream.close();		 
	            	cs.getImgsTx().remove(0);
	       		}
	                   				
				try {
					Thread.sleep(20);
				}catch (Exception e) {
				}
				
	       }
	       
	     
			}catch (UnknownHostException ex) {
             //  Logger.writeln("Server not found: " + ex.getMessage());
				
              try { 
               if(outputStream !=null) {
					outputStream.close();
				}					
				if(inputStream !=null) {
					inputStream.close();
				}					
				if(socket !=null) {
					socket.close();
				}              
              }catch (Exception e){
            	  Logger.writeln("Error closing socket " + ex.getMessage());
              }
     
            } catch (IOException ex) {
            	
           // 	Logger.writeln("I/O error: " + ex.getMessage());
				try {
//					if(!socket.isClosed()) {
//						outputStream.close();
//						inputStream.close();
//						socket.close();
//						Logger.writeln("error found, client close socket");
//					}
					if(outputStream !=null) {
						outputStream.close();
					}					
					if(inputStream !=null) {
						inputStream.close();
					}					
					if(socket !=null) {
						socket.close();
					}
					
				} catch (Exception e) {

				}
				
				
				
				try {
				Thread.sleep(10);
				} catch (Exception e) {

				}				
				
				
            }catch (Exception e) {
            	try {
                if(outputStream !=null) {
 					outputStream.close();
 				}					
 				if(inputStream !=null) {
 					inputStream.close();
 				}					
 				if(socket !=null) {
 					socket.close();
 				}              
               }catch (Exception e1){
             	 // Logger.writeln("Error closing socket " + e1.getMessage());
               }
			}
    }
       
    
	public class HeartBeatTimer {
	    private Timer timer;
	    boolean end = false;
	    
	    public HeartBeatTimer(int delay, long seconds) {
	        timer = new Timer();
	        timer.schedule(new HbTimer(), delay, seconds*1000);
		}

	    class HbTimer extends TimerTask {
	        public void run() {
	        	hbFlag = true;
	        	//logger.info("hb timer");
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
	}
	
	public void sendLprsImage(BufferedImage img) {
		
		if(socket == null) {
			try {
	//		Logger.writeln("try open new lprs imgTX socket");
			socket = new Socket(serverIP, port);
	        outputStream = new DataOutputStream(socket.getOutputStream());
	        socketError =  false;
	     //   inputStream = new DataInputStream(socket.getInputStream());
			} catch (UnknownHostException e) {
				errCntSocketUnknownHost++;
				e.printStackTrace();
				socketError =  true;
			} catch (JSONException e) {
				errCntJson++;
				e.printStackTrace();
				socketError =  true;
			} catch (IOException e) {
				e.printStackTrace();
				errCntSocketIO++;
				socketError =  true;
			}
		}
		if(socket != null) {
			try {
	       			outputStream.writeUTF(cpName);
	            	ImageIO.write(img, "jpg", outputStream);
	            	outputStream.close();
	        //    	socketError =  false;
	       //     	inputStream.close();
			} catch (IOException e) {
				socket = null;
				errCntSocketSend++;
			//	socketError =  true;

			}
		}
	
	}
 
	
	private void writeErrLog() {
		
		cnt++;
		if(cnt>240) {
			cnt=0;
			Logger.writeln("send img buffer alive");
		}		
		
		if(errCntSocketIO>20) {
			errCntSocketIO=0;
			Logger.writeln("serrCntSocketIO > 20");
		}   			
		if(errCntSocketUnknownHost>20) {
			errCntSocketUnknownHost=0;
			Logger.writeln("errCntSocketUnknownHost > 20");
		}  		

		if(errCntJson>100) {
			errCntJson=0;
			Logger.writeln("errCntJson > 100");
		}   			
		if(errCntSocketSend>100) {
			errCntSocketSend=0;
			Logger.writeln("errCntSocketSend > 100");
		} 
		
	}
}

