# Feature Specification: Calendar cho Bảng đã lưu

**Feature Branch**: `N/A (workspace không có Git metadata)`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Trong mục Bảng đã lưu, thêm một nút để mở calendar riêng. Ngày và thứ dàn theo hàng ngang; mã ticket xếp theo hàng dọc. Mỗi task có đường lịch trình từ ngày Dynamic Start Time đến ngày Dynamic Due Date."

## Clarifications

### Session 2026-11-01

- Q: Calendar nên được hiển thị cạnh bảng kết quả hay mở riêng, và ngày cùng mã ticket được bố trí theo hướng nào? → A: Thêm nút trong Bảng đã lưu để mở calendar riêng; bảng lịch chỉ có cột mã ticket cố định ở bên trái và cột calendar cuộn ngang ở bên phải, với ticket xếp theo hàng dọc và ngày nằm ngang.
- Q: Calendar nên hiển thị phạm vi ngày nào khi các task trải dài nhiều tháng? → A: Dùng cửa sổ lịch cố định theo ngày hiện tại trong múi giờ Jira đã lưu của bảng, từ cùng ngày của tháng trước đến cùng ngày của tháng thứ ba sau đó, tính cả hai đầu; nếu ngày đó không tồn tại ở tháng đích thì dùng ngày cuối tháng.
- Q: Trục ngày có chỉ cần hiển thị hai mốc đầu/cuối hay phải hiển thị từng ngày ở giữa? → A: Phải hiển thị ô riêng cho mọi ngày liên tiếp trong cửa sổ lịch, không chỉ hai mốc đầu/cuối hoặc bỏ qua ngày trung gian; người dùng có thể cuộn ngang để xem toàn bộ cửa sổ.
- Q: Khi một task bắt đầu trước hoặc kết thúc sau giới hạn cuộn của calendar, thanh lịch trình phải được xử lý thế nào? → A: Giới hạn là cứng; giữ mọi hàng ticket, cắt thanh giao với khung tại mép và đánh dấu phần tiếp diễn bên ngoài, còn task hoàn toàn ngoài khung không có thanh và có thông báo ngoài phạm vi đang hiển thị.
- Q: Khi múi giờ hợp lệ nhưng bảng rỗng hoặc không có task nào có khoảng ngày hợp lệ, có ẩn trục ngày không? → A: Không; vẫn hiển thị cửa sổ ngày cố định và các hàng ticket hiện có, không vẽ thanh, đồng thời hiển thị giải thích rằng không có khoảng hợp lệ.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Theo dõi task trên calendar (Priority: P1)

Người dùng chọn một bảng đã lưu và mở calendar bằng nút dành riêng. Trong calendar riêng này, mỗi task có một hàng mang mã ticket; cột mã ticket luôn cố định bên trái, còn cột ngày cuộn ngang bên phải. Mỗi ngày trong cửa sổ từ một tháng trước đến ba tháng sau ngày hiện tại có ô riêng; thanh thể hiện khoảng Dynamic Start Time–Dynamic Due Date trong cửa sổ và đánh dấu phần tiếp diễn nếu khoảng vượt giới hạn.

**Why this priority**: Calendar giúp người dùng nhận biết khoảng thời gian dự kiến của nhiều task trong cùng một lần xem, thay vì phải so sánh từng giá trị ngày trong bảng.

**Independent Test**: Chọn một bảng đã lưu có các task nằm trong, cắt qua và nằm ngoài cửa sổ ngày; mở calendar và xác nhận cột mã ticket cố định, mỗi ngày liên tiếp trong cửa sổ có ô riêng, cuộn ngang không vượt giới hạn, thanh và dấu tiếp diễn đúng vị trí, và task ngoài cửa sổ vẫn có hàng cùng thông báo.

**Acceptance Scenarios**:

1. **Given** người dùng đã chọn một bảng đã lưu, **When** người dùng nhấn nút mở calendar, **Then** calendar của bảng đó mở thành một chế độ xem riêng, không hiển thị ghép cạnh bảng kết quả truy vấn.
2. **Given** calendar của một bảng đã lưu đang mở, **When** các ngày và task được hiển thị, **Then** calendar có cột mã ticket cố định bên trái và cột ngày cuộn ngang bên phải; mỗi ngày trong cửa sổ hiển thị có một ô riêng theo thứ tự liên tục với thứ cùng ngày/tháng/năm, kể cả mọi ngày ở giữa hai mốc của task.
3. **Given** một task có Dynamic Start Time và Dynamic Due Date hợp lệ giao với cửa sổ hiển thị, **When** calendar được hiển thị, **Then** thanh bao phủ các ngày nằm trong cửa sổ từ ngày bắt đầu đến ngày kết thúc; nếu khoảng task vượt mép cửa sổ, thanh được cắt ở mép và có dấu cho biết còn tiếp diễn bên ngoài.
4. **Given** ngày bắt đầu và ngày kết thúc của một task cùng thuộc một ngày, **When** calendar được hiển thị, **Then** task vẫn có dấu chỉ khoảng thời gian nhìn thấy được tại ngày đó.
5. **Given** một task thiếu hoặc có ngày bắt đầu/ngày kết thúc không hợp lệ, **When** calendar được hiển thị, **Then** mã ticket vẫn hiện trong hàng tương ứng nhưng không có khoảng thời gian đầy đủ gây hiểu nhầm; người dùng được biết dữ liệu thời gian chưa đầy đủ.
6. **Given** người dùng sắp xếp các task trong bảng đã lưu trước khi mở calendar, **When** calendar hiển thị, **Then** thứ tự các mã ticket trong calendar khớp thứ tự task của bảng đã lưu.
7. **Given** cửa sổ ngày rộng hơn phần đang nhìn thấy, **When** người dùng cuộn ngang, **Then** người dùng có thể xem lần lượt mọi ô ngày liên tiếp từ giới hạn một tháng trước đến giới hạn ba tháng sau ngày hiện tại, không bỏ qua ngày trung gian hoặc cuộn vượt ra ngoài hai giới hạn; cột mã ticket vẫn luôn hiển thị.
8. **Given** dữ liệu ngày của task thay đổi sau khi làm mới bảng đã lưu, **When** người dùng mở hoặc xem lại calendar, **Then** calendar phản ánh các giá trị Dynamic Start Time và Dynamic Due Date hiện có của bảng.
9. **Given** một task có khoảng hợp lệ nhưng nằm hoàn toàn ngoài cửa sổ hiển thị, **When** calendar được hiển thị, **Then** hàng mã ticket vẫn còn, không có thanh lịch trình và người dùng được thông báo rằng task nằm ngoài khoảng ngày đang xem; trục ngày không được mở rộng.
10. **Given** múi giờ của bảng hợp lệ nhưng bảng không có task hoặc không có khoảng task hợp lệ, **When** calendar được mở, **Then** cửa sổ ngày cố định vẫn hiển thị; các hàng ticket hiện có không có thanh và có trạng thái giải thích.

### Edge Cases

- Bảng đã lưu chưa có task nào có đủ cả ngày bắt đầu và ngày kết thúc; nếu múi giờ hợp lệ, calendar vẫn hiển thị cửa sổ ngày cố định và các hàng ticket hiện có nhưng không có thanh, kèm trạng thái trống có giải thích; bảng kết quả vẫn hoạt động bình thường.
- Ngày kết thúc sớm hơn ngày bắt đầu; không vẽ một thanh thời gian sai thứ tự và báo rằng khoảng thời gian không hợp lệ.
- Khoảng thời gian đi qua cuối tháng hoặc cuối năm; mọi ngày trong cửa sổ hiển thị vẫn có ô riêng trên trục liên tục và nhãn xác định rõ tháng/năm.
- Khoảng task vượt một hoặc cả hai giới hạn cuộn; phần giao với cửa sổ bị cắt và đánh dấu tiếp diễn, còn task nằm hoàn toàn ngoài cửa sổ giữ hàng nhưng không có thanh và có thông báo; dữ liệu task không được mở rộng giới hạn.
- Bảng có task thiếu Dynamic Start Time hoặc Dynamic Due Date; các task này vẫn giữ đúng vị trí hàng khi người dùng sắp xếp hoặc cuộn.
- Một lần làm mới loại task khỏi bảng; task và hàng tương ứng không còn xuất hiện trong calendar đang xem.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Trong mục Bảng đã lưu, ứng dụng PHẢI cung cấp nút mở calendar cho bảng đang chọn. Calendar PHẢI mở thành chế độ xem riêng, KHÔNG ĐƯỢC ghép cạnh bảng kết quả truy vấn hoặc làm thay đổi màn hình kết quả truy vấn.
- **FR-002**: Calendar PHẢI trình bày riêng từng ngày liên tiếp trong cửa sổ hiển thị, bao gồm mọi ngày ở giữa các mốc của task; KHÔNG ĐƯỢC bỏ qua ngày trung gian. Mỗi ngày PHẢI có thứ và ngày/tháng/năm rõ ràng, kể cả khi cửa sổ đi qua ranh giới tháng hoặc năm.
- **FR-003**: Phần dữ liệu của calendar PHẢI chỉ có cột mã ticket ở bên trái và cột calendar ở bên phải. Cột mã ticket PHẢI luôn cố định và hiển thị khi cuộn ngang cột calendar. Mỗi task trong bảng đã lưu PHẢI có một hàng theo đúng thứ tự hiện tại của bảng.
- **FR-004**: Với task có Dynamic Start Time và Dynamic Due Date hợp lệ, ứng dụng PHẢI hiển thị thanh inclusive trong phần giao giữa khoảng task và cửa sổ ngày; nếu khoảng vượt mép cửa sổ, thanh PHẢI được cắt tại mép và có dấu tiếp diễn ngoài khung. Nếu task nằm hoàn toàn ngoài cửa sổ, ứng dụng KHÔNG ĐƯỢC vẽ thanh và PHẢI thông báo task ngoài phạm vi đang hiển thị, nhưng vẫn giữ hàng mã ticket. Ngày PHẢI được xác định theo múi giờ Jira đã lưu cho bảng; khoảng cùng ngày nằm trong cửa sổ PHẢI có dấu nhìn thấy được.
- **FR-005**: Nếu Dynamic Start Time hoặc Dynamic Due Date thiếu hoặc không hợp lệ, ứng dụng PHẢI giữ task trong bảng và calendar, không vẽ khoảng thời gian đầy đủ, đồng thời cho người dùng biết ngày nào chưa có hoặc khoảng thời gian không hợp lệ.
- **FR-006**: Khi thứ tự task của bảng đã lưu thay đổi trước khi mở calendar, thứ tự mã ticket và vị trí hàng của các khoảng thời gian trong calendar PHẢI khớp với thứ tự mới nhất đó.
- **FR-007**: Người dùng PHẢI có thể cuộn ngang cột calendar qua từng ô ngày liên tiếp, nhưng cửa sổ cuộn PHẢI bị giới hạn cứng từ ngày hiện tại trừ một tháng lịch đến ngày hiện tại cộng ba tháng lịch, tính cả hai đầu theo múi giờ Jira đã lưu cho bảng. Không được cuộn qua hai giới hạn; cột mã ticket PHẢI luôn hiển thị.
- **FR-008**: Sau khi dữ liệu của Bảng đã lưu được làm mới, calendar PHẢI phản ánh Dynamic Start Time và Dynamic Due Date mới nhất đang hiển thị trong bảng; việc hiển thị calendar KHÔNG ĐƯỢC tự thay đổi các giá trị hoặc quy tắc tính hai trường này.
- **FR-009**: Nếu không có task nào có khoảng thời gian hợp lệ, ứng dụng PHẢI hiển thị trạng thái trống có giải thích; khi múi giờ hợp lệ, cửa sổ ngày cố định và các hàng ticket hiện có vẫn PHẢI hiển thị nhưng KHÔNG ĐƯỢC vẽ thanh lịch trình. Bảng kết quả và các task vẫn phải truy cập được.
- **FR-010**: Cửa sổ ngày PHẢI độc lập với ngày bắt đầu/kết thúc của task và không được mở rộng theo các task nằm ngoài cửa sổ. Phạm vi là từ cùng ngày của tháng lịch trước đến cùng ngày của tháng lịch thứ ba sau ngày hiện tại, inclusive; nếu ngày tương ứng không tồn tại ở tháng đích thì dùng ngày cuối tháng đó. Ngày hiện tại PHẢI được xác định theo múi giờ Jira đã lưu cho bảng. Cửa sổ này vẫn PHẢI hiển thị nếu không có khoảng task hợp lệ khi múi giờ hợp lệ; các task hợp lệ nhưng hoàn toàn ngoài cửa sổ phải được thông báo là nằm ngoài phạm vi đang xem.

### Key Entities *(include if feature involves data)*

- **Bảng đã lưu đang chọn**: Tập hợp các task người dùng mở calendar, cùng thứ tự hiển thị và múi giờ Jira đã lưu cho bảng.
- **Khoảng thời gian của task**: Khoảng trên lịch được xác định bởi ngày Dynamic Start Time và ngày Dynamic Due Date của một task; có thể chưa đủ dữ liệu để hiển thị thành thanh.
- **Ngày lịch**: Một ngày theo múi giờ Jira của bảng, được đặt liên tiếp theo hàng ngang trên trục calendar và có nhãn thứ cùng ngày/tháng/năm.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong 100% task của bảng đã lưu, calendar hiển thị đúng một hàng có mã ticket tương ứng và theo cùng thứ tự task tại thời điểm mở calendar.
- **SC-002**: Trong 100% task có khoảng hợp lệ giao với cửa sổ, phần thanh nhìn thấy khớp chính xác với ngày bắt đầu/kết thúc nằm trong cửa sổ; khoảng vượt mép có dấu tiếp diễn, khoảng cùng ngày trong cửa sổ vẫn nhìn thấy được, và task nằm hoàn toàn ngoài cửa sổ không có thanh gây hiểu nhầm.
- **SC-003**: Trong 100% calendar có múi giờ hợp lệ, mỗi ngày từ giới hạn một tháng trước đến giới hạn ba tháng sau ngày hiện tại có đúng một ô theo thứ tự liên tục, kể cả mọi ngày trung gian và kể cả khi không có khoảng task hợp lệ; người dùng cuộn ngang xem được toàn bộ cửa sổ nhưng không vượt giới hạn.
- **SC-004**: Trong 100% task thiếu hoặc có khoảng thời gian không hợp lệ, task vẫn được hiển thị nhưng không có thanh lịch trình gây hiểu nhầm.
- **SC-005**: Trong một thử nghiệm sử dụng với ít nhất 5 người, ít nhất 4 người xác định được ngày bắt đầu và ngày kết thúc của một task từ calendar mà không cần mở task trên Jira.
- **SC-006**: Ở mọi vị trí cuộn ngang, cột mã ticket luôn hiển thị; cuộn cột calendar không làm lệch header ngày và thanh task khỏi cùng một trục ngày.

## Assumptions

- Nút mở calendar nằm trong mục Bảng đã lưu và mở calendar riêng cho bảng đang chọn; màn hình kết quả truy vấn không thay đổi.
- Phần dữ liệu của calendar có hai cột: mã ticket cố định bên trái và trục ngày cuộn ngang bên phải; không cần các cột thông tin task khác.
- Trục calendar có độ chi tiết một ngày và mỗi ngày trong cửa sổ có một ô riêng với nhãn thứ cùng ngày/tháng/năm. Cửa sổ tính theo tháng lịch, inclusive, từ ngày hiện tại trừ một tháng đến ngày hiện tại cộng ba tháng; nếu ngày đó không tồn tại ở tháng đích thì dùng ngày cuối tháng. Ngày hiện tại và ngày của task đều dùng múi giờ Jira đã lưu cho bảng. Giờ trong ngày không được thể hiện riêng.
- Mã ticket là nhãn hàng dọc; vị trí task khớp thứ tự trong bảng đã lưu tại lúc mở calendar.
- Nếu không có khoảng hợp lệ nhưng múi giờ hợp lệ, calendar vẫn hiển thị cửa sổ ngày cố định và các hàng hiện có, không có thanh, kèm trạng thái trống có giải thích. Khoảng hợp lệ ngoài cửa sổ không mở rộng trục; phần giao bị cắt và đánh dấu tiếp diễn, còn khoảng hoàn toàn ngoài cửa sổ giữ hàng và có thông báo.
- Calendar là chế độ xem chỉ đọc; người dùng không kéo thanh để thay đổi ngày và calendar không sửa dữ liệu task.
- Calendar dùng dữ liệu hiện có của Bảng đã lưu và không cần kết nối Jira để xem dữ liệu đã lưu.
