# Nghiên cứu: Thêm Done vào Real Due Date

## R1. Quy tắc nhận diện trạng thái kết thúc

**Quyết định**: Mở rộng điều kiện status transition hiện có bằng trạng thái `Done`. So khớp `Close`, `Resolve` và `Done` sau `trim()` và không phân biệt hoa thường; không khớp chuỗi con.

**Lý do**: Mapper hiện đã dùng quy tắc trim/không phân biệt hoa thường cho `Close` và `Resolve`. Tái sử dụng quy tắc này tạo hành vi nhất quán cho workflow Jira tùy biến.

**Thay thế đã cân nhắc**:
- Cho phép cấu hình trạng thái: loại vì ngoài phạm vi feature, không có yêu cầu người dùng cấu hình.
- Khớp chính xác có phân biệt hoa thường: loại vì không nhất quán với hành vi hiện có.
- Khớp trạng thái chứa từ `Done`: loại vì dễ nhận nhầm `Done Review` hoặc trạng thái tương tự.

## R2. Cách chọn Real Due Date

**Quyết định**: Giữ nguyên cơ chế chọn sự kiện status transition mới nhất theo thời điểm changelog; `Done` tham gia cùng `Close` và `Resolve`.

**Lý do**: Real Due Date đã được định nghĩa là lần chuyển trạng thái hợp lệ cuối cùng. Không cần thêm schema hay trạng thái mới.

## R3. Kiểm thử

**Quyết định**: Cập nhật `StatusChangelogMapperTest` để phủ `Done`, biến thể hoa/thường và chuỗi không khớp; thêm trường hợp nhiều transition hỗn hợp chọn timestamp mới nhất.

**Lý do**: Đây là logic chia sẻ, cần test-first trong commonTest theo hiến pháp dự án.
