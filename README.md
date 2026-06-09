# GR 5: Employee Attendance System








































Java Swing desktop system with MySQL/XAMPP database and JDBC connection. Open this entire folder in Visual Studio Code.

## Quick Setup on Windows
1. Start **MySQL** in XAMPP.
2. Open phpMyAdmin and import `database/employee_attendance_system.sql`.
3. Run `download_mysql_connector_driver_windows.bat` once to place MySQL Connector/J in `lib`.
4. Run `run_windows.bat`, or open `src/com/eas/Main.java` in VS Code and choose **Run Java**.

## Demo Accounts
| Role | Username | Password |
|---|---|---|
| Administrator | admin | Admin@123 |
| Employee/User | employee1 | User@123 |

## Main Modules
Admin: accounts, departments, positions, employees, attendance locations/devices, shifts and assignment, schedules, holidays, daily attendance monitoring, punch logs, leave approvals, overtime approvals, correction approvals, payroll periods/summaries/adjustments, announcements, messages, reports, audit trail.

Employee/User: registration, time-in/time-out, attendance history, schedules, leave filing, overtime requests, attendance corrections, announcements, notifications, messages, profile and password maintenance.

## Technical Notes
- Compile target: Java 8 compatible bytecode (`javac --release 8`).
- Database: MySQL through Connector/J.
- Passwords: SHA-256 digest for classroom demonstration. For production, use salted password hashing such as BCrypt or Argon2.
- Time-in/out is desktop/manual station mode. Real biometric hardware integration requires vendor-specific SDK/API integration.
