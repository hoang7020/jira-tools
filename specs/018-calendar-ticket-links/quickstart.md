# Quickstart: Calendar Ticket Jira Links

## Preconditions

- Có bảng đã lưu với Jira base URL hợp lệ.
- Calendar hiển thị ít nhất một ticket có mã hợp lệ.

## Validation

1. Mở Calendar của bảng đã lưu.
2. Click mã ticket ở cột bên trái.
3. Xác nhận trình duyệt mở trang Jira đúng mã ticket.
4. Trở lại ứng dụng; xác nhận dữ liệu, cuộn và ngày đang xem không đổi.
5. Click timeline hoặc vùng trống; xác nhận không mở Jira.
6. Với bảng thiếu Jira base URL hợp lệ, xác nhận click mã ticket hiển thị thông báo lỗi.

## Automated Validation

```sh
./gradlew :composeApp:jvmTest
./gradlew :composeApp:ktlintCheck
```
