---
description: "Task list for Material Design UI/UX refresh"
---

# Tasks: Nâng cấp UI/UX theo Material Design

**Input**: Tài liệu thiết kế trong `specs/010-material-design-uiux/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/material-ui.md`, `quickstart.md`

**Tests**: Bắt buộc theo hiến pháp dự án: kiểm thử commonTest cho theme preference, persistence và migration phải được viết trước logic tương ứng. Dự án chưa có UI test harness; xác nhận UI Desktop bằng quickstart, không thêm dependency UI test.

**Organization**: Tasks được nhóm theo user story để có thể triển khai và kiểm thử độc lập sau khi hoàn thành nền tảng giao diện dùng chung.

## Phase 1: Setup

**Purpose**: Không cần khởi tạo dự án hoặc dependency; module Compose Multiplatform và Material 3 đã có.

Không có task setup.

---

## Phase 2: Foundational

**Purpose**: Lưu trữ theme mode tương thích ngược và cung cấp trạng thái theme dùng chung trước khi tích hợp vào các màn hình.

> **Checkpoint**: Hoàn tất các task này trước khi triển khai app shell hoặc các user story.

- [X] T001 [P] Thêm test trong `composeApp/src/commonTest/kotlin/com/jiratools/data/AppConfigStoreTest.kt` cho việc đọc schema v1/v2 mà giữ nguyên cấu hình và mặc định theme mode là `SYSTEM`, ghi/đọc lại lựa chọn `LIGHT`/`DARK`, và giữ nguyên dữ liệu nếu ghi thất bại.
- [X] T002 [P] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/UserPreferencesTest.kt` để xác nhận `ThemeMode` có đúng ba giá trị `SYSTEM`, `LIGHT`, `DARK` và mặc định `SYSTEM`.
- [X] T003 Thêm enum `ThemeMode` và trường `themeMode` vào `composeApp/src/commonMain/kotlin/com/jiratools/domain/UserPreferences.kt`; mặc định `SYSTEM`, giữ nguyên các trường tuỳ chọn hiện có và nâng `CURRENT_SCHEMA_VERSION` từ 2 lên 3.
- [X] T004 Cập nhật migration trong `composeApp/src/commonMain/kotlin/com/jiratools/data/AppConfigStore.kt` để schema v1 tiếp tục bổ sung `ORIGINAL_EXPECTED_TIME`, schema v1/v2 được nâng logic lên v3 với `SYSTEM`, và mọi tuỳ chọn cũ được giữ lại.
- [X] T005 Tạo `composeApp/src/commonTest/kotlin/com/jiratools/ui/theme/AppearanceViewModelTest.kt` kiểm thử tải preference mặc định/đã lưu, đổi theme và lưu bền vững qua config store bằng fake paths trước khi triển khai ViewModel.
- [X] T006 Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/theme/AppearanceViewModel.kt` để tải/lưu theme preference qua `AppConfigStore`, phát `StateFlow<ThemeMode>`, mặc định `SYSTEM`, và báo lỗi lưu theo pattern hiện có thay vì giả lập lưu thành công.

---

## Phase 3: User Story 1 - Điều hướng và màn hình truy vấn nhất quán (Priority: P1) 🎯 MVP

**Goal**: Dùng app shell Material 3 với thanh điều hướng trái có thể thu gọn và trình bày luồng truy vấn rõ ràng, đồng thời giữ nguyên trạng thái truy vấn.

**Independent Test**: Khởi chạy với cấu hình hiện có, thu gọn/mở thanh điều hướng, chạy truy vấn, kiểm tra loading/error/empty/success và chuyển màn hình; trạng thái truy vấn còn nguyên và thao tác chính dùng được bằng bàn phím.

### Tests for User Story 1

> Xác nhận UI Desktop theo quickstart; không thêm UI test dependency khi chưa có harness.

### Implementation for User Story 1

- [X] T007 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/theme/AppTheme.kt` với color scheme sáng/tối Material 3, typography và semantic roles dùng thống nhất; nhận theme mode hiệu lực từ app shell và giữ các thông báo có ngữ nghĩa ngoài màu sắc.
- [X] T008 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/AppNavigation.kt` với destination Truy vấn/Bảng đã lưu/Cài đặt trong thanh bên trái có thể mở rộng/thu gọn, chỉ báo mục hiện tại, nhãn truy cập khi thu gọn và thao tác bàn phím.
- [X] T009 [US1] Tích hợp `AppearanceViewModel`, `AppTheme` và `AppNavigation` trong `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`; mặc định theo system theme, giữ nội dung hiện tại khi chuyển destination và giữ calendar là luồng con của bảng đã lưu.
- [X] T010 [US1] Chuẩn hoá thứ bậc tiêu đề, khoảng cách, field, primary action và trạng thái loading trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/QueryInput.kt`, không đổi hành vi Ctrl+Enter hoặc chặn truy vấn trùng.
- [X] T011 [US1] Chuẩn hoá header, thanh công cụ, bảng, lựa chọn cột và trạng thái rỗng/lỗi trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`; giữ nguyên phân trang, sắp xếp, mở ticket và thông báo.

**Checkpoint**: Người dùng có thể điều hướng bằng sidebar và hoàn tất truy vấn, nhận diện mọi trạng thái mà không mất dữ liệu khi chuyển màn hình.

---

## Phase 4: User Story 2 - Bảng đã lưu và calendar nhất quán (Priority: P2)

**Goal**: Áp dụng cùng hệ thống Material 3 vào danh sách/chi tiết bảng đã lưu, xác nhận thao tác và timeline.

**Independent Test**: Mở Bảng đã lưu, kiểm tra trạng thái không có bảng, chọn bảng, đổi tên, huỷ/xác nhận xoá, làm mới dữ liệu, mở calendar và kiểm tra cuộn/resize; thao tác và trạng thái dữ liệu vẫn như trước.

### Tests for User Story 2

> Chạy regression tests hiện có cho saved tables/calendar; xác nhận bố cục trực tiếp trên Desktop theo quickstart.

### Implementation for User Story 2

- [X] T012 [P] [US2] Chuẩn hoá layout danh sách, mục được chọn, metadata, cảnh báo, empty state và hành động bảng trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`; giữ nguyên callback và thao tác hiện tại.
- [X] T013 [P] [US2] Chuẩn hoá header, lưới ngày, segment timeline, nhãn ticket, thông báo và nút quay lại trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt`; giữ nguyên cuộn ngang/dọc, đồng bộ trục và màu trạng thái overdue theo semantic role.
- [X] T014 [US2] Chuẩn hoá phân cấp nội dung, trường nhập, validation error và hành động an toàn cho hộp thoại lưu/đổi tên/xoá trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableDialogs.kt`; không đổi xác nhận hoặc quy tắc kiểm tra tên.

**Checkpoint**: Saved-table và calendar dùng cùng quy tắc thị giác, đồng thời hành vi cuộn, làm mới và xác nhận dữ liệu không đổi.

---

## Phase 5: User Story 3 - Cài đặt theme và kết nối rõ ràng (Priority: P2)

**Goal**: Cho phép chọn system/light/dark được lưu lại và trình bày cấu hình Jira, lỗi, trạng thái token cùng thao tác ngắt kết nối rõ ràng.

**Independent Test**: Trong Cài đặt, chọn từng theme, đóng/mở lại ứng dụng và kiểm tra persistence; thử kết nối thành công/thất bại, xác nhận token không bị lộ, rồi huỷ/xác nhận ngắt kết nối.

### Tests for User Story 3

> Dùng test preference/config từ Foundational và regression tests kết nối; xác nhận lựa chọn theme sau restart bằng quickstart Desktop.

### Implementation for User Story 3

- [X] T015 [US3] Thêm lựa chọn `SYSTEM`/`LIGHT`/`DARK` và mô tả trạng thái theme trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt`; kết nối với `AppearanceViewModel`, lưu lựa chọn và hiển thị rõ lỗi ghi nếu có.
- [X] T016 [US3] Chuẩn hoá nhóm thông tin kết nối, nhãn field, token đã lưu, lỗi gần field và hành động kết nối trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionForm.kt`; giữ draft sau lỗi và không hiển thị token đã lưu.
- [X] T017 [US3] Chuẩn hoá tiêu đề/trạng thái kết nối, nút ngắt kết nối và hộp thoại destructive trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/settings/SettingsScreen.kt`; phân biệt rõ xác nhận với huỷ và giữ nguyên điều kiện vô hiệu hoá hiện tại.

**Checkpoint**: Người dùng chọn được theme có persistence và cấu hình/ngắt Jira mà không nhầm trạng thái hoặc lộ token.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Xác nhận đầy đủ hành vi, accessibility và chất lượng build sau khi các màn hình đã được nâng cấp.

- [X] T018 Chạy `./gradlew :composeApp:jvmTest ktlintCheck build`; xử lý lỗi liên quan trong `composeApp/src/commonMain/kotlin/com/jiratools/` và `composeApp/src/commonTest/kotlin/com/jiratools/`.
- [ ] T019 Thực hiện toàn bộ tình huống trong `specs/010-material-design-uiux/quickstart.md` trên Desktop gồm system/light/dark, keyboard/focus, sidebar, luồng query/settings/saved/calendar, resize và trạng thái lỗi/rỗng; ghi nhận kết quả và sửa lỗi UI liên quan. **Còn chờ kiểm thử tương tác trực tiếp; ứng dụng đã khởi chạy thành công nhưng môi trường hiện tại không cung cấp điều khiển/chụp giao diện Desktop.**
- [X] T020 Đối chiếu yêu cầu Material 3, khả năng truy cập, bảo toàn hành vi và migration trong `specs/010-material-design-uiux/spec.md`, `specs/010-material-design-uiux/contracts/material-ui.md` và cập nhật `specs/010-material-design-uiux/quickstart.md` nếu tiêu chí kiểm thử thay đổi.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; dự án và dependency đã sẵn sàng.
- **Foundational (Phase 2)**: Thực hiện T001/T002 trước T003/T004; T005 viết trước T006; T006 cần model/config v3. Phải hoàn tất trước các user story.
- **User Story 1 (Phase 3)**: Sau Foundational; là MVP cho shell, theme mặc định và màn hình truy vấn.
- **User Story 2 (Phase 4)**: Sau US1 để dùng app shell và theme Material 3 chung.
- **User Story 3 (Phase 5)**: Sau US1 để nối cài đặt vào navigation và app shell; độc lập với UI chi tiết US2.
- **Polish (Phase 6)**: Sau các user story được chọn; T018/T019 trước T020.

### User Story Dependencies

- **US1 (P1)**: Phụ thuộc Foundational; không phụ thuộc story khác.
- **US2 (P2)**: Phụ thuộc Foundational và app shell/theme của US1; độc lập với US3.
- **US3 (P2)**: Phụ thuộc Foundational và route/app shell của US1; độc lập với US2.

### Within Each User Story

- Foundation tests T001/T002 phải có trước model/schema implementation T003/T004.
- App theme và sidebar T007/T008 hoàn tất trước wiring app T009; query presentation sau khi shell/theme ổn định.
- T012 và T013 làm việc trên file riêng nên có thể song song; T014 độc lập sau khi dùng chung Material theme đã sẵn.
- US2 và US3 có thể triển khai song song sau US1 nếu T015/T017 không sửa lại `App.kt`; nếu cần thay đổi wiring app, thực hiện tuần tự sau US1.
- Chạy test/lint/build và quickstart sau các thay đổi tương ứng; không đánh dấu task hoàn tất nếu chưa kiểm chứng.

### Dependency Graph

```text
T001 ─┐
      ├──> T003 ──> T004 ──> T005 ──> T006 ──┬──> T007 ──┐
T002 ─┘                                       │            ├──> T009 ──> T010/T011
                                              └──> T008 ──┘               │
                                                                          ├──> T012/T013/T014 (US2)
                                                                          └──> T015/T016/T017 (US3)
T018 ──> T019 ──> T020
```

### Parallel Opportunities

```text
# Foundation: test files riêng biệt
T001: config migration/persistence tests in AppConfigStoreTest.kt
T002: ThemeMode/default tests in UserPreferencesTest.kt

# Sau app shell/theme: saved tables and calendar use separate files
T012: SavedTablesArea.kt
T013: SavedTableCalendarArea.kt

# Sau US1: story-level parallel work on independent surfaces
US2: SavedTablesArea.kt, SavedTableCalendarArea.kt, SavedTableDialogs.kt
US3: SettingsScreen.kt (T015 then T017), ConnectionForm.kt (T016)
```

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn tất test-first cho theme preference/schema và triển khai persistence ở Foundational.
2. Tạo app theme Material 3 và sidebar thu gọn.
3. Nối app shell, giữ nguyên state khi đổi destination, chuẩn hoá luồng truy vấn.
4. **STOP and VALIDATE**: chạy test/lint/build liên quan và kiểm tra query/navigation bằng Desktop.

### Incremental Delivery

1. Sau MVP, hoàn thiện US2 cho bảng đã lưu, dialog và calendar.
2. Hoàn thiện US3 cho lựa chọn theme và cấu hình kết nối.
3. Chạy full validation và quickstart cho cả bốn khu vực trước khi hoàn tất.
