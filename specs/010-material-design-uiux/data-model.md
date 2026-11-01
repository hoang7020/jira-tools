# Data Model: Nâng cấp UI/UX theo Material Design

## ThemeMode

Đại diện cho lựa chọn giao diện màu của người dùng.

| Field | Type | Constraints |
|---|---|---|
| value | `SYSTEM` \| `LIGHT` \| `DARK` | Giá trị hữu hạn; mặc định `SYSTEM`. |

### Hành vi

- `SYSTEM`: theme đang hiệu lực theo chế độ hệ điều hành; thay đổi hệ điều hành được phản ánh khi Compose cập nhật system theme.
- `LIGHT`: luôn dùng bảng màu sáng, bất kể chế độ hệ điều hành.
- `DARK`: luôn dùng bảng màu tối, bất kể chế độ hệ điều hành.
- Lựa chọn người dùng được lưu; theme đang hiệu lực là giá trị suy ra, không lưu trùng.

## UserPreferences

Tuỳ chọn ứng dụng được lưu cục bộ trong cấu hình hiện có.

| Field | Type | Constraints |
|---|---|---|
| schemaVersion | Integer | Hiện hành chuyển từ 2 lên 3. |
| themeMode | `ThemeMode` | Bổ sung; mặc định `SYSTEM`. |
| baseUrl | String? | Giữ nguyên giá trị hiện có. |
| email | String? | Giữ nguyên giá trị hiện có. |
| visibleColumns | Set of `ColumnKey` | Giữ nguyên giá trị hiện có và quy tắc cột bắt buộc. |
| lastJql | String? | Giữ nguyên giá trị hiện có. |
| sort | `SortState?` | Giữ nguyên giá trị hiện có. |

### Migration và validation

- Tệp cấu hình thiếu `themeMode` được đọc thành `SYSTEM`.
- Schema v1 vẫn được chuyển đổi theo quy tắc hiện tại, gồm thêm `ORIGINAL_EXPECTED_TIME` vào các cột; sau đó đánh dấu schema v3.
- Schema v2 được giữ nguyên các tuỳ chọn hiện có, gán theme mode `SYSTEM`, và nâng lên v3.
- Schema hiện hành v3 được đọc/ghi như thường lệ; schema không hỗ trợ tiếp tục theo hành vi hiện tại của `AppConfigStore`.
- Migration khi load không ghi đè tệp nguồn cho tới lần lưu tuỳ chọn kế tiếp, theo quy ước cấu hình đang dùng.

## AppNavigation

Trạng thái điều hướng trình bày hiện có; không phải dữ liệu nghiệp vụ lưu trữ.

| Field | Type | Constraints |
|---|---|---|
| selectedDestination | `QUERY` \| `SAVED_TABLES` \| `SETTINGS` | Luôn chỉ một destination cấp cao đang được chọn. |
| isCollapsed | Boolean | Chỉ thay đổi cách trình bày; không thay đổi destination hoặc trạng thái màn hình. |
| savedTableSubscreen | `TABLE_DETAIL` \| `CALENDAR` | Calendar là luồng con; Back trở về chi tiết bảng. |

## ThemeComponentState

Các trạng thái thị giác phải được thể hiện nhất quán qua semantic role cùng văn bản/biểu tượng khi cần.

| State | Ý nghĩa |
|---|---|
| Default | Thành phần sẵn sàng tương tác. |
| Focused | Thành phần đang nhận focus bàn phím và có chỉ báo nhìn thấy được. |
| Selected | Destination hoặc mục dữ liệu hiện hành. |
| Disabled | Thao tác hiện không khả dụng. |
| Loading | Hành động hoặc dữ liệu đang được xử lý; không hàm ý giao diện bị treo. |
| Error / Warning | Thất bại hoặc cần chú ý; đi kèm thông điệp có ngữ nghĩa. |
| Empty / Success | Không có dữ liệu hoặc thao tác hoàn tất; thông điệp phân biệt rõ với lỗi. |
