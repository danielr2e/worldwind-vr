package com.tuohy.worldwindvr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class LaunchDialog extends JDialog {

	JCheckBox hiResImagerySelector = new JCheckBox();
	JCheckBox precacheModeSelector = new JCheckBox();
	
	public LaunchDialog(){
		this.setTitle("WorldWindVR");
		
		//initialize UI components
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel optionsPane = new JPanel(new GridBagLayout());
		JLabel resLabel = new JLabel("Resolution");
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
				new WorldWindVR(hiResImagerySelector.isSelected(), precacheModeSelector.isSelected());
			}
			
		});
		launchButton.setPreferredSize(new Dimension(60, 30));
		hiResImagerySelector.setSelected(true);
		
		//layout the UI
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5,5,5,5);
		c.ipadx = 5;
		c.ipady = 5;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		optionsPane.add(resBox, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		optionsPane.add(resLabel, c);
		c.gridy = 1;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
	    c.weightx = 0.1;
	    c.weighty = 1.0;
		optionsPane.add(hiResImagerySelector,c);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		optionsPane.add(new JLabel("Use MS Virtual Earth Hi-Res Imagery (recommended)"),c);
		c.gridy = 2;
		c.gridx = 0;
		c.anchor = GridBagConstraints.EAST;
	    c.weightx = 0.1;
	    c.weighty = 1.0;
		optionsPane.add(precacheModeSelector,c);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		optionsPane.add(new JLabel("First Time? Start in Imagery Pre-caching Mode"),c);
		
		mainPanel.add(new JLabel(new ImageIcon("resources/splash.png")),BorderLayout.NORTH);
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
		

		setSize(420,420);
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
