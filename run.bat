@echo off
title Secure File Storage System
echo ===================================================
echo   Secure File Storage System (AES-256 + RBAC)
echo   Starting local server at http://localhost:8080 ...
echo ===================================================

set "JAVA_EXE=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

start "" http://localhost:8080/

"%JAVA_EXE%" -Dspring.profiles.active=dev -Dserver.port=8080 -jar target\secure-file-storage-1.0.0.jar
pause
