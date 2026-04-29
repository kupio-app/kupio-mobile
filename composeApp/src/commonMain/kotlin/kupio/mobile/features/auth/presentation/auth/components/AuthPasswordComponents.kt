package kupio.mobile.features.auth.presentation.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kupio.mobile.core.designsystem.KupioTextField
import kupio.mobile.core.designsystem.bouncingDimClickable
import mobile.composeapp.generated.resources.Res
import mobile.composeapp.generated.resources.auth_hide_password
import mobile.composeapp.generated.resources.auth_password_strength_fair
import mobile.composeapp.generated.resources.auth_password_strength_good
import mobile.composeapp.generated.resources.auth_password_strength_strong
import mobile.composeapp.generated.resources.auth_password_strength_weak
import mobile.composeapp.generated.resources.auth_show_password
import org.jetbrains.compose.resources.stringResource

@Composable
fun PasswordStrengthMeter(password: String) {
    val hasLower = password.any { it.isLowerCase() }
    val hasUpper = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }

    val varietyScore = listOf(hasLower, hasUpper, hasDigit, hasSymbol).count { it }
    val lengthScore = (password.length / 4).coerceAtMost(4)
    val totalScore = ((varietyScore + lengthScore) / 2).coerceIn(0, 4)

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..4) {
                val isActive = i <= totalScore
                val color by animateColorAsState(
                    if (isActive) {
                        when (totalScore) {
                            1, 2 -> MaterialTheme.colorScheme.error
                            3 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(300),
                    label = "strength_color_$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
        val strengthText = when (totalScore) {
            0, 1 -> stringResource(Res.string.auth_password_strength_weak)
            2 -> stringResource(Res.string.auth_password_strength_fair)
            3 -> stringResource(Res.string.auth_password_strength_good)
            else -> stringResource(Res.string.auth_password_strength_strong)
        }
        
        Text(
            text = strengthText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier.Companion,
    error: String? = null,
    required: Boolean = false,
    trailingLabel: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    KupioTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier,
        error = error,
        required = required,
        trailingLabel = trailingLabel,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingSlot = {
            Icon(
                imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (visible) stringResource(Res.string.auth_hide_password) else stringResource(Res.string.auth_show_password),
                modifier = Modifier
                    .size(20.dp)
                    .bouncingDimClickable { visible = !visible },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}