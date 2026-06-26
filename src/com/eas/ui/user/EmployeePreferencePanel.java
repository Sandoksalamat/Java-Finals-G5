package com.eas.ui.user;

import com.eas.config.Database;
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class EmployeePreferencePanel extends JPanel {

    private int currentUserId;
    private JTextField restDayField = new JTextField(10);
    private JTextField unavailableDateField = new JTextField("YYYY-MM-DD", 10);

    public EmployeePreferencePanel(int userId) {
        this.currentUserId = userId;
        setLayout(new GridLayout(3, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        add(new JLabel("Preferred Rest Day:"));
        add(restDayField);
        add(new JLabel("Unavailable Date (YYYY-MM-DD):"));
        add(unavailableDateField);

        JButton saveBtn = new JButton("Save Preferences");
        saveBtn.addActionListener(e -> savePreference());
        add(saveBtn);
    }

    private void savePreference() {
        String sql = "INSERT INTO employee_preferences (employee_id, preferred_rest_day, unavailable_date) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, restDayField.getText());
            ps.setString(3, unavailableDateField.getText());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Preference saved!");
        } catch (SQLException e) { 
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Error saving preference.");
        }
    }
}