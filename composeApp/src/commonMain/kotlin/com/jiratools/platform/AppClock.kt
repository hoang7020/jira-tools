package com.jiratools.platform

/**
 * Nguồn thời gian duy nhất của ứng dụng (Nguyên tắc I).
 *
 * Phần dùng chung chỉ biết interface này; bản cài đặt thật nằm ở `jvmMain` ([JvmAppClock] bên phía
 * JVM), còn kiểm thử dùng `FakeClock`. Nhờ đó thời điểm lưu và làm mới bảng là dữ liệu xác định khi
 * kiểm thử, và `commonMain` không phải phụ thuộc API thời gian của hệ điều hành.
 */
interface AppClock {
    /** Thời điểm hiện tại, tính bằng mili giây kể từ mốc epoch. */
    fun nowMillis(): Long

    /** Định dạng thời điểm để hiển thị cho người dùng; nơi duy nhất định dạng thời gian của ứng dụng. */
    fun format(millis: Long): String
}
