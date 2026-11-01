package com.jiratools.ui.connection

import com.jiratools.data.AppConfigStore
import com.jiratools.data.ConnectionRepository
import com.jiratools.data.HttpClientFactory
import com.jiratools.data.JiraApiClient
import com.jiratools.data.UnknownHostException
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import com.jiratools.domain.userMessage
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.FakeSecureStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

private const val SAVED_CONFIG =
    """{"schemaVersion":1,"baseUrl":"https://congty.atlassian.net","email":"dev@congty.vn"}"""

private const val MYSELF_BODY = """{"displayName":"Nguyen Van A"}"""

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionViewModelTest {
    private val baseUrl = "https://congty.atlassian.net"

    private val email = "dev@congty.vn"

    private val token = "api-token-bi-mat"

    private fun createViewModel(
        engine: MockEngine,
        paths: FakeAppPaths = FakeAppPaths(),
        storage: FakeSecureStorage = FakeSecureStorage(),
    ) = ConnectionViewModel(
        repository =
            ConnectionRepository(
                apiClient = JiraApiClient(client = HttpClientFactory.create(engine)),
                configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
                secureStorage = storage,
                dispatcher = UnconfinedTestDispatcher(),
            ),
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun trangThaiBanDauLaUnconfigured() =
        runTest {
            val viewModel =
                createViewModel(engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) })

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
        }

    @Test
    fun stateSurfaceStartsEmpty() =
        runTest {
            val viewModel =
                createViewModel(engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) })

            assertEquals(ConnectionDraft(), viewModel.draft.value)
            assertNull(viewModel.tokenSaved.value)
            assertNull(viewModel.notice.value)
        }

    @Test
    fun updateDraftChangesOnlyLocalState() =
        runTest {
            val engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) }
            val viewModel = createViewModel(engine = engine)
            val draft = ConnectionDraft(baseUrl = baseUrl, email = email, token = token)

            viewModel.updateDraft(draft)

            assertEquals(draft, viewModel.draft.value)
            assertTrue(engine.requestHistory.isEmpty(), "Updating the draft must not call Jira")
        }

    @Test
    fun clearNoticeLeavesTheInitialNoticeEmpty() =
        runTest {
            val viewModel =
                createViewModel(engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) })

            viewModel.clearNotice()

            assertNull(viewModel.notice.value)
        }

    @Test
    fun loadSavedDocCauHinhDaLuu() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val viewModel =
                createViewModel(
                    engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) },
                    paths = paths,
                )

            viewModel.loadSaved()

            assertEquals(ConnectionStatus.Unverified(baseUrl = baseUrl, email = email), viewModel.status.value)
            assertEquals(ConnectionDraft(baseUrl = baseUrl, email = email), viewModel.draft.value)
        }

    @Test
    fun loadSavedKhiChuaCoCauHinh() =
        runTest {
            val viewModel =
                createViewModel(engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) })

            viewModel.loadSaved()

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
            assertEquals(ConnectionDraft(), viewModel.draft.value)
        }

    @Test
    fun loadSavedDoesNotReadTheSecretStore() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val viewModel =
                createViewModel(
                    engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) },
                    paths = paths,
                    storage = FakeSecureStorage(failOnLoad = true),
                )

            viewModel.loadSaved()

            assertEquals(ConnectionStatus.Unverified(baseUrl = baseUrl, email = email), viewModel.status.value)
            assertEquals("", viewModel.draft.value.token)
        }

    @Test
    fun loadSavedDoesNotOverwriteDraftAlreadyBeingEdited() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val viewModel =
                createViewModel(
                    engine = MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK) },
                    paths = paths,
                )
            val draft = ConnectionDraft(baseUrl = "https://other.example.com", email = email, token = "draft-token")
            viewModel.updateDraft(draft)

            viewModel.loadSaved()

            assertEquals(draft, viewModel.draft.value)
        }

    @Test
    fun validatePhatValidatingTrongKhiChoPhanHoi() =
        runTest {
            val observed = mutableListOf<ConnectionStatus>()
            lateinit var viewModel: ConnectionViewModel
            val engine =
                MockEngine {
                    observed += viewModel.status.value
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            viewModel = createViewModel(engine = engine)

            viewModel.validate(baseUrl = baseUrl, email = email, token = token)

            assertEquals<List<ConnectionStatus>>(listOf(ConnectionStatus.Validating), observed)
            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), viewModel.status.value)
        }

    @Test
    fun validateThanhCongChuyenSangConnected() =
        runTest {
            val storage = FakeSecureStorage()
            val engine =
                MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val viewModel = createViewModel(engine = engine, storage = storage)

            viewModel.validate(baseUrl = baseUrl, email = email, token = token)

            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), viewModel.status.value)
            assertEquals(listOf(token), storage.savedTokens)
        }

    @Test
    fun validateSuccessStoresTheTokenAndClearsItFromTheDraft() =
        runTest {
            val storage = FakeSecureStorage()
            val engine =
                MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val viewModel = createViewModel(engine = engine, storage = storage)
            viewModel.updateDraft(ConnectionDraft(token = token, tokenVisible = true))

            viewModel.validate(baseUrl = "congty.atlassian.net/", email = email, token = token)

            assertEquals(true, viewModel.tokenSaved.value)
            assertEquals(ConnectionDraft(baseUrl = baseUrl, email = email), viewModel.draft.value)
        }

    @Test
    fun validateFailureKeepsThePreviouslySavedConfigurationAndToken() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val storage = FakeSecureStorage()
            storage.saveToken("old-saved-token")
            val configBefore = paths.storedConfig()
            val tokensBefore = storage.savedTokens.toList()
            val engine =
                MockEngine {
                    respond(
                        content = """{"errorMessages":["Client must be authenticated."],"errors":{}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = JSON_HEADERS,
                    )
                }
            val viewModel = createViewModel(engine = engine, paths = paths, storage = storage)

            viewModel.validate(baseUrl = baseUrl, email = email, token = "new-invalid-token")

            assertIs<ConnectionStatus.Invalid>(viewModel.status.value)
            assertEquals(configBefore, paths.storedConfig())
            assertEquals(tokensBefore, storage.savedTokens)
        }

    @Test
    fun validateThatBaiChuyenSangInvalid() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = """{"errorMessages":["Client must be authenticated."],"errors":{}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = JSON_HEADERS,
                    )
                }
            val viewModel = createViewModel(engine = engine)

            viewModel.validate(baseUrl = baseUrl, email = email, token = "token-sai")

            val status = viewModel.status.value
            assertIs<ConnectionStatus.Invalid>(status)
            assertEquals(JiraError.AuthFailed, status.error)
        }

    @Test
    fun loiKhoBiMatKhongLamSapUngDung() =
        runTest {
            val storage = FakeSecureStorage(failOnSave = true)
            val engine =
                MockEngine { respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val viewModel = createViewModel(engine = engine, storage = storage)

            viewModel.validate(baseUrl = baseUrl, email = email, token = token)

            val status = viewModel.status.value
            assertIs<ConnectionStatus.Invalid>(status)
            assertTrue(status.error.userMessage().isNotBlank())
        }

    @Test
    fun verifySavedShowsValidatingThenConnectsAndMarksTokenSaved() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val storage = FakeSecureStorage()
            storage.saveToken(token)
            val observed = mutableListOf<ConnectionStatus>()
            lateinit var viewModel: ConnectionViewModel
            val engine =
                MockEngine {
                    observed += viewModel.status.value
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            viewModel = createViewModel(engine = engine, paths = paths, storage = storage)
            viewModel.loadSaved()

            viewModel.verifySaved()

            assertEquals<List<ConnectionStatus>>(listOf(ConnectionStatus.Validating), observed)
            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), viewModel.status.value)
            assertEquals(true, viewModel.tokenSaved.value)
        }

    @Test
    fun verifySavedWithoutTokenMarksTokenNotSaved() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val viewModel = createViewModel(engine = engine, paths = paths)
            viewModel.loadSaved()

            viewModel.verifySaved()

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
            assertEquals(true, viewModel.hasSavedConnection.value)
            assertEquals(false, viewModel.tokenSaved.value)
            assertTrue(engine.requestHistory.isEmpty())
        }

    @Test
    fun verifySavedWithOrphanedTokenStillAllowsDisconnect() =
        runTest {
            val storage = FakeSecureStorage()
            storage.saveToken(token)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val viewModel = createViewModel(engine = engine, storage = storage)
            viewModel.loadSaved()

            viewModel.verifySaved()

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
            assertEquals(true, viewModel.tokenSaved.value)
            assertEquals(true, viewModel.hasSavedConnection.value)
            assertTrue(engine.requestHistory.isEmpty())
        }

    @Test
    fun verifySavedNetworkFailureLeavesConnectionUnverifiedWithoutThrowing() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val storage = FakeSecureStorage()
            storage.saveToken(token)
            val engine = MockEngine { throw UnknownHostException(baseUrl) }
            val viewModel = createViewModel(engine = engine, paths = paths, storage = storage)
            viewModel.loadSaved()

            viewModel.verifySaved()

            assertEquals(ConnectionStatus.Unverified(baseUrl = baseUrl, email = email), viewModel.status.value)
            assertEquals(true, viewModel.tokenSaved.value)
        }

    @Test
    fun verifySavedSecretStoreFailureDoesNotCrashAndMarksTokenNotSaved() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val viewModel =
                createViewModel(
                    engine = engine,
                    paths = paths,
                    storage = FakeSecureStorage(failOnLoad = true),
                )
            viewModel.loadSaved()

            viewModel.verifySaved()

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
            assertEquals(true, viewModel.hasSavedConnection.value)
            assertEquals(false, viewModel.tokenSaved.value)
            assertTrue(engine.requestHistory.isEmpty())
        }

    @Test
    fun disconnectSuccessClearsConnectionStateAndReportsSuccess() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val storage = FakeSecureStorage()
            storage.saveToken(token)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val viewModel = createViewModel(engine = engine, paths = paths, storage = storage)
            viewModel.loadSaved()
            viewModel.verifySaved()
            viewModel.updateDraft(ConnectionDraft(baseUrl = baseUrl, email = email, token = "draft-token"))

            viewModel.disconnect()

            assertEquals(ConnectionStatus.Unconfigured, viewModel.status.value)
            assertEquals(false, viewModel.hasSavedConnection.value)
            assertEquals(false, viewModel.tokenSaved.value)
            assertEquals(ConnectionDraft(), viewModel.draft.value)
            assertContains(viewModel.notice.value.orEmpty(), "ngắt kết nối")
            assertNull(storage.loadToken())
        }

    @Test
    fun disconnectSecretStoreFailurePreservesStateAndReportsErrorWithoutThrowing() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(SAVED_CONFIG)
            val storage = FakeSecureStorage(failOnDelete = true)
            storage.saveToken(token)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val viewModel = createViewModel(engine = engine, paths = paths, storage = storage)
            viewModel.loadSaved()
            viewModel.verifySaved()
            val statusBefore = viewModel.status.value
            val draftBefore = ConnectionDraft(baseUrl = baseUrl, email = email)
            viewModel.updateDraft(draftBefore)

            viewModel.disconnect()

            assertEquals(statusBefore, viewModel.status.value)
            assertEquals(true, viewModel.hasSavedConnection.value)
            assertEquals(true, viewModel.tokenSaved.value)
            assertEquals(draftBefore, viewModel.draft.value)
            assertContains(viewModel.notice.value.orEmpty(), "Kho bí mật")
            assertEquals(token, storage.loadToken())
        }
}
