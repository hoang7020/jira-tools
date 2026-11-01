# Implementation Plan: Mở cửa sổ tối đa, vùng điều khiển cố định và cuộn bảng kết quả

**Branch**: `002-full-window-scrolling` | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-full-window-scrolling/spec.md`

## Summary

Cửa sổ chính mở ở trạng thái mở rộng tối đa ngay khi khởi động và không cho thu nhỏ dưới
1024 x 720 điểm ảnh logic; vùng điều khiển phía trên (biểu mẫu kết nối, ô nhập JQL, nút chạy truy
vấn) luôn cố định trong tầm nhìn, chỉ bảng kết quả cuộn; khi cửa sổ quá thấp, biểu mẫu kết nối tự
thu gọn thành một thanh một dòng và mở rộng lại được theo yêu cầu; bảng kết quả cuộn dọc, cuộn ngang
khi thiếu chỗ với cột Mã giữ cố định bên trái.

**Technical approach**: giữ nguyên kiến trúc hiện có (logic ở `commonMain`, Compose Multiplatform,
ViewModel + `StateFlow`) và chỉ dùng API đã có trong Compose Multiplatform 1.12.2 — **không thêm
phụ thuộc nào**.

- **Cửa sổ**: `rememberWindowState(placement = WindowPlacement.Maximized, size = DpSize(1280.dp, 800.dp))`
  và `window.minimumSize = Dimension(1024, 720)` trong `jvmMain` (đơn vị hình học cửa sổ AWT trùng dp
  theo chuyển đổi nội bộ của framework — mục 1 `research.md`).
- **Bố cục**: một `Column` cao bằng cửa sổ; vùng bảng nhận phần chiều cao còn lại bằng `weight(1f)` và
  chỉ `LazyColumn` bên trong cuộn dọc (không có cuộn toàn trang).
- **Thu gọn biểu mẫu kết nối**: hàm thuần `resolveConnectionAreaMode(availableHeight, metrics, userChoice)`
  với hằng số thiết kế tập trung ở `ui/layout`; chiều cao khả dụng đọc từ `BoxWithConstraints`; người
  dùng luôn ghi đè được (nút Mở rộng / Thu gọn), và khi mở rộng trong cửa sổ thấp thì vùng biểu mẫu
  bị giới hạn chiều cao và tự cuộn bên trong để bảng vẫn giữ chiều cao tối thiểu.
- **Bảng cuộn ngang với cột Mã cố định**: một `LazyColumn`; mỗi hàng = ô Mã cố định + phần các cột còn
  lại trong `Modifier.horizontalScroll(hState).clipScrollableContainer(Orientation.Horizontal)`; hàng
  tiêu đề dùng **cùng** `hState` nên luôn thẳng hàng. Chiều rộng cột do hàm thuần
  `computeTableColumnLayout(visibleColumns, availableWidth)` quyết định (cột Tiêu đề co giãn, tối thiểu 400 dp).
- **Thanh cuộn**: `VerticalScrollbar`/`HorizontalScrollbar` của Compose Desktop (quy ước hệ điều hành,
  hiển thị khi cần cuộn) được cô lập sau `expect/actual` để `commonMain` không phụ thuộc API chỉ có trên Desktop.
- **Không hồi quy**: không đổi tầng dữ liệu, ViewModel, lưu trữ hay luồng nghiệp vụ; mọi tính năng hiện
  có giữ nguyên hành vi (FR-015).

Chi tiết quyết định, phương án đã loại và bằng chứng API: [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21 (không đổi, ghim trong `gradle/libs.versions.toml`); JDK 17 (LTS)
trở lên; Gradle 9.7.1; Compose Multiplatform 1.12.2

**Primary Dependencies**: **không thêm phụ thuộc nào**. Chỉ dùng thêm API đã có trong các artifact
hiện dùng — `androidx.compose.ui.window` (`Window`, `WindowState`, `WindowPlacement`),
`androidx.compose.foundation.layout.BoxWithConstraints`,
`androidx.compose.foundation.horizontalScroll` + `clipScrollableContainer`,
`androidx.compose.foundation.VerticalScrollbar`/`HorizontalScrollbar` + `ScrollbarAdapter`
(chỉ Desktop) — mục 6 và 7 của [research.md](./research.md)

**Storage**: không đổi — không phát sinh dữ liệu mới cần lưu; kích thước/vị trí cửa sổ và vị trí cuộn
không được ghi nhớ (Assumptions của spec)

**Testing**: `kotlin.test` trong `commonTest` cho hai hàm thuần (`resolveConnectionAreaMode`,
`computeTableColumnLayout`) — viết trước theo Nguyên tắc II; không viết test Compose UI (hành vi cửa
sổ, cuộn, thanh cuộn kiểm chứng thủ công bằng KB-1..KB-6 trong [quickstart.md](./quickstart.md));
toàn bộ test hiện có phải tiếp tục đạt (SC-007)

**Target Platform**: Desktop JVM (macOS/Windows/Linux); chỉ target Desktop trong phiên bản này

**Project Type**: desktop-app (Kotlin Multiplatform, một module `composeApp`)

**Performance Goals**: thao tác cuộn phản hồi dưới 100 ms với danh sách khoảng 250 ticket (SC-004) —
`LazyColumn` đã ảo hoá danh sách, cuộn ngang chỉ tác động các hàng đang hiển thị

**Constraints**: kích thước cửa sổ tối thiểu 1024 x 720 điểm ảnh logic (FR-012); không dùng cuộn toàn
trang (FR-005); API chỉ có trên Desktop phải nằm sau `expect/actual` (Nguyên tắc I); không đổi phiên
bản hay thêm phụ thuộc (Nguyên tắc IV); định danh mã nguồn tiếng Anh, tài liệu tiếng Việt

**Scale/Scope**: một cửa sổ, một hồ sơ kết nối, tối đa sáu cột; ước lượng dưới 600 dòng thay đổi
(bao gồm test): sửa 4 tệp, thêm 6 tệp mã nguồn + 2 tệp test

**Ánh xạ yêu cầu -> thành phần**:

| Yêu cầu | Thành phần chịu trách nhiệm |
|---------|----------------------------|
| FR-001, FR-002, FR-012 (cửa sổ) | `jvmMain/Main.kt`: `WindowState` mở rộng tối đa + `window.minimumSize`; hằng số ở `ui/layout/AppWindowDefaults.kt` |
| FR-003, FR-015 | Không đổi luồng nghiệp vụ; trạng thái nằm trong ViewModel nên thay đổi kích thước chỉ bố cục lại |
| FR-004, FR-005, FR-006, FR-013, FR-014 | `app/App.kt` (vùng điều khiển cố định + vùng bảng `weight(1f)`), `ui/query/TicketTable.kt` (thanh cuộn dọc/ngang) |
| FR-007, FR-012 (thu gọn) | `ui/connection/ConnectionArea.kt`, `ui/layout/ConnectionAreaMode.kt` |
| FR-008, FR-009, FR-010, FR-011 | `ui/query/TicketTable.kt`, `ui/query/TableColumnLayout.kt`, `platform/Scrollbars.kt` |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Trạng thái | Bằng chứng trong kế hoạch |
|-----------|---------|--------------------------|
| I. Đa nền tảng trước, phát hành trên Desktop | PASS | Quy tắc thu gọn, mô hình chiều rộng cột và toàn bộ bố cục bảng nằm trong `commonMain` và không phụ thuộc JVM. Hai nhóm API chỉ có trên Desktop đều bị cô lập sau ranh giới nền tảng: cấu hình cửa sổ (`WindowState`, `window.minimumSize`) nằm trong `jvmMain/Main.kt`, thanh cuộn nằm sau `expect` (`commonMain/platform/Scrollbars.kt`) với `actual` trong `jvmMain/platform/JvmScrollbars.kt`. Thêm nền tảng mới chỉ cần bổ sung source set + `actual`, không sửa logic dùng chung |
| II. Kiểm thử trước (NON-NEGOTIABLE) | PASS | Hai hàm thuần được test trước trong `commonTest`: `ConnectionAreaModeTest` (ngưỡng thu gọn, liên hệ SC-002) và `TableColumnLayoutTest` (dư/thiếu chỗ, FR-010 và US3). Không test nào gọi Jira thật hay mở cửa sổ thật; không thêm logic dùng chung nào ngoài hai hàm này |
| III. Hybrid UI: trạng thái ngoài Compose | PASS | Compose Multiplatform là mặc định; không đưa quy tắc nghiệp vụ vào Compose (quy tắc bố cục là hàm thuần, chỉ dùng kiểu `Dp`); trạng thái nghiệp vụ vẫn phơi từ ViewModel qua `StateFlow`; draft nhập liệu là state UI, không phải quy tắc nghiệp vụ |
| IV. Kotlin chuẩn mực, an toàn kiểu | PASS | Không thêm phụ thuộc hay phiên bản (version catalog giữ nguyên); dùng `enum class`/`data class` bất biến cho chế độ vùng điều khiển và mô hình cột; không có I/O mới nên không chặn luồng UI |
| V. Đơn giản và gia tăng (YAGNI) | PASS | Không tách module mới, không thêm thư viện, không thêm framework DI; hoist draft bằng Compose state thay vì mở rộng ViewModel; chọn hằng số thiết kế thay vì `SubcomposeLayout` (phương án đã cân nhắc và ghi lại ở mục 3 `research.md`) |

Không có vi phạm ở cổng trước Phase 0.

**Tái đánh giá sau Phase 1**: PASS 5/5 — không phát sinh vi phạm mới. Thiết kế Phase 1 giữ nguyên các
ranh giới đã nêu: `data-model.md` chỉ mô tả state UI và hằng số thiết kế (không thêm model nghiệp vụ);
`contracts/ui-contracts.md` chốt đúng hai hàm `expect` mới cho thanh cuộn; `quickstart.md` chỉ kiểm
chứng thủ công, không thêm phụ thuộc test; `tasks.md` (Phase 2) sẽ đặt test của hai hàm thuần trước
phần triển khai tương ứng.

## Project Structure

### Documentation (this feature)

```text
specs/002-full-window-scrolling/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── ui-contracts.md  # Hợp đồng bố cục cửa sổ, quy tắc thu gọn, thanh cuộn
├── checklists/
│   └── requirements.md  # Checklist chất lượng spec (/speckit-checklist)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
jira-tools/
├── gradle/libs.versions.toml                       # KHÔNG đổi (không thêm phụ thuộc)
├── composeApp/
│   ├── build.gradle.kts                            # KHÔNG đổi
│   └── src/
│       ├── commonMain/kotlin/com/jiratools/
│       │   ├── app/App.kt                          # SỬA: BoxWithConstraints, chế độ vùng điều khiển,
│       │   │                                       #       draft kết nối, vùng bảng weight(1f), đặt lại cuộn
│       │   ├── ui/
│       │   │   ├── layout/
│       │   │   │   ├── AppWindowDefaults.kt        # MỚI: kích thước cửa sổ tối thiểu/khôi phục (hằng số thiết kế)
│       │   │   │   └── ConnectionAreaMode.kt       # MỚI: enum chế độ + hằng số vùng trên + hàm thuần quyết định
│       │   │   ├── connection/
│       │   │   │   ├── ConnectionArea.kt           # MỚI: biểu mẫu đầy đủ / thanh một dòng + nút Mở rộng - Thu gọn
│       │   │   │   └── ConnectionScreen.kt         # SỬA: nhận draft từ bên ngoài (không còn state cục bộ)
│       │   │   └── query/
│       │   │       ├── TableColumnLayout.kt        # MỚI: hàm thuần tính chiều rộng cột và nhu cầu cuộn ngang
│       │   │       ├── TicketTable.kt              # SỬA: cột Mã cố định, cuộn ngang dùng chung state,
│       │   │       │                               #       thanh cuộn dọc/ngang, đặt lại vị trí cuộn
│       │   │       └── QueryInput.kt               # KHÔNG đổi (ô JQL + nút chạy luôn hiển thị đầy đủ)
│       │   └── platform/Scrollbars.kt              # MỚI: expect cho thanh cuộn dọc/ngang của bảng
│       ├── commonTest/kotlin/com/jiratools/
│       │   ├── ui/layout/ConnectionAreaModeTest.kt # MỚI (viết trước): ngưỡng thu gọn, SC-002, ghi đè của người dùng
│       │   └── ui/query/TableColumnLayoutTest.kt   # MỚI (viết trước): dư chỗ/thiếu chỗ, FR-010, US3
│       └── jvmMain/kotlin/com/jiratools/
│           ├── Main.kt                             # SỬA: mở rộng tối đa khi khởi động + kích thước tối thiểu
│           └── platform/JvmScrollbars.kt           # MỚI: actual dùng VerticalScrollbar/HorizontalScrollbar
```

**Structure Decision**: giữ nguyên cấu trúc một module `composeApp` với các source set KMP chuẩn
(`commonMain`/`commonTest`/`jvmMain`/`jvmTest`) theo Nguyên tắc V. Thay đổi chỉ nằm trong `ui/`
(bố cục, thành phần trình bày) và `platform/` (ranh giới `expect/actual` cho thanh cuộn), cộng
`jvmMain/Main.kt` cho cấu hình cửa sổ. Tầng `domain/` và `data/` không thay đổi: tính năng này chỉ mở
rộng cách người dùng tiếp cận nội dung, không thêm quy tắc nghiệp vụ hay dữ liệu mới.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Không có vi phạm Hiến pháp cần biện minh. Không có mục nào cần điền.

Các quyết định có yếu tố đánh đổi nhưng không phải vi phạm (đã ghi lại cùng phương án thay thế trong
[research.md](./research.md)):

| Chủ đề | Lựa chọn | Phương án bị loại |
|-------|---------|------------------|
| Đặt kích thước tối thiểu | AWT `window.minimumSize` trong `jvmMain` | API cửa sổ v2 (`minSize: DpSize`) — cùng hiệu quả nhưng còn `@ExperimentalComposeUiApi` và có thể đổi vị trí trước khi ổn định |
| Quy tắc thu gọn biểu mẫu | Hằng số thiết kế + hàm thuần | Đo chiều cao thật bằng `SubcomposeLayout` — chính xác hơn nhưng phức tạp hơn hẳn (Nguyên tắc V) |
| Trạng thái nhập liệu khi thu gọn | Hoist draft vào Compose state ở `App` | Thêm API draft vào `ConnectionViewModel` — không có quy tắc nghiệp vụ nào để kiểm thử (YAGNI) |
| Thanh cuộn | Widget có sẵn của Compose Desktop sau `expect/actual` | Tự vẽ thanh cuộn thuần Compose — spec nói rõ không cần chỉ báo tự vẽ |
