# Nghiên cứu: Cuộn ngang và mở rộng Calendar

## Quyết định 1: Một nguồn state cho trục ngang

- **Decision**: Dùng cùng một `LazyListState` để xác định ngày đầu đang thấy và offset pixel cho header ngày, lưới và tất cả timeline row.
- **Rationale**: Hiện `SavedTableCalendarArea` có `LazyRow` chỉ ở header, còn timeline mỗi hàng được vẽ trên `Canvas` tĩnh bằng cách đọc state của header. Điều này khiến vùng nội dung không phải vùng cuộn; gesture đặt trên các hàng không được tiếp nhận bởi `LazyRow`. Dùng chung state giữ header và các hàng đồng bộ cả khi cuộn một phần ngày.
- **Alternatives considered**: Tạo một scroll state độc lập cho mỗi hàng; loại vì các state dễ lệch khi gesture, fling hoặc resize. Dùng `horizontalScroll` độc lập cho toàn bộ nội dung; loại vì cần đồng bộ thêm một trục ảo với header lazy hiện tại.
- **Evidence**: `composeApp/src/commonMain/kotlin/com/jiratools/ui/saved/SavedTableCalendarArea.kt` chứa `LazyRow` cho header và `Canvas` timeline dùng `firstVisibleDay`/`firstVisibleOffsetPx`; vùng hàng không có horizontal scroll modifier.

## Quyết định 2: Nhận gesture ngang trên toàn vùng ngày và giữ cuộn dọc

- **Decision**: Định tuyến gesture ngang từ viewport ngày tới cùng `LazyListState`, kể cả khi con trỏ bắt đầu trên timeline hoặc vùng trống; chỉ xử lý thành phần ngang của gesture để `LazyColumn` giữ tương tác dọc.
- **Rationale**: Khắc phục tương tác ở đúng bề mặt người dùng đang kéo/cuộn, đồng thời đáp ứng hỗ trợ horizontal wheel/trackpad mà không chiếm cuộn dọc ticket.
- **Alternatives considered**: Chỉ thêm scrollbar hoặc nút trái/phải; loại vì người dùng yêu cầu cuộn ngang tự nhiên và tiêu chí tương thích gesture desktop. Bắt toàn bộ wheel bằng một handler không phân biệt trục; loại vì sẽ phá cuộn dọc.
- **Evidence**: Dự án đã có Compose Foundation và Lazy components; không có Compose UI test dependency/test harness được khai báo. Kiểm chứng thao tác cuối cần hướng dẫn manual desktop.

## Quyết định 3: Mở rộng theo khối một tháng và giữ ngày neo

- **Decision**: Khi viewport tiến gần một biên, mở rộng khoảng ngày thêm một tháng ở biên đó. Nếu thêm vào đầu, điều chỉnh index hiện tại theo số ngày được thêm để cùng ngày và pixel offset vẫn nằm tại chỗ; nếu thêm ở cuối, giữ vị trí hiện tại.
- **Rationale**: Mở rộng theo tháng phù hợp với lịch và có giới hạn rõ cho mỗi lần cập nhật; giữ anchor ngăn viewport nhảy khi các index cũ đổi. Mở rộng chỉ xảy ra khi người dùng cuộn tới gần biên, nên ngày ban đầu không tạo khoảng trống thừa.
- **Alternatives considered**: Thêm từng ngày; bị loại vì gây cập nhật liên tục và tăng recomposition quanh biên. Thêm một khoảng lớn cố định một lần; bị loại vì cuối cùng lại gặp biên, trái với mong muốn tiếp tục cuộn. Tạo ngày vô hạn eager; bị loại vì không thể biểu diễn/hiển thị danh sách vô hạn hữu hạn và có rủi ro hiệu năng.
- **Boundary behavior**: Ngừng mở rộng riêng phía đã chạm giới hạn ngày hoặc chỉ số có thể biểu diễn; không tràn số và không reset vị trí.

## Quyết định 4: Tính lại projection từ snapshot với khoảng ngày tường minh

- **Decision**: Calendar nhận snapshot `tickets`, timezone và thời điểm tham chiếu hiện có; domain projection hỗ trợ khoảng ngày do UI cung cấp. Khi biên đổi, tính lại rows/segments theo khoảng mới.
- **Rationale**: Projection hiện tại cắt segment trong cửa sổ một tháng trước và ba tháng sau ngày tham chiếu. Không thể tạo segment ngày mới chỉ từ projection cũ vì dữ liệu ngoài cửa sổ đã bị loại. Tính lại từ snapshot duy trì logic domain và không cần Jira/network.
- **Alternatives considered**: Dịch segment cũ theo offset hoặc tổng hợp từ `LocalDate` hiển thị; loại vì mốc instant và đoạn làm việc cắt mất thông tin ngoài cửa sổ.
- **Evidence**: `SavedTablesArea` có `sorted` tickets, timezone, reference millis và hiện gọi `projectSavedTableCalendar`; phép chiếu nhận các mốc ticket và hai biên ngày.

## Quyết định 5: Giữ ngày khởi tạo và state mở rộng trong vòng đời Calendar

- **Decision**: Khởi tạo range ban đầu như hiện nay; range chỉ mở rộng trong bộ nhớ của màn Calendar và reset khi mở lại bảng/calendar mới. Chỉ gọi cuộn về ngày hiện tại khi khởi tạo hoặc đổi snapshot, không gọi lại khi range mở rộng.
- **Rationale**: Giữ hành vi ban đầu đã được yêu cầu và ngăn side effect hiện có (scrollToItem(todayIndex)) đưa người dùng về giữa trục mỗi lần range đổi.
- **Alternatives considered**: Persist range hoặc vị trí cuộn qua lần mở; bị loại vì đặc tả không yêu cầu lưu trạng thái và sẽ thêm dữ liệu/preferences ngoài scope.

## Validation strategy

- Common tests kiểm tra range mở rộng hai phía, index neo được điều chỉnh khi prepend, giới hạn ngày không tràn, projection có segment trên ngày mới và ngày cuối vẫn theo thứ tự.
- Manual desktop test kiểm tra cuộn ngang khi bắt đầu trên header, timeline và vùng trống; cuộn một phần ngày; đổi hướng; cuộn dọc; dữ liệu rỗng; mở rộng ở cả hai biên; resize.
- Chạy `./gradlew :composeApp:jvmTest`, `./gradlew ktlintCheck` và `./gradlew build`.
