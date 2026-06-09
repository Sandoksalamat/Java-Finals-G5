#!/usr/bin/env bash
set -e
mkdir -p build/classes dist
find src -name '*.java' > build/sources.txt
javac --release 8 -encoding UTF-8 -d build/classes @build/sources.txt
jar cfm dist/EmployeeAttendanceSystem.jar build/manifest.mf -C build/classes .
jar cf dist/EmployeeAttendanceSystem-SourceCode.jar -C src .
echo Build completed.
