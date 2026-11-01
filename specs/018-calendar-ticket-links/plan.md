# Implementation Plan: Calendar Ticket Jira Links

**Branch**: `018-calendar-ticket-links` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Cho phép người dùng click mã ticket trong Calendar của bảng đã lưu để mở đúng trang Jira ticket trong trình duyệt mặc định. Thiết kế tái dụng URL derivation và cơ chế mở liên kết hiện có cho bảng đã lưu, đồng thời truyền callback mở ticket từ Calendar UI về ViewModel để xử lý lỗi nhất quán.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin, Kotlin Multiplatform/Compose Desktop, JDK 17

**Primary Dependencies**: Compose Multiplatform, coroutines, URL/browser opener hiện có của ứng dụng

**Storage**: Không có thay đổi; địa chỉ Jira được lấy từ bảng đã lưu hiện có

**Testing**: Kotlin common tests, JVM tests và ktlint

**Target Platform**: Desktop JVM

**Project Type**: Desktop application

**Performance Goals**: Mở URL trong một lần click, không chặn luồng UI

**Constraints**: Chỉ mã ticket kích hoạt mở link; không thay đổi cuộn hoặc dữ liệu Calendar

**Scale/Scope**: Một bảng Calendar và các dòng ticket hiện có; không thêm persistence hoặc API mới

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Kết quả |
|---|---|
| Logic dùng chung trong commonMain | PASS |
| Test-first cho logic dùng chung | PASS |
| UI tách khỏi nghiệp vụ qua callback/ViewModel | PASS |
| Kotlin idiomatic và type-safe | PASS |
| Giải pháp đơn giản, tái dụng mở ticket hiện có | PASS |

## Project Structure

### Documentation (this feature)

```text
specs/018-calendar-ticket-links/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
```text
composeApp/
├── src/commonMain/kotlin/com/jiratools/ui/saved/
│   ├── SavedTableCalendarArea.kt
│   ├── SavedTablesArea.kt
│   └── SavedTablesViewModel.kt
└── src/commonTest/kotlin/com/jiratools/ui/saved/
```

**Structure Decision**: Giữ nguyên cấu trúc một module Compose Desktop; thay đổi chỉ ở Calendar UI và callback đã có của SavedTablesArea/ViewModel.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Không có vi phạm Constitution cần biện minh.
