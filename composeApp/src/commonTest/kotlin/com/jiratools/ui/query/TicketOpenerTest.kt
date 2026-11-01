package com.jiratools.ui.query

import com.jiratools.domain.Ticket
import com.jiratools.platform.RecordingUrlOpener
import com.jiratools.platform.ThrowingUrlOpener
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Kiểm thử lớp mở ticket: luôn trả thông báo thay vì ném ngoại lệ (FR-013, T051). */
class TicketOpenerTest {
    private val baseUrl = "https://acme.atlassian.net"

    @Test
    fun goiUrlOpenerVoiDungDiaChiDanXuatTuTicket() =
        runTest {
            val opener = RecordingUrlOpener()
            val ticketOpener = TicketOpener(urlOpener = opener, baseUrl = { baseUrl })

            val message = ticketOpener.open(ticket(key = "HSP-7"))

            assertNull(message)
            assertEquals(listOf("https://acme.atlassian.net/browse/HSP-7"), opener.openedUrls)
        }

    @Test
    fun dungDiaChiDaDucBoKhoangTrang() =
        runTest {
            val opener = RecordingUrlOpener()
            val ticketOpener = TicketOpener(urlOpener = opener, baseUrl = { "  $baseUrl/  " })

            ticketOpener.open(ticket(key = "HSP-8"))

            assertEquals(listOf("https://acme.atlassian.net/browse/HSP-8"), opener.openedUrls)
        }

    @Test
    fun traThongBaoKhiTrinhDuyetKhongMoDuoc() =
        runTest {
            val ticketOpener = TicketOpener(urlOpener = RecordingUrlOpener(alwaysFail = true), baseUrl = { baseUrl })

            val message = ticketOpener.open(ticket(key = "HSP-9"))

            assertNotNull(message)
            assertTrue(message.isNotBlank())
        }

    @Test
    fun traThongBaoKhiUrlOpenerNemNgoaiLe() =
        runTest {
            val ticketOpener = TicketOpener(urlOpener = ThrowingUrlOpener(), baseUrl = { baseUrl })

            val message = ticketOpener.open(ticket(key = "HSP-10"))

            assertNotNull(message)
            assertTrue(message.isNotBlank())
        }

    @Test
    fun traThongBaoKhiChuaCoDiaChiJira() =
        runTest {
            val opener = RecordingUrlOpener()
            val ticketOpener = TicketOpener(urlOpener = opener, baseUrl = { null })

            val message = ticketOpener.open(ticket(key = "HSP-11"))

            assertNotNull(message)
            assertTrue(opener.openedUrls.isEmpty())
        }

    @Test
    fun traThongBaoKhiDiaChiJiraRong() =
        runTest {
            val opener = RecordingUrlOpener()
            val ticketOpener = TicketOpener(urlOpener = opener, baseUrl = { "   " })

            val message = ticketOpener.open(ticket(key = "HSP-12"))

            assertNotNull(message)
            assertTrue(opener.openedUrls.isEmpty())
        }

    @Test
    fun baoGomDiaChiTrongThongBaoLoiDeNguoiDungMoThuCong() =
        runTest {
            val ticketOpener = TicketOpener(urlOpener = RecordingUrlOpener(alwaysFail = true), baseUrl = { baseUrl })

            val message = ticketOpener.open(ticket(key = "HSP-13"))

            assertNotNull(message)
            assertTrue(message.contains("https://acme.atlassian.net/browse/HSP-13"))
        }

    private fun ticket(key: String): Ticket =
        Ticket(
            key = key,
            summary = "Tiêu đề $key",
            status = "To Do",
            assignee = null,
            priority = null,
            updated = "01/01/2025 00:00",
        )
}
