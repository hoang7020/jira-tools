# Tasks: Calendar Ticket Jira Links

**Input**: Design documents from `/specs/018-calendar-ticket-links/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/calendar-ticket-click.md, quickstart.md

## Phase 1: Setup

**Purpose**: Xác nhận các điểm tích hợp mở ticket và Calendar đang tồn tại.

- [X] T001 Review Calendar ticket-label rendering in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt`.
- [X] T002 Review saved-table Calendar callback wiring from `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt` to `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`.
- [X] T003 Review existing saved-table `openTicket` URL and error behavior in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`.

---

## Phase 2: Foundational

**Purpose**: Không cần infrastructure mới; feature tái dụng luồng URL derivation và `UrlOpener` đã có.

- [X] T004 Confirm reusable `browseUrl`, `UrlOpener`, and saved-table `openTicket` contract in `composeApp/src/commonMain/kotlin/com/jiratools/domain/BrowseUrl.kt`, `composeApp/src/commonMain/kotlin/com/jiratools/platform`, and `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`.

**Checkpoint**: Shared opening behavior is ready without new persistence or API dependencies.

---

## Phase 3: User Story 1 - Mở ticket Jira từ Calendar (Priority: P1)

**Goal**: Click mã ticket trong Calendar mở đúng trang Jira ticket bằng một lần click.

**Independent Test**: Với một ticket có mã hợp lệ, click phần mã trong Calendar và xác nhận callback nhận đúng `tableId` và `ticketKey`.

### Tests for User Story 1

- [X] T005 [P] [US1] Add Calendar ticket-click wiring tests for successful same-table opening in `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`.

### Implementation for User Story 1

- [X] T006 [US1] Add a clickable ticket-key callback parameter to `SavedTableCalendarArea` in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt`.
- [X] T007 [US1] Attach click handling only to `ticketKey` in `TicketCalendarLabel` and keep timeline click targets unchanged in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt`.
- [X] T008 [US1] Pass the selected saved-table ticket-key callback from `SavedTablesArea` to `SavedTableCalendarArea` in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`.
- [X] T009 [US1] Connect the Calendar callback to `SavedTablesViewModel.openTicket(tableId, ticketKey)` through the existing message flow in `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`.

**Checkpoint**: User Story 1 can open the selected ticket while Calendar state remains unchanged.

---

## Phase 4: User Story 2 - Thông báo khi không thể mở Jira (Priority: P2)

**Goal**: Các lỗi địa chỉ Jira hoặc mở trình duyệt được báo rõ mà không làm Calendar bị gián đoạn.

**Independent Test**: Với invalid Jira base URL hoặc URL opener failure, click ticket-key và xác nhận lỗi hiện lên mà Calendar vẫn hiển thị.

### Tests for User Story 2

- [X] T010 [P] [US2] Extend saved-table open-ticket tests for invalid base URL and opener failure in `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`.

### Implementation for User Story 2

- [X] T011 [US2] Preserve the existing invalid-base-url and browser-failure messages for Calendar ticket clicks in `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`.
- [X] T012 [US2] Ensure Calendar ticket-click errors use the existing non-blocking saved-table message flow in `composeApp/src/commonMain/kotlin/com/jiratools/app/App.kt`.

**Checkpoint**: User Story 2 reports failures without closing or resetting Calendar.

---

## Phase 5: Polish

**Purpose**: Validation and documentation alignment.

- [X] T013 [P] Run focused saved-table ticket-opening tests in `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`.
- [X] T014 [P] Run full JVM tests and ktlint in the repository root.
- [X] T015 Validate Calendar ticket-link scenarios in `specs/018-calendar-ticket-links/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): No dependencies.
- Foundational (Phase 2): Depends on Setup; blocks all user stories.
- User Story 1 (Phase 3): Depends on Foundational.
- User Story 2 (Phase 4): Depends on User Story 1 callback wiring.
- Polish (Phase 5): Depends on both user stories.

### User Story Dependencies

- US1: Independent MVP once the existing opening flow is confirmed.
- US2: Reuses the US1 callback and existing ViewModel error behavior.

### Parallel Opportunities

- T001-T003 can be reviewed in parallel.
- T005 and T010 can be written in parallel because they both extend the same existing ViewModel test file and should be coordinated before implementation.
- T013 and T014 can run in parallel after implementation.

### Implementation Strategy

1. Complete setup and foundational review.
2. Implement and validate US1 as the MVP.
3. Add and validate US2 through the existing error flow.
4. Run focused and full validation.
