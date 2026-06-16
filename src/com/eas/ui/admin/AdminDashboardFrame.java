
package com.eas.ui.admin;

import com.eas.auth.LoginFrame; 
import com.eas.model.UserSession; 
import com.eas.service.AuditService; 
import com.eas.util.UITheme; 
import java.awt.*; 
import javax.swing.*;

public class AdminDashboardFrame extends JFrame { 
    private final UserSession session; 

    public AdminDashboardFrame(UserSession s){
        session=s;
        setTitle("GR 5 - Employee Attendance System | Administrator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1500,850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel head=new JPanel(new BorderLayout());
        head.setBackground(UITheme.NAVY);
        head.setBorder(BorderFactory.createEmptyBorder(14,20,14,20));

        JLabel title=new JLabel("EMPLOYEE ATTENDANCE SYSTEM  |  ADMIN PORTAL");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif",Font.BOLD,18));
        head.add(title,BorderLayout.WEST);

        JButton logout=UITheme.button("LOGOUT");
        JPanel account=new JPanel();
        account.setOpaque(false);
        JLabel welcome=new JLabel("Welcome, "+s.getFullName()+"   ");
        welcome.setForeground(Color.WHITE);
        account.add(welcome);
        account.add(logout);
        head.add(account,BorderLayout.EAST);
        add(head,BorderLayout.NORTH);

        JTabbedPane t=new JTabbedPane();
        t.addTab("Dashboard",new AdminHomePanel());
        t.addTab("Accounts",new AccountManagementPanel(s));
        t.addTab("Departments",crud("Departments","DEPARTMENTS","SELECT id,department_code,department_name,manager_name,location,status FROM departments ORDER BY id","SELECT id,department_code,department_name,manager_name,location,status FROM departments WHERE department_code LIKE ? OR department_name LIKE ? OR manager_name LIKE ?","departments","INSERT INTO departments(department_code,department_name,manager_name,location,status) VALUES(?,?,?,?,?)","UPDATE departments SET department_code=?,department_name=?,manager_name=?,location=?,status=? WHERE id=?","DELETE FROM departments WHERE id=?","Code","Department Name","Manager","Location","Status"));
        t.addTab("Positions",crud("Positions","POSITIONS","SELECT id,position_code,position_title,department_id,employment_type,hourly_rate,status FROM positions ORDER BY id","SELECT id,position_code,position_title,department_id,employment_type,hourly_rate,status FROM positions WHERE position_code LIKE ? OR position_title LIKE ? OR employment_type LIKE ?","positions","INSERT INTO positions(position_code,position_title,department_id,employment_type,hourly_rate,status) VALUES(?,?,?,?,?,?)","UPDATE positions SET position_code=?,position_title=?,department_id=?,employment_type=?,hourly_rate=?,status=? WHERE id=?","DELETE FROM positions WHERE id=?","Code","Position Title","Department ID","Employment Type","Hourly Rate","Status"));
        t.addTab("Employees",crud("Employee Master Records","EMPLOYEES","SELECT id,user_id,employee_no,department_id,position_id,date_hired,birth_date,address,emergency_contact,employment_status FROM employees ORDER BY id","SELECT id,user_id,employee_no,department_id,position_id,date_hired,birth_date,address,emergency_contact,employment_status FROM employees WHERE employee_no LIKE ? OR address LIKE ? OR employment_status LIKE ?","employees","INSERT INTO employees(user_id,employee_no,department_id,position_id,date_hired,birth_date,address,emergency_contact,employment_status) VALUES(?,?,?,?,?,?,?,?,?)","UPDATE employees SET user_id=?,employee_no=?,department_id=?,position_id=?,date_hired=?,birth_date=?,address=?,emergency_contact=?,employment_status=? WHERE id=?","DELETE FROM employees WHERE id=?","User ID","Employee No","Department ID","Position ID","Date Hired","Birth Date","Address","Emergency Contact","Status"));
        t.addTab("Locations",crud("Attendance Locations","LOCATIONS","SELECT id,location_code,location_name,address,allowed_ip,status FROM attendance_locations ORDER BY id","SELECT id,location_code,location_name,address,allowed_ip,status FROM attendance_locations WHERE location_code LIKE ? OR location_name LIKE ? OR status LIKE ?","locations","INSERT INTO attendance_locations(location_code,location_name,address,allowed_ip,status) VALUES(?,?,?,?,?)","UPDATE attendance_locations SET location_code=?,location_name=?,address=?,allowed_ip=?,status=? WHERE id=?","DELETE FROM attendance_locations WHERE id=?","Code","Location Name","Address","Allowed IP","Status"));
        t.addTab("Devices",crud("Attendance Devices","DEVICES","SELECT id,location_id,device_code,device_name,device_type,ip_address,status,last_sync_at FROM biometric_devices ORDER BY id","SELECT id,location_id,device_code,device_name,device_type,ip_address,status,last_sync_at FROM biometric_devices WHERE device_code LIKE ? OR device_name LIKE ? OR status LIKE ?","devices","INSERT INTO biometric_devices(location_id,device_code,device_name,device_type,ip_address,status,last_sync_at) VALUES(?,?,?,?,?,?,?)","UPDATE biometric_devices SET location_id=?,device_code=?,device_name=?,device_type=?,ip_address=?,status=?,last_sync_at=? WHERE id=?","DELETE FROM biometric_devices WHERE id=?","Location ID","Device Code","Device Name","Type","IP Address","Status","Last Sync"));
        t.addTab("Shifts",crud("Shift Templates","SHIFTS","SELECT id,shift_code,shift_name,start_time,end_time,break_minutes,grace_minutes,work_hours,status FROM shift_templates ORDER BY id","SELECT id,shift_code,shift_name,start_time,end_time,break_minutes,grace_minutes,work_hours,status FROM shift_templates WHERE shift_code LIKE ? OR shift_name LIKE ? OR status LIKE ?","shifts","INSERT INTO shift_templates(shift_code,shift_name,start_time,end_time,break_minutes,grace_minutes,work_hours,status) VALUES(?,?,?,?,?,?,?,?)","UPDATE shift_templates SET shift_code=?,shift_name=?,start_time=?,end_time=?,break_minutes=?,grace_minutes=?,work_hours=?,status=? WHERE id=?","DELETE FROM shift_templates WHERE id=?","Shift Code","Shift Name","Start Time","End Time","Break Minutes","Grace Minutes","Work Hours","Status"));
        t.addTab("Assignments",crud("Shift Assignments","ASSIGNMENTS","SELECT id,employee_id,shift_id,effective_from,effective_to,day_pattern,status FROM shift_assignments ORDER BY id DESC","SELECT id,employee_id,shift_id,effective_from,effective_to,day_pattern,status FROM shift_assignments WHERE day_pattern LIKE ? OR status LIKE ? OR CAST(employee_id AS CHAR) LIKE ?","shift_assignments","INSERT INTO shift_assignments(employee_id,shift_id,effective_from,effective_to,day_pattern,status) VALUES(?,?,?,?,?,?)","UPDATE shift_assignments SET employee_id=?,shift_id=?,effective_from=?,effective_to=?,day_pattern=?,status=? WHERE id=?","DELETE FROM shift_assignments WHERE id=?","Employee ID","Shift ID","Effective From","Effective To","Days","Status"));
        t.addTab("Schedules",crud("Work Schedules","SCHEDULES","SELECT id,employee_id,schedule_date,shift_id,schedule_status,remarks FROM work_schedules ORDER BY schedule_date DESC","SELECT id,employee_id,schedule_date,shift_id,schedule_status,remarks FROM work_schedules WHERE schedule_status LIKE ? OR remarks LIKE ? OR CAST(employee_id AS CHAR) LIKE ?","schedules","INSERT INTO work_schedules(employee_id,schedule_date,shift_id,schedule_status,remarks) VALUES(?,?,?,?,?)","UPDATE work_schedules SET employee_id=?,schedule_date=?,shift_id=?,schedule_status=?,remarks=? WHERE id=?","DELETE FROM work_schedules WHERE id=?","Employee ID","Date","Shift ID","Schedule Status","Remarks"));
        t.addTab("Optimization", new WorkforceOptimizationPanel(s.getId()));
        t.addTab("Holidays",crud("Holiday Calendar","HOLIDAYS","SELECT id,holiday_date,holiday_name,holiday_type,multiplier,status FROM holidays ORDER BY holiday_date DESC","SELECT id,holiday_date,holiday_name,holiday_type,multiplier,status FROM holidays WHERE holiday_name LIKE ? OR holiday_type LIKE ? OR status LIKE ?","holidays","INSERT INTO holidays(holiday_date,holiday_name,holiday_type,multiplier,status) VALUES(?,?,?,?,?)","UPDATE holidays SET holiday_date=?,holiday_name=?,holiday_type=?,multiplier=?,status=? WHERE id=?","DELETE FROM holidays WHERE id=?","Holiday Date","Holiday Name","Type","Multiplier","Status"));
        t.addTab("Off-Site Approvals", new OffSiteApprovalPanel(s.getId()));
        t.addTab("Hybrid Report Logs", new HybridReportPanel());
        t.addTab("Attendance",new AttendanceManagementPanel(s));
        t.addTab("Logs",new ReadOnlyQueryPanel("Attendance Punch Logs","SELECT l.id,e.employee_no,u.full_name,l.log_type,l.logged_at,l.device_id,l.notes FROM attendance_logs l JOIN employees e ON l.employee_id=e.id JOIN users u ON e.user_id=u.id ORDER BY l.id DESC","SELECT l.id,e.employee_no,u.full_name,l.log_type,l.logged_at,l.device_id,l.notes FROM attendance_logs l JOIN employees e ON l.employee_id=e.id JOIN users u ON e.user_id=u.id WHERE e.employee_no LIKE ? OR u.full_name LIKE ? OR l.log_type LIKE ? ORDER BY l.id DESC","attendance_logs"));
        t.addTab("Leaves",new LeaveApprovalPanel(s));
        t.addTab("Leave Types",crud("Leave Types","LEAVE_TYPES","SELECT id,leave_code,leave_name,annual_credits,paid_status,status FROM leave_types ORDER BY id","SELECT id,leave_code,leave_name,annual_credits,paid_status,status FROM leave_types WHERE leave_code LIKE ? OR leave_name LIKE ? OR status LIKE ?","leave_types","INSERT INTO leave_types(leave_code,leave_name,annual_credits,paid_status,status) VALUES(?,?,?,?,?)","UPDATE leave_types SET leave_code=?,leave_name=?,annual_credits=?,paid_status=?,status=? WHERE id=?","DELETE FROM leave_types WHERE id=?","Code","Leave Name","Credits","Paid Status","Status"));
        t.addTab("Overtime",new OvertimeApprovalPanel(s));
        t.addTab("Corrections",new CorrectionApprovalPanel(s));
        t.addTab("Payroll Periods",crud("Payroll Cutoff Periods","PAYROLL","SELECT id,period_name,date_from,date_to,cutoff_status FROM payroll_periods ORDER BY id DESC","SELECT id,period_name,date_from,date_to,cutoff_status FROM payroll_periods WHERE period_name LIKE ? OR cutoff_status LIKE ? OR date_from LIKE ?","payroll_periods","INSERT INTO payroll_periods(period_name,date_from,date_to,cutoff_status) VALUES(?,?,?,?)","UPDATE payroll_periods SET period_name=?,date_from=?,date_to=?,cutoff_status=? WHERE id=?","DELETE FROM payroll_periods WHERE id=?","Period Name","Date From","Date To","Status"));
        t.addTab("Summaries",new SummaryGenerationPanel(s));
        t.addTab("Adjustments",crud("Payroll Adjustments","ADJUSTMENTS","SELECT id,payroll_period_id,employee_id,adjustment_type,amount,description,created_at FROM payslip_adjustments ORDER BY id DESC","SELECT id,payroll_period_id,employee_id,adjustment_type,amount,description,created_at FROM payslip_adjustments WHERE adjustment_type LIKE ? OR description LIKE ? OR CAST(employee_id AS CHAR) LIKE ?","adjustments","INSERT INTO payslip_adjustments(payroll_period_id,employee_id,adjustment_type,amount,description) VALUES(?,?,?,?,?)","UPDATE payslip_adjustments SET payroll_period_id=?,employee_id=?,adjustment_type=?,amount=?,description=? WHERE id=?","DELETE FROM payslip_adjustments WHERE id=?","Period ID","Employee ID","Type","Amount","Description"));
        t.addTab("Announcements",crud("Announcements","ANNOUNCEMENTS","SELECT id,title,content,target_audience,posted_by,valid_until,status FROM announcements ORDER BY id DESC","SELECT id,title,content,target_audience,posted_by,valid_until,status FROM announcements WHERE title LIKE ? OR target_audience LIKE ? OR status LIKE ?","announcements","INSERT INTO announcements(title,content,target_audience,posted_by,valid_until,status) VALUES(?,?,?,?,?,?)","UPDATE announcements SET title=?,content=?,target_audience=?,posted_by=?,valid_until=?,status=? WHERE id=?","DELETE FROM announcements WHERE id=?","Title","Content","Audience","Posted By ID","Valid Until","Status"));
        t.addTab("Messages",new AdminMessagePanel(s));
        t.addTab("Reports",new ReportsPanel());
        t.addTab("Audit Trail",new ReadOnlyQueryPanel("System Audit Logs","SELECT a.id,u.username,a.action_type,a.module_name,a.description,a.logged_at FROM audit_logs a LEFT JOIN users u ON a.user_id=u.id ORDER BY a.id DESC","SELECT a.id,u.username,a.action_type,a.module_name,a.description,a.logged_at FROM audit_logs a LEFT JOIN users u ON a.user_id=u.id WHERE u.username LIKE ? OR a.module_name LIKE ? OR a.description LIKE ? ORDER BY a.id DESC","audit_logs"));
        
        add(t,BorderLayout.CENTER);
        
        logout.addActionListener(e->{
            AuditService.log(session.getId(),"LOGOUT","AUTHENTICATION","Administrator logged out.");
            dispose();
            new LoginFrame().setVisible(true);
        });
    }

    private GenericCrudPanel crud(String title,String module,String base,String search,String export,String insert,String update,String delete,String... labels){
        return new GenericCrudPanel(session,title,module,base,search,export,insert,update,delete,labels);
    }
}