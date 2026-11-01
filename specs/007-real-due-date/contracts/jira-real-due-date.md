# Hợp đồng: Jira status changelog và Real Due Date

## Jira REST API

### Request

- Method/path: `POST /rest/api/3/changelog/bulkfetch`
- Dùng credential Jira Cloud hiện có của ứng dụng.
- JSON body:
  - `issueIdsOrKeys`: IDs Jira của các issue trong snapshot; gửi nhóm tối đa 1000 IDs.
  - `fieldIds`: `["status"]`.
  - `maxResults`: page size theo giới hạn API.
  - `nextPageToken`: bỏ trống ở trang đầu, lấy từ phản hồi để tải trang tiếp theo.
- Gọi lần lượt cho tới khi không còn token; xử lý toàn bộ trang của từng batch.

Endpoint yêu cầu quyền Browse Projects cho các project và quyền đọc issue-level security nếu áp dụng. Phản hồi HTTP/API lỗi hoặc lỗi giải mã khiến tập changelog của lần làm mới không được xem là hoàn chỉnh.

### Mapping

- Nối changelog với `Ticket.issueId`.
- Chỉ xét changelog items có `field == "status"`.
- Trạng thái đích đủ điều kiện nếu `toString.trim()` bằng `Close` hoặc `Resolve`, không phân biệt hoa thường.
- Chọn sự kiện mới nhất theo thời điểm tạo changelog; không lấy `fields.updated` hiện tại vì nó có thể phản ánh cập nhật về sau.
- Parse timestamp theo offset Jira thành epoch millis; chuỗi hiển thị dùng formatter Updated hiện có. Calendar chuyển instant theo timezone Jira lưu trong bảng.

## Hợp đồng refresh saved table

1. Kết quả JQL chỉ được thay sau khi tất cả trang JQL tải thành công như trước.
2. Changelog đủ tất cả trang: cập nhật Real Due Date từ lịch sử hoàn chỉnh; nếu không có transition đủ điều kiện thì lưu giá trị trống.
3. API changelog trả lỗi ở bất kỳ batch/trang nào: bỏ toàn bộ dữ liệu changelog một phần, lưu snapshot tìm kiếm mới theo refresh hiện tại, giữ Real Due Date cũ theo issue ID cho ticket trùng; issue mới để trống. Thông báo rõ rằng ngày thực tế chưa được cập nhật.
4. Người dùng hủy refresh: không commit snapshot hoặc changelog, giữ dữ liệu cũ theo hành vi refresh hiện có.
5. Mở bảng/calendar offline chỉ đọc snapshot, không gọi Jira.

## Hợp đồng bảng và calendar

- Bảng đã lưu có thể hiển thị cột `Real Due Date`; kết quả truy vấn thông thường không có cột này.
- Calendar chiếu khoảng đỏ riêng từ ngày kế tiếp Dynamic Due Date tới ngày Real Due Date, inclusive.
- Đỏ chỉ được dùng nếu ngày Real Due Date sau ngày Dynamic Due Date theo timezone của bảng.
- Đoạn đỏ tuân theo giới hạn cửa sổ calendar hiện hành, có thể bị cắt/đánh dấu ở mép theo quy tắc calendar; không mở rộng cửa sổ.
- Màu đỏ là dấu hiệu duy nhất cho đoạn quá hạn.
