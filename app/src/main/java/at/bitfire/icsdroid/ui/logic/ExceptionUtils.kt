package at.bitfire.icsdroid.ui.logic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object ExceptionUtils {
    /**
     * Returns the throwable's content, formatted as an [AnnotatedString]. It will be composed by:
     * - (exception name): (exception message)
     * - (exception stack trace)
     * For each cause:
     * - (cause name): (cause message)
     * - (cause stack trace)
     */
    fun Throwable.annotatedString(color: Color): AnnotatedString = buildAnnotatedString {
        val exception = this@annotatedString
        withStyle(SpanStyle(color = color)) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendLine(exception::class.simpleName + ": " + exception.message)
            }
            for (line in exception.stackTrace) {
                appendLine("  $line")
            }

            var cause = exception.cause
            while (cause != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendLine("Caused by ${cause!!::class.simpleName}: ${exception.message}")
                }
                for (line in cause.stackTrace) {
                    appendLine("  $line")
                }
                cause = cause.cause
            }
        }
    }
}
