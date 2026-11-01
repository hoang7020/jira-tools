package com.jiratools.data

import com.jiratools.domain.CatalogLoadResult
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.SAVED_TABLES_SCHEMA_VERSION
import com.jiratools.domain.SavedTableCatalog
import com.jiratools.platform.AppPaths
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * Đọc/ghi danh mục bảng đã lưu trong file JSON cục bộ (FR-004, FR-020).
 *
 * Ba kết quả đọc được phân biệt rõ: [CatalogLoadResult.Empty] khi chưa có file (trạng thái bình
 * thường của người dùng mới), [CatalogLoadResult.Loaded] khi đọc được, và
 * [CatalogLoadResult.Unreadable] khi file hỏng hoặc khác phiên bản lược đồ. Ở trạng thái không đọc
 * được, kho **không** xoá và **không** ghi đè file: tầng trên chặn mọi thao tác ghi cho tới khi
 * người dùng xử lý xong. Lỗi ghi được báo ngược ra ngoài để giao diện thông báo (FR-015).
 */
class SavedTableStore(
    private val paths: AppPaths,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    suspend fun load(): CatalogLoadResult =
        withContext(dispatcher) {
            val raw =
                try {
                    paths.readSavedTables()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    return@withContext CatalogLoadResult.Unreadable(failure.message ?: READ_FAILURE_DETAIL)
                }
            raw?.let { decode(it) } ?: CatalogLoadResult.Empty
        }

    suspend fun save(catalog: SavedTableCatalog) {
        withContext(dispatcher) {
            paths.writeSavedTables(json.encodeToString(catalog))
        }
    }

    /** Đường dẫn file danh mục, dùng cho cảnh báo kèm vị trí file (FR-020). */
    fun filePath(): String = paths.savedTablesFile()

    private fun decode(raw: String): CatalogLoadResult {
        val catalog = runCatching { json.decodeFromString<SavedTableCatalog>(raw) }.getOrNull()
        if (catalog == null) return CatalogLoadResult.Unreadable(INVALID_JSON_DETAIL)
        return when (catalog.schemaVersion) {
            1 ->
                CatalogLoadResult.Loaded(
                    catalog.copy(
                        schemaVersion = SAVED_TABLES_SCHEMA_VERSION,
                        tables =
                            catalog.tables.map { table ->
                                table.copy(visibleColumns = table.visibleColumns + ColumnKey.schemaV3SavedColumns())
                            },
                        dynamicStartTimes = emptyList(),
                    ),
                )
            2 ->
                CatalogLoadResult.Loaded(
                    catalog.copy(
                        schemaVersion = SAVED_TABLES_SCHEMA_VERSION,
                        tables =
                            catalog.tables.map { table ->
                                table.copy(visibleColumns = table.visibleColumns + ColumnKey.REAL_DUE_DATE)
                            },
                    ),
                )
            3, 4 -> CatalogLoadResult.Loaded(catalog.copy(schemaVersion = SAVED_TABLES_SCHEMA_VERSION))
            SAVED_TABLES_SCHEMA_VERSION -> CatalogLoadResult.Loaded(catalog)
            else -> CatalogLoadResult.Unreadable(unsupportedSchemaDetail(catalog.schemaVersion))
        }
    }

    private fun unsupportedSchemaDetail(version: Int): String =
        "Phiên bản lược đồ không được hỗ trợ (file: $version, hiện tại: $SAVED_TABLES_SCHEMA_VERSION)."

    private companion object {
        const val INVALID_JSON_DETAIL = "File danh mục bảng đã lưu không đọc được."
        const val READ_FAILURE_DETAIL = "Không đọc được file danh mục bảng đã lưu."
    }
}
