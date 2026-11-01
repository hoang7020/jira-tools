# Tasks: Truy vấn Jira bằng JQL và hiển thị kết quả dạng bảng

**Feature**: `001-jql-ticket-table` | **Ngày**: 2026-10-30
**Input**: Design documents trong `specs/001-jql-ticket-table/` — `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`
**Prerequisites**: plan.md ✔, spec.md ✔, research.md ✔, data-model.md ✔, contracts/ ✔, quickstart.md ✔

## Ghi chú về kiểm thử (BẮT BUỘC — Hiến pháp Nguyên tắc II)

Hiến pháp dự án quy định **Kiểm thử trước (NON-NEGOTIABLE)**: mọi logic chia sẻ trong `commonMain` PHẢI có test trong `commonTest`, viết TRƯỚC khi triển khai và PHẢI thất bại (Red) trước khi triển khai (Green). Vì vậy trong mọi phase, các task test luôn đứng trước các task triển khai tương ứng.

- Framework: `kotlin.test` + `kotlinx-coroutines-test` + `kotlinx-serialization-json`.
- HTTP: `io.ktor:ktor-client-mock` (MockEngine) với fixture JSON trong `composeApp/src/commonTest/resources/`.
- **KHÔNG** test tự động nào được gọi Jira thật (Nguyên tắc II).
- Dịch vụ nền tảng dùng fake trong `commonTest`; phần `actual` JVM chỉ được unit test ở các hàm thuần (chuẩn hóa đường dẫn) — không mở trình duyệt, không chạm keychain thật khi chạy test.
- Không viết test Compose UI; UI được kiểm chứng thủ công bằng kịch bản KB-1..KB-5 trong `quickstart.md`.

## Tổ chức theo User Story

- **US1 (P1)**: Cấu hình kết nối tới Jira — 3 kịch bản nghiệm thu
- **US2 (P1)**: Chạy truy vấn JQL và xem kết quả dạng bảng — 5 kịch bản nghiệm thu
- **US3 (P2)**: Tùy biến bảng kết quả — 2 kịch bản nghiệm thu
- **US4 (P3)**: Mở ticket trực tiếp trong Jira — 1 kịch bản nghiệm thu

Mỗi user story tạo ra một phần gia tăng độc lập: có thể triển khai, kiểm thử và kiểm chứng riêng.

## Định dạng task

`- [ ] T### [P?] [US#?] Mô tả kèm đường dẫn file`

- `[P]`: có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành).
- `[US#]`: chỉ gắn cho task thuộc phase user story (Setup/Foundational/Polish không gắn).

## Quy ước đường dẫn (KMP, một module `composeApp`)

- `composeApp/src/commonMain/kotlin/com/jiratools/...` — logic chia sẻ (`app/`, `domain/`, `data/`, `ui/`, `platform/`)
- `composeApp/src/commonTest/kotlin/com/jiratools/...` — test logic chia sẻ
- `composeApp/src/jvmMain/kotlin/com/jiratools/...` — phần `actual` JVM + entry point
- `composeApp/src/jvmTest/kotlin/com/jiratools/...` — test đặc thù JVM
- Cấu hình build: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`

---

## Phase 1: Setup (Hạ tầng chung của dự án)

- [x] T001 Tạo bộ khung Gradle ở gốc repo: `settings.gradle.kts` (`rootProject.name = "jira-tools"`, `include(":composeApp")`, `dependencyResolutionManagement`), `build.gradle.kts` (khai báo plugin dạng `apply false`), `gradle.properties` (`org.gradle.jvmargs`, `kotlin.code.style=official`, `org.gradle.caching=true`) và Gradle Wrapper 9.7.1 tại `gradle/wrapper/gradle-wrapper.properties`
- [x] T002 [P] Tạo version catalog `gradle/libs.versions.toml`: ghim `kotlin = 2.4.21`, `compose-multiplatform = 1.12.2`, `ktor = 3.6.1`, `kotlinx-serialization = 1.12.0`, `kotlinx-coroutines = 1.11.0`, plugin `org.jlleitschuh.gradle.ktlint = 14.2.0`, `java-keyring = 1.0.4` (Nguyên tắc IV: KHÔNG ghim phiên bản trong build script)
- [x] T003 Tạo `composeApp/build.gradle.kts`: plugin `org.jetbrains.kotlin.multiplatform` + `org.jetbrains.compose` + `org.jetbrains.kotlin.plugin.compose` (cùng phiên bản Kotlin) + `org.jetbrains.kotlin.plugin.serialization` + `org.jlleitschuh.gradle.ktlint`; target `jvm()` với `jvmToolchain(17)`; `compose.desktop.application`; khai báo đầy đủ phụ thuộc commonMain/jvmMain/commonTest/jvmTest từ catalog
- [x] T004 [P] Thêm `.editorconfig` ở gốc repo: `ktlint_code_style = ktlint_official`, `indent_size = 4`, `max_line_length = 120`, quy tắc trailing comma — phục vụ cổng chất lượng lint
- [x] T005 [P] Tạo entry point tối thiểu để xác nhận build chạy được: `composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt` (hàm `main` mở `application { Window(...) }`) và `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt` (Composable placeholder)
- [x] T006 Kiểm chứng Phase 1: chạy `./gradlew build ktlintCheck` với cấu hình trong `composeApp/build.gradle.kts`, xác nhận build Desktop thành công và lint sạch trước khi sang Phase 2

---

## Phase 2: Foundational (Điều kiện tiên quyết — CHẶN mọi user story)

Toàn bộ phase này PHẢI hoàn thành trước khi bắt đầu US1.

- [x] T007 [P] Viết test cho `normalizeBaseUrl` trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/BaseUrlTest.kt`: cắt khoảng trắng hai đầu; thêm `https://` khi thiếu scheme; bỏ dấu `/` ở cuối; giữ nguyên khi đã hợp lệ; chuỗi rỗng -> trả về lỗi thay vì ném ngoại lệ (PHẢI thất bại trước T011)
- [x] T008 [P] Viết test cho `AppConfigStore` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/AppConfigStoreTest.kt`: ghi rồi đọc lại đúng giá trị (round-trip); file chưa tồn tại -> trả `UserPreferences` mặc định; JSON hỏng -> quay về mặc định và KHÔNG ném ngoại lệ; `schemaVersion` khác -> quay về mặc định (PHẢI thất bại trước T017)
- [x] T009 [P] Viết test cho `JiraErrorMapper` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraErrorMapperTest.kt`: 400 -> `InvalidJql`; 401 -> `AuthFailed`; 403 -> `PermissionDenied`; 429 -> `RateLimited` kèm `Retry-After` (có thể null); `UnknownHostException` -> `InstanceNotFound`; `ConnectException`/`IOException` -> `NetworkUnreachable`; `HttpRequestTimeoutException`/`ConnectTimeoutException` -> `Timeout`; mã khác -> `Unexpected(status, detail)`; thông báo lỗi KHÔNG chứa token (PHẢI thất bại trước T018)
- [x] T010 [P] Khai báo các seam nền tảng (Nguyên tắc I) theo `contracts/platform-services.md`: `composeApp/src/commonMain/kotlin/com/jiratools/platform/SecureStorage.kt` (`saveToken`/`loadToken`/`deleteToken`), `platform/AppPaths.kt` (`configFile()`), `platform/UrlOpener.kt` (`open(url)` không ném ra ngoài), `platform/Dispatchers.kt` (`expect val ioDispatcher`) + `SecureStorageException`
- [x] T011 [P] Triển khai `normalizeBaseUrl` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/BaseUrl.kt`
- [x] T012 [P] Tạo test double trong `composeApp/src/commonTest/kotlin/com/jiratools/platform/Fakes.kt`: `FakeSecureStorage` (lưu trong bộ nhớ, có chế độ ném `SecureStorageException`), `FakeAppPaths` (dùng thư mục tạm), `RecordingUrlOpener` (ghi lại URL, có chế độ ném lỗi), `TestDispatcherProvider` cho `ioDispatcher`
- [x] T013 [P] Triển khai các `actual` JVM: `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmSecureStorage.kt` (java-keyring 1.0.4, service `jira-tools`, key `jira-tools:api-token`, fail-closed: kho không khả dụng -> ném `SecureStorageException`, KHÔNG bao giờ ghi plaintext), `JvmAppPaths.kt`, `JvmUrlOpener.kt`, `JvmDispatchers.kt`
- [x] T014 [P] Định nghĩa model lỗi trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Errors.kt`: `ConnectionError` và `QueryError` là `sealed interface` (hoặc một sealed chung) với 8 biến thể `InvalidJql(message)`, `AuthFailed`, `PermissionDenied`, `RateLimited(retryAfterSeconds: Int?)`, `InstanceNotFound(host)`, `NetworkUnreachable`, `Timeout`, `Unexpected(status: Int?, detail: String?)`
- [x] T015 [P] Định nghĩa `ColumnKey` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/ColumnKey.kt`: `KEY`, `SUMMARY`, `STATUS`, `ASSIGNEE`, `PRIORITY`, `UPDATED` kèm nhãn hiển thị tiếng Việt và tên field JQL tương ứng
- [x] T016 [P] Định nghĩa `SortState` (`column: ColumnKey?`, `ascending: Boolean`) và `UserPreferences` `@Serializable` (`schemaVersion = 1`, `baseUrl`, `email`, `visibleColumns` mặc định đủ 6 cột, `lastJql`, `sort`) trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/UserPreferences.kt`
- [x] T017 Triển khai `AppConfigStore` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/AppConfigStore.kt`: đọc/ghi JSON qua `AppPaths` trên `ioDispatcher`, tự tạo thư mục cha, ghi file tạm rồi thay thế, file hỏng -> giá trị mặc định
- [x] T018 Triển khai `JiraErrorMapper` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraErrorMapper.kt`: đọc `ErrorCollection {errorMessages, errors}` từ Jira, ánh xạ 8 nhóm lỗi ở `contracts/jira-cloud-api.md`, tuyệt đối không đưa token vào thông báo
- [x] T019 Triển khai tầng HTTP trong `composeApp/src/commonMain/kotlin/com/jiratools/data/HttpClientFactory.kt` (client Ktor với `ContentNegotiation` JSON `ignoreUnknownKeys = true`, timeout, engine mặc định) và khung `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraApiClient.kt` (constructor nhận `HttpClient` + nguồn thông tin xác thực + base URL; gắn header `Basic base64(email:api_token)` chủ động; helper giải mã JSON và gọi `JiraErrorMapper`) — engine truyền từ ngoài để test dùng `MockEngine`
- [x] T020 Triển khai `AppContainer` trong `composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt`: composition root thủ công (không dùng DI framework — Nguyên tắc V) cung cấp `AppConfigStore`, `SecureStorage`, `AppPaths`, `UrlOpener`, `ioDispatcher`, `JiraApiClient`
- [x] T021 Dựng theme và khung ứng dụng trong `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: `MaterialTheme`, bố cục hai vùng (khu vực kết nối + khu vực nội dung), chuỗi tiếng Việt
- [x] T022 Kiểm chứng Phase 2: chạy `./gradlew allTests ktlintCheck` cho hạ tầng đã dựng trong `composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt`, xác nhận toàn bộ test T007–T009 chuyển từ đỏ sang xanh và lint sạch

---

## Phase 3: User Story 1 (P1) — Cấu hình kết nối tới Jira 🎯

**Mục tiêu**: người dùng nhập địa chỉ Jira Cloud, email và API token; ứng dụng kiểm tra qua `GET /rest/api/3/myself` và chỉ lưu khi kiểm tra thành công.

**Tiêu chí kiểm thử độc lập**: chạy ứng dụng khi chưa cấu hình -> nhập thông tin đúng -> báo kết nối thành công và chuyển sang khu vực truy vấn; nhập token sai -> thông báo lỗi xác thực rõ ràng và KHÔNG lưu gì; đóng rồi mở lại ứng dụng -> cấu hình vẫn còn (KB-1, KB-2).

**FR liên quan**: FR-001, FR-002, FR-003

- [x] T023 [P] [US1] Viết test `ConnectionRepository` với `MockEngine` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt`: (a) 200 từ `/rest/api/3/myself` -> phát trạng thái kết nối thành công và lưu `baseUrl`/`email` vào `AppConfigStore` cùng token vào `SecureStorage`; (b) 401 -> lỗi xác thực và KHÔNG lưu gì; (c) `UnknownHostException` -> lỗi sai địa chỉ; (d) timeout -> lỗi timeout; (e) trường rỗng -> không gọi mạng (PHẢI thất bại trước T026)
- [x] T024 [P] [US1] Viết test `ConnectionViewModel` trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt`: `StateFlow<ConnectionStatus>` chuyển `Unconfigured -> Validating -> Connected`/`Invalid`; lúc khởi động đọc cấu hình đã lưu -> trạng thái kết nối sẵn sàng; nút kiểm tra bị vô hiệu khi đang `Validating`; `SecureStorageException` -> hiển thị lỗi chứ không làm sập ứng dụng (PHẢI thất bại trước T027)
- [x] T025 [P] [US1] Định nghĩa `ConnectionStatus` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt`: `Unconfigured`, `Validating`, `Connected(baseUrl, email)`, `Invalid(error: ConnectionError)`
- [x] T026 [US1] Triển khai `ConnectionRepository` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt`: đọc/ghi cấu hình qua `AppConfigStore`, lưu token qua `SecureStorage`, gọi `GET /rest/api/3/myself` để kiểm tra, chuyển mọi thất bại sang `ConnectionError`
- [x] T027 [US1] Triển khai `ConnectionViewModel` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt`: phơi `StateFlow<ConnectionStatus>` (Nguyên tắc III — tách khỏi Compose), `loadSaved()`, `validate(baseUrl, email, token)`, chạy trên `ioDispatcher`
- [x] T028 [US1] Dựng màn hình cấu hình trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionScreen.kt`: trường địa chỉ Jira, email, API token (dạng mật khẩu có nút hiện/ẩn), nút `Kiểm tra kết nối`, chỉ báo đang kiểm tra, thông báo lỗi riêng cho từng `ConnectionError`, thông báo thành công
- [x] T029 [US1] Nối `ConnectionViewModel` + `ConnectionScreen` vào `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt` (chặn khu vực truy vấn khi chưa cấu hình) và mở rộng `AppContainer` để cung cấp `ConnectionRepository` trong `composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt`
- [ ] T030 [US1] Kiểm chứng thủ công KB-1 và KB-2 theo `quickstart.md` bằng `./gradlew :composeApp:run`, ghi lại kết quả — **chờ người dùng kiểm chứng** (cần site Jira Cloud thật và phiên desktop có màn hình); kết quả mong đợi đã ghi trong mục "Kết quả kiểm chứng" của `quickstart.md`

---

## Phase 4: User Story 2 (P1) — Chạy truy vấn JQL và xem kết quả dạng bảng 🎯

**Mục tiêu**: nhập JQL và chạy truy vấn qua `POST /rest/api/3/search/jql`, xem kết quả trong bảng có phân trang, có trạng thái đang tải/trống/lỗi, và giữ lại JQL gần nhất.

**Tiêu chí kiểm thử độc lập**: với cấu hình hợp lệ, chạy một truy vấn JQL hợp lệ -> bảng hiển thị các cột Mã/Tiêu đề/Trạng thái/Người xử lý/Độ ưu tiên/Cập nhật; kết quả rỗng -> thông báo "không có kết quả"; JQL sai cú pháp -> lỗi rõ ràng kèm thông báo của Jira; hơn 100 kết quả -> nút tải thêm hoạt động; JQL rỗng -> không gọi mạng (KB-3).

**FR liên quan**: FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-014

- [x] T031 [P] [US2] Viết test ánh xạ DTO -> `Ticket` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt`: `assignee` null -> ô trống; `priority` null -> ô trống; `updated` sai định dạng -> không làm sập; tiêu đề rất dài giữ nguyên dữ liệu; `fields` thiếu khóa -> `Unexpected` hoặc ô trống có kiểm soát; `names` dùng làm nhãn cột (PHẢI thất bại trước T037)
- [x] T032 [P] [US2] Viết test `JiraApiClient.searchJql` với `MockEngine` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientSearchTest.kt`: request chứa đúng `jql`, `fields` gồm 5 field đã chốt, `maxResults = 100`, không gửi `nextPageToken` ở trang đầu; trang sau dùng token trả về; `isLast = true` hoặc thiếu/`null` `nextPageToken` -> kết thúc; 400 -> `InvalidJql` giữ nguyên `errorMessages` của Jira; 401 -> `AuthFailed`; 403 -> `PermissionDenied`; 429 -> `RateLimited` (PHẢI thất bại trước T038)
- [x] T033 [P] [US2] Viết test `JiraApiClient.approximateCount` trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientCountTest.kt`: 200 -> trả về `count`; lỗi hoặc timeout -> trả `null` và kết quả tìm kiếm vẫn thành công (số lượng chỉ là thông tin bổ trợ, không phải điều kiện hiển thị bảng) (PHẢI thất bại trước T038)
- [x] T034 [P] [US2] Viết test `QueryViewModel` trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelTest.kt`: khởi động khôi phục `lastJql`; `run()` -> `Idle -> Loading -> Success`; kết quả rỗng -> `Empty`; lỗi -> `Error` và vẫn giữ JQL để thử lại; `loadMore()` nối thêm ticket không trùng lặp và dừng khi hết trang; `lastJql` được ghi vào `AppConfigStore`; JQL rỗng -> không gọi mạng (PHẢI thất bại trước T039)
- [x] T035 [P] [US2] Định nghĩa model `Ticket` và `QueryPage` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt`: `Ticket(key, summary, status, assignee, priority, updated)`; `QueryPage(tickets, nextPageToken: String?, isLast: Boolean, approximateTotal: Int?)`
- [x] T036 [P] [US2] Định nghĩa `QueryState` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/QueryState.kt`: `Idle`, `Loading`, `Success(tickets, hasMore, approximateTotal)`, `Empty`, `Error(error: QueryError, jql: String)`
- [x] T037 [US2] Triển khai DTO và mapper trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt` (`SearchJqlResponse` với `issues`, `nextPageToken`, `isLast`, `names`, `warnings` — lưu ý KHÔNG có trường `total`; `ApproximateCountResponse`; `IssueDto`; `FieldsDto`; `UserDto`; `StatusDto`; `PriorityDto`; `ErrorCollectionDto`) và `composeApp/src/commonMain/kotlin/com/jiratools/data/TicketMapper.kt`
- [x] T038 [US2] Mở rộng `JiraApiClient` với `searchJql(jql, nextPageToken)` và `approximateCount(jql)` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraApiClient.kt`, tuân thủ `contracts/jira-cloud-api.md`
- [x] T039 [US2] Triển khai `QueryViewModel` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt`: `run(jql)`, `loadMore()`, `retry()`, phơi `StateFlow<QueryState>`, gọi `approximateCount` không chặn luồng chính, lưu `lastJql` vào cấu hình
- [x] T040 [US2] Dựng khu vực nhập JQL trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryInput.kt`: ô nhập nhiều dòng, nút `Chạy truy vấn`, phím tắt Enter/Ctrl+Enter để chạy, vô hiệu hóa khi đang tải
- [x] T041 [US2] Dựng bảng ticket trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`: `LazyColumn` để ảo hóa danh sách dài, tiêu đề theo các cột đang bật, ô null -> trống, nội dung dài -> cắt bằng ellipsis, lần lượt hiển thị trạng thái đang tải / không có kết quả / lỗi kèm nút `Thử lại`, nút `Tải thêm` khi còn trang
- [x] T042 [US2] Nối màn hình truy vấn vào `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt` và mở rộng `AppContainer` để tạo `QueryViewModel` trong `composeApp/src/commonMain/kotlin/com/jiratools/app/AppContainer.kt`
- [ ] T043 [US2] Kiểm chứng thủ công KB-3 theo `quickstart.md`: phân trang, kết quả rỗng, JQL sai cú pháp, mất mạng, và việc giữ `lastJql` sau khi khởi động lại — **chờ người dùng kiểm chứng** (cần site Jira Cloud thật và phiên desktop có màn hình); kết quả mong đợi đã ghi trong mục "Kết quả kiểm chứng" của `quickstart.md`

---

## Phase 5: User Story 3 (P2) — Tùy biến bảng kết quả

**Mục tiêu**: bật/tắt các cột hiển thị của bảng và sắp xếp theo cột bằng cách viết lại mệnh đề `ORDER BY` phía server.

**Tiêu chí kiểm thử độc lập**: tắt cột `Độ ưu tiên` -> bảng cập nhật ngay và giữ nguyên sau khi khởi động lại ứng dụng; bấm tiêu đề cột `Cập nhật` -> truy vấn chạy lại với `ORDER BY updated DESC`, bấm lần nữa -> `ASC`; cột `Mã` luôn hiển thị (KB-4).

**FR liên quan**: FR-011, FR-012

- [x] T044 [P] [US3] Viết test hàm viết lại `ORDER BY` trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/JqlOrderByTest.kt`: JQL chưa có `ORDER BY` -> thêm vào cuối; đã có `ORDER BY` ở cuối -> thay thế; `ORDER BY` chỉ xuất hiện trong chuỗi/nháy hoặc trong ngoặc -> KHÔNG bị sửa; phần còn lại của truy vấn giữ nguyên; JQL rỗng -> trả về nguyên vẹn; chỉ dùng từ khóa tiếng Anh chuẩn của JQL (PHẢI thất bại trước T046)
- [x] T045 [P] [US3] Viết test tùy biến bảng trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelPreferencesTest.kt`: `visibleColumns` mặc định đủ 6 cột; tắt/bật cột -> ghi ngay vào `AppConfigStore` và phát lại qua `StateFlow`; chặn tắt hết cột (luôn giữ ít nhất cột `Mã`); bấm cùng một cột -> đổi chiều ASC/DESC; chọn cột mới -> chạy lại truy vấn với JQL đã viết lại (PHẢI thất bại trước T047)
- [x] T046 [P] [US3] Triển khai hàm viết lại `ORDER BY` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/JqlOrderBy.kt` (sắp xếp phía server vì sắp xếp một trang đơn lẻ phía client sẽ gây hiểu nhầm)
- [x] T047 [US3] Nối `SortState` và `visibleColumns` vào `QueryViewModel` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryViewModel.kt`: khôi phục từ `UserPreferences` lúc khởi động, chạy lại truy vấn khi `sort` thay đổi
- [x] T048 [US3] Thêm bộ chọn cột và tiêu đề sắp xếp được trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`: menu chọn cột cho 6 cột, hiển thị chiều sắp xếp, chỉ báo đang tải khi chạy lại truy vấn
- [ ] T049 [US3] Kiểm chứng thủ công KB-4 theo `quickstart.md`, gồm cả việc tuỳ biến cột và chiều sắp xếp được giữ sau khi khởi động lại — **chờ người dùng kiểm chứng** (cần site Jira Cloud thật và phiên desktop có màn hình); kết quả mong đợi đã ghi trong mục "Kết quả kiểm chứng" của `quickstart.md`

---

## Phase 6: User Story 4 (P3) — Mở ticket trực tiếp trong Jira

**Mục tiêu**: bấm vào một hàng trong bảng để mở ticket tương ứng trong trình duyệt mặc định.

**Tiêu chí kiểm thử độc lập**: bấm một hàng -> trình duyệt mở đúng địa chỉ dạng `{baseUrl}/browse/{KEY}`; khi trình duyệt mở thất bại -> chỉ hiện thông báo, ứng dụng không sập và bảng vẫn giữ nguyên kết quả (KB-5).

**FR liên quan**: FR-013

- [x] T050 [P] [US4] Viết test dẫn xuất URL trong `composeApp/src/commonTest/kotlin/com/jiratools/domain/BrowseUrlTest.kt`: `{baseUrl}/browse/{key}`; không nhân đôi dấu `/` khi `baseUrl` có sẵn hoặc thiếu `/` ở cuối; địa chỉ không có scheme vẫn ra URL hợp lệ (PHẢI thất bại trước T052)
- [x] T051 [P] [US4] Viết test `TicketOpener` trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TicketOpenerTest.kt`: gọi `RecordingUrlOpener` với đúng URL đã dẫn xuất từ `Ticket`; `UrlOpener` ném lỗi -> trả về thông báo lỗi thay vì ném ra ngoài (PHẢI thất bại trước T053)
- [x] T052 [P] [US4] Triển khai `browseUrl` trong `composeApp/src/commonMain/kotlin/com/jiratools/domain/BrowseUrl.kt`
- [x] T053 [US4] Triển khai `TicketOpener` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketOpener.kt`: chỉ nhận `Ticket` + địa chỉ từ cấu hình đã lưu, KHÔNG nhận URL tự do từ người dùng, bắt mọi lỗi từ `UrlOpener` và trả thông báo cho UI
- [x] T054 [US4] Nối thao tác bấm hàng trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` (con trỏ dạng bàn tay, gọi `TicketOpener`) và hiển thị thông báo lỗi không chặn luồng trong `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`
- [ ] T055 [US4] Kiểm chứng thủ công KB-5 theo `quickstart.md` — **chờ người dùng kiểm chứng** (cần site Jira Cloud thật và phiên desktop có màn hình); kết quả mong đợi đã ghi trong mục "Kết quả kiểm chứng" của `quickstart.md`

---

## Phase 7: Polish (Cổng chất lượng và hoàn thiện)

- [x] T056 [P] Viết test hồi quy chống rò rỉ bí mật trong `composeApp/src/commonTest/kotlin/com/jiratools/data/SecretLeakTest.kt`: sau khi lưu kết nối thành công, nội dung file cấu hình KHÔNG chứa token; thông báo của `JiraErrorMapper` không chứa token (FR-003)
- [x] T057 [P] Bổ sung test biên trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryEdgeCaseTest.kt`: JQL chứa ký tự Unicode/tiếng Việt; tiêu đề rất dài; 0 kết quả; `nextPageToken` là chuỗi rỗng; `issues` là mảng rỗng kèm `isLast = false`; `warnings` của Jira vẫn không làm hỏng việc hiển thị bảng
- [x] T058 [P] Viết `README.md` ở gốc repo: yêu cầu JDK 17, lệnh `./gradlew :composeApp:run`, `./gradlew allTests`, `./gradlew ktlintCheck`, cách tạo API token Jira Cloud, nơi token được lưu (keychain hệ điều hành, không nằm trong file cấu hình)
- [x] T059 Chạy cổng chất lượng đầy đủ theo Hiến pháp bằng `./gradlew build allTests ktlintCheck` (khai báo plugin trong `composeApp/build.gradle.kts`) — build Desktop thành công, toàn bộ test pass, lint sạch
- [ ] T060 Chạy trọn bộ kịch bản KB-1..KB-5 trong `specs/001-jql-ticket-table/quickstart.md` và ghi lại kết quả ngay trong mục kiểm chứng của file đó — **chờ người dùng kiểm chứng**: KB-1..KB-5 cần site Jira Cloud thật, API token hợp lệ và phiên desktop có màn hình nên không chạy được trong môi trường không giao diện; mục "Kết quả kiểm chứng" của `quickstart.md` đã ghi trạng thái và kết quả mong đợi của từng kịch bản
- [x] T061 Đối chiếu FR-001..FR-014 và SC-001..SC-006 với các task đã hoàn thành, bổ sung task cho phần còn thiếu trong `specs/001-jql-ticket-table/tasks.md` — đã đối chiếu đủ 14 FR và 6 SC; tất cả đều có mã nguồn và test tương ứng nên không phát hiện khoảng trống, không cần thêm task
- [x] T062 Kiểm chứng SC-005 và SC-006 trên bảng ở `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`: điều hướng tới ticket thứ 250 không quá 3 thao tác; truy vấn ngắn phản hồi trong dưới 10 giây — ghi số liệu đo được vào `specs/001-jql-ticket-table/quickstart.md` — SC-005 đã kiểm chứng tự động (2 lần "Tải thêm", 250 ticket, 3 lời gọi API, không trùng dòng); SC-006 đo được chi phí phía ứng dụng ~1,33 ms/truy vấn và ~9,1 µs để chuyển 100 ticket, phần Jira/mạng chờ đo thủ công cùng T060

---

## Dependencies & Execution Order

### Phụ thuộc giữa các phase

- **Phase 1 (Setup)**: không phụ thuộc, bắt đầu ngay.
- **Phase 2 (Foundational)**: phụ thuộc Phase 1 — CHẶN mọi user story.
- **Phase 3 (US1, P1)**: phụ thuộc Phase 2.
- **Phase 4 (US2, P1)**: phụ thuộc Phase 2; trên thực tế cần US1 để có phiên kết nối hợp lệ khi kiểm chứng thủ công.
- **Phase 5 (US3, P2)**: phụ thuộc US2 (cần bảng và luồng truy vấn đã có).
- **Phase 6 (US4, P3)**: phụ thuộc US2 (cần bảng để bấm hàng).
- **Phase 7 (Polish)**: sau khi các story mong muốn đã xong.

### Thứ tự trong mỗi story

1. Test trước (Red — phải thất bại)
2. Model/domain
3. Repository/client (service)
4. ViewModel (trạng thái ngoài Compose)
5. UI Compose
6. Kiểm chứng thủ công theo `quickstart.md`

### Sơ đồ phụ thuộc user story

```text
Setup (T001-T006)
   -> Foundational (T007-T022)
        -> US1 (T023-T030)
             -> US2 (T031-T043)
                  -> US3 (T044-T049)
                  -> US4 (T050-T055)
                       -> Polish (T056-T062)
```

US3 và US4 độc lập với nhau (khác file), có thể làm song song sau khi US2 xong.

### Cơ hội chạy song song

- **Setup**: T002, T004, T005 cùng lúc (khác file).
- **Foundational**: T007–T016 cùng lúc (10 task khác file); T017, T018, T019 lần lượt sau đó; T020, T021 sau T019.
- **US1**: T023, T024, T025 cùng lúc; T026–T029 lần lượt.
- **US2**: T031–T036 cùng lúc (6 task); T037–T042 lần lượt.
- **US3**: T044, T045, T046 cùng lúc.
- **US4**: T050, T051, T052 cùng lúc.
- **Polish**: T056, T057, T058 cùng lúc; T059–T062 lần lượt.

Tổng cộng **31 task gắn `[P]`**.

### Ví dụ chạy song song theo story

**US1 — Cấu hình kết nối**

```bash
# Chạy cùng lúc T023, T024, T025:
Task: "Viết test ConnectionRepository với MockEngine trong composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt"
Task: "Viết test ConnectionViewModel trong composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt"
Task: "Định nghĩa ConnectionStatus trong composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt"
```

**US2 — Truy vấn JQL và bảng kết quả**

```bash
# Chạy cùng lúc T031, T032, T033, T034, T035, T036:
Task: "Viết test TicketMapper trong composeApp/src/commonTest/kotlin/com/jiratools/data/TicketMapperTest.kt"
Task: "Viết test JiraApiClient.searchJql trong composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientSearchTest.kt"
Task: "Viết test JiraApiClient.approximateCount trong composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientCountTest.kt"
Task: "Viết test QueryViewModel trong composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelTest.kt"
Task: "Định nghĩa Ticket và QueryPage trong composeApp/src/commonMain/kotlin/com/jiratools/domain/Ticket.kt"
Task: "Định nghĩa QueryState trong composeApp/src/commonMain/kotlin/com/jiratools/domain/QueryState.kt"
```

**US3 — Tùy biến bảng**

```bash
# Chạy cùng lúc T044, T045, T046:
Task: "Viết test JqlOrderBy trong composeApp/src/commonTest/kotlin/com/jiratools/domain/JqlOrderByTest.kt"
Task: "Viết test tùy biến bảng trong composeApp/src/commonTest/kotlin/com/jiratools/ui/query/QueryViewModelPreferencesTest.kt"
Task: "Triển khai JqlOrderBy trong composeApp/src/commonMain/kotlin/com/jiratools/domain/JqlOrderBy.kt"
```

**US4 — Mở ticket trong Jira**

```bash
# Chạy cùng lúc T050, T051, T052:
Task: "Viết test BrowseUrl trong composeApp/src/commonTest/kotlin/com/jiratools/domain/BrowseUrlTest.kt"
Task: "Viết test TicketOpener trong composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TicketOpenerTest.kt"
Task: "Triển khai BrowseUrl trong composeApp/src/commonMain/kotlin/com/jiratools/domain/BrowseUrl.kt"
```

---

## Implementation Strategy

### MVP trước (khuyến nghị)

**MVP = Phase 1 + Phase 2 + US1 + US2** (T001–T043).

Lý do: US1 và US2 đều là P1 và chỉ khi có cả hai thì ứng dụng mới mang lại giá trị cốt lõi — cấu hình kết nối (US1) là điều kiện bắt buộc để chạy truy vấn (US2), và US2 mới là tính năng người dùng cần. Nếu buộc phải cắt nhỏ hơn nữa, US1 vẫn là phần gia tăng đầu tiên kiểm chứng được độc lập.

### Gia tăng sau MVP

1. **US3 (P2)**: thêm tùy biến cột và sắp xếp — không thay đổi hành vi của MVP.
2. **US4 (P3)**: thêm mở ticket trong trình duyệt — độc lập với US3.
3. **Polish**: chạy cổng chất lượng, kiểm chứng quickstart, đo SC-005/SC-006.

### Chiến lược triển khai song song

Sau khi Phase 2 xong, một nhóm có thể làm US1–US2 (chuỗi phụ thuộc chính) trong khi nhóm khác chuẩn bị test/khung cho US3–US4. Các task `[P]` trong cùng phase có thể giao cho nhiều người vì không trùng file.

---

## Notes

- Đánh dấu task hoàn thành bằng cách đổi `- [ ]` thành `- [x]` ngay trong file này.
- Mỗi task kèm đường dẫn file cụ thể để có thể thực thi mà không cần thêm ngữ cảnh.
- Test PHẢI viết trước và chạy thất bại (Red) rồi mới triển khai (Green) — Hiến pháp Nguyên tắc II là NON-NEGOTIABLE.
- Không test tự động nào được gọi Jira thật; mọi tương tác HTTP dùng `MockEngine`.
- `commonMain` KHÔNG được dùng API chỉ có trên JVM; mọi mã nền tảng đi qua `platform/` (Nguyên tắc I).
- ViewModel phơi `StateFlow` và tách khỏi Compose; UI không chứa quy tắc nghiệp vụ (Nguyên tắc III).
- Mọi phụ thuộc khai báo trong `gradle/libs.versions.toml`, không ghim phiên bản trong build script (Nguyên tắc IV).
- Không thêm module/abstraction mới ngoài phạm vi plan.md (Nguyên tắc V).
- Cổng chất lượng trước khi coi là hoàn thành: `./gradlew build allTests ktlintCheck` sạch và KB-1..KB-5 đạt.
- Danh mục task có thể cập nhật khi phát hiện khoảng trống (xem T061).
