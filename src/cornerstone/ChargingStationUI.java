/**
 * @author Ho Wai Fung
 * @date 10/3/2017
 *
 */
package cornerstone;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.json.JSONObject;
import org.xml.sax.InputSource;

import javafx.embed.swing.JFXPanel;
import victorho.gui.SettingPanel;
import victorho.util.Logger;
import victorho.util.DataListener;

public class ChargingStationUI extends JLayeredPane {//implements ActionListener {
	/**
	 * 
	 */
	public static Color Red = new Color(255, 0, 0);
	public static Color Orange = new Color(255, 63, 0);
	public static Color Green = new Color(0, 255,  0);
	public static Color Blue = new Color(0, 0, 255);
	
	private static final long serialVersionUID = 1L;
	private static final int ConfigInset = 30;
	public static final DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Font textFont = new Font("Arial", Font.PLAIN, 36);
	private JLabel pip;
//	private ArrayList<OverlayText> overlayText;
	private JLabel bgImage;
	private JLabel overlay;
	private JLabel time;
	private JLabel info;
	
//	Time Select
	private JLabel selectedHour;
	private JLabel selectedMin;
	private JLabel remainingTime;
	private JLabel voltageTag;
	private JLabel currentTag;
	private JLabel energyTag;
	private JLabel selectedMsg;

//	Queueing
	private JLabel queueingNum;
	private JLabel queueingTime;
	public static final DateFormat queueingTimeFt = new SimpleDateFormat("hh:mm a");
	
//	private JLabel debug;
	private JLabel[] powerReading;
	private JFXPanel configPage;
	private Dimension size;
	private JLabel versionNum;
	
	private XPath xpath;
	private InputSource xml;
	
	public ChargingStationUI(Point location, Dimension size) {
		super();
		
		try {
			Font f = Font.createFont(Font.TRUETYPE_FONT, new File("./resources/fonts/NotoSansTC-Medium.otf"));
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(f);
			
//			String fonts[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
//		    for ( int i = 0; i < fonts.length; i++ )
//		    {
//		    	Logger.writeln("Font: " + fonts[i]);
//		    }
		} catch (IOException e) {
			Logger.writeln("Create Font with IOException: " + e.getMessage());		
		} catch (FontFormatException e) {
			Logger.writeln("Create Font with FontFormatException: " + e.getMessage());		
		}
		
		//read ui config
        XPathFactory xpf = XPathFactory.newInstance();
        xpath = xpf.newXPath();
        xml = new InputSource("./resources/ui_config.xml" );
		

    	dateTime.setTimeZone(TimeZone.getTimeZone("GMT+8"));
    	queueingTimeFt.setTimeZone(TimeZone.getTimeZone("GMT+8"));

		setPreferredSize(size);
		
		setCursor(Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR), new Point(0, 0), "blank cursor"));
		setLayout(null);
		
//		overlayText = new ArrayList<OverlayText>();
//
//		debug = new JLabel();
//		debug.setOpaque(false);
//		debug.setBounds(0, size.height - 24, 200, 24);
//		add(debug);
		
		info = new JLabel();
		info.setOpaque(false);
		//info.setBounds(200, size.height - 24, size.width - 200, 24);
		info.setBounds(getUIConfig("ui_config/info/x"), getUIConfig("ui_config/info/y"), getUIConfig("ui_config/info/w"), getUIConfig("ui_config/info/h"));
		info.setHorizontalAlignment(SwingConstants.RIGHT);
		add(info, new Integer(2));
		
		time = new JLabel();
		time.setOpaque(false);
		time.setBounds(getUIConfig("ui_config/time/x"), getUIConfig("ui_config/time/y"), getUIConfig("ui_config/time/w"), getUIConfig("ui_config/time/h"));
		time.setHorizontalAlignment(SwingConstants.RIGHT);
		add(time, new Integer(2));
		
		selectedHour = new JLabel();
		selectedHour.setOpaque(false);
		selectedHour.setBounds(getUIConfig("ui_config/selectedHour/x"), getUIConfig("ui_config/selectedHour/y"), getUIConfig("ui_config/selectedHour/w"), getUIConfig("ui_config/selectedHour/h"));
		selectedHour.setHorizontalAlignment(SwingConstants.CENTER);
		selectedHour.setVerticalAlignment(SwingConstants.CENTER);
		selectedHour.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/selectedHour/f")));
		selectedHour.setForeground(Color.WHITE);
		selectedHour.setVisible(false);
		add(selectedHour, new Integer(2));
		
		selectedMin = new JLabel();
		selectedMin.setOpaque(false);
		selectedMin.setBounds(getUIConfig("ui_config/selectedMin/x"), getUIConfig("ui_config/selectedMin/y"), getUIConfig("ui_config/selectedMin/w"), getUIConfig("ui_config/selectedMin/h"));
		selectedMin.setHorizontalAlignment(SwingConstants.CENTER);
		selectedMin.setVerticalAlignment(SwingConstants.CENTER);
		selectedMin.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/selectedMin/f")));
		selectedMin.setForeground(Color.WHITE);
		selectedMin.setVisible(false);
		add(selectedMin, new Integer(2));
		
		queueingNum = new JLabel();
		queueingNum.setOpaque(false);
		queueingNum.setBounds(getUIConfig("ui_config/queueingNum/x"), getUIConfig("ui_config/queueingNum/y"), getUIConfig("ui_config/queueingNum/w"), getUIConfig("ui_config/queueingNum/h"));
		queueingNum.setHorizontalAlignment(SwingConstants.CENTER);
		queueingNum.setVerticalAlignment(SwingConstants.CENTER);
		queueingNum.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/queueingNum/f")));
		queueingNum.setForeground(Color.WHITE);
		queueingNum.setVisible(false);
		add(queueingNum, new Integer(2));
		
		queueingTime = new JLabel();
		queueingTime.setOpaque(false);
		queueingTime.setBounds(getUIConfig("ui_config/queueingTime/x"), getUIConfig("ui_config/queueingTime/y"), getUIConfig("ui_config/queueingTime/w"), getUIConfig("ui_config/queueingTime/h"));
		queueingTime.setHorizontalAlignment(SwingConstants.CENTER);
		queueingTime.setVerticalAlignment(SwingConstants.CENTER);
		queueingTime.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, 70));
		queueingTime.setForeground(Color.WHITE);
		queueingTime.setVisible(false);
		add(queueingTime, new Integer(2));
		
			
		remainingTime = new JLabel();
		remainingTime.setOpaque(false);
		remainingTime.setBounds(getUIConfig("ui_config/remainingTime/x"), getUIConfig("ui_config/remainingTime/y"), getUIConfig("ui_config/remainingTime/w"), getUIConfig("ui_config/remainingTime/h"));
		remainingTime.setHorizontalAlignment(SwingConstants.LEFT);
		remainingTime.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/remainingTime/f")));
		remainingTime.setForeground(Color.WHITE);
		remainingTime.setVisible(false);
		add(remainingTime, new Integer(2));
		
		voltageTag = new JLabel();
		voltageTag.setOpaque(false);
		voltageTag.setBounds(getUIConfig("ui_config/voltageTag/x"), getUIConfig("ui_config/voltageTag/y"), getUIConfig("ui_config/voltageTag/w"), getUIConfig("ui_config/voltageTag/h"));
		voltageTag.setHorizontalAlignment(SwingConstants.LEFT);
		voltageTag.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/voltageTag/f")));
		voltageTag.setForeground(Color.WHITE);
		voltageTag.setVisible(false);
		add(voltageTag, new Integer(2));
		
		currentTag = new JLabel();
		currentTag.setOpaque(false);
		currentTag.setBounds(getUIConfig("ui_config/currentTag/x"), getUIConfig("ui_config/currentTag/y"), getUIConfig("ui_config/currentTag/w"), getUIConfig("ui_config/currentTag/h"));
		currentTag.setHorizontalAlignment(SwingConstants.LEFT);
		currentTag.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/currentTag/f")));
		currentTag.setForeground(Color.WHITE);
		currentTag.setVisible(false);
		add(currentTag, new Integer(2));
		
		energyTag = new JLabel();
		energyTag.setOpaque(false);
		energyTag.setBounds(getUIConfig("ui_config/energyTag/x"), getUIConfig("ui_config/energyTag/y"), getUIConfig("ui_config/energyTag/w"), getUIConfig("ui_config/energyTag/h"));
		energyTag.setHorizontalAlignment(SwingConstants.LEFT);
		energyTag.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/energyTag/f")));
		energyTag.setForeground(Color.WHITE);
		energyTag.setVisible(false);
		add(energyTag, new Integer(2));
		
        selectedMsg = new JLabel();
        selectedMsg.setOpaque(false);
        selectedMsg.setBounds(getUIConfig("ui_config/selectedMsg/x"), getUIConfig("ui_config/selectedMsg/y"), getUIConfig("ui_config/selectedMsg/w"), getUIConfig("ui_config/selectedMsg/h"));
        selectedMsg.setHorizontalAlignment(SwingConstants.CENTER);
        selectedMsg.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/selectedMsg/f")));
        selectedMsg.setForeground(Color.WHITE);
        selectedMsg.setVisible(false);
		add(selectedMsg, new Integer(2));
		
		
		
        versionNum = new JLabel();
        versionNum.setOpaque(false);
        versionNum.setBounds(getUIConfig("ui_config/versionNum/x"), getUIConfig("ui_config/versionNum/y"), getUIConfig("ui_config/versionNum/w"), getUIConfig("ui_config/versionNum/h"));
        versionNum.setHorizontalAlignment(SwingConstants.RIGHT);
        versionNum.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, getUIConfig("ui_config/versionNum/f")));
        versionNum.setForeground(Color.BLACK);
        versionNum.setVisible(false);
		add(versionNum, new Integer(2));
		
		powerReading = new JLabel[7];
		for(int i = 0; i < powerReading.length; ++i) {
			powerReading[i] = new JLabel();
			powerReading[i].setOpaque(false);
			powerReading[i].setForeground(Color.WHITE);
			powerReading[i].setFont(textFont);
			powerReading[i].setBounds(400 + (i / 2) * 150, 480 + (i % 2) * 60, 180, 60);
			add(powerReading[i], new Integer(2));
		}
		
		JFrame frame = new JFrame();
        frame.getContentPane().add(this);
        
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
        frame.pack();
        
        frame.setBounds(location.x, location.y, size.width, size.height);
        this.size = size;
        
//      frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
//    	java.awt.GraphicsDevice d = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//		d.setFullScreenWindow(frame);
        
		frame.setVisible(true);
	}
	
	public void showImage(Image image) {
		if(bgImage == null) {
//			bgImage = new JLabel(new ImageIcon(image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH)));
			bgImage = new JLabel(new ImageIcon(image));
			bgImage.setBounds(0, 0, size.width, size.height);
			add(bgImage, new Integer(0));
		} else {
//			remove(bgImage);
//			bgImage = new JLabel(new ImageIcon(image));
//			bgImage.setBounds(0, 0, size.width, size.height);
//			add(bgImage, new Integer(0));
//			bgImage.setIcon(new ImageIcon(image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH)));
			bgImage.setIcon(new ImageIcon(image));
		}
		if(overlay != null) {
			remove(overlay);
			overlay = null;
		}
		if(pip != null) {
			remove(pip);
			pip = null;
		}
		repaint();
	}
		
	public void showOverlay(BufferedImage image) {
		if(overlay == null) {
			overlay = new JLabel(new ImageIcon(image));
			overlay.setBounds(0, 0, size.width, size.height);
			add(overlay, new Integer(1));
		} else {
			overlay.setIcon(new ImageIcon(image));
		}
		repaint();
	}
	
	public void showConfig(boolean timeSetting, JSONObject config, String[] buttons, String permission, DataListener listener, String version) {	
        if(configPage == null) {
			configPage = new SettingPanel(timeSetting, config, buttons, permission, listener);
			configPage.setBounds(ConfigInset, ConfigInset, size.width - 2 * ConfigInset, size.height - 2 * ConfigInset);
			versionNum.setText("Version: " + version);
			versionNum.setVisible(true);
			add(configPage, new Integer(2));
        }
	}

	public void updateConfig(JSONObject config) {	
       	((SettingPanel)configPage).update(config);
	}

	public void showInfo(String text, Color c, Font f) {
		info.setText(text);
		repaint();
	}
	
	public void showInfo(String text) {
		info.setText(text);
		repaint();
	}
	
	public void showTime(String time) {
		this.time.setText(time);
	}
	
	public void hideQueueingMsg() {
		queueingTime.setText("");
	}
	
	public void hideQueueing() {
		queueingNum.setText("");
		queueingNum.setVisible(false);
		queueingTime.setText("");
		queueingTime.setVisible(false);
	}
	
	public void hideSelectedMsg() {
		selectedMsg.setVisible(false);
	}
	
	public void updateSelectedMsg(String msg) {
		selectedMsg.setText(msg);
		selectedMsg.setVisible(true);
	}
	
	public void updateQueueing(Date time, int num) {
		if(time != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(time);
			cal.add(Calendar.MINUTE, 1);
			queueingTime.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, 70));
			queueingTime.setText(queueingTimeFt.format(cal.getTime()));
		} else {
			queueingTime.setFont(new Font("Noto Sans TC Medium", Font.PLAIN, 30));
			queueingTime.setText("Unable to estimate 未能預計");
		}
		queueingTime.setVisible(true);
		queueingNum.setText("" + num);
		queueingNum.setVisible(true);
	}
	
	public void showTimeSlot(int minute) {
//		selectedHour.setText(String.format("%02d", minute / 60));
		selectedHour.setText("" + minute / 60);
		selectedHour.setVisible(true);
		
		selectedMin.setText(String.format("%02d", minute % 60));
		selectedMin.setVisible(true);
	}
	
	public void showTimeSlot() {
		selectedHour.setText("--");
		selectedHour.setVisible(true);
		
		selectedMin.setText("--");
		selectedMin.setVisible(true);
	}
	
	public void hideTimeSlot() {
		selectedHour.setText("");
		selectedHour.setVisible(false);
		
		selectedMin.setText("");
		selectedMin.setVisible(false);
	}
	
	public void showRemaingTime(String text) {
		remainingTime.setText(text);
		remainingTime.setVisible(true);
	}
	
	public void hideRemaingTime() {
		remainingTime.setText("");
		remainingTime.setVisible(false);
	}
	
	public void showVoltageTag(String text) {
		voltageTag.setText(text + "V");
		voltageTag.setVisible(true);
	}
	
	public void setAllTag(String voltage, String current, String energy) {
		voltageTag.setText(voltage + "V");
		currentTag.setText(current + "A");	
		energyTag.setText(energy + "kWh");
	}
	
	public void showTag() {
		voltageTag.setVisible(true);
		currentTag.setVisible(true);
		energyTag.setVisible(true);
	}
	
	public void hideTag() {
		voltageTag.setText("");
		voltageTag.setVisible(false);
		
		currentTag.setText("");
		currentTag.setVisible(false);
		
		energyTag.setText("");
		energyTag.setVisible(false);
	}
		
	public File selectDirectory() {
//		DirectoryChooser dc = new DirectoryChooser();
//		dc.setTitle("Select destination");
//		return dc.showDialog(configPage.getScene().getWindow());
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//		fc.setFileView(new FileView() {
//			ImageIcon dirIcon = new ImageIcon("resources/dir.png");
//			ImageIcon fileIcon = new ImageIcon("resources/file.png");
//			
//			@Override
//			public Icon getIcon(File f) {
//				if(f.isDirectory())	{
//					return dirIcon;
//				} else {
//					return fileIcon;
//				}
//			}
//		});
		fc.showSaveDialog(this);
		
		return fc.getSelectedFile();
	}

	public void showConfirmDialog(String msg) {
		JOptionPane.showMessageDialog(this, msg);
	}

	public void showPIP(BufferedImage img) {		
		if(pip == null) {
			pip = new JLabel(new ImageIcon(img));
			pip.setBounds(70, 130, img.getWidth(), img.getHeight());
			add(pip, new Integer(1));
		} else {
			pip.setIcon(new ImageIcon(img));
		}
		repaint();
	}

	public void showPIP(BufferedImage img, int x, int y) {		
		if(pip == null) {
			pip = new JLabel(new ImageIcon(img));
			pip.setBounds(x, y, img.getWidth(), img.getHeight());
			add(pip, new Integer(1));
		} else {
			pip.setIcon(new ImageIcon(img));
		}
		repaint();
	}	
	
	public int getUIConfig(String key) {
        try {
			return Integer.parseInt((String)xpath.evaluate(key, xml, XPathConstants.STRING));
		} catch (NumberFormatException e) {
			Logger.writeln(e.getMessage());
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			Logger.writeln(e.getMessage());
			e.printStackTrace();
		}
        return 0;
	}
	
}
