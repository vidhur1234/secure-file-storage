# Secure File Storage System - 1-Click PowerShell Launcher
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "   Secure File Storage System (AES-256 + RBAC)     " -ForegroundColor Yellow
Write-Host "   Launching local server on http://localhost:8080 " -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan

$javaExe = "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    $javaExe = "java"
}

Start-Process "http://localhost:8080/"

& $javaExe "-Dspring.profiles.active=dev" "-Dserver.port=8080" -jar target\secure-file-storage-1.0.0.jar
