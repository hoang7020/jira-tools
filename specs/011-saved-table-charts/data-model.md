# Data Model: Đồ thị bảng đã lưu

## SavedTable

Bản chụp kết quả Jira hiện được lưu cục bộ.

- `id`: định danh ổn định; snapshot gắn với bảng này.
- `tickets`: trạng thái hiện hành dùng cho pie, column và line.
- `burndownSnapshots`: danh sách lịch sử chỉ tăng thêm, theo thứ tự ghi nhận.
- Rename không đổi định danh hoặc snapshot; delete xoá bảng và toàn bộ snapshot.

## BurndownSnapshot

Một phép đo tổng hợp được ghi nhận khi bảng mới được lưu thành công hoặc được làm mới thành công.

| Field | Type | Constraints |
|---|---|---|
| recordedAtMillis | Long | Thời điểm ghi nhận từ clock; không null. |
| ticketCount | Int | Tổng số ticket trong bản chụp; >= 0. |
| remainingCount | Int | Số ticket không có status category `done`; 0..ticketCount. |
| unknownCategoryCount | Int | Số ticket thiếu/không nhận dạng category, là tập con của remaining; 0..remainingCount. |

### Quy tắc vòng đời

- Lưu bảng mới thành công tạo đúng một baseline snapshot trong cùng lần ghi catalog.
- Mỗi refresh thành công tạo đúng một snapshot, sau khi mọi trang Jira được lấy đủ và bảng/catalog được ghi thành công.
- Refresh bị huỷ, tải thất bại hoặc ghi catalog lỗi không tạo snapshot và không thay thế dữ liệu cũ.
- Snapshot được giữ vô thời hạn cho tới khi bảng bị xoá.
- Bảng/schema cũ được đọc với `burndownSnapshots` rỗng; không dựng ngược dữ liệu lịch sử.
- Nhiều thao tác có thể có cùng timestamp nếu clock trả cùng mili giây; thứ tự append được giữ trong danh sách.

## ChartProjection

Projection bất biến, tính từ một `SavedTable` và lựa chọn hiện tại; không được lưu lâu dài.

### CategoryChart

- `field`: `STATUS`, `ASSIGNEE` hoặc `PRIORITY`.
- `slices`: category label và count.
- Category null/rỗng được trình bày là “Chưa xác định”.
- Tổng count của mọi slice bằng số ticket của bảng hiện hành.

### UpdatedTicketsByDay

- `points`: ngày và số ticket có `updatedAtMillis` thuộc ngày đó.
- Ticket thiếu thời gian cập nhật không có điểm ngày; tổng thiếu được hiển thị bằng chú thích.
- Điểm được sắp tăng theo ngày.

### BurndownSeries

- `points`: timestamp, remaining count, tổng ticket và unknown-category count.
- Snapshot được render theo thứ tự ghi nhận.
- Ticket category `done` không được tính là remaining; category thiếu/không nhận dạng được có tính vào remaining và được phân biệt bằng chú thích.

## Persistence migration

- Tăng `SAVED_TABLES_SCHEMA_VERSION` từ 3 lên 4.
- Đọc schema v1/v2 tiếp tục áp dụng migration cột hiện hữu; v1/v2/v3 đều tạo `burndownSnapshots = []` và nâng lên v4 trong bộ nhớ.
- Schema v4 đọc/ghi như thường lệ.
- Mọi migration load giữ nguyên dữ liệu bảng; file được ghi lại theo v4 ở lần lưu kế tiếp theo quy ước hiện tại.
