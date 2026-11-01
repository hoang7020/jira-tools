# Nghiên cứu: Timeline theo ngày làm việc

## Quyết định 1: Giữ trục ngày hiện tại, chỉ bỏ segment khỏi cuối tuần

- **Decision**: Giữ nguyên một ô cho mọi ngày liên tiếp trong cửa sổ calendar. Thứ Bảy và Chủ nhật vẫn hiển thị trên header, nhưng projection không tạo segment timeline trên các ngày này.
- **Rationale**: Người dùng đã chọn phương án giữ cuối tuần là khoảng nghỉ nhìn thấy được; cách này bảo toàn địa chỉ ngày, thao tác cuộn, header và grid hiện có, trong khi biểu diễn khối lượng chỉ tính ngày làm việc.
- **Alternatives considered**: Bỏ ngày cuối tuần khỏi trục để ngày làm việc nối sát nhau; bị loại vì làm thay đổi cấu trúc ngày đang hiển thị và khiến cuối tuần không còn được nhận biết trực quan.
- **Evidence**: Đặc tả đã clarify trong `spec.md`; `SavedTableCalendarArea` hiện dựng mỗi ngày liên tiếp từ `windowStart` tới `windowEnd`.

## Quyết định 2: Tọa độ thanh theo phần ngày làm việc, không theo ngày lịch nguyên

- **Decision**: Chiếu khoảng instant của task lên hai phiên làm việc địa phương 08:00–12:00 và 13:00–17:00. Tọa độ ngang trong mỗi ô là tỷ lệ tích lũy của 8 giờ làm việc; phiên chiều nối liền ngay sau phiên sáng trên thang này, không dành chiều rộng cho giờ nghỉ trưa.
- **Rationale**: Đáp ứng vị trí bắt đầu/kết thúc giữa ngày và yêu cầu phần trăm chiều rộng chính xác. Chẳng hạn 10:00 là 2/8 = 25%; 14:00 là 5/8 = 62.5%. Khoảng 11:00–14:00 gồm 1 giờ sáng và 1 giờ chiều nên được biểu diễn liên tục từ 3/8 tới 5/8.
- **Alternatives considered**: Ánh xạ giờ đồng hồ 08:00–17:00 trực tiếp thành chiều rộng, để khoảng nghỉ trưa tạo khoảng trống; bị loại vì cùng số giờ làm việc sẽ có tỷ lệ sai và giờ nghỉ chiếm chiều rộng không thuộc thời gian làm việc.
- **Evidence**: `WorkingTimeCalculator` hiện đã quy định hai phiên, giờ nghỉ trưa, ngày trong tuần và timezone Jira; projection calendar hiện chỉ có offset/độ dài theo ngày lịch nên cần tọa độ fractional mới.

## Quyết định 3: Giao khoảng instant với phiên làm việc; không tự làm tròn mốc

- **Decision**: Với thanh dự kiến, chiếu giao của `[Dynamic Start Time, Dynamic Due Date]` với từng phiên làm việc trong cửa sổ. Thời điểm ngoài phiên không tạo segment; cuối tuần không tính vào thời lượng. Mốc nguồn và timezone không đổi.
- **Rationale**: Bảo toàn timestamp có sẵn để thanh bắt đầu/kết thúc đúng giữa ngày; chỉ phần thời gian làm việc được quy đổi. Việc kẹp theo session tránh vẽ ngoài giờ và tránh biến timestamp thành một ngày nguyên.
- **Alternatives considered**: Chuẩn hóa toàn bộ khoảng về thời điểm đầu phiên làm việc kế tiếp trước khi chiếu; bị loại vì có thể làm sai phần hiển thị của khoảng thực tế đã giao với phiên, và không cần biến đổi mốc nguồn để bỏ các khoảng nghỉ.
- **Edge behavior**: Khoảng rỗng hoặc thiếu/sai mốc không tạo segment. Start/end bằng nhau không tạo chiều dài dương. Múi giờ không hợp lệ tiếp tục giữ trạng thái lỗi hiện tại.

## Quyết định 4: Tái sử dụng lịch làm việc đã có, không thêm dependency hoặc persistence

- **Decision**: Tập trung các phép tính phiên làm việc/tỷ lệ trong shared domain, tái sử dụng hoặc mở rộng `WorkingTimeCalculator`; dữ liệu kết quả chỉ thuộc projection tạm thời. UI chỉ nhận segment và render.
- **Rationale**: Dynamic Due Date đã sử dụng cùng lịch làm việc, nên có một nguồn quy tắc tránh sai lệch giữa tính hạn và timeline. Tất cả logic nằm trong `commonMain` và có thể kiểm thử thuần trong `commonTest`.
- **Alternatives considered**: Sao chép hằng số/logic vào composable hoặc tạo calendar library riêng; bị loại vì vi phạm separation-of-concerns và tăng độ phức tạp không cần thiết.
- **No external contract**: Feature là thay đổi nội bộ cho calendar, không thêm endpoint hoặc tích hợp ngoài.

## Quyết định 5: Chiếu phần quá hạn bằng cùng thang ngày làm việc

- **Decision**: Giữ màu đỏ và mốc nghiệp vụ hiện tại: phần đỏ bắt đầu ở đầu ngày lịch sau Dynamic Due Date và kết thúc tại instant Real Due Date. Chiếu khoảng này theo cùng phiên làm việc, tỷ lệ ngày và timezone; ngày cuối tuần không có phần đỏ.
- **Rationale**: Giữ cách diễn giải đã được đặc tả Real Due Date phê duyệt nhưng không cho phần đỏ trở lại cách vẽ nguyên ngày. Ngày bắt đầu quá hạn là mốc ngày; phần đó xuất hiện tại giờ làm việc đầu tiên của ngày phù hợp.
- **Alternatives considered**: Bắt đầu đỏ ngay tại instant Dynamic Due Date; bị loại vì mâu thuẫn với quy tắc hiện hành “ngày kế tiếp Dynamic Due Date”.
- **Dependency**: Dùng Real Due Date snapshot hiện có; không đọc Jira hoặc thay đổi dữ liệu.

## Tương thích và kiểm chứng

- **Decision**: Giữ `SavedTableCalendarProjection` làm projection chỉ đọc và giới hạn phép duyệt theo cửa sổ ngày hiện hữu; render theo các segment và scroll state hiện có.
- **Rationale**: Không đổi schema, snapshot, sort, navigation, trục ngày hoặc cửa sổ calendar. Cửa sổ giới hạn phép tính theo số ticket nhân số ngày đang hiển thị.
- **Verification**: `SavedTableCalendarTest` bao phủ tỷ lệ đầu/cuối, nghỉ trưa, weekend, nhiều ngày, timezone/DST, clipping/continuation, dữ liệu thiếu và phần quá hạn; sau đó chạy JVM tests, ktlint và build.
