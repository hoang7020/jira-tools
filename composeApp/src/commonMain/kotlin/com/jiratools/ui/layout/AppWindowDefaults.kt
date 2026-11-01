package com.jiratools.ui.layout

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Hằng số thiết kế cho cửa sổ ứng dụng (FR-012).
 *
 * Trong Compose Desktop, 1 dp trùng với 1 đơn vị hình học cửa sổ AWT (1 điểm ảnh logic), nên các giá
 * trị ở đây dùng trực tiếp được cho `window.minimumSize` mà không cần quy đổi theo mật độ màn hình
 * (xem mục 1 của `research.md`).
 */
object AppWindowDefaults {
    /** Kích thước tối thiểu, bảo đảm ô nhập JQL, nút chạy truy vấn và bảng kết quả còn dùng được (FR-012). */
    val MIN_SIZE: DpSize = DpSize(1024.dp, 720.dp)

    /** Kích thước ban đầu khi người dùng bỏ mở rộng; không ghi nhớ giữa các lần chạy. */
    val RESTORE_SIZE: DpSize = DpSize(1280.dp, 800.dp)
}
