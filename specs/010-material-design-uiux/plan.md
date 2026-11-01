# Implementation Plan: Nâng cấp UI/UX theo Material Design

**Branch**: `010-material-design-uiux` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả tính năng từ `specs/010-material-design-uiux/spec.md`

## Summary

Chuẩn hoá giao diện truy vấn, cài đặt, bảng đã lưu và calendar theo Material Design 3, ưu tiên hệ thống điều hướng Desktop bằng thanh bên trái có thể thu gọn, cấu trúc nội dung và trạng thái dễ nhận biết, đồng thời hỗ trợ theme sáng/tối theo hệ điều hành hoặc lựa chọn lưu riêng. Tái sử dụng Material 3 và các state/luồng nghiệp vụ hiện có; không thay đổi hành vi Jira, bảng dữ liệu hay calendar.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, Compose Material 3 (đã được resolve trong desktop runtime), kotlinx.serialization; không cần thêm thư viện giao diện.  
**Storage**: Tuỳ chọn giao diện được thêm vào `UserPreferences` trong file cấu hình hiện có; tăng schema và giữ dữ liệu cấu hình cũ qua migration.  
**Testing**: `commonTest` cho model/theme preference, migration và persistence; kiểm thử JVM cho tích hợp cấu hình nếu cần; kiểm tra UI Desktop thủ công theo quickstart. Chạy `./gradlew :composeApp:jvmTest ktlintCheck build`.  
**Target Platform**: Ứng dụng Desktop JVM; UI hiện dùng chung trong `commonMain`.  
**Project Type**: Compose Multiplatform desktop app, module `composeApp`.  
**Performance Goals**: Chuyển màn hình hoặc đổi theme không làm mất trạng thái truy vấn/bảng đã lưu; thay đổi giao diện không gây tải mạng hoặc công việc đồng bộ trên luồng UI.  
**Constraints**: Tuân thủ Material Design 3; giữ hành vi và dữ liệu hiện có; system theme dùng `isSystemInDarkTheme()` từ Compose Foundation; state theme lưu theo schema cấu hình hiện có; UI không chứa quy tắc nghiệp vụ; không thêm dependency hoặc module.  
**Scale/Scope**: Bốn khu vực hiện có (Truy vấn, Bảng đã lưu, Cài đặt, Calendar), các dialog/banner và các trạng thái tải, rỗng, lỗi, lựa chọn; chỉ Desktop ở phiên bản đầu.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Model theme preference và theme composition đặt ở common source set; dùng API Compose Multiplatform đã có. Không thêm API JVM cho nghiệp vụ. |
| II. Kiểm thử trước | PASS | Bổ sung test commonTest cho theme enum mặc định và migration cấu hình; dùng quickstart để xác nhận tương tác Desktop và giữ luồng hiện tại. |
| III. Hybrid UI | PASS | State theme/preferences được quản lý tách khỏi composable; composable chỉ render và gửi sự kiện. Không đưa quy tắc nghiệp vụ vào UI. |
| IV. Kotlin an toàn kiểu | PASS | Dùng enum/data class và serializable preferences, không thêm dependency. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng Material 3, AppConfigStore, UserPreferences, app shell và màn hình hiện tại; không thêm module. |

Không có `[NEEDS CLARIFICATION]` trong spec sau bước clarify.

## Nghiên cứu và quyết định thiết kế

Các căn cứ và phương án được ghi trong [research.md](./research.md). Tóm tắt:

1. Dùng Material 3 hiện có; thống nhất color scheme, typography, spacing, surface/elevation, shape và trạng thái semantic qua một theme cấp ứng dụng.
2. Sắp xếp điều hướng bằng thanh bên trái có thể thu gọn, với destination Truy vấn, Bảng đã lưu và Cài đặt; Calendar tiếp tục là luồng con của bảng đã lưu và giữ nút quay lại.
3. Theme mode là lựa chọn `SYSTEM`, `LIGHT` hoặc `DARK`. `SYSTEM` mặc định đọc system setting qua `isSystemInDarkTheme()`; ghi đè do người dùng chọn được lưu trong user preferences.
4. Nâng schema cấu hình từ v2 lên v3. Migration của v1 tiếp tục bổ sung cột `ORIGINAL_EXPECTED_TIME`; v1/v2 nhận theme mode mặc định `SYSTEM` và giữ các thiết lập hiện có.
5. Làm mới presentation theo từng surface và giữ nguyên ViewModel/flow dữ liệu; không thay đổi cách phân trang, sắp xếp, cuộn, lưu bảng hoặc gọi Jira.
6. Giao diện Desktop được xác nhận bằng thao tác thực tế; không thêm UI test dependency chỉ để kiểm thử pixel/layout.

## Phase 1: Design & Contracts

### Mô hình dữ liệu

Chi tiết entity và migration nằm trong [data-model.md](./data-model.md).

- `ThemeMode`: lựa chọn `SYSTEM`, `LIGHT` hoặc `DARK`.
- `UserPreferences`: lưu theme mode cùng các tuỳ chọn hiện có; giá trị mặc định `SYSTEM`.
- Migration schema v1/v2 sang v3 giữ dữ liệu hiện có và dùng `SYSTEM` khi cấu hình cũ chưa có theme mode.
- Theme đang hiệu lực được suy ra từ theme mode và chế độ hệ điều hành; không ghi giá trị suy ra thay cho lựa chọn người dùng.

### Hợp đồng giao diện

Hợp đồng các destination, theme mode, trạng thái và khả năng tương tác được ghi trong [contracts/material-ui.md](./contracts/material-ui.md). Không có API công khai hay giao thức dịch vụ mới.

### Quickstart

Hướng dẫn kiểm chứng tự động và thủ công nằm trong [quickstart.md](./quickstart.md).

## Project Structure

### Tài liệu tính năng

```text
specs/010-material-design-uiux/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── material-ui.md
```

### Mã nguồn dự kiến

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   └── UserPreferences.kt
├── data/
│   └── AppConfigStore.kt
├── app/
│   └── App.kt
└── ui/
    ├── theme/                 # color schemes, typography, theme mode state
    │   └── AppearanceViewModel.kt
    ├── layout/                # app shell and collapsible navigation
    ├── query/                 # query surface and result presentation
    ├── saved/                 # saved tables and calendar presentation
    └── settings/              # connection and appearance settings

composeApp/src/commonTest/kotlin/com/jiratools/
├── data/
│   └── AppConfigStoreTest.kt
└── domain/
    └── UserPreferencesTest.kt
```

**Structure Decision**: Giữ một module `composeApp`, đặt model/persistence trong domain/data hiện tại và chia sẻ presentation qua `commonMain`. Chỉ thêm thư mục theme/layout nếu cần gom trách nhiệm rõ ràng; tránh tách module hoặc nhân đôi state.

## Constitution Check sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Ranh giới đa nền tảng | PASS | Material 3, theme state và `isSystemInDarkTheme()` là API Compose Multiplatform; config serialization dùng chung. |
| Test-first | PASS | Test migration/default preference được thêm trước khi thay đổi model/store; không cần UI test framework mới. |
| Nghiệp vụ ngoài UI | PASS | Không chuyển quy tắc truy vấn/bảng sang composable; navigation/theme chỉ phản ánh state và tương tác. |
| Kiểu an toàn/đơn giản | PASS | Enum theme có tập giá trị hữu hạn; migration tương thích; không thêm dependency. |
| Bảo toàn hành vi | PASS | ViewModel và use case hiện có được tái sử dụng; xác nhận các luồng chính bằng test hồi quy và quickstart. |

Không có vi phạm hiến pháp cần Complexity Tracking.

## Complexity Tracking

Không có vi phạm hiến pháp.
