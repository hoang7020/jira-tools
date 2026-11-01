# Nghiên cứu: Thời gian động và ước lượng công việc

## R1. Trường Jira và snapshot tìm kiếm

**Quyết định**: Mở rộng request fields hiện tại để lấy `timetracking`, đồng thời map `IssueDto.id`, `fields.status.statusCategory.key`, `fields.updated` và `fields.timetracking.originalEstimate`. Hiển thị chuỗi Original Estimate Jira trả về; parse riêng cho phép tính. Dùng key category `indeterminate` để nhận diện In Progress, không so sánh tên trạng thái.

**Lý do**: Jira Cloud REST v3 search trả issue ID ổn định, status và timetracking trong kết quả issue. Ví dụ Jira cho thấy `originalEstimate` là chuỗi hiển thị và có `originalEstimateSeconds` tương ứng. Chuỗi gốc giữ định dạng Jira, còn ID/category hỗ trợ identity và trạng thái tùy chỉnh. Không cần thêm request per issue hay đọc changelog.

**Trường hợp thiếu**: Không có estimate thì để ô trống. Time tracking bị tắt hoặc phản hồi không có `timetracking` vẫn không được làm hỏng ticket khác. Issue không có ID/status category/Updated parse được không tạo record start mới; thử lại khi một snapshot sau cung cấp đủ dữ liệu.

**Thay thế đã cân nhắc**:
- Đọc tên status như `"In Progress"`: loại vì không nhận diện tên tùy chỉnh/địa phương hóa.
- Đọc lịch sử changelog: loại vì spec yêu cầu thời điểm quan sát đầu tiên, không phải lịch sử chuyển trạng thái.
- Chuyển estimate sang số giây để hiển thị: loại vì làm mất nguyên dạng Jira; số giây chỉ hữu ích cho tính toán.

**Nguồn**: [Jira Cloud REST API v3 — Issue search](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/), [Jira Cloud REST API v3 — Issues](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/).

## R2. Tuần làm việc và múi giờ

**Quyết định**: Dùng tuần làm việc chuẩn thứ Hai-thứ Sáu, 08:00-12:00 và 13:00-17:00; do đó `1d = 8h` và `1w = 40h`. Lấy múi giờ profile từ trường `timeZone` trong kết quả `GET /rest/api/3/myself`. Ghi múi giờ hiện hành trên từng SavedTable để tính/hiển thị offline. Không có hoặc không parse được múi giờ thì Due Date trống và giao diện báo rõ lý do.

**Lý do**: Endpoint `/myself` trả hồ sơ tài khoản hiện hành và response ví dụ có trường IANA `timeZone`; quyền yêu cầu là quyền truy cập Jira thông thường. Ngược lại, endpoint cấu hình time tracking yêu cầu quyền Administer Jira và chỉ trả `workingDaysPerWeek` dạng số cùng `workingHoursPerDay`, không trả danh sách các thứ trong tuần. Người dùng đã chọn fallback Mon-Fri; không gọi endpoint quản trị và không tự suy diễn tuần làm việc từ `workingDaysPerWeek`.

**Thay thế đã cân nhắc**:
- Gọi Jira time-tracking-options: loại vì cần quyền Administer Jira và không cung cấp tập thứ cụ thể; cấu hình 08-12/13-17 đã được user chốt.
- Dùng timezone máy: loại vì kết quả sẽ thay đổi theo máy và không đúng với profile Jira.
- Dùng lịch 24/7 hoặc bỏ qua cuối tuần: loại vì không đáp ứng yêu cầu ngày làm việc đã xác nhận.

**Nguồn**: [Jira Cloud REST API v3 — Current user](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-myself/#api-rest-api-3-myself-get), [Jira Cloud REST API v3 — Time tracking](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-time-tracking/).

## R3. Ngày giờ trong Kotlin Multiplatform

**Quyết định**: Dùng `org.jetbrains.kotlinx:kotlinx-datetime:0.8.0` trong `commonMain`, khai báo version/ref ở `gradle/libs.versions.toml`. Dùng `Instant` cho timestamp tuyệt đối, `TimeZone`/`LocalDateTime` để diễn giải khung giờ địa phương; định dạng Due Date theo timezone Jira.

**Lý do**: Đây là thư viện Kotlin Multiplatform cho `Instant`, ngày/giờ địa phương và IANA timezone; Maven Central xác nhận `0.8.0` là bản release hiện có. Nó tránh phụ thuộc `java.time` trong shared logic.

**Thay thế đã cân nhắc**:
- `java.time`: loại vì chỉ dành cho JVM và không phù hợp `commonMain`.
- Tự cộng số mili giây cố định: loại vì sai khi bỏ qua cuối tuần/nghỉ trưa và không áp dụng quy tắc timezone.
- Gọi dịch vụ hoặc Jira cho từng phép tính: loại vì dữ liệu đã có ở snapshot, cần offline và không cần thêm request.

**Nguồn**: [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime), [Maven Central metadata](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-datetime/maven-metadata.xml).

## R4. Identity và vòng đời Dynamic Start Time

**Quyết định**: Lưu sổ start-time dùng chung trong cùng `SavedTableCatalog`, khóa bằng base URL Jira đã chuẩn hóa và issue ID. Mỗi record giữ `startedAtMillis` để tính/sắp xếp và `displayValue` chính là Updated đã hiển thị ở snapshot đầu tiên. Không xóa record khi task vắng mặt hoặc bảng bị xóa. Catalog và ticket đã làm giàu được ghi trong cùng một lần ghi JSON sau refresh đầy đủ.

**Lý do**: Issue key có thể đổi; Jira issue ID ổn định hơn. Một ledger duy nhất đáp ứng chia sẻ qua nhiều bảng và lưu mốc ngay cả khi bảng không còn chứa issue. Cùng file giúp kết quả refresh và ledger được ghi nguyên tử, không phải điều phối transaction hai file. Materialize lại các cột Dynamic cho mọi snapshot khác đang chứa cùng issue giúp UI nhất quán khi refresh một bảng.

**Thay thế đã cân nhắc**:
- Lưu start time trên từng SavedTable: loại vì tạo mốc khác nhau giữa các bảng.
- Lưu riêng file tracker: loại vì refresh cần transaction hai file.
- Tự dọn record không còn trong bảng hoặc khi xóa bảng: loại vì có thể mất mốc user yêu cầu giữ lại.
- Khôi phục từ changelog: loại theo clarification.

## R5. Migration và cột

**Quyết định**: Nâng config từ schema v1 lên v2 và saved catalog từ v1 lên v2. Khi đọc v1, giữ nguyên JQL/sort/base URL/email và các lựa chọn cột trước đó; thêm Original Expected Time vào bộ cột query và thêm ba cột mới vào từng saved table. Ticket cũ nhận field mới nullable; ledger mới bắt đầu rỗng. Migration được áp dụng trong bộ nhớ, ghi schema mới khi có lần ghi hợp lệ tiếp theo; schema hỏng/tương lai vẫn được xử lý theo chính sách lỗi hiện có.

**Lý do**: Người dùng cần mở dữ liệu cũ mà không mất cài đặt hoặc bảng. Dữ liệu Original Estimate và Dynamic Start chưa từng được lưu không thể phục hồi offline; chúng xuất hiện sau lần query/refresh Jira thành công. Cột dynamic chỉ có trong saved context, không được lẫn vào danh sách cột query hoặc JQL sorting.

**Thay thế đã cân nhắc**:
- Bỏ toàn bộ config cũ khi tăng version: loại vì làm mất tùy chọn người dùng.
- Từ chối catalog v1: loại vì vi phạm yêu cầu mở bảng đã lưu cũ.
- Đặt toàn bộ enum column mới vào query: loại vì Dynamic columns không có giá trị trong query snapshot và không phải trường Jira.

**Phạm vi**: Không có thay đổi mã nguồn hoặc test trong giai đoạn nghiên cứu. Các kiểm thử cụ thể được ghi ở [quickstart.md](./quickstart.md).
