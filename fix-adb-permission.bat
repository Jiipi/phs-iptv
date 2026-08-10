@echo off
REM Chay file nay voi quyen Administrator de fix loi ADB 10013

echo.
echo ========================================
echo   Fix loi ADB permission denied (10013)
echo ========================================
echo.

REM Kiem tra quyen admin
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo LOI: Can chay voi quyen Administrator!
    echo.
    echo Cach chay:
    echo 1. Chuot phai vao file nay
    echo 2. Chon "Run as administrator"
    echo.
    pause
    exit /b 1
)

echo Dang them rule cho ADB trong Windows Firewall...
netsh advfirewall firewall add rule name="ADB TCP" dir=out action=allow protocol=TCP localport=5555 >nul 2>&1
netsh advfirewall firewall add rule name="ADB TCP In" dir=in action=allow protocol=TCP localport=5555 >nul 2>&1

echo.
echo ✓ Da them rule thanh cong!
echo.
echo Bay gio chay lenh:
echo   D:\Apps\platform-tools\adb.exe connect 192.168.1.167:5555
echo.
echo Nho bam OK tren popup hien tren TV Box!
echo.
pause
