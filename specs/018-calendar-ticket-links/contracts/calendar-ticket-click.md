# Contract: Calendar Ticket Click

## Input

`openTicket(ticketKey: String): Unit`

- Kích hoạt chỉ khi click phần mã ticket.
- Không kích hoạt khi click timeline hoặc vùng trống.
- `ticketKey` phải non-empty.

## Success Result

- Trình duyệt mở URL Jira ticket tương ứng.
- Calendar giữ nguyên dữ liệu, bộ lọc, cuộn và ngày đang xem.

## Failure Result

- Không có Jira base URL hợp lệ: trả thông báo thiếu địa chỉ Jira.
- Hệ điều hành hoặc URL opener không mở được: trả thông báo cùng URL để mở thủ công.
- Calendar không bị đóng hoặc reset.
