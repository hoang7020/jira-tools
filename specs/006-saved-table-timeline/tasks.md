---
description: "Task list for the bounded saved-table calendar timeline"
---

# Tasks: Calendar cho Bảng đã lưu — cửa sổ ngày giới hạn

**Input**: Tài liệu thiết kế trong `specs/006-saved-table-timeline/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/saved-table-calendar.md`, `quickstart.md`

**Phạm vi**: Các task T001–T008 của lần triển khai calendar trước đã hoàn tất. Danh sách này chỉ chứa công việc còn lại do yêu cầu cửa sổ ngày cố định và clipping; walkthrough Desktop trước đó được chuyển thành task cuối để kiểm tra đúng hành vi mới.

**Tests**: Bắt buộc theo Constitution II. Viết test `commonTest` và xác nhận thất bại trước khi triển khai logic projection/state; không gọi Jira thật.

**Organization**: Chỉ có một user story P1 trong `spec.md`; các task còn lại được nhóm theo story này để giữ khả năng kiểm thử độc lập.

## Phase 1: Setup

**Mục đích**: Chuẩn bị dependency và cấu trúc dùng chung.

Không cần task setup: module `composeApp` đã có Compose Multiplatform, kotlinx-datetime, coroutine và `AppClock`; không thêm dependency hoặc module.

---

## Phase 2: Foundational

**Mục đích**: Prerequisite chặn mọi user story.

Không cần thay đổi nền tảng dùng chung. Snapshot `SavedTable`/`Ticket`, timezone Jira, `SavedTablesViewModel` và callback điều hướng calendar đã tồn tại.

---

## Phase 3: User Story 1 — Theo dõi task trên calendar (Priority: P1) 🎯 MVP

**Mục tiêu**: Giới hạn trục calendar trong cửa sổ từ một tháng lịch trước đến ba tháng lịch sau ngày hiện tại theo timezone Jira; giữ cột ticket cố định; cắt và đánh dấu khoảng vượt biên; vẫn hiển thị cửa sổ khi timezone hợp lệ nhưng không có khoảng task hợp lệ.

**Independent Test**: Với `FakeClock` cố định và timezone Jira hợp lệ, kiểm tra cửa sổ inclusive cùng ngày clamp cuối tháng; xác nhận mọi ngày liên tiếp được hiển thị và cuộn bị giới hạn. Dùng task nằm trong, vượt từng mép, vượt cả hai mép, hoàn toàn trước/sau cửa sổ, thiếu/sai mốc và bảng không có khoảng hợp lệ; xác nhận thanh hiển thị đúng phần giao, có dấu tiếp diễn hoặc thông báo phù hợp, mỗi ticket giữ hàng đúng thứ tự, header không lệch thanh và không có request Jira.

### Tests for User Story 1

> Viết các test trước implementation và xác nhận chúng thất bại vì projection/state chưa hỗ trợ cửa sổ cố định.

- [X] T009 [P] [US1] Mở rộng `SavedTableCalendarTest` tại `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableCalendarTest.kt` để truyền `nowMillis` xác định và kiểm tra timezone bảng hợp lệ/thiếu/sai, biên inclusive từ một tháng lịch trước tới ba tháng lịch sau, clamp ngày cuối tháng/năm nhuận, dãy ngày liên tục, khoảng giao bình thường, cắt trái/phải/cả hai mép, khoảng hoàn toàn ngoài khung về hai phía, bảng rỗng và không có khoảng hợp lệ nhưng vẫn có cửa sổ. Bao phủ các ràng buộc: “Một khoảng gốc hợp lệ khi có cả start/due và due không trước start.” và “Thanh chỉ tồn tại cho range hợp lệ có giao với cửa sổ.”
- [X] T010 [P] [US1] Mở rộng `SavedTablesViewModelTest` tại `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` để xác nhận mở calendar chụp `FakeClock`, giữ mốc khi đang mở, đóng rồi mở lại lấy mốc mới, đổi/bỏ chọn bảng đóng calendar và xóa mốc, và không gọi Jira hoặc sửa catalog; xác nhận test thất bại trước khi thêm state thời điểm tham chiếu.

### Implementation for User Story 1

- [X] T011 [P] [US1] Cập nhật projection trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt` để nhận `nowMillis`, chuyển ngày hiện tại theo timezone Jira, tạo cửa sổ tháng inclusive với quy tắc clamp cuối tháng, và giữ cửa sổ khi bảng rỗng/không có range hợp lệ. Giữ nguyên hàng và ngày gốc của ticket; tính offset/độ dài inclusive của phần giao, cờ tiếp diễn trước/sau, cùng trạng thái nằm hoàn toàn trước/sau. Áp dụng đúng các ràng buộc: “Một khoảng gốc hợp lệ khi có cả start/due và due không trước start.”; “Thanh chỉ tồn tại cho range hợp lệ có giao với cửa sổ.”; “Khoảng cùng ngày trong cửa sổ có `visibleDurationDays = 1`.”; “Hàng thiếu/sai dữ liệu và hàng ngoài cửa sổ vẫn được giữ để người dùng nhận biết ticket và lý do không có thanh.”
- [X] T012 [P] [US1] Cập nhật `SavedTablesUiState` và `SavedTablesViewModel` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` để thêm `calendarReferenceMillis`; `openCalendar()` chụp `clock.nowMillis()`, `closeCalendar()` xóa mốc, và đổi/bỏ chọn hoặc xóa bảng đang chọn cũng xóa mốc cùng trạng thái calendar khi selection thay đổi. Không lưu mốc này vào `SavedTableCatalog`; giữ sort hiện tại khi quay lại.
- [X] T013 [P] [US1] Cập nhật `SavedTablesArea`/`SavedTableDetail` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` để truyền `state.calendarReferenceMillis` vào projection cùng ticket đã sort và timezone của đúng bảng; tính lại projection khi snapshot ticket, timezone hoặc mốc tham chiếu thay đổi, không dùng system clock trực tiếp.
- [X] T014 [P] [US1] Cập nhật `SavedTableCalendarArea` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` để dựng trục từ cửa sổ projection thay vì min/max task, bắt đầu cuộn tại ngày hiện tại, giới hạn cuộn trong hai biên, giữ cột ticket cố định và đồng bộ header/thanh. Vẽ dấu tiếp diễn ở mép bị cắt, thông báo cho range hoàn toàn ngoài cửa sổ, và khi timezone hợp lệ nhưng không có range vẫn hiện trục cùng trạng thái giải thích; timezone không hợp lệ thì cảnh báo và không dựng trục.

**Checkpoint**: Projection và UI dùng cùng cửa sổ ngày bất biến trong phiên mở; mọi range giữ đúng ngày nguồn và chỉ vẽ phần giao; task ngoài khung và dữ liệu rỗng/sai có thông báo, không làm mất hàng.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Mục đích**: Xác minh tự động và walkthrough giao diện cho hành vi mới.

- [X] T015 Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build` sau các thay đổi tại `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` và các test tương ứng; xử lý lỗi do thay đổi trước khi đánh dấu hoàn tất.
- [ ] T016 Thực hiện walkthrough Desktop theo `specs/006-saved-table-timeline/quickstart.md` với bảng test có range trong/ngoài cửa sổ, vượt các mép, range thiếu/sai và bảng không có range hợp lệ; xác nhận ngày clamp, cuộn giới hạn, cột ticket cố định, header/thanh đồng bộ, dấu tiếp diễn, thông báo ngoài khung và trạng thái rỗng. Ghi nhận kết quả thực tế trước khi đánh dấu task hoàn tất. **Pending:** chưa thực hiện walkthrough tương tác trên Desktop.

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: Không có task; dependency và module hiện có đã đáp ứng feature.
- **Foundational (Phase 2)**: Không có task; không có prerequisite mới.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay; viết và xác nhận T009/T010 thất bại trước implementation.
- **Polish (Phase 4)**: T015 phụ thuộc hoàn tất T009–T014; T016 phụ thuộc T015 và cần phiên Desktop tương tác.

### User Story Dependencies

- **User Story 1 (P1)**: Độc lập; không phụ thuộc user story khác.

### Within User Story 1

- T009 và T010 có thể viết/chạy song song vì sửa hai file test khác nhau.
- Sau khi tests fail, T011 và T012 có thể triển khai song song vì sửa domain projection và ViewModel riêng.
- T013 và T014 có thể triển khai song song sau T011/T012 vì sửa hai file UI riêng; integration cuối cùng được kiểm tra ở T015.
- T015 phải chạy sau toàn bộ thay đổi; T016 chỉ chạy sau khi build và kiểm thử tự động đạt.

## Parallel Example: User Story 1

```text
Batch 1 — tests:
T009 SavedTableCalendarTest.kt
T010 SavedTablesViewModelTest.kt

Batch 2 — implementation sau khi tests thất bại:
T011 SavedTableCalendar.kt
T012 SavedTablesViewModel.kt

Batch 3 — UI sau khi model/state sẵn sàng:
T013 SavedTablesArea.kt
T014 SavedTableCalendarArea.kt
```

## Implementation Strategy

### MVP First

1. Dùng source set và dependency hiện có; không thêm setup/foundation.
2. Hoàn tất T009/T010 theo test-first, xác nhận fail trước code.
3. Triển khai T011/T012; sau đó wiring và bố cục UI ở T013/T014.
4. Chạy T015; chỉ khi đạt mới thực hiện walkthrough T016.

### Incremental delivery

1. Xác nhận logic cửa sổ lịch, clamp, giao khoảng và state tham chiếu bằng test `commonTest`.
2. Tích hợp projection vào màn hình saved-table và calendar; kiểm tra sticky column, cuộn và thông báo.
3. Chạy JVM tests, ktlint, build và quickstart Desktop để xác nhận toàn bộ User Story 1.

## Completion Criteria

- T009–T010 xác nhận test-first cho cửa sổ/ngày, clipping và mốc thời gian mở calendar.
- T011–T014 đáp ứng FR-001–FR-010 liên quan cửa sổ cố định mà không đổi schema hoặc gọi Jira.
- T015 pass tests, lint và build; T016 có kết quả walkthrough thực tế.
- Mọi task cần làm đều có checkbox `- [ ]`, ID tuần tự, nhãn `[US1]` ở user-story phase và đường dẫn file cụ thể.
