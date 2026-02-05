# PowerShell build script for Search Platform Services
$ErrorActionPreference = "Continue"

Write-Host "========================================"  -ForegroundColor Cyan
Write-Host "Building Search Platform Services" -ForegroundColor Cyan
Write-Host "========================================"  -ForegroundColor Cyan

$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.2"
$MVN = "D:\dev\apache-maven-3.8.3\bin\mvn.cmd"

# Build config-repo first
Write-Host "`n[0/3] Building config-repo..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\repositories\config-repo"
& $MVN clean install -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] config-repo build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] config-repo built successfully" -ForegroundColor Green
Pop-Location

# Build data-sync
Write-Host "`n[1/3] Building data-sync..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\services\data-sync"
& $MVN clean package spring-boot:repackage -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] data-sync build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] data-sync built successfully" -ForegroundColor Green
Pop-Location

# Build api-gateway
Write-Host "`n[2/3] Building api-gateway..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\services\api-gateway"
& $MVN clean package spring-boot:repackage -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] api-gateway build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] api-gateway built successfully" -ForegroundColor Green
Pop-Location

# Build vector-service
Write-Host "`n[3/3] Building vector-service..." -ForegroundColor Yellow
Push-Location "D:\dev\claudecode\search-platform-spec\services\vector-service"
& $MVN clean package spring-boot:repackage -DskipTests 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] vector-service build failed!" -ForegroundColor Red
    Pop-Location
    exit 1
}
Write-Host "[OK] vector-service built successfully" -ForegroundColor Green
Pop-Location

Write-Host "`n========================================"  -ForegroundColor Cyan
Write-Host "All builds completed successfully!" -ForegroundColor Green
Write-Host "========================================"  -ForegroundColor Cyan

# List built JARs
Write-Host "`nBuilt JARs:" -ForegroundColor Cyan
Get-ChildItem -Path "D:\dev\claudecode\search-platform-spec\services" -Recurse -Filter "*-1.0.0.jar" | Where-Object { $_.Name -notlike "*sources*" } | ForEach-Object {
    Write-Host "  - $($_.Name)" -ForegroundColor Gray
}
