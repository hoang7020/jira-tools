---

description: "Danh sách công việc triển khai tính năng đồng bộ Due Date"
---

# Tasks: Đồng bộ Due Date từ bảng đã lưu

**Input**: Tài liệu thiết kế trong `/specs/014-saved-due-date-sync/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`

**Tests**: Có task test theo yêu cầu Test-First trong Hiến pháp dự án. Test tích hợp Jira phải dùng HTTP fake/mock, không gọi Jira thật.

**Organization**: Tasks được nhóm theo User Story để có thể triển khai và kiểm chứng độc lập.

## Quy ước đường dẫn

- Mã dùng chung: `composeApp/src/commonMain/kotlin/com/jiratools/`
- Test dùng chung: `composeApp/src/commonTest/kotlin/com/jiratools/`

## Phase 1: Setup

**Mục đích**: Tái sử dụng module KMP, dependency và test harness hiện có; không cần khởi tạo cấu trúc hoặc dependency mới.

Không có thay đổi setup riêng cho feature này.

---

## Phase 2: Foundational

**Mục đích**: Các entity `Ticket`, `SavedTable`, `SavedTableStore` và API client đã tồn tại; không cần migration catalog hoặc hạ tầng nền tảng mới. Các thay đổi sẽ được thực hiện trong User Story 1.

Không có task foundational độc lập.

---

## Phase 3: User Story 1 - Cập nhật Due Date Jira từ bảng đã lưu (Priority: P1) 🎯 MVP

**Mục tiêu**: Hiển thị thao tác cập nhật đúng hàng khi Due Date trống/khác ngày lịch Dynamic Due Date; chỉ gửi thay đổi khi người dùng bấm; báo trạng thái và cập nhật snapshot sau khi Jira thành công.

**Kiểm thử độc lập**: Tạo bảng đã lưu có issue với Due Date trống, Due Date khác ngày Dynamic, ngày khớp, và Dynamic Due Date không hợp lệ/không có. Xác nhận chỉ hai hàng lệch có thao tác, không phát sinh request khi mở bảng, request chỉ cập nhật đúng issue và `fields.duedate`, snapshot chỉ đổi sau Jira 2xx, lỗi cho phép thử lại, và cập nhật issue khác vẫn độc lập.

### Tests for User Story 1

> Viết các test trước phần triển khai và xác nhận chúng thất bại với hành vi chưa được hỗ trợ.

- [X] T001 [P] [US1] Thêm test HTTP cho PUT cập nhật đúng issue, payload chỉ có `fields.duedate` định dạng `yyyy-MM-dd`, mọi phản hồi 2xx kể cả 204 không body, lỗi HTTP và lỗi mạng trong `composeApp/src/commonTest/kotlin/com/jiratools/data/JiraApiClientIssueUpdateTest.kt`.
- [X] T002 [P] [US1] Thêm test ViewModel cho ngày đích theo `jiraTimeZone`, điều kiện hiện thao tác, site/kết nối không phù hợp, chống request trùng, cập nhật snapshot sau thành công, giữ snapshot khi Jira lỗi và phân biệt lỗi lưu local sau Jira 2xx trong `composeApp/src/commonTest/kotlin/com/jiratools/ui/saved/SavedTablesViewModelTest.kt`.

### Implementation for User Story 1

- [X] T003 [US1] Thêm DTO request và hàm cập nhật issue qua `PUT /rest/api/3/issue/{issueIdOrKey}`, ưu tiên `issueId`, dùng `key` khi thiếu, chỉ gửi `fields.duedate`, nhận mọi 2xx kể cả 204 và ánh xạ lỗi theo client hiện có trong `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraDtos.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/data/JiraApiClient.kt`.
- [X] T004 [US1] Thêm hành động đồng bộ theo issue vào ViewModel: chuyển `dynamicDueAtMillis` thành ngày lịch bằng `SavedTable.jiraTimeZone`, không cho cập nhật khi thiếu dữ liệu/ngày hoặc kết nối Jira site không khớp, chặn request trùng cùng issue nhưng không khóa issue khác, và chỉ sửa `Ticket.dueDate` snapshot sau Jira 2xx trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`.
- [X] T005 [US1] Hoàn thiện xử lý lỗi đồng bộ trong ViewModel để lỗi Jira giữ snapshot cũ và cho phép retry, còn lỗi lưu catalog sau Jira 2xx được thể hiện riêng là Jira đã cập nhật nhưng snapshot local chưa lưu trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesViewModel.kt`.
- [X] T006 [US1] Mở rộng callback hàng của bảng theo cách nullable để chỉ Bảng đã lưu cung cấp thao tác; hiển thị ngày đích, trạng thái đang cập nhật, lỗi/retry tại đúng hàng, và giữ nguyên hành vi bảng query trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt` và `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTablesArea.kt`.

**Checkpoint**: User Story 1 hoạt động độc lập; test xác nhận quy tắc ngày/timezone, yêu cầu Jira, cập nhật snapshot và lỗi; kiểm tra UI thủ công xác nhận đúng hàng và không tự gửi request khi mở bảng.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Mục đích**: Xác minh feature trên các cổng chất lượng của dự án và đồng bộ hướng dẫn kiểm chứng.

- [X] T007 Xác nhận hướng dẫn trong `specs/014-saved-due-date-sync/quickstart.md` phù hợp với triển khai; chạy `./gradlew :composeApp:jvmTest` và `./gradlew :composeApp:ktlintCheck build`, xử lý mọi lỗi liên quan trong các file mã nguồn/test của feature.
- [X] T008 Căn giữa dữ liệu theo trục dọc trong toàn bộ dòng bảng đã lưu; căn khối Due Date và nút cập nhật ngay bên dưới như một nhóm giữa ô mà không làm thay đổi chiều cao dòng trong `composeApp/src/commonMain/kotlin/com/jiratools/ui/query/TicketTable.kt`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không cần thay đổi; dùng cấu trúc dự án hiện tại.
- **Foundational (Phase 2)**: Không có task độc lập; các model, store và test harness hiện có được tái sử dụng.
- **User Story 1 (Phase 3)**: Có thể bắt đầu ngay; không phụ thuộc story khác.
- **Polish (Phase 4)**: Phụ thuộc hoàn tất User Story 1.

### User Story Dependencies

- **User Story 1 (P1)**: Không phụ thuộc story khác; là toàn bộ MVP của feature.

### Trong User Story 1

- T001 và T002 có thể thực hiện song song; cả hai test phải được viết và xác nhận thất bại trước triển khai.
- T003 cung cấp API mà T004 sử dụng.
- T005 mở rộng luồng ViewModel của T004.
- T006 cần callback/state từ T004–T005 để tích hợp UI.
- T007 chỉ chạy sau khi hoàn tất các thay đổi và test của story.

## Parallel Example: User Story 1

```text
Song song:
- T001: JiraApiClientIssueUpdateTest.kt
- T002: SavedTablesViewModelTest.kt

Sau khi test đỏ:
- T003 -> T004 -> T005 -> T006 -> T007
```

## Implementation Strategy

### MVP trước

1. Hoàn thành test-first cho HTTP client và ViewModel.
2. Triển khai API Jira và luồng cập nhật snapshot/UI trong User Story 1.
3. Dừng để xác minh độc lập story bằng `./gradlew :composeApp:jvmTest` và kiểm tra UI.
4. Chạy ktlint và Desktop build qua task T007.

### Phân phối gia tăng

User Story 1 là toàn bộ phạm vi người dùng của feature, vì vậy không có story tiếp theo cần chờ hoặc triển khai song song. Chỉ đưa feature vào sử dụng khi request, snapshot, lỗi lưu local và trạng thái hàng đều đã được kiểm chứng.
