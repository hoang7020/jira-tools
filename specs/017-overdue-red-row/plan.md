# Implementation Plan: Highlight overdue Dynamic Due Date

**Branch**: `017-overdue-red-row` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/017-overdue-red-row/spec.md`

## Summary

Tô đỏ toàn bộ dòng ticket quá hạn trong bảng đã lưu khi Dynamic Due Date nhỏ hơn thời điểm hiện tại tính theo giây. Logic xác định quá hạn nằm trong domain/common code; màn hình đã lưu cung cấp thời điểm hiện tại theo chu kỳ một giây và truyền trạng thái này qua thành phần bảng dùng chung.

## Technical Context

**Language/Version**: Kotlin 2.x, JVM 17

**Primary Dependencies**: Kotlin Multiplatform, Compose Multiplatform, kotlinx.datetime, kotlinx.coroutines

**Storage**: Snapshot bảng đã lưu hiện có; không thêm dữ liệu mới

**Testing**: commonTest JVM, Compose UI integration tests nếu phù hợp, ktlint

**Target Platform**: Compose Desktop JVM

**Project Type**: Ứng dụng desktop Kotlin Multiplatform, một module `composeApp`

**Performance Goals**: Trạng thái đỏ được cập nhật trong vòng tối đa một giây sau mốc quá hạn; không làm thay đổi tốc độ cuộn/list.

**Constraints**: Chỉ áp dụng cho bảng đã lưu; so sánh theo epoch millisecond nhưng chuẩn hóa cùng mốc giây; dữ liệu không hợp lệ không được đánh dấu.

**Scale/Scope**: Các dòng trong một bảng đã lưu; không thay đổi API Jira hoặc persistence.

## Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| I. Platform-first | PASS | Logic so sánh quá hạn trong commonMain; UI desktop chỉ cung cấp đồng hồ. |
| II. Test-first | PASS | Thêm commonTest cho các mốc trước/bằng/sau hạn. |
| III. Shared logic/selected UI | PASS | Quy tắc nghiệp vụ không nằm trong Compose. |
| IV. Type-safe Kotlin | PASS | Dùng timestamp Long? và hàm kiểm tra thuần. |
| V. Simplicity | PASS | Không thêm module hoặc persistence mới. |

## Project Structure

### Documentation

```text
specs/017-overdue-red-row/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
```

### Source Code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   ├── Ticket.kt
│   └── TicketOverdue.kt
└── ui/
    ├── query/TicketTable.kt
    └── saved/SavedTablesArea.kt

composeApp/src/commonTest/kotlin/com/jiratools/domain/
└── TicketOverdueTest.kt
```

**Structure Decision**: Chỉ mở rộng module hiện có. Hàm domain kiểm tra quá hạn được tái sử dụng theo tham số thời điểm hiện tại để kiểm thử độc lập với clock; UI bảng đã lưu refresh clock theo chu kỳ giây.

## Complexity Tracking

Không có vi phạm nguyên tắc cần ngoại lệ.
