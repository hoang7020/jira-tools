package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Kiểm thử việc dẫn xuất địa chỉ mở ticket trong Jira (FR-013, T050). */
class BrowseUrlTest {
    @Test
    fun ghepDungDuongDanBrowse() {
        assertEquals(
            "https://acme.atlassian.net/browse/HSP-1",
            browseUrl(baseUrl = "https://acme.atlassian.net", key = "HSP-1"),
        )
    }

    @Test
    fun khongNhanDoiDauGachCheoKhiBaseUrlDaCoDauCuoi() {
        assertEquals(
            "https://acme.atlassian.net/browse/HSP-2",
            browseUrl(baseUrl = "https://acme.atlassian.net/", key = "HSP-2"),
        )
    }

    @Test
    fun khongNhanDoiDauGachCheoKhiBaseUrlThieuDauCuoi() {
        assertEquals(
            "https://acme.atlassian.net/browse/HSP-3",
            browseUrl(baseUrl = "https://acme.atlassian.net", key = "HSP-3"),
        )
    }

    @Test
    fun diaChiThieuSchemeVanRaUrlHopLe() {
        assertEquals(
            "https://acme.atlassian.net/browse/HSP-4",
            browseUrl(baseUrl = "acme.atlassian.net", key = "HSP-4"),
        )
    }

    @Test
    fun tonTrongHttpNeuNguoiDungChuDongNhap() {
        assertEquals(
            "http://jira.example.com:8080/browse/HSP-5",
            browseUrl(baseUrl = "http://jira.example.com:8080", key = "HSP-5"),
        )
    }

    @Test
    fun catKhoangTrangThuaQuanhDiaChiVaMaTicket() {
        assertEquals(
            "https://acme.atlassian.net/browse/HSP-6",
            browseUrl(baseUrl = "  https://acme.atlassian.net/  ", key = "  HSP-6  "),
        )
    }

    @Test
    fun giuNguyenChuHoaChuThuongCuaMaTicket() {
        assertEquals(
            "https://acme.atlassian.net/browse/hsp-7",
            browseUrl(baseUrl = "https://acme.atlassian.net", key = "hsp-7"),
        )
    }

    @Test
    fun traVeNullKhiChuaCoDiaChi() {
        assertNull(browseUrl(baseUrl = "", key = "HSP-8"))
        assertNull(browseUrl(baseUrl = "   ", key = "HSP-8"))
    }

    @Test
    fun traVeNullKhiMaTicketRong() {
        assertNull(browseUrl(baseUrl = "https://acme.atlassian.net", key = ""))
    }

    @Test
    fun traVeNullKhiDiaChiKhongHopLe() {
        assertNull(browseUrl(baseUrl = "ftp://acme.atlassian.net", key = "HSP-9"))
    }
}
