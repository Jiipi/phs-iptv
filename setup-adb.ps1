# Script tự động tải và cài ADB cho Windows
# Chạy trong PowerShell: .\setup-adb.ps1

$ADB_DIR = "D:\Apps\platform-tools"
$ADB_ZIP = "$env:TEMP\platform-tools-latest-windows.zip"
$ADB_URL = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"

Write-Host "Đang tải Android SDK Platform Tools..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $ADB_URL -OutFile $ADB_ZIP

Write-Host "Đang giải nén vào $ADB_DIR..." -ForegroundColor Cyan
Expand-Archive -Path $ADB_ZIP -DestinationPath "D:\Apps\" -Force

Write-Host "Đang thêm ADB vào PATH..." -ForegroundColor Cyan
$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$ADB_DIR*") {
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$ADB_DIR", "User")
    Write-Host "✓ Đã thêm $ADB_DIR vào PATH" -ForegroundColor Green
} else {
    Write-Host "✓ ADB đã có trong PATH" -ForegroundColor Yellow
}

Remove-Item $ADB_ZIP -Force

Write-Host "`n✓ Cài đặt hoàn tất!" -ForegroundColor Green
Write-Host "`nĐóng terminal này và mở terminal mới, sau đó chạy:" -ForegroundColor Yellow
Write-Host "  adb connect 192.168.1.167:5555" -ForegroundColor White
Write-Host "`nNhớ bấm OK trên popup xuất hiện trên TV Box!" -ForegroundColor Red
