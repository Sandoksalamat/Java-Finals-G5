package com.eas.ui.admin;

import javax.swing.*;

public class ReportsPanel extends JTabbedPane {

    public ReportsPanel() {

        addTab(
            "Roster",
            new ReadOnlyQueryPanel(
                "Employee Roster",
                "SELECT * FROM v_employee_roster",
                "SELECT * FROM v_employee_roster WHERE employee_no LIKE ? OR full_name LIKE ? OR department_name LIKE ?",
                "employee_roster"
            )
        );

        addTab(
            "Attendance",
            new ReadOnlyQueryPanel(
                "Daily Attendance",
                "SELECT * FROM v_daily_attendance ORDER BY attendance_date DESC",
                "SELECT * FROM v_daily_attendance WHERE employee_no LIKE ? OR full_name LIKE ? OR attendance_status LIKE ?",
                "daily_attendance"
            )
        );

        addTab(
            "Late/Absences",
            new ReadOnlyQueryPanel(
                "Late and Absence Summary",
                "SELECT * FROM v_late_absence_summary",
                "SELECT * FROM v_late_absence_summary WHERE employee_no LIKE ? OR full_name LIKE ? OR department_name LIKE ?",
                "late_absence_summary"
            )
        );

        addTab(
            "Leaves",
            new ReadOnlyQueryPanel(
                "Leave Monitoring",
                "SELECT * FROM v_leave_monitoring",
                "SELECT * FROM v_leave_monitoring WHERE employee_no LIKE ? OR full_name LIKE ? OR status LIKE ?",
                "leave_monitoring"
            )
        );

        addTab(
            "Overtime",
            new ReadOnlyQueryPanel(
                "Overtime Monitoring",
                "SELECT * FROM v_overtime_monitoring",
                "SELECT * FROM v_overtime_monitoring WHERE employee_no LIKE ? OR full_name LIKE ? OR status LIKE ?",
                "overtime_monitoring"
            )
        );

        addTab(
            "Payroll Summary",
            new ReadOnlyQueryPanel(
                "Payroll Attendance Summary",
                "SELECT * FROM v_payroll_attendance_summary",
                "SELECT * FROM v_payroll_attendance_summary WHERE employee_no LIKE ? OR full_name LIKE ? OR period_name LIKE ?",
                "payroll_attendance_summary"
            )
        );

        addTab("Health & Safety", new ReadOnlyQueryPanel(
            "Health & Safety Summary",
            "SELECT * FROM v_hs_incident_summary",
            "SELECT * FROM v_hs_incident_summary " +
            "WHERE employee_no LIKE ? OR full_name LIKE ? OR incident_type LIKE ? " +
            "   OR department_name LIKE ? OR incident_status LIKE ?",
            "hs_incident_summary"
        ));
    }
}