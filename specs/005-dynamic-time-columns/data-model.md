# Mô hình dữ liệu: Thời gian động và ước lượng công việc

## 1. Ticket

`Ticket` là snapshot chung cho bảng query và saved table; các trường mới có giá trị mặc định nullable để JSON cũ tiếp tục giải mã được.

| Trường | Kiểu | Ràng buộc / ý nghĩa |
|---|---|---|
| `key` | `String` | Khóa hiển thị Jira; bắt buộc theo model hiện tại. |
| `issueId` | `String?` | Jira issue ID ổn định; bắt buộc để tạo record start mới. |
| `summary`, `status`, `assignee`, `priority` | hiện có | Giữ nguyên hợp đồng hiện tại. |
| `statusCategoryKey` | `String?` | Jira category key; chỉ `indeterminate` được tính là In Progress. Không dùng tên trạng thái. |
| `updated` | `String` | Giá trị hiển thị hiện tại; giữ định dạng của query table. |
| `updatedAtMillis` | `Long?` | Instant của Updated; null nếu thiếu/không parse được. Offset Jira dạng `+HHMM` được chuẩn hóa trước khi parse. |
| `originalExpectedTime` | `String?` | Chuỗi `timetracking.originalEstimate` nguyên gốc; null khi Jira không trả estimate. |
| `dynamicStartTime` | `String?` | Giá trị hiển thị đã ghi nhận; bằng `updated` ở snapshot đầu tiên hợp lệ. |
| `dynamicStartAtMillis` | `Long?` | Instant của start để tính toán và sắp xếp. |
| `dynamicDueDate` | `String?` | Due Date định dạng theo timezone Jira của SavedTable. |
| `dynamicDueAtMillis` | `Long?` | Instant của due để sắp xếp theo thời gian thực, không theo chuỗi hiển thị. |

`Ticket` chỉ lưu metadata không bí mật. Token/API credential không được đưa vào model, catalog hoặc log.

## 2. DynamicStartTimeRecord

Sổ toàn cục nằm trong `SavedTableCatalog`; khóa logic là cặp (`normalizedJiraBaseUrl`, `issueId`).

| Trường | Kiểu | Ràng buộc / ý nghĩa |
|---|---|---|
| `normalizedJiraBaseUrl` | `String` | Chuẩn hóa bằng quy tắc `normalizeBaseUrl`; phân biệt Jira site. |
| `issueId` | `String` | ID ổn định Jira; không dùng issue key vì key có thể đổi. |
| `startedAtMillis` | `Long` | Instant của Updated trong snapshot đầu tiên có category `indeterminate`. |
| `displayValue` | `String` | Giá trị `Ticket.updated` tại snapshot đầu tiên để Dynamic Start Time hiển thị đúng bằng Updated. |

Một record đã tồn tại là bất biến. Chỉ tạo record mới khi status category là `indeterminate`, ID hợp lệ và Updated parse thành instant. Trường hợp thiếu dữ liệu không tạo record; snapshot sau vẫn có thể thử lại. Record không tự bị xóa khi issue vắng mặt, khi bảng bị xóa hoặc khi không còn bảng nào tham chiếu.

## 3. SavedTable và SavedTableCatalog

### SavedTable

Giữ các trường hiện có (`id`, `name`, `jql`, `jiraBaseUrl`, mốc lưu/làm mới, cột và tickets) và bổ sung:

- `jiraTimeZone: String?`: timezone IANA của tài khoản Jira được xác minh khi lưu/làm mới gần nhất. Được giữ cùng bảng để hiển thị/tính offline. Nếu thiếu hoặc không hợp lệ, Due Date trống và detail view báo lý do.
- `visibleColumns`: danh sách cột riêng của bảng. Saved table có thể chứa cột query cùng `DYNAMIC_START_TIME` và `DYNAMIC_DUE_DATE`.

### SavedTableCatalog

- `schemaVersion = 2`.
- `tables: List<SavedTable>`.
- `dynamicStartTimes: List<DynamicStartTimeRecord>`; duy trì cả khi `tables` rỗng.
- Một lần save/refresh ghi nguyên catalog; lỗi/hủy không thay đổi bản lưu.

Xóa SavedTable chỉ xóa bảng đó; không xóa tracker record. Dữ liệu start chỉ được xóa cùng file catalog do người dùng chủ động xử lý ngoài ứng dụng.

## 4. UserPreferences

- `schemaVersion = 2`.
- `visibleColumns` là tập cột query; mặc định gồm Original Expected Time, không gồm Dynamic columns.
- Query và saved table có hai tập column khả dụng riêng. Cột KEY vẫn luôn hiển thị theo UX hiện có.

## 5. WorkDuration và lịch làm việc

`WorkDuration` là giá trị thời lượng không âm. Parser nhận toàn bộ chuỗi gồm một hoặc nhiều thành phần số nguyên/thập phân dấu chấm và đơn vị `s`, `m`, `h`, `d`, `w`, không phân biệt hoa thường. Chuỗi thiếu, âm, sai cú pháp, có đơn vị lạ hoặc tràn miền biểu diễn không dùng để tính Due Date; Original Expected Time nguyên gốc vẫn hiển thị.

Hợp đồng shared-domain:

- `WorkDuration.parse(raw: String): WorkDuration?` trả null khi chuỗi không hợp lệ.
- `WorkingTimeCalculator.addWorkingTime(startAtMillis: Long, duration: WorkDuration, timeZoneId: String): Long?` trả instant due, hoặc null nếu timezone không hợp lệ hay kết quả vượt miền biểu diễn.
- `WorkingTimeCalculator.formatInTimeZone(millis: Long, timeZoneId: String): String?` định dạng thành `yyyy-MM-dd HH:mm`; trả null nếu timezone không hợp lệ.

| Đơn vị | Giá trị cộng |
|---|---:|
| `s` | 1 giây |
| `m` | 60 giây |
| `h` | 60 phút |
| `d` | 8 giờ làm việc |
| `w` | 40 giờ làm việc |

`WorkingTimeCalculator` dùng `kotlinx-datetime` và timezone của SavedTable:

- Ngày làm việc: thứ Hai-thứ Sáu.
- Phiên làm việc: `[08:00, 12:00)` và `[13:00, 17:00)`.
- Mốc start dương ngoài lịch (nghỉ trưa, ngoài giờ, cuối tuần) được đưa tới phiên làm việc kế tiếp.
- Các phiên dùng giờ địa phương của timezone SavedTable và tuân theo thay đổi DST của timezone đó.
- Estimate bằng 0 cho Due Date đúng tại instant start.
- Không có lịch nghỉ lễ hoặc cấu hình tuần tùy chỉnh.

## 6. State transitions và refresh

### Khởi tạo Dynamic Start Time

```text
snapshot saved/refresh thành công
  ├─ đã có record (site, issueId) -> giữ record, không ghi đè
  ├─ category != indeterminate -> chưa tạo record
  ├─ thiếu issueId hoặc Updated không parse được -> chưa tạo record; thử lại snapshot sau
  └─ category == indeterminate + issueId + Updated hợp lệ
       -> thêm record, giữ displayValue bằng Updated của snapshot đó
```

### Tính Dynamic Due Date

```text
missing start / missing estimate / parse error / missing or invalid timezone
  -> dynamicDueDate = null, dynamicDueAtMillis = null
otherwise
  -> cộng WorkDuration vào start theo lịch và timezone của bảng
  -> lưu instant để sort và chuỗi định dạng để hiển thị
```

### Atomic refresh

1. Nạp toàn bộ trang Jira vào bộ nhớ; không đụng catalog.
2. Nếu một trang lỗi hoặc refresh bị hủy, giữ nguyên bảng, tracker và dữ liệu disk.
3. Sau khi đủ trang, cập nhật tracker, bảng được refresh và các snapshot khác đang chứa cùng issue; tính lại các field Dynamic dùng estimate và timezone riêng của mỗi bảng.
4. Ghi một SavedTableCatalog v2. Chỉ sau khi ghi thành công cập nhật StateFlow.

## 7. Migration

### `config.json` schema v1 → v2

- Giữ `baseUrl`, `email`, `lastJql`, `sort` và các cột người dùng đã chọn.
- Thêm Original Expected Time vào tập query columns vì các lựa chọn cũ chưa thể có cột này.
- Không đưa hai Dynamic columns vào query preferences.

### `saved-tables.json` schema v1 → v2

- Giữ metadata, ticket snapshot và cột người dùng đã chọn.
- Thêm Original Expected Time, Dynamic Start Time và Dynamic Due Date vào cột của từng bảng.
- Các field mới của Ticket mặc định null; `jiraTimeZone` null; ledger bắt đầu rỗng.
- Migration thực hiện khi load; lưu schema v2 ở lần ghi hợp lệ tiếp theo. JSON hỏng hoặc schema tương lai không bị ghi đè.
- Dữ liệu thiếu chỉ được bổ sung từ Jira khi refresh thành công; không thể khôi phục start time trước khi tính năng tồn tại.
