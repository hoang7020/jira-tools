# Hợp đồng bảng đã lưu (Saved Tables)

**Feature**: `003-saved-result-table` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này chốt bốn giao diện mà tính năng cam kết: (1) kho dữ liệu cục bộ, (2) dịch vụ nền tảng bổ
sung, (3) hợp đồng làm mới từ Jira, (4) hợp đồng giao diện. Hợp đồng Jira REST của 001 được **dùng lại
nguyên vẹn**, không sửa (`../../001-jql-ticket-table/contracts/jira-cloud-api.md`).

## 1. Kho dữ liệu cục bộ (`saved-tables.json`)

**Vị trí**: cùng thư mục cấu hình nền tảng với `config.json`, tên tệp `saved-tables.json`, truy cập qua
`AppPaths.savedTablesFile()`.

**Định dạng**: JSON của `SavedTableCatalog` (`schemaVersion` + danh sách `SavedTable`), ghi bằng
`kotlinx.serialization` với `prettyPrint = true`, `encodeDefaults = true`,
`ignoreUnknownKeys = true` — cùng cấu hình với `AppConfigStore`.

**Cam kết**:

- **Ghi nguyên tử**: `AppPaths.writeSavedTables` phải ghi tệp tạm rồi thay thế (`ATOMIC_MOVE`), để tệp cũ
  không bị hỏng khi thao tác thất bại giữa đường (FR-004, tính toàn vẹn của dữ liệu người dùng).
- **Một điểm ghi duy nhất**: mọi thay đổi danh mục (lưu mới, đổi tên, xoá, làm mới, đổi cột) đều ghi qua
  `SavedTableStore.save(...)`; không nơi nào khác được ghi vào tệp này.
- **Không chứa bí mật**: tệp chỉ chứa các trường trong `SavedTable`; token/mật khẩu chỉ nằm trong
  `SecureStorage` (FR-016, SC-008). Có test hồi quy riêng (`SavedTableSecretTest`).
- **Phiên bản lược đồ**: `schemaVersion` khác hằng số hiện hành → coi như không đọc được (không cố suy
  diễn dữ liệu cũ); tăng hằng số khi cấu trúc thay đổi không tương thích.
- **Không đọc được** (JSON hỏng, sai kiểu, khác phiên bản, lỗi đọc): trả
  `CatalogLoadResult.Unreadable(detail)`; **không** tự xoá, **không** ghi đè.
- **Trạng thái `Unreadable` chặn mọi thao tác ghi** cho tới khi `load()` trả về `Loaded`/`Empty`
  (FR-020): nút "Lưu bảng", đổi tên, xoá, ghi kết quả làm mới, đổi cột đều báo lỗi kèm lý do thay vì ghi.
- **Tệp chưa tồn tại**: `CatalogLoadResult.Empty` — trạng thái bình thường, không cảnh báo, ghi được.
- **Một tiến trình**: ứng dụng desktop đơn người dùng; không cần khoá tệp giữa nhiều tiến trình (ngoài
  phạm vi phiên bản này).

**Bảng chân trị đọc/ghi**:

| Trạng thái đọc | Danh sách hiển thị | Ghi được? | Thông báo |
|---------------|-------------------|----------|-----------|
| `Empty` (chưa có tệp) | Trống (không cảnh báo) | Có | Không |
| `Loaded` | Toàn bộ bảng trong danh mục | Có | Không |
| `Unreadable` | Trống | **Không** | Cảnh báo + đường dẫn tệp + nút "Thử lại" |

## 2. Dịch vụ nền tảng bổ sung

```kotlin
// commonMain/kotlin/com/jiratools/platform/AppPaths.kt (mở rộng interface đã có)
interface AppPaths {
    // … configFile/readConfig/writeConfig giữ nguyên …
    fun savedTablesFile(): String
    suspend fun readSavedTables(): String?
    suspend fun writeSavedTables(content: String)
}

// commonMain/kotlin/com/jiratools/platform/AppClock.kt (mới)
interface AppClock {
    fun nowMillis(): Long
    fun format(millis: Long): String   // "dd/MM/yyyy HH:mm" theo múi giờ hệ thống
}
```

**Cam kết**:

- `savedTablesFile()` trả đường dẫn tuyệt đối; `readSavedTables()` trả `null` khi tệp chưa tồn tại và
  **ném ngoại lệ** khi tệp có nhưng không đọc được (để tầng trên phân biệt `Empty` với `Unreadable`).
- `writeSavedTables()` tạo thư mục cha nếu cần và ghi nguyên tử; **ném ngoại lệ** khi ghi thất bại (hết
  dung lượng, không có quyền) để giao diện báo cho người dùng (FR-015).
- `AppClock.nowMillis()` là nguồn thời gian duy nhất cho `savedAtMillis`/`refreshedAtMillis`;
  `format(millis)` là nơi duy nhất định dạng thời gian để hiển thị.
- Cả hai đều là seam nền tảng (Nguyên tắc I): `commonMain` chỉ biết interface; `jvmMain` cài đặt
  (`JvmAppPaths`, `JvmAppClock`); kiểm thử dùng `FakeAppPaths`, `FakeClock`.
- Bổ sung này **không** thay đổi `SecureStorage`, `UrlOpener`, `ioDispatcher`, `AppPaths.{configFile,
  readConfig, writeConfig}` (hợp đồng của 001 giữ nguyên).

## 3. Hợp đồng làm mới (US3)

**Điều kiện tiên quyết**: `ConnectionStatus.Connected` (FR-023). Khi chưa kết nối, thao tác không thực
hiện được và giao diện giải thích ngắn gọn; **không** phát sinh request nào.

**Trình tự**:

1. Đặt `refreshingId = id`, `refreshedCount = 0`, hiển thị trạng thái đang tải (FR-025).
2. Gọi `JiraApiClient.searchJql(jql = table.jql, nextPageToken = token)` lặp cho tới `isLast = true`
   (FR-022); sau mỗi trang cộng dồn vào `refreshedCount`.
3. **Chỉ khi toàn bộ trang thành công**: thay thế `tickets` của bảng bằng kết quả mới (không nối thêm,
   không giữ ticket cũ — FR-021), đặt `refreshedAtMillis = clock.nowMillis()` và `jiraBaseUrl` = địa chỉ
   đang kết nối (FR-026), giữ nguyên `name` và `visibleColumns`, rồi ghi danh mục xuống tệp.
4. Thất bại ở bất kỳ bước nào (mạng, xác thực, JQL không hợp lệ, giới hạn tần suất, ghi tệp): **không**
   thay đổi `tickets`/`refreshedAtMillis`; `message` là thông báo từ `JiraError.userMessage()` hiện có
   hoặc lý do ghi tệp (FR-024).
5. Người dùng có thể huỷ giữa đường: huỷ coroutine của thao tác; kết quả bằng thất bại (không ghi gì),
   `message` nói rõ đã dừng (FR-025).

**Bất biến**:

- Số ticket sau khi làm mới **không** phụ thuộc số ticket trước đó (có thể tăng hoặc giảm).
- Không có trạng thái nửa vời: danh mục trên đĩa chỉ đổi sau khi có kết quả đầy đủ.
- Làm mới **không** ảnh hưởng các bảng khác và **không** ảnh hưởng bảng truy vấn đang xem.

## 4. Hợp đồng giao diện

**Bộ chuyển chế độ xem**: ngay dưới vùng kết nối, hai chế độ "Truy vấn" và "Bảng đã lưu"; chế độ mặc
định là "Truy vấn". Vùng kết nối (biểu mẫu Jira) vẫn luôn hiển thị ở cả hai chế độ.

> **Diễn giải yêu cầu FR-004 của 002**: yêu cầu đó nói vùng điều khiển không được trôi đi khi người dùng
> **cuộn danh sách ticket**; việc người dùng chủ động chuyển sang chế độ xem khác không vi phạm, miễn là
> quay lại chế độ "Truy vấn" thì bố cục và hành vi của 002 còn nguyên (FR-019, SC-007).

**Chế độ "Truy vấn"** (giữ nguyên 002, thêm đúng một nút):

- Nút "Lưu bảng" nằm trong hàng tiêu đề "Kết quả truy vấn"; chỉ **bật** khi `QueryState.Success` và
  `tickets` không rỗng (FR-012); khi bị vô hiệu, có dòng giải thích ngắn (đang tải / không có kết quả /
  lỗi / chưa kết nối).
- Bấm nút mở hộp thoại đặt tên: mặc định gợi ý `"Bảng " + clock.format(clock.nowMillis())`; xác nhận thì
  lưu, huỷ thì không thay đổi gì (FR-001, FR-015).
- Khi danh mục đang `Unreadable`, nút "Lưu bảng" báo lỗi giải thích thay vì ghi (mục 1).

**Chế độ "Bảng đã lưu"**:

- Cột trái: danh sách bảng đã lưu, mỗi mục hiển thị tên, số ticket, thời điểm lưu và JQL rút gọn
  (FR-005); nút "+ hiện tại" không cần. Mỗi mục có thao tác Mở, Đổi tên, Xoá.
- Cột phải: chi tiết bảng đang chọn — tên, JQL, thời điểm lưu/làm mới gần nhất, số ticket, nút "Làm
  mới" (kèm "Dừng" khi đang chạy) và lưới dữ liệu.
- Lưới dữ liệu **tái dùng `TicketTable`** bằng cách ánh xạ bảng đã lưu sang
  `QueryState.Success(tickets = table.tickets, hasMore = false, approximateTotal = null)`, với
  `columns = table.visibleColumns`; nhờ đó có cuộn dọc/ngang, cột Mã cố định, sắp xếp theo cột và ẩn/hiện
  cột (FR-007, FR-017) mà không sửa thành phần dùng chung.
- Sắp xếp và ẩn/hiện cột thực hiện trên dữ liệu đã lưu, **không** phát sinh truy vấn tới Jira (FR-007);
  ẩn/hiện cột được ghi lại theo từng bảng (FR-017), còn trạng thái sắp xếp chỉ trong phiên.
- Bấm một hàng ticket mở `browseUrl(table.jiraBaseUrl, ticket.key)` bằng `UrlOpener`; khi địa chỉ không
  hợp lệ, hiển thị thông báo thay vì im lặng (FR-008).
- Khi `catalogUnreadable`: hiển thị banner cảnh báo kèm đường dẫn tệp và nút "Thử lại"; danh sách trống;
  mọi thao tác ghi bị chặn (FR-020).
- Xoá bảng luôn có hộp thoại xác nhận trước khi thực hiện (FR-010).
- Đổi tên dùng hộp thoại có kiểm tra `validateTableName`: rỗng → báo "Tên bảng không được để trống";
  trùng → báo "Tên bảng đã tồn tại"; hợp lệ thì lưu và giữ nguyên `id` (FR-009, FR-011).

## 5. Không thay đổi

- Hợp đồng Jira REST, `JiraErrorMapper`, `ConnectionRepository`, `AppConfigStore`, `UserPreferences`:
  giữ nguyên (001).
- `TicketTable`, `QueryViewModel`, `TicketOpener`, bố cục và quy tắc thu gọn của 002: giữ nguyên (FR-019).
- `gradle/libs.versions.toml` và `composeApp/build.gradle.kts`: không đổi — tính năng chỉ dùng thư viện đã
  có (Nguyên tắc IV).
