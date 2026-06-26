package com.eas.ui.user;

import com.eas.model.UserSession;
import com.eas.ui.user.EmployeeVolunteerPanel; // Siguraduhing nandito ang import
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MyShiftSwapPanel extends JPanel {

    private final UserSession session;
    private JTable requestTable;
    private DefaultTableModel tableModel;
    private JTextField txtTargetEmployeeId;
    private JTextArea txtReason;
    private int currentEmployeeId = -1;

    public MyShiftSwapPanel(UserSession s) {
        this.session = s;
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        resolveEmployeeId();
        initComponent();
        refreshTableData();
    }

    private void resolveEmployeeId() {
        try (Connection conn = com.eas.config.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM employees WHERE user_id = ?")) {
            ps.setInt(1, session.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentEmployeeId = rs.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initComponent() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Create Shift Exchange Request"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Target Employee ID:"), gbc);
        gbc.gridx = 1;
        txtTargetEmployeeId = new JTextField(15);
        formPanel.add(txtTargetEmployeeId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Justification:"), gbc);
        gbc.gridx = 1;
        txtReason = new JTextArea(3, 20);
        formPanel.add(new JScrollPane(txtReason), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JButton btnSubmit = new JButton("SUBMIT REQUEST");
        btnSubmit.addActionListener(e -> submitRequest());
        formPanel.add(btnSubmit, gbc);

        tableModel = new DefaultTableModel(new String[]{"ID", "Target ID", "Reason", "Status"}, 0);
        requestTable = new JTable(tableModel);
        
        add(formPanel, BorderLayout.WEST);
        add(new JScrollPane(requestTable), BorderLayout.CENTER);
    }

    private void submitRequest() {
        if (currentEmployeeId == -1) {
            JOptionPane.showMessageDialog(this, "Employee ID not found.");
            return;
        }

        try {
            int targetId = Integer.parseInt(txtTargetEmployeeId.getText().trim());
            String reason = txtReason.getText().trim();

            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Reason cannot be empty.");
                return;
            }

            try (Connection conn = com.eas.config.Database.getConnection();
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO shift_swaps (employee_id, target_employee_id, schedule_id, reason, status) VALUES (?, ?, 1, ?, 'PENDING')")) {
                ps.setInt(1, currentEmployeeId);
                ps.setInt(2, targetId);
                ps.setString(3, reason);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Request submitted successfully!");
                txtTargetEmployeeId.setText("");
                txtReason.setText("");
                refreshTableData();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Employee ID format.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void refreshTableData() {
        tableModel.setRowCount(0);
        try (Connection conn = com.eas.config.Database.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, target_employee_id, reason, status FROM shift_swaps WHERE employee_id = ?")) {
            ps.setInt(1, currentEmployeeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("target_employee_id"),
                    rs.getString("reason"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}