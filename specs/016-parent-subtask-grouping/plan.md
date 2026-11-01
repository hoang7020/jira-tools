# Implementation Plan: Nhóm ticket cha và sub-task

**Branch**: `016-parent-subtask-grouping` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

## Summary

Nhóm các ticket cha/sub-task trong bảng kết quả truy vấn bằng quan hệ cha được Jira trả trong trường `parent` và danh sách `subtasks`. Ticket được Jira nhận diện là sub-task qua `issuetype.subtask`; dòng cha được tô xám mờ khi cha có mặt trong kết quả, bất kể có sub-task hiển thị hay không. Các sub-task được hiển thị bằng cây thư mục trực quan dưới cha. Logic gom nhóm và sort theo nhóm được đặt trong mã dùng chung; Compose chỉ áp dụng màu dòng và nhánh cây.

## Technical Context

**Language/Version**: Kotlin stable, JVM 17, Kotlin Multiplatform hiện có

**Primary Dependencies**: Kotlin coroutines, kotlinx.serialization, Ktor, Compose Multiplatform

**Storage**: Không thay đổi persistence; quan hệ cha-con chỉ được giữ trong phiên kết quả truy vấn

**Testing**: commonTest trên JVM, logic grouping UI-independent, ktlint

**Target Platform**: Desktop JVM

**Project Type**: Kotlin Multiplatform desktop application

**Performance Goals**: Gom nhóm tuyến tính theo số ticket trong một trang kết quả

**Constraints**: Không gọi Jira thật trong test; chỉ xử lý ticket đã có trong kết quả, không tải thêm dữ liệu cha/sub-task

**Scale/Scope**: DTO Jira, Ticket domain, mapper, logic grouping và TicketTable

**Visual Hierarchy**: Nhánh `├` cho sub-task không cuối và `└` cho sub-task cuối trong nhóm cha trực tiếp.

**Sorting**: Sort chạy theo Jira trên toàn bộ truy vấn; kết quả cha-con phải được gom lại thành nhóm atom trước khi hiển thị. Sort chỉ thay đổi thứ tự các nhóm cha và các ticket độc lập, không tách hoặc đổi thứ tự sub-task trong nhóm.

## Constitution Check

| Gate | Kết quả |
|---|---|
| Logic nghiệp vụ nằm trong commonMain | Đạt |
| Test-first và test chung JVM | Đạt |
| Compose không chứa business rule | Đạt |
| Không thêm dependency hoặc schema persistence | Đạt |
| Build Desktop và quality gates | Kiểm tra sau triển khai |

## Project Structure

### Documentation

```text
specs/016-parent-subtask-grouping/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── data/
├── domain/
└── ui/query/

composeApp/src/commonTest/kotlin/com/jiratools/
└── data/
```

**Structure Decision**: Tiếp tục một module KMP. Quan hệ cha-con và thứ tự nhóm nằm trong shared logic; Compose chỉ nhận danh sách đã xử lý và áp dụng màu dòng.

## Complexity Tracking

Không có vi phạm Hiến pháp cần ghi nhận.
