# Hợp đồng mapping Real Due Date

## Input

Danh sách Jira issue changelog, trong đó mỗi history có:
- thời điểm `created`;
- các changelog item;
- tên trạng thái đích `toString`.

## Điều kiện status hợp lệ

Một history tạo candidate Real Due Date khi:
1. Có item field status.
2. Trạng thái đích sau khi trim khớp chính xác một trong `Close`, `Resolve`, `Done`.
3. Phép khớp không phân biệt hoa thường.

## Output

Mapper trả về issue ID và timestamp mới nhất của candidate hợp lệ. Không trả output cho issue không có candidate.
