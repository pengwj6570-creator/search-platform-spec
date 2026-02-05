# PowerShell build script for Search Platform Services
$ErrorActionPreference = "Continue"

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "Building Search Platform Services" -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan

$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$MVN = "D:\dev\apache-maven-3.8.3\bin\mvn.cmd"

# Build data-sync
Write-Host "`n[1/2] Building data-sync..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\services\data-sync"
& $MVN clean package -DskipTests 2>&1 | Tee-Object -Variable dataSyncOutput
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] data-sync build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] data-sync built successfully" -ForegroundColor Green
Pop-Location

# Build api-gateway
Write-Host "`n[2/2] Building api-gateway..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\services\api-gateway"
& $MVN clean package -DskipTests 2>&1 | Tee-Object -Variable apiGatewayOutput
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] api-gateway build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] api-gateway built successfully" -ForegroundColor Green
Pop-Location

Write-Host "`n========================================"  -ForegroundColor Cyan
Write-Host "All builds completed successfully!" -ForegroundColor Green
Write-Host "========================================"  -ForegroundColor Cyan
