# Research: Thùng rác bảng đã lưu

## Quyết định 1: Lưu mục thùng rác trong cùng catalog JSON

**Decision**: Thêm danh sách `trashedTables` vào `SavedTableCatalog`; mỗi phần tử chứa nguyên trạng `SavedTable` và `deletedAtMillis`. Nâng schema catalog từ v4 lên v5; migration cũ tạo danh sách thùng rác rỗng.

**Rationale**:

- Bảng đang hoạt động và thùng rác được commit trong cùng một atomic write, nên không có trạng thái bảng đồng thời có mặt hoặc biến mất khỏi cả hai danh sách khi thao tác thành công.
- `SavedTable` chứa đầy đủ JQL, tickets, timezone, columns, burndown snapshots và các trường có thể được bổ sung sau này; giữ nguyên object giúp restore không phải sao chép lại các trường.
- Catalog JSON và schema migration đã tồn tại; không cần thêm storage hoặc dependency.

**Alternatives considered**:

- Cờ `isDeleted` trên bảng: khiến mọi nơi duyệt `tables` phải lọc thủ công và tăng nguy cơ hiển thị/refresh nhầm bảng trong thùng rác.
- Catalog/file riêng cho thùng rác: cần xử lý đồng bộ hai lần ghi và tạo nguy cơ một bảng xuất hiện trong cả hai trạng thái hoặc không ở đâu.

## Quyết định 2: Atomic move và cập nhật UI sau write

**Decision**: Mỗi move, restore hoặc permanent delete tạo catalog mới rồi gọi `SavedTableStore.save()` đúng một lần. Chỉ cập nhật StateFlow sau khi write thành công; lỗi write giữ nguyên dữ liệu/state trước đó và đưa thông báo lỗi theo chuẩn hiện có.

**Rationale**: Khớp hành vi `SavedTablesViewModel.write()` hiện tại, đảm bảo thao tác file lỗi không hiển thị thành công giả và không mất bản dữ liệu cũ trong bộ nhớ.

**Alternatives considered**:

- Cập nhật StateFlow trước, rollback nếu write lỗi: tăng nhánh trạng thái trung gian và có thể flash dữ liệu không được lưu.
- Xoá/gỡ bảng rồi ghi thùng rác ở hai bước: có cửa sổ mất dữ liệu khi bước thứ hai thất bại.

## Quyết định 3: Tên khôi phục phải duy nhất trong danh sách hoạt động

**Decision**: Khôi phục tên hiện tại nếu không trùng; nếu trùng, yêu cầu nhập tên thay thế trước thao tác commit. Tên mới dùng cùng trim, không rỗng và so sánh không phân biệt hoa/thường như tạo/đổi tên hiện tại. Chỉ trường `name` thay đổi.

**Rationale**: Đã được người dùng xác nhận trong clarification. Reuse quy tắc tên hiện có, giữ nguyên ID/JQL/ticket/history, không thể ghi đè bảng đang hoạt động.

**Alternatives considered**:

- Đổi tên tự động: dễ tạo kết quả bất ngờ và làm thay đổi tên khi người dùng không yêu cầu.
- Cho phép hai bảng cùng tên: danh sách và thông báo thao tác sẽ khó phân biệt.
- Chặn restore cho tới khi đổi tên bảng đang hoạt động: buộc người dùng sửa dữ liệu không liên quan.

## Quyết định 4: Permanent delete không xoá ledger Dynamic Start Time dùng chung

**Decision**: Permanent delete loại bỏ record bảng và toàn bộ nội dung gắn riêng với nó (tickets, burndown snapshots); giữ nguyên `dynamicStartTimes` ở catalog.

**Rationale**: Ledger được key theo Jira site + issue ID, không theo saved-table ID, và được chia sẻ qua nhiều bảng. Xóa ledger cùng một bảng có thể làm hỏng mốc Dynamic Start Time của bảng khác hoặc lần lưu mới.

**Alternatives considered**:

- Xóa mọi ledger record của các issue trong bảng: có thể phá dữ liệu được dùng chung bởi bảng khác; không có mapping ownership để chứng minh record chỉ thuộc bảng vừa xóa.
- Chuyển ledger vào bản ghi bảng: thay đổi mô hình dữ liệu ngoài phạm vi và làm thay đổi semantics hiện tại.

## Quyết định 5: Thứ tự và phạm vi UI thùng rác

**Decision**: Mục mới nhất bị xoá hiển thị trước. Sidebar destination mở màn hình thùng rác toàn cục; mỗi hàng cung cấp nhận diện, Khôi phục và Xoá vĩnh viễn. Thùng rác rỗng có thông báo giải thích; không cung cấp thao tác Jira hoặc mở chart/calendar từ hàng thùng rác.

**Rationale**: Cho phép tìm lại mục vừa chuyển vào thùng rác nhanh; tránh xem như bảng hoạt động trước khi restore; phù hợp thao tác theo từng bảng được đặc tả.

**Alternatives considered**:

- Thứ tự cũ nhất trước: không ưu tiên thao tác xoá gần nhất.
- Mở detail/chart trực tiếp từ trash: cần xác định thêm quyền thao tác trên record đã xoá và không cần thiết cho restore/delete.
