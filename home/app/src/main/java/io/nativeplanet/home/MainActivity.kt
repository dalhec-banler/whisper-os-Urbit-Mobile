package io.nativeplanet.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.nativeplanet.home.ui.HomeViewModel
import io.nativeplanet.home.ui.Root
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private val askCalendar = registerForActivityResult(ActivityResultContracts.RequestPermission()) { lifecycleScope.launch { vm.refresh() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Root(vm) }
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) askCalendar.launch(Manifest.permission.READ_CALENDAR)
    }

    /** The HOME key re-delivers our intent: always land on the quiet home. */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        vm.home()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { vm.refresh() }
    }
}
