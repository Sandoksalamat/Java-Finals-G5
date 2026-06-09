package com.eas.ui.admin;

import com.eas.config.Database;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.Dialogs;
import com.eas.util.UITheme;
import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SummaryGenerationPanel extends ReadOnlyQueryPanel {
    private final UserSession session;
    private final JTextField periodId = new JTextField(8);

    public SummaryGenerationPanel(UserSession session) {
        super("Payroll Attendance Summary Generation",
            "SELECT s.id,p.period_name,e.employee_no,u.full_name,s.present_days,s.absent_days,s.leave_days,s.late_minutes,s.undertime_minutes,s.overtime_hours,s.generated_at FROM attendance_summaries s JOIN payroll_periods p ON s.payroll_period_id=p.id JOIN employees e ON s.employee_id=e.id JOIN users u ON e.user_id=u.id ORDER BY s.id DESC",
            "SELECT s.id,p.period_name,e.employee_no,u.full_name,s.present_days,s.absent_days,s.leave_days,s.late_minutes,s.undertime_minutes,s.overtime_hours,s.generated_at FROM attendance_summaries s JOIN payroll_periods p ON s.payroll_period_id=p.id JOIN employees e ON s.employee_id=e.id JOIN users u ON e.user_id=u.id WHERE p.period_name LIKE ? OR e.employee_no LIKE ? OR u.full_name LIKE ? ORDER BY s.id DESC",
            "attendance_summaries");
        this.session = session;
        JPanel panel = new JPanel();
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Compute Summary from Attendance Records"));
        panel.add(new JLabel("Payroll Period ID"));
        panel.add(periodId);
        JButton generate = UITheme.button("GENERATE / RECOMPUTE");
        panel.add(generate);
        add(panel, BorderLayout.SOUTH);
        generate.addActionListener(event -> generate());
    }

    private void generate() {
        String sql = "INSERT INTO attendance_summaries(payroll_period_id,employee_id,present_days,absent_days,leave_days,late_minutes,undertime_minutes,overtime_hours) " +
            "SELECT p.id,e.id," +
            "SUM(CASE WHEN a.attendance_status IN ('PRESENT','LATE','CORRECTED') THEN 1 ELSE 0 END)," +
            "SUM(CASE WHEN a.attendance_status='ABSENT' THEN 1 ELSE 0 END)," +
            "SUM(CASE WHEN a.attendance_status='ON_LEAVE' THEN 1 ELSE 0 END)," +
            "COALESCE(SUM(a.late_minutes),0),COALESCE(SUM(a.undertime_minutes),0),COALESCE(SUM(a.overtime_minutes),0)/60 " +
            "FROM payroll_periods p CROSS JOIN employees e LEFT JOIN attendance_records a ON a.employee_id=e.id AND a.attendance_date BETWEEN p.date_from AND p.date_to " +
            "WHERE p.id=? AND e.employment_status IN ('ACTIVE','ON_LEAVE') GROUP BY p.id,e.id " +
            "ON DUPLICATE KEY UPDATE present_days=VALUES(present_days),absent_days=VALUES(absent_days),leave_days=VALUES(leave_days),late_minutes=VALUES(late_minutes),undertime_minutes=VALUES(undertime_minutes),overtime_hours=VALUES(overtime_hours),generated_at=CURRENT_TIMESTAMP";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Integer.parseInt(periodId.getText().trim()));
            int rows = statement.executeUpdate();
            AuditService.log(session.getId(), "GENERATE", "SUMMARIES", "Generated attendance summaries for payroll period " + periodId.getText().trim());
            load("");
            Dialogs.info(this, "Summary records generated or recomputed: " + rows);
        } catch (Exception exception) {
            Dialogs.error(this, exception.getMessage());
        }
    }
}
