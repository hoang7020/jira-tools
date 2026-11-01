# Kế hoạch triển khai: Ngày hoàn thành thực tế và quá hạn

**Branch**: `007-real-due-date` | **Date**: 2026-11-01 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả feature 007 về Real Due Date và đoạn quá hạn trong calendar.

## Summary

Trong mỗi lần làm mới bảng đã lưu, đọc lịch sử trạng thái Jira cho các issue của kết quả bằng bulk changelog, xác định chuyển trạng thái gần nhất sang trạng thái có tên Close hoặc Resolve, rồi lưu thời điểm sự kiện vào ticket snapshot dưới dạng Real Due Date. Bảng đã lưu hiển thị cột mới; calendar chiếu riêng phần sau Dynamic Due Date đến Real Due Date màu đỏ. Nếu API changelog thất bại, refresh vẫn cập nhật kết quả tìm kiếm nhưng giữ ngày thực tế đã có cho từng issue và thông báo dữ liệu chưa cập nhật; thao tác hủy refresh giữ nguyên hành vi hiện tại là không commit snapshot.

## Technical Context

**Language/Version**: Kotlin 2.4.21; Kotlin Multiplatform, Desktop JVM 17.
**Primary Dependencies**: Compose Multiplatform 1.12.2, Ktor 3.6.1, kotlinx.serialization 1.12.0, kotlinx-datetime 0.8.0; không cần thêm dependency.
**Storage**: Catalog JSON cục bộ; tăng schema bảng đã lưu từ v2 lên v3. Real Due Date được lưu trong ticket snapshot. Các field mới nullable với giá trị mặc định để JSON hiện có tiếp tục được giải mã.
**Testing**: `commonTest` qua `./gradlew :composeApp:jvmTest`; Jira REST giả lập bằng Ktor `MockEngine`; kiểm tra format bằng `./gradlew ktlintCheck`.
**Target Platform**: Desktop JVM hiện tại; nghiệp vụ, DTO, projection và state trong `commonMain`.
**Project Type**: Ứng dụng desktop Compose Multiplatform trong module hiện có `composeApp`.
**Performance Goals**: Không gọi API changelog riêng cho từng issue. Gộp tối đa 1000 issue mỗi yêu cầu bulk, lọc trường `status`, theo hết token phân trang. Không chặn luồng UI; xử lý trên dispatcher hiện dùng cho Jira.
**Constraints**: Chỉ khớp tên trạng thái đích Close/Resolve, không phân biệt hoa thường. Dùng timestamp tạo changelog của lần status transition, không dùng `fields.updated` hiện tại có thể đã đổi sau đó. Kết quả lịch sử chỉ được commit nếu toàn bộ trang changelog tải thành công; khi thất bại giữ ngày đã lưu và báo rõ. Giới hạn ngày và timezone của calendar không đổi.
**Scale/Scope**: Tập issue bằng số lượng kết quả của một bảng đã lưu. Bulk endpoint nhận tối đa 1000 issue IDs mỗi truy vấn; mọi changelog status cần thiết được đọc qua token phân trang. Không thêm module, file store mới, hoặc gọi Jira khi chỉ mở calendar.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | DTO, chọn sự kiện trạng thái, parse thời gian, projection calendar và quy tắc refresh nằm trong `commonMain`; không dùng API JVM. |
| II. Kiểm thử trước | PASS | Bổ sung kiểm thử `commonTest` cho mapper, tính Real Due Date, projection, migration và refresh. API Jira kiểm thử với `MockEngine`; không gọi Jira thật. |
| III. Hybrid UI | PASS | ViewModel công bố trạng thái refresh/cảnh báo; Compose chỉ render cột và các đoạn timeline. |
| IV. Kotlin an toàn kiểu | PASS | Dùng DTO/domain data class nullable, `kotlinx.serialization`, coroutine/Flow hiện có; không thêm dependency. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng client, Ticket, SavedTableStore và projection calendar; không thêm module hoặc abstraction ngoài nhu cầu. |

### Sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Đặc tả rõ ràng | PASS | Tên status đích, quy tắc lấy thời điểm, ngày bắt đầu phần đỏ và lỗi thiếu lịch sử đã được làm rõ. |
| Ranh giới đa nền tảng | PASS | Thời điểm và phép chiếu ngày dùng `kotlinx-datetime` trong `commonMain`. |
| Kiểm thử/tích hợp | PASS | Bulk changelog được mock; lỗi bất kỳ trang nào không commit một tập lịch sử dở dang. |
| Bảo mật | PASS | Chỉ dùng credentials hiện có trong Jira client; không lưu hoặc log token. |
| Tính đơn giản | PASS | Ngày thực tế thuộc ticket snapshot thay vì thêm ledger/file riêng; cột mới chỉ thuộc saved table. |

Không có vi phạm hiến pháp cần ghi nhận trong Complexity Tracking.

## Thiết kế

1. **Jira changelog**: Bổ sung DTO và phương thức API bulk `POST /rest/api/3/changelog/bulkfetch`; gửi ID issue từ kết quả tìm kiếm, `fieldIds = ["status"]`, chia nhóm không quá 1000 issue và tiếp tục `nextPageToken` đến khi hết. Ánh xạ status history theo issue ID. Chọn changelog mới nhất có item `status` với tên trạng thái đích `Close` hoặc `Resolve` sau khi trim và so sánh không phân biệt hoa thường. Dùng timestamp của changelog; nếu không có sự kiện đủ điều kiện trong lịch sử tải đầy đủ, giá trị là null.
2. **Refresh có lỗi lịch sử**: Hoàn tất tải các trang JQL trước, sau đó tải đầy đủ changelog status. Chỉ khi lịch sử đầy đủ thành công mới thay giá trị Real Due Date của snapshot theo kết quả. Nếu changelog trả lỗi API/mạng, vẫn áp dụng snapshot search mới, giữ Real Due Date trước đó khớp cùng `issueId` (fallback theo key nếu cần) cho từng ticket còn lại; issue mới không có giá trị. Phát thông báo refresh thành công một phần, không dùng dữ liệu changelog phân trang một phần. Nếu coroutine bị hủy, thoát theo quy tắc hiện tại và không commit thay đổi nào.
3. **Domain và cột**: Thêm `realDueDate: String?` và `realDueAtMillis: Long?` vào `Ticket`, thêm `ColumnKey.REAL_DUE_DATE` chỉ vào tập cột saved table, và hiển thị formatted date trong bảng. Chuyển changelog timestamp thành millis và chuỗi theo formatter Updated hiện có; projection calendar dùng millis và timezone Jira của bảng. Không thêm cột vào kết quả truy vấn thường.
4. **Lưu trữ và migration**: Nâng `SAVED_TABLES_SCHEMA_VERSION` lên 3. Khi load schema v2, giữ nguyên bảng, ticket, cột và Dynamic Start Time catalog; thêm Real Due Date vào cột saved table. Schema v1 được migrate trực tiếp đến v3 với các migration v1→v2 hiện hành rồi áp dụng bổ sung v3. Các field nullable mặc định null cho JSON cũ; không thể khôi phục giá trị cho tới lần refresh thành công có changelog. Schema hỏng/tương lai tiếp tục chặn ghi theo hành vi hiện tại.
5. **Calendar**: Mở rộng projection hàng calendar để chiếu Dynamic Start/Due hiện có cùng Real Due Date. Khi các mốc hợp lệ và Real Due Date thuộc ngày sau Dynamic Due Date, chiếu đoạn đỏ inclusive từ ngày kế tiếp Dynamic Due Date đến ngày Real Due Date; nếu cùng ngày hoặc sớm hơn thì không có đoạn đỏ. Cắt phần hiển thị theo cùng cửa sổ calendar, giữ cờ tiếp diễn ở cả mép trái và mép phải, hoặc lý do ngoài cửa sổ nếu không có phần giao. Đoạn đỏ là dấu hiệu duy nhất, không thêm nhãn hoặc phân biệt nét.
6. **Tương thích UX**: Lịch vẫn đọc snapshot đã lưu, giữ hàng/thứ tự hiện tại, không gọi Jira khi mở. Khi history lỗi, biểu diễn thông báo refresh chưa cập nhật ở trạng thái bảng hiện có; dữ liệu ngày cũ tiếp tục khả dụng.

## Project Structure

### Tài liệu feature

```text
specs/007-real-due-date/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── jira-real-due-date.md
```

### Source code

```text
composeApp/src/commonMain/kotlin/com/jiratools/
├── data/
│   ├── JiraApiClient.kt             # bulk changelog call
│   ├── JiraDtos.kt                  # DTO changelog
│   ├── TicketMapper.kt              # map history/status timestamp
│   └── SavedTableStore.kt           # catalog schema migration
├── domain/
│   ├── Ticket.kt                    # Real Due Date snapshot fields
│   ├── ColumnKey.kt                 # saved-table-only column
│   └── SavedTableCalendar.kt        # overdue segment projection
└── ui/
    ├── query/TicketTable.kt         # shared table value rendering
    └── saved/
        ├── SavedTablesViewModel.kt  # refresh/fallback/reporting
        └── SavedTableCalendarArea.kt # red timeline segment

composeApp/src/commonTest/kotlin/com/jiratools/
├── data/                            # mapping, REST mock, migration
├── domain/                          # calendar projection
└── ui/saved/                        # refresh atomicity/fallback
```

**Structure Decision**: Giữ một module KMP hiện có. Jira transport và mapping thuộc `data`; quy tắc thời gian/calendar thuộc `domain`; orchestration trạng thái refresh thuộc ViewModel; Compose chỉ hiển thị.

## Complexity Tracking

Không có vi phạm hiến pháp hoặc độ phức tạp cần biện minh.
