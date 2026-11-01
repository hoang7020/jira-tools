# Quickstart kiểm chứng: Calendar cho Bảng đã lưu

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn.
- Automated tests chỉ dùng fixture; không cần gọi Jira thật.
- Kiểm tra UI cần bảng test đã lưu với timezone hợp lệ và các task trong cửa sổ, vượt từng mép, nằm ngoài cả hai phía, thiếu/sai ngày. Chuẩn bị thêm bảng rỗng hoặc chỉ có ngày không hợp lệ để xác nhận empty state.

## Kiểm thử tự động

Chạy kiểm thử shared:

```bash
./gradlew :composeApp:jvmTest
```

Kiểm tra style và build:

```bash
./gradlew ktlintCheck
./gradlew build
```

Các trường hợp cần có trong `commonTest`:

- `SavedTableCalendarTest`: chuyển `nowMillis` và timestamp task theo timezone bảng; cửa sổ một tháng trước/ba tháng sau inclusive; clamp ngày cuối tháng và năm nhuận; toàn bộ ngày liên tục qua cuối tuần/tháng/năm; khoảng cùng ngày; giao khoảng bình thường và cắt trái/phải/cả hai mép; task hoàn toàn ngoài khung về hai phía; dữ liệu thiếu/sai; timezone lỗi; không có khoảng hợp lệ nhưng cửa sổ vẫn được tạo.
- `SavedTablesViewModelTest`: chỉ mở với bảng được chọn; chụp thời điểm qua `FakeClock`; giữ tham chiếu khi calendar mở; đóng/mở lại lấy mốc mới; giữ sort khi quay lại và đóng/reset state khi đổi bảng.
- Mở calendar và dựng projection không phát sinh request Jira hoặc sửa catalog.

## Kiểm tra giao diện thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Mở **Bảng đã lưu**, chọn bảng test có timezone hợp lệ và nhiều loại khoảng đã nêu ở điều kiện tiên quyết.
3. Sắp xếp bảng theo một cột dễ nhận biết, sau đó nhấn **Mở lịch**.
4. Xác nhận calendar mở riêng; cột mã ticket đứng yên bên trái và theo đúng thứ tự đã sort. Vùng ngày bắt đầu tại ngày hiện tại theo timezone của bảng.
5. Cuộn tới biên trái và phải: biên đầu là cùng ngày tháng trước (hoặc ngày cuối tháng đích nếu cần clamp); biên cuối là cùng ngày sau ba tháng (hoặc ngày cuối tháng đích). Xác nhận không cuộn vượt biên và mã ticket luôn hiển thị.
6. Cuộn qua các ngày: mỗi ngày có một ô liên tiếp với thứ và ngày/tháng/năm; header và thanh không lệch nhau. Kiểm tra các khoảng trong khung, cùng ngày, cắt trái, cắt phải, cắt cả hai phía và hoàn toàn ngoài khung. Dấu tiếp diễn phải đúng mép; task ngoài khung có hàng và thông báo nhưng không có thanh.
7. Mở bảng rỗng hoặc bảng chỉ có task thiếu/sai ngày: xác nhận cửa sổ vẫn hiển thị khi timezone hợp lệ, hàng hiện có vẫn giữ, không có thanh và có giải thích. Với timezone không hợp lệ, xác nhận có cảnh báo và không dựng trục ngày.
8. Nhấn **Quay lại**: bảng và sort được giữ. Làm mới snapshot, mở calendar lại và xác nhận ngày task cập nhật; mở lại lấy mốc thời gian hiện tại mới.
9. Ngắt kết nối Jira rồi mở calendar từ snapshot có sẵn; xác nhận không cần request Jira để xem dữ liệu.

## Kết quả mong đợi

- Cửa sổ ngày độc lập với ngày của task, liên tục, inclusive và có giới hạn cuộn rõ ràng.
- Mọi thanh là phần giao đúng của khoảng task với cửa sổ; dấu cắt và trạng thái ngoài khung chính xác.
- Cột mã ticket luôn cố định; ngày và thanh căn theo cùng một trục ngang.
- Bảng rỗng/không có range hợp lệ vẫn có cửa sổ ngày khi timezone hợp lệ và không tạo thanh giả.
