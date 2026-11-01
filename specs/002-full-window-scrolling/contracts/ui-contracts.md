# Hợp đồng giao diện (UI Contracts)

**Feature**: `002-full-window-scrolling` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này chốt các giao diện mà tính năng này cam kết: (1) cấu hình cửa sổ, (2) hợp đồng bố cục
vùng điều khiển cố định, (3) hợp đồng bố cục và cuộn của bảng kết quả, (4) bề mặt dịch vụ nền tảng mới
cho thanh cuộn. Không có hợp đồng API mạng nào thay đổi: hợp đồng Jira REST của 001 vẫn giữ nguyên
(xem `../001-jql-ticket-table/contracts/jira-cloud-api.md`).

## 1. Cấu hình cửa sổ (jvmMain)

```kotlin
// jvmMain/kotlin/com/jiratools/Main.kt
Window(
    onCloseRequest = ::exitApplication,
    state = rememberWindowState(
        placement = WindowPlacement.Maximized,
        size = AppWindowDefaults.RESTORE_SIZE,   // DpSize(1280.dp, 800.dp)
    ),
    title = "Jira Tools",
) {
    LaunchedEffect(window) { window.minimumSize = Dimension(1024, 720) }  // AppWindowDefaults.MIN_SIZE
    App(container)
}
```

**Cam kết**:

- Cửa sổ mở ở trạng thái `Maximized` ngay từ khung hình đầu tiên, vẫn giữ thanh tiêu đề và các nút
  điều khiển chuẩn của hệ điều hành (không phải chế độ toàn màn hình không viền) — FR-001, FR-002.
- Không cho thu nhỏ dưới 1024 x 720 điểm ảnh logic — FR-012. Giá trị lấy từ `AppWindowDefaults.MIN_SIZE`
  (nguồn duy nhất), không viết số trực tiếp ở hai nơi.
- Người dùng bỏ mở rộng được và nội dung vẫn bố cục lại đúng, không mất dữ liệu, không chạy lại truy
  vấn, không đặt lại lựa chọn cột hay sắp xếp — FR-002, FR-003, SC-005.
- Không ghi nhớ kích thước/vị trí cửa sổ giữa các lần chạy (Assumptions của spec).
- Ràng buộc nền tảng: chỉ `jvmMain` được biết tới AWT/`WindowState`; phần còn lại của ứng dụng không
  phụ thuộc API cửa sổ (Nguyên tắc I).

## 2. Bố cục vùng điều khiển cố định (commonMain)

```kotlin
// commonMain/kotlin/com/jiratools/ui/layout/ConnectionAreaMode.kt
enum class ConnectionAreaMode { Expanded, Collapsed }

data class TopAreaMetrics(
    val connectionFormHeight: Dp,   // chiều cao biểu mẫu kết nối khi mở rộng (thiết kế: 320.dp)
    val queryAreaHeight: Dp,        // chiều cao thanh truy vấn + tiêu đề + đệm (thiết kế: 190.dp)
    val minTableHeight: Dp,         // chiều cao tối thiểu còn dùng được của bảng (thiết kế: 240.dp)
    val collapsedBarHeight: Dp,     // chiều cao thanh một dòng khi thu gọn (thiết kế: 48.dp)
) {
    val collapseThreshold: Dp get() = connectionFormHeight + queryAreaHeight + minTableHeight
}

val DEFAULT_TOP_AREA_METRICS: TopAreaMetrics

fun resolveConnectionAreaMode(
    availableHeight: Dp,
    metrics: TopAreaMetrics = DEFAULT_TOP_AREA_METRICS,
    userChoice: ConnectionAreaMode? = null,
): ConnectionAreaMode

/** Chiều cao tối đa cho vùng biểu mẫu khi mở rộng; phần vượt quá được cuộn bên trong. */
fun expandedFormMaxHeight(
    availableHeight: Dp,
    metrics: TopAreaMetrics = DEFAULT_TOP_AREA_METRICS,
): Dp
```

**Cam kết**:

- Hàm thuần, không phụ thuộc Compose runtime: cùng đầu vào luôn cho cùng kết quả, kiểm thử được trong
  `commonTest` (Nguyên tắc II).
- `userChoice != null` luôn thắng quyết định tự động (người dùng mở rộng được biểu mẫu trong cửa sổ
  thấp — FR-007).
- `expandedFormMaxHeight` >= `collapsedBarHeight` với mọi chiều cao hợp lệ, và bảo đảm phần còn lại
  cho thanh truy vấn + bảng không nhỏ hơn `queryAreaHeight + minTableHeight`: bảng không bao giờ bị ép
  dưới `minTableHeight` vì biểu mẫu — FR-012.
- Ở kích thước cửa sổ tối thiểu (`AppWindowDefaults.MIN_SIZE.height - TitleBarAllowance`), kết quả mặc
  định là `Collapsed` — SC-002.
- Ô nhập JQL và nút chạy truy vấn **không** nằm trong vùng có thể thu gọn — FR-007.

## 3. Bố cục và cuộn của bảng kết quả (commonMain)

```kotlin
// commonMain/kotlin/com/jiratools/ui/query/TableColumnLayout.kt
data class TableColumnLayout(
    val keyWidth: Dp,
    val widths: Map<ColumnKey, Dp>,
    val scrollableContentWidth: Dp,
    val scrollableViewportWidth: Dp,
) {
    val needsHorizontalScroll: Boolean
    fun widthOf(column: ColumnKey): Dp
}

fun computeTableColumnLayout(
    visibleColumns: List<ColumnKey>,
    availableWidth: Dp,
): TableColumnLayout
```

**Chiều rộng cột (thiết kế ban đầu)**: `Mã` 110 dp; `Trạng thái` 130 dp; `Người xử lý` 150 dp;
`Độ ưu tiên` 120 dp; `Cập nhật` 130 dp; `Tiêu đề` co giãn với tối thiểu 400 dp và lấp đầy phần còn
lại khi dư chỗ.

**Cam kết**:

- Cột `Mã` luôn có trong `widths` (`widthOf(ColumnKey.KEY) = keyWidth`) dù danh sách cột hiển thị thế
  nào (FR-011) và luôn nằm ngoài vùng cuộn ngang.
- Dư chỗ: `needsHorizontalScroll = false` và tổng chiều rộng nội dung vừa đúng khung nhìn (không để
  khoảng trắng thừa, giữ cảm giác hiện tại) — FR-010.
- Thiếu chỗ: `needsHorizontalScroll = true`, cột `Tiêu đề` giữ tối thiểu 400 dp → người dùng cuộn
  ngang tới được cột ngoài cùng bên phải (SC-003).
- Bố cục bảng (một `LazyColumn`):
  - Mỗi hàng = ô `Mã` cố định + phần các cột còn lại trong
    `Modifier.horizontalScroll(horizontalScroll).clipScrollableContainer(Orientation.Horizontal)`.
  - Hàng tiêu đề nằm ngoài `LazyColumn` (luôn hiển thị) và dùng **cùng** `horizontalScroll` — FR-009,
    SC-003, SC-008.
  - Không có `Modifier.verticalScroll` ở cấp trang: chỉ `LazyColumn` cuộn dọc — FR-005.
- Cuộn dọc hoạt động với con lăn chuột, bàn di chuột và phím điều hướng; Shift + con lăn và thao tác
  ngang của bàn di chuột cuộn ngang (hành vi sẵn có của Compose Desktop, mục 5 `research.md`) — FR-006.
- Thanh cuộn dọc chỉ hiển thị khi danh sách vượt khung nhìn; thanh cuộn ngang chỉ hiển thị khi
  `needsHorizontalScroll` — US2 #6, FR-008.
- Khi `QueryState` chuyển sang `Loading`, vị trí cuộn dọc đặt lại về đầu danh sách; "Tải thêm" giữ
  nguyên vị trí — Edge Case của spec, FR-014.
- Cuộn không làm thay đổi `QueryState`, không hủy truy vấn hay tải thêm (FR-014).

## 4. Dịch vụ nền tảng: thanh cuộn (expect/actual)

```kotlin
// commonMain/kotlin/com/jiratools/platform/Scrollbars.kt
@Composable
expect fun TableVerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun TableHorizontalScrollbar(state: ScrollState, modifier: Modifier = Modifier)
```

**Cam kết**:

- Thanh cuộn phản ánh đúng vị trí cuộn hiện tại, kéo được để cuộn, tuân theo quy ước hiển thị của hệ
  điều hành/Compose Desktop — FR-006, Clarifications.
- Không rò rỉ kiểu chỉ có trên Desktop (`ScrollbarAdapter`, `ScrollbarStyle`, `VerticalScrollbar`) vào
  `commonMain` (Nguyên tắc I).
- `actual` trên JVM dùng API hiện hành, không deprecated:
  `androidx.compose.foundation.VerticalScrollbar(adapter = ScrollbarAdapter(lazyListState), ...)` và
  `androidx.compose.foundation.HorizontalScrollbar(adapter = ScrollbarAdapter(scrollState), ...)`.
- Adapter được `remember` theo state để không tái tạo mỗi khung hình.
- Nền tảng mới chỉ cần thêm `actual` tương ứng (hoặc thanh cuộn mặc định của nền tảng đó), không phải
  sửa `TicketTable` hay logic dùng chung.
- Hợp đồng này **bổ sung** cho `001-jql-ticket-table/contracts/platform-services.md`; các dịch vụ
  `SecureStorage`, `AppPaths`, `UrlOpener`, `ioDispatcher` giữ nguyên không đổi.

## 5. Không thay đổi

- Hợp đồng Jira REST API (`/search/jql`, `/approximate-count`, `/myself`), ánh xạ lỗi, định dạng lưu
  trữ cấu hình: giữ nguyên (`001-jql-ticket-table/contracts/`).
- Chữ ký và hành vi của `QueryViewModel`, `ConnectionViewModel`, `ConnectionRepository`,
  `JiraApiClient`: giữ nguyên. Draft của biểu mẫu kết nối là state UI, không đi vào ViewModel.
- Không thêm phụ thuộc, không đổi phiên bản trong `gradle/libs.versions.toml` (Nguyên tắc IV).
