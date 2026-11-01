# Feature Specification: Highlight Overdue Dynamic Due Date

**Feature Branch**: `017-overdue-red-row`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "task nào có Dynamic Due Date trễ hơn thời gian hiện tại, tính theo giây, thì sẽ bôi đỏ dòng đó"

## Clarifications

### Session 2026-11-01

- Q: Trạng thái đỏ quá hạn áp dụng ở bảng nào? → A: Chỉ bảng đã lưu.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nhận biết task quá hạn ngay trong bảng (Priority: P1)

Khi người dùng xem danh sách ticket trong bảng đã lưu, các task có Dynamic Due Date đã trễ so với thời điểm hiện tại phải được tô đỏ toàn bộ dòng để dễ nhận biết tình trạng quá hạn.

**Why this priority**: Đây là nhu cầu trực tiếp giúp người dùng phát hiện task quá hạn mà không phải đối chiếu thủ công từng ngày.

**Independent Test**: Có thể kiểm tra bằng một danh sách gồm task quá hạn, chưa quá hạn và không có Dynamic Due Date, sau đó xác minh chỉ task quá hạn được tô đỏ.

**Acceptance Scenarios**:

1. **Given** một task có Dynamic Due Date nhỏ hơn thời điểm hiện tại theo đơn vị giây, **When** bảng được hiển thị, **Then** toàn bộ dòng của task được tô màu đỏ.
2. **Given** một task có Dynamic Due Date lớn hơn hoặc bằng thời điểm hiện tại, **When** bảng được hiển thị, **Then** dòng task không được tô đỏ.
3. **Given** một task không có Dynamic Due Date hợp lệ, **When** bảng được hiển thị, **Then** dòng task không được tô đỏ.

---

### User Story 2 - Trạng thái quá hạn được cập nhật theo thời gian thực (Priority: P2)

Khi Dynamic Due Date đang gần đến thời điểm hiện tại, màu đỏ của dòng phải phản ánh chính xác thời gian trôi qua theo từng giây mà không phụ thuộc vào việc người dùng mở lại bảng.

**Why this priority**: Việc so sánh theo giây giúp trạng thái quá hạn chuyển đổi chính xác tại đúng thời điểm Dynamic Due Date.

**Independent Test**: Với một task có Dynamic Due Date cách thời điểm hiện tại vài giây, xác minh dòng chưa đỏ trước thời điểm đến hạn và chuyển đỏ ngay sau khi đã quá hạn.

**Acceptance Scenarios**:

1. **Given** Dynamic Due Date còn lớn hơn thời điểm hiện tại, **When** thời gian trôi qua qua đúng mốc due date theo giây, **Then** dòng chuyển sang màu đỏ.
2. **Given** Dynamic Due Date đã trễ so với thời điểm hiện tại, **When** thời gian tiếp tục trôi qua nhưng giá trị due date không đổi, **Then** dòng vẫn giữ màu đỏ.

---

### Edge Cases

- Dynamic Due Date ở một định dạng không hợp lệ hoặc thiếu: dòng không được đánh dấu quá hạn.
- Dynamic Due Date bằng chính xác thời điểm hiện tại tính theo giây: dòng chưa được coi là trễ; chỉ được tô đỏ khi nhỏ hơn thời điểm hiện tại.
- Ticket đã đóng hoặc đã hoàn thành: đặc tả không bỏ qua trạng thái hoặc trạng thái đóng khi đánh dấu quá hạn; vì đã có Dynamic Due Date quá hạn thì dòng vẫn được tô đỏ theo cùng quy tắc.
- So sánh khi ứng dụng hoạt động trên nhiều múi giờ: Dynamic Due Date và thời điểm hiện tại được quy đổi về cùng một mốc thời gian tuyệt đối trước khi so sánh theo giây.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI xác định một task là quá hạn khi Dynamic Due Date nhỏ hơn thời điểm hiện tại theo đơn vị giây.
- **FR-002**: Hệ thống PHẢI tô đỏ toàn bộ dòng của task quá hạn, bao gồm cả các ô dữ liệu trong dòng.
- **FR-003**: Hệ thống PHẢI không tô đỏ dòng khi Dynamic Due Date bằng hoặc lớn hơn thời điểm hiện tại.
- **FR-004**: Hệ thống PHẢI không tô đỏ dòng khi Dynamic Due Date trống hoặc không hợp lệ.
- **FR-005**: Hệ thống PHẢI áp dụng quy tắc tô đỏ chỉ trong bảng đã lưu có dữ liệu Dynamic Due Date; bảng kết quả truy vấn không nằm trong phạm vi tính năng này.
- **FR-006**: Trạng thái quá hạn PHẢI được cập nhật khi thời điểm so sánh thay đổi để dòng chuyển sang đỏ đúng sau khi quá hạn.
- **FR-007**: Việc tô đỏ quá hạn KHÔNG ĐƯỢC thay đổi giá trị, thứ tự hoặc quan hệ cha-con của ticket trong bảng.

### Key Entities *(include if feature involved data)*

- **Task ticket**: Một dòng trong bảng, có mã ticket và giá trị Dynamic Due Date nếu được thiết lập.
- **Dynamic Due Date**: Mốc thời gian tính theo giây của ticket, dùng để xác định trạng thái quá hạn.
- **Thời điểm hiện tại**: Mốc thời gian hệ thống dùng để so sánh với Dynamic Due Date theo cùng chuẩn thời gian.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% task có Dynamic Due Date quá hạn nhỏ hơn thời điểm hiện tại tính theo giây được tô đỏ trên toàn dòng.
- **SC-002**: 100% task có Dynamic Due Date bằng hoặc sau thời điểm hiện tại không bị tô đỏ.
- **SC-003**: 100% task không có Dynamic Due Date hợp lệ không bị tô đỏ.
- **SC-004**: Sau khi Dynamic Due Date đã trễ qua đúng mốc theo giây, trạng thái đỏ được phản ánh trong thời gian hiển thị hiện hành mà không cần thay đổi dữ liệu ticket.
- **SC-005**: Màu đỏ quá hạn không làm thay đổi thứ tự, giá trị các cột hoặc cây cha-con của bảng.

## Assumptions

- Dynamic Due Date đang được lưu trữ dưới dạng giá trị ngày và giờ có thể chuyển đổi thành một mốc thời gian.
- Quy tắc màu đỏ quá hạn chỉ được áp dụng cho bảng đã lưu.
- Dữ liệu Dynamic Due Date có múi giờ hợp lệ sẽ được quy đổi về cùng chuẩn thời gian tuyệt đối trước khi so sánh.
