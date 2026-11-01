# Hướng dẫn kiểm chứng nhanh (Quickstart)

**Feature**: `001-jql-ticket-table` | **Ngày**: 2026-10-30

Tài liệu này mô tả cách chạy ứng dụng và các kịch bản kiểm chứng thủ công. Chi tiết triển khai nằm ở `tasks.md` và giai đoạn implement.

## Điều kiện tiên quyết

| Yêu cầu | Ghi chú |
|---------|---------|
| JDK 17+ | Bắt buộc cho toolchain Gradle |
| Gradle Wrapper | Dùng `./gradlew`, không cần cài Gradle toàn cục |
| Tài khoản Jira Cloud | Cần site URL, email và API token (tạo tại Atlassian account settings) |
| Mạng Internet | Kết nối tới `<site>.atlassian.net` |

## Lệnh thường dùng

```bash
# Build toàn bộ (bao gồm cổng chất lượng trong hiến pháp)
./gradlew build

# Chạy ứng dụng desktop
./gradlew :composeApp:run

# Chạy toàn bộ test
./gradlew allTests

# Chỉ kiểm tra định dạng và lint
./gradlew ktlintCheck
```

## Kịch bản kiểm chứng thủ công

### KB-1: Kết nối Jira (US1, SC-001)

1. Chạy `./gradlew :composeApp:run`.
2. Nhập site URL, email và API token hợp lệ -> nhấn "Kiểm tra kết nối".
3. **Mong đợi**: hiển thị trạng thái đã kết nối và chuyển sang màn hình truy vấn.
4. Nhập **token sai** -> **Mong đợi**: thông báo xác thực thất bại, phân biệt rõ với lỗi mạng; cấu hình sai **không** được lưu.
5. Nhập **host không tồn tại** -> **Mong đợi**: thông báo không kết nối được, khác với lỗi xác thực.
6. Khởi động lại ứng dụng -> **Mong đợi**: không phải nhập lại thông tin; token không xuất hiện dạng văn bản thuần trong file cấu hình.

### KB-2: Truy vấn JQL và bảng kết quả (US2, SC-002, SC-003)

1. Nhập JQL, ví dụ `project = ABC AND status != Done` -> nhấn "Tìm".
2. **Mong đợi**: bảng hiển thị các cột mặc định Mã / Tiêu đề / Trạng thái / Người xử lý / Độ ưu tiên / Cập nhật; mọi hàng đều khớp truy vấn.
3. Với truy vấn >100 kết quả: **Mong đợi** trang đầu ≤100 hàng, có nút "Tải thêm" và có thể cuộn tới ticket thứ 250 trong ≤3 lần điều hướng (SC-005).
4. **Mong đợi**: truy vấn ngắn hoàn tất trong <10 giây (SC-006); truy vấn trả 100 ticket trong <5 giây (SC-002), tính từ lúc nhấn "Tìm" tới khi bảng hiển thị.
5. Nhập JQL sai cú pháp (ví dụ `project =`) -> **Mong đợi**: thông báo lỗi JQL kèm chi tiết từ Jira, nội dung JQL vẫn còn trong ô nhập.
6. Nhập JQL không có kết quả -> **Mong đợi**: thông báo rõ "không có kết quả" (không phải bảng trống hay lỗi).
7. Trong lúc đang tải, nhận thấy chỉ báo đang tải; UI không bị treo (FR-010).
8. Kiểm tra mạng: ngắt mạng rồi tìm -> **Mong đợi**: lỗi mạng được phân biệt với 4 loại lỗi còn lại (SC-004).

### KB-3: Tùy biến cột (US3, SC-004 về phân biệt lỗi không liên quan)

1. Bật/tắt vài cột trong tùy chọn hiển thị.
2. **Mong đợi**: bảng cập nhật ngay; không có cột nào bị ẩn ngoài lựa chọn.
3. Khởi động lại ứng dụng -> **Mong đợi**: lựa chọn cột được giữ nguyên (FR-012).
4. Nhấn header cột để sắp xếp -> **Mong đợi**: chỉ báo chiều sắp xếp; thứ tự đúng trên **toàn bộ** kết quả (server-side ORDER BY), không chỉ trang hiện tại.

### KB-4: Mở ticket trên trình duyệt (US4)

1. Nhấp vào một dòng ticket.
2. **Mong đợi**: trình duyệt mặc định mở `{baseUrl}/browse/{KEY}` (FR-013).
3. **Mong đợi**: ticket không có assignee/priority vẫn hiển thị hàng với ô trống, không lỗi.

### KB-5: Khôi phục JQL (FR-014)

1. Chạy một truy vấn bất kỳ, đóng ứng dụng.
2. Mở lại -> **Mong đợi**: ô nhập JQL khôi phục truy vấn gần nhất, chưa tự động chạy lại.

## Kiểm chứng tự động

- Toàn bộ test chạy **không** gọi Jira thực (Nguyên tắc II) - dùng `MockEngine` của Ktor với fixture JSON.
- Lệnh: `./gradlew allTests`.
- Cổng chất lượng hiến pháp: `./gradlew build` (build desktop), `allTests`, `ktlintCheck` đều phải xanh.

## Kết quả kiểm chứng

**Cập nhật lần cuối**: 2026-10-30 (Phase 7 của giai đoạn implement)

### Tự động (đã chạy xong)

| Hạng mục | Lệnh | Kết quả |
|----------|------|---------|
| Cổng chất lượng hiến pháp | `./gradlew build allTests ktlintCheck` | BUILD SUCCESSFUL |
| Test tự động | `./gradlew allTests` | 138 test, 0 thất bại |

Không test nào gọi Jira thật: mọi lời gọi HTTP đi qua `MockEngine` của Ktor với fixture JSON (Nguyên tắc II).

### SC-005 - điều hướng tới ticket thứ 250 (đã kiểm chứng tự động)

Kiểm chứng bằng test `tiepCanTicketThu250ChiVoiHaiLanTaiThem` trong
`composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryEdgeCaseTest.kt`, mô phỏng 3 trang Jira
(100 + 100 + 50 ticket) qua `MockEngine`:

| Chỉ số | Đo được | Ngưỡng | Kết luận |
|--------|---------|--------|----------|
| Số lần nhấn "Tải thêm" để tới ticket thứ 250 | 2 | ≤ 3 | ĐẠT |
| Số ticket tích lũy trong bảng | 250 | 250 | ĐẠT |
| Số dòng trùng lặp | 0 | 0 | ĐẠT |
| Số lần gọi API tìm kiếm | 3 (1 ban đầu + 2 tải thêm) | không chạy lại truy vấn | ĐẠT |

Cơ chế: `PAGE_SIZE = 100` (`data/JiraApiClient.kt`), nút "Tải thêm" chỉ hiện khi `hasMore`
(`ui/query/TicketTable.kt`), và `QueryViewModel.loadMore()` nối trang bằng `nextPageToken` rồi loại trùng theo `key`.

### SC-006 và SC-002 - thời gian phản hồi

Phần xử lý phía ứng dụng đã được đo bằng một harness tạm (đã xóa sau khi đo, không nằm trong bộ test):

| Chỉ số | Đo được |
|--------|---------|
| Chuyển 100 ticket DTO -> domain (`TicketMapper.toPage`, gồm định dạng thời gian) | ~9,1 µs / trang |
| Toàn bộ chi phí phía ứng dụng cho một truy vấn 100 ticket (`QueryViewModel.run`, engine trong tiến trình) | ~1,33 ms |

Chi phí phía ứng dụng chiếm khoảng **0,03 %** ngân sách 5 giây của SC-002 và **0,01 %** ngân sách 10 giây
của SC-006. Phần thời gian còn lại phụ thuộc Jira Cloud và mạng nên **chưa đo được** nếu không có site thật.

- SC-002 (100 ticket < 5 giây): **chờ đo thủ công** ở KB-2 bước 4.
- SC-006 (truy vấn ngắn < 10 giây): **chờ đo thủ công** ở KB-2 bước 4.

### KB-1..KB-5 - kiểm chứng thủ công (T060)

Các kịch bản KB-1..KB-5 cần **site Jira Cloud thật, API token hợp lệ và một phiên desktop có màn hình**,
nên không thể chạy trong môi trường không giao diện. Trạng thái hiện tại: **chờ người dùng kiểm chứng**.

| Kịch bản | Kết quả mong đợi đã ghi nhận | Trạng thái |
|----------|------------------------------|------------|
| KB-1 Kết nối Jira (SC-001) | Kết nối thành công; phân biệt token sai với host sai và lỗi mạng; không lưu cấu hình sai; token không nằm dạng văn bản thuần trong file cấu hình | Chờ kiểm chứng |
| KB-2 Truy vấn JQL và bảng (SC-002, SC-003, SC-004, SC-006) | Bảng đúng 6 cột; trang đầu ≤ 100 hàng; "Tải thêm" tới được ticket 250; lỗi JQL, không kết quả và lỗi mạng phân biệt rõ; có chỉ báo đang tải | Chờ kiểm chứng (SC-005 đã kiểm chứng tự động ở trên) |
| KB-3 Tùy biến cột (FR-012) | Bảng cập nhật ngay; giữ nguyên lựa chọn sau khi khởi động lại; sắp xếp áp dụng cho toàn bộ kết quả | Chờ kiểm chứng |
| KB-4 Mở ticket trên trình duyệt (FR-013) | Trình duyệt mở `{baseUrl}/browse/{KEY}`; ticket thiếu assignee/priority vẫn hiển thị hàng | Chờ kiểm chứng |
| KB-5 Khôi phục JQL (FR-014) | Ô nhập JQL khôi phục truy vấn gần nhất, không tự chạy lại | Chờ kiểm chứng |

Hành vi nền tảng của KB-1..KB-5 đều đã có test tự động tương ứng - che token trong thông báo lỗi
(`SecretLeakTest.kt`), phân biệt 5 loại lỗi (`JiraErrorMapperTest.kt`), khôi phục JQL và tùy biến cột
(`QueryViewModelTest.kt`, `QueryViewModelPreferencesTest.kt`), mở trình duyệt (`TicketOpenerTest.kt`,
`BrowseUrlTest.kt`) - nhưng thao tác thực trên giao diện desktop vẫn cần con người xác nhận.

## Tham chiếu

- Hợp đồng API: [contracts/jira-cloud-api.md](./contracts/jira-cloud-api.md)
- Mô hình dữ liệu: [data-model.md](./data-model.md)
- Quyết định kỹ thuật: [research.md](./research.md)
- Đặc tả: [spec.md](./spec.md)
