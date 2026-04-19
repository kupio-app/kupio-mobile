package kupio.mobile.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kupio.mobile.core.designsystem.KupioButton
import kupio.mobile.core.designsystem.KupioScaffold
import kupio.mobile.core.designsystem.KupioText
import kupio.mobile.core.designsystem.KupioThemeDefaults

@Composable
fun HomeScreen() {
    KupioScaffold(title = "Kupio Starter") {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            KupioText(
                text = "This starter defines the shared app shell and the initial package structure.",
            )
            KupioText(
                text = "Navigation, DI, DataStore, and localization are wired in the next commits.",
            )
            KupioButton(
                text = "Starter home",
                onClick = { },
            )
        }
    }
}

// TODO: Replace this placeholder home screen with the real feature entry once app navigation is in place.
