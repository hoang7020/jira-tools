# Kiểm chứng nhóm ticket cha và sub-task

## Chuẩn bị

1. Mở ứng dụng và kết nối Jira.
2. Chạy truy vấn có cha và sub-task trong cùng kết quả.

## Kịch bản

1. Xác nhận ticket cha xuất hiện với dòng xám mờ.
2. Xác nhận mọi sub-task hiển thị nằm ngay dưới ticket cha tương ứng.
3. Xác nhận sub-task của các cha khác không xen vào nhóm.
4. Tải trang tiếp theo và xác nhận nhóm được duy trì trong mỗi trang kết quả.
5. Loại cha khỏi kết quả và xác nhận orphan sub-task không tạo dòng cha giả.
6. Xác nhận mỗi sub-task có nhánh cây: `├` nếu không cuối và `└` nếu cuối trong nhóm.
7. Sort theo một cột bất kỳ và xác nhận mọi sub-task vẫn nằm ngay dưới cha; thứ tự các nhóm cha có thể thay đổi.

## Kiểm tra tự động mục tiêu

```bash
./gradlew :composeApp:jvmTest
./gradlew :composeApp:ktlintCheck
```
