package kupio.mobile.features.listings.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import kupio.mobile.core.designsystem.KupioDefaultButton
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.features.listings.presentation.detail.ListingDetailIntent
import kupio.mobile.features.listings.presentation.detail.ListingDetailState
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.listing_detail_cancel
import mobile.composeapp.generated.resources.listing_detail_message_empty
import mobile.composeapp.generated.resources.listing_detail_message_placeholder
import mobile.composeapp.generated.resources.listing_detail_message_title
import mobile.composeapp.generated.resources.listing_detail_send
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageSellerSheet(
    state: ListingDetailState,
    onIntent: (ListingDetailIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isSendingMessage) onIntent(ListingDetailIntent.CloseMessageSheet)
        },
        sheetState = sheetState,
        shape = KupioShapes.ExtraLarge,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        val emptyMessage = stringResource(Res.string.listing_detail_message_empty)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = KupioThemeDefaults.spacing.lg)
                .padding(bottom = KupioThemeDefaults.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.md),
        ) {
            Text(
                text = stringResource(Res.string.listing_detail_message_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            KupioTextField(
                value = state.messageDraft,
                onValueChange = { onIntent(ListingDetailIntent.MessageChanged(it)) },
                label = stringResource(Res.string.listing_detail_message_title),
                placeholder = stringResource(Res.string.listing_detail_message_placeholder),
                error = state.messageError?.ifBlank { emptyMessage },
                singleLine = false,
                minLines = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(KupioThemeDefaults.spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onIntent(ListingDetailIntent.CloseMessageSheet) },
                    enabled = !state.isSendingMessage,
                ) {
                    Text(stringResource(Res.string.listing_detail_cancel))
                }
                KupioDefaultButton(
                    text = stringResource(Res.string.listing_detail_send),
                    modifier = Modifier.weight(1f),
                    onClick = { onIntent(ListingDetailIntent.SendMessage) },
                    enabled = !state.isSendingMessage,
                    loading = state.isSendingMessage,
                )
            }
        }
    }
}
