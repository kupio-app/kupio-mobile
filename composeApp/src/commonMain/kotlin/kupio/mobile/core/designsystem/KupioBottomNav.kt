package kupio.mobile.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.nav_create
import org.jetbrains.compose.resources.stringResource

data class KupioBottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

@Composable
fun KupioBottomNav(
    items: List<KupioBottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onCenterActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    val fabSize = 56.dp
    // 10% of the FAB protrudes above the bar
    val fabOverhang = fabSize * 0.1f

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .borderTop(KupioThemeDefaults.borderWidths.regular, KupioThemeDefaults.navDividerColor),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.sm, end = spacing.sm, top = spacing.sm, bottom = spacing.xl),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.take(2).forEachIndexed { index, item ->
                    BottomNavCell(
                        item = item,
                        selected = selectedIndex == index,
                        onClick = { onItemSelected(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                items.drop(2).forEachIndexed { index, item ->
                    BottomNavCell(
                        item = item,
                        selected = selectedIndex == index + 2,
                        onClick = { onItemSelected(index + 2) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        CenterFab(
            onClick = onCenterActionClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -fabOverhang),
        )
    }
}

@Composable
private fun BottomNavCell(
    item: KupioBottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    Column(
        modifier = modifier
            .bouncingClickable(onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun CenterFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.size(60.dp).shadow(
            elevation = 10.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(Res.string.nav_create),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
