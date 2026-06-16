package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class MyLeavePanel extends UserTablePanel {

    private final JTextField typeId = new JTextField(5);
    private final JTextField start  = new JTextField(10);
    private final JTextField end    = new JTextField(10);
    private final JTextField days   = new JTextField(5);
    private final JTextField reason = new JTextField(28);

    public MyLeavePanel(UserSession s) {
        super(s, "My Leave Requests",
            "SELECT l.id, lt.leave_name, l.start_date, l.end_date, " +
            "       l.total_days, l.reason, l.status, l.admin_remarks " +
            "FROM employees e " +
            "JOIN leave_requests l  ON e.id = l.employee_id " +
            "JOIN leave_types lt    ON l.leave_type_id = lt.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY l.id DESC",

            "SELECT l.id, lt.leave_name, l.start_date, l.end_date, " +
            "       l.total_days, l.reason, l.status, l.admin_remarks " +
            "FROM employees e " +
            "JOIN leave_requests l  ON e.id = l.employee_id " +
            "JOIN leave_types lt    ON l.leave_type_id = lt.id " +
            "WHERE e.user_id = ? " +
            "  AND (lt.leave_name LIKE ? OR l.status LIKE ? OR l.reason LIKE ?) " +
            "ORDER BY l.id DESC",

            true
        );

        JButton submit = UITheme.button("SUBMIT");
        JButton cancel = UITheme.button("CANCEL SELECTED PENDING");
        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> cancel());

        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder(
            "File Leave Request: Type IDs in Leave Types tab"
        ));
        form.add(new JLabel("Type ID"));
        form.add(typeId);
        form.add(new JLabel("Start"));
        form.add(start);
        form.add(new JLabel("End"));
        form.add(end);
        form.add(new JLabel("Days"));
        form.add(days);
        form.add(reason);
        form.add(submit);
        form.add(cancel);

        add(form, BorderLayout.SOUTH);
    }

    private void submit() {
        String sql =
            "INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date, total_days, reason) " +
            "SELECT id, ?, ?, ?, ?, ? FROM employees WHERE user_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, typeId.getText().trim());
            p.setString(2, start.getText().trim());
            p.setString(3, end.getText().trim());
            p.setString(4, days.getText().trim());
            p.setString(5, reason.getText().trim());
            p.setInt(6, session.getId());
            p.executeUpdate();

            AuditService.log(session.getId(), "SUBMIT", "LEAVE", "Filed leave request.");
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void cancel() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        String sql =
            "UPDATE leave_requests l " +
            "JOIN employees e ON l.employee_id = e.id " +
            "SET l.status = 'CANCELLED' " +
            "WHERE l.id = ? AND e.user_id = ? AND l.status = 'PENDING'";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, Integer.parseInt(table.getValueAt(row, 0).toString()));
            p.setInt(2, session.getId());
            p.executeUpdate();
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}