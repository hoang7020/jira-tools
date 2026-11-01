package com.jiratools.data

import com.jiratools.domain.BaseUrlResult
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import com.jiratools.domain.SavedConnectionInfo
import com.jiratools.domain.normalizeBaseUrl
import com.jiratools.platform.SecureStorage
import com.jiratools.platform.SecureStorageException
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val EMPTY_FIELDS_MESSAGE = "Vui lòng nhập đầy đủ địa chỉ Jira, email và API token."

private data class SavedConnectionSnapshot(
    val info: SavedConnectionInfo,
    val credentials: JiraCredentials?,
)

data class SavedConnectionVerification(
    val status: ConnectionStatus,
    val info: SavedConnectionInfo,
)

/**
 * Nghiệp vụ cấu hình kết nối Jira (FR-001, FR-002, FR-003).
 *
 * Token chỉ được lưu vào kho bí mật của hệ điều hành sau khi Jira xác thực thành công, nên một
 * lần kiểm tra thất bại không để lại dấu vết nào trên đĩa.
 */
class ConnectionRepository(
    private val apiClient: JiraApiClient,
    private val configStore: AppConfigStore,
    private val secureStorage: SecureStorage,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    /**
     * Đọc cấu hình đã lưu mà không truy cập kho bí mật, tránh hỏi quyền hệ điều hành lúc khởi động.
     */
    suspend fun loadSaved(): ConnectionStatus =
        withContext(dispatcher) {
            val preferences = configStore.load()
            val baseUrl = preferences.baseUrl?.trim().orEmpty()
            val email = preferences.email?.trim().orEmpty()
            if (baseUrl.isEmpty() || email.isEmpty()) {
                ConnectionStatus.Unconfigured
            } else {
                ConnectionStatus.Unverified(baseUrl = baseUrl, email = email)
            }
        }

    /** Đọc địa chỉ/email và chỉ báo token đã lưu mà không trả giá trị token ra khỏi tầng dữ liệu. */
    suspend fun savedInfo(): SavedConnectionInfo =
        withContext(dispatcher) {
            readSavedConnectionSnapshot().info
        }

    /** Xác minh thông tin đã lưu mà không ghi cấu hình hoặc thay đổi token. */
    suspend fun verifySaved(): ConnectionStatus = verifySavedWithInfo().status

    /** Cùng kết quả xác minh kèm trạng thái token đã đọc trong chính snapshot đó. */
    suspend fun verifySavedWithInfo(): SavedConnectionVerification =
        withContext(dispatcher) {
            val saved = readSavedConnectionSnapshot()
            val status =
                saved.credentials?.let { credentials ->
                    when (val response = apiClient.myself(explicit = credentials)) {
                        is ApiResult.Success ->
                            ConnectionStatus.Connected(
                                baseUrl = saved.info.baseUrl,
                                email = saved.info.email,
                                jiraTimeZone = profileTimeZone(response.value),
                            )
                        is ApiResult.Failure ->
                            when (response.error) {
                                JiraError.AuthFailed, JiraError.PermissionDenied ->
                                    ConnectionStatus.Invalid(response.error)
                                else ->
                                    ConnectionStatus.Unverified(
                                        baseUrl = saved.info.baseUrl,
                                        email = saved.info.email,
                                    )
                            }
                    }
                } ?: ConnectionStatus.Unconfigured
            SavedConnectionVerification(status = status, info = saved.info)
        }

    /** Xoá token trước, rồi chỉ xoá địa chỉ/email khỏi cấu hình. */
    suspend fun disconnect(): ConnectionStatus =
        withContext(dispatcher) {
            secureStorage.deleteToken()
            val preferences = configStore.load()
            configStore.save(preferences.copy(baseUrl = null, email = null))
            ConnectionStatus.Unconfigured
        }

    /**
     * Kiểm tra thông tin đăng nhập bằng endpoint `/myself`; chỉ lưu khi Jira chấp nhận.
     */
    suspend fun validate(
        baseUrl: String,
        email: String,
        token: String,
    ): ConnectionStatus =
        withContext(dispatcher) {
            val trimmedBaseUrl = baseUrl.trim()
            val trimmedEmail = email.trim()
            val trimmedToken = token.trim()
            if (trimmedBaseUrl.isEmpty() || trimmedEmail.isEmpty() || trimmedToken.isEmpty()) {
                return@withContext ConnectionStatus.Invalid(JiraError.Unexpected(detail = EMPTY_FIELDS_MESSAGE))
            }
            val normalizedBaseUrl =
                when (val result = normalizeBaseUrl(trimmedBaseUrl)) {
                    is BaseUrlResult.Valid -> result.baseUrl
                    is BaseUrlResult.Invalid ->
                        return@withContext ConnectionStatus.Invalid(JiraError.Unexpected(detail = result.message))
                }
            val credentials = JiraCredentials(baseUrl = normalizedBaseUrl, email = trimmedEmail, token = trimmedToken)
            when (val response = apiClient.myself(explicit = credentials)) {
                is ApiResult.Success -> {
                    secureStorage.saveToken(trimmedToken)
                    val preferences = configStore.load()
                    configStore.save(preferences.copy(baseUrl = normalizedBaseUrl, email = trimmedEmail))
                    ConnectionStatus.Connected(
                        baseUrl = normalizedBaseUrl,
                        email = trimmedEmail,
                        jiraTimeZone = profileTimeZone(response.value),
                    )
                }
                is ApiResult.Failure -> ConnectionStatus.Invalid(response.error)
            }
        }

    private suspend fun readSavedConnectionSnapshot(): SavedConnectionSnapshot {
        val preferences = configStore.load()
        val baseUrl = preferences.baseUrl?.trim().orEmpty()
        val email = preferences.email?.trim().orEmpty()
        val token =
            try {
                secureStorage.loadToken()?.trim()?.takeIf(String::isNotEmpty)
            } catch (_: SecureStorageException) {
                null
            }
        val info = SavedConnectionInfo(baseUrl = baseUrl, email = email, tokenSaved = token != null)
        val credentials =
            if (baseUrl.isNotEmpty() && email.isNotEmpty() && token != null) {
                JiraCredentials(baseUrl = baseUrl, email = email, token = token)
            } else {
                null
            }
        return SavedConnectionSnapshot(info = info, credentials = credentials)
    }

    private fun profileTimeZone(profile: MyselfDto): String? {
        val timeZone = profile.timeZone?.trim() ?: return null
        return timeZone.takeIf(String::isNotEmpty)
    }
}
