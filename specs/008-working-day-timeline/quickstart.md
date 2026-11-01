# Quickstart kiểm chứng: Timeline theo ngày làm việc

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn.
- Không cần kết nối Jira: kiểm thử projection dùng các instant fixture và timezone cụ thể.
- Lịch làm việc chuẩn: thứ Hai–thứ Sáu, 08:00–12:00 và 13:00–17:00 theo timezone Jira đã lưu.

## Kiểm thử tự động

Chạy shared tests:

```bash
./gradlew :composeApp:jvmTest
```

Kiểm tra style và build:

```bash
./gradlew ktlintCheck
./gradlew build
```

Trong `WorkingTimeCalculatorTest` và `SavedTableCalendarTest`, kiểm chứng:

- Quy đổi 08:00–12:00 thành 50% ô ngày; 10:00 nằm ở 25%, 12:00/13:00 ở 50%, 14:00 ở 62.5% và 17:00 ở 100%.
- Khoảng 10:00–14:00 đi qua giờ nghỉ trưa được vẽ liền trong ngày từ 25% đến 62.5%, không tạo khoảng chiều rộng dành cho nghỉ trưa.
- Khoảng giữa hai ngày làm việc tạo segment đúng ở từng ô, từ vị trí 10:00 ngày đầu đến vị trí 10:00 ngày cuối.
- Khoảng đi qua thứ Bảy/Chủ nhật không tạo segment ở các ngày đó; thứ Hai tiếp tục vị trí tương ứng trong ô thứ Hai.
- Mốc ngoài phiên làm việc chỉ vẽ phần giao với phiên; khoảng zero, thiếu/sai mốc hoặc timezone lỗi không tạo thanh sai lệch.
- Segment dự kiến và quá hạn cùng tuân theo tỷ lệ ngày; màu quá hạn và mốc bắt đầu ngày kế tiếp hạn được giữ nguyên.
- Segment cắt đúng ở cửa sổ và cờ tiếp diễn/ngoài cửa sổ không mất.

## Kiểm tra giao diện thủ công

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Mở **Bảng đã lưu**, chọn bảng có timezone hợp lệ và mở calendar.
3. Với task có mốc 10:00 và 14:00 trong ngày làm việc, xác nhận thanh bắt đầu tại 25% và kết thúc tại 62.5% chiều rộng ô; phần qua giờ nghỉ trưa nhìn liền mạch.
4. Với task kết thúc lúc 10:00 ngày làm việc sau, xác nhận segment ngày trước bắt đầu giữa ô và segment ngày sau dừng ở 25% ô.
5. Cuộn qua cuối tuần: ô thứ Bảy/Chủ nhật vẫn xuất hiện nhưng không có thanh; segment của ngày làm việc kế tiếp nằm đúng trong ô ngày đó.
6. Với task hoàn thành sau Dynamic Due Date, xác nhận phần màu đỏ vẫn bắt đầu từ ngày tiếp theo theo quy tắc hiện hành nhưng chỉ được vẽ trên giờ/ngày làm việc.
7. Xác nhận header, grid, cuộn ngang, thứ tự ticket, dấu tiếp diễn và thông báo dữ liệu thiếu vẫn hoạt động như trước.

## Kết quả mong đợi

- Chiều dài và điểm đặt thanh đúng theo phần thời gian làm việc trong từng ngày.
- Không có đoạn thanh hoặc thời lượng được cộng vào nghỉ trưa, ngoài giờ và cuối tuần.
- Trục ngày vẫn liên tục, weekend vẫn hiển thị trống; các thanh dự kiến/quá hạn giữ đúng màu và mốc nghiệp vụ.
