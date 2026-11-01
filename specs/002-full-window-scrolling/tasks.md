# Tasks: Mở cửa sổ tối đa, vùng điều khiển cố định và cuộn bảng kết quả

**Feature**: `002-full-window-scrolling` | **Ngày**: 2026-10-31
**Input**: Design documents trong `specs/002-full-window-scrolling/` — `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/ui-contracts.md`, `quickstart.md`
**Prerequisites**: plan.md ✔, spec.md ✔, research.md ✔, data-model.md ✔, contracts/ ✔, quickstart.md ✔

## Ghi chú về kiểm thử (BẮT BUỘC — Hiến pháp Nguyên tắc II)

Hiến pháp dự án quy định **Kiểm thử trước (NON-NEGOTIABLE)**: mọi logic chia sẻ trong `commonMain` PHẢI có test trong `commonTest`, viết TRƯỚC khi triển khai và PHẢI thất bại (Red) trước khi triển khai (Green).

- Hai hàm thuần của tính năng này bắt buộc có test viết trước: `resolveConnectionAreaMode`/`expandedFormMaxHeight` (T009 đỏ → T010 xanh) và `computeTableColumnLayout` (T016 đỏ → T017 xanh).
- **Không** viết test Compose UI (cửa sổ, cuộn, thanh cuộn): kiểm chứng thủ công bằng kịch bản KB-1..KB-6 trong `specs/002-full-window-scrolling/quickstart.md` — đồng nhất với tính năng 001.
- Không test nào được mở cửa sổ thật, chạm keychain thật hay gọi Jira thật.
- Framework sẵn có: `kotlin.test` + `kotlinx-coroutines-test`; **KHÔNG** thêm phụ thuộc hay phiên bản mới (Nguyên tắc IV).

## Tổ chức theo User Story

- **US1 (P1)**: Cửa sổ mở rộng tối đa khi khởi động — 3 kịch bản nghiệm thu
- **US2 (P1)**: Cuộn dọc trong bảng kết quả, vùng điều khiển luôn cố định — 6 kịch bản nghiệm thu
- **US3 (P2)**: Cuộn ngang để xem đủ cột của bảng — 4 kịch bản nghiệm thu

Mỗi user story tạo ra một phần gia tăng độc lập: có thể triển khai, kiểm thử và kiểm chứng riêng.

## Định dạng task

`- [ ] T### [P?] [US#?] Mô tả kèm đường dẫn file`

- `[P]`: có thể chạy song song (khác file, không phụ thuộc task chưa hoàn thành).
- `[US#]`: chỉ gắn cho task thuộc phase user story (Setup/Foundational/Polish không gắn).
- Task kiểm chứng thủ công ghi rõ "chờ người dùng kiểm chứng" khi cần phiên desktop có màn hình hoặc site Jira thật.

## Quy ước đường dẫn (KMP, một module `composeApp`)

- `composeApp/src/commonMain/kotlin/com/jiratools/...` — logic chia sẻ (`app/`, `domain/`, `data/`, `ui/`, `platform/`)
- `composeApp/src/commonTest/kotlin/com/jiratools/...` — test logic chia sẻ
- `composeApp/src/jvmMain/kotlin/com/jiratools/...` — phần `actual` JVM + entry point
- Cấu hình build: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts` (**không sửa** trong tính năng này)

---

## Phase 1: Setup (Xác lập baseline)

**Mục tiêu**: chốt trạng thái nền trước khi thay đổi để phát hiện hồi quy (SC-007) và xác nhận không cần phụ thuộc mới.

- [X] T001 Kiểm chứng baseline trước khi sửa mã: chạy `./gradlew allTests ktlintCheck` tại gốc repo với cấu hình hiện có trong `composeApp/build.gradle.kts`, xác nhận toàn bộ test hiện có đạt và lint sạch; ghi kết quả vào mục "Kết quả kiểm chứng > Tự động" của `specs/002-full-window-scrolling/quickstart.md`
- [X] T002 [P] Xác nhận không cần phụ thuộc mới: rà `composeApp/build.gradle.kts` (đã có `compose.foundation`, `compose.ui`, `compose.material3`, `compose.desktop.currentOs`) đủ cho `BoxWithConstraints`, `Modifier.horizontalScroll`, `clipScrollableContainer`, `WindowPlacement`, `VerticalScrollbar`/`HorizontalScrollbar`; KHÔNG sửa `gradle/libs.versions.toml`

**Checkpoint**: baseline xanh, không có thay đổi phụ thuộc — có thể bắt đầu Phase 2.

---

## Phase 2: Foundational (Điều kiện tiên quyết — CHẶN mọi user story)

**Mục tiêu**: hằng số thiết kế dùng chung và bề mặt `expect/actual` cho thanh cuộn — thứ mà cả US1, US2, US3 đều cần.

**⚠️ CRITICAL**: không bắt đầu user story nào trước khi phase này xong.

- [X] T003 [P] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/AppWindowDefaults.kt` với hằng số thiết kế (nguồn duy nhất, không viết số trực tiếp ở nơi khác): `MIN_SIZE = DpSize(1024.dp, 720.dp)` (FR-012), `RESTORE_SIZE = DpSize(1280.dp, 800.dp)`, `internal val TITLE_BAR_ALLOWANCE = 28.dp` (ước lượng thanh tiêu đề, dùng để suy ra chiều cao vùng nội dung khi test SC-002) — theo `contracts/ui-contracts.md` mục 1 và 2
- [X] T004 [P] Khai báo bề mặt nền tảng cho thanh cuộn trong `composeApp/src/commonMain/kotlin/com/jiratools/platform/Scrollbars.kt`: `@Composable expect fun TableVerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)` và `@Composable expect fun TableHorizontalScrollbar(state: ScrollState, modifier: Modifier = Modifier)` — KHÔNG để kiểu chỉ có trên Desktop (`ScrollbarAdapter`, `ScrollbarStyle`) rò rỉ vào `commonMain` (Nguyên tắc I, `contracts/ui-contracts.md` mục 4)
- [X] T005 Triển khai `actual` JVM trong `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmScrollbars.kt`: dùng `androidx.compose.foundation.VerticalScrollbar`/`HorizontalScrollbar` với `rememberScrollbarAdapter(state)` (overload nhận `androidx.compose.foundation.v2.ScrollbarAdapter` — KHÔNG dùng overload đã `@Deprecated`), `ScrollbarStyle` mảnh (thickness khoảng 8.dp, `minimalHeight` khoảng 24.dp); phụ thuộc T004
- [X] T006 Kiểm chứng Phase 2: chạy `./gradlew allTests ktlintCheck`, xác nhận cặp `expect/actual` trong `composeApp/src/commonMain/kotlin/com/jiratools/platform/Scrollbars.kt` và `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmScrollbars.kt` biên dịch được trên target `jvm`, hằng số ở `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/AppWindowDefaults.kt` không gây lỗi lint

**Checkpoint**: hạ tầng dùng chung sẵn sàng — US1, US2, US3 có thể bắt đầu.

---

## Phase 3: User Story 1 (P1) — Cửa sổ mở rộng tối đa khi khởi động 🎯 MVP

**Mục tiêu**: cửa sổ chính mở ở trạng thái mở rộng tối đa, vẫn giữ thanh tiêu đề/nút điều khiển chuẩn, không cho thu nhỏ dưới 1024 x 720 điểm ảnh logic.

**Tiêu chí kiểm thử độc lập**: chạy `./gradlew :composeApp:run` -> cửa sổ mở rộng tối đa; bấm nút khôi phục của hệ điều hành -> cửa sổ trở về 1280 x 800 và co giãn được; kéo thu nhỏ hết mức -> không nhỏ hơn 1024 x 720 (KB-1).

**FR liên quan**: FR-001, FR-002, FR-003, FR-012

### Triển khai cho User Story 1

- [X] T007 [US1] Sửa `composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt`: dùng `rememberWindowState(placement = WindowPlacement.Maximized, size = AppWindowDefaults.RESTORE_SIZE)` (khởi tạo đã mở rộng tối đa, khôi phục về 1280 x 800); trong `WindowScope`, thêm `LaunchedEffect(window) { window.minimumSize = Dimension(AppWindowDefaults.MIN_SIZE.width.value.roundToInt(), AppWindowDefaults.MIN_SIZE.height.value.roundToInt()) }` (đơn vị AWT trùng dp — mục 1 `research.md`); giữ `title = "Jira Tools"` và để mặc định trang trí hệ điều hành; phụ thuộc T003
- [ ] T008 [US1] Kiểm chứng thủ công KB-1 trong `specs/002-full-window-scrolling/quickstart.md` bằng `./gradlew :composeApp:run`, gồm cả bước 7 (thay đổi kích thước khi bảng có dữ liệu: 100% ticket còn nguyên, lựa chọn cột/chiều sắp xếp không đổi, không phát sinh truy vấn mới) — **chờ người dùng kiểm chứng** (cần phiên desktop có màn hình); ghi kết quả vào mục "Kết quả kiểm chứng > Thủ công" của `quickstart.md`

**Checkpoint**: US1 hoàn tất và kiểm chứng độc lập được (MVP).

---

## Phase 4: User Story 2 (P1) — Cuộn dọc trong bảng, vùng điều khiển cố định

**Mục tiêu**: vùng điều khiển (biểu mẫu kết nối, ô JQL, nút chạy) luôn cố định; chỉ bảng cuộn dọc; khi cửa sổ quá thấp, biểu mẫu kết nối thu gọn thành một thanh một dòng và mở rộng lại được mà không mất dữ liệu đang nhập.

**Tiêu chí kiểm thử độc lập**: chạy truy vấn dài, cuộn tới cuối bảng -> biểu mẫu kết nối và ô JQL vẫn nguyên vị trí; thu nhỏ cửa sổ về 1024 x 720 -> biểu mẫu thu gọn một dòng, ô JQL + nút chạy vẫn đầy đủ, bảng vẫn cuộn được (KB-2, KB-3).

**FR liên quan**: FR-004, FR-005, FR-006, FR-007, FR-012, FR-013, FR-014

### Kiểm thử cho User Story 2 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

> **LƯU Ý**: viết test trước, chạy và xác nhận thất bại trước khi làm T010.

- [X] T009 [P] [US2] Viết test `composeApp/src/commonTest/kotlin/com/jiratools/ui/layout/ConnectionAreaModeTest.kt` cho `resolveConnectionAreaMode` và `expandedFormMaxHeight`, các ca bắt buộc: (a) `availableHeight == collapseThreshold` -> `Expanded` (biên, đúng bằng ngưỡng vẫn mở rộng); (b) nhỏ hơn ngưỡng 1.dp -> `Collapsed`; (c) SC-002: `AppWindowDefaults.MIN_SIZE.height - AppWindowDefaults.TITLE_BAR_ALLOWANCE` (= 692.dp) -> `Collapsed`; (d) `userChoice = Expanded` luôn thắng kể cả khi chiều cao thấp và `userChoice = Collapsed` luôn thắng kể cả khi chiều cao lớn; (e) `expandedFormMaxHeight >= collapsedBarHeight` với mọi chiều cao; (f) `availableHeight - expandedFormMaxHeight >= queryAreaHeight + minTableHeight` khi cửa sổ đủ chỗ cho biểu mẫu mở rộng — phụ thuộc T003, T006 (PHẢI thất bại trước T010)

### Triển khai cho User Story 2

- [X] T010 [US2] Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/ConnectionAreaMode.kt` theo `contracts/ui-contracts.md` mục 2: `enum class ConnectionAreaMode { Expanded, Collapsed }`; `data class TopAreaMetrics(connectionFormHeight, queryAreaHeight, minTableHeight, collapsedBarHeight)` với `val collapseThreshold: Dp get() = connectionFormHeight + queryAreaHeight + minTableHeight`; `DEFAULT_TOP_AREA_METRICS` với giá trị thiết kế đúng như tài liệu: `connectionFormHeight = 320.dp`, `queryAreaHeight = 190.dp`, `minTableHeight = 240.dp`, `collapsedBarHeight = 48.dp`; `fun resolveConnectionAreaMode(availableHeight: Dp, metrics: TopAreaMetrics = DEFAULT_TOP_AREA_METRICS, userChoice: ConnectionAreaMode? = null): ConnectionAreaMode` (userChoice khác `null` luôn thắng); `fun expandedFormMaxHeight(availableHeight: Dp, metrics: TopAreaMetrics = DEFAULT_TOP_AREA_METRICS): Dp` = `(availableHeight - metrics.queryAreaHeight - metrics.minTableHeight).coerceAtLeast(metrics.collapsedBarHeight)`; làm T009 chuyển sang XANH
- [X] T011 [US2] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionScreen.kt`: nhận `draft: ConnectionDraft` + `onDraftChange: (ConnectionDraft) -> Unit` từ bên ngoài (bỏ `remember` cục bộ cho `baseUrl`/`email`/`token`/`tokenVisible`), giữ nguyên các nhãn tiếng Việt và trường hiện có, thêm nút "Thu gọn" (gọi `onCollapse`) ở hàng tiêu đề biểu mẫu; phụ thuộc T010
- [X] T012 [US2] Tạo `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionArea.kt`: khai báo `data class ConnectionDraft(val baseUrl: String = "", val email: String = "", val token: String = "", val tokenVisible: Boolean = false)` (state UI, token KHÔNG serialize/ghi log) và composable `ConnectionArea(status, mode, draft, onDraftChange, onExpand, onCollapse, onValidate, maxExpandedHeight, modifier)`; nhánh `Expanded` = `ConnectionScreen` bọc trong `Modifier.heightIn(max = maxExpandedHeight).verticalScroll(rememberScrollState())` để mọi trường vẫn tiếp cận được khi cửa sổ thấp; nhánh `Collapsed` = một thanh một dòng cao đúng bằng `DEFAULT_TOP_AREA_METRICS.collapsedBarHeight` (48.dp, không hardcode số) hiển thị trạng thái kết nối hiện tại (`ConnectionStatus`) + nút "Mở rộng"; phụ thuộc T010, T011
- [X] T013 [US2] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`: bọc nội dung trong `BoxWithConstraints`, giữ `ConnectionDraft` bằng `remember { mutableStateOf(ConnectionDraft()) }` (dữ liệu nhập không mất khi thu gọn/mở rộng), giữ `userChoice: ConnectionAreaMode?` bằng `remember`, tính `mode = resolveConnectionAreaMode(maxHeight, DEFAULT_TOP_AREA_METRICS, userChoice)`, truyền `expandedFormMaxHeight(maxHeight, DEFAULT_TOP_AREA_METRICS)` xuống `ConnectionArea`, thay `ConnectionScreen` trực tiếp bằng `ConnectionArea`, giữ vùng bảng `weight(1f)` và KHÔNG thêm `Modifier.verticalScroll` cấp trang (FR-005); đồng bộ `draft.baseUrl`/`draft.email` khi `ConnectionStatus.Connected` xuất hiện (giữ hành vi hiện có, FR-015); phụ thuộc T010, T011, T012
- [X] T014 [US2] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`: tạo `val rowsState = remember { LazyListState() }` dùng cho `LazyColumn` của danh sách, thêm làn thanh cuộn dọc rộng 12.dp (hằng số `private val SCROLLBAR_LANE_WIDTH` trong tệp; T018 hợp nhất với `TABLE_SCROLLBAR_LANE_WIDTH`) ở mép phải vùng bảng (`Row { nội dung (weight 1f); TableVerticalScrollbar(rowsState) }`) chỉ hiển thị khi `rowsState.canScrollBackward || rowsState.canScrollForward` (US2 #6); đặt lại vị trí cuộn bằng `LaunchedEffect(Unit) { rowsState.scrollToItem(0) }` đặt trong `SuccessTable` (bảng rời composition khi trạng thái là `Loading` nên mỗi truy vấn mới bắt đầu từ đầu; "Tải thêm" giữ nguyên vị trí vì bảng không rời composition — an toàn hơn gọi `scrollToItem` khi danh sách chưa được bố trí); giữ nguyên `Footer` (nút "Tải thêm", chỉ báo đang tải) nằm ngoài vùng cuộn (FR-013, FR-014); chỉ phụ thuộc T005 nên có thể làm SONG SONG với T011–T013 vì khác tệp
- [ ] T015 [US2] Kiểm chứng thủ công KB-2 và KB-3 trong `specs/002-full-window-scrolling/quickstart.md` bằng `./gradlew :composeApp:run`: thu gọn/mở rộng biểu mẫu ở cửa sổ thấp, dữ liệu đang nhập không mất, cuộn dọc bằng con lăn/bàn di chuột/phím, thanh cuộn dọc đúng vị trí, danh sách ngắn không có thanh cuộn — **chờ người dùng kiểm chứng** (cần phiên desktop có màn hình); ghi kết quả vào `quickstart.md`

**Checkpoint**: US1 và US2 cùng hoạt động độc lập; vùng điều khiển cố định và cuộn dọc hoàn chỉnh.

---

## Phase 5: User Story 3 (P2) — Cuộn ngang để xem đủ cột của bảng

**Mục tiêu**: khi tổng chiều rộng cột vượt chiều rộng vùng bảng, người dùng cuộn ngang để xem cột bị khuất; cột Mã luôn cố định bên trái; tiêu đề thẳng hàng với dữ liệu; khi các cột vừa khung nhìn thì không cần cuộn ngang.

**Tiêu chí kiểm thử độc lập**: bật đủ sáu cột, thu hẹp cửa sổ -> cuộn ngang tới được cột ngoài cùng, cột Mã đứng yên, tiêu đề thẳng hàng; tắt bớt cột -> bảng gọn trong khung nhìn và thanh cuộn ngang biến mất (KB-4).

**FR liên quan**: FR-008, FR-009, FR-010, FR-011

### Kiểm thử cho User Story 3 (BẮT BUỘC — viết trước, phải ĐỎ) ⚠️

> **LƯU Ý**: viết test trước, chạy và xác nhận thất bại trước khi làm T017.

- [X] T016 [P] [US3] Viết test `composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TableColumnLayoutTest.kt` cho `computeTableColumnLayout`, các ca bắt buộc: (a) chỉ còn cột `KEY` -> `needsHorizontalScroll == false` và `scrollableContentWidth == 0.dp`; (b) cửa sổ tối thiểu 1024.dp với đủ sáu cột -> `needsHorizontalScroll == true` (US3/SC-006; khung nhìn = 1024 - 32 (đệm 16.dp hai bên) - 110 (cột Mã) - 12 (làn thanh cuộn dọc) = 870.dp, nội dung = 400 + 530 = 930.dp); (c) cửa sổ rộng (≥ 1100.dp) với đủ sáu cột -> `needsHorizontalScroll == false`, cột `SUMMARY` co giãn đúng bằng `khung nhìn - 530.dp` (FR-010) và không có khoảng trắng thừa; (d) `widthOf(ColumnKey.KEY) == 110.dp` với mọi tập cột (FR-011); (e) `scrollableContentWidth == widths.values.sum()`; (f) tập cột con (ví dụ chỉ `KEY` + `SUMMARY`) luôn vừa khung nhìn khi cửa sổ rộng — phụ thuộc T006 (PHẢI thất bại trước T017)

### Triển khai cho User Story 3

- [X] T017 [US3] Triển khai `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TableColumnLayout.kt` theo `contracts/ui-contracts.md` mục 3: `data class TableColumnLayout(keyWidth, widths, scrollableContentWidth, scrollableViewportWidth)` với `val needsHorizontalScroll: Boolean get() = scrollableContentWidth > scrollableViewportWidth` và `fun widthOf(column: ColumnKey): Dp`; `fun computeTableColumnLayout(visibleColumns: List<ColumnKey>, availableWidth: Dp): TableColumnLayout` dùng chiều rộng cố định đúng như tài liệu: `KEY = 110.dp`, `STATUS = 130.dp`, `ASSIGNEE = 150.dp`, `PRIORITY = 120.dp`, `UPDATED = 130.dp`, `SUMMARY = max(400.dp, scrollableViewportWidth - tổng các cột cố định còn lại)`; hằng số làn thanh cuộn dọc `val TABLE_SCROLLBAR_LANE_WIDTH: Dp = 12.dp` khai báo trong tệp này; làm T016 chuyển sang XANH
- [X] T018 [US3] Sửa `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`: dùng `BoxWithConstraints` lấy `availableWidth` và `computeTableColumnLayout(columns, availableWidth)` cho mọi ô (thay các hằng số `KEY_WIDTH`/`STATUS_WIDTH`/... hiện có) và thay `12.dp` của T014 bằng `TABLE_SCROLLBAR_LANE_WIDTH`; cấu trúc mỗi hàng = ô `Mã` cố định + phần các cột còn lại trong `Modifier.horizontalScroll(horizontalScroll).clipScrollableContainer(Orientation.Horizontal)` (kèm `clipScrollableContainer` tường minh để bảo đảm nội dung cuộn không đè lên cột Mã — mục 5 `research.md`); hàng tiêu đề nằm ngoài `LazyColumn` và dùng CÙNG `horizontalScroll` để luôn thẳng hàng (FR-009); thêm `TableHorizontalScrollbar(horizontalScroll)` ở đáy vùng cuộn ngang chỉ khi `needsHorizontalScroll`; phụ thuộc T014, T017
- [ ] T019 [US3] Kiểm chứng thủ công KB-4 trong `specs/002-full-window-scrolling/quickstart.md` bằng `./gradlew :composeApp:run`: cuộn ngang bằng kéo thanh cuộn, Shift + con lăn và thao tác ngang của bàn di chuột; cột Mã đứng yên; tiêu đề thẳng hàng; nội dung không tràn sang cột Mã; tắt bớt cột thì thanh cuộn ngang biến mất — **chờ người dùng kiểm chứng** (cần phiên desktop có màn hình); ghi kết quả vào `quickstart.md`

**Checkpoint**: cả ba user story hoạt động độc lập và đầy đủ.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Mục tiêu**: chốt cổng chất lượng của hiến pháp, xác nhận không hồi quy và cập nhật tài liệu kiểm chứng.

- [X] T020 Chạy cổng chất lượng đầy đủ tại gốc repo: `./gradlew build allTests ktlintCheck`; xác nhận build Desktop thành công, toàn bộ test (gồm `ConnectionAreaModeTest`, `TableColumnLayoutTest` và bộ test cũ) đạt, lint sạch (SC-007); ghi kết quả vào mục "Kết quả kiểm chứng > Tự động" của `specs/002-full-window-scrolling/quickstart.md`
- [X] T021 [P] Rà soát tuân thủ 5 nguyên tắc hiến pháp trên diff cuối cùng: chỉ `composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt` và `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmScrollbars.kt` chạm API chỉ có trên JVM; `composeApp/src/commonMain/kotlin/com/jiratools/platform/Scrollbars.kt` chỉ có `expect`; `gradle/libs.versions.toml` không đổi; không có khóa/log token trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionArea.kt`
- [ ] T022 [P] Kiểm chứng không hồi quy theo KB-5 và KB-6 trong `specs/002-full-window-scrolling/quickstart.md` bằng `./gradlew :composeApp:run`: kết nối, chạy JQL, sắp xếp, ẩn/hiện cột, mở ticket, tải thêm, khôi phục tùy chọn sau khi khởi động lại, JQL lỗi/rỗng/mất mạng, màn hình phụ — **chờ người dùng kiểm chứng** (cần site Jira Cloud thật và phiên desktop); ghi kết quả vào `quickstart.md`
- [X] T023 Cập nhật `specs/002-full-window-scrolling/quickstart.md` mục "Kết quả kiểm chứng": điền trạng thái thực tế cho từng hạng mục tự động và thủ công (kể cả các mục "chờ người dùng kiểm chứng"), và ghi lại giá trị hằng số thiết kế thực tế nếu phải hiệu chỉnh so với `320.dp`/`190.dp`/`240.dp`/`48.dp` trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/layout/ConnectionAreaMode.kt` (mục "Điểm chưa kiểm chứng" của `research.md`)
- [X] T024 Xác nhận đối chiếu yêu cầu: rà từng FR-001..FR-015 và SC-001..SC-008 trong `specs/002-full-window-scrolling/spec.md` với mã đã triển khai (`composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionArea.kt`); ghi danh sách FR/SC chưa có bằng chứng kiểm chứng vào `quickstart.md` để người dùng chốt

---

## Dependencies & Execution Order

### Phụ thuộc giữa các phase

- **Setup (Phase 1)**: không phụ thuộc — bắt đầu ngay (T001, T002)
- **Foundational (Phase 2)**: sau Setup — CHẶN mọi user story (T003, T004, T005; T006 là cổng kiểm chứng)
- **User Story (Phase 3+)**: sau Foundational; US1/US2/US3 có thể làm song song nếu đủ người
- **Polish (Phase 6)**: sau khi các user story mong muốn hoàn tất

### Phụ thuộc giữa các user story

- **US1 (P1)**: sau Foundational (cần `AppWindowDefaults` ở T003) — không phụ thuộc US2/US3
- **US2 (P1)**: sau Foundational (cần thanh cuộn `actual` ở T005) — không phụ thuộc US1
- **US3 (P2)**: độc lập về logic (`TableColumnLayout` + test riêng), nhưng **dùng chung tệp** `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` với T014 của US2 -> T018 phải chạy sau T014 để tránh xung đột cùng tệp

### Trong mỗi user story

- Test viết trước PHẢI đỏ rồi mới triển khai (T009 → T010; T016 → T017)
- Hàm thuần trước, thành phần UI sau, tích hợp cuối (T010 → T011 → T012 → T013); T014 (khác tệp) chỉ phụ thuộc T005 nên chạy song song được với T011–T013
- Kiểm chứng thủ công ngay sau khi story hoàn tất, trước khi chuyển story kế tiếp

### Cơ hội chạy song song

- T002 song song với T001; T003 và T004 song song trong Phase 2
- Sau Phase 2: US1 (T007) và US2 (T009) chạy song song; T009 và T016 (hai tệp test khác nhau) chạy song song
- Trong US2: T014 (TicketTable.kt) chạy song song với nhóm T011–T013 (khác tệp)
- Phase 6: T021 và T022 song song

---

## Parallel Example: Sau Foundational (US1 + US2)

```bash
# Hai tệp test độc lập, chạy song song (US2 + US3):
Task: "T009 viết composeApp/src/commonTest/kotlin/com/jiratools/ui/layout/ConnectionAreaModeTest.kt"
Task: "T016 viết composeApp/src/commonTest/kotlin/com/jiratools/ui/query/TableColumnLayoutTest.kt"

# US1 (một tệp jvmMain) song song với US2 (commonMain):
Task: "T007 sửa composeApp/src/jvmMain/kotlin/com/jiratools/Main.kt"
Task: "T009 viết composeApp/src/commonTest/kotlin/com/jiratools/ui/layout/ConnectionAreaModeTest.kt"

# Sau T010: nhóm giao diện kết nối và bảng chạy song song (khác tệp):
Task: "T011 sửa composeApp/src/commonMain/kotlin/com/jiratools/ui/connection/ConnectionScreen.kt"
Task: "T014 sửa composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt"
```

---

## Parallel Example: User Story 3

```bash
# Sau T016 (test đỏ):
Task: "T017 tạo composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TableColumnLayout.kt"
# T018 chỉ bắt đầu sau khi T014 (US2) và T017 xong vì cùng sửa TicketTable.kt
```

---

## Implementation Strategy

### MVP trước (chỉ US1)

1. Hoàn tất Phase 1 (Setup)
2. Hoàn tất Phase 2 (Foundational — bắt buộc, chặn mọi story)
3. Hoàn tất Phase 3 (US1): T007 -> T008
4. **DỪNG và KIỂM CHỨNG** KB-1 độc lập

### Giao hàng gia tăng

1. Setup + Foundational -> hạ tầng sẵn sàng (hằng số thiết kế + thanh cuộn `expect/actual`)
2. US1 -> kiểm chứng KB-1 độc lập -> demo (MVP: cửa sổ mở rộng tối đa, kích thước tối thiểu)
3. US2 -> kiểm chứng KB-2, KB-3 -> demo (vùng điều khiển cố định, thu gọn biểu mẫu, cuộn dọc)
4. US3 -> kiểm chứng KB-4 -> demo (cuộn ngang, cột Mã cố định)
5. Polish -> chạy cổng chất lượng, KB-5/KB-6, cập nhật `quickstart.md`
6. Mỗi story tăng giá trị mà không phá story trước (SC-007)

### Chiến lược nhóm song song

1. Nhóm hoàn tất Setup + Foundational cùng nhau
2. Sau Foundational: Dev A -> US1 (T007), Dev B -> US2 (T009–T015), Dev C -> US3 (T016–T017); T018 chờ T014
3. Kiểm chứng thủ công gom theo story, chạy khi có phiên desktop

---

## Notes

- `[P]` = khác tệp, không phụ thuộc task chưa xong
- `[US#]` dùng để truy vết task theo user story trong `spec.md`
- Mọi task đều ghi rõ đường dẫn tệp cụ thể; không có task mơ hồ
- Tránh sửa cùng tệp `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` ở hai nhánh song song (T014 → T018 theo thứ tự)
- Không đổi phụ thuộc/phiên bản; không thêm thư viện test; không viết test UI
- Sau mỗi task hoặc nhóm task logic: commit (khi có git) và ghi kết quả kiểm chứng vào `quickstart.md`
