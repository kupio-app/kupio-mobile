package kupio.mobile.features.me.presentation.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.number
import kupio.mobile.core.datetime.monthName
import kupio.mobile.core.datetime.toLocalDate
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.profile_member_since
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UserInfoSection(user: AuthenticatedUser?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
    ) {
        AvatarCircle(user = user)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val name = user?.displayName ?: user?.username ?: "—"
            Text(
                text = "Hello, $name.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                user?.username?.let { username ->
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val memberSince = memberSinceMonthYear(user?.createdAt)
                if (memberSince != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.profile_member_since, memberSince),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AvatarCircle(user: AuthenticatedUser?, modifier: Modifier = Modifier) {
    val initials = buildInitials(user)
    val bgColor = avatarColor(user?.username ?: user?.id ?: "")
    Box(
        modifier = modifier
            .size(64.dp)
            .background(bgColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

internal fun buildInitials(user: AuthenticatedUser?): String {
    val name = user?.displayName ?: user?.username ?: return "?"
    return name.trim().split("\\s+".toRegex())
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
}

@Composable
internal fun memberSinceMonthYear(createdAt: String?): String? {
    val date = createdAt?.toLocalDate() ?: return null
    return "${monthName(date.month.number)} ${date.year}"
}

private val avatarColors = listOf(
    Color(0xFF9B7653),
    Color(0xFFB4623A),
    Color(0xFF7A6650),
    Color(0xFF5C4A36),
    Color(0xFF856E58),
)

internal fun avatarColor(seed: String): Color {
    val index = (seed.hashCode() and 0x7FFFFFFF) % avatarColors.size
    return avatarColors[index]
}
