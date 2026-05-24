package kupio.mobile.features.payments.presentation.topup

import androidx.compose.runtime.Composable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.topup_error_generic
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TopUpError.toErrorMessage(): String = when (this) {
    TopUpError.Generic -> stringResource(Res.string.topup_error_generic)
}
