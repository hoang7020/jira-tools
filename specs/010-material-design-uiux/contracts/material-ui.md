# Hợp đồng giao diện: Material Design 3

## Phạm vi

Áp dụng cho các destination Truy vấn, Bảng đã lưu và Cài đặt cùng calendar lồng trong bảng đã lưu. Không thay đổi contract dữ liệu Jira hoặc chức năng của các thao tác hiện có.

## App shell và điều hướng

- App shell hiển thị thanh điều hướng trái có thể mở rộng/thu gọn.
- Có ba destination cấp cao: **Truy vấn**, **Bảng đã lưu**, **Cài đặt**.
- Destination đang chọn có chỉ báo thị giác rõ ràng; khi thu gọn, biểu tượng và accessibility label vẫn nhận diện được.
- Trạng thái nội dung màn hình được giữ nguyên khi chuyển destination.
- Calendar là màn hình con của Bảng đã lưu; hành động quay lại trở về chi tiết bảng.
- Toàn bộ destination và thao tác chính có thể truy cập bằng bàn phím; focus được nhìn thấy.

## Theme

- Giá trị lựa chọn: **Theo hệ điều hành**, **Sáng**, **Tối**.
- Mặc định là **Theo hệ điều hành**; thay đổi thiết lập hệ điều hành được phản ánh nếu chưa có ghi đè.
- Lựa chọn Sáng/Tối riêng được lưu giữa các lần chạy.
- Theme cung cấp semantic colors cho surface, content, primary, outline, error và trạng thái tương ứng; nội dung không mã hoá trạng thái chỉ bằng màu.

## Quy tắc trình bày chung

- Mỗi destination có tiêu đề rõ, nội dung phân cấp và một hành động chính dễ nhận ra.
- Dùng vai trò typography và màu Material nhất quán; không tạo kiểu tuỳ tiện cho từng màn hình.
- Trạng thái loading, empty, error, warning, success, selected và disabled có chỉ dấu phù hợp; lỗi và cảnh báo có văn bản.
- Form giữ nội dung người dùng cần sửa; token đã lưu không hiển thị nguyên văn.
- Dialog phá huỷ phân biệt rõ nút huỷ với nút xác nhận; nội dung nêu hệ quả.
- Bố cục resize không làm mất thao tác trọng yếu hoặc chồng lấn vùng nội dung.

## Bảo toàn chức năng

- Truy vấn JQL, phân trang, sắp xếp, chọn cột, lưu bảng, đổi tên, xoá, làm mới, mở ticket và cuộn calendar giữ hành vi hiện tại.
- Không thêm hoặc bỏ bước nghiệp vụ của Jira; thay đổi trạng thái trình bày không ghi đè dữ liệu nghiệp vụ.
