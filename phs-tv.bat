@echo off
setlocal enabledelayedexpansion
title PHS IPTV - Share man hinh TV box

REM ===========================================================
REM  PHS IPTV - mo nhanh man hinh TV box qua adb + scrcpy
REM
REM  Cach dung:
REM     phs-tv.bat                 -> dung IP mac dinh ben duoi
REM     phs-tv.bat 192.168.1.200   -> dung IP khac
REM     phs-tv.bat usb             -> dung thiet bi dang cam USB
REM
REM  Script tu lam: ket noi adb -> mo app IPTV -> mo cua so xem man hinh.
REM
REM  Luu y: khong dung find/timeout vi neu chay tu Git Bash thi cac
REM  lenh do bi thay boi ban Unix. Dung ping.exe + for/f thay the.
REM ===========================================================

REM ---- Cau hinh ----
set "TV_IP=192.168.1.167"
set "PKG=vn.phs.iptv"
set "ACT=%PKG%/.MainActivity"
set "WINDOW=PHS IPTV - TV Box"
set "MAXSIZE=1280"

REM ---- Doc tham so dong lenh ----
set "MODE=net"
if not "%~1"=="" (
    if /i "%~1"=="usb" (
        set "MODE=usb"
    ) else (
        set "TV_IP=%~1"
    )
)
REM Them cong 5555 neu nguoi dung chi nhap IP
if "!TV_IP::=!"=="!TV_IP!" set "TV_IP=!TV_IP!:5555"

echo.
echo  ============================================
echo   PHS IPTV - Share man hinh TV box
echo  ============================================
echo.

REM ---- Tim adb ----
set "ADB="
if exist "D:\apps\platform-tools\adb.exe" set "ADB=D:\apps\platform-tools\adb.exe"
if not defined ADB if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not defined ADB for %%X in (adb.exe) do if not "%%~$PATH:X"=="" set "ADB=%%~$PATH:X"
if not defined ADB (
    echo  [LOI] Khong tim thay adb.exe
    echo        Kiem tra lai: D:\apps\platform-tools\adb.exe
    goto :FAIL
)

REM ---- Tim scrcpy (khop moi phien ban scrcpy-win64-*) ----
set "SCRCPY="
for /d %%D in ("D:\apps\scrcpy-win64-*") do if exist "%%D\scrcpy.exe" set "SCRCPY=%%D\scrcpy.exe"
if not defined SCRCPY if exist "D:\apps\scrcpy\scrcpy.exe" set "SCRCPY=D:\apps\scrcpy\scrcpy.exe"
if not defined SCRCPY for %%X in (scrcpy.exe) do if not "%%~$PATH:X"=="" set "SCRCPY=%%~$PATH:X"
if not defined SCRCPY (
    echo  [LOI] Khong tim thay scrcpy.exe
    echo        Tai ve va giai nen vao D:\apps\
    echo        https://github.com/Genymobile/scrcpy/releases
    goto :FAIL
)

REM ---- Ket noi thiet bi ----
REM scrcpy doc bien moi truong ADB de chon adb executable, nen no
REM dung chung mot adb server voi script -> khong da nhau kill-server.
set "ADB=%ADB%"
"%ADB%" start-server >nul 2>&1

if "%MODE%"=="usb" goto :USBMODE

echo  [1/3] Ket noi %TV_IP% ...
set "TARGET=%TV_IP%"
set "OK="
"%ADB%" connect %TV_IP% >nul 2>&1

REM adb qua TCP bat tay RSA cham: ngay sau connect thuong con bao
REM "unauthorized", vai giay sau moi thanh "device". Cho tu tu, tuyet
REM doi khong kill-server / disconnect giua chung vi se pha bat tay.
for /l %%I in (1,1,12) do (
    if not defined OK (
        set "STATE="
        for /f "tokens=1,2" %%S in ('""%ADB%" devices"') do (
            if "%%S"=="%TV_IP%" set "STATE=%%T"
        )
        if "!STATE!"=="device" (
            set "OK=1"
        ) else (
            if %%I==1 (
                if "!STATE!"=="unauthorized" (
                    echo        Dang cho TV cap quyen ^(neu TV hien hop thoai, bam "Allow"^)...
                ) else (
                    echo        Dang cho thiet bi san sang...
                )
            )
            REM Thu goi lai connect moi 3 vong phong khi goi dau roi mang.
            set /a "RETRY=%%I %% 3"
            if "!RETRY!"=="0" "%ADB%" connect %TV_IP% >nul 2>&1
            "%SystemRoot%\System32\ping.exe" -n 2 127.0.0.1 >nul 2>&1
        )
    )
)
if not defined OK (
    echo.
    echo  [LOI] Khong ket noi duoc %TV_IP%
    echo.
    echo        Kiem tra:
    echo         - TV box va may tinh cung mang wifi/LAN
    echo         - TV box da bat "ADB debugging" trong Developer options
    echo         - IP con dung khong ^(Settings ^> Network^)
    echo         - Neu TV hien hop thoai xin quyen: bam "Allow"/"Cho phep"
    echo.
    echo        Doi IP:  phs-tv.bat 192.168.1.xxx
    echo        Dung USB: phs-tv.bat usb
    goto :FAIL
)
echo        Ket noi OK.
goto :LAUNCH

:USBMODE
echo  [1/3] Tim thiet bi cam USB...
set "TARGET="
for /f "usebackq skip=1 tokens=1,2" %%A in (`""%ADB%" devices"`) do (
    if "%%B"=="device" if not defined TARGET set "TARGET=%%A"
)
if not defined TARGET (
    echo  [LOI] Khong co thiet bi USB nao san sang.
    echo        Bat USB debugging tren TV box roi cam lai cap.
    goto :FAIL
)
echo        Da thay: !TARGET!

:LAUNCH
REM ---- Mo app IPTV ----
echo  [2/3] Mo app %PKG% ...
set "HASPKG="
for /f "usebackq delims=" %%P in (`""%ADB%" -s !TARGET! shell pm list packages %PKG% 2^>nul"`) do set "HASPKG=%%P"
if not defined HASPKG (
    echo        [CANH BAO] App chua duoc cai tren TV box.
    echo        Cai bang: gradlew installDebug
) else (
    "%ADB%" -s !TARGET! shell am start -n %ACT% >nul 2>&1
    if errorlevel 1 (
        "%ADB%" -s !TARGET! shell monkey -p %PKG% -c android.intent.category.LAUNCHER 1 >nul 2>&1
    )
    echo        Da mo app.
)

REM ---- Mo cua so xem man hinh ----
echo  [3/3] Mo cua so xem man hinh...
echo.
echo        Dieu khien: phim mui ten = D-pad, Enter/chuot trai = OK
echo                    Esc / chuot phai = Back, Alt+F = toan man hinh
echo        Dong cua so hoac Ctrl+C o day de thoat.
echo.

"%SCRCPY%" -s !TARGET! --window-title "%WINDOW%" --max-size %MAXSIZE% --stay-awake
echo.
echo  Da dong. Hen gap lai.
endlocal
exit /b 0

:FAIL
echo.
pause
endlocal
exit /b 1
