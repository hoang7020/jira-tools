# Quickstart kiểm chứng: Ngày hoàn thành thực tế và quá hạn

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn.
- Automated tests chỉ dùng fixture và Ktor MockEngine; không gọi Jira thật.
- Kiểm thử UI thủ công cần một bảng đã lưu với timezone hợp lệ và các task có lịch sử status bao gồm Close/Resolve, trạng thái khác, reopen rồi resolve/close lại, cùng các mốc hạn trước/cùng/sau ngày dự kiến.

## Kiểm thử tự động

Chạy test shared:

```bash
./gradlew :composeApp:jvmTest
```

Kiểm tra style và build:

```bash
./gradlew ktlintCheck
./gradlew build
```

Các trường hợp cần có:

- `JiraApiClient` với MockEngine: payload batch/filter status, tiếp tục `nextPageToken`, ánh xạ nhiều issue, API lỗi, lỗi parse và hủy.
- Mapper/domain: chọn transition Close/Resolve mới nhất; khớp tên không phân biệt hoa thường/trim; bỏ qua transition status khác; null khi không có transition; timestamp offset và timestamp không hợp lệ.
- `SavedTableStore`: migration v1 và v2 tới v3, giữ dữ liệu/cột cũ, thêm cột Real Due Date, JSON cũ giải mã với field mới null; schema không hỗ trợ vẫn không ghi đè.
- `SavedTablesViewModel`: khi changelog thành công thay ngày bằng sự kiện cuối; transition mới hơn thắng; không có transition xóa ngày cũ; lỗi API changelog giữ ngày cũ theo issue và issue mới để trống cùng cảnh báo; hủy refresh và lỗi JQL giữ snapshot cũ như hiện tại.
- Calendar projection: ngày đỏ bắt đầu hôm sau Dynamic Due Date, inclusive kết thúc tại Real Due Date, ngày bằng/sớm không có đoạn, cắt qua cửa sổ, ngoài cửa sổ và timezone không hợp lệ.

## Kiểm thử giao diện thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Trong **Bảng đã lưu**, xác nhận cột `Real Due Date` xuất hiện với ngày thực tế trong snapshot; task chưa từng Close/Resolve để trống.
3. Làm mới bảng có task đã đóng/giải quyết nhiều lần, gồm một lần reopen rồi Close/Resolve lại. Xác nhận giá trị dùng lần cuối, không phải Updated hiện tại của một chỉnh sửa tiếp theo.
4. Mở calendar. Với task hoàn thành sau hạn, xác nhận thanh dự kiến giữ màu hiện có và đoạn tiếp theo từ ngày sau Dynamic Due Date tới ngày Real Due Date được tô đỏ. Không có đoạn đỏ cho task đúng hạn, hoàn thành sớm hoặc thiếu một trong hai mốc.
5. Xác nhận đoạn đỏ bị cắt đúng ở giới hạn calendar và không làm đổi trục ngày.
6. Chặn request changelog nhưng để search hoạt động rồi làm mới. Xác nhận snapshot/search được cập nhật, Real Due Date cũ vẫn giữ cho issue đã biết, issue mới để trống và người dùng thấy cảnh báo dữ liệu lịch sử chưa cập nhật.
7. Mở lại bảng/calendar offline và xác nhận dữ liệu snapshot vẫn hiển thị, không phát sinh request Jira.

## Kết quả mong đợi

- Real Due Date là timestamp của lần status transition đủ điều kiện mới nhất, không bị nhầm với Updated sau đó.
- Lỗi lịch sử không làm mất ngày đã lưu và không ghi nhận dữ liệu changelog một phần.
- Chỉ phần quá hạn nhìn thấy được có màu đỏ; hàng ticket, trục và cửa sổ calendar giữ nguyên quy tắc hiện hành.
