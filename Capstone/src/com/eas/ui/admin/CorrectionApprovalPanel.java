package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;
import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CorrectionApprovalPanel extends ReadOnlyQueryPanel {
    private final UserSession session;
    private final JTextField remarks = new JTextField(30);

    public CorrectionApprovalPanel(UserSession session) {
        super("Attendance Correction Processing",
            "SELECT c.id,e.employee_no,u.full_name,c.attendance_id,c.correction_date,c.requested_clock_in,c.requested_clock_out,c.reason,c.status,c.admin_remarks FROM attendance_corrections c JOIN employees e ON c.employee_id=e.id JOIN users u ON e.user_id=u.id ORDER BY c.id DESC",
            "SELECT c.id,e.employee_no,u.full_name,c.attendance_id,c.correction_date,c.requested_clock_in,c.requested_clock_out,c.reason,c.status,c.admin_remarks FROM attendance_corrections c JOIN employees e ON c.employee_id=e.id JOIN users u ON e.user_id=u.id WHERE e.employee_no LIKE ? OR u.full_name LIKE ? OR c.status LIKE ? ORDER BY c.id DESC",
            "attendance_corrections");
        this.session = session;
        JPanel panel = new JPanel();
        panel.add(new JLabel("Admin Remarks"));
        panel.add(remarks);
        JButton approve = UITheme.button("APPROVE AND APPLY");
        JButton reject = UITheme.button("REJECT");
        panel.add(approve);
        panel.add(reject);
        add(panel, BorderLayout.SOUTH);
        approve.addActionListener(event -> approve());
        reject.addActionListener(event -> reject());
    }

    private int selectedId() {
        int row = table.getSelectedRow();
        return row < 0 ? -1 : Integer.parseInt(table.getValueAt(row, 0).toString());
    }

    private void approve() {
        int correctionId = selectedId();
        if (correctionId < 0) {
            Dialogs.error(this, "Select a correction request.");
            return;
        }
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int attendanceId;
                java.sql.Timestamp requestedIn;
                java.sql.Timestamp requestedOut;
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT attendance_id,requested_clock_in,requested_clock_out FROM attendance_corrections WHERE id=? AND status='PENDING'")) {
                    query.setInt(1, correctionId);
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) throw new SQLException("Only pending requests can be approved.");
                        attendanceId = result.getInt("attendance_id");
                        if (result.wasNull()) throw new SQLException("Correction request is not connected to an attendance record.");
                        requestedIn = result.getTimestamp("requested_clock_in");
                        requestedOut = result.getTimestamp("requested_clock_out");
                    }
                }
                try (PreparedStatement attendance = connection.prepareStatement(
                        "UPDATE attendance_records SET clock_in=?,clock_out=?,attendance_status='CORRECTED',remarks=? WHERE id=?")) {
                    attendance.setTimestamp(1, requestedIn);
                    attendance.setTimestamp(2, requestedOut);
                    attendance.setString(3, "Corrected by administrator: " + remarks.getText().trim());
                    attendance.setInt(4, attendanceId);
                    attendance.executeUpdate();
                }
                try (PreparedStatement correction = connection.prepareStatement(
                        "UPDATE attendance_corrections SET status='APPROVED',reviewed_by=?,reviewed_at=NOW(),admin_remarks=? WHERE id=?")) {
                    correction.setInt(1, session.getId());
                    correction.setString(2, remarks.getText().trim());
                    correction.setInt(3, correctionId);
                    correction.executeUpdate();
                }
                connection.commit();
                AuditService.log(session.getId(), "APPROVE", "CORRECTIONS", "Applied correction #" + correctionId);
                load("");
                Dialogs.info(this, "Correction approved and applied.");
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            Dialogs.error(this, exception.getMessage());
        }
    }

    private void reject() {
        int correctionId = selectedId();
        if (correctionId < 0) {
            Dialogs.error(this, "Select a correction request.");
            return;
        }
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE attendance_corrections SET status='REJECTED',reviewed_by=?,reviewed_at=NOW(),admin_remarks=? WHERE id=? AND status='PENDING'")) {
            statement.setInt(1, session.getId());
            statement.setString(2, remarks.getText().trim());
            statement.setInt(3, correctionId);
            if (statement.executeUpdate() == 0) throw new SQLException("Only pending requests can be rejected.");
            AuditService.log(session.getId(), "REJECT", "CORRECTIONS", "Rejected correction #" + correctionId);
            load("");
            Dialogs.info(this, "Correction request rejected.");
        } catch (Exception exception) {
            Dialogs.error(this, exception.getMessage());
        }
    }
}
