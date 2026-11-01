# Research: Nâng cấp UI/UX theo Material Design

## 1. Material Design và giao diện hiện có

- **Decision**: Tái sử dụng Compose Material 3 đã có, tạo theme cấp ứng dụng và áp dụng có hệ thống cho app shell, truy vấn, bảng đã lưu, calendar và Cài đặt.
- **Rationale**: Các màn hình hiện đã dùng `MaterialTheme`, `Button`, `TextButton`, `OutlinedTextField`, `AlertDialog`, `Surface` và các semantic color role. Nút thắt là cách tổ chức và áp dụng chưa nhất quán, không phải thiếu toolkit.
- **Alternatives considered**:
  - Thêm design-system hoặc UI library khác: bị loại vì trùng lặp với Material 3 và làm tăng dependency/bảo trì.
  - Chỉ đổi palette: không đủ đáp ứng thứ bậc điều hướng, trạng thái, bố cục và khả năng truy cập.

## 2. System theme và lựa chọn theme của người dùng

- **Decision**: Hỗ trợ `SYSTEM`, `LIGHT`, `DARK`; `SYSTEM` là mặc định và đọc trạng thái hệ điều hành qua `androidx.compose.foundation.isSystemInDarkTheme()`. Cho phép ghi đè bằng tuỳ chọn lưu trong cài đặt.
- **Rationale**: API có trong Compose Foundation common source; desktop actual gọi `androidx.compose.ui.isUiSystemInDarkTheme()`. Cách này theo đúng khuyến nghị Compose: đọc theme một lần ở tầng ứng dụng, sau đó truyền semantic color scheme cho các màn hình; người dùng có thể ép sáng/tối mà không nhân bản logic theme.
- **Alternatives considered**:
  - Tự truy vấn AWT/native OS API: bị loại vì cần mã nền tảng riêng và không cần thiết khi Compose cung cấp API đa nền tảng.
  - Chỉ sáng/tối cố định: không đáp ứng yêu cầu đã làm rõ cho system default và lựa chọn người dùng.

## 3. Lưu trữ tuỳ chọn

- **Decision**: Thêm enum theme vào `UserPreferences` serializable, dùng `AppConfigStore` hiện có, tăng schema từ 2 lên 3 và bảo toàn migration v1.
- **Rationale**: File cấu hình đã chứa tuỳ chọn truy vấn và có cơ chế migration/backward compatibility; theme mode là cài đặt người dùng, không phải dữ liệu Jira.
- **Alternatives considered**:
  - Tạo file cài đặt riêng: bị loại vì tạo thêm nguồn dữ liệu, đường dẫn và đồng bộ.
  - Không lưu lựa chọn riêng: bị loại vì user selection phải còn nguyên khi mở lại ứng dụng.

## 4. Điều hướng Desktop

- **Decision**: Dùng thanh điều hướng bên trái có thể thu gọn cho ba destination cấp cao: Truy vấn, Bảng đã lưu, Cài đặt. Calendar là màn hình con trong Bảng đã lưu, giữ affordance quay lại hiện tại.
- **Rationale**: Người dùng đã chọn rõ cấu trúc này. Thanh bên tạo vị trí ổn định cho điều hướng và có thể giải phóng chiều ngang cho bảng khi thu gọn.
- **Alternatives considered**:
  - Thanh ngang phía trên: giữ nhiều diện tích chiều cao và trạng thái hiện tại khó phân biệt.
  - Thanh bên luôn mở: dễ nhận biết nhưng chiếm chiều ngang thường trực.

## 5. Kiểm thử UI và khả năng truy cập

- **Decision**: Kiểm thử schema/theme mode trong commonTest; xác nhận layout, bàn phím, focus, trạng thái, resize và các luồng hiện có bằng quickstart trên Desktop.
- **Rationale**: Dự án hiện có commonTest cho domain/data/state nhưng chưa có harness UI; kiểm tra giao diện thủ công tránh thêm phụ thuộc chỉ để kiểm thử hình ảnh. Nội dung/biểu tượng phải bổ sung cho màu sắc để phân biệt trạng thái.
- **Alternatives considered**:
  - Thêm UI automation framework trong phạm vi này: chưa cần thiết cho mục tiêu và tạo chi phí cấu hình mới; có thể xem xét riêng nếu dự án cần kiểm thử UI tự động lâu dài.
