# Feature Specification: Cột Due Date trong kết quả truy vấn

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "ở bảng Kết quả truy vấn sẽ lấy được thêm cột Due Date"

## Clarifications

### Session 2026-11-01

- Q: Cột Due Date có nên xuất hiện cả trong chi tiết Bảng đã lưu, hay chỉ trong Kết quả truy vấn? → A: Trong cả Kết quả truy vấn và Bảng đã lưu.
- Q: Với các Bảng đã lưu từ trước, cột Due Date nên được tự động thêm và hiển thị sau khi cập nhật ứng dụng, hay chỉ xuất hiện khi người dùng bật thủ công? → A: Chỉ thêm khi người dùng bật thủ công.
- Q: Với cấu hình cột Kết quả truy vấn đã lưu từ phiên bản trước, có nên tự bật Due Date không? → A: Có nếu cấu hình vẫn đúng bằng bộ cột mặc định cũ; nếu đã tuỳ chỉnh thì giữ nguyên lựa chọn.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem Due Date trong kết quả truy vấn (Priority: P1)

Người dùng chạy truy vấn Jira hoặc mở một bảng đã lưu và xem ngày đến hạn của từng issue ngay trong bảng mà không cần mở từng issue trên Jira.

**Why this priority**: Ngày đến hạn là thông tin lập kế hoạch quan trọng, giúp người dùng rà soát các issue sắp hoặc đã đến hạn cùng những thông tin truy vấn khác.

**Independent Test**: Chạy truy vấn trả về một issue có Due Date và một issue không có Due Date; xác nhận cột Due Date hiển thị ngày đúng cho issue thứ nhất và ô trống cho issue thứ hai. Lưu kết quả rồi xác nhận cùng giá trị Due Date xuất hiện trong chi tiết bảng đã lưu.

**Acceptance Scenarios**:

1. **Given** kết quả truy vấn có issue với Due Date do Jira cung cấp, **When** bảng kết quả hiển thị, **Then** cột Due Date hiển thị đúng ngày đến hạn của issue đó.
2. **Given** kết quả truy vấn có issue không có Due Date, **When** bảng kết quả hiển thị, **Then** cột Due Date vẫn hiện diện và ô của issue đó để trống.
3. **Given** cột Due Date đang bị ẩn trong tuỳ chọn cột, **When** người dùng bật cột này, **Then** bảng hiển thị ngày đến hạn của các issue có dữ liệu.
4. **Given** người dùng chọn sắp xếp theo Due Date, **When** bảng sắp xếp kết quả, **Then** các ngày được sắp xếp theo thứ tự thời gian và các issue không có ngày không bị hiển thị thành một ngày giả.
5. **Given** người dùng lưu một kết quả có Due Date, **When** họ mở chi tiết bảng đã lưu, **Then** cột Due Date hiển thị cùng ngày Jira đã cung cấp.

### Edge Cases

- Issue không có Due Date hoặc Jira trả giá trị null: ô Due Date để trống.
- Một số issue có Due Date trong khi các issue khác không có: dữ liệu có ngày hiển thị chính xác, ô còn lại để trống.
- Cột Due Date bị người dùng ẩn: lựa chọn này không làm mất dữ liệu Due Date của issue.
- Bảng đã lưu tạo trước khi có cột Due Date: cột mới không tự bật và không làm thay đổi lựa chọn cột đã lưu; người dùng có thể bật thủ công.
- Jira không trả được kết quả truy vấn hoặc truy vấn thất bại: ứng dụng giữ cách xử lý trạng thái rỗng và lỗi hiện có; không hiển thị giá trị Due Date giả.
- Due Date là ngày lịch không kèm thời gian: hiển thị theo ngày Jira cung cấp, không tự chuyển ngày do múi giờ.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Bảng Kết quả truy vấn và chi tiết Bảng đã lưu MUST cung cấp cột có tên **Due Date**.
- **FR-002**: Với mỗi issue có Due Date do Jira cung cấp, cột MUST hiển thị chính xác ngày đó trong Kết quả truy vấn và sau khi lưu trong Bảng đã lưu.
- **FR-003**: Với mỗi issue không có Due Date, cột MUST hiển thị ô trống và MUST NOT thay bằng ngày mặc định hoặc ngày suy diễn.
- **FR-004**: Cột Due Date MUST có thể được bật hoặc ẩn bằng các tuỳ chọn cột hiện có trong Kết quả truy vấn và Bảng đã lưu.
- **FR-005**: Cột Due Date MUST có thể được dùng để sắp xếp các issue theo thứ tự thời gian trong các bảng hỗ trợ sắp xếp.
- **FR-006**: Cột Due Date MUST hiển thị mặc định trong Kết quả truy vấn và các Bảng đã lưu mới khi người dùng chưa tuỳ chỉnh các cột; với Bảng đã lưu cũ, ứng dụng MUST giữ nguyên lựa chọn cột và chỉ hiển thị Due Date khi người dùng bật thủ công.
- **FR-007**: Việc hiển thị, ẩn hoặc sắp xếp theo Due Date MUST NOT làm thay đổi các giá trị khác của issue hoặc thứ tự xử lý lỗi truy vấn hiện có.

### Key Entities

- **Jira issue**: Một issue trong kết quả truy vấn hoặc bảng đã lưu; có thể có hoặc không có ngày đến hạn do Jira cung cấp.
- **Due Date**: Ngày lịch đến hạn được Jira gán cho issue; khác với Dynamic Due Date và Real Due Date vốn được xác định bởi các quy tắc theo dõi tiến độ riêng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% issue trong kết quả truy vấn có Due Date hiển thị đúng ngày Jira cung cấp khi cột được hiển thị.
- **SC-002**: 100% issue không có Due Date hiển thị ô trống, không phải ngày giả hoặc giá trị suy diễn.
- **SC-003**: Người dùng có thể bật, ẩn và sắp xếp cột Due Date bằng các điều khiển cột hiện có.
- **SC-004**: Khi không có tuỳ chỉnh cột trước đó, Due Date xuất hiện mặc định trong Kết quả truy vấn và Bảng đã lưu mới; lựa chọn cột của Bảng đã lưu cũ không bị thay đổi.

## Assumptions

- Due Date là trường ngày chuẩn do Jira cung cấp; không phải Dynamic Due Date hoặc Real Due Date.
- Cột được hiển thị mặc định cho người dùng chưa có tuỳ chỉnh cột; người dùng vẫn có thể ẩn hoặc bật lại theo nhu cầu.
- Bảng đã lưu được tạo trước khi có Due Date giữ nguyên lựa chọn cột hiện tại; Due Date chỉ hiển thị sau khi người dùng bật cột đó.
- Cấu hình cột truy vấn cũ đúng bằng bộ mặc định trước khi thêm Due Date được xem là chưa tuỳ chỉnh và sẽ tự nhận cột mới; cấu hình khác bộ mặc định được giữ nguyên.
- Due Date được hiển thị trong Kết quả truy vấn và Bảng đã lưu; các quy tắc riêng của timeline/calendar không thay đổi.
- Do Due Date là ngày lịch, việc hiển thị không áp dụng chuyển đổi múi giờ.
