package kupio.mobile.features.me.presentation.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.features.auth.domain.model.AuthenticatedUser
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.profile_balance_label
import mobile.composeapp.generated.resources.profile_topup
import org.jetbrains.compose.resources.stringResource
import kupio.mobile.core.designsystem.KupioShapes

@Composable
internal fun BalanceCard(user: AuthenticatedUser?, onTopUp: () -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.ExtraLarge,
        color = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = KupioShapes.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column {
                Text(
                    text = stringResource(Res.string.profile_balance_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                )
                Text(
                    text = formatBalance(user?.balance ?: 0),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.surface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                modifier = Modifier.bouncingClickable(onClick = onTopUp),
                shape = KupioShapes.Full,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                    text = stringResource(Res.string.profile_topup),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

internal fun formatBalance(cents: Int): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole.$fraction €"
}
