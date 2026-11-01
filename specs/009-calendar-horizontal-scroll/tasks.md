---

description: "Task list for calendar horizontal scrolling"
---

# Tasks: Sửa cuộn ngang Calendar

**Input**: Tài liệu thiết kế trong `specs/009-calendar-horizontal-scroll/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`

**Tests**: Bắt buộc theo hiến pháp dự án: viết và chạy test shared `commonTest` trước logic date-range/projection. UI gesture xác nhận bằng quickstart trên Desktop; không thêm UI test dependency.

**Organization**: Tasks được nhóm theo user story để có thể triển khai và kiểm thử độc lập.

## Phase 1: Setup

**Purpose**: Chuẩn bị cấu trúc dự án.

Không cần task setup; module Compose Multiplatform, calendar screen và domain projection đã tồn tại.

---

## Phase 2: Foundational

**Purpose**: Hạ tầng dùng chung, phải hoàn thành trước user story.

Không cần task foundational riêng; feature dùng snapshot hiện có, không thêm dependency, storage hoặc Jira integration.

---

## Phase 3: User Story 1 - Cuộn trục ngày Calendar (Priority: P1) 🎯 MVP

**Goal**: Cho phép cuộn ngang trên calendar, đồng bộ header/grid/timeline và tự mở rộng các ngày quá khứ/tương lai khi gần biên.

**Independent Test**: Kiểm tra mở rộng range ở cả hai hướng và projection trên range mới; chạy ứng dụng, cuộn bắt đầu từ header và hàng timeline, xác nhận ngày neo không nhảy, nội dung các hàng thẳng với header, và cuộn dọc vẫn hoạt động.

### Tests for User Story 1

> Viết test trước implementation và xác nhận test mới thất bại trước khi triển khai logic tương ứng.

- [X] T001 [P] [US1] Bổ sung kiểm thử vào `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableCalendarTest.kt` để xác nhận projection với biên ngày tường minh chiếu segment trên ngày mới, tính offset theo `windowStart` mới và giữ trạng thái khi khoảng ngày không hợp lệ.
- [X] T002 [P] [US1] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/CalendarDateRangeTest.kt` với kiểm thử mở rộng một tháng qua biên trái/phải, tính số ngày prepend để giữ ngày neo, tính range liên tục không trùng/mất ngày và xử lý giới hạn LocalDate không overflow.

### Implementation for User Story 1

- [X] T003 [US1] Thêm phép tính range ngày ban đầu/mở rộng và các kiểm tra biên an toàn trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt`; giữ range ban đầu từ một tháng trước đến ba tháng sau ngày tham chiếu và mở rộng từng tháng mà không đổi thứ tự/ngày liên tục.
- [X] T004 [US1] Cho phép `projectSavedTableCalendar` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt` nhận `windowStart`/`windowEnd` tùy chọn để chiếu lại tất cả ticket từ instant gốc theo range hiện hành; giữ mặc định và hành vi của các caller hiện tại.
- [X] T005 [US1] Truyền snapshot ticket đã sort, timezone và `calendarReferenceMillis` từ `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` vào calendar để calendar có thể tính lại projection sau khi range mở rộng; giữ nguyên xử lý calendar không hợp lệ và trạng thái rỗng.
- [X] T006 [US1] Sửa vùng trục ngày trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` để một `LazyListState` dùng chung điều khiển header, grid và mọi hàng timeline, đồng thời nhận cuộn ngang từ vùng header/timeline mà không chiếm gesture cuộn dọc ticket.
- [X] T007 [US1] Thêm state range tạm trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` để mở rộng một tháng gần biên đang cuộn, tính lại projection bằng snapshot đã truyền vào, giữ nguyên ngày/offset pixel khi prepend và không reset vị trí về ngày hiện tại khi range đổi.

**Checkpoint**: US1 hoàn chỉnh khi người dùng cuộn ngang qua cả hai phía không gặp biên cuối cố định, header và timeline luôn đồng bộ, và danh sách ticket vẫn cuộn dọc được.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Xác nhận chất lượng và cập nhật hướng dẫn.

- [X] T008 Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`; xử lý lỗi liên quan trong `composeApp/src/commonMain/kotlin/com/jiratools/` và `composeApp/src/commonTest/kotlin/com/jiratools/`.
- [X] T009 Đối chiếu và cập nhật các bước xác nhận cuộn ngang, mở rộng hai phía, giữ anchor, cuộn dọc, range rỗng và resize trong `specs/009-calendar-horizontal-scroll/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; cấu trúc dự án đã sẵn sàng.
- **Foundational (Phase 2)**: Không có task; không có hạ tầng mới phải chặn user story.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay; là user story duy nhất.
- **Polish (Phase 4)**: Sau khi domain, projection và UI của US1 hoàn tất.

### User Story Dependencies

- **User Story 1 (P1)**: Độc lập; không phụ thuộc story khác.

### Within User Story 1

- T001 và T002 có thể chạy song song vì sửa hai test file riêng biệt; cả hai phải thất bại/chứng minh thiếu hành vi trước implementation.
- T003 hoàn tất trước T004 vì projection dùng quy tắc range an toàn.
- T004 hoàn tất trước T005 vì UI cần projection hỗ trợ biên tùy chỉnh.
- T005 hoàn tất trước T006/T007 vì calendar cần nhận snapshot để recalculate.
- T006 hoàn tất trước T007; mở rộng range phải dùng chung scroll state và offset đang hoạt động.
- T008 và T009 chạy sau US1; build/test phải đạt trước khi hoàn tất.

### Dependency Graph

```text
T001 ─┐
      ├──> T003 ──> T004 ──> T005 ──> T006 ──> T007 ──> T008 ──> T009
T002 ─┘
```

### Parallel Opportunities

```text
# Song song: bổ sung test projection và test date range trước implementation
T001: projection tests trong SavedTableCalendarTest.kt
T002: range expansion tests trong CalendarDateRangeTest.kt
```

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn thành T001/T002 và xác nhận các test mới thất bại trên hành vi hiện tại.
2. Hoàn thành T003/T004 để domain hỗ trợ mở rộng và projection theo range mới.
3. Hoàn thành T005–T007 để truyền snapshot, nhận gesture trên toàn vùng ngày và mở rộng range không làm nhảy viewport.
4. Chạy T008 và thực hiện xác nhận Desktop theo T009.

### Incremental Delivery

- Chỉ có một user story; scroll, mở rộng range và đồng bộ projection/header/timeline phải được bàn giao cùng nhau.
- Không thêm lưu trữ, dependency hoặc API Jira.

## Format Validation

- Mọi task có checkbox, ID tuần tự và đường dẫn file cụ thể.
- Mọi task trong phase user story có nhãn `[US1]`; `[P]` chỉ đánh dấu hai task test ở file riêng.
