# Tasks: Bảng kết quả riêng lưu cục bộ

**Feature**: `003-saved-result-table` | **Ngày**: 2026-10-31
**Input**: Design documents trong `specs/003-saved-result-table/` — `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/saved-tables.md`, `quickstart.md`
**Prerequisites**: plan.md ✔, spec.md ✔, research.md ✔, data-model.md ✔, contracts/ ✔, quickstart.md ✔

## Ghi chú về kiểm thử (BẮT BUỘC — Hiến pháp Nguyên tắc II)

Hiến pháp dự án quy định **Kiểm thử trước (NON-NEGOTIABLE)**: mọi logic chia sẻ trong `commonMain` PHẢI có test trong `commonTest`, viết TRƯỚC khi triển khai và PHẢI thất bại (ĐỎ) trước khi triển khai (XANH).

- Test viết trước cho: kho dữ liệu (T008 → T009), hàm thuần tên bảng (T011 → T013), hàm thuần sắp xếp (T020 → T022), ViewModel (T012 → T014, T021 → T023, T027 → T028, T031 → T032).
- HTTP cho luồng làm mới dùng `MockEngine` + fixture JSON sẵn có của 001; **KHÔNG** test nào gọi Jira thật.
- Dịch vụ nền tảng dùng fake trong `commonTest` (`FakeAppPaths`, `FakeClock`, `FakeSecureStorage`, `RecordingUrlOpener`); lỗi ghi/đọc được mô phỏng bằng cờ trong fake.
- **Không** viết test Compose UI (đồng nhất với 001/002): bố cục và tương tác được kiểm chứng thủ công bằng KB-1..KB-7 trong `quickstart.md`.
- **KHÔNG** thêm phụ thuộc test mới: chỉ dùng `kotlin.test`, `kotlinx-coroutines-test`, `ktor-client-mock` đã khai báo trong `composeApp/build.gradle.kts` (Nguyên tắc IV).

## Tổ chức theo User Story

- **US1 (P1)**: Lưu bảng kết quả truy vấn thành một bảng riêng
- **US2 (P2)**: Xem lại các bảng đã lưu
- **US3 (P3)**: Làm mới bảng đã lưu từ Jira
- **US4 (P4)**: Quản lý các bảng đã lưu (đổi tên, xoá)

Mỗi user story tạo ra một phần gia tăng độc lập: có thể triển khai, kiểm thử và kiểm chứng riêng.

## Định dạng task

`- [ ] T### [P?] [US#?] Mô tả kèm đường dẫn file`

- `[P]`: có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành).
- `[US#]`: chỉ gắn cho task thuộc phase user story (Setup/Foundational/Polish không gắn).
- Task kiểm chứng thủ công ghi rõ "chờ người dùng kiểm chứng" khi cần phiên desktop hoặc site Jira thật.

## Quy ước đường dẫn (KMP, một module `composeApp`)

- `composeApp/src/commonMain/kotlin/com/jiratools/...` — logic chia sẻ (`app/`, `data/`, `domain/`, `platform/`, `ui/`)
- `composeApp/src/commonTest/kotlin/com/jiratools/...` — test logic chia sẻ
- `composeApp/src/jvmMain/kotlin/com/jiratools/...` — phần `actual`/implementation JVM + entry point
- Cấu hình build: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts` (**không sửa** trong tính năng này)

## Tệp được sửa ở nhiều phase (thứ tự bắt buộc, không chạy song song)

`app/App.kt` (T018 → T025) · `ui/saved/SavedTablesArea.kt` (T024 → T029 → T034) · `ui/saved/SavedTablesViewModel.kt` (T014 → T023 → T028 → T032) · `ui/saved/SavedTableDialogs.kt` (T016 → T033) · `commonTest/ui/saved/SavedTablesViewModelTest.kt` (T012 → T021 → T027 → T031)

---

## Phase 1: Setup (Xác lập baseline)

**Mục tiêu**: chốt trạng thái nền trước khi thay đổi để phát hiện hồi quy (SC-007) và xác nhận không cần phụ thuộc mới.

- [X] T001 Kiểm chứng baseline trước khi sửa mã: chạy `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` tại gốc repo với cấu hình hiện có trong `composeApp/build.gradle.kts`, xác nhận toàn bộ test hiện có (155 test của 001/002) đạt và lint sạch; ghi kết quả vào mục "Ghi kết quả / KB-7" của `specs/003-saved-result-table/quickstart.md`
- [X] T002 [P] Xác nhận không cần phụ thuộc mới: rà `composeApp/build.gradle.kts` (đã có `compose.material3`, `compose.foundation`, `kotlinx-serialization-json`, `kotlinx-coroutines-core`, `ktor-client-*`, `compose.desktop.currentOs`) đủ cho JSON lưu cục bộ, `AlertDialog`/`OutlinedTextField`, huỷ coroutine và `java.time`; KHÔNG sửa `gradle/libs.versions.toml`

**Checkpoint**: baseline xanh, không thay đổi phụ thuộc — có thể bắt đầu Phase 2.

---

## Phase 2: Foundational (Điều kiện tiên quyết — CHẶN mọi user story)

**Mục tiêu**: hai seam nền tảng (thời gian, tệp dữ liệu), model dữ liệu và kho đọc/ghi — thứ mà US1, US2, US3, US4 đều cần.

**⚠️ CRITICAL**: không bắt đầu user story nào trước khi phase này xong.

- [X] T003 [P] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/platform/AppClock.kt` (interface `AppClock { fun nowMillis(): Long; fun format(millis: Long): String }`) và `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmAppClock.kt` (`System.currentTimeMillis()`; `format` dùng `java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")` với `ZoneId.systemDefault()`) — theo `contracts/saved-tables.md` mục 2
- [X] T004 [P] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/platform/AppPaths.kt` với `savedTablesFile(): String`, `readSavedTables(): String?`, `writeSavedTables(content: String)` và cài đặt trong `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmAppPaths.kt`: tên tệp `saved-tables.json` cùng thư mục `config.json`; `readSavedTables` trả `null` khi tệp chưa tồn tại và **ném ngoại lệ** khi tệp có nhưng không đọc được; `writeSavedTables` tạo thư mục cha và ghi nguyên tử (`ATOMIC_MOVE`, có fallback như `moveIntoPlace` hiện có) và **ném ngoại lệ** khi ghi thất bại
- [X] T005 Cập nhật `composeApp/src/commonTest/kotlin/com/jiratools/platform/Fakes.kt`: mở rộng `FakeAppPaths` cho 3 phương thức mới (kho tệp trong bộ nhớ như `configFile`, thêm `failOnReadSavedTables`, `failOnWriteSavedTables`, `seedRawSavedTables(content)`, `storedSavedTables()`), thêm `FakeClock(var millis: Long = 1_700_000_000_000, var formatted: String = "14/11/2023 22:13")` trả giá trị cố định — phụ thuộc T003, T004
- [X] T006 [P] Thêm `@Serializable` cho `Ticket` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt` (import `kotlinx.serialization.Serializable`; **không** đổi tên/kiểu/giá trị mặc định của bất kỳ trường nào) — giữ nguyên `QueryPage`
- [X] T007 [P] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt` theo `data-model.md` mục 1–2: `const val SAVED_TABLES_SCHEMA_VERSION: Int = 1`; `@Serializable data class SavedTable(id: String, name: String, jql: String, jiraBaseUrl: String, savedAtMillis: Long, refreshedAtMillis: Long? = null, visibleColumns: Set<ColumnKey> = ColumnKey.all(), tickets: List<Ticket> = emptyList())`; `@Serializable data class SavedTableCatalog(schemaVersion: Int = SAVED_TABLES_SCHEMA_VERSION, tables: List<SavedTable> = emptyList())`; `sealed interface CatalogLoadResult { data class Loaded(...); data object Empty; data class Unreadable(val detail: String?) }`
- [X] T008 Viết test trước `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableStoreTest.kt` (PHẢI ĐỎ trước T009) cho `SavedTableStore`: (a) chưa có tệp → `CatalogLoadResult.Empty`; (b) ghi rồi đọc vòng giữ nguyên mọi trường (`name`, `jql`, `jiraBaseUrl`, `savedAtMillis`, `refreshedAtMillis = null` và `!= null`, `visibleColumns`, danh sách `tickets`, `id`) và `schemaVersion`; (c) JSON hỏng (`{"schemaVersion":1,"tables":[`) → `Unreadable`; (d) `schemaVersion` khác (`2`) → `Unreadable`; (e) khi `Unreadable`, tệp **không** bị xoá/ghi đè (so sánh `storedSavedTables()` trước–sau); (f) `writeSavedTables` ném lỗi → `save()` ném ra ngoài để giao diện báo (FR-015); (g) tệp chưa tồn tại không sinh cảnh báo
- [X] T009 Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt` (XANH cho T008): `Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }` giống `AppConfigStore`; `suspend fun load(): CatalogLoadResult` (đọc qua `paths.readSavedTables()` trên `ioDispatcher`, phân biệt `null` → `Empty`, parse/schema sai hoặc ngoại lệ đọc → `Unreadable(detail)`); `suspend fun save(catalog: SavedTableCatalog)` ghi qua `paths.writeSavedTables(...)`; `fun filePath(): String` cho thông báo FR-020
- [X] T010 Kiểm chứng Phase 2: chạy `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck`, xác nhận `SavedTableStoreTest` XANH, cặp `AppPaths`/`JvmAppPaths` và `AppClock`/`JvmAppClock` biên dịch trên target `jvm`, `Fakes.kt` dùng được, không lỗi lint

**Checkpoint**: hạ tầng dữ liệu và nền tảng sẵn sàng — US1..US4 có thể bắt đầu.

---

## Phase 3: User Story 1 (P1) — Lưu bảng kết quả truy vấn thành một bảng riêng 🎯 MVP

**Mục tiêu**: từ bảng truy vấn đang có kết quả, người dùng đặt tên và lưu thành một bảng riêng trên máy; bảng riêng chứa đủ mọi trường của mọi ticket đang có (kể cả cột đang ẩn) cùng JQL và địa chỉ Jira; lưu **không** gọi thêm Jira.

**Tiêu chí kiểm thử độc lập**: chạy một truy vấn, bấm "Lưu bảng", đặt tên, thấy thông báo thành công; mở `saved-tables.json` và đối chiếu số ticket/trường với lưới đang xem (KB-1).

**FR liên quan**: FR-001, FR-002, FR-003, FR-011, FR-012, FR-013, FR-014, FR-015, FR-016, FR-018

### Kiểm thử cho User Story 1 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

> **LƯU Ý**: T011 và T012 viết trước và chạy để xác nhận thất bại trước khi làm T013/T014.

- [X] T011 [P] [US1] Viết test `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableNameTest.kt` cho `normalizeTableName`, `validateTableName` và `newTableId`: `"  Báo cáo  "` → `Valid` và tên chuẩn hoá `"Báo cáo"`; `""`/`"   "` → `Empty`; `"báo CÁO"` khi `otherNames = ["Báo cáo"]` → `Duplicate`; `" Báo cáo "` khi `otherNames = ["Báo cáo"]` → `Duplicate` (so sánh sau trim, không phân biệt hoa/thường — FR-011); tên của chính bảng đang đổi không nằm trong `otherNames` → `Valid`; `newTableId(nowMillis = 1000, existingIds = ["1000"])` → `"1000-2"` và `["1000","1000-2"]` → `"1000-3"` (PHẢI thất bại trước T013)
- [X] T012 [P] [US1] Viết test `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` cho phần **lưu bảng** (PHẢI thất bại trước T014): lưu thành công tạo `SavedTable` với `name` đã trim, `id` từ `newTableId`, `jql` truyền vào, `jiraBaseUrl` lưu **nguyên văn** tham số truyền vào (tầng giao diện lấy từ `ConnectionStatus.Connected`), `savedAtMillis = FakeClock.millis`, `refreshedAtMillis = null`, `visibleColumns` = tham số truyền vào, `tickets` đúng thứ tự và **không** gọi mạng (MockEngine không nhận request nào); ghi tệp một lần qua `FakeAppPaths.storedSavedTables()`; tên rỗng → trả lỗi và **không** ghi tệp; tên trùng (khác hoa/thường) → lỗi và không ghi; khi `CatalogLoadResult.Unreadable` → từ chối lưu, không ghi, `message` nêu lý do (FR-020); thông báo thành công chứa tên bảng (FR-015)
- [X] T013 [US1] Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableName.kt` theo `data-model.md` mục 4 (XANH cho T011): `normalizeTableName(raw) = raw.trim()`; `sealed interface TableNameValidation { data object Valid; data object Empty; data object Duplicate }`; `validateTableName(raw, otherNames)` so sánh `trim().lowercase()`; `newTableId(nowMillis, existingIds)` thêm hậu tố `-2`, `-3`, … khi trùng
- [X] T014 [US1] Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` (XANH cho T012) theo `data-model.md` mục 6: `SavedTablesUiState` đúng các trường đã chốt; `StateFlow<SavedTablesUiState>`; `suspend fun load()` (dùng `SavedTableStore.load()`, đặt `catalogUnreadable`/`loadWarning` kèm `filePath()` khi `Unreadable`); `suspend fun saveTable(name, jql, tickets, visibleColumns, jiraBaseUrl)` (kiểm tra tên, `clock.nowMillis()`, từ chối khi `catalogUnreadable`, ghi qua store, đặt `message`); các hàm đều là `suspend` để tầng giao diện tự quản phạm vi coroutine như `QueryViewModel`/`ConnectionViewModel` hiện có
- [X] T015 [US1] Viết test bảo vệ `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableSecretTest.kt` (FR-016, SC-008) ngay sau khi luồng lưu có thật (T014) và trước khi đấu dây giao diện (T016+), để mọi rò rỉ bí mật bị chặn sớm: sau khi lưu một bảng có token nằm trong `FakeSecureStorage`, nội dung `FakeAppPaths.storedSavedTables()` **không** chứa token và không chứa khoá kiểu `token`/`apiToken`; cấu hình Jira (`config.json`) vẫn không chứa token sau khi lưu bảng
- [X] T016 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableDialogs.kt`: hộp thoại đặt tên bảng dùng `AlertDialog` + `OutlinedTextField` với giá trị gợi ý mặc định `"Bảng " + clock.format(clock.nowMillis())`, hiển thị lỗi từ `TableNameValidation` (rỗng → "Tên bảng không được để trống"; trùng → "Tên bảng đã tồn tại"), nút Xác nhận/Huỷ; huỷ không thay đổi dữ liệu (FR-001, FR-011)
- [X] T017 [US1] Cập nhật `composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt` (thêm `appClock: AppClock`, `savedTableStore: SavedTableStore`, `savedTablesViewModel: SavedTablesViewModel`) và `composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt` (truyền `JvmAppClock()` vào `AppContainer`); giữ nguyên chữ ký các thành viên hiện có
- [X] T018 [US1] Cập nhật `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: gọi `savedTablesViewModel.load()` trong `LaunchedEffect(Unit)` sẵn có; thêm nút "Lưu bảng" vào hàng tiêu đề "Kết quả truy vấn" chỉ **bật** khi `state is QueryState.Success && tickets.isNotEmpty()` kèm dòng giải thích khi bị vô hiệu (đang tải / không có kết quả / lỗi — FR-012); nối hộp thoại ở T016 với `saveTable(...)` truyền `jql`, `tickets`, `visibleColumns` từ `QueryViewModel` và `jiraBaseUrl` từ `ConnectionStatus.Connected`; hiển thị `message` thành công/lỗi (FR-015); **không** đổi bố cục, thứ tự hay hành vi hiện có của chế độ truy vấn (FR-019)
- [ ] T019 [US1] Kiểm chứng thủ công KB-1 trong `specs/003-saved-result-table/quickstart.md` bằng `./gradlew :composeApp:run`: hộp thoại gợi ý tên, huỷ không đổi gì, xác nhận tạo tệp `saved-tables.json` đúng số ticket/trường so với lưới, không chứa token — **chờ người dùng kiểm chứng** (cần phiên desktop và site Jira thật); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1 hoàn tất và kiểm chứng độc lập được (MVP).

---

## Phase 4: User Story 2 (P2) — Xem lại các bảng đã lưu

**Mục tiêu**: xem danh sách bảng đã lưu (tên, số ticket, thời điểm lưu, JQL), mở một bảng và xem dữ liệu dạng bảng có sắp xếp/ẩn hiện cột và mở ticket — tất cả **không** cần kết nối Jira.

**Tiêu chí kiểm thử độc lập**: với hai bảng đã lưu, mở danh sách, mở từng bảng, sắp xếp theo một cột, ẩn một cột (rồi vào lại thấy còn ẩn), mở một ticket — tất cả khi đang ngắt mạng (KB-2).

**FR liên quan**: FR-004, FR-005, FR-006, FR-007, FR-008, FR-017, FR-020

### Kiểm thử cho User Story 2 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T020 [P] [US2] Viết test `composeApp/src/commonTest/kotlin/com/jiratools/domain/TicketSortingTest.kt` cho `sortTickets` theo `data-model.md` mục 5: sắp xếp theo từng cột `KEY`/`SUMMARY`/`STATUS`/`ASSIGNEE`/`PRIORITY`/`UPDATED` cả hai chiều; so sánh **không phân biệt hoa/thường**; `assignee`/`priority` null coi như chuỗi rỗng; **ổn định** (hai ticket bằng nhau giữ nguyên thứ tự đã lưu); `sort.column == null` → trả về danh sách y nguyên (PHẢI thất bại trước T022)
- [X] T021 [P] [US2] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` cho phần **danh mục và xem** (PHẢI thất bại trước T023): `load()` với tệp JSON hỏng → `catalogUnreadable = true`, `tables` rỗng, `loadWarning` chứa `paths.savedTablesFile()`; với tệp sai `schemaVersion` → tương tự; tệp chưa tồn tại → rỗng nhưng **không** cảnh báo; `retryLoad()` sau khi sửa tệp (hoặc xoá tệp) → nạp lại đúng và tắt cảnh báo; `selectTable(id)` đặt `selectedId` và không đọc tệp lại; `toggleColumn(tableId, column)` cập nhật `visibleColumns` của đúng bảng đó, ghi tệp, và **giữ** `ColumnKey.KEY`; `openTicket(tableId, ticketKey)` trả `null` khi mở thành công (qua `RecordingUrlOpener`) và trả thông báo khi `jiraBaseUrl` không hợp lệ (FR-008)
- [X] T022 [US2] Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketSorting.kt` (XANH cho T020): hàm thuần `sortTickets(tickets, sort)` đúng quy tắc đã chốt ở `data-model.md` mục 5, không gọi mạng, không sửa thứ tự gốc của danh sách đầu vào
- [X] T023 [US2] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` (XANH cho T021): `retryLoad()`, `selectTable(id)`, `clearSelection()`, `toggleColumn(tableId, column)` (bỏ qua `ColumnKey.KEY`, ghi qua store), `openTicket(tableId, ticketKey)` (dùng `browseUrl` + `UrlOpener` như `TicketOpener`), giữ chính sách chặn ghi khi `catalogUnreadable`
- [X] T024 [US2] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` theo `contracts/saved-tables.md` mục 4: bố cục hai cột — cột trái danh sách bảng (tên, số ticket, `clock.format(savedAtMillis)`, JQL rút gọn một dòng), cột phải chi tiết bảng đang chọn; lưới dữ liệu **tái dùng** `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` bằng cách ánh xạ sang `QueryState.Success(tickets = sortedTickets, hasMore = false, approximateTotal = null)` với `columns = table.visibleColumns`, `onSortBy` cập nhật trạng thái sắp xếp cục bộ qua `sortTickets`, `onToggleColumn` gọi `toggleColumn`, `onOpenTicket` gọi `openTicket`; hiển thị banner FR-020 (lý do + đường dẫn tệp + nút "Thử lại") khi `catalogUnreadable`; khi chưa chọn bảng thì hiển thị hướng dẫn ngắn
- [X] T025 [US2] Cập nhật `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: thêm bộ chuyển chế độ xem "Truy vấn" / "Bảng đã lưu" ngay dưới vùng kết nối (vùng kết nối vẫn luôn hiển thị ở cả hai chế độ); chế độ mặc định "Truy vấn"; chế độ "Truy vấn" giữ **nguyên** bố cục và hành vi hiện có (chỉ thêm nút ở T018); chế độ "Bảng đã lưu" render `SavedTablesArea`; chuyển qua lại không làm mất kết quả truy vấn đang tải (state nằm trong ViewModel — FR-019)
- [ ] T026 [US2] Kiểm chứng thủ công KB-2 trong `specs/003-saved-result-table/quickstart.md` bằng `./gradlew :composeApp:run`, **trong lúc ngắt mạng**: danh sách hiển thị đủ metadata, mở bảng xem được dữ liệu, sắp xếp/ẩn hiện cột không phát sinh request, ẩn cột rồi vào lại vẫn còn ẩn, mở ticket mở đúng `{jiraBaseUrl}/browse/{key}` — **chờ người dùng kiểm chứng** (cần phiên desktop); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1 và US2 cùng hoạt động độc lập; xem lại bảng đã lưu hoàn chỉnh.

---

## Phase 5: User Story 3 (P3) — Làm mới bảng đã lưu từ Jira

**Mục tiêu**: khi dữ liệu Jira đã đổi, bấm "Làm mới" để nạp lại **toàn bộ** kết quả hiện tại của đúng JQL đã lưu vào chính bảng đó; thất bại thì giữ nguyên dữ liệu cũ.

**Tiêu chí kiểm thử độc lập**: với một bảng đã lưu và kết nối Jira đang hoạt động, bấm "Làm mới" → bảng có ticket mới, không còn ticket rời truy vấn, `refreshedAtMillis` cập nhật, tên/cột giữ nguyên; ngắt mạng rồi bấm lại → dữ liệu cũ còn nguyên kèm thông báo lỗi; bấm "Dừng" giữa đường → không ghi gì (KB-3, KB-4).

**FR liên quan**: FR-021, FR-022, FR-023, FR-024, FR-025, FR-026

### Kiểm thử cho User Story 3 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T027 [P] [US3] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` cho phần **làm mới** (PHẢI thất bại trước T028): khi chưa kết nối (status không `Connected`) → từ chối, **không** request nào tới `MockEngine`, `message` giải thích (FR-023); làm mới thành công qua nhiều trang (MockEngine trả 2 trang với `nextPageToken`/`isLast`) → `tickets` **được thay thế** đúng bằng toàn bộ kết quả mới, `refreshedAtMillis = FakeClock.millis`, `jiraBaseUrl` = địa chỉ đang kết nối, `name`/`visibleColumns` không đổi, ghi tệp **một lần** (FR-021, FR-022, FR-026); lỗi ở trang thứ hai (500/401/429/mạng) → `tickets` và `refreshedAtMillis` giữ nguyên, **không** ghi tệp, `message` là thông báo từ `JiraError.userMessage()` (FR-024); huỷ giữa đường → không ghi tệp, dữ liệu cũ nguyên vẹn, `refreshingId = null` (FR-025); `refreshedCount` cộng dồn theo từng trang trong lúc chạy (FR-025)
- [X] T028 [US3] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` (XANH cho T027): `suspend fun refresh(tableId)` theo `contracts/saved-tables.md` mục 3 — kiểm tra `ConnectionStatus.Connected`, vòng lặp `apiClient.searchJql(jql = table.jql, nextPageToken = …)` tới `isLast`, tăng `refreshedCount` mỗi trang, **chỉ** thay thế dữ liệu + cập nhật `refreshedAtMillis`/`jiraBaseUrl` khi tất cả trang thành công rồi ghi qua store; `fun cancelRefresh()` huỷ coroutine đang chạy (giữ `Job` trong ViewModel); mọi lỗi giữ nguyên dữ liệu cũ và đặt `message`
- [X] T029 [US3] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`: nút "Làm mới" ở phần chi tiết (không bật khi chưa kết nối, kèm dòng giải thích — FR-023), nút "Dừng" khi `refreshingId == table.id`, hiển thị tiến trình `"Đang nạp … N ticket"` từ `refreshedCount` (FR-025), dữ liệu cũ vẫn xem được trong lúc làm mới
- [ ] T030 [US3] Kiểm chứng thủ công KB-3 và KB-4 trong `specs/003-saved-result-table/quickstart.md` bằng `./gradlew :composeApp:run`: làm mới thành công (thay thế, cập nhật thời điểm, ghi một lần), làm mới khi mất mạng (dữ liệu cũ nguyên vẹn), huỷ giữa đường, làm mới khi chưa kết nối — **chờ người dùng kiểm chứng** (cần site Jira thật và phiên desktop); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1, US2, US3 cùng hoạt động độc lập.

---

## Phase 6: User Story 4 (P4) — Quản lý các bảng đã lưu (đổi tên, xoá)

**Mục tiêu**: đổi tên một bảng đã lưu (giữ nguyên dữ liệu và `id`) và xoá bảng không cần nữa với bước xác nhận.

**Tiêu chí kiểm thử độc lập**: đổi tên một bảng → tên mới còn sau khi khởi động lại, dữ liệu ticket không đổi; đổi sang tên trùng/rỗng → báo lỗi, tên cũ giữ nguyên; xoá một bảng có xác nhận → bảng biến mất, các bảng khác và lưới đang xem không bị ảnh hưởng (KB-5).

**FR liên quan**: FR-009, FR-010, FR-011

### Kiểm thử cho User Story 4 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T031 [P] [US4] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` cho phần **đổi tên và xoá** (PHẢI thất bại trước T032): `renameTable(id, newName)` với tên hợp lệ → `name` đổi, `id` **giữ nguyên**, `tickets`/`visibleColumns`/`savedAtMillis` không đổi, ghi tệp; với tên rỗng hoặc trùng bảng khác → trả lỗi tương ứng, **không** ghi tệp, tên cũ giữ nguyên (FR-011); đổi tên chính nó sang chính tên đó (khác hoa/thường) → hợp lệ; `deleteTable(id)` → bảng biến mất khỏi `tables`, ghi tệp, `selectedId = null` nếu đang mở đúng bảng đó, các bảng khác nguyên vẹn; `deleteTable` khi `catalogUnreadable` → từ chối và không ghi
- [X] T032 [US4] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` (XANH cho T031): `suspend fun renameTable(id, newName)` (dùng `validateTableName` với `otherNames` loại trừ chính bảng đó) và `suspend fun deleteTable(id)`, cả hai ghi qua store và đặt `message` theo FR-009/FR-010/FR-011
- [X] T033 [US4] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableDialogs.kt`: hộp thoại **đổi tên** (giá trị mặc định là tên hiện tại, dùng chung thông báo lỗi với hộp thoại đặt tên) và hộp thoại **xác nhận xoá** nêu rõ tên bảng sẽ bị xoá vĩnh viễn với hai nút Huỷ/Xoá (FR-010)
- [X] T034 [US4] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`: mỗi mục trong danh sách có thao tác Đổi tên và Xoá mở hai hộp thoại ở T033; sau khi xoá bảng đang mở thì phần chi tiết quay về hướng dẫn chọn bảng; không thay đổi các phần khác của màn hình
- [ ] T035 [US4] Kiểm chứng thủ công KB-5 trong `specs/003-saved-result-table/quickstart.md` bằng `./gradlew :composeApp:run`: các trường hợp tên rỗng/trùng/hợp lệ, xác nhận xoá, dữ liệu các bảng khác không đổi — **chờ người dùng kiểm chứng** (cần phiên desktop); ghi kết quả vào `quickstart.md`

**Checkpoint**: cả bốn user story hoạt động độc lập và đầy đủ.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Mục tiêu**: chốt cổng chất lượng của hiến pháp, xác nhận không hồi quy và cập nhật tài liệu kiểm chứng.

- [X] T036 Chạy cổng chất lượng đầy đủ tại gốc repo: `./gradlew build ktlintCheck`; xác nhận build Desktop thành công, toàn bộ test (155 test của 001/002 + test mới của 003) đạt, lint sạch (SC-007); ghi kết quả vào mục "Ghi kết quả" của `specs/003-saved-result-table/quickstart.md`
- [X] T037 [P] Rà soát tuân thủ 5 nguyên tắc hiến pháp trên diff cuối cùng: chỉ `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmAppClock.kt`, `JvmAppPaths.kt` và `composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt` chạm API chỉ có trên JVM (`java.time`, `java.io`, `java.nio`); `commonMain` không import `java.*`; `gradle/libs.versions.toml` và `composeApp/build.gradle.kts` không đổi; không có token trong `domain/SavedTable.kt`, `data/SavedTableStore.kt` hay thông báo lỗi; `ui/query/TicketTable.kt` và `ui/query/QueryViewModel.kt` **không** bị sửa (FR-019)
- [ ] T038 [P] Kiểm chứng không hồi quy và dữ liệu hỏng: chạy các kịch bản KB-6 (ghi đè `saved-tables.json` bằng JSON hỏng, xác nhận banner + chặn ghi + "Thử lại" hoạt động, tệp hỏng không bị ghi đè) và KB-7 (lặp lại các kịch bản chính của `../../001-jql-ticket-table/quickstart.md` và `../../002-full-window-scrolling/quickstart.md`, mở bảng ~1.000 ticket và cuộn) — **chờ người dùng kiểm chứng** (cần phiên desktop và site Jira thật); ghi kết quả vào `quickstart.md`
- [X] T039 Cập nhật `specs/003-saved-result-table/quickstart.md` mục "Ghi kết quả": điền trạng thái thực tế cho từng KB và cho các lệnh tự động; ghi lại nếu phải hiệu chỉnh gì so với `research.md`/`contracts/saved-tables.md` (ví dụ định dạng thời gian của `AppClock` hoặc cách hiển thị JQL rút gọn)
- [X] T040 Đối chiếu yêu cầu: rà từng FR-001..FR-026 và SC-001..SC-012 trong `specs/003-saved-result-table/spec.md` với mã đã triển khai (`domain/SavedTable.kt`, `domain/SavedTableName.kt`, `domain/TicketSorting.kt`, `data/SavedTableStore.kt`, `ui/saved/*`, `app/App.kt`, `app/AppContainer.kt`); ghi danh sách FR/SC chỉ có bằng chứng thủ công vào `quickstart.md` để người dùng chốt

---

## Dependencies & Execution Order

### Phụ thuộc giữa các phase

- **Setup (Phase 1)**: không phụ thuộc — bắt đầu ngay (T001, T002)
- **Foundational (Phase 2)**: sau Setup — CHẶN mọi user story (T003–T009; T010 là cổng kiểm chứng)
- **User Story (Phase 3..6)**: sau Foundational; các story có thể làm song song nếu đủ người (xem lưu ý tệp dùng chung)
- **Polish (Phase 7)**: sau khi các user story mong muốn hoàn tất

### Phụ thuộc giữa các user story

- **US1 (P1)**: sau Foundational — không phụ thuộc story khác. Là MVP.
- **US2 (P2)**: sau Foundational; **dùng lại** `domain/SavedTableName.kt` (T013) và `SavedTablesViewModel.kt` (T014) từ US1 -> T023/T024/T025 chạy sau T014; T025 sửa `app/App.kt` sau T018 (cùng tệp)
- **US3 (P3)**: sau Foundational; mở rộng `SavedTablesViewModel.kt` (T028) và `SavedTablesArea.kt` (T029) nên chạy sau T023/T024
- **US4 (P4)**: sau Foundational; mở rộng `SavedTablesViewModel.kt` (T032), `SavedTableDialogs.kt` (T033) và `SavedTablesArea.kt` (T034) nên chạy sau T028/T029

### Trong mỗi user story

- Test viết trước PHẢI ĐỎ rồi mới triển khai (T008 → T009; T011 → T013; T012 → T014; T020 → T022; T021 → T023; T027 → T028; T031 → T032)
- Hàm thuần trước, store/ViewModel sau, giao diện cuối (T013 → T014 → T016 → T017 → T018)
- Kiểm chứng thủ công ngay sau khi story hoàn tất, trước khi chuyển story kế tiếp

### Cơ hội chạy song song

- T002 song song với T001; trong Phase 2: T003, T004, T006, T007 song song (khác tệp); T005 sau T003+T004; T008 trước T009
- Trong US1: T011 và T012 song song (hai tệp test khác nhau); T016 (dialogs) song song được với T013–T015 vì khác tệp
- Trong US2: T020 và T021 song song; T022 song song với T023 (khác tệp)
- Polish: T037 và T038 song song

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Bốn tệp khác nhau, không phụ thuộc nhau:
Task: "T003 tạo composeApp/src/commonMain/kotlin/com/jiratools/platform/AppClock.kt + JvmAppClock.kt"
Task: "T004 mở rộng composeApp/src/commonMain/kotlin/com/jiratools/platform/AppPaths.kt + JvmAppPaths.kt"
Task: "T006 thêm @Serializable vào composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt"
Task: "T007 tạo composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt"

# Sau khi T003+T004 xong:
Task: "T005 cập nhật composeApp/src/commonTest/kotlin/com/jiratools/platform/Fakes.kt"
```

---

## Parallel Example: User Story 1

```bash
# Hai tệp test khác nhau, viết trước:
Task: "T011 composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableNameTest.kt"
Task: "T012 composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt"

# Có thể làm song song với chuỗi T013 -> T014 vì khác tệp:
Task: "T016 composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableDialogs.kt"
```

---

## Implementation Strategy

### MVP trước (chỉ US1)

1. Hoàn tất Phase 1 (Setup)
2. Hoàn tất Phase 2 (Foundational — bắt buộc, chặn mọi story)
3. Hoàn tất Phase 3 (US1): T011 → T019
4. **DỪNG và KIỂM CHỨNG** KB-1 độc lập

### Giao hàng gia tăng

1. Setup + Foundational -> hạ tầng sẵn sàng (seam nền tảng, model, kho dữ liệu)
2. US1 -> kiểm chứng KB-1 -> demo (MVP: lưu bảng riêng đầy đủ dữ liệu)
3. US2 -> kiểm chứng KB-2 -> demo (xem lại, sắp xếp, ẩn/hiện cột, mở ticket không cần Jira)
4. US3 -> kiểm chứng KB-3/KB-4 -> demo (làm mới toàn bộ kết quả, an toàn khi lỗi)
5. US4 -> kiểm chứng KB-5 -> demo (đổi tên, xoá có xác nhận)
6. Polish -> cổng chất lượng, KB-6/KB-7, cập nhật `quickstart.md`
7. Mỗi story tăng giá trị mà không phá story trước (SC-007)

### Chiến lược nhóm song song

1. Nhóm hoàn tất Setup + Foundational cùng nhau
2. Sau Foundational: Dev A -> US1 (T011–T019), Dev B chuẩn bị T020/T021 (US2 tests) song song
3. Sau khi US1 xong: Dev B -> US2 (T022–T026), Dev C -> US3 (T027–T028) sau khi T023 có `refresh` hook, Dev D -> US4 (T031–T033) sau T032 phụ thuộc T028
4. Kiểm chứng thủ công gom theo story, chạy khi có phiên desktop

---

## Notes

- `[P]` = khác tệp, không phụ thuộc task chưa xong
- `[US#]` dùng để truy vết task theo user story trong `spec.md`
- Mọi task đều ghi rõ đường dẫn tệp cụ thể; không có task mơ hồ
- Bốn tệp dùng chung nhiều phase PHẢI sửa tuần tự theo thứ tự đã ghi ở mục "Tệp được sửa ở nhiều phase"
- Không đổi phụ thuộc/phiên bản; không thêm thư viện test; không viết test Compose UI; không sửa `ui/query/TicketTable.kt`, `ui/query/QueryViewModel.kt`, `ui/query/TicketOpener.kt`, `data/JiraApiClient.kt`, `data/ConnectionRepository.kt`, `data/AppConfigStore.kt`
- Ràng buộc cần nhắc nguyên văn khi triển khai: tên bảng `trim().lowercase()` để so trùng và giữ nguyên `KEY` trong `visibleColumns`; `SAVED_TABLES_SCHEMA_VERSION = 1`; thời điểm lưu/làm mới lấy từ `AppClock`; làm mới **chỉ** khi `ConnectionStatus.Connected` và nạp tới `isLast = true`; khi `CatalogLoadResult.Unreadable` thì mọi thao tác ghi bị từ chối và tệp không bị ghi đè
- Sau mỗi task hoặc nhóm task logic: commit (khi có git) và ghi kết quả kiểm chứng vào `quickstart.md`
