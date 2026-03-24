@echo off
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
echo Starting MotoRental application...
echo Access at: http://localhost:8088
echo Press Ctrl+C to stop
call mvnw.cmd spring-boot:run
