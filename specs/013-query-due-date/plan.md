# Implementation Plan: Cột Due Date trong kết quả truy vấn

**Branch**: `013-query-due-date` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả `specs/013-query-due-date/spec.md`

## Summary

Thêm trường Due Date chuẩn của Jira vào dữ liệu issue, yêu cầu Jira trả trường này, và hiển thị/sắp xếp nó trong Kết quả truy vấn lẫn Bảng đã lưu. Giữ giá trị ngày lịch không chuyển múi giờ. Cột mới mặc định bật cho truy vấn và bảng mới; migration chỉ tự thêm vào tuỳ chọn truy vấn cũ nếu chúng vẫn đúng bằng bộ cột mặc định trước thay đổi. Lựa chọn cột trong bảng đã lưu hiện hữu không bị thay đổi.

## Technical Context

**Language/Version**: Kotlin 2.4.21; JVM target 17.
**Primary Dependencies**: Kotlin Multiplatform, Compose Multiplatform 1.12.2, kotlinx.serialization, kotlinx-datetime, coroutines/Flow.
**Storage**: Tuỳ chọn người dùng JSON; snapshot Bảng đã lưu JSON.
**Testing**: `commonTest`; chạy bằng `./gradlew :composeApp:jvmTest`; kiểm tra chất lượng bằng `./gradlew :composeApp:ktlintCheck build`.
**Target Platform**: Desktop (JVM); logic và UI hiện tại thuộc KMP `commonMain`.
**Project Type**: Ứng dụng desktop đa nền tảng, một module `composeApp`.
**Performance Goals**: Không thêm round-trip Jira; ánh xạ và sắp xếp cột phải tuyến tính hoặc theo độ phức tạp sắp xếp hiện tại, không làm tăng đáng kể thời gian hiển thị bảng.
**Constraints**: Due Date là ngày lịch `yyyy-MM-dd`; không áp dụng timezone. Nâng schema config từ 3 lên 4 và giữ tương thích JSON đã lưu; không thay đổi Dynamic Due Date/Real Due Date hoặc lựa chọn cột đã lưu của từng bảng cũ.
**Scale/Scope**: Một trường Jira, một cột dùng chung cho query và saved-table; cập nhật model, DTO/mapper, sorting, query preferences, UI, và kiểm thử liên quan.

## Constitution Check

*GATE: Đánh giá trước Phase 0 và sau Phase 1.*

- **I. Đa nền tảng**: Đạt. DTO, model, parse ngày và sorting nằm trong `commonMain`; không cần API JVM.
- **II. Test-first**: Đạt theo kế hoạch. Bổ sung kiểm thử `commonTest` cho DTO/mapper, cấu hình migration, sorting và bảng; Jira HTTP được kiểm thử qua fake/mock hiện có, không gọi Jira thật.
- **III. Hybrid UI**: Đạt. UI chỉ hiển thị giá trị `Ticket.dueDate` và dùng điều khiển cột/sắp xếp hiện có; không thêm nghiệp vụ trong Compose.
- **IV. Kotlin an toàn kiểu**: Đạt. DTO/model typed nullable, serialization hiện có được tái sử dụng; không thêm dependency hoặc phiên bản.
- **V. Đơn giản**: Đạt. Dùng enum cột và model hiện có; không thêm module, abstraction hoặc định dạng lưu mới.
- **Quality gates**: Đạt về kế hoạch; trước khi merge phải chạy JVM tests, ktlint và Desktop build theo quickstart.

**Đánh giá sau Phase 1**: Các quyết định thiết kế không thêm dependency, abstraction hoặc source set đặc thù nền tảng; các nguyên tắc I–V tiếp tục đạt.

## Project Structure

### Tài liệu feature

```text
specs/013-query-due-date/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── ui.md
└── tasks.md
```

### Source code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── data/
│   ├── JiraDtos.kt
│   ├── TicketMapper.kt
│   └── AppConfigStore.kt
├── domain/
│   ├── ColumnKey.kt
│   ├── Ticket.kt
│   ├── TicketSorting.kt
│   └── SavedTable.kt
└── ui/
    ├── query/
    │   ├── QueryViewModel.kt
    │   ├── TicketTable.kt
    │   └── TableColumnLayout.kt
    └── saved/
        └── SavedTablesArea.kt

composeApp/src/commonTest/kotlin/com/jiratools/
├── data/
│   ├── TicketMapperTest.kt
│   └── AppConfigStoreTest.kt
├── domain/
│   └── TicketSortingTest.kt
└── ui/
    ├── query/QueryViewModelPreferencesTest.kt
    └── saved/SavedTablesViewModelTest.kt
```

**Structure Decision**: Giữ một module KMP đang có. Tầng data xử lý Jira và migration cấu hình; domain giữ dữ liệu/sorting; Compose query/saved dùng chung mô hình Ticket và enum ColumnKey.

## Complexity Tracking

Không có vi phạm hiến pháp cần biện minh.
