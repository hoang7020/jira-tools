# Mô hình dữ liệu (Phase 1)

**Feature**: `003-saved-result-table` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này chốt các thực thể, ràng buộc và chuyển trạng thái mà tính năng "bảng riêng lưu cục bộ" thêm
vào. Mọi model đều nằm trong `commonMain` (Nguyên tắc I) và dùng `kotlinx.serialization` (Nguyên tắc IV).

## 1. Bảng đã lưu (Saved Table)

```kotlin
@Serializable
data class SavedTable(
    val id: String,                                        // định danh, sinh một lần khi lưu
    val name: String,                                      // tên hiển thị, đã trim (FR-001, FR-009)
    val jql: String,                                       // truy vấn gốc (FR-003)
    val jiraBaseUrl: String,                               // địa chỉ dùng khi lưu hoặc làm mới gần nhất (FR-003, FR-026)
    val savedAtMillis: Long,                               // thời điểm lưu (FR-005)
    val refreshedAtMillis: Long? = null,                   // thời điểm làm mới gần nhất (FR-026)
    val visibleColumns: Set<ColumnKey> = ColumnKey.all(),  // lựa chọn cột của riêng bảng (FR-017)
    val tickets: List<Ticket> = emptyList(),               // toàn bộ ticket đã lưu (FR-002, FR-013, FR-021)
)
```

| Trường | Ràng buộc | Nguồn |
|-------|-----------|-------|
| `id` | Không rỗng, duy nhất trong danh mục; **không đổi** khi đổi tên; sinh bằng hàm thuần `newTableId(nowMillis, existingIds)` (thêm hậu tố `-2`, `-3`, … khi trùng) | FR-009 |
| `name` | Không rỗng sau `trim()`; không trùng (so sánh không phân biệt hoa/thường trên chuỗi đã trim) với bảng khác | FR-011 |
| `jql` | Giữ nguyên truy vấn đã chạy; có thể rỗng về mặt kỹ thuật nhưng trên luồng thực tế luôn khác rỗng vì nút lưu chỉ bật khi có kết quả | FR-003, FR-012 |
| `jiraBaseUrl` | Chuỗi địa chỉ đã dùng; có thể rỗng nếu chưa cấu hình — khi đó mở ticket phải báo lỗi thay vì im lặng | FR-008, FR-026 |
| `savedAtMillis` | Thời điểm tạo bảng, lấy từ `AppClock.nowMillis()` | FR-005 |
| `refreshedAtMillis` | Chỉ được đặt sau khi làm mới **thành công**; giữ nguyên khi làm mới thất bại | FR-024, FR-026 |
| `visibleColumns` | Tập con khác rỗng của `ColumnKey`; luôn chứa `KEY` (cột Mã không tắt được) | FR-017 |
| `tickets` | Đúng bằng số ticket của bảng truy vấn tại thời điểm lưu (FR-013), hoặc toàn bộ kết quả khớp truy vấn sau khi làm mới (FR-022); mỗi ticket giữ đủ sáu trường | FR-002, FR-013, FR-021, FR-022 |

**Bất biến**:

- Bảng đã lưu **không** chứa token, mật khẩu hay bất kỳ trường bí mật nào (FR-016, SC-008).
- Bảng đã lưu không tham chiếu sống tới Jira: nội dung chỉ đổi qua thao tác "Lưu bảng" (tạo bảng mới)
  hoặc "Làm mới" của chính nó (FR-014, FR-021).

## 2. Danh mục bảng đã lưu (Saved Table Catalog)

```kotlin
const val SAVED_TABLES_SCHEMA_VERSION: Int = 1

@Serializable
data class SavedTableCatalog(
    val schemaVersion: Int = SAVED_TABLES_SCHEMA_VERSION,
    val tables: List<SavedTable> = emptyList(),
)
```

- Danh mục là **một** thực thể duy nhất tương ứng với tệp `saved-tables.json` trong thư mục cấu hình nền
  tảng (mục 1 `research.md`).
- `schemaVersion` khác `SAVED_TABLES_SCHEMA_VERSION` → coi như **không đọc được** (FR-020), không cố diễn
  giải dữ liệu cũ.
- Thứ tự `tables` là thứ tự hiển thị mặc định trong danh sách; danh sách được hiển thị theo thứ tự này
  (mới lưu nằm ở cuối) trừ khi người dùng chọn sắp xếp khác ở tầng giao diện (ngoài phạm vi phiên bản này).

## 3. Kết quả đọc kho (Catalog load result)

```kotlin
sealed interface CatalogLoadResult {
    /** Đọc thành công và đúng phiên bản lược đồ. */
    data class Loaded(val catalog: SavedTableCatalog) : CatalogLoadResult

    /** Chưa có tệp dữ liệu — trạng thái bình thường của người dùng mới. */
    data object Empty : CatalogLoadResult

    /** Tệp có nhưng không đọc được (JSON hỏng, sai kiểu, khác phiên bản lược đồ, lỗi đọc). */
    data class Unreadable(val detail: String?) : CatalogLoadResult
}
```

Phân biệt `Empty` với `Unreadable` là điều kiện để FR-020 hoạt động: chỉ `Unreadable` mới sinh cảnh báo
và chặn ghi.

## 4. Tên bảng: chuẩn hoá và kiểm tra (FR-011)

```kotlin
fun normalizeTableName(raw: String): String                                   // trim hai đầu

sealed interface TableNameValidation {
    data object Valid : TableNameValidation
    data object Empty : TableNameValidation
    data object Duplicate : TableNameValidation
}

fun validateTableName(raw: String, otherNames: List<String>): TableNameValidation
```

| Đầu vào | Kết quả |
|--------|--------|
| `"   "` hoặc `""` | `Empty` |
| `"Báo cáo"` khi chưa có bảng nào tên tương đương | `Valid` |
| `"báo CÁO"` khi đã có `"Báo cáo"` (khác hoa/thường, khác khoảng trắng thừa) | `Duplicate` |
| Khi đổi tên chính bảng đó | `otherNames` **không** chứa tên hiện tại của bảng đang đổi → `Valid` |

## 5. Sắp xếp dữ liệu đã lưu (FR-007)

```kotlin
fun sortTickets(tickets: List<Ticket>, sort: SortState): List<Ticket>
```

| Quy tắc | Chi tiết |
|--------|---------|
| Trường so sánh | `KEY`→`key`, `SUMMARY`→`summary`, `STATUS`→`status`, `ASSIGNEE`→`assignee`, `PRIORITY`→`priority`, `UPDATED`→`updated` |
| So sánh | Chuỗi, **không phân biệt hoa/thường**; `assignee`/`priority` null được coi là chuỗi rỗng |
| Chiều | `sort.ascending = true` tăng dần, `false` giảm dần |
| Ổn định | Giữ nguyên thứ tự đã lưu khi hai phần tử bằng nhau |
| Không có cột | `sort.column == null` → trả về danh sách y nguyên |
| Không gọi Jira | Hàm thuần, chạy trong bộ nhớ; kết quả không ghi xuống tệp |

## 6. Trạng thái giao diện (Saved Tables UI state)

```kotlin
data class SavedTablesUiState(
    val tables: List<SavedTable> = emptyList(),   // đã nạp trong bộ nhớ
    val selectedId: String? = null,               // bảng đang mở ở cột chi tiết
    val catalogUnreadable: Boolean = false,       // FR-020: chặn ghi cho tới khi đọc lại được
    val loadWarning: String? = null,              // thông báo kèm đường dẫn tệp
    val refreshingId: String? = null,             // bảng đang được làm mới
    val refreshedCount: Int = 0,                  // số ticket đã nạp trong lần làm mới hiện tại (FR-025)
    val message: String? = null,                  // kết quả thao tác gần nhất (FR-015, FR-011, FR-024)
)
```

**Chuyển trạng thái**:

| Hành động | Chuyển trạng thái | Ràng buộc |
|----------|------------------|-----------|
| Mở ứng dụng / "Thử lại" | `Empty`/`Loaded` → `tables` tương ứng, `catalogUnreadable = false`; `Unreadable` → `tables` rỗng, `catalogUnreadable = true`, `loadWarning` kèm đường dẫn tệp | FR-004, FR-020 |
| Lưu bảng (US1) | Thêm `SavedTable` mới vào cuối danh mục, ghi tệp, `message = "Đã lưu bảng <tên>"` | FR-001, FR-013, FR-015, FR-018 |
| Mở bảng | `selectedId = id` (chi tiết hiển thị dữ liệu trong bộ nhớ, không đọc tệp, không gọi Jira) | FR-006 |
| Đổi tên | Cập nhật `name` của bảng, ghi tệp, `selectedId` **không đổi** | FR-009 |
| Xoá (sau xác nhận) | Bỏ bảng khỏi danh mục; nếu bảng đang mở bị xoá thì `selectedId = null`; ghi tệp | FR-010 |
| Làm mới (bắt đầu) | `refreshingId = id`, `refreshedCount = 0` | FR-023, FR-025 |
| Làm mới (mỗi trang) | `refreshedCount += số ticket của trang` | FR-025 |
| Làm mới (thành công) | `tickets` thay thế toàn bộ, `refreshedAtMillis`/`jiraBaseUrl` cập nhật, `refreshingId = null`, ghi tệp, `message` thành công | FR-021, FR-022, FR-026 |
| Làm mới (thất bại hoặc bị huỷ) | Không ghi tệp, `tickets`/`refreshedAtMillis` giữ nguyên, `refreshingId = null`, `message` là lỗi tương ứng hoặc "đã dừng" | FR-024, FR-025 |
| Bật/tắt cột của bảng | Cập nhật `visibleColumns` của bảng đó, ghi tệp | FR-017 |

## 7. Dịch vụ nền tảng (bổ sung)

| Dịch vụ | Thành viên | Mục đích |
|--------|-----------|---------|
| `AppPaths` (mở rộng) | `savedTablesFile(): String`, `readSavedTables(): String?`, `writeSavedTables(content: String)` | Vị trí và vào/ra tệp danh mục, giữ `commonMain` không phụ thuộc hệ tệp (Nguyên tắc I) |
| `AppClock` (mới) | `nowMillis(): Long`, `format(millis: Long): String` | Thời điểm lưu/làm mới và hiển thị cho người dùng (FR-005, FR-026) |

## 8. Truy vết yêu cầu chức năng -> thực thể

| Yêu cầu | Thực thể / quy tắc liên quan |
|---------|------------------------------|
| FR-001, FR-002, FR-003, FR-013, FR-014, FR-018 | `SavedTable` (tạo mới từ bảng truy vấn), `SavedTableStore.save` |
| FR-004, FR-005, FR-006 | `SavedTableCatalog`, `CatalogLoadResult`, `SavedTablesUiState.tables` |
| FR-007 | `sortTickets`, `SavedTable.visibleColumns` |
| FR-008 | `SavedTable.jiraBaseUrl` + `browseUrl` (đã có) |
| FR-009, FR-010, FR-011 | `SavedTable.id`/`name`, `validateTableName`, `newTableId` |
| FR-012 | `SavedTablesUiState` + trạng thái `QueryState` của bảng truy vấn ở tầng giao diện |
| FR-015 | `SavedTablesUiState.message` |
| FR-016 | `SavedTable` không có trường bí mật; test `SavedTableSecretTest` |
| FR-017 | `SavedTable.visibleColumns` |
| FR-019 | Không đổi model/logic của 001 và 002 |
| FR-020 | `CatalogLoadResult.Unreadable`, `SavedTablesUiState.catalogUnreadable`/`loadWarning` |
| FR-021..FR-026 | Trường `tickets`, `refreshedAtMillis`, `jiraBaseUrl` + chuyển trạng thái làm mới ở mục 6 |

## 9. Không thay đổi

- `UserPreferences`/`config.json`: giữ nguyên (không thêm khoá nào cho tính năng này).
- `Ticket` giữ nguyên các trường; chỉ **thêm** `@Serializable` để dùng lại cho dữ liệu lưu.
- `QueryState`, `ColumnKey`, `SortState`, `JiraError`, `ConnectionStatus`: giữ nguyên định nghĩa.
