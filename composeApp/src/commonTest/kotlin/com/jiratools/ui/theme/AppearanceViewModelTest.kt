package com.jiratools.ui.theme

import com.jiratools.data.AppConfigStore
import com.jiratools.domain.ThemeMode
import com.jiratools.domain.UserPreferences
import com.jiratools.platform.FakeAppPaths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private fun viewModel(paths: FakeAppPaths) =
        AppearanceViewModel(
            configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
            dispatcher = UnconfinedTestDispatcher(),
        )

    @Test
    fun loadSavedDefaultsToSystemTheme() =
        runTest {
            val viewModel = viewModel(FakeAppPaths())

            viewModel.loadSaved()

            assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
            assertNull(viewModel.notice.value)
        }

    @Test
    fun loadSavedRestoresExplicitThemeChoice() =
        runTest {
            val paths = FakeAppPaths()
            val configStore = AppConfigStore(paths, UnconfinedTestDispatcher())
            configStore.save(UserPreferences(themeMode = ThemeMode.DARK))
            val viewModel = viewModel(paths)

            viewModel.loadSaved()

            assertEquals(ThemeMode.DARK, viewModel.themeMode.value)
        }

    @Test
    fun changingThemePersistsChoiceAndUpdatesState() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths)
            viewModel.loadSaved()

            viewModel.setThemeMode(ThemeMode.LIGHT)

            assertEquals(ThemeMode.LIGHT, viewModel.themeMode.value)
            assertEquals(ThemeMode.LIGHT, AppConfigStore(paths, UnconfinedTestDispatcher()).load().themeMode)
            assertNull(viewModel.notice.value)
        }

    @Test
    fun failedThemeSaveKeepsCurrentChoiceAndSurfacesError() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths)
            viewModel.loadSaved()
            paths.failOnWrite = true

            viewModel.setThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
            assertNotNull(viewModel.notice.value)
        }
}
