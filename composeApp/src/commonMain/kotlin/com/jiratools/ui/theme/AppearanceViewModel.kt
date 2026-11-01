package com.jiratools.ui.theme

import com.jiratools.data.AppConfigStore
import com.jiratools.domain.ThemeMode
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Holds the saved appearance preference outside Compose UI state. */
class AppearanceViewModel(
    private val configStore: AppConfigStore,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val mutableThemeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val mutableNotice = MutableStateFlow<String?>(null)

    val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()
    val notice: StateFlow<String?> = mutableNotice.asStateFlow()

    fun clearNotice() {
        mutableNotice.value = null
    }

    suspend fun loadSaved() {
        mutableThemeMode.value = withContext(dispatcher) { configStore.load().themeMode }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        if (themeMode == mutableThemeMode.value) return

        try {
            withContext(dispatcher) {
                val preferences = configStore.load()
                configStore.save(preferences.copy(themeMode = themeMode))
            }
            mutableThemeMode.value = themeMode
            mutableNotice.value = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            mutableNotice.value = failure.message ?: "Không thể lưu chế độ giao diện."
        }
    }
}
