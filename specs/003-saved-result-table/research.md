# Nghiên cứu kỹ thuật (Phase 0)

**Feature**: `003-saved-result-table` | **Ngày**: 2026-10-31 | **Kế hoạch**: [plan.md](./plan.md)

Tài liệu này giải quyết các quyết định thiết kế còn mở của tính năng "bảng riêng lưu cục bộ", dựa trên
khảo sát mã nguồn hiện có của dự án (001 và 002) và các ràng buộc trong hiến pháp.

## Tóm tắt quyết định

| Chủ đề | Quyết định |
|-------|-----------|
| Nơi lưu | Một tệp JSON `saved-tables.json` trong thư mục cấu hình nền tảng, có `schemaVersion`, ghi nguyên tử |
| Đọc hỏng/khác phiên bản | Cảnh báo + danh sách trống + giữ nguyên tệp + **chặn ghi** + nút "Thử lại" (FR-020) |
| Thời gian | `AppClock` (interface, inject qua `AppContainer`): `nowMillis()` + `format(millis)`; JVM dùng `java.time` |
| Serialization | `Ticket` thêm `@Serializable`; `SavedTable`/`SavedTableCatalog` là model `@Serializable` trong `domain` |
| Bố cục UI | Bộ chuyển chế độ xem "Truy vấn" / "Bảng đã lưu" ngay dưới vùng kết nối; tái dùng `TicketTable` cho dữ liệu đã lưu |
| Sắp xếp dữ liệu đã lưu | Hàm thuần trong bộ nhớ (`sortTickets`), không gọi Jira (FR-007) |
| Làm mới | Vòng lặp `searchJql` tới `isLast`, thay thế toàn bộ, có thể huỷ, hiển thị tiến trình (FR-021..FR-025) |
| Quy tắc trùng tên | Chuẩn hoá `trim()` + so sánh không phân biệt hoa/thường (FR-011) |
| Cột hiển thị của bảng lưu | Lưu theo từng bảng (FR-017); trạng thái sắp xếp chỉ trong phiên |
| Phụ thuộc mới | Không có |

## 1. Nơi lưu và định dạng dữ liệu

**Quyết định**: lưu toàn bộ danh mục bảng đã lưu trong **một tệp JSON** `saved-tables.json` đặt cạnh
`config.json` trong thư mục cấu hình nền tảng (đi qua `AppPaths`, giữ `commonMain` không phụ thuộc hệ
tệp). Tệp có `schemaVersion`; ghi theo cách nguyên tử (tệp tạm rồi thay thế) như `AppConfigStore` đang
làm với `config.json`.

**Lý do**:

- Lựa chọn của người dùng ở phiên làm rõ (Q1 phương án A) là: dữ liệu lưu không đọc được thì **cảnh báo,
  danh sách tạm thời trống, giữ nguyên dữ liệu cũ**. Một tệp duy nhất biểu diễn đúng ngữ nghĩa
  "toàn bộ kho bảng đã lưu không đọc được" và tránh trường hợp nửa đọc được nửa không.
- Ghi nguyên tử đã có sẵn trong `JvmAppPaths` (`Files.move` với `ATOMIC_MOVE`), dùng lại được ngay.
- Quy mô theo giả định của spec (hàng chục bảng, mỗi bảng vài nghìn ticket) tương ứng vài MB JSON —
  nằm trong bộ nhớ và ghi lại toàn tệp mỗi thao tác là chấp nhận được (SC-001: lưu 100 ticket < 5 giây;
  SC-004: mở bảng 1000 ticket < 3 giây vì dữ liệu đã ở trong bộ nhớ, không đọc lại tệp).
- Danh mục được nạp **một lần** vào bộ nhớ khi ứng dụng khởi động; danh sách bảng chỉ đọc phần metadata
  (tên, số ticket, thời điểm) nên không phải phân tích lại dữ liệu ticket cho mỗi lần hiển thị.

**Phương án đã cân nhắc**:

| Phương án | Loại vì |
|----------|---------|
| Mỗi bảng một tệp + một tệp chỉ mục | Nhanh hơn khi kho rất lớn, nhưng tệp của một bảng hỏng thì hành vi tự nhiên là "bỏ qua phần hỏng, hiển thị phần còn lại" — đúng phương án B mà người dùng đã **không** chọn ở Q1; đồng thời phải quản lý hai mức lỗi khác nhau |
| Cơ sở dữ liệu nhúng (SQLite) | Thêm phụ thuộc mới và tầng ánh xạ cho quy mô không cần thiết (Nguyên tắc IV, V) |
| Lưu trong `UserPreferences` (`config.json`) | Trộn dữ liệu lớn vào tệp cấu hình nhỏ, làm hỏng nhịp đọc/ghi cấu hình hiện có |
| Lưu từng bảng ngay khi tạo, không có danh mục | Không đáp ứng FR-005 (danh sách có tên, số ticket, thời điểm) |

## 2. Hành vi khi dữ liệu lưu không đọc được

**Quyết định**: khi `saved-tables.json` không đọc được (JSON hỏng, sai kiểu, `schemaVersion` khác) hoặc
khi đọc tệp thất bại:

1. Ứng dụng vẫn khởi động và dùng bình thường.
2. Khu vực bảng đã lưu hiển thị **danh sách trống** kèm thông báo rõ ràng rằng dữ liệu bảng đã lưu
   không đọc được, **kèm đường dẫn tệp** để người dùng tự xử lý.
3. **Mọi thao tác ghi bị chặn** cho tới khi đọc lại thành công: nút "Lưu bảng", đổi tên, xoá, làm mới
   (phần ghi kết quả) đều báo lỗi thay vì ghi đè tệp cũ.
4. Người dùng có nút **"Thử lại"** để đọc lại tệp (sau khi tự sửa/di chuyển tệp) mà không cần khởi động lại.

**Lý do**: FR-020 cấm tự xoá hoặc ghi đè dữ liệu cũ. Nếu không chặn ghi, thao tác "Lưu bảng" đầu tiên
sau khi gặp tệp hỏng sẽ lặng lẽ thay thế tệp đó và dữ liệu người dùng định cứu sẽ biến mất — trái cả
FR-020 lẫn ý nghĩa lựa chọn Q1-A.

**Phương án đã cân nhắc**: sao lưu ngầm tệp hỏng rồi cho phép ghi (thao tác hệ tệp ngoài phạm vi spec,
gây bất ngờ); cho phép ghi đè kèm cảnh báo (trái FR-020); khoá hoàn toàn tính năng bảng đã lưu
(phương án C ở Q1, người dùng đã không chọn).

## 3. Nguồn thời gian và định dạng hiển thị

**Quyết định**: thêm dịch vụ nền tảng `AppClock` trong `commonMain/platform`:

```kotlin
interface AppClock {
    fun nowMillis(): Long
    fun format(millis: Long): String   // "dd/MM/yyyy HH:mm" theo múi giờ hệ thống
}
```

`jvmMain` cài đặt bằng `System.currentTimeMillis()` + `java.time` (múi giờ hệ thống); kiểm thử dùng
`FakeClock` trả giá trị cố định.

**Lý do**:

- Cần `nowMillis()` cho `savedAtMillis` (FR-005) và `refreshedAtMillis` (FR-026); cần định dạng để hiển
  thị cho người dùng.
- `commonMain` không có API định dạng ngày giờ chuẩn, mà thêm `kotlinx-datetime` là phụ thuộc mới không
  cần thiết (Nguyên tắc IV).
- Tách sau interface cho phép kiểm thử xác định (thời điểm lưu/ làm mới cố định) — hiện dự án chưa có
  nguồn thời gian nào nên cần bổ sung seam.

**Phương án đã cân nhắc**: `kotlin.time.Clock` (có trong stdlib Kotlin 2.4 nhưng vẫn cần opt-in ở một số
phiên bản và không giả được trong test); `kotlinx-datetime` (phụ thuộc mới); lưu sẵn chuỗi định dạng
(không sắp xếp/lọc được theo thời gian, dữ liệu lưu phụ thuộc định dạng hiển thị).

## 4. Model dữ liệu và serialization

**Quyết định**: `Ticket` (domain) được thêm `@Serializable`; thêm `SavedTable` và `SavedTableCatalog`
`@Serializable` trong `domain`. Dùng `kotlinx.serialization` với `ignoreUnknownKeys = true`,
`encodeDefaults = true`, `prettyPrint = true` — cùng cấu hình như `AppConfigStore`.

**Lý do**: `UserPreferences` đã có tiền lệ là model `@Serializable` trong `domain`; `Ticket` chỉ gồm các
trường nguyên thuỷ nên thêm annotation không đổi hành vi; `ColumnKey` là enum nên đã serialize được theo
tên (đang dùng trong `UserPreferences.visibleColumns`).

**Phương án đã cân nhắc**: DTO riêng trong `data` + ánh xạ hai chiều (trùng lặp trường và thêm code
không sinh giá trị); lưu dạng chuỗi tự chế (không an toàn kiểu, trái Nguyên tắc IV).

## 5. Bố cục giao diện

**Quyết định**: thêm bộ chuyển chế độ xem ngay dưới vùng kết nối, hai chế độ:

- **Truy vấn**: giữ nguyên bố cục 002 (ô JQL + nút chạy + bảng kết quả), thêm một nút **"Lưu bảng"**
  trong hàng tiêu đề "Kết quả truy vấn".
- **Bảng đã lưu**: bố cục hai cột — cột trái là danh sách bảng đã lưu (tên, số ticket, thời điểm lưu,
  JQL rút gọn) kèm các thao tác Mở/Đổi tên/Xoá; cột phải là chi tiết bảng đang chọn (metadata, nút
  "Làm mới"/"Dừng", và lưới dữ liệu).

Bảng dữ liệu của bảng đã lưu **tái dùng `TicketTable`** bằng cách ánh xạ dữ liệu đã lưu sang
`QueryState.Success(tickets = …, hasMore = false, approximateTotal = null)` và truyền
`columns = table.visibleColumns`. Nhờ vậy cuộn dọc/ngang, cột Mã cố định, sắp xếp theo cột, ẩn/hiện cột
và mở ticket của 002 dùng lại được toàn bộ mà không sửa `TicketTable`.

**Lý do**:

- Không sửa thành phần dùng chung `TicketTable` -> rủi ro hồi quy cho bảng truy vấn gần như bằng không
  (FR-019).
- Vùng kết nối vẫn luôn hiển thị ở cả hai chế độ, giữ tinh thần "vùng điều khiển cố định" của 002;
  yêu cầu FR-004 của 002 nói về việc vùng điều khiển không trôi khi **cuộn danh sách ticket**, không
  ràng buộc việc người dùng chủ động chuyển sang màn hình khác. Điều này được ghi nhận như một diễn giải
  thiết kế (xem `contracts/saved-tables.md` mục 4).
- Cột trái + chi tiết bên phải là bố cục desktop quen thuộc, đủ chỗ cho bảng nhiều cột ở chế độ xem
  rộng (SC-004).

**Phương án đã cân nhắc**: cửa sổ/dialog riêng cho bảng đã lưu (khó xem bảng 1000 dòng, thêm quản lý
vòng đời cửa sổ); tab con nằm trong khu vực kết quả (chật, trộn hai khái niệm); thay thế hẳn màn hình
truy vấn (mất ngữ cảnh, trái FR-019).

## 6. Sắp xếp dữ liệu đã lưu

**Quyết định**: hàm thuần `sortTickets(tickets: List<Ticket>, sort: SortState): List<Ticket>` sắp xếp
trong bộ nhớ theo cột đang chọn: so sánh chuỗi **không phân biệt hoa/thường**, giá trị rỗng/null xếp
trước khi tăng dần, và sắp xếp **ổn định** (giữ nguyên thứ tự đã lưu khi bằng nhau). Trạng thái sắp xếp
của bảng đã lưu chỉ tồn tại trong phiên làm việc (không lưu xuống tệp).

**Lý do**: FR-007 yêu cầu sắp xếp thực hiện trên chính dữ liệu đã lưu và **không** phát sinh truy vấn tới
Jira, nên không thể dùng lại cơ chế viết lại `ORDER BY` của bảng truy vấn.

**Phương án đã cân nhắc**: chạy lại JQL với `ORDER BY` mới (cần mạng, trái FR-007, làm thay đổi dữ liệu
đã lưu ngoài hai thao tác được phép); lưu sẵn nhiều thứ tự (tốn dung lượng, không giải quyết được mọi
tổ hợp cột).

## 7. Làm mới bảng đã lưu

**Quyết định**: khi người dùng bấm "Làm mới" cho bảng đang chọn:

1. Chỉ thực hiện khi `ConnectionStatus.Connected` (FR-023); nếu chưa kết nối, nút không khả dụng kèm
   giải thích.
2. Gọi lặp `JiraApiClient.searchJql(jql = table.jql, nextPageToken = …)` cho tới khi `isLast = true`,
   gom toàn bộ ticket (FR-022). Mỗi vòng lặp cập nhật số ticket đã nạp để hiển thị tiến trình (FR-025).
3. Nếu thành công: thay thế toàn bộ danh sách ticket của bảng, cập nhật `refreshedAtMillis` và địa chỉ
   Jira đã dùng (FR-026), giữ nguyên tên và `visibleColumns` (FR-021), rồi ghi danh mục xuống tệp.
4. Nếu thất bại ở bất kỳ trang nào: **không** ghi gì, giữ nguyên dữ liệu cũ và hiển thị thông báo lỗi
   theo `JiraError` hiện có (FR-024). Người dùng có thể bấm "Dừng" để huỷ (FR-025).

**Lý do**: `searchJql` đã hỗ trợ phân trang bằng `nextPageToken`; `JiraErrorMapper` đã ánh xạ 8 nhóm lỗi
nên thông báo lỗi nhất quán với luồng truy vấn; giữ dữ liệu cũ tới khi có kết quả đầy đủ tránh trạng thái
nửa vời (edge case của spec).

**Phương án đã cân nhắc**: nạp từng trang theo yêu cầu người dùng (trái FR-022); nạp song song nhiều
trang (cursor của Jira là tuần tự, không song song được); dùng lại `QueryViewModel` cho việc làm mới
(khác vòng đời và khác ngữ nghĩa "lưu vào đâu").

## 8. Tên bảng: chuẩn hoá và kiểm tra trùng

**Quyết định**: hàm thuần `validateTableName(name, existingNames, currentId)`:

- Cắt khoảng trắng hai đầu; tên sau khi cắt không được rỗng (FR-011).
- So sánh trùng theo khoá `trim().lowercase()` (không phân biệt hoa/thường, không tính khoảng trắng thừa)
  với các bảng khác, trừ chính bảng đang đổi tên.
- Trả về lỗi cụ thể (`Empty`, `Duplicate`) để giao diện hiển thị đúng lý do.

**Lý do**: đây là điểm còn để mở ở bước làm rõ (tác động thấp, được chốt ở giai đoạn thiết kế); chọn quy
tắc chặt vừa phải để tránh hai bảng chỉ khác nhau về hoa/thường gây nhầm lẫn.

**Phương án đã cân nhắc**: so sánh chính xác từng ký tự (cho phép "ABC" và "abc" cùng tồn tại — dễ gây
nhầm); tự động thêm hậu tố khi trùng (im lặng đổi tên người dùng đặt).

## 9. Ẩn/hiện cột và các thao tác ghi

**Quyết định**: `visibleColumns` của từng bảng được lưu cùng bảng và ghi lại mỗi lần người dùng bật/tắt
cột (FR-017). Mọi thao tác ghi (lưu mới, đổi tên, xoá, làm mới, đổi cột) đi qua cùng một hàm ghi của
`SavedTableStore`; lỗi ghi được trả về thông báo cho giao diện (FR-015, FR-011) và không làm thay đổi
trạng thái trong bộ nhớ ngoài thao tác đang thực hiện.

**Lý do**: một điểm ghi duy nhất giúp áp chính sách chặn ghi khi dữ liệu không đọc được (mục 2) ở đúng
một chỗ.

## 10. Kiểm thử

- **Test trước** cho logic chia sẻ mới (Nguyên tắc II): `validateTableName`, `sortTickets`,
  `SavedTableStore` (round-trip, hỏng/khác phiên bản, chặn ghi), `SavedTablesViewModel` (lưu, đổi tên,
  xoá, làm mới thành công/thất bại, tiến trình, không rò rỉ token).
- HTTP cho làm mới dùng `MockEngine` + fixture JSON hiện có; **không** test nào gọi Jira thật.
- `AppClock` và `AppPaths` được giả lập (`FakeClock`, `FakeAppPaths`) nên thời điểm và đường dẫn xác định.
- Không viết test Compose UI: bố cục và tương tác được kiểm chứng thủ công bằng KB-1..KB-6 trong
  [quickstart.md](./quickstart.md).

## 11. Điểm chưa kiểm chứng

- Chi phí đọc/ghi tệp danh mục ở quy mô rất lớn (hàng chục bảng × vài nghìn ticket) chỉ được kiểm chứng
  gián tiếp bằng giả định quy mô và kịch bản KB-5; nếu thực tế vượt xa, có thể tách thành nhiều tệp sau
  mà không đổi hợp đồng dữ liệu (nhờ `schemaVersion`).
- `format(millis)` phụ thuộc `java.time` và múi giờ hệ thống; định dạng được chốt trong hợp đồng nhưng
  hiển thị thực tế cần xác nhận bằng mắt ở KB-2.

## Nguồn tham khảo

- Mã nguồn hiện có: `composeApp/src/commonMain/kotlin/com/jiratools/data/AppConfigStore.kt`,
  `data/JiraApiClient.kt`, `domain/Ticket.kt`, `domain/UserPreferences.kt`, `domain/BrowseUrl.kt`,
  `platform/AppPaths.kt`, `ui/query/QueryViewModel.kt`, `ui/query/TicketTable.kt`,
  `ui/query/TicketOpener.kt`, `app/AppContainer.kt`, `app/App.kt`;
  `composeApp/src/jvmMain/kotlin/com/jiratools/platform/JvmAppPaths.kt`;
  `composeApp/src/commonTest/kotlin/com/jiratools/platform/Fakes.kt`,
  `commonTest/kotlin/com/jiratools/data/SecretLeakTest.kt`.
- Tài liệu tính năng trước: `specs/001-jql-ticket-table/` (hợp đồng Jira REST, dịch vụ nền tảng) và
  `specs/002-full-window-scrolling/` (bố cục, cuộn, thanh cuộn).
