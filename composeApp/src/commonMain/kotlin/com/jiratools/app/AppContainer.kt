package com.jiratools.app

import com.jiratools.data.AppConfigStore
import com.jiratools.data.ConnectionRepository
import com.jiratools.data.HttpClientFactory
import com.jiratools.data.JiraApiClient
import com.jiratools.data.JiraCredentials
import com.jiratools.data.SavedTableStore
import com.jiratools.platform.AppClock
import com.jiratools.platform.AppPaths
import com.jiratools.platform.SecureStorage
import com.jiratools.platform.UrlOpener
import com.jiratools.ui.query.QueryViewModel
import com.jiratools.ui.query.TicketOpener
import com.jiratools.ui.saved.SavedTablesViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Lắp ghép thủ công mọi phụ thuộc của ứng dụng (Nguyên tắc V - không dùng framework DI).
 *
 * `expect/actual` chỉ được phép xuất hiện ở tầng `platform`, nên container được tạo ở `jvmMain`
 * và truyền vào giao diện dưới dạng tham số.
 */
class AppContainer(
    val appPaths: AppPaths,
    val secureStorage: SecureStorage,
    val urlOpener: UrlOpener,
    val appClock: AppClock,
    val ioDispatcher: CoroutineDispatcher = com.jiratools.platform.ioDispatcher,
    val httpClient: HttpClient = HttpClientFactory.create(),
) {
    val configStore: AppConfigStore = AppConfigStore(paths = appPaths, dispatcher = ioDispatcher)

    /**
     * Thông tin đăng nhập được đọc lại mỗi lần gọi để phản ánh cấu hình mới nhất; token chỉ tồn
     * tại trong bộ nhớ tại thời điểm gọi (FR-003).
     */
    private val credentialsProvider: suspend () -> JiraCredentials? = {
        val preferences = configStore.load()
        val baseUrl = preferences.baseUrl?.trim().orEmpty()
        val email = preferences.email?.trim().orEmpty()
        val token = secureStorage.loadToken()?.trim().orEmpty()
        if (baseUrl.isEmpty() || email.isEmpty() || token.isEmpty()) {
            null
        } else {
            JiraCredentials(baseUrl = baseUrl, email = email, token = token)
        }
    }

    val jiraApiClient: JiraApiClient =
        JiraApiClient(client = httpClient, credentials = credentialsProvider)

    val connectionRepository: ConnectionRepository =
        ConnectionRepository(
            apiClient = jiraApiClient,
            configStore = configStore,
            secureStorage = secureStorage,
            dispatcher = ioDispatcher,
        )

    val ticketOpener: TicketOpener =
        TicketOpener(urlOpener = urlOpener, baseUrl = { configStore.load().baseUrl })

    val queryViewModel: QueryViewModel =
        QueryViewModel(
            apiClient = jiraApiClient,
            configStore = configStore,
            dispatcher = ioDispatcher,
        )

    val savedTableStore: SavedTableStore = SavedTableStore(paths = appPaths, dispatcher = ioDispatcher)

    val savedTablesViewModel: SavedTablesViewModel =
        SavedTablesViewModel(
            store = savedTableStore,
            clock = appClock,
            urlOpener = urlOpener,
            apiClient = jiraApiClient,
            dispatcher = ioDispatcher,
        )
}
