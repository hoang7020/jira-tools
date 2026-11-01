# Data Model: Truy vấn Jira bằng JQL và hiển thị kết quả dạng bảng

**Feature**: `001-jql-ticket-table` | **Ngày**: 2026-10-30 | **Nguồn**: [spec.md](./spec.md)

Tài liệu này mô hình hóa dữ liệu và trạng thái cho tính năng. Không chứa chi tiết API của Jira (xem [contracts/jira-cloud-api.md](./contracts/jira-cloud-api.md)).

## Sơ đồ quan hệ

```text
UserPreferences 1 -- 1 JiraConnection (baseUrl + email; token nằm trong SecureStorage)
         |
         +-- visibleColumns: Set<ColumnKey>
         +-- lastJql: String?
         +-- sort: SortState?
JQL Query (chuỗi nhập) --> QueryState --> QueryPage (tickets) 
```

## Thực thể

### 1. JiraConnection (Cấu hình kết nối)

Cấu hình truy cập một instance Jira Cloud. Một người dùng có đúng một cấu hình.

| Trường | Kiểu | Bắt buộc | Quy tắc |
|--------|------|---------|-------|
| baseUrl | String | Có | Chuẩn hóa: trim, thêm `https://` nếu thiếu scheme, bỏ dấu `/` cuối; phải phân giải được host |
| email | String | Có | Không rỗng, định dạng email; là email tài khoản Atlassian |
| token | (không lưu trong model) | Có | API token chỉ tồn tại trong `SecureStorage` với khóa `jira-tools:api-token`; không serialize ra file/log (FR-003) |
| lastValidatedAt | Instant? | Không | Thời điểm kiểm tra kết nối thành công gần nhất |

**Trạng thái kết nối** (`ConnectionStatus`, sealed interface):

- `Unconfigured` - chưa có baseUrl/email/token.
- `Validating` - đang kiểm tra (FR-001).
- `Connected(validatedAt: Instant)` - kiểm tra thành công; cấu hình được lưu.
- `Invalid(reason: ConnectionError)` - lỗi phân biệt được nguyên nhân (US1 scenario 3); cấu hình sai KHÔNG được lưu.

Luồng chuyển: `Unconfigured -> Validating -> Connected | Invalid`; từ `Invalid` có thể sửa và thử lại.

### 2. Ticket

Một issue trả về từ Jira. Chỉ đọc.

| Trường | Kiểu | Ghi chú |
|--------|------|---------|
| key | String | Định danh duy nhất hiển thị (ví dụ `PROJ-123`); dùng làm khóa bảng |
| summary | String | Tiêu đề; có thể dài, UI cắt hiển thị |
| status | String | Tên trạng thái (ví dụ `In Progress`) |
| assignee | String? | Null khi ticket chưa gán; hiển thị ô trống (edge case) |
| priority | String? | Null khi không có; hiển thị ô trống |
| updated | Instant | Thời điểm cập nhật gần nhất; hiển thị theo định dạng ngày giờ địa phương |
| url | String | Dẫn xuất: `{baseUrl}/browse/{key}` (FR-013) |

So sánh/hợp nhất theo `key`.

### 3. QueryPage (trang kết quả)

Kết quả của một lần gọi tìm kiếm (một trang).

| Trường | Kiểu | Ghi chú |
|--------|------|---------|
| tickets | List<Ticket> | Các dòng của trang hiện tại |
| nextPageToken | String? | Null = hết kết quả; dùng để tải trang tiếp (FR-007) |
| approximateTotal | Long? | Tổng xấp xỉ từ endpoint đếm; có thể null nếu đếm lỗi (không chặn kết quả) |

### 4. ColumnKey (cột hiển thị)

Enum cố định, thứ tự hiển thị cố định:

`KEY`, `SUMMARY`, `STATUS`, `ASSIGNEE`, `PRIORITY`, `UPDATED`

- Nhãn hiển thị tiếng Anh theo thuật ngữ Jira: `Key`, `Summary`, `Status`, `Assignee`, `Priority`, `Updated`.
- Mặc định: tất cả 6 cột hiển thị (FR-006).
- Trường tương ứng trong JQL để sắp xếp: `key`, `summary`, `status`, `assignee`, `priority`, `updated`.

### 5. SortState (sắp xếp)

| Trường | Kiểu | Quy tắc |
|--------|------|-------|
| column | ColumnKey | Cột đang sắp xếp |
| direction | ASC / DESC | Chiều; chỉ báo trên header bảng (US3) |

Sắp xếp được thực hiện phía Jira bằng mệnh đề `ORDER BY` của JQL (thay thế `ORDER BY` hiện có nếu có) để đảm bảo đúng thứ tự trên toàn bộ kết quả, không chỉ trang đang tải.

### 6. UserPreferences (tùy chọn người dùng)

Lưu trong file JSON cấu hình (không bí mật).

| Trường | Kiểu | Mặc định | Liên kết |
|--------|------|---------|---------|
| schemaVersion | Int | 1 | Cho nâng cấp định dạng file sau này |
| connection.baseUrl | String? | null | FR-001 |
| connection.email | String? | null | FR-001 |
| visibleColumns | Set<ColumnKey> | tất cả | FR-011, FR-012 |
| lastJql | String? | null | FR-014 |
| sort | SortState? | null | US3 |

Nếu file hỏng/không đọc được: coi như mặc định, không làm sập ứng dụng.

### 7. QueryState (trạng thái truy vấn)

`sealed interface` phơi từ `QueryViewModel` qua `StateFlow`:

- `Idle(lastJql: String?)` - chưa chạy; ô nhập khôi phục JQL gần nhất (FR-014).
- `Loading(jql, loadedTickets)` - đang tải (trang đầu hoặc trang tiếp); UI hiển thị spinner, không chặn thao tác khác (FR-010).
- `Success(jql, tickets, approximateTotal, hasMore)` - có kết quả; `hasMore` = còn `nextPageToken`.
- `Empty(jql)` - 0 kết quả, kèm truy vấn đã nhập (FR-008).
- `Error(jql, error: QueryError, previousTickets?)` - lỗi; giữ nguyên JQL để chỉnh sửa (FR-009).

### 8. Lỗi (sealed)

`ConnectionError` (US1) và `QueryError` (US2) chia sẻ các loại phân biệt được theo SC-004:

| Loại | Nguyên nhân | Thông điệp hướng người dùng |
|-----|----------|------------------------|
| `InvalidJql(message)` | JQL sai cú pháp (HTTP 400) | "JQL không hợp lệ: {chi tiết từ Jira}" |
| `AuthFailed` | Sai/thu hồi API token hoặc email (401) | "Xác thực thất bại - kiểm tra email và API token" |
| `PermissionDenied` | Thiếu quyền tìm kiếm (403) | "Tài khoản không có quyền thực hiện truy vấn này" |
| `RateLimited(retryAfterSeconds?)` | Vượt giới hạn tần suất (429) | "Jira tạm thời giới hạn yêu cầu - thử lại sau {n} giây" |
| `InstanceNotFound(host)` | Tên miền không phân giải được (địa chỉ sai) | "Không tìm thấy địa chỉ {host} - kiểm tra lại địa chỉ site" |
| `NetworkUnreachable` | Mất mạng/không kết nối được | "Không kết nối được tới Jira - kiểm tra kết nối mạng" |
| `Timeout` | Jira phản hồi chậm (edge case) | "Jira phản hồi quá lâu - thử lại" |
| `Unexpected(status, detail)` | Lỗi khác | "Lỗi không mong đợi (mã {status})" |

Ghi chú: `InstanceNotFound` và `NetworkUnreachable` tách riêng vì SC-004 yêu cầu phân biệt được "địa chỉ sai" với "mạng gián đoạn". `PermissionDenied` giữ lại dù 403 không được tài liệu hóa riêng cho endpoint tìm kiếm. Ánh xạ HTTP/ngoại lệ chi tiết xem `contracts/jira-cloud-api.md`.

## Ràng buộc xuyên thực thể

- Mọi trạng thái UI được suy ra từ các model trên, không lưu trữ riêng trong Compose (Nguyên tắc III).
- Chỉ `UserPreferences` và `JiraConnection` (không gồm token) được serialize xuống đĩa; token chỉ trong `SecureStorage`.
- Tất cả model trong `commonMain`, thuần Kotlin, không phụ thuộc JVM (Nguyên tắc I).
