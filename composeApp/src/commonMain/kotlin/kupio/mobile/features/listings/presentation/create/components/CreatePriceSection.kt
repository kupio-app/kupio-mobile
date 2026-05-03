package kupio.mobile.features.listings.presentation.create.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kupio.mobile.core.designsystem.KupioErrorText
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.bouncingDimClickable
import kupio.mobile.features.listings.presentation.create.CreateError
import kupio.mobile.features.listings.presentation.create.CreateField
import kupio.mobile.features.listings.presentation.create.CreateIntent
import kupio.mobile.features.listings.presentation.create.CreateState
import kupio.mobile.features.listings.presentation.create.toErrorMessage
import kupio.mobile.features.listings.domain.model.Currency
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.create_free_subtitle
import mobile.composeapp.generated.resources.create_free_title
import mobile.composeapp.generated.resources.create_open_trades_subtitle
import mobile.composeapp.generated.resources.create_open_trades_title
import mobile.composeapp.generated.resources.create_price
import mobile.composeapp.generated.resources.create_publish
import mobile.composeapp.generated.resources.create_save_draft
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PriceSection(
    state: CreateState,
    onIntent: (CreateIntent) -> Unit,
) {
    FormSection(title = stringResource(Res.string.create_price)) {
        CreatePriceField(
            price = state.price,
            currency = state.currency,
            error = state.fieldErrors[CreateField.PRICE],
            enabled = !state.isFree,
            onPriceChange = { onIntent(CreateIntent.PriceChanged(it.digitsOnly())) },
            onCurrencySelect = { onIntent(CreateIntent.CurrencyChanged(it)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(
                checked = state.isFree,
                title = stringResource(Res.string.create_free_title),
                subtitle = stringResource(Res.string.create_free_subtitle),
                onClick = { onIntent(CreateIntent.ToggleFree) },
                modifier = Modifier.weight(1f),
            )
            ToggleRow(
                checked = state.isTradable,
                title = stringResource(Res.string.create_open_trades_title),
                subtitle = stringResource(Res.string.create_open_trades_subtitle),
                onClick = { onIntent(CreateIntent.ToggleTradable) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CreatePriceField(
    price: String,
    currency: Currency,
    error: CreateError?,
    enabled: Boolean,
    onPriceChange: (String) -> Unit,
    onCurrencySelect: (Currency) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.Medium,
        color = MaterialTheme.colorScheme.surface,
        border = if (error == null) {
            KupioThemeDefaults.strongBorder
        } else {
            BorderStroke(
                width = KupioThemeDefaults.borderWidths.regular,
                color = MaterialTheme.colorScheme.error,
            )
        },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyMenu(
                selected = currency,
                onSelect = onCurrencySelect,
            )
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = { Text("0") },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.6).sp,
                ),
                colors = priceTextFieldColors(),
                shape = KupioShapes.Medium,
            )
        }
    }
    error?.let { KupioErrorText(it.toErrorMessage()) }
}

@Composable
private fun priceTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    disabledBorderColor = Color.Transparent,
    errorBorderColor = Color.Transparent,
    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline,
    disabledPlaceholderColor = MaterialTheme.colorScheme.outline,
)

@Composable
private fun CurrencyMenu(
    selected: Currency,
    onSelect: (Currency) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .bouncingDimClickable(shape = KupioShapes.Medium) { expanded = true },
            shape = KupioShapes.Medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = KupioThemeDefaults.strongBorder,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = selected.symbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency.name) },
                    leadingIcon = {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(currency)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    checked: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .bouncingDimClickable(shape = KupioShapes.Medium, onClick = onClick),
        color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (checked) {
            BorderStroke(
                width = KupioThemeDefaults.borderWidths.regular,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            )
        } else {
            KupioThemeDefaults.strongBorder
        },
        shape = KupioShapes.Medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(KupioShapes.Micro)
                    .border(
                        if (checked) {
                            BorderStroke(
                                width = KupioThemeDefaults.borderWidths.regular,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            KupioThemeDefaults.strongBorder
                        },
                        KupioShapes.Micro,
                    )
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
internal fun PublishBar(
    state: CreateState,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.strongBorder,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 10.dp, end = 18.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onSaveDraft,
                enabled = !state.isSubmitting,
                modifier = Modifier.height(48.dp),
                shape = KupioShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.outline,
                ),
                border = KupioThemeDefaults.strongBorder,
            ) {
                Text(stringResource(Res.string.create_save_draft))
            }
            Button(
                onClick = onPublish,
                enabled = state.canSubmit && !state.isSubmitting,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = KupioShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(Res.string.create_publish))
                }
            }
        }
    }
}

internal fun String.digitsOnly(): String = filter { it.isDigit() }