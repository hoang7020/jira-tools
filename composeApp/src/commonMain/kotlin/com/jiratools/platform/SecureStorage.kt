package com.jiratools.platform

/** Lỗi khi kho bí mật của hệ điều hành không khả dụng (FR-003). */
class SecureStorageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Cổng lưu API token vào kho bí mật của hệ điều hành.
 *
 * Triển khai JVM dùng Keychain/Windows Credential Manager/libsecret; token không bao giờ
 * được ghi ra file cấu hình hay log. Khi kho bí mật không khả dụng, mọi thao tác phải ném
 * [SecureStorageException] thay vì âm thầm ghi ở dạng văn bản thuần (FR-003).
 */
interface SecureStorage {
    suspend fun saveToken(token: String)

    /** Trả về null khi chưa có token; không ném ngoại lệ trong trường hợp đó. */
    suspend fun loadToken(): String?

    /** Idempotent: xoá token khi chưa có cũng không gây lỗi. */
    suspend fun deleteToken()
}
