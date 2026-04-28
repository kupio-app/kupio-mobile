package kupio.mobile.features.listings.presentation.create

import androidx.compose.runtime.Composable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_error_category_required
import mobile.composeapp.generated.resources.create_error_description_too_long
import mobile.composeapp.generated.resources.create_error_description_too_short
import mobile.composeapp.generated.resources.create_error_filter_invalid_option
import mobile.composeapp.generated.resources.create_error_filter_number
import mobile.composeapp.generated.resources.create_error_filter_number_max
import mobile.composeapp.generated.resources.create_error_filter_number_min
import mobile.composeapp.generated.resources.create_error_generic
import mobile.composeapp.generated.resources.create_error_image_limit
import mobile.composeapp.generated.resources.create_error_price_negative
import mobile.composeapp.generated.resources.create_error_price_not_number
import mobile.composeapp.generated.resources.create_error_price_too_high
import mobile.composeapp.generated.resources.create_error_required
import mobile.composeapp.generated.resources.create_error_title_too_long
import mobile.composeapp.generated.resources.create_error_title_too_short
import mobile.composeapp.generated.resources.create_error_unsupported_image
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreateError.toErrorMessage(): String = when (this) {
    is CreateError.TitleTooShort -> stringResource(Res.string.create_error_title_too_short, min)
    is CreateError.TitleTooLong -> stringResource(Res.string.create_error_title_too_long, max)
    is CreateError.DescriptionTooShort -> stringResource(Res.string.create_error_description_too_short, min)
    is CreateError.DescriptionTooLong -> stringResource(Res.string.create_error_description_too_long, max)
    CreateError.CategoryRequired -> stringResource(Res.string.create_error_category_required)
    CreateError.PriceNotNumber -> stringResource(Res.string.create_error_price_not_number)
    CreateError.PriceNegative -> stringResource(Res.string.create_error_price_negative)
    is CreateError.PriceTooHigh -> stringResource(Res.string.create_error_price_too_high, max)
    CreateError.Required -> stringResource(Res.string.create_error_required)
    CreateError.FilterNotNumber -> stringResource(Res.string.create_error_filter_number)
    is CreateError.FilterNumberTooSmall -> stringResource(Res.string.create_error_filter_number_min, min)
    is CreateError.FilterNumberTooLarge -> stringResource(Res.string.create_error_filter_number_max, max)
    CreateError.FilterInvalidOption -> stringResource(Res.string.create_error_filter_invalid_option)
    is CreateError.ImageLimitReached -> stringResource(Res.string.create_error_image_limit, max)
    CreateError.UnsupportedImage -> stringResource(Res.string.create_error_unsupported_image)
    CreateError.Generic -> stringResource(Res.string.create_error_generic)
    is CreateError.ServerMessage -> message
}
