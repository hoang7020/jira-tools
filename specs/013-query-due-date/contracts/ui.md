# Hợp đồng UI: Cột Due Date

## Kết quả truy vấn

- Bộ chọn cột có mục **Due Date** và tuân theo cơ chế bật/tắt cột hiện có.
- Mặc định hiển thị trên thiết lập mới.
- Khi nâng thiết lập cũ, tự bật chỉ khi danh sách cột lưu bằng bộ mặc định trước khi bổ sung Due Date; các thiết lập đã tuỳ chỉnh giữ nguyên.
- Ô hiển thị ngày lịch Jira nguyên dạng; issue không có ngày để trống.
- Tiêu đề cột hỗ trợ sort hai chiều theo thời gian lịch; dữ liệu thiếu hoặc không hợp lệ không được biểu diễn thành ngày mặc định.

## Bảng đã lưu

- Bộ chọn cột của từng bảng có mục **Due Date**.
- Bảng mới mặc định hiển thị cột.
- Bảng cũ giữ nguyên `visibleColumns`; người dùng chủ động bật cột mới nếu muốn.
- Giá trị lấy từ snapshot Ticket đã lưu và không cần gọi Jira để hiển thị.

## Ngoài phạm vi

- Không thay đổi cách tính hoặc hiển thị Dynamic Due Date, Real Due Date, timeline/calendar hoặc Due Date trong JQL.
