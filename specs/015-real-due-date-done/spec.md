# Feature Specification: Bổ sung Done cho Real Due Date

**Feature Branch**: `[015-real-due-date-done]`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Cập nhật tính năng cho Real Due Date, bổ sung điều kiện trạng thái chuyển sang Done."

## User Scenarios & Testing *(mandatory)*

## Clarifications

### Session 2026-11-01

- Q: Trạng thái Jira nào mới được xem là `Done`? → A: Khớp tên sau khi bỏ khoảng trắng hai đầu và không phân biệt hoa thường, tương tự `Close` và `Resolve`.

### User Story 1 - Ghi nhận Real Due Date khi task chuyển sang Done (Priority: P1)

Khi người dùng chuyển một task sang trạng thái `Done`, hệ thống cần ghi nhận thời điểm chuyển trạng thái đó làm `Real Due Date` của task, tương tự như khi task chuyển sang `Close` hoặc `Resolve`.

**Why this priority**: Đây là điều kiện hoàn tất công việc còn thiếu trong quy tắc xác định Real Due Date, giúp dữ liệu thống nhất với các quy trình Jira dùng trạng thái `Done`.

**Independent Test**: Chuyển một task sang trạng thái `Done`, sau đó xác nhận `Real Due Date` bằng thời điểm `Updated` của lần chuyển trạng thái cuối cùng. Các trạng thái khác không làm thay đổi Real Due Date.

**Acceptance Scenarios**:

1. **Given** một task chưa có trạng thái kết thúc hợp lệ, **When** task chuyển sang `Done`, **Then** hệ thống lưu thời điểm chuyển sang `Done` làm Real Due Date.
2. **Given** một task đã từng chuyển sang `Done`, **When** task rời trạng thái đó rồi chuyển lại `Done`, **Then** Real Due Date được cập nhật thành thời điểm lần chuyển cuối cùng.
3. **Given** một task chuyển sang `Done`, **When** dữ liệu được làm mới trong bảng đã lưu hoặc bảng kết quả truy vấn, **Then** cột Real Due Date hiển thị đúng thời điểm đã ghi nhận.

---

### User Story 2 - Giữ đầy đủ quy tắc cho các trạng thái kết thúc hiện có (Priority: P2)

Khi bổ sung `Done`, các quy tắc hiện có cho trạng thái `Close` và `Resolve` cần tiếp tục hoạt động như trước và được xem xét cùng với `Done`.

**Why this priority**: Việc cập nhật không được làm mất hoặc thay đổi hành vi Real Due Date đối với quy trình Jira hiện tại.

**Independent Test**: Kiểm tra một task chuyển qua tổ hợp trạng thái `Close`, `Resolve` và `Done`, xác nhận Real Due Date là thời điểm của lần chuyển sang trạng thái kết thúc hợp lệ cuối cùng.

**Acceptance Scenarios**:

1. **Given** một task chuyển từ trạng thái thường sang `Close`, **When** sau đó chuyển sang `Resolve` rồi sang `Done`, **Then** Real Due Date là thời điểm của lần chuyển sang `Done`.
2. **Given** một task chuyển sang `Resolve`, **When** người dùng chuyển sang trạng thái thường khác, **Then** dữ liệu Real Due Date hiện có không bị xóa.
3. **Given** một task chuyển sang trạng thái thường trước lần chuyển kết thúc hợp lệ cuối cùng, **When** dữ liệu được xử lý lại, **Then** chỉ các lần chuyển sang `Close`, `Resolve` hoặc `Done` mới được xem xét để xác định Real Due Date.

---

### Edge Cases

- Task chuyển sang `Done` nhiều lần qua các trạng thái khác nhau: Real Due Date là thời điểm lần chuyển cuối cùng vào một trong ba trạng thái kết thúc hợp lệ.
- Tên trạng thái Jira được so khớp sau khi bỏ khoảng trắng hai đầu và không phân biệt hoa thường; chỉ trạng thái có tên bằng `Done` mới hợp lệ, không suy rộng sang trạng thái chứa từ `Done`.
- Task chưa từng chuyển sang `Close`, `Resolve` hoặc `Done`: Real Due Date không có giá trị.
- Jira trả thiếu hoặc không đọc được thời điểm `Updated`: hệ thống không ghi một giá trị Real Due Date không xác định.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống MUST xem trạng thái `Done` là trạng thái kết thúc hợp lệ để xác định Real Due Date.
- **FR-002**: Khi task chuyển sang trạng thái `Done`, hệ thống MUST lấy giá trị `Updated` tại lần chuyển trạng thái đó làm Real Due Date.
- **FR-003**: Khi xác định Real Due Date, hệ thống MUST xem xét cùng các trạng thái kết thúc hợp lệ `Close`, `Resolve` và `Done`.
- **FR-004**: Hệ thống MUST chọn thời điểm của lần chuyển cuối cùng sang một trong ba trạng thái kết thúc hợp lệ.
- **FR-005**: Lần chuyển sang trạng thái khác không hợp lệ MUST không làm xóa hoặc ghi đè Real Due Date đã xác định trước đó.
- **FR-006**: Bảng đã lưu và bảng kết quả truy vấn MUST hiển thị Real Due Date nhất quán sau khi dữ liệu được làm mới.
- **FR-007**: Task chưa từng chuyển sang `Close`, `Resolve` hoặc `Done` MUST có Real Due Date trống.
- **FR-008**: Việc nhận diện trạng thái `Done` MUST bỏ khoảng trắng hai đầu và không phân biệt hoa thường, nhưng MUST không khớp với tên trạng thái chứa chuỗi `Done`.

### Key Entities *(include if feature involves data)*

- **Task**: Công việc Jira có trạng thái hiện tại và lịch sử thay đổi trạng thái.
- **Real Due Date**: Thời điểm `Updated` của lần chuyển trạng thái cuối cùng vào `Close`, `Resolve` hoặc `Done`.
- **Trạng thái kết thúc hợp lệ**: Nhóm trạng thái Jira được dùng để xác định Real Due Date, gồm `Close`, `Resolve`, `Done`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% task chuyển sang `Done` trong kịch bản kiểm thử hiển thị Real Due Date bằng thời điểm chuyển trạng thái tương ứng.
- **SC-002**: 100% chuỗi chuyển qua `Close`, `Resolve` và `Done` chọn đúng thời điểm của lần chuyển cuối cùng.
- **SC-003**: Không có task nào chưa từng đạt trạng thái kết thúc hợp lệ bị gán Real Due Date sai.
- **SC-004**: Việc hiển thị Real Due Date trong bảng đã lưu và bảng kết quả nhất quán với cùng dữ liệu Jira trong 100% kịch bản kiểm thử.

## Assumptions

- Trạng thái Jira được so khớp theo tên hiển thị sau khi bỏ khoảng trắng hai đầu và không phân biệt hoa thường.
- `Updated` đại diện cho thời điểm Jira ghi nhận lần chuyển trạng thái.
- Dữ liệu Real Due Date được làm mới từ lịch sử trạng thái của task khi truy vấn hoặc đồng bộ dữ liệu.
