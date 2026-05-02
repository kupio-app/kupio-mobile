package kupio.mobile.features.search.presentation.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioSwitch
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.search.presentation.filters.components.DealTypeSection
import kupio.mobile.features.search.presentation.filters.components.FiltersBottomBar
import kupio.mobile.features.search.presentation.filters.components.FiltersTopBar
import kupio.mobile.features.search.presentation.filters.components.PriceRangeSection
import kupio.mobile.features.search.presentation.filters.components.SearchCategoryPicker
import kupio.mobile.features.search.presentation.filters.components.SortBySection
import kupio.mobile.features.search.presentation.results.SearchResultsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.retry
import mobile.composeapp.generated.resources.search_filters_category_placeholder
import mobile.composeapp.generated.resources.search_filters_error_load_filters
import mobile.composeapp.generated.resources.search_filters_only_with_photos
import mobile.composeapp.generated.resources.search_filters_section_category
import mobile.composeapp.generated.resources.search_filters_section_deal_type
import mobile.composeapp.generated.resources.search_filters_section_price
import mobile.composeapp.generated.resources.search_filters_section_query
import mobile.composeapp.generated.resources.search_filters_section_show
import mobile.composeapp.generated.resources.search_filters_section_sort
import mobile.composeapp.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel


data class SearchFiltersScreen(val openResultsOnApply: Boolean = false) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<SearchFiltersViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                SearchFiltersEffect.NavigateBack -> navigator.pop()
                SearchFiltersEffect.NavigateToResults -> {
                    if (openResultsOnApply) {
                        navigator.replace(SearchResultsScreen(viewModel.state.value.draft))
                    } else {
                        navigator.pop()
                    }
                }
            }
        }

        if (state.isCategoryPickerOpen) {
            SearchCategoryPicker(state = state, onIntent = viewModel::onIntent)
        }

        SearchFiltersContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun SearchFiltersContent(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    Scaffold(
        topBar = { FiltersTopBar(state = state, onIntent = onIntent) },
        bottomBar = { FiltersBottomBar(onIntent = onIntent) },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn (
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_query)) }
            item { QuerySection(state = state, onIntent = onIntent) }

            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_category)) }
            item { CategorySection(state = state, onIntent = onIntent) }

            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_price)) }
            item { PriceRangeSection(state = state, onIntent = onIntent) }

            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_deal_type)) }
            item { DealTypeSection(state = state, onIntent = onIntent) }

            if (state.isLoadingFilters) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (state.filtersError != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = KupioThemeDefaults.spacing.lg,
                                vertical = KupioThemeDefaults.spacing.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.search_filters_error_load_filters),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(Res.string.retry),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .bouncingClickable {
                                    onIntent(
                                        SearchFiltersIntent.CategorySelected(
                                            state.draft.categoryId ?: return@bouncingClickable
                                        )
                                    )
                                }
                                .padding(KupioThemeDefaults.spacing.sm),
                        )
                    }
                }
            } else {
                items(state.filterDefinitions) { filter ->
                    SearchFilterItem(
                        filter = filter,
                        value = state.filterValues[filter.slug],
                        onIntent = onIntent,
                    )
                }
            }

            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_show)) }
            item { ShowSection(state = state, onIntent = onIntent) }

            item { FilterSectionLabel(stringResource(Res.string.search_filters_section_sort)) }
            item { SortBySection(state = state, onIntent = onIntent) }

            item { Spacer(Modifier.height(KupioThemeDefaults.spacing.xl)) }
        }
    }
}

@Composable
private fun FilterSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            start = KupioThemeDefaults.spacing.lg,
            end = KupioThemeDefaults.spacing.lg,
            top = KupioThemeDefaults.spacing.lg,
            bottom = KupioThemeDefaults.spacing.xs,
        ),
    )
}

@Composable
private fun QuerySection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .height(48.dp),
        shape = KupioShapes.ExtraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = state.draft.query,
                onValueChange = { onIntent(SearchFiltersIntent.QueryChanged(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (state.draft.query.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.search_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun CategorySection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg)
            .bouncingDimClickable(shape = KupioShapes.Large) { onIntent(SearchFiltersIntent.OpenCategoryPicker) },
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.draft.categoryName
                        ?: stringResource(Res.string.search_filters_category_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (state.draft.categoryName != null) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (state.draft.categoryName != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (state.draft.categoryPath != null) {
                    Text(
                        text = state.draft.categoryPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SearchFilterItem(
    filter: FilterDefinition,
    value: SearchFilterInput?,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KupioThemeDefaults.spacing.lg, vertical = KupioThemeDefaults.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.xs),
    ) {
        Text(
            text = filter.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.SemiBold,
        )
        when (filter.type) {
            FilterType.TEXT, FilterType.NUMBER, FilterType.RANGE -> {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = KupioShapes.Large,
                    color = MaterialTheme.colorScheme.surface,
                    border = KupioThemeDefaults.defaultBorder,
                ) {
                    BasicTextField(
                        value = (value as? SearchFilterInput.Text)?.value ?: "",
                        onValueChange = { onIntent(SearchFiltersIntent.FilterTextChanged(filter.slug, it)) },
                        modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = if (filter.type == FilterType.TEXT) KeyboardOptions.Default else KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if ((value as? SearchFilterInput.Text)?.value.isNullOrEmpty()) {
                                    Text(
                                        filter.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }

            FilterType.BOOLEAN -> {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
                ) {
                    val boolVal = (value as? SearchFilterInput.BooleanValue)?.value
                    if (!filter.isRequired) {
                        FilterChoiceChip(
                            text = "—",
                            selected = boolVal == null,
                            onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, null)) })
                    }
                    FilterChoiceChip(
                        text = "Yes",
                        selected = boolVal == true,
                        onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, true)) })
                    FilterChoiceChip(
                        text = "No",
                        selected = boolVal == false,
                        onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, false)) })
                }
            }

            FilterType.SELECT -> {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
                ) {
                    val selected = (value as? SearchFilterInput.Text)?.value ?: ""
                    filter.options.values.forEach { option ->
                        FilterChoiceChip(
                            text = option,
                            selected = selected == option,
                            onClick = { onIntent(SearchFiltersIntent.FilterTextChanged(filter.slug, option)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(34.dp)
            .bouncingDimClickable(shape = KupioShapes.Full, onClick = onClick),
        shape = KupioShapes.Full,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = if (selected) null else KupioThemeDefaults.strongBorder,
    ) {
        Box(modifier = Modifier.padding(horizontal = 13.dp), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShowSection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ToggleRow(
            label = stringResource(Res.string.search_filters_only_with_photos),
            checked = state.draft.onlyWithPhotos,
            onToggle = { onIntent(SearchFiltersIntent.OnlyWithPhotosToggled) },
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            KupioSwitch(checked = checked, onToggle = onToggle)
        }
    }
}