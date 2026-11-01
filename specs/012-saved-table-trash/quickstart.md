# Quickstart kiểm chứng: Thùng rác bảng đã lưu

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper.
- Dùng fake Jira client trong test; thao tác thùng rác không cần kết nối Jira.
- Để kiểm tra Desktop, chuẩn bị ít nhất hai bảng đã lưu, một bảng có burndown history.

## Kiểm thử tự động

Chạy:

```bash
./gradlew :composeApp:jvmTest
./gradlew ktlintCheck
./gradlew build
```

Xác nhận test bao gồm:

- Migration catalog v1/v2/v3/v4 lên v5 giữ nguyên bảng và thêm trash rỗng.
- Move giữ payload, gỡ khỏi active list, thêm vào trash đúng một lần.
- Restore không conflict giữ nguyên ID/tickets/history; conflict yêu cầu tên mới và không ghi đè bảng hoạt động.
- Tên restore được trim, không rỗng và không trùng không phân biệt hoa/thường.
- Permanent delete chỉ bỏ mục trash sau xác nhận; cancel giữ nguyên.
- Write failure của move/restore/permanent delete giữ nguyên catalog và UI state trước thao tác.
- Xoá bảng không ảnh hưởng bảng còn lại hoặc ledger Dynamic Start Time dùng chung.

## Kiểm tra giao diện Desktop thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Mở/thu gọn sidebar; xác nhận destination **Thùng rác** có tên/accessible label và có thể mở trong cả hai chế độ.
3. Vào **Bảng đã lưu**, chọn bảng có burndown history và xác nhận xoá hiện tại; bảng phải biến mất khỏi active list, xuất hiện đầu danh sách thùng rác cùng nội dung nhận diện.
4. Kiểm tra bảng đã chuyển không còn mở được calendar/chart hoặc refresh trực tiếp; danh sách hoạt động vẫn dùng được với bảng khác.
5. Khôi phục một bảng không trùng tên; xác nhận bảng về active list với cùng ID/tickets/history.
6. Tạo tên trùng trong active list rồi khôi phục mục trash tương ứng; xác nhận ứng dụng yêu cầu tên khác, từ chối tên rỗng/trùng, và chấp nhận tên hợp lệ.
7. Xoá vĩnh viễn một mục, xác nhận thao tác; xác nhận nó biến mất khỏi trash. Huỷ xác nhận cho một mục khác và xác nhận mục đó còn nguyên.
8. Xác nhận trạng thái trash trống có giải thích, và các lỗi ghi hiển thị rõ mà không làm mất dữ liệu.

## Kết quả mong đợi

- Bảng chuyển qua active/trash mà không mất payload hoặc lịch sử.
- Không có thao tác permanent delete khi chưa xác nhận.
- Không bao giờ ghi đè bảng đang hoạt động khi tên restore bị trùng.
- Các thao tác local không cần gọi Jira.

Tham chiếu: [hợp đồng UI](./contracts/saved-table-trash.md), [data model](./data-model.md).
