package edu.wisc.ssec.mcidasv;

/**
 * Application wide constants.
 * @version $Id$
 */
public interface Constants {

	/**
	 * The name of a thing that contains the data choosers and
	 * field selector
	 */
	public static final String DATASELECTOR_NAME = "Data Selector";
	
	/**
	 * A thing that contains one or more of the things named
	 * <tt>PANEL_NAME</tt>. One of these can be either in a tab
	 * or in it's own window.
	 */
	public static final String DISPLAY_NAME = "Display";
	
	/**
	 * The name of a thing that contains the display/layer controls
	 */
	public static final String DISPLAYCONTROLLER_NAME = "Display Controller";
	/** Macro for the build date. */
	public static String MACRO_BUILDDATE = "%BUILDDATE%";
	/** Macro for the copyright year in the about HTML file. */
	public static String MACRO_COPYRIGHT_YEAR = "%COPYRIGHT_YEAR%";
	/** Macro for the IDV version in the about HTML file. */
	public static String MACRO_IDV_VERSION = "%IDVVERSION%";
	/** Macro for the version in the about HTML file. */
	public static String MACRO_VERSION = "%MCVERSION%";
	/** 
	 * Java OS descriptor for the Max OSX operating system. This should be 
	 * constant for any machine running java on OSX.
	 */
	public static final String OS_OSX = "Mac OS X";
	/**
	 * The name of thing that contains the actual VisAD display,
	 * the animation control, view and projection menus, and the
	 * toolbar.
	 */
	public static final String PANEL_NAME = "Panel";
	
	/** Gail's server preference manager. */
	public static final String PREF_LIST_ADDE_SERVERS = "ADDE Servers";
	
	/** Prefs for which display types to allow. */
	public static final String PREF_LIST_AVAILABLE_DISPLAYS = "Available Displays";
	
	/** Prefs for which data choosers should show up. */
	public static final String PREF_LIST_DATA_CHOOSERS = "Data Sources";
	
	/** Name of panel containing prefs related to data formatting. */
	public static final String PREF_LIST_FORMATS_DATA = "Formats & Data";
	
	/** Name of the panel that holds the "general" sorts of user prefs. */
	public static final String PREF_LIST_GENERAL = "General";
	
	/** Panel name for the different nav control scheme prefs. */
	public static final String PREF_LIST_NAV_CONTROLS = "Navigation Controls";
	
	/** Prefs for configuring what appears in the toolbar. */
	public static final String PREF_LIST_TOOLBAR = "Toolbar Options";
	
	/** Name of the different prefs for configuring how tabs/windows look. */
	public static final String PREF_LIST_VIEW = "Display Window";
	
	/** Enabled the toolbar manipulation JPopupMenu. */
	public static final String PREF_TBM_ENABLED ="tbm.enabled";
	
	/** Whether or not icons should be shown in the toolbar. */
	public static final String PREF_TBM_ICONS = "tbm.icon.enabled";

	/** Whether or not labels should be shown in the toolbar. */
	public static final String PREF_TBM_LABELS = "tbm.label.enabled";
	
	/** The toolbar display option that was chosen. */
	public static final String PREF_TBM_SELOPT = "tbm.bg.selected";
	
	/** 
	 * Show large or small icons. If PREF_TBM_ICONS is disabled, this pref
	 * has no meaning.
	 */
	public static final String PREF_TBM_SIZE = "tbm.icon.size";
	
	/** Property name for for the path to about dialog template. */
	public static String PROP_ABOUTTEXT = "mcidasv.about.text";
	
	public static String PROP_BUILD_DATE = "mcidasv.build.date";
	
	/** Property name for the copyright year. */
	public static String PROP_COPYRIGHT_YEAR = "mcidasv.copyright.year";
	
	/** Property name for the McIdas-V homepage URL. */
	public static String PROP_HOMEPAGE = "mcidasv.homepage";
	
	/** Specifies use of {@link edu.wisc.ssec.mcidasv.ui.TabbedUIManager}. */
	public static final String PROP_TABBED_UI = "mcidasv.tabbedDisplay";
	
	/** The location of the actions file. */
	public static final String PROP_TBM_ACTIONS = "mcidasv.apptoolbar.actions";
	
	/** The location of the file with the large icon actions. */
	public static final String PROP_TBM_LARGE = 
		"mcidasv.apptoolbar.largeactions";
	
	/** The location of the file with the small icon actions. */
	public static final String PROP_TBM_SMALL = 
		"mcidasv.apptoolbar.smallactions";
	
	/** Property name for the major version number. */
	public static String PROP_VERSION_MAJOR = "mcidasv.version.major";
	
	/** Property name for the minor version number. */
	public static String PROP_VERSION_MINOR = "mcidasv.version.minor";
	
	/** Property name for the version release number. */
	public static String PROP_VERSION_RELEASE = "mcidasv.version.release";
	
	/** Property name for the path to version file. */
	public static String PROP_VERSIONFILE = "mcidasv.version.file";
	
	/** Application property file name. */
	public static final String PROPERTIES_FILE = 
		"/edu/wisc/ssec/mcidasv/resources/mcidasv.properties";
}
