@echo off
title Secure File Storage System
echo =================================================================
echo   Starting Secure File Storage System on http://localhost:8080/
echo   Tech: Spring Boot + Java 21 + AES-256 + SHA-256 + RBAC
echo =================================================================
echo.
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"
java -Dserver.port=8080 -jar target\secure-file-storage-1.0.0.jar
pause
