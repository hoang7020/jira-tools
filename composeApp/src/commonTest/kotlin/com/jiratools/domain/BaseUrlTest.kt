package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BaseUrlTest {
    @Test
    fun themHttpsKhiThieuScheme() {
        val result = normalizeBaseUrl("congty.atlassian.net")

        assertEquals("https://congty.atlassian.net", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun catKhoangTrangHaiDau() {
        val result = normalizeBaseUrl("   https://congty.atlassian.net   ")

        assertEquals("https://congty.atlassian.net", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun boDauGachCheoCuoi() {
        val result = normalizeBaseUrl("https://congty.atlassian.net//")

        assertEquals("https://congty.atlassian.net", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun giuNguyenDiaChiDaHopLe() {
        val result = normalizeBaseUrl("https://congty.atlassian.net")

        assertEquals("https://congty.atlassian.net", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun giuHttpNeuNguoiDungChuDongNhap() {
        val result = normalizeBaseUrl("http://jira.noibo.local")

        assertEquals("http://jira.noibo.local", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun boDuongDanVaTruyVanThua() {
        val result = normalizeBaseUrl("https://congty.atlassian.net/jira/software?x=1#top")

        assertEquals("https://congty.atlassian.net", assertIs<BaseUrlResult.Valid>(result).baseUrl)
    }

    @Test
    fun chuoiRongTraVeLoiKhongNemNgoaiLe() {
        assertIs<BaseUrlResult.Invalid>(normalizeBaseUrl("   "))
    }

    @Test
    fun chuoiKhongPhaiDiaChiTraVeLoi() {
        assertIs<BaseUrlResult.Invalid>(normalizeBaseUrl("khong-phai-dia-chi"))
    }

    @Test
    fun schemeKhongHoTroTraVeLoi() {
        assertIs<BaseUrlResult.Invalid>(normalizeBaseUrl("ftp://congty.atlassian.net"))
    }
}
