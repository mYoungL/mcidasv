package edu.wisc.ssec.mcidasv.control;


/**
 * Holds a set of definitions concerning McIDAS data.
 */
public interface McIdasComponents {

    /** Image frame component */
    public static final String IMAGE = "McIdasComponents.image";

    /** Graphics frame component */
    public static final String GRAPHICS = "McIdasComponents.graphics";

    /** Color Table frame component */
    public static final String COLORTABLE = "McIdasComponents.colortable";
    
    /** Annotation frame component */
    public static final String ANNOTATION = "McIdasComponents.annotation";
    
    /** Annotation frame component */
    public static final String FAKEDATETIME = "McIdasComponents.fakedatetime";
    
    /** Array of dirty frame info */
    public static final String DIRTYINFO = "McIdasComponents.dirtyinfo";
    
    /** Image dirty component */
    public static final String DIRTYIMAGE = "McIdasComponents.dirtyimage";

    /** Graphics dirty component */
    public static final String DIRTYGRAPHICS = "McIdasComponents.dirtygraphics";

    /** Color Table dirty component */
    public static final String DIRTYCOLORTABLE = "McIdasComponents.dirtycolortable";

}
