# Quickstart kiểm chứng: Đồng bộ Due Date

## Điều kiện

- JDK 17 trở lên và Gradle Wrapper sẵn có.
- Test tự động dùng `MockEngine`/fake; không cấu hình hoặc gọi Jira thật.
- Thử nghiệm thủ công cần kết nối đã xác thực đến Jira site giống site của Bảng đã lưu và tài khoản có quyền sửa issue.

## Kiểm thử tự động

```bash
./gradlew :composeApp:jvmTest
```

Kiểm chứng tối thiểu:

1. HTTP fake ghi nhận PUT đúng issue và `fields.duedate` là ngày `yyyy-MM-dd`; chỉ 2xx (kể cả 204 không body) thành công.
2. HTTP 401/403/429/5xx và lỗi mạng trả thất bại, không làm snapshot thành công giả.
3. Không gửi khi Due Date đã khớp, Dynamic Due Date thiếu/không hợp lệ, kết nối/site không phù hợp, hoặc catalog không thể ghi.
4. Request đang chạy chặn lặp cùng issue nhưng không khoá issue khác.
5. Thành công cập nhật đúng snapshot và lưu bền; Jira thất bại giữ snapshot; local write failure sau Jira 2xx được thông báo là kết quả tách biệt.

## Kiểm tra UI thủ công

1. Mở Bảng đã lưu gắn với Jira site đang kết nối.
2. Chuẩn bị issue có Due Date trống, Due Date sai ngày, Due Date đúng ngày, và Dynamic Due Date không khả dụng.
3. Xác nhận chỉ hai hàng lệch ngày có nút nhỏ cùng ngày đích; không có PUT khi chỉ mở bảng.
4. Bấm nút ở một hàng và xác nhận Jira chỉ đổi đúng issue; sau thành công, hàng đồng bộ và vẫn đồng bộ sau đóng/mở lại bảng.
5. Thử quyền bị từ chối hoặc ngắt mạng; xác nhận lỗi rõ ràng, nút thử lại được và snapshot không báo thành công.
6. Nếu có thể mô phỏng lỗi ghi catalog, xác nhận thông báo nói rõ Jira đã cập nhật nhưng snapshot local chưa lưu.

## Cổng chất lượng

```bash
./gradlew :composeApp:ktlintCheck build
```

Kết quả mong đợi: JVM tests, ktlint và Desktop build đều thành công.
