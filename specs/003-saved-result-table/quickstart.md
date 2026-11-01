# Hướng dẫn kiểm chứng nhanh: Bảng kết quả đã lưu

**Feature**: `003-saved-result-table` | **Ngày**: 2026-10-31 | **Đặc tả**: [spec.md](./spec.md) |
**Dữ liệu**: [data-model.md](./data-model.md) | **Hợp đồng**: [contracts/saved-tables.md](./contracts/saved-tables.md)

Mục đích: các kịch bản chạy được dưới đây chứng minh tính năng hoạt động đầu-cuối và không làm hỏng
001/002. Chi tiết trường dữ liệu xem `data-model.md`; chi tiết ràng buộc xem hợp đồng — không lặp lại ở
đây.

## 0. Chuẩn bị

```bash
cd /Users/ryanze/Sources/jira-tools
./gradlew --version          # Gradle wrapper, JDK 17+
```

- Ứng dụng desktop Compose (KMP), chạy bằng `./gradlew :composeApp:run` (JVM).
- Thư mục cấu hình theo nền tảng (dùng chung cho `config.json` và `saved-tables.json`):
  macOS `~/Library/Application Support/jira-tools/`, Windows `%APPDATA%\jira-tools\`, Linux
  `$XDG_CONFIG_HOME/jira-tools/` (mặc định `~/.config/jira-tools/`). Cùng tên tệp `saved-tables.json`.
- Trước khi thử các kịch bản phá huỷ dữ liệu (KB-6), sao lưu `saved-tables.json` nếu đã có dữ liệu thật.

### Lệnh tự động (dùng xuyên suốt)

| Mục đích | Lệnh |
|----------|------|
| Kiểm thử hàm thuần (tên bảng, sắp xếp) | `./gradlew :composeApp:jvmTest --tests "com.jiratools.domain.SavedTableNameTest" --tests "com.jiratools.domain.TicketSortingTest"` |
| Kiểm thử kho dữ liệu bảng đã lưu | `./gradlew :composeApp:jvmTest --tests "com.jiratools.data.SavedTableStoreTest"` |
| Kiểm thử bí mật không rò rỉ | `./gradlew :composeApp:jvmTest --tests "com.jiratools.data.SavedTableSecretTest"` |
| Kiểm thử trạng thái giao diện (lưu, đổi tên, xoá, làm mới, tiến trình) | `./gradlew :composeApp:jvmTest --tests "com.jiratools.ui.saved.SavedTablesViewModelTest"` |
| Toàn bộ kiểm thử + lint | `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` |
| Chạy ứng dụng | `./gradlew :composeApp:run` |

Không có kiểm thử Compose UI (đồng nhất với 001/002): hành vi bố cục và tương tác được kiểm chứng thủ
công bằng các kịch bản KB-1..KB-7 dưới đây.

## KB-1 — Lưu bảng từ kết quả truy vấn (US1)

**Điều kiện**: đã kết nối Jira; truy vấn trả về ít nhất một ticket (ví dụ JQL `project = ABC ORDER BY
created DESC`).

**Thực hiện**: ở chế độ "Truy vấn", bấm "Lưu bảng", để nguyên tên gợi ý (hoặc nhập tên khác), xác nhận.

**Kỳ vọng**:

- Hộp thoại hiện tên gợi ý dạng `Bảng dd/MM/yyyy HH:mm`; huỷ thì không có gì thay đổi.
- Sau khi xác nhận: thông báo lưu thành công kèm tên bảng.
- `saved-tables.json` xuất hiện, chứa đúng một bảng với `jql`, `name`, `visibleColumns`, `jiraBaseUrl`,
  `savedAtMillis`, `refreshedAtMillis = null`, và danh sách `tickets` **bằng đúng** số ticket đang thấy
  trên lưới.
- Không có token/mật khẩu trong tệp (kiểm tra bằng KB-7 hoặc `SavedTableSecretTest`).

**Kiểm chứng tự động**: `SavedTableStoreTest` (lưu từ kết quả rỗng bị chặn; lưu đúng số ticket).

## KB-2 — Chuyển sang "Bảng đã lưu" và xem dữ liệu (US2, US4)

**Điều kiện**: có ít nhất hai bảng đã lưu (làm KB-1 hai lần với JQL khác nhau).

**Thực hiện**: chuyển bộ chuyển chế độ sang "Bảng đã lưu"; chọn lần lượt từng bảng ở danh sách bên trái.

**Kỳ vọng**:

- Vùng kết nối Jira vẫn hiển thị; nút "Lưu bảng" của chế độ truy vấn không còn.
- Mỗi mục danh sách hiện tên, số ticket, thời điểm lưu, JQL rút gọn.
- Chi tiết bảng hiện tên, JQL, thời điểm lưu, số ticket và lưới dữ liệu **không** gọi Jira.
- Cuộn dọc/ngang hoạt động; cột Mã cố định khi cuộn ngang; bấm tiêu đề cột để sắp xếp (dữ liệu đã lưu,
  không phát sinh mạng); ẩn/hiện cột qua menu cột như 001.
- Bấm một hàng mở trình duyệt đúng `{jiraBaseUrl}/browse/{key}`.
- Ẩn một cột, quay lại chế độ "Truy vấn" rồi vào lại "Bảng đã lưu": cột vẫn ẩn (đã ghi xuống tệp).

**Kiểm chứng tự động**: `SavedTablesViewModelTest` (ánh xạ dữ liệu đã lưu sang lưới, đếm hàng, đổi `visibleColumns` và ghi lại);
`SavedTableStoreTest` (lưu/đọc vòng `visibleColumns`).

## KB-3 — Làm mới một bảng đã lưu (US3, kịch bản thành công)

**Điều kiện**: đang kết nối Jira; bảng đã lưu có JQL hợp lệ.

**Thực hiện**: mở bảng, bấm "Làm mới"; trong lúc chạy thử bấm "Dừng" ở lần thử thứ hai (xem KB-4).

**Kỳ vọng**:

- Trạng thái đang tải hiện trên bảng; số ticket đếm được tăng dần khi nhiều trang.
- Kết thúc: danh sách ticket **được thay thế** bằng kết quả mới (không nhân đôi, không còn ticket đã bị
  xoá ở Jira), `refreshedAtMillis` được cập nhật và hiển thị, `jiraBaseUrl` khớp địa chỉ đang kết nối,
  `name`/`visibleColumns` giữ nguyên.
- Tệp `saved-tables.json` được ghi lại đúng một lần với dữ liệu mới.
- Bảng truy vấn đang xem (chế độ "Truy vấn") và các bảng khác không đổi.

**Kiểm chứng tự động**: `SavedTablesViewModelTest` (thay thế toàn bộ danh sách, giữ tên/cột, cập nhật `refreshedAtMillis` và `jiraBaseUrl`).

## KB-4 — Làm mới thất bại và huỷ (US3)

**Thực hiện**:

1. Ngắt mạng (hoặc dùng token hết hạn / JQL sai) rồi bấm "Làm mới".
2. Khôi phục mạng, bấm "Làm mới" với một bảng lớn rồi bấm "Dừng" giữa đường.
3. Thử "Làm mới" khi chưa kết nối (đăng xuất).

**Kỳ vọng**:

- (1) Thông báo lỗi dễ hiểu (từ `JiraError.userMessage()` hiện có); dữ liệu đã lưu **không** đổi;
  `refreshedAtMillis` giữ nguyên; không có lần ghi tệp nào.
- (2) Sau khi dừng: thông báo nói rõ đã dừng; dữ liệu cũ còn nguyên; không ghi tệp.
- (3) Không phát sinh request; giao diện giải thích cần kết nối trước.

**Kiểm chứng tự động**: `SavedTablesViewModelTest` (lỗi giữ nguyên dữ liệu và không ghi tệp; huỷ cũng không ghi).

## KB-5 — Đổi tên và xoá (US4)

**Thực hiện**: đổi tên một bảng thành tên rỗng; thành tên trùng bảng khác; thành tên hợp lệ. Sau đó xoá
một bảng (bấm Huỷ trong hộp thoại xác nhận, rồi bấm Xoá thật).

**Kỳ vọng**:

- Tên rỗng → lỗi "Tên bảng không được để trống", không lưu.
- Tên trùng → lỗi "Tên bảng đã tồn tại", không lưu.
- Tên hợp lệ → danh sách cập nhật ngay; `id` giữ nguyên; dữ liệu ticket không đổi.
- Xoá + Huỷ → bảng còn nguyên.
- Xoá + Xác nhận → bảng biến mất khỏi danh sách và khỏi tệp; các bảng khác còn nguyên; chọn bảng khác
  thì chi tiết chuyển theo (không còn tham chiếu bảng đã xoá).

**Kiểm chứng tự động**: `SavedTableStoreTest` (kiểm tra tên, đổi tên giữ `id`, xoá).

## KB-6 — Dữ liệu bảng đã lưu bị hỏng (US2, FR-020)

**Thực hiện**:

1. Đóng ứng dụng. Ghi đè `saved-tables.json` bằng nội dung hỏng, ví dụ `{"schemaVersion":1,"tables":[`
   (JSON dở dang). Mở ứng dụng, vào "Bảng đã lưu".
2. Thử lưu một bảng mới từ chế độ "Truy vấn".
3. Sửa lại tệp hợp lệ (hoặc xoá tệp), bấm "Thử lại" trong banner.

**Kỳ vọng**:

- (1) Banner cảnh báo "không đọc được dữ liệu bảng đã lưu" kèm đường dẫn tệp và nút "Thử lại"; danh sách
  trống; **không** có thông báo sai kiểu "chưa có bảng nào".
- (2) Thao tác lưu bị từ chối với lý do rõ ràng; tệp hỏng **không** bị ghi đè (kiểm tra bằng `cat`).
- (3) Sau khi sửa/xoá và bấm "Thử lại": nếu xoá tệp → trạng thái rỗng bình thường (không cảnh báo), lưu
  được; nếu sửa hợp lệ → danh sách hiện lại.
- Khởi động lại ứng dụng sau khi xoá tệp vẫn cho phép lưu bình thường.

**Kiểm chứng tự động**: `SavedTableStoreTest` (JSON hỏng → `Unreadable`; không ghi đè; `schemaVersion`
khác → `Unreadable`).

## KB-7 — Không hồi quy 001/002 và hiệu năng

**Thực hiện**: chạy toàn bộ kiểm thử + lint; sau đó chạy ứng dụng và lặp lại các kịch bản chính của
`../../001-jql-ticket-table/quickstart.md` và `../../002-full-window-scrolling/quickstart.md`.

**Kỳ vọng**:

- `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` xanh, gồm cả kiểm thử cũ của 001/002.
- Chế độ "Truy vấn" giữ nguyên hành vi 001: kết nối, truy vấn, phân trang, mở ticket, cài đặt.
- Hành vi 002 giữ nguyên: vùng điều khiển cố định, danh sách cuộn, thu gọn khi cửa sổ thấp, bố cục
  không nhảy khi kết quả thay đổi; chỉ thêm một nút "Lưu bảng".
- Mở một bảng có ~1.000 ticket đã lưu và cuộn: không giật rõ rệt (SC-004); không có request mạng khi
  sắp xếp/ẩn cột.
- Chuyển qua lại giữa hai chế độ không mất dữ liệu đang tải ở chế độ truy vấn (kết quả và vị trí cuộn
  còn nguyên khi quay lại — xem giả định "giữ trạng thái" trong spec).

**Kiểm chứng tự động**: toàn bộ `jvmTest` + `ktlintCheck`; `SavedTablesViewModelTest` chạy trên danh sách lớn giả lập.

## Ghi kết quả

### Lệnh tự động đã chạy (agent, 2026-10-31)

| Lệnh | Kết quả |
|------|---------|
| `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` (baseline trước khi sửa mã) | 155 test đạt, 0 lỗi, lint sạch |
| `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` (sau mỗi phase) | 164 → 180 → 200 → 205 → 211 test đạt, lint sạch |
| `./gradlew build ktlintCheck --rerun-tasks` (cổng chất lượng cuối) | BUILD SUCCESSFUL, 23/23 task chạy lại, **211 test đạt**, 0 lỗi, lint sạch (SC-007) |
| `./gradlew :composeApp:jvmTest --tests "com.jiratools.domain.SavedTableNameTest" --tests "com.jiratools.domain.TicketSortingTest"` | 16 test đạt (6 + 10) |
| `./gradlew :composeApp:jvmTest --tests "com.jiratools.data.SavedTableStoreTest"` | 9 test đạt |
| `./gradlew :composeApp:jvmTest --tests "com.jiratools.data.SavedTableSecretTest"` | 2 test đạt |
| `./gradlew :composeApp:jvmTest --tests "com.jiratools.ui.saved.SavedTablesViewModelTest"` | 30 test đạt |

### Truy vết yêu cầu → kiểm chứng tự động

| Yêu cầu | Kiểm chứng tự động |
|--------|-------------------|
| FR-001, FR-002, FR-003, FR-013, FR-018 | `SavedTablesViewModelTest.saveTableStoresEverythingFromQueryResultWithoutNetworkCalls` (không request nào tới `MockEngine`), `saveTableKeepsEveryTicketFieldOfTheQueryTable`, `saveTableWritesCatalogOncePerSave` |
| FR-004 | `SavedTableStoreTest.saveThenLoadKeepsEveryFieldAndSchemaVersion` (ghi rồi đọc lại giữ nguyên mọi trường và `schemaVersion`) |
| FR-005 | `SavedTablesArea` hiển thị tên, số ticket, thời gian và JQL — cần KB-2 để xác nhận hiển thị |
| FR-006 | `selectingATableOpensItWithoutReadingTheFileAgain` (chỉ dùng dữ liệu trong bộ nhớ, không đọc lại đĩa) |
| FR-007 | `TicketSortingTest` (10 test: sáu cột, hai chiều, không phân biệt hoa/thường, null coi như rỗng, ổn định, không đổi danh sách gốc) và `togglingASortColumnFlipsDirectionAndStartsNewColumnsAscending` |
| FR-008 | `openTicketUsesTheBaseUrlSavedWithTheTable`, `openTicketExplainsWhenTheSavedBaseUrlIsUnusable` |
| FR-009 | `renameTableKeepsIdentityAndData`, `renameTableAcceptsItsOwnNameInAnyCase` |
| FR-010 | `deleteTableRemovesOnlyThatTable`, `deleteTableKeepsTheSelectionOfAnotherTable` |
| FR-011 | `SavedTableNameTest` (6 test), `saveTableRejectsBlankNameWithoutWriting`, `saveTableRejectsDuplicateNameIgnoringCaseWithoutWriting`, `renameTableRejectsBlankAndDuplicateNamesWithoutWriting` |
| FR-015 | `saveTableMessageMentionsTableName`, `writeFailureIsReportedWithoutChangingTheList`, `SavedTableStoreTest.savePropagatesWriteFailure` |
| FR-016, SC-008 | `SavedTableSecretTest` (2 test) |
| FR-017 | `togglingAColumnUpdatesOnlyItsOwnTable`, `theKeyColumnCannotBeHidden` |
| FR-019, SC-007 | 155 test của 001/002 vẫn đạt trong tổng số 211 test; `ui/query/TicketTable.kt` và `ui/query/QueryViewModel.kt` không bị sửa |
| FR-020 | `loadWarnsWhenTheSavedFileIsUnreadable`, `loadWarnsWhenSchemaVersionIsUnknown`, `saveTableRefusesWhenCatalogIsUnreadable`, `renameAndDeleteAreRefusedWhenTheCatalogIsUnreadable`, `retryLoadPicksUpARepairedFileAndClearsTheWarning` và `SavedTableStoreTest.unreadableFileIsNeitherDeletedNorOverwritten` |
| FR-021, FR-022, FR-026 | `refreshReplacesEveryTicketFromAllPages` (hai trang, thay thế toàn bộ, ghi một lần, cập nhật thời điểm và địa chỉ Jira) |
| FR-023 | `refreshIsRefusedWithoutAConnection` (không request nào tới `MockEngine`) |
| FR-024, SC-012 | `refreshKeepsOldDataWhenAPageFails` (giữ nguyên `tickets`/`refreshedAtMillis`, không ghi file, thông báo theo `JiraError.userMessage()`) |
| FR-025 | `refreshReportsProgressWhileLoadingPages`, `cancellingARefreshKeepsTheStoredDataAndWritesNothing` |
| SC-005 | Mọi test của `SavedTablesViewModelTest` chạy không có kết nối Jira thật; mở bảng chỉ dùng dữ liệu trong bộ nhớ |

### Cần người dùng kiểm chứng

Không thể xác minh trong môi trường không có giao diện và không có site Jira thật:

| Yêu cầu | Kịch bản |
|--------|---------|
| FR-001, FR-004, FR-012, FR-013, FR-015, SC-001, SC-002, SC-003: lưu bảng và mở lại sau khi khởi động lại | KB-1 |
| FR-005, FR-006, FR-007, FR-008, SC-005 | KB-2 |
| FR-021, FR-022, FR-026, SC-010, SC-011 | KB-3 |
| FR-023, FR-024, FR-025, SC-012 | KB-4 |
| FR-009, FR-010, FR-011 | KB-5 |
| FR-020, SC-009 | KB-6 |
| FR-014, FR-019, SC-004, SC-007 | KB-7 |
| SC-006 (khảo sát người dùng lần đầu) | Ngoài phạm vi kiểm chứng tự động |

| Kịch bản | Kết quả | Ghi chú |
|----------|---------|---------|
| KB-1 Lưu bảng | [ ] | chờ người dùng kiểm chứng |
| KB-2 Xem bảng đã lưu | [ ] | chờ người dùng kiểm chứng |
| KB-3 Làm mới thành công | [ ] | chờ người dùng kiểm chứng |
| KB-4 Làm mới thất bại / huỷ | [ ] | chờ người dùng kiểm chứng |
| KB-5 Đổi tên / xoá | [ ] | chờ người dùng kiểm chứng |
| KB-6 Dữ liệu hỏng | [ ] | chờ người dùng kiểm chứng |
| KB-7 Không hồi quy + hiệu năng | [ ] | chờ người dùng kiểm chứng |

### Hiệu chỉnh so với `research.md` / `contracts/saved-tables.md`

Ghi nhận trong quá trình triển khai (hành vi cam kết không đổi):

1. `validateTableName(raw, otherNames)` chỉ nhận hai tham số: bên gọi loại trừ tên của chính bảng đang
   đổi (`research.md` mục 8 phác thảo ba tham số `name, existingNames, currentId`). Cách làm này giữ hàm
   thuần đơn giản hơn mà vẫn đúng quy tắc "đổi tên chính bảng đó không bị coi là trùng".
2. Trạng thái kết nối được đẩy vào `SavedTablesViewModel` bằng `onConnectionStatusChanged(status)` (gọi
   từ `LaunchedEffect(status)` trong `App.kt`) thay vì inject một hàm cung cấp trạng thái: `ConnectionViewModel`
   được tạo trong `App.kt` chứ không nằm trong `AppContainer`, nên đây là cách ít thay đổi cấu trúc 001/002 nhất.
3. `cancelRefresh()` huỷ chính coroutine mà tầng giao diện dùng để gọi `refresh(...)` (ViewModel giữ `Job`
   của lời gọi hiện tại) — không cần thêm `CoroutineScope` riêng cho ViewModel.
4. `toggleColumn` ghi file nhưng **không** đặt `message` (thao tác im lặng thành công); hàm ghi dùng chung
   nhận thông báo có thể `null`.
5. Thêm hai hàm trợ giúp nhỏ ngoài danh sách task: `TableNameValidation.errorMessage()` (một nguồn câu chữ
   cho lỗi tên bảng) và `SortState.toggledBy(column)` (quy tắc đảo chiều khi bấm tiêu đề cột, có test).
6. `FakeAppPaths` có thêm bộ đếm `savedTablesReads`/`savedTablesWrites` để kiểm chứng "ghi đúng một lần"
   và "không đọc lại đĩa" (chỉ dùng trong kiểm thử).
