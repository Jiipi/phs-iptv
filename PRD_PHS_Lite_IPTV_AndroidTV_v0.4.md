# PRD — PHS-Lite IPTV App trên Google TV (Android TV)

> **Version**: v0.4
> **Thay đổi so với v0.3**: Đổi model provisioning F0 sang **auto-register + console assign** (§8, §10.1, §13 ADR-7, §16). Thiết bị tự sinh `deviceId` (UUID vĩnh viễn) và gọi `POST /iptv/devices/register`; lễ tân gán phòng trong PHS console thay vì nhập mã đối chiếu trên TV. Xem changelog §18.
> **Phạm vi**: App Android TV (Kotlin) + module `iptv` trong monolith phs-lite
> **Stack target**: Kotlin · Compose for TV · Media3 · FCM · NestJS (backend side)

---

## 1. Tổng quan & mục tiêu

### 1.1. Sản phẩm

IPTV in-room application chạy trên Google TV / Android TV, đặt tại từng phòng khách sạn, tích hợp với PHS-Lite. App cá nhân hóa trải nghiệm khách theo trạng thái check-in thật trong PMS.

### 1.2. Mục tiêu (theo độ ưu tiên)

| Mã | Chức năng | Priority |
|---|---|---|
| F0 | Idle screen + device provisioning | **P0** |
| F1 | Welcome screen hiển thị tên khách | **P0** |
| F2 | Video giới thiệu, autoplay theo event check-in | **P0** |
| F3 | Voice AI qua remote (chat bằng giọng nói) | **P1** |
| F4 | Xem hóa đơn/folio trên TV | **P1** |
| F5 | Gọi món tại phòng (POS menu lên TV) | **P2** |

### 1.3. Ràng buộc chiến lược

App là **add-on differentiator**, không phải core PMS. Theo "prove one case study first": **P0 phải đủ nhỏ để chạy thật ở một property pilot và đo phản hồi**. F3–F5 chỉ mở khi pilot xác nhận khách thực sự dùng.

---

## 2. Phạm vi

### 2.1. In-scope

1. App Android TV (APK) cài trên Google TV consumer, chạy kiosk mode.
2. Module `iptv` trong monolith phs-lite: nghe domain event từ PMS, quản lý FCM token, expose read API cho TV.
3. Bind thiết bị TV ↔ `(branchId, roomId)` (provisioning một lần khi lắp đặt).
4. Luồng push check-in → Welcome → autoplay video → về home/idle.
5. Voice AI: capture giọng nói → STT → RAG → TTS.

### 2.2. Out-of-scope

1. KHÔNG intercept nút mic vật lý remote Google TV (bind cứng Assistant — §15.3).
2. KHÔNG làm MDM riêng — dùng MDM/kiosk launcher bên thứ ba.
3. KHÔNG thanh toán trên TV (folio chỉ xem).
4. KHÔNG livestream truyền hình cáp/DVB — đây là smart app, không phải đầu thu.
5. KHÔNG đăng nhập tài khoản khách (TV dùng chung của phòng).
6. KHÔNG định nghĩa cách PMS lấy/lưu dữ liệu nội bộ — IPTV chỉ phụ thuộc interface §5.

---

## 3. Bối cảnh kỹ thuật & ràng buộc

### 3.1. Stack Android (chốt)

| Thành phần | Lựa chọn |
|---|---|
| Ngôn ngữ | Kotlin |
| UI | Jetpack **Compose for TV** (`androidx.tv:tv-material`) |
| Video | **Media3 (ExoPlayer)** |
| Push | **Firebase Cloud Messaging** (data message) |
| Networking | **Retrofit + OkHttp + kotlinx.serialization** |
| DI | **Hilt** |
| Ảnh | **Coil** |
| Local config | **DataStore** |
| Voice STT | `SpeechRecognizer` (vi-VN) |
| Voice TTS | `TextToSpeech` on-device, fallback cloud TTS |

### 3.2. Ràng buộc backend (phs-lite)

1. Monolith NestJS, module boundary nghiêm ngặt, multi-tenant theo branch.
2. IPTV **consume domain event** từ PMS qua `@nestjs/event-emitter`.
3. IPTV **không chạm dữ liệu nội bộ PMS** — chỉ phụ thuộc interface §5 (tránh lặp lỗi `booking→crm` đảo chiều).
4. Mọi request scope `tenantId` + `branchId`.

---

## 4. Kiến trúc tích hợp

```
phs-lite (NestJS monolith)
├── PMS module      → phát domain event check-in/check-out
│                     → cung cấp query interface (§5.2)
└── IPTV module     → @OnEvent('guest.checked_in' / 'guest.checked_out')
                       ├── lưu session state per room
                       ├── gửi FCM push
                       └── expose TV-facing read API (§8)
                                  ↓ FCM data message
                          Android TV app (kiosk)
```

**Chiến lược dữ liệu (hybrid)**: Welcome/video lấy từ event payload (tĩnh); folio gọi query interface tươi (§5).

---

## 5. PMS Integration Contract (Việt tổ chức)

> Toàn bộ bề mặt phụ thuộc IPTV↔PMS. Cơ chế kết nối (in-process port / internal API) và nguồn dữ liệu do Việt quyết. Pseudocode TypeScript; app Kotlin có DTO mirror.

### 5.1. Inbound — Domain events IPTV consume

```ts
interface GuestCheckedInEvent {
  tenantId: string;
  branchId: string;
  roomId: string;
  roomNo: string;
  stayId: string;        // mã lượt lưu trú, opaque — idempotency video
  guest: GuestProfile;   // §5.3
  checkedInAt: string;   // ISO 8601 có timezone
}
interface GuestCheckedOutEvent {
  tenantId: string; branchId: string; roomId: string; stayId: string;
}
```

**Yêu cầu PMS:** mọi đường check-in (kể cả "nhận phòng nhanh") đều phát cùng event `guest.checked_in`.

### 5.2. Outbound — Query interface PMS cung cấp

```ts
interface PmsIptvQueryPort {
  getInhouseGuest(branchId: string, roomId: string): Promise<GuestProfile | null>;
  getFolio(branchId: string, roomId: string): Promise<Folio | null>;
}
interface PosIptvPort {                                   // F5 (P2)
  getIptvMenu(branchId: string): Promise<IptvMenuItem[]>;
  placeRoomOrder(branchId: string, roomId: string,
                 items: IptvOrderLine[]): Promise<OrderResult>;
}
```

### 5.3. Shared DTO

```ts
interface GuestProfile {
  name: string;          // tên khách chính
  title: string;         // kính ngữ ("Mr."/"Ms.") — PMS tự suy ra
  roomNo: string;
  nationality: string;   // mã quốc tịch (vd "VNM") — IPTV dùng chọn ngôn ngữ
  isBirthday: boolean;   // PMS tự tính
}
interface FolioLine { date: string; description: string; quantity: number; amount: number; }
interface Folio { roomNo: string; lines: FolioLine[]; total: number; }
interface IptvMenuItem { id: string; name: string; price: number; category: string; available: boolean; }
interface IptvOrderLine { itemId: string; quantity: number; }
interface OrderResult { orderId: string; status: "accepted" | "rejected"; }
```

> Mọi logic suy diễn (kính ngữ, sinh nhật, ghép tiền phòng, item bán qua IPTV) nằm ở **phía PMS**. IPTV chỉ render DTO.

---

## 6. Yêu cầu chức năng (tóm tắt — chi tiết UI ở §10)

| Mã | Tóm tắt | Acceptance chính |
|---|---|---|
| F0 | Idle + provisioning | Kiosk, bind room, offline-tolerant |
| F1 | Welcome | "Chào mừng {title} {name}" + phòng + sinh nhật; ngôn ngữ theo nationality |
| F2 | Video | Autoplay theo `guest.checked_in`; skip được; 1 lần/`stayId`; latency <10s; fallback poll |
| F3 | Voice AI | Nút on-screen + SpeechRecognizer vi-VN → RAG → TTS |
| F4 | Folio | `getFolio` pull tươi; chỉ xem, không pay |
| F5 | Gọi món | `getIptvMenu` + `placeRoomOrder`; phụ thuộc POS module |

---

## 7. Yêu cầu phi chức năng

1. **Kiosk**: app luôn foreground; không thoát ra launcher/settings.
2. **Offline-tolerant**: idle + welcome cache được; chỉ folio/voice/order cần mạng.
3. **Latency**: check-in → video <10s (push); fallback poll <60s.
4. **Bảo mật**: TV chỉ read + voice + order; token thiết bị; clear session khi check-out.
5. **Multi-tenant**: scope `tenantId`+`branchId`, không bypass.
6. **i18n**: string resource vi/en theo `nationality`.

---

## 8. API contract — IPTV expose cho TV

| Endpoint | Method | Mô tả |
|---|---|---|
| `/iptv/devices/register` | POST | Đăng ký device: nhận `displayCode` + `status` (unassigned/assigned) |
| `/iptv/devices/me` | GET | Poll trạng thái gán phòng: `status`, `branchId?`, `roomId?`, `roomNo?` |
| `/iptv/devices/heartbeat` | POST | Cập nhật FCM token, báo online |
| `/iptv/rooms/me/session` | GET | State phòng (có khách không, welcome data) |
| `/iptv/rooms/me/folio` | GET | Folio (F4) |
| `/iptv/rooms/me/menu` | GET | Menu gọi món (F5) |
| `/iptv/rooms/me/orders` | POST | Tạo order (F5) |
| `/iptv/assistant/ask` | POST | Voice AI (F3) |

> **Ghi chú quan trọng**: **Gán phòng cho thiết bị nằm ở PHS admin console** (ngoài app TV). App chỉ đăng ký danh tính thiết bị và poll chờ kết quả. `displayCode` là nhãn ngắn nhận đúng máy trong console — không phải mã để nhập đối chiếu. Mọi request dùng header `X-Device-Id: <uuid>` thay vì device token.

**DTO thiết bị** (v0.4 — `data/remote/dto/Dtos.kt`):

```kotlin
data class RegisterDeviceRequest(val deviceId: String)
data class DeviceRegistration(
    val displayCode: String,
    val status: String,           // "unassigned" | "assigned"
    val assignment: DeviceAssignment? = null,
)
data class DeviceAssignment(val branchId: String, val roomId: String, val roomNo: String)
data class DeviceStatus(
    val status: String,
    val branchId: String? = null,
    val roomId: String? = null,
    val roomNo: String? = null,
)
```

**FCM data types**: `guest.checked_in`, `guest.checked_out`, `folio.updated`.

---

## 9. Design System & Design Tokens

> Codegen lấy token trực tiếp từ đây. **Không bao giờ hardcode hex trong screen** — chỉ dùng token từ `theme/`.

### 9.1. Bảng màu (Compose `Color`)

```kotlin
// theme/Color.kt — brand PHS
val PrimaryGold   = Color(0xFFC9A961)
val AntiqueGold   = Color(0xFFA88932)
val MidnightNavy  = Color(0xFF1F2937)
val IvoryCream    = Color(0xFFFAF7F0)
// derived
val NavyElevated  = Color(0xFF2A3645)   // card trên nền navy
val IvoryMuted    = Color(0xFFCFC9BD)   // text phụ trên navy
val SlateMuted    = Color(0xFF4A5568)   // text phụ trên ivory
```

### 9.2. Color role — hai ngữ cảnh surface

App dùng 2 ngữ cảnh: **hero** (sang trọng, nền sáng) cho khoảnh khắc Welcome/Idle; **app** (nền tối, contrast cao cho 10-foot) cho màn hình tương tác.

| Token | Giá trị | Dùng ở |
|---|---|---|
| `heroSurface` | IvoryCream | Welcome, Idle |
| `onHero` | MidnightNavy | text trên hero |
| `heroAccent` | AntiqueGold | accent trên ivory |
| `appSurface` | MidnightNavy | Home, Folio, Voice |
| `appSurfaceElevated` | NavyElevated | card |
| `onApp` | IvoryCream | text chính trên navy |
| `onAppMuted` | IvoryMuted | text phụ trên navy |
| `accent` | PrimaryGold | focus, nút, highlight |
| `accentPressed` | AntiqueGold | trạng thái pressed |
| `focusRing` | PrimaryGold | viền focus |

> **Quy tắc contrast quan trọng**: PrimaryGold (#C9A961) trên IvoryCream bị chìm → trên nền ivory dùng **AntiqueGold** hoặc Navy cho text/accent. Giữ PrimaryGold sáng cho nền navy.

### 9.3. Typography (10-foot UI)

| Style | Size | Weight | Dùng |
|---|---|---|---|
| `displayHero` | 56sp | Bold | Tên khách ở Welcome |
| `headline` | 34sp | SemiBold | Tiêu đề màn hình |
| `title` | 24sp | Medium | Tiêu đề card |
| `body` | 20sp | Normal | Nội dung |
| `label` | 16sp | Medium | Nhãn nút/nhỏ |

Min body 18sp cho khoảng cách 10 feet. Line-height rộng (1.4). Font: system (Roboto). Optional: drop một serif display cho `displayHero` để tăng cảm giác sang — để slot, không bắt buộc.

### 9.4. Spacing & shape

- Screen safe padding: **48dp ngang, 27dp dọc** (overscan 5%).
- Spacing scale: 8 / 16 / 24 / 32 / 48 dp.
- Corner radius: 12dp card, 8dp button.

### 9.5. Focus treatment (cốt lõi TV)

- Element focused: scale **1.06**, viền `focusRing` (PrimaryGold) **3dp**, elevation tăng, animate mượt (~150ms).
- Unfocused: không viền, text `onAppMuted`.
- Mỗi màn hình phải có **initial focus rõ ràng**.

### 9.6. Logo

- Asset slot: `res/drawable/logo_phs` (vector ưu tiên). **Drop file logo của khách sạn / PHS vào đây.**
- Idle: logo center, width ~360dp trên `heroSurface`.
- Welcome: logo nhỏ góc trên-trái, height ~64dp.
- **Fallback nếu chưa có asset**: wordmark "PHS" bằng `displayHero`, màu Gold trên Navy (Idle) / Navy trên Ivory (Welcome).

### 9.7. Theme.kt (anchor cho codegen)

```kotlin
// Hai bộ token cho 2 ngữ cảnh, expose qua CompositionLocal hoặc 2 ColorScheme tv-material.
// Welcome/Idle render trong "hero" context; Home/Folio/Voice trong "app" context.
@Composable fun PhsIptvTheme(content: @Composable () -> Unit) { /* tv-material MaterialTheme + typography §9.3 */ }
```

---

## 10. UI spec từng màn hình (codegen-ready)

> Mỗi màn hình: ngữ cảnh surface · layout · component · state · initial focus. Tất cả Composable đặt dưới `ui/<feature>/`.

### 10.1. ProvisioningScreen (F0)
- **Context**: app (navy).
- **Model** (v0.4): thiết bị tự sinh `deviceId` (UUID, 1 lần, persist) → `POST /devices/register` → nhận `displayCode` → hiển thị → lễ tân vào **PHS console** gán phòng cho thiết bị có mã này → app poll `GET /devices/me` mỗi 5s → khi `status=="assigned"` → lưu `branchId/roomId/roomNo` → chuyển IdleScreen.
- **Layout**: center column — wordmark "PHS" nhỏ/trầm (`titleLarge`, AntiqueGold via `secondary`); headline "Kết nối thiết bị"; nhãn "Mã thiết bị:"; `displayCode` (`displayLarge`, PrimaryGold — điểm nhấn chính); hướng dẫn "Vào PHS console và gán phòng cho mã này" (`bodyLarge`, IvoryMuted); "Đang chờ gán phòng…"; nút "Làm mới".
- **State**: `Registering / WaitingForAssignment(displayCode) / Assigned(roomNo) / Error`.
- **Initial focus**: nút "Làm mới" (disabled khi Registering, enabled sau đó).
- **Debug bypass** (debug build only): nút "Debug: Bỏ qua" gọi trực tiếp `onAssigned()` để dev đi tiếp Idle mà không cần backend thật.

### 10.2. IdleScreen (F0)
- **Context**: hero (ivory).
- **Layout**: logo center; đồng hồ (`headline`) góc; tên khách sạn (`title`); badge "offline" nhỏ khi mất mạng. Optional P2: thời tiết.
- **State**: `Online / Offline`. Không có element focus (màn hình thụ động); bấm OK không làm gì (chờ event).

### 10.3. WelcomeScreen (F1)
- **Context**: hero (ivory).
- **Layout**: logo góc trên-trái; center — "Chào mừng" (`headline`, SlateMuted) → "{title} {name}" (`displayHero`, MidnightNavy) → "Phòng {roomNo}" (`title`, heroAccent). Nếu `isBirthday` → dòng "🎂 Chúc mừng sinh nhật" (text, không emoji nếu không hợp brand — dùng icon gold).
- **State**: `Loading / Loaded / Fallback("Chào mừng quý khách")`.
- **Behavior**: auto → IntroVideo sau 5s hoặc OK. Ngôn ngữ theo `nationality`.
- **Initial focus**: nút "Bỏ qua" (skip) ẩn, OK toàn màn skip được.

### 10.4. IntroVideoScreen (F2)
- **Context**: full-bleed black.
- **Layout**: Media3 `PlayerView`/Compose player full screen; overlay nhỏ "Bấm Back để bỏ qua" tự ẩn sau 3s.
- **State**: `Buffering / Playing / Ended / Error`.
- **Behavior**: autoplay; Back/OK skip → Home; Ended → Home. Idempotency theo `stayId` (không phát lại).
- **Initial focus**: vùng player (nhận Back/OK).

### 10.5. HomeScreen (hub)
- **Context**: app (navy).
- **Layout**: header "Chào {title} {name} · Phòng {roomNo}" (`title`, onApp); hàng 3 card lớn focusable: **Trợ lý (Voice)** · **Hóa đơn** · **Gọi món** (F5 ẩn nếu P2 chưa bật). Card = NavyElevated, icon gold, label.
- **State**: cố định theo session.
- **Initial focus**: card "Trợ lý".

### 10.6. FolioScreen (F4)
- **Context**: app (navy).
- **Layout**: headline "Hóa đơn phòng {roomNo}"; list `FolioLine` (ngày · mô tả · SL · thành tiền) trong card cuộn được bằng D-pad; footer "Tổng: {total}" (`title`, accent).
- **State**: `Loading / Loaded / Empty / Error`.
- **Initial focus**: list (cuộn). Không có nút thanh toán.

### 10.7. VoiceAssistantScreen (F3)
- **Context**: app (navy).
- **Layout**: vùng giữa hiển thị trạng thái (idle/đang nghe/đang trả lời); nút **mic on-screen** lớn (focus mặc định, gold khi focus, pulse khi nghe); khung text hiển thị câu hỏi + câu trả lời song song.
- **State**: `Idle / Listening / Thinking / Speaking / Error`.
- **Behavior**: OK trên nút mic → SpeechRecognizer vi-VN → `/iptv/assistant/ask` → TTS + text.
- **Initial focus**: nút mic.

---

## 11. Compose for TV — quy ước implementation

1. Dùng `androidx.tv:tv-material` (Surface, Card, Button, ListItem cho TV) — **không** dùng Material mobile cho element focusable.
2. **Focus**: mọi tương tác qua D-pad; `Modifier.focusRequester` + `onFocusChanged`; `bringIntoViewRequester` cho list dài. Test focus bằng **remote/emulator thật**, không tin `@Preview`.
3. Mỗi screen có `@Preview(device = Devices.TV_1080p)` + một preview state chính.
4. Safe area: padding §9.4; không đặt nội dung sát mép (overscan).
5. Không gesture cảm ứng, không `clickable` không-focusable. Min focusable size đủ lớn.
6. Animate focus (scale/elevation) mượt; tránh layout shift.
7. Video: Media3 `ExoPlayer`, release đúng lifecycle (DisposableEffect).

---

## 12. Cấu trúc dự án Kotlin

```
phs-iptv-tv/                      # package root: vn.phs.iptv
├── app/
│   ├── di/                       # Hilt modules
│   ├── data/
│   │   ├── remote/               # Retrofit API (§8), DTO mirror §5.3
│   │   ├── fcm/                  # FcmService (data message handler)
│   │   └── local/                # DataStore (provisioning config)
│   ├── domain/                   # Models, AppStateMachine
│   ├── ui/
│   │   ├── provisioning/         # §10.1
│   │   ├── idle/                 # §10.2
│   │   ├── welcome/              # §10.3
│   │   ├── video/                # §10.4 (Media3)
│   │   ├── home/                 # §10.5
│   │   ├── folio/                # §10.6
│   │   ├── assistant/            # §10.7
│   │   └── theme/                # Color.kt, Type.kt, Theme.kt (§9)
│   └── MainActivity.kt           # Kiosk host, nav
```

**App state machine:**
```
PROVISIONING → IDLE
IDLE --(FCM guest.checked_in)--> WELCOME --(5s/OK)--> INTRO_VIDEO --(end/skip)--> HOME
HOME → { FOLIO | VOICE | ORDER } → HOME
HOME --(FCM guest.checked_out)--> IDLE
```

---

## 13. Quyết định kiến trúc (ADR)

| ADR | Quyết định | Rejected |
|---|---|---|
| 1 | IPTV consume event + phụ thuộc interface §5, không chạm dữ liệu PMS nội bộ | Query thẳng storage PMS / poll làm nguồn chính |
| 2 | Hybrid: event payload cho Welcome, query interface cho folio | Pull tất cả / event mang cả folio |
| 3 | Voice nút on-screen + SpeechRecognizer | Intercept nút mic remote (bind Assistant) |
| 4 | FCM data push + fallback poll, idempotency `stayId` | Chỉ poll / chỉ push |
| 5 | TV chỉ read + voice + order, không pay | Thanh toán/nhập liệu trên TV |
| 6 | Code-first UI bằng Compose for TV (không vòng design tool) | Figma→Compose handoff cho 7 màn hình đơn giản |
| 7 | Device tự register (device-identity) + gán phòng ở PHS console | (a) nhập mã đối chiếu thủ công trên TV — UX xấu, lỗi nhập liệu; (b) MAC-based — Android chặn lấy MAC thật từ API 29. **Hướng gần chuẩn ngành hospitality IPTV**: bind device↔room một lần lúc lắp đặt, guest↔room do PMS quản lý tự động. Tận dụng lợi thế PHS sở hữu luôn PMS — gán phòng thực hiện trong cùng một console admin. |

---

## 14. Claude Code Build Guide

### 14.1. Prerequisites
1. Android Studio (bản mới nhất), JDK 17, Android SDK + một **Android TV emulator (Television 1080p)** hoặc thiết bị thật.
2. Firebase project → tải `google-services.json` đặt vào `app/` trước khi build.
3. Claude Code mở tại thư mục repo `phs-iptv-tv`.

### 14.2. Dependency (version catalog — chốt latest stable khi scaffold)

```toml
# gradle/libs.versions.toml (phiên bản tham chiếu — Claude Code resolve latest stable)
[versions]
agp = "8.x"
kotlin = "2.0.x"
composeBom = "2024.xx.xx"
tvMaterial = "1.0.0"          # androidx.tv:tv-material
media3 = "1.x.x"
hilt = "2.5x"
retrofit = "2.11.x"
kotlinxSerialization = "1.6.x"
firebaseBom = "33.x.x"
coil = "2.x.x"
datastore = "1.1.x"

[libraries]
androidx-tv-material = { group = "androidx.tv", name = "tv-material", version.ref = "tvMaterial" }
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-ui = { group = "androidx.media3", name = "media3-ui", version.ref = "media3" }
# ... compose-bom, hilt, retrofit, kotlinx-serialization, firebase-messaging, coil-compose, datastore-preferences
```

minSdk 28 · targetSdk latest · `<uses-feature android:name="android.software.leanback" android:required="true"/>` + intent-filter `LEANBACK_LAUNCHER`.

### 14.3. Thứ tự scaffold (chạy lần lượt với Claude Code)
1. Tạo project Android TV (Empty Compose Activity), set package `vn.phs.iptv`, cấu trúc §12, version catalog §14.2.
2. Generate `theme/` (Color.kt, Type.kt, Theme.kt) từ §9.
3. Generate `domain/AppStateMachine` + navigation theo §12.
4. Generate F0: ProvisioningScreen + IdleScreen (§10.1–10.2) + DataStore config + pair API stub.
5. Generate F1+F2: WelcomeScreen + IntroVideoScreen (§10.3–10.4, Media3) + FcmService nhận `guest.checked_in`.
6. Generate HomeScreen (§10.5).
7. Wire Retrofit API §8 + DTO mirror §5.3.
8. (Sau pilot) F4 Folio, F3 Voice, F5 Order.

### 14.4. Nội dung `CLAUDE.md` (copy vào root repo)

```md
# phs-iptv-tv — project guide for Claude Code

## What this is
Android TV (Google TV) IPTV app for PHS-Lite hotels. Kotlin + Compose for TV.
Spec of record: PRD_PHS_Lite_IPTV_AndroidTV_v0.3.md (đọc trước khi sửa UI/logic).

## Hard rules
- UI: Compose for TV only (androidx.tv:tv-material). Mọi element D-pad navigable, có gold focus ring (PRD §9.5).
- Brand: chỉ dùng token trong theme/Color.kt + Type.kt. KHÔNG hardcode hex trong screen (PRD §9).
- Thin client: không nhúng logic thuộc về PMS. Chỉ nói chuyện backend qua Retrofit API (PRD §8); DTO mirror PRD §5.3.
- Kiosk: app luôn foreground; không điều hướng ra launcher/settings.
- Persist: chỉ DataStore (config provisioning). Clear guest session khi check-out.
- i18n: mọi string trong res/values(-en)/strings.xml; ngôn ngữ theo guest nationality.

## State machine (PRD §12)
PROVISIONING → IDLE → (FCM checked_in) WELCOME → INTRO_VIDEO → HOME → {FOLIO|VOICE|ORDER}; HOME → (checked_out) IDLE

## Build / run
- ./gradlew assembleDebug
- Emulator: AVD "Television (1080p)"
- Đặt google-services.json vào app/ trước khi build

## Conventions
- Package root: vn.phs.iptv ; DI: Hilt ; Networking: Retrofit + kotlinx.serialization
- Một Composable / screen file dưới ui/<feature>/
- @Preview(device = Devices.TV_1080p) cho mọi screen
- Focus test phải trên emulator/remote, không tin @Preview
```

### 14.5. Chuỗi prompt khởi động (paste vào Claude Code)
1. "Đọc PRD và CLAUDE.md. Scaffold project Android TV theo §12: package vn.phs.iptv, Compose for TV, Hilt, Media3, FCM, Retrofit, DataStore; tạo gradle/libs.versions.toml theo §14.2 với latest stable."
2. "Generate theme/ (Color.kt, Type.kt, Theme.kt) đúng token §9, có 2 ngữ cảnh hero/app."
3. "Generate AppStateMachine + navigation §12, kèm ProvisioningScreen + IdleScreen §10.1–10.2 với @Preview TV_1080p."
4. "Generate WelcomeScreen §10.3 + IntroVideoScreen §10.4 (Media3) + FcmService xử lý guest.checked_in."
5. "Generate HomeScreen §10.5 + Retrofit API §8 + DTO §5.3."

---

## 15. Roadmap (linear, P0 trước)

1. Sprint 0 — Setup repo + scaffold (§14.3 bước 1–2).
2. Sprint 1 — F0 (provisioning + idle + kiosk + pair API).
3. Sprint 2 — Integration: PMS phát `guest.checked_in` (mọi đường check-in); IPTV nghe + session + FCM.
4. Sprint 3 — F1 + F2 + fallback poll. **→ Mốc pilot tối thiểu.**
5. Sprint 4 — Pilot 1 property, đo phản hồi.
6. Sprint 5 — F4 folio.
7. Sprint 6 — F3 voice AI.
8. Sprint 7 — F5 gọi món (khi `PosIptvPort` sẵn sàng).

---

## 16. Cần Việt xác nhận trước khi code (BLOCKING)

1. PMS có (hoặc sẽ thêm) event `guest.checked_in` / `guest.checked_out` theo §5.1?
2. Mọi đường check-in (gồm "nhận phòng nhanh") có phát cùng một event?
3. PMS expose được `PmsIptvQueryPort` §5.2 (`getInhouseGuest` + `getFolio`), cơ chế kết nối Việt muốn dùng?
4. **[v0.4 — mới]** PHS console cần thêm màn **"Gán phòng cho thiết bị đang online"**: hiển thị danh sách thiết bị đã đăng ký (deviceId + displayCode + trạng thái), cho phép admin bind từng thiết bị vào một `roomId`. Đây là luồng phía admin/backend mới — cần Việt confirm scope và timeline trước khi coi F0 là done thật.

---

## 17. Câu hỏi mở (non-blocking)

1. Video giới thiệu: chung/branch hay theo loại phòng/hạng khách?
2. Brand TV: brand PHS hay white-label cho từng khách sạn upload logo + video?
3. Thiết bị: Google TV consumer (cần MDM) hay TV hospitality thương mại?
4. TTS tiếng Việt: on-device hay cloud TTS ngay từ F3?
5. Logo asset: anh gửi file `logo_phs` (vector) để drop vào `res/drawable`.

---

## 18. Changelog

| Version | Thay đổi |
|---|---|
| v0.1 | Bản đầu — có tham chiếu data model legacy |
| v0.2 | Trừu tượng hóa kết nối IPTV↔PMS xuống interface/contract (§5). Bỏ tham chiếu schema/table |
| **v0.3** | Thêm Design System & token brand (§9), UI spec từng màn hình (§10), quy ước Compose for TV (§11), Claude Code Build Guide gồm deps/scaffold/CLAUDE.md/prompt (§14). Sẵn sàng codegen UI |
| **v0.4** | Đổi model provisioning F0: device auto-register + gán phòng ở PHS console (§8 mới: `/devices/register` + `/devices/me`; bỏ `/devices/pair`). Thêm ADR-7 (device-identity vs mã thủ công vs MAC). Thêm blocking item §16.4 (admin console cần màn gán phòng cho thiết bị). |

---

*Tài liệu định nghĩa app IPTV + module `iptv` + bề mặt interface với PMS. Cách PMS tổ chức do Việt quyết. Pricing/packaging ở `Pricing_Policy_PHS_Lite` riêng.*
