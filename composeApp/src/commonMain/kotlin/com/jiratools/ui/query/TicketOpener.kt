package com.jiratools.ui.query

import com.jiratools.domain.Ticket
import com.jiratools.domain.browseUrl
import com.jiratools.platform.UrlOpener
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mở ticket trong trình duyệt mặc định (FR-013).
 *
 * Địa chỉ luôn được dẫn xuất từ địa chỉ Jira đã lưu và mã ticket, không nhận URL tự do từ người
 * dùng. Mọi lỗi từ [UrlOpener] đều được chuyển thành thông báo trả về cho giao diện: ứng dụng
 * không sập và bảng kết quả vẫn giữ nguyên.
 */
class TicketOpener(
    private val urlOpener: UrlOpener,
    private val baseUrl: suspend () -> String?,
) {
    /** @return `null` nếu đã mở thành công, ngược lại là thông báo lỗi cho người dùng. */
    suspend fun open(ticket: Ticket): String? {
        val url = browseUrl(baseUrl = baseUrl().orEmpty(), key = ticket.key)
        if (url == null) return NO_ADDRESS_MESSAGE
        return try {
            if (urlOpener.open(url)) null else cannotOpenMessage(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            cannotOpenMessage(url)
        }
    }

    private fun cannotOpenMessage(url: String): String = "Không mở được trình duyệt. Bạn có thể mở thủ công: $url"

    private companion object {
        const val NO_ADDRESS_MESSAGE = "Chưa có địa chỉ Jira hợp lệ. Vui lòng kiểm tra lại kết nối."
    }
}
