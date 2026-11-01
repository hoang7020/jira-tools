# Hợp đồng: Jira Cloud REST API v3

Phạm vi: các endpoint mà ứng dụng gọi tới Jira Cloud. Đây là hợp đồng bên ngoài, không phải mã nguồn.

## Xác thực

- Header: `Authorization: Basic base64(email:api_token)`.
- Gửi ngay từ request đầu tiên (Jira Cloud cho phép truy cập ẩn danh nên không có bước thách thức 401).
- `Accept: application/json`; với request có body: `Content-Type: application/json`.

## Endpoint 1 - Kiểm tra thông tin xác thực

`GET {baseUrl}/rest/api/3/myself`

- 200: đối tượng `User` - dùng `displayName` (và `emailAddress` nếu có) để hiển thị trạng thái kết nối đã xác thực (FR-002).
- 401: thông tin đăng nhập sai -> `AuthFailed`.
- Dùng cho nút "Kiểm tra kết nối" và kiểm tra lại khi khởi động.

## Endpoint 2 - Tìm kiếm bằng JQL

`POST {baseUrl}/rest/api/3/search/jql`

Yêu cầu (JSON):

| Trường | Bắt buộc | Giá trị dùng trong ứng dụng |
|---------|----------|----------------------|
| `jql` | Co | JQL người dùng nhập, đã ghép `ORDER BY` khi cần |
| `fields` | Không | `["summary","status","assignee","priority","updated"]` |
| `maxResults` | Không | `100` |
| `nextPageToken` | Không | token từ trang trước, chỉ khi tải thêm |

Lưu ý: `fields` mặc định chỉ trả `id`, nên bắt buộc phải truyền danh sách trường.

Phản hồi 200:

```json
{
  "isLast": false,
  "nextPageToken": "CAEaAggCIgIIACoCCAA...",
  "issues": [
    {
      "id": "10001",
      "key": "HSP-1",
      "self": "https://site.atlassian.net/rest/api/3/issue/10001",
      "fields": {
        "summary": "Sai số lượng hàng tồn",
        "status": { "id": "10000", "name": "To Do", "statusCategory": { "key": "new" } },
        "assignee": { "displayName": "Nguyen Van A" },
        "priority": { "id": "2", "name": "High" },
        "updated": "2026-10-30T08:15:00.000+0700"
      }
    }
  ]
}
```

Quy tắc phân trang:

- `isLast = true` -> đã hết.
- `nextPageToken` vắng mặt hoặc `null` -> coi như đã hết (tài liệu không nhất quán, xử lý cả hai trường hợp).
- Token hết hiệu lực sau 7 ngày.
- **Không có trường `total`** trong phản hồi.

Trường `null` cần xử lý: `assignee`, `priority` có thể là `null`; `summary`/`status`/`updated` luôn có.

## Endpoint 3 - Đếm gần đúng tổng số kết quả

`POST {baseUrl}/rest/api/3/search/approximate-count`

Yêu cầu: `{ "jql": "project = HSP" }`

Phản hồi 200: `{ "count": 153 }`

- Kết quả đã được lọc theo quyền của người dùng.
- 400 nếu JQL không phân tích được, 401 nếu thông tin đăng nhập sai.
- Dùng để hiển thị tổng số ước lượng (FR-007); lỗi ở endpoint này không được làm hỏng danh sách đã tải.

## Ánh xạ lỗi

Thân lỗi theo chuẩn `ErrorCollection`:

```json
{ "errorMessages": ["The provided JQL is not valid"], "errors": {}, "status": 400 }
```

| Lớp lỗi nội bộ | Điều kiện nhận biết |
|---------------|------------------|
| `InvalidJql` | HTTP 400 (kể cả truy vấn không có biên) |
| `AuthFailed` | HTTP 401 |
| `PermissionDenied` | HTTP 403 |
| `RateLimited` | HTTP 429, kèm header `Retry-After` khi có |
| `InstanceNotFound` | `UnknownHostException` (sai địa chỉ site) |
| `NetworkUnreachable` | `ConnectException` / `IOException` khác |
| `Timeout` | `HttpRequestTimeoutException` / `ConnectTimeoutException` |
| `Unexpected` | Mã khác hoặc phản hồi không phân tích được |

Lưu ý: 403 không được tài liệu hóa riêng cho endpoint tìm kiếm (Jira lọc bỏ issue không có quyền xem thay vì trả lỗi), nhưng vẫn giữ lớp lỗi này. Địa chỉ sai thường lỗi ở tầng DNS/TLS trước khi Jira xử lý nên không có thân JSON.

## URL mở ticket

`{baseUrl}/browse/{key}` - mở bằng trình duyệt mặc định (FR-012/US4).

## Tham chiếu

- Chi tiết nghiên cứu: xem `research.md` mục 1-6.
- Mô hình dữ liệu nội bộ: xem `data-model.md`.
