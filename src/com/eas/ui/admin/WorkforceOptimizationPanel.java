package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, historyTable, empTable;
    private DefaultTableModel swapModel, historyModel, empModel;
    private JTextField dateField = new JTextField("2026-06-25");
    private JLabel staffingStatusLabel = new JLabel("STATUS: Checking...");
    private int currentAdminId;
    private SchedulingService schedulingService;

    public WorkforceOptimizationPanel(int adminId) {
        this.currentAdminId = adminId;
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        refreshData();
    }

    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // 1. Pending Requests & Volunteers Panel
        JPanel swapPanel = new JPanel(new BorderLayout(5, 5));
        swapModel = new DefaultTableModel(new Object[]{"ID", "Emp ID", "Target ID", "Reason", "Status"}, 0);
        swapTable = new JTable(swapModel);
        swapPanel.add(new JScrollPane(swapTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton rejectButton = new JButton("Reject");
        JButton approveButton = new JButton("Approve");
        
        approveButton.addActionListener(e -> handleApproval());
        rejectButton.addActionListener(e -> handleRejection());
        
        actionPanel.add(rejectButton);
        actionPanel.add(approveButton);
        swapPanel.add(actionPanel, BorderLayout.SOUTH);

        // 2. Workforce Optimization / Staffing Monitoring Panel
        JPanel monitorPanel = new JPanel(new BorderLayout());
        JPanel statusTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        staffingStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusTop.add(staffingStatusLabel);
        monitorPanel.add(statusTop, BorderLayout.NORTH);

        // 3. Reliever Assignment Panel
        JPanel relieverPanel = new JPanel(new BorderLayout(5, 5));
        empModel = new DefaultTableModel(new Object[]{"ID", "Name"}, 0);
        empTable = new JTable(empModel);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Date:"));
        filterPanel.add(dateField);
        JButton searchBtn = new JButton("Find Available");
        searchBtn.addActionListener(e -> loadAvailableEmployees());
        filterPanel.add(searchBtn);
        
        JButton assignBtn = new JButton("Assign as Reliever");
        assignBtn.addActionListener(e -> handleAssignment());
        
        relieverPanel.add(filterPanel, BorderLayout.NORTH);
        relieverPanel.add(new JScrollPane(empTable), BorderLayout.CENTER);
        relieverPanel.add(assignBtn, BorderLayout.SOUTH);

        tabbedPane.addTab("Requests & Volunteers", swapPanel);
        tabbedPane.addTab("Staffing Monitor", monitorPanel);
        tabbedPane.addTab("Assign Reliever", relieverPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void refreshData() {
        loadSwapRequests();
        loadAvailableEmployees();
        updateStaffingStatus();
    }

    private void updateStaffingStatus() {
        // Assume Dept 2, Shift 1 for current date monitoring
        boolean isUnderstaffed = schedulingService.isUnderstaffed(2, 1, dateField.getText());
        if (isUnderstaffed) {
            staffingStatusLabel.setText("STATUS: UNDERSTAFFED - Needs Volunteers!");
            staffingStatusLabel.setForeground(Color.RED);
        } else {
            staffingStatusLabel.setText("STATUS: FULL COVERAGE");
            staffingStatusLabel.setForeground(new Color(0, 153, 0)); // Dark Green
        }
    }

    public void loadSwapRequests() {
        swapModel.setRowCount(0);
        String query = "SELECT id, employee_id, target_employee_id, reason, status FROM shift_swaps WHERE status IN ('PENDING', 'PENDING_VOLUNTEER')";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), rs.getString("status")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadAvailableEmployees() {
        empModel.setRowCount(0);
        for (String[] emp : schedulingService.getAvailableEmployees(dateField.getText())) {
            empModel.addRow(emp);
        }
    }

    private void handleApproval() {
        int row = swapTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a request."); return; }
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        String reason = JOptionPane.showInputDialog(this, "Remarks:");
        if (reason != null && schedulingService.approveShiftSwap(id, currentAdminId, reason)) {
            refreshData();
        }
    }

    private void handleRejection() {
        int row = swapTable.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        String reason = JOptionPane.showInputDialog(this, "Remarks:");
        if (reason != null && schedulingService.rejectShiftSwap(id, currentAdminId, reason)) {
            refreshData();
        }
    }

    private void handleAssignment() {
        int row = empTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select an employee."); return; }
        int empId = Integer.parseInt(empModel.getValueAt(row, 0).toString());
        if (schedulingService.assignReliever(empId, 1, dateField.getText())) {
            JOptionPane.showMessageDialog(this, "Assigned successfully!");
            refreshData();
        }
    }
}