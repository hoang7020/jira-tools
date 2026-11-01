# Quickstart kiểm chứng: UI/UX Material Design

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn.
- Không cần kết nối Jira để kiểm thử theme, navigation và persistence.
- Có một cấu hình người dùng test và có thể chạy ứng dụng Desktop.

## Kiểm thử tự động

Chạy test và kiểm tra chất lượng:

```bash
./gradlew :composeApp:jvmTest
./gradlew ktlintCheck
./gradlew build
```

Xác nhận test bao gồm:

- Theme mode mặc định là `SYSTEM`.
- Lưu/đọc lại lựa chọn `LIGHT`/`DARK`.
- Đọc cấu hình v1/v2 giữ lại các thiết lập hiện có và gán `SYSTEM`.
- Schema cấu hình mới được đọc/ghi chính xác; cấu hình thiếu theme vẫn tương thích.
- Các test truy vấn, cài đặt kết nối, bảng đã lưu và calendar hiện có tiếp tục đạt.

## Kiểm tra giao diện Desktop thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Với cài đặt màu hệ điều hành đang ở sáng, để theme ứng dụng là **Theo hệ điều hành**; xác nhận palette sáng.
3. Đổi chế độ màu của hệ điều hành sang tối và xác nhận ứng dụng cập nhật khi đang dùng **Theo hệ điều hành**.
4. Trong Cài đặt, chọn **Sáng**, sau đó **Tối**; xác nhận mỗi lựa chọn giữ nguyên dù hệ điều hành đang ở chế độ ngược lại.
5. Đóng/mở lại ứng dụng; xác nhận lựa chọn Sáng/Tối được giữ. Chọn lại **Theo hệ điều hành** và xác nhận theo system setting.
6. Thu gọn/mở rộng thanh điều hướng; xác nhận biểu tượng, tên destination, mục được chọn và focus bàn phím vẫn nhận biết được. Chuyển Truy vấn → Bảng đã lưu → Cài đặt và xác nhận nội dung hiện tại được giữ.
7. Kiểm tra thao tác bàn phím qua navigation, trường JQL, nút chạy và điều khiển trong Cài đặt; focus luôn nhìn thấy và thứ tự hợp lý.
8. Chạy một truy vấn hợp lệ, lỗi và trả về rỗng; xác nhận trạng thái loading/error/empty không chỉ khác nhau bằng màu và JQL không mất sau lỗi.
9. Mở Bảng đã lưu, kiểm tra trạng thái rỗng, chọn bảng, làm mới, đổi tên và huỷ/xác nhận xoá; xác nhận trạng thái đang chọn, thông báo và hành động destructive rõ ràng.
10. Mở calendar, cuộn ngang/ngang-dọc, resize cửa sổ; xác nhận trục ngày, header, timeline và vùng điều hướng không chồng lấn hoặc mất hành vi.
11. Lặp lại các bước chính ở kích thước cửa sổ tối thiểu được hỗ trợ và kiểm tra banner lỗi dài, bảng nhiều cột, summary dài.

## Kết quả mong đợi

- Ba destination chính được tổ chức trong thanh bên trái có thể thu gọn.
- Theme theo hệ điều hành mặc định; ghi đè sáng/tối được lưu đến lần khởi chạy sau.
- Material visual hierarchy, semantic status, keyboard focus và bố cục nhất quán trên các màn hình.
- Các luồng truy vấn Jira, bảng đã lưu, cài đặt kết nối và calendar giữ nguyên hành vi nghiệp vụ.

Tham chiếu hợp đồng: [contracts/material-ui.md](./contracts/material-ui.md).  
Tham chiếu model và migration: [data-model.md](./data-model.md).
