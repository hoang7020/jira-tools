# Tasks: Highlight overdue Dynamic Due Date

**Input**: Design documents from `/specs/017-overdue-red-row/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Spec has explicit acceptance scenarios, so this list includes test-first tasks.

## Phase 1: Setup

**Purpose**: Xác nhận điểm tích hợp hiện có cho ticket và bảng đã lưu.

- [X] T001 Review existing saved-table row rendering and clock APIs in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` and `composeApp/src/commonMain/kotlin/com/jiratools/platform/AppClock.kt`.
- [X] T002 Review shared TicketTable presentation parameters in `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.

---

## Phase 2: Foundational

**Purpose**: Domain helper dùng chung cho cả hai user story.

- [X] T003 Add the overdue domain API in `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketOverdue.kt`.

**Checkpoint**: Domain API ready for red/green tests.

---

## Phase 3: User Story 1 - Tô đỏ dòng quá hạn trong bảng đã lưu (Priority: P1) 🎯 MVP

**Goal**: Ticket có Dynamic Due Date nhỏ hơn thời điểm hiện tại theo giây được tô đỏ toàn dòng trong bảng đã lưu.

**Independent Test**: Với ticket quá hạn, bằng hạn, tương lai và thiếu timestamp, chỉ ticket quá hạn được tô đỏ.

### Tests for User Story 1

- [X] T004 [P] [US1] Write timestamp boundary tests for overdue, equal, future, null, and invalid values in `composeApp/src/commonTest/kotlin/com/jiratools/domain/TicketOverdueTest.kt`.
- [X] T005 [P] [US1] Verify saved-table row contract applies red background only when `highlightOverdueRows=true` and `dynamicDueAtMillis/1000 < nowMillis/1000` in `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesAreaTest.kt`.

### Implementation for User Story 1

- [X] T006 [US1] Implement second-level overdue comparison in `composeApp/src/commonMain/kotlin/com/jiratools/domain/TicketOverdue.kt`.
- [X] T007 [US1] Add optional `highlightOverdueRows` and `nowAtMillis` presentation parameters to `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.
- [X] T008 [US1] Apply overdue red background to the full saved-table row in `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.
- [X] T009 [US1] Enable overdue highlighting only for saved-table rendering in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`.

**Checkpoint**: User Story 1 is independently functional.

---

## Phase 4: User Story 2 - Cập nhật trạng thái đỏ theo từng giây (Priority: P2)

**Goal**: Bảng đã lưu chuyển màu đỏ đúng sau khi Dynamic Due Date trôi qua mốc giây mà không cần reload.

**Independent Test**: Với due date vài giây trong tương lai, ticker chuyển dòng sang đỏ ngay sau mốc due date và giữ đỏ sau đó.

### Tests for User Story 2

- [X] T010 [P] [US2] Write one-second clock transition tests for before, at, and after the due-second boundary in `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesAreaTest.kt`.

### Implementation for User Story 2

- [X] T011 [US2] Add lifecycle-safe one-second current-time refresh for the saved-table detail in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`.
- [X] T012 [US2] Pass the refreshed current timestamp to the saved-table TicketTable in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`.
- [X] T013 [US2] Ensure query-result TicketTable leaves overdue highlighting disabled in `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.

**Checkpoint**: User Stories 1 and 2 are complete.

---

## Phase 5: Polish

**Purpose**: Cross-cutting validation and documentation.

- [X] T014 [P] Run focused overdue domain and saved-table tests in `composeApp/src/commonTest`.
- [X] T015 [P] Run full JVM tests and ktlint in the repository root.
- [X] T016 Validate the overdue-row scenarios in `specs/017-overdue-red-row/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup; blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational.
- **User Story 2 (Phase 4)**: Depends on User Story 1 presentation and clock integration.
- **Polish (Phase 5)**: Depends on both user stories.

### User Story Dependencies

- **US1**: Independent and recommended MVP.
- **US2**: Integrates with US1 row presentation and can be tested independently with clock boundary cases.

### Parallel Opportunities

- T004 and T005 can run in parallel.
- T010 can run in parallel with code review of US1 after T004-T009.
- T014 and T015 can run in parallel.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Setup and Foundational.
2. Complete US1.
3. Stop and validate static overdue red-row behavior.

### Incremental Delivery

1. Add ticking behavior for US2.
2. Run full validation.
