package kupio.mobile.features.listings.presentation.create

import kupio.mobile.core.network.ApiException

sealed interface CreateError {
    data class TitleTooShort(val min: Int) : CreateError
    data class TitleTooLong(val max: Int) : CreateError
    data class DescriptionTooShort(val min: Int) : CreateError
    data class DescriptionTooLong(val max: Int) : CreateError
    data object CategoryRequired : CreateError
    data object PriceNotNumber : CreateError
    data object PriceNegative : CreateError
    data class PriceTooHigh(val max: Int) : CreateError
    data object Required : CreateError
    data object FilterNotNumber : CreateError
    data class FilterNumberTooSmall(val min: String) : CreateError
    data class FilterNumberTooLarge(val max: String) : CreateError
    data object FilterInvalidOption : CreateError
    data class ImageLimitReached(val max: Int) : CreateError
    data object UnsupportedImage : CreateError
    data object Generic : CreateError
    data class ServerMessage(val message: String) : CreateError
}

internal fun Throwable.fieldErrors(): Map<CreateField, CreateError> {
    val apiException = this as? ApiException ?: return emptyMap()
    return apiException.fieldErrors.mapNotNull { error ->
        val field = when (error.field) {
            "title" -> CreateField.TITLE
            "description" -> CreateField.DESCRIPTION
            "price" -> CreateField.PRICE
            "category_id" -> CreateField.CATEGORY
            "custom_filters" -> CreateField.CUSTOM_FILTERS
            else -> null
        }
        field?.let { it to CreateError.ServerMessage(error.message) }
    }.toMap()
}
