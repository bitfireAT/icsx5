/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Icons.Rounded.ModeCoolOff: ImageVector
    get() {
        if (_ModeCoolOff != null) {
            return _ModeCoolOff!!
        }
        _ModeCoolOff = ImageVector.Builder(
            name = "Rounded.ModeCoolOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(520f, 713f)
                verticalLineToRelative(127f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(480f, 880f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(440f, 840f)
                verticalLineToRelative(-126f)
                lineTo(338f, 815f)
                quadToRelative(-12f, 11f, -28.5f, 10.5f)
                reflectiveQuadTo(282f, 814f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                lineToRelative(141f, -142f)
                lineToRelative(-80f, -80f)
                lineToRelative(-141f, 142f)
                quadToRelative(-11f, 11f, -27.5f, 11.5f)
                reflectiveQuadTo(146f, 678f)
                quadToRelative(-11f, -11f, -11f, -28f)
                reflectiveQuadToRelative(11f, -28f)
                lineToRelative(100f, -102f)
                lineTo(120f, 520f)
                quadToRelative(-17f, 0f, -28.5f, -11.5f)
                reflectiveQuadTo(80f, 480f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(120f, 440f)
                horizontalLineToRelative(127f)
                lineTo(59f, 252f)
                quadToRelative(-12f, -12f, -11.5f, -28.5f)
                reflectiveQuadTo(60f, 195f)
                quadToRelative(12f, -12f, 28.5f, -12f)
                reflectiveQuadToRelative(28.5f, 12f)
                lineTo(796f, 875f)
                quadToRelative(12f, 12f, 12f, 28f)
                reflectiveQuadToRelative(-12f, 28f)
                quadToRelative(-12f, 12f, -28.5f, 12f)
                reflectiveQuadTo(739f, 931f)
                lineTo(520f, 713f)
                close()
                moveTo(600f, 520f)
                horizontalLineToRelative(-45f)
                lineTo(440f, 405f)
                verticalLineToRelative(-45f)
                lineTo(282f, 202f)
                quadToRelative(-11f, -11f, -11.5f, -27.5f)
                reflectiveQuadTo(282f, 146f)
                quadToRelative(11f, -11f, 28f, -11f)
                reflectiveQuadToRelative(28f, 11f)
                lineToRelative(102f, 100f)
                verticalLineToRelative(-126f)
                quadToRelative(0f, -17f, 11.5f, -28.5f)
                reflectiveQuadTo(480f, 80f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(520f, 120f)
                verticalLineToRelative(126f)
                lineToRelative(102f, -101f)
                quadToRelative(12f, -11f, 28.5f, -10.5f)
                reflectiveQuadTo(678f, 146f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                lineTo(520f, 360f)
                verticalLineToRelative(80f)
                horizontalLineToRelative(80f)
                lineToRelative(158f, -158f)
                quadToRelative(11f, -11f, 27.5f, -11.5f)
                reflectiveQuadTo(814f, 282f)
                quadToRelative(11f, 11f, 11f, 28f)
                reflectiveQuadToRelative(-11f, 28f)
                lineTo(714f, 440f)
                horizontalLineToRelative(126f)
                quadToRelative(17f, 0f, 28.5f, 11.5f)
                reflectiveQuadTo(880f, 480f)
                quadToRelative(0f, 17f, -11.5f, 28.5f)
                reflectiveQuadTo(840f, 520f)
                lineTo(714f, 520f)
                lineToRelative(101f, 102f)
                quadToRelative(6f, 6f, 8.5f, 13.5f)
                reflectiveQuadToRelative(2.5f, 15f)
                quadToRelative(0f, 7.5f, -3f, 14.5f)
                reflectiveQuadToRelative(-9f, 13f)
                quadToRelative(-11f, 11f, -28f, 11f)
                reflectiveQuadToRelative(-28f, -11f)
                lineTo(600f, 520f)
                close()
            }
        }.build()

        return _ModeCoolOff!!
    }

@Suppress("ObjectPropertyName")
private var _ModeCoolOff: ImageVector? = null
