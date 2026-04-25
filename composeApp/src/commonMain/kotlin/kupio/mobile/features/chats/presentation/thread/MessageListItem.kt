package kupio.mobile.features.chats.presentation.thread

import kotlinx.datetime.LocalDate
import kupio.mobile.core.datetime.today
import kupio.mobile.core.datetime.toLocalDate
import kupio.mobile.features.chats.domain.model.MessageItem

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
    val todayDate = today()
    val result = mutableListOf<MessageListItem>()
    var lastDate: LocalDate? = null
    for (message in messages) {
        val msgDate = message.createdAtIso.toLocalDate()
        if (msgDate != lastDate) {
            val label = msgDate?.let { buildDayLabel(it, todayDate) }
            if (label != null) result.add(MessageListItem.DaySeparator(label))
            lastDate = msgDate
        }
        result.add(MessageListItem.Message(message))
    }
    return result
}

private fun buildDayLabel(date: LocalDate, today: LocalDate): DayLabel {
    val diff = (today.toEpochDays() - date.toEpochDays()).toInt()
    return when {
        diff <= 0 -> DayLabel.Today
        diff == 1 -> DayLabel.Yesterday
        diff in 2..6 -> DayLabel.DaysAgo(diff)
        else -> DayLabel.AbsoluteDate(date.day, date.month.ordinal)
    }
}
