package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class MyCorrectionPanel extends UserTablePanel {

    private final JTextField attendanceId = new JTextField(6);
    private final JTextField date         = new JTextField(10);
    private final JTextField inTime       = new JTextField(18);
    private final JTextField outTime      = new JTextField(18);
    private final JTextField reason       = new JTextField(24);

    public MyCorrectionPanel(UserSession s) {
        super(s, "My Attendance Correction Requests",
            "SELECT c.id, c.attendance_id, c.correction_date, " +
            "       c.requested_clock_in, c.requested_clock_out, " +
            "       c.reason, c.status, c.admin_remarks " +
            "FROM employees e " +
            "JOIN attendance_corrections c ON e.id = c.employee_id " +
            "WHERE e.user_id = ? " +
            "ORDER BY c.id DESC",

            "SELECT c.id, c.attendance_id, c.correction_date, " +
            "       c.requested_clock_in, c.requested_clock_out, " +
            "       c.reason, c.status, c.admin_remarks " +
            "FROM employees e " +
            "JOIN attendance_corrections c ON e.id = c.employee_id " +
            "WHERE e.user_id = ? " +
            "  AND (c.status LIKE ? OR c.reason LIKE ? OR CAST(c.correction_date AS CHAR) LIKE ?) " +
            "ORDER BY c.id DESC",

            true
        );

        JButton submit = UITheme.button("SUBMIT");
        submit.addActionListener(e -> submit());

        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder(
            "Request Correction: times format YYYY-MM-DD HH:MM:SS"
        ));
        form.add(new JLabel("Attendance ID"));
        form.add(attendanceId);
        form.add(new JLabel("Date"));
        form.add(date);
        form.add(new JLabel("New Time In"));
        form.add(inTime);
        form.add(new JLabel("New Time Out"));
        form.add(outTime);
        form.add(reason);
        form.add(submit);

        add(form, BorderLayout.SOUTH);
    }

    private void submit() {
        String sql =
            "INSERT INTO attendance_corrections " +
            "    (employee_id, attendance_id, correction_date, requested_clock_in, requested_clock_out, reason) " +
            "SELECT id, ?, ?, ?, ?, ? FROM employees WHERE user_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, attendanceId.getText().trim());
            p.setString(2, date.getText().trim());
            p.setString(3, inTime.getText().trim());
            p.setString(4, outTime.getText().trim());
            p.setString(5, reason.getText().trim());
            p.setInt(6, session.getId());
            p.executeUpdate();

            AuditService.log(session.getId(), "SUBMIT", "CORRECTIONS", "Requested attendance correction.");
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}