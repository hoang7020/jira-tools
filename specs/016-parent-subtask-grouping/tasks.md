---

description: "Danh sách công việc nhóm ticket cha và sub-task trong bảng kết quả truy vấn"
---

# Tasks: Nhóm ticket cha và sub-task

**Input**: Tài liệu thiết kế trong `/specs/016-parent-subtask-grouping/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

## Phase 1: Setup

- [X] T001 Kiểm tra các DTO và mapper Jira hiện tại liên quan đến search issue trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt`, `TicketMapper.kt`.

## Phase 2: Foundational

- [X] T002 [P] Bổ sung dữ liệu quan hệ Jira vào `IssueDto`/`FieldsDto`: `parent`, `issuetype.subtask`, `subtasks` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt`.
- [X] T003 [P] Thêm trường `parentKey`, `hasKnownSubtasks`, `isSubtask`, `isParent` vào `Ticket` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt`.
- [X] T004 Thêm các field Jira cần truy vấn `parent`, `issuetype`, `subtasks` vào `TicketMapper.requestFields` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt`.

**Checkpoint**: DTO và domain đã có đủ dữ liệu quan hệ cha-con để triển khai các user story.

## Phase 3: User Story 1 - Nhận biết ticket cha (Priority: P1) 🎯 MVP

**Goal**: Ticket cha trong kết quả được tô xám mờ trên toàn bộ dòng, kể cả khi không có sub-task hiển thị.

**Independent Test**: DTO cha có `subtasks` hoặc có sub-task tham chiếu cha được ánh xạ thành `isParent`; TicketTable tô màu dòng cha và không tô ticket thường.

### Tests for User Story 1

- [X] T005 [P] [US1] Viết test mapper nhận diện cha có sub-task trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt`.
- [X] T006 [P] [US1] Viết test mapper nhận diện cha vẫn là cha khi cha không có sub-task hiển thị trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt`.
- [X] T007 [P] [US1] Viết test mapper không đánh dấu ticket thường là cha trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt`.

### Implementation for User Story 1

- [X] T008 [US1] Ánh xạ `parent`, `issuetype.subtask`, `subtasks` sang các trường quan hệ trong `TicketMapper.toTicket()` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt`.
- [X] T009 [US1] Xác định `isParent` cho ticket có danh sách `subtasks` không rỗng hoặc có sub-task trực tiếp trong kết quả trong `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt`.
- [X] T010 [US1] Áp dụng nền xám mờ toàn bộ dòng cho `ticket.isParent` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.

**Checkpoint**: User Story 1 hoạt động độc lập và có thể kiểm tra màu dòng cha.

## Phase 4: User Story 2 - Hiển thị sub-task dưới ticket cha (Priority: P1)

**Goal**: Các sub-task thuộc cùng cha xuất hiện liên tục ngay dưới ticket cha, giữ thứ tự tương đối.

**Independent Test**: Danh sách đầu vào có cha đứng sau sub-task và ticket không liên quan xen giữa; hàm grouping xuất ra cha trước các sub-task, không có ticket không liên quan trong nhóm.

### Tests for User Story 2

- [X] T011 [P] [US2] Viết test gom cha trước sub-task ngay cả khi thứ tự Jira đặt cha sau sub-task trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.
- [X] T012 [P] [US2] Viết test giữ thứ tự tương đối của các sub-task cùng cha trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.
- [X] T013 [P] [US2] Viết test orphan sub-task không tạo dòng cha giả trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.
- [X] T014 [P] [US2] Viết test ticket không liên quan không xen vào nhóm cha/sub-task trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.

### Implementation for User Story 2

- [X] T015 [US2] Tạo hàm grouping UI-independent trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/QueryResultGrouping.kt`.
- [X] T016 [US2] Áp dụng kết quả grouping trước khi đưa danh sách ticket vào trạng thái query trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt` hoặc file quản lý query hiện có.
- [X] T017 [US2] Đảm bảo `TicketTable` nhận danh sách đã được nhóm và giữ cả danh sách cha/sub-task trong `items` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.

**Checkpoint**: User Story 2 hoạt động độc lập và các sub-task được hiển thị đúng nhóm.

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T018 Chạy test logic grouping trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.
- [X] T019 Chạy toàn bộ `./gradlew :composeApp:jvmTest` và `./gradlew :composeApp:ktlintCheck`.
- [X] T020 Hướng dẫn kiểm chứng cha/sub-task trong `specs/016-parent-subtask-grouping/quickstart.md` đã phù hợp với fixture và không cần chỉnh sửa.

## Phase 6: User Story 2 - Cây thư mục và bảo toàn nhóm khi sort

**Goal**: Sub-task hiển thị trực quan bằng cây thư mục dưới ticket cha và không bị tách khỏi cha khi sort.

**Independent Test**: Với ba sub-task hiển thị, UI dùng `├` cho các sub-task trước cuối và `└` cho sub-task cuối; khi sort thay đổi thứ tự nhóm, mỗi sub-task vẫn nằm sau cha của nó.

### Tests for User Story 2

- [x] T021 [P] [US2] Viết test sort giữ nguyên từng nhóm cha-con và thứ tự tương đối của sub-task trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/QueryResultGroupingTest.kt`.
- [x] T022 [P] [US2] Viết test xác định nhánh `├` cho sub-task không cuối và `└` cho sub-task cuối trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryTreeBranchTest.kt`.

### Implementation for User Story 2

- [x] T023 [US2] Mở rộng grouping UI-independent để trả metadata nhánh cuối/không cuối cho từng sub-task trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/QueryResultGrouping.kt`.
- [x] T024 [US2] Áp dụng metadata nhánh vào bản trình bày sub-task trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` mà không thay đổi dữ liệu Ticket hoặc các giá trị cột.
- [x] T025 [US2] Đảm bảo dữ liệu sort vẫn được gom lại thành nhóm atom trước khi cập nhật state trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt`.
- [x] T026 [US2] Bổ sung test sort theo cột bảo toàn cha phía trước sub-task trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelTest.kt`.

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) không phụ thuộc task khác.
- Foundational (Phase 2) phụ thuộc Setup và chặn cả hai user story.
- User Story 1 và User Story 2 phụ thuộc Foundational.
- Polish phụ thuộc cả hai user story đã hoàn tất.
- Phần cập nhật cây thư mục và sort (Phase 6) phụ thuộc grouping hiện có ở User Story 2 và chạy trước khi hoàn tất polish cuối cùng.

### User Story Dependencies

- User Story 1: độc lập; cần DTO domain quan hệ cha nhưng không phụ thuộc thuật toán grouping.
- User Story 2: độc lập về kiểm thử; có thể tích hợp cùng `isParent` sau Phase 2.
- Cập nhật Phase 6 thuộc User Story 2 vì mở rộng nhóm hiện có cho nhánh cây và sort.

### Parallel Opportunities

- T002 và T003 có thể chạy song song vì thay đổi hai file khác nhau.
- Các test T005-T007 có thể chạy song song.
- Các test T011-T014 có thể chạy song song trong cùng test file nếu phân công hợp nhất, nhưng đều phụ thuộc API grouping dự kiến.
- T021 và T022 có thể chạy song song vì nằm ở domain grouping và presentation test khác nhau.

## Parallel Example: User Story 1

```text
T005: test cha có sub-task
T006: test cha không có sub-task hiển thị
T007: test ticket thường
```

## Parallel Example: User Story 2

```text
T011: test cha đứng sau sub-task
T012: test thứ tự sub-task
T013: test orphan sub-task
T014: test ticket không liên quan
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Hoàn thành Setup và Foundational.
2. Viết test US1 trước và xác nhận red.
3. Ánh xạ dữ liệu cha và tô màu dòng.
4. Dừng lại để xác nhận US1 độc lập.

### Incremental Delivery

1. Hoàn thành nền DTO/domain và US1.
2. Bổ sung grouping và US2.
3. Chạy toàn bộ test và lint ở Phase 5.

## Format Validation

Mọi task đều bắt đầu bằng `- [ ]`, có Task ID liên tục và có file path cụ thể; task thuộc user story có thêm nhãn `[US1]` hoặc `[US2]`.
