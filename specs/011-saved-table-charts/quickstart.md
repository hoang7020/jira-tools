# Quickstart kiểm chứng: Đồ thị bảng đã lưu

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper.
- Chạy test với fake Jira client; không gọi Jira thật trong test.
- Để kiểm tra Desktop, có kết nối Jira test và một bảng đã lưu có nhiều ticket/status.

## Kiểm thử tự động

Chạy:

```bash
./gradlew :composeApp:jvmTest
./gradlew ktlintCheck
./gradlew build
```

Xác nhận test bao gồm:

- Grouping pie/column theo status, assignee, priority; giá trị thiếu; tổng số count bảo toàn.
- Line grouping theo ngày Updated và xử lý ticket thiếu timestamp.
- Burndown tính baseline và remaining count, unknown category và thứ tự timestamp.
- Mỗi lần save/refresh thành công chỉ tạo một snapshot; refresh cancelled/failure và write failure không tạo snapshot/không làm mất state cũ.
- Rename giữ lịch sử; delete xoá lịch sử; migration catalog v1/v2/v3 giữ bảng cũ với lịch sử rỗng.
- Chart calculation không gọi Jira client.

## Kiểm tra giao diện Desktop thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Tạo bảng đã lưu mới; mở chi tiết và kiểm tra ba nút **Đồ thị**, **Mở lịch**, **Làm mới** cùng variant/chiều cao, theo đúng thứ tự và cách nhau `5.dp` (5 px theo hợp đồng UI). Khi refresh chạy, nút thứ ba đổi thành **Dừng** nhưng vẫn cùng variant và giữ nguyên khoảng cách; tiến trình hiển thị bên dưới hàng nút.
3. Mở Đồ thị khi không kết nối Jira; xác nhận chart dùng dữ liệu local và hiển thị đúng tên bảng.
4. Chuyển pie/column giữa Status, Assignee và Priority; đối chiếu tổng số ticket với bảng; kiểm tra category rỗng hiện “Chưa xác định”.
5. Mở line; đối chiếu số ticket theo ngày Updated; bảo đảm ticket thiếu timestamp được ghi chú.
6. Mở burndown ngay sau khi save; xác nhận baseline. Làm mới bảng thành công để tạo điểm mới; làm mới thất bại/huỷ không làm thay đổi lịch sử.
7. Xác nhận nhãn, giá trị, chú giải hiển thị rõ ở light/dark theme và không cần phân biệt chỉ bằng màu sắc.
8. Quay lại; xác nhận bảng được chọn và trạng thái chi tiết được giữ. Mở chart của bảng khác và kiểm tra không lẫn dữ liệu.
9. Đổi tên bảng; xác nhận lịch sử còn nguyên. Xoá bảng sau xác nhận; xác nhận history thuộc bảng không còn xuất hiện.
10. Kiểm tra bảng rỗng, dữ liệu thiếu, một snapshot, resize cửa sổ và trường hợp lịch sử dài.

## Kết quả mong đợi

- Mỗi chart biểu diễn đúng dữ liệu và nhóm có thể đối chiếu với ticket đầu vào.
- Snapshot chỉ được thêm cùng lần lưu/làm mới thành công.
- Không cần kết nối Jira để xem dữ liệu đã lưu.
- Trở lại detail giữ nguyên bảng được chọn; thao tác cũ và lịch vẫn hoạt động.

Tham chiếu: [hợp đồng UI](./contracts/saved-table-charts.md), [data model](./data-model.md).
