package com.eas.ui.user;

import com.eas.model.UserSession;
import com.eas.util.UITheme;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class MyShiftSwapPanel extends JPanel {

    private UserSession session;
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
        try {
            Connection conn = com.eas.config.Database.getConnection();
            String sql = "SELECT id FROM employees WHERE user_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, session.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                currentEmployeeId = rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponent() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.NAVY, 1), 
            "Create Shift Exchange Request", 
            0, 0, 
            new Font("SansSerif", Font.BOLD, 14), 
            UITheme.NAVY
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblTarget = new JLabel("Target Employee ID:");
        lblTarget.setFont(new Font("SansSerif", Font.BOLD, 12));
        formPanel.add(lblTarget, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        txtTargetEmployeeId = new JTextField(15);
        formPanel.add(txtTargetEmployeeId, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblReason = new JLabel("Justification Reason:");
        lblReason.setFont(new Font("SansSerif", Font.BOLD, 12));
        formPanel.add(lblReason, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        txtReason = new JTextArea(4, 20);
        txtReason.setLineWrap(true);
        txtReason.setWrapStyleWord(true);
        formPanel.add(new JScrollPane(txtReason), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        JButton btnSubmit = UITheme.button("SUBMIT SWAP REQUEST");
        btnSubmit.addActionListener(e -> submitRequest());
        formPanel.add(btnSubmit, gbc);

        String[] columns = {"Request ID", "Target Employee ID", "Reason/Justification", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestTable = new JTable(tableModel);
        requestTable.setRowHeight(25);
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(Color.WHITE);
        tableContainer.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1), 
            "My Sent Requests History", 
            0, 0, 
            new Font("SansSerif", Font.BOLD, 14), 
            UITheme.NAVY
        ));
        tableContainer.add(new JScrollPane(requestTable), BorderLayout.CENTER);

        add(formPanel, BorderLayout.WEST);
        add(tableContainer, BorderLayout.CENTER);
    }

    private void submitRequest() {
        String targetStr = txtTargetEmployeeId.getText().trim();
        String reasonStr = txtReason.getText().trim();

        if (targetStr.isEmpty() || reasonStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all input fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (currentEmployeeId == -1) {
            JOptionPane.showMessageDialog(this, "Session employee mapping missing.", "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int targetId = Integer.parseInt(targetStr);
            Connection conn = com.eas.config.Database.getConnection();
            
            String sql = "INSERT INTO shift_swaps (employee_id, target_employee_id, reason, status) VALUES (?, ?, ?, 'PENDING')";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, currentEmployeeId);
            ps.setInt(2, targetId);
            ps.setString(3, reasonStr);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Shift exchange request routed to Admin queue successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                txtTargetEmployeeId.setText("");
                txtReason.setText("");
                refreshTableData();
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Target Employee ID must be a structural numeric digit.", "Format Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database entry pipeline transaction error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refreshTableData() {
        if (currentEmployeeId == -1) return;
        
        tableModel.setRowCount(0);
        try {
            Connection conn = com.eas.config.Database.getConnection();
            String sql = "SELECT id, target_employee_id, reason, status FROM shift_swaps WHERE employee_id = ? ORDER BY id DESC";
            PreparedStatement ps = conn.prepareStatement(sql);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}