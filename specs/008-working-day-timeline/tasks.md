---

description: "Task list for working-day timeline rendering"
---

# Tasks: Timeline theo ngày làm việc

**Input**: Design documents from `specs/008-working-day-timeline/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`

**Tests**: Test-first là yêu cầu của hiến pháp dự án. Logic projection shared phải được kiểm thử trong `commonTest` trước implementation; không gọi Jira thật.

**Organization**: Tasks được nhóm theo user story để có thể triển khai và kiểm thử độc lập.

## Phase 1: Setup

**Purpose**: Chuẩn bị cấu trúc dự án.

Không cần task setup; module Compose Multiplatform, calendar, domain calculator và test source set đã tồn tại.

---

## Phase 2: Foundational

**Purpose**: Hạ tầng dùng chung, phải hoàn thành trước user story.

Không cần task foundational riêng. Feature chỉ mở rộng quy tắc lịch làm việc và projection calendar hiện có; không thêm dependency, schema hay endpoint.

---

## Phase 3: User Story 1 - Đọc timeline theo thời gian làm việc (Priority: P1) 🎯 MVP

**Goal**: Calendar phân bổ timeline theo phần thời gian làm việc từng ngày; bỏ giờ nghỉ, ngoài giờ và cuối tuần nhưng giữ các ô cuối tuần trống trên trục.

**Independent Test**: Với fixture trong timezone hợp lệ, xác nhận mốc 10:00 ở 25% ngày, 14:00 ở 62.5%, khoảng 4 giờ chiếm nửa ô, thời gian nghỉ trưa/tuần không có chiều rộng, và các segment đa ngày/đoạn đỏ giữ đúng thứ tự cùng clipping.

### Tests for User Story 1

> Viết test trước implementation và xác nhận các test mới thất bại theo hành vi hiện tại.

- [X] T001 [P] [US1] Thêm test projection trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableCalendarTest.kt` cho segment theo giờ bắt đầu/kết thúc, khoảng 4 giờ chiếm 50%, nhiều ngày làm việc, bỏ giờ nghỉ trưa và cuối tuần, dữ liệu thiếu/sai, timezone, clipping và segment quá hạn.
- [X] T002 [P] [US1] Thêm test cho phép quy đổi thời điểm phiên làm việc thành tỷ lệ ngày trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/WorkingTimeCalculatorTest.kt`; kiểm tra 08:00→0%, 12:00/13:00→50%, 17:00→100%, khoảng qua nghỉ trưa liền mạch, ngày ngoài lịch không đóng góp và timezone/DST.

### Implementation for User Story 1

- [X] T003 [US1] Mở rộng lịch làm việc dùng chung trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/WorkingTimeCalculator.kt` để xác định ngày làm việc, hai phiên 08:00–12:00/13:00–17:00 và tọa độ thời gian làm việc tích lũy từ 0 đến 1; giữ đúng timezone bảng và không tính thứ Bảy/Chủ nhật hoặc thời gian ngoài phiên.
- [X] T004 [US1] Thay projection khoảng nguyên ngày trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt` bằng các `CalendarWorkSegment` có `dayOffset`, `startFraction` và `endFraction`; sinh segment từ phần giao của instant nguồn với phiên làm việc trong cửa sổ, giữ thứ tự hàng, xử lý khoảng rỗng/thiếu/sai, cờ tiếp diễn và ngoài cửa sổ cho cả thanh dự kiến lẫn phần quá hạn.
- [X] T005 [US1] Cập nhật renderer trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` để vẽ từng segment tại `dayOffset` với vị trí và chiều rộng theo fraction của `DAY_COLUMN_WIDTH`; giữ nguyên trục ngày, scroll, cột cuối tuần trống, màu dự kiến và màu đỏ quá hạn.

**Checkpoint**: Story 1 hoàn chỉnh khi test projection xác nhận tỷ lệ từng ngày, nghỉ trưa/cuối tuần bị bỏ qua, và renderer giữ nguyên giao diện/trục hiện có.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Kiểm tra tích hợp và tài liệu xác nhận.

- [X] T006 Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`; xử lý mọi lỗi do feature trong `composeApp/src/commonMain/kotlin/com/jiratools/` và `composeApp/src/commonTest/kotlin/com/jiratools/`.
- [X] T007 Đối chiếu các tình huống tự động và thủ công trong `specs/008-working-day-timeline/quickstart.md` với hành vi cuối cùng; cập nhật hướng dẫn nếu phạm vi test hoặc cách xác nhận UI thay đổi.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; cấu trúc hiện tại đã đáp ứng.
- **Foundational (Phase 2)**: Không có task; không có hạ tầng mới phải chặn user story.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay; là user story duy nhất của feature.
- **Polish (Phase 4)**: Thực hiện sau khi projection và renderer của US1 hoàn tất.

### User Story Dependencies

- **User Story 1 (P1)**: Độc lập; không phụ thuộc story khác.

### Within User Story 1

- T001 và T002 là test-first và có thể chạy song song vì sửa hai test file khác nhau.
- T003 hoàn tất trước T004 vì projection dùng quy tắc phiên/tỷ lệ làm việc dùng chung.
- T004 hoàn tất trước T005 vì renderer phụ thuộc segment projection.
- T006/T007 chạy sau phần triển khai; toàn bộ unit tests và build phải đạt trước khi hoàn thành.

### Dependency Graph

```text
T001 ─┐
      ├──> T003 ──> T004 ──> T005 ──> T006 ──> T007
T002 ─┘
```

### Parallel Opportunities

```text
# Song song: bổ sung hai nhóm test độc lập, trước implementation
T001: projection tests trong SavedTableCalendarTest.kt
T002: work-session ratio tests trong WorkingTimeCalculatorTest.kt
```

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn thành T001 và T002, xác nhận test mới bắt được hành vi ngày nguyên hiện tại.
2. Hoàn thành T003 và T004 để domain tạo segment ngày làm việc chính xác.
3. Hoàn thành T005 để Compose render segment theo fraction, không đổi trục ngày.
4. Chạy kiểm thử độc lập của US1 rồi hoàn tất T006/T007.

### Incremental Delivery

- Chỉ có một user story; projection và renderer phải được bàn giao cùng nhau để đạt trải nghiệm sử dụng độc lập.
- Không cần migration, request Jira hoặc cấu hình người dùng.

## Format Validation

- Tất cả task đều có checkbox, ID tuần tự, nhãn `[US1]` trong phase user story và đường dẫn file cụ thể.
- `[P]` chỉ được dùng cho hai nhóm test có thể sửa song song ở hai file riêng biệt.
