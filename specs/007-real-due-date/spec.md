# Feature Specification: Ngày hoàn thành thực tế và quá hạn

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Trong bảng đã lưu thêm một cột Real Due Date. Real Due Date sẽ lấy dữ liệu từ Updated khi task chuyển trạng thái sang Close hoặc Resolve (luôn lấy dữ liệu của lần cuối cùng chuyển trạng thái). Thanh timeline trong calendar sẽ vẽ tiếp từ Dynamic Due Date tới Real Due Date nhưng sẽ có thành màu đỏ (tượng trưng cho over time)."

## Clarifications

### Session 2026-11-01

- Q: Những trạng thái Jira nào nên được tính là Close hoặc Resolve để ghi nhận Real Due Date? → A: Chỉ khớp tên trạng thái “Close” hoặc “Resolve” (không phân biệt hoa thường).
- Q: Nếu Dynamic Due Date và Real Due Date rơi vào hai ngày khác nhau, đoạn màu đỏ nên bắt đầu từ ngày nào trên calendar? → A: Bắt đầu từ ngày kế tiếp Dynamic Due Date.
- Q: Nếu ứng dụng không lấy được lịch sử chuyển trạng thái trong lúc làm mới bảng, Real Due Date nên được xử lý thế nào? → A: Giữ giá trị đã lưu gần nhất (nếu có) và báo dữ liệu chưa cập nhật; nếu chưa có giá trị thì để trống.
- Q: Ngoài màu đỏ, đoạn quá hạn có cần thêm dấu hiệu để người không phân biệt màu vẫn nhận ra không? → A: Không, chỉ dùng màu đỏ.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem ngày hoàn thành thực tế (Priority: P1)

Người dùng xem bảng đã lưu và muốn biết task thực sự được đóng hoặc giải quyết vào thời điểm nào. Bảng hiển thị cột Real Due Date, lấy thời điểm Updated của lần chuyển trạng thái gần nhất sang Close hoặc Resolve.

**Why this priority**: Ngày hoàn thành thực tế là dữ liệu cần thiết để đối chiếu tiến độ dự kiến với kết quả thật và làm cơ sở thể hiện phần quá hạn trên calendar.

**Independent Test**: Làm mới bảng đã lưu chứa task có lịch sử chuyển trạng thái sang Close hoặc Resolve; xác nhận cột mới hiển thị Updated của lần chuyển trạng thái đủ điều kiện gần nhất, và giá trị thay đổi khi có lần chuyển trạng thái đủ điều kiện mới hơn.

**Acceptance Scenarios**:

1. **Given** task có ít nhất một lần chuyển trạng thái sang Close hoặc Resolve, **When** người dùng xem bảng đã lưu, **Then** cột Real Due Date hiển thị Updated tương ứng với lần chuyển trạng thái đủ điều kiện gần nhất.
2. **Given** task đã từng sang Close hoặc Resolve rồi được mở lại và sau đó chuyển sang Close hoặc Resolve lần nữa, **When** bảng được làm mới, **Then** Real Due Date phản ánh lần chuyển trạng thái mới nhất, không giữ ngày của lần trước.
3. **Given** task chưa từng chuyển sang Close hoặc Resolve, **When** người dùng xem bảng đã lưu, **Then** cột Real Due Date vẫn hiện diện và giá trị của task để trống.
4. **Given** task trong bảng không có dữ liệu Updated hợp lệ gắn với lần chuyển trạng thái đủ điều kiện, **When** bảng được làm mới, **Then** Real Due Date để trống và không được thay bằng Updated không liên quan.
5. **Given** ứng dụng không lấy được lịch sử chuyển trạng thái khi làm mới, **When** bảng cập nhật dữ liệu, **Then** Real Due Date đã lưu gần nhất được giữ nguyên (nếu có), task chưa có giá trị tiếp tục để trống, và người dùng được báo rằng ngày chưa được cập nhật.

### User Story 2 - Nhận biết phần thời gian quá hạn trên calendar (Priority: P2)

Người dùng mở calendar của bảng đã lưu và muốn nhận biết task hoàn thành sau Dynamic Due Date. Calendar tiếp tục timeline từ Dynamic Due Date đến Real Due Date bằng một đoạn màu đỏ để phân biệt phần thời gian quá hạn với khoảng dự kiến.

**Why this priority**: Cách thể hiện trực quan giúp người dùng phát hiện nhanh task hoàn thành trễ mà không phải tự so sánh từng ngày trong bảng.

**Independent Test**: Mở calendar cho bảng có task với Real Due Date sau, bằng và trước Dynamic Due Date; xác nhận chỉ khoảng sau Dynamic Due Date đến Real Due Date được biểu diễn thành đoạn quá hạn màu đỏ và các khoảng không quá hạn không bị tô đỏ.

**Acceptance Scenarios**:

1. **Given** task có Dynamic Due Date và Real Due Date hợp lệ, trong đó Real Due Date muộn hơn Dynamic Due Date, **When** calendar hiển thị task, **Then** timeline có đoạn nối tiếp sau khoảng dự kiến đến Real Due Date và đoạn quá hạn có màu đỏ.
2. **Given** Real Due Date bằng hoặc sớm hơn Dynamic Due Date, **When** calendar hiển thị task, **Then** không có đoạn quá hạn màu đỏ; cột Real Due Date vẫn hiển thị giá trị thực tế.
3. **Given** task chưa có Real Due Date hoặc thiếu Dynamic Due Date, **When** calendar hiển thị task, **Then** không vẽ đoạn quá hạn và hàng task vẫn hiển thị bình thường.
4. **Given** Real Due Date mới nhất thay đổi sau một lần chuyển trạng thái Close hoặc Resolve mới, **When** người dùng xem lại calendar sau khi bảng được cập nhật, **Then** đoạn màu đỏ kết thúc tại ngày Real Due Date mới nhất.

### Edge Cases

- Task chưa từng chuyển sang Close hoặc Resolve: cột Real Due Date để trống và calendar không có đoạn quá hạn.
- Không lấy được lịch sử chuyển trạng thái trong một lần làm mới: giữ Real Due Date đã lưu gần nhất (nếu có) và báo dữ liệu chưa được cập nhật; không nhầm lỗi tải dữ liệu với trường hợp đã xác nhận task chưa từng chuyển sang trạng thái đủ điều kiện.
- Task được mở lại sau khi đóng: giữ ngày của lần chuyển trạng thái đủ điều kiện gần nhất cho đến khi task chuyển sang Close hoặc Resolve lần nữa; khi có lần mới, thay bằng thời điểm mới.
- Nhiều lần chuyển trạng thái đủ điều kiện có cùng ngày: dùng Updated của lần chuyển trạng thái mới nhất, không tạo nhiều đoạn quá hạn.
- Real Due Date sớm hơn Dynamic Due Date: vẫn hiển thị Real Due Date trong bảng, nhưng không vẽ đoạn quá hạn ngược chiều.
- Một trong hai mốc Dynamic Due Date hoặc Real Due Date thiếu hoặc không hợp lệ: không vẽ đoạn quá hạn gây hiểu nhầm.
- Đoạn quá hạn vượt giới hạn ngày của calendar: áp dụng giới hạn và cách cắt/đánh dấu ngoài phạm vi hiện có; không mở rộng cửa sổ calendar.
- Giá trị thời điểm không thể diễn giải theo múi giờ của bảng: không vẽ đoạn quá hạn cho task đó và không làm ảnh hưởng các hàng khác.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Bảng đã lưu MUST có cột Real Due Date cho mỗi task.
- **FR-002**: Real Due Date MUST bằng Updated tương ứng với lần gần nhất task chuyển sang trạng thái có tên “Close” hoặc “Resolve”, không phân biệt hoa thường.
- **FR-003**: Khi task chưa có lần chuyển trạng thái đủ điều kiện hoặc không có Updated hợp lệ cho lần đó, Real Due Date MUST để trống.
- **FR-004**: Sau khi bảng được cập nhật, Real Due Date MUST phản ánh lần chuyển trạng thái mới nhất sang trạng thái có tên “Close” hoặc “Resolve”, không phân biệt hoa thường, và MUST NOT bị thay bằng Updated của một thay đổi không phải chuyển trạng thái đủ điều kiện.
- **FR-005**: Calendar MUST tiếp tục timeline của task bằng một đoạn riêng, dùng màu đỏ làm dấu hiệu duy nhất cho phần quá hạn, từ ngày kế tiếp Dynamic Due Date đến ngày Real Due Date khi Real Due Date muộn hơn Dynamic Due Date.
- **FR-006**: Calendar MUST NOT vẽ đoạn quá hạn khi một trong hai mốc cần thiết bị thiếu/không hợp lệ hoặc Real Due Date không muộn hơn Dynamic Due Date.
- **FR-007**: Đoạn quá hạn MUST tuân theo thứ tự task, múi giờ và giới hạn ngày hiện có của calendar; đoạn vượt ngoài giới hạn MUST được cắt/đánh dấu theo quy tắc hiện hành mà không làm thay đổi giới hạn.
- **FR-008**: Giá trị Real Due Date MUST được cập nhật cùng dữ liệu bảng đã lưu và được phản ánh trên calendar khi người dùng xem dữ liệu đã cập nhật.
- **FR-009**: Nếu không lấy được lịch sử chuyển trạng thái trong lần làm mới, hệ thống MUST giữ Real Due Date đã lưu gần nhất (nếu có), để trống nếu chưa có giá trị, và MUST báo người dùng rằng giá trị chưa được cập nhật.

### Key Entities

- **Real Due Date**: Thời điểm Updated ứng với lần chuyển trạng thái gần nhất của task sang Close hoặc Resolve; có thể chưa có giá trị.
- **Đoạn quá hạn**: Phần timeline bắt đầu sau Dynamic Due Date và kết thúc tại Real Due Date, được phân biệt bằng màu đỏ khi task hoàn thành muộn.
- **Task trong bảng đã lưu**: Task cùng dữ liệu trạng thái, Updated, Dynamic Due Date và Real Due Date được dùng để hiển thị bảng và calendar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong 100% task có lịch sử chuyển sang Close hoặc Resolve và có Updated hợp lệ, Real Due Date hiển thị đúng thời điểm của lần chuyển trạng thái đủ điều kiện mới nhất.
- **SC-002**: Trong 100% task có Real Due Date muộn hơn Dynamic Due Date, calendar hiển thị đoạn quá hạn màu đỏ kết thúc đúng ngày Real Due Date trong phạm vi đang xem.
- **SC-003**: Trong 100% task thiếu mốc hợp lệ hoặc có Real Due Date không muộn hơn Dynamic Due Date, calendar không hiển thị đoạn màu đỏ gây hiểu nhầm.
- **SC-004**: Trong thử nghiệm với ít nhất 5 người dùng, ít nhất 4 người xác định đúng task hoàn thành quá hạn và ngày hoàn thành thực tế từ bảng đã lưu/calendar mà không cần mở task trên Jira.

## Assumptions

- Trạng thái đủ điều kiện là trạng thái đích có tên “Close” hoặc “Resolve”, không phân biệt hoa thường; trạng thái khác, kể cả trạng thái thuộc nhóm hoàn tất của Jira, không tự động được xem là tương đương.
- Updated dùng cho Real Due Date là Updated gắn với chính lần chuyển trạng thái đủ điều kiện, không phải Updated hiện tại của task nếu có thay đổi sau đó.
- Nếu task được mở lại, Real Due Date tiếp tục giữ lần chuyển trạng thái Close hoặc Resolve gần nhất đã ghi nhận; một lần chuyển trạng thái đủ điều kiện mới sẽ thay thế giá trị đó.
- Calendar tiếp tục sử dụng độ chi tiết ngày, múi giờ, thứ tự task, cửa sổ ngày và quy tắc cắt ngoài phạm vi đã được xác định cho calendar của bảng đã lưu. Không thể hiện phần thời gian trong ngày riêng.
- Đoạn quá hạn bắt đầu từ ngày tiếp theo Dynamic Due Date và kết thúc tại ngày Real Due Date; nếu cả hai thời điểm rơi cùng ngày thì không có đoạn quá hạn nhìn thấy được ở độ chi tiết ngày.
- Thay đổi chỉ bổ sung cột Real Due Date và đoạn timeline quá hạn; không thay đổi cách tính Dynamic Due Date hoặc khoảng thời gian dự kiến hiện có.
