package kupio.mobile.features.home.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.home_headline_italic
import mobile.composeapp.generated.resources.home_headline_primary
import org.jetbrains.compose.resources.stringResource

@Composable
fun AdvertisementHeadline(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.home_headline_primary),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(Res.string.home_headline_italic),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
