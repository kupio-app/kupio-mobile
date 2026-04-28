package kupio.mobile.features.createlisting.presentation.create

internal fun String.digitsOnly(): String = filter { it.isDigit() }

internal fun String.numericText(): String = filterIndexed { index, char ->
    char.isDigit() || char == '.' || (char == '-' && index == 0)
}

internal fun Double.formatForDisplay(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
