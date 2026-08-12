package com.example.pythagoros.presentation.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Иконки макета нарисованы вручную: в проекте нет material-icons-extended,
 * а часть глифов (искра AI, ось графика) в стандартном наборе отсутствует вовсе.
 * Все векторы одноцветные — цвет задаётся через tint у [androidx.compose.material3.Icon].
 */
object PythIcons {

    val ArrowLeft: ImageVector by lazy {
        strokeIcon("ArrowLeft") {
            moveTo(20f, 12f); lineTo(4f, 12f)
            moveTo(10f, 5.5f); lineTo(3.5f, 12f); lineTo(10f, 18.5f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close") {
            moveTo(5f, 5f); lineTo(19f, 19f)
            moveTo(19f, 5f); lineTo(5f, 19f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight", width = 2.2f) {
            moveTo(9f, 4.5f); lineTo(16.5f, 12f); lineTo(9f, 19.5f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check", width = 2.6f) {
            moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f)
        }
    }

    val Menu: ImageVector by lazy {
        strokeIcon("Menu") {
            moveTo(4f, 7f); lineTo(20f, 7f)
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(4f, 17f); lineTo(20f, 17f)
        }
    }

    val Dots: ImageVector by lazy {
        fillIcon("Dots") {
            circleAt(5f, 12f, 1.7f)
            circleAt(12f, 12f, 1.7f)
            circleAt(19f, 12f, 1.7f)
        }
    }

    val Search: ImageVector by lazy {
        strokeIcon("Search") {
            circlePath(11f, 11f, 6.2f)
            moveTo(15.6f, 15.6f); lineTo(20.5f, 20.5f)
        }
    }

    val Settings: ImageVector by lazy {
        strokeIcon("Settings", width = 1.8f) {
            circlePath(12f, 12f, 3.2f)
            moveTo(12f, 2.5f); lineTo(12f, 6f)
            moveTo(12f, 18f); lineTo(12f, 21.5f)
            moveTo(2.5f, 12f); lineTo(6f, 12f)
            moveTo(18f, 12f); lineTo(21.5f, 12f)
            moveTo(5.2f, 5.2f); lineTo(7.7f, 7.7f)
            moveTo(16.3f, 16.3f); lineTo(18.8f, 18.8f)
            moveTo(18.8f, 5.2f); lineTo(16.3f, 7.7f)
            moveTo(7.7f, 16.3f); lineTo(5.2f, 18.8f)
        }
    }

    val Play: ImageVector by lazy {
        fillIcon("Play") {
            moveTo(6.5f, 4f); lineTo(19.5f, 12f); lineTo(6.5f, 20f); close()
        }
    }

    val Pause: ImageVector by lazy {
        fillIcon("Pause") {
            moveTo(6f, 4.5f); lineTo(10f, 4.5f); lineTo(10f, 19.5f); lineTo(6f, 19.5f); close()
            moveTo(14f, 4.5f); lineTo(18f, 4.5f); lineTo(18f, 19.5f); lineTo(14f, 19.5f); close()
        }
    }

    /** Искра — маркер «здесь работал AI». Форма взята из макета (четырёхлучевая звезда). */
    val Sparkle: ImageVector by lazy {
        fillIcon("Sparkle") {
            moveTo(12f, 1.5f)
            lineTo(14.5f, 8.3f)
            lineTo(21.3f, 10.8f)
            lineTo(14.5f, 13.3f)
            lineTo(12f, 20.1f)
            lineTo(9.5f, 13.3f)
            lineTo(2.7f, 10.8f)
            lineTo(9.5f, 8.3f)
            close()
        }
    }

    /** Молния — быстрый режим / вспышка камеры. */
    val Bolt: ImageVector by lazy {
        fillIcon("Bolt") {
            moveTo(13.5f, 2f); lineTo(5f, 13.5f); lineTo(11f, 13.5f); lineTo(10f, 22f)
            lineTo(19f, 10f); lineTo(12.7f, 10f); close()
        }
    }

    /** Круговая стрелка — «повторить шаг». */
    val Reset: ImageVector by lazy {
        strokeIcon("Reset") {
            moveTo(20f, 12f)
            arcTo(8f, 8f, 0f, true, true, 17.2f, 5.9f)
            moveTo(17.2f, 1.6f); lineTo(17.2f, 6.4f); lineTo(12.4f, 6.4f)
        }
    }

    /** Мини-график: оси и парабола — кнопка перехода к графику. */
    val Chart: ImageVector by lazy {
        strokeIcon("Chart", width = 1.8f) {
            moveTo(3f, 3f); lineTo(3f, 21f); lineTo(21f, 21f)
            moveTo(5.5f, 18f)
            quadTo(12f, 0.5f, 18.5f, 18f)
        }
    }

    val Download: ImageVector by lazy {
        strokeIcon("Download") {
            moveTo(12f, 3f); lineTo(12f, 15.5f)
            moveTo(6.5f, 10f); lineTo(12f, 15.8f); lineTo(17.5f, 10f)
            moveTo(4f, 20.5f); lineTo(20f, 20.5f)
        }
    }

    /** Восклицательный знак в круге — предупреждение решателя. */
    val Warning: ImageVector by lazy {
        strokeIcon("Warning", width = 1.8f) {
            circlePath(12f, 12f, 9f)
            moveTo(12f, 6.5f); lineTo(12f, 13f)
            moveTo(12f, 16.4f); lineTo(12f, 17.4f)
        }
    }

    val ArrowRight: ImageVector by lazy {
        strokeIcon("ArrowRight") {
            moveTo(4f, 12f); lineTo(20f, 12f)
            moveTo(14f, 5.5f); lineTo(20.5f, 12f); lineTo(14f, 18.5f)
        }
    }

    /** Кружок затвора в нижней навигации — вкладка «Задача». */
    val Camera: ImageVector by lazy {
        strokeIcon("Camera", width = 1.8f) {
            moveTo(3f, 8.5f); lineTo(7f, 8.5f); lineTo(9f, 5.5f); lineTo(15f, 5.5f)
            lineTo(17f, 8.5f); lineTo(21f, 8.5f); lineTo(21f, 19f); lineTo(3f, 19f); close()
            circlePath(12f, 13f, 4f)
        }
    }

    /** Картинка — выбор снимка из галереи. */
    val Gallery: ImageVector by lazy {
        strokeIcon("Gallery", width = 1.8f) {
            moveTo(3.5f, 4.5f); lineTo(20.5f, 4.5f); lineTo(20.5f, 19.5f); lineTo(3.5f, 19.5f); close()
            moveTo(3.5f, 16f); lineTo(9f, 10.5f); lineTo(14f, 15.5f); lineTo(16.5f, 13f); lineTo(20.5f, 17f)
            circlePath(15.5f, 8.5f, 1.6f)
        }
    }

    /** Клавиатура — ручной ввод условия. */
    val Keyboard: ImageVector by lazy {
        strokeIcon("Keyboard", width = 1.8f) {
            moveTo(2.5f, 6f); lineTo(21.5f, 6f); lineTo(21.5f, 18f); lineTo(2.5f, 18f); close()
            moveTo(6f, 9.5f); lineTo(6.6f, 9.5f)
            moveTo(10f, 9.5f); lineTo(10.6f, 9.5f)
            moveTo(14f, 9.5f); lineTo(14.6f, 9.5f)
            moveTo(18f, 9.5f); lineTo(18.6f, 9.5f)
            moveTo(8f, 14.5f); lineTo(16f, 14.5f)
        }
    }

    /** Замок — закрытый Pro-контент. */
    val Lock: ImageVector by lazy {
        strokeIcon("Lock", width = 1.8f) {
            moveTo(5.5f, 10.5f); lineTo(18.5f, 10.5f); lineTo(18.5f, 20.5f); lineTo(5.5f, 20.5f); close()
            moveTo(8.2f, 10.5f); lineTo(8.2f, 7.5f)
            arcTo(3.8f, 3.8f, 0f, false, true, 15.8f, 7.5f)
            lineTo(15.8f, 10.5f)
        }
    }

    /** Звезда — AI-преимущества на paywall. */
    val Star: ImageVector by lazy {
        fillIcon("Star") {
            moveTo(12f, 2.5f); lineTo(14.9f, 8.6f); lineTo(21.5f, 9.5f); lineTo(16.7f, 14.3f)
            lineTo(17.9f, 21f); lineTo(12f, 17.8f); lineTo(6.1f, 21f); lineTo(7.3f, 14.3f)
            lineTo(2.5f, 9.5f); lineTo(9.1f, 8.6f); close()
        }
    }

    /** Знак вопроса — подсказка и теория к шагу. */
    val Question: ImageVector by lazy {
        strokeIcon("Question", width = 2f) {
            moveTo(8.6f, 8.8f)
            arcTo(3.5f, 3.5f, 0f, true, true, 12f, 13.4f)
            moveTo(12f, 13.4f); lineTo(12f, 15.4f)
            moveTo(12f, 18.4f); lineTo(12f, 19.2f)
        }
    }

    /** Конверт — вход по почте. */
    val Mail: ImageVector by lazy {
        strokeIcon("Mail", width = 1.8f) {
            moveTo(3f, 5.5f); lineTo(21f, 5.5f); lineTo(21f, 18.5f); lineTo(3f, 18.5f); close()
            moveTo(3f, 6f); lineTo(12f, 13f); lineTo(21f, 6f)
        }
    }

    /** Часы со стрелкой — вкладка «История». */
    val History: ImageVector by lazy {
        strokeIcon("History", width = 1.8f) {
            circlePath(12f, 12f, 8.5f)
            moveTo(12f, 7f); lineTo(12f, 12.4f); lineTo(15.8f, 14.6f)
        }
    }

    /** Треугольник — задача по геометрии. */
    val Triangle: ImageVector by lazy {
        strokeIcon("Triangle", width = 1.8f) {
            moveTo(12f, 4f); lineTo(21f, 20f); lineTo(3f, 20f); close()
        }
    }

    /** Силуэт — вкладка «Профиль». */
    val Person: ImageVector by lazy {
        strokeIcon("Person", width = 1.8f) {
            circlePath(12f, 8f, 3.8f)
            moveTo(4.5f, 20.5f)
            arcTo(7.5f, 7.5f, 0f, false, true, 19.5f, 20.5f)
        }
    }
}

private fun strokeIcon(
    name: String,
    width: Float = 2f,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
    pathBuilder = block,
).build()

private fun fillIcon(
    name: String,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    fill = SolidColor(Color.Black),
    pathBuilder = block,
).build()

/** Окружность через две дуги — в PathBuilder нет примитива «круг». */
private fun PathBuilder.circlePath(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, false, true, cx + r, cy)
    arcTo(r, r, 0f, false, true, cx - r, cy)
    close()
}

private fun PathBuilder.circleAt(cx: Float, cy: Float, r: Float) = circlePath(cx, cy, r)
