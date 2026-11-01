# Quickstart kiểm chứng: Cột Due Date

## Điều kiện

- JDK 17 trở lên.
- Có Gradle Wrapper trong repository.
- Để kiểm tra tích hợp thủ công, cấu hình Jira hợp lệ và có issue với/không có Due Date; kiểm thử tự động không gọi Jira thật.

## Kiểm thử tự động

Chạy bộ kiểm thử JVM cho `commonTest`:

```bash
./gradlew :composeApp:jvmTest
```

Các trường hợp cần có:

1. DTO/mapper nhận đúng chuỗi `duedate`, và trường thiếu/null ánh xạ thành null.
2. Sắp xếp tăng/giảm theo ngày lịch; dữ liệu thiếu không tạo ngày giả.
3. Migration cấu hình cũ bổ sung Due Date khi tập cột bằng mặc định trước đây và giữ nguyên cấu hình đã tuỳ chỉnh.
4. Bảng đã lưu cũ giữ nguyên lựa chọn cột; bảng mới có thể mặc định hiển thị Due Date.

## Kiểm tra UI thủ công

1. Chạy ứng dụng Desktop bằng cấu hình chạy hiện có.
2. Chạy truy vấn chứa ít nhất một issue có Due Date và một issue không có.
3. Xác nhận cột mặc định hiển thị đúng ngày Jira và ô còn lại để trống; tắt/bật cột và thử sort hai chiều.
4. Lưu kết quả rồi mở bảng: xác nhận giá trị vẫn hiển thị trong snapshot.
5. Mở một bảng lưu từ trước; xác nhận cột không tự bật và có thể bật bằng bộ chọn cột.

## Cổng chất lượng

```bash
./gradlew :composeApp:ktlintCheck build
```

Kết quả mong đợi: kiểm thử JVM, ktlint và build Desktop đều thành công.
