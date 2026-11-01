# Data Model: Highlight overdue Dynamic Due Date

## Ticket

Ticket đã có các thuộc tính cần thiết:

- `dynamicDueDate: String?`: chuỗi ngày/giờ hiển thị.
- `dynamicDueAtMillis: Long?`: timestamp epoch dùng cho so sánh.

Không thêm trạng thái quá hạn vào dữ liệu lưu trữ vì đây là derived presentation state.

## Derived State

- `nowAtMillis: Long`: thời điểm hiện tại được cung cấp bởi UI.
- `isOverdue: Boolean`: kết quả domain thuần của Dynamic Due Date và thời điểm hiện tại.

## Validation Rules

- Ticket không có timestamp hợp lệ không quá hạn.
- Timestamp quá hạn khi `dynamicDueAtMillis / 1000 < nowMillis / 1000`.
- Timestamp bằng hoặc lớn hơn thời điểm hiện tại không quá hạn.
