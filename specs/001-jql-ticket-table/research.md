# Nghiên cứu kỹ thuật (Phase 0)

**Feature**: `001-jql-ticket-table` | **Ngày**: 2026-10-30 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này giải quyết các điểm chưa xác định trong Technical Context. Mọi kết luận được kiểm chứng từ tài liệu chính thức (Atlassian, JetBrains, Maven Central) ngày 2026-10-30 và được thể chế hóa trong [contracts/jira-cloud-api.md](./contracts/jira-cloud-api.md).

## Tóm tắt quyết định

| Chủ đề | Quyết định |
|-------|-----------|
| Endpoint tìm kiếm | `POST /rest/api/3/search/jql`, JQL trong body JSON |
| Phân trang | Cursor `nextPageToken`; kết thúc khi `isLast = true` hoặc thiếu token |
| Kích thước trang | `maxResults = 100` |
| Tổng kết quả | `POST /rest/api/3/search/approximate-count`, hiển thị "≈ N" |
| Xác thực | Basic `base64(email:api_token)`, gửi chủ động trên mọi request |
| Kiểm tra đăng nhập | `GET /rest/api/3/myself` |
| Trường ticket | `fields=summary,status,assignee,priority,updated`; `key` ở cấp cao nhất |
| Ánh xạ lỗi | 8 lớp: `InvalidJql` (400), `AuthFailed` (401), `PermissionDenied` (403), `RateLimited` (429), `InstanceNotFound`, `NetworkUnreachable`, `Timeout`, `Unexpected` |
| Sắp xếp | Viết lại mệnh đề `ORDER BY`, xử lý phía Jira |
| Mở ticket | `{baseUrl}/browse/{key}` |
| Lưu token | java-keyring 1.0.4 (keychain hệ điều hành), đóng khi lỗi |
| Phiên bản | Kotlin 2.4.21, Compose MP 1.12.2, Ktor 3.6.1, Gradle 9.7.1, JDK 17 |
| Kiểm thử HTTP | Ktor `MockEngine` + fixture JSON |

## 1. Endpoint tìm kiếm và phân trang

**Quyết định**: dùng `POST /rest/api/3/search/jql` với body JSON chứa `jql`, `maxResults`, `fields` và `nextPageToken` (chỉ khi tải trang tiếp theo).

**Lý do**:

- JQL có thể dài và chứa ký tự đặc biệt; đưa vào body JSON tránh giới hạn độ dài URL và lỗi mã hóa.
- Cùng một định dạng với endpoint đếm (mục 2), giảm số kiểu request phải viết và kiểm thử.
- Đây là endpoint tìm kiếm đang được Atlassian khuyến nghị; endpoint cũ đã bị đánh dấu deprecated.

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| `GET /rest/api/3/search/jql` (JQL trên query string) | Giới hạn độ dài URL, phải mã hóa JQL thủ công |
| `GET/POST /rest/api/3/search` (cũ) | Bị đánh dấu deprecated, "currently being removed" (CHANGE-2046); không có ngày loại bỏ được xác nhận |
| Tự phân trang bằng `startAt` | Không còn hiệu lực với endpoint mới |

**Ghi chú ràng buộc** (từ OpenAPI và tài liệu):

- `maxResults` mặc định 50, tối đa 5000; số bản ghi trả về có thể ít hơn yêu cầu khi xin nhiều trường.
- `nextPageToken` của trang đầu là `null` (bỏ trống); token hết hạn sau 7 ngày.
- Dấu hiệu kết thúc: `isLast = true`. Tài liệu mô tả token trang cuối là `null` ở chỗ này và "không xuất hiện" ở chỗ khác, nên phải coi cả hai trường hợp là hết kết quả, ưu tiên `isLast`.
- Endpoint mới không trả về `total` (khác endpoint cũ).

**Ví dụ body yêu cầu**:

```json
{"jql": "project = ABC AND status != Done ORDER BY updated DESC", "maxResults": 100, "fields": ["summary", "status", "assignee", "priority", "updated"]}
```

**Ví dụ phản hồi (200)**:

```json
{"isLast": true, "issues": [{"id": "10000", "key": "ABC-24", "self": "https://site.atlassian.net/rest/api/3/issue/10000", "fields": {"summary": "...", "status": {"id": "3", "name": "In Progress"}, "assignee": {"accountId": "...", "displayName": "..."}, "priority": {"id": "2", "name": "High"}, "updated": "2026-10-20T08:15:00.000+0700"}}]}
```

## 2. Tổng số kết quả xấp xỉ

**Quyết định**: gọi `POST /rest/api/3/search/approximate-count` với body `{"jql": "<JQL>"}` sau khi chạy truy vấn, hiển thị dạng "≈ N kết quả". Nếu gọi đếm thất bại, giao diện vẫn hiển thị kết quả và báo số lượng đã tải (FR-007 không bị chặn).

**Lý do**: endpoint tìm kiếm mới không trả `total`; đây là endpoint chính thức thay thế. Giá trị là **ước lượng**, phù hợp để hiển thị kèm ký hiệu "≈".

**Phản hồi mẫu**: `{"count": 153}`

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| Dùng `total` từ endpoint `/search` cũ | Endpoint deprecated, sẽ bị loại bỏ |
| Tự đếm bằng cách tải hết các trang | Tốn băng thông và thời gian, phá vỡ mục tiêu SC-002 (< 5 giây) |
| Không hiển thị tổng | Không đáp ứng FR-007 |

**Ghi chú**: count cũng bị lọc theo quyền của người dùng (khớp với tập kết quả tìm kiếm); JQL phải có biên (xem mục 11); lỗi 400 nếu JQL không phân tích được, 401 nếu thông tin đăng nhập sai.

## 3. Xác thực và kiểm tra đăng nhập

**Quyết định**: gửi header `Authorization: Basic base64("<email>:<api_token>")` trên **mọi** request; khi người dùng lưu cấu hình, gọi `GET /rest/api/3/myself` để xác nhận (200 = hợp lệ; 401 = sai).

**Lý do**:

- Atlassian không hỗ trợ mật khẩu tài khoản cho REST API; API token là cách xác thực được khuyến nghị cho kịch bản cá nhân.
- Jira Cloud cho phép truy cập ẩn danh nên server có thể không trả 401 khi thiếu header; vì vậy phải gửi header chủ động thay vì chờ thách thức.
- `/myself` là endpoint nhẹ, trả về người dùng hiện tại, phù hợp để kiểm tra cấu hình trước khi lưu (US1).

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| OAuth 2.0 (3LO) | Cần đăng ký ứng dụng và callback URL, độ phức tạp vượt phạm vi v1 (YAGNI) |
| Personal Access Token | Chỉ dùng cho Jira Server/Data Center, không áp dụng Cloud |
| Kiểm tra bằng chính truy vấn search | Lỗi JQL và lỗi xác thực lẫn lộn, khó thông báo chính xác |

**Ghi chú về token có scope**: Atlassian cho phép tạo token có scope; loại đó phải gọi qua gateway `https://api.atlassian.com/ex/jira/{cloudId}`. Phiên bản 1 chỉ hỗ trợ token cổ điển (không scope) gọi trực tiếp `https://<site>.atlassian.net`; ghi rõ trong hướng dẫn và thông báo lỗi. Token cần quyền `read:jira-work` (hoặc các quyền chi tiết tương ứng).

## 4. Ánh xạ lỗi

**Quyết định**: phân loại lỗi theo bảng dưới đây; giữ nguyên thông điệp gốc của Jira khi có (`errorMessages` / `errors`).

| Nguồn lỗi | Loại trong mô hình | Thông báo cho người dùng |
|-------------|--------------------|----------------------|
| HTTP 400 (JQL không phân tích được, JQL không có biên) | `InvalidJql` | "JQL không hợp lệ: {chi tiết}" |
| HTTP 401 (thông tin đăng nhập sai/hết hiệu lực) | `AuthFailed` | "Xác thực thất bại - kiểm tra email và API token" |
| HTTP 403 | `PermissionDenied` | "Tài khoản không đủ quyền thực hiện truy vấn" |
| `UnknownHostException` | `InstanceNotFound` | "Địa chỉ Jira không tồn tại hoặc gõ sai" |
| `ConnectException` / `IOException` | `NetworkUnreachable` | "Không kết nối được tới Jira - kiểm tra mạng" |
| `HttpRequestTimeoutException` / `ConnectTimeoutException` | `Timeout` | "Jira phản hồi quá lâu - thử lại" |
| HTTP 429 (vượt giới hạn tần suất) | `RateLimited` | "Quá nhiều yêu cầu - thử lại sau {n} giây" |
| Còn lại | `Unexpected` | "Lỗi không mong đợi (mã {status})" |

**Lý do**: SC-004 yêu cầu phân biệt 5 nhóm lỗi phổ biến (JQL sai, xác thực hết hạn, địa chỉ sai, mạng gián đoạn, thiếu quyền). Địa chỉ sai (`UnknownHostException`) và mạng gián đoạn (`ConnectException`) phải tách riêng vì chúng khác nhau về hành động khắc phục. 429 được tách riêng vì Atlassian công bố giới hạn tần suất theo endpoint và trả `Retry-After`.

**Ghi chú**: tài liệu API không liệt kê 403 cho endpoint tìm kiếm - Jira lọc bỏ các issue người dùng không được xem thay vì trả lỗi. Vẫn giữ `PermissionDenied` cho 403 vì đây là trường hợp có thể xảy ra với token thiếu scope hoặc chính sách site, và vì người dùng cần thông báo khác với lỗi xác thực. Khi truy vấn trả 0 kết quả, giao diện hiển thị thông báo trống kèm gợi ý rằng quyền truy cập có thể là nguyên nhân.

## 5. Trường dữ liệu ticket

**Quyết định**: luôn gửi `fields=summary,status,assignee,priority,updated`; đọc `key` và `id` ở cấp cao nhất của mỗi phần tử `issues[]`.

**Lý do**: tham số `fields` mặc định chỉ trả `id`, nên nếu không chỉ định sẽ thiếu toàn bộ giá trị hiển thị. Chỉ xin đúng 5 trường cần dùng giúp giảm kích thước phản hồi.

**Hình dạng trường** (theo biểu diễn chuẩn của Jira):

| Trường | Kiểu JSON | Ghi chú |
|--------|-----------|-------|
| `key` | string | Ví dụ `ABC-24`; dùng làm khóa bảng |
| `fields.summary` | string | |
| `fields.status` | object | Lấy `name` |
| `fields.assignee` | object \| null | Lấy `displayName`; null khi chưa gán |
| `fields.priority` | object \| null | Lấy `name`; có site không dùng priority |
| `fields.updated` | string | ISO-8601 có độ lệch múi giờ |

## 6. URL mở ticket

**Quyết định**: ghép `{baseUrl}/browse/{key}` sau khi chuẩn hóa `baseUrl` (bỏ dấu `/` cuối).

**Lý do**: đây là định dạng chuẩn của Jira Cloud; chính tài liệu Atlassian dùng mẫu này để liên kết tới issue. Không cần thêm lọi gọi API nào khác.

**Phương án đã cân nhắc**: dùng trường `self` của issue (là endpoint REST, không phải trang web dành cho người dùng); dùng `id` thay `key` (URL `/browse/` cần key).

## 7. Sắp xếp phía Jira

**Quyết định**: khi người dùng bấm tiêu đề cột, viết lại JQL - nếu JQL đã kết thúc bằng mệnh đề `ORDER BY` thì thay thế, nếu chưa có thì thêm ` ORDER BY <field> <ASC|DESC>`.

**Lý do**: phân trang bằng cursor chỉ nhất quán khi thứ tự ổn định giữa các trang; sắp xếp phía server bảo đảm điều đó và cho thứ tự đúng trên toàn bộ kết quả (FR-007, US3).

**Phương án đã cân nhắc**: sắp xếp cục bộ trong ứng dụng - chỉ đúng trong trang đã tải nên gây hiểu nhầm, đã bị loại từ giai đoạn kế hoạch.

**Ánh xạ cột sang trường JQL**: `KEY` -> `key`, `SUMMARY` -> `summary`, `STATUS` -> `status`, `ASSIGNEE` -> `assignee`, `PRIORITY` -> `priority`, `UPDATED` -> `updated`.

## 8. Kích thước trang

**Quyết định**: `maxResults = 100`.

**Lý do**: bằng một nửa số vòng gọi so với mặc định 50; tải 250 ticket chỉ cần 2 lần "tải thêm" (SC-005 cho phép tối đa 3 thao tác); payload 5 trường vẫn nhẹ nên SC-002 (< 5 giây cho 100 ticket) khả thi.

**Phương án đã cân nhắc**: 50 (mặc định, nhiều vòng gọi hơn, sát giới hạn 3 thao tác của SC-005); 5000 (payload lớn, chậm dựng bảng, thực tế trả về ít hơn khi xin nhiều trường).

## 9. Lưu token an toàn trên JVM

**Quyết định**: dùng `com.github.javakeyring:java-keyring:1.0.4` làm implementation `actual` cho `SecureStorage`.

**Lý do**: API tối giản (tạo, ghi, đọc, xóa mật khẩu theo cặp domain/account); hỗ trợ cả macOS Keychain, Windows Credential Manager và Linux Secret Service; giấy phép BSD-3; chỉ một phụ thuộc, phù hợp Nguyên tắc V.

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| `de.swiesend:secret-service` | Chỉ Linux và chỉ có bản alpha |
| Gọi JNA trực tiếp Security.framework / CredRead | Phải tự viết binding, nhiều mã hơn; để dành khi cần |
| Tự mã hóa file bằng JCE | Vẫn phải quản lý khóa ở đâu đó, không giải quyết gốc vấn đề FR-003 |
| File văn bản thuần | Vi phạm FR-003 và hiến pháp |

**Chính sách đóng khi lỗi (fail closed)**: nếu keychain không khả dụng (ví dụ Linux không có Secret Service), ứng dụng báo lỗi rõ ràng và **không** lưu token ở bất kỳ nơi khác.

**Ghi chú rủi ro**: thư viện không có bản phát hành mới từ 2023; chấp nhận vì API ổn định và phạm vi sử dụng nhỏ. Bảo vệ thực tế phụ thuộc đóng gói (macOS cần ứng dụng được ký; Windows cần package identity/MSIX); trên Linux chỉ có mức cô lập theo người dùng. Ứng dụng không tuyên bố mức bảo mật cao hơn thực tế.

## 10. Ma trận phiên bản phụ thuộc

| Thành phần | Phiên bản chọn | Ghi chú |
|----------|---------------|-------|
| Kotlin | 2.4.21 | Bản ổn định mới nhất (2026-10-08) |
| Compose Multiplatform | 1.12.2 | Plugin Compose Compiler dùng cùng phiên bản Kotlin; yêu cầu Kotlin >= 2.1.0 |
| Ktor (client + MockEngine) | 3.6.1 | Engine OkHttp trên JVM; cần kotlinx-coroutines >= 1.11.0 |
| kotlinx.serialization | 1.12.0 | |
| kotlinx.coroutines | 1.11.0 | Khớp yêu cầu của Ktor 3.6.1 |
| Gradle | 9.7.1 | Nằm trong dải KGP hỗ trợ đầy đủ (tới 9.7.0, bản vá 9.7.1 cùng dòng) |
| JDK | 17 | Gradle 9 yêu cầu JVM 17-27; CMP cần >= 11 khi chạy, >= 17 khi đóng gói |
| Plugin ktlint (`org.jlleitschuh.gradle.ktlint`) | 14.2.0 | Hỗ trợ Gradle 9 từ 13.1.0 |
| java-keyring | 1.0.4 | Xem mục 9 |

**Lý do chọn**: chỉ dùng bản ổn định (không alpha/beta); các phiên bản này được kiểm chứng tương thích chéo từ tài liệu chính thức; hiến pháp yêu cầu ghim phiên bản trong `gradle/libs.versions.toml`.

**Phương án đã cân nhắc**: Gradle 9.8.2 là bản mới nhất nhưng nằm ngoài dải KGP hỗ trợ đầy đủ nên không chọn.

## 11. Yêu cầu JQL có biên

**Quyết định**: chuyển nguyên văn JQL người dùng nhập; nếu Jira trả 400 vì truy vấn không có biên, hiển thị lỗi `InvalidJql` kèm thông điệp gốc và giữ nội dung trong ô nhập.

**Lý do**: endpoint tìm kiếm và đếm mới yêu cầu truy vấn có biên vì lý do hiệu năng. Tự thêm điều kiện (ví dụ `AND created > -30d`) sẽ thay đổi âm thầm ngữ nghĩa truy vấn nên không được phép.

**Phương án đã cân nhắc**: tự động thêm biên (làm sai kết quả, đã loại); phân tích cú pháp JQL phía client để chặn trước (không đáng tin cậy với cú pháp JQL đầy đủ, đã loại).

**Ghi chú giao diện**: thông báo lỗi nên kèm gợi ý cụ thể (ví dụ thêm điều kiện `project = ...`).

## 12. Kiểm thử HTTP giả

**Quyết định**: dùng `MockEngine` của Ktor cho mọi bài test client/repository; fixture JSON đặt trong `commonTest`.

**Lý do**: hiến pháp II cấm gọi Jira thật trong test tự động. `MockEngine` cho phép mô phỏng xác định: phân trang nhiều trang, 400/401/403/429, hết thời gian, ngoại lệ mạng, và chạy được từ `commonTest`.

**Phương án đã cân nhắc**: MockWebServer/WireMock (chỉ chạy trên JVM, không dùng được trong `commonTest`); gọi site Jira thật (vi phạm hiến pháp).

**Kèm theo**: fakes cho `SecureStorage`, `AppPaths`, `UrlOpener`, `ioDispatcher` đặt trong `commonTest`.

## Điểm chưa kiểm chứng

- Ngày loại bỏ cụ thể của endpoint `/search` cũ (CHANGE-2046) không truy cập được; chỉ xác nhận được trạng thái deprecated.
- Hành vi 403 trên endpoint tìm kiếm không được tài liệu hóa; mô hình vẫn giữ lớp lỗi để phòng.
- Token có scope khi gọi trực tiếp site URL: chưa rõ, có thể phải dùng gateway `api.atlassian.com/ex/jira/{cloudId}`.
- Kiểu ngoại lệ cụ thể khi không có keychain trên Linux headless được suy đoán từ README java-keyring.
- Định dạng URL browse không được nêu như một tuyên bố quy chuẩn, nhưng khớp với mọi liên kết trong tài liệu Atlassian.

## Nguồn tham khảo

Atlassian:

- https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/
- https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json
- https://developer.atlassian.com/cloud/jira/platform/basic-auth-for-rest-apis/
- https://developer.atlassian.com/cloud/jira/platform/rate-limiting/
- https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-myself/
- https://developer.atlassian.com/cloud/jira/platform/search-and-reconcile/
- https://support.atlassian.com/atlassian-account/docs/manage-api-tokens-for-your-atlassian-account/

JetBrains và cộng đồng:

- https://kotlinlang.org/docs/releases.html
- https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html
- https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html
- https://github.com/JetBrains/compose-multiplatform/releases
- https://github.com/ktorio/ktor/releases
- https://github.com/Kotlin/kotlinx.serialization/releases
- https://github.com/Kotlin/kotlinx.coroutines/releases
- https://services.gradle.org/versions/current
- https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint
- https://github.com/javakeyring/java-keyring
- https://repo1.maven.org/maven2/com/github/javakeyring/java-keyring/maven-metadata.xml
- https://github.com/swiesend/secret-service
