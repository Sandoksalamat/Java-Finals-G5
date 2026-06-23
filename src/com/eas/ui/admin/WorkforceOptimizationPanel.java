package com.eas.ui.admin;

import com.eas.config.Database;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class WorkforceOptimizationPanel extends JPanel {

    private JTable swapTable, historyTable, coverageTable;
    private DefaultTableModel swapModel, historyModel, coverageModel;
    private int currentAdminId;

    public WorkforceOptimizationPanel(int adminId) {
        this.currentAdminId = adminId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        loadSwapRequests();
        loadRevisionHistory();
        loadCoverageReport();
    }

    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Pending Requests Panel
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

        // Revision History Panel (Audit Trail)
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyModel = new DefaultTableModel(new Object[]{"ID", "Orig ID", "Repl ID", "Reason", "Status", "Admin ID"}, 0);
        historyTable = new JTable(historyModel);
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        tabbedPane.addTab("Pending Requests", swapPanel);
        tabbedPane.addTab("Schedule Revision History", historyPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void loadSwapRequests() {
        swapModel.setRowCount(0);
        String query = "SELECT id, employee_id, target_employee_id, reason FROM shift_swaps WHERE status = 'PENDING'";
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("employee_id"), rs.getInt("target_employee_id"), rs.getString("reason"), "PENDING"});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadRevisionHistory() {
        historyModel.setRowCount(0);
        String query = "SELECT id, original_employee_id, replacement_employee_id, reason, approved_by FROM schedule_revision_history";
        try (Connection conn = Database.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                historyModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("original_employee_id"), rs.getInt("replacement_employee_id"), rs.getString("reason"), "APPROVED", rs.getInt("approved_by")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleApproval() {
        int row = swapTable.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        int empId = Integer.parseInt(swapModel.getValueAt(row, 1).toString());
        int targetId = Integer.parseInt(swapModel.getValueAt(row, 2).toString());
        String reason = swapModel.getValueAt(row, 3).toString();

        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE shift_swaps SET status = 'APPROVED' WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO schedule_revision_history (schedule_id, original_employee_id, replacement_employee_id, reason, approved_by) VALUES (1, ?, ?, ?, ?)")) {
                ps.setInt(1, empId);
                ps.setInt(2, targetId);
                ps.setString(3, reason);
                ps.setInt(4, currentAdminId);
                ps.executeUpdate();
            }
            loadSwapRequests();
            loadRevisionHistory();
            JOptionPane.showMessageDialog(this, "Approved!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRejection() {
        int row = swapTable.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        try (Connection conn = Database.getConnection();
            PreparedStatement ps = conn.prepareStatement("UPDATE shift_swaps SET status = 'REJECTED' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadSwapRequests();
            JOptionPane.showMessageDialog(this, "Rejected!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadCoverageReport() { /* Keep existing */ }
}