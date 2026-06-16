-- GR 8: EMPLOYEE ATTENDANCE SYSTEM
-- MySQL / XAMPP Database Script
DROP DATABASE IF EXISTS employee_attendance_system;
CREATE DATABASE employee_attendance_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE employee_attendance_system;

CREATE TABLE users (
 id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, password_hash CHAR(64) NOT NULL,
 role ENUM('ADMIN','EMPLOYEE') NOT NULL DEFAULT 'EMPLOYEE', full_name VARCHAR(120) NOT NULL,
 email VARCHAR(120) NOT NULL UNIQUE, phone VARCHAR(30), status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE departments (
 id INT AUTO_INCREMENT PRIMARY KEY, department_code VARCHAR(20) NOT NULL UNIQUE, department_name VARCHAR(100) NOT NULL,
 manager_name VARCHAR(120), location VARCHAR(120), status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
);
CREATE TABLE positions (
 id INT AUTO_INCREMENT PRIMARY KEY, position_code VARCHAR(20) NOT NULL UNIQUE, position_title VARCHAR(100) NOT NULL,
 department_id INT, employment_type ENUM('REGULAR','CONTRACTUAL','PART_TIME','PROBATIONARY') DEFAULT 'REGULAR',
 hourly_rate DECIMAL(10,2) DEFAULT 0, status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
 FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);
CREATE TABLE employees (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT UNIQUE, employee_no VARCHAR(30) NOT NULL UNIQUE,
 department_id INT, position_id INT, date_hired DATE, birth_date DATE, address VARCHAR(255), emergency_contact VARCHAR(150),
 employment_status ENUM('ACTIVE','ON_LEAVE','RESIGNED','TERMINATED') DEFAULT 'ACTIVE',
 FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL,
 FOREIGN KEY(department_id) REFERENCES departments(id) ON DELETE SET NULL,
 FOREIGN KEY(position_id) REFERENCES positions(id) ON DELETE SET NULL
);
CREATE TABLE attendance_locations (
 id INT AUTO_INCREMENT PRIMARY KEY, location_code VARCHAR(20) UNIQUE NOT NULL, location_name VARCHAR(120) NOT NULL,
 address VARCHAR(255), allowed_ip VARCHAR(80), status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
);
CREATE TABLE biometric_devices (
 id INT AUTO_INCREMENT PRIMARY KEY, location_id INT, device_code VARCHAR(30) UNIQUE NOT NULL, device_name VARCHAR(100),
 device_type ENUM('MANUAL','BIOMETRIC','RFID','QR') DEFAULT 'MANUAL', ip_address VARCHAR(60),
 status ENUM('ONLINE','OFFLINE','MAINTENANCE') DEFAULT 'ONLINE', last_sync_at DATETIME,
 FOREIGN KEY(location_id) REFERENCES attendance_locations(id) ON DELETE SET NULL
);
CREATE TABLE shift_templates (
 id INT AUTO_INCREMENT PRIMARY KEY, shift_code VARCHAR(20) UNIQUE NOT NULL, shift_name VARCHAR(100) NOT NULL,
 start_time TIME NOT NULL, end_time TIME NOT NULL, break_minutes INT DEFAULT 60, grace_minutes INT DEFAULT 10,
 work_hours DECIMAL(5,2) DEFAULT 8.00, status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
);
CREATE TABLE shift_assignments (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, shift_id INT NOT NULL, effective_from DATE NOT NULL,
 effective_to DATE, day_pattern VARCHAR(50) DEFAULT 'MON-FRI', status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(shift_id) REFERENCES shift_templates(id) ON DELETE RESTRICT
);
CREATE TABLE work_schedules (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, schedule_date DATE NOT NULL, shift_id INT NOT NULL,
 schedule_status ENUM('WORKDAY','REST_DAY','HOLIDAY','ON_LEAVE') DEFAULT 'WORKDAY', remarks VARCHAR(255),
 UNIQUE KEY uq_employee_schedule(employee_id,schedule_date),
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(shift_id) REFERENCES shift_templates(id) ON DELETE RESTRICT
);
CREATE TABLE holidays (
 id INT AUTO_INCREMENT PRIMARY KEY, holiday_date DATE UNIQUE NOT NULL, holiday_name VARCHAR(120) NOT NULL,
 holiday_type ENUM('REGULAR','SPECIAL_NON_WORKING','COMPANY') DEFAULT 'REGULAR', multiplier DECIMAL(4,2) DEFAULT 1.00,
 status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
);
CREATE TABLE attendance_records (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, attendance_date DATE NOT NULL, shift_id INT,
 location_id INT, clock_in DATETIME, clock_out DATETIME, work_minutes INT DEFAULT 0, late_minutes INT DEFAULT 0,
 undertime_minutes INT DEFAULT 0, overtime_minutes INT DEFAULT 0,
 attendance_status ENUM('PRESENT','LATE','ABSENT','ON_LEAVE','REST_DAY','HOLIDAY','CORRECTED') DEFAULT 'PRESENT',
 source ENUM('MANUAL','BIOMETRIC','RFID','QR') DEFAULT 'MANUAL', remarks VARCHAR(255),
 UNIQUE KEY uq_daily_attendance(employee_id,attendance_date),
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(shift_id) REFERENCES shift_templates(id) ON DELETE SET NULL,
 FOREIGN KEY(location_id) REFERENCES attendance_locations(id) ON DELETE SET NULL
);
CREATE TABLE attendance_logs (
 id INT AUTO_INCREMENT PRIMARY KEY, attendance_id INT, employee_id INT NOT NULL, log_type ENUM('TIME_IN','TIME_OUT','ADJUSTMENT') NOT NULL,
 logged_at DATETIME NOT NULL, device_id INT, notes VARCHAR(255),
 FOREIGN KEY(attendance_id) REFERENCES attendance_records(id) ON DELETE SET NULL,
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(device_id) REFERENCES biometric_devices(id) ON DELETE SET NULL
);
CREATE TABLE attendance_corrections (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, attendance_id INT, correction_date DATE NOT NULL,
 requested_clock_in DATETIME, requested_clock_out DATETIME, reason VARCHAR(255) NOT NULL,
 status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING', reviewed_by INT, reviewed_at DATETIME, admin_remarks VARCHAR(255),
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(attendance_id) REFERENCES attendance_records(id) ON DELETE SET NULL,
 FOREIGN KEY(reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE leave_types (
 id INT AUTO_INCREMENT PRIMARY KEY, leave_code VARCHAR(20) UNIQUE NOT NULL, leave_name VARCHAR(100) NOT NULL,
 annual_credits DECIMAL(5,2) DEFAULT 0, paid_status ENUM('PAID','UNPAID') DEFAULT 'PAID', status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
);
CREATE TABLE leave_requests (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, leave_type_id INT NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
 total_days DECIMAL(5,2) NOT NULL, reason VARCHAR(255), status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING',
 filed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, reviewed_by INT, reviewed_at DATETIME, admin_remarks VARCHAR(255),
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(leave_type_id) REFERENCES leave_types(id) ON DELETE RESTRICT,
 FOREIGN KEY(reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE overtime_requests (
 id INT AUTO_INCREMENT PRIMARY KEY, employee_id INT NOT NULL, overtime_date DATE NOT NULL, requested_hours DECIMAL(5,2) NOT NULL,
 purpose VARCHAR(255), status ENUM('PENDING','APPROVED','REJECTED','CANCELLED') DEFAULT 'PENDING', approved_hours DECIMAL(5,2) DEFAULT 0,
 reviewed_by INT, reviewed_at DATETIME, admin_remarks VARCHAR(255),
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE,
 FOREIGN KEY(reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE payroll_periods (
 id INT AUTO_INCREMENT PRIMARY KEY, period_name VARCHAR(80) NOT NULL, date_from DATE NOT NULL, date_to DATE NOT NULL,
 cutoff_status ENUM('OPEN','CLOSED','POSTED') DEFAULT 'OPEN', UNIQUE KEY uq_payroll_period(date_from,date_to)
);
CREATE TABLE attendance_summaries (
 id INT AUTO_INCREMENT PRIMARY KEY, payroll_period_id INT NOT NULL, employee_id INT NOT NULL, present_days DECIMAL(5,2) DEFAULT 0,
 absent_days DECIMAL(5,2) DEFAULT 0, leave_days DECIMAL(5,2) DEFAULT 0, late_minutes INT DEFAULT 0,
 undertime_minutes INT DEFAULT 0, overtime_hours DECIMAL(7,2) DEFAULT 0, generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_summary(payroll_period_id,employee_id),
 FOREIGN KEY(payroll_period_id) REFERENCES payroll_periods(id) ON DELETE CASCADE,
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE
);
CREATE TABLE payslip_adjustments (
 id INT AUTO_INCREMENT PRIMARY KEY, payroll_period_id INT NOT NULL, employee_id INT NOT NULL,
 adjustment_type ENUM('LATE_DEDUCTION','UNDERTIME_DEDUCTION','OVERTIME_PAY','ALLOWANCE','OTHER') NOT NULL,
 amount DECIMAL(12,2) NOT NULL, description VARCHAR(255), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(payroll_period_id) REFERENCES payroll_periods(id) ON DELETE CASCADE,
 FOREIGN KEY(employee_id) REFERENCES employees(id) ON DELETE CASCADE
);
CREATE TABLE announcements (
 id INT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(150) NOT NULL, content TEXT NOT NULL,
 target_audience ENUM('ALL','ADMIN','EMPLOYEE') DEFAULT 'ALL', posted_by INT, posted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 valid_until DATE, status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE', FOREIGN KEY(posted_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE messages (
 id INT AUTO_INCREMENT PRIMARY KEY, sender_id INT NOT NULL, receiver_id INT NOT NULL, subject VARCHAR(150) NOT NULL,
 message_body TEXT NOT NULL, sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, read_status ENUM('UNREAD','READ') DEFAULT 'UNREAD',
 FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE notifications (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, title VARCHAR(150) NOT NULL, message_body TEXT NOT NULL,
 notification_type VARCHAR(40) DEFAULT 'SYSTEM', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 read_status ENUM('UNREAD','READ') DEFAULT 'UNREAD', FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE TABLE audit_logs (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, action_type VARCHAR(50) NOT NULL, module_name VARCHAR(80) NOT NULL,
 description VARCHAR(255), logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE TABLE system_settings (
 id INT AUTO_INCREMENT PRIMARY KEY, setting_key VARCHAR(80) UNIQUE NOT NULL, setting_value VARCHAR(255), description VARCHAR(255)
);

CREATE INDEX idx_attendance_date ON attendance_records(attendance_date);
CREATE INDEX idx_leave_status ON leave_requests(status);
CREATE INDEX idx_overtime_status ON overtime_requests(status);
CREATE INDEX idx_corrections_status ON attendance_corrections(status);
CREATE INDEX idx_employee_name ON employees(employee_no);

INSERT INTO users(username,password_hash,role,full_name,email,phone,status) VALUES
('admin','e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7','ADMIN','System Administrator','admin@company.local','09170000001','ACTIVE'),
('employee1','3e7c19576488862816f13b512cacf3e4ba97dd97243ea0bd6a2ad1642d86ba72','EMPLOYEE','Juan Dela Cruz','employee1@company.local','09170000002','ACTIVE');
INSERT INTO departments(department_code,department_name,manager_name,location) VALUES
('HR','Human Resources','Maria Santos','Main Office'),('IT','Information Technology','Carlo Reyes','2nd Floor'),('ACC','Accounting','Liza Cruz','Main Office');
INSERT INTO positions(position_code,position_title,department_id,employment_type,hourly_rate) VALUES
('HR-ASST','HR Assistant',1,'REGULAR',120.00),('IT-TECH','IT Support Technician',2,'REGULAR',150.00),('ACC-CLK','Accounting Clerk',3,'REGULAR',125.00);
INSERT INTO employees(user_id,employee_no,department_id,position_id,date_hired,address,emergency_contact) VALUES
(2,'EMP-0001',2,2,'2025-06-01','Manila, Philippines','Ana Dela Cruz - 09171234567');
INSERT INTO attendance_locations(location_code,location_name,address,allowed_ip) VALUES ('MAIN','Main Office Attendance Station','Main Building Lobby','192.168.1.%'),('ITLAB','IT Office Station','Second Floor','192.168.2.%');
INSERT INTO biometric_devices(location_id,device_code,device_name,device_type,status) VALUES (1,'DEV-MANUAL-01','Manual Desktop Login','MANUAL','ONLINE');
INSERT INTO shift_templates(shift_code,shift_name,start_time,end_time,break_minutes,grace_minutes,work_hours) VALUES
('DAY','Regular Day Shift','08:00:00','17:00:00',60,10,8.00),('MID','Mid Shift','10:00:00','19:00:00',60,10,8.00);
INSERT INTO shift_assignments(employee_id,shift_id,effective_from,day_pattern) VALUES (1,1,'2026-01-01','MON-FRI');
INSERT INTO work_schedules(employee_id,schedule_date,shift_id,schedule_status,remarks) VALUES (1,CURDATE(),1,'WORKDAY','Regular schedule');
INSERT INTO leave_types(leave_code,leave_name,annual_credits,paid_status) VALUES ('VL','Vacation Leave',15,'PAID'),('SL','Sick Leave',15,'PAID'),('EL','Emergency Leave',5,'PAID'),('LWOP','Leave Without Pay',0,'UNPAID');
INSERT INTO holidays(holiday_date,holiday_name,holiday_type,multiplier) VALUES ('2026-06-12','Independence Day','REGULAR',2.00);
INSERT INTO payroll_periods(period_name,date_from,date_to) VALUES ('May 16-31, 2026','2026-05-16','2026-05-31');
INSERT INTO announcements(title,content,target_audience,posted_by,valid_until) VALUES ('Attendance Reminder','Please clock in and clock out using the assigned attendance station.','ALL',1,'2026-12-31');
INSERT INTO system_settings(setting_key,setting_value,description) VALUES ('company_name','GR 8 Demo Corporation','Displayed company name'),('default_grace_minutes','10','Allowed late grace period');

DELIMITER $$
CREATE TRIGGER trg_leave_created AFTER INSERT ON leave_requests FOR EACH ROW
BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type)
 SELECT user_id,'Leave Request Filed',CONCAT('Your leave request #',NEW.id,' is pending approval.'),'LEAVE' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
END$$
CREATE TRIGGER trg_leave_reviewed AFTER UPDATE ON leave_requests FOR EACH ROW
BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Leave Request Updated',CONCAT('Your leave request #',NEW.id,' status is ',NEW.status,'.'),'LEAVE' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END$$
CREATE TRIGGER trg_overtime_reviewed AFTER UPDATE ON overtime_requests FOR EACH ROW
BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Overtime Request Updated',CONCAT('Your overtime request #',NEW.id,' status is ',NEW.status,'.'),'OVERTIME' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END$$
CREATE TRIGGER trg_correction_reviewed AFTER UPDATE ON attendance_corrections FOR EACH ROW
BEGIN
 IF NEW.status <> OLD.status THEN
  INSERT INTO notifications(user_id,title,message_body,notification_type)
  SELECT user_id,'Attendance Correction Updated',CONCAT('Your attendance correction #',NEW.id,' status is ',NEW.status,'.'),'CORRECTION' FROM employees WHERE id=NEW.employee_id AND user_id IS NOT NULL;
 END IF;
END$$
CREATE TRIGGER trg_announcement_notice AFTER INSERT ON announcements FOR EACH ROW
BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type)
 SELECT id,NEW.title,NEW.content,'ANNOUNCEMENT' FROM users WHERE status='ACTIVE' AND (NEW.target_audience='ALL' OR role=NEW.target_audience);
END$$
CREATE TRIGGER trg_message_notice AFTER INSERT ON messages FOR EACH ROW
BEGIN
 INSERT INTO notifications(user_id,title,message_body,notification_type) VALUES (NEW.receiver_id,CONCAT('New Message: ',NEW.subject),NEW.message_body,'MESSAGE');
END$$
DELIMITER ;

CREATE VIEW v_employee_roster AS
SELECT e.id,e.employee_no,u.full_name,u.email,d.department_name,p.position_title,e.date_hired,e.employment_status
FROM employees e JOIN users u ON e.user_id=u.id LEFT JOIN departments d ON e.department_id=d.id LEFT JOIN positions p ON e.position_id=p.id;
CREATE VIEW v_daily_attendance AS
SELECT ar.id,ar.attendance_date,e.employee_no,u.full_name,d.department_name,s.shift_name,ar.clock_in,ar.clock_out,ar.late_minutes,ar.undertime_minutes,ar.overtime_minutes,ar.attendance_status
FROM attendance_records ar JOIN employees e ON ar.employee_id=e.id JOIN users u ON e.user_id=u.id LEFT JOIN departments d ON e.department_id=d.id LEFT JOIN shift_templates s ON ar.shift_id=s.id;
CREATE VIEW v_late_absence_summary AS
SELECT e.employee_no,u.full_name,d.department_name,COUNT(CASE WHEN ar.attendance_status='ABSENT' THEN 1 END) absent_days,SUM(ar.late_minutes) late_minutes,SUM(ar.undertime_minutes) undertime_minutes
FROM employees e JOIN users u ON e.user_id=u.id LEFT JOIN departments d ON e.department_id=d.id LEFT JOIN attendance_records ar ON e.id=ar.employee_id GROUP BY e.id,e.employee_no,u.full_name,d.department_name;
CREATE VIEW v_leave_monitoring AS
SELECT lr.id,e.employee_no,u.full_name,lt.leave_name,lr.start_date,lr.end_date,lr.total_days,lr.reason,lr.status,lr.filed_at
FROM leave_requests lr JOIN employees e ON lr.employee_id=e.id JOIN users u ON e.user_id=u.id JOIN leave_types lt ON lr.leave_type_id=lt.id;
CREATE VIEW v_overtime_monitoring AS
SELECT o.id,e.employee_no,u.full_name,o.overtime_date,o.requested_hours,o.approved_hours,o.purpose,o.status
FROM overtime_requests o JOIN employees e ON o.employee_id=e.id JOIN users u ON e.user_id=u.id;
CREATE VIEW v_payroll_attendance_summary AS
SELECT pp.period_name,e.employee_no,u.full_name,s.present_days,s.absent_days,s.leave_days,s.late_minutes,s.undertime_minutes,s.overtime_hours
FROM attendance_summaries s JOIN payroll_periods pp ON s.payroll_period_id=pp.id JOIN employees e ON s.employee_id=e.id JOIN users u ON e.user_id=u.id;
