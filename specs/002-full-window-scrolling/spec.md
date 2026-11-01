# Feature Specification: Mở cửa sổ tối đa và cuộn nội dung

**Feature Branch**: `002-full-window-scrolling`
**Created**: 2026-10-31
**Status**: Draft
**Input**: User description: "mở full window và có thể scroll"

## Clarifications

### Session 2026-10-31

- Q: Khi nội dung cao hơn vùng hiển thị, người dùng cuộn toàn bộ trang như một tài liệu dài, hay danh sách ticket vẫn tự cuộn riêng và thanh cuộn trang chỉ dùng để tiếp cận phần nội dung bị cắt? → A: Phương án C — không cuộn toàn trang; biểu mẫu kết nối và ô nhập JQL luôn cố định trên đỉnh, chỉ bảng kết quả cuộn; khi cửa sổ quá thấp thì phần trên thu gọn lại.
- Q: Khi cửa sổ quá thấp để chứa cả vùng điều khiển lẫn một bảng dùng được, vùng điều khiển nên thu gọn theo cách nào? → A: Phương án A — chỉ biểu mẫu kết nối thu gọn thành một thanh một dòng hiển thị trạng thái kết nối và mở rộng lại được; ô nhập JQL và nút chạy truy vấn luôn hiển thị đầy đủ.
- Q: Khi danh sách ticket vượt khung nhìn, chỉ báo vị trí cuộn có bắt buộc luôn nhìn thấy, hay chỉ cần theo quy ước thanh cuộn của hệ điều hành (có thể tự ẩn khi người dùng không tương tác)? → A: Phương án A — dùng thanh cuộn theo quy ước hệ điều hành; thanh cuộn phản ánh đúng vị trí cuộn khi người dùng tương tác và được phép tự ẩn khi rảnh, không cần chỉ báo tự vẽ luôn hiển thị.
- Q: Khi người dùng cuộn ngang để xem các cột còn lại, cột KEY (mã ticket) có nên được giữ cố định ở bên trái không? → A: Phương án A — giữ cố định cột mã ticket ở bên trái khi bảng cuộn ngang; các cột còn lại trượt ngang bên dưới nó.
- Q: Kích thước cửa sổ tối thiểu nên được đặt cụ thể là bao nhiêu để ô nhập JQL, nút chạy truy vấn và bảng kết quả vẫn dùng được? → A: Phương án A — kích thước cửa sổ tối thiểu là 1024 x 720 điểm ảnh logic.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cửa sổ mở rộng tối đa khi khởi động (Priority: P1)

Người dùng mở ứng dụng và cửa sổ chính lập tức chiếm toàn bộ vùng màn hình khả dụng, thay vì mở ở một kích thước nhỏ mặc định, nhờ đó biểu mẫu kết nối và bảng kết quả có tối đa không gian hiển thị ngay từ lần đầu.

**Why this priority**: Đây là yêu cầu trực tiếp của người dùng và là điều kiện để phần lớn nội dung hiển thị đầy đủ mà không cần thao tác thủ công. Nếu chỉ làm được một việc, đây là việc mang lại giá trị rõ ràng nhất.

**Independent Test**: Khởi động ứng dụng trên một màn hình đã biết độ phân giải, xác nhận cửa sổ chiếm trọn vùng màn hình khả dụng (trừ thanh hệ thống), vẫn có thanh tiêu đề và các nút điều khiển chuẩn, và có thể khôi phục về kích thước thường rồi thay đổi kích thước bình thường.

**Acceptance Scenarios**:

1. **Given** ứng dụng chưa chạy, **When** người dùng khởi động ứng dụng, **Then** cửa sổ chính mở ra ở trạng thái mở rộng tối đa, chiếm toàn bộ vùng màn hình khả dụng của màn hình hiện hành.
2. **Given** cửa sổ đang mở rộng tối đa, **When** người dùng khôi phục cửa sổ về kích thước thường bằng nút điều khiển của hệ điều hành, **Then** cửa sổ trở về kích thước nhỏ hơn, nội dung vẫn hiển thị đúng và dùng được.
3. **Given** cửa sổ đang ở kích thước thường, **When** người dùng thay đổi kích thước cửa sổ, **Then** nội dung bố cục lại theo kích thước mới mà không mất dữ liệu đang hiển thị.

### User Story 2 - Cuộn dọc trong bảng kết quả, vùng điều khiển luôn cố định (Priority: P1)

Vùng điều khiển phía trên — biểu mẫu kết nối, ô nhập JQL và nút chạy truy vấn — luôn nằm cố định trong tầm nhìn, không trôi đi khi người dùng duyệt kết quả. Toàn bộ thao tác cuộn dọc diễn ra bên trong bảng kết quả. Khi cửa sổ quá thấp để chứa cả biểu mẫu kết nối và một bảng dùng được, biểu mẫu kết nối thu gọn thành một thanh một dòng thay vì chiếm mất chỗ của bảng, trong khi ô nhập JQL và nút chạy truy vấn vẫn hiển thị đầy đủ.

**Why this priority**: "Có thể scroll" là yêu cầu trực tiếp của người dùng, và cách hiểu đã chốt là cuộn trong bảng chứ không phải cuộn cả trang. Giữ vùng điều khiển cố định cho phép người dùng chạy truy vấn mới ngay khi đang xem kết quả dài, đồng thời bảo đảm mọi nội dung vẫn tiếp cận được trên màn hình thấp.

**Independent Test**: Chạy một truy vấn trả về danh sách dài, cuộn tới cuối bảng, rồi xác nhận biểu mẫu kết nối và ô nhập JQL vẫn nguyên vị trí và dùng được; sau đó thu nhỏ cửa sổ hết mức cho phép và xác nhận biểu mẫu kết nối thu gọn thành một thanh một dòng còn ô nhập JQL và nút chạy truy vấn vẫn hiển thị đầy đủ.

**Acceptance Scenarios**:

1. **Given** danh sách ticket dài hơn vùng hiển thị của bảng, **When** người dùng cuộn bằng con lăn chuột hoặc bàn di chuột trong vùng bảng, **Then** danh sách ticket dịch chuyển theo chiều dọc và biểu mẫu kết nối cùng ô nhập JQL giữ nguyên vị trí.
2. **Given** danh sách ticket dài hơn vùng hiển thị của bảng, **When** người dùng kéo thanh cuộn dọc của bảng, **Then** danh sách dịch chuyển tương ứng và vị trí cuộn hiện tại được phản ánh trên thanh cuộn.
3. **Given** cửa sổ bị thu nhỏ tới mức không đủ chỗ cho biểu mẫu kết nối đầy đủ cùng một bảng dùng được, **When** người dùng quan sát cửa sổ, **Then** biểu mẫu kết nối thu gọn thành một thanh một dòng hiển thị trạng thái kết nối, ô nhập JQL và nút chạy truy vấn vẫn hiển thị đầy đủ, và bảng vẫn giữ được vùng hiển thị tối thiểu để cuộn.
4. **Given** biểu mẫu kết nối đang ở trạng thái thu gọn, **When** người dùng cần kiểm tra hoặc thay đổi cấu hình kết nối, **Then** người dùng mở rộng được biểu mẫu và mọi trường cùng nút bên trong vẫn tiếp cận được và dùng được.
5. **Given** biểu mẫu kết nối đang ở trạng thái thu gọn, **When** người dùng chạy một truy vấn mới bằng ô nhập JQL và nút chạy truy vấn, **Then** truy vấn chạy bình thường mà không cần mở rộng biểu mẫu.
6. **Given** danh sách ticket ngắn hơn vùng hiển thị của bảng, **When** người dùng quan sát bảng, **Then** toàn bộ kết quả hiển thị đầy đủ và không xuất hiện thanh cuộn dọc không cần thiết.

### User Story 3 - Cuộn ngang để xem đủ cột của bảng (Priority: P2)

Khi người dùng bật hiển thị nhiều cột và cửa sổ hẹp hơn tổng chiều rộng các cột, người dùng cuộn ngang trong vùng bảng để xem những cột nằm ngoài tầm nhìn thay vì buộc phải tắt bớt cột; cột mã ticket được giữ cố định bên trái để người dùng luôn biết mỗi hàng thuộc ticket nào.

**Why this priority**: Bổ sung trực tiếp cho yêu cầu cuộn, nhưng chỉ phát sinh khi cửa sổ hẹp với nhiều cột. Giá trị thấp hơn cuộn dọc vì người dùng vẫn có lối thoát tạm thời là ẩn bớt cột.

**Independent Test**: Bật đủ sáu cột, thu hẹp cửa sổ tới mức các cột không còn vừa, rồi xác nhận có thể cuộn ngang tới cột cuối cùng, tiêu đề cột vẫn thẳng hàng với dữ liệu, và cột mã ticket vẫn đứng yên bên trái.

**Acceptance Scenarios**:

1. **Given** các cột đang hiển thị rộng hơn vùng bảng, **When** người dùng cuộn ngang, **Then** các cột bị khuất xuất hiện và mọi cột đều xem được.
2. **Given** bảng đang cuộn ngang, **When** người dùng quan sát một dòng bất kỳ, **Then** giá trị từng ô vẫn nằm thẳng hàng dưới tiêu đề cột tương ứng.
3. **Given** bảng đang phải cuộn ngang vì thiếu chỗ, **When** người dùng ẩn bớt cột cho tới khi các cột còn lại vừa với chiều rộng cửa sổ, **Then** bảng không còn cần cuộn ngang và hiển thị gọn trong khung nhìn.
4. **Given** bảng đang phải cuộn ngang, **When** người dùng cuộn tới cột ngoài cùng bên phải, **Then** cột mã ticket vẫn hiển thị nguyên vị trí ở bên trái và mỗi hàng vẫn nhận biết được là ticket nào.

### Edge Cases

- Cửa sổ bị thu nhỏ xuống dưới kích thước tối thiểu 1024 x 720 điểm ảnh logic: ứng dụng ngăn không cho thu nhỏ hơn mức bảo đảm các thành phần cốt lõi còn dùng được.
- Màn hình độ phân giải thấp hoặc có tỉ lệ hiển thị khác thường: cửa sổ vẫn mở trong vùng hiển thị, biểu mẫu kết nối thu gọn khi cần và mọi nội dung vẫn tiếp cận được.
- Biểu mẫu kết nối đổi trạng thái thu gọn hoặc mở rộng trong khi người dùng đang nhập: dữ liệu đã nhập trong các trường và kết quả kiểm tra kết nối gần nhất không bị mất.
- Cửa sổ được mở trên màn hình phụ, hoặc màn hình phụ bị ngắt kết nối: cửa sổ vẫn nằm trong vùng hiển thị của màn hình còn lại.
- Truy vấn JQL dài hơn chiều rộng ô nhập: nội dung ô nhập tự cuộn bên trong và không đẩy các thành phần khác ra ngoài tầm nhìn.
- Kết quả lớn (khoảng 250 ticket): cuộn vẫn mượt và không làm giật giao diện.
- Người dùng cuộn trong khi truy vấn đang chạy hoặc đang tải thêm kết quả: thao tác cuộn không hủy truy vấn và trạng thái đang tải vẫn hiển thị đúng.
- Cửa sổ được thay đổi kích thước trong khi dữ liệu đang tải: nội dung vẫn cập nhật đúng, không xuất hiện chồng lấn hay khoảng trắng bất thường.
- Người dùng chạy truy vấn mới khi đang cuộn ở giữa danh sách: trang kết quả mới hiển thị từ đầu, không giữ vị trí cuộn cũ.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Khi khởi động, ứng dụng PHẢI mở cửa sổ chính ở trạng thái mở rộng tối đa, chiếm toàn bộ vùng màn hình khả dụng của màn hình hiện hành. (US1)
- **FR-002**: Cửa sổ PHẢI giữ thanh tiêu đề và các nút điều khiển chuẩn của hệ điều hành để người dùng thu nhỏ, khôi phục kích thước thường, thay đổi kích thước và đóng. (US1)
- **FR-003**: Khi người dùng thay đổi kích thước cửa sổ, nội dung PHẢI bố cục lại theo kích thước mới, không mất dữ liệu đang hiển thị, không tự chạy lại truy vấn và không đặt lại lựa chọn cột hay thứ tự sắp xếp. (US1)
- **FR-004**: Vùng điều khiển phía trên — biểu mẫu cấu hình kết nối, ô nhập JQL và nút chạy truy vấn — PHẢI luôn nằm cố định trong tầm nhìn và KHÔNG ĐƯỢC trôi đi khi người dùng cuộn danh sách ticket; biểu mẫu kết nối có thể ở trạng thái thu gọn nhưng vẫn PHẢI nhìn thấy được. (US2)
- **FR-005**: Khi danh sách ticket vượt chiều cao vùng hiển thị của bảng, người dùng PHẢI cuộn được theo chiều dọc trong vùng bảng để tiếp cận mọi ticket đã tải; ứng dụng KHÔNG ĐƯỢC dùng cuộn toàn trang làm cách tiếp cận nội dung. (US2)
- **FR-006**: Cuộn dọc trong vùng bảng PHẢI hoạt động với con lăn chuột, bàn di chuột và phím điều hướng; khi danh sách vượt khung nhìn, thanh cuộn PHẢI phản ánh đúng vị trí cuộn hiện tại trong lúc người dùng tương tác và ĐƯỢC PHÉP tự ẩn khi người dùng không tương tác. (US2)
- **FR-007**: Khi cửa sổ không đủ cao để hiển thị đầy đủ biểu mẫu kết nối cùng một vùng bảng dùng được, biểu mẫu kết nối PHẢI thu gọn thành một thanh một dòng hiển thị trạng thái kết nối hiện tại và PHẢI mở rộng lại được khi người dùng cần; ô nhập JQL và nút chạy truy vấn KHÔNG ĐƯỢC thu gọn. (US2)
- **FR-008**: Khi tổng chiều rộng các cột đang hiển thị vượt chiều rộng khả dụng của vùng bảng, người dùng PHẢI cuộn được theo chiều ngang để xem các cột còn lại. (US3)
- **FR-009**: Khi bảng được cuộn ngang, hàng tiêu đề cột và các hàng dữ liệu PHẢI giữ thẳng hàng với nhau. (US3)
- **FR-010**: Khi người dùng thay đổi tập cột hiển thị tới mức các cột còn lại vừa với chiều rộng cửa sổ, bảng PHẢI hiển thị gọn trong khung nhìn mà không cần cuộn ngang. (US3)
- **FR-011**: Khi bảng phải cuộn ngang, cột mã ticket PHẢI được giữ cố định ở bên trái, luôn hiển thị đầy đủ cho mọi hàng, trong khi các cột còn lại trượt ngang. (US3)
- **FR-012**: Ứng dụng PHẢI áp đặt kích thước cửa sổ tối thiểu là 1024 x 720 điểm ảnh logic, sao cho các thành phần cốt lõi (ô nhập JQL, nút chạy truy vấn, bảng kết quả) không bị chồng lấn hoặc không dùng được ở kích thước nhỏ nhất. (US2)
- **FR-013**: Trạng thái đang tải, trạng thái không có kết quả, thông báo lỗi và nút tải thêm PHẢI tiếp tục hiển thị và sử dụng được trong khi cuộn. (US2)
- **FR-014**: Thao tác cuộn PHẢI không hủy truy vấn đang chạy, không hủy việc tải thêm kết quả và không làm mất dữ liệu đã tải. (US2)
- **FR-015**: Các tính năng hiện có (chạy truy vấn JQL, sắp xếp theo cột, ẩn/hiện cột, mở ticket trong trình duyệt, tải thêm kết quả, ghi nhớ lựa chọn cột và truy vấn gần nhất) PHẢI giữ nguyên hành vi sau thay đổi này. (US1, US2, US3)

### Key Entities

- **Cửa sổ ứng dụng (Application Window)**: vùng hiển thị chính của ứng dụng, có trạng thái hiển thị (mở rộng tối đa hoặc kích thước thường), kích thước hiện tại và kích thước tối thiểu cho phép.
- **Biểu mẫu kết nối (Connection Form)**: phần đầu cửa sổ chứa các trường cấu hình kết nối cùng trạng thái kết nối; có hai trạng thái hiển thị — đầy đủ, và thu gọn thành một thanh một dòng.
- **Thanh truy vấn (Query Bar)**: ô nhập JQL cùng nút chạy truy vấn; luôn hiển thị đầy đủ ở mọi kích thước cửa sổ hợp lệ.
- **Bảng kết quả cuộn (Scrollable Result Table)**: bảng ticket với tập cột đang hiển thị có thể cao hơn chiều cao vùng bảng và rộng hơn chiều rộng vùng bảng, cùng vị trí cuộn dọc và cuộn ngang hiện tại; cột mã ticket nằm ngoài vùng cuộn ngang nên luôn giữ nguyên vị trí bên trái.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Trong 100% lần khởi động, cửa sổ chính xuất hiện ở trạng thái mở rộng tối đa ngay khi ứng dụng sẵn sàng, không cần người dùng phóng to thủ công.
- **SC-002**: Ở kích thước cửa sổ nhỏ nhất được hỗ trợ (1024 x 720 điểm ảnh logic), 100% vùng nội dung đều tiếp cận được — biểu mẫu kết nối thu gọn thành một thanh một dòng, ô nhập JQL và nút chạy truy vấn vẫn hiển thị đầy đủ, và bảng vẫn cuộn được; không phần tử nào nằm vĩnh viễn ngoài tầm tiếp cận.
- **SC-003**: Với bảng hiển thị đủ sáu cột trong cửa sổ hẹp hơn tổng chiều rộng cột, người dùng tới được cột ngoài cùng bên phải bằng tối đa hai thao tác cuộn ngang.
- **SC-004**: Thao tác cuộn phản hồi tức thời — nội dung dịch chuyển trong vòng dưới 100 mili giây kể từ thao tác của người dùng — khi danh sách đang hiển thị khoảng 250 ticket.
- **SC-005**: Thay đổi kích thước cửa sổ không gây mất mát: sau mỗi lần thay đổi kích thước, 100% ticket đang hiển thị vẫn còn, lựa chọn cột và thứ tự sắp xếp không đổi, và không phát sinh truy vấn mới tới Jira.
- **SC-006**: Trong một lần thử với tối thiểu 5 người dùng lần đầu sử dụng, tối thiểu 4 người nhận ra bảng có thể cuộn ngang để xem cột bị khuất trong vòng 10 giây mà không cần hướng dẫn.
- **SC-007**: 100% kịch bản kiểm thử hiện có của ứng dụng vẫn đạt sau thay đổi này, xác nhận không có hồi quy hành vi.
- **SC-008**: Khi bảng được cuộn ngang tới vị trí xa nhất, 100% hàng đang hiển thị vẫn cho thấy cột mã ticket ở bên trái, không hàng nào mất định danh.

## Assumptions

- "Mở full window" được hiểu là cửa sổ mở rộng tối đa chiếm vùng màn hình khả dụng nhưng vẫn giữ thanh tiêu đề và các nút điều khiển chuẩn của hệ điều hành; chế độ toàn màn hình độc quyền không viền (kiosk) nằm ngoài phạm vi.
- Không yêu cầu ghi nhớ kích thước và vị trí cửa sổ giữa các lần chạy, vì cửa sổ luôn mở ở trạng thái mở rộng tối đa.
- Nền tảng đích của phiên bản này là Desktop theo hiến pháp dự án; các nền tảng khác nằm ngoài phạm vi.
- Thay đổi này không thay đổi luồng nghiệp vụ hiện có (kết nối, truy vấn JQL, bảng kết quả, ẩn/hiện cột, sắp xếp, mở ticket, tải thêm); chỉ mở rộng cách người dùng tiếp cận nội dung.
- Thanh cuộn tuân theo quy ước hiển thị của hệ điều hành (có thể tự ẩn khi người dùng không tương tác).
- Người dùng điều khiển bằng chuột, bàn di chuột hoặc bàn phím; thiết bị cảm ứng và cử chỉ chạm nằm ngoài phạm vi phiên bản này.
- Không phát sinh dữ liệu mới cần lưu trữ; vị trí cuộn không cần ghi nhớ giữa các phiên làm việc.
