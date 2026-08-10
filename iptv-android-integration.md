# Hướng dẫn tích hợp app IPTV (Android TV) với PHS-Lite

Tài liệu dành cho team làm app `phs-iptv`. Đặc tả API gốc: [`iptv-api.md §A`](./iptv-api.md) — file này là **hướng dẫn thi công**, nói rõ sửa file nào, viết gì, test ra sao.

Bản render tham chiếu chạy được ngay: mở `/dev/iptv-tv` trên FE PHS-Lite. Nó làm đúng luồng dưới đây (cùng API, cùng nhịp poll, cùng cách xử lý lỗi) — khi nghi ngờ, mở nó ra đối chiếu.

---

## 1. Nguyên tắc

**TV không sở hữu dữ liệu nào.** Nó không biết mình ở phòng nào, không biết khách là ai, không tự tính tiền. Mọi thứ xin từ PHS-Lite bằng `deviceToken` của chính nó, 20 giây một lần. Mất token = không đọc được gì.

Hệ quả khi code:
- Không cần database (Room/SQLite). Chỉ 4 giá trị trong DataStore.
- Không cache tên khách / số tiền qua đêm.
- Không có màn "đăng nhập". Không ai gõ gì trên TV.
- Checkin/checkout/đổi phòng đều do PMS quyết định — app chỉ phản ứng.

## 2. Hiện trạng repo `phs-iptv` (bản v0.4)

Đã có: state machine, navigation, các màn Idle/Welcome/Video/Home, theme, Retrofit + DTO, DataStore.

**Chưa chạy được** — đường mạng còn là stub:

| File | Vấn đề |
|---|---|
| `ui/provisioning/ProvisioningViewModel.kt:59-61` | `register` bị comment; `displayCode` **bịa ra local** từ `deviceId.take(6)`, không đi qua backend |
| `ui/provisioning/ProvisioningViewModel.kt:70-84` | `startPolling` là `while(true){ delay(5000) }` với thân rỗng — không bao giờ tới được `onAssigned()` |
| `ui/provisioning/ProvisioningScreen.kt:54` | Chỉ qua được provisioning bằng nút debug bypass |
| `data/remote/IptvApi.kt` | Xác thực bằng header `X-Device-Id` trần — **phải bỏ** (xem §3) |
| `di/NetworkModule.kt:22` | `BASE_URL` hardcode `https://api.phs247.com/` |
| `di/NetworkModule.kt:33-41` | `HttpLoggingInterceptor` mức `BODY` bật vô điều kiện — log cả bản release |
| `di/AppModule.kt` | Object rỗng, chưa có binding nào |
| `ui/folio/`, `ui/order/` | Chưa tồn tại; `AppNavigation.kt:48,50` route Folio/Order rơi về HOME |
| `PhsFcmService.kt:22` | Heartbeat còn TODO — **không cần nữa**, đã gộp vào `Screen` |

## 3. Ba thay đổi bắt buộc so với PRD v0.4

### 3.1 Bỏ `X-Device-Id`, dùng `Authorization: Bearer`

PRD cũ định danh bằng UUID trần trên mọi request. Đó là **mật khẩu vĩnh viễn không đổi được**: lọt vào log, proxy công ty, hay ai đó đọc được là xem được tên khách + toàn bộ chi phí phòng đó, mãi mãi.

Thay bằng `deviceToken` (JWT) lấy sau khi ghép cặp. Token thu hồi được từ PMS cho **từng máy một** — đổi phòng, tạm dừng, gỡ TV đều làm token cũ chết ngay lập tức.

### 3.2 Thêm `deviceSecret`

`Register` trả về `deviceSecret`. Mọi lần gọi `Device/Me` phải gửi kèm. Không có nó thì ai đoán được `deviceId` là lấy được token của phòng đó.

### 3.3 Gộp endpoint

| PRD cũ | Nay |
|---|---|
| `GET /iptv/rooms/me/session` | ↘ |
| `GET /iptv/rooms/me/folio` | → **`GET /api/Iptv/Screen`** (1 call) |
| `POST /iptv/devices/heartbeat` | ↗ (gọi `Screen` tính luôn là heartbeat) |

Bỏ FCM cho v1 — poll 20 giây là đủ cho TV.

---

## 4. Lưu trữ cục bộ

Dùng DataStore Preferences đã có ở `data/local/ProvisioningDataStore.kt`. Đúng 4 key:

| Key | Ghi khi nào | Xoá khi nào |
|---|---|---|
| `device_id` | Lần chạy đầu tiên (`UUID.randomUUID()`) | **Không bao giờ.** Mất = phải xoá thiết bị trong PMS rồi ghép lại |
| `device_secret` | Sau `Register` thành công | Không bao giờ |
| `device_token` | Mỗi lần `Device/Me` trả `assigned` | Ghi đè khi lấy token mới |
| `room_no` | Kèm token, chỉ để hiện lúc mất mạng | Tuỳ |

Không lưu tên khách, số tiền, folio. Không Room, không SQLite.

## 5. Vòng đời

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
    │  poll GET Screen mỗi 20s (Bearer device_token)   │
    │   occupied=true  → màn chào khách                │
    │   occupied=false → màn chờ                       │
    └──────────────────┬──────────────────────────────┘
                       │ 400 systemalert.iptv.auth.denied
                       └──────────► quay lại PROVISIONING
```

Không có đường nào khác. Không back stack (`AppNavigation` đang dùng `popUpTo(0)` — giữ nguyên).

---

## 6. Tầng network

### 6.1 Base URL

```kotlin
// build.gradle.kts — module app
buildTypes {
    debug   { buildConfigField("String", "API_BASE", "\"https://<domain-test>/\"") }
    release { buildConfigField("String", "API_BASE", "\"https://<domain-that>/\"") }
}
```
```kotlin
// di/NetworkModule.kt — thay dòng 22
private val BASE_URL = BuildConfig.API_BASE
```

Đồng thời sửa logging (dòng 33-41) — bản release không được log body chứa tên khách:

```kotlin
HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) Level.BODY else Level.NONE
}
```

### 6.2 Retrofit

```kotlin
// data/remote/IptvApi.kt — thay toàn bộ
interface IptvApi {
    @POST("api/Iptv/Device/Register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/Iptv/Device/Me")
    suspend fun me(@Body body: MeRequest): MeResponse

    @GET("api/Iptv/Screen")
    suspend fun screen(@Header("Authorization") bearer: String): ScreenResponse
}
```

Chỉ 3 hàm. Xoá `heartbeat`, `session`, `folio`, `menu`, `orders` (menu/orders chỉ cần nếu làm §10).

### 6.3 DTO

```kotlin
// data/remote/dto/Dtos.kt
@Serializable data class RegisterRequest(val deviceId: String, val appVersion: String)
@Serializable data class RegisterResponse(
    val displayCode: String,
    val status: String,
    val deviceSecret: String? = null,      // null nếu thiết bị đã ghép cặp trước đó
)

@Serializable data class MeRequest(
    val deviceId: String, val deviceSecret: String, val appVersion: String,
)
@Serializable data class MeResponse(
    val status: String,                    // "pending" | "assigned" | "disabled"
    val displayCode: String,
    val branchId: String? = null,
    val roomId: String? = null,
    val roomNo: String? = null,
    val deviceToken: String? = null,
)

@Serializable data class ScreenResponse(
    val roomNo: String,
    val occupied: Boolean,
    val hotel: Hotel,
    val qrUrl: String? = null,
    val breakfast: Breakfast,
    val stay: Stay? = null,                // null khi phòng trống
    val folio: Folio? = null,              // null khi phòng trống HOẶC engine tính tiền lỗi
)

@Serializable data class Hotel(
    val name: String, val wifiSsid: String, val wifiPassword: String,
    val hotline: String, val breakfastTime: String, val welcomeNote: String,
)
@Serializable data class Breakfast(
    val enabled: Boolean, val cutoffTime: String, val serviceDate: String,
    val eligible: Boolean, val alreadySelected: Boolean, val canOrder: Boolean,
)
@Serializable data class Stay(
    val guestName: String, val adults: Int, val children: Int,
    val arrival: String, val departure: String, val nights: Int,
)
@Serializable data class Folio(
    val room: Long, val service: Long, val surcharge: Long,
    val discount: Long, val deposit: Long, val total: Long,
)

@Serializable data class ApiError(val mess: String = "", val defaultMessage: String = "")
```

Giữ `Json { ignoreUnknownKeys = true }` như hiện tại — backend thêm field mới sẽ không làm app crash.

**Tiền dùng `Long`, không `Double`/`Float`.** VND nguyên, không phần thập phân.

**Ngày giờ là chuỗi local-naive** `"2026-08-04T12:01:51"` — **không có `Z`, không offset**. Đây là giờ Việt Nam. Parse bằng `LocalDateTime.parse(...)`, đừng dùng `Instant.parse` (sẽ ném exception), đừng tự cộng trừ múi giờ. `breakfast.serviceDate` là `"2026-08-05"`, parse bằng `LocalDate.parse`.

### 6.4 Đọc lỗi

Lỗi luôn là HTTP 400 kèm body JSON. Bắt `HttpException`, đọc `errorBody`, phân nhánh theo `mess`:

```kotlin
private fun parseError(e: Throwable): String? =
    (e as? HttpException)?.response()?.errorBody()?.string()
        ?.let { runCatching { json.decodeFromString<ApiError>(it).mess }.getOrNull() }
```

| `mess` | Nghĩa | App làm gì |
|---|---|---|
| `systemalert.iptv.auth.denied` | Token hết hiệu lực: bị gỡ, đổi phòng, hoặc tạm dừng | Xoá `device_token`, quay về PROVISIONING (poll `Device/Me` lại) |
| `systemalert.iptv.device.invalid` | Sai `deviceId`/`deviceSecret` | Hiện màn lỗi: "Thiết bị chưa đăng ký, vui lòng liên hệ lễ tân" |
| `systemalert.iptv.disabled` | Chi nhánh tắt IPTV | Về màn chờ, **vẫn tiếp tục poll** (bật lại là chạy tiếp) |

Lỗi mạng (timeout, không có internet) **không** phải lỗi nghiệp vụ — giữ nguyên màn đang hiện, thử lại ở nhịp poll sau, đừng đá về PROVISIONING.

---

## 7. Repository

```kotlin
@Singleton
class IptvRepository @Inject constructor(
    private val api: IptvApi,
    private val store: ProvisioningDataStore,
) {
    suspend fun ensureRegistered(): String {           // trả về displayCode
        val deviceId = store.getOrCreateDeviceId()
        val secret = store.deviceSecret()
        if (secret != null) return api.me(MeRequest(deviceId, secret, VERSION)).displayCode
        val r = api.register(RegisterRequest(deviceId, VERSION))
        r.deviceSecret?.let { store.setDeviceSecret(it) }
        return r.displayCode
    }

    suspend fun checkPairing(): MeResponse {
        val id = store.deviceId() ?: error("chưa đăng ký")
        val secret = store.deviceSecret() ?: error("mất deviceSecret")
        return api.me(MeRequest(id, secret, VERSION)).also { r ->
            r.deviceToken?.let { store.setDeviceToken(it) }
            r.roomNo?.let { store.setRoomNo(it) }
        }
    }

    suspend fun screen(): ScreenResponse {
        val token = store.deviceToken() ?: error("chưa có token")
        return api.screen("Bearer $token")
    }
}
```

`Register` idempotent theo `deviceId`: gọi lại luôn an toàn, nhưng **chỉ trả `deviceSecret` khi thiết bị còn `pending`**. Vì vậy nếu đã có secret trong DataStore thì gọi thẳng `Device/Me`, đừng `Register` lại.

## 8. Vòng poll

```kotlin
// PROVISIONING — 5s
while (isActive) {
    runCatching { repo.checkPairing() }
        .onSuccess { if (it.status == "assigned" && it.deviceToken != null) { toRunning(); return } }
    delay(5_000)
}

// RUNNING — 20s
while (isActive) {
    runCatching { repo.screen() }
        .onSuccess { render(it) }
        .onFailure { if (parseError(it) == "systemalert.iptv.auth.denied") { toProvisioning(); return } }
    delay(20_000)
}
```

Chạy trong `viewModelScope`, huỷ khi rời màn. Không dùng `WorkManager` (poll 20s không hợp với WorkManager, tối thiểu 15 phút).

**Đừng để TV ngủ:** thêm `FLAG_KEEP_SCREEN_ON` trong Activity, nếu không màn hình tắt sau vài phút.

## 9. Màn hình cần vẽ

| Trạng thái | Điều kiện | Nội dung |
|---|---|---|
| **Provisioning** | chưa có token | `displayCode` cỡ chữ rất lớn (đọc được từ cuối phòng, ≥ 96sp), kèm dòng "Vui lòng đọc mã này cho lễ tân". **Không hiện `deviceId` hay `deviceSecret`.** |
| **Chờ** | `occupied = false` | Tên khách sạn, số phòng, `welcomeNote`, wifi, số lễ tân. Tuyệt đối không còn dấu vết khách trước |
| **Chào khách** | `occupied = true` | Lời chào + `stay.guestName`, ngày nhận/trả, số đêm, số khách, khối chi phí, QR gọi món, wifi/hotline |

Khối chi phí (khi `folio != null`): tiền phòng, dịch vụ, phụ thu (ẩn nếu 0), giảm giá (ẩn nếu 0), đã cọc (ẩn nếu 0), **tổng còn lại** làm nổi bật. Định dạng `"%,d ₫"` locale `vi-VN`.

QR: render từ `qrUrl` bằng ZXing (`com.google.zxing:core`). **Render lại mỗi lần poll** — chuỗi đổi khi khách sạn bấm "Tạo lại QR" trong PMS; cache cứng sẽ làm QR chết mà không ai biết. `qrUrl == null` nghĩa là chi nhánh tắt gọi món → ẩn cả khối.

Trạng thái ăn sáng (khi `breakfast.enabled && breakfast.eligible`):
- `alreadySelected` → "Đã chọn món ăn sáng cho ngày {serviceDate}"
- `canOrder` → "Chọn món ăn sáng cho ngày mai trước {cutoffTime}"
- còn lại → "Đã hết giờ chọn món ăn sáng"

Giữ nguyên design token trong PRD §9 và padding overscan 48dp × 27dp — TV cắt mép.

## 10. Ngoài phạm vi v1

- **FCM push** — poll 20s là đủ. Chỉ làm nếu khách sạn yêu cầu checkin phải hiện dưới 10 giây.
- **Video intro, trợ lý giọng nói** (PRD F2/F3) — không liên quan tích hợp này.
- **Đặt món ngay trên TV** — mặc định khách quét QR bằng điện thoại. Nếu muốn đặt bằng remote, dùng thêm 4 API `/api/RoomOrder/Public/*` ở [`iptv-api.md §B`](./iptv-api.md), token lấy từ phần đuôi của `qrUrl`. Cân nhắc: gõ ghi chú bằng remote rất khó chịu.
- **Cache offline** — nếu bắt buộc phải hiện gì đó khi mất mạng, lưu nguyên JSON `Screen` gần nhất và vẽ kèm nhãn "Đang cập nhật…". Nhưng như vậy màn hình có thể hiện tên khách đã trả phòng suốt thời gian rớt mạng. An toàn hơn là về màn chờ.

---

## 11. Checklist nghiệm thu

Mở PMS ở `/rooms/iptv` (tab lễ tân) và app trên TV, chạy lần lượt:

- [ ] Cài app lần đầu → hiện mã 6 ký tự, đọc được từ cuối phòng
- [ ] Nhân viên nhập mã + chọn phòng trong PMS → TV **tự** chuyển sang màn khách trong 5 giây, không đụng gì vào TV
- [ ] Tên khách, ngày nhận/trả, tổng tiền **khớp đúng** màn Thanh toán của phòng đó trong PMS
- [ ] Quét QR bằng điện thoại thật → mở đúng trang gọi món của phòng đó
- [ ] Đặt 1 món trên điện thoại → đơn xuất hiện ở màn "Đơn gọi phòng" trong PMS
- [ ] PMS đổi TV sang phòng khác → TV đổi nội dung trong 20 giây, **không** phải ghép lại
- [ ] PMS bấm ⏸ tạm dừng → TV về màn mã; bấm ▶ → chạy lại bình thường
- [ ] **Checkout phòng trong PMS → TV về màn chờ, không còn tên khách cũ** (quan trọng nhất)
- [ ] Sửa wifi/hotline trong PMS → TV cập nhật trong 20 giây
- [ ] Rút dây mạng 30 giây rồi cắm lại → TV tự hồi, không cần khởi động lại
- [ ] Rút điện TV rồi cắm lại → vào thẳng màn khách, **không** hỏi ghép cặp lại
- [ ] Để TV chạy 30 phút → màn hình không tự tắt, không sập app
- [ ] PMS thấy chấm ● xanh của TV đó trong danh sách thiết bị
- [ ] Build release: `adb logcat` **không** thấy tên khách hay số tiền

## 12. Khi có sự cố

| Hiện tượng | Nguyên nhân thường gặp |
|---|---|
| TV kẹt ở màn mã dù đã gán trong PMS | Sai base URL, hoặc `deviceSecret` không được lưu (kiểm tra DataStore) |
| Điện thoại quét QR không mở được | `PUBLIC_WEB_ORIGIN` phía server đang là `localhost` — lỗi cấu hình server, không phải app |
| TV hiện tên khách đã trả phòng | App đang cache `Screen`. Backend luôn trả `stay: null` khi phòng trống — kiểm tra lại chỗ render |
| Số tiền lệch so với PMS | App tự cộng lại thay vì dùng `folio.total` |
| Cứ ~1 phút TV nhảy về màn mã | Đang coi lỗi mạng như `auth.denied`. Chỉ đá về PROVISIONING khi `mess` đúng là `systemalert.iptv.auth.denied` |
| Giờ hiển thị lệch 7 tiếng | Đang parse ngày bằng `Instant`/UTC. Chuỗi là giờ VN không offset, dùng `LocalDateTime` |
