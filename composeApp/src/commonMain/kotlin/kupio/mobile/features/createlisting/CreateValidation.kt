package kupio.mobile.features.createlisting

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
    val fieldErrors: Map<CreateField, String>,
    val filterErrors: Map<String, String>,
) {
    val isValid: Boolean = listing != null
}

fun validateCreateListing(state: CreateState): CreateValidationResult {
    val fieldErrors = mutableMapOf<CreateField, String>()
    val filterErrors = mutableMapOf<String, String>()

    val title = state.title.trim()
    if (title.length < TitleMinLength) {
        fieldErrors[CreateField.TITLE] = "Title must have at least $TitleMinLength characters."
    } else if (title.length > TitleMaxLength) {
        fieldErrors[CreateField.TITLE] = "Title must have at most $TitleMaxLength characters."
    }

    val description = state.description.trim()
    if (description.length < DescriptionMinLength) {
        fieldErrors[CreateField.DESCRIPTION] =
            "Description must have at least $DescriptionMinLength characters."
    } else if (description.length > DescriptionMaxLength) {
        fieldErrors[CreateField.DESCRIPTION] =
            "Description must have at most $DescriptionMaxLength characters."
    }

    val categoryId = state.selectedCategoryId
    if (categoryId == null) {
        fieldErrors[CreateField.CATEGORY] = "Choose a category."
    }

    val price = if (state.isFree) {
        0
    } else {
        state.price.trim().toIntOrNull()
    }
    if (price == null) {
        fieldErrors[CreateField.PRICE] = "Enter a whole-number price."
    } else if (price < 0) {
        fieldErrors[CreateField.PRICE] = "Price cannot be negative."
    } else if (price >= MaxPriceExclusive) {
        fieldErrors[CreateField.PRICE] = "Price must be less than $MaxPriceExclusive."
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
    errors: MutableMap<String, String>,
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
    errors: MutableMap<String, String>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = "Required."
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun FilterDefinition.readNumber(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, String>,
): Pair<String, CustomFilterPayloadValue>? {
    val raw = values[slug].asText().trim()
    if (raw.isBlank()) {
        if (isRequired) errors[slug] = "Required."
        return null
    }
    val number = raw.toDoubleOrNull()
    if (number == null) {
        errors[slug] = "Enter a number."
        return null
    }
    options.min?.let { min ->
        if (number < min) {
            errors[slug] = "Must be at least ${min.formatForDisplay()}."
            return null
        }
    }
    options.max?.let { max ->
        if (number > max) {
            errors[slug] = "Must be at most ${max.formatForDisplay()}."
            return null
        }
    }
    return slug to CustomFilterPayloadValue.Number(number)
}

private fun FilterDefinition.readBoolean(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, String>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = (values[slug] as? CreateFilterInput.BooleanValue)?.value
    if (value == null) {
        if (isRequired) errors[slug] = "Required."
        return null
    }
    return slug to CustomFilterPayloadValue.BooleanValue(value)
}

private fun FilterDefinition.readSelect(
    values: Map<String, CreateFilterInput>,
    errors: MutableMap<String, String>,
): Pair<String, CustomFilterPayloadValue>? {
    val value = values[slug].asText().trim()
    if (value.isBlank()) {
        if (isRequired) errors[slug] = "Required."
        return null
    }
    if (options.values.isNotEmpty() && value !in options.values) {
        errors[slug] = "Choose one of the available options."
        return null
    }
    return slug to CustomFilterPayloadValue.Text(value)
}

private fun CreateFilterInput?.asText(): String =
    (this as? CreateFilterInput.Text)?.value.orEmpty()

private fun Double.formatForDisplay(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
