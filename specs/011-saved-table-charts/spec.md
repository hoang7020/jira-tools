# Feature Specification: Đồ thị bảng đã lưu

**Feature Branch**: `011-saved-table-charts`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Trong bảng đã lưu sẽ có 3 nút Đồ thị, mở lịch, làm mới; các nút này có thiết kế giống nhau và cách nhau 5px. Khi bấm nút Đồ thị sẽ mở ra màn hình để show các loại đồ thị như pie, column, line, burn down, ... mục đích là để visualize bảng đã lưu."

## Clarifications

### Session 2026-11-01

- Q: Khi người dùng lưu một bảng lần đầu, có nên tạo điểm snapshot ban đầu cho biểu đồ burndown, ngoài các snapshot được tạo sau mỗi lần làm mới thành công không? → A: Có, tạo snapshot ban đầu khi lưu bảng; tiếp tục snapshot sau mỗi lần làm mới thành công.
- Q: Bạn muốn ứng dụng giữ lịch sử snapshot burndown của mỗi bảng trong bao lâu? → A: Giữ toàn bộ snapshot cho đến khi bảng bị xoá.
- Q: Biểu đồ cần thể hiện thông tin gì ngoài màu sắc để người dùng có thể đọc và phân biệt dữ liệu? → A: Luôn hiển thị nhãn/giá trị và chú giải; không dựa riêng vào màu.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Trực quan hoá bảng đã lưu (Priority: P1)

Người dùng mở một bảng đã lưu, chọn **Đồ thị**, rồi chọn một kiểu biểu đồ để xem phân bố hoặc xu hướng của dữ liệu trong bảng mà không phải rời ứng dụng.

**Why this priority**: Biểu đồ giúp người dùng nhận ra phân bố và xu hướng ticket nhanh hơn việc đọc từng dòng.

**Independent Test**: Mở bảng đã lưu có ticket, mở màn hình Đồ thị, lần lượt chọn từng loại biểu đồ được hỗ trợ và xác nhận dữ liệu hiển thị tương ứng với các ticket trong bảng.

**Acceptance Scenarios**:

1. **Given** một bảng đã lưu đang được chọn, **When** người dùng chọn **Đồ thị**, **Then** ứng dụng mở màn hình trực quan hoá cho đúng bảng đó.
2. **Given** màn hình Đồ thị đang mở, **When** người dùng chọn pie, column hoặc line, **Then** ứng dụng hiển thị biểu đồ với dữ liệu được tính từ các ticket của bảng đã chọn.
3. **Given** người dùng đang xem biểu đồ, **When** họ quay lại, **Then** ứng dụng trở về chi tiết đúng bảng đã lưu.
4. **Given** bảng đã lưu không có ticket, **When** người dùng mở Đồ thị, **Then** ứng dụng thông báo chưa có dữ liệu và không hiển thị biểu đồ gây hiểu nhầm.
5. **Given** biểu đồ pie hoặc column đang hiển thị, **When** người dùng chọn status, assignee hoặc priority làm trường phân nhóm, **Then** biểu đồ được tính lại theo trường đó.

### User Story 2 - Thao tác nhất quán trên bảng (Priority: P2)

Người dùng có thể mở đồ thị, mở lịch hoặc làm mới bảng qua ba nút cùng kiểu dáng và cách nhau đúng 5 px.

**Why this priority**: Ba thao tác là các hành động ngang hàng trên chi tiết bảng; tính nhất quán giúp người dùng nhận diện chúng như một nhóm.

**Independent Test**: Mở chi tiết bảng đã lưu và xác nhận ba nút cùng kiểu dáng, kích thước/chiều cao và trạng thái tương tác; khoảng cách giữa các nút là 5 px; từng nút vẫn thực hiện đúng hành động.

**Acceptance Scenarios**:

1. **Given** chi tiết bảng đã lưu đang hiển thị, **When** người dùng quan sát nhóm thao tác, **Then** **Đồ thị**, **Mở lịch** và **Làm mới** có thiết kế nhất quán và cách nhau 5 px.
2. **Given** một thao tác đang không khả dụng hoặc đang tải, **When** nút đó chuyển trạng thái, **Then** trạng thái riêng của nút được thể hiện mà kiểu dáng và khoảng cách chung của nhóm vẫn nhất quán.

### User Story 3 - Xem biểu đồ burndown (Priority: P2)

Người dùng chọn biểu đồ burndown để theo dõi lượng công việc còn lại theo thời gian cho bảng đã lưu.

**Why this priority**: Burndown được yêu cầu cụ thể và cần các mốc dữ liệu theo thời gian để người dùng theo dõi tiến độ thực tế.

**Independent Test**: Làm mới bảng nhiều lần với các trạng thái ticket thay đổi giữa các lần, sau đó xác nhận burndown trình bày lượng ticket chưa hoàn tất theo thứ tự thời gian.

**Acceptance Scenarios**:

1. **Given** người dùng chọn kiểu burndown, **When** có các snapshot từ những lần làm mới thành công, **Then** ứng dụng hiển thị số ticket chưa hoàn tất ở mỗi thời điểm snapshot.
2. **Given** bảng chưa có đủ lịch sử snapshot để thể hiện thay đổi, **When** người dùng chọn kiểu biểu đồ này, **Then** ứng dụng giải thích rằng dữ liệu lịch sử chỉ bắt đầu được ghi nhận từ khi tính năng được sử dụng và hiển thị các điểm hiện có một cách trung thực.
3. **Given** lần làm mới bảng thất bại, **When** người dùng xem burndown, **Then** lần làm mới thất bại không tạo thêm điểm lịch sử.
4. **Given** bảng được lưu lần đầu thành công, **When** người dùng xem burndown, **Then** biểu đồ có một điểm baseline tại thời điểm lưu.
5. **Given** ticket thiếu hoặc có status category không xác định, **When** burndown được tính, **Then** ticket được tính là chưa hoàn tất và số lượng category chưa xác định được chú thích.

### Edge Cases

- Bảng đã lưu không có ticket hoặc các giá trị phù hợp để phân nhóm.
- Một số ticket thiếu status, assignee, priority hoặc mốc thời gian dùng cho biểu đồ.
- Một kiểu biểu đồ không thể tính được từ dữ liệu của bảng.
- Dữ liệu bảng không đọc được hoặc đang được làm mới trong khi màn hình đồ thị mở.
- Kiểu burndown cần chuỗi lịch sử nhưng bảng mới có một hoặc chưa có snapshot.
- Thao tác lưu bảng lần đầu thất bại; không được tạo snapshot baseline cho bảng chưa được lưu.
- Một snapshot được làm mới nhưng thao tác ghi bảng thất bại; snapshot đó không được tính là dữ liệu lịch sử thành công.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Người dùng MUST có thể mở màn hình Đồ thị từ chi tiết của bảng đã lưu đang chọn.
- **FR-002**: Màn hình Đồ thị MUST chỉ trực quan hoá dữ liệu thuộc bảng đã chọn và MUST cung cấp thao tác quay lại chi tiết bảng đó.
- **FR-003**: Màn hình Đồ thị MUST cung cấp tối thiểu các kiểu pie, column, line và burndown.
- **FR-004**: Người dùng MUST có thể chọn status, assignee hoặc priority làm trường nhóm cho biểu đồ pie và column; dữ liệu thiếu MUST được gom nhóm hoặc chú thích rõ.
- **FR-005**: Biểu đồ line MUST thể hiện số ticket được cập nhật theo ngày dựa trên mốc Updated có trong bảng đã lưu và MUST mô tả rõ đại lượng được thể hiện.
- **FR-006**: Ứng dụng MUST hiển thị trạng thái giải thích được khi bảng rỗng, dữ liệu không đủ hoặc không thể tính biểu đồ; không được hiển thị dữ liệu sai lệch như dữ liệu đầy đủ.
- **FR-007**: Ứng dụng MUST tính biểu đồ từ phiên bản dữ liệu bảng đã lưu hiện hành; biểu đồ MUST cập nhật sau khi bảng được làm mới và lưu thành công.
- **FR-008**: Chi tiết bảng đã lưu MUST có đúng ba nút **Đồ thị**, **Mở lịch** và **Làm mới** trong cùng một nhóm thao tác.
- **FR-009**: Ba nút MUST dùng thiết kế và kích thước cơ bản giống nhau, và cách nhau đúng 5 px.
- **FR-010**: Khi làm mới đang chạy hoặc thao tác không khả dụng, ứng dụng MUST duy trì thứ tự, khoảng cách và tính nhất quán trực quan của nhóm nút, đồng thời biểu thị trạng thái riêng của thao tác.
- **FR-011**: Khi lưu bảng lần đầu thành công và sau mỗi lần làm mới bảng thành công, ứng dụng MUST ghi nhận một snapshot thời gian của ticket và trạng thái cần thiết để dựng burndown; thao tác lưu/làm mới thất bại MUST NOT tạo snapshot thành công.
- **FR-012**: Burndown MUST biểu diễn số lượng ticket chưa hoàn tất theo thời điểm của các snapshot đã ghi nhận; ticket thuộc trạng thái Jira có category `done` không được tính là còn lại.
- **FR-013**: Lịch sử snapshot MUST được giữ cùng vòng đời của bảng đã lưu và MUST bị xoá khi người dùng xoá bảng đó.
- **FR-014**: Khi chưa có đủ lịch sử snapshot để thể hiện biến thiên, ứng dụng MUST giải thích giới hạn dữ liệu và không gắn nhãn dữ liệu suy diễn là dữ liệu thực tế.
- **FR-015**: Phạm vi loại biểu đồ ban đầu MUST bao gồm pie, column, line và burndown; các loại bổ sung ngoài danh sách này không bắt buộc trong phiên bản đầu.
- **FR-016**: Ứng dụng MUST giữ toàn bộ lịch sử snapshot trong suốt vòng đời của bảng; không tự động giới hạn số điểm hoặc xoá snapshot cũ.
- **FR-017**: Mọi biểu đồ MUST hiển thị nhãn và giá trị dữ liệu cùng chú giải phù hợp; màu sắc MUST NOT là cách duy nhất để phân biệt nhóm hoặc trạng thái.
- **FR-018**: Ticket thiếu hoặc có status category không xác định MUST được tính là chưa hoàn tất trong burndown và số lượng đó MUST được chú thích riêng.

### Key Entities

- **Bảng đã lưu**: Ảnh chụp truy vấn gồm tên, các ticket, thời điểm lưu/làm mới và các trường ticket; là nguồn dữ liệu của biểu đồ.
- **Biểu đồ**: Cách trình bày dữ liệu của một bảng đã lưu, gồm loại biểu đồ, chiều phân nhóm/trục và các giá trị tổng hợp.
- **Chuỗi burndown**: Các điểm dữ liệu về số ticket chưa hoàn tất được ghi nhận tại thời điểm mỗi lần làm mới bảng thành công.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng mở được một trong ba kiểu biểu đồ pie, column hoặc line từ bảng đang chọn trong tối đa hai thao tác sau khi mở chi tiết bảng.
- **SC-002**: Với bảng có dữ liệu hợp lệ, số lượng ticket trong các nhóm biểu đồ khớp với số ticket tương ứng; mọi nhóm bị loại hoặc thiếu đều được giải thích.
- **SC-003**: Trong kiểm tra giao diện, cả ba nút thao tác có cùng chiều cao và kiểu trình bày; khoảng cách giữa từng cặp liền kề đo được là 5 px.
- **SC-004**: Người dùng quay từ màn hình Đồ thị về đúng bảng đang chọn mà không mất trạng thái của bảng.
- **SC-005**: Khi dữ liệu không đủ để dựng một biểu đồ, người dùng được thông báo nguyên nhân thay vì thấy biểu đồ trống không giải thích hoặc số liệu bịa đặt.
- **SC-006**: Khi lưu bảng lần đầu thành công và sau mỗi lần làm mới thành công, có thêm tối đa một điểm snapshot burndown cho bảng; lần lưu/làm mới thất bại không làm tăng số điểm.
- **SC-007**: Người dùng có thể đối chiếu mỗi điểm burndown với thời điểm làm mới và số ticket chưa hoàn tất của snapshot tương ứng.
- **SC-008**: Người dùng có thể nhận biết nhóm dữ liệu và giá trị của từng biểu đồ mà không cần dựa riêng vào màu sắc.

## Assumptions

- Pie/column mặc định đếm ticket theo một trường phân loại có sẵn; line mặc định tổng hợp số ticket theo mốc thời gian đã lưu.
- Burndown lưu một snapshot baseline khi lưu bảng lần đầu thành công và thêm snapshot tại mỗi lần làm mới thành công; lịch sử cũ trước khi hỗ trợ snapshot không được tạo ngược lại.
- Burndown dùng số ticket chưa hoàn tất làm đơn vị công việc; các trạng thái thuộc category `done` được coi là hoàn tất.
- Ticket thiếu hoặc có status category không xác định được xem là chưa xác nhận hoàn tất, được tính vào remaining và được ghi chú riêng trên burndown.
- Lịch sử snapshot được giữ toàn bộ cho đến khi bảng đã lưu bị xoá; không tự động cắt bớt theo số lượng hay thời gian.
- Cấu hình tuỳ ý công thức, nhiều trục hoặc xuất biểu đồ nằm ngoài phạm vi ban đầu.
