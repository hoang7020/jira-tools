# Kiểm chứng nhanh (Phase 1)

**Feature**: `004-jira-connection-settings` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này là hướng dẫn kiểm chứng: phần tự động chạy được trong môi trường không giao diện, phần
thủ công (KB-1..KB-6) cần phiên desktop và một tài khoản Jira thật. Không có bước nào yêu cầu đọc mã
nguồn để hiểu cách chạy.

## 0. Chuẩn bị

- JVM 17 trở lên; chạy lệnh tại gốc repo.
- Một tài khoản Jira Cloud với API token hợp lệ (tạo tại trang quản lý tài khoản Atlassian).
- Vị trí dữ liệu trên macOS: `~/Library/Application Support/jira-tools/config.json`,
  `~/Library/Application Support/jira-tools/saved-tables.json`, token trong Keychain (mục `jira-tools`).
- Để kiểm chứng "người dùng mới", xoá thông tin đã lưu: xoá hai tệp JSON ở trên và xoá mục
  `jira-tools` trong Keychain (macOS: ứng dụng Keychain Access), hoặc dùng thao tác "Ngắt kết nối".

### Lệnh tự động (dùng xuyên suốt)

| Mục đích | Lệnh |
|----------|------|
| Kiểm thử hàm thuần câu chữ trạng thái | `./gradlew :composeApp:jvmTest --tests "com.jiratools.ui.connection.ConnectionStatusTextTest"` |
| Kiểm thử tầng dữ liệu kết nối | `./gradlew :composeApp:jvmTest --tests "com.jiratools.data.ConnectionRepositoryTest"` |
| Kiểm thử trạng thái màn hình kết nối | `./gradlew :composeApp:jvmTest --tests "com.jiratools.ui.connection.ConnectionViewModelTest"` |
| Kiểm thử không hồi quy bảng đã lưu | `./gradlew :composeApp:jvmTest --tests "com.jiratools.ui.saved.SavedTablesViewModelTest"` |
| Toàn bộ kiểm thử + lint | `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` |
| Cổng chất lượng đầy đủ | `./gradlew build ktlintCheck` |
| Chạy ứng dụng | `./gradlew :composeApp:run` |

---

## KB-1 — Người dùng mới cấu hình kết nối trong Cài đặt (US1, FR-001..FR-004, FR-016, SC-001)

1. Xoá thông tin kết nối đã lưu (mục 0), rồi chạy `./gradlew :composeApp:run`.
2. Màn hình chính hiển thị **một dòng** "Chưa cấu hình kết nối Jira." với nút "Cài đặt"; **không** có
   ô nhập địa chỉ Jira, email hay API token ở bất kỳ đâu trên màn hình chính.
3. Bấm "Cài đặt" → chuyển sang màn hình Cài đặt (bộ chuyển có ba mục "Truy vấn" / "Bảng đã lưu" / "Cài đặt").
4. Nhập địa chỉ Jira, email, API token hợp lệ → bấm "Kiểm tra kết nối".
5. Kiểm chứng: thấy tiến trình khi đang kiểm tra, sau đó thông báo "Kết nối thành công tới <địa chỉ>
   bằng tài khoản <email>." và chỉ báo "Đã có token được lưu trên máy."
6. Quay lại "Truy vấn": dòng trạng thái hiển thị "Đã kết nối Jira." (**không** hiển thị địa chỉ hay email),
   và khu vực nhập JQL sẵn sàng dùng.
7. Thử nhập sai (địa chỉ không tồn tại, hoặc token sai): thông báo lỗi thân thiện ngay trong Cài đặt,
   dòng trạng thái trên màn hình chính **không** đổi sang "đã kết nối".

## KB-2 — Tái sử dụng token sau khi khởi động lại (US2, FR-005..FR-009, SC-002, SC-003)

1. Sau KB-1 (đã kết nối thành công), đóng ứng dụng hoàn toàn.
2. Mở tệp `config.json` và xác nhận **không** có chuỗi token (chỉ có địa chỉ Jira và email).
3. Chạy lại `./gradlew :composeApp:run`:
   - dòng trạng thái có nội dung ngay khi mở (không chờ mạng), sau đó chuyển thành "Đã kết nối Jira."
     khi kiểm tra ngầm xong — không cần người dùng thao tác gì;
   - mở Cài đặt: địa chỉ Jira và email đã điền sẵn, ô API token **rỗng**, chỉ báo "Đã có token được
     lưu trên máy.";
   - chạy một truy vấn JQL **mà không mở Cài đặt lần nào**: kết quả trả về bình thường (không phải
     nhập lại token).
4. Kiểm chứng thêm: mở Keychain Access, mục `jira-tools` vẫn có mật khẩu đã lưu.

## KB-3 — Kiểm tra ngầm khi mất mạng và khi token bị thu hồi (US2, FR-017, FR-022, FR-012, SC-007, SC-009)

1. **Mất mạng**: ngắt mạng của máy (tắt Wi-Fi), rồi mở ứng dụng khi đã có thông tin kết nối đã lưu.
   - Dòng trạng thái hiển thị trạng thái từ thông tin đã lưu ngay, sau đó chuyển thành "Chưa xác minh
     được kết nối Jira — kiểm tra mạng." (**không** phải thông báo sai token).
   - Khu vực truy vấn vẫn hiển thị; thử chạy truy vấn → lỗi mạng thân thiện, ứng dụng không sập.
2. **Token bị thu hồi**: vào trang Atlassian, thu hồi API token đang dùng, rồi mở lại ứng dụng (có mạng).
   - Sau ít giây, dòng trạng thái báo kết nối bị từ chối kèm lời nhắc mở Cài đặt (không phải lỗi chung chung).
   - Địa chỉ Jira và email **vẫn được điền sẵn** trong Cài đặt; chỉ cần dán token mới là dùng lại được.

## KB-4 — Thay token đã lưu (US2, FR-007, FR-009, FR-013)

1. Với kết nối đang hoạt động, mở Cài đặt → dán một token mới hợp lệ vào ô API token → "Kiểm tra kết nối".
2. Kiểm chứng: báo kết nối thành công; chỉ báo vẫn là "Đã có token được lưu trên máy."; đóng và mở lại
   ứng dụng, chạy truy vấn được với token mới; token cũ không còn dùng được (nếu đã thu hồi ở Atlassian).
3. Thử nhập token sai khi đã có token tốt đang lưu: báo lỗi trong Cài đặt, và truy vấn bằng token cũ
   **vẫn hoạt động** (thông tin đã lưu không bị thay thế).

## KB-5 — Ngắt kết nối (US3, FR-010, FR-011, SC-006)

1. Với kết nối đang hoạt động, mở Cài đặt → bấm "Ngắt kết nối".
2. Kiểm chứng: có hộp thoại xác nhận nêu rõ thông tin sẽ bị xoá; bấm "Huỷ" thì không có gì thay đổi và
   truy vấn vẫn chạy được.
3. Bấm lại "Ngắt kết nối" → xác nhận:
   - dòng trạng thái trở về "Chưa cấu hình kết nối Jira.";
   - các ô nhập trong Cài đặt trống, chỉ báo "Chưa có token được lưu.";
   - `config.json` không còn `baseUrl`/`email` nhưng **vẫn giữ** truy vấn JQL gần nhất và lựa chọn cột;
   - Keychain Access không còn mật khẩu cho mục `jira-tools`;
   - chạy truy vấn hoặc làm mới bảng đã lưu: ứng dụng **không** gọi Jira và hướng dẫn mở Cài đặt.
4. Mở một bảng đã lưu (tính năng 003): vẫn xem được dữ liệu đã lưu bình thường.

## KB-6 — Không hồi quy và lối vào Cài đặt ở mọi chế độ xem (FR-001, FR-015, FR-020, SC-004, SC-005)

1. Với kết nối hoạt động, chạy một truy vấn JQL, tải thêm kết quả, sắp xếp theo cột, ẩn/hiện cột, mở
   một ticket — tất cả như trước.
2. Chuyển sang "Bảng đã lưu": xem/sắp xếp/làm mới một bảng đã lưu như trước.
3. Ở **cả hai** chế độ, dòng trạng thái và nút "Cài đặt" luôn hiển thị; bấm "Cài đặt" rồi quay lại:
   kết quả truy vấn và bảng đã lưu đang mở vẫn còn nguyên.
4. Kiểm chứng bố cục: màn hình chính không còn biểu mẫu kết nối; diện tích cho kết quả truy vấn tăng
   (không còn cơ chế thu gọn biểu mẫu).
5. Chạy lại các kịch bản chính của `../../001-jql-ticket-table/quickstart.md` để xác nhận không hồi quy.

## Ghi kết quả

### Kiểm tra tự động

| Kiểm tra | Kết quả | Ghi chú |
|----------|---------|---------|
| Baseline `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` | PASS | 212 test, 0 lỗi; ktlint thành công |
| Phụ thuộc mới | Không cần | Tận dụng thư viện và ranh giới lưu trữ hiện có |
| Foundational `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` | PASS | 221 test, 0 lỗi; ktlint thành công |
| Kiểm thử tập trung `ConnectionRepositoryTest`, `ConnectionViewModelTest`, `ConnectionStatusTextTest`, `SavedTablesViewModelTest` + `ktlintCheck` | PASS | Các ca verify/disconnect, lỗi mạng/kho bí mật, token mồ côi và trạng thái giao diện đều đạt |
| `./gradlew build ktlintCheck` | PASS | Build Desktop/JVM, 234 test, 0 lỗi; ktlint thành công |
| `./gradlew build ktlintCheck --rerun-tasks` | PASS | Chạy lại toàn bộ build/test/lint; 234 test, 0 lỗi; ktlint thành công |
| Rà soát mã nguồn/hiến pháp | PASS | `commonMain` không import `java.*`; không có chuỗi token thử nghiệm trong các tệp trạng thái/kết nối; không thêm phụ thuộc hay sửa các tệp được bảo vệ. Workspace không có Git metadata nên không có `git diff`; đã rà mã nguồn trực tiếp. |

### Ghi chú triển khai so với thiết kế

- `verifySavedWithInfo()` dùng cùng snapshot nội bộ với `savedInfo()` để chỉ đọc kho bí mật một lần,
  đồng thời trả trạng thái token (không trả giá trị token) chính xác cho ViewModel.
- `hasSavedConnection` tách biệt với trạng thái xác thực: Cài đặt vẫn cho phép ngắt kết nối khi cấu hình
  còn địa chỉ/email nhưng thiếu token, hoặc khi chỉ còn token mồ côi.
- `App` nạp trạng thái đã lưu trước rồi khởi chạy xác minh trong coroutine con; việc nạp truy vấn và
  bảng đã lưu cục bộ không phải chờ Jira.
- Dòng trạng thái lỗi dùng lý do ngắn đã chuẩn hoá, không in địa chỉ, email hoặc chi tiết lỗi thô.

### Kịch bản thủ công

Các kịch bản này chưa được chạy trong phiên triển khai vì cần cửa sổ Desktop, mạng và tài khoản Jira
thật. Giữ nguyên trạng thái chờ để người dùng kiểm chứng:

| Kịch bản | Kết quả | Ghi chú |
|----------|---------|---------|
| KB-1 Cấu hình trong Cài đặt | Chưa kiểm chứng | Cần Desktop + tài khoản Jira thật |
| KB-2 Tái sử dụng token sau khi khởi động lại | Chưa kiểm chứng | Cần Desktop + Keychain + tài khoản Jira thật |
| KB-3 Mất mạng / token bị thu hồi | Chưa kiểm chứng | Cần Desktop, mạng và tài khoản Jira thật |
| KB-4 Thay token | Chưa kiểm chứng | Cần Desktop + tài khoản Jira thật |
| KB-5 Ngắt kết nối | Chưa kiểm chứng | Cần Desktop + Keychain + tài khoản Jira thật |
| KB-6 Không hồi quy + lối vào Cài đặt | Chưa kiểm chứng | Cần Desktop + tài khoản Jira thật |

### Truy vết yêu cầu chức năng

| Yêu cầu | Mã / kiểm thử liên quan | Kiểm chứng |
|---------|------------------------|------------|
| FR-001 | [App.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt), [SettingsScreen.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt) | Tự động build; KB-1/KB-6 chờ thủ công |
| FR-002 | [App.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt), [SettingsScreen.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt) | Bố cục mới đã compile; KB-1/KB-6 chờ thủ công |
| FR-003 | [ConnectionStatus.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt), [ConnectionStatusText.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionStatusText.kt), [ConnectionStatusTextTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionStatusTextTest.kt) | Test câu chữ đạt |
| FR-004 | [ConnectionForm.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionForm.kt), [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Test thành công/thất bại đạt; KB-1/KB-4 chờ thủ công |
| FR-005 | [ConnectionRepository.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt), [AppContainer.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt) | Test lưu token sau khi Jira xác nhận đạt; KB-2 chờ thủ công |
| FR-006 | [SavedConnectionInfo.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedConnectionInfo.kt), [SecretLeakTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/SecretLeakTest.kt) | Test rò rỉ bí mật và full suite đạt; kiểm tra tệp/Keychain thật chờ KB-2/KB-5 |
| FR-007 | [AppContainer.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt), [ConnectionRepository.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt) | Credential provider đọc thông tin đã lưu cho từng lời gọi Jira; KB-2 kiểm chứng sau khởi động lại còn chờ thủ công |
| FR-008 | [ConnectionForm.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionForm.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Test trạng thái có/không token đạt; KB-2 chờ thủ công |
| FR-009 | [ConnectionViewModel.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Test nạp và điền sẵn địa chỉ/email đạt; KB-1/KB-2 chờ thủ công |
| FR-010 | [SettingsScreen.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt), [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt) | Test xoá token và giữ tuỳ chọn đạt; hộp thoại thật chờ KB-5 |
| FR-011 | [QueryViewModel.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt), [SavedTablesViewModel.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt), [SavedTablesViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt) | Test từ chối làm mới khi chưa có kết nối đạt; truy vấn/tải thêm được kiểm soát qua credential provider; KB-5 chờ thủ công |
| FR-012 | [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt), [ConnectionStatusTextTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionStatusTextTest.kt) | 401/403 và lý do ngắn + Cài đặt đạt; thu hồi token thật chờ KB-3 |
| FR-013 | [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Cấu hình/token không đổi khi kiểm tra thất bại; test đạt |
| FR-014 | [ConnectionViewModel.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Cập nhật trạng thái trong phiên được kiểm thử |
| FR-015 | [App.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt) giữ ViewModel ở cấp ứng dụng | Chuyển màn hình không huỷ state theo cấu trúc; KB-6 chờ thủ công |
| FR-016 | [ConnectionRepository.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt), [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt) | Chỉ lưu sau `/myself` thành công; test đạt |
| FR-017 | [App.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt) | Thứ tự nạp rồi xác minh nền và trạng thái hoàn tất được kiểm thử; KB-2/KB-3 chờ thủ công |
| FR-018 | [App.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt), [SettingsScreen.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt) | Settings nằm trong bộ chuyển chế độ của cửa sổ chính; KB-1/KB-6 chờ thủ công |
| FR-019 | [ConnectionStatusText.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionStatusText.kt), [ConnectionStatusTextTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionStatusTextTest.kt) | Test xác nhận không lộ URL/email/token và có lý do lỗi đạt |
| FR-020 | [SavedTablesViewModel.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt), [SavedTablesViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt) | 234 test và build đạt; kịch bản Desktop KB-6 chờ thủ công |
| FR-021 | [AppContainer.kt](../../composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt), Jira credential provider hiện có | Dùng thông tin đã lưu cho lời gọi Jira; KB-2 chờ thủ công |
| FR-022 | [ConnectionRepositoryTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt), [ConnectionViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt), [SavedTablesViewModelTest.kt](../../composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt) | Mất mạng giữ `Unverified` và không chặn thử truy vấn/làm mới; test đạt, KB-3 chờ thủ công |

### Truy vết tiêu chí thành công

| Tiêu chí | Bằng chứng | Trạng thái |
|---------|-----------|-----------|
| SC-001 | KB-1 đo thời gian cấu hình trên Desktop | Chưa kiểm chứng thủ công |
| SC-002 | Test lưu token thành công; KB-2 kiểm tra chạy truy vấn sau khi khởi động lại | Lưu token tự động đạt; tái sử dụng sau khởi động lại chờ KB-2 |
| SC-003 | `SecretLeakTest`, test không ghi token vào config và kiểm tra status text | Tự động đạt; kiểm tra Keychain thật chờ KB-2/KB-5 |
| SC-004 | `App.kt` bỏ biểu mẫu khỏi màn hình chính; KB-6 đo bố cục | Mã đã build; đo UI chờ thủ công |
| SC-005 | `./gradlew build ktlintCheck` và lệnh rerun | Đạt: 234 test, 0 lỗi; ktlint sạch |
| SC-006 | Test disconnect xác nhận token bị xoá và state `Unconfigured`; KB-5 kiểm tra Keychain | Tự động đạt; Keychain thật chờ thủ công |
| SC-007 | Test 401 và câu chữ lỗi yêu cầu mở Cài đặt; KB-3 thu hồi token thật | Tự động đạt; KB-3 chờ thủ công |
| SC-008 | KB-6 đo thời gian mở Cài đặt | Chưa kiểm chứng thủ công |
| SC-009 | Test trạng thái `Validating`/kết quả và mã startup coroutine nền; KB-2/KB-3 kiểm tra UI | Tự động đạt; phản hồi UI thật chờ thủ công |

## Tham chiếu

- Yêu cầu: [spec.md](./spec.md)
- Kế hoạch: [plan.md](./plan.md)
- Nghiên cứu kỹ thuật: [research.md](./research.md)
- Mô hình dữ liệu và trạng thái: [data-model.md](./data-model.md)
- Hợp đồng dữ liệu/ViewModel/giao diện: [contracts/connection-settings.md](./contracts/connection-settings.md)
- Kịch bản kiểm chứng của tính năng trước: `../001-jql-ticket-table/quickstart.md`,
  `../002-full-window-scrolling/quickstart.md`, `../003-saved-result-table/quickstart.md`
