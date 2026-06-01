@echo off
if not exist lib\mysql-connector-j-8.4.0.jar (
 echo JDBC driver missing. Run download_mysql_connector_driver_windows.bat first.
 pause
 exit /b 1
)
java -cp "dist\EmployeeAttendanceSystem.jar;lib\mysql-connector-j-8.4.0.jar" com.eas.Main
