# Nghiên cứu kỹ thuật (Phase 0)

**Feature**: `002-full-window-scrolling` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này giải quyết các điểm chưa xác định của Technical Context. Mọi kết luận về API được
kiểm chứng trực tiếp từ mã nguồn của các thư viện đang dùng trong dự án
(Compose Multiplatform 1.12.2, artifact trong Gradle cache cục bộ — xem mục "Nguồn tham khảo").

## Tóm tắt quyết định

| Chủ đề | Quyết định |
|-------|-----------|
| Mở rộng tối đa khi khởi động | `Window(state = rememberWindowState(placement = WindowPlacement.Maximized, size = DpSize(1280.dp, 800.dp)))` (API cửa sổ v1 đang dùng) |
| Kích thước tối thiểu 1024 x 720 | `window.minimumSize = Dimension(1024, 720)` trong `jvmMain` (`LaunchedEffect(window)`) |
| Vùng điều khiển cố định | Không dùng cuộn toàn trang; chỉ `LazyColumn` của bảng cuộn dọc |
| Thu gọn biểu mẫu kết nối | Hàm thuần `resolveConnectionAreaMode(availableHeight)` + hằng số trong `ui/layout`; quyết định từ `BoxWithConstraints` |
| Giữ dữ liệu đang nhập khi thu gọn/mở rộng | Hoist draft (baseUrl/email/token/tokenVisible) vào Compose state ở `App` |
| Cuộn ngang + cột Mã cố định | Một `LazyColumn`; mỗi hàng `[ô Mã][Modifier.horizontalScroll(hState) cho các cột còn lại]`, dùng chung một `ScrollState` |
| Chiều rộng cột | Hàm thuần `computeTableColumnLayout(...)`; cột Tiêu đề co giãn, tối thiểu 400dp |
| Thanh cuộn | `androidx.compose.foundation.VerticalScrollbar/HorizontalScrollbar` + `ScrollbarAdapter(...)` (desktop) sau `expect/actual` |
| Cuộn bằng chuột | Con lăn dọc -> bảng; Shift + con lăn -> ngang (framework đã ánh xạ); bàn di chuột 2 chiều theo trục trội |
| Đặt lại vị trí cuộn khi chạy truy vấn mới | `LaunchedEffect(state) { if (state is QueryState.Loading) rowsState.scrollToItem(0) }` |
| Phụ thuộc mới | Không có — chỉ dùng API đã có trong Compose Multiplatform 1.12.2 |
| Kiểm thử | Unit test `commonTest` cho hai hàm thuần (viết trước); UI kiểm chứng thủ công theo `quickstart.md` |

## 1. Cửa sổ: mở rộng tối đa khi khởi động và kích thước tối thiểu

**Quyết định**: giữ API cửa sổ v1 mà dự án đang dùng (`androidx.compose.ui.window.Window`),
thêm `state = rememberWindowState(placement = WindowPlacement.Maximized, size = DpSize(1280.dp, 800.dp))`,
và đặt kích thước tối thiểu ở tầng AWT trong `jvmMain`:

```kotlin
LaunchedEffect(window) { window.minimumSize = Dimension(1024, 720) }
```

**Lý do**:

- `WindowPlacement` là enum `{ Floating, Maximized, Fullscreen }`; `Maximized` = "chiếm toàn bộ
  vùng màn hình khả dụng trừ vùng dành cho inset của hệ điều hành (taskbar/dock, menu bar macOS)"
  — đúng nghĩa "mở full window" mà spec chốt (vẫn giữ thanh tiêu đề và nút điều khiển hệ điều hành,
  khác `Fullscreen`/kiosk nằm ngoài phạm vi).
- `WindowState.size` là "kích thước khi chưa mở rộng": framework đặt kích thước này cho cửa sổ khi
  nó chưa hiển thị (để có kích thước quay về khi người dùng bỏ mở rộng). Chọn 1280 x 800 để lần
  khôi phục đầu tiên có kích thước thoải mái, không dính sát ngưỡng tối thiểu.
- API v1 không có tham số kích thước tối thiểu, nên phải đặt ở AWT. Đơn vị khớp tuyệt đối: các hàm
  chuyển đổi nội bộ của framework dùng **giá trị dp thô** làm đơn vị hình học cửa sổ AWT
  (`DpRect.toAwtRectangleSizeRoundedUp()` = `left/top/width/height` lấy từ `.value` của dp), nên
  1 dp = 1 đơn vị AWT = 1 điểm ảnh logic. Do đó `Dimension(1024, 720)` chính là "1024 x 720 điểm
  ảnh logic" của FR-012 (mật độ màn hình không ảnh hưởng tới hình học cửa sổ).
- Hệ quả quan trọng cho quy tắc thu gọn: chiều cao vùng nội dung cửa sổ tính bằng dp ≈ chiều cao
  cửa sổ (đơn vị AWT) trừ thanh tiêu đề. Vì vậy `BoxWithConstraints.maxHeight` (dp) so sánh trực
  tiếp được với các hằng số thiết kế mà không cần chuyển đổi mật độ.

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| API cửa sổ v2 (`androidx.compose.ui.window.v2.Window`, có `minSize: DpSize`) | Đang ở mức `@ExperimentalComposeUiApi`, tài liệu ghi rõ "may be moved to `androidx.compose.ui.window` before stabilization", còn TODO về quản lý focus. Đổi toàn bộ điểm khởi tạo cửa sổ chỉ để lấy `minSize` là rủi ro không cần thiết; `minSize` của v2 cũng chỉ đặt `window.minimumSize` với cùng đơn vị 1:1 |
| `singleWindowApplication` + tham số | Không kiểm soát được `WindowState` linh hoạt như `application { Window(...) }`; dự án đã dùng dạng sau |
| Không đặt kích thước tối thiểu, chỉ giới hạn bằng bố cục | Không thoả FR-012: người dùng vẫn thu nhỏ được tới mức các thành phần cốt lõi không dùng được |
| Ghi nhớ kích thước/vị trí giữa các lần chạy | Spec đã loại khỏi phạm vi (Assumptions) |

**Rủi ro đã biết**: một số trình quản lý cửa sổ có thể không tôn trọng kích thước tối thiểu (hạn chế
được tài liệu hoá của cả AWT lẫn Compose). Kịch bản KB-1 trong `quickstart.md` xác minh trên nền tảng
đích (macOS) và ghi lại kết quả.

## 2. Vùng điều khiển cố định, chỉ bảng cuộn dọc

**Quyết định**: giữ nguyên mô hình bố cục hiện tại (một `Column` cao bằng cửa sổ, bảng nhận phần
chiều cao còn lại bằng `weight(1f)`), **không** thêm `Modifier.verticalScroll` cho toàn trang; chỉ
`LazyColumn` bên trong bảng cuộn. Bổ sung hai bảo đảm còn thiếu:

1. Chiều cao tối thiểu cho vùng bảng (`MinTableHeight`) để bảng luôn dùng được — kể cả khi người
   dùng mở rộng biểu mẫu kết nối trong cửa sổ thấp (khi đó biểu mẫu bị giới hạn chiều cao và tự
   cuộn bên trong).
2. Thanh truy vấn (ô JQL + nút chạy) không bao giờ bị thu gọn (FR-007).

**Lý do**: đây là cách hiểu đã chốt ở phần Clarifications của spec (Phương án C: không cuộn toàn
trang). `LazyColumn` sẵn có đã ảo hoá danh sách nên cuộn mượt với ~250 ticket (SC-004).

**Phương án đã cân nhắc**: cuộn toàn trang (`Column` + `verticalScroll`) — bị loại vì vùng điều
khiển sẽ trôi khỏi tầm nhìn, trái FR-004/FR-005.

## 3. Thu gọn biểu mẫu kết nối khi cửa sổ không đủ cao

**Quyết định**: quyết định bằng **hàm thuần** với hằng số thiết kế tập trung một chỗ
(`commonMain/.../ui/layout/`):

```kotlin
enum class ConnectionAreaMode { Expanded, Collapsed }

fun resolveConnectionAreaMode(availableHeight: Dp, metrics: TopAreaMetrics): ConnectionAreaMode =
    if (availableHeight >= metrics.collapseThreshold) Expanded else Collapsed

// metrics.collapseThreshold = connectionFormHeight + queryAreaHeight + minTableHeight
```

Giá trị thiết kế ban đầu (đơn vị dp): `connectionFormHeight = 320`, `queryAreaHeight = 190`,
`minTableHeight = 240` -> ngưỡng 750 dp.

- `availableHeight` lấy từ `BoxWithConstraints` ở gốc `App` (chiều cao vùng nội dung cửa sổ, dp).
- Ở kích thước cửa sổ tối thiểu 1024 x 720, chiều cao nội dung ≈ 720 - thanh tiêu đề (~28) ≈ 692 dp
  < 750 dp -> biểu mẫu **thu gọn** (đúng SC-002).
- Ở cửa sổ cao hơn (~900 dp) -> biểu mẫu **đầy đủ** (không thu gọn vô ích).
- Người dùng luôn có thể ghi đè: nút "Mở rộng" trên thanh thu gọn và nút "Thu gọn" trên biểu mẫu
  đầy đủ. Khi người dùng mở rộng trong cửa sổ thấp, vùng biểu mẫu bị giới hạn
  `heightIn(max = availableHeight - queryAreaHeight - minTableHeight)` và cuộn được bên trong, nên
  mọi trường vẫn tiếp cận được (mục 4) và bảng vẫn giữ chiều cao tối thiểu.

**Lý do chọn hằng số thay vì đo đạc tại thời điểm chạy**:

- Các thành phần của biểu mẫu là Material 3 cố định chiều cao (`OutlinedTextField` một dòng = 56 dp),
  `LocalDensity.fontScale` trên Desktop mặc định bằng 1.0 — nên chiều cao là hằng số thiết kế.
- Quy tắc thuần cho phép viết test ngay trong `commonTest` (Hiến pháp II) và tái lập được
  (SC-002 kiểm chứng được bằng unit test cộng với kiểm chứng thủ công).

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| `SubcomposeLayout`/`Layout` tự đo chiều cao thật của biểu mẫu rồi quyết định | Chính xác nhất nhưng phức tạp hơn hẳn (đo/đặt lại, tránh lặp compose, tương tác với weight); Nguyên tắc V ưu tiên giải pháp đơn giản khi hằng số đủ chính xác |
| `LocalWindowInfo.current.containerSize` + chia mật độ | Cần quy đổi px -> dp thủ công, phụ thuộc API nền tảng; `BoxWithConstraints` thuần Compose và cho đúng chiều cao vùng nội dung |
| Đo bằng `onSizeChanged` sau lần hiển thị đầu | Thêm trạng thái, có nguy cơ nhấp nháy khung hình đầu và dao động ở ngưỡng |

## 4. Không mất dữ liệu đang nhập khi biểu mẫu thu gọn/mở rộng

**Quyết định**: hoist trạng thái nhập liệu của biểu mẫu kết nối lên `App`
(`remember { mutableStateOf(ConnectionDraft()) }`) và truyền xuống `ConnectionArea`; biểu mẫu đầy đủ
và thanh thu gọn cùng đọc/ghi draft này.

**Lý do**: `ConnectionScreen` hiện giữ `baseUrl`/`email`/`token` bằng `remember` cục bộ; khi chuyển
giữa hai nhánh compose (đầy đủ <-> thanh một dòng), state cục bộ sẽ bị huỷ. Hoist lên `App` giữ đúng
dữ liệu người dùng đã nhập mà không cần thêm API cho ViewModel.

**Phương án đã cân nhắc**:

- Đưa draft vào `ConnectionViewModel`: tăng bề mặt API và cần test mới cho phần không có quy tắc
  nghiệp vụ nào; Nguyên tắc V (YAGNI) không khuyến khích.
- Giữ state trong `ConnectionArea` (thành phần không rời khỏi composition): cũng đủ, nhưng
  `App` là nơi đã nắm `ConnectionStatus` để đồng bộ `baseUrl`/`email` khi kết nối thành công
  (giữ nguyên hành vi hiện có — FR-015), nên đặt draft cùng chỗ sẽ rõ ràng hơn.

## 5. Bảng kết quả: cuộn dọc, cuộn ngang và cột Mã cố định

**Quyết định (bố cục)**: một `LazyColumn` duy nhất cho dữ liệu; mỗi hàng gồm ô Mã (cố định) và phần
các cột còn lại đặt trong `Modifier.horizontalScroll(hState).clipScrollableContainer(Orientation.Horizontal)`;
hàng tiêu đề nằm ngoài `LazyColumn` (luôn nhìn thấy) và dùng **cùng** `hState`.

```text
Row {                                  // hàng tiêu đề / mỗi hàng dữ liệu
  KeyCell(width = 110.dp)              // cố định, không cuộn ngang
  Box(weight = 1f) {
    Row(Modifier.horizontalScroll(hState).clipScrollableContainer(Horizontal)) { ...các cột còn lại... }
  }
}
```

**Lý do**:

- Cột Mã nằm ngoài vùng cuộn ngang nên luôn đứng yên (FR-011, SC-008) và hàng tiêu đề dùng chung
  `hState` nên luôn thẳng hàng với dữ liệu (FR-009, SC-003).
- Chỉ có một `LazyColumn` -> không phải chia sẻ `LazyListState` giữa hai danh sách ảo hoá.
- Chia sẻ một `ScrollState` cho nhiều `horizontalScroll` là an toàn về mặt API: `ScrollNode` tự đặt
  `state.maxValue`, `state.viewportSize`, `state.contentSize` theo phép đo của chính nó, và trong bố
  cục này mọi chủ thể đều có cùng chiều rộng nội dung/khung nhìn, nên các giá trị luôn nhất quán
  (không có ràng buộc "một chủ thể" trong `ScrollState` của 1.12.2).
- Cắt nội dung khi cuộn ngang: trong 1.12.2, `Modifier.scroll` (được `horizontalScroll`/`verticalScroll`
  dùng) đã tự áp `clipScrollableContainer(orientation)` qua `scrollableArea`, nên nội dung không tràn ra
  ngoài theo mặc định. Bản triển khai vẫn thêm `Modifier.clipScrollableContainer(Orientation.Horizontal)`
  tường minh để bảo đảm cột Mã không bao giờ bị nội dung cuộn ngang đè lên, kể cả khi hành vi mặc định
  của framework đổi ở phiên bản sau.
- Cuộn bằng chuột hoạt động đúng theo cả hai trục: `MouseWheelScrollingLogic.canConsumeDelta` chọn
  trục theo góc của delta, nên delta thuần dọc không bị vùng cuộn ngang "ăn" mà nổi lên cho
  `LazyColumn`, còn delta thuần ngang thì ngược lại. Trên Desktop, framework đổi trục khi giữ Shift
  (`scrollDelta = if (event.isShiftDown) Offset(x, 0f) else Offset(0f, y)`), nên Shift + con lăn cuộn
  ngang; bàn di chuột phát delta 2 chiều và cũng được chọn theo trục trội.

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| Hai `LazyColumn` (cột Mã + phần còn lại) chia sẻ một `LazyListState` | `LazyListState` gắn với một bố cục lười (vị trí mặt hàng đầu tiên, remeasurement); chia sẻ giữa hai danh sách là hành vi không được bảo đảm |
| Cột Mã "đè" lên nội dung cuộn (overlay + vẽ lại ô theo chỉ số hàng) | Phải tự tính cửa sổ hàng đang nhìn thấy và đồng bộ thủ công; phức tạp và dễ lệch hàng |
| `horizontalScroll` bao quanh cả bảng (tiêu đề + dữ liệu) rồi bù trừ cột Mã | Vẫn cần lớp overlay cho cột cố định như trên |

**Chiều rộng cột (hàm thuần)**: `computeTableColumnLayout(visibleColumns, availableWidth)` trả về
chiều rộng từng cột, tổng chiều rộng nội dung vùng cuộn ngang, và cờ "cần cuộn ngang".

- Mã = 110 dp; Trạng thái = 130; Người xử lý = 150; Độ ưu tiên = 120; Cập nhật = 130 (giữ như hiện tại).
- Tiêu đề = `max(400.dp, khung nhìn vùng cuộn - tổng các cột cố định)`: khi đủ chỗ thì co giãn lấp đầy
  (giữ nguyên cảm giác hiện tại, FR-010), khi thiếu chỗ thì giữ tối thiểu 400 dp và bảng cuộn ngang.
- Với cửa sổ tối thiểu 1024 dp: khung nhìn vùng cuộn = 1024 - 32 (đệm 16 dp hai bên) - 110 - 12
  (làn thanh cuộn dọc) = 870 dp; nội dung = 400 + 530 = 930 dp > 870 dp -> **có** cuộn ngang khi bật
  đủ sáu cột, thoả US3/SC-006 (người dùng nhận ra nhờ thanh cuộn ngang hiển thị).

**Thanh cuộn**: dọc đặt trong làn riêng 12 dp ở mép phải vùng bảng (không che ô dữ liệu); ngang đặt
ở đáy vùng cuộn ngang (bắt đầu từ sau cột Mã) để tương ứng đúng vùng nó điều khiển. Chỉ hiển thị khi
nội dung thực sự vượt khung nhìn (US2 #6) — kiểm tra bằng `canScrollBackward || canScrollForward`.

**Tự ẩn thanh cuộn**: spec *cho phép* tự ẩn nhưng không bắt buộc; chọn hiển thị khi cần cuộn để tăng
khả năng khám phá (SC-006) và giữ đơn giản. Phương án tự ẩn theo thời gian rảnh (bọc animation quanh
thanh cuộn) đã cân nhắc và tạm hoãn — có thể bổ sung sau mà không đổi bố cục.

**Đặt lại vị trí cuộn**: khi chạy truy vấn mới, `QueryState` chuyển sang `Loading` -> hiệu ứng
`LaunchedEffect` gọi `rowsState.scrollToItem(0)`, nên trang kết quả mới hiển thị từ đầu (Edge Case
của spec). Khi "Tải thêm", `QueryState` vẫn là `Success` nên vị trí cuộn được giữ nguyên (FR-014).

## 6. Thanh cuộn: API và ranh giới nền tảng

**Quyết định**: bọc API thanh cuộn Desktop sau `expect/actual`:

```kotlin
// commonMain/.../platform/Scrollbars.kt
@Composable expect fun TableVerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)
@Composable expect fun TableHorizontalScrollbar(state: ScrollState, modifier: Modifier = Modifier)
```

**Lý do**:

- API hiện hành (không deprecated) trong 1.12.2 là
  `androidx.compose.foundation.VerticalScrollbar(adapter: androidx.compose.foundation.v2.ScrollbarAdapter, ...)`
  cùng bộ tạo adapter `ScrollbarAdapter(scrollState: ScrollState | LazyListState | LazyGridState)`;
  các overload cũ đã bị đánh dấu `@Deprecated("Use VerticalScrollbar(adapter: androidx.compose.foundation.v2.ScrollbarAdapter) instead")`.
- Các API này nằm trong source set `skikoMain` của thư viện (chỉ Desktop/web), **không** thuộc API
  dùng chung -> theo Nguyên tắc I phải cô lập sau `expect/actual`, không được gọi trực tiếp từ
  `commonMain` (dù dự án hiện chỉ có target JVM, việc gọi API chỉ có trên JVM từ `commonMain` sẽ
  phá vỡ tính di động và trái hiến pháp).

**Phương án đã cân nhắc**: tự vẽ thanh cuộn bằng Compose thuần trong `commonMain` — bị loại vì
phần Clarifications nói rõ không cần chỉ báo tự vẽ luôn hiển thị và ưu tiên quy ước hệ điều hành;
thanh cuộn có sẵn của Compose Desktop chính là widget theo quy ước đó (có track/thumb, kéo được,
hỗ trợ cuộn bằng nút trên track).

## 7. Ma trận phiên bản và phụ thuộc

Không có phụ thuộc mới, không đổi phiên bản: Kotlin 2.4.21, Compose Multiplatform 1.12.2,
Ktor 3.6.1, kotlinx.serialization 1.12.0, kotlinx-coroutines 1.11.0, Gradle 9.7.1, JDK 17.
Toàn bộ thành phần dùng thêm (`BoxWithConstraints`, `Modifier.horizontalScroll`,
`clipScrollableContainer`, `WindowPlacement`, `VerticalScrollbar`/`HorizontalScrollbar`,
`ScrollbarAdapter`) đều đã có trong các artifact hiện dùng -> version catalog giữ nguyên
(Nguyên tắc IV).

## 8. Kiểm thử

- **Test trước** cho hai hàm thuần (Hiến pháp II): `resolveConnectionAreaMode` và
  `computeTableColumnLayout`, gồm: ngưỡng biên (đúng bằng ngưỡng -> mở rộng), chiều cao cửa sổ tối
  thiểu -> thu gọn (SC-002), chỉ còn cột Mã -> không cần cuộn ngang, đủ sáu cột ở cửa sổ tối thiểu ->
  cần cuộn ngang, cột Tiêu đề co giãn khi dư chỗ (FR-010).
- **Không** viết test UI/Compose (đồng nhất với 001): hành vi cuộn, thanh cuộn, cửa sổ được kiểm
  chứng thủ công bằng các kịch bản KB-1..KB-6 trong `quickstart.md`.
- Bộ test hiện có (57 test đã qua ở 001) phải tiếp tục đạt (SC-007).

## 9. Điểm chưa kiểm chứng

- Chiều cao thực tế của biểu mẫu kết nối (giả định 320 dp) và vùng truy vấn (190 dp) chỉ được khẳng
  định bằng mắt; hằng số nằm một chỗ và kịch bản KB-2 trong `quickstart.md` dùng để hiệu chỉnh nếu
  lệch. Quy tắc vẫn tái lập được vì phụ thuộc hằng số chứ không phụ thuộc phép đo ngầm.
- Việc trình quản lý cửa sổ tôn trọng `minimumSize` được kiểm chứng thủ công trên macOS (KB-1);
  hạn chế này được tài liệu hoá cho cả API v1 (AWT) và v2.
- Nếu sau này API cửa sổ v2 ổn định, có thể chuyển `minSize = DpSize(1024.dp, 720.dp)` mà không đổi
  quy tắc thu gọn hay hằng số thiết kế.

## Nguồn tham khảo

Mã nguồn thư viện đang dùng (đọc trực tiếp từ artifact trong Gradle cache cục bộ, phiên bản 1.12.2):

- `ui-desktop-1.12.2-sources.jar`: `desktopMain/androidx/compose/ui/window/Window.desktop.kt`,
  `WindowState.desktop.kt`, `WindowPlacement.desktop.kt.kt`, `WindowScope.kt`,
  `desktopMain/androidx/compose/ui/awt/SwingWindow.desktop.kt`,
  `desktopMain/androidx/compose/ui/awt/v2/SwingWindow.desktop.kt`,
  `desktopMain/androidx/compose/ui/window/v2/Window.desktop.kt`, `WindowState.desktop.kt`,
  `desktopMain/androidx/compose/ui/scene/ComposeSceneMediator.desktop.kt` (ánh xạ Shift + con lăn),
  `desktopMain/androidx/compose/ui/awt/Utils.desktop.kt` (chuyển đổi dp <-> AWT).
- `foundation-desktop-1.12.2-sources.jar`: `commonMain/androidx/compose/foundation/Scroll.kt`
  (`ScrollState`, `ScrollNode` đặt `maxValue`, `Modifier.horizontalScroll`),
  `commonMain/androidx/compose/foundation/ClipScrollableContainer.kt`,
  `commonMain/androidx/compose/foundation/gestures/MouseWheelScrollingLogic.kt`,
  `desktopMain/androidx/compose/foundation/gestures/DesktopScrollable.desktop.kt`,
  `skikoMain/androidx/compose/foundation/Scrollbar.skiko.kt`,
  `skikoMain/androidx/compose/foundation/v2/Scrollbar.skiko.kt`.
- Đối chiếu hành vi hiện có của dự án: `specs/001-jql-ticket-table/` (plan, research) và mã nguồn
  trong `composeApp/src/`.
