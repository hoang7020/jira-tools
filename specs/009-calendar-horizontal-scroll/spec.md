# Feature Specification: Sửa cuộn ngang Calendar

**Feature Branch**: `009-calendar-horizontal-scroll`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Sửa lỗi calendar không scroll ngang được, calendar có thể scroll qua trái và phải để thấy quá khứ và tương lai."

## Clarifications

### Session 2026-11-01

- Q: Khi cuộn sang trái hoặc phải, calendar chỉ cần cho xem các ngày trong phạm vi hiện có, hay cần tự mở rộng phạm vi để tiếp tục xem quá khứ và tương lai? → A: Tự mở rộng phạm vi ngày khi cuộn đến biên để tiếp tục xem quá khứ hoặc tương lai.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cuộn trục ngày Calendar (Priority: P1)

Người dùng xem calendar của bảng đã lưu và cuộn theo chiều ngang sang trái hoặc phải để xem các ngày quá khứ hoặc tương lai trong phạm vi lịch.

**Why this priority**: Khi không thể cuộn ngang, người dùng không thể xem các ngày và timeline nằm ngoài phần đang hiển thị; đây là chức năng chính của calendar.

**Independent Test**: Mở calendar có trục ngày rộng hơn vùng hiển thị, cuộn lần lượt sang hai hướng và xác nhận các ngày trước/sau ban đầu xuất hiện, cùng timeline của ticket vẫn thẳng hàng với đúng ngày.

**Acceptance Scenarios**:

1. **Given** calendar có các ngày nằm ngoài vùng nhìn thấy ở bên phải, **When** người dùng cuộn ngang sang phải, **Then** trục ngày và timeline hiển thị các ngày tiếp theo với nội dung vẫn thẳng hàng.
2. **Given** người dùng đã cuộn calendar sang phải và còn các ngày ở bên trái, **When** người dùng cuộn ngang sang trái, **Then** các ngày trước đó và timeline tương ứng xuất hiện.
3. **Given** người dùng cuộn một phần ngày, **When** header hoặc bất kỳ hàng ticket nào được quan sát, **Then** tất cả dùng cùng vị trí cuộn và ngày trong header vẫn khớp với cột timeline bên dưới.
4. **Given** calendar vừa được mở, **When** trục ngày khởi tạo, **Then** vị trí ban đầu tiếp tục đặt ngày hiện tại trong vùng nhìn thấy như hành vi hiện tại.
5. **Given** người dùng cuộn tới gần đầu hoặc cuối phạm vi ngày đang hiển thị, **When** tiếp tục cuộn theo hướng đó, **Then** calendar tự bổ sung các ngày quá khứ hoặc tương lai tương ứng để người dùng tiếp tục cuộn.
6. **Given** calendar đã tự mở rộng một phía, **When** người dùng đổi hướng cuộn, **Then** các ngày đã được thêm vẫn có thể xem lại và trục ngày cùng timeline tiếp tục thẳng hàng.

### Edge Cases

- Phạm vi ngày vừa khít hoặc nhỏ hơn chiều rộng vùng hiển thị thì không cần cuộn và không làm lệch header/grid; khi người dùng cuộn đến biên, phạm vi tiếp tục mở rộng.
- Calendar không có hàng ticket vẫn cho phép cuộn trục ngày nếu phạm vi ngày rộng hơn vùng hiển thị.
- Cuộn nhanh liên tục hoặc đổi hướng giữa chừng không làm header và các hàng timeline mất đồng bộ.
- Calendar không có phạm vi ngày hợp lệ vẫn giữ thông báo lỗi hiện tại và không phát sinh lỗi cuộn hoặc mở rộng.
- Resize cửa sổ sau khi cuộn không làm ngày và timeline lệch nhau; vị trí cuộn hiện có được duy trì.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Calendar MUST cho phép người dùng cuộn ngang sang trái và phải khi phạm vi ngày vượt quá vùng hiển thị.
- **FR-002**: Calendar MUST tự mở rộng phạm vi ngày theo hướng người dùng đang cuộn khi họ đến gần biên hiện tại, cho phép tiếp tục xem các ngày quá khứ hoặc tương lai mà không cần thao tác tải thêm thủ công.
- **FR-003**: Header ngày, lưới và timeline của mọi hàng ticket MUST dùng cùng một vị trí cuộn ngang tại mọi thời điểm.
- **FR-004**: Cuộn ngang MUST hỗ trợ tương tác cuộn ngang thông thường của môi trường desktop, bao gồm trackpad hoặc wheel ngang khi có sẵn; thao tác cuộn dọc của danh sách ticket MUST tiếp tục hoạt động.
- **FR-005**: Calendar MUST giữ vị trí khởi tạo hiện tại với ngày hiện tại được đưa vào vùng nhìn thấy.
- **FR-006**: Calendar MUST giữ lại các ngày đã được thêm khi phạm vi mở rộng, để người dùng có thể cuộn ngược lại xem chúng; việc mở rộng MUST không đổi thứ tự ngày, dữ liệu ticket hoặc căn chỉnh timeline.
- **FR-007**: Khi phạm vi ngày chưa vượt quá vùng hiển thị, calendar MUST hiển thị bình thường mà không tạo khoảng trống cuộn giả; ngày mới chỉ được thêm khi cần để tiếp tục cuộn.
- **FR-008**: Hành vi cuộn ngang và mở rộng phạm vi MUST hoạt động khi danh sách ticket rỗng và vẫn giữ nguyên thông báo/trạng thái calendar hiện có.

### Key Entities

- **Trục ngày Calendar**: Phạm vi ngày quá khứ, hiện tại và tương lai được hiển thị trong calendar.
- **Vị trí cuộn ngang**: Vị trí dùng chung xác định phần trục ngày, header và timeline đang nhìn thấy.
- **Hàng timeline ticket**: Các đoạn timeline được căn chỉnh theo ngày trên trục Calendar.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Khi tiếp tục cuộn về quá khứ hoặc tương lai, calendar tự thêm ngày theo hướng đó và người dùng tiếp tục cuộn được mà không gặp biên cuối cố định.
- **SC-002**: Trong 100% trạng thái cuộn được kiểm thử, nhãn ngày trong header căn đúng với cột timeline tương ứng.
- **SC-003**: Cuộn nhanh và đổi hướng liên tục không tạo trạng thái lệch giữa header và các hàng ticket.
- **SC-004**: Với phạm vi ngày không rộng hơn vùng hiển thị, toàn bộ ngày vẫn nhìn thấy mà không có vùng cuộn thừa trước khi cần mở rộng.
- **SC-005**: Việc cuộn ngang không làm giảm khả năng cuộn dọc danh sách ticket.

## Assumptions

- Phạm vi ngày ban đầu và vị trí khởi tạo hiện tại được giữ nguyên; khi người dùng cuộn đến biên, calendar tự mở rộng phạm vi về hướng đó để tiếp tục xem thêm ngày.
- Ngày hiện tại tiếp tục là điểm bắt đầu nhìn thấy khi mở calendar lần đầu.
- Các cử chỉ cuộn ngang tiêu chuẩn mà thiết bị desktop cung cấp được xem là cách tương tác mặc định; không yêu cầu thêm nút điều hướng ngày.
