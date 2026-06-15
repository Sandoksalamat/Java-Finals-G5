package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.SchedulingService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable;
    private JTable coverageTable;
    private DefaultTableModel swapModel;
    private DefaultTableModel coverageModel;
    private JLabel warningLabel;
    private SchedulingService schedulingService;
    private int currentAdminId;

    public WorkforceOptimizationPanel(int adminId) {
        this.currentAdminId = adminId;
        this.schedulingService = new SchedulingService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadSwapRequests();
        loadCoverageReport();
    }

    private void initUI() {
        JPanel northPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Workforce Scheduling & Optimization Management");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        northPanel.add(titleLabel, BorderLayout.WEST);

        warningLabel = new JLabel("Status: All departments meet minimum staffing requirements.");
        warningLabel.setFont(new Font("Arial", Font.BOLD, 12));
        warningLabel.setForeground(new Color(0, 128, 0));
        northPanel.add(warningLabel, BorderLayout.EAST);
        add(northPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        JPanel swapPanel = new JPanel(new BorderLayout(5, 5));
        swapPanel.setBorder(BorderFactory.createTitledBorder("Pending Shift Swap & Reliever Requests"));
        swapModel = new DefaultTableModel(new Object[]{"Request ID", "Employee ID", "Target Employee ID", "Schedule ID", "Reason", "Status"}, 0);
        swapTable = new JTable(swapModel);
        swapPanel.add(new JScrollPane(swapTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton rejectButton = new JButton("Reject Swap");
        JButton approveButton = new JButton("Approve Swap");
        
        approveButton.addActionListener(e -> handleApproval());
        rejectButton.addActionListener(e -> handleRejection());
        
        actionPanel.add(rejectButton);
        actionPanel.add(approveButton);
        swapPanel.add(actionPanel, BorderLayout.SOUTH);

        JPanel coveragePanel = new JPanel(new BorderLayout());
        coveragePanel.setBorder(BorderFactory.createTitledBorder("Staffing Coverage Report & Requirements"));
        coverageModel = new DefaultTableModel(new Object[]{"Department ID", "Shift Template ID", "Date", "Current Assigned Staff", "Min Required", "Status"}, 0);
        coverageTable = new JTable(coverageModel);
        coveragePanel.add(new JScrollPane(coverageTable), BorderLayout.CENTER);

        splitPane.setTopComponent(swapPanel);
        splitPane.setBottomComponent(coveragePanel);
        add(splitPane, BorderLayout.CENTER);
    }

    public void loadSwapRequests() {
        swapModel.setRowCount(0);
        String query = "SELECT id, requesting_employee_id, target_employee_id, original_schedule_id, reason, status FROM shift_swaps WHERE status = 'PENDING'";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                swapModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getInt("requesting_employee_id"),
                    rs.getInt("target_employee_id"),
                    rs.getInt("original_schedule_id"),
                    rs.getString("reason"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadCoverageReport() {
        coverageModel.setRowCount(0);
        boolean foundInsufficiency = false;

        String query = "SELECT r.department_id, r.shift_id, " +
                       "COALESCE(sa.effective_from, CURDATE()) as report_date, " +
                       "COUNT(sa.id) as assigned_count, r.min_required_staff " +
                       "FROM department_staffing_requirements r " +
                       "LEFT JOIN shift_assignments sa ON sa.shift_id = r.shift_id " +
                       "GROUP BY r.department_id, r.shift_id, report_date";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int deptId = rs.getInt("department_id");
                int shiftId = rs.getInt("shift_id");
                String dateStr = rs.getString("report_date");
                int currentCount = rs.getInt("assigned_count");
                int minRequired = rs.getInt("min_required_staff");

                String status = "OPTIMAL";
                if (currentCount < minRequired) {
                    status = "INSUFFICIENT STAFF";
                    foundInsufficiency = true;
                }

                coverageModel.addRow(new Object[]{deptId, shiftId, dateStr, currentCount, minRequired, status});
            }

            if (foundInsufficiency) {
                warningLabel.setText("CRITICAL WARNING: Insufficient workforce personnel coverage detected in departments!");
                warningLabel.setForeground(Color.RED);
            } else {
                warningLabel.setText("Status: All departments meet minimum staffing requirements.");
                warningLabel.setForeground(new Color(0, 128, 0));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleApproval() {
        int selectedRow = swapTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a swap request from the table.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) swapModel.getValueAt(selectedRow, 0);
        String reason = JOptionPane.showInputDialog(this, "Enter approval remarks or confirmation reason:");
        
        if (reason == null) {
            return;
        }

        boolean success = schedulingService.approveShiftSwap(requestId, currentAdminId, reason);
        if (success) {
            JOptionPane.showMessageDialog(this, "Shift swap transaction successfully processed and updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadSwapRequests();
            loadCoverageReport();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to complete schedule adjustment verification.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRejection() {
        int selectedRow = swapTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a swap request from the table.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int requestId = (int) swapModel.getValueAt(selectedRow, 0);
        String reason = JOptionPane.showInputDialog(this, "Enter reason for rejection:");
        
        if (reason == null || reason.trim().isEmpty()) {
            return;
        }

        boolean success = schedulingService.rejectShiftSwap(requestId, currentAdminId, reason);
        if (success) {
            JOptionPane.showMessageDialog(this, "Shift swap request has been rejected.", "Updated", JOptionPane.INFORMATION_MESSAGE);
            loadSwapRequests();
            loadCoverageReport();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}