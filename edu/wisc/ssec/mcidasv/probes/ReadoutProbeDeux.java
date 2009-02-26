package edu.wisc.ssec.mcidasv.probes;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import ucar.unidata.collab.SharableImpl;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.ShapeUtility;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.TextDisplayable;

import visad.Data;
import visad.FlatField;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.georef.EarthLocationTuple;

public class ReadoutProbeDeux extends SharableImpl implements PropertyChangeListener {

    public static final String SHARE_PROFILE = "ReadoutProbeDeux.SHARE_PROFILE";

    public static final String SHARE_POSITION = "ReadoutProbeDeux.SHARE_POSITION";

    private static final Color DEFAULT_COLOR = Color.MAGENTA;

    private static final TupleType TUPTYPE = makeTupleType();

    private final CopyOnWriteArrayList<ProbeListener> listeners = 
        new CopyOnWriteArrayList<ProbeListener>();

    /** Displays the value of the data at the current position. */
    private final TextDisplayable valueDisplay = createValueDisplay(DEFAULT_COLOR);

    private final LineProbe probe = new LineProbe(getInitialLinePosition());

    private final DisplayMaster master;

    private Color currentColor = DEFAULT_COLOR;

//    private float currentValue = Float.NaN;
    private String currentValue = "NaN";

    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;

//    private RealTuple initialPosition;

//    private String marker;

    private float pointSize = 1.0f;

//    private String positionText;

    private FlatField field;

    // TODO(jon): implement these in place of Grid2DReadoutProbe's isLonLat.
//    public enum PositionFormat { LON_LAT, LAT_LON };
//    private PositionFormat positionFormat = PositionFormat.LON_LAT;

//    private final DataReference positionRef;

    private static final DecimalFormat numFmt = new DecimalFormat();

    public ReadoutProbeDeux(final DisplayMaster master, final FlatField field) throws VisADException, RemoteException {
        super();
        if (master == null)
            throw new NullPointerException("Non-null DisplayMaster required");
        if (field == null)
            throw new NullPointerException("Non-null field required");

//        positionRef = new DataReferenceImpl(hashCode() + "_positionRef");
//        positionRef.setData(new LatLonTuple());

        this.master = master;
        this.field = field;

        master.addDisplayable(valueDisplay);
//        master.getDisplay().addDisplayListener(new DisplayListener() {
//            public void displayChanged(DisplayEvent de) {
//                if (de.getId() == DisplayEvent.MOUSE_RELEASED) {
//                    try {
//                        doShare(SHARE_POSITION, getPosition());
//                    } catch (Exception e) {
//                        System.err.println("displayChanged: "+e);
//                    }
//                }
//            }
//        });

        initSharable();

        master.addDisplayable(probe);

        probe.setColor(DEFAULT_COLOR);
        probe.setVisible(true);
        probe.setPointSize(pointSize);
        probe.setAutoSize(true);
        probe.addPropertyChangeListener(this);
        probe.setPointSize(getDisplayScale());
//        probe.setMarker(ShapeUtility.createShape(ShapeUtility.AIRPLANE3D)[0]);

        numFmt.setMaximumFractionDigits(2);
    }

    /**
     * Called whenever the probe fires off a {@link PropertyChangeEvent}. Only
     * handles position changes right now, all other events are discarded.
     *
     * @param e Object that describes the property change.
     * 
     * @throws NullPointerException if passed a {@code null} 
     * {@code PropertyChangeEvent}.
     */
    public void propertyChange(final PropertyChangeEvent e) {
        if (e == null)
            throw new NullPointerException("Cannot handle a null property change event");

        if (e.getPropertyName().equals(SelectorDisplayable.PROPERTY_POSITION)) {
            RealTuple prev = getEarthPosition();
            handleProbeUpdate();
            RealTuple current = getEarthPosition();
            fireProbePositionChanged(prev, current);
        }
    }

    public void setField(final FlatField field) {
        if (field == null)
            throw new NullPointerException("");

        this.field = field;
        handleProbeUpdate();
    }

    /**
     * Adds a {@link ProbeListener} to the listener list so that it can be
     * notified when the probe is changed.
     * 
     * @param listener {@code ProbeListener} to register. {@code null} 
     * listeners are not allowed.
     * 
     * @throws NullPointerException if {@code listener} is null.
     */
    public void addProbeListener(final ProbeListener listener) {
        if (listener == null)
            throw new NullPointerException("Cannot add a null listener");
        listeners.add(listener);
    }

    /**
     * Removes a {@link ProbeListener} from the notification list.
     * 
     * @param listener {@code ProbeListener} to remove. {@code null} values
     * are permitted, but since they are not allowed to be added...
     */
    public void removeProbeListener(final ProbeListener listener) {
        listeners.remove(listener);
    }

    public boolean hasListener(final ProbeListener listener) {
        return listeners.contains(listener);
    }

    /**
     * Notifies the registered {@link ProbeListener}s that this probe's 
     * position has changed.
     * 
     * @param previous Previous position.
     * @param current Current position.
     */
    protected void fireProbePositionChanged(final RealTuple previous, final RealTuple current) {
        if (previous == null)
            throw new NullPointerException();
        if (current == null)
            throw new NullPointerException();

        ProbeEvent<RealTuple> event = new ProbeEvent<RealTuple>(this, previous, current);
        for (ProbeListener listener : listeners)
            listener.probePositionChanged(event);
    }

    /**
     * Notifies the registered {@link ProbeListener}s that this probe's color
     * has changed.
     * 
     * @param previous Previous color.
     * @param current Current color.
     */
    protected void fireProbeColorChanged(final Color previous, final Color current) {
        if (previous == null)
            throw new NullPointerException();
        if (current == null)
            throw new NullPointerException();

        ProbeEvent<Color> event = new ProbeEvent<Color>(this, previous, current);
        for (ProbeListener listener : listeners)
            listener.probeColorChanged(event);
    }

    /**
     * Notifies registered {@link ProbeListener}s that this probe's visibility
     * has changed. Only takes a {@literal "previous"} value, which is negated
     * to form the {@literal "current"} value.
     * 
     * @param previous Visibility <b>before</b> change.
     */
    protected void fireProbeVisibilityChanged(final boolean previous) {
        ProbeEvent<Boolean> event = new ProbeEvent<Boolean>(this, previous, !previous);
        for (ProbeListener listener : listeners)
            listener.probeVisibilityChanged(event);
    }

    public void setColor(final Color color) {
        if (color == null)
            throw new NullPointerException("Cannot provide a null color");

        if (currentColor.equals(color))
            return;

        try {
            probe.setColor(color);
            valueDisplay.setColor(color);
            Color prev = currentColor;
            currentColor = color;
            fireProbeColorChanged(prev, currentColor);
        } catch (Exception e) {
            LogUtil.logException("Couldn't set the color of the probe", e);
        }
    }

    public Color getColor() {
        return currentColor;
    }

//    public float getValue() {
//        return currentValue;
//    }
    public String getValue() {
        return currentValue;
    }

    public double getLatitude() {
        return currentLatitude;
    }

    public double getLongitude() {
        return currentLongitude;
    }

    public void handleProbeUpdate() {
        RealTuple pos = getEarthPosition();
        if (pos == null)
            return;

        Tuple positionValue = valueAtPosition(pos, field);
        if (positionValue == null)
            return;

        try {
            valueDisplay.setData(positionValue);
        } catch (Exception e) {
            LogUtil.logException("Failed to set readout value", e);
        }
    }

    public void handleProbeRemoval() {
        listeners.clear();
        try {
            master.removeDisplayable(valueDisplay);
            master.removeDisplayable(probe);
        } catch (Exception e) {
            LogUtil.logException("Problem removing visible portions of readout probe", e);
        }
        currentColor = null;
        field = null;
    }

    /**
     * Get the scaling factor for probes and such. The scaling is
     * the parameter that gets passed to TextControl.setSize() and
     * ShapeControl.setScale().
     * 
     * @return ratio of the current matrix scale factor to the
     * saved matrix scale factor.
     */
    // why not return 1.0f all the time?
    public float getDisplayScale() {
        float scale = 1.0f;
        try {
            scale = master.getDisplayScale();
        } catch (Exception e) {
            System.err.println("Error getting display scale: "+e);
        }
        return scale;
    }

    public void setXYPosition(final RealTuple position) {
        if (position == null)
            throw new NullPointerException("cannot use a null position");

        try {
            probe.setPosition(position);
        } catch (Exception e) {
            LogUtil.logException("Had problems setting probe's xy position", e);
        }
    }

    public RealTuple getXYPosition() {
        RealTuple position = null;
        try {
            position = probe.getPosition();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's xy location", e);
        }
        return position;
    }

    public EarthLocationTuple getEarthPosition() {
        EarthLocationTuple earthTuple = null;
        try {
            double[] values = probe.getPosition().getValues();
            earthTuple = (EarthLocationTuple)((NavigatedDisplay)master).getEarthLocation(values[0], values[1], 1.0, true);
            currentLatitude = earthTuple.getLatitude().getValue();
            currentLongitude = earthTuple.getLongitude().getValue();
        } catch (Exception e) {
            LogUtil.logException("Could not determine the probe's earth location", e);
        }
        return earthTuple;
    }

    private Tuple valueAtPosition(final RealTuple position, final FlatField imageData) {
        assert position != null : "Cannot provide a null position";
        assert imageData != null : "Cannot provide a null image";

        double[] values = position.getValues();
        if (values[1] < -180)
            values[1] += 360f;

        if (values[0] > 180)
            values[0] -= 360f;

        Tuple positionTuple = null;
        try {
            // TODO(jon): do the positionFormat stuff in here. maybe this'll 
            // have to be an instance method?
            RealTuple corrected = new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] { values[1], values[0] });

            Real realVal = (Real)imageData.evaluate(corrected, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
//            float val = (float)realVal.getValue();
//            positionTuple = new Tuple(TUPTYPE, new Data[] { corrected, new Text(TextType.Generic, numFmt.format(val)) });
//            currentValue = val;
            currentValue = numFmt.format(realVal.getValue());
            positionTuple = new Tuple(TUPTYPE, new Data[] { corrected, new Text(TextType.Generic, currentValue) });
        } catch (Exception e) {
            LogUtil.logException("Encountered trouble when determining value at probe position", e);
        }
        return positionTuple;
    }

    private static RealTuple getInitialLinePosition() {
        RealTuple position = null;
        try {
            double[] center = new double[] { 0.0, 0.0 };
            position = new RealTuple(RealTupleType.SpatialCartesian2DTuple, 
                    new double[] { center[0], center[1] });
        } catch (Exception e) {
            LogUtil.logException("Problem with finding an initial probe position", e);
        }
        return position;
    }

    private static TextDisplayable createValueDisplay(final Color color) {
        assert color != null;

        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        TextDisplayable td = null;
        try {
            td = new TextDisplayable(TextType.Generic);
            td.setLineWidth(2f);
            td.setColor(color);
            td.setNumberFormat(fmt);
        } catch (Exception e) {
            LogUtil.logException("Problem creating readout value container", e);
        }
        return td;
    }

    private static TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] { RealTupleType.SpatialEarth2DTuple, TextType.Generic });
        } catch (Exception e) {
            LogUtil.logException("Problem creating readout tuple type", e);
        }
        return t;
    }
}
