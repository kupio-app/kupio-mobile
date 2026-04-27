package kupio.mobile.features.createlisting.presentation.create

import kupio.mobile.features.listings.domain.model.FilterDefinition

internal fun String.digitsOnly(): String = filter { it.isDigit() }

internal fun String.numericText(): String = filterIndexed { index, char ->
    char.isDigit() || char == '.' || (char == '-' && index == 0)
}

internal fun FilterDefinition.numberPlaceholder(): String {
    val min = options.min?.formatForDisplay()
    val max = options.max?.formatForDisplay()
    return when {
        min != null && max != null -> "$min - $max"
        min != null -> "At least $min"
        max != null -> "Up to $max"
        else -> label
    }
}

internal fun CreateState.categoryDisplayText(): String =
    when {
        categoryPath.isNotEmpty() -> categoryPath.joinToString(" / ") { it.name }
        selectedCategoryName != null -> selectedCategoryName
        else -> "Choose category"
    }

internal fun Double.formatForDisplay(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
