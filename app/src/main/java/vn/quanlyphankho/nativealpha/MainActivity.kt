package vn.quanlyphankho.nativealpha

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel

class PhanKhoApplication : Application() {
    val database by lazy { PhanKhoDatabase.create(this) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhanKhoTheme {
                val vm: PhanKhoViewModel = viewModel()
                PhanKhoApp(vm)
            }
        }
    }
}
