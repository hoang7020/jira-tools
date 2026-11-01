# Quickstart kiểm chứng: Cuộn ngang Calendar

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn.
- Không cần kết nối Jira; kiểm thử domain dùng snapshot ticket cố định.
- Calendar ban đầu giữ cửa sổ một tháng trước và ba tháng sau ngày tham chiếu.

## Kiểm thử tự động

Chạy JVM/common tests:

```bash
./gradlew :composeApp:jvmTest
```

Kiểm tra style và build:

```bash
./gradlew ktlintCheck
./gradlew build
```

Kiểm chứng trong `SavedTableCalendarTest`:

- Mở rộng một tháng qua biên trái và biên phải tạo range liên tục, đúng thứ tự và không lặp/mất ngày.
- Khi thêm ngày ở đầu, giữ nguyên ngày neo và pixel offset; khi thêm ngày ở cuối, vị trí hiện tại không đổi.
- Projection trên range mở rộng tạo segment cho ticket nằm ở các ngày mới, với offset tương đối đúng `windowStart` mới.
- Không mở rộng hoặc overflow khi timezone, ngày tham chiếu hoặc date range không hợp lệ/đã ở giới hạn biểu diễn.
- Range rỗng hoặc ngày không có ticket không làm mất khả năng dựng trục ngày.

## Kiểm tra giao diện Desktop thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Mở **Bảng đã lưu**, chọn bảng hợp lệ và mở calendar.
3. Kéo/cuộn ngang bắt đầu trên header, trên một hàng timeline và trên vùng trống; xác nhận mọi thao tác đều cuộn cùng trục ngày.
4. Cuộn sang phải tới gần biên; xác nhận các ngày tương lai được thêm liên tục và việc cuộn tiếp tục được. Cuộn ngược để kiểm tra các ngày cũ vẫn còn.
5. Lặp lại sang trái để xác nhận mở rộng quá khứ và kiểm tra ngày neo không nhảy khi các ngày prepend.
6. Cuộn từng phần ngày và xác nhận nhãn header, grid và thanh task vẫn căn nhau.
7. Cuộn nhanh, đổi hướng, rồi cuộn dọc danh sách ticket; xác nhận cả hai trục vẫn hoạt động mà không làm lệch header/rows.
8. Lặp lại với bảng rỗng và sau khi resize cửa sổ; xác nhận trục ngày vẫn scroll, thông báo trạng thái không đổi và vị trí hiện tại được giữ.

## Kết quả mong đợi

- Cả header và nội dung timeline nhận cuộn ngang từ vùng ngày.
- Calendar thêm ngày quá khứ/tương lai ở phía sắp hết phạm vi và giữ ngày đang nhìn thấy.
- Header, lưới, timeline và cuộn dọc ticket không mất đồng bộ.
