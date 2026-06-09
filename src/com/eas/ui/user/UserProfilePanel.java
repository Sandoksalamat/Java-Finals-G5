package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.*;
import com.eas.util.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import javax.swing.*;

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
        p.setBorder(BorderFactory.createTitledBorder("Employee Medical Details")); // FIX 1: border stays on p, not overwritten
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
        cb.setBorder(BorderFactory.createTitledBorder("Occupational Safety Checklist")); // FIX 1: border correctly set on cb

        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(6, 8, 6, 8);
        gbc2.fill   = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.WEST;

        int row2 = 0;

        // Department selector — FIX 5: added to cb first, at the top, with gbc2
        JLabel dept_selector = new JLabel("Select Department:");
        String[] dept_Options = {"-Select-", "Office", "Laboratory", "Warehouse"};
        JComboBox<String> dept_dropdown = new JComboBox<>(dept_Options);

        gbc2.gridx = 0; gbc2.gridy = row2; gbc2.weightx = 0;
        cb.add(dept_selector, gbc2);
        gbc2.gridx = 1; gbc2.weightx = 1; gbc2.gridwidth = 2;
        cb.add(dept_dropdown, gbc2);
        gbc2.gridwidth = 1;
        row2++;

        // Placeholder panel for the dynamic checklist items
        JPanel checklistItems = new JPanel(new GridBagLayout());
        gbc2.gridx = 0; gbc2.gridy = row2;
        gbc2.gridwidth = 3; gbc2.weightx = 1;
        cb.add(checklistItems, gbc2);
        gbc2.gridwidth = 1;
        row2++;

        // Save checklist button
        JButton saveChecklist = UITheme.button("SAVE CHECKLIST");
        gbc2.gridx = 0; gbc2.gridy = row2; // FIX 3: use gbc2.gridy, not gbc.gridy
        gbc2.gridwidth = 3;
        gbc2.anchor = GridBagConstraints.CENTER;
        gbc2.fill   = GridBagConstraints.NONE;
        cb.add(saveChecklist, gbc2);
        gbc2.gridwidth = 1;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.WEST;

        // FIX 2: ActionListener — rebuilds checklist items reactively on selection change
        dept_dropdown.addActionListener(e -> {
            checklistItems.removeAll();

            String selected = (String) dept_dropdown.getSelectedItem();
            GridBagConstraints ic = new GridBagConstraints();
            ic.insets = new Insets(4, 8, 4, 8);
            ic.fill   = GridBagConstraints.HORIZONTAL;
            ic.anchor = GridBagConstraints.WEST;
            ic.gridx  = 0;
            int r = 0;

            if ("Office".equals(selected)) {
                ic.gridy = r++; checklistItems.add(new JLabel("Office Checklist"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Ergonomic workstation setup completed"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Emergency exits identified"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Fire extinguisher location noted"), ic);

            } else if ("Laboratory".equals(selected)) {
                ic.gridy = r++; checklistItems.add(new JLabel("Laboratory Checklist"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("PPE worn before entering lab"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Chemical storage labels verified"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Eyewash station accessible"), ic);

            } else if ("Warehouse".equals(selected)) {
                ic.gridy = r++; checklistItems.add(new JLabel("Warehouse Checklist"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Forklift path clear of obstructions"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Safety footwear worn"), ic);
                ic.gridy = r++; checklistItems.add(new JCheckBox("Load limits on shelving checked"), ic);
            }

            checklistItems.revalidate();
            checklistItems.repaint();
        });

        // ── Assemble center panel ─────────────────────────────────────────
        // FIX 4: cb is now added to the layout inside a wrapper so all panels appear
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
        saveMedical.addActionListener(e -> save());
        change.addActionListener(e -> change());

        JScrollPane scroll = new JScrollPane(center);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        load();
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