package com.jiratools.data

import com.jiratools.domain.CURRENT_SCHEMA_VERSION
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.SortState
import com.jiratools.domain.ThemeMode
import com.jiratools.domain.UserPreferences
import com.jiratools.platform.FakeAppPaths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigStoreTest {
    private fun store(paths: FakeAppPaths) =
        AppConfigStore(
            paths = paths,
            dispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun luuVaDocLaiGiuNguyenDuLieu() =
        runTest {
            val paths = FakeAppPaths()
            val store = store(paths)
            val preferences =
                UserPreferences(
                    baseUrl = "https://congty.atlassian.net",
                    email = "dev@congty.vn",
                    visibleColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.STATUS),
                    lastJql = "project = ABC ORDER BY created DESC",
                    sort = SortState(column = ColumnKey.STATUS, ascending = false),
                    themeMode = ThemeMode.DARK,
                )

            store.save(preferences)
            val loaded = store.load()

            assertEquals(preferences, loaded)
        }

    @Test
    fun fileKhongTonTaiTraVeMacDinh() =
        runTest {
            val store = store(FakeAppPaths())

            val loaded = store.load()

            assertEquals(UserPreferences(), loaded)
            assertNull(loaded.baseUrl)
            assertNull(loaded.email)
            assertNull(loaded.lastJql)
            assertEquals(ColumnKey.all(), loaded.visibleColumns)
            assertEquals(CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
        }

    @Test
    fun jsonHongTraVeMacDinhVaKhongNemNgoaiLe() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("{ day khong phai json hop le ")

            val loaded = store(paths).load()

            assertEquals(UserPreferences(), loaded)
        }

    @Test
    fun jsonDungCuPhapNhungSaiKieuTraVeMacDinh() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("""{"schemaVersion": "mot", "baseUrl": 5}""")

            val loaded = store(paths).load()

            assertEquals(UserPreferences(), loaded)
        }

    @Test
    fun schemaVersionKhacTraVeMacDinh() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("""{"schemaVersion": 99, "baseUrl": "https://cu.atlassian.net"}""")

            val loaded = store(paths).load()

            assertEquals(UserPreferences(), loaded)
            assertNull(loaded.baseUrl)
        }

    @Test
    fun luuKhongDungFileCauHinhCuKhiGhiThatBai() =
        runTest {
            val paths = FakeAppPaths()
            val store = store(paths)
            store.save(UserPreferences(baseUrl = "https://giu-lai.atlassian.net"))
            val truocKhiLoi = paths.storedConfig()

            paths.failOnWrite = true
            val ketQua = runCatching { store.save(UserPreferences(baseUrl = "https://moi.atlassian.net")) }

            assertTrue(ketQua.isFailure, "lỗi ghi phải được báo ra ngoài để UI hiển thị")
            assertEquals(truocKhiLoi, paths.storedConfig())
        }

    @Test
    fun jsonLuuRaDocDuocBangBoDocKhac() =
        runTest {
            val paths = FakeAppPaths()
            store(paths).save(UserPreferences(email = "dev@congty.vn"))

            val raw = paths.storedConfig()

            assertTrue(raw != null && raw.contains("dev@congty.vn"))
            assertTrue(raw.contains("schemaVersion"))
        }

    @Test
    fun migratesSchemaV1AndKeepsExistingPreferences() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """
                        {
                          "schemaVersion": 1,
                          "baseUrl": "https://legacy.atlassian.net",
                          "email": "legacy@example.com",
                          "visibleColumns": ["KEY", "SUMMARY"],
                          "lastJql": "project = OLD",
                          "sort": {"column": "STATUS", "ascending": false}
                        }
                        """.trimIndent(),
                    )
                }
            val originalJson = paths.storedConfig()

            val loaded = store(paths).load()

            assertEquals(CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
            assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
            assertEquals("https://legacy.atlassian.net", loaded.baseUrl)
            assertEquals("legacy@example.com", loaded.email)
            assertEquals("project = OLD", loaded.lastJql)
            assertEquals(SortState(ColumnKey.STATUS, ascending = false), loaded.sort)
            assertContains(loaded.visibleColumns.map { it.name }, "KEY")
            assertContains(loaded.visibleColumns.map { it.name }, "SUMMARY")
            assertContains(loaded.visibleColumns.map { it.name }, "ORIGINAL_EXPECTED_TIME")
            assertTrue(loaded.visibleColumns.none { it.name.startsWith("DYNAMIC_") })
            assertEquals(originalJson, paths.storedConfig(), "Migration khi load không được ghi file")
        }

    @Test
    fun migratesSchemaV2ToCurrentVersionWithSystemThemeAndPreservesPreferences() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """
                        {
                          "schemaVersion": 2,
                          "baseUrl": "https://v2.atlassian.net",
                          "email": "v2@example.com",
                          "visibleColumns": ["KEY", "SUMMARY"],
                          "lastJql": "project = V2",
                          "sort": {"column": "SUMMARY", "ascending": true}
                        }
                        """.trimIndent(),
                    )
                }

            val loaded = store(paths).load()

            assertEquals(CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
            assertEquals(ThemeMode.SYSTEM, loaded.themeMode)
            assertEquals("https://v2.atlassian.net", loaded.baseUrl)
            assertEquals("v2@example.com", loaded.email)
            assertEquals("project = V2", loaded.lastJql)
            assertEquals(SortState(ColumnKey.SUMMARY, ascending = true), loaded.sort)
            assertEquals(setOf(ColumnKey.KEY, ColumnKey.SUMMARY), loaded.visibleColumns)
        }

    @Test
    fun readsLegacyConfigWithoutThemeModeAsSystemAndPersistsExplicitThemeChoice() =
        runTest {
            val paths = FakeAppPaths()
            paths.seedRawConfig("""{"schemaVersion": 3, "baseUrl": "https://example.atlassian.net"}""")
            val store = store(paths)

            assertEquals(ThemeMode.SYSTEM, store.load().themeMode)

            store.save(store.load().copy(themeMode = ThemeMode.LIGHT))

            assertEquals(ThemeMode.LIGHT, store.load().themeMode)
        }

    @Test
    fun migratesSchemaV3DefaultColumnsToIncludeDueDate() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """
                        {
                          "schemaVersion": 3,
                          "visibleColumns": ["KEY", "SUMMARY", "STATUS", "ASSIGNEE", "PRIORITY", "UPDATED", "ORIGINAL_EXPECTED_TIME"]
                        }
                        """.trimIndent(),
                    )
                }

            val loaded = store(paths).load()

            assertEquals(CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
            assertTrue("DUE_DATE" in loaded.visibleColumns.map { it.name })
        }

    @Test
    fun migratesSchemaV3CustomizedColumnsWithoutChangingVisibility() =
        runTest {
            val customColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.STATUS)
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """
                        {
                          "schemaVersion": 3,
                          "visibleColumns": ["KEY", "SUMMARY", "STATUS"]
                        }
                        """.trimIndent(),
                    )
                }

            val loaded = store(paths).load()

            assertEquals(CURRENT_SCHEMA_VERSION, loaded.schemaVersion)
            assertEquals(customColumns, loaded.visibleColumns)
        }

    @Test
    fun defaultPreferencesIncludeDueDate() {
        assertTrue("DUE_DATE" in UserPreferences().visibleColumns.map { it.name })
    }
}
