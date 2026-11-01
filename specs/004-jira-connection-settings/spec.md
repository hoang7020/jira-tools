# Feature Specification: Cấu hình kết nối Jira trong Cài đặt và tái sử dụng token đã lưu

**Feature Branch**: `004-jira-connection-settings`
**Created**: 2026-10-31
**Status**: Draft
**Input**: User description: "tính năng kết nối jira phải để trong menu settings, và lưu lại token mỗi khi connect thành công để tái sử dụng sau đó"

## Clarifications

### Session 2026-10-31

- Q: Thời điểm kiểm tra thông tin kết nối đã lưu khi mở ứng dụng — tự kiểm tra ngay khi mở, kiểm tra ngầm sau khi mở, hay chỉ kiểm tra khi người dùng dùng tới Jira? → A: Phương án B — ứng dụng hiển thị ngay trạng thái từ thông tin đã lưu rồi kiểm tra ngầm với Jira, không chặn giao diện; kiểm tra xong thì trạng thái tự cập nhật.
- Q: Hình thức trình bày của Cài đặt — màn hình trong cửa sổ chính, hộp thoại nổi, hay cửa sổ riêng? → A: Phương án A — một màn hình trong cửa sổ chính, đồng nhất với bộ chuyển chế độ xem hiện có.
- Q: Mức thông tin kết nối hiển thị ngoài Cài đặt — một dòng đầy đủ kèm địa chỉ Jira và email, hay chỉ một dòng ngắn? → A: Phương án B — một dòng ngắn chỉ nêu trạng thái kèm lối vào Cài đặt; địa chỉ Jira và email chỉ hiển thị trong Cài đặt.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cấu hình kết nối Jira trong màn hình Cài đặt (Priority: P1)

Người dùng mở ứng dụng và thấy màn hình chính chỉ dành cho công việc truy vấn. Khi cần khai báo hoặc sửa thông tin Jira, người dùng mở mục "Cài đặt", nhập địa chỉ Jira, email và API token, rồi bấm kết nối; ứng dụng kiểm tra với Jira và báo kết quả ngay trong Cài đặt. Trở lại màn hình chính, người dùng thấy trạng thái kết nối ngắn gọn thay cho biểu mẫu nhập liệu.

**Why this priority**: Đây là yêu cầu trực tiếp của người dùng và là điều kiện để toàn bộ tính năng "đưa kết nối vào Cài đặt" tồn tại. Khi chỉ làm được một việc, đây là việc mang lại giá trị rõ ràng nhất: biểu mẫu kết nối rời khỏi màn hình làm việc, nhường chỗ cho kết quả truy vấn, nhưng vẫn thao tác được đầy đủ.

**Independent Test**: Xoá thông tin kết nối đã lưu, mở ứng dụng: màn hình chính không còn ô nhập địa chỉ/email/token và có lối vào "Cài đặt"; mở Cài đặt, nhập thông tin Jira hợp lệ, xác nhận thấy thông báo kết nối thành công; quay lại màn hình chính và thấy trạng thái đã kết nối kèm địa chỉ Jira.

**Acceptance Scenarios**:

1. **Given** ứng dụng chưa có thông tin kết nối, **When** người dùng mở ứng dụng, **Then** màn hình chính không hiển thị biểu mẫu nhập thông tin kết nối, có lối vào "Cài đặt" và hướng dẫn ngắn để cấu hình kết nối.
2. **Given** người dùng đang ở Cài đặt, **When** người dùng nhập địa chỉ Jira, email và API token hợp lệ rồi bấm kết nối, **Then** ứng dụng kiểm tra với Jira, báo kết nối thành công, và màn hình chính hiển thị trạng thái đã kết nối (địa chỉ Jira và email được xem đầy đủ trong Cài đặt).
3. **Given** người dùng đang ở Cài đặt, **When** người dùng nhập thông tin sai (địa chỉ không tồn tại, email/token sai) rồi bấm kết nối, **Then** ứng dụng báo lỗi thân thiện tương ứng ngay trong Cài đặt và giữ nguyên thông tin kết nối đã lưu trước đó (nếu có).
4. **Given** người dùng đã kết nối thành công, **When** người dùng mở lại Cài đặt, **Then** địa chỉ Jira và email đã lưu được điền sẵn còn ô token để trống cho lần thay thế.
5. **Given** người dùng đang xem kết quả truy vấn, **When** người dùng mở rồi đóng Cài đặt mà không thay đổi gì, **Then** kết quả truy vấn vẫn còn nguyên và trạng thái kết nối không đổi.

---

### User Story 2 - Tái sử dụng token đã lưu ở những lần chạy sau (Priority: P2)

Sau khi kết nối thành công một lần, người dùng không phải nhập lại API token nữa: thông tin kết nối được lưu trên máy và dùng lại ở lần chạy kế tiếp. Cài đặt cho biết đã có token được lưu (không hiển thị lại giá trị), và khi token cũ không còn hiệu lực thì người dùng được hướng dẫn nhập lại đúng chỗ đó.

**Why this priority**: Đây là nửa thứ hai của yêu cầu ("lưu lại token mỗi khi connect thành công để tái sử dụng sau đó") và là giá trị người dùng cảm nhận được ngay ở mỗi lần mở ứng dụng. Phụ thuộc US1 vì chỉ có token đã kết nối thành công mới được lưu, nhưng có thể kiểm chứng độc lập bằng một lần kết nối rồi khởi động lại.

**Independent Test**: Kết nối thành công trong Cài đặt, đóng và mở lại ứng dụng, chạy một truy vấn JQL mà không mở Cài đặt lần nào; kết quả trả về bình thường. Sau đó đọc nội dung tệp cấu hình để xác nhận token không xuất hiện dạng văn bản thường, và quan sát trạng thái kết nối trên màn hình chính tự cập nhật sau khi kiểm tra ngầm kết thúc mà không cần thao tác gì.

**Acceptance Scenarios**:

1. **Given** người dùng đã kết nối thành công ở lần chạy trước, **When** người dùng mở lại ứng dụng và chạy một truy vấn JQL, **Then** truy vấn chạy được mà người dùng không phải nhập lại token.
2. **Given** ứng dụng đã có token được lưu, **When** người dùng mở Cài đặt, **Then** ứng dụng cho biết đã có token được lưu nhưng không hiển thị lại giá trị token.
3. **Given** token đã lưu bị Jira từ chối (sai, hết hạn hoặc bị thu hồi), **When** người dùng chạy truy vấn, **Then** ứng dụng báo rõ token cần được nhập lại và hướng người dùng mở Cài đặt, đồng thời giữ lại địa chỉ Jira và email đã lưu.
4. **Given** người dùng đang ở Cài đặt và đã có token được lưu, **When** người dùng nhập một token mới hợp lệ rồi bấm kết nối, **Then** token mới được lưu và dùng cho các lần chạy sau, token cũ không còn được sử dụng.
5. **Given** người dùng đã kết nối thành công ở lần chạy trước, **When** người dùng mở lại ứng dụng, **Then** trạng thái kết nối hiển thị ngay từ thông tin đã lưu, ứng dụng kiểm tra ngầm ở phía sau, và trạng thái tự cập nhật khi kiểm tra xong mà không chặn thao tác của người dùng.
6. **Given** thông tin kết nối đã lưu nhưng token đã bị thu hồi, **When** ứng dụng kiểm tra ngầm lúc mở, **Then** trạng thái chuyển thành cần nhập lại token kèm lối vào Cài đặt, và địa chỉ Jira cùng email vẫn được giữ nguyên.
7. **Given** đã có thông tin kết nối được lưu, **When** ứng dụng mở lúc máy không có mạng, **Then** trạng thái cho biết chưa xác minh được (không phải lỗi token) và người dùng vẫn thử chạy truy vấn được.

---

### User Story 3 - Ngắt kết nối và xoá thông tin đã lưu (Priority: P3)

Người dùng muốn đổi sang tài khoản khác hoặc không muốn thông tin đăng nhập còn nằm trên máy nữa, nên bấm "Ngắt kết nối" trong Cài đặt và xác nhận; ứng dụng xoá toàn bộ thông tin kết nối đã lưu (kể cả token) và trở về trạng thái chưa cấu hình.

**Why this priority**: Là điều kiện an toàn cần thiết khi ứng dụng có lưu bí mật trên máy, nhưng không phải là luồng sử dụng hằng ngày, nên đứng sau hai mục ưu tiên trên.

**Independent Test**: Kết nối thành công, mở Cài đặt và bấm "Ngắt kết nối", xác nhận; kiểm tra ứng dụng hiển thị trạng thái chưa cấu hình, các ô nhập trống, và token đã lưu không còn trên máy; sau đó chạy truy vấn để xác nhận ứng dụng yêu cầu cấu hình lại.

**Acceptance Scenarios**:

1. **Given** người dùng đã kết nối thành công, **When** người dùng bấm "Ngắt kết nối" và xác nhận, **Then** thông tin kết nối đã lưu (địa chỉ, email, token) bị xoá và ứng dụng hiển thị trạng thái chưa cấu hình.
2. **Given** người dùng đang bấm "Ngắt kết nối" và được hỏi xác nhận, **When** người dùng chọn huỷ, **Then** không có thông tin nào bị xoá và kết nối hiện tại vẫn dùng được.
3. **Given** người dùng vừa ngắt kết nối, **When** người dùng chạy truy vấn hoặc làm mới bảng đã lưu, **Then** ứng dụng không thực hiện lời gọi nào tới Jira và hướng dẫn người dùng cấu hình lại trong Cài đặt.

---

### Edge Cases

- Kho bí mật của hệ điều hành không khả dụng khi lưu token (không có quyền, khoá bị khoá): ứng dụng báo lỗi rõ ràng cho người dùng thay vì im lặng hoặc coi như đã kết nối.
- Token đã lưu biến mất khỏi kho bí mật của hệ điều hành (người dùng xoá thủ công, hoặc hệ điều hành thu hồi): ứng dụng xử lý như trạng thái chưa cấu hình khi chạy truy vấn.
- Người dùng đổi địa chỉ Jira sang một site khác: thông tin cũ bị thay thế; các bảng đã lưu trước đó giữ nguyên địa chỉ Jira ghi cùng chúng.
- Mất mạng hoặc Jira phản hồi chậm khi bấm kết nối: thông báo lỗi tương ứng, thông tin đã lưu trước đó không bị thay thế.
- Người dùng mở Cài đặt trong khi đang chạy truy vấn: truy vấn đang chạy không bị huỷ, kết quả vẫn được giữ khi quay lại.
- Người dùng kết nối nhiều lần trong cùng một phiên: chỉ thông tin của lần kết nối thành công gần nhất được dùng cho các lời gọi tiếp theo.
- Người dùng dán token kèm khoảng trắng hoặc xuống dòng thừa: ứng dụng tự cắt khoảng trắng trước khi kiểm tra và lưu.
- Ứng dụng chạy lần đầu trên máy mới, chưa có thông tin kết nối: màn hình chính hướng dẫn người dùng mở Cài đặt, không hiển thị lỗi khó hiểu.
- Ứng dụng mở lúc không có mạng nhưng đã có thông tin kết nối: trạng thái là chưa xác minh được (không phải lỗi token), thông tin đã lưu giữ nguyên, người dùng vẫn thử chạy truy vấn được.
- Kiểm tra ngầm kết thúc khi người dùng đã chuyển sang Cài đặt hoặc đang xem kết quả: việc cập nhật trạng thái không làm gián đoạn hay mất dữ liệu đang xem.
- Thông tin kết nối đã lưu còn địa chỉ Jira và email nhưng thiếu token (token bị xoá khỏi kho bí mật của hệ điều hành): trạng thái là chưa cấu hình đầy đủ và Cài đặt chỉ yêu cầu nhập lại token.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Ứng dụng PHẢI có mục "Cài đặt" truy cập được từ màn hình chính ở mọi chế độ xem (kết quả truy vấn và bảng đã lưu) và ở mọi trạng thái kết nối.
- **FR-002**: Màn hình chính KHÔNG ĐƯỢC hiển thị biểu mẫu nhập thông tin kết nối Jira (địa chỉ Jira, email, API token).
- **FR-003**: Màn hình chính PHẢI cho biết trạng thái kết nối hiện tại, phân biệt tối thiểu: chưa có thông tin kết nối, đang kiểm tra, đã kết nối thành công, và không kết nối được kèm lý do ngắn gọn.
- **FR-004**: Cài đặt PHẢI chứa đầy đủ chức năng kết nối hiện có: nhập địa chỉ Jira, email và API token; kiểm tra kết nối; thông báo thành công; thông báo lỗi thân thiện theo từng loại lỗi (địa chỉ sai, xác thực sai, không đủ quyền, bị giới hạn tần suất, mất mạng, phản hồi chậm).
- **FR-005**: Khi kết nối thành công, địa chỉ Jira, email và token PHẢI được lưu trên máy để dùng lại ở những lần chạy sau.
- **FR-006**: Token PHẢI được lưu bằng cơ chế lưu trữ bảo mật của hệ điều hành; token KHÔNG ĐƯỢC xuất hiện trong tệp cấu hình dạng văn bản thường, trong thông báo lỗi, hay trong nhật ký của ứng dụng.
- **FR-007**: Sau khi đã kết nối thành công một lần, người dùng KHÔNG PHẢI nhập lại token ở những lần chạy sau, trừ khi người dùng ngắt kết nối hoặc token đã lưu bị Jira từ chối.
- **FR-008**: Cài đặt PHẢI cho biết đã có token được lưu trên máy và KHÔNG ĐƯỢC hiển thị lại giá trị của token.
- **FR-009**: Cài đặt PHẢI điền sẵn địa chỉ Jira và email đã lưu (nếu có) khi mở, để người dùng chỉ cần nhập token khi thay thế thông tin.
- **FR-010**: Người dùng PHẢI ngắt kết nối được; thao tác này yêu cầu xác nhận trước và xoá toàn bộ thông tin kết nối đã lưu, kể cả token.
- **FR-011**: Sau khi ngắt kết nối, ứng dụng PHẢI ở trạng thái chưa cấu hình; các thao tác cần Jira (chạy truy vấn, tải thêm, làm mới bảng đã lưu) không được gọi Jira và PHẢI hướng dẫn người dùng cấu hình lại trong Cài đặt.
- **FR-012**: Khi token đã lưu bị Jira từ chối, ứng dụng PHẢI thông báo rõ ràng rằng cần nhập lại token trong Cài đặt và PHẢI giữ lại địa chỉ Jira cùng email đã lưu.
- **FR-013**: Khi kết nối thất bại, thông tin kết nối đã lưu trước đó KHÔNG ĐƯỢC bị thay thế hoặc bị xoá.
- **FR-014**: Trạng thái kết nối PHẢI cập nhật ngay trong phiên làm việc khi người dùng kết nối hoặc ngắt kết nối trong Cài đặt, không cần khởi động lại ứng dụng.
- **FR-015**: Mở và đóng Cài đặt KHÔNG ĐƯỢC làm mất kết quả truy vấn đang xem, bảng đã lưu đang mở, hay truy vấn đang chạy.
- **FR-016**: Thông tin kết nối được kiểm tra với Jira trước khi lưu; chỉ thông tin đã kiểm tra thành công mới được ghi nhớ.
- **FR-017**: Khi mở ứng dụng mà đã có thông tin kết nối được lưu, ứng dụng PHẢI hiển thị trạng thái ngay từ thông tin đã lưu và kiểm tra ngầm với Jira mà KHÔNG ĐƯỢC chặn giao diện; khi kiểm tra xong, trạng thái PHẢI tự cập nhật mà người dùng không phải thao tác gì. Mỗi lần mở ứng dụng chỉ chạy tối đa một lần kiểm tra ngầm.
- **FR-018**: Cài đặt PHẢI là một màn hình trong cửa sổ chính, mở được từ màn hình chính ở mọi chế độ xem và quay lại được mà không mất trạng thái đang làm việc (kết quả truy vấn, bảng đã lưu đang mở, truy vấn đang chạy).
- **FR-019**: Trạng thái kết nối trên màn hình chính PHẢI nằm gọn trong một dòng kèm lối vào "Cài đặt"; địa chỉ Jira và email chỉ hiển thị trong Cài đặt, và token KHÔNG BAO GIỜ được hiển thị ở bất kỳ đâu.
- **FR-020**: Các tính năng hiện có (chạy truy vấn JQL, tải thêm kết quả, sắp xếp, ẩn/hiện cột, mở ticket, xem bảng đã lưu, làm mới bảng đã lưu) PHẢI tiếp tục hoạt động như trước, chỉ khác nguồn thông tin kết nối nay do Cài đặt quản lý.
- **FR-021**: Thông tin kết nối đã lưu PHẢI được tái sử dụng cho mọi lời gọi Jira của ứng dụng (kiểm tra kết nối, chạy truy vấn, đếm kết quả, làm mới bảng đã lưu) mà người dùng không phải nhập lại.
- **FR-022**: Khi việc kiểm tra ngầm không thực hiện được vì lý do mạng (không phải vì token bị từ chối), ứng dụng PHẢI giữ nguyên thông tin kết nối đã lưu, hiển thị trạng thái chưa xác minh được, và KHÔNG ĐƯỢC ngăn người dùng thử chạy truy vấn.

### Key Entities

- **Thông tin kết nối Jira đã lưu**: địa chỉ Jira, email, token (bí mật, không hiển thị lại), và trạng thái xác minh gần nhất; là nguồn duy nhất cho mọi lời gọi Jira của ứng dụng.
- **Trạng thái kết nối**: chưa có thông tin kết nối, đang kiểm tra, đã kết nối thành công (kèm địa chỉ Jira và email), hoặc thất bại kèm lý do đã thân thiện hoá; hiển thị cả ở màn hình chính và trong Cài đặt.
- **Màn hình Cài đặt**: nơi nhập, kiểm tra, thay thế và xoá thông tin kết nối Jira; cho biết đã có token được lưu mà không tiết lộ giá trị.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng lần đầu cấu hình kết nối hoàn tất trong dưới 2 phút tính từ lúc mở ứng dụng, không cần đọc tài liệu.
- **SC-002**: Trong 100% lần thử, sau khi khởi động lại ứng dụng người dùng chạy được truy vấn mà không phải nhập lại token.
- **SC-003**: 0 lần token xuất hiện dạng văn bản thường trong dữ liệu lưu trên máy hoặc trong thông báo mà ứng dụng hiển thị (kiểm tra bằng cách đọc lại nội dung đã lưu và các thông báo lỗi).
- **SC-004**: Màn hình chính không còn ô nhập địa chỉ/email/token, và diện tích dành cho kết quả truy vấn tăng đúng bằng phần chiều cao trước đây biểu mẫu kết nối chiếm chỗ.
- **SC-005**: 100% kịch bản kiểm thử hiện có của ứng dụng vẫn đạt sau thay đổi này, xác nhận không hồi quy hành vi.
- **SC-006**: Trong 100% lần thử sau khi ngắt kết nối, token đã lưu không còn tồn tại trên máy và ứng dụng hiển thị trạng thái chưa cấu hình.
- **SC-007**: 100% trường hợp token đã lưu bị Jira từ chối đều khiến người dùng thấy thông báo nêu rõ cần nhập lại token trong Cài đặt, thay vì một lỗi chung chung.
- **SC-008**: Thao tác mở Cài đặt cho phản hồi trong dưới 1 giây kể từ lúc người dùng bấm.
- **SC-009**: Trong 100% lần mở ứng dụng khi đã có thông tin kết nối, trạng thái kết nối tự cập nhật sau khi kiểm tra ngầm kết thúc mà người dùng không phải thao tác gì, và giao diện không bị chặn trong lúc kiểm tra.

## Assumptions

- Thông tin kết nối vẫn gồm ba phần như hiện nay — địa chỉ Jira, email, API token — và không bổ sung hình thức đăng nhập khác (SSO/OAuth) trong phiên bản này.
- Ứng dụng dùng một tài khoản Jira tại một thời điểm; không hỗ trợ nhiều hồ sơ kết nối.
- Người dùng có kết nối mạng tại thời điểm bấm kết nối trong Cài đặt.
- Thao tác kết nối vẫn kiểm tra thông tin với Jira trước khi ghi nhớ, giữ nguyên hành vi đã có.
- Cơ chế thu gọn/mở rộng biểu mẫu kết nối theo chiều cao cửa sổ (tính năng trước) không còn áp dụng cho biểu mẫu nhập liệu, vì màn hình chính chỉ còn một dòng trạng thái và biểu mẫu nằm trong Cài đặt; các hành vi khác của màn hình chính giữ nguyên.
- Bảng đã lưu giữ địa chỉ Jira riêng của nó; đổi hoặc ngắt kết nối không tự thay đổi dữ liệu của các bảng đã lưu, và việc làm mới bảng đã lưu vẫn cần kết nối hợp lệ.
- Mục "Cài đặt" trong phiên bản này chỉ chứa phần kết nối Jira, nhưng được đặt tên và bố trí để có thể mở rộng thêm mục khác sau này.
- Ngắt kết nối cần xác nhận trước, đồng nhất với thao tác xoá bảng đã lưu.
- Các thông báo lỗi kết nối hiện có (thân thiện, không chứa bí mật) được dùng lại trong Cài đặt.
