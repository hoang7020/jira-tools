# Data Model: Calendar cho Bảng đã lưu

## Nguyên tắc

Calendar là projection tạm thời từ snapshot hiện có. Không thêm field vào JSON, không lưu cửa sổ ngày hoặc kết quả projection vào catalog.

## Thực thể hiện có

### SavedTable

- **Vai trò**: Snapshot cục bộ và ngữ cảnh của calendar.
- **Trường được dùng**:
  - `id`, `name`: nhận diện bảng và tiêu đề calendar.
  - `jiraTimeZone`: timezone IANA dùng cho ngày hiện tại và ngày của task.
  - `tickets`: nguồn hàng và khoảng thời gian.
- **Thứ tự**: Dùng danh sách sau khi áp dụng `SavedTablesUiState.savedTableSort`.
- **Lưu trữ**: Không thay đổi schema hoặc ghi dữ liệu calendar.

### Ticket

- **Vai trò**: Một hàng calendar.
- **Trường được dùng**:
  - `key`: nhãn hàng.
  - `dynamicStartAtMillis`, `dynamicDueAtMillis`: hai instant đầu/cuối, có thể thiếu hoặc không biểu diễn được.
- **Lưu trữ**: Không thêm field.

## Projection chỉ đọc

### SavedTableCalendarProjection

- **Vai trò**: Cửa sổ ngày và hàng đã được chiếu cho một snapshot.
- **Dữ liệu đầu vào**: Danh sách ticket đã sắp xếp, `jiraTimeZone`, `nowMillis` được chụp khi mở calendar.
- **Thuộc tính dẫn xuất**:
  - `todayDate`: ngày hiện tại trong timezone bảng.
  - `windowStart`, `windowEnd`: biên inclusive, lần lượt một tháng lịch trước và ba tháng lịch sau ngày hiện tại; clamp day-of-month ở ngày cuối tháng nếu tháng đích không có ngày tương ứng.
  - `rows`: một hàng cho mỗi ticket, giữ nguyên thứ tự đầu vào.
  - `timeZoneValid`: xác định có thể dựng ngày hay không.
  - `hasValidRanges`: có ít nhất một khoảng task hợp lệ hay không; dùng để hiển thị empty-state notice, không quyết định có dựng cửa sổ.
- **Quy tắc**:
  - Chuyển `nowMillis` và từng instant task thành LocalDate theo timezone bảng.
  - Khi timezone hợp lệ, cửa sổ ngày tồn tại kể cả khi bảng không có ticket hoặc không có khoảng hợp lệ.
  - Sinh mọi ngày liên tiếp từ `windowStart` tới `windowEnd`, bao gồm cuối tuần và cả hai biên.
  - Một khoảng gốc hợp lệ khi có cả start/due và due không trước start.
  - Timezone thiếu/sai thì `todayDate`, biên cửa sổ và các vị trí thanh không xác định; giữ hàng và thông báo lỗi, không fallback sang timezone thiết bị.

### SavedTableCalendarRow

- **Dữ liệu nguồn đã chiếu**:
  - `ticketKey`.
  - `startDate`, `dueDate`: ngày gốc nếu chuyển đổi được.
  - `issue`: `NONE`, thiếu start/due/cả hai, giá trị instant không hợp lệ, timezone không hợp lệ hoặc due trước start.
- **Dữ liệu hiển thị**:
  - `visibleStartDayOffset`: offset từ `windowStart` tới ngày đầu phần giao; null nếu không có thanh.
  - `visibleDurationDays`: số ngày inclusive trong phần giao; null nếu không có thanh.
  - `continuesBeforeWindow`, `continuesAfterWindow`: đánh dấu khoảng hợp lệ đi qua mép tương ứng.
  - `outsideWindow`: `BEFORE`, `AFTER` hoặc null; chỉ đặt khi khoảng hợp lệ nằm hoàn toàn ngoài cửa sổ.
- **Validation**:
  - Thanh chỉ tồn tại cho range hợp lệ có giao với cửa sổ.
  - Khoảng cùng ngày trong cửa sổ có `visibleDurationDays = 1`.
  - Range bị cắt giữ ngày gốc, chỉ cắt phần hiển thị; không mutate `Ticket`.
  - Hàng thiếu/sai dữ liệu và hàng ngoài cửa sổ vẫn được giữ để người dùng nhận biết ticket và lý do không có thanh.

## Trạng thái giao diện

`SavedTablesUiState` giữ:

- `calendarOpen`: chế độ xem hiện tại.
- `calendarReferenceMillis`: thời điểm `AppClock.nowMillis()` chụp khi mở; xóa khi đóng hoặc chuyển bảng.
- `savedTableSort`: thứ tự dùng chung giữa bảng và calendar.

Các trường này là state phiên UI, không serialize vào `SavedTableCatalog`. Khi refresh cập nhật snapshot trong lúc calendar mở, ticket mới nhất được chiếu lại với cùng mốc cửa sổ; lần mở mới chụp mốc mới.
