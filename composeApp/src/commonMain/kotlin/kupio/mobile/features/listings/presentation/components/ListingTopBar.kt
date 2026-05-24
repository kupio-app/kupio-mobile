package kupio.mobile.features.listings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioThemeDefaults.spacing
import kupio.mobile.core.designsystem.borderBottom
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListingTopBar(
    onBack: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    backButtonModifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val backgroundAlpha by remember {
        derivedStateOf {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                ?: return@derivedStateOf 0f
            if (firstItem.index > 0) return@derivedStateOf 1f
            (-firstItem.offset.toFloat() / firstItem.size).coerceIn(0f, 1f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .borderBottom(
                KupioThemeDefaults.borderWidths.thin,
                KupioThemeDefaults.navDividerColor.copy(alpha = backgroundAlpha * 0.3f),
            ),
        color = MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ListingFloatingIconButton(
                onClick = onBack,
                contentDescription = stringResource(Res.string.back),
                modifier = backButtonModifier,
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null
                )
            }

            actions()
        }
    }
}
