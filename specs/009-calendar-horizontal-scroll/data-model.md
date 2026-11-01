# Mô hình dữ liệu: Cuộn ngang Calendar

## Nguyên tắc

Calendar là projection tạm thời từ snapshot bảng đã lưu. Khoảng ngày mở rộng và vị trí scroll chỉ tồn tại trong vòng đời giao diện; không thay đổi schema, snapshot hoặc Jira.

## Thực thể hiện có

### SavedTable snapshot

- **Vai trò**: Nguồn ổn định để dựng lại calendar khi cửa sổ ngày tăng/giảm.
- **Dữ liệu sử dụng**: danh sách ticket đã sort, timezone Jira của bảng và `calendarReferenceMillis`.
- **Vòng đời**: Khi ticket snapshot, sort hoặc bảng thay đổi, projection được tính lại theo range của màn hiện hành; không ghi dữ liệu phát sinh vào snapshot.

### SavedTableCalendarProjection

- **Vai trò**: Cung cấp header dates và rows/segments cho khoảng ngày đang được hiển thị hoặc cần hiển thị.
- **Trường liên quan**:
  - `windowStart`, `windowEnd`: hai ngày biên bao gồm.
  - `todayDate`, `timeZoneValid`, `hasValidRanges`: giữ ý nghĩa và trạng thái hiện có.
  - `rows`: ticket row và expected/overdue segments đã chiếu trong hai biên.
- **Ràng buộc**: `windowStart <= windowEnd`; `dayOffset` của segment được tính lại tương đối `windowStart`. Không dùng projection cũ để giả lập segment ngoài cửa sổ cũ.

## State chỉ tồn tại trong UI

### CalendarDateRange

- `startDate: LocalDate`: ngày đầu tiên trên trục.
- `endDate: LocalDate`: ngày cuối cùng trên trục.
- **Khởi tạo**: giữ khoảng hiện tại một tháng trước đến ba tháng sau ngày tham chiếu.
- **Mở rộng**: gần biên trái thì lùi `startDate` một tháng; gần biên phải thì tiến `endDate` một tháng.
- **Bất biến**: các ngày liên tục không trùng/mất ngày; các ngày đã được thêm giữ lại trong vòng đời calendar; không mở rộng vượt biên ngày có thể biểu diễn.
- **Tính chất lưu trữ**: state tạm; mở lại calendar quay về khoảng khởi tạo.

### CalendarViewportPosition

- `firstVisibleDayIndex: Int`: index trong range hiện tại của ngày đầu tiên đang thấy.
- `firstVisibleOffsetPx: Int`: phần pixel đã cuộn bên trong ngày đầu.
- `anchorDate: LocalDate`: ngày thực tế tại index đó, dùng bảo toàn viewport khi thêm ngày ở đầu.
- **Quy tắc prepend**: khi thêm `n` ngày trước range, range mới bắt đầu sớm hơn `n` ngày; index neo tăng `n` trong khi `firstVisibleOffsetPx` giữ nguyên.
- **Quy tắc append**: ngày neo và offset giữ nguyên.
- **Nguồn sự thật**: một scroll state duy nhất; header, lưới và mọi row đọc cùng index/offset.

## Luồng và kiểm tra

1. Tạo range ban đầu từ ngày tham chiếu và snapshot hiện tại.
2. Tính projection thuần từ tickets/timezone/reference/range rõ ràng.
3. Khi viewport đến gần biên, mở rộng range một tháng theo hướng cuộn và tính projection mới.
4. Bảo toàn ngày neo và offset pixel; nếu đã chạm giới hạn ngày hoặc index an toàn thì không mở rộng phía đó.
5. Render ngày liên tiếp lazy; ngày không có ticket vẫn có thể cuộn và hiển thị.

Việc mở rộng phải an toàn với overflow khi tính ngày, số ngày và index; lỗi timezone hoặc projection tiếp tục dùng trạng thái lỗi hiện tại và không tạo ngày giả.
