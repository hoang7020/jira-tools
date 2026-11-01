# Research: Calendar Ticket Jira Links

## Decision 1: Tái dụng luồng mở ticket hiện có

- **Decision**: Calendar gọi callback mở ticket của bảng đã lưu thay vì dựng URL trong Compose UI.
- **Rationale**: ViewModel đã có địa chỉ Jira của bảng và cơ chế chuyển lỗi từ `UrlOpener`.
- **Alternatives considered**: Tạo TicketOpener riêng cho Calendar; bị bác bỏ vì trùng logic hiện có.

## Decision 2: Click chỉ trên mã ticket

- **Decision**: Thêm click handler trực tiếp trên text mã ticket trong label.
- **Rationale**: Thỏa mãn yêu cầu mã ticket là điểm tương tác duy nhất và không thay đổi cuộn Calendar.
- **Alternatives considered**: Click cả dòng hoặc label; bị bác bỏ vì mở link ngoài phạm vi ý định.

## Decision 3: Thông báo lỗi qua message flow hiện có

- **Decision**: Tiếp tục dùng notification/message flow của màn hình bảng đã lưu.
- **Rationale**: Calendar nằm trong flow đó và ViewModel đã trả thông báo lỗi cho các luồng mở ticket khác.
- **Alternatives considered**: Thông báo riêng trong Calendar; bị bác bỏ vì làm trùng cơ chế UI.
