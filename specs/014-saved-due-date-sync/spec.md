# Đặc tả: Đồng bộ Due Date từ bảng đã lưu

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-11-01

**Status**: Draft

**Input**: Yêu cầu người dùng: "ở bảng đã lưu, nếu due date không có value, thì sẽ lấy giá trị ở Dynamic Due Date để cập nhật lên Jira. Nếu có sự khác nhau giữa dynamic due date và due date thì sẽ có nút cập nhật nhỏ hiển thị tại dòng không chính xác, khi bấm vào nút cập nhật thì due date sẽ được cập nhật lên Jira"

## Clarifications

### Session 2026-11-01

- Q: Sau khi Jira xác nhận cập nhật Due Date thành công, bảng đã lưu nên xử lý giá trị Due Date trên hàng đó như thế nào? → A: Cập nhật snapshot đã lưu với ngày đích; nút cập nhật không hiện lại khi mở bảng sau này.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cập nhật Due Date Jira từ bảng đã lưu (Priority: P1)

Người dùng rà soát bảng đã lưu để so sánh Due Date chuẩn của Jira với Dynamic Due Date của từng issue. Khi ngày Due Date trên Jira bị thiếu hoặc không khớp với ngày Dynamic Due Date, người dùng có thể cập nhật Due Date của đúng issue lên Jira bằng nút cập nhật tại hàng đó.

**Why this priority**: Đồng bộ ngày đến hạn theo lịch làm việc dự kiến giúp các issue trên Jira phản ánh đúng hạn xử lý và tránh cập nhật nhầm ngày hoặc nhầm issue.

**Independent Test**: Mở bảng đã lưu có issue với Due Date trống, Due Date khác Dynamic Due Date, hai ngày khớp nhau, và issue không có Dynamic Due Date. Xác nhận chỉ hai trường hợp đầu có nút cập nhật; bấm nút ở một hàng và xác nhận Jira nhận đúng issue cùng ngày đích; xác nhận hàng khớp và hàng không có Dynamic Due Date không bị cập nhật.

**Acceptance Scenarios**:

1. **Given** issue trong bảng đã lưu có Dynamic Due Date nhưng Due Date Jira đang trống, **When** người dùng xem hàng đó, **Then** hàng hiển thị nút cập nhật với ngày đích lấy từ Dynamic Due Date.
2. **Given** issue có cả hai ngày nhưng Due Date khác ngày lịch của Dynamic Due Date, **When** người dùng xem hàng đó, **Then** chỉ hàng không khớp hiển thị nút cập nhật và ngày đích được thể hiện rõ.
3. **Given** Due Date Jira bằng ngày lịch Dynamic Due Date, **When** bảng được hiển thị, **Then** hàng không hiển thị nút cập nhật.
4. **Given** issue không có Dynamic Due Date khả dụng, **When** bảng được hiển thị, **Then** ứng dụng không gửi cập nhật và không hiển thị nút cập nhật cho issue đó.
5. **Given** người dùng bấm nút cập nhật tại một hàng sai lệch, **When** Jira chấp nhận cập nhật, **Then** Due Date của đúng issue được đặt thành ngày lịch Dynamic Due Date, snapshot bảng đã lưu được cập nhật với ngày đích, và nút cập nhật không hiện lại khi mở bảng lần sau.
6. **Given** cập nhật Jira thất bại, **When** kết quả lỗi được trả về, **Then** người dùng được thông báo lỗi, giá trị chưa được xem là đã đồng bộ, và có thể thử lại.
7. **Given** một cập nhật đang được xử lý, **When** người dùng tương tác với hàng đó, **Then** thao tác cập nhật lặp trên cùng issue bị ngăn cho đến khi yêu cầu kết thúc; các hàng khác tiếp tục hoạt động độc lập.

### Edge Cases

- Due Date Jira null được xem là không khớp khi Dynamic Due Date có ngày hợp lệ.
- Dynamic Due Date chứa cả ngày và giờ; đối chiếu và gửi cập nhật dùng phần ngày lịch theo múi giờ Jira của bảng, không chuyển ngày theo múi giờ máy người dùng.
- Dynamic Due Date không thể quy đổi thành ngày lịch hợp lệ: không hiện nút và không gửi yêu cầu cập nhật.
- Mất kết nối, thông tin xác thực hết hạn, thiếu quyền sửa issue hoặc Jira trả lỗi: báo lỗi rõ ràng và không thể hiện thành công giả.
- Nếu dữ liệu bảng đã lưu không còn phản ánh Jira hiện tại, thao tác cập nhật vẫn chỉ dùng ngày đích đã thể hiện cho hàng; người dùng có thể làm mới bảng để nạp trạng thái mới nhất.
- Việc cập nhật một issue không làm thay đổi Dynamic Due Date hoặc các issue khác trong bảng.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Bảng đã lưu MUST đối chiếu Due Date chuẩn của Jira với ngày lịch của Dynamic Due Date cho từng issue.
- **FR-002**: Khi Dynamic Due Date có ngày hợp lệ và Due Date Jira đang trống hoặc khác ngày đó, bảng MUST hiển thị nút cập nhật nhỏ tại đúng hàng issue sai lệch.
- **FR-003**: Nút cập nhật MUST cho người dùng biết ngày đích sẽ được đặt lên Jira trước khi thao tác được thực hiện.
- **FR-004**: Ứng dụng MUST NOT tự ghi cập nhật Due Date lên Jira chỉ vì bảng được mở, tải hoặc hiển thị; chỉ thao tác bấm nút của người dùng mới khởi chạy cập nhật.
- **FR-005**: Khi người dùng xác nhận cập nhật tại một hàng, ứng dụng MUST cập nhật trường Due Date chuẩn của đúng issue trên Jira thành ngày lịch Dynamic Due Date hiển thị cho hàng đó.
- **FR-006**: Khi Due Date Jira khớp ngày lịch Dynamic Due Date hoặc Dynamic Due Date không có ngày hợp lệ, ứng dụng MUST NOT hiển thị nút cập nhật cho hàng đó.
- **FR-007**: Đối chiếu và cập nhật MUST dùng ngày lịch theo múi giờ Jira của bảng đã lưu; không được chuyển đổi theo múi giờ thiết bị làm lệch ngày.
- **FR-008**: Trong khi cập nhật một issue, ứng dụng MUST ngăn gửi nhiều yêu cầu cập nhật đồng thời cho cùng issue và MUST giữ khả năng cập nhật các issue khác.
- **FR-009**: Chỉ sau khi Jira xác nhận cập nhật thành công, ứng dụng MUST cập nhật Due Date trong snapshot của đúng hàng thành ngày đích và thể hiện hàng đã đồng bộ; sau khi thất bại, MUST giữ nguyên snapshot, thông báo lỗi và cho phép thử lại.
- **FR-010**: Cập nhật MUST NOT thay đổi Dynamic Due Date, Due Date của issue khác, hoặc các trường không liên quan.
- **FR-011**: Nếu Jira từ chối cập nhật do lỗi kết nối, xác thực, quyền hạn hoặc lỗi máy chủ, ứng dụng MUST thông báo lỗi cụ thể theo khả năng hiện có và MUST NOT báo thành công.

### Key Entities

- **Issue trong bảng đã lưu**: Issue snapshot có Jira key/ID, Due Date chuẩn có thể trống, và Dynamic Due Date có thể không khả dụng.
- **Ngày đồng bộ mục tiêu**: Ngày lịch được lấy từ Dynamic Due Date theo múi giờ Jira của bảng; đây là giá trị dùng để đối chiếu và cập nhật lên Jira.
- **Trạng thái cập nhật**: Trạng thái theo issue gồm chưa cần cập nhật, cần cập nhật, đang cập nhật, cập nhật thành công, hoặc cập nhật thất bại có thể thử lại.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% hàng có Dynamic Due Date hợp lệ nhưng Due Date Jira trống hoặc sai ngày lịch hiển thị nút cập nhật tại đúng issue.
- **SC-002**: 100% hàng có ngày lịch khớp hoặc không có Dynamic Due Date hợp lệ không phát sinh cập nhật và không hiện nút.
- **SC-003**: Trong mọi lần cập nhật thành công, Jira nhận đúng issue và ngày lịch đích được người dùng chọn; không có issue khác bị thay đổi.
- **SC-004**: 100% lỗi cập nhật được thể hiện cho người dùng và không bị trình bày như thành công.
- **SC-005**: Người dùng có thể cập nhật một hàng mà không làm chặn thao tác trên các hàng còn lại.

## Assumptions

- Nút cập nhật thực hiện cập nhật ngay sau khi bấm; không cần hộp thoại xác nhận riêng vì nút ở đúng hàng nêu rõ ngày đích.
- Mục tiêu cập nhật là trường Due Date chuẩn của Jira, không phải Dynamic Due Date hoặc Real Due Date.
- Dynamic Due Date được chuyển thành ngày lịch bằng múi giờ Jira đã lưu với bảng; nếu không có múi giờ hoặc giá trị không hợp lệ, ứng dụng không đoán ngày và không đưa ra nút cập nhật.
- Thao tác cập nhật cần kết nối Jira hợp lệ và tài khoản hiện tại có quyền sửa issue.
- Phạm vi là các issue trong một Bảng đã lưu; không cập nhật hàng loạt hoặc sửa JQL.
