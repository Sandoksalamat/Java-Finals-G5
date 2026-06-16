
package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.service.HybridAttendanceService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class OffSiteApprovalPanel extends JPanel {
    private final HybridAttendanceService service = new HybridAttendanceService();
    private final int sessionManagerId;

    private JTable requestsTable, verifyTable;
    private DefaultTableModel modelRequests, modelVerify;

    public OffSiteApprovalPanel(int managerId) {

        this.sessionManagerId = managerId;

        setLayout(new GridLayout(2, 1, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildRequestsManagementWindow());
        add(buildAccomplishmentVerificationWindow());
        refreshDataViews();
    }

    private JPanel buildRequestsManagementWindow() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Review Active Hybrid & Duty Exception Applications"));

        modelRequests = new DefaultTableModel(new String[]{"ID", "Emp ID", "Type", "Start Date", "End Date", "Location/Client", "Status"}, 0);
        requestsTable = new JTable(modelRequests);
        panel.add(new JScrollPane(requestsTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approveBtn = new JButton("Approve Request");
        JButton rejectBtn = new JButton("Reject");
        btnRow.add(approveBtn); btnRow.add(rejectBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        approveBtn.addActionListener(e -> updateRequestState("APPROVED"));
        rejectBtn.addActionListener(e -> updateRequestState("REJECTED"));

        return panel;
    }

    private JPanel buildAccomplishmentVerificationWindow() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Audit Post-Duty Completion Reports & Material Proofs"));

        modelVerify = new DefaultTableModel(new String[]{"Emp ID", "Date Target", "Written Accomplishment Logs", "Document Reference", "Status"}, 0);
        verifyTable = new JTable(modelVerify);
        panel.add(new JScrollPane(verifyTable), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton verifyBtn = new JButton("Verify & Lock Payroll Status");
        btnRow.add(verifyBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        verifyBtn.addActionListener(e -> {
            int row = verifyTable.getSelectedRow();
            if (row != -1) {
                int empId = (int) modelVerify.getValueAt(row, 0);
                LocalDate date = LocalDate.parse(modelVerify.getValueAt(row, 1).toString());
                if (service.verifyAccomplishment(empId, date, sessionManagerId, "VERIFIED", "Approved via Dashboard context execution.")) {
                    JOptionPane.showMessageDialog(this, "Day closed and authorized for processing.");
                    refreshDataViews();
                }
            }
        });

        return panel;
    }

    private void updateRequestState(String nextState) {
        int row = requestsTable.getSelectedRow();
        if (row != -1) {
            int reqId = (int) modelRequests.getValueAt(row, 0);
            if (service.reviewOffSiteRequest(reqId, sessionManagerId, nextState, "Evaluated by administrative supervisor context window.")) {
                JOptionPane.showMessageDialog(this, "Application marked as: " + nextState);
                refreshDataViews();
            }
        }
    }

    public void refreshDataViews() {
        modelRequests.setRowCount(0);
        modelVerify.setRowCount(0);

        try (Connection conn = Database.getConnection()) {

            Statement st1 = conn.createStatement();
            ResultSet rs1 = st1.executeQuery("SELECT id, employee_id, request_type, start_date, end_date, destination_or_location, status FROM offsite_requests WHERE status='PENDING'");
            while (rs1.next()) {
                modelRequests.addRow(new Object[]{rs1.getInt("id"), rs1.getInt("employee_id"), rs1.getString("request_type"), rs1.getDate("start_date"), rs1.getDate("end_date"), rs1.getString("destination_or_location"), rs1.getString("status")});
            }

            Statement st2 = conn.createStatement();
            ResultSet rs2 = st2.executeQuery("SELECT employee_id, attendance_date, accomplishment_text, document_path, verification_status FROM offsite_accomplishments WHERE verification_status='PENDING'");
            while (rs2.next()) {
                modelVerify.addRow(new Object[]{rs2.getInt("employee_id"), rs2.getDate("attendance_date"), rs2.getString("accomplishment_text"), rs2.getString("document_path"), rs2.getString("verification_status")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}