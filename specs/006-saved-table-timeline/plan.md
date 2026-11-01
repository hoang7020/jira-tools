# Kế hoạch triển khai: Calendar cho Bảng đã lưu

**Branch identifier**: `006-saved-table-timeline` (workspace hiện không có Git metadata) | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả calendar riêng cho một Bảng đã lưu, có ngày nằm ngang và mã ticket theo hàng dọc.

## Summary

Calendar riêng dùng một cửa sổ lăn cố định từ ngày hiện tại trừ một tháng lịch đến ngày hiện tại cộng ba tháng lịch, tính cả hai đầu theo múi giờ Jira của bảng. Mỗi ngày có một ô riêng; các khoảng Dynamic Start Time–Dynamic Due Date được chiếu lên phần giao với cửa sổ, cắt và đánh dấu nếu tiếp diễn qua mép, hoặc thông báo nếu hoàn toàn ngoài khung. Cột mã ticket cố định trong khi phần ngày cuộn ngang. Cửa sổ vẫn hiển thị khi không có khoảng task hợp lệ nếu timezone hợp lệ. Không thêm request Jira, dữ liệu persistent hoặc thư viện calendar.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2; kotlinx-datetime 0.8.0 và kotlinx-coroutines hiện có. Không thêm dependency.  
**Storage**: Không có thay đổi lưu trữ. Calendar là projection tạm thời từ `SavedTable`, `Ticket` và `SavedTable.jiraTimeZone` hiện có.  
**Testing**: Logic projection/window/intersection trong `commonTest`; ViewModel state/navigation và thời điểm mở calendar trong `SavedTablesViewModelTest`; chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`. Kiểm tra bố cục và cuộn trực quan qua kịch bản quickstart.  
**Target Platform**: Ứng dụng Desktop JVM; UI và phép chuyển ngày dùng mã Compose/Kotlin Multiplatform trong `commonMain`.  
**Project Type**: Ứng dụng Compose Multiplatform, module hiện có `composeApp`.  
**Performance Goals**: Không thêm request mạng. Trục ngày có kích thước bị giới hạn bởi cửa sổ bốn tháng lịch; dùng danh sách lazy và chỉ vẽ phần thanh trong vùng ngày hiển thị. Không đặt giới hạn task mới ngoài quy mô bảng hiện tại.  
**Constraints**: Nút chỉ xuất hiện khi có bảng được chọn. Calendar là màn hình con riêng trong mục Bảng đã lưu, không hiển thị đồng thời bảng kết quả task. Cửa sổ ngày lịch liên tục, gồm cuối tuần, được tính từ ngày hiện tại ở timezone Jira của bảng; mốc hiện tại được chụp khi mở calendar để cửa sổ ổn định trong phiên xem và tính lại khi mở lại. Nếu timezone hợp lệ, cửa sổ vẫn tồn tại khi bảng rỗng hoặc không có khoảng hợp lệ. Ngày task được lấy từ instant theo timezone bảng, không theo timezone thiết bị. Không thay đổi cách tính hoặc lưu Dynamic Start Time/Due Date.  
**Scale/Scope**: Một bảng đã lưu và các task đang có trong snapshot đó. Không thêm endpoint, schema, module hay hành vi chỉnh sửa lịch trình.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Dùng Compose Multiplatform và kotlinx-datetime đã có trong `commonMain`; không dùng API thời gian riêng JVM. |
| II. Kiểm thử trước | PASS | Viết test `commonTest` cho phép tính cửa sổ tháng, timezone, giao/cắt khoảng, trạng thái ngoài khung và state mở/đóng/sort trước khi triển khai; không cần request Jira thật. |
| III. Hybrid UI | PASS | State mở calendar, thời điểm tham chiếu và thứ tự sort được phơi qua `SavedTablesUiState`; phép tạo cửa sổ/khoảng ngày ở domain, Compose chỉ hiển thị projection. |
| IV. Kotlin an toàn kiểu | PASS | Dùng model bất biến/kiểu ngày hiện có, không thêm dependency hoặc serialization. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng `SavedTable`, `Ticket`, timezone và sort hiện có; không thêm module, persistence hay thư viện calendar. |

Không có `[NEEDS CLARIFICATION]` trong đặc tả đã làm rõ. Giai đoạn nghiên cứu tập trung vào cách tận dụng trạng thái bảng và dependency sẵn có.

### Sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Hành vi UX | PASS | Mở calendar riêng từ bảng đang chọn; cửa sổ cố định cuộn được, cột ticket luôn đứng yên; nút quay lại giữ bảng và sort hiện tại. |
| Mô hình dữ liệu | PASS | Projection chỉ đọc; không thay schema hoặc lưu dữ liệu dẫn xuất. |
| Ranh giới đa nền tảng | PASS | Chuyển instant thành ngày địa phương dùng kotlinx-datetime trong `commonMain`. |
| Kiểm thử/tích hợp | PASS | Unit test `commonTest` bao phủ timezone, khoảng ngày và hàng; không có tích hợp Jira mới. |
| Hiệu năng/phạm vi | PASS | Không phát sinh network; chỉ có một cửa sổ xấp xỉ bốn tháng lịch, danh sách task cuộn dọc và ngày dùng LazyRow. |
| Tính đơn giản | PASS | Dùng một màn hình Compose chuyên biệt trong mục Bảng đã lưu, không thêm điều hướng ứng dụng hoặc dependency mới. |

Không có vi phạm hiến pháp cần ghi nhận trong Complexity Tracking.

## Thiết kế

1. **Luồng mở/đóng và thời điểm tham chiếu**: Màn hình calendar riêng mở từ chi tiết bảng và thay phần xem bảng; quay lại phục hồi đúng bảng/sort. `SavedTablesViewModel` chụp `AppClock.nowMillis()` khi mở và giữ trong `SavedTablesUiState` tới khi đóng/đổi bảng; mở lại lấy mốc mới. Đây là state phiên UI, không ghi vào catalog.
2. **Projection miền**: Cập nhật projection thuần `domain/SavedTableCalendar.kt` để nhận `nowMillis`, timezone và ticket đã sắp xếp. Chuyển instant hiện tại thành ngày địa phương trong timezone bảng. Tính `windowStart` từ ngày đó trừ một tháng lịch và `windowEnd` từ ngày đó cộng ba tháng lịch; tính riêng từng tháng đích và kẹp day-of-month vào ngày cuối tháng nếu cần. Cửa sổ gồm cả hai đầu và tạo mọi ngày liên tục; khi timezone hợp lệ, cửa sổ tồn tại cả khi không có ticket hoặc khoảng hợp lệ.
3. **Giao khoảng và trạng thái hàng**: Giữ một hàng cho mỗi ticket. Chỉ khoảng có cả hai ngày hợp lệ và due không trước start mới tạo thanh. Thanh hiển thị là giao inclusive giữa khoảng task và cửa sổ; lưu offset/độ dài sau khi cắt cùng cờ tiếp diễn trước/sau. Khoảng hợp lệ hoàn toàn trước/sau cửa sổ có hướng ngoài khung và không có thanh. Dữ liệu thiếu, sai hoặc timezone không hợp lệ giữ hàng nhưng không tạo thanh; lý do được trình bày rõ. Không cho task mở rộng cửa sổ.
4. **Lưới lịch**: Cập nhật `SavedTableCalendarArea` hiện có. Cột mã ticket cố định bên trái, header ngày và thanh ở cột calendar bên phải chia sẻ chính xác trạng thái cuộn ngang; hàng cuộn dọc. Ngày có nhãn thứ và ngày/tháng/năm, gồm cuối tuần; ngày hôm nay là vị trí cuộn ban đầu, nhưng vẫn có thể cuộn ngược về đầu cửa sổ và xuôi tới cuối cửa sổ. Dùng LazyRow và projection để chỉ vẽ phần đang nhìn thấy; hai đầu thanh inclusive, khoảng cùng ngày có dấu nhìn thấy được. Thông báo thiếu/sai/ngoài khung nằm trong nhãn hàng hoặc vùng thông báo, không thêm cột dữ liệu thứ ba.
5. **Rỗng và dữ liệu mới**: Với timezone hợp lệ, không có khoảng hợp lệ thì vẫn dựng cửa sổ ngày và giữ các hàng hiện có nhưng không có thanh; hiển thị giải thích. Timezone sai/thiếu hiển thị cảnh báo và không dựng trục không xác định được. Calendar chiếu snapshot mới nhất từ StateFlow; refresh vẫn thay dữ liệu ticket trong khi mốc cửa sổ được giữ ổn định tới lúc đóng. Mở lại tính mốc hiện tại mới; không gọi Jira khi dựng projection hoặc mở màn hình.
6. **Kiểm thử**: Mở rộng `SavedTableCalendarTest` trong `commonTest` cho chuyển timezone, tháng/năm, cuối tháng và năm nhuận, cửa sổ inclusive, ngày liên tục, thanh giao/cắt cả hai mép, khoảng hoàn toàn ngoài khung về hai phía, cùng ngày, thiếu/sai mốc, timezone không hợp lệ, không có khoảng hợp lệ nhưng vẫn có trục và thứ tự hàng. Mở rộng `SavedTablesViewModelTest` cho chụp/reset mốc thời gian, mở/đóng, sort và chọn bảng khác. Dùng quickstart để xác nhận bố cục và cuộn thực tế.

## Project Structure

### Tài liệu của feature

```text
specs/006-saved-table-timeline/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── saved-table-calendar.md
└── tasks.md                 # sinh riêng bởi /speckit-tasks
```

### Source code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   ├── SavedTable.kt                         # hiện có: bảng và timezone
│   ├── Ticket.kt                             # hiện có: Dynamic Start/Due instants
│   └── SavedTableCalendar.kt                 # cập nhật: cửa sổ ngày và khoảng đã cắt
├── ui/saved/
│   ├── SavedTablesViewModel.kt               # state calendar/sort tạm thời
│   ├── SavedTablesArea.kt                    # nút mở, màn hình con, quay lại
│   └── SavedTableCalendarArea.kt             # cập nhật: cuộn giới hạn và cột ticket cố định
└── app/
    └── App.kt                                # wiring callback hiện có sang ViewModel

composeApp/src/commonTest/kotlin/com/jiratools/
├── domain/
│   └── SavedTableCalendarTest.kt              # cập nhật: cửa sổ, giao khoảng, timezone
└── ui/saved/
    └── SavedTablesViewModelTest.kt            # mở/đóng và sort state
```

**Structure Decision**: Giữ một module KMP hiện có. Projection ngày nằm trong domain shared; điều hướng tạm và sort thuộc StateFlow của `SavedTablesViewModel`; Compose riêng trong `ui/saved`. Không thay đổi `SavedTableStore`, JSON schema, Jira API hoặc `Ticket`.

## Ánh xạ yêu cầu tới thiết kế

| Yêu cầu | Thành phần chính | Điều kiện kiểm chứng |
|---|---|---|
| FR-001, FR-008 | `SavedTablesArea`, `SavedTableCalendarArea`, `SavedTablesViewModel` | Nút mở calendar riêng cho bảng đang chọn; đóng quay lại đúng bảng; dữ liệu sau refresh được đọc từ snapshot hiện tại. |
| FR-002, FR-004, FR-007, FR-010 | `SavedTableCalendar` projection, header/trục trong `SavedTableCalendarArea` | Ngày/thứ đúng timezone; cửa sổ liên tục, inclusive, clamp cuối tháng; thanh giao bị cắt đúng và đánh dấu tiếp diễn; không cuộn ngoài biên. |
| FR-003, FR-006 | `SavedTablesUiState` sort và `SavedTableCalendarArea` | Mỗi ticket có một hàng, mã ticket luôn cố định và thứ tự khớp bảng. |
| FR-005, FR-009 | Projection trạng thái hàng và empty state | Task thiếu/sai ngày vẫn có hàng; khi không có khoảng hợp lệ, cửa sổ vẫn hiện nếu timezone hợp lệ, có giải thích và không có thanh. |

## Complexity Tracking

Không có vi phạm hiến pháp. Thay đổi cập nhật projection miền và composable Compose hiện có; không phát sinh dependency, schema hay endpoint mới.
