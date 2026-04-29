package kupio.mobile.features.listings.presentation.create

import kupio.mobile.features.listings.domain.model.CreateListing
import kupio.mobile.features.listings.domain.model.CustomFilterPayloadValue
import kupio.mobile.features.listings.domain.model.FilterDefinition
import kupio.mobile.features.listings.domain.model.FilterType

private const val TitleMinLength = 8
private const val TitleMaxLength = 255
private const val DescriptionMinLength = 50
private const val DescriptionMaxLength = 5000
private const val MaxPriceExclusive = 10_000_000

data class CreateValidationResult(
    val listing: CreateListing?,
    val fieldErrors: Map<CreateField, CreateError>,
    val filterErrors: Map<String, CreateError>,
) {
    val isValid: Boolean = listing != null
}

fun validateCreateListing(state: CreateState): CreateValidationResult {
    val fieldErrors = mutableMapOf<CreateField, CreateError>()
    val filterErrors = mutableMapOf<String, CreateError>()

    val title = state.title.trim()
    if (title.length < TitleMinLength) {
        fieldErrors[CreateField.TITLE] = CreateError.TitleTooShort(TitleMinLength)
    } else if (title.length > TitleMaxLength) {
        fieldErrors[CreateField.TITLE] = CreateError.TitleTooLong(TitleMaxLength)
    }

    val description = state.description.trim()
    if (description.length < DescriptionMinLength) {
        fieldErrors[CreateField.DESCRIPTION] = CreateError.DescriptionTooShort(DescriptionMinLength)
    } else if (description.length > DescriptionMaxLength) {
        fieldErrors[CreateField.DESCRIPTION] = CreateError.DescriptionTooLong(DescriptionMaxLength)
    }

    val categoryId = state.selectedCategoryId
    if (categoryId == null) {
        fieldErrors[CreateField.CATEGORY] = CreateError.CategoryRequired
    }

    val price = if (state.isFree) {
        0
    } else {
        state.price.trim().toIntOrNull()
    }
    if (price == null) {
        fieldErrors[CreateField.PRICE] = CreateError.PriceNotNumber
    } else if (price < 0) {
        fieldErrors[CreateField.PRICE] = CreateError.PriceNegative
    } else if (price >= MaxPriceExclusive) {
        fieldErrors[CreateField.PRICE] = CreateError.PriceTooHigh(MaxPriceExclusive)
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
    errors: MutableMap<String, CreateError>,
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
    errors: MutableMap<String, CreateError>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = CreateError.Required
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun FilterDefinition.readNumber(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateError>,
): Pair<String, CustomFilterPayloadValue>? {
    val raw = values[slug].asText().trim()
    if (raw.isBlank()) {
        if (isRequired) errors[slug] = CreateError.Required
        return null
    }
    val number = raw.toDoubleOrNull()
    if (number == null) {
        errors[slug] = CreateError.FilterNotNumber
        return null
    }
    options.min?.let { min ->
        if (number < min) {
            errors[slug] = CreateError.FilterNumberTooSmall(min.formatForDisplay())
            return null
        }
    }
    options.max?.let { max ->
        if (number > max) {
            errors[slug] = CreateError.FilterNumberTooLarge(max.formatForDisplay())
            return null
        }
    }
    return slug to CustomFilterPayloadValue.Number(number)
}

private fun FilterDefinition.readBoolean(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateError>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = (values[slug] as? CreateFilterInput.BooleanValue)?.value
    if (value == null) {
        if (isRequired) errors[slug] = CreateError.Required
        return null
    }
    return slug to CustomFilterPayloadValue.BooleanValue(value)
}

private fun FilterDefinition.readSelect(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, CreateError>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = CreateError.Required
        return null
    }
    if (options.values.isNotEmpty() && value !in options.values) {
        errors[slug] = CreateError.FilterInvalidOption
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun CreateFilterInput?.asText(): String =
    (this as? CreateFilterInput.Text)?.value.orEmpty()
