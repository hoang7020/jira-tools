# Nghiên cứu kỹ thuật (Phase 0)

**Feature**: `004-jira-connection-settings` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này chốt các quyết định kỹ thuật cho việc đưa phần khai báo kết nối Jira vào màn hình Cài
đặt và bảo đảm token đã lưu được tái sử dụng ở những lần chạy sau.

## Tóm tắt quyết định

| # | Quyết định | Ảnh hưởng |
|---|-----------|----------|
| 1 | Cài đặt là **chế độ xem thứ ba** trong bộ chuyển sẵn có ("Truy vấn" / "Bảng đã lưu" / "Cài đặt") | `app/App.kt` |
| 2 | Thêm trạng thái **"có thông tin đã lưu nhưng chưa xác minh được"** vào `ConnectionStatus` | `domain/ConnectionStatus.kt` |
| 3 | Mở ứng dụng: đọc **tệp cấu hình trước** (hiển thị ngay), rồi **kiểm tra ngầm** đọc token + gọi Jira | `ConnectionRepository`, `ConnectionViewModel`, `App.kt` |
| 4 | Mất mạng khi kiểm tra ngầm → trạng thái "chưa xác minh được", **không** chặn truy vấn; token bị từ chối → trạng thái lỗi kèm hướng dẫn mở Cài đặt | `ConnectionRepository.verifySaved()` |
| 5 | Điều kiện hiển thị khu vực truy vấn đổi từ "đã kết nối" sang "**không phải chưa cấu hình**" | `app/App.kt` |
| 6 | `ConnectionDraft` chuyển từ `remember` trong `App.kt` vào `ConnectionViewModel` để **điền sẵn** địa chỉ Jira và email đã lưu | `ui/connection/ConnectionViewModel.kt` |
| 7 | Ngắt kết nối xoá **token + địa chỉ Jira + email**, giữ nguyên mọi tuỳ chọn khác và dữ liệu bảng đã lưu | `ConnectionRepository.disconnect()` |
| 8 | Xoá cơ chế thu gọn biểu mẫu của 002 (mã chết sau khi biểu mẫu rời màn hình chính) | `ConnectionArea.kt`, `ConnectionAreaMode.kt` |
| 9 | Câu chữ trạng thái là **hàm thuần** dùng chung cho dòng trạng thái và màn hình Cài đặt | `ui/connection/ConnectionStatusText.kt` |
| 10 | Không thêm phụ thuộc, không OAuth/SSO, không nhiều hồ sơ kết nối | Nguyên tắc IV, V |

## 1. Nơi đặt Cài đặt và cách điều hướng

**Quyết định**: thêm "Cài đặt" làm mục thứ ba trong bộ chuyển chế độ xem hiện có (ngay dưới dòng
trạng thái kết nối), và dòng trạng thái trên màn hình chính có nút "Cài đặt" chuyển thẳng tới đó.
Màn hình Cài đặt chiếm vùng nội dung còn lại, giống "Bảng đã lưu"; bộ chuyển và dòng trạng thái luôn
hiển thị ở cả ba chế độ nên yêu cầu FR-001 luôn thoả.

**Lý do**: người dùng đã chọn hình thức "màn hình riêng trong cửa sổ chính" (phiên làm việc
2026-10-31). Dùng lại bộ chuyển có sẵn giữ đúng bố cục, không phát sinh khái niệm điều hướng mới, và
trạng thái làm việc (kết quả truy vấn, bảng đã lưu đang mở) không bị mất khi chuyển qua lại (FR-015).

**Phương án đã cân nhắc**: hộp thoại nổi (che kết quả, phải đóng mới xem được); cửa sổ riêng (phải
quản lý vòng đời cửa sổ, thêm phức tạp không cần thiết); menu thanh bên (bố cục mới hoàn toàn).

## 2. Trạng thái kết nối và điều kiện hiển thị khu vực truy vấn

**Quyết định**: giữ bốn trạng thái hiện có và thêm một trạng thái:

```kotlin
sealed interface ConnectionStatus {
    data object Unconfigured                      // chưa đủ thông tin để gọi Jira
    data object Validating                        // đang kiểm tra (người dùng bấm hoặc kiểm tra ngầm)
    data class Unverified(baseUrl, email)         // MỚI: có thông tin đã lưu, chưa xác minh được
    data class Connected(baseUrl, email)          // đã xác minh thành công với Jira
    data class Invalid(error)                     // lần kiểm tra gần nhất thất bại kèm lý do
}
```

Điều kiện hiển thị khu vực truy vấn (và cho phép mở ticket, làm mới bảng đã lưu): trạng thái **khác**
`Unconfigured`, tức là ứng dụng đang có thông tin kết nối đã lưu để dùng.

**Lý do**: mất mạng lúc khởi động vẫn phải cho người dùng thử truy vấn (FR-022) — nếu bắt buộc
`Connected` thì người dùng bị chặn oan khi mạng chập chờn. `Unconfigured` là trạng thái duy nhất mang
nghĩa "không thể gọi Jira" (chưa có gì, thiếu token, hoặc vừa ngắt kết nối), nên nó là điều kiện chặn
duy nhất — dễ hiểu và dễ kiểm thử.

**Phương án đã cân nhắc**: thêm cờ `verified: Boolean` vào trạng thái hiện có (mọi nơi dùng trạng thái
phải xử lý thêm một trường, dễ sai); dùng `Invalid` cho cả lỗi mạng (vi phạm FR-022 vì sẽ chặn người
dùng); giữ điều kiện `Connected` (chặn oan khi chưa kiểm tra xong).

## 3. Thứ tự đọc dữ liệu khi khởi động và kiểm tra ngầm

**Quyết định**: một `LaunchedEffect` duy nhất trong `App.kt` gọi tuần tự:

1. `connectionViewModel.loadSaved()` — chỉ đọc `config.json` (nhanh, **không** hỏi kho bí mật):
   có địa chỉ Jira + email → `Unverified(baseUrl, email)`; thiếu → `Unconfigured`.
2. `connectionViewModel.verifySaved()` — đọc token từ kho bí mật rồi gọi `/myself`:
   - không có token → `Unconfigured` (giữ nguyên địa chỉ/email đã lưu);
   - Jira chấp nhận → `Connected`;
   - Jira từ chối (401/403) → `Invalid` kèm lý do;
   - lỗi mạng/timeout → giữ `Unverified`.

Vì `LaunchedEffect` chạy trong coroutine của giao diện, bước 1 hoàn tất trước và dòng trạng thái có
nội dung ngay; bước 2 chỉ cập nhật trạng thái khi xong, không chặn thao tác (FR-017, SC-009). Kiểm tra
ngầm **chỉ đọc**, không ghi gì xuống đĩa (không lưu lại token, không sửa cấu hình khi thất bại —
FR-013).

**Lý do**: `ConnectionRepository.loadSaved()` hiện có chủ ý không đọc kho bí mật để tránh hỏi quyền hệ
điều hành lúc khởi động (ghi chú trong mã nguồn 001). Giữ nguyên đặc tính đó cho bước hiển thị tức thì,
và chỉ chạm kho bí mật trong bước kiểm tra — đúng một lần cho mỗi lần mở ứng dụng (FR-017).

**Phương án đã cân nhắc**: đọc kho bí mật ngay trong `loadSaved()` (trạng thái chính xác hơn nhưng có
thể hiện hộp thoại xin quyền của hệ điều hành trước khi giao diện vẽ xong); kiểm tra ngầm bằng
`CoroutineScope` riêng trong ViewModel (trái quy ước hiện có: mọi ViewModel đều để tầng giao diện quản
lý phạm vi coroutine); kiểm tra định kỳ theo thời gian (YAGNI — không có yêu cầu).

## 4. Phân loại kết quả kiểm tra

**Quyết định**: hai nhánh rõ ràng, dùng lại `JiraError` đã có:

| Kết quả | Trạng thái | Câu chữ gợi ý | Truy vấn được? |
|--------|-----------|---------------|----------------|
| Jira chấp nhận | `Connected` | "Đã kết nối Jira." | Có |
| Lỗi mạng, không phân giải được tên miền, quá thời gian chờ | `Unverified` (giữ thông tin đã lưu) | "Chưa xác minh được kết nối Jira — kiểm tra mạng." | Có (FR-022) |
| Sai/hết hạn token, không đủ quyền | `Invalid` | Thông báo từ `JiraError.userMessage()` + gợi ý mở Cài đặt | Vẫn hiển thị khu vực truy vấn nhưng lời gọi sẽ lỗi y hệt (FR-012 yêu cầu thông báo rõ, không yêu cầu ẩn đi) |
| Không có token | `Unconfigured` | "Chưa cấu hình kết nối Jira." | Không |

**Lý do**: `JiraError` đã phân biệt sẵn nhóm lỗi mạng với nhóm lỗi xác thực, nên không cần thêm khái
niệm mới; việc ánh xạ chỉ là một hàm thuần nhỏ, dễ viết test trước.

**Phương án đã cân nhắc**: coi mọi lỗi là `Invalid` (vi phạm FR-022); thêm trạng thái riêng cho từng
nhóm lỗi mạng (dư thừa — câu chữ đã đủ phân biệt).

## 5. Chỉ báo token và điền sẵn thông tin đã lưu

**Quyết định**: `ConnectionRepository.savedInfo()` trả `SavedConnectionInfo(baseUrl, email, tokenSaved)`
— đọc `config.json` và **chỉ hỏi kho bí mật xem có token hay không** (không đọc giá trị token ra khỏi
tầng dữ liệu). `ConnectionViewModel` giữ `tokenSaved: Boolean?` (`null` = chưa biết) và `draft` để
điền sẵn địa chỉ Jira + email khi mở Cài đặt; ô token luôn rỗng với gợi ý "đã có token được lưu — nhập
để thay thế" (FR-008, FR-009).

**Lý do**: yêu cầu cấm hiển thị lại giá trị token, nhưng người dùng vẫn cần biết máy đã giữ token hay
chưa. Cờ ba trạng thái (`null`/`false`/`true`) cho phép giao diện nói "đang kiểm tra" thay vì khẳng
định sai trong lúc chờ kiểm tra ngầm.

**Phương án đã cân nhắc**: đọc token rồi che bớt (`abc...xyz`) — vẫn là tiết lộ một phần bí mật; hiển
thị đầy đủ — vi phạm FR-008; không có chỉ báo — người dùng không biết vì sao không phải nhập lại.

## 6. Ngắt kết nối

**Quyết định**: `ConnectionRepository.disconnect()` xoá token khỏi kho bí mật và xoá `baseUrl`/`email`
trong `config.json`, **giữ nguyên** các tuỳ chọn khác (`lastJql`, `visibleColumns`, `sort`,
`schemaVersion`) và **không** chạm dữ liệu bảng đã lưu. Giao diện hỏi xác nhận trước khi gọi. Trả về
`ConnectionStatus.Unconfigured` khi thành công; nếu kho bí mật không truy cập được thì ném lỗi để giao
diện thông báo và **không** xoá cấu hình (tránh trạng thái nửa vời: cấu hình mất nhưng token còn).

**Lý do**: `SecureStorage.deleteToken()` đã idempotent và fail-closed; thứ tự "xoá bí mật trước, xoá
cấu hình sau" bảo đảm không bao giờ còn token mồ côi mà người dùng tưởng đã xoá. Bảng đã lưu của 003
mang địa chỉ Jira riêng nên vẫn mở được sau khi ngắt kết nối (giả định đã ghi trong spec).

**Phương án đã cân nhắc**: chỉ xoá token mà giữ địa chỉ/email (người dùng vẫn thấy "đã cấu hình" nhưng
không dùng được — trái FR-011); xoá cả tuỳ chọn truy vấn (mất dữ liệu người dùng ngoài phạm vi yêu cầu).

## 7. Loại bỏ cơ chế thu gọn của 002

**Quyết định**: xoá `ui/connection/ConnectionArea.kt`, `ui/layout/ConnectionAreaMode.kt` và
`ConnectionAreaModeTest.kt`; gỡ `TITLE_BAR_ALLOWANCE` khỏi `AppWindowDefaults` (chỉ phục vụ cơ chế
này); giữ `MIN_SIZE`/`RESTORE_SIZE` vì `Main.kt` còn dùng. `ConnectionScreen.kt` được thay bằng
`ConnectionForm.kt` không còn nút thu gọn.

**Lý do**: sau khi biểu mẫu rời màn hình chính, cơ chế thu gọn không còn đối tượng để áp dụng; giữ lại
là mã chết (Nguyên tắc V). Đây là hệ quả đã được ghi nhận trong giả định của spec.

**Phương án đã cân nhắc**: giữ cơ chế thu gọn cho dòng trạng thái (dòng trạng thái đã cao đúng một
dòng, không cần thu gọn); để nguyên mã cũ không dùng (mã chết, gây nhầm lẫn khi đọc `App.kt`).

## 8. Chuyển draft nhập liệu vào ViewModel

**Quyết định**: `ConnectionDraft` (địa chỉ Jira, email, token, hiện/ẩn token) do
`ConnectionViewModel` giữ và phơi qua `StateFlow`; `App.kt` và màn hình Cài đặt chỉ đọc và phát sự kiện.

**Lý do**: cần điền sẵn thông tin đã lưu (FR-009) — việc này cần dữ liệu từ tầng dữ liệu, không nên
làm trong Compose; đồng thời giữ đúng Nguyên tắc III (trạng thái màn hình nằm trong ViewModel).

**Phương án đã cân nhắc**: giữ `remember` ở `App.kt` và truyền giá trị điền sẵn từ ngoài (phải đọc
tầng dữ liệu trong Compose hoặc thêm tham số rối); dùng lại draft cũ với giá trị mặc định rỗng (không
đạt FR-009).

## 9. Ảnh hưởng lên các tính năng đã có

**Quyết định**: điều kiện làm mới bảng đã lưu của 003 mở rộng từ "đã kết nối" sang "không phải chưa
cấu hình" (tức thêm trạng thái chưa xác minh được), vì khi mất mạng người dùng vẫn phải thử được
(FR-022) và lời gọi sẽ tự báo lỗi nếu thông tin thật sự sai. Không đổi `JiraApiClient`,
`JiraErrorMapper`, `AppConfigStore`, `SecureStorage`, `TicketOpener`, `TicketTable`, `QueryViewModel`,
hay hợp đồng dữ liệu bảng đã lưu.

**Lý do**: giữ đúng tinh thần FR-020 (không hồi quy) trong khi vẫn phục vụ FR-022; thay đổi chỉ là mở
rộng một điều kiện, và test hiện có ("từ chối khi chưa cấu hình") vẫn đúng.

**Phương án đã cân nhắc**: giữ nguyên "chỉ khi đã kết nối" (làm mới bị chặn oan khi chưa xác minh
xong); bỏ hẳn điều kiện (mất phản hồi rõ ràng khi chưa có thông tin kết nối).

## 10. Kiểm thử

**Quyết định**: viết test trước cho

- hàm thuần câu chữ trạng thái (`ConnectionStatusTextTest`): mọi trạng thái, kể cả trạng thái chưa xác
  minh được và trạng thái lỗi;
- `ConnectionRepository.verifySaved()`: chưa có cấu hình, thiếu token, thành công, lỗi mạng, token bị
  từ chối, và **không ghi gì** (so sánh nội dung cấu hình + danh sách token đã lưu trước/sau);
- `ConnectionRepository.disconnect()`: xoá token + địa chỉ/email, giữ tuỳ chọn khác, idempotent khi
  chưa có gì, và báo lỗi khi kho bí mật không truy cập được (không xoá cấu hình);
- `ConnectionViewModel`: điền sẵn draft, `tokenSaved` ba trạng thái, thông báo sau khi ngắt kết nối,
  và luồng `loadSaved()` → `verifySaved()` cập nhật trạng thái;
- `SavedTablesViewModel`: làm mới được chấp nhận khi trạng thái chưa xác minh, vẫn từ chối khi chưa
  cấu hình (ca cũ giữ nguyên).

Kho bí mật dùng `FakeSecureStorage` (đã có cờ lỗi cho từng thao tác), mạng dùng `MockEngine`, thời
gian/IO dùng `UnconfinedTestDispatcher`. **Không** viết test Compose; bố cục và tương tác kiểm chứng
thủ công bằng KB-1..KB-6 trong `quickstart.md`.

**Lý do**: Hiến pháp Nguyên tắc II bắt buộc với logic chia sẻ; ranh giới hệ điều hành (Keychain) chỉ
kiểm chứng được thật trên máy người dùng nên cần kịch bản thủ công.

## 11. Những gì cố ý KHÔNG làm

- Không hỗ trợ nhiều hồ sơ kết nối (nhiều tài khoản Jira) — ngoài yêu cầu.
- Không OAuth/SSO/PAT của Atlassian — yêu cầu chỉ nói về token hiện có.
- Không "ghi nhớ mật khẩu ở dạng mã hoá riêng" — kho bí mật hệ điều hành đã đáp ứng (FR-006).
- Không tự động kiểm tra lại định kỳ, không đếm ngược hạn token — YAGNI.
- Không thêm mục Cài đặt khác ngoài Kết nối Jira — đặt tên `ui/settings` để mở rộng sau.
- Không đổi định dạng `config.json` hiện có (chỉ bỏ/ghi hai khoá `baseUrl`, `email`).

## 12. Điểm chưa kiểm chứng

- Trải nghiệm thật của kho bí mật theo hệ điều hành (hộp thoại xin quyền của macOS Keychain, hành vi
  khi người dùng từ chối) — cần phiên desktop và người dùng thật.
- Thời gian kiểm tra ngầm trong điều kiện mạng thật (SC-009 chỉ kiểm chứng được là "không chặn giao diện").
- Cảm nhận về bố cục mới (một dòng trạng thái + ba chế độ xem) — kiểm chứng bằng KB-1/KB-6.

## Nguồn tham khảo

- Spec: [spec.md](./spec.md)
- Hiến pháp dự án: `.specify/memory/constitution.md`
- Hợp đồng kết nối của 001: `../001-jql-ticket-table/contracts/`
- Hợp đồng bảng đã lưu của 003: `../003-saved-result-table/contracts/saved-tables.md`
