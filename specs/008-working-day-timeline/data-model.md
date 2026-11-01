# Mô hình dữ liệu: Timeline theo ngày làm việc

## Nguyên tắc

Timeline là projection tạm thời từ snapshot đã lưu. Không thêm hoặc sửa field trong JSON/catalog và không gọi Jira khi mở calendar.

## Thực thể hiện có

### SavedTable

- **Vai trò**: Cung cấp ngữ cảnh timezone và tập ticket của calendar.
- **Trường được dùng**:
  - `jiraTimeZone`: timezone IANA cho ngày địa phương và phiên làm việc.
  - `tickets`: nguồn mốc thời gian của thanh.
- **Lưu trữ**: Không đổi.

### Ticket

- **Vai trò**: Nguồn khoảng thời gian của một hàng.
- **Trường được dùng**:
  - `key`: nhãn hàng.
  - `dynamicStartAtMillis`, `dynamicDueAtMillis`: hai đầu của thanh dự kiến.
  - `realDueAtMillis`: đầu cuối của thanh quá hạn (nếu tồn tại).
- **Lưu trữ**: Không thêm field.

## Projection chỉ đọc

### SavedTableCalendarProjection

Tiếp tục giữ `todayDate`, `windowStart`, `windowEnd`, `hasValidRanges`, `timeZoneValid` và danh sách `rows` như hiện tại. Cửa sổ tiếp tục gồm mọi ngày liên tiếp, gồm cả thứ Bảy/Chủ nhật.

### SavedTableCalendarRow

- **Dữ liệu nguồn đã chiếu**: `ticketKey`, ngày địa phương của start/due/real due, `issue`.
- **Các segment timeline**:
  - `expectedSegments`: segment làm việc thuộc thanh Dynamic Start Time–Dynamic Due Date.
  - `overdueSegments`: segment làm việc thuộc khoảng từ đầu ngày kế tiếp Dynamic Due Date tới Real Due Date.
  - Danh sách rỗng nghĩa là không có phần thời gian làm việc nhìn thấy trong cửa sổ.
- **Trạng thái cửa sổ**: Giữ các cờ tiếp diễn trước/sau và trạng thái hoàn toàn ngoài cửa sổ cho khoảng dự kiến và quá hạn để calendar tiếp tục trình bày clipping hiện có.
- **Thứ tự**: Một row cho mỗi ticket, theo đúng thứ tự đầu vào đã sắp xếp.

### CalendarWorkSegment

- `dayOffset: Long`: số ngày từ `windowStart` tới ô ngày chứa segment.
- `startFraction: Double`: tọa độ bắt đầu trong ô làm việc, từ `0.0` tới dưới `1.0`.
- `endFraction: Double`: tọa độ kết thúc, lớn hơn `startFraction` và tối đa `1.0`.
- `dayOffset` chỉ được trỏ tới ngày thứ Hai–thứ Sáu. Không tạo segment trên ngày nghỉ cuối tuần.
- Các phân số dùng tổng thời gian làm việc tích lũy trong ngày: 08:00 là `0.0`, 12:00/13:00 là `0.5`, 17:00 là `1.0`. Nghỉ trưa không tạo khoảng cách trong tọa độ.
- Segment là phần giao không rỗng giữa khoảng instant và các phiên địa phương 08:00–12:00/13:00–17:00; segment trước/sau cửa sổ được cắt theo giới hạn hiện tại.

## Quy tắc kiểm tra và chiếu

- Chỉ chiếu khi timezone hợp lệ và hai mốc nguồn hợp lệ theo loại khoảng.
- Khoảng dự kiến dùng Dynamic Start Time và Dynamic Due Date; khoảng quá hạn giữ quy tắc hiện hành là ngày sau ngày Dynamic Due Date đến Real Due Date.
- Với từng ngày trong cửa sổ, chuyển phiên làm việc địa phương sang instant theo timezone của bảng, giao với khoảng nguồn rồi chuyển phần giao thành tọa độ tỷ lệ trong ngày.
- Nghỉ trưa, ngoài giờ, thứ Bảy và Chủ nhật không đóng góp chiều dài; ngày weekend vẫn giữ ô trên trục.
- Mốc nguồn được giữ nguyên; phép chiếu không mutate `Ticket`.
- Khoảng rỗng/đảo ngược không sinh segment dương. Thiếu/sai mốc hoặc timezone tiếp tục biểu hiện qua `issue` và trạng thái hàng hiện có.

## Trạng thái giao diện

Không đổi cấu trúc state của `SavedTablesUiState`. Cửa sổ calendar, scroll axis và sort tiếp tục theo hành vi hiện tại; Compose đọc các segment của row để vẽ trong ngày. Không có dữ liệu dẫn xuất được serialize.
