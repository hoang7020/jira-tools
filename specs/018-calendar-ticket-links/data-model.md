# Data Model: Calendar Ticket Jira Links

## Ticket Calendar Entry

- `ticketKey`: mã Jira duy nhất, bắt buộc để tạo liên kết ticket.
- `jiraBaseUrl`: URL Jira của bảng đã lưu, được cung cấp khi mở ticket.

## Derived Jira Ticket Link

- Không lưu trong model Calendar.
- Được dẫn xuất từ `jiraBaseUrl` và `ticketKey` bởi luồng mở ticket hiện có.
- Không hợp lệ khi Jira base URL không hợp lệ hoặc mã ticket rỗng.

## Relationships

- Một Ticket Calendar Entry thuộc đúng một bảng đã lưu.
- Một bảng đã lưu cung cấp một Jira base URL dùng chung cho mọi ticket trong Calendar.
