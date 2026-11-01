# Hợp đồng tích hợp Jira: Cập nhật Due Date issue

## Request

- **Method/path**: `PUT /rest/api/3/issue/{issueIdOrKey}`
- **Path identity**: `issueId` nếu có; nếu không, Jira `key`.
- **Content-Type**: `application/json`
- **Authentication**: Thông tin xác thực Jira hiện có; không ghi credential vào log, UI, hay snapshot.
- **Body**: Chỉ sửa `fields.duedate`:

```json
{
  "fields": {
    "duedate": "2026-11-20"
  }
}
```

Ngày là lịch `yyyy-MM-dd`, lấy theo timezone Jira lưu trong SavedTable. Không gửi trường `duedate` khi ngày đích không hợp lệ.

## Response

- Bất kỳ mã HTTP 2xx nào (bao gồm 204 No Content) xác nhận cập nhật đã được Jira chấp nhận; không yêu cầu response body.
- Lỗi HTTP được phân loại bằng `JiraErrorMapper` để giữ thông báo lỗi Jira hiện tại cho xác thực, quyền, giới hạn tốc độ, và lỗi máy chủ.
- Lỗi mạng/timeout được trả về dưới dạng thất bại; không được đổi snapshot thành giá trị đích.
- Không retry tự động. Người dùng có thể thử lại sau khi thông báo lỗi được hiển thị.

## Tác động dữ liệu

API chỉ sửa Due Date của issue chỉ định; sau 2xx ứng dụng mới cập nhật Due Date ở snapshot SavedTable.
