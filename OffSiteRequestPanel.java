package com.eas.ui.user;

import com.eas.model.OffSiteRequest;
import com.eas.model.UserSession;
import com.eas.model.OffSiteAccomplishment;
import com.eas.service.HybridAttendanceService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class OffSiteRequestPanel extends JPanel {
    private final HybridAttendanceService service = new HybridAttendanceService();
    private final int currentEmployeeId;
    private JComboBox<String> typeCombo;
    private JTextField startField, endField, locationField, purposeField;
    private JTextField latField, lngField, fileField;
    private JTextArea accomplishmentArea;

    public OffSiteRequestPanel(UserSession session) {
        this.currentEmployeeId = session.getId();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Apply Pre-Approval Request", createApplicationForm());
        tabbedPane.addTab("2. Mobile Remote Clocking", createRemoteClockPanel());
        tabbedPane.addTab("3. Post-Work Accomplishment", createAccomplishmentPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createApplicationForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        typeCombo = new JComboBox<>(new String[]{"WFH", "OFFICIAL_BUSINESS", "FIELD_ASSIGNMENT", "TRAVEL_DUTY"});
        startField = new JTextField(LocalDate.now().toString());
        endField = new JTextField(LocalDate.now().toString());
        locationField = new JTextField();
        purposeField = new JTextField();
        JButton submitBtn = new JButton("Submit Application");

        addGridRow(panel, gbc, 0, "Duty Classification Type:", typeCombo);
        addGridRow(panel, gbc, 1, "Start Date (YYYY-MM-DD):", startField);
        addGridRow(panel, gbc, 2, "End Date (YYYY-MM-DD):", endField);
        addGridRow(panel, gbc, 3, "Destination / Target Client:", locationField);
        addGridRow(panel, gbc, 4, "Purpose Details:", purposeField);

        gbc.gridx = 1; gbc.gridy = 5;
        panel.add(submitBtn, gbc);

        submitBtn.addActionListener(e -> {
            try {
                OffSiteRequest req = new OffSiteRequest();
                req.setEmployeeId(currentEmployeeId);
                req.setRequestType((String) typeCombo.getSelectedItem());
                req.setStartDate(LocalDate.parse(startField.getText().trim()));
                req.setEndDate(LocalDate.parse(endField.getText().trim()));
                req.setDestinationOrLocation(locationField.getText().trim());
                req.setPurpose(purposeField.getText().trim());

                if (service.submitOffSiteRequest(req)) {
                    JOptionPane.showMessageDialog(this, "Application filed successfully! Awaiting supervisor signature.");
                } else {
                    JOptionPane.showMessageDialog(this, "Submission failed. Ensure date strings fit YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "Format Error: Please use the absolute YYYY-MM-DD format.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Validation Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createRemoteClockPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        latField = new JTextField("14.7011", 10);
        lngField = new JTextField("121.0422", 10);
        JButton gpsBtn = new JButton("Auto Grab GPS Coordinates");
        JButton timeInBtn = new JButton("Remote TIME IN");
        JButton timeOutBtn = new JButton("Remote TIME OUT");

        timeInBtn.setBackground(new Color(46, 139, 87)); timeInBtn.setForeground(Color.BLACK);
        timeOutBtn.setBackground(new Color(178, 34, 34)); timeOutBtn.setForeground(Color.BLACK);

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Latitude:"), gbc);
        gbc.gridx = 1; panel.add(latField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Longitude:"), gbc);
        gbc.gridx = 1; panel.add(lngField, gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(gpsBtn, gbc);

        JPanel actionRow = new JPanel(new FlowLayout());
        actionRow.add(timeInBtn); actionRow.add(timeOutBtn);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; panel.add(actionRow, gbc);

        gpsBtn.addActionListener(e -> {
            double shift = (Math.random() - 0.5) * 0.01;
            latField.setText(String.format("%.4f", 14.7011 + shift));
            lngField.setText(String.format("%.4f", 121.0422 + shift));
        });

        timeInBtn.addActionListener(e -> fireClockEvent("TIME_IN"));
        timeOutBtn.addActionListener(e -> fireClockEvent("TIME_OUT"));

        return panel;
    }

    private JPanel createAccomplishmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel topForm = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(4, 4, 4, 4);

        fileField = new JTextField(15);
        JButton browseBtn = new JButton("Attach Document Proof");
        accomplishmentArea = new JTextArea(5, 20);
        accomplishmentArea.setLineWrap(true);
        JButton submitDocBtn = new JButton("Upload & Submit Report");

        addGridRow(topForm, gbc, 0, "Select Travel Order/Certificate Path:", fileField);
        gbc.gridx = 2; gbc.gridy = 0; topForm.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        topForm.add(new JLabel("Provide Written Operational Activity Execution Summary:"), gbc);

        panel.add(topForm, BorderLayout.NORTH);
        panel.add(new JScrollPane(accomplishmentArea), BorderLayout.CENTER);
        panel.add(submitDocBtn, BorderLayout.SOUTH);

        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        submitDocBtn.addActionListener(e -> {
            OffSiteAccomplishment report = new OffSiteAccomplishment();
            report.setEmployeeId(currentEmployeeId);
            report.setAttendanceDate(LocalDate.now());
            report.setAccomplishmentText(accomplishmentArea.getText().trim());
            report.setDocumentPath(fileField.getText().trim());

            if (service.submitAccomplishment(report)) {
                JOptionPane.showMessageDialog(this, "Accomplishment summary saved successfully!");
                accomplishmentArea.setText(""); fileField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Submission error: Verify that your parent clock record row for today is open.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void fireClockEvent(String logType) {
        double lat = Double.parseDouble(latField.getText());
        double lng = Double.parseDouble(lngField.getText());
        
        boolean success = service.processMobileClockEvent(currentEmployeeId, logType, lat, lng, null);
        if (success) {
            JOptionPane.showMessageDialog(this, "Remote coordinate transaction log established for: " + logType);
        } else {
            JOptionPane.showMessageDialog(this, "Transaction Rejected: Ensure you have an active APPROVED offsite application for today.", "Access Denied", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addGridRow(JPanel p, GridBagConstraints gbc, int y, String label, Component comp) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 1; p.add(new JLabel(label), gbc);
        gbc.gridx = 1; p.add(comp, gbc);
    }
}