# Kế hoạch triển khai: Thời gian động và ước lượng công việc

**Branch identifier**: `005-dynamic-time-columns` (workspace hiện không có Git metadata) | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Đặc tả Feature 005 về Original Expected Time, Dynamic Start Time và Dynamic Due Date.

## Summary

Lấy `timetracking.originalEstimate`, Jira issue ID, status category và `updated` từ phản hồi tìm kiếm Jira hiện có. Original Expected Time được hiển thị trong cả bảng truy vấn và bảng đã lưu. Khi lưu hoặc làm mới bảng đã lưu, lần đầu task được quan sát thuộc status category `indeterminate` (In Progress), lưu `updated` cùng thời điểm tuyệt đối làm Dynamic Start Time. Một sổ theo dõi toàn cục trong danh mục bảng đã lưu chia sẻ mốc theo Jira site + issue ID, không xóa mốc khi task tạm vắng hoặc bảng bị xóa. Dynamic Due Date được tính cục bộ từ mốc đó và Original Expected Time theo thứ Hai-thứ Sáu, 08:00-12:00 và 13:00-17:00, dùng múi giờ tài khoản Jira lấy từ `/myself`. Tất cả phép tính và mô hình mới nằm trong `commonMain`; các file JSON hiện có được nâng phiên bản có migration tương thích.

## Technical Context

**Language/Version**: Kotlin 2.4.21; Kotlin Multiplatform, bytecode Desktop JVM 17.  
**Primary Dependencies**: Compose Multiplatform 1.12.2, Ktor 3.6.1, kotlinx.serialization 1.12.0; thêm `kotlinx-datetime` 0.8.0 qua Gradle version catalog để xử lý `Instant`, múi giờ và lịch ngày trong `commonMain`.  
**Storage**: Các file JSON hiện có; `config.json` schema v2 giữ tùy chọn cột sau migration v1; `saved-tables.json` schema v2 chứa bảng, múi giờ từng bảng và sổ Dynamic Start Time dùng chung. Token tiếp tục ở secure storage của nền tảng.  
**Testing**: `commonTest` chạy trên JVM/Desktop bằng `./gradlew :composeApp:jvmTest`; mock HTTP Jira bằng Ktor `MockEngine`; chạy lint bằng `./gradlew ktlintCheck`.  
**Target Platform**: Desktop JVM hiện tại; nghiệp vụ vẫn tương thích các source set Kotlin Multiplatform khác.  
**Project Type**: Một ứng dụng Compose Multiplatform, module `composeApp`.  
**Performance Goals**: Không thêm request Jira cho từng issue; dùng các trang search hiện có (100 issue/trang). Ánh xạ, cập nhật sổ và tính giá trị hiển thị theo số ticket; không chặn luồng UI. Chỉ ghi một lần sau khi toàn bộ trang refresh thành công.  
**Constraints**: Không đọc lịch sử trạng thái Jira; chỉ bắt đầu khi snapshot lưu/làm mới đầu tiên cho thấy category `indeterminate`. Lịch là thứ Hai-thứ Sáu, 08:00-12:00 và 13:00-17:00; khung giờ là giờ địa phương và tuân theo timezone/DST; không hỗ trợ ngày lễ hay lịch tùy chỉnh. Múi giờ thiếu/không hợp lệ làm Due Date trống kèm cảnh báo. Không gọi endpoint cấu hình time-tracking cần quyền Administer Jira. Giữ nguyên dữ liệu khi refresh thất bại, hủy hoặc ghi file lỗi.  
**Scale/Scope**: Cùng lượng kết quả phân trang mà bảng đã lưu hiện hỗ trợ; thêm ba cột, metadata Jira và một bản ghi bắt đầu cho mỗi issue duy nhất/site. Không thêm module, endpoint hay dịch vụ ngoài.

## Constitution Check

### Trước nghiên cứu

| Nguyên tắc/cổng | Kết quả | Cách đáp ứng |
|---|---|---|
| I. Đa nền tảng trước | PASS | Dùng `kotlinx-datetime` và giữ mapping, parser, lịch, persistence trong `commonMain`; không dùng `java.time`. |
| II. Kiểm thử trước | PASS | Bổ sung kiểm thử `commonTest` cho mapper, parser/lịch, migration và refresh; Jira được giả lập bằng `MockEngine`, không gọi Jira thật. |
| III. Hybrid UI | PASS | ViewModel tiếp tục công bố StateFlow; Compose chỉ hiển thị cột và cảnh báo. |
| IV. Kotlin an toàn kiểu | PASS | Dùng DTO/domain data class, kotlinx.serialization; dependency mới khai báo trong version catalog. |
| V. Đơn giản và gia tăng | PASS | Tái sử dụng client, TicketTable, SavedTableStore và catalog hiện có; không thêm module/DI framework. |

### Sau thiết kế

| Cổng | Kết quả | Ghi chú |
|---|---|---|
| Yêu cầu đã làm rõ | PASS | Tuần làm việc chuẩn Mon-Fri và múi giờ từ `/myself` đã giải quyết giới hạn API; không còn `NEEDS CLARIFICATION`. |
| Ranh giới đa nền tảng | PASS | Parse offset/timestamp, cộng thời lượng theo múi giờ, migration và sắp xếp nằm trong shared Kotlin. |
| Kiểm thử/tích hợp | PASS | Test logic lịch trong `commonTest`; test REST qua MockEngine; migration và transaction catalog có test hồi quy. |
| Tính đơn giản | PASS | Sổ dùng chung ở chính catalog giúp ghi nguyên tử cùng bảng; không thêm file/store giao dịch thứ hai. |
| Bảo mật | PASS | Không đưa token vào model mới hoặc catalog; token vẫn ở secure storage. |

Không có vi phạm hiến pháp cần ghi nhận trong Complexity Tracking.

## Thiết kế

1. **Jira mapping**: Mở rộng DTO/search fields để đọc `IssueDto.id`, `status.statusCategory.key`, `fields.updated` và `fields.timetracking.originalEstimate`. Giữ nguyên estimate gốc để hiển thị; phân tích riêng khi tính. Parse timestamp có offset thành epoch milliseconds nhưng giữ `Ticket.updated` hiện có cho giao diện.
2. **Lịch và parser**: Tạo logic miền thuần trong `commonMain`. Parser nhận các thành phần `s/m/h/d/w`, không phân biệt hoa thường, số thập phân dấu chấm, chỉ chấp nhận toàn bộ chuỗi hợp lệ; `1d = 8h`, `1w = 40h`. Với estimate dương, chuẩn hóa mốc ngoài giờ sang phiên kế tiếp; estimate bằng 0 giữ nguyên đúng mốc start theo spec. Bỏ qua trưa/cuối tuần; khung giờ là giờ địa phương và tuân theo timezone/DST. Dùng múi giờ IANA từ profile Jira; không dùng múi giờ máy. Timestamp Updated Jira dạng `+HHMM` được chuẩn hóa trước khi parse.
3. **Theo dõi dùng chung**: Thêm record `{ normalized Jira base URL, issue ID, start epoch millis, display value }` vào `SavedTableCatalog`. Khóa lookup là cặp base URL chuẩn hóa + issue ID. Khi quan sát lần đầu ở category `indeterminate`, chỉ tạo record nếu Updated parse được; giữ `display value` chính là giá trị Updated ở snapshot đó. Record không bị thay thế hay tự xóa khi issue vắng mặt hoặc bảng bị xóa.
4. **Refresh nguyên tử**: Thu thập mọi trang trước; nếu có lỗi/hủy thì không thay đổi bảng hay sổ. Khi đủ trang, cập nhật record mới và dữ liệu bảng, materialize cùng Dynamic Start/Due vào các snapshot khác đang chứa cùng Jira issue, rồi ghi trọn catalog một lần. Khi refresh thành công, cập nhật múi giờ bảng từ kết nối đã xác minh; nếu chỉ `Unverified`, giữ múi giờ đã lưu của bảng.
5. **Offline và migration**: Lưu `jiraTimeZone` trên từng SavedTable để mở/xem/tính lại được offline. Ticket cũ nhận các trường nullable mặc định. Store đọc schema v1 và chuyển đổi trong bộ nhớ: giữ tùy chọn hiện có, bật Original Expected Time ở query, bật ba cột mới cho bảng đã lưu; catalog mới có ledger rỗng. Ghi schema v2 ở lần ghi hợp lệ tiếp theo. Schema tương lai/hỏng vẫn theo cơ chế lỗi hiện tại và không ghi đè file.
6. **Cột/sắp xếp**: Tách danh sách cột query khỏi danh sách cột saved; query chỉ có Original Expected Time mới, saved có cả hai cột Dynamic. Cột mới dùng đúng các nhãn trong spec. Cột query Original Expected Time sắp xếp qua Jira JQL `originalEstimate`; cột saved sắp xếp tại chỗ theo giá trị thời lượng/timestamp thay vì chuỗi đã định dạng. Cập nhật đầy đủ value mapping, chiều rộng bảng, chooser và migration preferences.
7. **Cảnh báo**: Saved-table detail hiển thị thông báo không chặn nếu timezone thiếu/không hợp lệ; Original Expected Time và Dynamic Start Time vẫn dùng được, Dynamic Due Date để trống. Estimate thiếu/không hỗ trợ chỉ để Due Date trống; chuỗi Jira nguyên gốc vẫn hiển thị.

## Project Structure

### Tài liệu của feature

```text
specs/005-dynamic-time-columns/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── jira-and-table-ui.md
└── tasks.md                 # sinh riêng bởi /speckit-tasks
```

### Source code

```text
gradle/libs.versions.toml
composeApp/build.gradle.kts
composeApp/src/commonMain/kotlin/com/jiratools/
├── domain/
│   ├── Ticket.kt
│   ├── ColumnKey.kt
│   ├── SavedTable.kt
│   ├── UserPreferences.kt
│   ├── TicketSorting.kt
│   ├── WorkDuration.kt                 # mới: parser/kiểu thời lượng
│   └── WorkingTimeCalculator.kt        # mới: lịch, timezone, due calculation
├── data/
│   ├── JiraDtos.kt
│   ├── TicketMapper.kt
│   ├── ConnectionRepository.kt
│   ├── AppConfigStore.kt
│   └── SavedTableStore.kt
├── ui/query/
│   ├── QueryViewModel.kt
│   ├── TicketTable.kt
│   └── TableColumnLayout.kt
├── ui/saved/
│   ├── SavedTablesViewModel.kt
│   └── SavedTablesArea.kt
└── app/
    ├── App.kt
    └── AppContainer.kt
composeApp/src/commonTest/kotlin/com/jiratools/
├── domain/                         # parser, lịch, sorting
├── data/                           # mapping, REST mock, migration
└── ui/saved/                       # start-time lifecycle, refresh atomicity
```

**Structure Decision**: Giữ một module KMP hiện có. Domain mới thuộc `domain`; DTO, profile timezone và persistence migration thuộc `data`; StateFlow/use cases ở ViewModel; Compose chỉ render.

## Ánh xạ yêu cầu tới thiết kế

| User story | Thành phần chính | Điều kiện kiểm chứng độc lập |
|---|---|---|
| US1 — Original Expected Time ở mọi bảng | `JiraDtos.kt`, `TicketMapper.kt`, `Ticket.kt`, `ColumnKey.kt`, `TicketTable.kt`, `QueryViewModel.kt`, `SavedTableStore.kt` | Mock response có/không có `timetracking.originalEstimate`; query và saved đều hiển thị đúng/để trống; cột mặc định bật và schema v1 được giữ dữ liệu. |
| US2 — Ghi Dynamic Start Time một lần | `StatusDto`, `Ticket.kt`, `SavedTableCatalog`, `SavedTableStore.kt`, `SavedTablesViewModel.kt` | Category `indeterminate` bắt đầu bằng Updated hợp lệ; custom name được nhận diện; các refresh/bảng khác/vắng mặt không ghi đè hoặc mất mốc; thất bại không ghi. |
| US3 — Dynamic Due Date theo lịch | `MyselfDto`, `ConnectionStatus`, `ConnectionRepository.kt`, `SavedTable`, `WorkDuration.kt`, `WorkingTimeCalculator.kt`, saved UI | Test đơn vị, giờ nghỉ, cuối tuần, timezone, thiếu/không hợp lệ; Original Estimate đổi làm Due Date tính lại từ start cố định. |

## Complexity Tracking

Không có vi phạm hiến pháp. Thư viện `kotlinx-datetime` là phụ thuộc bổ sung duy nhất vì phép tính IANA timezone phải chạy trong `commonMain`; dùng API thời gian JVM sẽ vi phạm Nguyên tắc I.
