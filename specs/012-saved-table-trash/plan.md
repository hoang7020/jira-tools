# Implementation Plan: Thùng rác bảng đã lưu

**Branch**: `012-saved-table-trash` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả tính năng tại `specs/012-saved-table-trash/spec.md`

## Summary

Thêm destination Thùng rác ở sidebar và chuyển thao tác xoá bảng hiện tại thành chuyển vào thùng rác. Catalog JSON sẽ giữ các mục đã xoá cùng `deletedAtMillis`, nguyên trạng bảng và lịch sử burndown; khôi phục trả lại bảng hiện hành hoặc yêu cầu tên mới nếu xung đột; xoá vĩnh viễn yêu cầu xác nhận riêng. Thực hiện trên catalog hiện có bằng một atomic write cho mỗi chuyển trạng thái; nâng schema v4 lên v5, các catalog cũ khởi tạo thùng rác rỗng.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Kotlin Multiplatform; JVM bytecode target 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, Material 3, kotlinx.serialization, kotlinx.coroutines hiện có; không thêm dependency.  
**Storage**: File catalog JSON `saved-tables.json`; schema v5 thêm danh sách mục thùng rác. Các thao tác cập nhật danh sách hoạt động và thùng rác được ghi cùng catalog.  
**Testing**: commonTest cho model/lifecycle, migration v1-v4, ViewModel state và lỗi ghi; JVM test, ktlint và Desktop build. UI xác minh theo quickstart.  
**Target Platform**: Ứng dụng Desktop JVM; logic và trạng thái dùng commonMain.  
**Project Type**: Ứng dụng Compose Multiplatform desktop, module `composeApp`.  
**Performance Goals**: Mở và điều hướng thùng rác chỉ đọc dữ liệu local; không gọi Jira. Mỗi thao tác move/restore/permanent delete dùng một catalog write.  
**Constraints**: Không mất dữ liệu khi write lỗi; không ghi đè tên trùng; giữ nguyên nội dung và burndown khi move/restore; không tự dọn thùng rác; giữ ledger Dynamic Start Time dùng chung cho các bảng.  
**Scale/Scope**: Một local catalog; danh sách bảng hiện hành và thùng rác; thao tác theo từng bảng, không có xoá/khôi phục hàng loạt hoặc tự động hết hạn.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Model thùng rác, persistence lifecycle và ViewModel ở commonMain; UI dùng Compose Multiplatform. |
| II. Kiểm thử trước | PASS | Viết commonTest cho migration, tên trùng, atomic move/restore/delete và lỗi lưu trước khi triển khai logic. |
| III. Hybrid UI | PASS | ViewModel công bố StateFlow; composable render danh sách và gửi callback, không tự quyết định luật nghiệp vụ. |
| IV. Kotlin an toàn kiểu | PASS | Model data class serializable, state immutable và thao tác suspend; không thêm dependency. |
| V. Đơn giản và gia tăng | PASS | Tái dùng catalog JSON, SavedTablesViewModel, AppNavigation và confirm-dialog hiện có; không thêm database/module. |

Không có `[NEEDS CLARIFICATION]` còn lại. Xung đột tên khi khôi phục đã được chốt: yêu cầu tên khác; không cho phép ghi đè hoặc trùng tên hoạt động.

## Research & Design Decisions

Chi tiết quyết định, lý do và phương án thay thế nằm trong [research.md](./research.md).

1. Lưu `TrashedSavedTable` trong `SavedTableCatalog` cùng danh sách bảng hoạt động; schema v5 khởi tạo trash rỗng khi đọc catalog v1-v4. Giữ nguyên payload `SavedTable` để khôi phục không mất field mới/cũ.
2. Move/restore/permanent delete tính catalog mới bất biến và ghi đúng một lần; cập nhật StateFlow chỉ sau khi write thành công. Nếu write lỗi, file và UI tiếp tục thể hiện catalog trước thao tác.
3. Mục thùng rác được sắp theo `deletedAtMillis` giảm dần. Thùng rác chỉ có thao tác Khôi phục và Xoá vĩnh viễn; không cho mở chart/calendar hoặc refresh khi chưa khôi phục.
4. Khôi phục kiểm tra tên bằng quy tắc hiện hành (trim, không rỗng, không trùng không phân biệt hoa/thường). Tên mới chỉ thay `SavedTable.name`; ID và các trường dữ liệu khác không đổi.
5. Ledger Dynamic Start Time ở catalog không thuộc riêng một bảng và có thể phục vụ các bảng khác; xóa vĩnh viễn bảng không xóa ledger chia sẻ. Ticket, snapshot burndown và mọi nội dung `SavedTable` của mục bị xóa vĩnh viễn sẽ bị loại bỏ.

## Phase 1: Design & Contracts

### Data Model

Chi tiết tại [data-model.md](./data-model.md):

- `TrashedSavedTable`: ảnh chụp nguyên vẹn `SavedTable` cùng thời điểm chuyển vào thùng rác.
- `SavedTableCatalog.trashedTables`: danh sách các mục thùng rác độc lập với bảng hiện hành.
- Lifecycle: active → trashed → active hoặc permanently deleted.
- Schema v5; dữ liệu schema v1-v4 được giữ nguyên và có trash rỗng.
- Quy tắc tên khôi phục dùng `validateTableName` hiện tại.

### UI Contract

Hợp đồng destination sidebar, danh sách rỗng, thao tác restore, tên xung đột và xác nhận xoá vĩnh viễn được mô tả trong [contracts/saved-table-trash.md](./contracts/saved-table-trash.md). Không thêm API public hoặc Jira endpoint.

### Quickstart

Các tình huống tự động và xác minh Desktop thủ công nằm trong [quickstart.md](./quickstart.md).

## Project Structure

### Tài liệu tính năng

```text
specs/012-saved-table-trash/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── saved-table-trash.md
```

### Mã nguồn dự kiến

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   └── SavedTable.kt                         # TrashedSavedTable và catalog schema v5
├── data/
│   └── SavedTableStore.kt                   # Migration v1-v4 -> v5
├── ui/
│   ├── layout/
│   │   └── AppNavigation.kt                 # Destination Thùng rác
│   └── saved/
│       ├── SavedTablesViewModel.kt           # Move, restore, permanent delete
│       ├── SavedTablesArea.kt                # Tách delete hiện hành thành move-to-trash
│       └── SavedTableTrashArea.kt             # Danh sách/trạng thái trống/thao tác thùng rác
└── app/
    └── App.kt                               # Destination routing và confirm/rename dialogs

composeApp/src/commonTest/kotlin/com/jiratools/
├── data/SavedTableStoreTest.kt               # Migration và round-trip schema v5
├── domain/SavedTableTrashTest.kt              # Model/state/name rules
└── ui/saved/SavedTablesViewModelTest.kt       # Move/restore/delete/write failure
```

**Structure Decision**: Giữ một module `composeApp`; mở rộng catalog/model và ViewModel hiện có, thêm composable cho thùng rác, thêm entry vào enum destination. Không tạo module/database mới.

## Constitution Check sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Ranh giới đa nền tảng | PASS | Model/persistence/state dùng commonMain; UI dùng Compose chung. |
| Test-first | PASS | Các chuyển trạng thái và migration có commonTest chạy trước logic. |
| Nghiệp vụ ngoài UI | PASS | Conflict validation, lifecycle và atomic-commit policy đặt ở ViewModel/domain. |
| Kiểu an toàn/đơn giản | PASS | Data class serializable; reuse catalog và validate tên hiện hành. |
| Bảo toàn dữ liệu | PASS | Move/restore giữ nguyên record; UI state chỉ cập nhật sau catalog write thành công. |

Không có vi phạm hiến pháp cần Complexity Tracking. Không có việc tạo/xoá bí mật hoặc thay đổi tích hợp Jira.

## Complexity Tracking

Không có vi phạm hiến pháp.
