# Kế hoạch triển khai: Timeline theo ngày làm việc

**Branch identifier**: `008-working-day-timeline` (workspace hiện không có Git metadata) | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả timeline theo tỷ lệ thời gian làm việc của từng ngày, bỏ giờ nghỉ/ngày nghỉ và giữ các cột ngày cuối tuần rỗng.

## Summary

Thay projection calendar theo số ngày inclusive bằng các đoạn timeline được chiếu trên từng ngày làm việc với tọa độ bắt đầu/kết thúc chuẩn hóa trong khoảng 0–1 của ngày 8 giờ. Các phiên 08:00–12:00 và 13:00–17:00 được nối liền trên thang tỷ lệ ngày; khoảng nghỉ trưa, ngoài giờ và thứ Bảy/Chủ nhật không đóng góp chiều dài. Trục ngày liên tục, các cột cuối tuần rỗng, cửa sổ calendar, thứ tự hàng và quy tắc màu/mốc nghiệp vụ hiện có vẫn được giữ. Không thay đổi dữ liệu lưu, API Jira hay dependency.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, kotlinx-datetime 0.8.0, kotlinx-coroutines 1.11.0; đều đã có trong version catalog.  
**Storage**: Không đổi. Calendar vẫn là projection chỉ đọc từ snapshot `SavedTable`/`Ticket` hiện có.  
**Testing**: Test-first trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableCalendarTest.kt`; kiểm thử hiển thị qua build shared/JVM hiện có. Xác nhận bằng `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`.  
**Target Platform**: Ứng dụng Desktop JVM; projection ở `commonMain` để giữ khả năng đa nền tảng.  
**Project Type**: Compose Multiplatform desktop app, module `composeApp`.  
**Performance Goals**: Không có request mạng hoặc công việc lưu trữ mới. Projection bị giới hạn bởi cửa sổ calendar hiện hữu (xấp xỉ 4 tháng ngày); thời gian xử lý tăng tuyến tính theo số ticket nhân số ngày trong cửa sổ. Compose chỉ vẽ các segment thuộc vùng đang hiển thị.  
**Constraints**: Dùng timezone Jira đã lưu. Lịch chuẩn thứ Hai–thứ Sáu, 08:00–12:00 và 13:00–17:00; 8 giờ công được ánh xạ thành toàn chiều ngang của ô ngày. Cuối tuần vẫn nằm trên trục ngày nhưng không vẽ segment. Không tính ngày lễ hoặc lịch làm việc tùy chỉnh. Mốc bắt đầu/kết thúc dự kiến dùng instant hiện có; phần quá hạn giữ mốc bắt đầu nghiệp vụ là ngày sau Dynamic Due Date và mốc kết thúc Real Due Date hiện có.  
**Scale/Scope**: Tất cả ticket của một snapshot bảng đã lưu trong cửa sổ calendar cố định. Chỉ thay projection và render timeline; không đổi schema, nguồn dữ liệu, sort, navigation hoặc cấu hình calendar.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Phép chiếu giờ/ngày dùng `kotlinx-datetime` trong `commonMain`; không gọi API JVM. |
| II. Kiểm thử trước | PASS | Bổ sung test `commonTest` cho tỷ lệ nửa ngày, các mốc giữa ngày, nghỉ trưa, cuối tuần, clipping, timezone và phần quá hạn trước khi thay projection/render. |
| III. Hybrid UI | PASS | Domain tính các segment timeline thuần; Compose chỉ render tọa độ và màu. Không đưa quy tắc ngày làm việc vào UI. |
| IV. Kotlin an toàn kiểu | PASS | Dùng kiểu dữ liệu projection bất biến, timestamp hiện có và dependency đã khai báo; không thêm thư viện. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng cửa sổ calendar, ticket snapshot và quy tắc ngày làm việc hiện hành; không thêm module hoặc dữ liệu persistent. |

Không có `[NEEDS CLARIFICATION]` trong đặc tả đã clarify. Nghiên cứu xác nhận thiết kế dựa trên các helper và cấu trúc hiện có.

### Sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Hành vi UX | PASS | Trục ngày và cột cuối tuần giữ nguyên; cuối tuần không có thanh, phần trong ngày làm việc giữ vị trí tỷ lệ. |
| Mô hình dữ liệu | PASS | Chỉ mở rộng projection tạm thời bằng segment theo ngày; snapshot và schema không đổi. |
| Ranh giới đa nền tảng | PASS | Chuyển instant/local sessions dùng `kotlinx-datetime` trong shared domain. |
| Kiểm thử/tích hợp | PASS | Bao phủ bằng `commonTest`; không có Jira/network integration mới. |
| Hiệu năng/phạm vi | PASS | Cửa sổ có giới hạn; render chỉ các segment của ngày nhìn thấy; không phát sinh I/O. |
| Tính đơn giản | PASS | Dùng helper lịch làm việc hiện có hoặc mở rộng nó tại một nơi; không tạo hệ thống lịch làm việc mới. |

Không có vi phạm hiến pháp cần Complexity Tracking.

## Thiết kế

1. **Nguồn lịch làm việc**: Giữ một nguồn chuẩn cho ngày làm việc, giờ bắt đầu/kết thúc và nghỉ trưa, đồng nhất với `WorkingTimeCalculator`. Tránh tạo các con số lịch riêng trong calendar. Nếu cần chia sẻ thêm helper cho projection, mở rộng domain calculator hiện hữu thay vì tạo module/dependency mới.
2. **Projection timeline**: Mở rộng `SavedTableCalendarRow` để cung cấp danh sách segment cho thanh dự kiến và segment quá hạn. Mỗi segment liên kết một `dayOffset` với `startFraction`/`endFraction` trong [0, 1] của ô ngày làm việc. Projection duyệt các ngày trong cửa sổ, bỏ cuối tuần, giao khoảng instant với hai phiên làm việc địa phương và cộng thời lượng phiên theo thứ tự; giờ nghỉ trưa không tạo khoảng cách trên tọa độ nội bộ của ngày.
3. **Mốc và tỷ lệ**: Với thanh dự kiến, dùng `dynamicStartAtMillis` và `dynamicDueAtMillis` chính xác thay vì làm tròn về `LocalDate`. Với mỗi phiên, phần giao bắt đầu/kết thúc xác định vị trí tương ứng trên thang 8 giờ. Mốc ngoài phiên chỉ tạo phần giao trong phiên làm việc; khoảng rỗng hoặc thiếu/sai mốc không tạo segment. Xử lý theo timezone Jira để các phiên địa phương giữ giờ chuẩn qua ngày và DST.
4. **Phần quá hạn**: Giữ điều kiện và màu đỏ hiện tại. Tạo khoảng nguồn bắt đầu từ đầu ngày lịch kế tiếp Dynamic Due Date và kết thúc ở instant Real Due Date; chỉ phần giao với phiên làm việc mới được chiếu thành segment. Ngày nghỉ và giờ nghỉ không vẽ hoặc cộng thêm thời lượng. Giữ dấu tiếp diễn/clipping hiện hữu theo biên cửa sổ.
5. **Render**: Cập nhật `SavedTableCalendarArea` để vẽ mỗi segment trong ô ngày tương ứng, dùng `DAY_COLUMN_WIDTH * fraction` cho vị trí và chiều dài. Giữ scroll offset/header/grid hiện tại; không vẽ segment trong cột thứ Bảy/Chủ nhật. Thanh dự kiến và thanh quá hạn dùng cùng phép chiếu với màu tương ứng hiện có.
6. **Validation**: TDD domain test bằng fixture epoch/timezone; xác nhận 4 giờ = 50% ô, mốc 10:00 ở 25% ngày, 14:00 ở 62.5%, nối liền qua giờ nghỉ trưa, không có segment cuối tuần, chia đúng phần trong nhiều ngày, cùng clipping/continuation và quá hạn. Chạy `jvmTest`, ktlint và build sau tích hợp.

## Project Structure

### Documentation

```text
specs/008-working-day-timeline/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
└── quickstart.md
```

Không tạo `contracts/`: feature chỉ thay đổi projection/render nội bộ, không thêm endpoint hoặc hợp đồng với hệ thống ngoài.

### Source Code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   ├── SavedTableCalendar.kt
│   └── WorkingTimeCalculator.kt
└── ui/saved/
    └── SavedTableCalendarArea.kt

composeApp/src/commonTest/kotlin/com/jiratools/domain/
└── SavedTableCalendarTest.kt
```

**Structure Decision**: Dùng cấu trúc KMP hiện có trong một module `composeApp`: phép chiếu và quy tắc lịch ở `domain`, vẽ segment ở Compose saved-table calendar, test domain ở `commonTest`.

## Complexity Tracking

Không có vi phạm hiến pháp.
