package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.*;
import com.eas.util.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import javax.swing.*;

//== Unknown == 
// H&S safety report 
// Wellness program
// Sick Employee monitoring
// Pre-Employment & Annual med exam record
public class UserProfilePanel extends JPanel {

    private final UserSession session;

    private final JTextField
        full      = new JTextField(22),
        email     = new JTextField(22),
        phone     = new JTextField(15),
        address   = new JTextField(30),
        emergency = new JTextField(25),
        allergy   = new JTextField(30),
        exist_medcon = new JTextField(30);

    private final JPasswordField
        oldp = new JPasswordField(15),
        newp = new JPasswordField(15);

    public UserProfilePanel(UserSession s) {
        session = s;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        add(UITheme.title("My Profile and Password"), BorderLayout.NORTH);

        // ── Employee Details ──────────────────────────────────────────────
        JPanel f = new JPanel(new GridLayout(0, 2, 8, 8));
        f.setBorder(BorderFactory.createTitledBorder("Employee Details"));
        addField(f, "Full Name",          full);
        addField(f, "Email",              email);
        addField(f, "Phone",              phone);
        addField(f, "Address",            address);
        addField(f, "Emergency Contact",  emergency);

        JButton save = UITheme.button("SAVE PROFILE");
        f.add(save);
        f.add(new JLabel(""));

        // ── Change Password ───────────────────────────────────────────────
        JPanel pwd = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pwd.setBorder(BorderFactory.createTitledBorder("Change Password"));
        pwd.add(new JLabel("Current"));
        pwd.add(oldp);
        pwd.add(new JLabel("New"));
        pwd.add(newp);
        JButton change = UITheme.button("CHANGE PASSWORD");
        pwd.add(change);

        // ── Employee Medical Details ──────────────────────────────────────
        JLabel certLabel    = new JLabel("No file selected");
        JLabel injuryLabel  = new JLabel("No file selected");
        JLabel healthLabel  = new JLabel("No file selected");
        JLabel fitnessLabel = new JLabel("No file selected");
        JLabel wellnessLabel = new JLabel("No file selected");

        JButton certBtn    = UITheme.button("Attach");
        JButton injuryBtn  = UITheme.button("Attach");
        JButton healthBtn  = UITheme.button("Attach");
        JButton fitnessBtn = UITheme.button("Attach");
        JButton wellnessBtn = UITheme.button("Attach");

        certBtn.addActionListener(e    -> attachFile(certLabel));
        injuryBtn.addActionListener(e  -> attachFile(injuryLabel));
        healthBtn.addActionListener(e  -> attachFile(healthLabel));
        fitnessBtn.addActionListener(e -> attachFile(fitnessLabel));
        wellnessBtn.addActionListener(e -> attachFile(wellnessLabel));

        // Blood type dropdown
        String[] btypeOptions = {"-Select-", "A+", "A-", "AB+", "AB-", "B+", "B-", "O+", "O-"};
        JComboBox<String> btypeDropdown = new JComboBox<>(btypeOptions);

        // Emergency medical notes textarea
        JTextArea mednote = new JTextArea();
        mednote.setLineWrap(true);
        mednote.setWrapStyleWord(true);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Employee Medical Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Blood Type
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel("Blood Type"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        p.add(btypeDropdown, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        p.add(new JLabel(""), gbc);
        row++;

        // Allergy
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel("Allergy"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        p.add(allergy, gbc);
        gbc.gridwidth = 1;
        row++;

        // Existing Medical Condition
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        p.add(new JLabel("Existing Medical Condition"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2;
        p.add(exist_medcon, gbc);
        gbc.gridwidth = 1;
        row++;

        // Emergency Medical Notes
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        p.add(new JLabel("Emergency Medical Notes"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.gridwidth = 2; gbc.ipady = 60;
        p.add(new JScrollPane(mednote), gbc);
        gbc.ipady = 0; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        row++;

        // Attachment rows
        String[]  docLabels  = {
            "Medical Certificate",
            "Workplace Injury Report",
            "Health Declaration",
            "Fitness to Work Clearance",
            "Wellness Activity"
        };
        JLabel[]  fileLabels = { certLabel, injuryLabel, healthLabel, fitnessLabel, wellnessLabel };
        JButton[] attachBtns = { certBtn,   injuryBtn,   healthBtn,   fitnessBtn,   wellnessBtn   };

        for (int i = 0; i < docLabels.length; i++) {
            gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
            p.add(new JLabel(docLabels[i]), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            p.add(fileLabels[i], gbc);
            gbc.gridx = 2; gbc.weightx = 0;
            p.add(attachBtns[i], gbc);
            row++;
        }

        // Save medical button
        JButton saveMedical = UITheme.button("SAVE MEDICAL DETAILS");
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill   = GridBagConstraints.NONE;
        p.add(saveMedical, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // ── Occupational Safety Checklist ─────────────────────────────────
        JPanel cb = new JPanel(new GridBagLayout());
        cb.setBorder(BorderFactory.createTitledBorder("Occupational Safety Checklist"));

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(6, 8, 6, 8);
        gbc2.fill   = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.WEST;

        int row2 = 0;

        // Department selector
        JLabel dept_selector = new JLabel("Select Department:");
        String[] dept_Options = {"-Select-", "Office", "Laboratory", "Warehouse"};
        JComboBox<String> dept_dropdown = new JComboBox<>(dept_Options);

        gbc2.gridx = 0; gbc2.gridy = row2; gbc2.weightx = 0;
        cb.add(dept_selector, gbc2);
        gbc2.gridx = 1; gbc2.weightx = 1; gbc2.gridwidth = 2;
        cb.add(dept_dropdown, gbc2);
        gbc2.gridwidth = 1;
        row2++;

        // Container for the dynamic checklist items
        JPanel checklistItems = new JPanel(new GridBagLayout());
        gbc2.gridx = 0; gbc2.gridy = row2;
        gbc2.gridwidth = 3; gbc2.weightx = 1;
        cb.add(checklistItems, gbc2);
        gbc2.gridwidth = 1;
        row2++;

        // Status label — confirms last save, shown under the checklist
        JLabel checklistStatus = new JLabel(" ");
        checklistStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        checklistStatus.setForeground(Color.GRAY);
        gbc2.gridx = 0; gbc2.gridy = row2;
        gbc2.gridwidth = 3;
        cb.add(checklistStatus, gbc2);
        gbc2.gridwidth = 1;
        row2++;

        // Save checklist button
        JButton saveChecklist = UITheme.button("SAVE CHECKLIST");
        gbc2.gridx = 0; gbc2.gridy = row2;
        gbc2.gridwidth = 3;
        gbc2.anchor = GridBagConstraints.CENTER;
        gbc2.fill   = GridBagConstraints.NONE;
        cb.add(saveChecklist, gbc2);
        gbc2.gridwidth = 1;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.WEST;

        // Rebuilds checklist items for the selected department and loads any
        // previously saved checked state for this employee from the database.
        dept_dropdown.addActionListener(e -> {
            checklistItems.removeAll();
            checklistStatus.setText(" ");

            String selected = (String) dept_dropdown.getSelectedItem();
            buildChecklistItems(checklistItems, selected);
            loadChecklistState(checklistItems, selected);

            checklistItems.revalidate();
            checklistItems.repaint();
        });

        saveChecklist.addActionListener(e -> {
            String selected = (String) dept_dropdown.getSelectedItem();
            if (selected == null || "-Select-".equals(selected)) {
                Dialogs.error(this, "Please select a department before saving the checklist.");
                return;
            }
            saveChecklistState(checklistItems, selected, checklistStatus);
        });

        // ── Assemble center panel ─────────────────────────────────────────
        JPanel medAndChecklist = new JPanel(new BorderLayout(0, 10));
        medAndChecklist.add(p,  BorderLayout.NORTH);
        medAndChecklist.add(cb, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.add(f,              BorderLayout.NORTH);
        center.add(medAndChecklist, BorderLayout.CENTER);
        center.add(pwd,            BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        // ── Wire up actions ───────────────────────────────────────────────
        save.addActionListener(e -> save());

        saveMedical.addActionListener(e -> saveMedical(btypeDropdown, mednote,
        certLabel, injuryLabel, healthLabel, fitnessLabel, wellnessLabel));
        
        change.addActionListener(e -> change());

        JScrollPane scroll = new JScrollPane(center);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        load();
        loadMedical(btypeDropdown, mednote,
            certLabel, injuryLabel, healthLabel, fitnessLabel, wellnessLabel);
    }

    private void addField(JPanel p, String label, JTextField field) {
        p.add(new JLabel(label));
        p.add(field);
    }

    private void attachFile(JLabel label) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Documents (*.pdf, *.jpg, *.png)", "pdf", "jpg", "jpeg", "png"
        ));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            label.setText(file.getName());
        }
    }

    private void saveMedical(JComboBox<String> btypeDropdown, JTextArea mednote,
            JLabel certLabel, JLabel injuryLabel, JLabel healthLabel,
            JLabel fitnessLabel, JLabel wellnessLabel) {
        String btype = (String) btypeDropdown.getSelectedItem();
        if ("-Select-".equals(btype)) btype = null;

        try (Connection c = Database.getConnection();
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO employee_medical_details " +
                "  (employee_id, blood_type, allergy, existing_condition, emergency_notes, " +
                "   medical_certificate, workplace_injury_report, health_declaration, " +
                "   fitness_to_work_clearance, wellness_activity) " +
                "VALUES ((SELECT id FROM employees WHERE user_id = ?), ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "  blood_type               = VALUES(blood_type), " +
                "  allergy                  = VALUES(allergy), " +
                "  existing_condition       = VALUES(existing_condition), " +
                "  emergency_notes          = VALUES(emergency_notes), " +
                "  medical_certificate      = VALUES(medical_certificate), " +
                "  workplace_injury_report  = VALUES(workplace_injury_report), " +
                "  health_declaration       = VALUES(health_declaration), " +
                "  fitness_to_work_clearance = VALUES(fitness_to_work_clearance), " +
                "  wellness_activity        = VALUES(wellness_activity)")) {

            ps.setInt(1, session.getId());
            ps.setString(2, btype);
            ps.setString(3, allergy.getText().trim());
            ps.setString(4, exist_medcon.getText().trim());
            ps.setString(5, mednote.getText().trim());

            // File labels: store the filename text, or null if still "No file selected"
            ps.setString(6,  toPath(certLabel));
            ps.setString(7,  toPath(injuryLabel));
            ps.setString(8,  toPath(healthLabel));
            ps.setString(9,  toPath(fitnessLabel));
            ps.setString(10, toPath(wellnessLabel));

            ps.executeUpdate();
            AuditService.log(session.getId(), "UPDATE", "MEDICAL", "Updated employee medical details.");
            Dialogs.info(this, "Medical details saved.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void loadMedical(JComboBox<String> btypeDropdown, JTextArea mednote,
                         JLabel certLabel, JLabel injuryLabel, JLabel healthLabel,
                         JLabel fitnessLabel, JLabel wellnessLabel) {
        try (Connection c = Database.getConnection();
            PreparedStatement ps = c.prepareStatement(
                "SELECT m.blood_type, m.allergy, m.existing_condition, m.emergency_notes, " +
                "       m.medical_certificate, m.workplace_injury_report, m.health_declaration, " +
                "       m.fitness_to_work_clearance, m.wellness_activity " +
                "FROM employee_medical_details m " +
                "JOIN employees e ON m.employee_id = e.id " +
                "WHERE e.user_id = ?")) {

            ps.setInt(1, session.getId());
            try (ResultSet r = ps.executeQuery()) {
                if (r.next()) {
                    String btype = r.getString("blood_type");
                    if (btype != null) btypeDropdown.setSelectedItem(btype);

                    allergy.setText(r.getString("allergy") != null ? r.getString("allergy") : "");
                    exist_medcon.setText(r.getString("existing_condition") != null ? r.getString("existing_condition") : "");
                    mednote.setText(r.getString("emergency_notes") != null ? r.getString("emergency_notes") : "");

                    setFileLabel(certLabel,    r.getString("medical_certificate"));
                    setFileLabel(injuryLabel,  r.getString("workplace_injury_report"));
                    setFileLabel(healthLabel,  r.getString("health_declaration"));
                    setFileLabel(fitnessLabel, r.getString("fitness_to_work_clearance"));
                    setFileLabel(wellnessLabel,r.getString("wellness_activity"));
                }
            }
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void setFileLabel(JLabel label, String path) {
        label.setText(path != null ? path : "No file selected");
    }

    // ── Occupational Safety Checklist helpers ───────────────────────────────

    /** Master list of checklist item labels per department. Single source of truth
     *  so build, load, and save all stay in sync with the same item text. */
    private static final java.util.Map<String, String[]> CHECKLIST_ITEMS = java.util.Map.of(
        "Office", new String[]{
            "Ergonomic workstation setup completed",
            "Emergency exits identified",
            "Fire extinguisher location noted"
        },
        "Laboratory", new String[]{
            "PPE worn before entering lab",
            "Chemical storage labels verified",
            "Eyewash station accessible"
        },
        "Warehouse", new String[]{
            "Forklift path clear of obstructions",
            "Safety footwear worn",
            "Load limits on shelving checked"
        }
    );

    /** Builds the JCheckBox rows for the selected department into the given container. */
    private void buildChecklistItems(JPanel checklistItems, String department) {
        String[] items = CHECKLIST_ITEMS.get(department);
        if (items == null) return; // "-Select-" or unknown

        GridBagConstraints ic = new GridBagConstraints();
        ic.insets = new Insets(4, 8, 4, 8);
        ic.fill   = GridBagConstraints.HORIZONTAL;
        ic.anchor = GridBagConstraints.WEST;
        ic.gridx  = 0;
        int r = 0;

        ic.gridy = r++;
        checklistItems.add(new JLabel(department + " Checklist"), ic);

        for (String item : items) {
            JCheckBox box = new JCheckBox(item);
            box.setName(item); // used as the lookup key on save
            ic.gridy = r++;
            checklistItems.add(box, ic);
        }
    }

    /** Loads this employee's previously saved checked state for the department
     *  and applies it to the JCheckBox components just built by buildChecklistItems. */
    private void loadChecklistState(JPanel checklistItems, String department) {
        if (!CHECKLIST_ITEMS.containsKey(department)) return;

        String sql =
            "SELECT item_label, is_checked " +
            "FROM safety_checklist_records sc " +
            "JOIN employees e ON sc.employee_id = e.id " +
            "WHERE e.user_id = ? AND sc.department_type = ?";

        java.util.Map<String, Boolean> saved = new java.util.HashMap<>();

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, session.getId());
            p.setString(2, department);

            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    saved.put(rs.getString("item_label"), rs.getBoolean("is_checked"));
                }
            }

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
            return;
        }

        for (Component comp : checklistItems.getComponents()) {
            if (comp instanceof JCheckBox box) {
                Boolean checked = saved.get(box.getName());
                if (checked != null) box.setSelected(checked);
            }
        }
    }

    /** Persists the current checkbox states for the selected department. Uses
     *  INSERT ... ON DUPLICATE KEY UPDATE so re-saving simply updates is_checked. */
    private void saveChecklistState(JPanel checklistItems, String department, JLabel statusLabel) {
        String sql =
            "INSERT INTO safety_checklist_records " +
            "  (employee_id, department_type, item_label, is_checked) " +
            "VALUES ((SELECT id FROM employees WHERE user_id = ?), ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE is_checked = VALUES(is_checked)";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            boolean any = false;
            for (Component comp : checklistItems.getComponents()) {
                if (comp instanceof JCheckBox box) {
                    p.setInt(1, session.getId());
                    p.setString(2, department);
                    p.setString(3, box.getName());
                    p.setBoolean(4, box.isSelected());
                    p.executeUpdate();
                    any = true;
                }
            }

            if (!any) {
                Dialogs.error(this, "No checklist items to save for this department.");
                return;
            }

            AuditService.log(session.getId(), "UPDATE", "SAFETY_CHECKLIST",
                "Saved " + department + " safety checklist.");

            statusLabel.setForeground(new Color(0, 128, 0));
            statusLabel.setText("Checklist saved at " +
                java.time.LocalTime.now().withNano(0));

            Dialogs.info(this, "Safety checklist saved.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

/** Returns null when the user hasn't picked a file yet. */
    private String toPath(JLabel fileLabel) {
        String text = fileLabel.getText();
        return "No file selected".equals(text) ? null : text;
    }

    private void load() {
        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(
                 "SELECT u.full_name, u.email, u.phone, e.address, e.emergency_contact " +
                 "FROM users u JOIN employees e ON u.id = e.user_id WHERE u.id = ?")) {
            p.setInt(1, session.getId());
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    full.setText(r.getString(1));
                    email.setText(r.getString(2));
                    phone.setText(r.getString(3));
                    address.setText(r.getString(4));
                    emergency.setText(r.getString(5));
                }
            }
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void save() {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement u = c.prepareStatement(
                     "UPDATE users SET full_name=?, email=?, phone=? WHERE id=?");
                 PreparedStatement e = c.prepareStatement(
                     "UPDATE employees SET address=?, emergency_contact=? WHERE user_id=?")) {

                u.setString(1, full.getText().trim());
                u.setString(2, email.getText().trim());
                u.setString(3, phone.getText().trim());
                u.setInt(4, session.getId());
                u.executeUpdate();

                e.setString(1, address.getText().trim());
                e.setString(2, emergency.getText().trim());
                e.setInt(3, session.getId());
                e.executeUpdate();

                c.commit();
                AuditService.log(session.getId(), "UPDATE", "PROFILE", "Updated employee profile.");
                Dialogs.info(this, "Profile saved.");
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void change() {
        try {
            new AuthService().changePassword(
                session.getId(),
                new String(oldp.getPassword()),
                new String(newp.getPassword())
            );
            Dialogs.info(this, "Password changed.");
            oldp.setText("");
            newp.setText("");
        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}