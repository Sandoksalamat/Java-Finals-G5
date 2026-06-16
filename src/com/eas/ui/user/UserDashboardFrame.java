package com.eas.ui.user;

import com.eas.auth.LoginFrame;
import com.eas.model.UserSession;
import com.eas.service.AuditService;
import com.eas.util.UITheme;
import java.awt.*;
import javax.swing.*;

public class UserDashboardFrame extends JFrame {

    public UserDashboardFrame(UserSession s) {
        setTitle("GR 8 - Employee Attendance System | Employee Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1420, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(s), BorderLayout.NORTH);
        add(buildTabs(s), BorderLayout.CENTER);
    }

    private JPanel buildHeader(UserSession s) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("EMPLOYEE ATTENDANCE SYSTEM  |  EMPLOYEE PORTAL");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel account = new JPanel();
        account.setOpaque(false);
        JLabel name = new JLabel("Welcome, " + s.getFullName() + "   ");
        name.setForeground(Color.WHITE);
        JButton logout = UITheme.button("LOGOUT");
        logout.addActionListener(e -> {
            AuditService.log(s.getId(), "LOGOUT", "AUTHENTICATION", "Employee logged out.");
            dispose();
            new LoginFrame().setVisible(true);
        });
        account.add(name);
        account.add(logout);
        header.add(account, BorderLayout.EAST);

        return header;
    }

    private JTabbedPane buildTabs(UserSession s) {
        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard",     new UserHomePanel(s));
        tabs.addTab("Time In/Out",   new ClockPanel(s));
        tabs.addTab("My Attendance", buildAttendancePanel(s));
        tabs.addTab("Schedule",      buildSchedulePanel(s));
        tabs.addTab("Leave Types",   buildLeaveTypesPanel(s));
        tabs.addTab("My Leaves",     new MyLeavePanel(s));
        tabs.addTab("My Overtime",   new MyOvertimePanel(s));
        tabs.addTab("My Sick Record", new MySickRecordPanel(s));
        tabs.addTab("My Wellness Programs",   new MyWellnessPanel(s));
        tabs.addTab("My Health & Safety",     new MyHealthSafetyPanel(s));
        tabs.addTab("My RTW Clearances",      new MyRTWClearancePanel(s));
        tabs.addTab("My Medical Exams",   new MyMedicalExamPanel(s));
        tabs.addTab("Corrections",   new MyCorrectionPanel(s));
        tabs.addTab("Announcements", buildAnnouncementsPanel(s));
        tabs.addTab("Offsite Request", new OffSiteRequestPanel(s));
        tabs.addTab("Notifications", new NotificationPanel(s));
        tabs.addTab("Messages",      new UserMessagePanel(s));
        tabs.addTab("Profile",       new UserProfilePanel(s));

        return tabs;
    }

    private UserTablePanel buildAttendancePanel(UserSession s) {
        String base =
            "SELECT ar.id, ar.attendance_date, st.shift_name, ar.clock_in, ar.clock_out, " +
            "       ar.work_minutes, ar.late_minutes, ar.undertime_minutes, ar.overtime_minutes, " +
            "       ar.attendance_status, ar.remarks " +
            "FROM employees e " +
            "JOIN attendance_records ar ON e.id = ar.employee_id " +
            "LEFT JOIN shift_templates st ON ar.shift_id = st.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY ar.attendance_date DESC";

        String search = base.replace(
            "WHERE e.user_id = ?",
            "WHERE e.user_id = ? AND (ar.attendance_status LIKE ? OR ar.remarks LIKE ? OR CAST(ar.attendance_date AS CHAR) LIKE ?)"
        );

        return new UserTablePanel(s, "My Attendance History", base, search, true);
    }

    private UserTablePanel buildSchedulePanel(UserSession s) {
        String base =
            "SELECT ws.id, ws.schedule_date, st.shift_name, st.start_time, st.end_time, " +
            "       ws.schedule_status, ws.remarks " +
            "FROM employees e " +
            "JOIN work_schedules ws ON e.id = ws.employee_id " +
            "JOIN shift_templates st ON ws.shift_id = st.id " +
            "WHERE e.user_id = ? " +
            "ORDER BY ws.schedule_date DESC";

        String search = base.replace(
            "WHERE e.user_id = ?",
            "WHERE e.user_id = ? AND (st.shift_name LIKE ? OR ws.schedule_status LIKE ? OR ws.remarks LIKE ?)"
        );

        return new UserTablePanel(s, "My Shift Schedule", base, search, true);
    }

    private UserTablePanel buildLeaveTypesPanel(UserSession s) {
        String base =
            "SELECT lt.id, lt.leave_code, lt.leave_name, lt.annual_credits, lt.paid_status " +
            "FROM leave_types lt " +
            "JOIN users u ON u.id = ? " +
            "WHERE lt.status = 'ACTIVE'";

        String search = base.replace(
            "WHERE lt.status = 'ACTIVE'",
            "WHERE lt.status = 'ACTIVE' AND (lt.leave_code LIKE ? OR lt.leave_name LIKE ? OR lt.paid_status LIKE ?)"
        );

        return new UserTablePanel(s, "Available Leave Types", base, search, true);
    }

    private UserTablePanel buildAnnouncementsPanel(UserSession s) {
        String base =
            "SELECT a.id, a.title, a.content, a.posted_at, a.valid_until " +
            "FROM announcements a " +
            "JOIN users u ON u.id = ? " +
            "WHERE a.status = 'ACTIVE' AND (a.target_audience = 'ALL' OR a.target_audience = 'EMPLOYEE') " +
            "ORDER BY a.id DESC";

        String search = base.replace(
            "ORDER BY a.id DESC",
            "AND (a.title LIKE ? OR a.content LIKE ? OR CAST(a.posted_at AS CHAR) LIKE ?) ORDER BY a.id DESC"
        );

        return new UserTablePanel(s, "Announcements", base, search, true);
    }
}