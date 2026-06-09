@echo off
if not exist lib mkdir lib
powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar' -OutFile 'lib/mysql-connector-j-8.4.0.jar'"
if exist lib\mysql-connector-j-8.4.0.jar (echo Driver downloaded successfully.) else (echo Download failed. Download Connector/J manually and place the JAR in lib.)
pause
