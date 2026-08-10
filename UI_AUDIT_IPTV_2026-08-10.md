# IPTV UI audit — 2026-08-10

## Phạm vi kiểm tra

- Thiết bị thật: ViettelTV 4K Box (Android 10 / API 29), `1920 × 1080`, density `320 dpi`.
- Điều khiển bằng ADB: cold start, HOME/BACK/DPAD/OK, focus hierarchy, screenshot và logcat.
- Màn hình đã rà: Provisioning, Idle, Welcome/Language, Intro video, Home, Live TV, Folio, Room service, Hotel services, Help và Voice assistant.
- Build xác nhận: `:app:assembleDebug` thành công; không có crash runtime trong vòng kiểm tra.

## Chuẩn UI thống nhất đề xuất

1. Dark/cinematic là theme mặc định của IPTV; ảnh/video chỉ làm nền, nội dung luôn nằm trên scrim đủ tương phản.
2. Champagne gold là accent duy nhất cho focus, CTA và số liệu quan trọng.
3. Overscan-safe: trái/phải tối thiểu `80dp`, trên/dưới `60dp`; không đặt text hoặc focus ring sát mép TV.
4. Radius dùng một thang duy nhất: `8 / 12 / 18 / 24dp`; focus ring `3dp`, scale khoảng `1.06`, animation `150ms`.
5. Mỗi màn hình phải có focus mặc định xác định; trạng thái loading/empty/error vẫn phải còn nút Quay lại hoặc CTA focusable.
6. Màn hình con dùng cùng một `AppleSubHeader`: Quay lại — tiêu đề — metadata phòng.
7. Toàn bộ copy guest-facing phải theo ngôn ngữ đã chọn; không trộn Việt/Anh/Nga trong cùng một màn hình.

## Kết quả theo màn hình

| Màn hình | Kết quả | Vấn đề chính |
|---|---|---|
| Provisioning | Khá đồng bộ | Copy chỉ có tiếng Anh; nên coi đây là staff UI hoặc thêm song ngữ. |
| Idle | Bố cục cinematic tốt | Ngày, Room/Wi‑Fi/Password/Hotline/Offline đang hard-code tiếng Anh. Dùng chung video background nên chịu lỗi white shutter. |
| Welcome / Language | Focus card rõ, vùng an toàn tốt | Cold start có thể trắng 7–15 giây trước frame video đầu tiên. Theme toggle nằm thấp và Light mode chưa đồng bộ với màn hình con. |
| Intro video | Full-bleed đúng kiểu IPTV | Đã sửa lỗi một lần OK vừa skip video vừa kích hoạt tile ở Home. |
| Home | Hệ phân cấp tốt nhất app | Widget có mật độ chữ chưa đều; badge còn hard-code; dữ liệu demo xuất hiện ở debug; tên guest xấu từ PMS cần rule fallback/sanitize. |
| Live TV | Header/focus/i18n đã đồng bộ | Channel vẫn lấy từ `Demo.channels`; chọn kênh chưa phát nội dung thật — đây là khoảng trống chức năng IPTV lớn nhất. |
| Folio | Đồng bộ, dễ đọc | Nên bổ sung skeleton/retry khi API lỗi thay vì chỉ giữ dữ liệu trống. |
| Room service | QR flow rõ | Empty state nên có CTA gọi lễ tân/Help; QR lớn cần kiểm tra khả năng quét ở khoảng cách phòng thực tế. |
| Hotel services | Row và hours chip hợp hệ thống | Empty/loading state còn quá thô; đã sửa focus mặc định về nút Quay lại. |
| Help | Cấu trúc rõ, Wi‑Fi nổi bật | Kiểm tra intent gọi điện/map trên TV box không có dialer; cần fallback QR hoặc hướng dẫn. |
| Voice assistant | Visual đơn giản, focus mic rõ | Chỉ hiện ở debug, copy tiếng Anh nhưng recognizer/TTS cố định `vi-VN`, backend trả trạng thái “chưa khả dụng”. Không nên đưa vào release trước khi hoàn thiện. |

## Backlog ưu tiên

### P0 — chặn trải nghiệm chính

- [x] Chặn toàn bộ KeyDown/KeyUp của OK trên Intro video để không click xuyên sang Home.
- [ ] Thay white shutter của video nền bằng black/poster fallback. GitNexus đánh CRITICAL: 3 caller trực tiếp, 6 flow, 4 module; cần test riêng Language + Idle + Welcome trước khi merge.
- [ ] Thay `Demo.channels` bằng nguồn channel/EPG/player thật; hiện Live TV mới là mock UI và `onClick` không làm gì.
- [ ] Chốt ownership ADB/worktree khi test; không chạy hai phiên điều khiển remote và sửa theme cùng lúc.

### P1 — đồng bộ và khả dụng

- [x] Live TV mở với header còn trên màn hình, focus mặc định ở Quay lại và nhãn Việt/Anh/Nga theo lựa chọn.
- [x] Hotel services luôn có focus an toàn khi loading/empty.
- [ ] Chọn một chiến lược theme: chỉ dark cho toàn app, hoặc bỏ mọi `PhsAppTheme {}` lồng để Light mode được kế thừa trên tất cả màn hình. Trạng thái hiện tại vẫn là Light ở Language/Home nhưng sub-page tự quay về dark.
- [ ] Tạo component chung `LoadingState / EmptyState / ErrorState` có icon, mô tả ngắn, CTA và focus.
- [ ] Đưa copy hard-code của Idle, Live badge, Home badge, Voice và Provisioning vào hệ i18n.
- [ ] Format ngày theo `AppLanguage`, không cố định `Locale.ENGLISH`.
- [ ] Chuẩn hóa dữ liệu guest từ PMS: trim, không hiện tên rỗng/test, có fallback “Quý khách”.

### P2 — hoàn thiện hình ảnh

- [ ] Cân lại hierarchy của bốn Home widget: title/value/subtitle cùng baseline, badge ngắn và cùng ngôn ngữ.
- [ ] Kiểm tra focus path đầy đủ bằng remote thật: rail ↔ hero ↔ action row ↔ shelves, không chỉ ADB.
- [ ] Thêm poster placeholder và crossfade cho mọi ảnh mạng; giữ layout không nhảy khi ảnh tải.
- [ ] Kiểm tra 720p và overscan 5%; hiện audit chính mới ở 1080p/320dpi.
- [ ] Chụp golden screenshots cho Dark Home, Language, Live TV, Folio, Order empty/QR, Services empty/data và Help.

## Ma trận nghiệm thu vòng kế tiếp

- Cold start: không trắng quá 300ms; có black/poster fallback ngay frame đầu.
- HOME luôn về Language; BACK từ màn hình con về Home; BACK ở Home về Language; không thoát kiosk.
- Một lần OK ở Intro chỉ về Home, không mở tile.
- Mỗi route có đúng một focus ban đầu và focus ring nhìn rõ ở khoảng cách 2–3m.
- VI/EN/RU không còn copy trộn ngôn ngữ ngoài tên thương hiệu/kênh.
- Debug và release được kiểm riêng; release không lộ demo shelf/Voice chưa hoàn thiện.
- Không có text/focus ring nằm ngoài vùng an toàn 80/60dp ở 1080p và 720p.

## Bằng chứng ADB

- `shots/audit-home-real.png`: Home trước đợt sửa, focus action tile.
- `shots/audit-livetv-result.png`: Live TV sau sửa, header còn hiển thị và focus ở Quay lại.
- `shots/audit-after-cold.png`: frame trắng khi cold start (white shutter).
- `shots/audit-skip-result.xml`: sau một lần OK skip, vẫn ở Home thay vì mở Room service.
- `shots/audit-livetv-result.xml`: hierarchy xác nhận `Truyền hình`, `8 kênh`, `Đang phát`, `Tất cả kênh` và focus Quay lại.
