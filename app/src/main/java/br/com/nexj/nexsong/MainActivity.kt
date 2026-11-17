package br.com.nexj.nexsong

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.nexj.nexsong.ui.MainViewModel
import br.com.nexj.nexsong.ui.NexSongRoot
import br.com.nexj.nexsong.ui.NexSongTheme
import br.com.nexj.nexsong.ui.PermissionScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NexSongTheme {
                NexSongApp()
            }
        }
    }
}

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

@Composable
private fun NexSongApp() {
    val context = LocalContext.current
    val vm: MainViewModel = viewModel()
    var granted by remember { mutableStateOf(hasAudioPermission(context)) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        granted = hasAudioPermission(context)
        if (granted) vm.loadSongs()
    }

    LaunchedEffect(Unit) {
        vm.connectPlayer()
        if (granted) vm.loadSongs()
    }

    if (granted) {
        NexSongRoot(vm)
    } else {
        PermissionScreen(onRequest = {
            val permissions = buildList {
                add(audioPermission())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            launcher.launch(permissions.toTypedArray())
        })
    }
}
