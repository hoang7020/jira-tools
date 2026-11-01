package com.jiratools.platform

/**
 * Mở URL bằng trình duyệt mặc định của hệ điều hành.
 *
 * Không bao giờ ném ngoại lệ ra ngoài: kết quả thất bại được trả về bằng `false` để UI hiển
 * thị thông báo không chặn luồng chính (FR-013).
 */
interface UrlOpener {
    /** @return true nếu trình duyệt đã được mở. */
    fun open(url: String): Boolean
}
