# Nghiên cứu: Đồng bộ Due Date từ bảng đã lưu

## Cập nhật Due Date trên Jira

- **Decision**: Dùng Jira Cloud REST API v3 để sửa đúng issue bằng `PUT /rest/api/3/issue/{issueIdOrKey}`, body chỉ chứa trường `fields.duedate` ở dạng chuỗi ngày `yyyy-MM-dd`. Xem mọi mã HTTP 2xx (gồm 204 không có body) là thành công; lỗi HTTP/mạng chuyển qua cơ chế `JiraErrorMapper` hiện có.
- **Rationale**: Jira issue update hỗ trợ sửa một trường qua `fields`; 204 là phản hồi thành công hợp lệ và không yêu cầu đọc JSON response. Request chỉ chứa `duedate` tránh gửi lại hoặc ghi đè field khác. Endpoint theo key hoặc ID bám model Ticket hiện hữu.
- **Alternatives considered**: Cập nhật toàn bộ issue bị loại vì có thể ghi đè trường không liên quan; tìm nạp issue sau PUT để xác nhận bị loại vì thêm request không cần thiết cho API trả 2xx. Dùng HTTP fake xác minh method, URL, payload, auth header có mặt nhưng không lộ giá trị, success 204 và lỗi.
- **Nguồn chính thức**: [Atlassian Jira Cloud REST API v3 — Issues](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-put), tài liệu thao tác cập nhật issue.

## Xác định trạng thái cần đồng bộ

- **Decision**: Lấy `LocalDate` từ `Ticket.dynamicDueAtMillis` theo `SavedTable.jiraTimeZone`; so sánh với `Ticket.dueDate` đang lưu. Chỉ có hành động nếu target date hợp lệ và khác Due Date (bao gồm null). Thiếu/không hợp lệ timezone hoặc không có dynamic timestamp thì không có hành động.
- **Rationale**: Timestamp Dynamic Due Date biểu diễn thời điểm trong lịch làm việc Jira; dùng timezone lưu ở bảng tránh lệch ngày do máy người dùng. So sánh ngày lịch phù hợp với field Due Date date-only.
- **Alternatives considered**: So sánh chuỗi hiển thị hoặc dùng timezone thiết bị bị loại vì không đảm bảo ngày Jira đúng.

## Luồng thao tác, trạng thái và lưu snapshot

- **Decision**: Từ chối thao tác nếu bảng/catalog không ghi được hoặc kết nối hiện tại chưa xác thực/cùng Jira site; theo dõi các issue đang cập nhật độc lập và chặn trùng cùng issue. Sau khi Jira trả thành công, cập nhật Due Date trong bảng đang mở và ghi catalog hiện hành qua `SavedTableStore`. Lỗi Jira giữ snapshot cũ và cho phép thử lại. Lỗi ghi sau khi Jira đã thành công phải báo rõ “Jira đã cập nhật nhưng snapshot chưa lưu”; trạng thái trong phiên hiện tại phản ánh ngày đã cập nhật, còn kết quả lưu bền vững không được tuyên bố thành công.
- **Rationale**: Ghi Jira là tác động bên ngoài không thể rollback nếu file cục bộ lỗi; phân biệt hai kết quả giúp người dùng biết cần làm mới/lưu lại thay vì báo thành công giả. Tác vụ theo khóa issue tránh request lặp nhưng giữ khả năng cập nhật song song các hàng khác.
- **Alternatives considered**: Ghi file trước Jira bị loại vì sẽ tạo trạng thái snapshot sai nếu Jira từ chối; khoá toàn bảng bị loại vì không cần thiết và ngăn tác vụ khác.

## Tích hợp UI

- **Decision**: Mở rộng callback hàng của `TicketTable` để chỉ cung cấp thao tác update khi có sai khác; saved table truyền callback qua `SavedTablesArea` đến ViewModel. Hàng đang cập nhật hiển thị tiến trình; lỗi gắn với hàng hoặc thông báo trạng thái hiện có và cho phép thử lại.
- **Rationale**: Tái sử dụng bảng hiển thị hai chế độ query/saved; callback nullable theo hàng tránh thay đổi hành vi truy vấn thường. ViewModel giữ toàn bộ chính sách kết nối, ngày và lưu dữ liệu.
- **Alternatives considered**: Thêm UI cập nhật Jira trong cell nghiệp vụ hoặc tự cập nhật lúc mở bảng bị loại vì phá ranh giới UI/logic và quyền chủ động của người dùng.
