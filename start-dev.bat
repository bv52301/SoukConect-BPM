@echo off
echo ==============================================
echo Starting SoukConect Development Environment
echo ==============================================

echo [1/2] Launching SoukConect-API (Combined Service) on port 8082...
start "SoukConect API" cmd /k "cd /d c:\work\SoukConect-API\services\combined-service && mvn spring-boot:run"

echo [2/2] Launching SoukConect-BPM (Order Worker) on port 8090...
start "SoukConect BPM Worker" cmd /k "cd /d c:\work\Soukconect-BPM\order-worker && mvn spring-boot:run"

echo ==============================================
echo Services are starting in new windows.
echo Please wait for them to initialize (look for "Started ...").
echo.
echo Once started, you can trigger the workflow.
echo ==============================================
pause
