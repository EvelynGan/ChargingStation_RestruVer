package cornerstone;

import javax.swing.JButton;
import victorho.util.Logger;

public class SoftButton {
	/**
	 * 
	 */
	
	private JButton softBtn;
	private boolean isVisible=false;
	
	public JButton getSoftButton() {
		return softBtn;
	}
	
	public void setVisible(boolean isEnable) {	
		if(isEnable) {
			isVisible=true;
			if(softBtn != null) {
				softBtn.setEnabled(true);
				softBtn.setVisible(true);
				Logger.writeln("stop button enabled");
			}
		} else {
			isVisible=false;
			if(softBtn != null) {
				softBtn.setEnabled(false);
				softBtn.setVisible(false);
				Logger.writeln("stop button disable");
			}
		}
	}
	
	public void setSoftButton(JButton btn) {
		softBtn = btn;
	}
	
	public boolean getIsVisible() {
		return isVisible;
	}
	
	public void setEnable(boolean input) {
		softBtn.setEnabled(input);
	}
	
}

