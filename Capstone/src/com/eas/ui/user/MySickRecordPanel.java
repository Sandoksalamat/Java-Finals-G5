package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class MySickRecordPanel extends UserTablePanel {

    private final JTextField diagnosis      = new JTextField(20);
    private final JTextField doctor         = new JTextField(20);
    private final JTextField recoveryDays   = new JTextField(5);
    private final JTextArea  recommendation = new JTextArea(2, 30);

    public MySickRecordPanel(UserSession s) {
        super(s, "My Sick Records",
            "SELECT sr.id, sr.report_date, sr.diagnosis, sr.doctor_name, " +
            "       sr.recommendation, sr.recovery_days, sr.expected_return, " +
            "       sr.status, sr.admin_remarks " +
            "FROM employees e " +
            "JOIN sick_records sr ON e.id = sr.employee_id " +
            "WHERE e.user_id = ? " +
            "ORDER BY sr.id DESC",

            "SELECT sr.id, sr.report_date, sr.diagnosis, sr.doctor_name, " +
            "       sr.recommendation, sr.recovery_days, sr.expected_return, " +
            "       sr.status, sr.admin_remarks " +
            "FROM employees e " +
            "JOIN sick_records sr ON e.id = sr.employee_id " +
            "WHERE e.user_id = ? " +
            "  AND (sr.diagnosis LIKE ? OR sr.doctor_name LIKE ? OR sr.status LIKE ?) " +
            "ORDER BY sr.id DESC",

            true
        );

        JButton submit = UITheme.button("SUBMIT SICK RECORD");
        submit.addActionListener(e -> submit());

        JPanel form = new JPanel();
        form.setBorder(BorderFactory.createTitledBorder("Report Sick — Doctor's Details"));
        form.add(new JLabel("Diagnosis"));
        form.add(diagnosis);
        form.add(new JLabel("Doctor Name"));
        form.add(doctor);
        form.add(new JLabel("Recovery Days"));
        form.add(recoveryDays);
        form.add(new JLabel("Recommendation"));
        form.add(new JScrollPane(recommendation));
        form.add(submit);

        add(form, BorderLayout.SOUTH);
    }

    private void submit() {
        String sql =
            "INSERT INTO sick_records (employee_id, diagnosis, doctor_name, recovery_days, recommendation) " +
            "SELECT id, ?, ?, ?, ? FROM employees WHERE user_id = ?";

        try (Connection c = Database.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, diagnosis.getText().trim());
            p.setString(2, doctor.getText().trim());
            p.setInt(3,    Integer.parseInt(recoveryDays.getText().trim()));
            p.setString(4, recommendation.getText().trim());
            p.setInt(5,    session.getId());
            p.executeUpdate();

            AuditService.log(session.getId(), "SUBMIT", "SICK_RECORDS", "Filed sick record.");
            load("");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}