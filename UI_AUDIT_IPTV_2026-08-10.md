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
| Provisioning | Đã đồng bộ | Copy Việt/Anh, focus Làm mới và debug bypass cùng hàng, nằm trọn safe area. Debug bypass không có trong release. |
| Idle | Bố cục cinematic tốt | White shutter đã được xử lý ở launch window và PlayerView; copy ngày/Room/Wi‑Fi còn ưu tiên tiếng Anh vì chưa có guest chọn ngôn ngữ. |
| Welcome / Language | Focus card rõ, vùng an toàn tốt | Cold start đã có nền `#0C0D11` ngay frame đầu; Light mode được kế thừa trên màn hình con. |
| Intro video | Full-bleed đúng kiểu IPTV | Đã sửa lỗi một lần OK vừa skip video vừa kích hoạt tile ở Home. |
| Home | Hệ phân cấp tốt nhất app | Đã bỏ fallback phòng/tiền giả, badge guest-facing hard-code và ngày cố định `Locale.ENGLISH`; tên guest được trim/chặn placeholder. |
| Live TV | Header/focus/i18n đã đồng bộ | Contract v1 không cung cấp channel/stream và ghi rõ Live TV/VOD ngoài phạm vi; production đã gate tính năng, mock chỉ còn ở debug. |
| Folio | Đồng bộ, dễ đọc | Nên bổ sung skeleton/retry khi API lỗi thay vì chỉ giữ dữ liệu trống. |
| Room service | QR flow rõ | Empty state nên có CTA gọi lễ tân/Help; QR lớn cần kiểm tra khả năng quét ở khoảng cách phòng thực tế. |
| Hotel services | Row và hours chip hợp hệ thống | Empty/loading state còn quá thô; đã sửa focus mặc định về nút Quay lại. |
| Help | Cấu trúc rõ, Wi‑Fi nổi bật | Kiểm tra intent gọi điện/map trên TV box không có dialer; cần fallback QR hoặc hướng dẫn. |
| Voice assistant | Visual đơn giản, focus mic rõ | Chỉ hiện ở debug, copy tiếng Anh nhưng recognizer/TTS cố định `vi-VN`, backend trả trạng thái “chưa khả dụng”. Không nên đưa vào release trước khi hoàn thiện. |

## Backlog ưu tiên

### P0 — chặn trải nghiệm chính

- [x] Chặn toàn bộ KeyDown/KeyUp của OK trên Intro video để không click xuyên sang Home.
- [x] Thay white shutter bằng launch window + PlayerView nền `#0C0D11`, giữ frame khi reset và chuẩn hóa URI video. Đã test cold start thật trên box.
- [x] Gate `Demo.channels`/Live TV khỏi production vì API v1 chưa có channel/EPG/stream; mock chỉ hiển thị trong debug. Khi backend bổ sung contract thì mới nối player thật.
- [ ] Chốt ownership ADB/worktree khi test; không chạy hai phiên điều khiển remote và sửa theme cùng lúc.

### P1 — đồng bộ và khả dụng

- [x] Live TV mở với header còn trên màn hình, focus mặc định ở Quay lại và nhãn Việt/Anh/Nga theo lựa chọn.
- [x] Hotel services luôn có focus an toàn khi loading/empty.
- [x] `PhsAppTheme` lồng kế thừa `LocalAppThemeMode`; đã test Light trên Home, Live TV debug, Folio, Order, Services và Help.
- [x] Tạo `ContentStateMessage`/`ContentStatePanel` chung cho Folio empty, Services loading/empty và Order unavailable; CTA vẫn giữ focus theo từng màn.
- [ ] Đưa copy hard-code của Idle, Live badge, Home badge, Voice và Provisioning vào hệ i18n.
- [x] Format ngày Home theo `AppLanguage` (`vi-VN`, `en-US`, `ru-RU`).
- [x] Chuẩn hóa guest từ PMS: trim/gộp khoảng trắng, giới hạn 80 ký tự, chặn placeholder và fallback `Guest`.

### P2 — hoàn thiện hình ảnh

- [ ] Cân lại hierarchy của bốn Home widget: title/value/subtitle cùng baseline, badge ngắn và cùng ngôn ngữ.
- [ ] Kiểm tra focus path đầy đủ bằng remote thật: rail ↔ hero ↔ action row ↔ shelves, không chỉ ADB.
- [ ] Thêm poster placeholder và crossfade cho mọi ảnh mạng; giữ layout không nhảy khi ảnh tải.
- [ ] Kiểm tra 720p và overscan 5%; hiện audit chính mới ở 1080p/320dpi.
- [ ] Chụp golden screenshots cho Dark Home, Language, Live TV, Folio, Order empty/QR, Services empty/data và Help.

## Ma trận nghiệm thu vòng kế tiếp

- Cold start: nền tối xuất hiện ngay starting window; không còn frame trắng trước Compose/video.
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
- `shots/audit-cold-dark-final.png`: frame đầu cold start đã là nền tối thay vì trắng.
- `shots/audit-home-light-final.png`: Home Light Mode, ngày Việt hóa và dữ liệu PMS thật.
- `shots/audit-folio-light-final.xml`, `audit-order-light-final.xml`, `audit-services-light-final.xml`, `audit-help-light-final.xml`: regression theme/focus trên các màn con.
