package utils.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Report: ImageVector
    get() {
        if (_Flag != null) return _Flag!!

        _Flag = ImageVector.Builder(
            name = "Flag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000))
            ) {
                moveTo(200f, 840f)
                verticalLineToRelative(-680f)
                horizontalLineToRelative(360f)
                lineToRelative(16f, 80f)
                horizontalLineToRelative(224f)
                verticalLineToRelative(400f)
                horizontalLineTo(520f)
                lineToRelative(-16f, -80f)
                horizontalLineTo(280f)
                verticalLineToRelative(280f)
                close()
                moveToRelative(386f, -280f)
                horizontalLineToRelative(134f)
                verticalLineToRelative(-240f)
                horizontalLineTo(510f)
                lineToRelative(-16f, -80f)
                horizontalLineTo(280f)
                verticalLineToRelative(240f)
                horizontalLineToRelative(290f)
                close()
            }
        }.build()

        return _Flag!!
    }

private var _Flag: ImageVector? = null

