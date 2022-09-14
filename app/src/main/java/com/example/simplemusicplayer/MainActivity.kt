package com.example.simplemusicplayer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simplemusicplayer.ui.audio.AudioViewModel
import com.example.simplemusicplayer.ui.audio.HomeScreen
import com.example.simplemusicplayer.ui.theme.SimpleMusicPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimpleMusicPlayerTheme {
                val permissionState = rememberPermissionState(permission = Manifest.permission.READ_EXTERNAL_STORAGE)
                // A surface container using the 'background' color from the theme
                val lifeCycleOwner = LocalLifecycleOwner.current
                DisposableEffect(key1 = lifeCycleOwner){
                    val observer = LifecycleEventObserver{_,event->
                        if (event == Lifecycle.Event.ON_RESUME){
                            permissionState.launchPermissionRequest()
                        }
                    }
                    lifeCycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifeCycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionState.hasPermission){
                        val audioViewModel = viewModel(modelClass = AudioViewModel::class.java)
                        val audioList = audioViewModel.audioList
                        HomeScreen(
                            progress = audioViewModel.currentAudioProgress.value,
                            onProgressChange = {audioViewModel.seekTo(it)},
                            isAudioPlaying = audioViewModel.isAudioPlaying,
                            audioList = audioList,
                            currentlyPlayingAudio = audioViewModel.currentPlayingAudio.value,
                            onStart = {audioViewModel.playAudio(it)},
                            onItemClick = {audioViewModel.playAudio(it)},
                            onNext = {audioViewModel.skipToNext()}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SimpleMusicPlayerTheme {
        Greeting("Android")
    }
}