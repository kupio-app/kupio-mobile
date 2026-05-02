package kupio.mobile.features.search.presentation.filters.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.borderTop
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.search.presentation.filters.SearchFiltersIntent
import kupio.mobile.features.search.presentation.filters.SearchFiltersState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.search_filters_apply
import mobile.composeapp.generated.resources.search_filters_clear_all
import mobile.composeapp.generated.resources.search_filters_results_match
import mobile.composeapp.generated.resources.search_filters_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FiltersTopBar(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    val subtitle = state.resultCount?.let {
        stringResource(Res.string.search_filters_results_match, state.resultCount)
    }
    KupioTopNavbar(
        title = stringResource(Res.string.search_filters_title),
        subtitle = subtitle,
        leadingContent = {
            KupioTopBarBackAction(
                contentDescription = stringResource(Res.string.back),
                onClick = { onIntent(SearchFiltersIntent.Back) },
            )
        },
        trailingContent = {
            Text(
                text = stringResource(Res.string.search_filters_clear_all),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .bouncingClickable { onIntent(SearchFiltersIntent.ClearAll) }
                    .padding(spacing.sm),
            )
        },
    )
}

@Composable
internal fun FiltersBottomBar(
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .borderTop(KupioThemeDefaults.borderWidths.thin, KupioThemeDefaults.navDividerColor),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.lg,
                    end = spacing.lg,
                    bottom = spacing.lg,
                    top = spacing.md
                ),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .bouncingDimClickable(shape = KupioShapes.Large) { onIntent(SearchFiltersIntent.Apply) },
                shape = KupioShapes.Large,
                color = MaterialTheme.colorScheme.onSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.search_filters_apply),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }
    }
}