@echo off
if not exist build\classes mkdir build\classes
dir /s /b src\*.java > build\sources.txt
javac --release 8 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 pause & exit /b 1
jar cfm dist\EmployeeAttendanceSystem.jar build\manifest.mf -C build\classes .
jar cf dist\EmployeeAttendanceSystem-SourceCode.jar -C src .
echo Build completed.
pause
