package kupio.mobile.features.home.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector

private val categoryIconMap: Map<String, ImageVector> = mapOf(
    "home" to Icons.Outlined.Home,
    "fashion" to Icons.Outlined.Checkroom,
    "electronics" to Icons.Outlined.DevicesOther,
    "automotive" to Icons.Outlined.DirectionsCar,
    "tv" to Icons.Outlined.Tv,
    "phones" to Icons.Outlined.Smartphone,
    "sports" to Icons.Outlined.FitnessCenter,
    "books" to Icons.AutoMirrored.Outlined.MenuBook,
    "toys" to Icons.Outlined.SmartToy,
)

fun iconForCategorySlug(slug: String?): ImageVector =
    categoryIconMap[slug] ?: Icons.Outlined.Category
