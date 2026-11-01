package com.jiratools.domain

import kotlin.math.roundToLong

/** Duration from Jira Original Estimate, normalized to nanoseconds. */
@JvmInline
value class WorkDuration private constructor(
    val nanoseconds: Long,
) : Comparable<WorkDuration> {
    override fun compareTo(other: WorkDuration): Int = nanoseconds.compareTo(other.nanoseconds)

    companion object {
        private val componentSeparator = Regex("\\s+")

        /** Parses a complete Jira estimate such as `1d 2h`; unsupported input returns null. */
        fun parse(raw: String): WorkDuration? {
            val normalized = raw.trim()
            if (normalized == "0") return WorkDuration(0)
            val components = normalized.takeIf(String::isNotEmpty)?.split(componentSeparator) ?: return null
            var totalNanoseconds = 0L

            for (component in components) {
                val unit = component.lastOrNull()?.lowercaseChar() ?: return null
                val unitNanoseconds =
                    when (unit) {
                        's' -> NANOS_PER_SECOND
                        'm' -> NANOS_PER_MINUTE
                        'h' -> NANOS_PER_HOUR
                        'd' -> NANOS_PER_WORKDAY
                        'w' -> NANOS_PER_WORKWEEK
                        else -> return null
                    }
                val amount = component.dropLast(1)
                if (!amount.isValidDecimal()) return null
                val numericAmount = amount.toDoubleOrNull() ?: return null
                val componentNanoseconds = numericAmount * unitNanoseconds.toDouble()
                if (!componentNanoseconds.isFinite() || componentNanoseconds >= Long.MAX_VALUE.toDouble()) return null
                val roundedNanoseconds = componentNanoseconds.roundToLong()
                if (roundedNanoseconds < 0 || totalNanoseconds > Long.MAX_VALUE - roundedNanoseconds) return null
                totalNanoseconds += roundedNanoseconds
            }

            return WorkDuration(totalNanoseconds)
        }

        private fun String.isValidDecimal(): Boolean {
            if (isEmpty()) return false
            var decimalPointSeen = false
            var digitsBeforeDecimal = 0
            var digitsAfterDecimal = 0
            for (character in this) {
                when {
                    character in '0'..'9' -> {
                        if (decimalPointSeen) {
                            digitsAfterDecimal++
                        } else {
                            digitsBeforeDecimal++
                        }
                    }
                    character == '.' && !decimalPointSeen -> decimalPointSeen = true
                    else -> return false
                }
            }
            return digitsBeforeDecimal > 0 && (!decimalPointSeen || digitsAfterDecimal > 0)
        }

        private const val NANOS_PER_SECOND = 1_000_000_000L
        private const val NANOS_PER_MINUTE = 60 * NANOS_PER_SECOND
        private const val NANOS_PER_HOUR = 60 * NANOS_PER_MINUTE
        private const val NANOS_PER_WORKDAY = 8 * NANOS_PER_HOUR
        private const val NANOS_PER_WORKWEEK = 5 * NANOS_PER_WORKDAY
    }
}
