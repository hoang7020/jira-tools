# Hợp đồng nhóm kết quả truy vấn

## Đầu vào

Danh sách `Ticket` có:

- `key`
- `parentKey`
- `isSubtask`
- `hasKnownSubtasks`
- Quan hệ cha có thể được biết từ `parentKey` hoặc dữ liệu issue type.

## Đầu ra

Danh sách ticket đã nhóm với:

- Cha xuất hiện trước toàn bộ sub-task trực tiếp của nó.
- Sub-task cùng cha duy trì thứ tự đầu vào.
- Ticket không liên quan không nằm giữa cha và nhóm sub-task.
- Khi có sort: thứ tự các nhóm cha và ticket độc lập thay đổi theo cột sort, nhưng các sub-task không đổi vị trí tương đối trong nhóm cha.

## Hợp đồng trực quan cây thư mục

- Mỗi sub-task được hiển thị với một nhánh trước mã hoặc tiêu đề.
- Sub-task không cuối: `├`.
- Sub-task cuối: `└`.
- Nhánh không được thêm vào dữ liệu ticket hoặc thay đổi giá trị các cột.

## Xác định màu dòng

Một dòng được đánh dấu cha nếu Jira cung cấp danh sách `subtasks` không rỗng cho ticket, hoặc kết quả chứa ít nhất một sub-task tham chiếu ticket đó.
