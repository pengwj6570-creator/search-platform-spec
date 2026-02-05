@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Building Search Platform Services
echo ========================================

set MAVEN_HOME=D:\dev\apache-maven-3.9.6
set MVN=!MAVEN_HOME!\bin\mvn.cmd

echo.
echo [1/2] Building data-sync...
cd /d D:\dev\claudecode\search-platform-spec\services\data-sync
call !MVN! clean package -DskipTests
if !ERRORLEVEL! neq 0 (
    echo [ERROR] data-sync build failed!
    exit /b 1
)
echo [OK] data-sync built successfully

echo.
echo [2/2] Building api-gateway...
cd /d D:\dev\claudecode\search-platform-spec\services\api-gateway
call !MVN! clean package -DskipTests
if !ERRORLEVEL! neq 0 (
    echo [ERROR] api-gateway build failed!
    exit /b 1
)
echo [OK] api-gateway built successfully

echo.
echo ========================================
echo All builds completed successfully!
echo ========================================

endlocal
