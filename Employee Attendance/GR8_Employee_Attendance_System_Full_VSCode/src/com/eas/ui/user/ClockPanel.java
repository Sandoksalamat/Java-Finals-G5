package com.eas.ui.user;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.*;
import java.awt.*;
import java.sql.*;
import javax.swing.*;

public class ClockPanel extends UserTablePanel {

    private final JTextField locationId = new JTextField("1", 6);
    private final JTextField notes      = new JTextField(25);

    public ClockPanel(UserSession s) {
        super(s, "Time In / Time Out",
            "SELECT ar.id, ar.attendance_date, st.shift_name, ar.clock_in, ar.clock_out, " +
            "       ar.work_minutes, ar.late_minutes, ar.undertime_minutes, ar.overtime_minutes, " +
            "       ar.attendance_status, ar.remarks " +
            "FROM employees e " +
            "JOIN attendance_records ar ON e.id = ar.employee_id " +
            "LEFT JOIN shift_templates st ON ar.shift_id = st.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY ar.attendance_date DESC",

            "SELECT ar.id, ar.attendance_date, st.shift_name, ar.clock_in, ar.clock_out, " +
            "       ar.work_minutes, ar.late_minutes, ar.undertime_minutes, ar.overtime_minutes, " +
            "       ar.attendance_status, ar.remarks " +
            "FROM employees e " +
            "JOIN attendance_records ar ON e.id = ar.employee_id " +
            "LEFT JOIN shift_templates st ON ar.shift_id = st.id " +
            "WHERE e.user_id = ? " +
            "  AND (ar.attendance_status LIKE ? OR ar.remarks LIKE ? OR CAST(ar.attendance_date AS CHAR) LIKE ?) " +
            "ORDER BY ar.attendance_date DESC",

            true
        );

        JButton in  = UITheme.button("TIME IN");
        JButton out = UITheme.button("TIME OUT");
        in.addActionListener(e  -> clockIn());
        out.addActionListener(e -> clockOut());

        JPanel actions = new JPanel();
        actions.setBorder(BorderFactory.createTitledBorder("Manual Attendance Station"));
        actions.add(new JLabel("Location ID"));
        actions.add(locationId);
        actions.add(new JLabel("Notes"));
        actions.add(notes);
        actions.add(in);
        actions.add(out);

        add(actions, BorderLayout.SOUTH);
    }

    private int employeeId(Connection c) throws SQLException {
        String sql = "SELECT id FROM employees WHERE user_id = ? AND employment_status = 'ACTIVE'";

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, session.getId());

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Active employee profile was not found.");
                return r.getInt(1);
            }
        }
    }

    private int shiftId(Connection c, int emp) throws SQLException {
        String sql =
            "SELECT shift_id FROM work_schedules " +
            "WHERE employee_id = ? AND schedule_date = CURDATE() AND schedule_status = 'WORKDAY' " +
            "UNION " +
            "SELECT shift_id FROM shift_assignments " +
            "WHERE employee_id = ? AND status = 'ACTIVE' " +
            "ORDER BY shift_id LIMIT 1";

        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, emp);
            p.setInt(2, emp);

            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("No active shift schedule assigned for today.");
                return r.getInt(1);
            }
        }
    }

    private void clockIn() {
        String insertRecord =
            "INSERT INTO attendance_records " +
            "    (employee_id, attendance_date, shift_id, location_id, clock_in, " +
            "     late_minutes, attendance_status, source, remarks) " +
            "SELECT ?, CURDATE(), ?, ?, NOW(), " +
            "    GREATEST(0, TIMESTAMPDIFF(MINUTE, " +
            "        TIMESTAMP(CURDATE(), ADDTIME(start_time, SEC_TO_TIME(grace_minutes * 60))), NOW())), " +
            "    IF(NOW() > TIMESTAMP(CURDATE(), ADDTIME(start_time, SEC_TO_TIME(grace_minutes * 60))), " +
            "        'LATE', 'PRESENT'), " +
            "    'MANUAL', ? " +
            "FROM shift_templates WHERE id = ?";

        String insertLog =
            "INSERT INTO attendance_logs (attendance_id, employee_id, log_type, logged_at, notes) " +
            "VALUES (?, ?, 'TIME_IN', NOW(), ?)";

        try (Connection c = Database.getConnection()) {
            int emp   = employeeId(c);
            int shift = shiftId(c, emp);

            try (PreparedStatement p = c.prepareStatement(insertRecord, Statement.RETURN_GENERATED_KEYS)) {
                p.setInt(1, emp);
                p.setInt(2, shift);
                p.setInt(3, Integer.parseInt(locationId.getText().trim()));
                p.setString(4, notes.getText().trim());
                p.setInt(5, shift);
                p.executeUpdate();

                int attendanceId;
                try (ResultSet keys = p.getGeneratedKeys()) {
                    keys.next();
                    attendanceId = keys.getInt(1);
                }

                try (PreparedStatement log = c.prepareStatement(insertLog)) {
                    log.setInt(1, attendanceId);
                    log.setInt(2, emp);
                    log.setString(3, notes.getText().trim());
                    log.executeUpdate();
                }
            }

            AuditService.log(session.getId(), "TIME_IN", "ATTENDANCE", "Employee clocked in.");
            load("");
            Dialogs.info(this, "Time-in recorded.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }

    private void clockOut() {
        String updateRecord =
            "UPDATE attendance_records ar " +
            "JOIN shift_templates st ON ar.shift_id = st.id " +
            "SET ar.clock_out        = NOW(), " +
            "    ar.work_minutes     = GREATEST(0, TIMESTAMPDIFF(MINUTE, ar.clock_in, NOW()) - st.break_minutes), " +
            "    ar.undertime_minutes = GREATEST(0, TIMESTAMPDIFF(MINUTE, NOW(), TIMESTAMP(CURDATE(), st.end_time))), " +
            "    ar.overtime_minutes  = GREATEST(0, TIMESTAMPDIFF(MINUTE, TIMESTAMP(CURDATE(), st.end_time), NOW())) " +
            "WHERE ar.employee_id = ? " +
            "  AND ar.attendance_date = CURDATE() " +
            "  AND ar.clock_in IS NOT NULL " +
            "  AND ar.clock_out IS NULL";

        String insertLog =
            "INSERT INTO attendance_logs (attendance_id, employee_id, log_type, logged_at, notes) " +
            "SELECT id, employee_id, 'TIME_OUT', NOW(), ? " +
            "FROM attendance_records " +
            "WHERE employee_id = ? AND attendance_date = CURDATE()";

        try (Connection c = Database.getConnection()) {
            int emp = employeeId(c);

            try (PreparedStatement p = c.prepareStatement(updateRecord)) {
                p.setInt(1, emp);
                if (p.executeUpdate() == 0)
                    throw new SQLException("There is no open time-in record for today.");
            }

            try (PreparedStatement log = c.prepareStatement(insertLog)) {
                log.setString(1, notes.getText().trim());
                log.setInt(2, emp);
                log.executeUpdate();
            }

            AuditService.log(session.getId(), "TIME_OUT", "ATTENDANCE", "Employee clocked out.");
            load("");
            Dialogs.info(this, "Time-out recorded.");

        } catch (Exception ex) {
            Dialogs.error(this, ex.getMessage());
        }
    }
}