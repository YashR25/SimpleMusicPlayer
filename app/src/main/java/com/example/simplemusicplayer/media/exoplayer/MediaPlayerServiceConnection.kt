package com.example.simplemusicplayer.media.exoplayer

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.simplemusicplayer.data.model.Audio
import com.example.simplemusicplayer.media.constants.Konstant
import com.example.simplemusicplayer.media.service.MediaPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

//in this class create some state that can be observed by viewModel
class MediaPlayerServiceConnection @Inject constructor(@ApplicationContext context: Context) {


    private val _playBackState: MutableStateFlow<PlaybackStateCompat?> = MutableStateFlow(null)
    val playbackState: StateFlow<PlaybackStateCompat?>
    get() = _playBackState

    //to check/get state of connection between mediaBrowser and ui
    private val _isConnected : MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean>
        get() = _isConnected

    val currentPlayingAudio = mutableStateOf<Audio?>(null)

    lateinit var mediaController: MediaControllerCompat

    //callback to check the connection between mediaBrowserService and MainActivity
    private val mediaBrowserServiceCallback = MediaBrowserConnectionCallBack(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context,MediaPlayerService::class.java),
        mediaBrowserServiceCallback,
        null
    ).apply {
        connect()
    }

   private var audioList = listOf<Audio>()
    val rootMediaId:String
    get() =  mediaBrowser.root

    val transportControls:MediaControllerCompat.TransportControls
    get() = mediaController.transportControls

    fun playAudio(audios:List<Audio>){
        audioList = audios
        mediaBrowser.sendCustomAction(Konstant.START_MEDIA_PLAY_ACTION,null,null)
    }

    fun fastForward(seconds:Int = 10){
        playbackState.value?.currentPosition?.let {
            transportControls.seekTo(it + seconds * 1000)
        }
    }

    fun rewind(seconds:Int = 10){
        playbackState.value?.currentPosition?.let {
            transportControls.seekTo(it - seconds * 1000)
        }
    }

    fun skipToNext(){
        transportControls.skipToNext()
    }

    fun subscribe(parentId:String,callBack:MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.subscribe(parentId,callBack)
    }

    fun unsubscribe(parentId:String,callBack:MediaBrowserCompat.SubscriptionCallback){
        mediaBrowser.unsubscribe(parentId,callBack)
    }

    fun refreshMediaBrowserChildren(){
        mediaBrowser.sendCustomAction(Konstant.REFRESH_MEDIA_PLAY_ACTION,null,null)
    }



    private inner class MediaBrowserConnectionCallBack(
        private val context: Context
    ): MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
            _isConnected.value = true
            mediaController = MediaControllerCompat(context,mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallBack())
            }
        }

        override fun onConnectionSuspended() {
            _isConnected.value = false
        }

        override fun onConnectionFailed() {
            _isConnected.value = false
        }
    }

    private inner class MediaControllerCallBack: MediaControllerCompat.Callback(){
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            _playBackState.value = state
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            currentPlayingAudio.value = metadata?.let {data->
                audioList.find {
                    it.id.toString() == data.description.mediaId
                }
            }
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            mediaBrowserServiceCallback.onConnectionSuspended()
        }
    }


}