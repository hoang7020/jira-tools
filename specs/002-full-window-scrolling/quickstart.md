# Hướng dẫn kiểm chứng nhanh (Quickstart)

**Feature**: `002-full-window-scrolling` | **Ngày**: 2026-10-31

Tài liệu này mô tả cách chạy ứng dụng và các kịch bản kiểm chứng thủ công để xác nhận tính năng hoạt
động đầu-cuối. Chi tiết triển khai nằm ở `tasks.md` và giai đoạn implement; quy tắc bố cục nằm ở
[data-model.md](./data-model.md) và [contracts/ui-contracts.md](./contracts/ui-contracts.md).

## Điều kiện tiên quyết

| Yêu cầu | Ghi chú |
|---------|---------|
| JDK 17+ | Bắt buộc cho toolchain Gradle |
| Gradle Wrapper | Dùng `./gradlew`, không cần cài Gradle toàn cục |
| Phiên desktop có màn hình | Kiểm chứng cửa sổ/cuộn cần môi trường có giao diện |
| Tài khoản Jira Cloud (tuỳ chọn) | Chỉ cần cho phần kiểm chứng dữ liệu thật; các kịch bản bố cục/cuộn có thể dùng dữ liệu bất kỳ |

## Lệnh thường dùng

```bash
# Chạy toàn bộ test (bao gồm test hai hàm thuần của tính năng này)
./gradlew allTests

# Chỉ chạy test của module ứng dụng
./gradlew :composeApp:jvmTest

# Kiểm tra định dạng và lint
./gradlew ktlintCheck

# Build toàn bộ (cổng chất lượng của hiến pháp)
./gradlew build

# Chạy ứng dụng desktop để kiểm chứng thủ công
./gradlew :composeApp:run
```

## Kiểm chứng tự động (bắt buộc trước khi kiểm chứng thủ công)

| Lệnh | Kỳ vọng |
|------|--------|
| `./gradlew :composeApp:jvmTest` | Toàn bộ test đạt, bao gồm `ConnectionAreaModeTest` và `TableColumnLayoutTest` mới |
| `./gradlew ktlintCheck` | Không có vi phạm định dạng |
| `./gradlew build` | Build thành công |

Các bất biến được kiểm tra tự động bởi test của hai hàm thuần:

- Ngưỡng thu gọn: `availableHeight >= collapseThreshold` -> `Expanded`; nhỏ hơn -> `Collapsed`; đúng
  bằng ngưỡng -> `Expanded` (biên).
- Ở chiều cao vùng nội dung của kích thước cửa sổ tối thiểu
  (`AppWindowDefaults.MIN_SIZE.height - TitleBarAllowance`) -> `Collapsed` (SC-002).
- `userChoice` luôn thắng quyết định tự động.
- `expandedFormMaxHeight` không đẩy bảng xuống dưới `minTableHeight` và không nhỏ hơn `collapsedBarHeight`.
- Bố cục cột: chỉ còn cột `Mã` -> không cần cuộn ngang; dư chỗ -> cột `Tiêu đề` co giãn lấp đầy và
  không cần cuộn ngang (FR-010); đủ sáu cột ở chiều rộng cửa sổ tối thiểu -> **cần** cuộn ngang (US3).

## Kịch bản kiểm chứng thủ công

### KB-1: Cửa sổ mở rộng tối đa và kích thước tối thiểu (US1, FR-001, FR-002, FR-012)

1. Chạy `./gradlew :composeApp:run`.
2. **Mong đợi**: cửa sổ mở ra ở trạng thái mở rộng tối đa, chiếm toàn bộ vùng màn hình khả dụng (trừ
   thanh hệ thống), vẫn có thanh tiêu đề và các nút thu nhỏ / khôi phục / đóng chuẩn của hệ điều hành.
3. Bấm nút khôi phục kích thước thường của hệ điều hành.
4. **Mong đợi**: cửa sổ trở về kích thước nhỏ hơn (khoảng 1280 x 800 điểm ảnh logic trở xuống), nội
   dung vẫn hiển thị đúng và dùng được.
5. Kéo góc cửa sổ để thu nhỏ hết mức.
6. **Mong đợi**: cửa sổ không thu nhỏ được dưới 1024 x 720 điểm ảnh logic; ở kích thước đó ô nhập JQL,
   nút chạy truy vấn và bảng kết quả đều hiển thị, không chồng lấn.
7. Thay đổi kích thước cửa sổ trong khi bảng đang có dữ liệu -> **Mong đợi**: bố cục lại theo kích
   thước mới, 100% ticket đang hiển thị vẫn còn, lựa chọn cột và thứ tự sắp xếp không đổi, không phát
   sinh truy vấn mới tới Jira (SC-005).

### KB-2: Biểu mẫu kết nối thu gọn ở cửa sổ thấp (US2, FR-004, FR-007, SC-002)

1. Chạy ứng dụng, thu nhỏ chiều cao cửa sổ xuống mức tối thiểu (720 điểm ảnh logic).
2. **Mong đợi**: biểu mẫu kết nối thu gọn thành một thanh một dòng hiển thị trạng thái kết nối hiện
   tại (chưa kết nối / đã kết nối tới đâu) kèm nút mở rộng; ô nhập JQL và nút chạy truy vấn vẫn hiển
   thị đầy đủ; bảng vẫn có vùng hiển thị dùng được và cuộn được.
3. Bấm nút mở rộng trên thanh thu gọn, nhập thử vài ký tự vào các trường, kiểm tra kết nối.
4. **Mong đợi**: biểu mẫu mở rộng, mọi trường và nút bên trong vẫn tiếp cận được (nếu cửa sổ quá thấp,
   vùng biểu mẫu tự cuộn bên trong); dữ liệu đã nhập và kết quả kiểm tra kết nối gần nhất không bị mất.
5. Thu nhỏ cửa sổ (nếu cần) rồi bấm nút thu gọn.
6. **Mong đợi**: biểu mẫu thu về thanh một dòng, dữ liệu đã nhập vẫn còn khi mở rộng lại; chạy truy
   vấn được ngay từ trạng thái thu gọn mà không cần mở rộng biểu mẫu.
7. Mở rộng cửa sổ lên chiều cao thoải mái (ví dụ 900 điểm ảnh logic trở lên) -> **Mong đợi**: biểu mẫu
   hiển thị đầy đủ trở lại (không thu gọn vô ích).

### KB-3: Cuộn dọc trong bảng, vùng điều khiển cố định (US2, FR-004, FR-005, FR-006, FR-013)

1. Chạy một truy vấn trả về danh sách dài (hoặc cuộn sau khi "Tải thêm" nhiều trang để có ~250 ticket).
2. Cuộn bằng con lăn chuột, sau đó bằng bàn di chuột, sau đó bằng phím điều hướng (mũi tên, Page
   Down/Up, Home/End).
3. **Mong đợi**: danh sách ticket dịch chuyển theo chiều dọc; biểu mẫu kết nối, ô nhập JQL và nút chạy
   truy vấn giữ nguyên vị trí; hàng tiêu đề cột vẫn hiển thị và thẳng hàng với dữ liệu.
4. **Mong đợi**: thanh cuộn dọc phản ánh đúng vị trí cuộn và kéo được; khi danh sách ngắn hơn khung
   nhìn thì không xuất hiện thanh cuộn dọc.
5. Cuộn trong khi truy vấn đang chạy / đang tải thêm -> **Mong đợi**: thao tác cuộn không hủy truy vấn,
   trạng thái đang tải (Loading / nút "Tải thêm" + vòng xoay) vẫn hiển thị đúng.
6. Chạy truy vấn mới khi đang cuộn ở giữa danh sách -> **Mong đợi**: trang kết quả mới hiển thị từ đầu
   danh sách; bấm "Tải thêm" thì vị trí cuộn được giữ nguyên.
7. Quan sát khi danh sách ~250 ticket: thao tác cuộn phản hồi tức thời, không giật (SC-004).

### KB-4: Cuộn ngang và cột Mã cố định (US3, FR-008, FR-009, FR-010, FR-011, SC-003, SC-006)

1. Bật đủ sáu cột trong menu "Cột hiển thị", thu hẹp cửa sổ về gần kích thước tối thiểu.
2. **Mong đợi**: xuất hiện thanh cuộn ngang ở đáy vùng bảng; cột `Mã` vẫn đứng yên bên trái.
3. Cuộn ngang bằng cách kéo thanh cuộn, bằng Shift + con lăn và bằng thao tác ngang của bàn di chuột.
4. **Mong đợi**: các cột bị khuất xuất hiện, mọi cột đều xem được trong tối đa hai thao tác cuộn ngang
   (SC-003); khi tới cột ngoài cùng bên phải, cột `Mã` vẫn nguyên vị trí và mỗi hàng vẫn nhận biết được
   là ticket nào (SC-008).
5. **Mong đợi**: khi cuộn ngang, tiêu đề cột và ô dữ liệu luôn thẳng hàng; nội dung không tràn sang cột
   `Mã`.
6. Tắt bớt cột cho tới khi các cột còn lại vừa với chiều rộng cửa sổ -> **Mong đợi**: bảng hiển thị gọn
   trong khung nhìn, thanh cuộn ngang biến mất, cột `Tiêu đề` lấp đầy phần còn lại.
7. Nhờ một người dùng lần đầu quan sát bảng đang phải cuộn ngang -> **Mong đợi**: người dùng nhận ra có
   thể cuộn ngang để xem cột bị khuất trong vòng 10 giây mà không cần hướng dẫn (SC-006).

### KB-5: Không hồi quy các tính năng hiện có (FR-015, SC-007)

1. Kết nối Jira (site URL, email, API token hợp lệ) -> **Mong đợi**: kết nối thành công như trước.
2. Chạy truy vấn JQL; sắp xếp theo cột (bấm lại cùng cột để đảo chiều); ẩn/hiện cột; mở ticket trong
   trình duyệt bằng cách bấm vào hàng; tải thêm kết quả.
3. **Mong đợi**: tất cả hành vi giữ nguyên như trước tính năng này.
4. Khởi động lại ứng dụng -> **Mong đợi**: truy vấn gần nhất, lựa chọn cột và chiều sắp xếp vẫn được
   khôi phục; cửa sổ vẫn mở ở trạng thái mở rộng tối đa.
5. Nhập JQL sai cú pháp; chạy truy vấn không có kết quả; ngắt mạng rồi chạy truy vấn.
6. **Mong đợi**: thông báo lỗi phân loại đúng (JQL sai / không có kết quả / lỗi mạng), nút "Thử lại"
   hoạt động, nội dung JQL vẫn còn trong ô nhập.

### KB-6: Trường hợp biên về kích thước và vùng hiển thị

1. Di chuyển cửa sổ sang màn hình phụ (nếu có), sau đó ngắt kết nối màn hình phụ đó.
2. **Mong đợi**: cửa sổ vẫn nằm trong vùng hiển thị của màn hình còn lại, nội dung vẫn dùng được.
3. Nhập truy vấn JQL dài hơn chiều rộng ô nhập -> **Mong đợi**: nội dung ô nhập tự cuộn bên trong,
   không đẩy các thành phần khác ra ngoài tầm nhìn.
4. Thay đổi kích thước cửa sổ trong khi dữ liệu đang tải -> **Mong đợi**: nội dung cập nhật đúng, không
   chồng lấn, không xuất hiện khoảng trắng bất thường.

## Kết quả kiểm chứng

Cập nhật ngày 2026-10-31 (giai đoạn implement).

### Tự động (đã chạy)

| Hạng mục | Trạng thái | Bằng chứng |
|---------|-----------|-----------|
| `ConnectionAreaModeTest` | ✓ ĐẠT (9/9) | `./gradlew :composeApp:jvmTest`; viết trước theo Nguyên tắc II — lần chạy đầu ĐỎ (thiếu `resolveConnectionAreaMode`) |
| `TableColumnLayoutTest` | ✓ ĐẠT (8/8) | `./gradlew :composeApp:jvmTest`; viết trước — lần chạy đầu ĐỎ (thiếu `computeTableColumnLayout`) |
| Bộ test hiện có (SC-007) | ✓ ĐẠT | 138 test cũ + 17 test mới = 155 test, 0 lỗi (`composeApp/build/test-results/jvmTest/`) |
| `ktlintCheck` | ✓ SẠCH | `./gradlew ktlintCheck` (cả `commonMain`, `commonTest`, `jvmMain`) |
| `build` | ✓ THÀNH CÔNG | `./gradlew build ktlintCheck` -> BUILD SUCCESSFUL |

Hằng số thiết kế **không phải hiệu chỉnh**: `320.dp`/`190.dp`/`240.dp`/`48.dp` trong
`ConnectionAreaMode.kt` và bề rộng cột `110/130/150/120/130` + tối thiểu `400` cho cột Tiêu đề trong
`TableColumnLayout.kt` giữ đúng giá trị đã chốt ở `plan.md`. Các giá trị này được bảo vệ bằng test bất
biến (SC-002, FR-010, FR-011) và cần xác nhận bằng mắt ở KB-2 và KB-4.

### Thủ công (chờ người dùng kiểm chứng)

| Kịch bản | Trạng thái | Ghi chú |
|---------|-----------|--------|
| KB-1 | chờ người dùng kiểm chứng | Cần phiên desktop có màn hình: cửa sổ mở rộng tối đa, khôi phục 1280 x 800, chặn thu nhỏ dưới 1024 x 720 |
| KB-2 | chờ người dùng kiểm chứng | Cần phiên desktop: thu gọn/mở rộng biểu mẫu ở cửa sổ thấp, dữ liệu đang nhập không mất |
| KB-3 | chờ người dùng kiểm chứng | Cần danh sách dài (site Jira thật hoặc "Tải thêm" nhiều trang) |
| KB-4 | chờ người dùng kiểm chứng | Cần phiên desktop: cuộn ngang, cột Mã cố định, tiêu đề thẳng hàng |
| KB-5 | chờ người dùng kiểm chứng | Cần site Jira Cloud thật và API token hợp lệ |
| KB-6 | chờ người dùng kiểm chứng | Cần màn hình phụ (bước 1–2) và phiên desktop |
| SC-004 (độ mượt với ~250 ticket) | chờ người dùng kiểm chứng | Cần danh sách dài thật |
| SC-006 (khả năng khám phá cuộn ngang) | chờ người dùng kiểm chứng | Cần tối thiểu 5 người dùng lần đầu |

### Đối chiếu yêu cầu (T024)

**Đã có bằng chứng tự động**:

| Yêu cầu | Bằng chứng |
|--------|-----------|
| FR-007 (thu gọn/mở rộng biểu mẫu, ghi đè của người dùng) | `ConnectionAreaModeTest` (ngưỡng, biên, `userChoice` thắng) |
| FR-012 (kích thước tối thiểu, bảng giữ chỗ dùng được) | `ConnectionAreaModeTest` (`collapsedAtMinimumWindowContentHeight`, `expandedFormLeavesQueryBarAndUsableTableIntact`) và `TableColumnLayoutTest` (khung nhìn trừ cột Mã và làn thanh cuộn) |
| FR-010, FR-011 (cột vừa khung nhìn, cột Mã cố định) | `TableColumnLayoutTest` (khung nhìn/nội dung, bề rộng Mã, cột Tiêu đề co giãn) |
| FR-008 (nhu cầu cuộn ngang khi thiếu chỗ) | `TableColumnLayoutTest` (`allColumnsNeedHorizontalScrollAtMinimumWindowWidth`) |
| SC-002 (biểu mẫu thu gọn ở kích thước cửa sổ tối thiểu) | `ConnectionAreaModeTest` (`collapsedAtMinimumWindowContentHeight`) |
| SC-003 (tới được cột ngoài cùng bên phải) | `TableColumnLayoutTest` xác nhận nội dung vượt khung nhìn 60 dp ở cửa sổ tối thiểu — điều kiện của SC-003; thao tác cuộn thực tế cần KB-4 |
| SC-007 (không hồi quy) | 138 test cũ vẫn đạt trong tổng số 155 test |

**Cần người dùng kiểm chứng** (không thể xác minh trong môi trường không giao diện/không có site Jira thật):

| Yêu cầu | Kịch bản |
|--------|---------|
| FR-001, FR-002 | KB-1 |
| FR-003 | KB-1 (bước 7) |
| FR-004, FR-005, FR-006 | KB-3 |
| FR-009 (thẳng hàng khi cuộn ngang) | KB-4 |
| FR-013, FR-014 | KB-3 |
| FR-015 | KB-5 |
| SC-001, SC-005 | KB-1 |
| SC-004 | KB-3 (bước 7) |
| SC-006, SC-008 | KB-4 |

Không có FR/SC nào bị bỏ sót: mọi mục ở bảng trên đều có kịch bản tương ứng trong
`quickstart.md` hoặc test tự động, chỉ khác nhau ở việc ai (agent hay người dùng) chạy.


## Tham chiếu

- Yêu cầu: [spec.md](./spec.md)
- Kế hoạch: [plan.md](./plan.md)
- Nghiên cứu kỹ thuật: [research.md](./research.md)
- Mô hình dữ liệu và quy tắc bố cục: [data-model.md](./data-model.md)
- Hợp đồng giao diện: [contracts/ui-contracts.md](./contracts/ui-contracts.md)
- Kịch bản kiểm chứng của tính năng trước: `../001-jql-ticket-table/quickstart.md`
