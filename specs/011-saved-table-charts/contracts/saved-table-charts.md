# Hợp đồng UI: Đồ thị bảng đã lưu

## Context và navigation

- Màn hình Đồ thị được mở từ detail của bảng đã lưu đang chọn.
- Màn hình chỉ nhận dữ liệu của bảng đó; header nhận diện tên bảng.
- Back đóng chart và trở về detail cùng bảng, giữ selection và state.
- Màn hình chart không phát sinh request Jira.

## Nhóm thao tác detail

- Hiển thị các nút **Đồ thị**, **Mở lịch**, **Làm mới** theo đúng thứ tự trong một hàng.
- Ba nút dùng cùng variant, chiều cao và kiểu chữ; khoảng cách ngoài giữa các nút là 5 px (5 dp trong layout Compose).
- Refresh vẫn dùng trạng thái enable/loading hiện có; trạng thái không làm thay đổi kích thước hoặc gap.
- Đồ thị và Mở lịch không bị vô hiệu hoá do không có Jira connection.

## Loại biểu đồ và dữ liệu

| Loại | Dữ liệu | Lựa chọn |
|---|---|---|
| Pie | Ticket count theo một category | Status, Assignee, Priority |
| Column | Ticket count theo một category | Status, Assignee, Priority |
| Line | Ticket cập nhật theo ngày từ Updated hiện có | Ngày tự sắp tăng |
| Burndown | Remaining ticket count tại mỗi snapshot thành công | Snapshot baseline + successful refresh |

- Pie/column mặc định nhóm theo Status; null/rỗng hiển thị “Chưa xác định”.
- Line nhóm ngày theo múi giờ Jira đã lưu; nếu thiếu/múi giờ không hợp lệ, dùng UTC và hiển thị chú thích.
- Burndown loại trừ status category `done`; category thiếu/không rõ được tính là remaining và có chú thích.
- Mọi chart hiển thị tiêu đề, nhãn, giá trị và chú giải bằng thông tin ngoài màu sắc.
- Dữ liệu rỗng/không đủ có thông báo giải thích; không dựng số liệu giả.

## Trạng thái tương tác

- Chart type mặc định là Pie khi mở chart lần đầu.
- Field nhóm mặc định là Status; đổi field chỉ tái tính pie/column, không đổi field của line/burndown.
- Đổi chart type hoặc field không ghi cấu hình vào catalog.
- Nếu không có dữ liệu Updated hợp lệ, line chart hiển thị thông báo thay vì trục không có điểm.
- Burndown với một snapshot vẫn hiển thị baseline và thông báo lịch sử mới bắt đầu; không nội suy điểm lịch sử.
