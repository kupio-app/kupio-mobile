package kupio.mobile.core.datetime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class DateTimeUtilsTest {
    private val bratislava = TimeZone.of("Europe/Bratislava")
    private val newYork = TimeZone.of("America/New_York")

    @Test
    fun `toLocalDate converts ISO instant using provided time zone`() {
        assertEquals(
            LocalDate(2024, 4, 1),
            "2024-03-31T22:30:00Z".toLocalDate(bratislava),
        )
        assertEquals(
            LocalDate(2023, 12, 31),
            "2024-01-01T00:15:00Z".toLocalDate(newYork),
        )
    }

    @Test
    fun `toLocalDate keeps local date-time date without zone shifting`() {
        assertEquals(
            LocalDate(2024, 1, 1),
            "2024-01-01T00:15:00".toLocalDate(newYork),
        )
    }

    @Test
    fun `toLocalDate handles instants around local midnight`() {
        assertEquals(
            LocalDate(2024, 4, 1),
            "2024-04-01T21:59:00Z".toLocalDate(bratislava),
        )
        assertEquals(
            LocalDate(2024, 4, 2),
            "2024-04-01T22:00:00Z".toLocalDate(bratislava),
        )
    }

    @Test
    fun `toLocalDate returns null for invalid strings`() {
        assertNull("not-a-date".toLocalDate(bratislava))
    }

    @Test
    fun `toTimeLabel formats ISO instants in provided time zone`() {
        assertEquals("00:30", "2024-03-31T22:30:00Z".toTimeLabel(bratislava))
        assertEquals("19:15", "2024-01-01T00:15:00Z".toTimeLabel(newYork))
    }

    @Test
    fun `toTimeLabel formats local date-times without zone shifting`() {
        assertEquals("00:15", "2024-01-01T00:15:00".toTimeLabel(newYork))
    }

    @Test
    fun `toTimeLabel falls back for invalid strings`() {
        assertEquals("not-a-date", "not-a-date".toTimeLabel(bratislava))
        assertEquals("12:34", "invalidT12:34:99".toTimeLabel(bratislava))
    }
}
