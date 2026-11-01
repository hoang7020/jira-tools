---
description: "Task list for Original Expected Time and dynamic saved-table time columns"
---

# Tasks: Thời gian động và ước lượng công việc

**Input**: Tài liệu thiết kế trong `specs/005-dynamic-time-columns/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/jira-and-table-ui.md`  
**Tests**: Bắt buộc theo Constitution II: test trong `commonTest`, xác nhận thất bại trước implementation; Jira HTTP được giả lập bằng Ktor `MockEngine`.  
**Organization**: Chia theo user story và priority trong `spec.md`; các task dùng path của module `composeApp`.

Mỗi task có checkbox, ID tuần tự, marker `[P]` chỉ khi có thể làm song song, story label cho task thuộc user story và đường dẫn file cụ thể.

## Phase 1: Setup

**Mục đích**: Khai báo phụ thuộc shared multiplatform được plan lựa chọn.

- [X] T001 Thêm `kotlinx-datetime` phiên bản `0.8.0` vào `gradle/libs.versions.toml` và khai báo dependency cho `commonMain` trong `composeApp/build.gradle.kts`.

## Phase 2: Foundational

**Mục đích**: Bổ sung các trường domain/persistence dùng chung trước khi triển khai từng story.

- [X] T002 [P] Bổ sung field nullable có default tương thích JSON cũ vào `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt`: `issueId` với ràng buộc “Jira issue ID ổn định; bắt buộc để tạo record start mới”; `statusCategoryKey` với “chỉ `indeterminate` được tính là In Progress. Không dùng tên trạng thái.”; `updatedAtMillis` với “null nếu thiếu/không parse được.”; `originalExpectedTime` với “Chuỗi `timetracking.originalEstimate` nguyên gốc; null khi Jira không trả estimate.”; `dynamicStartTime` với “Giá trị hiển thị đã ghi nhận; bằng `updated` ở snapshot đầu tiên hợp lệ.”; `dynamicStartAtMillis` với “Instant của start để tính toán và sắp xếp.”; `dynamicDueDate` với “Due Date định dạng theo timezone Jira của SavedTable.”; và `dynamicDueAtMillis` với “Instant của due để sắp xếp theo thời gian thực, không theo chuỗi hiển thị.”
- [X] T003 [P] Thêm `DynamicStartTimeRecord`, `SavedTable.jiraTimeZone`, `SavedTableCatalog.dynamicStartTimes` (mặc định rỗng) trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt` và `Connected.jiraTimeZone` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt`. Record phải khóa theo `normalizedJiraBaseUrl` + `issueId`, dùng `normalizeBaseUrl`, giữ ID ổn định thay vì issue key, lưu `startedAtMillis` và `displayValue` đúng bằng Updated lần đầu; record đã tạo bất biến và không bị dọn khi issue/bảng vắng mặt. `jiraTimeZone` giữ IANA timezone đã xác minh để tính offline; thiếu/sai timezone làm Due Date trống và detail view báo lý do.

---

## Phase 3: User Story 1 — Xem Original Expected Time ở mọi bảng (Priority: P1) 🎯 MVP

**Mục tiêu**: Lấy và hiển thị nguyên giá trị Original Estimate từ Jira trong query table và mọi saved table; giữ dữ liệu/bảng cũ qua migration.

**Independent Test**: Dùng Jira `MockEngine` với issue có và không có `timetracking.originalEstimate`; chạy query rồi lưu/làm mới saved table. Xác nhận giá trị gốc hiển thị chính xác hoặc trống, Original Expected Time bật mặc định ở cả hai nơi, các Dynamic columns không xuất hiện trong query và schema v1 được giữ nguyên dữ liệu.

### Tests for User Story 1

> Viết các test dưới đây trước implementation, chạy để xác nhận thất bại; không gọi Jira thật.

- [X] T004 [P] [US1] Bổ sung test migration `config.json` v1 trong `composeApp/src/commonTest/kotlin/com/jiratools/data/AppConfigStoreTest.kt`: giữ base URL, email, JQL, sort và lựa chọn cột cũ; thêm Original Expected Time mặc định, không đưa Dynamic columns vào query; schema lỗi/tương lai không bị ghi đè.
- [X] T005 [P] [US1] Bổ sung test migration `saved-tables.json` v1 trong `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableStoreTest.kt`: giữ tên/JQL/tickets/cột cũ, thêm ba cột mới, field Ticket mới và timezone nhận default null, ledger rỗng, schema tương lai/JSON hỏng vẫn theo cơ chế lỗi hiện có.
- [X] T006 [P] [US1] Bổ sung test search fields trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientSearchTest.kt`, xác nhận request có `timetracking` và vẫn giữ các field hiện dùng.
- [X] T007 [P] [US1] Bổ sung test trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt` cho issue ID, status category, Original Estimate nguyên gốc, Updated có offset `+HHMM`, estimate/timestamp/status bị thiếu và Updated không parse được.
- [X] T008 [P] [US1] Bổ sung test default/migration cột query trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelPreferencesTest.kt` và `composeApp/src/commonTest/kotlin/com/jiratools/domain/JqlOrderByTest.kt`: Original Expected Time bật, hai Dynamic columns không khả dụng, KEY vẫn luôn hiển thị và sort dùng JQL field `originalEstimate`.
- [X] T009 [P] [US1] Bổ sung test nhãn và chiều rộng Original Expected Time cùng các cột saved mới trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TableColumnLayoutTest.kt`.
- [X] T010 [P] [US1] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/WorkDurationTest.kt` cho `WorkDuration.parse(raw: String): WorkDuration?`: kiểm tra `15m`, `1h`, `1D`, `1s`, `0.5h`, `1d 2h`, `0`, `1d = 8h`, `1w = 40h`; nhận toàn chuỗi gồm một hay nhiều thành phần không âm, số nguyên/thập phân dấu chấm, đơn vị `s/m/h/d/w` không phân biệt hoa thường; trả null với rỗng, âm, sai cú pháp, đơn vị lạ và overflow.
- [X] T011 [P] [US1] Bổ sung test vào `composeApp/src/commonTest/kotlin/com/jiratools/domain/TicketSortingTest.kt` để Original Expected Time được sort theo tổng thời lượng đã parse, không sort chuỗi lexicographic; cover giá trị thiếu/không parse được theo thứ tự rỗng hiện có.

### Implementation for User Story 1

- [X] T012 [US1] Khai báo `ORIGINAL_EXPECTED_TIME`, `DYNAMIC_START_TIME` và `DYNAMIC_DUE_DATE`, hai bộ cột query/saved, JQL field nullable, default query/saved và version schema v2 trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/ColumnKey.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/domain/UserPreferences.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt`; Original dùng JQL field `originalEstimate`, Dynamic columns không có JQL field, KEY luôn khả dụng.
- [X] T013 [P] [US1] Thực hiện migration đọc v1→v2 trong `composeApp/src/commonMain/kotlin/com/jiratools/data/AppConfigStore.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt`: giữ toàn bộ cấu hình/snapshot hiện có, thêm cột theo default query/saved, khởi tạo ledger rỗng và chỉ ghi v2 ở lần ghi hợp lệ tiếp theo.
- [X] T014 [P] [US1] Mở rộng Jira DTO/search mapping trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraApiClient.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt` để lấy `IssueDto.id`, `status.statusCategory.key`, `updated` và `timetracking.originalEstimate`; giữ chuỗi estimate/Updated để hiển thị, parse Updated thành instant, không làm lỗi ticket khi field thiếu.
- [X] T015 [P] [US1] Triển khai `WorkDuration.parse(raw: String): WorkDuration?` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/WorkDuration.kt` theo grammar đã test, chuẩn hóa thành thời lượng có thể so sánh, kiểm tra overflow và không chấp nhận chuỗi một phần hoặc đơn vị chưa hỗ trợ.
- [X] T016 [P] [US1] Cập nhật query column availability/defaults và sorting trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/domain/JqlOrderBy.kt`; không cho query dùng Dynamic columns và dùng `originalEstimate` cho JQL sort của Original Expected Time.
- [X] T017 [US1] Cập nhật label/value mapping, chooser, chiều rộng và saved-table column availability trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TableColumnLayout.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketSorting.kt`; hiển thị nguyên estimate Jira, sort estimate saved bằng `WorkDuration`, giữ KEY và hành vi cột hiện có.

**Checkpoint**: Query và saved table hiển thị Original Expected Time; migration giữ dữ liệu cũ; task không estimate vẫn hiện với ô trống.

---

## Phase 4: User Story 2 — Ghi nhận Dynamic Start Time lần đầu task vào In Progress (Priority: P2)

**Mục tiêu**: Ghi một mốc Updated bất biến theo Jira site + issue ID, chia sẻ cho mọi saved table và giữ cả khi issue tạm vắng.

**Independent Test**: Dùng saved-table store và Jira giả lập để lưu task chưa In Progress, sau đó quan sát task có category `indeterminate`; xác nhận start bằng Updated của lần quan sát hợp lệ đầu tiên và không đổi qua refresh, đổi trạng thái, reload hoặc khi task xuất hiện trong bảng khác. Lỗi/hủy refresh phải giữ nguyên snapshot và ledger.

### Tests for User Story 2

> Viết và chạy các test lifecycle trước khi sửa luồng lưu/làm mới.

- [X] T018 [US2] Bổ sung test lifecycle trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`: bảng mới bật ba cột đúng mặc định; chỉ category `indeterminate` được ghi start; custom status name được hỗ trợ; thiếu ID/Updated chưa tạo record nhưng được retry; record đầu tiên bất biến, dùng chung qua nhiều bảng, tồn tại sau khi issue/bảng bị xóa và reload; lỗi trang, hủy hoặc lỗi ghi không commit.

### Implementation for User Story 2

- [X] T019 [US2] Cập nhật luồng tạo saved table trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` để giữ/ghi toàn `SavedTableCatalog`, chuẩn hóa Jira base URL, khởi tạo start ngay khi snapshot tạo bảng đã ở category `indeterminate`, và bật mặc định các cột Original Expected Time/Dynamic Start Time/Dynamic Due Date; các thao tác rename/delete/column phải giữ ledger.
- [X] T020 [US2] Cập nhật luồng refresh trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`: chỉ sau khi nhận đủ trang mới tạo record chưa có, materialize start chung cho snapshot các bảng liên quan và ghi catalog một lần; giữ record khi issue vắng mặt, không ghi đè mốc đã có, không đổi StateFlow/disk khi Jira lỗi, hủy hoặc store ghi lỗi.

**Checkpoint**: Dynamic Start Time được khởi tạo đúng một lần, dùng chung và bền qua refresh/reload; refresh thất bại không làm mất dữ liệu.

---

## Phase 5: User Story 3 — Xem Dynamic Due Date theo ước lượng ban đầu (Priority: P3)

**Mục tiêu**: Tính Due Date từ start bất biến + Original Expected Time theo lịch thứ Hai-thứ Sáu, 08:00–12:00 và 13:00–17:00 trong timezone Jira đã lưu.

**Independent Test**: Với instant và timezone cố định, kiểm tra estimate qua giờ nghỉ trưa/cuối tuần/ngoài ca, `1d`, `1w`, số thập phân, zero và DST. Kiểm tra estimate mới làm Due Date tính lại từ start cũ; thiếu/sai timezone hoặc estimate làm Due Date trống kèm cảnh báo mà không ẩn dữ liệu còn lại.

### Tests for User Story 3

> Viết test lịch, timezone, state và sort trước khi triển khai calculator/profile mapping.

- [X] T021 [P] [US3] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/WorkingTimeCalculatorTest.kt` cho lịch 08:00–12:00/13:00–17:00, thứ Hai-thứ Sáu, ngoài ca, nghỉ trưa, cuối tuần, `1d = 8h`, `1w = 40h`, zero giữ nguyên instant, timezone và chuyển DST; xác nhận Monday 11:30 + `1h` = Monday 13:30, Friday 16:00 + `1d` = Monday 16:00, Monday 08:00 + `1w` = Friday 17:00 và Friday 18:00 + `30m` = Monday 08:30.
- [X] T022 [P] [US3] Bổ sung test vào `composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt`: đọc `timeZone` từ `/rest/api/3/myself` khi connect/verify; thiếu hoặc sai timezone không biến kết nối hợp lệ thành thất bại.
- [X] T023 [P] [US3] Bổ sung test Due Date trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`: tính từ start ledger; thay đổi estimate chỉ tính lại due; thiếu start/estimate, estimate lỗi hoặc timezone thiếu/sai cho due trống, cảnh báo phù hợp và giữ các field/ticket khác.
- [X] T024 [P] [US3] Bổ sung test trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/TicketSortingTest.kt` để Dynamic Start Time/Due Date sort bằng instant millis, không theo chuỗi đã định dạng, đồng thời giữ thứ tự xử lý giá trị thiếu hiện tại.

### Implementation for User Story 3

- [X] T025 [P] [US3] Thêm `MyselfDto.timeZone` và map timezone của profile sang `ConnectionStatus.Connected.jiraTimeZone` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt`; hỗ trợ cả connect mới lẫn verify credential đã lưu, cho phép timezone vắng mặt.
- [X] T026 [P] [US3] Triển khai `WorkingTimeCalculator.addWorkingTime(startAtMillis: Long, duration: WorkDuration, timeZoneId: String): Long?` và `formatInTimeZone(millis: Long, timeZoneId: String): String?` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/WorkingTimeCalculator.kt` bằng `kotlinx-datetime`; cộng theo phiên làm việc địa phương, bỏ qua trưa/cuối tuần, theo DST, đưa start dương ngoài lịch sang phiên kế tiếp, giữ nguyên start khi duration bằng 0 và định dạng Due Date là `yyyy-MM-dd HH:mm`.
- [X] T027 [P] [US3] Mở rộng comparator trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketSorting.kt` để Dynamic Start Time/Due Date so sánh `dynamicStartAtMillis`/`dynamicDueAtMillis` thay vì text; giá trị thiếu tiếp tục theo quy tắc sort rỗng hiện có.
- [X] T028 [US3] Tích hợp timezone và tính toán vào save/refresh trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`: lưu timezone profile đã xác minh trên SavedTable; khi refresh `Unverified` giữ timezone đã lưu; tính lại Due Date từ start ledger và estimate mới cho từng bảng theo timezone riêng; đưa cảnh báo timezone thiếu/sai ra StateFlow riêng, không cảnh báo lỗi estimate; cập nhật state chỉ sau khi toàn catalog ghi thành công.
- [X] T029 [US3] Render cảnh báo timezone không chặn từ StateFlow trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`; để Dynamic Due Date trống nhưng vẫn hiển thị Original Expected Time, Dynamic Start Time và các dữ liệu ticket khác.

**Checkpoint**: Due Date tuân thủ lịch/timezone, được tính lại từ start cố định, sort theo instant và giải thích rõ trường hợp không thể tính.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Mục đích**: Kiểm tra hồi quy và xác nhận các kịch bản quickstart.

- [X] T030 Chạy `./gradlew build`, `./gradlew :composeApp:jvmTest` và `./gradlew ktlintCheck`; hoàn tất kịch bản Jira giả lập/offline và lịch mẫu trong `specs/005-dynamic-time-columns/quickstart.md`, xử lý mọi lỗi do feature gây ra.

## Dependencies & Execution Order

### Phase dependencies

- **Setup**: T001 không có prerequisite.
- **Foundational**: T002 và T003 cần T001; hai task sửa các model file độc lập và có thể chạy song song. Hoàn tất foundation trước US1.
- **US1 (P1)**: Sau foundation; thiết lập Jira mapping, cột và parser estimate nền tảng.
- **US2 (P2)**: Sau US1 vì cần issue ID, status category, Updated đã map và catalog v2.
- **US3 (P3)**: Sau US1 và US2 vì cần estimate parser, ledger start và snapshot refresh.
- **Polish**: T030 sau mọi story được chọn.

### Dependencies within stories

- **US1**: T004–T011 là test-first và có thể viết/chạy song song sau foundation. T012 bổ sung column/schema contracts. T013–T016 có thể chạy song song sau T012; T017 cần Jira mapping T014 và parser T015.
- **US2**: T018 trước implementation; T019 trước T020 vì cả hai sửa `SavedTablesViewModel.kt` và refresh phụ thuộc cấu trúc catalog từ luồng save.
- **US3**: T021–T024 là các test độc lập và có thể làm song song. Sau test, T025, T026 và T027 có thể triển khai song song; T028 phụ thuộc T020, T025, T026; T029 phụ thuộc T028.

### Parallel execution examples

**US1 — chạy nhóm test cùng lúc, sau đó triển khai độc lập sau T012:**

```text
T004 AppConfigStoreTest.kt
T005 SavedTableStoreTest.kt
T006 JiraApiClientSearchTest.kt
T007 TicketMapperTest.kt
T008 QueryViewModelPreferencesTest.kt
T009 TableColumnLayoutTest.kt
T010 WorkDurationTest.kt
T011 TicketSortingTest.kt
```

Sau T012, có thể chạy song song T013 (migration), T014 (Jira mapping), T015 (parser) và T016 (query defaults/JQL); hoàn thành T017 sau T014/T015.

**US2 — làm tuần tự do cùng ViewModel và thứ tự state transition:**

```text
T018 SavedTablesViewModelTest.kt -> T019 save/catalog -> T020 refresh/atomic commit
```

**US3 — tách test và domain implementation theo file:**

```text
T021 WorkingTimeCalculatorTest.kt
T022 ConnectionRepositoryTest.kt
T023 SavedTablesViewModelTest.kt
T024 TicketSortingTest.kt
```

Sau đó chạy song song T025 (timezone), T026 (calculator) và T027 (sort); tích hợp T028 rồi UI warning T029.

## Implementation Strategy

### MVP

Hoàn thành Setup + Foundation + **US1**. MVP hiển thị Original Expected Time ở query/saved tables, hỗ trợ sorting và mở dữ liệu cũ sau migration. Chưa phát hành Dynamic Start Time/Due Date cho đến khi US2/US3 hoàn tất.

### Incremental delivery

1. Hoàn tất T001–T003 để sẵn sàng model/phụ thuộc.
2. Hoàn tất T004–T017, chạy độc lập US1 và kiểm tra migration.
3. Hoàn tất T018–T020, xác minh vòng đời Dynamic Start Time và tính nguyên tử.
4. Hoàn tất T021–T029, xác minh lịch/timezone/Due Date/cảnh báo.
5. Hoàn tất T030 trước khi merge.

## Completion Criteria

- Mỗi task có checkbox, ID liên tục, nhãn `[P]` chỉ ở task song song được, story label ở mọi task US1/US2/US3 và file path cụ thể.
- Test logic shared nằm trong `commonTest`; Jira integration dùng MockEngine, không gọi Jira thật.
- Independent test criteria: US1 kiểm tra mapping/hiển thị/migration; US2 kiểm tra start bất biến/chia sẻ/persistence/atomicity; US3 kiểm tra parser lịch/timezone/sort/warning.
- MVP đề xuất: **US1 — Xem Original Expected Time ở mọi bảng**.
