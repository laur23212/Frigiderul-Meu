package ro.frigiderulmeu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ro.frigiderulmeu.domain.ExpiryState

private val Scheme = lightColorScheme(
    primary = Color(0xFF2E7D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBDA),
    onPrimaryContainer = Color(0xFF0B3A25),
    secondaryContainer = Color(0xFFE3ECE6),
    onSecondaryContainer = Color(0xFF34473C),
    surface = Color(0xFFFBFCFB),
    background = Color(0xFFFBFCFB),
    surfaceVariant = Color(0xFFEEF1EF),
    outlineVariant = Color(0xFFDCE2DE),
    error = Color(0xFFC0392B),
)

@Composable
fun FrigiderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

/** Culorile stărilor de expirare și ale indicatorului „deschis”. */
object StatusColors {
    val opened = Color(0xFF2F6FD6)
    private val neutralBg = Color(0xFFF6F7F6)

    fun background(state: ExpiryState?): Color = when (state) {
        ExpiryState.GREEN -> Color(0xFFE5F4EA)
        ExpiryState.ORANGE -> Color(0xFFFFEDD5)
        ExpiryState.RED -> Color(0xFFFBE1E1)
        null -> neutralBg
    }

    fun border(state: ExpiryState?): Color = when (state) {
        ExpiryState.GREEN -> Color(0xFF55B07A)
        ExpiryState.ORANGE -> Color(0xFFEE8E2B)
        ExpiryState.RED -> Color(0xFFD94848)
        null -> Color(0xFFE3E7E4)
    }

    /** Variante mai închise, cu contrast bun pentru text. */
    fun text(state: ExpiryState?): Color = when (state) {
        ExpiryState.GREEN -> Color(0xFF2A7046)
        ExpiryState.ORANGE -> Color(0xFFA85A12)
        ExpiryState.RED -> Color(0xFFB02A2A)
        null -> Color(0xFF5F6B64)
    }
}
