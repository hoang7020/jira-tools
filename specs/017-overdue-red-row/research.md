# Research: Highlight overdue Dynamic Due Date

## Decision 1: So sánh theo epoch giây

- **Decision**: Chuẩn hóa timestamp epoch về giây bằng phép chia `1000`, sau đó task quá hạn khi `dynamicDueAtMillis / 1000 < nowMillis / 1000`.
- **Rationale**: Đáp ứng yêu cầu tính theo giây và tránh cùng giây nhưng khác mili giây bị coi là khác nhau.
- **Alternatives considered**: So sánh mili giây trực tiếp; loại bỏ vì không đúng yêu cầu tính theo giây. Sử dụng chuỗi hiển thị; loại bỏ vì dễ sai múi giờ và định dạng.

## Decision 2: Cập nhật trạng thái qua ticker từng giây

- **Decision**: Bảng đã lưu duy trì thời điểm hiện tại được refresh mỗi giây trong khi màn hình đang hiển thị.
- **Rationale**: Dòng chuyển đỏ đúng sau mốc quá hạn mà không cần reload hoặc thao tác mới.
- **Alternatives considered**: Refresh khi tương tác; loại bỏ vì không cập nhật theo thời gian thực. Refresh mỗi 500ms; không cần thiết cho ngưỡng giây.

## Decision 3: Tái dụng TicketTable

- **Decision**: Thêm tham số presentation cho TicketTable bật tô đỏ theo thời điểm hiện tại, mặc định tắt để bảng truy vấn không bị ảnh hưởng.
- **Rationale**: Bảng đã lưu đã tái dụng TicketTable, nên UI có thể chia sẻ layout mà không nhân bản màn hình/cột/sort.
- **Alternatives considered**: Viết bảng riêng cho snapshot; loại bỏ vì trùng lặp và dễ làm lệch sort/cột.
