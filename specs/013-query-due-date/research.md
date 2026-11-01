# Nghiên cứu: Cột Due Date

## Quyết định 1: Ánh xạ trường ngày chuẩn của Jira

- **Decision**: Yêu cầu trường Jira `duedate` trong `TicketMapper.requestFields`, thêm `FieldsDto.dueDate` nullable với tên JSON `duedate`, và ánh xạ vào `Ticket.dueDate` nullable.
- **Rationale**: API tìm kiếm chỉ trả những trường được yêu cầu; DTO và mapper là ranh giới hiện có giữa JSON Jira và model dùng chung. `Ticket` đã là payload được lưu cùng snapshot nên giá trị được giữ nguyên khi lưu/mở bảng.
- **Alternatives considered**: Truy vấn từng issue riêng lẻ bị loại vì thêm mạng và làm chậm luồng truy vấn; dùng `updated` hoặc các due-date động bị loại vì không tương đương ngày đến hạn chuẩn Jira.

## Quyết định 2: Biểu diễn và sắp xếp ngày

- **Decision**: Lưu chuỗi ngày lịch nullable Jira trả về (thường `yyyy-MM-dd`), hiển thị nguyên giá trị, không chuyển timezone. Khi sắp xếp, parse theo ngày lịch và so sánh theo thứ tự thời gian; giá trị thiếu/không parse được được xem như thiếu, không tạo ngày giả.
- **Rationale**: Due Date không có giờ hoặc offset. Giữ nguyên chuỗi loại bỏ nguy cơ ngày bị đổi do timezone; parse riêng cho sorting tránh sắp xếp từ điển sai khi dữ liệu không đồng dạng.
- **Alternatives considered**: Chuyển thành instant bị loại vì trường không biểu diễn thời điểm; sắp xếp chuỗi trực tiếp bị loại vì không bảo đảm đúng thứ tự ngày khi gặp dữ liệu bất thường.

## Quyết định 3: Cột mặc định và migration

- **Decision**: Tăng schema config từ 3 lên 4. Sau khi áp dụng các migration phiên bản cũ hiện có, chỉ tự thêm cột nếu tập cột đang lưu bằng chính xác bộ cột mặc định của schema 3; nếu khác, giữ nguyên. Thêm `DUE_DATE` vào các cột truy vấn mặc định và cột khả dụng của bảng đã lưu. Với SavedTable đã có, không migration `visibleColumns`; user có thể bật cột thủ công. Bảng mới nhận default chứa Due Date.
- **Rationale**: Đáp ứng default mới mà không ghi đè tuỳ chỉnh; tuỳ chọn hiển thị của SavedTable lưu riêng theo từng bảng. Cần tăng schema config để nhận diện và migrate đúng cấu hình cũ.
- **Alternatives considered**: Thêm cột cho mọi người dùng bị loại vì ghi đè việc ẩn có chủ ý; không thêm cho cấu hình mặc định cũ bị loại vì khiến người dùng mặc định không thấy tính năng mới.

## Quyết định 4: UI và kiểm thử

- **Decision**: Tích hợp vào các switch exhaustive hiện hữu của `TicketTable`, `TableColumnLayout`, `TicketSorting`, query preferences và SavedTable columns. Kiểm thử trong `commonTest` cho null/giá trị hợp lệ, sắp xếp theo lịch, migration mặc định và tuỳ chỉnh, và hiển thị/tính khả dụng trong hai loại bảng.
- **Rationale**: Tái sử dụng điểm mở rộng hiện tại giữ UI nhất quán và tuân thủ test-first cho logic chia sẻ.
- **Alternatives considered**: Tạo thành phần UI/model riêng bị loại vì cột dùng cùng cấu trúc bảng và không có hành vi đặc biệt ngoài kiểu ngày.
