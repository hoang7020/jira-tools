# Feature Specification: Nâng cấp UI/UX theo Material Design

**Feature Branch**: `010-material-design-uiux`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "cập nhật giao diện theo chuẩn material design. Thiết kế UIUX tốt nhất"

## Clarifications

### Session 2026-11-01

- Q: Giao diện nên hỗ trợ chế độ màu sáng/tối như thế nào? → A: Theo giao diện hệ điều hành mặc định và cho phép người dùng chọn sáng/tối trong Cài đặt.
- Q: Bạn muốn điều hướng chính giữa Truy vấn, Bảng đã lưu và Cài đặt được bố trí như thế nào? → A: Dùng thanh điều hướng bên trái có thể thu gọn.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Điều hướng và sử dụng màn hình truy vấn nhất quán (Priority: P1)

Người dùng mở ứng dụng Jira Tools và nhanh chóng nhận biết trạng thái kết nối, nơi nhập JQL, cách chạy truy vấn và nơi xem kết quả. Các thành phần điều hướng và thao tác chính có thứ bậc rõ ràng, cách trình bày nhất quán theo Material Design, giúp người dùng tập trung vào công việc thay vì phải đoán chức năng.

**Why this priority**: Truy vấn ticket là công việc cốt lõi và là điểm bắt đầu của phần lớn phiên sử dụng; cải thiện luồng này mang lại giá trị trực tiếp nhất.

**Independent Test**: Mở ứng dụng ở trạng thái đã kết nối, xác định vùng nhập JQL, chạy truy vấn, đọc trạng thái tải/kết quả và chuyển sang các màn hình khác. Xác nhận các thao tác chính dễ nhận ra, phản hồi rõ ràng và không làm thay đổi kết quả nghiệp vụ hiện có.

**Acceptance Scenarios**:

1. **Given** người dùng đang ở màn hình truy vấn, **When** họ quan sát giao diện, **Then** tiêu đề, trạng thái kết nối, vùng nhập truy vấn, thao tác chạy và bảng kết quả có thứ bậc thị giác rõ ràng.
2. **Given** truy vấn đang tải, **When** người dùng chờ kết quả, **Then** trạng thái tải được thể hiện rõ tại thao tác liên quan và giao diện không tạo cảm giác bị treo.
3. **Given** truy vấn trả về lỗi hoặc không có ticket, **When** kết quả được hiển thị, **Then** thông báo giải thích trạng thái và hành động tiếp theo được trình bày dễ nhận biết mà vẫn giữ nội dung JQL.
4. **Given** người dùng chuyển giữa các khu vực chính, **When** họ dùng thanh điều hướng bên trái, **Then** mục hiện tại được nhận biết rõ và thanh bên có thể thu gọn để tăng không gian cho nội dung.

### User Story 2 - Quản lý bảng đã lưu và xem lịch dễ dàng (Priority: P2)

Người dùng tìm, chọn và quản lý bảng đã lưu; khi mở chi tiết hoặc lịch, họ có thể phân biệt dữ liệu, thao tác và trạng thái. Các bảng rộng, nội dung dài, cảnh báo, thông báo rỗng và tiến trình làm mới vẫn dễ đọc, không gây rối hoặc che khuất thông tin quan trọng.

**Why this priority**: Bảng đã lưu và lịch hỗ trợ công việc rà soát định kỳ; tính nhất quán ở các màn hình này giúp người dùng chuyển đổi ngữ cảnh mà không cần học lại cách tương tác.

**Independent Test**: Mở khu vực bảng đã lưu, chọn một bảng, đổi tên hoặc xoá qua luồng xác nhận, làm mới dữ liệu, rồi mở lịch. Kiểm tra trạng thái được chọn, thông báo, hành động chính và vùng dữ liệu đều dễ phân biệt.

**Acceptance Scenarios**:

1. **Given** có nhiều bảng đã lưu, **When** người dùng chọn một bảng, **Then** mục được chọn nổi bật và vùng chi tiết hiển thị tên, thông tin cùng các thao tác liên quan theo thứ bậc rõ ràng.
2. **Given** người dùng bắt đầu thao tác có thể mất dữ liệu như xoá bảng, **When** thao tác cần xác nhận, **Then** nội dung xác nhận nêu rõ hệ quả và nút an toàn để huỷ dễ tìm.
3. **Given** dữ liệu đang làm mới, bị lỗi hoặc rỗng, **When** trạng thái tương ứng xuất hiện, **Then** giao diện phân biệt được trạng thái và cung cấp hành động phù hợp như thử lại hoặc làm mới.
4. **Given** người dùng mở lịch, **When** họ cuộn ngày và danh sách ticket, **Then** header, timeline và điều khiển vẫn dễ đọc, dễ thao tác và không che khuất nhau.

### User Story 3 - Cấu hình kết nối và nhận phản hồi dễ hiểu (Priority: P2)

Người dùng mở Cài đặt để cấu hình hoặc kiểm tra kết nối Jira. Họ phân biệt được thông tin nhập, trạng thái đã lưu, lỗi và thao tác chính; thông tin nhạy cảm không bị lộ trong nội dung hiển thị sau khi lưu.

**Why this priority**: Kết nối là điều kiện cho truy vấn, nhưng phần lớn người dùng chỉ cần vào Cài đặt khi cấu hình lần đầu hoặc khắc phục sự cố; luồng tốt giúp giảm lỗi mà không lấn át công việc chính.

**Independent Test**: Mở Cài đặt ở trạng thái chưa cấu hình, nhập thông tin kết nối, kiểm tra trạng thái thành công và thất bại, rồi kiểm tra cách thông tin đã lưu được biểu thị mà không tiết lộ token.

**Acceptance Scenarios**:

1. **Given** người dùng mở Cài đặt, **When** họ xem biểu mẫu kết nối, **Then** nhãn, trạng thái đã lưu, nút kết nối và thông báo được phân biệt rõ.
2. **Given** thông tin kết nối sai hoặc Jira không phản hồi, **When** việc kiểm tra thất bại, **Then** lỗi được trình bày gần ngữ cảnh thao tác bằng ngôn ngữ dễ hiểu và không xoá nội dung người dùng cần sửa.
3. **Given** người dùng xác nhận ngắt kết nối, **When** hộp thoại xác nhận hiển thị, **Then** hành động huỷ và hành động xoá được phân biệt rõ, không dễ bấm nhầm.

### Edge Cases

- Cửa sổ ứng dụng hẹp hoặc được resize: nội dung chính vẫn tiếp cận được, không chồng lấn và không làm mất thao tác thiết yếu.
- Bảng có nhiều cột, tiêu đề dài hoặc nội dung ticket dài: thông tin không làm vỡ bố cục; phần bị rút gọn vẫn có cách truy cập theo hành vi hiện có.
- Màn hình đang tải, lỗi, rỗng hoặc dữ liệu không đọc được: trạng thái không chỉ dựa vào màu sắc và không bị nhầm với trạng thái thành công.
- Người dùng thao tác bằng bàn phím: thứ tự focus hợp lý, focus hiện tại nhìn thấy được và thao tác chính vẫn dùng được.
- Khi hệ điều hành đổi chế độ sáng/tối, giao diện theo thay đổi đó nếu người dùng chưa đặt lựa chọn riêng; lựa chọn riêng được giữ khi mở lại ứng dụng.
- Lỗi Jira hoặc cảnh báo dài: thông báo vẫn đọc được, không che mất các điều khiển cần thiết.
- Bảng đã lưu không có ticket hoặc chưa có bảng nào: giao diện giải thích tình trạng và cho biết bước tiếp theo phù hợp.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Các màn hình truy vấn, cài đặt, bảng đã lưu và lịch PHẢI tuân theo một ngôn ngữ thị giác nhất quán theo Material Design về thứ bậc nội dung, typography, màu sắc, khoảng cách, hình dạng và trạng thái tương tác.
- **FR-002**: Giao diện PHẢI làm rõ điều hướng hiện tại, tiêu đề màn hình và thao tác chính; các thao tác phụ không được cạnh tranh thị giác với thao tác chính.
- **FR-003**: Các nút, trường nhập, bảng, lựa chọn, hộp thoại và thông báo PHẢI có trạng thái mặc định, đang thao tác, vô hiệu hoá, lỗi hoặc đã chọn phù hợp với ngữ cảnh.
- **FR-004**: Các luồng truy vấn Jira, tuỳ chỉnh bảng, lưu bảng, quản lý bảng đã lưu, mở lịch và cấu hình kết nối hiện có PHẢI tiếp tục thực hiện được sau thay đổi giao diện; thay đổi này không được làm đổi quy tắc nghiệp vụ hay dữ liệu.
- **FR-005**: Trạng thái tải, kết quả rỗng, lỗi, cảnh báo và thành công PHẢI được thể hiện bằng văn bản hoặc biểu tượng có ngữ nghĩa rõ ràng, không chỉ dựa vào màu sắc.
- **FR-006**: Thao tác phá huỷ dữ liệu hoặc ngắt kết nối PHẢI được phân biệt rõ với thao tác thường và giữ cơ chế xác nhận hiện có.
- **FR-007**: Biểu mẫu PHẢI có nhãn dễ hiểu, trạng thái hợp lệ/lỗi gắn với trường liên quan và giữ dữ liệu người dùng cần sửa sau lỗi; token đã lưu không được hiển thị nguyên văn.
- **FR-008**: Bố cục PHẢI thích ứng với kích thước cửa sổ Desktop thường dùng, duy trì khả năng truy cập các thao tác và nội dung trọng yếu mà không để các vùng giao diện chồng lấn.
- **FR-009**: Các thao tác chính PHẢI sử dụng được bằng bàn phím với thứ tự di chuyển hợp lý và chỉ báo focus rõ ràng.
- **FR-010**: Văn bản, biểu tượng và vùng tương tác PHẢI duy trì khả năng đọc, nhận biết và thao tác phù hợp với hướng dẫn trợ năng của Material Design; trạng thái không được truyền tải chỉ bằng màu sắc.
- **FR-011**: Thông báo phản hồi sau thao tác PHẢI xuất hiện gần ngữ cảnh liên quan, diễn đạt kết quả và cung cấp hành động tiếp theo khi có thể.
- **FR-012**: Giao diện PHẢI giữ các hành vi đang có về cuộn dọc/ngang, cố định cột, sắp xếp, lọc và mở ticket trong các khu vực tương ứng.
- **FR-013**: Giao diện PHẢI mặc định theo chế độ sáng/tối của hệ điều hành và cho phép người dùng chọn rõ chế độ sáng hoặc tối trong Cài đặt; lựa chọn riêng PHẢI được giữ lại cho lần mở ứng dụng tiếp theo.
- **FR-014**: Điều hướng chính giữa Truy vấn, Bảng đã lưu và Cài đặt PHẢI nằm trong thanh bên trái có thể thu gọn; trạng thái thu gọn không được làm mất nhãn hoặc khả năng nhận biết mục hiện tại.

### Key Entities

- **Màn hình ứng dụng**: Một khu vực công việc hiện có gồm truy vấn, cài đặt, bảng đã lưu hoặc lịch, cùng nội dung và thao tác tương ứng.
- **Thành phần giao diện**: Điều khiển hoặc vùng hiển thị dùng chung, có vai trò, trạng thái tương tác và cách trình bày nhất quán.
- **Trạng thái giao diện**: Trạng thái có ý nghĩa đối với người dùng như tải, thành công, lỗi, rỗng, đã chọn hoặc vô hiệu hoá.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% các khu vực truy vấn, cài đặt, bảng đã lưu và lịch sử dụng cùng quy tắc trình bày cho tiêu đề, thao tác chính, trạng thái và thông báo.
- **SC-002**: Trong kiểm thử các luồng chính, ít nhất 4/5 người dùng hoàn thành không cần trợ giúp các tác vụ: chạy truy vấn, mở bảng đã lưu, tìm Cài đặt và xác nhận/huỷ một thao tác phá huỷ.
- **SC-003**: 100% trạng thái tải, lỗi, rỗng, thành công và lựa chọn được kiểm tra có dấu hiệu văn bản hoặc biểu tượng ngoài màu sắc.
- **SC-004**: 100% thao tác chính của các màn hình trong phạm vi có thể hoàn thành bằng bàn phím và focus hiện tại luôn nhận biết được.
- **SC-005**: Ở các kích thước cửa sổ được kiểm tra, không có chồng lấn làm mất thao tác chính hoặc che hoàn toàn nội dung trọng yếu.
- **SC-006**: Toàn bộ kịch bản chức năng hiện có cho truy vấn, cấu hình Jira, bảng đã lưu và lịch tiếp tục đạt kết quả như trước sau khi cập nhật giao diện.

## Assumptions

- Phạm vi là các màn hình và trạng thái giao diện hiện có của ứng dụng Jira Tools, không bao gồm chức năng nghiệp vụ mới.
- Material Design là chuẩn thị giác và tương tác xuyên suốt ứng dụng; phiên bản đầu tập trung vào giao diện Desktop hiện đang được hỗ trợ.
- Các khu vực chính được điều hướng bằng thanh bên trái có thể thu gọn để ưu tiên chiều ngang cho bảng dữ liệu.
- Các hành vi, nội dung dữ liệu, quyền truy cập và quy tắc Jira hiện tại được giữ nguyên; chỉ cách trình bày và khả năng sử dụng được cải thiện.
- Giao diện phải giữ nguyên khả năng cuộn và sử dụng bảng lớn; không thay thế các thao tác hiện có bằng một luồng mới.
- Không yêu cầu thêm tuỳ chọn cá nhân hoá ngoài chế độ màu sáng/tối đã được xác định.
- Nếu người dùng chưa chọn chế độ màu riêng, ứng dụng theo chế độ hệ điều hành; khi đã chọn riêng, lựa chọn đó được ưu tiên cho các lần sử dụng sau.
