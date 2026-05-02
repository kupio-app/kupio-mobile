package kupio.mobile.features.search.presentation.filters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.core.designsystem.borderBottom
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import kupio.mobile.features.search.domain.model.DealType
import kupio.mobile.features.search.domain.model.SearchSortBy
import kupio.mobile.features.search.presentation.results.SearchResultsScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.back
import mobile.composeapp.generated.resources.retry
import mobile.composeapp.generated.resources.search_filters_all_categories
import mobile.composeapp.generated.resources.search_filters_apply
import mobile.composeapp.generated.resources.search_filters_categories
import mobile.composeapp.generated.resources.search_filters_category_placeholder
import mobile.composeapp.generated.resources.search_filters_clear_all
import mobile.composeapp.generated.resources.search_filters_deal_for_sale
import mobile.composeapp.generated.resources.search_filters_deal_free
import mobile.composeapp.generated.resources.search_filters_deal_trade
import mobile.composeapp.generated.resources.search_filters_delivery_available
import mobile.composeapp.generated.resources.search_filters_error_load_filters
import mobile.composeapp.generated.resources.search_filters_error_load_subcategories
import mobile.composeapp.generated.resources.search_filters_only_with_photos
import mobile.composeapp.generated.resources.search_filters_price_from
import mobile.composeapp.generated.resources.search_filters_price_range
import mobile.composeapp.generated.resources.search_filters_price_to
import mobile.composeapp.generated.resources.search_filters_results_match
import mobile.composeapp.generated.resources.search_filters_section_category
import mobile.composeapp.generated.resources.search_filters_section_deal_type
import mobile.composeapp.generated.resources.search_filters_section_price
import mobile.composeapp.generated.resources.search_filters_section_query
import mobile.composeapp.generated.resources.search_filters_section_show
import mobile.composeapp.generated.resources.search_filters_section_sort
import mobile.composeapp.generated.resources.search_filters_sort_newest
import mobile.composeapp.generated.resources.search_filters_sort_price_asc
import mobile.composeapp.generated.resources.search_filters_sort_price_desc
import mobile.composeapp.generated.resources.search_filters_sort_recommended
import mobile.composeapp.generated.resources.search_filters_subcategories
import mobile.composeapp.generated.resources.search_filters_title
import mobile.composeapp.generated.resources.search_filters_use_category
import mobile.composeapp.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val PRICE_MAX_VALUE = 10000f

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
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        FiltersTopBar(state = state, onIntent = onIntent)

        LazyColumn(
            modifier = Modifier.weight(1f),
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
                            .padding(horizontal = KupioThemeDefaults.spacing.lg, vertical = KupioThemeDefaults.spacing.sm),
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
                                .bouncingClickable { onIntent(SearchFiltersIntent.CategorySelected(state.draft.categoryId ?: return@bouncingClickable)) }
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

        FiltersBottomBar(onIntent = onIntent)
    }
}

@Composable
private fun FiltersTopBar(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .borderBottom(KupioThemeDefaults.borderWidths.thin, KupioThemeDefaults.navDividerColor),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = spacing.md, end = spacing.lg, top = spacing.md, bottom = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .bouncingClickable { onIntent(SearchFiltersIntent.Back) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(Res.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(spacing.sm))
            Column {
                Text(
                    text = stringResource(Res.string.search_filters_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.resultCount != null) {
                    Text(
                        text = stringResource(Res.string.search_filters_results_match, state.resultCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.search_filters_clear_all),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .bouncingClickable { onIntent(SearchFiltersIntent.ClearAll) }
                    .padding(spacing.sm),
            )
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
private fun PriceRangeSection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (state.draft.minPrice != null || state.draft.maxPrice != null) {
            Text(
                text = stringResource(
                    Res.string.search_filters_price_range,
                    state.draft.minPrice ?: 0,
                    state.draft.maxPrice ?: PRICE_MAX_VALUE.toInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            PriceInputField(
                label = stringResource(Res.string.search_filters_price_from),
                value = state.draft.minPrice?.toString() ?: "",
                onValueChange = { onIntent(SearchFiltersIntent.PriceMinChanged(it)) },
                modifier = Modifier.weight(1f),
            )
            PriceInputField(
                label = stringResource(Res.string.search_filters_price_to),
                value = state.draft.maxPrice?.toString() ?: "",
                onValueChange = { onIntent(SearchFiltersIntent.PriceMaxChanged(it)) },
                modifier = Modifier.weight(1f),
            )
        }

        KupioRangeSlider(
            startValue = state.draft.minPrice?.toFloat() ?: 0f,
            endValue = state.draft.maxPrice?.toFloat() ?: PRICE_MAX_VALUE,
            valueRange = 0f..PRICE_MAX_VALUE,
            onValueChange = { start, end ->
                onIntent(SearchFiltersIntent.PriceRangeChanged(start.toInt(), end.toInt()))
            },
        )
    }
}

@Composable
private fun PriceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(52.dp),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md, vertical = 8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DealTypeSection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KupioThemeDefaults.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm),
    ) {
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_for_sale),
            selected = state.draft.dealType == DealType.FOR_SALE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.FOR_SALE)) },
            modifier = Modifier.weight(1f),
        )
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_free),
            selected = state.draft.dealType == DealType.FREE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.FREE)) },
            modifier = Modifier.weight(1f),
        )
        DealTypeChip(
            label = stringResource(Res.string.search_filters_deal_trade),
            selected = state.draft.dealType == DealType.TRADE,
            onClick = { onIntent(SearchFiltersIntent.DealTypeSelected(DealType.TRADE)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DealTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .bouncingDimClickable(shape = KupioShapes.Large, onClick = onClick),
        shape = KupioShapes.Large,
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = if (selected) null else KupioThemeDefaults.strongBorder,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
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
                        keyboardOptions = if (filter.type == FilterType.TEXT) KeyboardOptions.Default else KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if ((value as? SearchFilterInput.Text)?.value.isNullOrEmpty()) {
                                    Text(filter.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        FilterChoiceChip(text = "—", selected = boolVal == null, onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, null)) })
                    }
                    FilterChoiceChip(text = "Yes", selected = boolVal == true, onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, true)) })
                    FilterChoiceChip(text = "No", selected = boolVal == false, onClick = { onIntent(SearchFiltersIntent.FilterBooleanChanged(filter.slug, false)) })
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
        ToggleRow(
            label = stringResource(Res.string.search_filters_delivery_available),
            checked = state.draft.deliveryAvailable,
            onToggle = { onIntent(SearchFiltersIntent.DeliveryAvailableToggled) },
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

@Composable
private fun KupioSwitch(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val thumbX by animateDpAsState(targetValue = if (checked) 20.dp else 2.dp)
    val trackColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .bouncingClickable(onClick = onToggle),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = KupioShapes.Full,
            color = trackColor,
        ) {}
        Surface(
            modifier = Modifier
                .size(22.dp)
                .absoluteOffset(x = thumbX, y = 2.dp),
            shape = KupioShapes.Full,
            color = Color.White,
        ) {}
    }
}

@Composable
private fun KupioRangeSlider(
    startValue: Float,
    endValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (start: Float, end: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current
    val thumbRadiusPx = with(density) { 10.dp.toPx() }

    val currentStart by rememberUpdatedState(startValue)
    val currentEnd by rememberUpdatedState(endValue)
    val currentCallback by rememberUpdatedState(onValueChange)

    var sliderWidthPx by remember { mutableStateOf(0f) }
    var draggingThumb by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onGloballyPositioned { coords -> sliderWidthPx = coords.size.width.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val tw = (sliderWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val range = valueRange.endInclusive - valueRange.start
                        val sf = if (range == 0f) 0f else ((currentStart - valueRange.start) / range).coerceIn(0f, 1f)
                        val ef = if (range == 0f) 1f else ((currentEnd - valueRange.start) / range).coerceIn(0f, 1f)
                        val sx = sf * tw + thumbRadiusPx
                        val ex = ef * tw + thumbRadiusPx
                        draggingThumb = if (kotlin.math.abs(offset.x - sx) <= kotlin.math.abs(offset.x - ex)) 0 else 1
                    },
                    onDragEnd = { draggingThumb = null },
                    onDragCancel = { draggingThumb = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val tw = (sliderWidthPx - thumbRadiusPx * 2).coerceAtLeast(1f)
                        val f = ((change.position.x - thumbRadiusPx) / tw).coerceIn(0f, 1f)
                        val v = valueRange.start + f * (valueRange.endInclusive - valueRange.start)
                        when (draggingThumb) {
                            0 -> currentCallback(v.coerceAtMost(currentEnd), currentEnd)
                            1 -> currentCallback(currentStart, v.coerceAtLeast(currentStart))
                        }
                    },
                )
            },
    ) {
        val tw = (size.width - thumbRadiusPx * 2).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val range = valueRange.endInclusive - valueRange.start
        val sf = if (range == 0f) 0f else ((currentStart - valueRange.start) / range).coerceIn(0f, 1f)
        val ef = if (range == 0f) 1f else ((currentEnd - valueRange.start) / range).coerceIn(0f, 1f)
        val sx = sf * tw + thumbRadiusPx
        val ex = ef * tw + thumbRadiusPx
        val trackH = 4.dp.toPx()
        val cr = CornerRadius(trackH / 2)

        drawRoundRect(
            color = inactiveColor,
            topLeft = Offset(thumbRadiusPx, centerY - trackH / 2),
            size = Size(tw, trackH),
            cornerRadius = cr,
        )
        if (ex > sx) {
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(sx, centerY - trackH / 2),
                size = Size(ex - sx, trackH),
                cornerRadius = cr,
            )
        }
        listOf(sx, ex).forEach { cx ->
            drawCircle(color = primaryColor, radius = thumbRadiusPx, center = Offset(cx, centerY))
            drawCircle(color = Color.White, radius = thumbRadiusPx - 2.5f, center = Offset(cx, centerY))
        }
    }
}

@Composable
private fun SortBySection(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val sorts = listOf(
        SearchSortBy.RECOMMENDED to stringResource(Res.string.search_filters_sort_recommended),
        SearchSortBy.NEWEST_FIRST to stringResource(Res.string.search_filters_sort_newest),
        SearchSortBy.PRICE_LOW_HIGH to stringResource(Res.string.search_filters_sort_price_asc),
        SearchSortBy.PRICE_HIGH_LOW to stringResource(Res.string.search_filters_sort_price_desc),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KupioThemeDefaults.spacing.lg),
        shape = KupioShapes.Large,
        color = MaterialTheme.colorScheme.surface,
        border = KupioThemeDefaults.defaultBorder,
    ) {
        Column {
            sorts.forEachIndexed { index, (sort, label) ->
                SortByRow(
                    label = label,
                    selected = state.draft.sortBy == sort,
                    onClick = { onIntent(SearchFiltersIntent.SortBySelected(sort)) },
                    showDivider = index < sorts.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun SortByRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KupioThemeDefaults.spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(horizontal = KupioThemeDefaults.spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp,
            )
        }
    }
}

@Composable
private fun FiltersBottomBar(
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(KupioThemeDefaults.borderWidths.thin, KupioThemeDefaults.navDividerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
                .padding(top = spacing.md),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchCategoryPicker(
    state: SearchFiltersState,
    onIntent: (SearchFiltersIntent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onIntent(SearchFiltersIntent.CloseCategoryPicker) },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val spacing = KupioThemeDefaults.spacing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.search_filters_category_placeholder),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            CategoryBreadcrumb(
                path = state.categoryPath,
                onRootClick = { onIntent(SearchFiltersIntent.CategoryPickerReset) },
                onCategoryClick = { onIntent(SearchFiltersIntent.CategorySelected(it.id)) },
            )

            if (state.categoryPath.isNotEmpty()) {
                CategoryBackRow(
                    path = state.categoryPath,
                    onClick = { onIntent(SearchFiltersIntent.CategoryPickerBack) },
                )
            }

            state.selectedCategoryName?.let { name ->
                androidx.compose.material3.Button(
                    onClick = { onIntent(SearchFiltersIntent.CloseCategoryPicker) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = KupioShapes.Medium,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(stringResource(Res.string.search_filters_use_category, name))
                }
            }

            val displayCategories = if (state.categoryPath.isEmpty()) {
                state.topLevelCategories
            } else {
                state.visibleSubcategories
            }

            if (displayCategories.isEmpty() && !state.isLoadingSubcategories && state.subcategoriesError == null && state.categoryPath.isNotEmpty()) {
                // leaf category selected — nothing to show
            } else {
                Text(
                    text = if (state.categoryPath.isEmpty()) {
                        stringResource(Res.string.search_filters_categories)
                    } else {
                        stringResource(Res.string.search_filters_subcategories)
                    }.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            when {
                state.isLoadingSubcategories -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                state.subcategoriesError != null -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.search_filters_error_load_subcategories),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(Res.string.retry),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.bouncingClickable {
                            val parent = state.categoryPath.lastOrNull()
                            if (parent != null) onIntent(SearchFiltersIntent.CategorySelected(parent.id))
                            else onIntent(SearchFiltersIntent.CategoryPickerReset)
                        }.padding(spacing.sm),
                    )
                }
                displayCategories.isNotEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayCategories.forEach { cat ->
                        PickerCategoryRow(
                            name = cat.name,
                            selected = state.selectedCategoryId == cat.id,
                            onClick = { onIntent(SearchFiltersIntent.CategorySelected(cat.id)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryBreadcrumb(
    path: List<kupio.mobile.features.listings.domain.model.Category>,
    onRootClick: () -> Unit,
    onCategoryClick: (kupio.mobile.features.listings.domain.model.Category) -> Unit,
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.search_filters_all_categories),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .bouncingDimClickable(shape = KupioShapes.Small, onClick = onRootClick)
                .padding(horizontal = 4.dp, vertical = 4.dp),
        )
        path.forEach { cat ->
            Text(
                "/",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 7.dp),
            )
            Text(
                text = cat.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .bouncingDimClickable(shape = KupioShapes.Small, onClick = { onCategoryClick(cat) })
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CategoryBackRow(
    path: List<kupio.mobile.features.listings.domain.model.Category>,
    onClick: () -> Unit,
) {
    val prev = path.dropLast(1).lastOrNull()?.name ?: stringResource(Res.string.search_filters_all_categories)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
        shape = KupioShapes.Medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = prev,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PickerCategoryRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
        shape = KupioShapes.Medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(KupioThemeDefaults.borderWidths.regular, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
        } else {
            KupioThemeDefaults.strongBorder
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
