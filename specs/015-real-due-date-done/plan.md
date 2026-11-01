# Implementation Plan: Bổ sung Done cho Real Due Date

**Branch**: `015-real-due-date-done` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Bổ sung trạng thái `Done` vào tập trạng thái đủ điều kiện của mapper Jira status changelog. Hệ thống tiếp tục chọn changelog status mới nhất trong số `Close`, `Resolve` và `Done`, khớp tên không phân biệt hoa thường sau khi trim, và tái sử dụng luồng snapshot, hiển thị bảng và calendar hiện có.

## Technical Context

**Language/Version**: Kotlin stable, JVM 17, KMP hiện có

**Primary Dependencies**: Kotlin coroutines, kotlinx.serialization, Ktor, Compose Multiplatform

**Storage**: Saved-table snapshot JSON hiện có

**Testing**: Kotlin commonTest trên JVM, MockEngine cho Jira, ktlint

**Target Platform**: Desktop JVM

**Project Type**: Kotlin Multiplatform desktop application

**Performance Goals**: Không thay đổi độ trễ refresh; thêm tối đa một phép khớp trạng thái cho mỗi status item

**Constraints**: Không gọi Jira thật trong test; chỉ mapper status và snapshot hiện có được thay đổi

**Scale/Scope**: Một mapper và bộ test hồi quy; không thay đổi schema hoặc UI

## Constitution Check

| Gate | Kết quả |
|---|---|
| Logic chia sẻ trong commonMain | Đạt |
| Test-first trong commonTest | Đạt |
| Không có thay đổi UI nghiệp vụ | Đạt |
| Không thêm dependency hoặc schema | Đạt |
| Build Desktop và chất lượng hiện có | Đạt, kiểm tra sau triển khai |

## Project Structure

### Documentation (feature)

```text
specs/015-real-due-date-done/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code

```text
composeApp/src/commonMain/kotlin/com/jiratools/data/
└── StatusChangelogMapper.kt

composeApp/src/commonTest/kotlin/com/jiratools/data/
└── StatusChangelogMapperTest.kt
```

**Structure Decision**: Tiếp tục mô hình một module KMP ứng dụng; thay đổi thực thi chỉ nằm trong shared data mapper và commonTest.

## Complexity Tracking

Không có vi phạm Hiến pháp cần ghi nhận.
