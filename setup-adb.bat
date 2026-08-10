@echo off
REM Script tự động tải và cài ADB cho Windows (batch version)
REM Chạy bằng cách double-click file này

setlocal
set ADB_DIR=D:\Apps\platform-tools
set ADB_ZIP=%TEMP%\platform-tools-latest-windows.zip
set ADB_URL=https://dl.google.com/android/repository/platform-tools-latest-windows.zip

echo.
echo ========================================
echo   Cai dat Android SDK Platform Tools
echo ========================================
echo.

if exist "%ADB_DIR%\adb.exe" (
    echo ADB da duoc cai dat tai: %ADB_DIR%
    echo.
    goto :connect
)

echo Dang tai Android SDK Platform Tools...
curl -L -o "%ADB_ZIP%" "%ADB_URL%"
if errorlevel 1 (
    echo Loi: Khong tai duoc file. Kiem tra ket noi mang.
    pause
    exit /b 1
)

echo Dang giai nen vao %ADB_DIR%...
powershell -Command "Expand-Archive -Path '%ADB_ZIP%' -DestinationPath 'D:\Apps\' -Force"
del "%ADB_ZIP%"

echo.
echo Them ADB vao PATH...
setx PATH "%PATH%;%ADB_DIR%" >nul 2>&1

echo.
echo ========================================
echo   Cai dat hoan tat!
echo ========================================
echo.

:connect
echo Huong dan ket noi TV Box:
echo.
echo 1. Bat USB Debugging tren TV Box:
echo    - Settings ^> About ^> Bam "Build number" 7 lan
echo    - Settings ^> Developer options ^> Bat "USB debugging"
echo.
echo 2. Ket noi:
echo    "%ADB_DIR%\adb.exe" connect 192.168.1.167:5555
echo.
echo 3. Bam OK tren popup hien tren TV Box
echo.
echo 4. Kiem tra:
echo    "%ADB_DIR%\adb.exe" devices
echo.
echo Nhan phim bat ky de dong cua so...
pause >nul
