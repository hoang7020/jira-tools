# Mô hình dữ liệu: Done trong Real Due Date

## Trạng thái kết thúc hợp lệ

Tập trạng thái đủ điều kiện gồm `Close`, `Resolve`, và `Done`.

- `toString.trim()` trước khi so sánh.
- Không phân biệt hoa thường.
- So khớp toàn chuỗi.

## Real Due Date

`Real Due Date` vẫn là instant của changelog status mới nhất mà trạng thái đích thuộc tập trên. Task không có trạng thái đủ điều kiện vẫn có giá trị trống.

## Luồng xử lý

1. Chỉ item `status` được xem xét.
2. Tên trạng thái đích được chuẩn hóa bằng trim.
3. So sánh không phân biệt hoa thường với `Close`, `Resolve`, `Done`.
4. Chọn changelog có thời điểm lớn nhất trong các transition đủ điều kiện.
