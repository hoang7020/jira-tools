# Nghiên cứu: Calendar cho Bảng đã lưu

## 1. Giao diện calendar

**Decision**: Giữ calendar là chế độ xem riêng trong mục Bảng đã lưu. Cột mã ticket nằm cố định bên trái; phần ngày ở bên phải cuộn ngang và hàng task cuộn dọc.

**Rationale**: Đây là bố cục đã được làm rõ trong spec. `SavedTableCalendarArea` hiện có đã tách cột ticket khỏi LazyRow ngày và vẽ thanh theo trạng thái ngày đang nhìn thấy; có thể cập nhật cấu trúc này mà không thêm màn hình cấp ứng dụng hoặc dependency.

**Alternatives considered**:
- Đặt calendar cạnh bảng kết quả: bị loại vì trái yêu cầu.
- Mở cửa sổ desktop hoặc route mới: bị loại vì không cần điều hướng cấp ứng dụng hay API nền tảng.

## 2. Nguồn dữ liệu và vòng đời

**Decision**: Chiếu `SavedTable.tickets` theo thứ tự đã sắp xếp và `SavedTable.jiraTimeZone`; không tạo bản sao persistent và không gọi Jira khi mở calendar.

**Rationale**: Ticket đã chứa hai instant cần vẽ; bảng lưu timezone Jira; StateFlow cung cấp snapshot mới nhất sau refresh. `AppClock` đã được inject trong ứng dụng và có `FakeClock` cho test, nên thời điểm hiện tại có thể xác định mà không dùng system clock ở `commonMain`.

**Alternatives considered**:
- Lưu dữ liệu timeline riêng: bị loại vì trùng lặp với ticket và có thể lỗi thời.
- Dùng timezone thiết bị hoặc `Clock.System`: bị loại vì không khớp timezone bảng và khó kiểm thử xác định.
- Tính lại mốc thời gian giữa lúc calendar đang mở: bị loại để cửa sổ và vị trí cuộn không dịch bất ngờ; chụp `nowMillis` khi mở, làm mới mốc khi mở lại.

## 3. Cửa sổ ngày theo tháng lịch

**Decision**: Chuyển `nowMillis` thành ngày địa phương theo timezone bảng, sau đó tính đầu cửa sổ bằng ngày đó lùi một tháng lịch và cuối cửa sổ bằng ngày đó tiến ba tháng lịch. Hai biên inclusive. Nếu tháng đích không có cùng day-of-month, clamp về ngày cuối tháng đích. Sinh một ô cho mọi LocalDate trong cửa sổ.

**Rationale**: Yêu cầu là khoảng tháng lịch, không phải số ngày cố định. `kotlinx-datetime` cung cấp phép toán `LocalDate`/`DateTimeUnit.MONTH` độc lập với timezone cho phần date arithmetic; phép đổi instant sang ngày cần timezone tường minh. Quy tắc clamp được kiểm thử riêng cho cuối tháng và năm nhuận thay vì cộng duration hoặc 24 giờ.

**Alternatives considered**:
- Trừ/cộng 30 hoặc 90 ngày: bị loại vì không tương đương một/ba tháng lịch.
- Cộng duration hoặc mili giây vào instant: bị loại vì độ dài tháng khác nhau, không thực hiện clamp ngày theo tháng.
- Tạo trục từ min/max của task: bị loại vì task ngoài cửa sổ không được phép nới biên.

**Nguồn kỹ thuật**:
- [`LocalDate.plus` trong kotlinx-datetime 0.8.0](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/plus.html) mô tả phép cộng đơn vị ngày/tháng trên LocalDate.
- [`Instant.toLocalDateTime(timeZone)`](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/to-local-date-time.html) xác định thời gian dân sự của instant trong timezone tường minh.
- [`DateTimeUnit.MonthBased`](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-date-time-unit/-month-based/) xác nhận tháng là đơn vị lịch, không thể biểu diễn bằng một số ngày cố định.

## 4. Projection khoảng task

**Decision**: Giữ projection miền thuần, nhận danh sách task, timezone và `nowMillis`. Với mỗi range hợp lệ, tính giao inclusive với cửa sổ; lưu offset/độ dài nhìn thấy, cờ tiếp diễn qua mép trái/phải và hướng ngoài cửa sổ khi không có phần giao. Không xóa hàng task thiếu/sai ngày hoặc ngoài khung.

**Rationale**: Phép giao ngày thuộc logic miền và có thể kiểm thử không cần Compose. Tách khoảng gốc khỏi phần nhìn thấy giúp không làm sai dữ liệu Dynamic Start/Due đã lưu; cờ tiếp diễn cho phép UI đánh dấu clipping chính xác.

**Alternatives considered**:
- Sửa start/due của ticket thành biên cửa sổ: bị loại vì làm mất khoảng gốc và có thể gây hiểu nhầm.
- Bỏ task ngoài phạm vi khỏi calendar: bị loại vì mỗi ticket phải giữ hàng và có thông báo.
- Vẽ thanh vượt ngoài trục: bị loại vì vượt giới hạn cuộn cứng.

## 5. Hành vi khi không có khoảng hợp lệ hoặc timezone

**Decision**: Với timezone hợp lệ, luôn dựng cửa sổ ngày cố định, kể cả bảng rỗng hoặc không có khoảng task hợp lệ. Giữ các hàng hiện có, không vẽ thanh và hiển thị thông báo trống. Nếu timezone thiếu/sai, hiển thị cảnh báo, giữ hàng ticket nhưng không dựng ngày không xác định.

**Rationale**: Cửa sổ là độc lập với dữ liệu task. Quyết định clarification mới nhất đã chốt hiển thị ngày khi không có khoảng hợp lệ; trạng thái trống mô tả việc không có thanh, không thay thế trục ngày đã định nghĩa.

**Alternatives considered**:
- Ẩn trục khi không có task hợp lệ: bị loại theo câu trả lời clarification.
- Dùng timezone thiết bị làm fallback: bị loại vì hiển thị ngày sai ngữ cảnh bảng.

## 6. Cuộn, thứ tự và hiệu năng

**Decision**: Giữ cột ticket bên ngoài vùng LazyRow; header ngày và các thanh dùng cùng một trạng thái cuộn ngang. Hàng giữ thứ tự từ `sortTickets`; mở calendar khởi tạo vị trí nhìn thấy tại ngày hiện tại. Chụp mốc `AppClock` khi mở để giới hạn ổn định tới khi đóng.

**Rationale**: Cửa sổ chỉ khoảng bốn tháng lịch, nên một item mỗi ngày vừa đáp ứng việc đọc chính xác từng ngày vừa giới hạn tổng số item. Cột cố định và một offset chung tránh header/thanh lệch nhau trong lúc cuộn.

**Alternatives considered**:
- Cuộn header và từng hàng độc lập: bị loại vì dễ mất đồng bộ ngày/thanh.
- Dựng toàn bộ tháng/năm theo task: bị loại vì vi phạm giới hạn và không cần thiết.

## Phạm vi xác minh

- Automated: test `commonTest` cho phép tính cửa sổ, timezone, clamp tháng, giao khoảng, cắt hai mép, hàng ngoài khung và trường hợp không có khoảng hợp lệ.
- UI: quickstart kiểm tra sticky ticket column, cuộn tới hai biên, header/thanh đồng bộ, trạng thái trống và thanh cắt.
- Không cần Jira thật; calendar là projection chỉ đọc từ snapshot.
