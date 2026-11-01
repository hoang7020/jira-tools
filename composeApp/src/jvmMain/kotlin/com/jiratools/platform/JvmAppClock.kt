package com.jiratools.platform

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Cài đặt [AppClock] trên JVM: giờ hệ thống và định dạng `dd/MM/yyyy HH:mm` theo múi giờ của máy. */
class JvmAppClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun format(millis: Long): String {
        val moment = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return FORMATTER.format(moment)
    }

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }
}
