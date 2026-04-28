package kupio.mobile.features.listings.presentation.create

import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_error_category_required
import mobile.composeapp.generated.resources.create_error_description_too_long
import mobile.composeapp.generated.resources.create_error_description_too_short
import mobile.composeapp.generated.resources.create_error_filter_invalid_option
import mobile.composeapp.generated.resources.create_error_filter_number
import mobile.composeapp.generated.resources.create_error_filter_number_max
import mobile.composeapp.generated.resources.create_error_filter_number_min
import mobile.composeapp.generated.resources.create_error_price_negative
import mobile.composeapp.generated.resources.create_error_price_not_number
import mobile.composeapp.generated.resources.create_error_price_too_high
import mobile.composeapp.generated.resources.create_error_required
import mobile.composeapp.generated.resources.create_error_title_too_long
import mobile.composeapp.generated.resources.create_error_title_too_short

private const val TitleMinLength = 8
private const val TitleMaxLength = 255
private const val DescriptionMinLength = 50
private const val DescriptionMaxLength = 5000
private const val MaxPriceExclusive = 10_000_000

data class CreateValidationResult(
    val listing: CreateListing?,
    val fieldErrors: Map<CreateField, CreateText>,
    val filterErrors: Map<String, CreateText>,
) {
    val isValid: Boolean = listing != null
}

fun validateCreateListing(state: CreateState): CreateValidationResult {
    val fieldErrors = mutableMapOf<CreateField, CreateText>()
    val filterErrors = mutableMapOf<String, CreateText>()

    val title = state.title.trim()
    if (title.length < TitleMinLength) {
        fieldErrors[CreateField.TITLE] = createText(Res.string.create_error_title_too_short, TitleMinLength)
    } else if (title.length > TitleMaxLength) {
        fieldErrors[CreateField.TITLE] = createText(Res.string.create_error_title_too_long, TitleMaxLength)
    }

    val description = state.description.trim()
    if (description.length < DescriptionMinLength) {
        fieldErrors[CreateField.DESCRIPTION] =
            createText(Res.string.create_error_description_too_short, DescriptionMinLength)
    } else if (description.length > DescriptionMaxLength) {
        fieldErrors[CreateField.DESCRIPTION] =
            createText(Res.string.create_error_description_too_long, DescriptionMaxLength)
    }

    val categoryId = state.selectedCategoryId
    if (categoryId == null) {
        fieldErrors[CreateField.CATEGORY] = createText(Res.string.create_error_category_required)
    }

    val price = if (state.isFree) {
        0
    } else {
        state.price.trim().toIntOrNull()
    }
    if (price == null) {
        fieldErrors[CreateField.PRICE] = createText(Res.string.create_error_price_not_number)
    } else if (price < 0) {
        fieldErrors[CreateField.PRICE] = createText(Res.string.create_error_price_negative)
    } else if (price >= MaxPriceExclusive) {
        fieldErrors[CreateField.PRICE] = createText(Res.string.create_error_price_too_high, MaxPriceExclusive)
    }

    val customFilters = buildCustomFilterPayload(
        definitions = state.filters,
        values = state.filterValues,
        errors = filterErrors,
    )

    if (fieldErrors.isNotEmpty() || filterErrors.isNotEmpty() || categoryId == null || price == null) {
        return CreateValidationResult(
            listing = null,
            fieldErrors = fieldErrors,
            filterErrors = filterErrors,
        )
    }

    return CreateValidationResult(
        listing = CreateListing(
            title = title,
            description = description,
            price = price,
            currency = state.currency,
            categoryId = categoryId,
            isFree = state.isFree,
            isTradable = state.isTradable,
            customFilters = customFilters,
        ),
        fieldErrors = emptyMap(),
        filterErrors = emptyMap(),
    )
}

private fun buildCustomFilterPayload(
    definitions: List<FilterDefinition>,
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateText>,
): Map<String, CustomFilterPayloadValue> {
    return definitions.mapNotNull { definition ->
        when (definition.type) {
            FilterType.TEXT -> definition.readText(values, errors)
            FilterType.NUMBER, FilterType.RANGE -> definition.readNumber(values, errors)
            FilterType.BOOLEAN -> definition.readBoolean(values, errors)
            FilterType.SELECT -> definition.readSelect(values, errors)
        }
    }.toMap()
}

private fun FilterDefinition.readText(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateText>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = createText(Res.string.create_error_required)
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun FilterDefinition.readNumber(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateText>,
): Pair<String, CustomFilterPayloadValue>? {
    val raw = values[slug].asText().trim()
    if (raw.isBlank()) {
        if (isRequired) errors[slug] = createText(Res.string.create_error_required)
        return null
    }
    val number = raw.toDoubleOrNull()
    if (number == null) {
        errors[slug] = createText(Res.string.create_error_filter_number)
        return null
    }
    options.min?.let { min ->
        if (number < min) {
            errors[slug] = createText(Res.string.create_error_filter_number_min, min.formatForDisplay())
            return null
        }
    }
    options.max?.let { max ->
        if (number > max) {
            errors[slug] = createText(Res.string.create_error_filter_number_max, max.formatForDisplay())
            return null
        }
    }
    return slug to CustomFilterPayloadValue.Number(number)
}

private fun FilterDefinition.readBoolean(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateText>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = (values[slug] as? CreateFilterInput.BooleanValue)?.value
    if (value == null) {
        if (isRequired) errors[slug] = createText(Res.string.create_error_required)
        return null
    }
    return slug to CustomFilterPayloadValue.BooleanValue(value)
}

private fun FilterDefinition.readSelect(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateText>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = createText(Res.string.create_error_required)
        return null
    }
    if (options.values.isNotEmpty() && value !in options.values) {
        errors[slug] = createText(Res.string.create_error_filter_invalid_option)
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun CreateFilterInput?.asText(): String =
    (this as? CreateFilterInput.Text)?.value.orEmpty()
