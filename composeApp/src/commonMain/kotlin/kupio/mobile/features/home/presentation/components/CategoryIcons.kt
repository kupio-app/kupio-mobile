package kupio.mobile.features.home.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForCategorySlug(slug: String?): ImageVector {
    val s = slug?.lowercase().orEmpty()
    return when {
        "home" in s || "house" in s || "furniture" in s || "interior" in s -> Icons.Outlined.Home
        "fashion" in s || "cloth" in s || "apparel" in s || "wear" in s || "dress" in s -> Icons.Outlined.Checkroom
        "electron" in s || "tech" in s || "gadget" in s || "computer" in s || "laptop" in s -> Icons.Outlined.DevicesOther
        "auto" in s || "car" in s || "vehicle" in s || "motorbike" in s || "moto" in s -> Icons.Outlined.DirectionsCar
        "tv" in s || "television" in s || "media" in s -> Icons.Outlined.Tv
        "phone" in s || "mobile" in s -> Icons.Outlined.Smartphone
        "sport" in s || "fitness" in s -> Icons.Outlined.FitnessCenter
        "book" in s || "education" in s -> Icons.AutoMirrored.Outlined.MenuBook
        "toy" in s || "child" in s || "kid" in s -> Icons.Outlined.SmartToy
        else -> Icons.Outlined.Category
    }
}
