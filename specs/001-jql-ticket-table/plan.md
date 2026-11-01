# Implementation Plan: Truy vấn Jira bằng JQL và hiển thị kết quả dạng bảng

**Branch**: `001-jql-ticket-table` | **Date**: 2026-10-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-jql-ticket-table/spec.md`

## Summary

Ứng dụng desktop (Kotlin Multiplatform, chỉ target Desktop JVM) cho phép cấu hình kết nối tới Jira Cloud (email + API token), nhập và chạy truy vấn JQL, rồi xem kết quả dưới dạng bảng có phân trang, tùy biến cột, sắp xếp và mở ticket trong trình duyệt. Toàn bộ logic (model, client REST, trạng thái) nằm trong `commonMain`; phần đặc thù JVM (secure storage, mở trình duyệt, đường dẫn cấu hình) nằm sau `expect/actual`. UI dùng Compose Multiplatform, trạng thái phơi bày qua ViewModel + `StateFlow`.

**Technical approach**: Ktor Client (engine OkHttp, gọi từ `commonMain`) gọi Jira Cloud REST API v3 qua endpoint tìm kiếm nâng cao (`/rest/api/3/search/jql`, phân trang bằng cursor `nextPageToken`) và endpoint đếm xấp xỉ (`/rest/api/3/search/approximate-count`) để hiển thị tổng số; xác thực Basic email + API token; token lưu trong keychain hệ điều hành qua `SecureStorage` (expect/actual), cấu hình không bí mật lưu file JSON; bảng ảo hóa bằng `LazyColumn`.

## Technical Context

**Language/Version**: Kotlin 2.4.21 (ổn định, chốt sau nghiên cứu — ghim trong `gradle/libs.versions.toml`); JDK 17 (LTS) toolchain; Gradle 9.7.1

**Primary Dependencies**: Compose Multiplatform 1.12.2 (Desktop); Ktor Client 3.6.1 + engine OkHttp (MockEngine cho test); kotlinx.serialization 1.12.0; kotlinx-coroutines 1.11.0; java-keyring 1.0.4 (secure storage trên JVM); plugin ktlint `org.jlleitschuh.gradle.ktlint` 14.2.0. Ma trận đầy đủ và lý do chọn: xem `research.md` mục 10.

**Storage**: File JSON cấu hình người dùng (base URL, email, cột hiển thị, JQL gần nhất, trạng thái sắp xếp) trong thư mục cấu hình nền tảng + API token trong keychain hệ điều hành; không dùng cơ sở dữ liệu

**Testing**: `kotlin.test` + `kotlinx-coroutines-test` trong `commonTest`; HTTP fake bằng Ktor `MockEngine` + fixture JSON trong `commonTest` (không gọi Jira thật theo hiến pháp); fakes cho `SecureStorage`/`AppPaths`/`UrlOpener`/`ioDispatcher`. Chiến lược chi tiết: `research.md` mục 12

**Target Platform**: Desktop JVM (macOS/Windows/Linux); chỉ target desktop trong phiên bản này

**Project Type**: desktop-app (Kotlin Multiplatform, một module `composeApp`)

**Performance Goals**: Bảng ≤ 100 ticket hiển thị trong < 5 giây (SC-002); danh sách ảo hóa giữ UI mượt với hàng nghìn dòng; trạng thái tải không chặn UI (FR-010)

**Constraints**: Không lưu bí mật dạng văn bản thuần (FR-003 + hiến pháp); test tự động không gọi Jira thật (hiến pháp II); mọi phụ thuộc khai báo trong version catalog; mạng thông thường, không proxy đặc biệt; định danh mã nguồn tiếng Anh

**Scale/Scope**: 1 người dùng desktop, 1 hồ sơ kết nối duy nhất; kết quả tới hàng nghìn ticket (tải theo trang 100 dòng); ~2 màn hình + hộp thoại; ước lượng < 5k LOC

**Ánh xạ lỗi (SC-004)**: 8 lớp lỗi nội bộ phân biệt được — `InvalidJql` (400), `AuthFailed` (401), `PermissionDenied` (403), `RateLimited` (429), `InstanceNotFound` (unknown host), `NetworkUnreachable` (IO/connect), `Timeout`, `Unexpected`. Việc tách `InstanceNotFound` khỏi `NetworkUnreachable` là điều kiện để phân biệt "địa chỉ sai" với "mạng gián đoạn" theo SC-004. Chi tiết: `research.md` mục 4, `contracts/jira-cloud-api.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Trạng thái | Bằng chứng trong kế hoạch |
|-----------|---------|--------------------------|
| I. Đa nền tảng trước, phát hành trên Desktop | PASS | Model, client REST, repository, ViewModel đều ở `commonMain`; chỉ `SecureStorage`, `AppPaths`, `UrlOpener`, IO dispatcher dùng `expect/actual` trong `jvmMain`, không rò rỉ vào mã dùng chung |
| II. Kiểm thử trước (NON-NEGOTIABLE) | PASS | Test `commonTest` cho client (Ktor `MockEngine` + JSON mẫu), repository, ViewModel; tasks sắp theo trình tự test-first; không test nào gọi Jira thật |
| III. Hybrid UI: trạng thái ngoài Compose | PASS | Compose Multiplatform là mặc định; ViewModel `commonMain` phơi `StateFlow`; UI chỉ render + gửi sự kiện; không cần UI đặc thù nền tảng (không có khay hệ thống/tích hợp OS sâu) |
| IV. Kotlin chuẩn mực, an toàn kiểu | PASS | coroutines/Flow, không chặn luồng UI; `sealed interface` cho trạng thái truy vấn và lỗi; kotlinx.serialization; mọi phụ thuộc trong version catalog |
| V. Đơn giản và gia tăng (YAGNI) | PASS | Một module `composeApp` với source set KMP chuẩn; không DI framework (constructor injection thủ công qua `AppContainer`); không thêm module con |

Không có vi phạm ở cổng trước Phase 0.

**Tái đánh giá sau Phase 1**: PASS 5/5 — không phát sinh vi phạm mới. Các thiết kế Phase 1 giữ nguyên ranh giới: chỉ `SecureStorage`/`AppPaths`/`UrlOpener`/`ioDispatcher` là `expect/actual` (`contracts/platform-services.md`); mọi model và ánh xạ lỗi ở `commonMain` (`data-model.md`); không thêm module hay thư viện DI (Nguyên tắc V); phiên bản phụ thuộc được ghim cụ thể thay vì để mở (Nguyên tắc IV).

## Project Structure

### Documentation (this feature)

```text
specs/001-jql-ticket-table/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
jira-tools/
├── gradle/
│   ├── libs.versions.toml          # version catalog - mọi phụ thuộc khai báo tại đây
│   └── wrapper/                    # Gradle Wrapper (commit theo hiến pháp)
├── composeApp/
│   ├── build.gradle.kts            # KMP + Compose Desktop + ktlint
│   └── src/
│       ├── commonMain/kotlin/com/jiratools/
│       │   ├── app/                # Main composable, AppContainer (composition root), theme
│       │   ├── domain/             # model: Ticket, JiraConnection, QueryState, lỗi sealed, ColumnKey, UserPreferences
│       │   ├── data/               # JiraApiClient (Ktor), DTO + mapper, ConnectionRepository, AppConfigStore
│       │   ├── ui/
│       │   │   ├── connection/     # ConnectionViewModel + màn hình/hộp thoại cấu hình kết nối
│       │   │   └── query/          # QueryViewModel + màn hình truy vấn + components/ (bảng, chọn cột)
│       │   └── platform/           # expect: SecureStorage, AppPaths, UrlOpener, ioDispatcher
│       ├── commonTest/kotlin/com/jiratools/   # test logic chia sẻ + fakes + MockEngine tests
│       ├── jvmMain/kotlin/com/jiratools/      # actual: keychain, paths, Desktop.browse, entry point (main)
│       └── jvmTest/kotlin/com/jiratools/      # test nhỏ cho actual (chuẩn hóa đường dẫn, mở URL)
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

**Structure Decision**: Một module KMP duy nhất `composeApp` với các source set chuẩn (`commonMain`/`commonTest`/`jvmMain`/`jvmTest`) theo Nguyên tắc V. Không tách module `domain`/`data`/`ui` ở phiên bản đầu vì chỉ có một nền tảng phát hành và một ứng dụng duy nhất; việc tách module sẽ được xem xét khi có nhu cầu thực tế được chứng minh trong plan.

## Complexity Tracking

Không có vi phạm hiến pháp — không cần biện minh thêm.
