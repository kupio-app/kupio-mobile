package kupio.mobile.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun String.toLocalDate(): LocalDate? = try {
    parseToInstantOrNull()
        ?.toLocalDateTime(TimeZone.currentSystemDefault())
        ?.date
} catch (_: Exception) {
    null
}

fun String.toTimeLabel(): String = try {
    val instant = parseToInstantOrNull() ?: return fallbackTimeLabel()
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
} catch (_: Exception) {
    fallbackTimeLabel()
}

private fun String.parseToInstantOrNull(): Instant? = try {
    Instant.parse(this)
} catch (_: Exception) {
    try { LocalDateTime.parse(this).toInstant(TimeZone.UTC) } catch (_: Exception) { null }
}

private fun String.fallbackTimeLabel(): String {
    val tIndex = indexOf('T').takeIf { it >= 0 } ?: return this
    return substring(tIndex + 1, minOf(tIndex + 6, length))
}
