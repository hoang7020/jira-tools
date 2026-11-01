# Saved Table Row Presentation Contract

## Inputs

- `ticket.dynamicDueAtMillis`: timestamp epoch hợp lệ hoặc null.
- `nowAtMillis`: thời điểm hiện tại của màn hình.
- `highlightOverdueRows`: true chỉ khi render bảng đã lưu.

## Output

- `isOverdue = true`: dòng ticket được tô màu quá hạn toàn dòng.
- `isOverdue = false`: dòng giữ nền mặc định.
- Timestamp null hoặc không hợp lệ: `isOverdue = false`.

## Boundary Rules

- Không thay đổi ticket, thứ tự, sort hoặc quan hệ cha-con.
- Áp dụng màu đỏ chỉ trong bảng đã lưu.
