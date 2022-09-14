package com.example.simplemusicplayer.ui.audio

import android.support.v4.media.MediaBrowserCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simplemusicplayer.data.model.Audio
import com.example.simplemusicplayer.data.repository.AudioRepository
import com.example.simplemusicplayer.media.constants.Konstant
import com.example.simplemusicplayer.media.exoplayer.MediaPlayerServiceConnection
import com.example.simplemusicplayer.media.exoplayer.currentPosition
import com.example.simplemusicplayer.media.exoplayer.isPlaying
import com.example.simplemusicplayer.media.service.MediaPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRepository,
    serviceConnection: MediaPlayerServiceConnection
): ViewModel() {
    var audioList = mutableStateListOf<Audio>()
    val currentPlayingAudio = serviceConnection.currentPlayingAudio
    private val isConnected = serviceConnection.isConnected
    lateinit var rootMediaId: String
    var currentPlaybackPosition by mutableStateOf(0L)
    private var updatePosition = true
    private val playbackState = serviceConnection.playbackState
    val isAudioPlaying:Boolean
    get() = playbackState.value?.isPlaying == true

    private val subscriptionCallBack = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)

        }
    }

    private val serviceConnection = serviceConnection.also {
        updatePlayBack()
    }

    val currentDuration:Long
    get() = MediaPlayerService.currentDuration

    var currentAudioProgress = mutableStateOf(0f)

    init {
        viewModelScope.launch {
            audioList += getAndFormatAudioData()

            isConnected.collect{
                if (it){
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playbackState.value?.apply {
                        currentPlaybackPosition = position
                    }
                    serviceConnection.subscribe(rootMediaId,subscriptionCallBack)
                }
            }
        }
    }

    private suspend fun getAndFormatAudioData(): List<Audio>{
        return repository.getAudioData().map {
            val displayName = it.displayName.substringBefore(",")
            val artist = if (it.artist.contains("<unknown>"))
                "Unknown Artist" else it.artist
            it.copy(
                displayName = displayName,
                artist = artist
            )
        }
    }

    fun playAudio(currentAudio: Audio){
        serviceConnection.playAudio(audioList)
        if(currentAudio.id == currentPlayingAudio.value?.id){
            if (isAudioPlaying){
                serviceConnection.transportControls.pause()
            }else{
                serviceConnection.transportControls.play()
            }
        }else{
            serviceConnection.transportControls.playFromMediaId(currentAudio.id.toString(),null)
        }
    }

    fun stopPlayback(){
        serviceConnection.transportControls.stop()
    }

    fun fastForward(){
        serviceConnection.fastForward()
    }

    fun rewind(){
        serviceConnection.rewind()
    }

    fun skipToNext(){
        serviceConnection.skipToNext()
    }
    fun seekTo(value: Float){
        serviceConnection.transportControls.seekTo(
            (currentDuration * value / 100f).toLong()
        )

    }

    private fun updatePlayBack(){
        viewModelScope.launch {
            val position = playbackState.value?.currentPosition ?: 0
            if (currentPlaybackPosition != position){
                currentPlaybackPosition = position
            }

            if (currentDuration > 0){
                currentAudioProgress.value = (
                        currentPlaybackPosition.toFloat() / currentDuration.toFloat() * 100f
                        )
            }
            delay(Konstant.PLAYBACK_UPDATE_INTERVAL)
            if (updatePosition){
                updatePlayBack()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unsubscribe(Konstant.MEDIA_ROOT_ID,
        object: MediaBrowserCompat.SubscriptionCallback(){}
        )
        updatePosition = false
    }

}