package io.nativeplanet.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.nativeplanet.launcher.nav.NavGraph
import io.nativeplanet.launcher.theme.NativePlanetTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NativePlanetTheme {
                NavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
