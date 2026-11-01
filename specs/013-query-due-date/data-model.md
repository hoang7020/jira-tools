# Mô hình dữ liệu: Cột Due Date

## Jira Fields DTO

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `FieldsDto.dueDate` | `String?` | Ánh xạ JSON name `duedate`; null hoặc thiếu nghĩa là issue không có Due Date. |

## Ticket

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `Ticket.dueDate` | `String?` | Ngày lịch Jira, giữ nguyên chuỗi `yyyy-MM-dd`; không chuyển múi giờ. Trường có default null để tương thích các ticket JSON đã lưu trước đây. |

Ticket là thành phần của kết quả query và snapshot SavedTable. Không đổi các trường `dynamicDueDate`, `dynamicDueAtMillis`, `realDueDate`, hoặc `realDueAtMillis`.

## ColumnKey

| Thành phần | Thay đổi |
|---|---|
| `DUE_DATE` | Nhãn hiển thị “Due Date”; cột query và saved-table; thêm vào các bộ cột mặc định mới. |
| `UserPreferences.visibleColumns` | Nâng schema config 3 lên 4; sau migration schema cũ, nếu cột trùng bộ mặc định schema 3 thì thêm `DUE_DATE`; cấu hình đã tuỳ chỉnh được giữ nguyên. |
| `SavedTable.visibleColumns` | Bảng mới dùng mặc định có `DUE_DATE`; lựa chọn của bảng hiện hữu không bị migration thay đổi. |

## Quy tắc dữ liệu

- Mapping bảo toàn ngày lịch Jira, không suy diễn ngày từ trường khác.
- Giá trị null/thiếu hiển thị ô trống.
- Sắp xếp parse giá trị ngày thành ngày lịch để so sánh thời gian; null/giá trị không hợp lệ không trở thành ngày giả.
- Ẩn cột không xoá `Ticket.dueDate` khỏi snapshot.
