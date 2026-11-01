# Quickstart kiểm chứng: Thời gian động và ước lượng công việc

## Điều kiện tiên quyết

- JDK 17+ và Gradle Wrapper có sẵn trong repository.
- Để kiểm tra thủ công, có Jira Cloud test site, token Jira hợp lệ, project test với issue có Original Estimate và quyền xem issue.
- Automated tests chỉ dùng Ktor `MockEngine`; không cần tài khoản, token hoặc kết nối Jira thật.

## Kiểm thử tự động

Chạy logic shared và các regression tests trên JVM:

```bash
./gradlew :composeApp:jvmTest
```

Kiểm tra style:

```bash
./gradlew ktlintCheck
```

Nhóm test cần có trong `commonTest`:

- `TicketMapperTest`, `JiraApiClientSearchTest`: issue ID, status category, Original Estimate, Updated parsing và search fields.
- `WorkDurationTest`: các đơn vị `1s`, `15m`, `1h`, `1D`, số thập phân, nhiều thành phần, 0, chuỗi rỗng/âm/sai/đơn vị lạ.
- `WorkingTimeCalculatorTest`: giờ làm, nghỉ trưa, cuối tuần, mốc ngoài lịch, tuần 40 giờ, timezone và mốc 0.
- `AppConfigStoreTest`, `SavedTableStoreTest`: migration v1→v2, giữ dữ liệu, JSON/schema lỗi không bị ghi đè.
- `SavedTablesViewModelTest`: status custom theo category, chỉ ghi start một lần, share giữa bảng, giữ khi issue vắng/bảng bị xóa, retry timestamp thiếu và lỗi/hủy không commit.
- `TicketSortingTest`, `TableColumnLayoutTest`: sort theo estimate/timestamp và layout của các cột mới.
- `ConnectionRepositoryTest`: lấy timezone từ `/myself`; thiếu timezone vẫn tạo Connected nhưng Due Date có cảnh báo.

## Kiểm tra thủ công trên Jira

1. Chạy ứng dụng:

   ```bash
   ./gradlew :composeApp:run
   ```

2. Vào **Cài đặt**, kết nối Jira; kiểm tra kết nối thành công và timezone profile được nhận.
3. Chạy JQL giới hạn project test. Xác nhận `Original Expected Time` có mặt mặc định, khớp giá trị Jira (`15m`, `1h`, `1d`...) và để trống khi estimate không có.
4. Tạo saved table từ kết quả. Xác nhận Dynamic Start Time/Due Date có mặt; issue chưa In Progress chưa có start/due.
5. Đổi một issue sang một status thuộc nhóm In Progress (có thể đặt tên custom), làm mới saved table. Xác nhận Dynamic Start Time bằng Updated của response mới nhất.
6. Sửa issue để Updated đổi, làm mới; chuyển ra khỏi rồi trở lại In Progress; làm mới lần nữa. Dynamic Start Time phải giữ nguyên, Original Expected Time cập nhật và Due Date tính lại từ mốc start cũ.
7. Tạo/làm mới bảng khác có cùng Jira issue. Xác nhận cùng Dynamic Start Time; refresh một bảng không xóa mốc dùng chung.
8. Đóng/mở ứng dụng khi offline; xác nhận bảng, start, due và timezone đã lưu vẫn xem được. Xóa task khỏi kết quả của các bảng qua refresh rồi thêm lại; start cũ vẫn được dùng.
9. Ẩn/hiện riêng từng cột mới; xác nhận lựa chọn của mỗi saved table được giữ sau khi mở lại.

## Kết quả lịch mong đợi trong fixture `commonTest`

| Start trong timezone | Estimate | Due mong đợi |
|---|---:|---|
| Thứ Hai 11:30 | `1h` | Thứ Hai 13:30 (30 phút trước trưa, 30 phút sau 13:00). |
| Thứ Sáu 16:00 | `1d` | Thứ Hai 16:00 (1 giờ thứ Sáu + 7 giờ thứ Hai). |
| Thứ Hai 08:00 | `1w` | Thứ Sáu 17:00 (40 giờ làm việc). |
| Thứ Sáu 18:00 | `30m` | Thứ Hai 08:30. |
| Bất kỳ instant nào | `0` | Chính instant start, không chuẩn hóa sang kỳ tiếp theo. |

## Kết quả cần xác nhận

- Query và saved table không đổi số lượng ticket; issue không estimate vẫn hiển thị.
- Status được xác định bằng category key, không bằng chuỗi tên trạng thái.
- Lỗi trang Jira, hủy refresh hoặc lỗi ghi file không thay đổi snapshot và ledger cũ.
- Timezone thiếu/không hợp lệ để Due Date trống kèm cảnh báo; các giá trị Jira khác vẫn sử dụng được.
- Không có automated test nào gửi HTTP tới Jira thật.
