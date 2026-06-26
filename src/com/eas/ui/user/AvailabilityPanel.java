package com.eas.ui.user;

import com.eas.config.Database;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class AvailabilityPanel extends JPanel {
    private int employeeId;

    public AvailabilityPanel(int employeeId) {
        this.employeeId = employeeId;
        setLayout(new BorderLayout(10, 10));
        initUI();
    }

    private void initUI() {
        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(BorderFactory.createTitledBorder("Set My Availability"));

        form.add(new JLabel("Date:"));
        JTextField dateField = new JTextField("YYYY-MM-DD");
        form.add(dateField);

        form.add(new JLabel("Status:"));
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"AVAILABLE", "UNAVAILABLE"});
        form.add(statusBox);

        JButton saveBtn = new JButton("Save Availability");
        saveBtn.addActionListener(e -> updateAvailability(dateField.getText(), statusBox.getSelectedItem().toString()));
        form.add(saveBtn);

        add(form, BorderLayout.NORTH);
    }

    private void updateAvailability(String date, String status) {
        // Logic para i-save sa database table (e.g., employee_availability)
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO employee_availability (employee_id, date, status) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE status = ?")) {
            ps.setInt(1, employeeId);
            ps.setString(2, date);
            ps.setString(3, status);
            ps.setString(4, status);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Availability updated!");
        } catch (SQLException e) { e.printStackTrace(); }
    }
}