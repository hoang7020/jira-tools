package com.jiratools.ui.connection

import com.jiratools.data.ConnectionRepository
import com.jiratools.data.JiraErrorMapper
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.userMessage
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Trạng thái cấu hình kết nối cho giao diện (Nguyên tắc III - logic nằm ngoài Compose).
 *
 * Các hàm đều là `suspend` để tầng giao diện tự quyết định phạm vi coroutine, nhờ đó kiểm thử
 * diễn ra tất định mà không cần ViewModel sở hữu `CoroutineScope` riêng.
 */
class ConnectionViewModel(
    private val repository: ConnectionRepository,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val _status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unconfigured)
    private val _draft = MutableStateFlow(ConnectionDraft())
    private val _hasSavedConnection = MutableStateFlow(false)
    private val _tokenSaved = MutableStateFlow<Boolean?>(null)
    private val _notice = MutableStateFlow<String?>(null)

    val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
    val draft: StateFlow<ConnectionDraft> = _draft.asStateFlow()
    val hasSavedConnection: StateFlow<Boolean> = _hasSavedConnection.asStateFlow()
    val tokenSaved: StateFlow<Boolean?> = _tokenSaved.asStateFlow()
    val notice: StateFlow<String?> = _notice.asStateFlow()

    fun updateDraft(draft: ConnectionDraft) {
        _draft.value = draft
    }

    fun clearNotice() {
        _notice.value = null
    }

    suspend fun loadSaved() {
        val savedStatus = withContext(dispatcher) { guard { repository.loadSaved() } }
        _status.value = savedStatus
        _hasSavedConnection.value = savedStatus is ConnectionStatus.Unverified
        if (_draft.value == ConnectionDraft() && savedStatus is ConnectionStatus.Unverified) {
            _draft.value = ConnectionDraft(baseUrl = savedStatus.baseUrl, email = savedStatus.email)
        }
    }

    suspend fun verifySaved() {
        _status.value = ConnectionStatus.Validating
        val result = withContext(dispatcher) { guardResult { repository.verifySavedWithInfo() } }
        result.fold(
            onSuccess = { verification ->
                _status.value = verification.status
                _hasSavedConnection.value = verification.info.hasSavedConnection
                _tokenSaved.value = verification.info.tokenSaved
            },
            onFailure = { failure ->
                _status.value = ConnectionStatus.Invalid(JiraErrorMapper.fromThrowable(failure))
            },
        )
    }

    suspend fun validate(
        baseUrl: String,
        email: String,
        token: String,
    ) {
        _notice.value = null
        _status.value = ConnectionStatus.Validating
        val result =
            withContext(dispatcher) {
                guard { repository.validate(baseUrl = baseUrl, email = email, token = token) }
            }
        _status.value = result
        if (result is ConnectionStatus.Connected) {
            _hasSavedConnection.value = true
            _tokenSaved.value = true
            _draft.value = ConnectionDraft(baseUrl = result.baseUrl, email = result.email)
        }
    }

    suspend fun disconnect() {
        _notice.value = null
        val result = withContext(dispatcher) { guard { repository.disconnect() } }
        if (result is ConnectionStatus.Invalid) {
            _notice.value = result.error.userMessage()
            return
        }
        _status.value = result
        _hasSavedConnection.value = false
        _tokenSaved.value = false
        _draft.value = ConnectionDraft()
        _notice.value = "Đã ngắt kết nối Jira."
    }

    private suspend fun guard(block: suspend () -> ConnectionStatus): ConnectionStatus =
        guardResult(block).getOrElse { failure ->
            ConnectionStatus.Invalid(JiraErrorMapper.fromThrowable(failure))
        }

    private suspend fun <T> guardResult(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
}
