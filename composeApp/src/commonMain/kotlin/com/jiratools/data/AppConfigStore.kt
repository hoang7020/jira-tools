package com.jiratools.data

import com.jiratools.domain.CURRENT_SCHEMA_VERSION
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.UserPreferences
import com.jiratools.platform.AppPaths
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Đọc/ghi tuỳ chọn người dùng trong file JSON (FR-010).
 *
 * File hỏng, sai kiểu dữ liệu hoặc khác [CURRENT_SCHEMA_VERSION] đều được coi như chưa có cấu
 * hình: trả về giá trị mặc định thay vì làm ứng dụng dừng. Lỗi ghi được báo ngược ra ngoài để
 * UI thông báo cho người dùng.
 */
class AppConfigStore(
    private val paths: AppPaths,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    suspend fun load(): UserPreferences =
        withContext(dispatcher) {
            val raw = runCatching { paths.readConfig() }.getOrNull()
            val preferences = raw?.let { decode(it) }
            when {
                preferences == null -> UserPreferences()
                preferences.schemaVersion == 1 ->
                    migrateSchema3(
                        preferences.copy(
                            schemaVersion = 3,
                            visibleColumns = preferences.visibleColumns + ColumnKey.ORIGINAL_EXPECTED_TIME,
                        ),
                    )
                preferences.schemaVersion == 2 ->
                    migrateSchema3(preferences.copy(schemaVersion = 3))
                preferences.schemaVersion == 3 -> migrateSchema3(preferences)
                preferences.schemaVersion == CURRENT_SCHEMA_VERSION -> preferences
                else -> UserPreferences()
            }
        }

    suspend fun save(preferences: UserPreferences) {
        withContext(dispatcher) {
            paths.writeConfig(json.encodeToString(preferences))
        }
    }

    private fun decode(raw: String): UserPreferences? {
        val parsed = runCatching { json.decodeFromString<UserPreferences>(raw) }.getOrNull()
        return parsed
    }

    private fun migrateSchema3(preferences: UserPreferences): UserPreferences =
        preferences.copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            visibleColumns =
                if (preferences.visibleColumns == ColumnKey.schemaV3QueryColumns()) {
                    preferences.visibleColumns + ColumnKey.DUE_DATE
                } else {
                    preferences.visibleColumns
                },
        )
}
