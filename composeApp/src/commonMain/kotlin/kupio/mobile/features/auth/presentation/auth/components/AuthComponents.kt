package kupio.mobile.features.auth.presentation.auth.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import kupio.mobile.core.designsystem.KupioCardSurface
import kupio.mobile.features.auth.presentation.auth.AuthMode
import kotlin.math.roundToInt

data class AuthLayoutMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val contentMinHeight: Dp,
    val topSpacer: Dp,
    val bottomSpacer: Dp,
    val backdropHeight: Dp,
    val backdropCornerRadius: Dp,
    val topOrbSize: Dp,
    val topOrbTopPadding: Dp,
    val topOrbEndPadding: Dp,
    val sideTileWidth: Dp,
    val sideTileHeight: Dp,
    val sideTileTopPadding: Dp,
    val sideTileStartPadding: Dp,
    val cardPadding: Dp,
    val sectionSpacing: Dp,
    val modeSelectorHeight: Dp,
    val modeSelectorCornerRadius: Dp,
)

private fun authLayoutMetrics(
    width: Dp,
    height: Dp,
): AuthLayoutMetrics {
    val widthValue = width.value
    val heightValue = height.value

    fun scaledWidthDp(factor: Float, min: Int, max: Int) =
        (widthValue * factor).roundToInt().dp.coerceIn(min.dp, max.dp)

    fun scaledHeightDp(factor: Float, min: Int, max: Int) =
        (heightValue * factor).roundToInt().dp.coerceIn(min.dp, max.dp)

    return AuthLayoutMetrics(
        horizontalPadding = scaledWidthDp(factor = 0.06f, min = 18, max = 28),
        verticalPadding = scaledHeightDp(factor = 0.035f, min = 20, max = 32),
        contentMinHeight = (height - scaledHeightDp(factor = 0.035f, min = 20, max = 32) * 2)
            .coerceAtLeast(0.dp),
        topSpacer = scaledHeightDp(factor = 0.02f, min = 10, max = 24),
        bottomSpacer = scaledHeightDp(factor = 0.05f, min = 32, max = 56),
        backdropHeight = scaledHeightDp(factor = 0.34f, min = 280, max = 360),
        backdropCornerRadius = scaledWidthDp(factor = 0.12f, min = 40, max = 56),
        topOrbSize = scaledWidthDp(factor = 0.30f, min = 92, max = 132),
        topOrbTopPadding = scaledHeightDp(factor = 0.04f, min = 24, max = 40),
        topOrbEndPadding = scaledWidthDp(factor = 0.07f, min = 20, max = 32),
        sideTileWidth = scaledWidthDp(factor = 0.24f, min = 76, max = 104),
        sideTileHeight = scaledWidthDp(factor = 0.24f, min = 76, max = 104),
        sideTileTopPadding = scaledHeightDp(factor = 0.14f, min = 96, max = 126),
        sideTileStartPadding = scaledWidthDp(factor = 0.05f, min = 14, max = 22),
        cardPadding = scaledWidthDp(factor = 0.055f, min = 20, max = 28),
        sectionSpacing = scaledHeightDp(factor = 0.018f, min = 14, max = 20),
        modeSelectorHeight = scaledHeightDp(factor = 0.064f, min = 48, max = 56),
        modeSelectorCornerRadius = scaledWidthDp(factor = 0.055f, min = 18, max = 24),
    )
}

@Composable
fun AuthViewport(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(AuthLayoutMetrics) -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val metrics = authLayoutMetrics(
            width = maxWidth,
            height = maxHeight,
        )

        AuthBackdrop(metrics = metrics)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = metrics.horizontalPadding)
                .padding(vertical = metrics.verticalPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(metrics.topSpacer))
            content(metrics)
            Spacer(modifier = Modifier.height(metrics.bottomSpacer))
        }
    }
}

@Composable
private fun AuthBackdrop(
    metrics: AuthLayoutMetrics,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.backdropHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = metrics.backdropCornerRadius,
                    bottomEnd = metrics.backdropCornerRadius,
                ),
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = metrics.topOrbTopPadding,
                    end = metrics.topOrbEndPadding,
                )
                .size(metrics.topOrbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = metrics.sideTileTopPadding,
                    start = metrics.sideTileStartPadding,
                )
                .size(
                    width = metrics.sideTileWidth,
                    height = metrics.sideTileHeight,
                )
                .clip(RoundedCornerShape(metrics.modeSelectorCornerRadius + 6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
        )
    }
}

@Composable
fun AuthShell(
    metrics: AuthLayoutMetrics,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var shellHeightPx by remember { mutableIntStateOf(0) }
    var cardBlockHeightPx by remember { mutableIntStateOf(0) }
    val cardTopSpacer = centeredCardSpacer(
        shellHeightPx = shellHeightPx,
        cardBlockHeightPx = cardBlockHeightPx,
        minimum = metrics.sectionSpacing,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = metrics.contentMinHeight)
            .onSizeChanged { size -> shellHeightPx = size.height },
    ) {
        Spacer(modifier = Modifier.height(cardTopSpacer))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> cardBlockHeightPx = size.height },
            verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing + 2.dp),
        ) {
            AuthHero()
            KupioCardSurface(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.92f,
                                stiffness = 520f,
                            ),
                        )
                        .padding(metrics.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(metrics.sectionSpacing),
                    content = content,
                )
            }
            if (footer != null) {
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun centeredCardSpacer(
    shellHeightPx: Int,
    cardBlockHeightPx: Int,
    minimum: Dp,
): Dp {
    val density = androidx.compose.ui.platform.LocalDensity.current
    if (shellHeightPx == 0 || cardBlockHeightPx == 0) {
        return minimum
    }

    val centeredTopPx = (shellHeightPx - cardBlockHeightPx) / 2
    return with(density) {
        centeredTopPx.coerceAtLeast(minimum.roundToPx()).toDp()
    }
}

@Composable
private fun AuthHero() {
    Text(
        text = "Kupio",
        style = MaterialTheme.typography.displaySmall.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun AuthModeSelector(
    selectedMode: AuthMode,
    loginLabel: String,
    registerLabel: String,
    metrics: AuthLayoutMetrics,
    onModeSelected: (AuthMode) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.modeSelectorCornerRadius + 2.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(4.dp),
    ) {
        val layoutDirection = LocalLayoutDirection.current
        val segmentSpacing = 6.dp
        val segmentWidth = (maxWidth - segmentSpacing) / 2
        val selectedSegmentIndex = when (layoutDirection) {
            LayoutDirection.Ltr -> if (selectedMode == AuthMode.LOGIN) 0 else 1
            LayoutDirection.Rtl -> if (selectedMode == AuthMode.LOGIN) 1 else 0
        }
        val indicatorOffset by animateDpAsState(
            targetValue = (segmentWidth + segmentSpacing) * selectedSegmentIndex,
            animationSpec = spring(
                dampingRatio = 0.88f,
                stiffness = 480f,
            ),
            label = "authModeIndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .height(metrics.modeSelectorHeight)
                .clip(RoundedCornerShape(metrics.modeSelectorCornerRadius))
                .background(MaterialTheme.colorScheme.surface),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(segmentSpacing),
        ) {
            AuthModeSegment(
                modifier = Modifier.weight(1f),
                text = loginLabel,
                selected = selectedMode == AuthMode.LOGIN,
                height = metrics.modeSelectorHeight,
                cornerRadius = metrics.modeSelectorCornerRadius,
                onClick = { onModeSelected(AuthMode.LOGIN) },
            )
            AuthModeSegment(
                modifier = Modifier.weight(1f),
                text = registerLabel,
                selected = selectedMode == AuthMode.REGISTER,
                height = metrics.modeSelectorHeight,
                cornerRadius = metrics.modeSelectorCornerRadius,
                onClick = { onModeSelected(AuthMode.REGISTER) },
            )
        }
    }
}

@Composable
private fun AuthModeSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    height: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val textAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = 0.9f,
            stiffness = 520f,
        ),
        label = "authModeTextAlpha",
    )
    val buttonColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = Color.Transparent,
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        disabledContainerColor = Color.Transparent,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        colors = buttonColors,
        shape = RoundedCornerShape(cornerRadius),
        elevation = null,
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.run {
                if (selected) {
                    onSurface.copy(alpha = textAlpha)
                } else {
                    onSurfaceVariant.copy(alpha = textAlpha)
                }
            },
        )
    }
}

@Composable
fun AuthInlineError(
    message: String?,
) {
    if (message == null) return

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun AuthCardHeader(
    title: String,
    supporting: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
