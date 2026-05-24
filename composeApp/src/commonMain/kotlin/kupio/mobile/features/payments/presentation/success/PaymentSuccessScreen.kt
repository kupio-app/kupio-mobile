package kupio.mobile.features.payments.presentation.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kupio.mobile.core.designsystem.KupioShapes
import kupio.mobile.core.designsystem.KupioThemeDefaults
import kupio.mobile.core.presentation.CollectEffect
import kupio.mobile.features.me.presentation.profile.components.formatBalance
import kupio.mobile.features.payments.presentation.history.PaymentsHistoryScreen
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.payment_success_done
import mobile.composeapp.generated.resources.payment_success_new_balance
import mobile.composeapp.generated.resources.payment_success_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PaymentSuccessScreen(private val amountCents: Int) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinViewModel<PaymentSuccessViewModel> { parametersOf(amountCents) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        CollectEffect(viewModel.effects) { effect ->
            when (effect) {
                PaymentSuccessEffect.GoToHistory -> {
                    navigator.popUntilRoot()
                    navigator.push(PaymentsHistoryScreen())
                }
            }
        }

        PaymentSuccessContent(state = state, onIntent = viewModel::onIntent)
    }
}

@Composable
private fun PaymentSuccessContent(state: PaymentSuccessState, onIntent: (PaymentSuccessIntent) -> Unit) {
    val spacing = KupioThemeDefaults.spacing
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = KupioThemeDefaults.strongBorder,
            ) {
                Button(
                    onClick = { onIntent(PaymentSuccessIntent.DoneClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                        .height(48.dp),
                    shape = KupioShapes.Medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.payment_success_done),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CheckmarkIcon()
                Spacer(modifier = Modifier.size(28.dp))
                Text(
                    text = stringResource(Res.string.payment_success_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "+${formatBalance(state.amountCents)}",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (state.newBalanceCents != null) {
                    Text(
                        text = stringResource(Res.string.payment_success_new_balance, formatBalance(state.newBalanceCents)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckmarkIcon() {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(108.dp)
            .drawBehind { drawHaloAndDots(primaryColor) },
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = primaryColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawHaloAndDots(primaryColor: Color) {
    drawCircle(color = primaryColor.copy(alpha = 0.08f))
    val dotRadius = 3.dp.toPx()
    val ringRadius = size.minDimension / 2f * 0.82f
    val angles = listOf(45.0, 135.0, 225.0, 315.0)
    angles.forEach { angleDeg ->
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        drawCircle(
            color = primaryColor.copy(alpha = 0.3f),
            radius = dotRadius,
            center = Offset(
                x = center.x + ringRadius * cos(angleRad),
                y = center.y + ringRadius * sin(angleRad),
            ),
        )
    }
}
