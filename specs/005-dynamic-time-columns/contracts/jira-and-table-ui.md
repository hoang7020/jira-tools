# Contract: Jira fields và hợp đồng bảng

## 1. Jira Cloud REST v3 search

Dùng tiếp `POST /rest/api/3/search/jql`; không thêm request cho mỗi issue. Request fields mở rộng như sau:

```json
{
  "fields": [
    "summary",
    "status",
    "assignee",
    "priority",
    "updated",
    "timetracking"
  ]
}
```

Issue response được map từ dữ liệu sau (các field Jira có thể vắng mặt/null):

```json
{
  "id": "10001",
  "key": "HSP-1",
  "fields": {
    "status": {
      "name": "Doing",
      "statusCategory": {
        "key": "indeterminate"
      }
    },
    "updated": "2026-10-30T08:15:00.000+0700",
    "timetracking": {
      "originalEstimate": "1d 2h",
      "originalEstimateSeconds": 36000
    }
  }
}
```

- `id` là identity dùng cùng base URL đã chuẩn hóa; `key` tiếp tục là mã hiển thị.
- Category `indeterminate` là nguồn nhận diện In Progress; `name` chỉ hiển thị.
- `originalEstimate` được giữ nguyên; `originalEstimateSeconds` không thay thế chuỗi hiển thị.
- Thiếu `timetracking`, estimate hoặc timestamp không làm lỗi toàn trang.
- `updated` vừa được giữ để hiển thị, vừa parse thành instant cho record start.

## 2. Jira profile timezone

Dùng response của `GET /rest/api/3/myself`:

```json
{
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

`timeZone` là optional. Lỗi/thiếu field không làm kết nối thành công thất bại; thay vào đó SavedTable giữ giá trị null và Due Date hiển thị trống kèm cảnh báo. `ConnectionStatus.Connected` truyền timezone cho ViewModel; `SavedTable.jiraTimeZone` giữ giá trị cần cho thao tác offline. Không gọi `/configuration/timetracking/options`.

## 3. Hợp đồng mapper và model

- `TicketMapper.toTicket` trả thêm `issueId`, `statusCategoryKey`, `updatedAtMillis` và `originalExpectedTime`, nhưng vẫn bỏ qua issue thiếu key như hiện tại.
- Timestamp lỗi tạo `updatedAtMillis = null`; chuỗi `updated` vẫn hiển thị theo hành vi hiện có.
- Domain models và persistence vẫn dùng `kotlinx.serialization`; field mới trong Ticket có default null cho JSON snapshot cũ.
- DynamicStartTimeRecord được tìm theo `(normalizedJiraBaseUrl, issueId)`. Record có sẵn không bị update.
- Dynamic Start Time hiển thị `displayValue` đã lưu; không format lại theo timezone hiện hành.
- Due Date hiển thị theo timezone của SavedTable và được sắp xếp bằng `dynamicDueAtMillis`.

## 4. Cột và hành vi UI

| Context | Cột mới khả dụng | Default |
|---|---|---|
| Query result | `Original Expected Time` | Bật; có thể ẩn/hiện bằng chooser hiện có. |
| Saved table | `Original Expected Time`, `Dynamic Start Time`, `Dynamic Due Date` | Ba cột được bật khi tạo/migration; có thể ẩn/hiện riêng từng bảng. |

- Query column list không chứa Dynamic columns; saved table dùng danh sách mở rộng.
- Query sorting của Original Expected Time dùng JQL `ORDER BY originalEstimate`; saved sorting dùng thời lượng đã parse/timestamp, không sort chuỗi ngày giờ.
- Thêm nhãn/value mapping/width vào bảng dùng chung; KEY vẫn cố định.
- Khi timezone thiếu hoặc không hợp lệ, saved detail hiển thị cảnh báo không chặn và giữ các cột khác.
- Estimate không parse được: Original Expected Time vẫn hiện chuỗi gốc, Dynamic Due Date để trống; issue khác không bị ảnh hưởng.

## 5. Persistence và lỗi

- `config.json` v1 được chuyển đổi sang v2 trong bộ nhớ, giữ cài đặt hiện có và bật Original Expected Time.
- `saved-tables.json` v1 được chuyển đổi sang v2 trong bộ nhớ, giữ snapshot/cột cũ, thêm các cột mới và ledger rỗng.
- Refresh chỉ commit sau khi tất cả trang thành công. Lỗi Jira, hủy hoặc lỗi ghi không làm mất bảng cũ hay ledger.
- Lỗi đọc/schema hỏng tiếp tục dùng cơ chế catalog hiện có: cảnh báo và chặn ghi; không tạo success-shaped fallback.
- Token/API secret không nằm trong request log, model hay file catalog.
