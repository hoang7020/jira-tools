# Implementation Plan: Cấu hình kết nối Jira trong Cài đặt và tái sử dụng token đã lưu

**Branch**: `004-jira-connection-settings` | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-jira-connection-settings/spec.md`

## Summary

Đưa toàn bộ phần khai báo kết nối Jira vào một màn hình **Cài đặt** trong cửa sổ chính, thay chỗ cho
biểu mẫu đang chiếm cố định phần trên màn hình làm việc; màn hình chính chỉ còn **một dòng trạng thái
ngắn** kèm lối vào Cài đặt. Khi mở ứng dụng với thông tin kết nối đã lưu, ứng dụng hiển thị trạng thái
ngay từ thông tin đã lưu rồi **kiểm tra ngầm** với Jira (không chặn giao diện) và tự cập nhật kết quả.
Token đã lưu được tái sử dụng cho mọi lời gọi Jira, người dùng không phải nhập lại; Cài đặt cho biết
đã có token nhưng không bao giờ hiển thị lại giá trị, và có thao tác **Ngắt kết nối** để xoá sạch
thông tin đã lưu (kèm xác nhận).

Hướng kỹ thuật: mở rộng `ConnectionStatus` thêm trạng thái "có thông tin đã lưu nhưng chưa xác minh
được", bổ sung hai luồng đọc/ghi mới ở `ConnectionRepository` (`verifySaved`, `disconnect`), chuyển
`ConnectionDraft` vào `ConnectionViewModel` để điền sẵn thông tin đã lưu, và thay cơ chế thu gọn biểu
mẫu của tính năng 002 bằng dòng trạng thái một dòng + chế độ xem "Cài đặt". Không thêm phụ thuộc mới.

## Technical Context

**Language/Version**: Kotlin 2.4.21 (Kotlin Multiplatform), JVM target 17

**Primary Dependencies**: Compose Multiplatform 1.12.2, kotlinx-coroutines 1.11.0, kotlinx-serialization,
Ktor client 3.6.1 (dùng lại `JiraApiClient`), java-keyring (kho bí mật hệ điều hành) — **không thêm thư viện nào**

**Storage**: `config.json` (địa chỉ Jira, email, tuỳ chọn người dùng) qua `AppConfigStore`; API token
trong kho bí mật hệ điều hành qua `SecureStorage` (`JvmSecureStorage`); danh mục bảng đã lưu của 003
giữ nguyên

**Testing**: `kotlin.test` + `kotlinx-coroutines-test` + `ktor-client-mock` trong `commonTest`, dùng
`FakeAppPaths`, `FakeSecureStorage`, `TestDispatcherProvider`; không test nào gọi Jira thật

**Target Platform**: Desktop (JVM) — macOS/Windows/Linux; UI Compose Multiplatform

**Project Type**: Ứng dụng desktop một module KMP (`composeApp`) với `commonMain` / `jvmMain` / `commonTest`

**Performance Goals**: dòng trạng thái kết nối hiển thị tức thì khi mở ứng dụng (chỉ đọc tệp cấu hình,
không chờ mạng, không chờ kho bí mật); mở Cài đặt phản hồi dưới 1 giây (SC-008); kiểm tra ngầm chạy
nền và không chặn thao tác (SC-009)

**Constraints**: không chặn luồng UI (Nguyên tắc IV); token không bao giờ ở dạng văn bản thường, không
vào thông báo lỗi hay nhật ký (FR-006, SC-003); không đổi phụ thuộc hay phiên bản (Nguyên tắc IV); mọi
hành vi hiện có của 001/002/003 giữ nguyên ngoài phần được thay thế có chủ đích (FR-020)

**Scale/Scope**: một tài khoản Jira cho mỗi máy; một màn hình Cài đặt với một mục "Kết nối Jira";
khoảng 6 tệp nguồn sửa + 3 tệp nguồn mới + 3 tệp test mới

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Kết quả | Căn cứ |
|-----------|---------|--------|
| I. Đa nền tảng trước, phát hành trên Desktop | PASS | Luồng mới nằm trong `commonMain` (`ConnectionRepository`, `ConnectionViewModel`, màn hình Cài đặt Compose); phần chạm hệ điều hành vẫn đi qua ranh giới đã có `SecureStorage` (`expect`/`interface` + `actual` JVM). Không thêm API chỉ có trên JVM vào `commonMain` |
| II. Kiểm thử trước (NON-NEGOTIABLE) | PASS | Viết trước cho: hàm thuần chuyển trạng thái thành câu chữ, `verifySaved`, `disconnect`, luồng `ConnectionViewModel` (điền sẵn, token đã lưu, ngắt kết nối), và ảnh hưởng lên `SavedTablesViewModel` khi trạng thái là "chưa xác minh được". Kiểm tra ngầm dùng `MockEngine`, không gọi Jira thật |
| III. Chia sẻ logic, chia sẻ UI có chọn lọc | PASS | Trạng thái kết nối, draft, `hasSavedConnection`, `tokenSaved`, thông báo nằm trong `ConnectionViewModel` phơi qua `StateFlow`; Compose chỉ render và phát sự kiện. Câu chữ trạng thái là hàm thuần có test |
| IV. Kotlin chuẩn mực, an toàn kiểu | PASS | Coroutines/Flow cho mọi thao tác; `sealed interface` cho trạng thái; giữ `kotlinx.serialization`; không thêm phụ thuộc, không ghim phiên bản mới |
| V. Đơn giản và gia tăng (YAGNI) | PASS | Không thêm hồ sơ kết nối, không OAuth/SSO, không bộ đếm hết hạn token, không cửa sổ riêng. Loại bỏ mã chết của 002 (cơ chế thu gọn biểu mẫu) thay vì để lại không dùng |

**Supersession có chủ đích (ghi nhận theo Nguyên tắc V)**: tính năng này bỏ cơ chế thu gọn/mở rộng
biểu mẫu kết nối theo chiều cao cửa sổ của 002 (FR-004, FR-007 của 002) vì biểu mẫu không còn nằm
trên màn hình chính. `ui/connection/ConnectionArea.kt`, `ui/layout/ConnectionAreaMode.kt` và test
`ConnectionAreaModeTest.kt` được xoá cùng hành vi đó; `AppWindowDefaults.MIN_SIZE`/`RESTORE_SIZE` vẫn
được giữ vì `Main.kt` còn dùng. Hệ quả: nhóm test của riêng cơ chế thu gọn không còn tồn tại để chạy,
mọi test còn lại của 001/002/003 phải tiếp tục đạt (FR-020, SC-005).

**Kết quả**: PASS — không có vi phạm cần biện minh (mục Complexity Tracking để trống).

## Project Structure

### Documentation (this feature)

```text
specs/004-jira-connection-settings/
├── plan.md              # Tệp này
├── research.md          # Phase 0: quyết định kỹ thuật
├── data-model.md        # Phase 1: trạng thái, chuyển trạng thái, trường dữ liệu
├── quickstart.md        # Phase 1: kịch bản kiểm chứng thủ công + lệnh tự động
├── contracts/           # Phase 1: hợp đồng kho dữ liệu, ViewModel, giao diện
│   └── connection-settings.md
├── checklists/
│   └── requirements.md  # Đã tạo ở bước đặc tả
└── tasks.md             # Phase 2 (/speckit-tasks - chưa tạo)
```

### Source Code (repository root)

```text
composeApp/src/
├── commonMain/kotlin/com/jiratools/
│   ├── app/
│   │   └── App.kt                          # SỬA: bỏ vùng kết nối + BoxWithConstraints, thêm dòng trạng thái,
│   │                                       #       chế độ xem "Cài đặt", gọi loadSaved() rồi verifySaved()
│   ├── data/
│   │   └── ConnectionRepository.kt         # SỬA: loadSaved() trả trạng thái chưa xác minh;
│   │                                       #       THÊM savedInfo(), verifySaved(), disconnect()
│   ├── domain/
│   │   ├── ConnectionStatus.kt             # SỬA: THÊM Unverified(baseUrl, email)
│   │   └── SavedConnectionInfo.kt          # MỚI: địa chỉ, email, tokenSaved (chỉ để đọc)
│   └── ui/
│       ├── connection/
│       │   ├── ConnectionForm.kt           # MỚI: biểu mẫu kết nối (không còn nút thu gọn) + chuyển
│       │   │                               #       `ConnectionDraft` từ ConnectionArea.kt sang đây,
│       │   │                               #       chỉ báo "đã có token được lưu", nút Ngắt kết nối
│       │   ├── ConnectionStatusText.kt     # MỚI: hàm thuần trạng thái -> câu chữ dùng ở hai nơi
│       │   ├── ConnectionViewModel.kt      # SỬA: draft, hasSavedConnection, tokenSaved, notice; verifySaved(); disconnect()
│       │   ├── ConnectionScreen.kt         # XOÁ (thay bằng ConnectionForm.kt)
│       │   └── ConnectionArea.kt           # XOÁ (biểu mẫu rời màn hình chính)
│       ├── layout/
│       │   ├── AppWindowDefaults.kt        # SỬA: bỏ TITLE_BAR_ALLOWANCE (chỉ phục vụ cơ chế thu gọn)
│       │   └── ConnectionAreaMode.kt       # XOÁ (cơ chế thu gọn không còn dùng)
│       ├── settings/
│       │   └── SettingsScreen.kt           # MỚI: màn hình Cài đặt (mục Kết nối Jira + xác nhận ngắt kết nối)
│       └── saved/
│           └── SavedTablesViewModel.kt     # SỬA: cho phép làm mới khi trạng thái chưa xác minh được
└── commonTest/kotlin/com/jiratools/
    ├── data/
    │   └── ConnectionRepositoryTest.kt     # SỬA + THÊM ca cho verifySaved(), disconnect(), savedInfo()
    ├── platform/
    │   └── Fakes.kt                        # SỬA (nếu cần): thêm bộ đếm/ghi nhận cho ca ngắt kết nối
    └── ui/
        ├── connection/
        │   ├── ConnectionAreaModeTest.kt   # XOÁ (hành vi bị thay thế)
        │   ├── ConnectionStatusTextTest.kt # MỚI (viết trước)
        │   └── ConnectionViewModelTest.kt  # SỬA + THÊM ca điền sẵn, token đã lưu, ngắt kết nối
        └── saved/
            └── SavedTablesViewModelTest.kt # SỬA: ca làm mới với trạng thái chưa xác minh được
```

**Structure Decision**: giữ nguyên cấu trúc một module `composeApp` với ba source set `commonMain`,
`jvmMain`, `commonTest` (Nguyên tắc V — không tách module mới). Toàn bộ logic mới nằm trong
`commonMain`; chỉ `jvmMain` hiện có (`JvmSecureStorage`, `JvmAppPaths`) chạm hệ điều hành nên không
phát sinh tệp `actual` mới. Màn hình Cài đặt là một thư mục `ui/settings` mới để sau này có thể thêm
mục khác mà không phải đổi tên.

## Yêu cầu chức năng -> thành phần

| Yêu cầu | Thành phần chính |
|--------|------------------|
| FR-001, FR-018, FR-019 (lối vào Cài đặt, dòng trạng thái) | `app/App.kt`, `ui/connection/ConnectionStatusText.kt` |
| FR-002 (biểu mẫu không còn trên màn hình chính) | `app/App.kt` (bỏ `ConnectionArea`), `ui/settings/SettingsScreen.kt` |
| FR-003, FR-017, FR-022 (trạng thái, kiểm tra ngầm, mất mạng) | `ui/connection/ConnectionViewModel.kt`, `data/ConnectionRepository.kt`, `domain/ConnectionStatus.kt` |
| FR-004, FR-016 (biểu mẫu đầy đủ, kiểm tra trước khi lưu) | `ui/connection/ConnectionForm.kt`, `data/ConnectionRepository.validate` (giữ nguyên hành vi) |
| FR-005, FR-006, FR-007, FR-021 (lưu và tái sử dụng token) | `data/ConnectionRepository.kt` + `SecureStorage` (đã có), `app/AppContainer.kt` (giữ nguyên cách đọc token cho mọi lời gọi) |
| FR-008, FR-009 (chỉ báo token, điền sẵn) | `ui/connection/ConnectionForm.kt`, `ui/connection/ConnectionViewModel.kt` (`savedInfo`, `draft`) |
| FR-010, FR-011 (ngắt kết nối) | `data/ConnectionRepository.disconnect()`, `ui/settings/SettingsScreen.kt` (hộp thoại xác nhận) |
| FR-012 (token bị từ chối) | `data/ConnectionRepository.verifySaved()`, `ui/connection/ConnectionStatusText.kt` |
| FR-013, FR-014, FR-015 (không mất dữ liệu, cập nhật trong phiên) | `ui/connection/ConnectionViewModel.kt`, `app/App.kt` |
| FR-020 (không hồi quy) | Toàn bộ test hiện có + `ui/saved/SavedTablesViewModel.kt` (điều chỉnh điều kiện làm mới) |

## Ghi chú thiết kế

- **Không đọc kho bí mật hai lần**: `loadSaved()` chỉ đọc tệp cấu hình (nhanh, không hỏi quyền hệ
  điều hành); `verifySaved()` mới đọc token và gọi Jira. Ứng dụng gọi tuần tự hai hàm này trong một
  `LaunchedEffect` nên dòng trạng thái có nội dung ngay, còn phần kiểm tra chạy ở phía sau.
- **Điều kiện hiển thị khu vực truy vấn** đổi từ "đã kết nối" sang "không phải chưa cấu hình", nhờ đó
  khi mất mạng người dùng vẫn thử chạy truy vấn được (FR-022) và khi chưa cấu hình thì vẫn thấy hướng dẫn.
- **Điều kiện làm mới bảng đã lưu** (003) mở rộng thêm trạng thái chưa xác minh được, giữ nguyên hành
  vi từ chối khi chưa cấu hình (đã có test).
- **Nhật ký và thông báo**: không thêm chỗ nào ghi token; `SecureStorage` hiện có đã fail-closed khi
  kho bí mật không khả dụng, luồng ngắt kết nối giữ nguyên tính chất đó.

## Complexity Tracking

Không có vi phạm hiến pháp cần biện minh.
