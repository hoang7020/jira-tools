# Tasks: Thùng rác bảng đã lưu

**Input**: Tài liệu thiết kế trong `/specs/012-saved-table-trash/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/saved-table-trash.md`, `quickstart.md`

**Tests**: Bao gồm test tasks theo yêu cầu Test-First bắt buộc trong Constitution.

**Organization**: Tasks được nhóm theo user story để có thể triển khai và kiểm chứng theo từng phần.

## Phase 1: Setup

**Purpose**: Dự án KMP, Gradle, commonMain/commonTest và tooling hiện có; không cần thêm cấu hình hoặc dependency cho feature này.

Không có task setup.

---

## Phase 2: Foundational

**Purpose**: Mở rộng model và schema catalog dùng chung trước khi triển khai các luồng user story.

- [X] T001 [P] Thêm test migration schema v4 sang v5 và round-trip `trashedTables` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableStoreTest.kt`; xác nhận bảng hoạt động, Dynamic Start Time ledger và nội dung trash được giữ đúng.
- [X] T002 [P] Thêm test serialization cho `TrashedSavedTable` trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableTrashTest.kt`; xác nhận round-trip giữ nguyên `SavedTable` payload và `deletedAtMillis`.
- [X] T003 Thêm `TrashedSavedTable` và `SavedTableCatalog.trashedTables` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt`; dùng `table: SavedTable` bắt buộc và `deletedAtMillis: Long` bắt buộc, trong đó `deletedAtMillis` là thời điểm chuyển vào thùng rác để sắp xếp mới nhất trước; nâng `SAVED_TABLES_SCHEMA_VERSION` lên 5.
- [X] T004 Cập nhật migration trong `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt` để đọc schema v1-v4 với `trashedTables` rỗng, giữ nguyên các migration hiện có và đọc/ghi schema v5.

**Checkpoint**: Model catalog v5 và migration đã có test; các user story có thể dùng danh sách trash chung.

---

## Phase 3: User Story 1 - Chuyển bảng đã lưu vào thùng rác (Priority: P1) 🎯 MVP

**Goal**: Người dùng xác nhận xoá bảng hoạt động thì bảng được chuyển vào thùng rác, giữ nguyên dữ liệu và có thể mở thùng rác từ sidebar.

**Independent Test**: Tạo bảng đã lưu có tickets và burndown history, xác nhận chuyển vào thùng rác; bảng không còn trong danh sách hoạt động, xuất hiện trong trash với nội dung nguyên vẹn, và destination sidebar mở được màn hình trash.

### Tests for User Story 1

- [X] T005 [US1] Thêm test cho chuyển bảng vào trash trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`; kiểm tra bảng được loại khỏi active list, thêm vào trash cùng payload/history, bảng khác và `dynamicStartTimes` không đổi, selection đang mở được clear, và write failure giữ nguyên state/catalog cùng thông báo lỗi.

### Implementation for User Story 1

- [X] T006 [US1] Thêm `trashedTables` vào UI state và triển khai thao tác move trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`; tạo catalog bất biến, ghi đúng một lần, chỉ cập nhật StateFlow sau write thành công, thay `deleteTable` phá huỷ bằng thao tác chuyển trash và giữ nguyên ledger Dynamic Start Time.
- [X] T007 [P] [US1] Thêm destination sidebar **Thùng rác** vào `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/AppNavigation.kt`; hiển thị nhất quán ở sidebar mở rộng/thu gọn và có accessible label.
- [X] T008 [US1] Tạo `SavedTableTrashArea` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableTrashArea.kt`; render mục trash theo `deletedAtMillis` giảm dần, thông tin nhận diện bảng và trạng thái rỗng có giải thích, không cung cấp thao tác calendar/chart/refresh.
- [X] T009 [US1] Wire destination và màn hình trash trong `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`; chuyển confirm xoá bảng hiện hành thành move-to-trash và đóng detail bảng nếu bảng đang mở được chuyển thành công.

**Checkpoint**: Chuyển vào trash và truy cập danh sách hoạt động độc lập; dữ liệu không bị xoá vĩnh viễn.

---

## Phase 4: User Story 2 - Khôi phục hoặc xoá vĩnh viễn bảng (Priority: P2)

**Goal**: Người dùng khôi phục bảng trash an toàn, xử lý xung đột tên bằng tên thay thế, hoặc xác nhận xoá vĩnh viễn.

**Independent Test**: Từ trash, khôi phục bảng không conflict và xác nhận nguyên ID/nội dung/history; khôi phục bảng trùng tên phải yêu cầu tên hợp lệ mà không ghi đè; xác nhận permanent delete loại bỏ riêng bảng và cancel giữ nguyên.

### Tests for User Story 2

- [X] T010 [US2] Thêm test restore và permanent delete trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`; kiểm tra giữ nguyên ID/JQL/tickets/columns/timezone/burndown, tên trùng yêu cầu tên khác (trim, không rỗng, không trùng không phân biệt hoa/thường), không ghi đè bảng hoạt động, permanent delete giữ ledger dùng chung, và mọi write failure giữ nguyên state/catalog cùng thông báo lỗi.

### Implementation for User Story 2

- [X] T011 [US2] Triển khai restore và permanent delete trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`; mỗi thao tác ghi catalog một lần, restore giữ nguyên toàn bộ `SavedTable` trừ tên thay thế hợp lệ, permanent delete loại bỏ ticket/burndown gắn với bảng nhưng giữ `dynamicStartTimes`, và state chỉ đổi sau write thành công.
- [X] T012 [US2] Bổ sung callback và nút **Khôi phục**/**Xoá vĩnh viễn** theo từng mục trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableTrashArea.kt`; chuyển yêu cầu hành động lên App thay vì thực hiện nghiệp vụ trong composable.
- [X] T013 [US2] Thêm dialog nhập tên thay thế khi restore bị conflict và dialog xác nhận xoá vĩnh viễn trong `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`; tên thay thế phải tuân theo `validateTableName`, confirm nêu rõ không thể hoàn tác, cancel không gọi thao tác ghi, và lỗi hiển thị theo state message hiện có.

**Checkpoint**: Restore, conflict handling và permanent delete có thể kiểm thử độc lập; không mất dữ liệu khi huỷ hoặc write lỗi.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Kiểm chứng toàn bộ feature và cập nhật hướng dẫn nếu thay đổi trong triển khai.

- [X] T014 Chạy và xử lý lỗi automated checks theo `specs/012-saved-table-trash/quickstart.md`: `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`.
- [ ] T015 Kiểm tra thủ công giao diện Desktop theo `specs/012-saved-table-trash/quickstart.md`: sidebar mở/thu gọn, move, restore, nhập tên khi conflict, permanent delete, cancel và trạng thái trash trống.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; cấu hình KMP và Gradle đã tồn tại.
- **Foundational (Phase 2)**: T001 và T002 có thể chạy song song; T003 phụ thuộc T001/T002 để kiểm chứng Red-Green; T004 phụ thuộc T003.
- **User Story 1 (Phase 3)**: Phụ thuộc hoàn tất Phase 2; T005 test trước T006; T007 có thể chạy song song với T005/T006 vì sửa file sidebar riêng; T008 phụ thuộc state từ T006; T009 phụ thuộc T006-T008.
- **User Story 2 (Phase 4)**: Phụ thuộc US1 vì thao tác diễn ra trên danh sách trash; T010 test trước T011; T012 phụ thuộc T011; T013 phụ thuộc T011/T012.
- **Polish (Phase 5)**: T014 phụ thuộc các story cần phát hành; T015 là xác minh giao diện thủ công sau build.

### User Story Dependencies

- **US1 (P1)**: Phụ thuộc model/schema v5 ở Phase 2; không phụ thuộc story khác.
- **US2 (P2)**: Phụ thuộc US1 để có thể đưa bảng vào trash và hiển thị danh sách trước khi khôi phục/xoá vĩnh viễn.

### Parallel Opportunities

- Phase 2: T001 và T002 ở hai file test khác nhau.
- US1: T007 sửa `AppNavigation.kt` độc lập với test/model `SavedTablesViewModel`; có thể song song trước khi wire App ở T009.
- Các thay đổi cùng `SavedTablesViewModelTest.kt`, `SavedTablesViewModel.kt`, `SavedTableTrashArea.kt` hoặc `App.kt` được giữ tuần tự để tránh xung đột file.

## Parallel Example: User Story 1

```text
Task: T005 — thêm test move-to-trash trong SavedTablesViewModelTest.kt
Task: T007 — thêm destination Thùng rác trong AppNavigation.kt
```

Sau khi T006 hoàn tất:

```text
Task: T008 — tạo màn hình danh sách trash trong SavedTableTrashArea.kt
```

## Parallel Example: User Story 2

US2 dùng chung file ViewModel, test và App wiring; không có nhóm implementation song song an toàn. Viết T010 trước, sau đó chạy tuần tự T011 → T012 → T013 để kiểm tra từng chuyển trạng thái.

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn tất Phase 2 để schema v5 đọc/ghi được.
2. Hoàn tất Phase 3 để xác nhận xoá chỉ chuyển bảng vào trash, truy cập được trash, và dữ liệu được giữ nguyên.
3. Dừng kiểm chứng độc lập US1; chỉ phát hành feature hoàn chỉnh sau khi US2 cho phép khôi phục hoặc xoá vĩnh viễn.

### Incremental Delivery

1. Phase 2: thêm model/migration và test tương ứng.
2. US1: cung cấp soft delete, sidebar và danh sách trash.
3. US2: thêm restore, name-conflict resolution và permanent delete có xác nhận.
4. Phase 5: chạy JVM tests, ktlint, build và quickstart Desktop.

## Notes

- Mọi task implementation ghi rõ file đích; task test phải được thực hiện trước logic tương ứng và xác nhận thất bại trước khi triển khai.
- Không thêm dependency hoặc storage mới; catalog và ledger hiện có tiếp tục là nguồn dữ liệu chuẩn.
