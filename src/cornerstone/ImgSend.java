package cornerstone;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Vector;

import javax.imageio.ImageIO;

import victorho.util.DataEvent;
import victorho.util.DataListener;
import victorho.util.Logger;

public class ImgSend extends Thread{

	
	private long t;
	BufferedImage img;
	String server;
	String cpName;
	int port=4445;
	Vector<DataListener> listenerlist;
	int logcnt=0;
	int logerrcnt=0;
	private Thread.State state = Thread.State.NEW;
	private int sleepCnt=0;

	public ImgSend(String server, int portIn, String cpNameIn) {
		this.server = server;
		cpName = cpNameIn;
		port = portIn;
		listenerlist = new Vector<DataListener>();
	}
	
	public void addDataListener(DataListener listener) {
		listenerlist.add(listener);
	}
	
	public void run() {
		while(true) {
			if(img != null) {

		        try {
					logcnt++;
					if(logcnt>120) {
						logcnt=0;
						Logger.writeln("Lprs Img Tx alive");
					}
			        Socket socket = new Socket(server, port);
//			        InputStream input = socket.getInputStream();
			        OutputStream output = socket.getOutputStream();
			        DataOutputStream outputStream = new DataOutputStream(output);
			        outputStream.writeUTF(cpName);
			        ImageIO.write(img, "jpg", outputStream);
			       	
	            	outputStream.close();
			        output.close();
			        socket.close();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					logerrcnt++;
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					logerrcnt++;
				} catch (Exception e1) {
					logerrcnt++;
				}
		        
				img = null;
				sleepCnt = 0;
				if(logerrcnt>100) {
					logerrcnt=0;
					Logger.writeln("Lprs Img Tx socket errors happened");
				}
			} else {
				try {
					sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void process(BufferedImage img) {
		if(this.img == null) {
			
			if(System.currentTimeMillis()<t) {				// to handle any problem due to incorrect system time reset/re-sync
				t = System.currentTimeMillis();
				Logger.writeln("lprs process time mark manullay reset to current time");
			}
			
			if((System.currentTimeMillis() - t) >= 1000) {	
				t = System.currentTimeMillis();
				this.img = img;
				
				try {
					state = this.getState();
					if(!this.isAlive()) {
						Logger.writeln("Tx img thread not alive, starts it "+this.getState() );
						this.start();
					}else if (state == Thread.State.TIMED_WAITING){
						++sleepCnt;
						if(sleepCnt>60) {
						Logger.writeln("Tx img thread TIME WAIT abnormally long, state: "+ state);
						sleepCnt = 0;
						}
					}else {
						Logger.writeln("Tx img thread alive but not TIMED WAIT, state "+this.getState() );
					}
				}catch (Exception e) {
					Logger.writeln(e);
				}
			}
			

		}
	}
}
