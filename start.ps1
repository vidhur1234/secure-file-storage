Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  Starting Secure File Storage System on http://localhost:8080/   " -ForegroundColor Green
Write-Host "  Tech: Spring Boot + Java 21 + AES-256 + SHA-256 + RBAC         " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12.1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location $PSScriptRoot
java -Dserver.port=8080 -jar target\secure-file-storage-1.0.0.jar
