# Hợp đồng UI: Thùng rác bảng đã lưu

## Navigation

- Sidebar có destination **Thùng rác** ở cả trạng thái mở rộng và thu gọn.
- Chọn destination mở danh sách thùng rác toàn cục, không phụ thuộc Jira connection.
- Trở lại **Bảng đã lưu** hiển thị danh sách đang hoạt động; bảng trong thùng rác không được render ở detail hoạt động.

## Trash list

- Hiển thị tên bảng và thông tin nhận diện đủ để người dùng chọn đúng mục.
- Sắp xếp theo thời điểm xoá mới nhất trước.
- Mỗi mục có hai hành động: **Khôi phục** và **Xoá vĩnh viễn**.
- Danh sách rỗng có nội dung giải thích, không chỉ là vùng trống.
- Không cung cấp refresh, mở lịch hoặc mở chart cho mục đang trong thùng rác.

## Restore

- Tên không xung đột: khôi phục ngay về danh sách bảng đã lưu.
- Tên xung đột với bảng hoạt động: yêu cầu nhập tên thay thế; áp dụng trim, không rỗng, duy nhất không phân biệt hoa/thường.
- Nếu tên thay thế rỗng/trùng, giữ nguyên mục trong thùng rác và giải thích lỗi.
- Khôi phục giữ ID, JQL, tickets, cột, timezone và burndown history.

## Permanent delete

- **Xoá vĩnh viễn** mở bước xác nhận riêng, nêu rõ không thể hoàn tác.
- Xác nhận loại bỏ mục khỏi thùng rác; huỷ đóng dialog và không thay đổi dữ liệu.
- Lỗi lưu phải hiển thị thông báo và giữ mục trong danh sách.

## Persistence/error behavior

- Move, restore và permanent delete là thao tác local; không phát sinh request Jira.
- Chỉ phản ánh thay đổi trong UI sau khi catalog write thành công.
- Khi catalog không đọc được, không thực hiện thao tác ghi và giữ nguyên file.
