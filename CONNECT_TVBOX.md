# Hướng dẫn kết nối ADB với TV Box

## 1. Bật USB Debugging trên TV Box

1. Mở **Settings** trên TV Box
2. Kéo xuống dưới cùng → chọn **About** (hoặc **Device Preferences** > **About**)
3. Tìm dòng **Build number** (hoặc **Android TV OS build**) → bấm liên tục **7 lần** cho đến khi thấy thông báo "You are now a developer"
4. Quay lại Settings → mở **Developer options** (mới xuất hiện)
5. Bật:
   - **USB debugging** → ON
   - **Network debugging** → ON (nếu có)

## 2. Kết nối từ laptop

Trong terminal (đang ở thư mục project):

```bash
# Xoá thiết bị cũ (nếu có lỗi unauthorized)
adb disconnect 192.168.1.167:5555

# Kết nối lại
adb connect 192.168.1.167:5555
```

**Quan trọng:** Ngay sau lệnh `adb connect`, trên màn hình TV Box sẽ hiện popup:

```
Allow USB debugging?
The computer's RSA key fingerprint is: ...
□ Always allow from this computer
[CANCEL] [OK]
```

→ **Dùng remote TV bấm OK** (hoặc tick "Always allow" rồi OK)

## 3. Kiểm tra kết nối

```bash
adb devices
```

Phải thấy:
```
List of devices attached
192.168.1.167:5555    device
```

(Nếu vẫn thấy "unauthorized" → quay lại bước 2, đảm bảo đã bấm OK trên TV)

## 4. Cài đặt app

```bash
# Build APK
./gradlew assembleDebug

# Cài lên TV Box
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 5. Chạy app

```bash
adb shell am start -n vn.phs.iptv/.MainActivity
```

## 6. Xem log realtime

```bash
adb logcat | grep "PHS\|IPTV\|ERROR"
```

## Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `unauthorized` | Chưa bấm OK trên popup TV | Chờ popup hiện trên TV, bấm OK bằng remote |
| `offline` | Firewall chặn port 5555 | Tắt firewall tạm, hoặc mở port 5555 |
| `no devices` | Sai IP hoặc TV tắt Network debugging | Kiểm tra IP bằng `ip addr` trên TV, hoặc bật lại Developer options |
| `10013 permission denied` | Windows Defender chặn | Chạy terminal as Administrator |

## Gỡ lỗi nếu không connect được

```bash
# Khởi động lại ADB server
adb kill-server
adb start-server

# Thử cổng mặc định
adb connect 192.168.1.167:5555
```
