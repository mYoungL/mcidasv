package edu.wisc.ssec.mcidasv.chooser;

import edu.wisc.ssec.mcidas.*;

import edu.wisc.ssec.mcidasv.data.McIdasXInfo;
import edu.wisc.ssec.mcidasv.data.McIdasFrame;
import edu.wisc.ssec.mcidasv.data.FrameDirectory;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.Defaults;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.PreferenceList;

import visad.*;
import visad.util.*;

/**
 * Widget to select frames from McIdas-X
 * Displays a list of the descriptors (names) of the frame datasets
 */
public class McIdasXChooser extends FrameChooser {

    /** A widget for the command line text */
    private JTextField hostLine;
    private JTextField portLine;
    private JTextField keyLine;

    private boolean goodToGo = true;

    private McIdasXInfo mcidasxInfo;
    
    /**
     * Construct an Adde image selection widget
     *
     * @param idvChooser Chooser to which this interface applies
     * @param descList Holds the preferences for the image descriptors
     */
    public McIdasXChooser(IdvChooser idvChooser,
                               PreferenceList descList) {
/*
        System.out.println("McIdasXChooser constructor:");
        System.out.println("   idvChooser=" + idvChooser);
        System.out.println("   descList=" + descList);
*/
        mcidasxInfo = new McIdasXInfo();
    }

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "McIDAS-X Frame Data";
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        String temp = null;
        return temp;
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */
    protected boolean canReadFrames() {
        return true;
    }

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        List allComps = new ArrayList();
        getComponents(allComps);
        JPanel linkPanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_N, GuiUtils.WT_N);
        return GuiUtils.topCenter(linkPanel, getDefaultButtons(this));
    }

    private void sendHost() {
        //System.out.println("sendHost");
        mcidasxInfo.setHostString((hostLine.getText()).trim());
        addSource();
    }

    private void sendPort() {
        //System.out.println("sendPort");
    	mcidasxInfo.setPortString((portLine.getText()).trim());
        addSource();
    }

    private void sendKey() {
        //System.out.println("sendKey");
    	mcidasxInfo.setKeyString((keyLine.getText()).trim());
        addSource();
    }

    private void addSource() {
        goodToGo = true;
    }
    
    /**
     * Return the host string from the McIdasXInfo object
     * 
     * @return host string
     */
    public String getHost() {
    	return mcidasxInfo.getHostString();
    }
    
    /**
     * Return the port string from the McIdasXInfo object
     * 
     * @return port string
     */
    public String getPort() {
    	return mcidasxInfo.getPortString();
    }
    
    /**
     * Return the key string from the McIdasXInfo object
     * 
     * @return key string
     */
    public String getKey() {
    	return mcidasxInfo.getKeyString();
    }
    
    /**
     * Get the names for the buttons (override).
     *
     * @return array of button names
     */
    protected String[] getButtonLabels() {
        return new String[] { getLoadCommandName(), GuiUtils.CMD_HELP };
    }

    /**
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        List firstLine = new ArrayList();

        /* Host */
        JLabel hostLabel = GuiUtils.rLabel("Host: ");
        firstLine.add(hostLabel);
        hostLine = new JTextField(mcidasxInfo.getHostString(), 10);
        hostLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { sendHost(); }
        });
        hostLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendHost();
            }
        });
        hostLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(hostLine);
        firstLine.add(new JLabel("  "));

        /* Port */
        JLabel portLabel = GuiUtils.rLabel("Port: ");
        firstLine.add(portLabel);
        portLine = new JTextField(mcidasxInfo.getPortString(), 6);
        portLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) { sendPort(); }
        });
        portLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) { sendPort(); }
        });
        portLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(portLine);
        firstLine.add(new JLabel("  "));

        /* Key */
        JLabel keyLabel = GuiUtils.rLabel("Key: ");
//        firstLine.add(keyLabel);
        keyLine = new JTextField(mcidasxInfo.getKeyString(), 32);
        keyLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 sendKey();
            }
        });
        keyLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 sendKey();
            }
        });
        keyLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
//        firstLine.add(keyLine);
//        firstLine.add(new JLabel("  "));
        double[] nineWt = { 0, 0, 0, 0, 0, 0, 0, 0, 1 };
        JPanel firstPanel = GuiUtils.doLayout(firstLine, 9, nineWt,
                                              GuiUtils.WT_N);
        
        comps.add(new JLabel(" "));
        comps.add(firstPanel);
        comps.add(new JLabel(" "));
    }

    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to readFramesInner; in a try/catch block
     */
    protected void readFrames() {
    	clearFramesList();
    	if (!canReadFrames()) {
    		return;
    	}
    	Misc.run(new Runnable() {
    		public void run() {
    			updateStatus();
    			showWaitCursor();
    			try {
    				readFramesInner();
    			} catch (Exception e) {
    			}
    			showNormalCursor();
    			updateStatus();
    		}
    	});
    }

    /**
     * Set the list of dates/times based on the image selection
     *
     */
    protected void readFramesInner() {
    	loadFrames();
    }

    /**
     * Load the frames
     *
     */
    protected void loadFrames() {
    }

    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
    	return goodToGo;
    }

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    public List getFrameList() {
        List frames = new ArrayList();
        List xFrames = this.mcidasxInfo.getFrameNumbers();
        if (xFrames.size() < 1) return frames;
        for (int i = 0; i < xFrames.size(); i++) {
            Integer frmInt = (Integer)xFrames.get(i);
            McIdasFrame frame = new McIdasFrame(frmInt.intValue(), this.mcidasxInfo);
            frames.add(frame);
        }
        return frames;
    }

    /**
     * Method to do the work of loading the data
     */
    public void doLoad() {
        List frames = getFrameList();
        if (frames.size() < 1) {
            LogUtil.userMessage("Connection refused");
            return;
        }
        try {
           firePropertyChange(NEW_SELECTION, null, frames);
        } catch (Exception exc) {
           logException("doLoad", exc);
        }
    }
}
