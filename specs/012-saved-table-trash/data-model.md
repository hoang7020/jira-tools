# Data Model: Thùng rác bảng đã lưu

## SavedTable

Record bảng đã lưu hiện hành; khi bị chuyển vào thùng rác được giữ nguyên bên trong `TrashedSavedTable`.

- `id`: định danh ổn định; giữ nguyên khi move/restore.
- `name`: tên hiển thị và tên dùng để phát hiện xung đột khi restore.
- `jql`, `jiraBaseUrl`, `savedAtMillis`, `refreshedAtMillis`, `visibleColumns`, `tickets`, `jiraTimeZone`: giữ nguyên qua move/restore.
- `burndownSnapshots`: giữ toàn bộ lịch sử gắn với bảng.

## TrashedSavedTable

Đại diện cho một bảng đang bị xoá mềm và chưa khôi phục hoặc xoá vĩnh viễn.

| Field | Type | Constraints |
|---|---|---|
| table | SavedTable | Bắt buộc; giữ nguyên payload của bảng tại thời điểm chuyển vào thùng rác. |
| deletedAtMillis | Long | Bắt buộc; thời điểm người dùng xác nhận chuyển bảng vào thùng rác, dùng để sắp xếp mới nhất trước. |

Một bảng ID không đồng thời xuất hiện trong `tables` và `trashedTables`. Danh tính trong thùng rác được xác định bởi `table.id`.

## SavedTableCatalog

- `schemaVersion`: tăng lên 5.
- `tables`: danh sách bảng hoạt động; không bao gồm mục thùng rác.
- `trashedTables`: danh sách `TrashedSavedTable`; mặc định rỗng với dữ liệu schema cũ.
- `dynamicStartTimes`: ledger dùng chung theo Jira site/issue, không thuộc riêng một bảng; không thay đổi khi xoá vĩnh viễn một bảng.

## Lifecycle transitions

| Current state | Action | Next state | Data effect |
|---|---|---|---|
| Active | Move to trash | Trashed | Chuyển nguyên `SavedTable` sang một `TrashedSavedTable`, ghi `deletedAtMillis`; commit cùng catalog write. |
| Trashed | Restore, name unique | Active | Chuyển nguyên `SavedTable` về `tables`; bỏ wrapper trash. |
| Trashed | Restore, name conflicts | Trashed until a valid new name is supplied | Không ghi catalog trước khi tên thay thế hợp lệ; sau đó chỉ cập nhật `table.name` khi restore. |
| Trashed | Permanent delete confirmed | Deleted | Loại bỏ wrapper và toàn bộ payload bảng, gồm ticket/burndown; không tự xoá ledger dùng chung. |
| Trashed | Cancel permanent delete | Trashed | Không thay đổi catalog. |

Tất cả chuyển trạng thái commit nguyên tử trong một lần ghi catalog. Khi write thất bại, dữ liệu đã lưu và StateFlow không đổi; hiển thị thông báo lỗi.

## Name validation on restore

- Trim khoảng trắng đầu/cuối như các tên bảng hiện hành.
- Tên rỗng không hợp lệ.
- So sánh không phân biệt hoa/thường với các bảng đang hoạt động.
- Tên đang dùng bởi một bảng khác trong thùng rác không cản restore; chỉ bảng hoạt động cần tên duy nhất.
- Không tạo ID mới khi khôi phục.

## Persistence migration

- Nâng `SAVED_TABLES_SCHEMA_VERSION` từ 4 lên 5.
- Schema v1/v2/v3/v4 đọc được tiếp tục giữ nguyên các migration cột/dynamic start time hiện có; thêm `trashedTables = []`, sau đó nâng catalog lên v5 trong bộ nhớ.
- Schema v5 đọc/ghi danh sách thùng rác như thường lệ.
- Load migration không ghi file; lần save catalog tiếp theo persist schema v5 theo quy ước hiện tại.
