package com.jiratools.platform

/**
 * Vị trí và truy cập file dữ liệu cục bộ trên từng nền tảng.
 *
 * Đây là ranh giới duy nhất giữa `commonMain` và hệ thống tệp của nền tảng, nhờ đó
 * [com.jiratools.data.AppConfigStore] và [com.jiratools.data.SavedTableStore] giữ toàn bộ logic
 * đọc/ghi JSON trong mã chung.
 */
interface AppPaths {
    /** Đường dẫn tuyệt đối tới file cấu hình JSON. */
    fun configFile(): String

    /**
     * Đọc nội dung file cấu hình; trả về null khi file chưa tồn tại.
     *
     * Triển khai phải ném ngoại lệ khi file có nhưng không đọc được.
     */
    suspend fun readConfig(): String?

    /**
     * Ghi nội dung file cấu hình.
     *
     * Triển khai phải tạo thư mục cha nếu cần và ghi theo cách nguyên tử (ghi file tạm rồi
     * thay thế) để file cấu hình cũ không bị hỏng khi thao tác thất bại.
     */
    suspend fun writeConfig(content: String)

    /** Đường dẫn tuyệt đối tới file danh mục bảng đã lưu, cùng thư mục với [configFile]. */
    fun savedTablesFile(): String

    /**
     * Đọc nội dung file danh mục bảng đã lưu.
     *
     * @return null khi file chưa tồn tại; ném ngoại lệ khi file có nhưng không đọc được, để tầng
     * trên phân biệt "chưa có dữ liệu" với "dữ liệu hỏng".
     */
    suspend fun readSavedTables(): String?

    /**
     * Ghi nội dung file danh mục bảng đã lưu.
     *
     * Triển khai phải tạo thư mục cha nếu cần, ghi nguyên tử và ném ngoại lệ khi ghi thất bại để
     * giao diện báo cho người dùng.
     */
    suspend fun writeSavedTables(content: String)
}
