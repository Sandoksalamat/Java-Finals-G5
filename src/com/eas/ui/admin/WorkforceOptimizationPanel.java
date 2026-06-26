package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import java.awt.*;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, empTable;
    private DefaultTableModel swapModel, empModel;
    private JTextField dateField = new JTextField("2026-06-25");
    private JLabel staffingStatusLabel = new JLabel("STATUS: Initializing...");
    private int currentAdminId;
    private SchedulingService schedulingService;

    public WorkforceOptimizationPanel(int adminId) {
        this.currentAdminId = adminId;
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout());
        initUI();
        refreshData();
    }

    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. Requests Tab
        JPanel swapPanel = new JPanel(new BorderLayout(5, 5));
        swapModel = new DefaultTableModel(new Object[]{"ID", "Emp ID", "Target ID", "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        swapTable = new JTable(swapModel);
        swapPanel.add(new JScrollPane(swapTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setPreferredSize(new Dimension(0, 50));
        JButton rejectButton = new JButton("Reject");
        JButton approveButton = new JButton("Approve");
        
        approveButton.addActionListener(e -> processAction("APPROVED"));
        rejectButton.addActionListener(e -> processAction("REJECTED"));
        
        actionPanel.add(rejectButton);
        actionPanel.add(approveButton);
        swapPanel.add(actionPanel, BorderLayout.SOUTH);

        // 2. Monitor Tab
        JPanel monitorPanel = new JPanel(new BorderLayout());
        staffingStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        monitorPanel.add(staffingStatusLabel, BorderLayout.CENTER);
        
        // 3. Assign Tab
        JPanel relieverPanel = new JPanel(new BorderLayout(5, 5));
        empModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        empTable = new JTable(empModel);
        relieverPanel.add(new JScrollPane(empTable), BorderLayout.CENTER);
        
        tabbedPane.addTab("Requests", swapPanel);
        tabbedPane.addTab("Monitor", monitorPanel);
        tabbedPane.addTab("Assign", relieverPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        loadSwapRequests();
        updateStaffingStatus();
        loadAvailableEmployees();
    }

    private void loadSwapRequests() {
        swapModel.setRowCount(0);
        String query = "SELECT id, employee_id, target_employee_id, reason, status FROM shift_swaps WHERE status = 'PENDING' " +
                       "UNION ALL " +
                       "SELECT id, employee_id, 0 as target_employee_id, 'Volunteer' as reason, status FROM shift_assignments WHERE status = 'PENDING_VOLUNTEER'";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), rs.getString("status")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadAvailableEmployees() {
        empModel.setRowCount(0);
        List<String[]> employees = schedulingService.getAvailableEmployees(dateField.getText());
        for (String[] emp : employees) {
            empModel.addRow(new Object[]{emp[0], emp[1]});
        }
    }

    private void updateStaffingStatus() {
        // Inayos ang query: Siguraduhing may column na 'date' sa shift_assignments
        String query = "SELECT COUNT(*) FROM shift_assignments WHERE status = 'ASSIGNED'";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                staffingStatusLabel.setText("STATUS: " + rs.getInt(1) + " active assignments found.");
            }
        } catch (SQLException e) { 
            staffingStatusLabel.setText("STATUS: Active (Monitor linked)"); 
        }
    }

    private void processAction(String newStatus) {
        int row = swapTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a row first."); return; }
        
        int id = (int) swapModel.getValueAt(row, 0);
        String currentStatus = (String) swapModel.getValueAt(row, 4);

        try (Connection conn = Database.getConnection()) {
            if ("PENDING_VOLUNTEER".equals(currentStatus)) {
                PreparedStatement ps = conn.prepareStatement("UPDATE shift_assignments SET status = ? WHERE id = ?");
                ps.setString(1, newStatus);
                ps.setInt(2, id);
                ps.executeUpdate();
            } else {
                PreparedStatement ps = conn.prepareStatement("UPDATE shift_swaps SET status = ? WHERE id = ?");
                ps.setString(1, newStatus);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            refreshData();
            JOptionPane.showMessageDialog(this, "Action Successful!");
        } catch (SQLException e) { 
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage()); 
        }
    }
}