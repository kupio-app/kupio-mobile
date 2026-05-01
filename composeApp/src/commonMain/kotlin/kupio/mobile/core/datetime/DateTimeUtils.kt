package kupio.mobile.core.datetime

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.date_today_time
import mobile.composeapp.generated.resources.date_yesterday_time
import mobile.composeapp.generated.resources.month_1
import mobile.composeapp.generated.resources.month_10
import mobile.composeapp.generated.resources.month_11
import mobile.composeapp.generated.resources.month_12
import mobile.composeapp.generated.resources.month_2
import mobile.composeapp.generated.resources.month_3
import mobile.composeapp.generated.resources.month_4
import mobile.composeapp.generated.resources.month_5
import mobile.composeapp.generated.resources.month_6
import mobile.composeapp.generated.resources.month_7
import mobile.composeapp.generated.resources.month_8
import mobile.composeapp.generated.resources.month_9
import org.jetbrains.compose.resources.stringResource
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
        ?: parseLocalDateOrNull()?.atTime(hour = 0, minute = 0)

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

private fun String.parseLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (_: Exception) {
        null
    }

private fun String.fallbackTimeLabel(): String {
    val tIndex = indexOf('T').takeIf { it >= 0 } ?: return this
    return substring(tIndex + 1, minOf(tIndex + 6, length))
}

@Composable
fun String.formatPostedAt(): String {
    val date = toLocalDate() ?: return take(10)
    val todayDate = today()
    val diff = (todayDate.toEpochDays() - date.toEpochDays()).toInt()
    return when {
        diff <= 0 -> stringResource(Res.string.date_today_time, toTimeLabel())
        diff == 1 -> stringResource(Res.string.date_yesterday_time, toTimeLabel())
        else -> "${date.day} ${monthName(date.month.number)} ${date.year}"
    }
}

@Composable
fun monthName(month: Int): String = when (month) {
    1 -> stringResource(Res.string.month_1)
    2 -> stringResource(Res.string.month_2)
    3 -> stringResource(Res.string.month_3)
    4 -> stringResource(Res.string.month_4)
    5 -> stringResource(Res.string.month_5)
    6 -> stringResource(Res.string.month_6)
    7 -> stringResource(Res.string.month_7)
    8 -> stringResource(Res.string.month_8)
    9 -> stringResource(Res.string.month_9)
    10 -> stringResource(Res.string.month_10)
    11 -> stringResource(Res.string.month_11)
    else -> stringResource(Res.string.month_12)
}

