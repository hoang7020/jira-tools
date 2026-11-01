package com.jiratools.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Kho bí mật giả lập trong bộ nhớ; mỗi thao tác có thể được cấu hình để thất bại. */
class FakeSecureStorage(
    private val failOnLoad: Boolean = false,
    private val failOnSave: Boolean = false,
    private val failOnDelete: Boolean = false,
) : SecureStorage {
    private var token: String? = null
    val savedTokens: MutableList<String> = mutableListOf()

    override suspend fun saveToken(token: String) {
        if (failOnSave) throw SecureStorageException("Kho bí mật của hệ điều hành không khả dụng")
        this.token = token
        savedTokens += token
    }

    override suspend fun loadToken(): String? {
        if (failOnLoad) throw SecureStorageException("Kho bí mật của hệ điều hành không khả dụng")
        return token
    }

    override suspend fun deleteToken() {
        if (failOnDelete) throw SecureStorageException("Kho bí mật của hệ điều hành không khả dụng")
        token = null
    }
}

/** Vị trí file cấu hình giả lập, dữ liệu được giữ trong bộ nhớ. */
class FakeAppPaths(
    private val directory: String = "/tmp/jira-tools-test",
) : AppPaths {
    private val files: MutableMap<String, String> = mutableMapOf()
    var failOnRead: Boolean = false
    var failOnWrite: Boolean = false
    var failOnReadSavedTables: Boolean = false
    var failOnWriteSavedTables: Boolean = false

    /** Số lần file danh mục bảng đã lưu được ghi, để kiểm chứng "ghi đúng một lần" mỗi thao tác. */
    var savedTablesWrites: Int = 0

    /** Số lần file danh mục được đọc, để kiểm chứng thao tác không đọc lại đĩa (FR-006). */
    var savedTablesReads: Int = 0

    override fun configFile(): String = "$directory/config.json"

    override suspend fun readConfig(): String? {
        if (failOnRead) throw IllegalStateException("không đọc được file cấu hình")
        return files[configFile()]
    }

    override suspend fun writeConfig(content: String) {
        if (failOnWrite) throw IllegalStateException("không ghi được file cấu hình")
        files[configFile()] = content
    }

    override fun savedTablesFile(): String = "$directory/saved-tables.json"

    override suspend fun readSavedTables(): String? {
        if (failOnReadSavedTables) throw IllegalStateException("không đọc được file bảng đã lưu")
        savedTablesReads += 1
        return files[savedTablesFile()]
    }

    override suspend fun writeSavedTables(content: String) {
        if (failOnWriteSavedTables) throw IllegalStateException("không ghi được file bảng đã lưu")
        savedTablesWrites += 1
        files[savedTablesFile()] = content
    }

    /** Ghi thẳng nội dung thô vào file cấu hình để mô phỏng file hỏng. */
    fun seedRawConfig(content: String?) {
        if (content == null) files.remove(configFile()) else files[configFile()] = content
    }

    fun storedConfig(): String? = files[configFile()]

    /** Ghi thẳng nội dung thô vào file bảng đã lưu để mô phỏng file hỏng hoặc file của bản cũ. */
    fun seedRawSavedTables(content: String?) {
        if (content == null) files.remove(savedTablesFile()) else files[savedTablesFile()] = content
    }

    fun storedSavedTables(): String? = files[savedTablesFile()]
}

/** Đồng hồ giả lập trả giá trị cố định để kiểm thử thời điểm lưu/làm mới (Nguyên tắc II). */
class FakeClock(
    var millis: Long = 1_700_000_000_000,
    var formatted: String = "14/11/2023 22:13",
) : AppClock {
    override fun nowMillis(): Long = millis

    override fun format(millis: Long): String = formatted
}

/** Trình mở URL giả lập: ghi lại mọi lần gọi thay vì mở trình duyệt thật. */
class RecordingUrlOpener(
    private val alwaysFail: Boolean = false,
) : UrlOpener {
    val openedUrls: MutableList<String> = mutableListOf()

    override fun open(url: String): Boolean {
        openedUrls += url
        return !alwaysFail
    }
}

/** Trình mở URL giả lập luôn ném ngoại lệ để kiểm tra việc bắt lỗi ở tầng gọi. */
class ThrowingUrlOpener : UrlOpener {
    override fun open(url: String): Boolean = throw IllegalStateException("không mở được trình duyệt: $url")
}

/** Cung cấp dispatcher xác định cho ViewModel trong kiểm thử (Nguyên tắc II). */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
)
