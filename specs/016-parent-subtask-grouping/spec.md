# Feature Specification: Nhóm ticket cha và sub-task

**Feature Branch**: `016-parent-subtask-grouping`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "ở bảng kết quả truy vấn, ticket nào là ticket cha thì màu sắc của dòng đó sẽ được tô xám mờ, những sub task của ticket cha sẽ được sắp xếp phía dưới ticket cha"

## Clarifications

### Session 2026-11-01

- Q: Ticket cha có được tô xám nếu không có sub-task nào xuất hiện trong kết quả truy vấn? → A: Luôn tô xám ticket cha.
- Q: Sub-task trong bảng được nhận biết như thế nào về quan hệ trực quan với task cha? → A: Hiển thị cây thư mục trực quan dưới task cha; sub-task không cuối dùng nhánh `├`, sub-task cuối dùng nhánh `└`.
- Q: Quan hệ cha-con có được giữ nguyên khi người dùng sử dụng tính năng sort? → A: Task con luôn nằm dưới task cha; sort chỉ thay đổi thứ tự các nhóm cha hoặc ticket độc lập.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nhận biết ticket cha (Priority: P1)

Người dùng xem bảng kết quả truy vấn và nhanh chóng nhận ra các ticket cha có sub-task trong kết quả. Dòng của ticket cha được tô màu xám mờ để phân biệt với các ticket thường.

**Why this priority**: Đây là tín hiệu trực quan cốt lõi giúp người dùng hiểu cấu trúc quan hệ cha-con trong kết quả mà không cần mở từng ticket.

**Independent Test**: Khi kết quả chứa ít nhất một ticket cha, người dùng có thể xác nhận toàn bộ dòng ticket cha mang trạng thái phân biệt xám mờ.

**Acceptance Scenarios**:

1. **Given** kết quả truy vấn chứa một ticket cha, kể cả khi không có sub-task nào của cha xuất hiện trong kết quả, **When** bảng kết quả được hiển thị, **Then** toàn bộ dòng ticket cha được tô xám mờ.
2. **Given** kết quả truy vấn chỉ chứa ticket thường không phải ticket cha, **When** bảng kết quả được hiển thị, **Then** dòng ticket không bị tô xám mờ.

---

### User Story 2 - Hiển thị sub-task dưới ticket cha (Priority: P1)

Người dùng xem bảng kết quả; các sub-task thuộc cùng một ticket cha được đặt liền phía dưới ticket cha thay vì bị phân tán trong thứ tự trả về ban đầu.

**Why this priority**: Sắp xếp theo nhóm cha-con giúp người dùng theo dõi phạm vi công việc liên quan mà không phải tìm kiếm thủ công trong bảng.

**Independent Test**: Với kết quả chứa một ticket cha và các sub-task của nó xen lẫn ticket khác, người dùng có thể xác minh các sub-task thuộc cha xuất hiện thành một nhóm ngay sau ticket cha.

**Acceptance Scenarios**:

1. **Given** kết quả chứa ticket cha A và các sub-task A1, A2 cùng với ticket B, **When** bảng kết quả được hiển thị, **Then** A1 và A2 xuất hiện liên tục ngay sau A.
2. **Given** ticket cha xuất hiện sau các sub-task trong thứ tự trả về, **When** bảng được hiển thị, **Then** hệ thống đưa các sub-task nằm ngay dưới ticket cha.
3. **Given** ticket cha có hai sub-task hiển thị, **When** bảng kết quả được hiển thị, **Then** cả hai sub-task được thể hiện thành cấu trúc cây thư mục dưới cha, với sub-task đầu dùng nhánh `├` và sub-task cuối dùng nhánh `└` trước mã hoặc tiêu đề.
4. **Given** người dùng bật sort trên bất kỳ cột nào, **When** bảng được hiển thị lại, **Then** mỗi sub-task vẫn nằm trong nhóm ngay dưới task cha của nó.

---

### Edge Cases

- Sub-task thuộc ticket cha không xuất hiện trong kết quả truy vấn: hệ thống chỉ hiển thị ticket cha theo thứ tự kết quả hiện tại và không tạo dòng placeholder cho sub-task thiếu.
- Một ticket cha có nhiều sub-task trong kết quả: tất cả sub-task thuộc cha được nhóm dưới cha, không thêm một dòng cha cho mỗi sub-task.
- Các sub-task của một cha xuất hiện xen lẫn ticket không liên quan: ticket không liên quan không bị chèn vào giữa cha và các sub-task của cha.
- Ticket cha không còn nằm trong kết quả nhưng sub-task của nó có: sub-task được hiển thị theo thứ tự kết quả hiện tại và không được gán màu cha.
- Khi sort được bật, không được tách sub-task khỏi ticket cha hoặc đưa sub-task lên phía trước cha; thứ tự bên trong nhóm cha được giữ nguyên.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI xác định ticket cha dựa trên quan hệ cha-con được cung cấp trong dữ liệu kết quả truy vấn.
- **FR-002**: Hệ thống PHẢI tô xám mờ toàn bộ dòng hiển thị của ticket cha, bao gồm khi cha không có sub-task nào xuất hiện trong kết quả; giao diện PHẢI áp dụng cho toàn bộ ô dữ liệu nằm trong dòng.
- **FR-003**: Hệ thống PHẢI xác định sub-task thuộc ticket cha dựa trên quan hệ cha-con trong dữ liệu kết quả truy vấn.
- **FR-004**: Hệ thống PHẢI đặt tất cả sub-task thuộc một ticket cha liền phía dưới ticket cha trong cùng danh sách hiển thị.
- **FR-005**: Khi ticket cha xuất hiện sau sub-task của nó trong thứ tự trả về, hệ thống PHẢI sắp xếp lại để sub-task nằm dưới ticket cha.
- **FR-006**: Hệ thống PHẢI giữ thứ tự tương đối của các sub-task trong một nhóm theo thứ tự mà kết quả truy vấn trả về.
- **FR-007**: Khi ticket cha không có mặt trong kết quả nhưng sub-task có mặt, hệ thống PHẢI hiển thị sub-task theo thứ tự kết quả hiện tại và không tạo nhóm giả.
- **FR-008**: Việc sắp xếp nhóm cha-con KHÔNG ĐƯỢC thay đổi dữ liệu hoặc các cột của từng ticket.
- **FR-009**: Hệ thống PHẢI hiển thị cây thư mục trực quan cho các sub-task dưới ticket cha để người dùng nhận biết quan hệ cha-con; sub-task không phải phần tử cuối trong nhóm phải dùng nhánh `├`, và phần tử cuối phải dùng nhánh `└`.
- **FR-010**: Khi người dùng bật hoặc thay đổi sort, hệ thống PHẢI coi mỗi cha cùng các sub-task trực tiếp của nó như một nhóm không thể tách; sort PHẢI thay đổi thứ tự các nhóm cha/ticket độc lập mà KHÔNG tách sub-task khỏi cha.
- **FR-011**: Hệ thống PHẢI giữ thứ tự tương đối hiện tại của các sub-task trong cùng một nhóm khi sort được áp dụng.

### Key Entities *(include if feature involves data)*

- **Ticket kết quả**: Một dòng trong bảng kết quả truy vấn, gồm mã ticket và quan hệ cha-con nếu có.
- **Quan hệ cha-con**: Liên kết từ sub-task đến ticket cha, được dùng để xác định ticket cha và nhóm hiển thị.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% ticket cha xuất hiện trong kết quả được tô xám mờ trên dòng tương ứng.
- **SC-002**: 100% nhóm sub-task có ticket cha trong kết quả được hiển thị liên tục ngay dưới ticket cha.
- **SC-003**: 100% ticket không liên quan không xuất hiện giữa ticket cha và các sub-task thuộc cha trong cùng một nhóm.
- **SC-004**: Không có thay đổi thứ tự tương đối giữa các sub-task thuộc cùng một ticket cha so với thứ tự kết quả ban đầu.
- **SC-005**: 100% lần thực hiện sort giữ mọi sub-task ngay dưới ticket cha tương ứng và không làm chệch cấu trúc cây cha-con.

## Assumptions

- Dữ liệu kết quả truy vấn chứa quan hệ đủ để nhận diện ticket cha và mã ticket cha của sub-task.
- Chỉ các ticket cha và sub-task đều nằm trong kết quả truy vấn mới được nhóm lại; hệ thống không tải thêm ticket từ Jira để phục vụ việc hiển thị.
- Phạm vi v1 áp dụng cho bảng kết quả truy vấn hiện tại; không áp dụng thay đổi nhóm cho bảng đã lưu hoặc danh sách khác.
