@echo off
setlocal EnableExtensions EnableDelayedExpansion
title PHS - Mo nhanh man hinh TV

REM ============================================================
REM  Double-click: mo thiet bi ADB dang online, hoac TV mac dinh.
REM  Tuy chon: share-tv.bat 192.168.1.200
REM            share-tv.bat 192.168.1.200:5555
REM            share-tv.bat usb
REM ============================================================

set "DEFAULT_TV=192.168.1.167:5555"
set "WINDOW_TITLE=PHS TV - Screen Share"

REM Tim ADB.
set "ADB_EXE="
if exist "D:\Apps\platform-tools\adb.exe" set "ADB_EXE=D:\Apps\platform-tools\adb.exe"
if not defined ADB_EXE if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB_EXE=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB_EXE for %%X in (adb.exe) do if not "%%~$PATH:X"=="" set "ADB_EXE=%%~$PATH:X"

REM Tim SCRCPY.
set "SCRCPY_EXE="
for /d %%D in ("D:\Apps\scrcpy-win64-*") do if exist "%%D\scrcpy.exe" set "SCRCPY_EXE=%%D\scrcpy.exe"
if not defined SCRCPY_EXE if exist "D:\Apps\scrcpy\scrcpy.exe" set "SCRCPY_EXE=D:\Apps\scrcpy\scrcpy.exe"
if not defined SCRCPY_EXE for %%X in (scrcpy.exe) do if not "%%~$PATH:X"=="" set "SCRCPY_EXE=%%~$PATH:X"

if not defined ADB_EXE (
    echo [LOI] Khong tim thay adb.exe.
    echo       Chay setup-adb.bat truoc.
    goto :ERROR
)
if not defined SCRCPY_EXE (
    echo [LOI] Khong tim thay scrcpy.exe.
    echo       Dat scrcpy trong D:\Apps\scrcpy-win64-* hoac them vao PATH.
    goto :ERROR
)

set "ADB=%ADB_EXE%"
"%ADB_EXE%" start-server >nul 2>&1
set "TARGET="

REM Co tham so thi uu tien dung dung thiet bi duoc chi dinh.
if not "%~1"=="" (
    if /i "%~1"=="usb" goto :FIND_CONNECTED
    set "TARGET=%~1"
    if "!TARGET::=!"=="!TARGET!" set "TARGET=!TARGET!:5555"
    "%ADB_EXE%" connect !TARGET! >nul 2>&1
    goto :CHECK_TARGET
)

:FIND_CONNECTED
REM Duong nhanh: dung ngay thiet bi dang online, khong connect lai.
for /f "usebackq skip=1 tokens=1,2" %%A in (`"%ADB_EXE%" devices 2^>nul`) do (
    if "%%B"=="device" if not defined TARGET set "TARGET=%%A"
)
if defined TARGET goto :OPEN

REM Chua co thiet bi: thu TV mac dinh.
set "TARGET=%DEFAULT_TV%"
"%ADB_EXE%" connect %DEFAULT_TV% >nul 2>&1

:CHECK_TARGET
set "DEVICE_STATE="
for /f "usebackq tokens=*" %%S in (`"%ADB_EXE%" -s !TARGET! get-state 2^>nul`) do set "DEVICE_STATE=%%S"
if not "!DEVICE_STATE!"=="device" (
    REM Cho them mot nhip cho lan bat tay ADB dau tien.
    "%SystemRoot%\System32\ping.exe" -n 2 127.0.0.1 >nul 2>&1
    for /f "usebackq tokens=*" %%S in (`"%ADB_EXE%" -s !TARGET! get-state 2^>nul`) do set "DEVICE_STATE=%%S"
)
if not "!DEVICE_STATE!"=="device" (
    echo [LOI] TV chua san sang: !TARGET!
    echo       Kiem tra TV cung mang va da bat ADB debugging.
    echo       Neu TV hoi quyen, chon Always allow roi bam OK.
    goto :ERROR
)

:OPEN
echo Dang mo man hinh !TARGET! ...
if defined SHARE_TV_DRY_RUN (
    echo [OK] ADB va scrcpy da san sang.
    endlocal
    exit /b 0
)
start "" "%SCRCPY_EXE%" -s "!TARGET!" --window-title "%WINDOW_TITLE%" --max-size=1600 --max-fps=30 --video-bit-rate=8M --video-codec=h264 --no-audio --stay-awake
if errorlevel 1 (
    echo [LOI] Khong khoi dong duoc scrcpy.
    goto :ERROR
)

endlocal
exit /b 0

:ERROR
echo.
pause
endlocal
exit /b 1
