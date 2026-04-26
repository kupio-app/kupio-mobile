package kupio.mobile.core.datetime

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

fun nowEpochSeconds(): Long = Clock.System.now().epochSeconds

fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun String.toLocalDate(): LocalDate? = toLocalDate(TimeZone.currentSystemDefault())

internal fun String.toLocalDate(timeZone: TimeZone): LocalDate? = try {
    parseToLocalDateTimeOrNull(timeZone)?.date
} catch (_: Exception) {
    null
}

fun String.toTimeLabel(): String = toTimeLabel(TimeZone.currentSystemDefault())

internal fun String.toTimeLabel(timeZone: TimeZone): String = try {
    val local = parseToLocalDateTimeOrNull(timeZone) ?: return fallbackTimeLabel()
    "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
} catch (_: Exception) {
    fallbackTimeLabel()
}

private fun String.parseToLocalDateTimeOrNull(timeZone: TimeZone): LocalDateTime? =
    parseInstantOrNull()?.toLocalDateTime(timeZone)
        ?: parseLocalDateTimeOrNull()

private fun String.parseInstantOrNull(): Instant? =
    try {
        Instant.parse(this)
    } catch (_: Exception) {
        null
    }

private fun String.parseLocalDateTimeOrNull(): LocalDateTime? =
    try {
        LocalDateTime.parse(this)
    } catch (_: Exception) {
        null
    }

private fun String.fallbackTimeLabel(): String {
    val tIndex = indexOf('T').takeIf { it >= 0 } ?: return this
    return substring(tIndex + 1, minOf(tIndex + 6, length))
}
