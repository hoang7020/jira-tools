# Tasks: Cấu hình kết nối Jira trong Cài đặt và tái sử dụng token đã lưu

**Feature**: `004-jira-connection-settings` | **Ngày**: 2026-10-31
**Input**: Design documents trong `specs/004-jira-connection-settings/` — `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/connection-settings.md`, `quickstart.md`
**Prerequisites**: plan.md ✔, spec.md ✔, research.md ✔, data-model.md ✔, contracts/ ✔, quickstart.md ✔

## Ghi chú về kiểm thử (BẮT BUỘC — Hiến pháp Nguyên tắc II)

Hiến pháp dự án quy định **Kiểm thử trước (NON-NEGOTIABLE)**: mọi logic chia sẻ trong `commonMain` PHẢI có test trong `commonTest`, viết TRƯỚC khi triển khai và PHẢI thất bại (ĐỎ) trước khi triển khai (XANH).

- Test viết trước cho: hàm thuần câu chữ trạng thái (T004 → T005), `savedInfo()` (T006 → T008), trạng thái ViewModel (T009 → T010), `loadSaved()` + điền sẵn (T012 → T013, T014), `validate()` (T015 → T016), điều kiện làm mới của 003 (T017 → T018), `verifySaved()` (T024 → T025, T026 → T027), `disconnect()` (T030 → T031, T032 → T033).
- Kho bí mật dùng `FakeSecureStorage` (đã có cờ `failOnLoad`/`failOnSave`/`failOnDelete`); mạng dùng `MockEngine` + fixture của 001; **KHÔNG** test nào gọi Jira thật.
- **Không** viết test Compose UI (đồng nhất với 001/002/003): bố cục và tương tác kiểm chứng thủ công bằng KB-1..KB-6 trong `quickstart.md`.
- **KHÔNG** thêm phụ thuộc test mới: chỉ dùng `kotlin.test`, `kotlinx-coroutines-test`, `ktor-client-mock` đã khai báo (Nguyên tắc IV).

## Tổ chức theo User Story

- **US1 (P1)**: Cấu hình kết nối Jira trong màn hình Cài đặt
- **US2 (P2)**: Tái sử dụng token đã lưu ở những lần chạy sau
- **US3 (P3)**: Ngắt kết nối và xoá thông tin đã lưu

Mỗi user story tạo ra một phần gia tăng độc lập: có thể triển khai, kiểm thử và kiểm chứng riêng.

## Định dạng task

`- [ ] T### [P?] [US#?] Mô tả kèm đường dẫn file`

- `[P]`: có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành).
- `[US#]`: chỉ gắn cho task thuộc phase user story (Setup/Foundational/Polish không gắn).
- Task kiểm chứng thủ công ghi rõ "chờ người dùng kiểm chứng" khi cần phiên desktop hoặc tài khoản Jira thật.

## Quy ước đường dẫn (KMP, một module `composeApp`)

- `composeApp/src/commonMain/kotlin/com/jiratools/...` — logic chia sẻ (`app/`, `data/`, `domain/`, `platform/`, `ui/`)
- `composeApp/src/commonTest/kotlin/com/jiratools/...` — test logic chia sẻ
- `composeApp/src/jvmMain/kotlin/com/jiratools/...` — phần `actual`/implementation JVM + entry point (**không sửa** trong tính năng này)
- Cấu hình build: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts` (**không sửa** trong tính năng này)

## Tệp được sửa ở nhiều phase (thứ tự bắt buộc, không chạy song song)

`ui/connection/ConnectionViewModel.kt` (T010 → T014 → T016 → T027 → T033) · `commonTest/ui/connection/ConnectionViewModelTest.kt` (T009 → T012 → T015 → T026 → T032) · `data/ConnectionRepository.kt` (T008 → T013 → T025 → T031) · `commonTest/data/ConnectionRepositoryTest.kt` (T006 → T024 → T030) · `app/App.kt` (T021 → T028) · `ui/settings/SettingsScreen.kt` (T020 → T034) · `ui/saved/SavedTablesViewModel.kt` (T018) và `commonTest/ui/saved/SavedTablesViewModelTest.kt` (T017)

---

## Phase 1: Setup (Xác lập baseline)

**Mục tiêu**: chốt trạng thái nền trước khi thay đổi để phát hiện hồi quy (SC-005) và xác nhận không cần phụ thuộc mới.

- [X] T001 Kiểm chứng baseline trước khi sửa mã: chạy `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` tại gốc repo, xác nhận toàn bộ test hiện có (212 test của 001/002/003) đạt và lint sạch; ghi kết quả vào mục "Ghi kết quả" của `specs/004-jira-connection-settings/quickstart.md`
- [X] T002 [P] Xác nhận không cần phụ thuộc mới: rà `composeApp/build.gradle.kts` và `gradle/libs.versions.toml` — tính năng chỉ dùng Compose Material 3, `kotlinx-coroutines-core`, `ktor-client-*` và `SecureStorage`/`AppPaths` đã có; **KHÔNG** sửa hai tệp cấu hình build

**Checkpoint**: baseline xanh, không thay đổi phụ thuộc — có thể bắt đầu Phase 2.

---

## Phase 2: Foundational (Điều kiện tiên quyết — CHẶN mọi user story)

**Mục tiêu**: trạng thái kết nối mới, câu chữ trạng thái dùng chung, ảnh chụp thông tin đã lưu và bề mặt trạng thái của ViewModel — thứ mà cả US1, US2, US3 đều dùng.

**⚠️ CRITICAL**: không bắt đầu user story nào trước khi phase này xong.

- [X] T003 [P] Thêm trạng thái `Unverified(baseUrl, email)` vào `composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt` (data class, hai trường đã cắt khoảng trắng) kèm KDoc nêu nghĩa "đã có thông tin kết nối trên máy nhưng chưa xác minh được (mới mở, hoặc lỗi mạng)"; **không** đổi bốn trạng thái còn lại
- [X] T004 [P] Viết test trước `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionStatusTextTest.kt` cho hai hàm thuần trong `data-model.md` mục 6 (PHẢI ĐỎ trước T005): `connectionStatusText` và `connectionStatusDetail` với đủ năm trạng thái; `Unconfigured` → "Chưa cấu hình kết nối Jira."; `Validating` → "Đang kiểm tra kết nối tới Jira…"; `Unverified` → "Chưa xác minh được kết nối Jira — kiểm tra mạng."; `Connected` → "Đã kết nối Jira."; `Invalid` → câu ngắn kèm gợi ý mở Cài đặt; mọi câu chữ **KHÔNG** được chứa token hay địa chỉ Jira/email (FR-019, SC-003)
- [X] T005 Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionStatusText.kt` (XANH cho T004): hai hàm thuần theo đúng bảng câu chữ ở `data-model.md` mục 6, `connectionStatusDetail` cho `Connected` dùng `baseUrl`/`email`, cho `Invalid` dùng `error.userMessage()` + gợi ý mở Cài đặt
- [X] T006 [P] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt` với các ca cho `savedInfo()` (PHẢI ĐỎ trước T008): chưa có gì → `baseUrl`/`email` rỗng và `tokenSaved = false`; có `config.json` nhưng kho bí mật trống → hai trường có giá trị và `tokenSaved = false`; có token → `tokenSaved = true`; kho bí mật lỗi đọc được coi như chưa có token (`tokenSaved = false`); kết quả **không** chứa giá trị token
- [X] T007 [P] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedConnectionInfo.kt` theo `data-model.md` mục 2: `data class SavedConnectionInfo(baseUrl: String = "", email: String = "", tokenSaved: Boolean = false)` kèm KDoc "chỉ để đọc — không có luồng nào ghi token từ đây trở lại kho bí mật"
- [X] T008 Triển khai `savedInfo()` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt` (XANH cho T006): đọc `configStore.load()`, cắt khoảng trắng hai trường, `tokenSaved = secureStorage.loadToken() != null`; **KHÔNG** trả về giá trị token và **KHÔNG** ghi gì
- [X] T009 Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt` cho bề mặt trạng thái mới (PHẢI ĐỎ trước T010): `draft` khởi tạo rỗng; `updateDraft` đổi nội dung mà không gọi mạng (MockEngine không nhận request nào); `tokenSaved` ban đầu là `null`; `notice` ban đầu `null`; `clearNotice()` xoá thông báo
- [X] T010 Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt` (XANH cho T009): thêm `draft: StateFlow<ConnectionDraft>`, `tokenSaved: StateFlow<Boolean?>`, `notice: StateFlow<String?>`, `fun updateDraft(draft: ConnectionDraft)`, `fun clearNotice()`; giữ nguyên chữ ký `status`, `loadSaved()`, `validate(...)`
- [X] T011 Kiểm chứng Phase 2: chạy `./gradlew :composeApp:jvmTest :composeApp:ktlintCheck` và ghi kết quả vào mục "Ghi kết quả" của `specs/004-jira-connection-settings/quickstart.md`; xác nhận `ConnectionStatusTextTest` và các ca mới XANH, không lỗi lint

**Checkpoint**: trạng thái mới, câu chữ, ảnh chụp thông tin đã lưu và bề mặt ViewModel sẵn sàng — US1..US3 có thể bắt đầu.

---

## Phase 3: User Story 1 (P1) — Cấu hình kết nối Jira trong màn hình Cài đặt 🎯 MVP

**Mục tiêu**: biểu mẫu kết nối rời khỏi màn hình chính và nằm trong một màn hình "Cài đặt" thứ ba; màn hình chính chỉ còn một dòng trạng thái kèm lối vào Cài đặt; mở Cài đặt thì địa chỉ Jira và email đã lưu được điền sẵn, ô token để trống.

**Tiêu chí kiểm thử độc lập**: xoá thông tin đã lưu, mở ứng dụng → màn hình chính không có ô nhập địa chỉ/email/token, có dòng trạng thái "Chưa cấu hình kết nối Jira." và nút "Cài đặt"; mở Cài đặt, nhập thông tin hợp lệ, kiểm tra → báo kết nối thành công; quay lại màn hình chính → dòng trạng thái "Đã kết nối Jira." và khu vực truy vấn dùng được (KB-1).

**FR liên quan**: FR-001, FR-002, FR-003, FR-004, FR-009, FR-016, FR-018, FR-019, FR-020, FR-021

### Kiểm thử cho User Story 1 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T012 [P] [US1] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt` cho **nạp cấu hình đã lưu** (PHẢI thất bại trước T013/T014, XANH sau T014): `loadSaved()` khi `config.json` có địa chỉ + email → `Unverified(baseUrl, email)` (**không** phải `Connected`) và `draft` được điền sẵn hai trường đó với ô token rỗng; khi chưa có gì → `Unconfigured` và `draft` rỗng; `loadSaved()` **không** đọc kho bí mật (dựng `FakeSecureStorage(failOnLoad = true)` và xác nhận kết quả vẫn là `Unverified` chứ không phải `Invalid`, nghĩa là `loadToken()` đã không được gọi)
- [X] T013 [US1] Sửa `loadSaved()` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt`: có đủ địa chỉ + email → `Unverified(baseUrl, email)`; thiếu → `Unconfigured`; giữ nguyên đặc tính **không** chạm kho bí mật
- [X] T014 [US1] Sửa `loadSaved()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt` (XANH cho T012): cập nhật `status` và điền sẵn `draft` (chỉ địa chỉ + email; **không** điền token, **không** xoá nội dung người dùng đang gõ dở nếu đã có)
- [X] T015 [P] [US1] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt` cho **kiểm tra kết nối** (PHẢI thất bại trước T016): `validate` thành công → `Connected`, `tokenSaved = true`, token bị xoá khỏi `draft` sau khi lưu, địa chỉ trong `draft` là địa chỉ đã chuẩn hoá; `validate` thất bại → `Invalid` và nội dung `config.json` + danh sách token đã lưu **không đổi** (FR-013, FR-016)
- [X] T016 [US1] Sửa `validate(...)` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt` (XANH cho T015): sau thành công đặt `tokenSaved = true`, cập nhật `draft` bằng địa chỉ/email đã chuẩn hoá và xoá token khỏi `draft`; sau thất bại giữ nguyên thông tin đã lưu (không đổi hành vi hiện có ngoài hai điểm này)
- [X] T017 [P] [US1] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` cho **điều kiện làm mới** (PHẢI thất bại trước T018): `onConnectionStatusChanged(Unverified(...))` rồi `refresh(id)` → thực hiện lời gọi và thay thế dữ liệu; `Unconfigured` vẫn bị từ chối không gọi mạng (giữ nguyên ca cũ)
- [X] T018 [US1] Sửa điều kiện làm mới trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` (XANH cho T017): chấp nhận mọi trạng thái khác `Unconfigured` (thay cho điều kiện `Connected`), giữ nguyên hành vi từ chối và thông báo khi chưa cấu hình
- [X] T019 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionForm.kt`: chuyển khai báo `ConnectionDraft` từ `ConnectionArea.kt` sang tệp này, và dựng biểu mẫu kết nối theo `contracts/connection-settings.md` mục 4 — ô "Địa chỉ Jira", "Email", "API token" (luôn rỗng khi mở, có nút Hiện/Ẩn), chỉ báo token ba trạng thái ("Đã có token được lưu trên máy." / "Chưa có token được lưu." / "Đang kiểm tra…" khi `tokenSaved == null`), nút "Kiểm tra kết nối" có tiến trình khi đang chạy, vùng thông báo dùng `connectionStatusDetail(status)` + `notice`; **không** còn nút "Thu gọn"
- [X] T020 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt`: màn hình "Cài đặt" với mục "Kết nối Jira" chứa `ConnectionForm` (chưa có nút Ngắt kết nối), nhận `status`, `draft`, `tokenSaved`, `notice`, `onDraftChange`, `onValidate`, `onDismissNotice`; bố trí để sau này thêm mục khác
- [X] T021 [US1] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: bỏ `ConnectionArea` và `BoxWithConstraints` cùng `areaChoice`/`areaMode`/`formMaxHeight`; thêm dòng trạng thái một dòng (`connectionStatusText(status)`) kèm nút "Cài đặt"; mở rộng `AppViewMode` thêm mục "Cài đặt" và render `SettingsScreen`; điều kiện hiển thị khu vực truy vấn đổi từ `status is Connected` sang `status !is Unconfigured`; **giữ nguyên** bố cục và hành vi của "Truy vấn" và "Bảng đã lưu" (FR-015, FR-019, FR-020, FR-022)
- [X] T022 [US1] Xoá mã của cơ chế thu gọn đã bị thay thế: `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionArea.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionScreen.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/ConnectionAreaMode.kt`, `composeApp/src/commonTest/kotlin/com/jiratools/ui/layout/ConnectionAreaModeTest.kt`; gỡ `TITLE_BAR_ALLOWANCE` khỏi `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/AppWindowDefaults.kt` (giữ `MIN_SIZE`/`RESTORE_SIZE` vì `Main.kt` còn dùng); xác nhận không còn tham chiếu nào tới các tên đã xoá
- [ ] T023 [US1] Kiểm chứng thủ công KB-1 trong `specs/004-jira-connection-settings/quickstart.md` bằng `./gradlew :composeApp:run`: màn hình chính không còn biểu mẫu kết nối, dòng trạng thái + nút "Cài đặt" hoạt động, nhập thông tin và kiểm tra kết nối thành công, quay lại "Truy vấn" thấy trạng thái đã kết nối — **chờ người dùng kiểm chứng** (cần phiên desktop và tài khoản Jira thật); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1 hoàn tất và kiểm chứng độc lập được (MVP).

---

## Phase 4: User Story 2 (P2) — Tái sử dụng token đã lưu ở những lần chạy sau

**Mục tiêu**: khi mở ứng dụng mà đã có thông tin kết nối, trạng thái hiển thị ngay từ thông tin đã lưu rồi kiểm tra ngầm với Jira (không chặn giao diện) và tự cập nhật; token đã lưu được dùng cho mọi lời gọi nên người dùng không phải nhập lại; khi token bị từ chối thì được hướng dẫn nhập lại đúng chỗ.

**Tiêu chí kiểm thử độc lập**: kết nối thành công rồi đóng và mở lại ứng dụng: dòng trạng thái có nội dung ngay, tự chuyển thành "Đã kết nối Jira." sau khi kiểm tra ngầm xong; chạy truy vấn không cần mở Cài đặt; tệp `config.json` không chứa token; ngắt mạng rồi mở lại → "Chưa xác minh được kết nối Jira — kiểm tra mạng." mà vẫn chạy truy vấn được; token bị thu hồi → thông báo nêu rõ cần nhập lại token trong Cài đặt (KB-2, KB-3, KB-4).

**FR liên quan**: FR-005, FR-006, FR-007, FR-008, FR-012, FR-017, FR-018, FR-021, FR-022

### Kiểm thử cho User Story 2 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T024 [P] [US2] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt` cho `verifySaved()` theo `contracts/connection-settings.md` mục 2 (PHẢI thất bại trước T025): thiếu địa chỉ/email hoặc thiếu token → `Unconfigured`; `/myself` 200 → `Connected(baseUrl, email)`; 401 → `Invalid(JiraError.AuthFailed)`; 403 → `Invalid(JiraError.PermissionDenied)`; lỗi mạng/timeout → `Unverified(baseUrl, email)`; trong **mọi** nhánh, nội dung `config.json` và danh sách token đã lưu **không đổi** (FR-013, FR-017, FR-022)
- [X] T025 [US2] Triển khai `verifySaved()` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt` (XANH cho T024): dùng một snapshot nội bộ dùng chung với `savedInfo()` để đọc cấu hình/token đúng một lần mà không đưa token ra ngoài; gọi `apiClient.myself(explicit = credentials)`; ánh xạ lỗi theo bảng ở `contracts/connection-settings.md` mục 2 (chỉ `AuthFailed`/`PermissionDenied` → `Invalid`, các lỗi còn lại → `Unverified`); **chỉ đọc**, không ghi `config.json`, không ghi/xoá token
- [X] T026 [P] [US2] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt` cho luồng kiểm tra ngầm (PHẢI thất bại trước T027): `verifySaved()` đặt `Validating` trong lúc chờ rồi kết thúc ở trạng thái cuối; `tokenSaved` phản ánh chính xác kho bí mật (kể cả token mồ côi), lỗi mạng cho `Unverified` và **không** chặn (không ném ngoại lệ); kho bí mật lỗi cũng không làm sập ứng dụng
- [X] T027 [US2] Triển khai `verifySaved()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt` (XANH cho T026): đặt `Validating`, gọi `verifySavedWithInfo()` qua `dispatcher` để tái sử dụng một lần đọc cấu hình/token, cập nhật `status`/`tokenSaved`/`hasSavedConnection`, bắt mọi lỗi bằng cơ chế guard hiện có
- [X] T028 [US2] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: trong `LaunchedEffect(Unit)` gọi `connectionViewModel.loadSaved()` rồi khởi chạy `connectionViewModel.verifySaved()` **theo thứ tự** (hiển thị trạng thái trước, kiểm tra ngầm không chặn nạp dữ liệu cục bộ, tối đa một lần cho mỗi lần mở ứng dụng); khi trạng thái là `Invalid`, dòng trạng thái nêu lý do ngắn kèm gợi ý mở Cài đặt (FR-012, FR-017, SC-009)
- [ ] T029 [US2] Kiểm chứng thủ công KB-2, KB-3, KB-4 trong `specs/004-jira-connection-settings/quickstart.md` bằng `./gradlew :composeApp:run`: tái sử dụng token sau khi khởi động lại (tệp `config.json` không chứa token, Keychain còn mục `jira-tools`), mở ứng dụng khi mất mạng → "chưa xác minh được" và vẫn truy vấn được, thu hồi token → thông báo nêu rõ cần nhập lại, thay token mới → dùng được ở lần chạy sau — **chờ người dùng kiểm chứng** (cần phiên desktop, mạng và tài khoản Jira thật); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1 và US2 cùng hoạt động độc lập; token đã lưu được tái sử dụng và trạng thái tự cập nhật.

---

## Phase 5: User Story 3 (P3) — Ngắt kết nối và xoá thông tin đã lưu

**Mục tiêu**: người dùng bấm "Ngắt kết nối" trong Cài đặt và xác nhận; ứng dụng xoá token cùng địa chỉ/email đã lưu, giữ nguyên tuỳ chọn truy vấn và bảng đã lưu, rồi trở về trạng thái chưa cấu hình.

**Tiêu chí kiểm thử độc lập**: với kết nối đang hoạt động, bấm "Ngắt kết nối" → huỷ thì không đổi gì; xác nhận thì `config.json` không còn `baseUrl`/`email` nhưng vẫn giữ `lastJql`/`visibleColumns`/`sort`, mục `jira-tools` biến mất khỏi Keychain, dòng trạng thái về "Chưa cấu hình kết nối Jira.", truy vấn và làm mới bị từ chối kèm hướng dẫn, bảng đã lưu vẫn mở xem được (KB-5).

**FR liên quan**: FR-010, FR-011, FR-013, FR-015

### Kiểm thử cho User Story 3 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

- [X] T030 [P] [US3] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt` cho `disconnect()` (PHẢI thất bại trước T031): xoá token khỏi kho bí mật **rồi** xoá `baseUrl`/`email` trong `config.json` nhưng **giữ nguyên** `lastJql`, `visibleColumns`, `sort`, `schemaVersion`; idempotent khi chưa có gì (không ném ngoại lệ); khi dựng `FakeSecureStorage(failOnDelete = true)` → `disconnect()` ném ngoại lệ và `config.json` **không** bị thay đổi (không có trạng thái nửa vời); trả về `Unconfigured`
- [X] T031 [US3] Triển khai `disconnect()` trong `composeApp/src/commonMain/kotlin/com/jiratools/data/ConnectionRepository.kt` (XANH cho T030): `secureStorage.deleteToken()` trước, sau đó `configStore.save(preferences.copy(baseUrl = null, email = null))`, trả `Unconfigured`; để ngoại lệ từ kho bí mật/tệp cấu hình nổi lên cho tầng giao diện
- [X] T032 [P] [US3] Mở rộng `composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt` cho ngắt kết nối (PHẢI thất bại trước T033): `disconnect()` thành công → `status = Unconfigured`, `tokenSaved = false`, `draft` rỗng, `notice` nêu đã ngắt kết nối; kho bí mật lỗi → `status` giữ nguyên và `notice` nêu lý do, không ném ngoại lệ ra giao diện
- [X] T033 [US3] Triển khai `disconnect()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionViewModel.kt` (XANH cho T032): gọi repository qua `dispatcher`; khi thành công cập nhật `status`/`tokenSaved`/`draft`/`notice`, khi lỗi giữ nguyên trạng thái và dùng `guard` để hiển thị lý do
- [X] T034 [US3] Mở rộng `composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt`: nút "Ngắt kết nối" (chỉ bật khi `hasSavedConnection`, kể cả token mồ côi) và hộp thoại xác nhận nêu rõ token cùng địa chỉ/email sẽ bị xoá và thao tác không hoàn tác được; huỷ không thay đổi gì (FR-010)
- [ ] T035 [US3] Kiểm chứng thủ công KB-5 trong `specs/004-jira-connection-settings/quickstart.md` bằng `./gradlew :composeApp:run`: huỷ hộp thoại xác nhận, xác nhận ngắt kết nối, kiểm tra `config.json` và Keychain, kiểm tra truy vấn/làm mới bị từ chối kèm hướng dẫn, bảng đã lưu vẫn xem được — **chờ người dùng kiểm chứng** (cần phiên desktop và tài khoản Jira thật); ghi kết quả vào `quickstart.md`

**Checkpoint**: cả ba user story hoạt động độc lập và đầy đủ.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Mục tiêu**: chốt cổng chất lượng của hiến pháp, xác nhận không hồi quy và cập nhật tài liệu kiểm chứng.

- [X] T036 Chạy cổng chất lượng đầy đủ tại gốc repo: `./gradlew build ktlintCheck` và sau đó `./gradlew build ktlintCheck --rerun-tasks`; xác nhận build Desktop thành công, toàn bộ 234 test đạt, lint sạch (SC-005); ghi kết quả vào mục "Ghi kết quả" của `specs/004-jira-connection-settings/quickstart.md`
- [X] T037 [P] Rà soát tuân thủ 5 nguyên tắc hiến pháp trên các tệp đã thay đổi: `commonMain` không import `java.*`; `gradle/libs.versions.toml` và `composeApp/build.gradle.kts` không đổi; không có chuỗi token thử nghiệm trong các tệp trạng thái/kết nối hay thông báo lỗi; các tệp được bảo vệ không bị sửa. Workspace không có Git metadata nên không thể tạo diff; đã rà mã nguồn trực tiếp và ghi rõ giới hạn này trong `quickstart.md` (FR-020)
- [ ] T038 [P] Kiểm chứng không hồi quy và bố cục mới: chạy KB-6 và lặp lại các kịch bản chính của `../../001-jql-ticket-table/quickstart.md` (chạy truy vấn, tải thêm, sắp xếp, ẩn/hiện cột, mở ticket) và `../../003-saved-result-table/quickstart.md` (xem bảng đã lưu, làm mới) — **chờ người dùng kiểm chứng** (cần phiên desktop, mạng và tài khoản Jira thật); ghi kết quả vào `quickstart.md`
- [X] T039 Cập nhật `specs/004-jira-connection-settings/quickstart.md` mục "Ghi kết quả": ghi trạng thái tự động đã chạy, để KB-1..KB-6 là chưa kiểm chứng vì cần Desktop/Jira thật, và ghi các hiệu chỉnh thiết kế (snapshot dùng chung, `hasSavedConnection`, xác minh nền)
- [X] T040 Đối chiếu FR-001..FR-022 và SC-001..SC-009 trong `specs/004-jira-connection-settings/spec.md` với mã đã triển khai và test tương ứng; ghi bảng truy vết vào mục "Ghi kết quả" của `quickstart.md`

---

## Dependencies & Execution Order

### Phụ thuộc giữa các phase

- **Setup (Phase 1)**: không phụ thuộc — bắt đầu ngay (T001, T002)
- **Foundational (Phase 2)**: sau Setup — CHẶN mọi user story (T003–T010; T011 là cổng kiểm chứng)
- **User Story (Phase 3..5)**: sau Foundational; US3 dùng lại `ConnectionViewModel`/`SettingsScreen` của US1 nên thực tế chạy sau US1
- **Polish (Phase 6)**: sau khi các user story mong muốn hoàn tất

### Phụ thuộc giữa các user story

- **US1 (P1)**: sau Foundational — không phụ thuộc story khác. Là MVP. Tạo `ConnectionForm`, `SettingsScreen`, dòng trạng thái và điều kiện hiển thị khu vực truy vấn mới
- **US2 (P2)**: sau Foundational; **mở rộng** `ConnectionRepository.kt` (T025) và `ConnectionViewModel.kt` (T027) nên chạy sau T013/T014/T016; sửa `app/App.kt` (T028) sau T021 (cùng tệp)
- **US3 (P3)**: sau Foundational; mở rộng `ConnectionRepository.kt` (T031), `ConnectionViewModel.kt` (T033) và `SettingsScreen.kt` (T034) nên chạy sau các task tương ứng của US1/US2

### Trong mỗi user story

- Test viết trước PHẢI ĐỎ rồi mới triển khai (T004 → T005; T006 → T008; T009 → T010; T012 → T013/T014; T015 → T016; T017 → T018; T024 → T025; T026 → T027; T030 → T031; T032 → T033)
- Hàm thuần trước, tầng dữ liệu sau, giao diện cuối (T005 → T008 → T010 → T019 → T020 → T021)
- Kiểm chứng thủ công ngay sau khi story hoàn tất, trước khi chuyển story kế tiếp

### Cơ hội chạy song song

- T002 song song với T001; trong Phase 2: T003, T004, T006 song song (khác tệp); T009 sau T010 không thể — T009 (test) và T010 (impl) tuần tự nhưng song song được với T007
- Trong US1: T012 và T015 cùng sửa `ConnectionViewModelTest.kt` nên **không** song song; T012 song song được với T017 (khác tệp), T019 và T020 song song được với T017/T018
- Trong US2: T024 (sửa `ConnectionRepositoryTest.kt`) và T026 (sửa `ConnectionViewModelTest.kt`) song song
- Trong US3: T030 và T032 song song; T034 (sửa `SettingsScreen.kt`) song song với T030/T031
- Polish: T037 và T038 song song

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Ba tệp khác nhau, không phụ thuộc nhau:
Task: "T003 thêm Unverified vào composeApp/src/commonMain/kotlin/com/jiratools/domain/ConnectionStatus.kt"
Task: "T004 viết test trước composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionStatusTextTest.kt"
Task: "T006 mở rộng composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt cho savedInfo()"

# Sau T006, có thể làm song song:
Task: "T007 tạo composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedConnectionInfo.kt"
```

---

## Parallel Example: User Story 3

```bash
# Hai tệp test khác nhau, viết trước cùng lúc:
Task: "T030 composeApp/src/commonTest/kotlin/com/jiratools/data/ConnectionRepositoryTest.kt"
Task: "T032 composeApp/src/commonTest/kotlin/com/jiratools/ui/connection/ConnectionViewModelTest.kt"

# Song song với chuỗi trên vì khác tệp:
Task: "T034 mở rộng composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt"
```

---

## Implementation Strategy

### MVP trước (chỉ US1)

1. Hoàn tất Phase 1 (Setup)
2. Hoàn tất Phase 2 (Foundational — bắt buộc, chặn mọi story)
3. Hoàn tất Phase 3 (US1): T012 → T023
4. **DỪNG và KIỂM CHỨNG** KB-1 độc lập

### Giao hàng gia tăng

1. Setup + Foundational → trạng thái mới, câu chữ, ảnh chụp thông tin đã lưu sẵn sàng
2. US1 → kiểm chứng KB-1 → demo (biểu mẫu nằm trong Cài đặt, màn hình chính chỉ còn một dòng trạng thái)
3. US2 → kiểm chứng KB-2/KB-3/KB-4 → demo (mở ứng dụng là dùng được ngay, trạng thái tự cập nhật, mất mạng không chặn)
4. US3 → kiểm chứng KB-5 → demo (ngắt kết nối xoá sạch thông tin đã lưu)
5. Polish → cổng chất lượng, KB-6, cập nhật `quickstart.md`
6. Mỗi story tăng giá trị mà không phá story trước (SC-005)

### Chiến lược nhóm song song

1. Nhóm hoàn tất Setup + Foundational cùng nhau
2. Sau Foundational: Dev A → US1 (T012–T023), Dev B chuẩn bị T024/T026 (test của US2) song song vì khác tệp
3. Sau khi US1 xong: Dev B → US2 (T025–T029), Dev C → US3 (T030–T034) sau khi T025/T027 có mặt
4. Kiểm chứng thủ công gom theo story, chạy khi có phiên desktop

---

## Notes

- `[P]` = khác tệp, không phụ thuộc task chưa xong
- `[US#]` dùng để truy vết task theo user story trong `spec.md`
- Mọi task đều ghi rõ đường dẫn tệp cụ thể; không có task mơ hồ
- Bảy tệp dùng chung nhiều phase PHẢI sửa tuần tự theo thứ tự đã ghi ở mục "Tệp được sửa ở nhiều phase"
- Không đổi phụ thuộc/phiên bản; không thêm thư viện test; không viết test Compose UI; không sửa `jvmMain`, `data/JiraApiClient.kt`, `data/JiraErrorMapper.kt`, `data/AppConfigStore.kt`, `platform/SecureStorage.kt`, `platform/AppPaths.kt`, `ui/query/*`, `ui/saved/SavedTablesArea.kt`, `ui/saved/SavedTableDialogs.kt`
- Ràng buộc cần nhắc nguyên văn khi triển khai: `loadSaved()` **không** chạm kho bí mật; `verifySaved()` **chỉ đọc** và ánh xạ "chỉ `AuthFailed`/`PermissionDenied` → `Invalid`, mọi lỗi còn lại → `Unverified`"; `tokenSaved = loadToken() != null` và **không** bao giờ trả về giá trị token; ngắt kết nối xoá token **rồi** mới xoá `baseUrl`/`email`, giữ `lastJql`/`visibleColumns`/`sort`/`schemaVersion`; điều kiện dùng Jira là "trạng thái khác `Unconfigured`"; mỗi lần mở ứng dụng chỉ chạy tối đa một lần kiểm tra ngầm
- Sau mỗi task hoặc nhóm task logic: ghi kết quả kiểm chứng vào `quickstart.md`
