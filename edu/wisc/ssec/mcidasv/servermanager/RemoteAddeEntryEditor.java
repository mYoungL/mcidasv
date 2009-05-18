package edu.wisc.ssec.mcidasv.servermanager;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.newLinkedHashSet;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.set;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JDialog;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.ServerPreferenceManager.AddeStatus;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry.EntryType;

/**
 * Mostly generated by NetBeans, and then hacked up a bit. This probably should
 * be a JDialog :(
 */
public class RemoteAddeEntryEditor extends javax.swing.JPanel {

    private static final String PREF_ENTERED_USER = "mcv.servers.defaultuser";
    private static final String PREF_ENTERED_PROJ = "mcv.servers.defaultproj";

    private static final String PREF_FORCE_CAPS = "mcv.servers.forcecaps";

    /** Background {@link Color} of an {@literal "invalid"} {@link javax.swing.JTextField}. */
    private static final Color ERROR_FIELD_COLOR = Color.PINK;

    /** Text {@link Color} of an {@literal "invalid"} {@link javax.swing.JTextField}. */
    private static final Color ERROR_TEXT_COLOR = Color.white;

    /** Background {@link Color} of a {@literal "valid"} {@link javax.swing.JTextField}. */
    private static final Color NORMAL_FIELD_COLOR = Color.WHITE;

    /** Text {@link Color} of a {@literal "valid"} {@link java.swing.JTextField}. */
    private static final Color NORMAL_TEXT_COLOR = Color.BLACK;

    /** Reference back to the container dialog. */
    private final JDialog dialog;

    private final RemoteAddeManager managerController;
    
    /** Reference back to the server manager. */
    private final EntryStore entryStore;

    /** Current contents of the editor. */
    private final Set<RemoteAddeEntry> currentEntries = newLinkedHashSet();

    /** 
     * Contains any {@code JTextField}s that may be in an invalid 
     * (to McIDAS-V) state. 
     */
    private final Set<javax.swing.JTextField> badFields = newLinkedHashSet();

    /** 
     * Creates a new entry editor without prepopulating any of the UI. This is
     * intended to signal that the user is creating an entirely new entry.
     * 
     * @param dialog Container dialog. Should not be {@code null}.
     * @param entryStore Should not be {@code null}.
     * 
     * @throws NullPointerException if either parameter is {@code null}.
     */
    public RemoteAddeEntryEditor(final JDialog dialog, final RemoteAddeManager managerController, final EntryStore entryStore) {
        if (dialog == null)
            throw new NullPointerException("Cannot provide a null container dialog");
        if (managerController == null)
            throw new NullPointerException("Null is bad");
        if (entryStore == null)
            throw new NullPointerException("Cannot provide a null server manager reference");

        this.dialog = dialog;
        this.managerController = managerController;
        this.entryStore = entryStore;
        initComponents();
    }

    /**
     * Creates a new entry editor, and prepopulates the UI components with the
     * contents of {@code editEntries}.
     * 
     * @param dialog Container dialog. Should not be {@code null}.
     * @param entryStore Should not be {@code null}.
     * @param editEntries Err.... But it shouldn't be {@code null}!!
     * 
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public RemoteAddeEntryEditor(final JDialog dialog, final RemoteAddeManager managerController, final EntryStore entryStore, final Set<RemoteAddeEntry> editEntries) {
        if (entryStore == null)
            throw new NullPointerException();
        if (managerController == null)
            throw new NullPointerException();
        if (editEntries == null)
            throw new NullPointerException();

        this.dialog = dialog;
        this.managerController = managerController;
        this.entryStore = entryStore;
        currentEntries.addAll(editEntries);
        initComponents();
        fillComponents();
    }

    /**
     * Populates the applicable components with values dictated by the entries
     * within {@link #currentEntries}. Primarily useful for editing entries.
     */
    private void fillComponents() {
        if (currentEntries.isEmpty())
            return;

        List<RemoteAddeEntry> entries = new ArrayList<RemoteAddeEntry>(currentEntries);
        RemoteAddeEntry entry = entries.get(0); // currently only allowing single selection. this'll have to change.
        serverField.setText(entry.getAddress());
        groupField.setText(entry.getGroup());

        if (entry.getAccount() != RemoteAddeEntry.DEFAULT_ACCOUNT) {
            acctBox.setSelected(true);
            userField.setText(entry.getAccount().getUsername());
            projField.setText(entry.getAccount().getProject());
        }

        // ugh
        if (entry.getEntryType() == EntryType.IMAGE)
            imageBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.POINT)
            pointBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.GRID)
            gridBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.TEXT)
            textBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.NAV)
            navBox.setSelected(true);
        else if (entry.getEntryType() == EntryType.RADAR)
            radarBox.setSelected(true);
    }

    /**
     * Poll the various UI components and attempt to construct valid ADDE 
     * entries based upon the information provided by the user.
     * 
     * @param ignoreCheckboxes Whether or not the {@literal "type"} checkboxes
     * should get ignored. Setting this to {@code true} means that <i>all</i>
     * types are considered valid--which is useful when attempting to verify
     * the user's input.
     * 
     * @return {@link Set} of entries that represent the user's input, or an
     * empty {@code Set} if the input was invalid somehow.
     */
    private Set<RemoteAddeEntry> pollWidgets(final boolean ignoreCheckboxes) {
        String host = serverField.getText().trim();
        String grp = groupField.getText().trim();
        String username = RemoteAddeEntry.DEFAULT_ACCOUNT.getUsername();
        String project = RemoteAddeEntry.DEFAULT_ACCOUNT.getProject();
        if (acctBox.isSelected()) {
            username = userField.getText().trim();
            project = projField.getText().trim();
        }

        // determine the "valid" types
        Set<EntryType> enabledTypes = newLinkedHashSet();
        if (!ignoreCheckboxes) {
            if (imageBox.isSelected())
                enabledTypes.add(EntryType.IMAGE);
            if (pointBox.isSelected())
                enabledTypes.add(EntryType.POINT);
            if (gridBox.isSelected())
                enabledTypes.add(EntryType.GRID);
            if (textBox.isSelected())
                enabledTypes.add(EntryType.TEXT);
            if (navBox.isSelected())
                enabledTypes.add(EntryType.NAV);
            if (radarBox.isSelected())
                enabledTypes.add(EntryType.RADAR);
        } else {
            enabledTypes.addAll(set(EntryType.IMAGE, EntryType.POINT, EntryType.GRID, EntryType.TEXT, EntryType.NAV, EntryType.RADAR));
        }

        if (enabledTypes.isEmpty())
            enabledTypes.add(EntryType.UNKNOWN);

        // deal with the user trying to add multiple groups at once (even though this UI doesn't work right with it)
        StringTokenizer tok = new StringTokenizer(grp, ",");
        Set<String> newGroups = newLinkedHashSet();
        while (tok.hasMoreTokens()) {
            newGroups.add(tok.nextToken().trim());
        }

        // create a new entry for each group and its valid types.
        Set<RemoteAddeEntry> entries = newLinkedHashSet();
        for (String newGroup : newGroups) {
            for (EntryType type : enabledTypes) {
                
                
                RemoteAddeEntry.Builder builder = new RemoteAddeEntry.Builder(host, newGroup).type(type);
                if (acctBox.isSelected()) {
                    builder = builder.account(username, project);
                }
//                if (!currentEntries.isEmpty()) {
//                    
//                }
                entries.add(builder.build());
            }
        }
        return entries;
    }

    /**
     * Attempts to verify that the current contents of the GUI are 
     * {@literal "valid"}.
     */
    private void verifyInput() {
        Set<RemoteAddeEntry> entries = pollWidgets(true);
        Set<EntryType> validTypes = newLinkedHashSet();
        for (RemoteAddeEntry entry : entries) {
            EntryType type = entry.getEntryType();
            if (validTypes.contains(type))
                continue;

            String server = entry.getAddress();
            String dataset = entry.getGroup();
            AddeStatus status = RemoteAddeVerification.checkEntry(entry);
            if (status == AddeStatus.OK) {
                setStatus("Verified that "+server+"/"+dataset+" has accessible "+type+" data.");
                validTypes.add(type);
            } else if (status == AddeStatus.BAD_SERVER) {
                setStatus("Could not connect to "+server);
                setBadField(serverField, true);
                return;
            } else if (status == AddeStatus.BAD_ACCOUNTING) {
                setStatus("Could not access "+server+"/"+dataset+" with current accounting information...");
                setBadField(userField, true);
                setBadField(projField, true);
                return;
            } else if (status == AddeStatus.BAD_GROUP) {
                // err...
            } else {
                setStatus("Unknown status returned: "+status);
                return;
            }
        }

        if (validTypes.isEmpty()) {
            setStatus("Could not verify any types of data...");
            setBadField(groupField, true);
        } else {
            setStatus("Server verification complete.");
            imageBox.setSelected(validTypes.contains(EntryType.IMAGE));
            pointBox.setSelected(validTypes.contains(EntryType.POINT));
            gridBox.setSelected(validTypes.contains(EntryType.GRID));
            textBox.setSelected(validTypes.contains(EntryType.TEXT));
            navBox.setSelected(validTypes.contains(EntryType.NAV));
            radarBox.setSelected(validTypes.contains(EntryType.RADAR));
        }
    }

    /**
     * Displays a short status message in {@link #statusLabel}.
     * 
     * @param msg Status message. Shouldn't be {@code null}.
     */
    private void setStatus(final String msg) {
        assert msg != null;
        statusLabel.setText(msg);
        statusLabel.revalidate();
    }

    /**
     * Marks a {@code JTextField} as {@literal "valid"} or {@literal "invalid"}.
     * Mostly this just means that the field is highlighted in order to provide
     * to the user a sense of {@literal "what do I fix"} when something goes
     * wrong.
     * 
     * @param field {@code JTextField} to mark.
     * @param isBad {@code true} means that the field is {@literal "invalid"},
     * {@code false} means that the field is {@literal "valid"}.
     */
    private void setBadField(javax.swing.JTextField field, final boolean isBad) {
        assert field != null;
        assert field == serverField || field == groupField || field == userField || field == projField;

        Color foreground = NORMAL_TEXT_COLOR;
        Color background = NORMAL_FIELD_COLOR;

        if (isBad) {
            foreground = ERROR_TEXT_COLOR;
            background = ERROR_FIELD_COLOR;
            badFields.add(field);
        } else {
            badFields.remove(field);
        }

        field.setForeground(foreground);
        field.setBackground(background);
        field.revalidate();
    }

    /**
     * Determines whether or not any fields are in an invalid state. Useful 
     * for disallowing the user to add invalid entries to the server manager.
     * 
     * @return Whether or not any fields are invalid.
     */
    private boolean anyBadFields() {
        assert badFields != null;
        return !badFields.isEmpty();
    }

    /**
     * Clear out {@link #badFields} and {@literal "set"} the field's status to
     * valid.
     */
    private void resetBadFields() {
        Set<javax.swing.JTextField> fields = new LinkedHashSet<javax.swing.JTextField>(badFields);
        for (javax.swing.JTextField field : fields)
            setBadField(field, false);
    }

    private void addEntry() {
        Set<RemoteAddeEntry> addedEntries = pollWidgets(false);
        entryStore.addEntries(currentEntries, addedEntries);
        dialog.dispose();
        managerController.refreshDisplay();
    }

    /** 
     * This method is called from within the constructor to initialize the 
     * form.
     */
    private void initComponents() {

        serverLabel.setText("Server:");

        serverField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverFieldActionPerformed(evt);
            }
        });

        groupLabel.setText("Dataset(s):");

        groupField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                groupFieldActionPerformed(evt);
            }
        });
        groupField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (!capBox.isSelected())
                    return;
                groupField.setText(groupField.getText().trim().toUpperCase());
            }
        });

        acctBox.setText("Specify accounting information:");
        acctBox.setSelected(false);
        acctBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acctBoxActionPerformed(evt);
            }
        });

        userLabel.setText("Username:");

        userField.setEnabled(acctBox.isSelected());
        userField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userFieldActionPerformed(evt);
            }
        });
        userField.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {}
            public void keyReleased(KeyEvent e) {
                if (!capBox.isSelected())
                    return;
                userField.setText(userField.getText().trim().toUpperCase());
            }
        });

        projLabel.setText("Project #:");

        projField.setEnabled(acctBox.isSelected());
        projField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                projFieldActionPerformed(evt);
            }
        });

        capBox.setText("Automatically capitalize datasets and username?");
        capBox.setSelected(getForceMcxCaps());
        capBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capBoxActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout entryPanelLayout = new org.jdesktop.layout.GroupLayout(entryPanel);
        entryPanel.setLayout(entryPanelLayout);
        entryPanelLayout.setHorizontalGroup(
            entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(entryPanelLayout.createSequentialGroup()
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(projLabel)
                    .add(userLabel)
                    .add(serverLabel)
                    .add(groupLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(capBox)
                    .add(acctBox)
                    .add(serverField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
                    .add(groupField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
                    .add(userField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE)
                    .add(projField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 445, Short.MAX_VALUE))
                .addContainerGap())
        );
        entryPanelLayout.setVerticalGroup(
            entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(entryPanelLayout.createSequentialGroup()
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(serverField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(serverLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(groupLabel)
                    .add(groupField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(acctBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(userField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(userLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(entryPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(projField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(projLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(capBox))
        );

        typePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Types"));

        imageBox.setText("Image");
        imageBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                imageBoxActionPerformed(evt);
            }
        });
        typePanel.add(imageBox);

        pointBox.setText("Point");
        typePanel.add(pointBox);

        gridBox.setText("Grid");
        typePanel.add(gridBox);

        textBox.setText("Text");
        typePanel.add(textBox);

        navBox.setText("Navigation");
        typePanel.add(navBox);

        radarBox.setText("Radar");
        typePanel.add(radarBox);

        verifyAndAddButton.setText("Verify and Add Server");
        verifyAndAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyAndAddButtonActionPerformed(evt);
            }
        });
        actionPanel.add(verifyAndAddButton);

        verifyButton.setText("Verify Server");
        verifyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verifyButtonActionPerformed(evt);
            }
        });
        actionPanel.add(verifyButton);

        addButton.setText("Add Server");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });
        actionPanel.add(addButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        actionPanel.add(cancelButton);

        statusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Status"));

        statusLabel.setBackground(new java.awt.Color(255, 255, 153));
        setStatus("Please provide the address of an ADDE server.");

        org.jdesktop.layout.GroupLayout statusPanelLayout = new org.jdesktop.layout.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(statusLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusPanelLayout.createSequentialGroup()
                .add(statusLabel)
                .addContainerGap(6, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(entryPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(layout.createSequentialGroup()
                        .add(typePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .add(statusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(actionPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(entryPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(typePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(statusPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(actionPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    private void serverFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void groupFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    /**
     * Handles the user selecting/deselecting {@link #acctBox}.
     * 
     * @param evt Event being handled. Not used at this time.
     */
    private void acctBoxActionPerformed(java.awt.event.ActionEvent evt) {
        boolean enabled = acctBox.isSelected();
        userField.setEnabled(enabled);
        projField.setEnabled(enabled);
    }

    private void capBoxActionPerformed(java.awt.event.ActionEvent evt) {
        if (capBox.isSelected()) {
            groupField.setText(groupField.getText().trim().toUpperCase());
            userField.setText(userField.getText().trim().toUpperCase());
        }

        setForceMcxCaps(capBox.isSelected());
    }

    private static void setForceMcxCaps(final boolean value) {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return;

        mcv.getStore().put(PREF_FORCE_CAPS, value);
    }

    private static boolean getForceMcxCaps() {
        McIDASV mcv = McIDASV.getStaticMcv();
        if (mcv == null)
            return false;

        return mcv.getStore().get(PREF_FORCE_CAPS, false);
    }
    
    private void imageBoxActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void verifyAndAddButtonActionPerformed(java.awt.event.ActionEvent evt) {
        verifyInput();
        if (!anyBadFields())
            addEntry();
    }

    private void verifyButtonActionPerformed(java.awt.event.ActionEvent evt) {
        verifyInput();
    }

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {
        addEntry();
    }

    /**
     * Handles the user clicking on the {@literal "Cancel"} button.
     * 
     * @param evt Event being handled. Not used at this time.
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        dialog.dispose();
    }

    private void userFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void projFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    // the widgets that've been placed in the form.
    private final javax.swing.JCheckBox acctBox = new javax.swing.JCheckBox();
    private final javax.swing.JPanel actionPanel = new javax.swing.JPanel();
    private final javax.swing.JButton addButton = new javax.swing.JButton();
    private final javax.swing.JButton cancelButton = new javax.swing.JButton();
    private final javax.swing.JCheckBox capBox = new javax.swing.JCheckBox();
    private final javax.swing.JPanel entryPanel = new javax.swing.JPanel();
    private final javax.swing.JCheckBox gridBox = new javax.swing.JCheckBox();
    private final javax.swing.JTextField groupField = new javax.swing.JTextField();
    private final javax.swing.JLabel groupLabel = new javax.swing.JLabel();
    private final javax.swing.JCheckBox imageBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox navBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox pointBox = new javax.swing.JCheckBox();
    private final javax.swing.JTextField projField = new javax.swing.JTextField();
    private final javax.swing.JLabel projLabel = new javax.swing.JLabel();
    private final javax.swing.JCheckBox radarBox = new javax.swing.JCheckBox();
    private final javax.swing.JTextField serverField = new javax.swing.JTextField();
    private final javax.swing.JLabel serverLabel = new javax.swing.JLabel();
    private final javax.swing.JLabel statusLabel = new javax.swing.JLabel();
    private final javax.swing.JPanel statusPanel = new javax.swing.JPanel();
    private final javax.swing.JCheckBox textBox = new javax.swing.JCheckBox();
    private final javax.swing.JPanel typePanel = new javax.swing.JPanel();
    private final javax.swing.JTextField userField = new javax.swing.JTextField();
    private final javax.swing.JLabel userLabel = new javax.swing.JLabel();
    private final javax.swing.JButton verifyAndAddButton = new javax.swing.JButton();
    private final javax.swing.JButton verifyButton = new javax.swing.JButton();
}
