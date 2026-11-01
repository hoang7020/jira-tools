# Nghiên cứu: Nhóm ticket cha và sub-task

## Quyết định 1: Lấy quan hệ cha từ Jira search

- **Decision**: Thêm `parent`, `issuetype` và `subtasks` vào requested fields; ánh xạ `parent.key`, `issuetype.subtask` và danh sách sub-task của cha.
- **Rationale**: Đây là dữ liệu trực tiếp, tránh API phụ và giới hạn thay đổi trong một trang kết quả.
- **Alternatives considered**: Gọi issue riêng cho từng sub-task hoặc suy đoán cha từ tiền tố mã ticket; hai cách đều không đáng tin cậy.

## Quyết định 2: Gom nhóm ở logic dùng chung

- **Decision**: Thêm hàm sắp xếp ticket cha-con UI-independent, trả về danh sách mới và không thay đổi object ticket.
- **Rationale**: Tuân thủ nguyên tắc UI không chứa business rule và cho phép test bằng commonTest.
- **Alternatives considered**: Gom trong `LazyColumn`; bị loại vì khó test và làm UI chứa logic nghiệp vụ.

## Quyết định 3: Giữ thứ tự tương đối

- **Decision**: Ghép cha trước toàn bộ sub-task trực tiếp của nó và giữ thứ tự sub-task theo thứ tự đầu vào.
- **Rationale**: Khớp yêu cầu và ổn định thứ tự khi người dùng tải thêm trang.
- **Alternatives considered**: Giữ vị trí ban đầu của cha đồng thời đảo các sub-task lên trên; cách này không đảm bảo cha luôn đứng trước sub-task.

## Quyết định 4: Sort theo nhóm cha-con

- **Decision**: Sort Jira trước, sau đó gom cha và sub-task thành nhóm atom; sort chỉ thay đổi thứ tự các nhóm cha và các ticket độc lập.
- **Rationale**: Truy vấn ORDER BY cần tiếp tục hoạt động trên toàn bộ dữ liệu, nhưng cấu trúc cha-con không được phép bị phá khi kết quả trả về.
- **Alternatives considered**: Sort từng ticket trong danh sách rải rác; cách này phá quan hệ trực quan và có thể đưa sub-task lên trước cha.

## Quyết định 5: Hiển thị cây thư mục trực quan

- **Decision**: Compose thêm nhánh `├`/`└` vào phần hiển thị của ticket sub-task, không đặt nhánh này vào dữ liệu lưu trữ Ticket.
- **Rationale**: Ký hiệu là presentation detail phụ thuộc vị trí trong nhóm, nên được tính từ danh sách đã nhóm thay vì lưu trong domain.
- **Alternatives considered**: Lưu chuỗi nhánh trong Ticket; cách này dễ tạo trạng thái không nhất quán khi grouping hoặc sort thay đổi.
