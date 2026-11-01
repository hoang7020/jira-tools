# Implementation Plan: Sửa cuộn ngang Calendar

**Branch**: `009-calendar-horizontal-scroll` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả tính năng từ `specs/009-calendar-horizontal-scroll/spec.md`

## Summary

Khắc phục cuộn ngang bằng cách làm cho vùng trục ngày và timeline nhận tương tác cuộn ngang, đồng thời tiếp tục dùng một vị trí cuộn duy nhất cho header, lưới và mọi hàng. Khi vị trí cuộn đến gần một biên, calendar mở rộng khoảng ngày theo từng khối ở hướng đó, giữ nguyên ngày đang nhìn thấy, rồi tính lại projection từ snapshot ticket để các segment vẫn căn đúng ngày. Giữ ngày hiện tại làm điểm mở ban đầu, không lưu khoảng ngày mở rộng và không gọi Jira.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, kotlinx-datetime 0.8.0, Kotlin Coroutines 1.11.0; đều có sẵn.  
**Storage**: Không đổi; khoảng mở rộng chỉ là state tạm của calendar, không serialize hoặc ghi vào snapshot.  
**Testing**: Test-first; logic tính khoảng ngày và projection trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/`, xác nhận UI desktop thủ công. Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`.  
**Target Platform**: Ứng dụng Desktop JVM; state/domain hiện có đặt trong `commonMain`.  
**Project Type**: Compose Multiplatform desktop app, module `composeApp`.  
**Performance Goals**: Giữ ngày trong header lazy; chỉ mở rộng khi gần biên; xử lý và chiếu ngày/ticket trong cửa sổ đang cần mà không chặn luồng UI.  
**Constraints**: Header, lưới và timeline phải chia sẻ cùng chỉ số ngày và offset pixel; không làm hỏng cuộn dọc ticket; giữ vị trí ngày đang thấy khi thêm ngày ở đầu; chỉ sử dụng ngày hợp lệ mà thư viện và UI có thể biểu diễn. Không thêm dependency hoặc gọi Jira.  
**Scale/Scope**: Một calendar của bảng snapshot đã lưu; khởi đầu bằng cửa sổ hiện tại (một tháng trước, ba tháng sau), mở rộng về quá khứ hoặc tương lai khi người dùng cuộn đến gần biên.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Phép tính ngày và projection dùng `kotlinx-datetime` trong shared domain; tương tác Compose nằm trong UI dùng chung. |
| II. Kiểm thử trước | PASS | Bổ sung kiểm thử commonTest cho mở rộng phạm vi, giữ mốc nhìn thấy, biên ngày và projection; kiểm tra UI trước/sau bằng thao tác desktop. |
| III. Hybrid UI | PASS | Quy tắc tính ngày và segment ở domain; Compose chỉ quản lý scroll state, phát hiện gần biên và render. |
| IV. Kotlin an toàn kiểu | PASS | State bất biến/typed, API datetime dùng chung; không thêm dependency hoặc API nền tảng. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng calendar hiện tại và mở rộng theo khối; không thêm module hoặc lưu trữ mới. |

Không có `[NEEDS CLARIFICATION]` trong spec sau bước clarify.

## Nghiên cứu và quyết định thiết kế

Đã ghi các lựa chọn và căn cứ trong [research.md](./research.md). Tóm tắt:

1. Dùng một `LazyListState` làm nguồn sự thật cho trục ngang; header và thanh timeline đọc cùng `firstVisibleItemIndex`/`firstVisibleItemScrollOffset`.
2. Tạo một vùng nhận gesture ngang bao quanh toàn bộ viewport ngày, không chỉ header. Chuyển gesture ngang tới cùng state; không tiêu thụ gesture dọc để `LazyColumn` ticket tiếp tục cuộn bình thường.
3. Mở rộng đầu/cuối theo khối một tháng khi viewport đến gần biên, bằng state tạm trong Calendar. Khi thêm ngày ở đầu, dịch chỉ số ngày đang nhìn để giữ nguyên ngày và offset pixel; khi thêm ở cuối, giữ nguyên state.
4. Cho Calendar nhận tickets và thời điểm tham chiếu, rồi tính projection cho khoảng ngày đang mở. Giữ nguyên projection domain thuần để test; bổ sung tham số biên ngày rõ ràng thay vì chiếu từ các segment đã bị cắt trong cửa sổ cũ.
5. Giữ danh sách header lazy và giới hạn an toàn cho số ngày có thể biểu diễn; không thử tạo danh sách vô hạn eager. Khi đạt giới hạn `LocalDate`/index, ngừng mở rộng phía đó một cách ổn định.

## Phase 1: Design & Contracts

### Mô hình dữ liệu

Chi tiết state và invariant nằm trong [data-model.md](./data-model.md).

- Mở rộng khoảng ngày của calendar là state UI tạm thời gồm `windowStart`/`windowEnd`.
- Vị trí scroll gồm index ngày đầu tiên nhìn thấy và offset pixel; ngày mà vị trí này đại diện phải được giữ nguyên khi prepend.
- Projection hiện có được tái tính từ tickets, timezone, thời điểm tham chiếu và biên khoảng ngày mới.
- Không có migration hoặc thay đổi dữ liệu đã lưu.

### Hợp đồng

Không tạo `contracts/`: tính năng chỉ thay đổi hành vi nội bộ của desktop UI và projection; không thêm API, giao thức hay tích hợp với hệ thống ngoài.

### Quickstart

Hướng dẫn kiểm chứng tự động và thủ công nằm trong [quickstart.md](./quickstart.md).

## Project Structure

### Tài liệu tính năng

```text
specs/009-calendar-horizontal-scroll/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
└── quickstart.md
```

### Mã nguồn

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   └── SavedTableCalendar.kt
└── ui/saved/
    ├── SavedTableCalendarArea.kt
    └── SavedTablesArea.kt

composeApp/src/commonTest/kotlin/com/jiratools/domain/
└── SavedTableCalendarTest.kt
```

**Structure Decision**: Giữ module `composeApp` hiện có. Thay đổi projection trong shared domain, truyền snapshot đầu vào và biên khoảng ngày từ saved-table screen, và giữ gesture/scroll/render trong calendar UI. Không thêm test source set hoặc thư viện UI test mới.

## Constitution Check sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Ranh giới đa nền tảng | PASS | Date arithmetic và projection thuần shared; UI dùng Compose hiện có. |
| Test-first | PASS | Có thể kiểm thử thuần quy tắc tăng/giảm range và projection trong commonTest trước UI wiring. |
| Nghiệp vụ ngoài UI | PASS | UI sở hữu viewport range/scroll; định nghĩa ngày và projection remains ở domain. |
| Kiểu an toàn/đơn giản | PASS | Dùng date range rõ ràng, không thêm dependency, serialization hoặc abstraction ngoài nhu cầu. |
| Hiệu năng và tương tác | PASS | Header lazy; mở rộng từng khối; gesture dọc được chuyển tiếp; giữ ngày nhìn thấy tránh nhảy trục. |

Không có vi phạm hiến pháp cần Complexity Tracking.

## Complexity Tracking

Không có vi phạm hiến pháp.
