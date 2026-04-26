package kupio.mobile.features.chats.presentation.thread

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kupio.mobile.features.chats.domain.model.MessageItem
import kupio.mobile.features.chats.domain.model.MessageSender

class MessageListItemTest {
    private val today = LocalDate(2024, 4, 10)

    @Test
    fun `buildDayLabel selects relative and absolute labels`() {
        assertEquals(DayLabel.Today, buildDayLabel(LocalDate(2024, 4, 10), today))
        assertEquals(DayLabel.Today, buildDayLabel(LocalDate(2024, 4, 11), today))
        assertEquals(DayLabel.Yesterday, buildDayLabel(LocalDate(2024, 4, 9), today))
        assertEquals(DayLabel.DaysAgo(2), buildDayLabel(LocalDate(2024, 4, 8), today))
        assertEquals(DayLabel.DaysAgo(6), buildDayLabel(LocalDate(2024, 4, 4), today))
        assertEquals(DayLabel.AbsoluteDate(day = 3, month = 4), buildDayLabel(LocalDate(2024, 4, 3), today))
    }

    @Test
    fun `groupMessagesByDay inserts separators and preserves message order`() {
        val first = message(id = "1", createdAtIso = "2024-04-09T23:50:00")
        val second = message(id = "2", createdAtIso = "2024-04-10T00:10:00")
        val third = message(id = "3", createdAtIso = "2024-04-09T23:55:00")

        assertEquals(
            listOf(
                MessageListItem.DaySeparator(DayLabel.Yesterday),
                MessageListItem.Message(first),
                MessageListItem.DaySeparator(DayLabel.Today),
                MessageListItem.Message(second),
                MessageListItem.DaySeparator(DayLabel.Yesterday),
                MessageListItem.Message(third),
            ),
            groupMessagesByDay(listOf(first, second, third), todayDate = today),
        )
    }

    @Test
    fun `groupMessagesByDay only inserts one separator for adjacent messages on same day`() {
        val first = message(id = "1", createdAtIso = "2024-04-10T08:00:00")
        val second = message(id = "2", createdAtIso = "2024-04-10T09:00:00")

        assertEquals(
            listOf(
                MessageListItem.DaySeparator(DayLabel.Today),
                MessageListItem.Message(first),
                MessageListItem.Message(second),
            ),
            groupMessagesByDay(listOf(first, second), todayDate = today),
        )
    }

    @Test
    fun `groupMessagesByDay skips separator for invalid createdAtIso`() {
        val invalid = message(id = "invalid", createdAtIso = "not-a-date")
        val valid = message(id = "valid", createdAtIso = "2024-04-10T08:00:00")

        assertEquals(
            listOf(
                MessageListItem.Message(invalid),
                MessageListItem.DaySeparator(DayLabel.Today),
                MessageListItem.Message(valid),
            ),
            groupMessagesByDay(listOf(invalid, valid), todayDate = today),
        )
    }

    @Test
    fun `groupMessagesByDay labels older messages by day distance and absolute date`() {
        val twoDaysAgo = message(id = "two", createdAtIso = "2024-04-08T12:00:00")
        val sixDaysAgo = message(id = "six", createdAtIso = "2024-04-04T12:00:00")
        val absolute = message(id = "absolute", createdAtIso = "2024-04-03T12:00:00")

        assertEquals(
            listOf(
                MessageListItem.DaySeparator(DayLabel.DaysAgo(2)),
                MessageListItem.Message(twoDaysAgo),
                MessageListItem.DaySeparator(DayLabel.DaysAgo(6)),
                MessageListItem.Message(sixDaysAgo),
                MessageListItem.DaySeparator(DayLabel.AbsoluteDate(day = 3, month = 4)),
                MessageListItem.Message(absolute),
            ),
            groupMessagesByDay(listOf(twoDaysAgo, sixDaysAgo, absolute), todayDate = today),
        )
    }

    private fun message(id: String, createdAtIso: String): MessageItem =
        MessageItem(
            id = id,
            sender = MessageSender.ME,
            text = "message $id",
            timeLabel = "12:00",
            createdAtIso = createdAtIso,
        )
}
