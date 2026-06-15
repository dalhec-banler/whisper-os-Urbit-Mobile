package io.nativeplanet.launcher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.nativeplanet.launcher.nav.HomeEvents
import io.nativeplanet.launcher.nav.NavGraph
import io.nativeplanet.launcher.theme.NativePlanetTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterLauncherChrome()
        setContent {
            NativePlanetTheme {
                NavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterLauncherChrome()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isHomeIntent()) {
            HomeEvents.requestHome()
        }
    }

    private fun enterLauncherChrome() {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun Intent?.isHomeIntent(): Boolean {
        return this?.action == Intent.ACTION_MAIN && hasCategory(Intent.CATEGORY_HOME)
    }
}
