package com.jiratools.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Vị trí các file dữ liệu cục bộ (cấu hình và bảng đã lưu) theo chuẩn của từng hệ điều hành:
 * macOS `~/Library/Application Support`, Windows `%APPDATA%`, Linux `$XDG_CONFIG_HOME`.
 */
class JvmAppPaths(
    private val directory: File = defaultDirectory(),
) : AppPaths {
    override fun configFile(): String = File(directory, CONFIG_FILE_NAME).absolutePath

    override suspend fun readConfig(): String? =
        withContext(Dispatchers.IO) {
            val file = File(configFile())
            if (file.isFile) file.readText() else null
        }

    override suspend fun writeConfig(content: String) {
        withContext(Dispatchers.IO) {
            val target = File(configFile())
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, "$CONFIG_FILE_NAME.tmp")
            temporary.writeText(content)
            moveIntoPlace(temporary, target)
        }
    }

    override fun savedTablesFile(): String = File(directory, SAVED_TABLES_FILE_NAME).absolutePath

    override suspend fun readSavedTables(): String? =
        withContext(Dispatchers.IO) {
            val file = File(savedTablesFile())
            if (file.exists()) file.readText() else null
        }

    override suspend fun writeSavedTables(content: String) {
        withContext(Dispatchers.IO) {
            val target = File(savedTablesFile())
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, "$SAVED_TABLES_FILE_NAME.tmp")
            temporary.writeText(content)
            moveIntoPlace(temporary, target)
        }
    }

    /** Ghi nguyên tử để file cũ không bị hỏng khi thao tác dở dang. */
    private fun moveIntoPlace(
        temporary: File,
        target: File,
    ) {
        try {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (failure: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val CONFIG_FILE_NAME = "config.json"
        const val SAVED_TABLES_FILE_NAME = "saved-tables.json"

        fun defaultDirectory(): File {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val base =
                when {
                    os.contains("mac") || os.contains("darwin") -> File(home, "Library/Application Support")
                    os.contains("win") -> File(System.getenv("APPDATA") ?: home)
                    else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$home/.config")
                }
            return File(base, "jira-tools")
        }
    }
}
