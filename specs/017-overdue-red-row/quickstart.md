# Quickstart Validation

## Preconditions

- Ứng dụng đã mở và có ít nhất một bảng đã lưu.
- Có ticket quá hạn, ticket còn tương lai và ticket thiếu Dynamic Due Date hợp lệ.

## Validation

1. Mở bảng đã lưu và quan sát ticket quá hạn.
2. Chờ qua đúng mốc một giây sau timestamp Dynamic Due Date của ticket thử nghiệm.
3. Xác nhận dòng chuyển sang màu đỏ mà không reload.
4. Kiểm tra ticket tương lai và ticket thiếu ngày không đổi màu.
5. Chuyển sang bảng kết quả truy vấn và xác nhận tính năng không kích hoạt.
6. Chạy:

```sh
./gradlew :composeApp:jvmTest :composeApp:ktlintCheck
```

## Expected

- 100% ticket quá hạn trong bảng đã lưu được tô đỏ toàn dòng.
- Ticket bằng hoặc sau thời điểm hiện tại không bị tô đỏ.
- Không thay đổi dữ liệu hoặc thứ tự ticket.
