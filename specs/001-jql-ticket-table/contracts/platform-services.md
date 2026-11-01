# Hợp đồng dịch vụ nền tảng (Platform Services)

**Feature**: `001-jql-ticket-table` | **Ngày**: 2026-10-30

Các dịch vụ nền tảng là điểm ranh giới duy nhất giữa `commonMain` và JVM (Nguyên tắc I). Mọi khai báo `expect` nằm trong `com.jiratools.platform`; implementation `actual` nằm trong `jvmMain`. Không có mã nền tảng nào rò rỉ vào các package `domain`/`ui`.

## 1. SecureStorage - lưu trữ API token an toàn (FR-003)

```kotlin
expect class SecureStorage {
    suspend fun saveToken(token: String)
    suspend fun loadToken(): String?
    suspend fun deleteToken()
}
```

**Hợp đồng hành vi**:

- `saveToken`: ghi đè giá trị cũ; token **không bao giờ** được serialize vào file cấu hình hay ghi log.
- `loadToken`: trả `null` nếu chưa từng lưu hoặc đã bị xóa. Không ném ngoại lệ khi chưa có giá trị.
- `deleteToken`: idempotent - gọi nhiều lần không gây lỗi.
- Mọi hàm `suspend` vì I/O có thể chậm; không chạy trên main thread (Nguyên tắc IV).
- Nếu kho hệ điều hành không khả dụng: ném `SecureStorageException` - tầng gọi chuyển thành `QueryError`/`ConnectionError` phù hợp, không làm sập ứng dụng.

**Ánh xạ JVM** (đề xuất, chốt khi triển khai): keychain của hệ điều hành qua thư viện Java keyring.

## 2. AppPaths - vị trí file cấu hình (FR-012, FR-014)

```kotlin
expect class AppPaths {
    fun configFile(): String
}
```

**Hợp đồng hành vi**:

- Trả về đường dẫn tuyệt đối tới file JSON lưu `UserPreferences`, không phụ thuộc thư mục làm việc hiện tại.
- Thư mục cha được tạo tự động khi ghi lần đầu.
- Một người dùng chỉ có một file cấu hình duy nhất.

## 3. UrlOpener - mở ticket trên trình duyệt (FR-013)

```kotlin
expect class UrlOpener {
    fun open(url: String)
}
```

**Hợp đồng hành vi**:

- Mở URL bằng trình duyệt mặc định của hệ điều hành.
- Không ném ngoại lệ ra ngoài khi mở thất bại; ghi nhận và hiển thị thông báo không chặn luồng chính.
- Chỉ được gọi với URL được tạo từ `Ticket.url` đã dẫn xuất, không nhận URL tự do từ người dùng.

## 4. ioDispatcher - luồng I/O

```kotlin
expect val ioDispatcher: CoroutineDispatcher
```

**Hợp đồng hành vi**:

- Trả về dispatcher dùng cho mọi I/O mạng và đĩa trong `commonMain`.
- Có thể thay thế trong test bằng dispatcher xác định (Nguyên tắc II) - do đó không được hard-code trong `viewModelScope`.

## Ràng buộc chung

- Bất kỳ `expect` mới nào cũng phải có `actual` cho JVM và test double ở `commonTest`.
- Các interface này được inject qua `AppContainer` (không dùng DI framework - YAGNI).
