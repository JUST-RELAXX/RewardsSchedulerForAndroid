package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Built-in vector icons mirroring the PC Electron app's Feather SVGs.
 * Zero external dependency on material-icons-core.
 */
object AppIcons {

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 4f)
                lineTo(20f, 12f)
                lineTo(7f, 20f)
                close()
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(10f, 17f)
                lineTo(19f, 7f)
            }
        }.build()
    }

    val Refresh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.5f, 2f)
                lineTo(21.5f, 8f)
                lineTo(15.5f, 8f)
                moveTo(2.5f, 22f)
                lineTo(2.5f, 16f)
                lineTo(8.5f, 16f)
                moveTo(2f, 12f)
                curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f)
                curveTo(16.5f, 2f, 20.2f, 5f, 21.5f, 8f)
                moveTo(22f, 12f)
                curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
                curveTo(7.5f, 22f, 3.8f, 19f, 2.5f, 16f)
            }
        }.build()
    }

    val RotateCcw: ImageVector by lazy {
        ImageVector.Builder(
            name = "RotateCcw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 4f)
                lineTo(1f, 10f)
                lineTo(7f, 10f)
                moveTo(3.51f, 15f)
                curveTo(4.7f, 18.5f, 8.1f, 21f, 12f, 21f)
                curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
                curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
                curveTo(9.5f, 3f, 7.2f, 4f, 5.6f, 5.6f)
                lineTo(1f, 10f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(6f, 18f)
                lineTo(18f, 6f)
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 12f)
                lineTo(5f, 12f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }.build()
    }

    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(19f, 12f)
                moveTo(12f, 5f)
                lineTo(19f, 12f)
                lineTo(12f, 19f)
            }
        }.build()
    }

    val Grid: ImageVector by lazy {
        ImageVector.Builder(
            name = "Grid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                moveTo(5f, 3f)
                lineTo(19f, 3f)
                curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                lineTo(21f, 19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                lineTo(5f, 21f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                lineTo(3f, 5f)
                curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f)
                close()
                // <line x1="3" y1="9" x2="21" y2="9"/>
                moveTo(3f, 9f)
                lineTo(21f, 9f)
                // <line x1="9" y1="21" x2="9" y2="9"/>
                moveTo(9f, 21f)
                lineTo(9f, 9f)
            }
        }.build()
    }

    val Terminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Terminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // <polyline points="4 17 10 11 4 5"/>
                moveTo(4f, 17f)
                lineTo(10f, 11f)
                lineTo(4f, 5f)
                // <line x1="12" y1="19" x2="20" y2="19"/>
                moveTo(12f, 19f)
                lineTo(20f, 19f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Circle in center
                moveTo(15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                close()
                // Surrounding gear notches
                moveTo(19.4f, 15f)
                lineTo(21f, 16.5f)
                lineTo(19.5f, 19f)
                lineTo(17.5f, 18.5f)
                moveTo(15f, 19.4f)
                lineTo(13.5f, 21f)
                lineTo(10.5f, 21f)
                lineTo(9.5f, 19.4f)
                moveTo(4.6f, 15f)
                lineTo(3f, 16.5f)
                lineTo(4.5f, 19f)
                lineTo(6.5f, 18.5f)
                moveTo(4.6f, 9f)
                lineTo(3f, 7.5f)
                lineTo(4.5f, 5f)
                lineTo(6.5f, 5.5f)
                moveTo(9.5f, 4.6f)
                lineTo(10.5f, 3f)
                lineTo(13.5f, 3f)
                lineTo(14.5f, 4.6f)
                moveTo(17.5f, 5.5f)
                lineTo(19.5f, 5f)
                lineTo(21f, 7.5f)
                lineTo(19.4f, 9f)
            }
        }.build()
    }

    val Sliders: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sliders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // <line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/>
                moveTo(4f, 21f); lineTo(4f, 14f)
                moveTo(4f, 10f); lineTo(4f, 3f)
                // <line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/>
                moveTo(12f, 21f); lineTo(12f, 12f)
                moveTo(12f, 8f); lineTo(12f, 3f)
                // <line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/>
                moveTo(20f, 21f); lineTo(20f, 16f)
                moveTo(20f, 12f); lineTo(20f, 3f)
                // Knobs
                moveTo(1f, 14f); lineTo(7f, 14f)
                moveTo(9f, 8f); lineTo(15f, 8f)
                moveTo(17f, 16f); lineTo(23f, 16f)
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                moveTo(21f, 15f)
                lineTo(21f, 19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                lineTo(5f, 21f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                lineTo(3f, 15f)
                // <polyline points="7 10 12 15 17 10"/>
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                // <line x1="12" y1="15" x2="12" y2="3"/>
                moveTo(12f, 15f)
                lineTo(12f, 3f)
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Two vertical bars
                moveTo(6f, 4f)
                lineTo(10f, 4f)
                lineTo(10f, 20f)
                lineTo(6f, 20f)
                close()
                moveTo(14f, 4f)
                lineTo(18f, 4f)
                lineTo(18f, 20f)
                lineTo(14f, 20f)
                close()
            }
        }.build()
    }

    val Square: ImageVector by lazy {
        ImageVector.Builder(
            name = "Square",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                close()
            }
        }.build()
    }

    val Flash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Flash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
                moveTo(13f, 2f)
                lineTo(3f, 14f)
                lineTo(12f, 14f)
                lineTo(11f, 22f)
                lineTo(21f, 10f)
                lineTo(12f, 10f)
                close()
            }
        }.build()
    }

    val Activity: ImageVector by lazy {
        ImageVector.Builder(
            name = "Activity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                moveTo(2f, 12f)
                lineTo(6f, 12f)
                lineTo(9f, 3f)
                lineTo(15f, 21f)
                lineTo(18f, 12f)
                lineTo(22f, 12f)
            }
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 8f)
                lineTo(12f, 8.5f)
                moveTo(12f, 12f)
                lineTo(12f, 16f)
            }
        }.build()
    }

    val MoreVertical: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVertical",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3.5f)
                curveTo(13.1f, 3.5f, 14f, 4.4f, 14f, 5.5f)
                curveTo(14f, 6.6f, 13.1f, 7.5f, 12f, 7.5f)
                curveTo(10.9f, 7.5f, 10f, 6.6f, 10f, 5.5f)
                curveTo(10f, 4.4f, 10.9f, 3.5f, 12f, 3.5f)
                close()
                moveTo(12f, 10.5f)
                curveTo(13.1f, 10.5f, 14f, 11.4f, 14f, 12.5f)
                curveTo(14f, 13.6f, 13.1f, 14.5f, 12f, 14.5f)
                curveTo(10.9f, 14.5f, 10f, 13.6f, 10f, 12.5f)
                curveTo(10f, 11.4f, 10.9f, 10.5f, 12f, 10.5f)
                close()
                moveTo(12f, 17.5f)
                curveTo(13.1f, 17.5f, 14f, 18.4f, 14f, 19.5f)
                curveTo(14f, 20.6f, 13.1f, 21.5f, 12f, 21.5f)
                curveTo(10.9f, 21.5f, 10f, 20.6f, 10f, 19.5f)
                curveTo(10f, 18.4f, 10.9f, 17.5f, 12f, 17.5f)
                close()
            }
        }.build()
    }

    val Maximize: ImageVector by lazy {
        ImageVector.Builder(
            name = "Maximize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 9f)
                moveTo(9f, 21f)
                lineTo(3f, 21f)
                lineTo(3f, 15f)
                moveTo(21f, 15f)
                lineTo(21f, 21f)
                lineTo(15f, 21f)
                moveTo(3f, 9f)
                lineTo(3f, 3f)
                lineTo(9f, 3f)
            }
        }.build()
    }

    val Minimize: ImageVector by lazy {
        ImageVector.Builder(
            name = "Minimize",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 14f)
                lineTo(10f, 14f)
                lineTo(10f, 20f)
                moveTo(20f, 10f)
                lineTo(14f, 10f)
                lineTo(14f, 4f)
                moveTo(14f, 20f)
                lineTo(14f, 14f)
                lineTo(20f, 14f)
                moveTo(10f, 4f)
                lineTo(10f, 10f)
                lineTo(4f, 10f)
            }
        }.build()
    }

    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 4f)
                lineTo(4f, 4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                lineTo(2f, 20f)
                curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f)
                lineTo(18f, 22f)
                curveTo(19.1f, 22f, 20f, 21.1f, 20f, 20f)
                lineTo(20f, 13f)
                moveTo(18.5f, 2.5f)
                lineTo(21.5f, 5.5f)
                lineTo(12f, 15f)
                lineTo(8f, 16f)
                lineTo(9f, 12f)
                close()
            }
        }.build()
    }

    val ChevronUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 15f)
                lineTo(12f, 9f)
                lineTo(6f, 15f)
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 9f)
                lineTo(12f, 2f)
                lineTo(21f, 9f)
                lineTo(21f, 20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                lineTo(5f, 22f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                close()
                moveTo(9f, 22f)
                lineTo(9f, 12f)
                lineTo(15f, 12f)
                lineTo(15f, 22f)
            }
        }.build()
    }
}

