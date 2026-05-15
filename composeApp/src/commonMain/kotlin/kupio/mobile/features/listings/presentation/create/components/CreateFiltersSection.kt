package kupio.mobile.features.listings.presentation.create.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kupio.mobile.core.designsystem.KupioCustomFilterInput
import kupio.mobile.core.designsystem.KupioErrorRetryRow
import kupio.mobile.core.designsystem.KupioLoadingRow
import kupio.mobile.features.listings.presentation.create.CreateFilterInput
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState
import kupio.mobile.features.listings.presentation.create.toErrorMessage
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_error_load_filters
import mobile.composeapp.generated.resources.create_filters
import mobile.composeapp.generated.resources.create_filters_empty
import mobile.composeapp.generated.resources.create_filters_select_category
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FiltersSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = stringResource(Res.string.create_filters)) {
        when {
            state.selectedCategoryId == null -> Text(
                text = stringResource(Res.string.create_filters_select_category),
                modifier = Modifier.testTag("listing.create.filters-placeholder"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.isLoadingFilters -> KupioLoadingRow()
            state.filtersError != null -> KupioErrorRetryRow(
                message = stringResource(Res.string.create_error_load_filters),
                onRetry = { onIntent(CreateIntent.RetryFilters) },
            )

            state.filters.isEmpty() -> Text(
                text = stringResource(Res.string.create_filters_empty),
                modifier = Modifier.testTag("listing.create.filters-empty"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> state.filters.forEach { filter ->
                KupioCustomFilterInput(
                    filter = filter,
                    textValue = (state.filterValues[filter.slug] as? CreateFilterInput.Text)?.value,
                    booleanValue = (state.filterValues[filter.slug] as? CreateFilterInput.BooleanValue)?.value,
                    error = state.filterErrors[filter.slug]?.toErrorMessage(),
                    onTextChanged = { slug, value -> onIntent(CreateIntent.FilterTextChanged(slug, value)) },
                    onBooleanChanged = { slug, value -> onIntent(CreateIntent.FilterBooleanChanged(slug, value)) },
                )
            }
        }
    }
}
