# Tasks: Cột Due Date trong kết quả truy vấn

**Input**: Tài liệu thiết kế trong `/specs/013-query-due-date/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/ui.md](./contracts/ui.md), [quickstart.md](./quickstart.md)

**Tests**: Có task kiểm thử vì Hiến pháp dự án yêu cầu test-first cho logic `commonMain` và fake/mock HTTP cho tích hợp Jira.

**Organization**: Các task được tổ chức theo user story để có thể triển khai và kiểm chứng độc lập.

## Quy ước đường dẫn

- Mã dùng chung: `composeApp/src/commonMain/kotlin/com/jiratools/`
- Kiểm thử dùng chung: `composeApp/src/commonTest/kotlin/com/jiratools/`

## Phase 1: Setup

**Purpose**: Hạ tầng KMP, Gradle Wrapper, `commonMain`/`commonTest` và HTTP fake hiện có; không cần khởi tạo hoặc thay đổi cấu hình dự án.

Không có task riêng trong phase này.

---

## Phase 2: Foundational

**Purpose**: Không có nền tảng dùng chung cần xây mới ngoài phạm vi user story; model, cột và migration được triển khai cùng US1.

Không có task riêng trong phase này.

---

## Phase 3: User Story 1 - Xem Due Date trong kết quả truy vấn (Priority: P1) 🎯 MVP

**Goal**: Đưa ngày lịch Due Date của Jira vào query và snapshot bảng đã lưu; hiển thị, bật/tắt và sắp xếp cột mà không làm thay đổi lựa chọn cột đã lưu trước đây.

**Independent Test**: Chạy truy vấn Jira qua HTTP fake với issue có và không có `duedate`; xác nhận ngày nguyên dạng và ô trống tương ứng, Due Date sắp xếp theo thời gian, cấu hình query cũ chỉ tự bật cột khi khớp chính xác bộ mặc định trước thay đổi, và bảng đã lưu cũ giữ nguyên `visibleColumns` trong khi bảng mới mặc định có cột.

### Tests for User Story 1

> Viết các kiểm thử này trước phần triển khai và xác nhận chúng thất bại trước khi chuyển sang implementation tasks.

- [X] T001 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt` để xác nhận `duedate` ánh xạ nguyên dạng vào Ticket, giá trị null/thiếu thành null, và `TicketMapper.requestFields` chứa `duedate`.
- [X] T002 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientSearchTest.kt` xác nhận request search gửi trường `duedate` trong `fields` qua HTTP fake.
- [X] T003 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/TicketSortingTest.kt` để xác nhận Due Date sắp xếp tăng/giảm theo ngày lịch, giá trị null/không hợp lệ không tạo ngày giả, và thứ tự ổn định được giữ khi giá trị tương đương.
- [X] T004 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/data/AppConfigStoreTest.kt` cho schema 3: chỉ thêm Due Date khi `visibleColumns` chính xác bằng bộ mặc định schema 3, giữ nguyên cấu hình đã tuỳ chỉnh, và mặc định schema hiện hành có Due Date.
- [X] T005 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` để xác nhận bảng mới mặc định có Due Date trong `visibleColumns`, còn bảng hiện hữu giữ lựa chọn cột của nó.
- [X] T006 [P] [US1] Bổ sung kiểm thử trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TableColumnLayoutTest.kt` xác nhận cột Due Date có kích thước hợp lệ và được bố trí khi hiển thị.

### Implementation for User Story 1

- [X] T007 [US1] Thêm `DUE_DATE` vào `ColumnKey` với nhãn `Due Date` và đưa vào bộ cột query/saved-table mặc định trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/ColumnKey.kt`; thêm `Ticket.dueDate: String? = null` để giữ ngày lịch nullable và tương thích dữ liệu đã lưu trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt`.
- [X] T008 [P] [US1] Thêm trường DTO có JSON name `duedate`, ánh xạ nguyên chuỗi vào `Ticket.dueDate`, và thêm `duedate` vào `TicketMapper.requestFields` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt`.
- [X] T009 [P] [US1] Thêm nhánh sắp xếp `ColumnKey.DUE_DATE` bằng ngày lịch, coi null/không parse được là thiếu thay vì ngày mặc định, và giữ các nhánh cột hiện tại trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketSorting.kt`.
- [X] T010 [P] [US1] Nâng `CURRENT_SCHEMA_VERSION` từ 3 lên 4; áp dụng migration schema 1/2 hiện có để chuẩn hoá cấu hình về schema 3, sau đó chỉ thêm `DUE_DATE` nếu tập cột bằng chính xác bộ mặc định schema 3, còn cấu hình tuỳ chỉnh giữ nguyên trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/UserPreferences.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/AppConfigStore.kt`.
- [X] T011 [US1] Hiển thị `Ticket.dueDate` nguyên dạng, để ô trống khi null, thêm độ rộng cột và hỗ trợ tiêu đề/sắp xếp trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TableColumnLayout.kt`; xác nhận bộ chọn cột Bảng đã lưu cung cấp `DUE_DATE` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`, nhưng không tự thêm cột vào `visibleColumns` của bảng cũ.

**Checkpoint**: US1 hoạt động độc lập khi mapping, migration, sorting, cột query/saved-table và kiểm thử đều hoàn tất.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Xác minh chất lượng toàn feature và tài liệu hướng dẫn.

- [X] T012 Chạy các lệnh trong `specs/013-query-due-date/quickstart.md` (`./gradlew :composeApp:jvmTest` và `./gradlew :composeApp:ktlintCheck build`), xử lý lỗi phát sinh từ thay đổi feature và xác nhận toàn bộ cổng chất lượng đạt.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Đã có sẵn; không có task.
- **Foundational (Phase 2)**: Không cần bổ sung hạ tầng; không có task.
- **US1 (Phase 3)**: Độc lập sau khi setup hiện tại sẵn sàng; các task kiểm thử phải được viết và chạy thất bại trước implementation.
- **Polish (Phase 4)**: Chỉ sau khi hoàn thành implementation US1.

### User Story Dependencies

- **US1 (P1)**: Không phụ thuộc user story khác; là toàn bộ MVP của feature.

### Dependency Graph

```text
T001 ─┐
T002 ─┤
T003 ─┤
T004 ─┼─> T007 ─┬─> T008 ─┐
T005 ─┤          ├─> T009 ─┼─> T011 ─> T012
T006 ─┘          └─> T010 ─┘
```

T001–T006 là các test ở những file khác nhau và có thể chuẩn bị song song. T008–T010 sửa các nhóm file riêng, có thể triển khai song song sau T007; T011 phụ thuộc model/mapper và ColumnKey.

### Parallel Opportunities

- Chạy song song T001–T006 khi viết test; các task tác động những file test riêng biệt.
- Sau T007, T008 (DTO/mapper), T009 (sorting), và T010 (preferences/config migration) có thể triển khai song song.
- T011 là điểm tích hợp UI chung cho query và saved-table, chạy sau các thay đổi model/mapper.

## Parallel Example: User Story 1

```text
# Viết kiểm thử trước, mỗi task ở file riêng:
T001 TicketMapperTest.kt
T002 JiraApiClientSearchTest.kt
T003 TicketSortingTest.kt
T004 AppConfigStoreTest.kt
T005 SavedTablesViewModelTest.kt
T006 TableColumnLayoutTest.kt

# Sau T007, triển khai các nhóm không trùng file:
T008 JiraDtos.kt + TicketMapper.kt
T009 TicketSorting.kt
T010 UserPreferences.kt + AppConfigStore.kt
```

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn tất các kiểm thử T001–T006 và xác nhận chúng thất bại trước triển khai.
2. Thêm model/cột dùng chung ở T007, rồi triển khai mapping, sorting và migration.
3. Hoàn thiện UI cột query/saved-table ở T011.
4. Chạy kiểm thử độc lập của US1, sau đó chạy đầy đủ lệnh kiểm chứng ở T012.

### Format Validation

Tất cả task đều theo định dạng checkbox `- [ ]`, mã tuần tự `T001`–`T012`, nhãn `[P]` chỉ cho task song song, nhãn `[US1]` có ở các task trong phase user story và không có ở setup/foundational/polish; mỗi mô tả chỉ rõ đường dẫn file.
