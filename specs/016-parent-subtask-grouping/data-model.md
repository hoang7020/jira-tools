# Mô hình dữ liệu

## Ticket

- `key`: mã duy nhất của ticket.
- `parentKey`: mã ticket cha; `null` nếu không phải sub-task hoặc Jira không cung cấp quan hệ.
- `hasKnownSubtasks`: Jira báo ticket có ít nhất một sub-task thông qua trường `subtasks`.
- `isParent`: ticket cha được xác định là cha trong danh sách kết quả hiện tại.
- `isSubtask`: ticket được Jira nhận diện là sub-task.

`isParent` được xác định bằng dữ liệu `subtasks` của chính cha, hoặc bằng một ticket con trong kết quả có `parentKey` trùng `key`. Vì vậy cha vẫn được tô xám ngay khi không có sub-task nào hiển thị trong kết quả.

## Quan hệ cha-con

- Một sub-task chỉ tham chiếu tối đa một ticket cha.
- Ticket cha có thể có nhiều sub-task trong kết quả.
- Orphan sub-task giữ nguyên vị trí tương đối nếu cha không xuất hiện trong kết quả hiện tại.

## Quy tắc nhóm

1. Đưa cha lên vị trí neo theo thứ tự kết quả hiện tại.
2. Chèn các sub-task trực tiếp ngay sau cha.
3. Giữ thứ tự tương đối của sub-task cùng cha.
4. Không tạo dòng cha giả cho sub-task orphan.
5. Khi sort được bật, sort các nhóm cha và ticket độc lập thay vì từng ticket con; mọi sub-task của một cha vẫn đi theo cha.

## Trạng thái nhánh cây

- Nhánh là metadata hiển thị, không thuộc dữ liệu Jira và không được lưu trong `Ticket`.
- Sub-task cuối trong nhóm dùng `└`.
- Sub-task không cuối trong nhóm dùng `├`.
