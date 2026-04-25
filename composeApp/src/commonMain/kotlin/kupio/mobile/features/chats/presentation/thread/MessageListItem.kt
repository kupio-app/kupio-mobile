package kupio.mobile.features.chats.presentation.thread

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kupio.mobile.features.chats.domain.model.MessageItem
import kotlin.time.Instant

sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel
    data class DaysAgo(val count: Int) : DayLabel
    data class AbsoluteDate(val day: Int, val month: Int) : DayLabel
}

sealed interface MessageListItem {
    data class Message(val item: MessageItem) : MessageListItem
    data class DaySeparator(val label: DayLabel) : MessageListItem
}

internal fun groupMessagesByDay(messages: List<MessageItem>): List<MessageListItem> {
    if (messages.isEmpty()) return emptyList()
    val todayJdn = currentJulianDay()
    val result = mutableListOf<MessageListItem>()
    var lastJdn: Int? = null
    for (message in messages) {
        val msgJdn = julianDayFromIso(message.createdAtIso)
        if (msgJdn != lastJdn) {
            val label = buildDayLabel(msgJdn, todayJdn, message.createdAtIso)
            if (label != null) result.add(MessageListItem.DaySeparator(label))
            lastJdn = msgJdn
        }
        result.add(MessageListItem.Message(message))
    }
    return result
}

private fun buildDayLabel(msgJdn: Int?, todayJdn: Int, iso: String): DayLabel? {
    if (msgJdn == null) return null
    return when (val diff = todayJdn - msgJdn) {
        0 -> DayLabel.Today
        1 -> DayLabel.Yesterday
        in 2..6 -> DayLabel.DaysAgo(diff)
        else -> {
            val (_, m, d) = parseDateFromIso(iso) ?: return null
            DayLabel.AbsoluteDate(d, m)
        }
    }
}

private fun julianDayFromIso(iso: String): Int? {
    val (y, m, d) = parseDateFromIso(iso) ?: return null
    val a = (14 - m) / 12
    val y2 = y + 4800 - a
    val m2 = m + 12 * a - 3
    return d + (153 * m2 + 2) / 5 + 365 * y2 + y2 / 4 - y2 / 100 + y2 / 400 - 32045
}

@OptIn(ExperimentalTime::class)
private fun currentJulianDay(): Int {
    val utcDays = (Clock.System.now().toEpochMilliseconds() / 86_400_000L).toInt()
    return utcDays + 2440588
}

private fun parseDateFromIso(iso: String): Triple<Int, Int, Int>? {
    return try {
        val datePart = iso.substringBefore('T').takeIf { it.length >= 10 } ?: return null
        val parts = datePart.split('-')
        if (parts.size < 3) return null
        Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    } catch (_: Exception) {
        null
    }
}
