# Exchange-rate collector miễn phí

Collector dùng hai nguồn không mất phí, không cần tài khoản và không cần API key:

- **Frankfurter v2**: bảng tỷ giá tham chiếu tổng hợp từ 84 ngân hàng trung ương, catalog hiện có 165 mã. Collector mặc định bỏ bốn mã kim loại `XAG`, `XAU`, `XPD`, `XPT`, còn 161 mã tiền tệ/đơn vị tiền tệ và quy đổi theo VND.
- **Vietcombank**: ba giá `cash`, `transfer`, `sell` cho 20 mã Vietcombank công bố.

Collector chỉ dùng Python standard library và không thay đổi ứng dụng Android TV.

## Giới hạn phải biết

Frankfurter là dữ liệu chính thức theo ngày. Từng cặp tiền có thể được ngân hàng trung ương công bố vào thời điểm khác nhau, vì vậy một snapshot có trường `rate_date` riêng cho từng mã. Chạy theo giờ giúp lưu lại mọi lần bảng tổng hợp thay đổi kể từ khi collector bắt đầu, nhưng không thể khôi phục các snapshot intraday đã bỏ lỡ.

Vietcombank cũng không cung cấp lịch sử intraday qua API công khai. Collector chỉ có thể lưu các thay đổi từ lúc tiến trình bắt đầu chạy.

## Lấy dữ liệu toàn cầu

Chụp toàn bộ 161 mã hiện tại theo VND:

```powershell
python tools\exchange_rates\collector.py global-once --date today
```

Chạy liên tục mỗi giờ, chỉ append khi bảng tỷ giá thay đổi:

```powershell
python tools\exchange_rates\collector.py global-watch --date today --interval 3600
```

Muốn giữ cả bốn mã kim loại:

```powershell
python tools\exchange_rates\collector.py global-once --date today --include-metals
```

Khởi động/dừng tiến trình toàn cầu chạy nền trên Windows:

```powershell
powershell -ExecutionPolicy Bypass -File tools\exchange_rates\start-global-watch.ps1
powershell -ExecutionPolicy Bypass -File tools\exchange_rates\stop-global-watch.ps1
```

## Thu thập Vietcombank

Chụp một lần:

```powershell
python tools\exchange_rates\collector.py vcb-once --date today
```

Chạy foreground mỗi 310 giây:

```powershell
python tools\exchange_rates\collector.py vcb-watch --date today --interval 310
```

Khởi động/dừng tiến trình nền:

```powershell
powershell -ExecutionPolicy Bypass -File tools\exchange_rates\start-vcb-watch.ps1
powershell -ExecutionPolicy Bypass -File tools\exchange_rates\stop-vcb-watch.ps1
```

Cả hai watch command tự chuyển sang file ngày mới khi dùng `--date today`.

## Kiểm tra dữ liệu

```powershell
python tools\exchange_rates\collector.py status --date today
```

`latest_validation.is_complete=true` xác nhận snapshot Frankfurter có đầy đủ mọi mã trong catalog sau khi lọc kim loại. `unique_snapshots` cho biết số bảng khác nhau đã thu được trong ngày.

## File đầu ra

```text
data/exchange-rates/
├── global/
│   ├── currencies-frankfurter.json
│   └── YYYY-MM-DD/frankfurter-VND.jsonl
├── vietcombank/
│   └── YYYY-MM-DD.jsonl
└── runtime/
    ├── global-watch.*
    └── vcb-watch.*
```

JSON Lines được dùng để append từng snapshot và bỏ qua dữ liệu trùng nội dung. File catalog chứa tên, ký hiệu, mã ISO và khoảng ngày có dữ liệu của từng tiền tệ.
