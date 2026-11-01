package com.jiratools.data

import com.jiratools.domain.ColumnKey
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import com.jiratools.domain.SavedConnectionInfo
import com.jiratools.domain.SortState
import com.jiratools.domain.UserPreferences
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.FakeSecureStorage
import com.jiratools.platform.SecureStorageException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.encodeBase64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionRepositoryTest {
    private val baseUrl = "https://congty.atlassian.net"

    private val email = "dev@congty.vn"

    private val token = "api-token-bi-mat"

    private fun repository(
        engine: MockEngine,
        paths: FakeAppPaths,
        storage: FakeSecureStorage,
    ) = ConnectionRepository(
        apiClient = JiraApiClient(client = HttpClientFactory.create(engine)),
        configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
        secureStorage = storage,
        dispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun kiemTraThanhCongPhatTrangThaiKetNoiVaLuuCauHinh() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine { request ->
                    assertEquals("/rest/api/3/myself", request.url.encodedPath)
                    assertEquals(
                        "Basic ${"$email:$token".encodeBase64()}",
                        request.headers[HttpHeaders.Authorization],
                    )
                    respond(
                        content = """{"accountId":"abc","displayName":"Nguyen Van A","active":true}""",
                        status = HttpStatusCode.OK,
                        headers = JSON_HEADERS,
                    )
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), status)
            assertEquals(listOf(token), storage.savedTokens)
            val saved = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).load()
            assertEquals(baseUrl, saved.baseUrl)
            assertEquals(email, saved.email)
        }

    @Test
    fun connectMapsTimezoneFromTheJiraProfile() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine {
                    respond(
                        content = """{"timeZone":"Europe/Paris"}""",
                        status = HttpStatusCode.OK,
                        headers = JSON_HEADERS,
                    )
                }

            val status = repository(engine, paths, storage).validate(baseUrl, email, token)

            assertEquals(
                ConnectionStatus.Connected(baseUrl, email, jiraTimeZone = "Europe/Paris"),
                status,
            )
        }

    @Test
    fun missingOrUnrecognizedProfileTimezoneDoesNotInvalidateConnection() =
        runTest {
            listOf(
                "{}" to null,
                """{"timeZone":"Not/A_Real_Zone"}""" to "Not/A_Real_Zone",
            ).forEach { (profile, expectedTimeZone) ->
                val status =
                    repository(
                        engine =
                            MockEngine {
                                respond(content = profile, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                            },
                        paths = FakeAppPaths(),
                        storage = FakeSecureStorage(),
                    ).validate(baseUrl, email, token)

                val connectedStatus = assertIs<ConnectionStatus.Connected>(status)
                assertEquals(expectedTimeZone, connectedStatus.jiraTimeZone)
            }
        }

    @Test
    fun saiXacThucTraVeLoiVaKhongLuuGi() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine {
                    respond(
                        content = """{"errorMessages":["Client must be authenticated."],"errors":{}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = JSON_HEADERS,
                    )
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = "token-sai")

            assertIs<ConnectionStatus.Invalid>(status)
            assertEquals(JiraError.AuthFailed, status.error)
            assertTrue(storage.savedTokens.isEmpty())
            assertNull(paths.storedConfig())
        }

    @Test
    fun khongPhanGiaiDuocTenMienTraVeLoiSaiDiaChi() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine = MockEngine { throw UnknownHostException(baseUrl) }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

            assertIs<ConnectionStatus.Invalid>(status)
            assertIs<JiraError.InstanceNotFound>(status.error)
            assertTrue(storage.savedTokens.isEmpty())
        }

    @Test
    fun quaThoiGianChoTraVeLoiTimeout() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine = MockEngine { throw HttpRequestTimeoutException("het thoi gian cho") }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

            assertIs<ConnectionStatus.Invalid>(status)
            assertEquals(JiraError.Timeout, status.error)
        }

    @Test
    fun truongRongKhongGoiMang() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = "", email = "", token = "")

            assertIs<ConnectionStatus.Invalid>(status)
            assertTrue(engine.requestHistory.isEmpty())
            assertTrue(storage.savedTokens.isEmpty())
            assertNull(paths.storedConfig())
        }

    @Test
    fun emailChiCoKhoangTrangKhongGoiMang() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = "   ", token = token)

            assertIs<ConnectionStatus.Invalid>(status)
            assertTrue(engine.requestHistory.isEmpty())
        }

    @Test
    fun diaChiKhongHopLeKhongGoiMang() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = "khong phai dia chi", email = email, token = token)

            assertIs<ConnectionStatus.Invalid>(status)
            assertEquals(JiraError.Unexpected::class, status.error::class)
            assertTrue(engine.requestHistory.isEmpty())
        }

    @Test
    fun diaChiThieuSchemeDuocChuanHoaTruocKhiGoi() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine { request ->
                    assertEquals("congty.atlassian.net", request.url.host)
                    assertEquals("https", request.url.protocol.name)
                    respond(
                        content = """{"displayName":"Nguyen Van A"}""",
                        status = HttpStatusCode.OK,
                        headers = JSON_HEADERS,
                    )
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = "congty.atlassian.net/", email = email, token = token)

            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), status)
            val saved = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).load()
            assertEquals(baseUrl, saved.baseUrl)
        }

    @Test
    fun savedInfoIsEmptyWhenNothingIsSaved() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val repository =
                repository(
                    engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) },
                    paths = paths,
                    storage = storage,
                )

            assertEquals(SavedConnectionInfo(), repository.savedInfo())
        }

    @Test
    fun savedInfoTrimsConfigurationAndReportsMissingToken() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig(
                """{"schemaVersion":1,"baseUrl":"  $baseUrl  ","email":"  $email  "}""",
            )
            val repository =
                repository(
                    engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) },
                    paths = paths,
                    storage = FakeSecureStorage(),
                )

            assertEquals(
                SavedConnectionInfo(baseUrl = baseUrl, email = email, tokenSaved = false),
                repository.savedInfo(),
            )
        }

    @Test
    fun savedInfoReportsTokenPresenceWithoutReturningTheToken() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("""{"schemaVersion":1,"baseUrl":"$baseUrl","email":"$email"}""")
            val storage = FakeSecureStorage()
            storage.saveToken(token)
            val repository =
                repository(
                    engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) },
                    paths = paths,
                    storage = storage,
                )

            val info = repository.savedInfo()

            assertEquals(SavedConnectionInfo(baseUrl = baseUrl, email = email, tokenSaved = true), info)
            assertFalse(info.toString().contains(token), "Saved connection data must never include the token")
        }

    @Test
    fun savedInfoTreatsSecretStoreReadFailureAsNoSavedToken() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("""{"schemaVersion":1,"baseUrl":"$baseUrl","email":"$email"}""")
            val repository =
                repository(
                    engine = MockEngine { respond(content = "", status = HttpStatusCode.OK, headers = JSON_HEADERS) },
                    paths = paths,
                    storage = FakeSecureStorage(failOnLoad = true),
                )

            assertEquals(
                SavedConnectionInfo(baseUrl = baseUrl, email = email, tokenSaved = false),
                repository.savedInfo(),
            )
        }

    @Test
    fun verifySavedWithoutConfigurationDoesNotCallJira() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            assertEquals(ConnectionStatus.Unconfigured, repository.verifySaved())
            assertTrue(engine.requestHistory.isEmpty())
            assertNull(paths.storedConfig())
            assertTrue(storage.savedTokens.isEmpty())
        }

    @Test
    fun verifySavedWithoutTokenDoesNotCallJira() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val config = savedConfig()
            paths.seedRawConfig(config)
            val engine =
                MockEngine {
                    respond(content = MYSELF_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            assertEquals(ConnectionStatus.Unconfigured, repository.verifySaved())
            assertTrue(engine.requestHistory.isEmpty())
            assertEquals(config, paths.storedConfig())
            assertTrue(storage.savedTokens.isEmpty())
        }

    @Test
    fun verifySavedSuccessDoesNotWriteConfigurationOrToken() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val config = seedSavedConnection(paths, storage)
            val tokensBefore = storage.savedTokens.toList()
            val repository =
                repository(
                    engine = verifyEngine(HttpStatusCode.OK, MYSELF_BODY),
                    paths = paths,
                    storage = storage,
                )

            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), repository.verifySaved())
            assertEquals(config, paths.storedConfig())
            assertEquals(tokensBefore, storage.savedTokens)
        }

    @Test
    fun verifySavedMapsTimezoneFromTheJiraProfile() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            seedSavedConnection(paths, storage)
            val repository =
                repository(
                    engine = verifyEngine(HttpStatusCode.OK, """{"timeZone":"Asia/Ho_Chi_Minh"}"""),
                    paths = paths,
                    storage = storage,
                )

            assertEquals(
                ConnectionStatus.Connected(baseUrl, email, jiraTimeZone = "Asia/Ho_Chi_Minh"),
                repository.verifySaved(),
            )
        }

    @Test
    fun verifySavedMapsAuthenticationFailureWithoutWriting() =
        runTest {
            assertSavedVerificationFailureMapsTo(
                status = HttpStatusCode.Unauthorized,
                expectedError = JiraError.AuthFailed,
            )
        }

    @Test
    fun verifySavedMapsPermissionFailureWithoutWriting() =
        runTest {
            assertSavedVerificationFailureMapsTo(
                status = HttpStatusCode.Forbidden,
                expectedError = JiraError.PermissionDenied,
            )
        }

    @Test
    fun verifySavedMapsNetworkAndTimeoutFailuresToUnverifiedWithoutWriting() =
        runTest {
            assertSavedVerificationFailureMapsToUnverified(
                engine = MockEngine { throw UnknownHostException(baseUrl) },
            )
            assertSavedVerificationFailureMapsToUnverified(
                engine = MockEngine { throw HttpRequestTimeoutException("het thoi gian cho") },
            )
        }

    @Test
    fun disconnectRemovesCredentialsAndPreservesPreferencesAndSavedTables() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val preferences =
                UserPreferences(
                    baseUrl = baseUrl,
                    email = email,
                    visibleColumns = setOf(ColumnKey.KEY, ColumnKey.STATUS),
                    lastJql = "project = TEST",
                    sort = SortState(column = ColumnKey.STATUS, ascending = false),
                )
            val configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
            configStore.save(preferences)
            storage.saveToken(token)
            val savedTables = """{"schemaVersion":1,"tables":[]}"""
            paths.seedRawSavedTables(savedTables)
            val repository = repository(engine = verifyEngine(HttpStatusCode.OK, MYSELF_BODY), paths, storage)

            val status = repository.disconnect()

            assertEquals(ConnectionStatus.Unconfigured, status)
            assertEquals(preferences.copy(baseUrl = null, email = null), configStore.load())
            assertNull(storage.loadToken())
            assertEquals(savedTables, paths.storedSavedTables())
            assertEquals(0, paths.savedTablesWrites)
            assertEquals(0, paths.savedTablesReads)
        }

    @Test
    fun disconnectIsIdempotentWhenNothingIsSaved() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val repository = repository(engine = verifyEngine(HttpStatusCode.OK, MYSELF_BODY), paths, storage)

            assertEquals(ConnectionStatus.Unconfigured, repository.disconnect())
            assertEquals(ConnectionStatus.Unconfigured, repository.disconnect())
            assertNull(storage.loadToken())
            assertEquals(null, AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).load().baseUrl)
            assertEquals(null, AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).load().email)
        }

    @Test
    fun disconnectDoesNotChangeConfigurationWhenTokenDeletionFails() =
        runTest {
            val paths = FakeAppPaths()
            val configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
            configStore.save(UserPreferences(baseUrl = baseUrl, email = email, lastJql = "project = TEST"))
            val configBefore = paths.storedConfig()
            val storage = FakeSecureStorage(failOnDelete = true)
            storage.saveToken(token)
            val repository = repository(engine = verifyEngine(HttpStatusCode.OK, MYSELF_BODY), paths, storage)

            assertFailsWith<SecureStorageException> { repository.disconnect() }

            assertEquals(configBefore, paths.storedConfig())
            assertEquals(token, storage.loadToken())
        }

    private suspend fun assertSavedVerificationFailureMapsTo(
        status: HttpStatusCode,
        expectedError: JiraError,
    ) {
        val paths = FakeAppPaths()
        val storage = FakeSecureStorage()
        val config = seedSavedConnection(paths, storage)
        val tokensBefore = storage.savedTokens.toList()
        val repository =
            repository(
                engine = verifyEngine(status, ERROR_BODY),
                paths = paths,
                storage = storage,
            )

        val result = repository.verifySaved()

        assertIs<ConnectionStatus.Invalid>(result)
        assertEquals(expectedError, result.error)
        assertEquals(config, paths.storedConfig())
        assertEquals(tokensBefore, storage.savedTokens)
    }

    private suspend fun assertSavedVerificationFailureMapsToUnverified(engine: MockEngine) {
        val paths = FakeAppPaths()
        val storage = FakeSecureStorage()
        val config = seedSavedConnection(paths, storage)
        val tokensBefore = storage.savedTokens.toList()
        val repository = repository(engine = engine, paths = paths, storage = storage)

        assertEquals(ConnectionStatus.Unverified(baseUrl = baseUrl, email = email), repository.verifySaved())
        assertEquals(config, paths.storedConfig())
        assertEquals(tokensBefore, storage.savedTokens)
    }

    private suspend fun seedSavedConnection(
        paths: FakeAppPaths,
        storage: FakeSecureStorage,
    ): String {
        val config = savedConfig()
        paths.seedRawConfig(config)
        storage.saveToken(token)
        return config
    }

    private fun savedConfig(): String =
        """{"schemaVersion":1,"baseUrl":"$baseUrl","email":"$email","lastJql":"project = TEST"}"""

    private fun verifyEngine(
        status: HttpStatusCode,
        body: String,
    ) = MockEngine { request ->
        assertEquals("/rest/api/3/myself", request.url.encodedPath)
        assertEquals(
            "Basic ${"$email:$token".encodeBase64()}",
            request.headers[HttpHeaders.Authorization],
        )
        respond(content = body, status = status, headers = JSON_HEADERS)
    }

    private companion object {
        const val MYSELF_BODY = """{"accountId":"abc","displayName":"Nguyen Van A","active":true}"""
        const val ERROR_BODY = """{"errorMessages":["request failed"],"errors":{}}"""
    }
}
