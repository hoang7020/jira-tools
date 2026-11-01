package com.jiratools.platform

import com.github.javakeyring.BackendNotSupportedException
import com.github.javakeyring.Keyring
import com.github.javakeyring.PasswordAccessException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lưu API token vào kho bí mật của hệ điều hành (macOS Keychain, Windows Credential Manager,
 * freedesktop Secret Service).
 *
 * Fail-closed theo FR-003: khi backend không khả dụng, mọi thao tác ném [SecureStorageException]
 * và tuyệt đối không ghi token ra file dạng văn bản thuần.
 */
class JvmSecureStorage : SecureStorage {
    override suspend fun saveToken(token: String) {
        withContext(Dispatchers.IO) {
            withKeyring { keyring -> keyring.setPassword(SERVICE, ACCOUNT, token) }
        }
    }

    override suspend fun loadToken(): String? =
        withContext(Dispatchers.IO) {
            withKeyring { keyring ->
                try {
                    keyring.getPassword(SERVICE, ACCOUNT)
                } catch (failure: PasswordAccessException) {
                    // Thư viện dùng cùng loại ngoại lệ cho cả "chưa có token" và "không truy cập
                    // được". Coi như chưa có token để người dùng nhập lại, thay vì chặn ứng dụng.
                    null
                }
            }
        }

    override suspend fun deleteToken() {
        withContext(Dispatchers.IO) {
            withKeyring { keyring ->
                try {
                    keyring.deletePassword(SERVICE, ACCOUNT)
                } catch (failure: PasswordAccessException) {
                    // Idempotent: xoá khi chưa có token cũng được coi là thành công.
                }
            }
        }
    }

    private inline fun <T> withKeyring(block: (Keyring) -> T): T =
        try {
            block(keyring)
        } catch (failure: PasswordAccessException) {
            throw SecureStorageException("Không truy cập được kho bí mật của hệ điều hành.", failure)
        }

    private companion object {
        const val SERVICE = "jira-tools"
        const val ACCOUNT = "jira-tools:api-token"

        val keyring: Keyring by lazy {
            try {
                Keyring.create()
            } catch (failure: BackendNotSupportedException) {
                throw SecureStorageException("Không tìm thấy kho bí mật của hệ điều hành.", failure)
            }
        }
    }
}
