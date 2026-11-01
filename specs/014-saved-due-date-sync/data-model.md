# Mô hình dữ liệu: Đồng bộ Due Date từ bảng đã lưu

## Dữ liệu đã lưu (hiện hữu)

### Ticket

| Trường | Kiểu | Vai trò trong đồng bộ |
|---|---|---|
| `issueId` | `String?` | ID Jira ưu tiên để định danh issue; nếu thiếu, dùng `key`. |
| `key` | `String` | Định danh hiển thị và fallback cho request Jira. |
| `dueDate` | `String?` | Due Date chuẩn từ Jira, ngày `yyyy-MM-dd`; null biểu thị trống. |
| `dynamicDueAtMillis` | `Long?` | Thời điểm Dynamic Due Date tính được; null nghĩa là không thể đưa ra ngày mục tiêu. |
| `dynamicDueDate` | `String?` | Giá trị hiển thị hiện có; không dùng chuỗi đã định dạng làm nguồn chuẩn nếu timestamp có sẵn. |

### SavedTable

| Trường | Kiểu | Vai trò |
|---|---|---|
| `id` | `String` | Khóa bảng để tìm đúng snapshot cần ghi. |
| `jiraBaseUrl` | `String` | Jira site gắn với snapshot; phải khớp site kết nối hiện tại trước khi ghi Jira. |
| `jiraTimeZone` | `String?` | Múi giờ Jira dùng để chuyển Dynamic Due Date thành ngày lịch. |
| `tickets` | `List<Ticket>` | Snapshot được cập nhật theo đúng issue sau khi Jira xác nhận thành công. |

Không thêm field bền vững hoặc tăng phiên bản catalog. `dueDate` và `dynamicDueAtMillis` đã được lưu trong Ticket hiện hữu.

## Trạng thái đồng bộ (runtime)

Trạng thái tạm thời theo khóa `(savedTableId, issueId-or-key)`:

| Trạng thái | Điều kiện/chuyển tiếp |
|---|---|
| Không cần cập nhật | Không có ngày dynamic hợp lệ hoặc Due Date bằng ngày mục tiêu. |
| Cần cập nhật | Có ngày mục tiêu hợp lệ và Due Date null/khác ngày đó. |
| Đang cập nhật | Request Jira đang chạy; không cho gửi request thứ hai cho cùng khóa. |
| Cập nhật lỗi | Jira từ chối hoặc lỗi mạng; giữ Due Date snapshot cũ, hiện lỗi và cho phép thử lại. |
| Cập nhật thành công | Jira 2xx; ghi ngày mục tiêu vào snapshot. Nếu lưu catalog thất bại, biểu thị rõ Jira thành công nhưng lưu local thất bại. |

Trạng thái tiến trình/lỗi không được lưu vào file catalog; sau khi khởi động lại, trạng thái được suy ra từ Due Date snapshot và Dynamic Due Date.

## Ngày mục tiêu

- Chuyển `dynamicDueAtMillis` sang `LocalDate` bằng `SavedTable.jiraTimeZone`.
- Serialize thành `yyyy-MM-dd` cho Jira và snapshot.
- Timezone thiếu/không hợp lệ, timestamp thiếu, hoặc issue không có `issueId` lẫn key dùng được: không cung cấp thao tác cập nhật.
