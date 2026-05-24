package kupio.mobile.features.payments.presentation.topup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kupio.mobile.core.platform.UrlOpener
import org.koin.compose.koinInject
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.designsystem.KupioTopBarBackAction
import kupio.mobile.core.designsystem.KupioTopNavbar
import kupio.mobile.core.designsystem.bouncingClickable
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.me.presentation.profile.components.formatBalance
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.topbar_back
import mobile.composeapp.generated.resources.topup_after_topup
import mobile.composeapp.generated.resources.topup_continue
import mobile.composeapp.generated.resources.topup_current_balance
import mobile.composeapp.generated.resources.topup_pick_one
import mobile.composeapp.generated.resources.topup_select_amount
import mobile.composeapp.generated.resources.topup_subtitle
import mobile.composeapp.generated.resources.topup_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AMOUNT_OPTIONS = listOf(1000, 2000, 5000, 10000, 20000)

class TopUpScreen(private val currentBalanceCents: Int) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val urlOpener = koinInject<UrlOpener>()
        val viewModel = koinViewModel<TopUpViewModel> { parametersOf(currentBalanceCents) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                TopUpEffect.NavigateBack -> navigator.pop()
                is TopUpEffect.OpenUrl -> urlOpener.openUrl(effect.url)
                is TopUpEffect.ShowError -> Unit
            }
        }

        TopUpContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun TopUpContent(state: TopUpState, onIntent: (TopUpIntent) -> Unit) {
    Scaffold(
        topBar = {
            KupioTopNavbar(
                title = stringResource(Res.string.topup_title),
                subtitle = stringResource(Res.string.topup_subtitle),
                leadingContent = {
                    KupioTopBarBackAction(
                        contentDescription = stringResource(Res.string.topbar_back),
                        onClick = { onIntent(TopUpIntent.BackClicked) },
                    )
                },
            )
        },
        bottomBar = {
            TopUpBottomBar(state = state, onIntent = onIntent)
        },
    ) { paddingValues ->
        val spacing = KupioThemeDefaults.spacing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(spacing.sm))
            BalancePreviewCard(
                currentBalanceCents = state.currentBalanceCents,
                selectedAmountCents = state.selectedAmountCents,
            )
            AmountSelector(
                selectedAmountCents = state.selectedAmountCents,
                onAmountSelected = { onIntent(TopUpIntent.AmountSelected(it)) },
            )
            Spacer(modifier = Modifier.height(spacing.sm))
        }
    }
}

@Composable
private fun BalancePreviewCard(currentBalanceCents: Int, selectedAmountCents: Int) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = KupioShapes.ExtraLarge,
        color = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = KupioShapes.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(Res.string.topup_current_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = formatBalance(currentBalanceCents),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(
                        Res.string.topup_after_topup,
                        formatBalance(currentBalanceCents + selectedAmountCents),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AmountSelector(selectedAmountCents: Int, onAmountSelected: (Int) -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.topup_select_amount),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.topup_pick_one),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AMOUNT_OPTIONS.chunked(3).forEach { rowAmounts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                rowAmounts.forEach { amountCents ->
                    AmountChip(
                        modifier = Modifier.weight(1f),
                        amountCents = amountCents,
                        isSelected = amountCents == selectedAmountCents,
                        onClick = { onAmountSelected(amountCents) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountChip(
    amountCents: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        modifier = modifier.bouncingClickable(onClick = onClick),
        shape = KupioShapes.Large,
        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else KupioThemeDefaults.defaultBorder,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = formatBalance(amountCents),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TopUpBottomBar(state: TopUpState, onIntent: (TopUpIntent) -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Surface(
        color = MaterialTheme.colorScheme.background,
        border = KupioThemeDefaults.strongBorder,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = { onIntent(TopUpIntent.ContinueClicked) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = KupioShapes.Medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                    disabledContentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                ),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.topup_continue, formatBalance(state.selectedAmountCents)),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
