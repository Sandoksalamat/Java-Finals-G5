#!/usr/bin/env bash
set -e
[ -f lib/mysql-connector-j-8.4.0.jar ] || { echo 'Run download_mysql_connector_driver_linux_macos.sh first.'; exit 1; }
java -cp 'dist/EmployeeAttendanceSystem.jar:lib/mysql-connector-j-8.4.0.jar' com.eas.Main
