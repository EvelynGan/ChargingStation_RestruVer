package cornerstone;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
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

public class AsiaVisionLPRS extends Thread {
		
	private long t;
	BufferedImage img;
	String server;
	Vector<DataListener> listenerlist;
	
	public AsiaVisionLPRS(String server) {
		this.server = server;
		listenerlist = new Vector<DataListener>();
	}
	
	public void addDataListener(DataListener listener) {
		listenerlist.add(listener);
	}
	
	public void run() {
		while(true) {
			if(img != null) {
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        try {
					ImageIO.write(img, "jpg", baos);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
		        try {
					baos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}

		        try {
			        Socket socket = new Socket(server, 11000);
			        InputStream input = socket.getInputStream();
			        OutputStream output = socket.getOutputStream();
			        int fs = baos.size();
			        byte[] fileSize = new byte[4];
			        for(int i = 0; i < 4; ++i) {
			        	fileSize[i] = (byte)(fs % 256);
			        	fs >>= 8;
			        }
			        output.write(fileSize);
			        output.write(baos.toByteArray());
			        long time = System.currentTimeMillis();
			        while(System.currentTimeMillis() - time < 5000 && input.available() == 0) {
			            try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}	
			        }
			        byte[] b = new byte[input.available()];
			        input.read(b);
			        if(b.length > 0 && b[0] != (byte)'*') {
			        	Logger.writeln("Licsense Plate : " + new String(b));
			        	for(int i = 0; i < listenerlist.size(); ++i) {
			        		listenerlist.get(i).dataReceived(new DataEvent(this, b));
			        	}			        	
//						ImageIO.write(img, "png", new File("/dev/shm/S_" + new String(b) + new Date() + ".png"));
//			        } else {
//						ImageIO.write(img, "png", new File("/dev/shm/F_" + new String(b) + new Date() + ".png"));
			        }
//					Thread.sleep(1000);
			        input.close();
			        output.close();
			        socket.close();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				img = null;
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
			if((System.currentTimeMillis() - t) >= 500) {
				t = System.currentTimeMillis();
				this.img = img;
				
				if(!this.isAlive()) {
					this.start();
				}
			}
		}
	}

}
