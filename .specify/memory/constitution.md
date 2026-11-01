<!--
Sync Impact Report
==================
- Version change: (scaffold chưa phê chuẩn) -> 1.0.0 (phê chuẩn lần đầu)
- Modified principles: không có - khởi tạo toàn bộ từ template core
- Added sections: Nguyên tắc Cốt lõi (I-V); Ràng buộc Kỹ thuật & Nền tảng;
  Quy trình Phát triển & Cổng Chất lượng; Quản trị (Governance)
- Removed sections: không
- Follow-up TODOs: không
- Ghi chú: tài liệu viết bằng tiếng Việt theo lựa chọn của dự án; các thuật ngữ
  Spec Kit (Core Principles, Governance, Constitution Check) giữ tiếng Anh để
  tương thích với template và các lệnh Spec Kit khác.
- Lưu ý: báo cáo này là ghi chú tạm thời cho người rà soát - xóa trước khi commit.
-->

# Hiến pháp Jira Tools (Constitution)

## Nguyên tắc Cốt lõi (Core Principles)

### I. Đa nền tảng trước, phát hành trên Desktop

Toàn bộ logic nghiệp vụ - model, tầng mạng, trạng thái, quy tắc xử lý - PHẢI nằm trong
`commonMain` và KHÔNG ĐƯỢC phụ thuộc API chỉ có trên JVM. Mã đặc thù nền tảng PHẢI được
cô lập sau ranh giới `expect`/`actual` (hoặc interface được inject), chỉ tồn tại trong
source set của nền tảng tương ứng và không rò rỉ vào mã dùng chung. Nền tảng phát hành
hiện tại là Desktop (JVM); việc thêm nền tảng mới KHÔNG ĐƯỢC đòi hỏi sửa đổi logic chia
sẻ - chỉ được bổ sung source set và phần `actual` tương ứng.

Lý do: giữ chi phí mở rộng nền tảng ở mức thấp nhất và tránh khóa cứng mã dùng chung vào JVM.

### II. Kiểm thử trước (Test-First, NON-NEGOTIABLE)

Chu trình bắt buộc cho logic chia sẻ: viết test trong `commonTest` -> xác nhận test thất
bại -> triển khai -> tái cấu trúc (Red-Green-Refactor). KHÔNG ĐƯỢC merge tính năng khi
logic chia sẻ chưa có test tương ứng. Test của logic chia sẻ PHẢI chạy được trên mọi nền
tảng mà mã đó hỗ trợ (hiện tại: JVM/Desktop). Tích hợp Jira PHẢI được kiểm thử bằng HTTP
fake/mock; test tự động KHÔNG ĐƯỢC gọi Jira thật.

Lý do: logic chia sẻ là nơi lỗi lan rộng nhất; test trong `commonTest` chạy trên mọi
target giúp phát hiện sớm và rẻ.

### III. Chia sẻ logic, chia sẻ UI có chọn lọc (Hybrid UI)

UI KHÔNG ĐƯỢC chứa quy tắc nghiệp vụ. Trạng thái màn hình PHẢI được phơi bày từ
`commonMain` qua ViewModel/holder dưới dạng `StateFlow` (hoặc tương đương) và tách biệt
khỏi Compose. Compose Multiplatform là mặc định cho UI dùng chung; UI đặc thù nền tảng
chỉ được dùng khi Compose không đáp ứng (khay hệ thống, tích hợp OS chuyên sâu, ...) và
PHẢI được biện minh trong `plan.md`. Mọi thành phần UI PHẢI tương tác với logic chia sẻ
qua interface ổn định định nghĩa trong `commonMain`.

Lý do: UI thay thế được, nghiệp vụ không bị rải rác; nền tảng mới chỉ cần một vỏ UI mỏng.

### IV. Kotlin chuẩn mực, an toàn kiểu (Idiomatic Type-Safe Kotlin)

Bất đồng bộ PHẢI dùng coroutines/Flow; KHÔNG ĐƯỢC chặn (block) luồng UI. Trạng thái và
kết quả PHẢI được mô hình hóa bằng `sealed interface`/`data class`; ưu tiên bất biến.
(De)serialization PHẢI dùng `kotlinx.serialization`; thư viện khác cần được biện minh.
Mọi phụ thuộc PHẢI khai báo trong Gradle version catalog (`gradle/libs.versions.toml`);
KHÔNG ĐƯỢC ghim phiên bản rải rác trong build script.

Lý do: chuẩn mực Kotlin giúp mã chạy được trên cả JVM lẫn Kotlin/Native khi mở rộng nền
tảng, đồng thời dễ kiểm thử và ít lỗi runtime.

### V. Đơn giản và gia tăng (Simplicity & YAGNI)

Khởi đầu với cấu trúc module tối giản (một module ứng dụng với các source set KMP
chuẩn); chỉ tách module mới khi có nhu cầu thực tế được chứng minh trong `plan.md`.
KHÔNG ĐƯỢC trừu tượng hóa trước nhu cầu. Mọi độ phức tạp hoặc ngoại lệ PHẢI được ghi
nhận trong mục "Complexity Tracking" của `plan.md`.

Lý do: chi phí bảo trì của module/abstraction thừa thường cao hơn lợi ích giả định.

## Ràng buộc Kỹ thuật & Nền tảng (Additional Constraints)

- Phiên bản: dùng Kotlin stable, ghim trong version catalog; cặp phiên bản Kotlin và
  Compose Multiplatform PHẢI tương thích và được chốt cùng lúc; nâng cấp qua PR riêng,
  không nâng cấp ngầm trong PR tính năng.
- Desktop (JVM): JDK 17 (LTS) trở lên; build bằng Gradle (Kotlin DSL); Gradle Wrapper
  PHẢI được commit.
- Mạng: HTTP client Kotlin-first chạy trong `commonMain` (Ktor hoặc tương đương); mọi
  lời gọi Jira REST API PHẢI đi qua lớp client riêng trong `commonMain`, tách biệt hoàn
  toàn khỏi UI.
- Bảo mật: token/mật khẩu Jira KHÔNG ĐƯỢC commit vào repo hoặc ghi vào log; lưu trữ cục
  bộ PHẢI qua secure storage của nền tảng (expect/actual).
- Ngôn ngữ: định danh mã nguồn và tên test dùng tiếng Anh; tài liệu (constitution, spec,
  plan, tasks) dùng tiếng Việt, giữ thuật ngữ kỹ thuật tiếng Anh khi cần.

## Quy trình Phát triển & Cổng Chất lượng (Development Workflow & Quality Gates)

- Mọi thay đổi tính năng PHẢI đi qua quy trình Spec Kit: `/speckit-specify` ->
  `/speckit-plan` -> `/speckit-tasks` -> `/speckit-implement`. `plan.md` PHẢI có mục
  "Constitution Check" đạt yêu cầu hoặc biện minh đầy đủ cho từng vi phạm.
- Review: mỗi PR PHẢI kèm đánh giá tuân thủ 5 nguyên tắc; người review PHẢI kiểm tra
  ranh giới `commonMain` và phần nền tảng, test đi kèm và tính đơn giản của thay đổi.
- Cổng chất lượng bắt buộc trước khi merge: build thành công target Desktop; toàn bộ
  test pass; lint/format (ktlint, detekt hoặc tương đương) sạch.
- Phụ thuộc PHẢI được rà soát định kỳ; bản vá bảo mật được xử lý ưu tiên qua PR riêng.

## Quản trị (Governance)

- Hiệu lực: hiến pháp thắng mọi thực hành khác; mọi spec, plan, task và PR PHẢI tuân
  thủ; khi xung đột, hiến pháp là nguồn chuẩn.
- Sửa đổi: chỉ thực hiện qua `/speckit-constitution`, kèm Sync Impact Report và lý do
  thay đổi. Phiên bản PHẢI tăng theo semver: MAJOR khi bỏ hoặc đổi nghĩa nguyên tắc,
  MINOR khi thêm nguyên tắc/mục hoặc mở rộng đáng kể, PATCH khi làm rõ câu chữ.
- Kiểm tra tuân thủ: mọi plan PHẢI qua Constitution Check; `/speckit-analyze` coi vi
  phạm nguyên tắc là CRITICAL; vi phạm PHẢI được sửa ở spec/plan/tasks - không được
  diễn giải lại nguyên tắc.
- Mọi sửa đổi PHẢI cập nhật dòng phiên bản bên dưới theo ngày ISO (YYYY-MM-DD).

**Version**: 1.0.0 | **Ratified**: 2026-10-30 | **Last Amended**: 2026-10-30
