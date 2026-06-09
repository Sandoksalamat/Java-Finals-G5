package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class MyOvertimePanel extends UserTablePanel {

    private final JTextField date    = new JTextField(10);
    private final JTextField hours   = new JTextField(6);
    private final JTextField purpose = new JTextField(32);

    public MyOvertimePanel(UserSession s) {
        super(s, "My Overtime Requests",
            "SELECT o.id, o.overtime_date, o.requested_hours, o.approved_hours, " +
            "       o.purpose, o.status, o.admin_remarks " +
            "FROM employees e " +
            "JOIN overtime_requests o ON e.id = o.employee_id " +
            "WHERE e.user_id = ? " +
            "ORDER BY o.id DESC",

            "SELECT o.id, o.overtime_date, o.requested_hours, o.approved_hours, " +
            "       o.purpose, o.status, o.admin_remarks " +
            "FROM employees e " +
            "JOIN overtime_requests o ON e.id = o.employee_id " +
            "WHERE e.user_id = ? " +
            "  AND (o.status LIKE ? OR o.purpose LIKE ? OR CAST(o.overtime_date AS CHAR) LIKE ?) " +
            "ORDER BY o.id DESC",

            true
        );

        JButton submit = UITheme.button("SUBMIT OVERTIME REQUEST");
        submit.addActionListener(e -> submit());

        JPanel form = new JPanel();
        form.add(new JLabel("OT Date"));
        form.add(date);
        form.add(new JLabel("Hours"));
        form.add(hours);
        form.add(new JLabel("Purpose"));
        form.add(purpose);
        form.add(submit);

        add(form, BorderLayout.SOUTH);
    }

    private void submit() {
        String sql =
            "INSERT INTO overtime_requests (employee_id, overtime_date, requested_hours, purpose) " +
            "SELECT id, ?, ?, ? FROM employees WHERE user_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, date.getText().trim());
            p.setString(2, hours.getText().trim());
            p.setString(3, purpose.getText().trim());
            p.setInt(4, session.getId());
            p.executeUpdate();

            AuditService.log(session.getId(), "SUBMIT", "OVERTIME", "Filed overtime request.");
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}