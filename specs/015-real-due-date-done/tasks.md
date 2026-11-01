---

description: "Danh sách công việc bổ sung trạng thái Done cho Real Due Date"
---

# Tasks: Bổ sung Done cho Real Due Date

**Input**: Tài liệu thiết kế trong `/specs/015-real-due-date-done/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Có task test-first cho mapper Real Due Date. Không gọi Jira thật.

## Quy ước đường dẫn

- Mã dùng chung: `composeApp/src/commonMain/kotlin/com/jiratools/`
- Test dùng chung: `composeApp/src/commonTest/kotlin/com/jiratools/`

## Phase 1: Setup

Không có thay đổi setup, dependency hoặc cấu trúc module.

## Phase 2: Foundational

Không có task nền tảng độc lập; mapper và snapshot Real Due Date hiện có được tái sử dụng.

## Phase 3: User Story 1 - Ghi nhận Real Due Date khi chuyển sang Done (Priority: P1) 🎯 MVP

**Goal**: Nhận diện chính xác trạng thái `Done` là transition hợp lệ và ghi Real Due Date theo lần chuyển cuối.

**Independent Test**: Với history chứa `Done`, biến thể hoa/thường có khoảng trắng và `Done Review`, xác nhận chỉ `Done` hợp lệ và lấy timestamp đúng.

### Tests for User Story 1

- [X] T001 [P] [US1] Thêm test `Done`, tên viết hoa/thường có khoảng trắng và trạng thái chứa chuỗi `Done` không hợp lệ trong `composeApp/src/commonTest/kotlin/com/jiratools/data/StatusChangelogMapperTest.kt`.
- [X] T002 [P] [US1] Thêm test chọn transition `Done` mới nhất theo changelog timestamp trong `composeApp/src/commonTest/kotlin/com/jiratools/data/StatusChangelogMapperTest.kt`.

### Implementation for User Story 1

- [X] T003 [US1] Mở rộng điều kiện status transition để nhận `Done` sau `trim()` và so sánh không phân biệt hoa thường trong `composeApp/src/commonMain/kotlin/com/jiratools/data/StatusChangelogMapper.kt`.
- [X] T004 [US1] Duy trì cơ chế chọn changelog hợp lệ mới nhất để candidate `Done`, `Close` và `Resolve` cạnh tranh theo timestamp trong `composeApp/src/commonMain/kotlin/com/jiratools/data/StatusChangelogMapper.kt`.

**Checkpoint**: User Story 1 hoạt động độc lập; task chuyển sang `Done` tạo Real Due Date đúng.

## Phase 4: User Story 2 - Giữ quy tắc Close và Resolve (Priority: P2)

**Goal**: Không làm thay đổi Real Due Date hiện có khi chuyển qua `Close` và `Resolve`, đồng thời chọn lần chuyển hợp lệ cuối cùng trên cả ba trạng thái.

**Independent Test**: Kiểm tra chuỗi `Close` → `Resolve` → `Done` và trạng thái không hợp lệ xen giữa, xác nhận timestamp cuối cùng là đúng.

### Tests for User Story 2

- [X] T005 [P] [US2] Thêm test hồi quy chuỗi `Close`, `Resolve`, `Done` và trạng thái thường xen giữa trong `composeApp/src/commonTest/kotlin/com/jiratools/data/StatusChangelogMapperTest.kt`.
- [X] T006 [P] [US2] Thêm test bảo toàn `Close`/`Resolve` và loại `Done Review` khỏi candidate trong `composeApp/src/commonTest/kotlin/com/jiratools/data/StatusChangelogMapperTest.kt`.

### Implementation for User Story 2

- [X] T007 [US2] Chuẩn hóa tập trạng thái hợp lệ thành `Close`, `Resolve`, `Done` mà không thay đổi schema, snapshot hoặc luồng UI trong `composeApp/src/commonMain/kotlin/com/jiratools/data/StatusChangelogMapper.kt`.
- [X] T008 [US2] Cập nhật documentation mapping Real Due Date để phản ánh tập trạng thái kết thúc ba giá trị trong `specs/015-real-due-date-done/contracts/real-due-date-mapping.md`.

**Checkpoint**: User Story 2 hoạt động độc lập; quy tắc cũ vẫn giữ và candidate mới nhất được chọn xuyên suốt ba trạng thái.

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T009 Chạy test mapper mục tiêu bằng `./gradlew :composeApp:jvmTest --tests com.jiratools.data.StatusChangelogMapperTest`.
- [X] T010 Chạy toàn bộ `./gradlew :composeApp:jvmTest` và `./gradlew :composeApp:ktlintCheck`.
- [X] T011 Cập nhật hướng dẫn kiểm chứng nếu kết quả fixture thực tế khác mô tả trong `specs/015-real-due-date-done/quickstart.md`.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không cần thay đổi.
- **Foundational (Phase 2)**: Không có task độc lập.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay sau khi đã có task test đỏ.
- **User Story 2 (Phase 4)**: Phụ thuộc mapper User Story 1 nhưng có thể chạy test riêng.
- **Polish (Phase 5)**: Sau khi US1 và US2 hoàn tất.

### User Story Dependencies

- **US1**: Không phụ thuộc story khác.
- **US2**: Phụ thuộc US1 vì mở rộng và bảo toàn cùng predicate trạng thái.

## Parallel Example: User Story 1

```text
Song song:
- T001: test nhận diện Done và trạng thái không hợp lệ
- T002: test chọn timestamp Done mới nhất

Sau đó:
- T003 -> T004
```

## Parallel Example: User Story 2

```text
Song song:
- T005: test chuỗi Close/Resolve/Done
- T006: test bảo toàn quy tắc cũ

Sau đó:
- T007 -> T008
```

## Implementation Strategy

### MVP trước

1. Viết test US1 và xác nhận mapper hiện thất bại với `Done`.
2. Mở rộng predicate và chạy test US1.
3. Bổ sung test và bảo toàn US2.
4. Chạy quality gates ở Phase 5.

MVP là hoàn thành User Story 1; phạm vi tối thiểu chỉ là `StatusChangelogMapper` và test mapper tương ứng.
