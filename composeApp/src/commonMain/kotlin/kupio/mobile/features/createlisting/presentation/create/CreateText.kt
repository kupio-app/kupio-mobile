package kupio.mobile.features.createlisting.presentation.create

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed interface CreateText {
    data class Resource(
        val resource: StringResource,
        val args: List<Any> = emptyList(),
    ) : CreateText

    data class Dynamic(val value: String) : CreateText
}

internal fun createText(
    resource: StringResource,
    vararg args: Any,
): CreateText = CreateText.Resource(resource, args.toList())

@Composable
internal fun CreateText.asString(): String =
    when (this) {
        is CreateText.Dynamic -> value
        is CreateText.Resource -> stringResource(resource, *args.toTypedArray())
    }
