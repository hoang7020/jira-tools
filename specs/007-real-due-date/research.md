# Nghiên cứu: Ngày hoàn thành thực tế và quá hạn

## R1. Truy vấn lịch sử chuyển trạng thái Jira

**Quyết định**: Dùng Jira Cloud REST API v3 bulk changelog `POST /rest/api/3/changelog/bulkfetch`, gửi `fieldIds: ["status"]`, IDs của các task trong snapshot và phân trang bằng `nextPageToken`. Chia danh sách issue thành nhóm tối đa 1000 IDs. Lọc mỗi item theo trường `status`, chọn lần gần nhất có tên trạng thái đích `Close` hoặc `Resolve` (không phân biệt hoa thường) và lấy thời điểm tạo changelog.

**Lý do**: Endpoint chính thức hỗ trợ yêu cầu changelog cho nhiều issue cùng lúc, lọc theo field và phân trang; mỗi batch hỗ trợ đến 1000 issue. Nó yêu cầu quyền Browse Projects và quyền đọc issue-level security tương ứng, phù hợp với khả năng truy cập issue hiện tại, không yêu cầu quyền quản trị Jira. Dùng lọc status giảm dữ liệu không cần thiết; bulk tránh một request riêng cho từng ticket.

**Trường hợp lỗi**: Chỉ coi lịch sử là hoàn chỉnh khi mọi batch/trang thành công. Nếu API trả lỗi, không áp dụng một phần kết quả; lưu Real Due Date cũ của từng issue còn trong snapshot mới, để trống cho issue mới và thông báo rằng giá trị chưa được cập nhật. Nếu coroutine refresh bị hủy, giữ hành vi hiện có là không commit snapshot.

**Thay thế đã cân nhắc**:
- Gọi GET changelog riêng cho từng issue: loại vì tạo request theo số lượng task, tăng độ trễ/rủi ro rate limit.
- Chỉ dùng `fields.updated` trong search response: loại vì trường đó có thể đổi do cập nhật khác sau lần chuyển trạng thái.
- Lấy toàn bộ changelog không lọc field: loại vì tốn dữ liệu hơn cần thiết.

**Nguồn**: [Jira Cloud REST API v3 — Issues, Bulk fetch changelogs](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-changelog-bulk-get).

## R2. Snapshot và migration saved table

**Quyết định**: Lưu `realDueDate` cùng ticket trong snapshot của từng bảng đã lưu, dưới dạng chuỗi hiển thị theo formatter Updated hiện có và instant epoch milliseconds. Khi refresh lịch sử thành công, thay giá trị theo kết quả changelog hiện thời; nếu không lấy được đầy đủ lịch sử, giữ giá trị snapshot cũ theo issue ID ổn định. Nâng saved-table catalog từ schema v2 lên v3 và bật cột mới cho bảng hiện có.

**Lý do**: SavedTable hiện là snapshot chỉ đổi khi người dùng lưu hoặc refresh, Ticket đã là đơn vị dữ liệu bảng và có `issueId`, còn SavedTableStore hỗ trợ migration JSON cùng nguyên tắc giữ dữ liệu khi không đọc được. Không cần thêm ledger hoặc file mới cho một giá trị vốn tính lại từ Jira changelog.

**Thay thế đã cân nhắc**:
- Thêm một ledger toàn cục song song DynamicStartTimeRecord: loại vì Real Due Date được đọc lại từ lịch sử khi refresh, tạo thêm dữ liệu trùng và thêm chính sách đồng bộ.
- Lưu giá trị trên Jira issue hay chỉ dùng trong UI: loại vì không thể sửa dữ liệu Jira và yêu cầu cần giữ giá trị giữa các lần refresh thất bại/offline.

## R3. Phép chiếu quá hạn vào calendar

**Quyết định**: Giữ lịch sử trạng thái và Real Due Date trong `Ticket`; tính phần quá hạn trong projection calendar dùng chung. Chuyển hai instant Due Date sang LocalDate theo timezone bảng. Chỉ chiếu đoạn đỏ nếu ngày Real Due Date sau ngày Dynamic Due Date; khoảng bắt đầu ngày kế tiếp hạn động, kết thúc inclusive tại ngày thực tế, rồi cắt theo cửa sổ ngày hiện hữu.

**Lý do**: Calendar hiện đã là projection chỉ đọc từ snapshot; mở rộng projection giúp bao phủ ngày cùng timezone/cửa sổ hiện tại mà không biến Compose thành nơi chứa logic nghiệp vụ. `kotlinx-datetime` đã được dùng trong domain calendar.

**Thay thế đã cân nhắc**:
- Tính vị trí/điều kiện trong Canvas: loại vì trộn quy tắc nghiệp vụ với UI và làm khó kiểm thử.
- Mở rộng cửa sổ ngày theo Real Due Date: loại vì feature yêu cầu giữ giới hạn calendar hiện có.
- Vẽ đoạn đỏ trong cùng một khoảng màu dự kiến: loại vì mất khả năng phân biệt phần quá hạn.

## R4. Cột và tương thích

**Quyết định**: Thêm enum cột `REAL_DUE_DATE` nhưng chỉ cho phép trong danh sách cột saved table. Giá trị Jira mới có mặc định null để tương thích snapshot cũ; migration v2→v3 giữ các cột và dữ liệu hiện có rồi thêm Real Due Date. Các bảng query thông thường không được thêm cột này.

**Lý do**: `ColumnKey` và các cột Dynamic hiện đã phân biệt cột truy vấn với cột saved-only; JSON Ticket dùng kotlinx.serialization với default nullable.

**Thay thế đã cân nhắc**:
- Đưa Real Due Date vào tất cả query tables: loại vì phạm vi yêu cầu chỉ là bảng đã lưu.
- Đổi định dạng/schema ngoài catalog hiện hữu: loại vì không cần thay đổi kiểu lưu trữ.

## Môi trường và kiểm chứng

- Logic shared: `./gradlew :composeApp:jvmTest`.
- Kiểm tra style: `./gradlew ktlintCheck`.
- Kiểm thử HTTP: Ktor MockEngine với fixture changelog; automated tests không gọi Jira thật.
- Kiểm thử thủ công end-to-end cần tài khoản Jira có quyền Browse Projects và bảng test có transition lịch sử Close/Resolve.
