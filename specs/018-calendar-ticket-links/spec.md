# Feature Specification: Calendar Ticket Jira Links

**Feature Branch**: `[018-calendar-ticket-links]`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "ở calendar khi click vào từng mã ticket, nó sẽ mở link jira của ticket đó"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mở ticket Jira từ calendar (Priority: P1)

Người dùng đang xem calendar của bảng đã lưu và muốn xem chi tiết một ticket. Người dùng chọn mã ticket hiển thị bên trái dòng timeline, ứng dụng mở đúng trang Jira của ticket đó.

**Why this priority**: Đây là luồng truy cập Jira trực tiếp quan trọng nhất, giúp người dùng chuyển từ bối cảnh lịch sang chi tiết công việc mà không phải tìm lại ticket thủ công.

**Independent Test**: Click mã ticket trong calendar và xác nhận trình duyệt mở đúng URL Jira tương ứng với mã ticket, mà không cần thực hiện hành động khác.

**Acceptance Scenarios**:

1. **Given** calendar đang hiển thị một ticket có mã hợp lệ và bảng đã lưu liên kết với một Jira instance, **When** người dùng click mã ticket đó, **Then** trình duyệt mặc định mở trang Jira của đúng mã ticket.
2. **Given** calendar hiển thị nhiều ticket, **When** người dùng click mã ticket khác nhau, **Then** mỗi lần mở đúng trang Jira của ticket tương ứng.

---

### User Story 2 - Không làm gián đoạn khi không thể mở link (Priority: P2)

Người dùng click mã ticket nhưng cấu hình Jira không có địa chỉ hợp lệ hoặc hệ điều hành không thể mở liên kết. Người dùng phải nhận thông báo rõ ràng thay vì ứng dụng bị treo hoặc không có phản hồi.

**Why this priority**: Cấu hình Jira hoặc môi trường desktop có thể không khả dụng, nên hành vi cần dự đoán được và không gây hiểu nhầm rằng ticket đã được mở.

**Independent Test**: Với ticket hợp lệ nhưng thiếu cấu hình địa chỉ Jira hoặc không thể mở liên kết, xác nhận người dùng nhận thông báo lỗi phù hợp.

**Acceptance Scenarios**:

1. **Given** bảng đã lưu không có địa chỉ Jira hợp lệ, **When** người dùng click mã ticket, **Then** ứng dụng không mở trang rỗng và hiển thị thông báo không thể mở Jira.
2. **Given** hệ điều hành không mở được liên kết, **When** người dùng click mã ticket, **Then** ứng dụng hiển thị thông báo không thể mở Jira và calendar vẫn tiếp tục hoạt động bình thường.

---

### Edge Cases

- Ticket mã trống hoặc dữ liệu ticket không hợp lệ không xuất hiện như một liên kết có thể click.
- Chỉ phần mã ticket được xác định là điểm click; click vào timeline hoặc vùng trống của dòng không được mở Jira.
- Khi URL Jira đã chứa đường dẫn cơ sở, ứng dụng phải tạo URL trang ticket đúng mà không nhân đôi đường dẫn cơ sở.
- Việc mở một ticket không thay đổi dữ liệu, bộ lọc, trạng thái cuộn hoặc ticket đang chọn trong calendar.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Calendar MUST hiển thị mã ticket trong mỗi dòng dưới dạng liên kết có thể nhận biết khi được tương tác.
- **FR-002**: Khi người dùng click mã ticket, hệ thống MUST mở trang Jira của đúng ticket được click bằng trình duyệt mặc định.
- **FR-003**: URL Jira của ticket MUST dựa trên Jira instance đã cấu hình cho bảng đã lưu.
- **FR-004**: Chỉ phần mã ticket MUST kích hoạt hành vi mở Jira; timeline và các vùng khác của dòng calendar MUST không kích hoạt.
- **FR-005**: Nếu không thể xác định URL Jira hợp lệ, hệ thống MUST thông báo cho người dùng và không mở liên kết rỗng.
- **FR-006**: Nếu môi trường desktop không thể mở liên kết ngoài, hệ thống MUST thông báo cho người dùng và không làm calendar bị gián đoạn.
- **FR-007**: Hành vi mở ticket MUST không thay đổi dữ liệu bảng đã lưu, phạm vi hiển thị calendar hoặc vị trí cuộn.

### Key Entities

- **Ticket Calendar Entry**: Một dòng ticket trong calendar, gồm mã ticket và thông tin lịch được hiển thị.
- **Jira Ticket Link**: URL đến trang chi tiết ticket trên Jira instance của bảng đã lưu.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% mã ticket hợp lệ trong calendar được click đều mở đúng URL trang Jira của mã đó trong một lần click.
- **SC-002**: Người dùng không cần thực hiện thao tác tìm kiếm hoặc sao chép mã để truy cập trang Jira ticket từ calendar.
- **SC-003**: Ít nhất 95% người dùng thử nghiệm hoàn thành thao tác mở ticket thành công trong lần thử đầu tiên khi cấu hình Jira hợp lệ.
- **SC-004**: 100% trường hợp lỗi mở liên kết được báo rõ cho người dùng mà không đóng hoặc làm mất trạng thái calendar.

## Assumptions

- Bảng đã lưu đã có Jira instance hoặc địa chỉ Jira được cấu hình để tạo URL ticket.
- Trình duyệt mặc định của desktop được sử dụng để mở trang Jira.
- Bản ràng buộc mở liên kết ngoài chỉ áp dụng cho các mã ticket trong calendar; hành vi click tại các vị trí khác của calendar không thay đổi.
