package com.tuohy.worldwindvr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class LaunchDialog extends JDialog {

	public LaunchDialog(){
		this.setTitle("WorldWindVR");
		
		//initialize UI components
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel optionsPane = new JPanel(new GridBagLayout());
		JLabel resLabel = new JLabel("Screen Resolution: ");
		final JComboBox<ScreenResolution> resBox = new JComboBox<ScreenResolution>();
		resBox.addItem(new ScreenResolution(1280,800));
		resBox.addItem(new ScreenResolution(1920,1080));
		resBox.addItem(new ScreenResolution(3840,2160));
		resBox.setSelectedIndex(1);
		JButton launchButton = new JButton("Launch!");
		launchButton.addActionListener(new ActionListener(){

			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				ScreenResolution res = (ScreenResolution) resBox.getSelectedItem();
				WorldWindVRConstants.RenderHorizontalResolution = res.w;
				WorldWindVRConstants.RenderVerticalResolution = res.h;
				new WorldWindVR();
			}
			
		});
		launchButton.setPreferredSize(new Dimension(60, 30));
		
		//layout the UI
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.ipadx = 10;
		c.ipady = 5;
		c.gridx = 0;
		c.gridy = 0;
		optionsPane.add(resLabel, c);
		c.gridx = 1;
		optionsPane.add(resBox, c);
		c.gridy = 1;
		
		mainPanel.add(optionsPane, BorderLayout.CENTER);
		mainPanel.add(launchButton,BorderLayout.SOUTH);
		this.setContentPane(mainPanel);
		
		this.pack();
		
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() { 
		    @Override public void windowClosed(WindowEvent e) { 
		        System.exit(0);
		      }
		    });
	}
	
	public class ScreenResolution{
		
		int w;
		int h;
		
		public ScreenResolution(int w, int h){
			this.w = w;
			this.h = h;
		}
		
		public String toString(){
			return w + " x " + h;
		}
	}
	
}
