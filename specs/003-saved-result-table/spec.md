# Feature Specification: Bảng kết quả riêng lưu cục bộ

**Feature Branch**: `003-saved-result-table`
**Created**: 2026-10-31
**Status**: Draft
**Input**: User description: "có thể tạo một bảng riêng và lưu local từ kết quả query; bảng này sẽ có mọi dữ liệu từ bảng query"

## Clarifications

### Session 2026-10-31

- Q: Phạm vi dữ liệu khi lưu bảng riêng — chỉ chứa các ticket đang có trong bảng truy vấn, hay ứng dụng phải tải toàn bộ ticket khớp truy vấn từ Jira trước khi lưu? → A: Phương án A — chỉ lưu những ticket đang có trong bảng truy vấn tại thời điểm lưu (gồm mọi trang người dùng đã tải thêm); thao tác lưu không gọi thêm Jira.
- Q: Dữ liệu bảng đã lưu trên máy không đọc được (tệp hỏng hoặc từ phiên bản ứng dụng cũ hơn) thì ứng dụng nên xử lý thế nào? → A: Phương án A — cảnh báo rõ ràng, danh sách bảng đã lưu tạm thời trống, giữ nguyên dữ liệu cũ trên máy để người dùng tự xử lý; ứng dụng vẫn khởi động và dùng bình thường.
- Q: Bảng đã lưu có cần thao tác "làm mới từ Jira" không, hay chỉ là ảnh chụp giữ nguyên như lúc lưu? → A: Phương án B — có nút "Làm mới" trong bảng đã lưu: chạy lại truy vấn đã lưu để nạp dữ liệu mới, cần đang kết nối Jira; nếu lỗi thì giữ nguyên dữ liệu cũ.
- Q: Khi bấm "Làm mới", ứng dụng nạp bao nhiêu ticket vào bảng đã lưu? → A: Phương án A — nạp toàn bộ ticket khớp truy vấn (chạy hết các trang kết quả), có tiến trình; thất bại giữa đường vẫn giữ dữ liệu cũ.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lưu bảng kết quả truy vấn thành một bảng riêng (Priority: P1)

Sau khi chạy một truy vấn JQL và thấy kết quả cần giữ lại, người dùng bấm "Lưu bảng", đặt tên cho bảng, và ứng dụng tạo một bảng riêng chứa toàn bộ dữ liệu đang có trong bảng truy vấn. Bảng riêng này nằm trên máy người dùng và vẫn còn sau khi đóng ứng dụng.

**Why this priority**: Đây là yêu cầu trực tiếp của người dùng và là điều kiện để mọi giá trị khác của tính năng tồn tại — không có thao tác lưu thì không có bảng riêng nào để xem lại. Nếu chỉ làm được một việc, đây là việc mang lại giá trị rõ ràng nhất.

**Independent Test**: Chạy một truy vấn trả về vài chục ticket, bấm "Lưu bảng", đặt tên, xác nhận có thông báo lưu thành công; đóng và mở lại ứng dụng; mở bảng vừa lưu và đối chiếu từng ticket cùng từng trường dữ liệu với bảng truy vấn ban đầu.

**Acceptance Scenarios**:

1. **Given** bảng truy vấn đang hiển thị danh sách ticket, **When** người dùng bấm "Lưu bảng" và nhập tên hợp lệ, **Then** ứng dụng tạo một bảng riêng chứa đúng số ticket đang có và báo lưu thành công kèm tên bảng.
2. **Given** bảng truy vấn đang hiển thị 100 ticket với các cột đang bật, **When** người dùng lưu bảng với một số cột đang bị ẩn, **Then** bảng riêng vẫn chứa đầy đủ dữ liệu của tất cả các trường (kể cả trường thuộc cột đang ẩn).
3. **Given** người dùng vừa lưu một bảng, **When** người dùng đóng và mở lại ứng dụng, **Then** bảng vừa lưu vẫn còn, mở được và hiển thị đúng dữ liệu đã lưu.
4. **Given** người dùng đang gõ tên bảng trùng với một bảng đã lưu, **When** người dùng xác nhận lưu, **Then** ứng dụng báo tên đã tồn tại và không tạo bảng mới, dữ liệu bảng truy vấn vẫn nguyên vẹn.
5. **Given** bảng truy vấn đang tải, đang trống kết quả, hoặc đang ở trạng thái lỗi, **When** người dùng quan sát, **Then** thao tác lưu không khả dụng và ứng dụng giải thích ngắn gọn vì sao chưa lưu được.

---

### User Story 2 - Xem lại các bảng đã lưu (Priority: P2)

Người dùng mở danh sách các bảng đã lưu để xem tên, số ticket, thời điểm lưu và truy vấn gốc; sau đó mở một bảng để xem dữ liệu dạng bảng, sắp xếp và ẩn/hiện cột như khi xem kết quả truy vấn, đồng thời mở được ticket trong trình duyệt.

**Why this priority**: Lưu mà không xem lại được thì không có giá trị sử dụng; tuy nhiên đây là bước kế tiếp sau khi thao tác lưu đã hoạt động.

**Independent Test**: Với ít nhất hai bảng đã lưu, mở danh sách, kiểm tra thông tin tóm tắt từng bảng, mở một bảng, sắp xếp theo một cột, ẩn một cột, mở một ticket trong trình duyệt, rồi xác nhận bảng truy vấn của phiên hiện tại không bị thay đổi.

**Acceptance Scenarios**:

1. **Given** đã có nhiều bảng lưu, **When** người dùng mở danh sách bảng đã lưu, **Then** mỗi bảng hiển thị tên, số ticket, thời điểm lưu và truy vấn gốc.
2. **Given** ứng dụng chưa kết nối tới Jira hoặc đang mất mạng, **When** người dùng mở một bảng đã lưu, **Then** dữ liệu của bảng vẫn hiển thị đầy đủ.
3. **Given** một bảng đã lưu đang mở, **When** người dùng bấm tiêu đề một cột hoặc bật/tắt cột hiển thị, **Then** bảng sắp xếp và ẩn/hiện cột trên chính dữ liệu đã lưu, không phát sinh truy vấn tới Jira.
4. **Given** một bảng đã lưu đang mở, **When** người dùng bấm một hàng ticket, **Then** ticket mở trong trình duyệt bằng địa chỉ Jira đã ghi lại cùng bảng lưu.
5. **Given** bảng đã lưu có nhiều hơn một màn hình ticket, **When** người dùng cuộn trong bảng, **Then** danh sách cuộn được và vùng điều khiển phía trên vẫn cố định như ở bảng truy vấn.

---

### User Story 3 - Làm mới bảng đã lưu từ Jira (Priority: P3)

Khi dữ liệu trên Jira đã thay đổi, người dùng mở một bảng đã lưu và bấm "Làm mới" để nạp lại toàn bộ kết quả hiện tại của đúng truy vấn đã lưu, thay cho dữ liệu cũ trong bảng đó.

**Why this priority**: Giữ cho bảng đã lưu không bị lạc hậu theo thời gian. Chỉ có giá trị sau khi việc lưu và xem đã hoàn chỉnh, và cần kết nối Jira nên không thuộc luồng lưu cục bộ cốt lõi.

**Independent Test**: Với một bảng đã lưu và một kết nối Jira đang hoạt động, bấm "Làm mới", xác nhận bảng chứa kết quả mới nhất (ticket mới xuất hiện, ticket không còn khớp bị loại) trong khi tên bảng và lựa chọn cột giữ nguyên; sau đó ngắt mạng rồi bấm "Làm mới" để xác nhận dữ liệu cũ được giữ nguyên kèm thông báo lỗi.

**Acceptance Scenarios**:

1. **Given** một bảng đã lưu và ứng dụng đang kết nối Jira, **When** người dùng bấm "Làm mới", **Then** bảng được cập nhật bằng kết quả hiện tại của truy vấn đã lưu, tên bảng và lựa chọn cột hiển thị không đổi, và thời điểm làm mới gần nhất được cập nhật.
2. **Given** truy vấn đã lưu hiện khớp nhiều hơn một trang kết quả, **When** người dùng bấm "Làm mới", **Then** bảng nhận toàn bộ ticket khớp truy vấn chứ không chỉ trang đầu tiên.
3. **Given** ứng dụng chưa kết nối Jira hoặc đang mất mạng, **When** người dùng bấm "Làm mới", **Then** thao tác không thực hiện được, ứng dụng giải thích ngắn gọn và dữ liệu cũ của bảng vẫn nguyên vẹn.
4. **Given** người dùng vừa bấm "Làm mới", **When** quá trình đang chạy, **Then** ứng dụng hiển thị trạng thái đang tải kèm tiến trình và người dùng vẫn xem được dữ liệu cũ.
5. **Given** một lần làm mới thất bại giữa đường (mất mạng, hết hạn xác thực, truy vấn không còn hợp lệ, bị giới hạn tần suất), **When** người dùng quan sát, **Then** bảng giữ nguyên dữ liệu cũ và ứng dụng báo lỗi rõ ràng.

---

### User Story 4 - Quản lý các bảng đã lưu (Priority: P4)

Người dùng đổi tên một bảng đã lưu khi tên cũ không còn phù hợp, và xoá những bảng không cần nữa; thao tác xoá phải được xác nhận để tránh mất dữ liệu ngoài ý muốn.

**Why this priority**: Cần thiết để danh sách bảng lưu dùng được lâu dài, nhưng chỉ có giá trị sau khi việc lưu và xem đã hoàn chỉnh.

**Independent Test**: Đổi tên một bảng đã lưu và xác nhận tên mới vẫn còn sau khi khởi động lại; xoá một bảng và xác nhận bảng biến mất khỏi danh sách, trong khi các bảng khác giữ nguyên.

**Acceptance Scenarios**:

1. **Given** một bảng đã lưu, **When** người dùng đổi tên sang tên mới hợp lệ và chưa tồn tại, **Then** tên mới được hiển thị ở danh sách và giữ nguyên sau khi khởi động lại.
2. **Given** người dùng đang đổi tên sang một tên đã tồn tại hoặc tên rỗng, **When** người dùng xác nhận, **Then** ứng dụng báo lỗi và tên cũ vẫn giữ nguyên.
3. **Given** một bảng đã lưu, **When** người dùng yêu cầu xoá, **Then** ứng dụng yêu cầu xác nhận trước khi xoá; nếu người dùng đồng ý, bảng bị xoá vĩnh viễn và các bảng khác không bị ảnh hưởng.

---

### Edge Cases

- Bảng truy vấn đang tải thêm kết quả khi người dùng bấm lưu: bảng riêng phải phản ánh đúng số ticket đã tải xong tại thời điểm lưu và không rơi vào trạng thái nửa vời.
- Bảng truy vấn có rất nhiều ticket (khoảng 1000 ticket): thao tác lưu vẫn hoàn tất trong thời gian chấp nhận được và không đơ giao diện.
- Người dùng lưu nhiều bảng (hàng chục bảng) và mở danh sách: danh sách vẫn hiển thị nhanh và không lẫn dữ liệu giữa các bảng.
- Tên bảng chứa ký tự Unicode, khoảng trắng thừa ở hai đầu, hoặc dài bất thường: hệ thống chuẩn hoá khoảng trắng và xử lý được, không làm hỏng danh sách bảng.
- Lưu thất bại vì không ghi được dữ liệu trên máy (ví dụ hết dung lượng hoặc không có quyền ghi): ứng dụng báo lỗi rõ ràng, không tạo bảng dở dang và không làm mất kết quả truy vấn đang xem.
- Dữ liệu bảng đã lưu không đọc được (tệp hỏng hoặc được ghi bởi phiên bản ứng dụng cũ hơn): ứng dụng vẫn khởi động và dùng bình thường, báo rõ là không đọc được dữ liệu bảng đã lưu, hiển thị danh sách trống và giữ nguyên dữ liệu cũ trên máy để người dùng tự xử lý.
- Người dùng xoá một bảng đang mở: ứng dụng đóng bảng đó và quay về danh sách, không hiển thị dữ liệu đã bị xoá.
- Sau khi lưu, người dùng đổi kết nối sang một Jira khác: bảng đã lưu vẫn dùng địa chỉ Jira ghi tại thời điểm lưu để mở ticket; nếu bảng lưu không có địa chỉ hợp lệ, ứng dụng phải thông báo thay vì im lặng.
- Ticket trong bảng đã lưu đã bị xoá hoặc người dùng không còn quyền xem trên Jira: việc mở ticket do trình duyệt/Jira xử lý; bảng đã lưu và dữ liệu cục bộ không bị ảnh hưởng.
- Kết quả truy vấn không có ticket nào: không có gì để lưu, thao tác lưu bị vô hiệu hoá kèm giải thích.
- Làm mới khi chưa kết nối Jira hoặc mất mạng: thao tác không thực hiện được, dữ liệu cũ của bảng giữ nguyên và ứng dụng nói rõ vì sao.
- Truy vấn đã lưu không còn hợp lệ trên Jira (ví dụ trường hoặc giá trị bị đổi): làm mới thất bại, dữ liệu cũ giữ nguyên.
- Sau khi làm mới, ticket không còn khớp truy vấn biến mất khỏi bảng và ticket mới trên Jira chỉ xuất hiện khi người dùng làm mới; bảng đã lưu không tự thay đổi ngoài hai thao tác lưu mới và làm mới.
- Làm mới một truy vấn khớp rất nhiều ticket: tiến trình hiển thị liên tục, người dùng vẫn xem được dữ liệu cũ và có thể dừng lại mà không làm hỏng bảng.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Người dùng PHẢI lưu được bảng truy vấn hiện tại thành một bảng riêng, với tên do người dùng đặt. (US1)
- **FR-002**: Bảng riêng PHẢI chứa toàn bộ trường dữ liệu của mỗi ticket giống bảng truy vấn — mã, tiêu đề, trạng thái, người xử lý, độ ưu tiên, cập nhật — bao gồm cả dữ liệu của những cột đang bị ẩn tại thời điểm lưu. (US1)
- **FR-003**: Bảng riêng PHẢI ghi lại truy vấn JQL đã dùng và địa chỉ Jira tại thời điểm lưu, để hiển thị lại bối cảnh và mở ticket. (US1, US2)
- **FR-004**: Bảng riêng PHẢI được lưu trên máy người dùng và vẫn tồn tại sau khi đóng và mở lại ứng dụng. (US1)
- **FR-005**: Người dùng PHẢI xem được danh sách các bảng đã lưu, mỗi bảng hiển thị tên, số ticket, thời điểm lưu và truy vấn gốc. (US2)
- **FR-006**: Người dùng PHẢI mở và xem dữ liệu của một bảng đã lưu mà KHÔNG cần kết nối tới Jira. (US2)
- **FR-007**: Bảng đã lưu PHẢI hiển thị dạng bảng có sắp xếp theo cột và ẩn/hiện cột, thực hiện trên chính dữ liệu đã lưu và không phát sinh truy vấn tới Jira. (US2)
- **FR-008**: Người dùng PHẢI mở được ticket từ bảng đã lưu trong trình duyệt bằng địa chỉ Jira đã ghi cùng bảng đó. (US2)
- **FR-009**: Người dùng PHẢI đổi tên được một bảng đã lưu và tên mới PHẢI được giữ sau khi khởi động lại. (US4)
- **FR-010**: Người dùng PHẢI xoá được một bảng đã lưu, và thao tác xoá PHẢI yêu cầu xác nhận trước khi thực hiện. (US4)
- **FR-011**: Tên bảng KHÔNG ĐƯỢC rỗng hoặc chỉ gồm khoảng trắng, và KHÔNG ĐƯỢC trùng với một bảng đã lưu khác; khi vi phạm, ứng dụng PHẢI báo rõ lý do và giữ nguyên dữ liệu hiện có. (US1, US4)
- **FR-012**: Thao tác lưu PHẢI chỉ khả dụng khi bảng truy vấn đang có ít nhất một ticket; với trạng thái đang tải, không có kết quả hoặc lỗi, ứng dụng PHẢI giải thích ngắn gọn vì sao chưa lưu được. (US1)
- **FR-013**: Số ticket trong bảng riêng PHẢI đúng bằng số ticket đang có trong bảng truy vấn tại thời điểm lưu, bao gồm mọi trang người dùng đã tải thêm. (US1)
- **FR-014**: Bảng riêng là ảnh chụp tại thời điểm lưu: nội dung của nó KHÔNG ĐƯỢC tự thay đổi khi người dùng chạy truy vấn mới, sắp xếp lại bảng truy vấn, hay khi dữ liệu trên Jira thay đổi. (US1)
- **FR-015**: Ứng dụng PHẢI thông báo rõ kết quả lưu (thành công kèm tên bảng, hoặc thất bại kèm lý do ngắn gọn); khi lưu thất bại, bảng truy vấn và dữ liệu đang xem PHẢI không bị ảnh hưởng. (US1)
- **FR-016**: Bảng đã lưu KHÔNG ĐƯỢC chứa API token hay bất kỳ thông tin bí mật nào của kết nối Jira. (US1)
- **FR-017**: Bảng riêng PHẢI giữ được lựa chọn cột hiển thị của riêng nó, để lần sau mở lại người dùng thấy đúng cách hiển thị đã chọn. (US2)
- **FR-018**: Thao tác lưu KHÔNG ĐƯỢC tự gọi Jira để tải thêm ticket: bảng riêng chỉ chứa đúng những ticket đang có trong bảng truy vấn tại thời điểm lưu, kể cả khi truy vấn còn nhiều trang chưa tải. (US1)
- **FR-019**: Các tính năng hiện có (chạy truy vấn JQL, tải thêm kết quả, sắp xếp, ẩn/hiện cột, mở ticket, ghi nhớ truy vấn và lựa chọn cột, cửa sổ mở rộng tối đa, cuộn bảng) PHẢI giữ nguyên hành vi sau thay đổi này. (US1, US2, US3, US4)
- **FR-020**: Khi dữ liệu bảng đã lưu trên máy không đọc được (hỏng hoặc không tương thích với phiên bản ứng dụng đang chạy), ứng dụng PHẢI vẫn khởi động và dùng bình thường, thông báo rõ là không đọc được dữ liệu bảng đã lưu, hiển thị danh sách trống, và KHÔNG ĐƯỢC tự xoá hoặc ghi đè dữ liệu cũ. (US1, US2)
- **FR-021**: Người dùng PHẢI làm mới được một bảng đã lưu: ứng dụng chạy lại đúng truy vấn JQL đã lưu và thay thế toàn bộ danh sách ticket của bảng bằng kết quả mới (không nối thêm, không giữ ticket không còn khớp truy vấn), giữ nguyên tên bảng và lựa chọn cột hiển thị. (US3)
- **FR-022**: Làm mới PHẢI nạp toàn bộ ticket khớp truy vấn đã lưu (chạy hết các trang kết quả), không dừng ở trang đầu tiên. (Khác với thao tác lưu: lưu chỉ lấy dữ liệu đang có trong bảng truy vấn — xem FR-018.) (US3)
- **FR-023**: Thao tác làm mới PHẢI chỉ thực hiện được khi ứng dụng đang có kết nối Jira hợp lệ; khi chưa kết nối, thao tác không thực hiện được và ứng dụng PHẢI giải thích ngắn gọn. (US3)
- **FR-024**: Khi làm mới thất bại (mất mạng, hết hạn xác thực, truy vấn không còn hợp lệ, bị giới hạn tần suất), dữ liệu của bảng PHẢI giữ nguyên như trước đó và ứng dụng PHẢI báo lỗi rõ ràng. (US3)
- **FR-025**: Trong khi làm mới, ứng dụng PHẢI hiển thị trạng thái đang tải kèm tiến trình, cho phép người dùng dừng lại, và dữ liệu cũ vẫn xem được cho tới khi kết quả mới sẵn sàng. (US3)
- **FR-026**: Sau khi làm mới thành công, bảng PHẢI ghi lại thời điểm làm mới gần nhất và địa chỉ Jira đã dùng cho lần làm mới đó, để mở ticket đúng nơi. (US3)

### Key Entities *(include if feature involves data)*

- **Bảng riêng đã lưu (Saved Table)**: một ảnh chụp kết quả truy vấn do người dùng đặt tên; thuộc tính gồm tên, thời điểm lưu, thời điểm làm mới gần nhất (nếu đã làm mới), truy vấn JQL đã dùng, địa chỉ Jira dùng cho lần lưu hoặc làm mới gần nhất, danh sách ticket đã lưu, và lựa chọn cột hiển thị của riêng bảng.
- **Ticket đã lưu (Saved Ticket)**: bản ghi dữ liệu của một ticket trong bảng riêng, gồm mã, tiêu đề, trạng thái, người xử lý, độ ưu tiên và cập nhật; là dữ liệu tĩnh, không tham chiếu trực tiếp tới Jira.
- **Danh mục bảng đã lưu (Saved Table Catalog)**: tập hợp các bảng riêng trên máy người dùng, là nơi liệt kê, mở, đổi tên và xoá bảng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng lưu được một bảng khoảng 100 ticket trong vòng dưới 5 giây kể từ lúc xác nhận tên bảng.
- **SC-002**: Bảng riêng chứa đủ 100% số ticket đang có trong bảng truy vấn tại thời điểm lưu và 100% trường dữ liệu của mỗi ticket, kể cả trường thuộc cột đang ẩn.
- **SC-003**: Sau khi khởi động lại ứng dụng, 100% bảng đã lưu vẫn mở được và hiển thị đúng dữ liệu đã lưu, không mất ticket nào.
- **SC-004**: Mở một bảng đã lưu có khoảng 1000 ticket hiển thị được hàng đầu tiên trong dưới 3 giây và cuộn mượt như bảng truy vấn.
- **SC-005**: Mở và xem bảng đã lưu hoạt động đúng trong 100% lần thử khi ứng dụng chưa kết nối Jira hoặc đang mất mạng.
- **SC-006**: Trong một lần thử với tối thiểu 5 người dùng lần đầu sử dụng, tối thiểu 4 người lưu được một bảng trong lần thử đầu tiên mà không cần hướng dẫn và tìm lại được bảng đó sau khi khởi động lại.
- **SC-007**: 100% kịch bản kiểm thử hiện có của ứng dụng vẫn đạt sau thay đổi này, xác nhận không có hồi quy hành vi.
- **SC-008**: 100% bảng đã lưu không chứa API token hoặc thông tin bí mật nào của kết nối (kiểm tra bằng cách rà nội dung dữ liệu lưu trên máy).
- **SC-009**: Trong 100% lần khởi động với dữ liệu bảng đã lưu không đọc được, ứng dụng vẫn mở và dùng được, có thông báo về việc không đọc được dữ liệu, và dữ liệu cũ vẫn còn nguyên trên máy sau đó.
- **SC-010**: Làm mới một bảng đã lưu với truy vấn khoảng 100 ticket hoàn tất trong dưới 10 giây kể từ lúc bấm "Làm mới".
- **SC-011**: Sau khi làm mới, bảng chứa đúng 100% kết quả hiện tại của truy vấn đã lưu — không thiếu ticket vì chỉ nạp một trang, và không còn ticket không khớp truy vấn.
- **SC-012**: 100% lần làm mới thất bại (mất mạng, hết hạn xác thực, truy vấn lỗi, bị giới hạn tần suất) đều giữ nguyên dữ liệu cũ của bảng và hiển thị thông báo lỗi rõ ràng cho người dùng.

## Assumptions

- "Bảng riêng" được hiểu là một bảng được lưu và xem lại bên trong ứng dụng; xuất ra tệp ngoài (CSV/Excel) và chia sẻ cho người khác nằm ngoài phạm vi phiên bản này.
- Bảng riêng không tự động cập nhật theo Jira: nội dung chỉ thay đổi khi người dùng lưu mới hoặc chủ động bấm "Làm mới"; làm mới chạy lại đúng truy vấn đã lưu và cần một kết nối Jira đang hoạt động.
- Bảng đã lưu nằm trên máy người dùng, gắn với phiên đăng nhập hệ điều hành hiện tại: không đồng bộ giữa nhiều máy, không dùng chung giữa nhiều người.
- Người dùng là một người dùng desktop đơn lẻ với một hồ sơ kết nối Jira; quy mô hợp lý là hàng chục bảng đã lưu, mỗi bảng tới vài nghìn ticket.
- Không cần kết nối tới Jira để lưu (dữ liệu đã có trong bảng truy vấn) hoặc để xem bảng đã lưu; cần mạng khi người dùng mở ticket trong trình duyệt hoặc làm mới bảng đã lưu.
- Bảng đã lưu dùng cùng bộ cột và cùng quy tắc hiển thị như bảng truy vấn (mã, tiêu đề, trạng thái, người xử lý, độ ưu tiên, cập nhật), trong đó cột mã luôn hiển thị.
- Xoá bảng đã lưu là thao tác vĩnh viễn, không có thùng rác hay khôi phục trong phiên bản này.
- Việc lưu không thay đổi luồng nghiệp vụ hiện có (kết nối, truy vấn JQL, phân trang, sắp xếp, ẩn/hiện cột, mở ticket, ghi nhớ tuỳ chọn); chỉ bổ sung một luồng mới song song.
