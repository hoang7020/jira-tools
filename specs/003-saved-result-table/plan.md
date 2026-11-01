# Implementation Plan: Bảng kết quả riêng lưu cục bộ

**Branch**: `003-saved-result-table` | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-saved-result-table/spec.md`

## Summary

Người dùng lưu bảng kết quả truy vấn hiện tại thành một **bảng riêng lưu trên máy** (đầy đủ mọi trường
dữ liệu của ticket, kể cả cột đang ẩn, cùng JQL và địa chỉ Jira tại thời điểm lưu), xem lại danh sách và
nội dung các bảng đã lưu kể cả khi không có kết nối Jira, **làm mới** một bảng đã lưu bằng cách chạy lại
đúng JQL và nạp toàn bộ kết quả hiện tại, rồi đổi tên hoặc xoá bảng khi không cần nữa.

**Technical approach**: giữ nguyên kiến trúc hiện có (logic ở `commonMain`, Compose Multiplatform,
ViewModel + `StateFlow`, container lắp ghép thủ công) và **không thêm phụ thuộc nào**.

- **Lưu trữ**: một tệp JSON `saved-tables.json` nằm cạnh `config.json` trong thư mục cấu hình nền tảng,
  đi qua `AppPaths` (mở rộng thêm 3 phương thức), có `schemaVersion`, ghi nguyên tử như `AppConfigStore`.
  Danh mục được nạp một lần vào bộ nhớ; danh sách bảng chỉ đọc metadata nên mở bảng 1000 ticket không
  phải phân tích lại tệp (mục 1 `research.md`).
- **Dữ liệu không đọc được**: cảnh báo + danh sách trống + giữ nguyên tệp + **chặn mọi thao tác ghi** +
  nút "Thử lại" (FR-020; mục 2 `research.md`).
- **Thời gian**: dịch vụ nền tảng mới `AppClock` (`nowMillis()`, `format(millis)`) inject qua
  `AppContainer`; `jvmMain` dùng `java.time`, kiểm thử dùng `FakeClock` (mục 3 `research.md`).
- **Serialization**: `Ticket` thêm `@Serializable`; `SavedTable`/`SavedTableCatalog` là model
  `@Serializable` trong `domain`, dùng `kotlinx.serialization` (mục 4 `research.md`).
- **Giao diện**: bộ chuyển chế độ xem "Truy vấn" / "Bảng đã lưu" ngay dưới vùng kết nối; nút "Lưu bảng"
  trong hàng tiêu đề kết quả truy vấn; chế độ "Bảng đã lưu" là bố cục hai cột (danh sách bên trái, chi
  tiết + lưới dữ liệu bên phải) và **tái dùng `TicketTable`** bằng cách ánh xạ dữ liệu đã lưu sang
  `QueryState.Success` (mục 5 `research.md`) — nhờ đó cuộn dọc/ngang, cột Mã cố định, sắp xếp, ẩn/hiện cột
  và mở ticket của 002 dùng lại được mà không sửa thành phần dùng chung.
- **Sắp xếp dữ liệu đã lưu**: hàm thuần trong bộ nhớ, không gọi Jira (FR-007; mục 6 `research.md`).
- **Làm mới**: vòng lặp `searchJql` tới `isLast`, thay thế toàn bộ danh sách ticket, cập nhật thời điểm và
  địa chỉ Jira đã dùng, có tiến trình và có thể huỷ; thất bại thì giữ nguyên dữ liệu cũ và báo lỗi theo
  `JiraError` hiện có (FR-021..FR-026; mục 7 `research.md`).
- **Tên bảng**: hàm thuần chuẩn hoá `trim()` + so sánh không phân biệt hoa/thường (FR-011; mục 8).

Chi tiết quyết định, phương án bị loại và bằng chứng: [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21 (không đổi, ghim trong `gradle/libs.versions.toml`); JDK 17 (LTS) trở
lên; Gradle 9.7.1; Compose Multiplatform 1.12.2

**Primary Dependencies**: **không thêm phụ thuộc nào**. Dùng lại: `kotlinx.serialization` (JSON lưu cục
bộ), Ktor Client (làm mới qua `JiraApiClient`), Compose Material 3 (`AlertDialog`, `TextField`, nút, danh
sách), `kotlinx-coroutines` (huỷ thao tác làm mới), `kotlin.test` + `ktor-client-mock` (kiểm thử). Nguồn
thời gian và định dạng ngày giờ đi qua dịch vụ nền tảng `AppClock` dùng `java.time` ở `jvmMain` — không
thêm thư viện ngày giờ

**Storage**: một tệp JSON `saved-tables.json` trong thư mục cấu hình nền tảng (cùng thư mục với
`config.json`), `schemaVersion = 1`, ghi nguyên tử qua `AppPaths`; API token **không** nằm trong tệp này
(FR-016)

**Testing**: `kotlin.test` + `kotlinx-coroutines-test` trong `commonTest`; `MockEngine` + fixture JSON cho
luồng làm mới (không gọi Jira thật — Nguyên tắc II); fake mới `FakeClock` và mở rộng `FakeAppPaths` trong
`commonTest/kotlin/com/jiratools/platform/Fakes.kt`; test viết trước cho hàm thuần, store và ViewModel;
kiểm chứng UI thủ công bằng KB-1..KB-6 trong [quickstart.md](./quickstart.md)

**Target Platform**: Desktop JVM (macOS/Windows/Linux); chỉ target Desktop trong phiên bản này

**Project Type**: desktop-app (Kotlin Multiplatform, một module `composeApp`)

**Performance Goals**: lưu bảng khoảng 100 ticket < 5 giây (SC-001); mở bảng đã lưu khoảng 1000 ticket
hiển thị hàng đầu < 3 giây và cuộn mượt như bảng truy vấn (SC-004); làm mới truy vấn khoảng 100 ticket <
10 giây (SC-010); danh sách bảng đã lưu hiển thị tức thời vì chỉ đọc metadata trong bộ nhớ

**Constraints**: không gọi Jira khi lưu, xem, sắp xếp hay ẩn/hiện cột (FR-006, FR-007, FR-018); làm mới
chỉ khi `ConnectionStatus.Connected` và phải nạp hết các trang (FR-022, FR-023); thất bại của làm mới
không được làm thay đổi dữ liệu đã lưu (FR-024); tệp lưu không chứa bí mật (FR-016, SC-008); mọi API chỉ
có trên JVM phải nằm sau ranh giới `platform`/`jvmMain` (Nguyên tắc I); hành vi hiện có giữ nguyên
(FR-019, SC-007)

**Scale/Scope**: giả định quy mô của spec — hàng chục bảng, mỗi bảng tới vài nghìn ticket (tương ứng vài
MB JSON); ước lượng thay đổi dưới 1200 dòng: sửa 6 tệp, thêm 9 tệp mã nguồn + 5 tệp test mới

**Ánh xạ yêu cầu -> thành phần**:

| Yêu cầu | Thành phần chịu trách nhiệm |
|---------|----------------------------|
| FR-001, FR-002, FR-003, FR-013, FR-015, FR-018 (lưu bảng) | `ui/saved/SavedTablesViewModel.kt`, `ui/saved/SavedTableDialogs.kt`, `app/App.kt` (nút "Lưu bảng") |
| FR-004, FR-005, FR-006, FR-017 (danh mục, xem lại) | `data/SavedTableStore.kt`, `domain/SavedTable.kt`, `ui/saved/SavedTablesArea.kt` |
| FR-007 (sắp xếp/ẩn hiện cột trên dữ liệu đã lưu) | `domain/TicketSorting.kt`, `ui/saved/SavedTablesArea.kt` (tái dùng `ui/query/TicketTable.kt`) |
| FR-008 (mở ticket của bảng đã lưu) | `domain/BrowseUrl.kt` (đã có) + `ui/saved/SavedTablesArea.kt` dùng `table.jiraBaseUrl` |
| FR-009, FR-010, FR-011 (đổi tên, xoá, kiểm tra tên) | `domain/SavedTableName.kt`, `ui/saved/SavedTableDialogs.kt`, `ui/saved/SavedTablesViewModel.kt` |
| FR-012 (chỉ lưu khi có kết quả) | `app/App.kt` (trạng thái nút), `ui/saved/SavedTablesViewModel.kt` |
| FR-014, FR-016 (ảnh chụp, không chứa bí mật) | `domain/SavedTable.kt` (chỉ lưu trường hiển thị), `data/SavedTableStore.kt` |
| FR-019 (không hồi quy) | Không sửa `data/JiraApiClient.kt`, `data/ConnectionRepository.kt`, `ui/query/QueryViewModel.kt`, `ui/query/TicketTable.kt` |
| FR-020 (dữ liệu lưu không đọc được) | `data/SavedTableStore.kt`, `ui/saved/SavedTablesArea.kt` (banner + "Thử lại") |
| FR-021, FR-022, FR-023, FR-024, FR-025, FR-026 (làm mới) | `ui/saved/SavedTablesViewModel.kt` (vòng lặp phân trang, tiến trình, huỷ), `data/JiraApiClient.kt` (dùng lại), `platform/AppClock.kt` |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Trạng thái | Bằng chứng trong kế hoạch |
|-----------|---------|--------------------------|
| I. Đa nền tảng trước, phát hành trên Desktop | PASS | Đọc/ghi tệp đi qua `AppPaths` (interface nền tảng đã có), thời gian đi qua `AppClock` (interface mới) — cả hai chỉ được cài đặt ở `jvmMain`; `domain`/`data`/`ui` không dùng API chỉ có trên JVM. Thêm nền tảng mới chỉ cần bổ sung hai `actual`/implementation nền tảng |
| II. Kiểm thử trước (NON-NEGOTIABLE) | PASS | Hàm thuần (`validateTableName`, `sortTickets`), store (`SavedTableStore`) và ViewModel (`SavedTablesViewModel`) đều có test trong `commonTest` viết trước; luồng làm mới kiểm thử bằng `MockEngine`; không test nào gọi Jira thật |
| III. Hybrid UI: trạng thái ngoài Compose | PASS | Trạng thái danh mục, bảng đang chọn, tiến trình làm mới nằm trong `SavedTablesViewModel` phơi qua `StateFlow`; Compose chỉ render và phát sự kiện; quy tắc nghiệp vụ (kiểm tra tên, sắp xếp, thay thế dữ liệu khi làm mới) là hàm thuần ngoài UI |
| IV. Kotlin chuẩn mực, an toàn kiểu | PASS | Không thêm phụ thuộc hay phiên bản (version catalog giữ nguyên); dùng `@Serializable` + `sealed interface`/`data class` bất biến cho kết quả đọc kho và trạng thái; bất đồng bộ bằng coroutines, có huỷ thao tác làm mới; không chặn luồng UI (đọc/ghi trên `ioDispatcher`) |
| V. Đơn giản và gia tăng (YAGNI) | PASS | Không tách module mới, không DI framework, không cơ sở dữ liệu; tái dùng `TicketTable`, `JiraApiClient`, `JiraErrorMapper`, `browseUrl`, `AppPaths` thay vì viết mới; chỉ thêm đúng hai seam nền tảng cần thiết (một tệp dữ liệu, một nguồn thời gian) |

Không có vi phạm ở cổng trước Phase 0.

**Tái đánh giá sau Phase 1**: PASS 5/5 — không phát sinh vi phạm mới. Thiết kế Phase 1 giữ nguyên các
ranh giới: `data-model.md` chỉ thêm model `@Serializable` trong `domain` (tiền lệ `UserPreferences`);
`contracts/saved-tables.md` chốt đúng một điểm ghi duy nhất cho `saved-tables.json` và hai bổ sung nền
tảng (`AppPaths`, `AppClock`); `quickstart.md` chỉ kiểm chứng thủ công, không thêm phụ thuộc test;
`tasks.md` (Phase 2) sẽ đặt test của hàm thuần/store/ViewModel trước phần triển khai tương ứng, và không
có task nào sửa `ui/query/TicketTable.kt` hay các tệp của 001/002 ngoài `App.kt` và `AppContainer.kt`.

## Project Structure

### Documentation (this feature)

```text
specs/003-saved-result-table/
├── plan.md                    # This file (/speckit-plan command output)
├── research.md                # Phase 0 output (/speckit-plan command)
├── data-model.md              # Phase 1 output (/speckit-plan command)
├── quickstart.md              # Phase 1 output (/speckit-plan command)
├── contracts/                 # Phase 1 output (/speckit-plan command)
│   └── saved-tables.md        # Kho dữ liệu, dịch vụ nền tảng bổ sung, hợp đồng làm mới, hợp đồng UI
├── checklists/
│   └── requirements.md        # Checklist chất lượng spec (/speckit-specify, /speckit-clarify)
├── spec.md                    # Spec của tính năng
└── tasks.md                   # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
jira-tools/
├── gradle/libs.versions.toml                       # KHÔNG đổi (không thêm phụ thuộc)
├── composeApp/
│   ├── build.gradle.kts                            # KHÔNG đổi
│   └── src/
│       ├── commonMain/kotlin/com/jiratools/
│       │   ├── domain/
│       │   │   ├── Ticket.kt                       # SỬA: thêm @Serializable (dùng lại cho dữ liệu lưu)
│       │   │   ├── SavedTable.kt                   # MỚI: SavedTable, SavedTableCatalog, schema version
│       │   │   ├── SavedTableName.kt               # MỚI: chuẩn hoá + kiểm tra tên (FR-011)
│       │   │   └── TicketSorting.kt                # MỚI: sắp xếp ticket trong bộ nhớ (FR-007)
│       │   ├── data/
│       │   │   └── SavedTableStore.kt              # MỚI: đọc/ghi saved-tables.json, áp FR-020
│       │   ├── platform/
│       │   │   ├── AppPaths.kt                     # SỬA: thêm 3 phương thức cho tệp bảng đã lưu
│       │   │   └── AppClock.kt                     # MỚI: nowMillis() + format(millis)
│       │   ├── ui/saved/
│       │   │   ├── SavedTablesViewModel.kt         # MỚI: danh mục, chọn bảng, làm mới, ghi dữ liệu
│       │   │   ├── SavedTablesArea.kt              # MỚI: danh sách + chi tiết, banner FR-020, tiến trình
│       │   │   └── SavedTableDialogs.kt            # MỚI: hộp thoại đặt tên/đổi tên và xác nhận xoá
│       │   └── app/
│       │       ├── AppContainer.kt                 # SỬA: thêm SavedTableStore, AppClock, ViewModel
│       │       └── App.kt                          # SỬA: chuyển chế độ xem, nút "Lưu bảng", đấu dây
│       ├── commonTest/kotlin/com/jiratools/
│       │   ├── platform/Fakes.kt                   # SỬA: FakeAppPaths (3 phương thức mới) + FakeClock
│       │   ├── domain/SavedTableNameTest.kt        # MỚI (viết trước)
│       │   ├── domain/TicketSortingTest.kt         # MỚI (viết trước)
│       │   ├── data/SavedTableStoreTest.kt         # MỚI (viết trước)
│       │   ├── data/SavedTableSecretTest.kt        # MỚI: tệp lưu không chứa token (FR-016, SC-008)
│       │   └── ui/saved/SavedTablesViewModelTest.kt # MỚI (viết trước): lưu/đổi tên/xoá/làm mới/tiến trình
│       └── jvmMain/kotlin/com/jiratools/
│           ├── Main.kt                             # SỬA: truyền JvmAppClock vào AppContainer
│           └── platform/
│               ├── JvmAppPaths.kt                  # SỬA: cài đặt 3 phương thức mới (ghi nguyên tử)
│               └── JvmAppClock.kt                  # MỚI: System.currentTimeMillis + java.time
```

**Structure Decision**: giữ nguyên một module `composeApp` với các source set KMP chuẩn (Nguyên tắc V).
Thay đổi nằm trong `domain/` (model mới + hai hàm thuần), `data/` (một store mới dùng lại `AppPaths`),
`platform/` (hai seam nền tảng), `ui/saved/` (màn hình mới) và hai tệp đấu dây `app/`. Toàn bộ mã dùng
lại được giữ nguyên: `ui/query/TicketTable.kt`, `ui/query/QueryViewModel.kt`, `ui/query/TicketOpener.kt`,
`data/JiraApiClient.kt`, `data/ConnectionRepository.kt`, `data/AppConfigStore.kt` — nhờ đó FR-019 và
SC-007 được bảo vệ ở mức thiết kế, không chỉ ở mức kiểm thử.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Không có vi phạm Hiến pháp cần biện minh. Không có mục nào cần điền.

Các quyết định có yếu tố đánh đổi nhưng không phải vi phạm (đã ghi kèm phương án bị loại trong
[research.md](./research.md)):

| Chủ đề | Lựa chọn | Phương án bị loại |
|-------|---------|------------------|
| Hình dạng kho lưu | Một tệp JSON duy nhất cho cả danh mục | Mỗi bảng một tệp + tệp chỉ mục — khi một tệp hỏng thì hành vi tự nhiên là "bỏ qua phần hỏng", trái lựa chọn Q1-A của người dùng |
| Nguồn thời gian | Interface `AppClock` inject qua `AppContainer` | `kotlin.time.Clock` (khó giả lập, xác định thời điểm trong test) và `kotlinx-datetime` (phụ thuộc mới) |
| Bố cục | Bộ chuyển chế độ xem + tái dùng `TicketTable` | Cửa sổ/dialog riêng (khó xem bảng lớn) và sửa `TicketTable` để dùng chung cho hai nguồn dữ liệu (rủi ro hồi quy cho 002) |
| Khi dữ liệu lưu hỏng | Chặn mọi thao tác ghi cho tới khi đọc lại được | Sao lưu ngầm rồi ghi mới (thao tác hệ tệp ngoài spec) hoặc cho ghi đè (trái FR-020) |
| Cột hiển thị / sắp xếp của bảng lưu | Lưu `visibleColumns` theo bảng; sắp xếp chỉ trong phiên | Lưu cả trạng thái sắp xếp (spec chỉ yêu cầu cột) và lưu nhiều thứ tự (tốn dung lượng, vô ích) |
