# Hợp đồng UI: Đồng bộ Due Date trong Bảng đã lưu

## Hàng issue

- Nếu Dynamic Due Date không quy đổi được sang ngày lịch theo `jiraTimeZone`, không hiển thị thao tác.
- Nếu Due Date đã đúng ngày mục tiêu, không hiển thị thao tác.
- Nếu Due Date trống/khác ngày mục tiêu, hiển thị nút nhỏ **Cập nhật** ở đúng hàng và cho thấy ngày mục tiêu.
- Bấm nút gửi ngay thao tác trên đúng issue; không yêu cầu hộp thoại bổ sung.
- Khi đang gửi, nút của hàng đó bị vô hiệu hóa/hiển thị tiến trình; hàng khác vẫn dùng được.
- Khi thành công, Due Date của snapshot được cập nhật và nút không còn hiển thị.
- Khi Jira thất bại, giữ Due Date cũ, hiển thị lỗi và cho phép thử lại.
- Nếu Jira thành công nhưng lưu snapshot lỗi, thông báo riêng rằng Jira đã đổi nhưng trạng thái local chưa lưu; không báo thành công đầy đủ.

## Điều kiện khả dụng

- Chỉ cho phép cập nhật khi kết nối xác thực đang hoạt động và normalized Jira site khớp với site của bảng.
- Nếu catalog không đọc được hoặc không thể ghi, không gửi request Jira.
- Query table không cung cấp callback đồng bộ nên hành vi query hiện có không thay đổi.

## Phạm vi hỗ trợ

Tác vụ chỉ áp dụng cho từng hàng trong Bảng đã lưu; không có cập nhật hàng loạt hoặc tự động đồng bộ nền.
