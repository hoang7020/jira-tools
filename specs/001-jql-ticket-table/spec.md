# Feature Specification: Truy vấn Jira bằng JQL và hiển thị kết quả dạng bảng

**Feature Branch**: `001-jql-ticket-table`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "viết một ứng dụng có thể query data bằng jql và trả về một list các ticket, sau đó hiển thị dưới dạng bảng"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cấu hình kết nối tới Jira (Priority: P1)

Người dùng mở ứng dụng lần đầu, nhập địa chỉ instance Jira và thông tin xác thực, rồi kiểm tra
kết nối. Ứng dụng xác nhận thành công và ghi nhớ cấu hình cho các lần sử dụng sau.

**Why this priority**: Không có kết nối thì không thể truy vấn dữ liệu; đây là điều kiện tiên
quyết cho mọi tính năng khác và chỉ cần thiết lập một lần.

**Independent Test**: Mở ứng dụng ở trạng thái chưa cấu hình, nhập địa chỉ Jira hợp lệ cùng thông
tin xác thực đúng, chạy kiểm tra kết nối và nhận thông báo thành công; sau đó mở lại ứng dụng để
xác nhận cấu hình vẫn hoạt động mà không cần nhập lại.

**Acceptance Scenarios**:

1. **Given** ứng dụng chưa có cấu hình Jira, **When** người dùng nhập địa chỉ instance hợp lệ và thông tin xác thực đúng, **Then** ứng dụng xác nhận kết nối thành công và lưu cấu hình để dùng lại.
2. **Given** cấu hình đã lưu từ lần trước, **When** người dùng mở lại ứng dụng, **Then** ứng dụng sẵn sàng truy vấn mà không yêu cầu nhập lại thông tin xác thực.
3. **Given** địa chỉ instance sai hoặc thông tin xác thực không hợp lệ, **When** người dùng kiểm tra kết nối, **Then** ứng dụng hiển thị thông báo lỗi phân biệt được nguyên nhân (địa chỉ sai, xác thực sai, không kết nối được) và không lưu cấu hình lỗi.

---

### User Story 2 - Chạy truy vấn JQL và xem kết quả dạng bảng (Priority: P1)

Người dùng nhập một truy vấn JQL, thực thi và xem danh sách ticket khớp điều kiện dưới dạng bảng
với các cột thông dụng như key, summary, status, assignee, priority, updated.

**Why this priority**: Đây là giá trị cốt lõi của tính năng - biến truy vấn JQL thành bảng ticket
dễ đọc để rà soát nhanh.

**Independent Test**: Với cấu hình kết nối sẵn có, chạy một JQL, xác nhận bảng hiển thị đúng
ticket, đúng cột mặc định và đúng tổng số kết quả; chạy truy vấn trả về 0 kết quả và xác nhận
thông báo rỗng hiển thị đúng.

**Acceptance Scenarios**:

1. **Given** kết nối Jira đã cấu hình, **When** người dùng nhập JQL hợp lệ và thực thi, **Then** ứng dụng hiển thị bảng ticket với các cột mặc định, kèm tổng số ticket khớp truy vấn.
2. **Given** truy vấn trả về nhiều hơn một trang kết quả, **When** người dùng duyệt trang hoặc cuộn, **Then** người dùng tiếp cận được toàn bộ kết quả và tổng số hiển thị khớp với Jira.
3. **Given** truy vấn không khớp ticket nào, **When** người dùng thực thi, **Then** ứng dụng hiển thị thông báo không có kết quả rõ ràng cùng truy vấn đã nhập.
4. **Given** người dùng nhập JQL sai cú pháp, **When** người dùng thực thi, **Then** ứng dụng hiển thị thông báo lỗi dễ hiểu và giữ nguyên nội dung truy vấn để chỉnh sửa.
5. **Given** truy vấn đang được thực thi, **When** người dùng chờ, **Then** ứng dụng hiển thị trạng thái đang tải và giao diện vẫn phản hồi các thao tác khác.

---

### User Story 3 - Tùy biến bảng kết quả (Priority: P2)

Người dùng chọn các cột muốn xem và sắp xếp bảng theo cột để tập trung vào thông tin cần thiết.

**Why this priority**: Tăng hiệu quả sử dụng hằng ngày nhưng không chặn giá trị cốt lõi; bảng mặc
định ở User Story 2 đã đủ dùng cho phiên bản đầu.

**Independent Test**: Chạy một truy vấn, bật/tắt vài cột, sắp xếp theo một cột; xác nhận bảng cập
nhật tương ứng và lựa chọn cột còn nguyên khi mở lại ứng dụng.

**Acceptance Scenarios**:

1. **Given** bảng kết quả đang hiển thị, **When** người dùng bật hoặc tắt một cột, **Then** bảng cập nhật ngay và lựa chọn cột được ghi nhớ cho lần sử dụng sau.
2. **Given** bảng kết quả đang hiển thị, **When** người dùng sắp xếp theo một cột, **Then** các dòng được sắp xếp theo cột đó và chiều sắp xếp được chỉ báo rõ ràng.

---

### User Story 4 - Mở ticket trực tiếp trong Jira (Priority: P3)

Người dùng bấm vào một dòng trong bảng để mở ticket tương ứng trong trình duyệt mặc định.

**Why this priority**: Tiện ích nhỏ nhưng hữu ích khi rà soát; không ảnh hưởng giá trị cốt lõi.

**Independent Test**: Chạy truy vấn, bấm một dòng và xác nhận trình duyệt mặc định mở đúng trang
ticket trên Jira.

**Acceptance Scenarios**:

1. **Given** bảng kết quả đang hiển thị, **When** người dùng bấm vào một ticket, **Then** trình duyệt mặc định mở trang ticket tương ứng trên Jira.

---

### Edge Cases

- Instance Jira không phản hồi hoặc phản hồi chậm: ứng dụng hiển thị lỗi hết thời gian chờ và cho phép thử lại, không treo giao diện.
- Thông tin xác thực hết hạn hoặc bị thu hồi giữa các lần sử dụng: ứng dụng thông báo cần cập nhật xác thực thay vì hiển thị lỗi khó hiểu.
- Truy vấn trả về số lượng rất lớn (hàng nghìn ticket): ứng dụng vẫn phản hồi và người dùng duyệt được toàn bộ kết quả.
- JQL chứa ký tự đặc biệt hoặc Unicode: truy vấn được gửi đi nguyên vẹn và kết quả hiển thị đúng.
- Ticket thiếu dữ liệu ở một số cột (ví dụ assignee hoặc priority trống): bảng hiển thị ô trống hợp lý thay vì lỗi hoặc giá trị giả.
- Nội dung cột quá dài (ví dụ summary dài): bố cục bảng không bị phá vỡ và nội dung vẫn nhận biết được.
- Địa chỉ instance Jira Cloud có hoặc không có tiền tố `https://` và dấu `/` cuối: kết nối vẫn thiết lập đúng.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép người dùng cấu hình địa chỉ instance Jira và thông tin xác thực, và PHẢI kiểm tra kết nối trước khi coi cấu hình là hợp lệ. (US1)
- **FR-002**: Hệ thống PHẢI kết nối được tới Jira Cloud bằng xác thực email + API token. (US1)
- **FR-003**: Thông tin xác thực PHẢI được lưu trữ an toàn trên máy người dùng và KHÔNG ĐƯỢC xuất hiện trong log hoặc tệp dưới dạng văn bản thuần. (US1)
- **FR-004**: Người dùng PHẢI có thể nhập truy vấn JQL tùy ý và thực thi bằng một thao tác rõ ràng (nút chạy hoặc phím tắt). (US2)
- **FR-005**: Hệ thống PHẢI hiển thị kết quả truy vấn dưới dạng bảng, mỗi dòng tương ứng một ticket. (US2)
- **FR-006**: Bảng PHẢI có các cột mặc định: key, summary, status, assignee, priority, updated. (US2)
- **FR-007**: Hệ thống PHẢI hiển thị tổng số ticket khớp truy vấn và cho phép người dùng tiếp cận toàn bộ kết quả vượt quá một trang. (US2)
- **FR-008**: Khi truy vấn không có kết quả, hệ thống PHẢI hiển thị thông báo không có kết quả rõ ràng cùng truy vấn đã nhập. (US2)
- **FR-009**: Khi JQL sai cú pháp hoặc Jira trả về lỗi, hệ thống PHẢI hiển thị thông báo lỗi dễ hiểu, giữ nguyên truy vấn đã nhập và cho phép thử lại. (US2)
- **FR-010**: Trong khi truy vấn đang chạy, hệ thống PHẢI hiển thị trạng thái đang tải và giao diện PHẢI tiếp tục phản hồi. (US2)
- **FR-011**: Người dùng PHẢI có thể bật/tắt từng cột hiển thị và sắp xếp bảng theo cột. (US3)
- **FR-012**: Lựa chọn cột hiển thị PHẢI được ghi nhớ giữa các phiên làm việc. (US3)
- **FR-013**: Người dùng PHẢI mở được ticket trong trình duyệt mặc định từ bảng kết quả. (US4)
- **FR-014**: Hệ thống PHẢI ghi nhớ truy vấn JQL của lần chạy gần nhất và khôi phục vào ô nhập khi mở lại ứng dụng. (US2)

### Key Entities

- **Cấu hình kết nối (Jira Connection)**: thông tin để truy cập một instance Jira - địa chỉ instance, loại xác thực, thông tin xác thực (lưu an toàn), trạng thái kết nối gần nhất. Mỗi người dùng có một cấu hình trong phiên bản này.
- **Truy vấn JQL (JQL Query)**: chuỗi JQL do người dùng nhập cho mỗi lần truy vấn; lần chạy gần nhất được ghi nhớ kèm số lượng kết quả.
- **Ticket**: một issue trả về từ Jira với các thuộc tính hiển thị key, summary, status, assignee, priority, updated và liên kết tới trang ticket trên Jira. Chỉ đọc trong phạm vi tính năng này.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng mới hoàn thành cấu hình kết nối và chạy truy vấn JQL thành công đầu tiên trong vòng dưới 5 phút kể từ lần mở ứng dụng đầu tiên.
- **SC-002**: Với truy vấn trả về tối đa 100 ticket, bảng kết quả hiển thị trong vòng dưới 5 giây trên kết nối mạng ổn định thông thường.
- **SC-003**: 100% ticket trong trang kết quả hiện tại được hiển thị đầy đủ và tổng số hiển thị khớp với số Jira báo về, không mất dòng.
- **SC-004**: Ở cả 5 nhóm lỗi phổ biến (JQL sai cú pháp, xác thực hết hạn, địa chỉ sai, mạng gián đoạn, thiếu quyền truy vấn), ứng dụng hiển thị thông báo phân biệt được nguyên nhân và không bị treo hoặc thoát bất thường.
- **SC-005**: Người dùng tiếp cận được ticket ở vị trí thứ 250 của kết quả chỉ với tối đa 3 thao tác điều hướng, không cần chạy lại truy vấn.
- **SC-006**: Người dùng đã cấu hình sẵn chạy xong một truy vấn ngắn (dưới 100 kết quả) trong vòng dưới 10 giây tính từ lúc bắt đầu nhập JQL.

## Assumptions

- Nền tảng đích của phiên bản này là Desktop theo hiến pháp dự án; các nền tảng khác nằm ngoài phạm vi.
- Người dùng có tài khoản Jira với quyền tìm kiếm trên các project sẽ truy vấn.
- Tính năng là chỉ đọc: tạo, sửa, xóa hoặc chuyển trạng thái ticket nằm ngoài phạm vi phiên bản này.
- Mỗi người dùng cấu hình một instance Jira duy nhất; hỗ trợ nhiều hồ sơ kết nối nằm ngoài phạm vi.
- Xác thực dùng email + API token của Jira Cloud (người dùng tạo API token trong tài khoản Atlassian); luồng OAuth 2.0 nằm ngoài phạm vi phiên bản này.
- Xuất kết quả (CSV/Excel), lưu lịch sử nhiều truy vấn và chia sẻ kết quả nằm ngoài phạm vi phiên bản này.
- Giao diện ứng dụng dùng tiếng Việt; thuật ngữ Jira (key, summary, status, ...) giữ nguyên tiếng Anh.
- Ứng dụng truy cập Jira qua kết nối mạng thông thường của máy người dùng và không đi qua cấu hình proxy đặc biệt.
- Chi tiết kỹ thuật như kích thước trang kết quả hay cơ chế tải dữ liệu sẽ được quyết định ở giai đoạn thiết kế (plan); spec chỉ ràng buộc hành vi người dùng.
