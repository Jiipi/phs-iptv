# Tích hợp app IPTV với PHS-Lite

Tài liệu làm việc của repo này. Bám theo đây để nối app với PMS.

- **Bản gốc hợp đồng API**: `phs-lite/docs/iptv-api.md §A` — khi BE đổi, sửa bên đó trước rồi đồng bộ file này.
- **Hướng dẫn thi công chi tiết** (DTO đầy đủ, bảng lỗi, bảng sự cố): `phs-lite/docs/iptv-android-integration.md`.
- **Bản render tham chiếu chạy được**: mở `http://<host-phs-lite>:5173/dev/iptv-tv` — TV giả lập làm đúng luồng dưới đây. Khi nghi ngờ, mở ra đối chiếu.

Cập nhật: 2026-08-05 · BE tương ứng: commit `446d4a6` nhánh `dev`.

---

## 1. Nguyên tắc

**TV không sở hữu dữ liệu nào.** Nó không biết mình ở phòng nào, không biết khách là ai, không tự tính tiền. Mọi thứ xin từ PHS-Lite bằng `deviceToken` của chính nó. Mất token = không đọc được gì.

Hệ quả khi code:
- Không cần database. Chỉ 4 giá trị trong DataStore.
- Không cache tên khách / số tiền qua đêm.
- Không có màn đăng nhập. Không ai gõ gì trên TV.
- Check-in / check-out / đổi phòng do PMS quyết định — app chỉ phản ứng.

## 2. Bốn API

| Endpoint | Nhịp gọi | Trả gì |
|---|---|---|
| `POST api/Iptv/Device/Register` | 1 lần, lần chạy đầu | `displayCode` + `deviceSecret` |
| `POST api/Iptv/Device/Me` | 5s khi chưa ghép cặp | `status`, khi `assigned` thì kèm `deviceToken` |
| `GET api/Iptv/Screen` | **20 giây** | khách · tiền · QR gọi món · ăn sáng · wifi |
| `GET api/Iptv/Content` | khởi động + **10 phút** | khách sạn · loại phòng · video · tiện ích · chữ đa ngữ |

Hai nhịp khác nhau là cố ý: `Screen` đổi liên tục nên phải nhanh; `Content` nặng ảnh và gần như không đổi nên gọi thưa, so `version` để biết có phải dựng lại màn. **Đừng gọi Content theo nhịp 20 giây.**

Gọi `Screen` cũng tính là heartbeat — PMS hiện chấm ● online. Không có endpoint heartbeat riêng.

## 3. Vòng đời

```
    ┌─ chưa có device_id ─────────────────────────────┐
    │  POST Device/Register                            │
    │  lưu device_id + device_secret                   │
    └──────────────────┬──────────────────────────────┘
                       ▼
    ┌─ PROVISIONING ──────────────────────────────────┐
    │  hiện displayCode to giữa màn hình               │
    │  poll POST Device/Me mỗi 5s                      │
    └──────────────────┬──────────────────────────────┘
                       │ status == "assigned"
                       ▼  lưu device_token
    ┌─ RUNNING ───────────────────────────────────────┐
    │  GET Content 1 lần → dựng khách sạn/phòng/tiện ích│
    │  GET Screen mỗi 20s                              │
    │    occupied=true  → màn chào khách               │
    │    occupied=false → màn chờ                      │
    │  GET Content lại mỗi 10 phút, version đổi mới vẽ │
    └──────────────────┬──────────────────────────────┘
                       │ 400 systemalert.iptv.auth.denied
                       └──────────► quay lại PROVISIONING
```

Không có đường nào khác. Không back stack.

## 4. Lưu cục bộ — đúng 4 key (DataStore)

| Key | Ghi khi nào | Xoá khi nào |
|---|---|---|
| `device_id` | lần chạy đầu (`UUID.randomUUID()`) | **không bao giờ** |
| `device_secret` | sau `Register` | **không bao giờ** |
| `device_token` | mỗi lần `Device/Me` trả `assigned` | ghi đè khi lấy token mới |
| `room_no` | kèm token, chỉ để hiện lúc mất mạng | tuỳ |

Không lưu tên khách, số tiền, folio.

**Nếu mất `device_secret`** (xoá dữ liệu app): `Register` lần 2 sẽ **không** trả secret nữa. Phải nhờ lễ tân xoá TV đó trong PMS rồi ghép lại. Vì vậy đã có secret thì gọi thẳng `Device/Me`, đừng `Register` lại.

## 5. Đọc lỗi

Lỗi nghiệp vụ luôn HTTP 400, body `{ "mess": "...", "defaultMessage": "..." }`.

| `mess` | App làm gì |
|---|---|
| `systemalert.iptv.auth.denied` | Token hết hiệu lực (bị gỡ / đổi phòng / tạm dừng) → xoá `device_token`, quay về PROVISIONING |
| `systemalert.iptv.device.invalid` | Sai `deviceId`/`deviceSecret` → màn lỗi, hướng dẫn liên hệ lễ tân |
| `systemalert.iptv.disabled` | Chi nhánh tắt IPTV → màn chờ, **vẫn tiếp tục poll** |

**Lỗi mạng (timeout, DNS, 5xx) KHÔNG phải lỗi nghiệp vụ.** Giữ nguyên màn đang hiện, thử lại nhịp sau. Đá về PROVISIONING khi wifi chập chờn = TV nhấp nháy về màn mã mỗi phút.

## 6. Nội dung nào từ đâu

Khách sạn không nhập lại gì cho TV — nội dung đã có sẵn từ Zalo Mini App / OTA:

| TV hiện | PMS nhập ở |
|---|---|
| Tên KS, địa chỉ, SĐT, email, mô tả, bộ ảnh, tiện nghi, giờ nhận/trả, chính sách, **wifi**, **toạ độ bản đồ** | Cấu hình hệ thống → Thông tin khách sạn |
| Ảnh + mô tả + diện tích + kiểu giường + hướng nhìn **theo loại phòng** | Cấu hình → Loại phòng |
| Video giới thiệu, video màn chờ, tiện ích dịch vụ, ô màn chính, lời chào | Phòng → IPTV phòng → **Nội dung TV** |
| Thực đơn gọi món | khách quét QR bằng điện thoại, TV chỉ hiện mã |

`Content.roomType` là loại phòng **của chính phòng gắn TV** — không phải danh sách.

## 7. Đa ngữ

Trường dạng `{vi, en, ru}` (`texts.welcome`, `services[].title`, `services[].description`). **Bản dịch trống → lùi về `vi`**, đừng hiện ô trắng:

```kotlin
fun I18nText.forLang(lang: AppLanguage): String = when (lang) {
    AppLanguage.EN -> en.ifBlank { vi }
    AppLanguage.RU -> ru.ifBlank { vi }
    AppLanguage.VI -> vi
}
```

Nội dung dùng chung (`hotel.description`, `roomType.description`, `policies`) chỉ có **một thứ tiếng** — hiện nguyên văn.

`hotel.facilities` và `roomType.amenities` là **MÃ** (`wifi`, `pool`, `gym`…), không phải chữ hiển thị. App tự map mã → nhãn + icon theo ngôn ngữ; mã lạ thì bỏ qua, **đừng in mã thô lên màn hình**.

## 8. Việc phải làm trong repo này

Trạng thái tại commit `4470a59` — đường mạng còn là stub.

| File | Việc |
|---|---|
| `di/NetworkModule.kt:22` | `BASE_URL` hardcode `https://api.phs247.com/` → chuyển sang `BuildConfig.API_BASE` |
| `di/NetworkModule.kt:33-41` | `HttpLoggingInterceptor` mức BODY bật vô điều kiện → chỉ bật ở `BuildConfig.DEBUG` (body chứa tên khách + folio) |
| `data/remote/IptvApi.kt` | Bỏ header `X-Device-Id`, dùng `Authorization: Bearer`. Rút còn 4 hàm ở §2 |
| `data/remote/dto/Dtos.kt` | Thay bằng DTO mới (bản đầy đủ ở `phs-lite/docs/iptv-android-integration.md §6.3`) |
| `data/local/ProvisioningDataStore.kt` | Thêm `device_secret`, `device_token` |
| `ui/provisioning/ProvisioningViewModel.kt:59-61` | `register` đang bị comment, `displayCode` **bịa ra local** từ `deviceId.take(6)` → gọi API thật |
| `ui/provisioning/ProvisioningViewModel.kt:70-84` | `startPolling` là vòng lặp rỗng, không bao giờ tới `onAssigned()` → poll `Device/Me` thật |
| `ui/provisioning/ProvisioningScreen.kt:54` | Bỏ nút debug bypass |
| `domain/AppStateMachine.kt` | Poll `Screen`, suy Idle/Welcome/Home từ `occupied` (thay FCM) |
| `ui/folio/FolioScreen.kt` | Dùng `folio` thật thay `Demo.folio` |
| `ui/help/HelpScreen.kt:70` | Dùng `content.hotel` thay `Demo.hotel` |
| `ui/services/ServicesScreen.kt` | Dùng `content.services` thay `Demo.hotelServices` |
| `ui/idle/IdleScreen.kt:94` | Tên KS thay chuỗi cứng "PHS Hotel · Hanoi" |
| `ui/order/OrderScreen.kt` | Hiện QR từ `screen.qrUrl` thay thực đơn giả |
| `PhsFcmService.kt:22` | Heartbeat TODO — **bỏ hẳn**, đã gộp vào `Screen` |
| `MainActivity.kt` | Thêm `FLAG_KEEP_SCREEN_ON`, không thì màn tắt sau vài phút |

Ngoài phạm vi v1: FCM push, Live TV, kho phim/VOD, trợ lý giọng nói.

## 9. Ba điểm lệch PRD v0.4 — và vì sao

1. **Bỏ `X-Device-Id` trần** → `Authorization: Bearer`. UUID đó là mật khẩu vĩnh viễn không thu hồi được; lọt log/proxy là đọc được tên khách + folio phòng đó mãi mãi. Token mới thu hồi được **từng máy** — PMS đổi phòng / tạm dừng / gỡ là token cũ chết ngay.
2. **Thêm `deviceSecret`** ở `Device/Me` — thiếu nó thì ai đoán được `deviceId` là lấy được token.
3. **Gộp `session` + `folio` + `heartbeat` → `GET Screen`**, thêm `GET Content`. Bỏ FCM ở v1, poll 20 giây là đủ cho TV.

## 10. Chi tiết dễ sai

- **Ngày giờ là local-naive giờ Việt Nam**: `"2026-08-04T12:01:51"`, **không `Z`, không offset**. Parse bằng `LocalDateTime.parse`. Dùng `Instant.parse` sẽ ném exception; tự cộng múi giờ sẽ lệch 7 tiếng.
- **Tiền dùng `Long`** (VND nguyên, không thập phân). Hiện `folio.total`, **đừng tự cộng lại** — cộng lại là lệch với hoá đơn khách nhận lúc trả phòng.
- **QR render lại mỗi lần poll** từ `screen.qrUrl`. Khách sạn bấm "Tạo lại QR" là chuỗi đổi; cache cứng thì QR trên TV chết mà không ai biết.
- **Phòng trống**: backend trả `stay: null, folio: null`. Về màn chờ, không còn dấu vết khách trước.
- `folio` có thể `null` dù `occupied: true` (engine tính tiền lỗi) — vẫn vẽ phần còn lại, đừng để trắng màn.
- `qrUrl: null` = chi nhánh tắt gọi món → ẩn cả khối.
- Giữ `Json { ignoreUnknownKeys = true }` — BE thêm field mới sẽ không làm app crash.

## 11. Checklist nghiệm thu

Mở PMS ở `/rooms/iptv` (tab lễ tân) và app trên TV:

- [ ] Cài lần đầu → hiện mã 6 ký tự, đọc được từ cuối phòng
- [ ] Lễ tân nhập mã + chọn phòng → TV **tự** sang màn khách trong 5 giây, không đụng gì vào TV
- [ ] Tên khách, ngày nhận/trả, tổng tiền **khớp** màn Thanh toán của phòng đó
- [ ] Ảnh + mô tả phòng đúng **loại phòng** khách đang ở
- [ ] Wifi, số lễ tân, giờ nhận/trả khớp màn Thông tin khách sạn
- [ ] Tiện ích hiện đủ tên/mô tả/giờ/ảnh; đổi ngôn ngữ → đổi chữ, thiếu bản dịch thì ra tiếng Việt
- [ ] Quét QR bằng điện thoại thật → mở đúng trang gọi món của phòng đó
- [ ] Đặt 1 món trên điện thoại → đơn hiện ở màn "Đơn gọi phòng" trong PMS
- [ ] PMS đổi TV sang phòng khác → TV đổi nội dung trong 20 giây, **không** phải ghép lại
- [ ] PMS bấm ⏸ → TV về màn mã; bấm ▶ → chạy lại
- [ ] **Check-out → TV về màn chờ, không còn tên khách cũ** (quan trọng nhất)
- [ ] Sửa lời chào / thêm tiện ích trong PMS → TV cập nhật trong 10 phút
- [ ] Rút mạng 30 giây rồi cắm lại → TV tự hồi, **không** nhảy về màn mã
- [ ] Rút điện rồi cắm lại → vào thẳng màn khách, **không** hỏi ghép cặp lại
- [ ] Chạy 30 phút → màn hình không tự tắt, không sập app
- [ ] Build release: `adb logcat` **không** thấy tên khách hay số tiền

## 12. Sự cố → nguyên nhân

| Hiện tượng | Thường là |
|---|---|
| Kẹt màn mã dù PMS đã gán | Sai base URL, hoặc `deviceSecret` không được lưu |
| Quét QR không mở được | `PUBLIC_WEB_ORIGIN` phía server đang là `localhost` — lỗi cấu hình server, không phải app |
| Hiện tên khách đã trả phòng | App đang cache `Screen`. BE luôn trả `stay: null` khi phòng trống |
| Số tiền lệch PMS | App tự cộng thay vì dùng `folio.total` |
| Cứ ~1 phút nhảy về màn mã | Coi lỗi mạng như `auth.denied` |
| Giờ lệch 7 tiếng | Parse ngày bằng `Instant`/UTC thay vì `LocalDateTime` |
| Tiện ích hiện chữ `pool`, `gym` | Đang in mã thô, chưa map sang nhãn |
