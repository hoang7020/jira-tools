# Kế hoạch triển khai: Đồng bộ Due Date từ bảng đã lưu

**Branch**: `014-saved-due-date-sync` | **Ngày**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Đầu vào**: Đặc tả trong `specs/014-saved-due-date-sync/spec.md`

## Tóm tắt

Trong mỗi hàng của Bảng đã lưu, so sánh Due Date chuẩn trong snapshot với ngày lịch lấy từ Dynamic Due Date theo múi giờ Jira đã lưu. Chỉ khi hai ngày lệch hoặc Due Date trống và ngày Dynamic hợp lệ mới hiện thao tác cập nhật cho hàng đó. Khi người dùng bấm, gửi một yêu cầu sửa đúng issue tới Jira; không ghi tự động khi mở bảng. Jira xác nhận thành công thì cập nhật Due Date trong snapshot và lưu catalog. Lỗi Jira phải hiển thị rõ, cho phép thử lại; lỗi ghi snapshot sau khi Jira đã thành công phải được phân biệt để không báo thành công giả.

## Bối cảnh kỹ thuật

**Ngôn ngữ/phiên bản**: Kotlin 2.4.21; JVM target 17.
**Phụ thuộc chính**: Kotlin Multiplatform, Compose Multiplatform 1.12.2, kotlinx.serialization, kotlinx-datetime, coroutines/Flow, Ktor client hiện có.
**Lưu trữ**: Catalog Bảng đã lưu JSON qua `SavedTableStore`; không cần schema mới vì `Ticket.dueDate` và `SavedTable` đã có.
**Kiểm thử**: `commonTest` với Ktor `MockEngine`, fake `AppPaths`, dispatcher/clock test; `./gradlew :composeApp:jvmTest`.
**Nền tảng đích**: Desktop JVM; logic nghiệp vụ và UI dùng `commonMain`.
**Loại dự án**: Ứng dụng desktop đa nền tảng, module `composeApp`.
**Mục tiêu hiệu năng**: Một yêu cầu Jira chỉ cho mỗi thao tác người dùng trên một issue; không phát sinh request khi render hoặc tải bảng; cập nhật issue khác không bị tuần tự hoá toàn cục.
**Ràng buộc**: Ngày Jira là ngày lịch `yyyy-MM-dd`; lấy ngày từ `dynamicDueAtMillis` theo `SavedTable.jiraTimeZone`, không theo múi giờ thiết bị. Chỉ gửi khi kết nối xác thực đang dùng cùng Jira site với bảng. Cập nhật snapshot sau HTTP thành công; không sửa Dynamic Due Date. Không thêm dependency hoặc schema catalog.
**Phạm vi**: API cập nhật issue, trạng thái tiến trình/lỗi theo issue, nút cập nhật trên hàng SavedTable, snapshot và kiểm thử HTTP/state/UI.

## Kiểm tra Hiến pháp

*Cổng trước Phase 0 và kiểm tra lại sau thiết kế.*

- **I. Đa nền tảng trước**: Đạt. Tính ngày, trạng thái điều phối và tích hợp Ktor ở `commonMain`; không gọi API JVM trực tiếp.
- **II. Test-first**: Đạt theo thiết kế. Test `commonTest` xác minh payload/status Jira qua HTTP fake và các chuyển trạng thái; không gọi Jira thật.
- **III. UI lai**: Đạt. ViewModel phát trạng thái qua `StateFlow`; Compose chỉ hiển thị CTA và gọi hành động.
- **IV. Kotlin an toàn kiểu**: Đạt. Dùng `ApiResult<Unit>`, model trạng thái có kiểu, kotlinx serialization cho request.
- **V. Đơn giản/YAGNI**: Đạt. Tái sử dụng `JiraApiClient`, `SavedTableStore`, Ticket và bảng hiện có; không thêm module, dependency hoặc catalog schema.
- **Bảo mật**: Đạt. Request dùng thông tin xác thực hiện có, không ghi token vào log hay snapshot; không cho thao tác nếu kết nối không khớp Jira site của bảng.
- **Cổng chất lượng**: Chạy JVM tests, ktlint và Desktop build ở cuối triển khai theo `quickstart.md`.

## Cấu trúc dự án

### Tài liệu feature

```text
specs/014-saved-due-date-sync/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── jira-issue-update.md
    └── saved-table-ui.md
```

### Mã nguồn

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── data/
│   ├── JiraApiClient.kt
│   ├── JiraDtos.kt
│   └── SavedTableStore.kt
├── domain/
│   ├── Ticket.kt
│   ├── SavedTable.kt
│   └── WorkingTimeCalculator.kt
└── ui/saved/
    ├── SavedTablesViewModel.kt
    └── SavedTablesArea.kt

composeApp/src/commonTest/kotlin/com/jiratools/
├── data/
│   ├── JiraApiClientIssueUpdateTest.kt
│   └── SavedTableStoreTest.kt
└── ui/saved/
    └── SavedTablesViewModelTest.kt
```

**Quyết định cấu trúc**: Giữ một module KMP hiện có. API ghi thuộc Jira client; quy tắc ngày đích, tiến trình, kết quả và lưu snapshot thuộc SavedTablesViewModel; Compose nhận trạng thái và callback theo hàng.

## Theo dõi độ phức tạp

Không có vi phạm Hiến pháp cần biện minh.
