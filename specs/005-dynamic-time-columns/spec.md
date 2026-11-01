# Feature Specification: Thời gian động và ước lượng công việc

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Ở mục bảng đã lưu sẽ có thêm một cột là Dynamic Start Time. Cột này sẽ được cập nhật chỉ lần đầu tiên khi task được chuyển trạng thái sang In Progress; khi trạng thái sang In Progress, lấy dữ liệu từ cột Updated. Thêm một cột tên là Dynamic Due Date, bằng Dynamic Start Time cộng với Original Expected Time (ví dụ 15m = 15 minutes, 1h = 1 hour, 1D = 1 day, 1s = 1 second, ...). Hiện tại chưa có cột Original Expected Time; lấy thêm dữ liệu này từ Jira cho tất cả các bảng."

## Clarifications

### Session 2026-10-31

- Q: Với task trong Bảng đã lưu, Dynamic Start Time nên lấy Updated ở lần đầu ứng dụng thấy task ở trạng thái In Progress khi tạo/làm mới bảng, hay lấy thời điểm chuyển trạng thái đầu tiên thực tế từ lịch sử Jira? → A: Dùng Updated từ lần đầu ứng dụng quan sát task là In Progress khi tạo hoặc làm mới bảng; không khôi phục thời điểm chuyển trạng thái lịch sử.
- Q: Nếu cùng một Jira task xuất hiện trong nhiều Bảng đã lưu, Dynamic Start Time nên được dùng chung giữa các bảng hay được theo dõi riêng trong từng bảng? → A: Dùng chung một Dynamic Start Time cho cùng Jira task ở mọi Bảng đã lưu.
- Q: Nếu task bị loại khỏi kết quả của mọi Bảng đã lưu sau khi làm mới rồi xuất hiện lại sau này, ứng dụng nên giữ Dynamic Start Time đã ghi nhận hay bắt đầu ghi nhận lại? → A: Giữ Dynamic Start Time theo Jira task dù task tạm thời không nằm trong bảng nào; dùng lại nếu task xuất hiện lại.
- Q: Nếu Jira dùng một tên trạng thái tùy chỉnh nhưng xếp trạng thái đó vào nhóm In Progress, ứng dụng có nên ghi nhận Dynamic Start Time cho task đó không? → A: Có; tính mọi trạng thái thuộc nhóm In Progress của Jira, kể cả khi tên hiển thị tùy chỉnh.
- Q: Khi tính Dynamic Due Date, một giá trị `1d` hoặc `1w` nên được cộng theo thời gian lịch liên tục hay theo ngày/tuần làm việc của Jira? → A: Cộng theo các phiên 08:00-12:00 và 13:00-17:00; tuần làm việc là thứ Hai-thứ Sáu và múi giờ lấy từ tài khoản Jira đang kết nối.
- Q: Một ngày làm việc được tính theo khung giờ nào và có nghỉ giữa ngày không? → A: Thời gian làm việc là 08:00-12:00 và 13:00-17:00, nghỉ trưa 12:00-13:00, tổng cộng 8 giờ làm việc mỗi ngày.
- Q: Jira Cloud API không cung cấp danh sách thứ làm việc tùy chỉnh cho tài khoản thông thường; nên dùng lịch nào và múi giờ nào khi tính Dynamic Due Date? → A: Dùng tuần làm việc chuẩn từ thứ Hai đến thứ Sáu (5 ngày); dùng múi giờ của tài khoản Jira đang kết nối. Không hỗ trợ ngày nghỉ lễ hoặc lịch làm việc tùy chỉnh trong phiên bản này.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem Original Expected Time ở mọi bảng (Priority: P1)

Người dùng xem kết quả truy vấn hoặc một bảng đã lưu và muốn biết thời lượng ước tính ban đầu của từng Jira task ngay trong bảng, không cần mở từng ticket trên Jira.

**Why this priority**: Original Expected Time là dữ liệu đầu vào cần thiết để người dùng hiểu ước lượng của task và để tính Dynamic Due Date. Đây cũng là phần áp dụng rộng nhất vì cần có mặt trong mọi bảng.

**Independent Test**: Chạy truy vấn có task gồm cả task có và không có Original Estimate; xác nhận cột Original Expected Time xuất hiện ở bảng kết quả, sau đó lưu và làm mới một bảng đã lưu để xác nhận cột tiếp tục có dữ liệu đúng với Jira.

**Acceptance Scenarios**:

1. **Given** Jira trả về task có Original Estimate, **When** người dùng xem bảng kết quả truy vấn, **Then** bảng hiển thị cột Original Expected Time với giá trị Jira cung cấp.
2. **Given** người dùng lưu kết quả truy vấn hoặc làm mới bảng đã lưu, **When** Jira trả về Original Estimate cho các task, **Then** bảng đã lưu hiển thị giá trị tương ứng của từng task.
3. **Given** Jira không có Original Estimate cho một task, **When** task xuất hiện trong bất kỳ bảng nào, **Then** cột vẫn hiện diện và giá trị của task đó để trống.

---

### User Story 2 - Ghi nhận Dynamic Start Time lần đầu task vào In Progress (Priority: P2)

Người dùng xem bảng đã lưu và muốn biết thời điểm bắt đầu theo dõi của một task. Khi task lần đầu được nhận diện ở trạng thái In Progress, bảng ghi nhận giá trị Updated tại thời điểm đó làm Dynamic Start Time và giữ nguyên giá trị này về sau.

**Why this priority**: Dynamic Start Time là mốc gốc để tính Dynamic Due Date. Việc chỉ ghi nhận một lần giúp mốc không bị đẩy về sau bởi các chỉnh sửa tiếp theo của ticket.

**Independent Test**: Lưu một task chưa In Progress, làm mới bảng sau khi trạng thái chuyển sang In Progress và xác nhận Dynamic Start Time bằng Updated của lần làm mới đó; làm mới thêm sau khi Updated đổi, rồi chuyển task ra khỏi và trở lại In Progress, xác nhận Dynamic Start Time ban đầu không đổi.

**Acceptance Scenarios**:

1. **Given** một task trong bảng đã lưu chưa có Dynamic Start Time, **When** task được nhận diện lần đầu ở trạng thái In Progress trong dữ liệu Jira nhận được khi lưu hoặc làm mới, **Then** Dynamic Start Time được đặt bằng Updated của cùng dữ liệu Jira đó.
2. **Given** một task đã có Dynamic Start Time, **When** task được cập nhật thêm trong Jira hoặc rời khỏi rồi quay lại In Progress, **Then** Dynamic Start Time ban đầu vẫn được giữ nguyên.
3. **Given** task đã ở In Progress khi lần đầu được đưa vào bảng đã lưu, **When** bảng được tạo hoặc làm mới, **Then** Dynamic Start Time được khởi tạo bằng Updated hiện có ở lần quan sát đầu tiên.
4. **Given** task lần đầu được nhận diện là In Progress nhưng chưa có giá trị Updated hợp lệ, **When** bảng được làm mới sau đó và task vẫn ở In Progress với Updated hợp lệ, **Then** Dynamic Start Time được khởi tạo bằng Updated đó.
5. **Given** bảng đã lưu có Dynamic Start Time, **When** người dùng đóng rồi mở lại ứng dụng hoặc làm mới bảng thành công, **Then** giá trị đã ghi nhận vẫn được giữ.

---

### User Story 3 - Xem Dynamic Due Date theo ước lượng ban đầu (Priority: P3)

Người dùng muốn biết ngày giờ dự kiến hoàn thành của task ngay trong bảng đã lưu. Ứng dụng tính Dynamic Due Date bằng Dynamic Start Time cộng với Original Expected Time của task.

**Why this priority**: Giá trị này kết hợp mốc bắt đầu và ước lượng ban đầu thành thông tin có thể dùng trực tiếp để theo dõi tiến độ. Nó phụ thuộc vào hai dữ liệu của các user story trước.

**Independent Test**: Tạo bảng đã lưu có task với Updated và các Original Estimate khác nhau; xác nhận Dynamic Due Date bằng Dynamic Start Time cộng đúng thời lượng, gồm các đơn vị đơn lẻ và tổ hợp.

**Acceptance Scenarios**:

1. **Given** task có Dynamic Start Time và Original Expected Time hợp lệ, **When** người dùng xem bảng đã lưu, **Then** Dynamic Due Date bằng thời điểm bắt đầu cộng tổng thời lượng ước tính.
2. **Given** Original Expected Time thay đổi trên Jira, **When** người dùng làm mới bảng thành công, **Then** giá trị ước lượng được cập nhật và Dynamic Due Date được tính lại từ Dynamic Start Time đã giữ nguyên.
3. **Given** task thiếu Dynamic Start Time, thiếu Original Expected Time, hoặc có thời lượng không nhận diện được, **When** người dùng xem bảng đã lưu, **Then** Dynamic Due Date để trống và dữ liệu của task vẫn hiển thị bình thường.
4. **Given** bảng đã lưu có các cột thời gian mới, **When** người dùng xem hoặc tùy chỉnh cột, **Then** Dynamic Start Time và Dynamic Due Date có mặt trong bảng và có thể được ẩn/hiện theo cơ chế cột hiện có.

### Edge Cases

- Original Expected Time vắng mặt: cột vẫn hiện diện nhưng giá trị để trống; Dynamic Due Date để trống nếu không có thời lượng để cộng.
- Giá trị Original Expected Time không đúng định dạng thời lượng được hỗ trợ hoặc có đơn vị lạ: giữ giá trị Jira để hiển thị, không tính Dynamic Due Date và không làm hỏng việc tải/làm mới các task khác.
- Giá trị thời lượng bằng 0: được xem là hợp lệ; Dynamic Due Date bằng Dynamic Start Time.
- Dynamic Start Time rơi ngoài giờ/ngày làm việc: thời lượng bắt đầu được cộng từ kỳ làm việc tiếp theo theo lịch Jira.
- Task chưa In Progress: Dynamic Start Time và Dynamic Due Date để trống, trừ khi Dynamic Start Time đã được ghi nhận trước đó.
- Bảng đã lưu cũ chưa có các giá trị mới: vẫn mở và xem được; dữ liệu Original Expected Time và Dynamic Start Time được bổ sung khi bảng được làm mới thành công.
- Làm mới thất bại: giữ nguyên dữ liệu bảng cùng Dynamic Start Time hiện có; không ghi nhận một giá trị mới.
- Một task xuất hiện trong nhiều bảng đã lưu: mọi bảng dùng chung Dynamic Start Time của Jira task đó.
- Một task tạm thời không xuất hiện trong bất kỳ Bảng đã lưu nào sau khi làm mới thành công: giữ Dynamic Start Time đã ghi nhận theo Jira task để dùng lại nếu task xuất hiện trong bảng sau này.
- Ngày giờ Updated không có hoặc không hợp lệ khi cần khởi tạo Dynamic Start Time: để trống và thử khởi tạo ở lần nhận diện In Progress sau có Updated hợp lệ.
- Múi giờ tài khoản Jira không có hoặc không hợp lệ: Dynamic Due Date để trống và ứng dụng thông báo rằng không thể tính ngày do thiếu múi giờ; Dynamic Start Time và các dữ liệu khác vẫn hiển thị.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ứng dụng PHẢI hiển thị cột Original Expected Time trong bảng kết quả truy vấn và mọi bảng đã lưu, bao gồm cả bảng mới tạo và bảng đã làm mới.
- **FR-002**: Với mỗi Jira task, ứng dụng PHẢI lấy và giữ giá trị Original Estimate do Jira cung cấp để hiển thị dưới tên Original Expected Time; khi không có giá trị, ô tương ứng PHẢI để trống.
- **FR-003**: Cột Original Expected Time PHẢI được bật mặc định trong các bảng; việc bổ sung cột KHÔNG ĐƯỢC làm mất dữ liệu hoặc thay đổi số lượng task hiện có.
- **FR-004**: Trong Bảng đã lưu, ứng dụng PHẢI hiển thị cột Dynamic Start Time và cột Dynamic Due Date, bật mặc định và cho phép người dùng ẩn/hiện chúng bằng điều khiển cột hiện có.
- **FR-005**: Khi một task trong Bảng đã lưu lần đầu được ứng dụng quan sát ở trạng thái thuộc nhóm In Progress của Jira trong lần tạo hoặc làm mới bảng, ứng dụng PHẢI đặt Dynamic Start Time bằng Updated của task trong chính lần dữ liệu đó; không dùng thời điểm chuyển trạng thái lịch sử.
- **FR-006**: Ứng dụng CHỈ ĐƯỢC đặt Dynamic Start Time một lần cho mỗi Jira task, PHẢI dùng chung giá trị đó ở mọi Bảng đã lưu có task này và PHẢI giữ mốc ngay cả khi task tạm thời không nằm trong bảng nào; các lần làm mới sau, thay đổi Updated, hoặc rời rồi trở lại In Progress KHÔNG ĐƯỢC ghi đè giá trị đã đặt.
- **FR-007**: Nếu task đã ở trạng thái thuộc nhóm In Progress của Jira khi lần đầu được thêm vào bảng đã lưu, ứng dụng PHẢI dùng Updated của lần đầu quan sát task đó làm Dynamic Start Time. Nếu chưa có Updated hợp lệ, ứng dụng PHẢI giữ giá trị trống và thử lại khi nhận được Updated hợp lệ ở lần sau.
- **FR-008**: Dynamic Start Time PHẢI được giữ lại sau khi ứng dụng đóng/mở, sau mỗi lần làm mới thành công và trong thời gian task tạm vắng khỏi mọi bảng; nếu task xuất hiện lại, ứng dụng PHẢI dùng mốc đã lưu. Làm mới thất bại PHẢI giữ nguyên toàn bộ dữ liệu cũ.
- **FR-009**: Dynamic Due Date PHẢI được tính bằng cách cộng Original Expected Time vào Dynamic Start Time theo tuần làm việc từ thứ Hai đến thứ Sáu, với các khoảng 08:00-12:00 và 13:00-17:00; chỉ thời gian trong các khoảng này được tính. Múi giờ PHẢI lấy từ tài khoản Jira đang kết nối. Khi Original Expected Time được cập nhật từ Jira, ứng dụng PHẢI tính lại Dynamic Due Date mà không thay đổi Dynamic Start Time đã ghi nhận. Nếu múi giờ không có hoặc không hợp lệ, ứng dụng PHẢI để trống Dynamic Due Date và thông báo rõ lý do mà không làm mất dữ liệu task.
- **FR-010**: Ứng dụng PHẢI nhận diện thời lượng không âm với đơn vị `s` (giây), `m` (phút), `h` (giờ), `d` (ngày) và `w` (tuần), không phân biệt chữ hoa/thường; PHẢI hỗ trợ số nguyên hoặc số thập phân dùng dấu chấm và một hay nhiều thành phần như `15m`, `1h`, `1D`, `1s`, `0.5h`, `1d 2h`. `1d` PHẢI bằng 8 giờ làm việc; `1w` PHẢI bằng 40 giờ làm việc (5 ngày × 8 giờ). Khi cộng mọi đơn vị, thời gian ngoài các khoảng 08:00-12:00 và 13:00-17:00 hoặc ngoài thứ Hai đến thứ Sáu KHÔNG ĐƯỢC tính vào Dynamic Due Date.
- **FR-011**: Nếu Dynamic Start Time hoặc Original Expected Time bị thiếu, hoặc Original Expected Time không phân tích được, ứng dụng PHẢI để Dynamic Due Date trống; giá trị Original Expected Time nguyên gốc vẫn PHẢI được hiển thị nếu Jira cung cấp.
- **FR-012**: Với bảng đã lưu được tạo từ phiên bản trước, ứng dụng PHẢI giữ khả năng mở/xem dữ liệu hiện có; các giá trị chưa có chỉ được bổ sung từ Jira khi làm mới thành công.
- **FR-013**: Việc thêm các cột mới KHÔNG ĐƯỢC làm thay đổi các hành vi hiện có của bảng: xem, sắp xếp, ẩn/hiện cột, lưu và làm mới.

### Key Entities *(include if feature involves data)*

- **Original Expected Time**: Ước lượng thời gian ban đầu của một Jira task, được hiển thị trong mọi bảng dưới dạng giá trị Jira cung cấp.
- **Dynamic Start Time**: Mốc thời gian dùng chung cho một Jira task trong mọi Bảng đã lưu, lấy từ Updated khi ứng dụng lần đầu nhận diện task là In Progress; không thay đổi sau khi đã ghi nhận.
- **Dynamic Due Date**: Thời điểm dự kiến hoàn thành của task trong Bảng đã lưu, được tính từ Dynamic Start Time cộng với Original Expected Time.
- **Jira task trong bảng đã lưu**: Bản ghi task trong một bảng riêng, gồm dữ liệu Jira mới nhất và tham chiếu tới Dynamic Start Time dùng chung theo định danh Jira task.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong 100% bảng kết quả truy vấn và bảng đã lưu được kiểm tra, cột Original Expected Time xuất hiện; mọi giá trị do Jira cung cấp khớp với giá trị hiển thị, còn giá trị thiếu được để trống.
- **SC-002**: Trong 100% kịch bản kiểm tra chuyển trạng thái, Dynamic Start Time bằng Updated ở lần nhận diện In Progress đầu tiên và không đổi qua các lần cập nhật, rời trạng thái, quay lại trạng thái, đóng/mở ứng dụng và làm mới thành công.
- **SC-003**: Trong 100% mẫu thời lượng hợp lệ gồm giây, phút, giờ, ngày, tuần, số thập phân và tổ hợp đơn vị, Dynamic Due Date bằng Dynamic Start Time cộng đúng tổng thời lượng theo tuần làm việc thứ Hai-thứ Sáu (8 giờ/ngày, khung 08:00-12:00 và 13:00-17:00) trong múi giờ tài khoản Jira; thời gian ngoài lịch không được tính.
- **SC-004**: Trong 100% mẫu thiếu hoặc không phân tích được, Original Expected Time vẫn hiển thị nếu có dữ liệu nguồn, Dynamic Due Date để trống và các task khác vẫn xem/làm mới bình thường.
- **SC-005**: Trong một lượt thử nghiệm với ít nhất 5 người dùng, ít nhất 4 người xác định được ước lượng ban đầu, thời điểm bắt đầu và ngày giờ dự kiến hoàn thành từ cùng một bảng đã lưu mà không cần mở từng Jira task.
- **SC-006**: Việc thêm các cột không làm thay đổi số lượng task hoặc làm mất chức năng xem, sắp xếp, ẩn/hiện cột và làm mới của các bảng hiện có.

## Assumptions

- Original Expected Time là cách gọi trong ứng dụng cho trường Original Estimate của Jira.
- Dynamic Start Time và Dynamic Due Date chỉ xuất hiện trong Bảng đã lưu; Original Expected Time xuất hiện trong cả bảng kết quả truy vấn và Bảng đã lưu.
- Một Jira task được định danh bằng Jira site và issue ID ổn định; cùng issue key trên các Jira site khác nhau là các task riêng.
- Task được nhận diện là In Progress khi trạng thái Jira thuộc nhóm In Progress, kể cả khi tên trạng thái hiển thị được tùy chỉnh (ví dụ “Doing” hoặc “In Development”); trạng thái thuộc nhóm khác không được tính. Người dùng không cấu hình danh sách trạng thái riêng.
- Dynamic Start Time được ghi nhận theo snapshot: dùng Updated của lần đầu ứng dụng quan sát task là In Progress khi tạo hoặc làm mới bảng, không khôi phục thời điểm chuyển trạng thái từ lịch sử Jira. Nếu task đã ở trạng thái này trước lần quan sát đầu tiên, Updated tại snapshot đó là giá trị được dùng.
- Dynamic Start Time được lưu theo định danh Jira task và dùng chung giữa các bảng đã lưu; nếu cùng task xuất hiện trong nhiều bảng, tất cả các bảng hiển thị cùng một mốc đã ghi nhận. Mốc vẫn được giữ khi task tạm thời vắng mặt khỏi toàn bộ bảng đã lưu để có thể dùng lại khi task xuất hiện trở lại.
- Một ngày làm việc gồm 08:00-12:00 và 13:00-17:00 (8 giờ làm việc); tuần làm việc chuẩn là thứ Hai-thứ Sáu, nên `1w = 40` giờ làm việc. Múi giờ lấy từ tài khoản Jira đang kết nối; khoảng nghỉ trưa và thời gian ngoài lịch làm việc được bỏ qua. Ngày nghỉ lễ và tuần làm việc tùy chỉnh không thuộc phạm vi.
- Giá trị số thập phân trong thời lượng dùng dấu chấm làm dấu thập phân; giá trị âm hoặc định dạng khác không được tính ra Dynamic Due Date.
- Dynamic Due Date là giá trị được tính từ hai trường nguồn, không cho phép người dùng nhập hoặc ghi đè thủ công.
- Với bảng đã lưu cũ, ứng dụng không thể khôi phục Dynamic Start Time lịch sử chưa từng được lưu; nếu task đang In Progress ở lần làm mới đầu tiên sau nâng cấp, ứng dụng dùng Updated khi đó.
- Khi Jira không cung cấp giá trị ước lượng, giá trị được hiển thị để trống; tính năng không yêu cầu người dùng cấu hình thêm.
