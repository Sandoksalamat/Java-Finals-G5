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
        String query = "SELECT id, original_employee_id, replacement_employee_id, reason FROM schedule_revision_history WHERE status = 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                swapModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("original_employee_id"), rs.getInt("replacement_employee_id"), rs.getString("reason"), "PENDING"});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadRevisionHistory() {
        historyModel.setRowCount(0);
        String query = "SELECT id, original_employee_id, replacement_employee_id, reason, status, approved_by FROM schedule_revision_history WHERE status != 'PENDING'";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                historyModel.addRow(new Object[]{rs.getInt("id"), rs.getInt("original_employee_id"), rs.getInt("replacement_employee_id"), rs.getString("reason"), rs.getString("status"), rs.getInt("approved_by")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleApproval() {
        int row = swapTable.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE schedule_revision_history SET status = 'APPROVED', approved_by = ? WHERE id = ?")) {
            ps.setInt(1, currentAdminId);
            ps.setInt(2, id);
            ps.executeUpdate();
            loadSwapRequests();
            loadRevisionHistory(); // Refresh audit trail live
            JOptionPane.showMessageDialog(this, "Approved!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleRejection() {
        int row = swapTable.getSelectedRow();
        if (row == -1) return;
        int id = Integer.parseInt(swapModel.getValueAt(row, 0).toString());
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE schedule_revision_history SET status = 'REJECTED' WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            loadSwapRequests();
            loadRevisionHistory();
            JOptionPane.showMessageDialog(this, "Rejected!");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadCoverageReport() { /* Keep existing */ }
}