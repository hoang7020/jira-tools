---

description: "Task list for Real Due Date and overdue calendar timeline"
---

# Tasks: Ngày hoàn thành thực tế và quá hạn

**Input**: Design documents from `specs/007-real-due-date/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/jira-real-due-date.md`, `quickstart.md`

**Tests**: Test tasks are included because the project constitution requires test-first development for shared logic and mocked Jira HTTP integration.

**Organization**: Tasks are grouped by user story and ordered for independent delivery.

## Phase 1: Setup

**Purpose**: Project initialization and shared infrastructure.

No setup task is required: the existing KMP module, Gradle configuration, shared tests, Jira client, and calendar are already established.

---

## Phase 2: Foundational

**Purpose**: Blocking prerequisites shared by all user stories.

No separate foundational implementation is required. Jira transport/mapping, saved-table snapshot and calendar projection changes are implemented in their respective story phases.

---

## Phase 3: User Story 1 - Xem ngày hoàn thành thực tế (Priority: P1) 🎯 MVP

**Goal**: Ghi nhận transition Close/Resolve mới nhất khi làm mới bảng đã lưu, hiển thị Real Due Date và giữ dữ liệu cũ nếu không tải được changelog.

**Independent Test**: Với fixture Jira, làm mới bảng có nhiều transition status; xác nhận ngày đến từ transition Close/Resolve mới nhất và hiện trong cột saved table. Khi changelog API lỗi, xác nhận snapshot search được cập nhật, ngày đã có được giữ, issue mới để trống và người dùng nhận cảnh báo.

### Tests for User Story 1

> Viết các test này trước implementation, xác nhận chúng thất bại vì hành vi mới chưa có, sau đó mới triển khai.

- [X] T001 [P] [US1] Thêm test MockEngine cho bulk changelog payload, batch tối đa 1000 issue IDs, lọc `status`, phân trang `nextPageToken` và lỗi API trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientChangelogTest.kt`.
- [X] T002 [P] [US1] Thêm test mapper chọn changelog status mới nhất có `toString` trim bằng `Close` hoặc `Resolve` không phân biệt hoa thường, bỏ qua status khác và xử lý timestamp thiếu/sai trong `composeApp/src/commonTest/kotlin/com/jiratools/data/StatusChangelogMapperTest.kt`.
- [X] T003 [P] [US1] Thêm test migration catalog v1/v2 lên v3, bật cột Real Due Date, giữ dữ liệu cũ và giải mã các field ticket mới mặc định null trong `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableStoreTest.kt`.
- [X] T004 [P] [US1] Thêm test refresh thành công dùng transition mới nhất, không có transition đủ điều kiện, lỗi changelog giữ ngày theo issue ID, issue mới để trống, báo lỗi và hủy không commit trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`.

### Implementation for User Story 1

- [X] T005 [US1] Thêm DTO bulk changelog cùng endpoint `POST /rest/api/3/changelog/bulkfetch` vào `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraApiClient.kt`; gửi `fieldIds = ["status"]`, chia nhóm không quá 1000 issue IDs, theo hết `nextPageToken`, dùng credentials hiện có và trả lỗi có kiểu rõ ràng.
- [X] T006 [US1] Tạo mapper changelog trong `composeApp/src/commonMain/kotlin/com/jiratools/data/StatusChangelogMapper.kt`; chỉ xét item `status` có trạng thái đích sau trim bằng `Close` hoặc `Resolve` không phân biệt hoa thường, chọn sự kiện có timestamp mới nhất, parse thành epoch milliseconds và trả không có ngày khi không có transition hợp lệ.
- [X] T007 [US1] Thêm `realDueDate: String? = null` và `realDueAtMillis: Long? = null` vào `Ticket`, thêm `ColumnKey.REAL_DUE_DATE` saved-only, rồi migrate catalog schema v2→v3 và v1→v3 để giữ nguyên dữ liệu/cột hiện có đồng thời thêm cột Real Due Date cho bảng cũ trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/domain/ColumnKey.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt`.
- [X] T008 [US1] Tích hợp bulk changelog vào refresh trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`; chỉ áp dụng ngày khi tất cả batch/trang thành công, khi API lỗi thì giữ Real Due Date cũ của issue theo `issueId` (fallback `key`), để trống issue mới và báo rõ ngày chưa cập nhật, còn hủy refresh phải giữ hành vi không commit.
- [X] T009 [US1] Hiển thị giá trị `Real Due Date` tại cột saved table bằng formatter Updated hiện có trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`; không đưa cột này vào danh sách cột query thông thường.

**Checkpoint**: Story 1 độc lập kiểm thử được bằng MockEngine và saved-table snapshot, không cần mở calendar.

---

## Phase 4: User Story 2 - Nhận biết phần thời gian quá hạn trên calendar (Priority: P2)

**Goal**: Tiếp nối thanh timeline bằng đoạn đỏ từ ngày sau Dynamic Due Date tới Real Due Date, không làm thay đổi cửa sổ calendar.

**Independent Test**: Với snapshot có ngày hạn động và ngày thực tế trước/cùng/sau hạn, xác nhận chỉ task hoàn thành sau hạn có đoạn đỏ bắt đầu ngày kế tiếp hạn và kết thúc inclusive đúng ngày thực tế; kiểm tra cắt đoạn ở giới hạn calendar.

### Tests for User Story 2

> Viết test projection trước implementation và xác nhận test thất bại trước khi thêm quy tắc mới.

- [X] T010 [P] [US2] Thêm test projection cho đoạn quá hạn bắt đầu ngày kế tiếp, kết thúc inclusive, ngày thực tế bằng/trước hạn, mốc thiếu/sai, timezone, và clipping/ngoài cửa sổ trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableCalendarTest.kt`.

### Implementation for User Story 2

- [X] T011 [US2] Mở rộng `SavedTableCalendarRow` và `projectSavedTableCalendar` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableCalendar.kt` để chiếu Real Due Date theo timezone bảng; chỉ tạo khoảng đỏ khi ngày Real Due Date sau Dynamic Due Date, bắt đầu ở ngày kế tiếp, kết thúc inclusive, cắt theo cửa sổ cố định và báo tiếp diễn ở mép trái/phải hoặc khoảng hoàn toàn ngoài cửa sổ.
- [X] T012 [US2] Vẽ đoạn quá hạn nối tiếp sau thanh dự kiến bằng màu đỏ (`MaterialTheme.colorScheme.error`) và áp dụng dấu tiếp diễn ở mép trái/phải hoặc thông báo ngoài phạm vi trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt`; không đổi trục ngày, thứ tự hàng hoặc thêm dấu hiệu khác màu.

**Checkpoint**: Story 2 hoàn chỉnh khi projection domain xác định chính xác khoảng và calendar render đoạn đỏ đúng trên snapshot đã lưu.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Xác nhận tích hợp và chất lượng toàn feature.

- [X] T013 [P] Cập nhật các bước xác nhận Real Due Date, thất bại changelog và đoạn đỏ trong `specs/007-real-due-date/quickstart.md` nếu đường dẫn test hoặc hành vi thực tế thay đổi khi triển khai.
- [X] T014 Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`; xử lý lỗi hồi quy trong `composeApp/src/commonMain/kotlin/com/jiratools/` và `composeApp/src/commonTest/kotlin/com/jiratools/` trước khi hoàn tất.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; cấu trúc ứng dụng hiện tại đã đáp ứng.
- **Foundational (Phase 2)**: Không có task riêng; không tạo thêm blocker dùng chung.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay; deliver MVP ngày thực tế trong saved table.
- **User Story 2 (Phase 4)**: Phụ thuộc US1 vì projection cần `realDueAtMillis` được lưu trong snapshot.
- **Polish (Phase 5)**: Sau các story cần phát hành; T014 chạy sau mọi thay đổi nguồn/test.

### User Story Dependencies

- **US1 (P1)**: Không phụ thuộc story khác.
- **US2 (P2)**: Phụ thuộc model/snapshot Real Due Date của US1; có thể bắt đầu projection tests khi model đã có.

### Task Dependency Graph

```text
T001 -> T005
T002 + T005 -> T006
T003 -> T007
T004 + T005 + T006 + T007 -> T008
T007 -> T009
T007 -> T010 -> T011 -> T012
T013 may run independently after implementation documentation is available
T001-T012 + T013 -> T014
```

### Within Each User Story

- Viết test trước implementation và xác nhận test fail theo nguyên tắc test-first.
- US1: T001–T004 là test riêng theo file và có thể phát triển song song; triển khai DTO/API, mapper, model/migration, orchestration, rồi render cột theo dependency tương ứng.
- US2: T010 phải có trước T011; T012 cần projection đã hoàn thành.
- T014 là xác minh cuối cùng và phụ thuộc mọi thay đổi nguồn/test.

### Parallel Opportunities

- Song song test-first US1: T001, T002, T003 và T004 chạm các file test riêng.
- Sau T005 (DTO), có thể triển khai T006 mapper và T007 model/migration độc lập; T008 cần T005–T007.
- T009 có thể song song với phần orchestration T008 sau khi T007 hoàn tất vì sửa file UI bảng riêng.
- T010 cần các trường Real Due Date từ T007 và đi trước implementation projection T011.

## Parallel Example: User Story 1

```text
# Viết các test trên những file riêng trước khi triển khai:
T001 JiraApiClientChangelogTest.kt
T002 StatusChangelogMapperTest.kt
T003 SavedTableStoreTest.kt
T004 SavedTablesViewModelTest.kt

# Sau khi DTO changelog/API type đã được thống nhất:
T006 StatusChangelogMapper.kt
T007 Ticket/ColumnKey/SavedTableStore migration

# Sau T007, có thể render cột trong khi hoàn tất refresh:
T008 SavedTablesViewModel.kt
T009 TicketTable.kt
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Hoàn tất test-first US1 và xác nhận tests fail trước implementation.
2. Thêm changelog bulk API/mapper, snapshot fields, migration và refresh fallback.
3. Hiển thị Real Due Date trong bảng đã lưu.
4. Chạy test US1; xác nhận lỗi changelog không xóa ngày cũ và migration giữ dữ liệu.
5. Dừng để demo cột Real Due Date mà chưa cần timeline calendar.

### Incremental Delivery

1. Hoàn thành MVP US1.
2. Thêm test projection US2 rồi tính khoảng quá hạn trong shared domain.
3. Render đoạn màu đỏ trên calendar và xác nhận clipping.
4. Chạy quickstart, test shared, ktlint và build Desktop.
