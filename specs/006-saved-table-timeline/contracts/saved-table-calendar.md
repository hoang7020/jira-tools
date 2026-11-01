# UI Contract: Calendar của Bảng đã lưu

## Phạm vi

Calendar là chế độ xem chỉ đọc trong ứng dụng Desktop, lấy snapshot của bảng đã lưu. Mở calendar không gọi Jira, sửa ticket hay thay đổi catalog.

## Luồng

1. Người dùng chọn một bảng trong mục **Bảng đã lưu**.
2. Chi tiết bảng hiển thị nút **Mở lịch**.
3. Calendar mở riêng, thay phần xem bảng kết quả trong vùng chi tiết.
4. Khi mở, ứng dụng chụp thời điểm hiện tại qua `AppClock`; ngày hôm nay được xác định theo timezone Jira lưu trong bảng. Mở lại calendar sẽ tính một cửa sổ mới.
5. Nút **Quay lại** trở về đúng bảng và thứ tự sort trước đó.
6. Khi snapshot bảng thay đổi sau refresh, calendar phản ánh ticket mới nhất; mốc thời gian tham chiếu giữ ổn định tới khi đóng calendar.

## Bố cục và trục ngày

- Phần dữ liệu chỉ có cột mã ticket ở trái và cột calendar ở phải.
- Cột ticket luôn cố định khi cuộn ngang; mỗi ticket giữ một hàng theo thứ tự bảng đã sắp xếp.
- Cửa sổ ngày là từ `today - 1 calendar month` tới `today + 3 calendar months`, inclusive. Nếu ngày tương ứng không tồn tại ở tháng đích, dùng ngày cuối tháng đó.
- Múi giờ bảng dùng để lấy `today` và chuyển instant của task thành ngày; không dùng timezone thiết bị làm fallback.
- Mỗi ngày lịch trong cửa sổ có một ô liên tục, kể cả cuối tuần; header cho biết thứ và ngày/tháng/năm. Header và thanh task dùng chung vị trí cuộn ngang; hàng ticket cuộn dọc.
- Khi mở, vùng ngày khởi đầu tại ngày hiện tại. Người dùng có thể cuộn về đầu cửa sổ hoặc tới cuối cửa sổ nhưng không ra ngoài biên.

## Thanh và thông báo

- Khoảng hợp lệ bao gồm cả ngày bắt đầu và ngày kết thúc.
- Chỉ vẽ phần giao của khoảng task với cửa sổ. Phần tiếp diễn trước/sau khung có dấu tại mép tương ứng; khoảng cùng ngày có dấu nhìn thấy được.
- Khoảng hợp lệ hoàn toàn ngoài cửa sổ không có thanh; hàng ticket vẫn có thông báo nằm ngoài phạm vi đang xem.
- Task thiếu/sai mốc vẫn giữ hàng và giải thích lý do, không có thanh gây hiểu nhầm.
- Nếu timezone hợp lệ nhưng bảng rỗng hoặc không có khoảng hợp lệ, vẫn hiển thị cửa sổ ngày và các hàng ticket hiện có; không có thanh và có trạng thái trống giải thích.
- Nếu timezone thiếu/sai, hiển thị cảnh báo, giữ hàng ticket nhưng không dựng trục ngày không xác định.

## Ngoài phạm vi contract

- Kéo/thả thanh, sửa Dynamic Start Time/Due Date, lưu lịch, lọc task, request Jira khi mở calendar hoặc thay schema bảng đã lưu.
