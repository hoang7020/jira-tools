# Feature Specification: Thùng rác bảng đã lưu

**Feature Branch**: `012-saved-table-trash`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "thêm nút thùng rác vào sidebar, khi xóa bảng đã lưu thì bảng sẽ được đưa vào thùng rác, trong thùng rác có thể recovery bảng đã xóa hoặc xóa hoàn toàn bảng"

## Clarifications

### Session 2026-11-01

- Q: Nếu khôi phục một bảng có tên trùng với bảng đang hoạt động, ứng dụng nên xử lý thế nào? → A: Yêu cầu nhập tên khác trước khi khôi phục.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Chuyển bảng đã lưu vào thùng rác (Priority: P1)

Người dùng chọn xoá một bảng đã lưu. Thay vì mất ngay, bảng được chuyển vào thùng rác và không còn xuất hiện trong danh sách bảng đang hoạt động.

**Why this priority**: Đây là bước thiết yếu để tránh mất dữ liệu ngoài ý muốn và tạo cơ sở cho việc khôi phục.

**Independent Test**: Tạo một bảng đã lưu, đưa bảng vào thùng rác, rồi xác nhận bảng không còn trong danh sách đang hoạt động nhưng xuất hiện trong thùng rác cùng dữ liệu nhận diện.

**Acceptance Scenarios**:

1. **Given** một bảng đã lưu đang hoạt động, **When** người dùng xác nhận xoá bảng, **Then** bảng được chuyển vào thùng rác và không còn trong danh sách đang hoạt động.
2. **Given** người dùng đang ở bất kỳ màn hình chính nào, **When** họ chọn nút Thùng rác trên sidebar, **Then** màn hình thùng rác mở và hiển thị các bảng đã xoá.
3. **Given** một bảng được chuyển vào thùng rác, **When** người dùng xem nội dung ở thùng rác, **Then** tên, JQL, ticket và lịch sử biểu đồ của bảng vẫn được giữ để có thể khôi phục.

### User Story 2 - Khôi phục hoặc xoá vĩnh viễn bảng (Priority: P2)

Người dùng mở thùng rác để khôi phục một bảng đã xoá hoặc xoá vĩnh viễn bảng không còn cần thiết.

**Why this priority**: Khôi phục hoàn tác việc xoá nhầm; xoá vĩnh viễn cho phép người dùng chủ động giải phóng dữ liệu không cần nữa.

**Independent Test**: Trong thùng rác, khôi phục một bảng và xác nhận nó trở lại danh sách với nội dung nguyên vẹn; sau đó xoá vĩnh viễn một bảng khác và xác nhận bảng đó không còn ở danh sách hoạt động hoặc thùng rác.

**Acceptance Scenarios**:

1. **Given** một bảng trong thùng rác và không có bảng đang hoạt động trùng tên, **When** người dùng chọn Khôi phục, **Then** bảng trở lại danh sách đang hoạt động với dữ liệu và lịch sử trước khi xoá.
2. **Given** tên bảng trong thùng rác đang được dùng bởi một bảng đang hoạt động, **When** người dùng chọn Khôi phục, **Then** ứng dụng yêu cầu chọn tên khác trước khi hoàn tất khôi phục và không ghi đè bảng đang hoạt động.
3. **Given** một bảng trong thùng rác, **When** người dùng chọn Xoá vĩnh viễn và xác nhận, **Then** bảng cùng toàn bộ ticket và lịch sử liên quan bị loại bỏ không thể khôi phục.
4. **Given** người dùng huỷ xác nhận xoá vĩnh viễn, **When** quay lại thùng rác, **Then** bảng vẫn còn nguyên trong thùng rác.
5. **Given** thùng rác không có bảng nào, **When** người dùng mở thùng rác, **Then** ứng dụng hiển thị trạng thái trống có giải thích.

### Edge Cases

- Thùng rác trống hoặc một bảng trong thùng rác có dữ liệu ticket rỗng.
- Có bảng đang hoạt động trùng tên với bảng được khôi phục; việc khôi phục phải an toàn và không ghi đè.
- Lưu thao tác chuyển vào thùng rác, khôi phục hoặc xoá vĩnh viễn thất bại; dữ liệu trước thao tác vẫn được giữ và người dùng nhận được thông báo lỗi.
- Người dùng chuyển vào thùng rác bảng đang mở; chi tiết bảng phải đóng và không tiếp tục hiển thị dữ liệu đã chuyển.
- Bảng trong thùng rác có lịch sử burndown; khôi phục phải giữ toàn bộ lịch sử, xoá vĩnh viễn phải xoá cả lịch sử.
- Người dùng mở màn hình thùng rác khi danh mục dữ liệu không đọc được; ứng dụng không được ghi đè dữ liệu hoặc hiển thị thao tác thành công giả.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Sidebar MUST cung cấp nút Thùng rác để người dùng mở danh sách bảng đã xoá.
- **FR-002**: Khi người dùng xác nhận xoá một bảng đang hoạt động, ứng dụng MUST chuyển bảng đó vào thùng rác thay vì xoá vĩnh viễn.
- **FR-003**: Bảng trong thùng rác MUST không xuất hiện trong danh sách bảng đang hoạt động và MUST không được dùng để mở lịch, biểu đồ hoặc làm mới cho đến khi khôi phục.
- **FR-004**: Thùng rác MUST hiển thị các bảng đã xoá cùng thông tin đủ để nhận diện và chọn thao tác.
- **FR-005**: Người dùng MUST có thể khôi phục riêng một bảng trong thùng rác về danh sách đang hoạt động.
- **FR-006**: Khôi phục MUST bảo toàn định danh bảng, JQL, ticket, cột hiển thị, thông tin múi giờ và toàn bộ lịch sử biểu đồ đã lưu.
- **FR-007**: Nếu tên bảng được khôi phục trùng với bảng đang hoạt động, ứng dụng MUST yêu cầu tên thay thế và MUST NOT ghi đè bảng đang hoạt động.
- **FR-008**: Người dùng MUST có thể yêu cầu xoá vĩnh viễn riêng một bảng trong thùng rác; ứng dụng MUST yêu cầu xác nhận rõ trước khi thực hiện.
- **FR-009**: Xoá vĩnh viễn MUST loại bỏ bảng khỏi cả danh sách hoạt động lẫn thùng rác, bao gồm ticket và lịch sử gắn với bảng đó.
- **FR-010**: Huỷ xác nhận xoá vĩnh viễn MUST giữ nguyên bảng trong thùng rác.
- **FR-011**: Nếu danh sách thùng rác rỗng, ứng dụng MUST hiển thị trạng thái trống giải thích được.
- **FR-012**: Khi thao tác chuyển vào thùng rác, khôi phục hoặc xoá vĩnh viễn không lưu thành công, ứng dụng MUST giữ nguyên trạng thái đã lưu trước thao tác và MUST thông báo lỗi cho người dùng.
- **FR-013**: Thao tác với thùng rác MUST không làm thay đổi nội dung các bảng khác.
- **FR-014**: Bảng đã xoá MUST được giữ trong thùng rác cho đến khi người dùng khôi phục hoặc xác nhận xoá vĩnh viễn; ứng dụng MUST NOT tự động xoá bảng trong thùng rác.

### Key Entities

- **Bảng đã lưu đang hoạt động**: Bảng người dùng có thể mở, xem lịch/biểu đồ và làm mới; có thể được chuyển vào thùng rác.
- **Bảng trong thùng rác**: Bảng đã bị xoá khỏi danh sách hoạt động nhưng vẫn giữ nguyên nội dung và lịch sử để khôi phục hoặc xoá vĩnh viễn.
- **Thùng rác**: Danh sách bảng đã xoá, mỗi bảng có thể được khôi phục hoặc xoá vĩnh viễn riêng.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng mở được thùng rác từ sidebar trong một thao tác.
- **SC-002**: Sau khi xác nhận chuyển bảng vào thùng rác, bảng biến mất khỏi danh sách hoạt động và xuất hiện trong thùng rác mà không mất ticket hoặc lịch sử.
- **SC-003**: Người dùng khôi phục được một bảng không xung đột tên trong tối đa hai thao tác từ màn hình thùng rác.
- **SC-004**: Tên trùng không bao giờ khiến thao tác khôi phục ghi đè một bảng đang hoạt động.
- **SC-005**: Xoá vĩnh viễn chỉ xảy ra sau xác nhận và làm bảng biến mất khỏi cả hai danh sách.
- **SC-006**: Trong kiểm thử thao tác lỗi lưu, 100% dữ liệu trước thao tác vẫn có thể đọc lại và giao diện không báo thành công.
- **SC-007**: Bảng khác không đổi sau khi người dùng chuyển, khôi phục hoặc xoá vĩnh viễn một bảng.

## Assumptions

- Bảng trong thùng rác được giữ vô thời hạn cho tới khi người dùng khôi phục hoặc xoá vĩnh viễn.
- Khôi phục một bảng có tên trùng sẽ yêu cầu người dùng nhập tên khác; tên mới phải tuân theo các quy tắc đặt tên bảng hiện hành.
- Thao tác chuyển bảng vào thùng rác tiếp tục dùng bước xác nhận xoá hiện có; thao tác xoá vĩnh viễn có bước xác nhận riêng, nêu rõ tính không thể hoàn tác.
- Thùng rác chứa các bảng đã lưu, không bao gồm lịch sử truy vấn hoặc cấu hình kết nối Jira.
- Dữ liệu bảng được khôi phục đầy đủ, bao gồm lịch sử burndown; xoá vĩnh viễn xoá cả lịch sử thuộc bảng đó.
