@echo off
REM Script ket noi ADB voi quyen Administrator - khac phuc loi 10013

echo.
echo Dang ket noi den TV Box: 192.168.1.167:5555
echo.

D:\Apps\platform-tools\adb.exe kill-server >nul 2>&1
D:\Apps\platform-tools\adb.exe start-server >nul 2>&1
D:\Apps\platform-tools\adb.exe connect 192.168.1.167:5555

echo.
echo Kiem tra ket noi:
D:\Apps\platform-tools\adb.exe devices

echo.
echo Neu thay "unauthorized" thi bam OK tren popup TV Box!
echo.
pause
