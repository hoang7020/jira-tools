# Implementation Plan: Đồ thị bảng đã lưu

**Branch**: `011-saved-table-charts` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả tính năng tại `specs/011-saved-table-charts/spec.md`

## Summary

Thêm màn hình Đồ thị cho bảng đã lưu, gồm pie/column theo status, assignee hoặc priority, line đếm ticket theo ngày Updated và burndown dựa trên số ticket chưa hoàn tất qua các snapshot. Thêm snapshot baseline khi lưu bảng, snapshot sau mỗi lần làm mới thành công; lưu snapshot cùng record bảng trong catalog JSON để thao tác ghi nguyên tử và vòng đời xoá/đổi tên được bảo toàn. Chuẩn hoá ba nút Đồ thị, Mở lịch và Làm mới thành cùng kiểu và cách nhau 5 dp. Tái sử dụng Compose Material 3 hiện có; chart data calculation đặt trong commonMain và được kiểm thử trước.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM bytecode target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, Material 3, kotlinx.serialization, kotlinx.coroutines và kotlinx.datetime hiện có; không thêm charting dependency.  
**Storage**: Catalog JSON cục bộ `saved-tables.json`; nâng schema danh mục từ v3 lên v4, lưu snapshot tổng hợp bên trong từng bảng. Giữ toàn bộ snapshot cho tới khi bảng bị xoá.  
**Testing**: commonTest cho phép tính chart, grouping/missing values, snapshot capture/lifecycle, migration schema và SavedTablesViewModel; JVM test target, ktlint và build Desktop. UI kiểm tra theo quickstart.  
**Target Platform**: Ứng dụng Desktop JVM; model, phép tính và trạng thái dùng commonMain.  
**Project Type**: Compose Multiplatform desktop app, module `composeApp`.  
**Performance Goals**: Chart tính từ dữ liệu local, không thực hiện request Jira khi mở màn hình. Snapshot append một record tóm tắt mỗi lần save/refresh thành công. Tính chart tuyến tính theo số ticket hiện tại hoặc số snapshot cần trình bày; giới hạn hiệu năng cụ thể chưa được xác định.  
**Constraints**: Không đổi nghiệp vụ truy vấn/làm mới hiện có; không ghi snapshot nếu thao tác lưu catalog thất bại; không loại bỏ lịch sử; không dựa riêng vào màu để truyền đạt thông tin; bảo toàn v1/v2/v3 migration; giữ logic nghiệp vụ trong commonMain.  
**Scale/Scope**: Một local catalog, các bảng đã lưu hiện có, ba trường phân nhóm và bốn loại biểu đồ (pie, column, line, burndown); không có dashboard liên bảng, cấu hình công thức tự do hay xuất biểu đồ ở phiên bản đầu.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Model snapshot và phép tính biểu đồ nằm trong commonMain; chỉ dùng API Kotlin/Compose Multiplatform hiện có. |
| II. Kiểm thử trước | PASS | Thêm commonTest cho phép tính chart, snapshot, migration và lifecycle trước khi triển khai logic. |
| III. Hybrid UI | PASS | Chart projection và trạng thái màn hình được giữ ngoài composable; UI chỉ render và phát thao tác. |
| IV. Kotlin an toàn kiểu | PASS | Dùng sealed/data types, coroutines và kotlinx.serialization; không thêm dependency ngoài version catalog. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng single module, catalog JSON, Compose Material 3 và các viewmodel hiện có; không thêm database hay chart library. |

Không có `[NEEDS CLARIFICATION]` trong spec. Các giả định hiển thị ở phần Assumptions của spec được dùng làm mặc định: line đếm ticket theo ngày Updated; burndown đếm ticket chưa hoàn tất và category `done` được loại trừ; status category thiếu được xem là chưa xác nhận hoàn tất và vẫn tính vào remaining, đồng thời nhóm dữ liệu thiếu được chú thích.

## Research & Design Decisions

Chi tiết quyết định, lý do và phương án thay thế nằm trong [research.md](./research.md).

1. Tích hợp `BurndownSnapshot` trực tiếp trong `SavedTable`; JSON catalog atomic write hiện có đảm bảo snapshot và nội dung mới của bảng được commit cùng nhau. Migration v3 sang v4 mặc định lịch sử rỗng, không tạo lịch sử giả cho bảng cũ.
2. Snapshot lưu thời điểm, tổng ticket và remaining count, không lưu bản sao ticket cho mỗi điểm. Snapshot baseline được ghi cùng thao tác lưu đầu tiên; snapshot refresh chỉ được ghi sau khi Jira refresh hoàn tất và catalog write thành công.
3. Pie/column/line/burndown được tạo từ phép chiếu dữ liệu thuần Kotlin và vẽ bằng Compose Canvas/Material 3; nhãn, giá trị và chú giải hiển thị như nội dung văn bản, kèm semantics để không phụ thuộc màu.
4. Chart screen là màn hình con trong Saved Tables, giống calendar; back trở lại chi tiết cùng bảng, giữ selection và state.
5. Nhóm ba nút dùng cùng component/variant và horizontal spacing 5.dp; điều kiện làm mới hiện có vẫn điều khiển khả dụng/trạng thái riêng.

## Phase 1: Design & Contracts

### Data Model

Mô hình chi tiết tại [data-model.md](./data-model.md):

- `BurndownSnapshot`: thời điểm ghi nhận, số ticket tổng và số chưa hoàn tất.
- `SavedTable.burndownSnapshots`: lịch sử nối thêm theo thời gian; giữ toàn bộ vòng đời bảng.
- Schema v4; migration v1/v2/v3 bảo toàn dữ liệu hiện có và mặc định lịch sử rỗng.
- Chart projections: category/value cho pie/column; ngày/đếm cập nhật cho line; timestamp/remaining cho burndown.

### UI Contract

Hợp đồng navigation, loại chart, trạng thái, nhãn và thao tác được mô tả trong [contracts/saved-table-charts.md](./contracts/saved-table-charts.md). Không thêm API public hay Jira endpoint.

### Quickstart

Các tình huống tự động và kiểm tra Desktop thủ công nằm trong [quickstart.md](./quickstart.md).

## Project Structure

### Tài liệu tính năng

```text
specs/011-saved-table-charts/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── saved-table-charts.md
```

### Mã nguồn dự kiến

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   ├── SavedTable.kt                  # BurndownSnapshot và lịch sử theo bảng
│   └── SavedTableChartCalculator.kt   # Chart projection thuần, không phụ thuộc UI
├── data/
│   └── SavedTableStore.kt             # Migration catalog v3 -> v4
└── ui/saved/
    ├── SavedTablesViewModel.kt        # Capture snapshots cùng save/refresh và chart sub-screen state
    ├── SavedTableChartsArea.kt        # Chart selector, labels/legend, Canvas charts
    └── SavedTablesArea.kt             # Shared action button row và route

composeApp/src/commonTest/kotlin/com/jiratools/
├── domain/SavedTableChartCalculatorTest.kt
├── data/SavedTableStoreTest.kt
└── ui/saved/SavedTablesViewModelTest.kt
```

**Structure Decision**: Giữ một module `composeApp`; phép tính domain, persistence và UI state đặt trong các package hiện có của commonMain. Không tạo module hay database mới.

## Constitution Check sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Ranh giới đa nền tảng | PASS | Chart projection và snapshot model chia sẻ; không phụ thuộc API Desktop. |
| Test-first | PASS | Các phép tính và các chuyển trạng thái lưu/làm mới/migration có commonTest. |
| Nghiệp vụ ngoài UI | PASS | Category aggregation, remaining count và snapshot policy thuộc domain/ViewModel; composable không tự quyết định dữ liệu nghiệp vụ. |
| Kiểu an toàn/đơn giản | PASS | Enum/sealed chart type, snapshot data class; reuse JSON serialization và Material 3. |
| Bảo toàn hành vi | PASS | Thao tác Jira hiện hữu không đổi; refresh thất bại không đổi bảng/snapshot; navigation back khôi phục chi tiết bảng. |

Không có vi phạm hiến pháp cần Complexity Tracking. Lịch sử snapshot vô hạn là lựa chọn sản phẩm đã được xác nhận; record chỉ giữ các số đếm tổng hợp để giảm tăng trưởng dung lượng so với lưu bản sao mọi ticket.

## Complexity Tracking

Không có vi phạm hiến pháp.
