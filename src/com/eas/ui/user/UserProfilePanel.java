package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.service.AuthService;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class UserProfilePanel extends JPanel {

    private final UserSession session;

    private final JTextField full = new JTextField(22);
    private final JTextField email = new JTextField(22);
    private final JTextField phone = new JTextField(15);
    private final JTextField address = new JTextField(30);
    private final JTextField emergency = new JTextField(25);
    private final JTextField allergy = new JTextField(30);
    private final JTextField existMedCon = new JTextField(30);

    private final JPasswordField oldPassword = new JPasswordField(15);
    private final JPasswordField newPassword = new JPasswordField(15);

    public UserProfilePanel(UserSession session) {
        this.session = session;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        add(
            UITheme.title("My Profile and Password"),
            BorderLayout.NORTH
        );

        /*
         * Employee details
         */
        JPanel employeePanel = new JPanel(
            new GridLayout(0, 2, 8, 8)
        );

        employeePanel.setBorder(
            BorderFactory.createTitledBorder("Employee Details")
        );

        addField(employeePanel, "Full Name", full);
        addField(employeePanel, "Email", email);
        addField(employeePanel, "Phone", phone);
        addField(employeePanel, "Address", address);
        addField(
            employeePanel,
            "Emergency Contact",
            emergency
        );

        JButton saveProfileButton =
            UITheme.button("SAVE PROFILE");

        employeePanel.add(saveProfileButton);
        employeePanel.add(new JLabel(""));

        /*
         * Change password
         */
        JPanel passwordPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 8)
        );

        passwordPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Change Password"
            )
        );

        passwordPanel.add(new JLabel("Current"));
        passwordPanel.add(oldPassword);
        passwordPanel.add(new JLabel("New"));
        passwordPanel.add(newPassword);

        JButton changePasswordButton =
            UITheme.button("CHANGE PASSWORD");

        passwordPanel.add(changePasswordButton);

        /*
         * Employee medical details
         */
        JLabel certificateLabel =
            new JLabel("No file selected");

        JLabel injuryReportLabel =
            new JLabel("No file selected");

        JLabel healthDeclarationLabel =
            new JLabel("No file selected");

        JLabel wellnessActivityLabel =
            new JLabel("No file selected");

        JButton certificateButton =
            UITheme.button("Attach");

        JButton injuryReportButton =
            UITheme.button("Attach");

        JButton healthDeclarationButton =
            UITheme.button("Attach");

        JButton wellnessActivityButton =
            UITheme.button("Attach");

        certificateButton.addActionListener(
            event -> attachFile(certificateLabel)
        );

        injuryReportButton.addActionListener(
            event -> attachFile(injuryReportLabel)
        );

        healthDeclarationButton.addActionListener(
            event -> attachFile(healthDeclarationLabel)
        );

        wellnessActivityButton.addActionListener(
            event -> attachFile(wellnessActivityLabel)
        );

        String[] bloodTypeOptions = {
            "-Select-",
            "A+",
            "A-",
            "AB+",
            "AB-",
            "B+",
            "B-",
            "O+",
            "O-"
        };

        JComboBox<String> bloodTypeDropdown =
            new JComboBox<>(bloodTypeOptions);

        JTextArea medicalNotesArea = new JTextArea();

        medicalNotesArea.setLineWrap(true);
        medicalNotesArea.setWrapStyleWord(true);

        JPanel medicalPanel =
            new JPanel(new GridBagLayout());

        medicalPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Employee Medical Details"
            )
        );

        GridBagConstraints medicalConstraints =
            new GridBagConstraints();

        medicalConstraints.insets =
            new Insets(6, 8, 6, 8);

        medicalConstraints.fill =
            GridBagConstraints.HORIZONTAL;

        medicalConstraints.anchor =
            GridBagConstraints.WEST;

        int medicalRow = 0;

        /*
         * Blood type
         */
        medicalConstraints.gridx = 0;
        medicalConstraints.gridy = medicalRow;
        medicalConstraints.weightx = 0;

        medicalPanel.add(
            new JLabel("Blood Type"),
            medicalConstraints
        );

        medicalConstraints.gridx = 1;
        medicalConstraints.weightx = 1;

        medicalPanel.add(
            bloodTypeDropdown,
            medicalConstraints
        );

        medicalConstraints.gridx = 2;
        medicalConstraints.weightx = 0;

        medicalPanel.add(
            new JLabel(""),
            medicalConstraints
        );

        medicalRow++;

        /*
         * Allergy
         */
        medicalConstraints.gridx = 0;
        medicalConstraints.gridy = medicalRow;
        medicalConstraints.weightx = 0;

        medicalPanel.add(
            new JLabel("Allergy"),
            medicalConstraints
        );

        medicalConstraints.gridx = 1;
        medicalConstraints.weightx = 1;
        medicalConstraints.gridwidth = 2;

        medicalPanel.add(
            allergy,
            medicalConstraints
        );

        medicalConstraints.gridwidth = 1;
        medicalRow++;

        /*
         * Existing medical condition
         */
        medicalConstraints.gridx = 0;
        medicalConstraints.gridy = medicalRow;
        medicalConstraints.weightx = 0;

        medicalPanel.add(
            new JLabel("Existing Medical Condition"),
            medicalConstraints
        );

        medicalConstraints.gridx = 1;
        medicalConstraints.weightx = 1;
        medicalConstraints.gridwidth = 2;

        medicalPanel.add(
            existMedCon,
            medicalConstraints
        );

        medicalConstraints.gridwidth = 1;
        medicalRow++;

        /*
         * Emergency medical notes
         */
        medicalConstraints.gridx = 0;
        medicalConstraints.gridy = medicalRow;
        medicalConstraints.weightx = 0;
        medicalConstraints.anchor =
            GridBagConstraints.NORTHWEST;

        medicalPanel.add(
            new JLabel("Emergency Medical Notes"),
            medicalConstraints
        );

        medicalConstraints.gridx = 1;
        medicalConstraints.weightx = 1;
        medicalConstraints.gridwidth = 2;
        medicalConstraints.ipady = 60;

        medicalPanel.add(
            new JScrollPane(medicalNotesArea),
            medicalConstraints
        );

        medicalConstraints.ipady = 0;
        medicalConstraints.gridwidth = 1;
        medicalConstraints.anchor =
            GridBagConstraints.WEST;

        medicalRow++;

        /*
         * Medical document attachments
         */
        String[] documentLabels = {
            "Medical Certificate",
            "Workplace Injury Report",
            "Health Declaration",
            "Wellness Activity"
        };

        JLabel[] fileLabels = {
            certificateLabel,
            injuryReportLabel,
            healthDeclarationLabel,
            wellnessActivityLabel
        };

        JButton[] attachmentButtons = {
            certificateButton,
            injuryReportButton,
            healthDeclarationButton,
            wellnessActivityButton
        };

        for (int i = 0; i < documentLabels.length; i++) {
            medicalConstraints.gridx = 0;
            medicalConstraints.gridy = medicalRow;
            medicalConstraints.weightx = 0;

            medicalPanel.add(
                new JLabel(documentLabels[i]),
                medicalConstraints
            );

            medicalConstraints.gridx = 1;
            medicalConstraints.weightx = 1;

            medicalPanel.add(
                fileLabels[i],
                medicalConstraints
            );

            medicalConstraints.gridx = 2;
            medicalConstraints.weightx = 0;

            medicalPanel.add(
                attachmentButtons[i],
                medicalConstraints
            );

            medicalRow++;
        }

        JButton saveMedicalButton =
            UITheme.button("SAVE MEDICAL DETAILS");

        medicalConstraints.gridx = 0;
        medicalConstraints.gridy = medicalRow;
        medicalConstraints.gridwidth = 3;
        medicalConstraints.anchor =
            GridBagConstraints.CENTER;

        medicalConstraints.fill =
            GridBagConstraints.NONE;

        medicalPanel.add(
            saveMedicalButton,
            medicalConstraints
        );

        medicalConstraints.gridwidth = 1;
        medicalConstraints.fill =
            GridBagConstraints.HORIZONTAL;

        medicalConstraints.anchor =
            GridBagConstraints.WEST;

        /*
         * Occupational safety checklist
         */
        JPanel checklistPanel =
            new JPanel(new GridBagLayout());

        checklistPanel.setBorder(
            BorderFactory.createTitledBorder(
                "Occupational Safety Checklist"
            )
        );

        GridBagConstraints checklistConstraints =
            new GridBagConstraints();

        checklistConstraints.insets =
            new Insets(6, 8, 6, 8);

        checklistConstraints.fill =
            GridBagConstraints.HORIZONTAL;

        checklistConstraints.anchor =
            GridBagConstraints.WEST;

        int checklistRow = 0;

        JLabel departmentLabel =
            new JLabel("Select Department:");

        String[] departmentOptions = {
            "-Select-",
            "Office",
            "Laboratory",
            "Warehouse"
        };

        JComboBox<String> departmentDropdown =
            new JComboBox<>(departmentOptions);

        checklistConstraints.gridx = 0;
        checklistConstraints.gridy = checklistRow;
        checklistConstraints.weightx = 0;

        checklistPanel.add(
            departmentLabel,
            checklistConstraints
        );

        checklistConstraints.gridx = 1;
        checklistConstraints.weightx = 1;
        checklistConstraints.gridwidth = 2;

        checklistPanel.add(
            departmentDropdown,
            checklistConstraints
        );

        checklistConstraints.gridwidth = 1;
        checklistRow++;

        JPanel checklistItems =
            new JPanel(new GridBagLayout());

        checklistConstraints.gridx = 0;
        checklistConstraints.gridy = checklistRow;
        checklistConstraints.gridwidth = 3;
        checklistConstraints.weightx = 1;

        checklistPanel.add(
            checklistItems,
            checklistConstraints
        );

        checklistConstraints.gridwidth = 1;
        checklistRow++;

        JLabel checklistStatus = new JLabel(" ");

        checklistStatus.setFont(
            new Font(
                "SansSerif",
                Font.ITALIC,
                11
            )
        );

        checklistStatus.setForeground(Color.GRAY);

        checklistConstraints.gridx = 0;
        checklistConstraints.gridy = checklistRow;
        checklistConstraints.gridwidth = 3;

        checklistPanel.add(
            checklistStatus,
            checklistConstraints
        );

        checklistConstraints.gridwidth = 1;
        checklistRow++;

        JButton saveChecklistButton =
            UITheme.button("SAVE CHECKLIST");

        checklistConstraints.gridx = 0;
        checklistConstraints.gridy = checklistRow;
        checklistConstraints.gridwidth = 3;
        checklistConstraints.anchor =
            GridBagConstraints.CENTER;

        checklistConstraints.fill =
            GridBagConstraints.NONE;

        checklistPanel.add(
            saveChecklistButton,
            checklistConstraints
        );

        checklistConstraints.gridwidth = 1;
        checklistConstraints.fill =
            GridBagConstraints.HORIZONTAL;

        checklistConstraints.anchor =
            GridBagConstraints.WEST;

        departmentDropdown.addActionListener(event -> {
            checklistItems.removeAll();
            checklistStatus.setText(" ");

            String selectedDepartment =
                (String) departmentDropdown.getSelectedItem();

            buildChecklistItems(
                checklistItems,
                selectedDepartment
            );

            loadChecklistState(
                checklistItems,
                selectedDepartment
            );

            checklistItems.revalidate();
            checklistItems.repaint();
        });

        saveChecklistButton.addActionListener(event -> {
            String selectedDepartment =
                (String) departmentDropdown.getSelectedItem();

            if (
                selectedDepartment == null
                || "-Select-".equals(selectedDepartment)
            ) {
                Dialogs.error(
                    this,
                    "Please select a department before saving "
                    + "the checklist."
                );

                return;
            }

            saveChecklistState(
                checklistItems,
                selectedDepartment,
                checklistStatus
            );
        });

        /*
         * Assemble the panel
         */
        JPanel medicalAndChecklistPanel =
            new JPanel(new BorderLayout(0, 10));

        medicalAndChecklistPanel.add(
            medicalPanel,
            BorderLayout.NORTH
        );

        medicalAndChecklistPanel.add(
            checklistPanel,
            BorderLayout.CENTER
        );

        JPanel centerPanel =
            new JPanel(new BorderLayout(0, 10));

        centerPanel.add(
            employeePanel,
            BorderLayout.NORTH
        );

        centerPanel.add(
            medicalAndChecklistPanel,
            BorderLayout.CENTER
        );

        centerPanel.add(
            passwordPanel,
            BorderLayout.SOUTH
        );

        JScrollPane scrollPane =
            new JScrollPane(centerPanel);

        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane
            .getVerticalScrollBar()
            .setUnitIncrement(16);

        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        /*
         * Button actions
         */
        saveProfileButton.addActionListener(
            event -> saveProfile()
        );

        saveMedicalButton.addActionListener(
            event -> saveMedical(
                bloodTypeDropdown,
                medicalNotesArea,
                certificateLabel,
                injuryReportLabel,
                healthDeclarationLabel,
                wellnessActivityLabel
            )
        );

        changePasswordButton.addActionListener(
            event -> changePassword()
        );

        /*
         * Initial data loading
         */
        loadProfile();

        loadMedical(
            bloodTypeDropdown,
            medicalNotesArea,
            certificateLabel,
            injuryReportLabel,
            healthDeclarationLabel,
            wellnessActivityLabel
        );
    }

    private void addField(
            JPanel panel,
            String label,
            JTextField field
    ) {
        panel.add(new JLabel(label));
        panel.add(field);
    }

    private void attachFile(JLabel label) {
        JFileChooser chooser = new JFileChooser();

        chooser.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter(
                "Documents (*.pdf, *.jpg, *.png)",
                "pdf",
                "jpg",
                "jpeg",
                "png"
            )
        );

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            label.setText(file.getName());
        }
    }

    private void saveMedical(
            JComboBox<String> bloodTypeDropdown,
            JTextArea medicalNotesArea,
            JLabel certificateLabel,
            JLabel injuryReportLabel,
            JLabel healthDeclarationLabel,
            JLabel wellnessActivityLabel
    ) {
        String bloodType =
            (String) bloodTypeDropdown.getSelectedItem();

        if ("-Select-".equals(bloodType)) {
            bloodType = null;
        }

        String sql =
            "INSERT INTO employee_medical_details " +
            "(employee_id, blood_type, allergy, " +
            " existing_condition, emergency_notes, " +
            " medical_certificate, workplace_injury_report, " +
            " health_declaration, wellness_activity) " +
            "VALUES (" +
            " (SELECT id FROM employees WHERE user_id = ?), " +
            " ?, ?, ?, ?, ?, ?, ?, ?" +
            ") " +
            "ON DUPLICATE KEY UPDATE " +
            "blood_type = VALUES(blood_type), " +
            "allergy = VALUES(allergy), " +
            "existing_condition = VALUES(existing_condition), " +
            "emergency_notes = VALUES(emergency_notes), " +
            "medical_certificate = " +
            "VALUES(medical_certificate), " +
            "workplace_injury_report = " +
            "VALUES(workplace_injury_report), " +
            "health_declaration = " +
            "VALUES(health_declaration), " +
            "wellness_activity = VALUES(wellness_activity)";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                session.getId()
            );

            statement.setString(
                2,
                bloodType
            );

            statement.setString(
                3,
                allergy.getText().trim()
            );

            statement.setString(
                4,
                existMedCon.getText().trim()
            );

            statement.setString(
                5,
                medicalNotesArea.getText().trim()
            );

            statement.setString(
                6,
                toPath(certificateLabel)
            );

            statement.setString(
                7,
                toPath(injuryReportLabel)
            );

            statement.setString(
                8,
                toPath(healthDeclarationLabel)
            );

            statement.setString(
                9,
                toPath(wellnessActivityLabel)
            );

            statement.executeUpdate();

            AuditService.log(
                session.getId(),
                "UPDATE",
                "MEDICAL",
                "Updated employee medical details."
            );

            Dialogs.info(
                this,
                "Medical details saved."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void loadMedical(
            JComboBox<String> bloodTypeDropdown,
            JTextArea medicalNotesArea,
            JLabel certificateLabel,
            JLabel injuryReportLabel,
            JLabel healthDeclarationLabel,
            JLabel wellnessActivityLabel
    ) {
        String sql =
            "SELECT " +
            "m.blood_type, " +
            "m.allergy, " +
            "m.existing_condition, " +
            "m.emergency_notes, " +
            "m.medical_certificate, " +
            "m.workplace_injury_report, " +
            "m.health_declaration, " +
            "m.wellness_activity " +
            "FROM employee_medical_details m " +
            "JOIN employees e " +
            "ON m.employee_id = e.id " +
            "WHERE e.user_id = ?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                session.getId()
            );

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {
                if (resultSet.next()) {
                    String bloodType =
                        resultSet.getString("blood_type");

                    if (bloodType != null) {
                        bloodTypeDropdown.setSelectedItem(
                            bloodType
                        );
                    } else {
                        bloodTypeDropdown.setSelectedItem(
                            "-Select-"
                        );
                    }

                    allergy.setText(
                        safeDatabaseText(
                            resultSet.getString("allergy")
                        )
                    );

                    existMedCon.setText(
                        safeDatabaseText(
                            resultSet.getString(
                                "existing_condition"
                            )
                        )
                    );

                    medicalNotesArea.setText(
                        safeDatabaseText(
                            resultSet.getString(
                                "emergency_notes"
                            )
                        )
                    );

                    setFileLabel(
                        certificateLabel,
                        resultSet.getString(
                            "medical_certificate"
                        )
                    );

                    setFileLabel(
                        injuryReportLabel,
                        resultSet.getString(
                            "workplace_injury_report"
                        )
                    );

                    setFileLabel(
                        healthDeclarationLabel,
                        resultSet.getString(
                            "health_declaration"
                        )
                    );

                    setFileLabel(
                        wellnessActivityLabel,
                        resultSet.getString(
                            "wellness_activity"
                        )
                    );
                }
            }

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void setFileLabel(
            JLabel label,
            String path
    ) {
        if (path == null || path.trim().isEmpty()) {
            label.setText("No file selected");
        } else {
            label.setText(path);
        }
    }

    private String safeDatabaseText(String value) {
        return value == null ? "" : value;
    }

    private static final java.util.Map<String, String[]>
        CHECKLIST_ITEMS = java.util.Map.of(

            "Office",
            new String[]{
                "Ergonomic workstation setup completed",
                "Emergency exits identified",
                "Fire extinguisher location noted"
            },

            "Laboratory",
            new String[]{
                "PPE worn before entering lab",
                "Chemical storage labels verified",
                "Eyewash station accessible"
            },

            "Warehouse",
            new String[]{
                "Forklift path clear of obstructions",
                "Safety footwear worn",
                "Load limits on shelving checked"
            }
        );

    private void buildChecklistItems(
            JPanel checklistItems,
            String department
    ) {
        String[] items =
            CHECKLIST_ITEMS.get(department);

        if (items == null) {
            return;
        }

        GridBagConstraints constraints =
            new GridBagConstraints();

        constraints.insets =
            new Insets(4, 8, 4, 8);

        constraints.fill =
            GridBagConstraints.HORIZONTAL;

        constraints.anchor =
            GridBagConstraints.WEST;

        constraints.gridx = 0;

        int row = 0;

        constraints.gridy = row++;

        checklistItems.add(
            new JLabel(department + " Checklist"),
            constraints
        );

        for (String item : items) {
            JCheckBox checkBox =
                new JCheckBox(item);

            checkBox.setName(item);

            constraints.gridy = row++;

            checklistItems.add(
                checkBox,
                constraints
            );
        }
    }

    private void loadChecklistState(
            JPanel checklistItems,
            String department
    ) {
        if (!CHECKLIST_ITEMS.containsKey(department)) {
            return;
        }

        String sql =
            "SELECT item_label, is_checked " +
            "FROM safety_checklist_records sc " +
            "JOIN employees e " +
            "ON sc.employee_id = e.id " +
            "WHERE e.user_id = ? " +
            "AND sc.department_type = ?";

        java.util.Map<String, Boolean> savedStates =
            new java.util.HashMap<>();

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                session.getId()
            );

            statement.setString(
                2,
                department
            );

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {
                while (resultSet.next()) {
                    savedStates.put(
                        resultSet.getString("item_label"),
                        resultSet.getBoolean("is_checked")
                    );
                }
            }

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );

            return;
        }

        for (
            Component component :
            checklistItems.getComponents()
        ) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox =
                    (JCheckBox) component;

                Boolean checked =
                    savedStates.get(checkBox.getName());

                if (checked != null) {
                    checkBox.setSelected(checked);
                }
            }
        }
    }

    private void saveChecklistState(
            JPanel checklistItems,
            String department,
            JLabel statusLabel
    ) {
        String sql =
            "INSERT INTO safety_checklist_records " +
            "(employee_id, department_type, " +
            " item_label, is_checked) " +
            "VALUES (" +
            " (SELECT id FROM employees WHERE user_id = ?), " +
            " ?, ?, ?" +
            ") " +
            "ON DUPLICATE KEY UPDATE " +
            "is_checked = VALUES(is_checked)";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            boolean foundChecklistItem = false;

            for (
                Component component :
                checklistItems.getComponents()
            ) {
                if (component instanceof JCheckBox) {
                    JCheckBox checkBox =
                        (JCheckBox) component;

                    statement.setInt(
                        1,
                        session.getId()
                    );

                    statement.setString(
                        2,
                        department
                    );

                    statement.setString(
                        3,
                        checkBox.getName()
                    );

                    statement.setBoolean(
                        4,
                        checkBox.isSelected()
                    );

                    statement.executeUpdate();
                    foundChecklistItem = true;
                }
            }

            if (!foundChecklistItem) {
                Dialogs.error(
                    this,
                    "No checklist items to save for this "
                    + "department."
                );

                return;
            }

            AuditService.log(
                session.getId(),
                "UPDATE",
                "SAFETY_CHECKLIST",
                "Saved " + department
                + " safety checklist."
            );

            statusLabel.setForeground(
                new Color(0, 128, 0)
            );

            statusLabel.setText(
                "Checklist saved at "
                + java.time.LocalTime
                    .now()
                    .withNano(0)
            );

            Dialogs.info(
                this,
                "Safety checklist saved."
            );

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private String toPath(JLabel fileLabel) {
        String text = fileLabel.getText();

        if (
            text == null
            || text.trim().isEmpty()
            || "No file selected".equals(text)
        ) {
            return null;
        }

        return text;
    }

    private void loadProfile() {
        String sql =
            "SELECT " +
            "u.full_name, " +
            "u.email, " +
            "u.phone, " +
            "e.address, " +
            "e.emergency_contact " +
            "FROM users u " +
            "JOIN employees e " +
            "ON u.id = e.user_id " +
            "WHERE u.id = ?";

        try (
            Connection connection =
                Database.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {
            statement.setInt(
                1,
                session.getId()
            );

            try (
                ResultSet resultSet =
                    statement.executeQuery()
            ) {
                if (resultSet.next()) {
                    full.setText(
                        safeDatabaseText(
                            resultSet.getString("full_name")
                        )
                    );

                    email.setText(
                        safeDatabaseText(
                            resultSet.getString("email")
                        )
                    );

                    phone.setText(
                        safeDatabaseText(
                            resultSet.getString("phone")
                        )
                    );

                    address.setText(
                        safeDatabaseText(
                            resultSet.getString("address")
                        )
                    );

                    emergency.setText(
                        safeDatabaseText(
                            resultSet.getString(
                                "emergency_contact"
                            )
                        )
                    );
                }
            }

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void saveProfile() {
        try (
            Connection connection =
                Database.getConnection()
        ) {
            connection.setAutoCommit(false);

            try (
                PreparedStatement userStatement =
                    connection.prepareStatement(
                        "UPDATE users SET " +
                        "full_name=?, email=?, phone=? " +
                        "WHERE id=?"
                    );

                PreparedStatement employeeStatement =
                    connection.prepareStatement(
                        "UPDATE employees SET " +
                        "address=?, emergency_contact=? " +
                        "WHERE user_id=?"
                    )
            ) {
                userStatement.setString(
                    1,
                    full.getText().trim()
                );

                userStatement.setString(
                    2,
                    email.getText().trim()
                );

                userStatement.setString(
                    3,
                    phone.getText().trim()
                );

                userStatement.setInt(
                    4,
                    session.getId()
                );

                userStatement.executeUpdate();

                employeeStatement.setString(
                    1,
                    address.getText().trim()
                );

                employeeStatement.setString(
                    2,
                    emergency.getText().trim()
                );

                employeeStatement.setInt(
                    3,
                    session.getId()
                );

                employeeStatement.executeUpdate();

                connection.commit();

                AuditService.log(
                    session.getId(),
                    "UPDATE",
                    "PROFILE",
                    "Updated employee profile."
                );

                Dialogs.info(
                    this,
                    "Profile saved."
                );

            } catch (Exception exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }

    private void changePassword() {
        try {
            new AuthService().changePassword(
                session.getId(),
                new String(oldPassword.getPassword()),
                new String(newPassword.getPassword())
            );

            Dialogs.info(
                this,
                "Password changed."
            );

            oldPassword.setText("");
            newPassword.setText("");

        } catch (Exception exception) {
            Dialogs.error(
                this,
                exception.getMessage()
            );
        }
    }
}