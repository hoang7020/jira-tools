# Feature Specification: Timeline theo ngày làm việc

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Chủ nhật sẽ không được tính trong timeline, timeline chỉ được vẽ trong working days. Độ dài timeline được vẽ theo phần trăm thời gian sử dụng trong working day đó; ví dụ working day có 8h, task mất 4h thì độ dài timeline là 1/2 working day, vẽ liên tục từ trái qua phải. Nếu task bắt đầu giữa ngày hôm trước và kết thúc giữa ngày hôm sau, timeline bắt đầu giữa ngày trước và kết thúc giữa ngày sau."

## Clarifications

### Session 2026-11-01

- Q: Trong calendar, thứ Bảy và Chủ nhật nên vẫn hiện thành ô ngày trống hay bị bỏ khỏi trục để các ngày làm việc nối sát nhau? → A: Vẫn hiện các ô thứ Bảy/Chủ nhật nhưng để trống; ngày làm việc sau cuối tuần tiếp tục thanh ở ô của nó.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Đọc timeline theo thời gian làm việc (Priority: P1)

Người dùng xem calendar của bảng đã lưu để biết một task chiếm bao nhiêu thời gian làm việc và diễn ra vào phần nào của từng ngày. Thanh timeline bỏ qua ngày nghỉ, thể hiện vị trí bắt đầu/kết thúc theo giờ trong ngày làm việc và giữ tỷ lệ chiều dài chính xác.

**Why this priority**: Thanh theo ngày lịch nguyên vẹn làm sai lệch thời lượng và vị trí thực tế của task, nhất là task ngắn hoặc task đi qua nhiều ngày. Biểu diễn theo ngày làm việc giúp người dùng ước lượng khối lượng và thời điểm kết thúc đáng tin cậy hơn.

**Independent Test**: Dùng các task có khoảng thời gian lần lượt bằng nửa ngày làm việc, bắt đầu/kết thúc giữa ngày, kéo qua giờ nghỉ trưa và đi qua cuối tuần. Xác nhận phần thanh trong mỗi ngày làm việc đúng tỷ lệ thời gian, thứ tự liên tục và không có đoạn trên ngày nghỉ.

**Acceptance Scenarios**:

1. **Given** một task sử dụng 4 giờ trong ngày làm việc 8 giờ, **When** calendar hiển thị task, **Then** thanh chiếm đúng một nửa chiều rộng ngày đó và đi từ trái sang phải.
2. **Given** task bắt đầu lúc 10:00 ngày làm việc thứ nhất và kết thúc lúc 10:00 ngày làm việc kế tiếp, **When** calendar hiển thị task, **Then** thanh bắt đầu tại vị trí tương ứng 2/8 ngày thứ nhất, kết thúc tại vị trí tương ứng 2/8 ngày kế tiếp và nối theo thứ tự thời gian.
3. **Given** task đi qua giờ nghỉ trưa, **When** thanh được hiển thị, **Then** thời gian nghỉ trưa không được tính vào thời lượng làm việc và các phần thanh trong ngày vẫn nối liền theo tỷ lệ giờ làm việc.
4. **Given** task đi qua thứ Bảy, Chủ nhật hoặc cả hai, **When** calendar hiển thị task, **Then** ngày nghỉ không cộng vào thời lượng làm việc và không có đoạn thanh nào được vẽ trên các ngày đó.
5. **Given** bảng có thanh dự kiến và phần quá hạn, **When** calendar hiển thị cả hai, **Then** chúng dùng cùng quy tắc tỷ lệ ngày làm việc; màu sắc và quy tắc xác định điểm bắt đầu/kết thúc phần quá hạn hiện hành không đổi.
6. **Given** khoảng task chỉ nằm trong một phần của một ngày làm việc, **When** calendar hiển thị task, **Then** thanh bắt đầu và kết thúc tại đúng phần tương ứng của ô ngày thay vì chiếm trọn ô.

### Edge Cases

- Thời điểm bắt đầu hoặc kết thúc rơi vào giờ nghỉ trưa; phần nghỉ trưa không được cộng vào thời gian làm việc.
- Thời điểm bắt đầu nằm ngoài lịch làm việc hoặc rơi vào ngày nghỉ; thanh bắt đầu từ thời điểm làm việc hợp lệ kế tiếp theo quy tắc lịch làm việc hiện hành.
- Thời điểm kết thúc nằm ngoài lịch làm việc hoặc vào ngày nghỉ; ngày nghỉ không có đoạn thanh và thời gian tiếp tục được biểu diễn tại ngày làm việc hợp lệ theo cùng trục lịch.
- Khoảng bắt đầu và kết thúc cùng một thời điểm; không tạo độ dài thời gian làm việc dương.
- Thiếu hoặc không thể xác định một trong hai mốc thời gian; giữ hàng ticket nhưng không vẽ thanh không đầy đủ.
- Khoảng thời gian chạm giới hạn cửa sổ calendar; chỉ vẽ phần nằm trong cửa sổ và giữ cách biểu thị tiếp diễn ngoài cửa sổ hiện có.
- Múi giờ bảng không hợp lệ; không suy đoán tỷ lệ theo múi giờ khác và tiếp tục hiển thị trạng thái lỗi hiện có.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Calendar MUST biểu diễn thanh theo thời gian làm việc tích lũy, không theo số ngày lịch nguyên vẹn.
- **FR-002**: Một ngày làm việc chuẩn MUST có 8 giờ, gồm 08:00–12:00 và 13:00–17:00 theo múi giờ Jira của bảng; giờ nghỉ trưa không được tính vào thời lượng hoặc vị trí thanh.
- **FR-003**: Tuần làm việc chuẩn MUST gồm thứ Hai đến thứ Sáu. Thứ Bảy, Chủ nhật và các thời điểm ngoài giờ làm việc không đóng góp thời lượng; không vẽ thanh trên ngày thứ Bảy hoặc Chủ nhật.
- **FR-004**: Trong mỗi ô ngày làm việc, vị trí ngang của thanh MUST tỷ lệ với số giờ làm việc đã qua trong ngày; phần chiều dài MUST tỷ lệ với số giờ làm việc của task trong ngày trên tổng 8 giờ.
- **FR-005**: Khi một task đi qua nhiều ngày làm việc, các đoạn MUST theo thứ tự thời gian từ trái sang phải và thể hiện điểm bắt đầu/kết thúc đúng phần ngày tương ứng, không làm tròn thành nguyên ngày.
- **FR-006**: Calendar MUST tiếp tục giữ nguyên trục ngày liên tục, thứ tự hàng ticket, cửa sổ hiển thị và hành vi cuộn hiện có; ngày nghỉ vẫn có thể hiện trên trục nhưng không chứa đoạn thanh.
- **FR-007**: Thanh thời gian dự kiến và phần quá hạn hiện có MUST dùng cùng cách quy đổi thời gian làm việc; quy tắc màu và mốc nghiệp vụ của phần quá hạn MUST không thay đổi.
- **FR-008**: Khi thiếu mốc, mốc không hợp lệ hoặc múi giờ không hợp lệ, calendar MUST không vẽ một thanh có vẻ đầy đủ hoặc chính xác sai; trạng thái hàng và thông báo hiện có MUST được giữ.
- **FR-009**: Phần thanh vượt giới hạn cửa sổ MUST tiếp tục tuân theo quy tắc cắt và đánh dấu tiếp diễn hiện có.

### Key Entities

- **Ngày làm việc**: Ngày từ thứ Hai đến thứ Sáu, có hai phiên 08:00–12:00 và 13:00–17:00 theo múi giờ Jira của bảng.
- **Khoảng thời gian làm việc của task**: Phần giao giữa khoảng bắt đầu–kết thúc của task với các phiên làm việc; xác định vị trí và chiều dài thanh trong từng ngày.
- **Thanh timeline**: Biểu diễn trực quan cho khoảng dự kiến hoặc phần quá hạn của task trên các ô ngày làm việc.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong mọi trường hợp kiểm thử, 4 giờ làm việc trong ngày chuẩn 8 giờ được hiển thị với chiều dài bằng 50% ô ngày.
- **SC-002**: Trong mọi trường hợp kiểm thử, thanh bắt đầu/kết thúc giữa ngày nằm đúng vị trí tương ứng với tỷ lệ giờ làm việc đã qua hoặc còn lại trong ngày.
- **SC-003**: 100% task đi qua thứ Bảy hoặc Chủ nhật không có phần thanh trên các ô ngày này; thời lượng của các ngày đó không được cộng vào chiều dài task.
- **SC-004**: Với task đi qua nhiều ngày làm việc, các đoạn hiển thị đúng thứ tự thời gian và không bị đảo hoặc làm tròn thành ngày nguyên.
- **SC-005**: Trục ngày, thứ tự ticket, giới hạn cuộn, thanh dự kiến và quy tắc màu quá hạn hiện hành vẫn được giữ nguyên ngoài cách phân bổ thanh theo giờ làm việc.

## Assumptions

- Calendar tiếp tục hiển thị từng ngày liên tiếp như hiện tại; ngày cuối tuần vẫn là ô trên trục nhưng để trống đối với thanh timeline.
- Lịch làm việc dùng chung với quy tắc hiện hành của Dynamic Due Date: thứ Hai–thứ Sáu, 08:00–12:00 và 13:00–17:00 theo múi giờ Jira đã lưu. Ngày nghỉ lễ và lịch làm việc tùy chỉnh không thuộc phạm vi.
- Khoảng thời gian được quy đổi theo thời gian làm việc thực tế giữa hai mốc, gồm việc bỏ qua giờ nghỉ trưa, ngoài giờ và ngày nghỉ.
- Quy tắc bắt đầu/kết thúc của phần quá hạn vẫn theo đặc tả Real Due Date hiện hành; thay đổi này chỉ điều chỉnh cách bố trí phần thanh theo tỷ lệ ngày làm việc.
