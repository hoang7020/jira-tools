# Hợp đồng Cài đặt kết nối Jira

**Feature**: `004-jira-connection-settings` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](../plan.md)

Tài liệu này chốt bốn hợp đồng của tính năng: (1) dữ liệu lưu trên máy và bảng chân trị đọc/ghi,
(2) tầng dữ liệu `ConnectionRepository`, (3) trạng thái `ConnectionViewModel`, (4) giao diện. Hợp đồng
Jira REST của 001 được **dùng lại nguyên vẹn**, không sửa
(`../../001-jql-ticket-table/contracts/jira-cloud-api.md`); hợp đồng bảng đã lưu của 003 giữ nguyên
trừ điều kiện cho phép làm mới (`../../003-saved-result-table/contracts/saved-tables.md`).

## 1. Dữ liệu lưu trên máy

| Nơi lưu | Nội dung | Ai ghi | Ai xoá |
|--------|---------|--------|--------|
| `config.json` | `baseUrl`, `email`, `lastJql`, `visibleColumns`, `sort`, `schemaVersion` | `ConnectionRepository.validate` (địa chỉ + email), `QueryViewModel` (tuỳ chọn truy vấn) | `ConnectionRepository.disconnect` (chỉ địa chỉ + email) |
| Kho bí mật hệ điều hành (`jira-tools` / `jira-tools:api-token`) | API token | `ConnectionRepository.validate` | `ConnectionRepository.disconnect` |
| `saved-tables.json` | Danh mục bảng đã lưu của 003 (kèm `jiraBaseUrl` riêng của từng bảng) | `SavedTableStore` | Không ai (ngoài thao tác xoá bảng của người dùng) |

**Cam kết**:

- Token **chỉ** nằm trong kho bí mật; không bao giờ trong `config.json`, `saved-tables.json`, thông báo
  lỗi hay nhật ký (FR-006, SC-003). `SecureStorage` hiện có đã fail-closed: khi kho bí mật không khả
  dụng, mọi thao tác ném `SecureStorageException` thay vì ghi ra tệp.
- **Kiểm tra ngầm chỉ đọc**: `verifySaved()` không ghi `config.json`, không ghi/xoá token (FR-013).
- **Ngắt kết nối xoá đúng hai thứ**: token và `baseUrl`/`email`; các khoá khác của `config.json` giữ
  nguyên (FR-010, FR-011). Nếu xoá token thất bại, dừng lại và báo lỗi — **không** xoá địa chỉ/email
  (tránh trạng thái nửa vời: mất cấu hình nhưng token còn).

**Bảng chân trị sau mỗi hành động**:

| Hành động | `config.json` baseUrl/email | Token trong kho bí mật | Trạng thái kết quả |
|----------|----------------------------|-----------------------|-------------------|
| Kết nối thành công | Được ghi (đã chuẩn hoá) | Được ghi | `Connected` |
| Kết nối thất bại | Không đổi | Không đổi | `Invalid(error)` |
| Kiểm tra ngầm thành công | Không đổi | Không đổi | `Connected` |
| Kiểm tra ngầm: lỗi mạng | Không đổi | Không đổi | `Unverified` |
| Kiểm tra ngầm: token bị từ chối | Không đổi (giữ địa chỉ/email) | Không đổi (người dùng nhập lại sau) | `Invalid(AuthFailed/PermissionDenied)` |
| Kiểm tra ngầm: không có token | Không đổi (giữ địa chỉ/email) | Trống | `Unconfigured` |
| Ngắt kết nối thành công | Bị xoá | Bị xoá | `Unconfigured` |
| Ngắt kết nối khi kho bí mật lỗi | Không đổi | Không đổi | Không đổi + thông báo lỗi |

## 2. Tầng dữ liệu: `ConnectionRepository`

```kotlin
data class SavedConnectionVerification(
    val status: ConnectionStatus,
    val info: SavedConnectionInfo,
)

class ConnectionRepository(
    private val apiClient: JiraApiClient,
    private val configStore: AppConfigStore,
    private val secureStorage: SecureStorage,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    /** SỬA: đọc nhanh từ config, KHÔNG hỏi kho bí mật. Có địa chỉ + email → Unverified; thiếu → Unconfigured. */
    suspend fun loadSaved(): ConnectionStatus

    /** MỚI: ảnh chụp thông tin đã lưu để điền sẵn và hiển thị chỉ báo token (FR-008, FR-009). */
    suspend fun savedInfo(): SavedConnectionInfo

    /** MỚI: kiểm tra ngầm thông tin đã lưu; chỉ đọc, không ghi gì (FR-017, FR-022). */
    suspend fun verifySaved(): ConnectionStatus

    /** MỚI: trả trạng thái cùng ảnh chụp token-free để ViewModel không đọc kho bí mật lần hai. */
    suspend fun verifySavedWithInfo(): SavedConnectionVerification

    /** GIỮ NGUYÊN: kiểm tra `/myself`, chỉ lưu khi Jira chấp nhận (FR-005, FR-016). */
    suspend fun validate(baseUrl: String, email: String, token: String): ConnectionStatus

    /** MỚI: xoá token rồi xoá địa chỉ/email; giữ mọi tuỳ chọn khác (FR-010, FR-011). */
    suspend fun disconnect(): ConnectionStatus
}
```

**Điều kiện sau của từng hàm**:

- `loadSaved()`: chỉ đọc `config.json`. Trả `Unverified(baseUrl, email)` khi có đủ địa chỉ + email,
  `Unconfigured` khi thiếu. **Không** ném ngoại lệ khi tệp hỏng (đã có hành vi mặc định từ 001).
- `savedInfo()`: trả về địa chỉ/email đã cắt khoảng trắng và `tokenSaved = (loadToken()?.isNotBlank() == true)`.
  Lỗi `SecureStorageException` được thể hiện là `tokenSaved = false`; không bao giờ trả về giá trị token.
- `verifySaved()`: dùng cùng snapshot nội bộ với `savedInfo()`; nếu thiếu địa chỉ/email hoặc thiếu token → `Unconfigured`.
  Ngược lại gọi `apiClient.myself(explicit = credentials)`:
  - thành công → `Connected(baseUrl, email)`;
  - `JiraError.AuthFailed`/`PermissionDenied` → `Invalid(error)`;
  - mọi lỗi còn lại (mạng, timeout, không phân giải được tên miền, lỗi không mong đợi) → `Unverified(baseUrl, email)`.
  Trong mọi nhánh, **không** ghi `config.json` và **không** ghi/xoá token.
- `verifySavedWithInfo()`: trả trạng thái cùng `SavedConnectionInfo` đọc trong chính snapshot đó, không
  đưa giá trị token ra khỏi tầng dữ liệu; nhờ vậy `tokenSaved` vẫn chính xác với token mồ côi.
- `validate(...)`: giữ nguyên hành vi 001 (kiểm tra rỗng, chuẩn hoá địa chỉ, gọi `/myself`, chỉ khi
  thành công mới `saveToken` + lưu địa chỉ/email).
- `disconnect()`: gọi `secureStorage.deleteToken()` rồi `configStore.save(prefs.copy(baseUrl = null,
  email = null))`, trả `Unconfigured`. `deleteToken()` vốn idempotent nên ngắt kết nối khi chưa có gì
  vẫn thành công. Ngoại lệ từ `SecureStorage`/`AppConfigStore` được ném ra ngoài để giao diện thông báo.

## 3. Trạng thái: `ConnectionViewModel`

```kotlin
class ConnectionViewModel(
    private val repository: ConnectionRepository,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    val status: StateFlow<ConnectionStatus>   // giữ nguyên
    val draft: StateFlow<ConnectionDraft>     // MỚI
    val hasSavedConnection: StateFlow<Boolean> // MỚI (còn dữ liệu cần ngắt kết nối)
    val tokenSaved: StateFlow<Boolean?>       // MỚI (null = chưa biết)
    val notice: StateFlow<String?>            // MỚI

    fun updateDraft(draft: ConnectionDraft)   // MỚI: không gọi mạng
    suspend fun loadSaved()                   // SỬA: cập nhật status + điền sẵn địa chỉ/email vào draft
    suspend fun verifySaved()                 // MỚI: đặt Validating → trạng thái cuối; cập nhật tokenSaved
    suspend fun verifySavedWithInfo(): SavedConnectionVerification // MỚI: giữ tokenSaved chính xác từ cùng snapshot
    suspend fun validate(baseUrl, email, token) // GIỮ NGUYÊN chữ ký; cập nhật tokenSaved/draft/notice
    suspend fun disconnect()                  // MỚI: đặt notice khi xong; xoá draft
    fun clearNotice()                         // MỚI
}
```

**Cam kết**:

- Mọi hàm đều là `suspend` (trừ các hàm cập nhật state đồng bộ) để tầng giao diện tự quản phạm vi
  coroutine — đúng quy ước của `QueryViewModel`/`SavedTablesViewModel`.
- Lỗi từ tầng dữ liệu được chuyển thành trạng thái/thông báo, **không** làm sập giao diện (giữ hành vi
  `guard` hiện có).
- `validate` thành công thì xoá token khỏi `draft` sau khi lưu (giảm thời gian token nằm trong bộ nhớ).
- `notice` là câu chữ tiếng Việt, không chứa token.
- `hasSavedConnection` là `true` nếu snapshot còn địa chỉ, email hoặc token; nút ngắt kết nối vẫn
  dùng được để xoá token mồ côi hoặc cấu hình thiếu.

## 4. Hợp đồng giao diện

**Dòng trạng thái trên màn hình chính** (FR-019): một dòng duy nhất, hiển thị ở cả ba chế độ xem:

- Nội dung: `connectionStatusText(status)` — không hiển thị địa chỉ Jira, email hay token.
- Kèm một nút "Cài đặt" chuyển sang chế độ xem Cài đặt (FR-001).
- Khi trạng thái là `Invalid`, dòng này thay bằng câu chữ nêu lý do ngắn + gợi ý mở Cài đặt (FR-012).

**Bộ chuyển chế độ xem**: ba mục "Truy vấn" (mặc định) / "Bảng đã lưu" / "Cài đặt", ngay dưới dòng
trạng thái; chuyển qua lại không làm mất kết quả truy vấn, bảng đã lưu đang mở hay truy vấn đang chạy
(FR-015).

**Màn hình Cài đặt** (`ui/settings/SettingsScreen.kt`), mục "Kết nối Jira":

| Thành phần | Nội dung | Yêu cầu |
|-----------|---------|---------|
| Ô "Địa chỉ Jira" | Điền sẵn từ `SavedConnectionInfo`; cho sửa | FR-004, FR-009 |
| Ô "Email" | Điền sẵn từ `SavedConnectionInfo`; cho sửa | FR-004, FR-009 |
| Ô "API token" | Luôn rỗng khi mở; có nút Hiện/Ẩn; gợi ý "đã có token được lưu — nhập để thay thế" khi `tokenSaved == true` | FR-004, FR-008, FR-009 |
| Chỉ báo token | "Đã có token được lưu trên máy." / "Chưa có token được lưu." / "Đang kiểm tra…" khi `tokenSaved == null` | FR-008 |
| Nút "Kiểm tra kết nối" | Gọi `validate(...)`; trong lúc chạy hiển thị tiến trình và khoá ô nhập | FR-004 |
| Vùng thông báo | `connectionStatusDetail(status)` + `notice`; lỗi hiển thị theo màu lỗi | FR-004, FR-012, FR-014 |
| Nút "Ngắt kết nối" | Chỉ bật khi `hasSavedConnection`; mở hộp thoại xác nhận; huỷ thì không thay đổi gì | FR-010 |

**Không còn trên màn hình chính**: biểu mẫu nhập địa chỉ/email/token, trạng thái thu gọn/mở rộng biểu
mẫu, và mọi ô nhập token (FR-002).

## 5. Điều chỉnh hợp đồng của 003 (làm mới bảng đã lưu)

Điều kiện tiên quyết của thao tác "Làm mới" đổi từ `ConnectionStatus.Connected` sang **"trạng thái
khác `Unconfigured`"** (tức chấp nhận cả `Unverified`). Lý do: khi mất mạng, người dùng vẫn phải thử
được làm mới (FR-022 của 004); nếu thông tin đã lưu thật sự sai thì lời gọi trả về lỗi tương ứng và
dữ liệu cũ được giữ nguyên (hành vi của 003 không đổi). Trường hợp chưa cấu hình vẫn bị từ chối như
trước — test hiện có của 003 giữ nguyên hiệu lực.

## 6. Không thay đổi

- Hợp đồng Jira REST và `JiraErrorMapper` (001).
- `SecureStorage`, `AppPaths`, `AppConfigStore`, `JiraApiClient`, `TicketOpener`, `TicketTable`,
  `QueryViewModel`: giữ nguyên.
- Định dạng `config.json` và `saved-tables.json`: giữ nguyên (không thêm khoá mới ở tính năng này).
- `gradle/libs.versions.toml` và `composeApp/build.gradle.kts`: không đổi — tính năng chỉ dùng thư viện
  đã có (Nguyên tắc IV).
