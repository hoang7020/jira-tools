# Mô hình dữ liệu (Phase 1)

**Feature**: `002-full-window-scrolling` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tính năng này **không thêm dữ liệu nghiệp vụ mới** và **không thay đổi dữ liệu lưu trữ**. Toàn bộ
"thực thể" dưới đây là trạng thái giao diện (UI state) và hằng số thiết kế quyết định bố cục. Mọi
thực thể đều nằm trong `commonMain` để không khóa cứng vào JVM (Nguyên tắc I), trừ phần cấu hình cửa
sổ vốn chỉ tồn tại ở `jvmMain`.

## 1. Cửa sổ ứng dụng (Application Window)

| Thuộc tính | Kiểu | Mô tả | Nguồn |
|-----------|------|-------|-------|
| `placement` | `WindowPlacement` | Trạng thái hiển thị; **giá trị khởi tạo** là `Maximized` | FR-001 |
| `size` | `DpSize` | Kích thước khi chưa mở rộng; giá trị khởi tạo `1280 x 800` (dùng khi người dùng bỏ mở rộng) | FR-002, FR-003 |
| `minimumSize` | `Dimension` (AWT) | `1024 x 720`, chặn người dùng thu nhỏ hơn mức bảo đảm thành phần cốt lõi dùng được | FR-012 |

**Quy tắc**:

- `placement` chỉ được đặt `Maximized` ở lần khởi tạo; sau đó do người dùng và hệ điều hành điều khiển
  qua thanh tiêu đề (thu nhỏ, khôi phục, thay đổi kích thước, đóng) — FR-002.
- Vị trí và kích thước cửa sổ **không** được ghi nhớ giữa các lần chạy (Assumptions của spec).
- Đơn vị: 1 dp = 1 điểm ảnh logic = 1 đơn vị hình học cửa sổ AWT trong Compose Desktop, nên
  `minimumSize = 1024 x 720` đúng nghĩa "điểm ảnh logic" của FR-012 (mục 1 `research.md`).

**Chuyển trạng thái**: `Maximized` -> (nút khôi phục của hệ điều hành) -> `Floating` với kích thước
`size` -> (kéo/thay đổi kích thước) -> `Floating` với kích thước mới, luôn >= `minimumSize`.

## 2. Cấu hình thiết kế cửa sổ (`AppWindowDefaults`, `commonMain/.../ui/layout`)

| Hằng số | Giá trị | Mục đích |
|--------|--------|---------|
| `MIN_SIZE` | `DpSize(1024.dp, 720.dp)` | Nguồn duy nhất cho FR-012; `jvmMain` dùng để đặt `window.minimumSize` |
| `RESTORE_SIZE` | `DpSize(1280.dp, 800.dp)` | Kích thước ban đầu khi người dùng bỏ mở rộng |
| `TITLE_BAR_ALLOWANCE` (internal) | `28.dp` | Ước lượng chiều cao thanh tiêu đề hệ điều hành, dùng để suy ra chiều cao vùng nội dung ở kích thước cửa sổ tối thiểu trong test (SC-002) |

## 3. Vùng kết nối (Connection Area)

| Thuộc tính | Kiểu | Mô tả | Nguồn |
|-----------|------|-------|-------|
| `mode` | `ConnectionAreaMode` (`Expanded` / `Collapsed`) | Chế độ hiển thị hiệu lực của biểu mẫu kết nối | FR-004, FR-007 |
| `userChoice` | `ConnectionAreaMode?` | Lựa chọn thủ công của người dùng; `null` = để bố cục tự quyết định | FR-007 |
| `draft` | `ConnectionDraft` (state UI) | Nội dung người dùng đang nhập, không mất khi đổi chế độ (Edge Case) | FR-007 |
| `maxExpandedHeight` | `Dp` | Chiều cao tối đa vùng biểu mẫu khi mở rộng trong cửa sổ thấp; phần vượt quá được cuộn bên trong | FR-007, FR-012 |

`ConnectionDraft` (state UI thuần, không phải model nghiệp vụ):

| Trường | Kiểu | Ghi chú |
|-------|------|--------|
| `baseUrl` | `String` | Đồng bộ từ `ConnectionStatus.Connected` khi kết nối thành công (giữ nguyên hành vi hiện có) |
| `email` | `String` | Như trên |
| `token` | `String` | Chỉ nằm trong bộ nhớ; **không** serialize, **không** ghi log (hiến pháp: Bảo mật) |
| `tokenVisible` | `Boolean` | Trạng thái ẩn/hiện token |

**Quy tắc quyết định chế độ** (hàm thuần, có test):

```text
mode = userChoice ?: if (availableHeight >= metrics.collapseThreshold) Expanded else Collapsed
metrics.collapseThreshold = connectionFormHeight + queryAreaHeight + minTableHeight
```

| Điều kiện | Kết quả |
|----------|--------|
| `availableHeight >= collapseThreshold` và không có lựa chọn thủ công | `Expanded` |
| `availableHeight < collapseThreshold` và không có lựa chọn thủ công | `Collapsed` |
| Người dùng bấm "Mở rộng" / "Thu gọn" | Ghi `userChoice` tương ứng và giữ cho tới khi người dùng đổi lại |

**Bất biến**:

- `queryAreaHeight` (ô JQL + nút chạy) **không bao giờ** bị thu gọn (FR-007).
- Vùng bảng luôn giữ tối thiểu `minTableHeight` khi biểu mẫu ở chế độ `Expanded` — kể cả khi người
  dùng mở rộng biểu mẫu trong cửa sổ thấp (khi đó biểu mẫu tự cuộn bên trong).
- Ở kích thước cửa sổ tối thiểu (1024 x 720), `Collapsed` là kết quả mặc định (SC-002).

## 4. Thanh truy vấn (Query Bar)

| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `jql` | `String` | Giữ nguyên từ `QueryViewModel` (không đổi) |
| `running` | `Boolean` | Suy ra từ `QueryState.Loading` (không đổi) |

**Bất biến**: luôn hiển thị đầy đủ ở mọi kích thước cửa sổ hợp lệ; nội dung JQL dài tự cuộn bên trong
ô nhập (Edge Case của spec) — không thay đổi so với hiện tại.

## 5. Bảng kết quả cuộn (Scrollable Result Table)

| Thuộc tính | Kiểu | Mô tả | Nguồn |
|-----------|------|-------|-------|
| `visibleColumns` | `List<ColumnKey>` | Danh sách cột đang bật; cột `Mã` luôn có mặt | FR-010, FR-011 |
| `columnLayout` | `TableColumnLayout` | Chiều rộng từng cột + chiều rộng nội dung vùng cuộn ngang + nhu cầu cuộn ngang | FR-008, FR-010 |
| `verticalScroll` | `LazyListState` | Vị trí cuộn dọc của danh sách ticket | FR-005, FR-006 |
| `horizontalScroll` | `ScrollState` | Vị trí cuộn ngang, **dùng chung** cho hàng tiêu đề và mọi hàng dữ liệu | FR-008, FR-009, FR-011 |

`TableColumnLayout`:

| Trường | Kiểu | Mô tả |
|-------|------|-------|
| `keyWidth` | `Dp` | Chiều rộng cột Mã (cố định, nằm ngoài vùng cuộn ngang) |
| `widths` | `Map<ColumnKey, Dp>` | Chiều rộng các cột còn lại đang hiển thị |
| `scrollableContentWidth` | `Dp` | Tổng chiều rộng các cột còn lại |
| `scrollableViewportWidth` | `Dp` | Chiều rộng khả dụng cho vùng cuộn ngang (đã trừ cột Mã và làn thanh cuộn dọc) |
| `needsHorizontalScroll` | `Boolean` | `scrollableContentWidth > scrollableViewportWidth` |

**Bất biến**:

- Cột Mã luôn hiển thị đầy đủ cho mọi hàng, kể cả khi cuộn ngang tới vị trí xa nhất (FR-011, SC-008).
- Hàng tiêu đề và các ô dữ liệu dùng cùng `horizontalScroll` nên luôn thẳng hàng (FR-009, SC-003).
- Danh sách ngắn hơn khung nhìn -> không hiển thị thanh cuộn dọc không cần thiết (US2 #6).
- Khi người dùng tắt bớt cột sao cho các cột còn lại vừa khung nhìn -> `needsHorizontalScroll = false`
  và bảng hiển thị gọn (FR-010).
- Cuộn không hủy truy vấn đang chạy, không hủy tải thêm, không mất dữ liệu đã tải (FR-014) — đây là
  hệ quả của việc trạng thái cuộn tách hoàn toàn khỏi `QueryViewModel`.

**Chuyển trạng thái vị trí cuộn**:

| Sự kiện | `verticalScroll` | `horizontalScroll` |
|--------|------------------|--------------------|
| Chạy truy vấn mới (`QueryState.Loading`) | Đặt lại về đầu danh sách (Edge Case: trang mới hiển thị từ đầu) | Giữ nguyên |
| Tải thêm kết quả (`loadMore`) | Giữ nguyên | Giữ nguyên |
| Thay đổi kích thước cửa sổ | Giữ nguyên (không mất dữ liệu, không chạy lại truy vấn — FR-003, SC-005) | Giữ nguyên, có thể bị ép về `0` nếu nội dung không còn vượt khung nhìn |
| Bật/tắt cột | Giữ nguyên | Có thể bị ép về `0` nếu nội dung không còn vượt khung nhìn |

## 6. Truy vết yêu cầu chức năng -> thực thể

| Yêu cầu | Thực thể / quy tắc liên quan |
|---------|------------------------------|
| FR-001, FR-002 | Cửa sổ ứng dụng (mục 1) |
| FR-003 | Cửa sổ ứng dụng + bảng kết quả (giữ trạng thái khi đổi kích thước) |
| FR-004 | Vùng kết nối (mục 3) + Thanh truy vấn (mục 4) |
| FR-005, FR-006 | Bảng kết quả cuộn (mục 5) — `verticalScroll`, quy tắc hiển thị thanh cuộn dọc |
| FR-007 | Vùng kết nối (mục 3) — `mode`, `userChoice`, `maxExpandedHeight`, draft |
| FR-008, FR-009, FR-010, FR-011 | Bảng kết quả cuộn (mục 5) — `columnLayout`, `horizontalScroll`, bất biến cột Mã |
| FR-012 | Cửa sổ ứng dụng + `AppWindowDefaults.MIN_SIZE` + `minTableHeight` |
| FR-013, FR-014 | Bảng kết quả cuộn + tách biệt với `QueryViewModel` (không đổi trạng thái truy vấn) |
| FR-015 | Không thay đổi `domain/`, `data/`; các thực thể ở trên chỉ là state UI |

## 7. Không thay đổi

- `Ticket`, `QueryState`, `QueryError`, `SortState`, `ColumnKey`, `UserPreferences`, `ConnectionStatus`
  giữ nguyên định nghĩa và ngữ nghĩa (FR-015).
- Không có thực thể lưu trữ mới; `AppConfigStore`/`UserPreferences` không đổi (không ghi nhớ kích
  thước cửa sổ hay vị trí cuộn).
