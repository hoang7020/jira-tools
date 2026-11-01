# Mô hình dữ liệu: Ngày hoàn thành thực tế và quá hạn

## 1. Ticket snapshot

`Ticket` tiếp tục là mô hình chung của bảng query và saved table. Các trường Real Due Date chỉ được điền trong flow saved-table; query table thông thường không hiển thị chúng.

| Trường | Kiểu | Ràng buộc / ý nghĩa |
|---|---|---|
| `issueId` | `String?` | ID ổn định từ Jira; dùng nối status changelog với ticket. |
| `realDueDate` | `String?` | Chuỗi thời gian hiển thị của chuyển trạng thái đủ điều kiện gần nhất; null khi không có transition hoặc chưa thể đọc. |
| `realDueAtMillis` | `Long?` | Instant của chuyển trạng thái mới nhất; dùng để sắp xếp/chiếu ngày và lưu chính xác. |
| `dynamicDueDate` | `String?` | Giá trị ngày đến hạn dự kiến sẵn có. |
| `dynamicDueAtMillis` | `Long?` | Instant hạn dự kiến sẵn có. |

Ticket cũ không có Real Due Date tiếp tục giải mã với hai trường mới null.

## 2. Jira status history

DTO nội bộ biểu diễn một changelog:

| Thuộc tính | Kiểu/ý nghĩa |
|---|---|
| Issue ID | Jira issue ID trong changelog, nối với `Ticket.issueId`. |
| `created` | Thời điểm tạo changelog, là thời điểm status transition. |
| Items | Các thay đổi trong changelog; chỉ item có field `status` tham gia quyết định. |
| `fromString`, `toString` | Tên trạng thái trước và sau; chỉ `toString` xác định đích đủ điều kiện. |

Kết quả tính cho một issue:

- Chỉ xét item `status` có `toString` sau khi trim bằng `Close` hoặc `Resolve`, không phân biệt hoa thường.
- Chọn sự kiện mới nhất theo thời điểm changelog.
- Parse thời điểm thành instant; nếu không parse được thì kết quả không có Real Due Date hợp lệ.
- Một phản hồi changelog hoàn chỉnh không có transition đủ điều kiện cho issue nghĩa là `realDueDate` và `realDueAtMillis` được đặt null.
- Nếu bất kỳ batch/trang nào thất bại, bỏ toàn bộ kết quả changelog của lần refresh đó; giữ giá trị cũ theo issue ID cho ticket còn lại, issue mới để null.

## 3. Cột saved table

`ColumnKey.REAL_DUE_DATE` có nhãn `Real Due Date`, thuộc nhóm saved-only. Tập cột query không đổi. Migration catalog thêm cột vào `visibleColumns` của mỗi bảng cũ trong khi giữ các lựa chọn đã lưu hiện tại.

## 4. Calendar projection

Mỗi hàng calendar hiện có tiếp tục chứa ngày dự kiến và trạng thái khoảng. Bổ sung các giá trị dẫn xuất:

| Giá trị | Ý nghĩa |
|---|---|
| `realDueDate` | Ngày Real Due Date sau khi đổi instant theo timezone của bảng. |
| `overdueStartDayOffset` | Offset từ đầu cửa sổ tới ngày tiếp theo Dynamic Due Date; null khi không có đoạn đỏ. |
| `overdueDurationDays` | Số ngày inclusive từ overdue start tới Real Due Date trong phần nhìn thấy; null khi không có đoạn đỏ. |
| `overdueContinuesBeforeWindow` | Khoảng quá hạn bắt đầu trước cửa sổ và tiếp tục vào phần đang hiển thị. |
| `overdueContinuesAfterWindow` | Đoạn đỏ hợp lệ tiếp tục vượt mép cuối cửa sổ. |
| `overdueOutsideWindow` | Hướng `BEFORE` hoặc `AFTER` nếu đoạn đỏ nằm hoàn toàn ngoài cửa sổ; null nếu có giao hoặc không có đoạn. |

Quy tắc:

- Tính đoạn chỉ khi có Dynamic Due Date và Real Due Date hợp lệ, timezone hợp lệ, và Real Due Date rơi sau Dynamic Due Date theo ngày địa phương.
- Bắt đầu vào ngày sau Dynamic Due Date; kết thúc inclusive tại ngày Real Due Date.
- Nếu Real Due Date cùng ngày hoặc trước Dynamic Due Date thì không có đoạn đỏ.
- Cắt phần giao theo cửa sổ hiện tại; đánh dấu tiếp diễn tại mép trái/phải khi khoảng quá hạn còn kéo dài bên ngoài, hoặc giữ hàng không có thanh và thông tin ngoài phạm vi khi khoảng nằm hoàn toàn ngoài cửa sổ. Không đổi trục ngày, không đổi hàng/task.
- Màu đỏ là dấu hiệu duy nhất theo lựa chọn của người dùng.

## 5. Migration catalog

Saved table catalog hiện ở schema v2; feature nâng lên schema v3.

- Schema v2 → v3 giữ bảng, snapshot ticket, DynamicStartTime records, timezone và visible columns hiện tại; thêm `REAL_DUE_DATE` vào các cột saved table.
- Schema v1 tiếp tục được migrate theo logic v1→v2 hiện hữu, sau đó áp dụng v2→v3.
- Ticket cũ nhận `realDueDate = null` và `realDueAtMillis = null`.
- Migration trên load không ghi ngay; lần ghi hợp lệ tiếp theo lưu schema v3.
- JSON lỗi hoặc schema tương lai không bị ghi đè.

## 6. State và refresh

```text
Load toàn bộ trang JQL thành công
  ├─ Lịch sử status tải hết thành công
  │    ├─ có transition Close/Resolve mới nhất -> lưu timestamp transition đó
  │    └─ không có transition đủ điều kiện -> đặt Real Due Date null
  └─ API lịch sử status trả lỗi
       ├─ issue đã có Real Due Date -> giữ giá trị cũ
       └─ issue mới -> giữ null
       -> lưu snapshot JQL mới và báo Real Due Date chưa cập nhật
```

- Jira status history được lấy qua bulk API, tối đa 1000 issue IDs mỗi nhóm và phân trang đầy đủ.
- Chỉ sử dụng kết quả changelog khi tất cả trang/batch hoàn tất.
- Nếu refresh coroutine bị hủy, không commit snapshot nào theo hành vi hiện tại.
- Ticket history lưu trong từng SavedTable snapshot; các bảng khác thay đổi khi chính chúng được làm mới.
- Lỗi ghi catalog theo quy tắc hiện có: không cập nhật StateFlow thành công giả.
