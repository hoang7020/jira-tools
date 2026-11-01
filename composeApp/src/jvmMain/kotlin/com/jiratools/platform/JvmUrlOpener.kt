package com.jiratools.platform

import java.awt.Desktop
import java.net.URI

/** Mở ticket trong trình duyệt mặc định của hệ điều hành, không bao giờ ném ngoại lệ. */
class JvmUrlOpener : UrlOpener {
    override fun open(url: String): Boolean =
        try {
            val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
                true
            } else {
                false
            }
        } catch (failure: Exception) {
            false
        }
}
