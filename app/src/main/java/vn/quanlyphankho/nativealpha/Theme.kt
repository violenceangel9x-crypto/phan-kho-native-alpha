package vn.quanlyphankho.nativealpha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PhanKhoColors = lightColorScheme(
    primary = Color(0xFF0878DF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9ECFF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF5D5F67),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFF7F8FC),
    error = Color(0xFFBA1A1A)
)

@Composable
fun PhanKhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhanKhoColors,
        content = content
    )
}
