---
description: "Task list for saved-table charts"
---

# Tasks: Đồ thị bảng đã lưu

**Input**: Tài liệu thiết kế trong `specs/011-saved-table-charts/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/saved-table-charts.md`, `quickstart.md`

**Tests**: Bắt buộc theo hiến pháp dự án: kiểm thử commonTest cho chart projection, snapshot lifecycle và schema migration phải được viết trước logic tương ứng. Kiểm tra tương tác UI Desktop bằng quickstart, không thêm test UI dependency.

**Organization**: Tasks được nhóm theo ba user story, ưu tiên user story P1 trước. Snapshot model/schema là nền tảng cần thiết cho burndown và được làm trước các story.

## Phase 1: Setup

**Purpose**: Không cần khởi tạo dự án hoặc dependency; Compose Material 3 và các tầng lưu bảng hiện có đã sẵn sàng.

Không có task setup.

---

## Phase 2: Foundational

**Purpose**: Bổ sung model snapshot tương thích ngược và catalog migration trước khi chart hoặc refresh có thể dùng lịch sử.

**Checkpoint**: Snapshot model được lưu/đọc trong schema v4; bảng cũ vẫn load được với lịch sử rỗng.

- [X] T001 [P] Thêm test trong `composeApp/src/commonTest/kotlin/com/jiratools/data/SavedTableStoreTest.kt` xác nhận catalog schema v3 được nâng lên v4, giữ nguyên bảng/ticket và đọc `burndownSnapshots` rỗng.
- [X] T002 [P] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/BurndownSnapshotTest.kt` kiểm thử snapshot có `recordedAtMillis`, `ticketCount`, `remainingCount`, `unknownCategoryCount` và ràng buộc `ticketCount >= 0`, `0 <= remainingCount <= ticketCount`, `0 <= unknownCategoryCount <= remainingCount`.
- [X] T003 Thêm `BurndownSnapshot` và `SavedTable.burndownSnapshots` vào `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTable.kt`; danh sách mặc định rỗng để dữ liệu cũ giải mã được.
- [X] T004 Cập nhật `SAVED_TABLES_SCHEMA_VERSION` và migration trong `composeApp/src/commonMain/kotlin/com/jiratools/data/SavedTableStore.kt` để schema v1/v2/v3 giữ nguyên dữ liệu, nhận lịch sử rỗng và nâng lên schema v4.

---

## Phase 3: User Story 1 - Trực quan hoá bảng đã lưu (Priority: P1) 🎯 MVP

**Goal**: Người dùng mở màn hình Đồ thị của bảng đang chọn, đổi kiểu/nhóm biểu đồ và đọc dữ liệu qua nhãn, giá trị và chú giải.

**Independent Test**: Dùng bảng fixture có ticket với status/assignee/priority/Updated và snapshot; mở Đồ thị, đổi pie/column grouping, line/burndown, đối chiếu dữ liệu với fixture, sau đó quay về đúng bảng.

### Tests for User Story 1

- [X] T005 [P] [US1] Tạo `composeApp/src/commonTest/kotlin/com/jiratools/domain/SavedTableChartCalculatorTest.kt` kiểm thử category count theo status/assignee/priority, nhóm “Chưa xác định”, bảo toàn tổng count, nhóm line theo ngày `updatedAtMillis`, bỏ qua và đếm chú thích ticket thiếu Updated, cùng burndown theo snapshot.

### Implementation for User Story 1

- [X] T006 [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/domain/SavedTableChartCalculator.kt` cung cấp projection thuần cho pie/column, Updated tickets by day và burndown; category `done` không thuộc remaining, category thiếu/không rõ vẫn thuộc remaining và được đếm riêng.
- [X] T007 [US1] Thêm `chartOpen` và lựa chọn chart/grouping field mặc định Pie/Status vào `SavedTablesUiState`, cùng `openCharts()`/`closeCharts()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`; việc mở/đóng chart giữ nguyên selected table và sort.
- [X] T008 [P] [US1] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableChartsArea.kt` hiển thị selector pie/column/line/burndown, grouping field, biểu đồ và trạng thái rỗng/dữ liệu thiếu; mọi loại chart có nhãn, giá trị và chú giải, không chỉ dựa vào màu.
- [X] T009 [US1] Tích hợp route chart con, callback mở/quay lại và tiêu đề tên bảng trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`; quay lại chart phải trả về detail của đúng bảng đang chọn mà không gọi Jira.
- [X] T010 [US1] Kết nối hành động mở/đóng chart từ `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt` tới `SavedTablesViewModel`, giữ các trạng thái truy vấn, bảng đã chọn và calendar hiện có.

**Checkpoint**: Pie/column/line hiển thị đúng từ bảng local; burndown đọc snapshot (có thể chưa có với bảng cũ); điều hướng quay lại đúng context.

---

## Phase 4: User Story 2 - Thao tác nhất quán trên bảng (Priority: P2)

**Goal**: Ba nút Đồ thị, Mở lịch và Làm mới là một nhóm thao tác cùng kiểu, cùng kích thước cơ bản, cách nhau 5 px và giữ trạng thái riêng.

**Independent Test**: Kiểm tra detail bảng trên Desktop; xác nhận đúng thứ tự ba nút, cùng variant/chiều cao, gap 5 px, chart/calendar mở offline được và refresh giữ enable/loading hiện hành.

### Implementation for User Story 2

- [X] T011 [US2] Chuẩn hoá row thao tác trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` thành đúng thứ tự Đồ thị, Mở lịch, Làm mới; dùng cùng Button variant/chiều cao và `Arrangement.spacedBy(5.dp)`; trạng thái refresh không làm thay đổi gap.
- [X] T012 [US2] Cập nhật `specs/011-saved-table-charts/quickstart.md` ghi rõ kiểm tra kích thước nút, thứ tự và khoảng cách 5 px (5 dp trong Compose) trong UI Desktop.

**Checkpoint**: Ba thao tác ngang hàng có kiểu nhất quán, không đổi hành vi hiện tại của mở lịch hoặc refresh.

---

## Phase 5: User Story 3 - Xem biểu đồ burndown (Priority: P2)

**Goal**: Tạo baseline snapshot khi lưu bảng mới thành công, thêm snapshot cho mỗi lần làm mới thành công, giữ toàn bộ lịch sử đến khi bảng bị xoá.

**Independent Test**: Lưu bảng mới để tạo một baseline; refresh thành công tạo thêm đúng một điểm; refresh fail/cancel hoặc ghi catalog lỗi không đổi bảng và không thêm điểm; rename giữ và delete xoá history.

### Tests for User Story 3

- [X] T013 [P] [US3] Bổ sung test vào `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` xác nhận save bảng mới tạo một baseline với timestamp/count/category chính xác và không phát sinh khi save/write thất bại.
- [X] T014 [US3] Bổ sung test vào `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt` xác nhận mỗi refresh thành công append đúng một snapshot; refresh fail/cancel/write failure không append hoặc thay dữ liệu cũ; rename giữ snapshot và delete xoá cùng bảng.

### Implementation for User Story 3

- [X] T015 [US3] Cập nhật `saveTable()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` để thêm baseline `BurndownSnapshot` vào bảng mới trước cùng catalog write; `ticketCount` là tổng ticket, `remainingCount` loại trừ category `done`, và `unknownCategoryCount` đếm category thiếu/không nhận dạng.
- [X] T016 [US3] Cập nhật `loadEveryPage()`/`refresh()` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` để chỉ append đúng một snapshot sau khi toàn bộ refresh hoàn tất; mọi failure/cancellation/write failure giữ nguyên snapshot cũ và không phát sinh điểm.
- [X] T017 [US3] Xác nhận trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt` rằng rename bảo toàn `burndownSnapshots` và delete loại bỏ lịch sử cùng bảng trong catalog write hiện hành.

**Checkpoint**: Baseline và refresh snapshots được lưu cùng bảng, giữ nguyên khi đổi tên và bị xoá cùng bảng.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Kiểm thử hồi quy, xác nhận build và hoàn tất các tình huống Desktop.

- [X] T018 Chạy `./gradlew :composeApp:jvmTest ktlintCheck build`; xử lý lỗi liên quan trong `composeApp/src/commonMain/kotlin/com/jiratools/` và `composeApp/src/commonTest/kotlin/com/jiratools/`.
- [ ] T019 Thực hiện các tình huống tự động và thủ công trong `specs/011-saved-table-charts/quickstart.md`; kiểm tra light/dark theme, labels/legend, dữ liệu rỗng/thiếu, snapshot lifecycle, refresh offline/failure, back navigation và resize.
- [X] T020 Đối chiếu tất cả FR/SC trong `specs/011-saved-table-charts/spec.md` với model, contract và triển khai; cập nhật `specs/011-saved-table-charts/quickstart.md` nếu tiêu chí xác nhận thay đổi.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không có task; project và dependency đã tồn tại.
- **Foundational (Phase 2)**: T001/T002 viết trước T003/T004; cần hoàn tất trước user stories để schema/model snapshot sẵn sàng.
- **User Stories (Phase 3+)**: Bắt đầu sau foundational; theo thứ tự ưu tiên mặc định P1 → P2.
- **Polish (Phase 6)**: Sau các user story; T018/T019 trước T020.

### User Story Dependencies

- **US1 (P1)**: Phụ thuộc model/schema snapshot từ Phase 2; độc lập về chức năng với snapshot capture, cho phép burndown hiện trạng thái chưa có history cho bảng cũ.
- **US2 (P2)**: Phụ thuộc route Đồ thị do US1 tạo vì nhóm nút cần callback mở chart; các hành vi Mở lịch/Làm mới hiện tại được giữ nguyên.
- **US3 (P2)**: Phụ thuộc model/schema snapshot từ Phase 2; phụ thuộc UI burndown US1 để biểu diễn history, nhưng logic ghi snapshot có thể kiểm thử độc lập.

### Within Each User Story

- Test logic/data trước implementation; xác nhận test thất bại vì behavior chưa có trước khi viết code.
- Model/schema trước projection và ViewModel; projection trước render chart.
- Các thay đổi chung `SavedTablesArea.kt` được thực hiện tuần tự giữa route US1 và style row US2.
- Snapshot capture US3 và chart rendering chỉ tích hợp sau khi foundation/model có mặt.

### Parallel Opportunities

```text
# Foundational: test ở file riêng
T001: Schema migration test in SavedTableStoreTest.kt
T002: Snapshot model constraint test in BurndownSnapshotTest.kt

# US1: phép tính và UI độc lập file sau T003/T004
T005 -> T006: chart calculator test then implementation
T008: SavedTableChartsArea.kt có thể làm song song sau khi chart contract/types xác định

# US3: tests trong cùng file là tuần tự; tách test/save-refresh nếu sau này tạo fixture/helper file riêng
T013/T014: SavedTablesViewModelTest.kt (thực hiện tuần tự, cùng file)
```

## Implementation Strategy

### MVP First (User Story 1)

1. Hoàn tất schema/model snapshot foundational với migration và test.
2. Viết test rồi triển khai chart calculator.
3. Tạo chart screen với pie/column/line/burndown, selector và empty/missing-data states.
4. Nối màn hình chart vào saved-table detail; xác nhận chart local và back navigation.
5. **STOP and VALIDATE**: chạy projection tests và kiểm tra US1 theo quickstart.

### Incremental Delivery

1. Foundation → model/schema an toàn.
2. US1 → biểu đồ cho bảng local; burndown có trạng thái rõ khi chưa có lịch sử.
3. US2 → nhóm ba nút thao tác thống nhất.
4. US3 → baseline/refresh snapshots để burndown có dữ liệu lịch sử.
5. Polish → test/build/lint và kiểm tra Desktop tổng thể.

### Ghi chú triển khai

- **[P]** chỉ đánh dấu task khác file và không phụ thuộc task chưa hoàn tất; không chạy song song T013/T014 vì cùng chỉnh `SavedTablesViewModelTest.kt`.
- Không thêm chart library, không thêm Jira request khi mở đồ thị, không rút ngắn lịch sử snapshot.
