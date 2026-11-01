# Mô hình dữ liệu (Phase 1)

**Feature**: `004-jira-connection-settings` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này chốt trạng thái, chuyển trạng thái và dữ liệu mà tính năng "Cài đặt kết nối + tái sử dụng
token" thêm vào. Mọi model nằm trong `commonMain` (Nguyên tắc I); phần chạm hệ điều hành vẫn đi qua
`SecureStorage`/`AppPaths` đã có.

## 1. Trạng thái kết nối (bổ sung một trạng thái)

```kotlin
sealed interface ConnectionStatus {
    /** Chưa đủ thông tin để gọi Jira: chưa có gì, thiếu địa chỉ/email, hoặc thiếu token. */
    data object Unconfigured : ConnectionStatus

    /** Đang kiểm tra thông tin với Jira (người dùng bấm "Kiểm tra kết nối" hoặc kiểm tra ngầm khi mở). */
    data object Validating : ConnectionStatus

    /** MỚI: đã có thông tin kết nối trên máy nhưng chưa xác minh được (mới mở, hoặc lỗi mạng). */
    data class Unverified(val baseUrl: String, val email: String) : ConnectionStatus

    /** Đã xác minh thành công với Jira. */
    data class Connected(val baseUrl: String, val email: String) : ConnectionStatus

    /** Lần kiểm tra gần nhất bị Jira từ chối; `error` đã được thân thiện hoá và không chứa bí mật. */
    data class Invalid(val error: JiraError) : ConnectionStatus
}
```

| Trạng thái | Nghĩa với người dùng | Có gọi Jira được? |
|-----------|---------------------|-------------------|
| `Unconfigured` | Chưa cấu hình (hoặc vừa ngắt kết nối) | Không — giao diện hướng dẫn mở Cài đặt (FR-011) |
| `Validating` | Đang kiểm tra | Không cần thiết — chỉ là trạng thái tạm |
| `Unverified` | Đã có thông tin trên máy, chưa xác minh được | **Có** — lời gọi sẽ tự báo lỗi nếu thông tin sai (FR-022) |
| `Connected` | Đã kết nối | Có |
| `Invalid` | Kết nối bị Jira từ chối kèm lý do | Vẫn hiển thị khu vực làm việc, nhưng thông báo nổi bật yêu cầu nhập lại token trong Cài đặt (FR-012) |

**Bất biến**: trạng thái **không** lưu trữ token và không bao giờ chứa token trong câu chữ. `Unverified`
và `Connected` cùng mang `baseUrl`/`email` (dùng cho mở ticket, làm mới bảng đã lưu) — dòng trạng thái
trên màn hình chính **không** hiển thị hai trường này (FR-019).

## 2. Thông tin kết nối đã lưu (đọc từ hai nguồn)

```kotlin
/** Ảnh chụp thông tin kết nối đang có trên máy, dùng để điền sẵn và hiển thị chỉ báo token (FR-008, FR-009). */
data class SavedConnectionInfo(
    val baseUrl: String = "",
    val email: String = "",
    val tokenSaved: Boolean = false,
) {
    val hasSavedConnection: Boolean
        get() = baseUrl.isNotBlank() || email.isNotBlank() || tokenSaved
}
```

| Trường | Nguồn | Ràng buộc |
|-------|-------|-----------|
| `baseUrl` | `config.json` → `UserPreferences.baseUrl` | Đã cắt khoảng trắng; rỗng nghĩa là chưa lưu |
| `email` | `config.json` → `UserPreferences.email` | Đã cắt khoảng trắng; rỗng nghĩa là chưa lưu |
| `tokenSaved` | `SecureStorage.loadToken()?.isNotBlank() == true` | **Không** trả về giá trị token ra khỏi tầng dữ liệu |
| `hasSavedConnection` | Địa chỉ, email hoặc token còn tồn tại | Giá trị suy ra; giữ nút ngắt kết nối khả dụng cả khi token mồ côi |

`SavedConnectionInfo` là dữ liệu **chỉ để đọc**: không có luồng nào ghi token từ đây trở lại kho bí mật.

## 3. Bản nháp nhập liệu (chuyển vào ViewModel)

```kotlin
/** Nội dung người dùng đang nhập; chỉ tồn tại trong bộ nhớ, không bao giờ ghi ra đĩa. */
data class ConnectionDraft(
    val baseUrl: String = "",
    val email: String = "",
    val token: String = "",
    val tokenVisible: Boolean = false,
)
```

Giữ nguyên định nghĩa hiện có (đã có từ 002) và **chuyển nhà**: khai báo hiện nằm trong
`ui/connection/ConnectionArea.kt` (tệp bị xoá ở tính năng này) nên được chuyển sang
`ui/connection/ConnectionForm.kt`. Chủ sở hữu state đổi từ `remember` trong `App.kt` sang
`ConnectionViewModel` để có thể điền sẵn từ `SavedConnectionInfo` khi mở Cài đặt.

## 4. Trạng thái màn hình (ConnectionViewModel)

```kotlin
val status: StateFlow<ConnectionStatus>   // giữ nguyên từ 001
val draft: StateFlow<ConnectionDraft>     // MỚI: nội dung đang nhập, điền sẵn từ thông tin đã lưu
val hasSavedConnection: StateFlow<Boolean> // MỚI: còn địa chỉ/email/token cần xoá
val tokenSaved: StateFlow<Boolean?>       // MỚI: null = chưa biết, true/false = đã/chưa có token
val notice: StateFlow<String?>            // MỚI: thông báo thao tác ("Đã ngắt kết nối.")
```

| Hành động | Chuyển trạng thái | Ràng buộc |
|----------|------------------|-----------|
| Mở ứng dụng | `loadSaved()`: có địa chỉ + email → `Unverified`; thiếu → `Unconfigured`; sau đó `verifySaved()` tinh chỉnh | Chỉ đọc tệp cấu hình ở bước đầu, không đọc kho bí mật (FR-017) |
| Kiểm tra ngầm: chấp nhận | `Validating` → `Connected`, `tokenSaved = true` | Chỉ đọc, không ghi đĩa (FR-013) |
| Kiểm tra ngầm: thiếu token | `Unconfigured`, `tokenSaved = false` | Giữ nguyên địa chỉ/email đã lưu |
| Kiểm tra ngầm: thiếu cấu hình nhưng còn token | `Unconfigured`, `tokenSaved = true`, `hasSavedConnection = true` | Cho phép người dùng xoá token mồ côi |
| Kiểm tra ngầm: lỗi mạng | `Unverified` (giữ nguyên), `tokenSaved = true` | Không chặn truy vấn (FR-022) |
| Kiểm tra ngầm: Jira từ chối | `Invalid(error)`, `tokenSaved = true` | Thông báo kèm hướng dẫn mở Cài đặt (FR-012) |
| Mở Cài đặt | `draft` được điền `baseUrl`/`email` từ `SavedConnectionInfo`; ô token để rỗng | FR-009 |
| Người dùng sửa draft | `draft` cập nhật; không gọi mạng | — |
| Người dùng bấm "Kiểm tra kết nối": thành công | `Validating` → `Connected`; `tokenSaved = true`; draft nhận địa chỉ/email đã chuẩn hoá; token trong draft được xoá khỏi bộ nhớ | FR-005, FR-016 |
| "Kiểm tra kết nối" thất bại | `Validating` → `Invalid(error)`; **không** ghi gì | FR-013 |
| Ngắt kết nối (sau xác nhận) | Xoá token + địa chỉ/email → `Unconfigured`; `hasSavedConnection = false`; `tokenSaved = false`; draft rỗng; `notice` báo đã ngắt | FR-010, FR-011 |

## 5. Dữ liệu lưu trên máy (phạm vi thay đổi)

| Nơi lưu | Khoá | Thay đổi ở tính năng này |
|--------|------|------------------------|
| `config.json` | `baseUrl`, `email` | Bị ghi khi kết nối thành công (như 001); **bị xoá** khi ngắt kết nối |
| `config.json` | `lastJql`, `visibleColumns`, `sort`, `schemaVersion` | **Không đổi** — ngắt kết nối không xoá tuỳ chọn truy vấn |
| Kho bí mật hệ điều hành | `jira-tools:api-token` | Bị ghi khi kết nối thành công (như 001); **bị xoá** khi ngắt kết nối |
| `saved-tables.json` | toàn bộ danh mục bảng đã lưu | **Không đổi** — bảng đã lưu giữ địa chỉ Jira riêng |

## 6. Câu chữ trạng thái (hàm thuần, dùng chung)

```kotlin
/** Câu chữ ngắn cho dòng trạng thái trên màn hình chính (FR-019). */
fun connectionStatusText(status: ConnectionStatus): String

/** Câu chữ đầy đủ hơn cho màn hình Cài đặt, kèm gợi ý hành động khi cần (FR-004, FR-012). */
fun connectionStatusDetail(status: ConnectionStatus): String
```

| Trạng thái | `connectionStatusText` (màn hình chính, một dòng) | `connectionStatusDetail` (Cài đặt) |
|-----------|--------------------------------------------------|-----------------------------------|
| `Unconfigured` | "Chưa cấu hình kết nối Jira." | "Nhập địa chỉ Jira, email và API token để bắt đầu." |
| `Validating` | "Đang kiểm tra kết nối tới Jira…" | "Đang kiểm tra kết nối tới Jira…" |
| `Unverified` | "Chưa xác minh được kết nối Jira — kiểm tra mạng." | "Đã có thông tin kết nối trên máy nhưng chưa xác minh được. Bạn vẫn có thể chạy truy vấn." |
| `Connected` | "Đã kết nối Jira." | "Kết nối thành công tới <địa chỉ Jira> bằng tài khoản <email>." |
| `Invalid` | "Kết nối Jira bị từ chối — mở Cài đặt." | Thông báo từ `JiraError.userMessage()` kèm gợi ý mở Cài đặt để nhập lại token |

Hai hàm này là **hàm thuần** và được kiểm thử trước (Nguyên tắc II); chúng không bao giờ in token.

## 7. Truy vết yêu cầu chức năng -> thực thể

| Yêu cầu | Thực thể / quy tắc liên quan |
|---------|------------------------------|
| FR-001, FR-018 | Chế độ xem "Cài đặt" trong `app/App.kt` (không có thực thể dữ liệu mới) |
| FR-002 | Việc xoá `ConnectionArea` khỏi màn hình chính |
| FR-003, FR-019 | `ConnectionStatus` + `connectionStatusText` |
| FR-004, FR-016 | `ConnectionDraft` + `ConnectionRepository.validate` (giữ nguyên) |
| FR-005, FR-006, FR-007, FR-021 | `SavedConnectionInfo`, khoá `config.json`, mục token trong kho bí mật |
| FR-008, FR-009 | `SavedConnectionInfo.tokenSaved`, `SavedConnectionInfo.baseUrl`/`email` điền vào `ConnectionDraft` |
| FR-010, FR-011 | `ConnectionRepository.disconnect()`, bảng "Dữ liệu lưu trên máy" (mục 5) |
| FR-012 | `ConnectionStatus.Invalid` + `connectionStatusDetail` |
| FR-013 | Bất biến "kiểm tra chỉ đọc" ở mục 1 và mục 4 |
| FR-014, FR-015 | `StateFlow` của `ConnectionViewModel` (trạng thái cập nhật trong phiên, không phụ thuộc màn hình) |
| FR-017, FR-022 | `Unverified` + luồng `loadSaved()` → `verifySaved()` |
| FR-020 | Mục 5: dữ liệu bảng đã lưu và tuỳ chọn truy vấn không đổi |

## 8. Không thay đổi

- `UserPreferences` (trừ việc `baseUrl`/`email` có thể bị đặt lại `null` khi ngắt kết nối), `Ticket`,
  `QueryState`, `ColumnKey`, `SortState`, `JiraError`, `RateLimited`, `SavedTable`, `SavedTableCatalog`.
- `SecureStorage`, `AppPaths`, `AppConfigStore`, `JiraApiClient`, `JiraErrorMapper`, `TicketOpener`,
  `TicketTable`, `QueryViewModel`.
- Định dạng `saved-tables.json` và mọi hợp đồng của 003, ngoài điều kiện cho phép làm mới.
