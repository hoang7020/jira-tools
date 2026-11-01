# jira-tools

Ứng dụng desktop (Kotlin Multiplatform + Compose Multiplatform) giúp **truy vấn Jira Cloud bằng JQL**
và hiển thị kết quả dưới dạng bảng. Toàn bộ giao diện bằng tiếng Việt.

Feature hiện tại: [`001-jql-ticket-table`](./specs/001-jql-ticket-table/spec.md).

## Tính năng

| Nhóm | Mô tả |
|------|-------|
| Kết nối | Nhập site URL, email và API token; kiểm tra kết nối qua `GET /rest/api/3/myself` |
| Truy vấn | Nhập JQL, chạy bằng nút **Tìm** hoặc `Ctrl+Enter`, phân trang bằng nút **Tải thêm** |
| Bảng | Cột Mã / Tiêu đề / Trạng thái / Người phụ trách / Độ ưu tiên / Cập nhật; bật/tắt cột; sắp xếp bằng cách nhấn vào tiêu đề cột |
| Mở ticket | Nhấp vào một dòng để mở `{site}/browse/{MÃ}` trong trình duyệt mặc định |
| Ghi nhớ | Truy vấn gần nhất, cột hiển thị và kiểu sắp xếp được khôi phục ở lần chạy sau |
| Lỗi | Phân biệt lỗi JQL, xác thực, quyền, quá tải, sai host, mất mạng và quá thời gian |

## Yêu cầu

| Yêu cầu | Ghi chú |
|---------|---------|
| JDK 17 trở lên | Đã kiểm chứng với JDK 26. Bytecode phát ra ở mức JVM 17 nên JDK mới hơn vẫn dựng được |
| Gradle Wrapper | Dùng `./gradlew`; không cần cài Gradle toàn cục |
| Tài khoản Jira Cloud | Cần site URL, email và API token |
| Mạng Internet | Kết nối tới `<site>.atlassian.net` |

Không cần cài Kotlin hay Compose: mọi thứ đến từ Gradle và version catalog trong `gradle/libs.versions.toml`.

## Lệnh thường dùng

```bash
# Chạy ứng dụng desktop
./gradlew :composeApp:run

# Chạy toàn bộ test (không gọi Jira thật)
./gradlew allTests

# Kiểm tra định dạng và lint
./gradlew ktlintCheck

# Tự động sửa lỗi định dạng
./gradlew ktlintFormat

# Dựng đầy đủ (biên dịch + test + kiểm tra chất lượng)
./gradlew build allTests ktlintCheck
```

Đóng gói bản phát hành (chỉ chạy được trên hệ điều hành tương ứng):
`./gradlew :composeApp:packageDmg` (macOS), `packageMsi` (Windows), `packageDeb` (Linux).

## Tạo API token của Jira Cloud

1. Đăng nhập <https://id.atlassian.com/manage-profile/security/api-tokens>.
2. Chọn **Create API token**, đặt tên gợi nhớ (ví dụ `jira-tools desktop`) rồi **Create**.
3. Sao chép token vừa hiện ra (chỉ hiển thị một lần) và dán vào ô **API token** của ứng dụng.
4. Nhập thêm **site URL** dạng `https://<tên-công-ty>.atlassian.net` và **email** đăng nhập Jira.

> Token thay cho mật khẩu; đừng dùng mật khẩu tài khoản Atlassian.

## Token được lưu ở đâu

Token **không bao giờ** được ghi ra file cấu hình. Nó chỉ được lưu trong kho bí mật của hệ điều hành
qua thư viện `java-keyring`, dưới service `jira-tools` và account `jira-tools:api-token`:

| Hệ điều hành | Kho bí mật |
|--------------|-----------|
| macOS | Keychain Access |
| Windows | Credential Manager |
| Linux | freedesktop Secret Service (GNOME Keyring / KWallet) |

File cấu hình chỉ chứa site URL, email, truy vấn gần nhất, cột hiển thị và kiểu sắp xếp:

| Hệ điều hành | Đường dẫn |
|--------------|-----------|
| macOS | `~/Library/Application Support/jira-tools/config.json` |
| Windows | `%APPDATA%\jira-tools\config.json` |
| Linux | `$XDG_CONFIG_HOME/jira-tools/config.json` (mặc định `~/.config/jira-tools/config.json`) |

Nếu kho bí mật không khả dụng (ví dụ phiên Linux không có Secret Service), ứng dụng báo lỗi và
**từ chối** lưu token thay vì ghi ra văn bản thuần.

## Kiểm thử

- Test dùng `MockEngine` của Ktor với JSON mô phỏng Jira; **không** có test nào gọi Jira thật.
- Chạy `./gradlew allTests` để chạy toàn bộ test trên mọi target.
- Kịch bản kiểm chứng thủ công (KB-1..KB-5): [`specs/001-jql-ticket-table/quickstart.md`](./specs/001-jql-ticket-table/quickstart.md).

## Cấu trúc mã nguồn

```text
composeApp/src/
  commonMain/kotlin/com/jiratools/
    app/       App.kt, AppContainer.kt (điểm ghép phụ thuộc)
    data/      JiraApiClient, DTO, bộ chuyển đổi, JiraErrorMapper, AppConfigStore
    domain/    Mô hình thuần Kotlin: Ticket, QueryState, JiraError, JqlOrderBy, BrowseUrl
    platform/  Hợp đồng nền tảng: AppPaths, SecureStorage, UrlOpener, Dispatchers
    ui/        ViewModel + màn hình Compose (connection, query)
  commonTest/  Test chung với fake và MockEngine
  jvmMain/     Cài đặt thật cho desktop: keyring, file cấu hình, mở trình duyệt
```

Quy ước kiến trúc nằm ở [`.specify/memory/constitution.md`](./.specify/memory/constitution.md):
logic nằm trong `commonMain`, phụ thuộc hệ điều hành tách qua interface, `StateFlow` đặt ngoài
Compose, và mọi thay đổi hành vi phải có test viết trước.

## Tài liệu feature

- Đặc tả: [`spec.md`](./specs/001-jql-ticket-table/spec.md)
- Kế hoạch: [`plan.md`](./specs/001-jql-ticket-table/plan.md)
- Mô hình dữ liệu: [`data-model.md`](./specs/001-jql-ticket-table/data-model.md)
- Hợp đồng API: [`contracts/`](./specs/001-jql-ticket-table/contracts)
- Danh sách công việc: [`tasks.md`](./specs/001-jql-ticket-table/tasks.md)
