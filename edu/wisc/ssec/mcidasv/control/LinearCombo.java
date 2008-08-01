package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.python.core.PyJavaInstance;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import visad.Data;
import visad.VisADException;
import visad.georef.MapProjection;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay.DragLine;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;

public class LinearCombo extends HydraControl implements ConsoleCallback {
    
    private static final String PARAM = "BrightnessTemp";
    
    private static final int DEFAULT_FLAGS =
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;
    
    private Console console;
    
    private MultiSpectralDisplay display;
    
    private DisplayMaster displayMaster;
    
    private String sourceFile = "";

    public LinearCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        sourceFile = ((MultiSpectralDataSource)sources.get(0)).getDatasetName();

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = MultiSpectralData.init_wavenumber;

        console = new Console();
        console.setCallbackHandler(this);

        console.injectObject("_linearCombo", new PyJavaInstance(this));
        console.injectObject("_jythonConsole", new PyJavaInstance(console));

        console.runFile("test", "/edu/wisc/ssec/mcidasv/resources/test.py");

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);

        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);

        addViewManager(display.getViewManager());

        setAttributeFlags(DEFAULT_FLAGS);

        return true;
    }

    @Override public void initDone() {
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(display.getImageData());
        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("LinearCombo.getDataProjection", e);
        }
        return mp;
    }

    @Override protected Range getInitialRange() throws VisADException, RemoteException {
        return getDisplayConventions().getParamRange(PARAM, null);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Display", GuiUtils.inset(getDisplayTab(), 5));
        return pane;
    }

    private JComponent getDisplayTab() {
        JPanel consolePanel = console.getPanel();
        consolePanel.setPreferredSize(new Dimension(500, 250));
        
        JPanel tmp = GuiUtils.centerBottom(display.getDisplayComponent(), consolePanel);
        return tmp;
    }

    @Override public void doRemove() throws VisADException, RemoteException {
        removeDisplayables();
    }

    @Override public String toString() {
        return "[LinearCombo@" + Integer.toHexString(hashCode()) + 
            ": sourceFile=" + sourceFile + "]";
    }

    public void updateSelector(final String id, final float channel) {
        if (!selectorMap.containsKey(id))
            return;

        selectorMap.get(id).setWaveNumber(channel);
    }

    protected void addSelector(final Selector selector) throws Exception {
        display.createSelector(selector.getId(), selector.getColor());
        display.setSelectorValue(selector.getId(), selector.getWaveNumber());
        selectorMap.put(selector.getId(), selector);
    }

    private Map<String, Selector> selectorMap = new HashMap<String, Selector>();

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    private Set<String> getSelectorIds() {
        Set<String> ids = new HashSet<String>();
        Collection<Object> jython = console.getJavaInstances().values();

        for (Iterator<Object> i = jython.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Selector))
                continue;

            String selectorId = ((Selector)obj).getId();
            ids.add(selectorId);
        }

        return ids;
    }

    public void ranBlock(final String line) {
        List<DragLine> dragLines = display.getSelectors();
        Set<String> ids = getSelectorIds();

        for (DragLine dragLine : dragLines) {
            String lineId = dragLine.getControlId();
            if (!ids.contains(lineId))
                display.removeSelector(lineId);
        }
    }

    public static abstract class JythonThing {
        public JythonThing() { }
        public abstract Data getData();
        public Combination __add__(final JythonThing other) throws VisADException, RemoteException {
            return new Combination(getData().add(other.getData()));
        }
        public Combination __sub__(final JythonThing other) throws VisADException, RemoteException {
            return new Combination(getData().subtract(other.getData()));
        }
        public Combination __mul__(final JythonThing other) throws VisADException, RemoteException {
            return new Combination(getData().multiply(other.getData()));
        }
        public Combination __div__(final JythonThing other) throws VisADException, RemoteException {
            return new Combination(getData().divide(other.getData()));
        }
    }

    public static class Selector extends JythonThing {
        private final String ID = hashCode() + "_jython";
        private float waveNumber = MultiSpectralData.init_wavenumber;
        private Color color = Color.RED;
        private Console console;
        private LinearCombo control;
        private Data data;

        public Selector(final float waveNumber, final Color color, final LinearCombo control, final Console console) {
            super();
            this.waveNumber = waveNumber;
            this.color = color;
            this.control = control;
            this.console = console;

            try {
                control.addSelector(this);
            } catch (Exception e) {
                System.err.println("Could not create selector: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void setWaveNumber(final float newChannel) {
            waveNumber = newChannel;
        }

        public float getWaveNumber() {
            return waveNumber;
        }

        public Color getColor() {
            return color;
        }

        public Data getData() {
            return control.getMultiSpectralDisplay().getImageDataFrom(waveNumber);
        }

        public String getId() {
            return ID;
        }

        @Override public String toString() {
            return "[Selector@" + Integer.toHexString(hashCode()) + 
                ": channel=" + waveNumber + ", color=" + color + "]";
        }
    }

    public static class Combination extends JythonThing {
        private String name;
        private Data data;

        public Combination(Data data) {
            this.data = data;
        }

        public Data getData() {
            return data;
        }
     }
}