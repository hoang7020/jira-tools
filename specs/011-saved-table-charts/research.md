# Research: Đồ thị bảng đã lưu

## Quyết định 1: Dùng thư viện vẽ hiện có thay vì thêm chart dependency

**Decision**: Tạo chart projection bằng Kotlin dùng chung và render bằng Compose Material 3/Canvas; không thêm thư viện biểu đồ trong bản đầu.

**Rationale**:

- Dự án đã có Compose Foundation, UI và Material 3 trên commonMain nhưng chưa có charting dependency.
- Các yêu cầu hiện tại là bốn dạng cơ bản với dữ liệu nhỏ gắn với một bảng đã lưu; không cần dashboard, zoom chuyên sâu hay export.
- Tự quản projection/domain data giữ chart calculations dễ kiểm thử trong commonTest, trong khi Canvas chỉ nhận giá trị chiếu và render.
- Nhãn, giá trị và chú giải phải được trình bày bằng thành phần văn bản/semantics riêng, không chỉ vẽ bằng màu.

**Alternatives considered**:

- Thêm charting library đa nền tảng: có thể giảm code dựng hình nhưng thêm API/dependency/version compatibility và cần xác nhận tính năng labels/legend/accessibility trên Desktop.
- Dùng chart raster/image: không phù hợp vì nội dung cần co giãn, có thể truy cập và kiểm thử bằng dữ liệu.

**Evidence**: `composeApp/build.gradle.kts` chỉ khai báo Compose runtime/foundation/material3/ui; `gradle/libs.versions.toml` không có thư viện chart. Spec giới hạn chart ở một bảng và bốn loại.

## Quyết định 2: Lưu snapshot tổng hợp bên trong từng SavedTable

**Decision**: Thêm danh sách snapshot vào `SavedTable` và nâng saved-table catalog schema từ v3 lên v4. Mỗi snapshot chỉ lưu timestamp, tổng số ticket, số remaining và số ticket có category chưa xác định.

**Rationale**:

- Lịch sử có cùng vòng đời với bảng: rename giữ nguyên lịch sử; delete xoá cả bảng lẫn snapshot mà không cần dọn danh mục phụ.
- `SavedTableStore` ghi catalog JSON theo một thao tác atomic write. Gắn snapshot vào bảng giúp nội dung mới và snapshot được commit cùng nhau.
- Snapshot tổng hợp đáp ứng burndown mà không nhân bản toàn bộ ticket cho mỗi refresh.
- Schema v1/v2/v3 vẫn được đọc qua các migration hiện có và nhận danh sách snapshot mặc định rỗng; không thể tái tạo lịch sử trước khi tính năng được dùng.

**Alternatives considered**:

- Danh sách snapshot riêng ở cấp catalog: cần quản lý foreign key/lifecycle và có rủi ro lệch giữa bảng với lịch sử.
- Sao chép toàn bộ ticket cho mỗi snapshot: lưu nhiều dữ liệu lặp lại và làm tăng nhanh kích thước file.
- Cắt bớt lịch sử: trái với yêu cầu đã xác nhận là giữ đến khi xoá bảng.

## Quyết định 3: Chỉ ghi snapshot khi thao tác lưu dữ liệu bảng thành công

**Decision**:

- Khi tạo bảng mới, thêm baseline snapshot trong cùng write với bảng mới.
- Khi refresh đã tải xong tất cả trang và catalog được ghi thành công, thêm đúng một snapshot.
- Refresh bị huỷ, Jira thất bại trước khi hoàn tất hoặc ghi catalog thất bại thì không thay đổi bảng hiện hành/snapshot.
- Snapshot timestamp lấy từ clock của ứng dụng; trạng thái còn lại được đếm từ category Jira `done`.

**Rationale**: Điều này giữ nguyên semantics atomic hiện tại của `SavedTablesViewModel`: chỉ thay thế dữ liệu khi đủ kết quả; lỗi ghi giữ state cũ. Không tạo điểm biểu đồ cho trạng thái mà ứng dụng chưa lưu thành công.

**Alternatives considered**:

- Snapshot từng trang Jira: biểu diễn trạng thái chưa hoàn chỉnh và làm chart sai trong refresh phân trang.
- Snapshot trước khi ghi file: có thể ghi history dù thao tác lưu thất bại.

## Quyết định 4: Quy ước phép tổng hợp cho chart

**Decision**:

- Pie/column: đếm ticket theo một trường tại một thời điểm; trường mặc định là status; người dùng có thể chọn status, assignee hoặc priority. Giá trị null/rỗng thành nhóm “Chưa xác định”.
- Line: nhóm ticket trong bảng hiện hành theo ngày `updatedAtMillis`; giá trị là số ticket có Updated trong ngày đó. Ticket không có mốc thời gian bị bỏ khỏi trục thời gian và được phản ánh bằng thông tin dữ liệu thiếu.
- Burndown: vẽ `remainingCount` theo `recordedAtMillis` của các snapshot. Category `done` không còn lại; category không có/không nhận dạng được được tính vào remaining để tránh giả định ticket đã xong, đồng thời ghi riêng `unknownCategoryCount` và chú thích.
- Kết quả được sắp thứ tự ổn định theo nhãn/ngày; không gộp hoặc bỏ nhóm mà không giải thích.

**Rationale**: Quy ước này khớp các trường đang có trên `Ticket`, tránh yêu cầu gọi Jira khi xem chart, và công khai sự thiếu hụt của dữ liệu cũ/không đầy đủ.

**Alternatives considered**:

- Dựa line chart trên lịch sử refresh: đó là burndown/chuỗi snapshot, không mô tả số ticket hiện tại theo Updated như yêu cầu FR-005.
- Bỏ ticket thiếu category khỏi burndown: có thể làm giảm số còn lại một cách sai lệch.

## Quyết định 5: Navigation và visual controls

**Decision**: Chart là màn hình con trong chi tiết bảng đã lưu; dùng trạng thái selected chart/grouping field và nút quay lại. Ba action buttons dùng cùng variant Material 3, cùng chiều cao và khoảng cách 5.dp.

**Rationale**: Khớp cấu trúc calendar hiện hành, không làm mất selected table state, và đáp ứng quy tắc trực quan đã nêu.

**Alternatives considered**:

- Destination cấp cao riêng: sẽ tách chart khỏi context bảng mà nó trực quan hoá.
- Dialog: không phù hợp khi cần nhiều loại biểu đồ và chú giải trong không gian hạn chế.
