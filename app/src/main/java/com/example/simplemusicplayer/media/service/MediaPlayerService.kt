package com.example.simplemusicplayer.media.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ColorSpace
import android.media.AudioMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.simplemusicplayer.R
import com.example.simplemusicplayer.media.constants.Konstant
import com.example.simplemusicplayer.media.exoplayer.MediaPlayerNotificationManager
import com.example.simplemusicplayer.media.exoplayer.MediaSource
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService: MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory

    @Inject
    lateinit var exoplayer: ExoPlayer

    @Inject
    lateinit var mediaSource: MediaSource

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    //create media session which is going to help to connect to the mediaPlayer
    //provide universal way of interacting with audio or video player
    // we can use media session to control playback from different places
    // newly created media session has no capabilities so we have to connect it to player
    //so we have to create mediaSessionConnector to connect it
    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var mediaPlayerNotificationManager: MediaPlayerNotificationManager
    private var currentPlayingMedia: MediaMetadataCompat? = null
    private val isPlayerInitialized = false
    private var isForeground = false

    companion object{
        private const val TAG = "MediaPlayerService"
        var currentDuration: Long = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val sessionActivityIntent = packageManager
            ?.getLaunchIntentForPackage(packageName)
            ?.let {sessionIntent ->
                PendingIntent.getActivity(this,0,sessionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession = MediaSessionCompat(this,TAG).apply {
            setSessionActivity(sessionActivityIntent)
            isActive = true
        }
        //to connect media session to the mediaBrowser define session token
        sessionToken = mediaSession.sessionToken

        mediaPlayerNotificationManager = MediaPlayerNotificationManager(this,mediaSession.sessionToken,PlayerNotificationListener())
        serviceScope.launch {
            mediaSource.load()
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            //help to prepare media for playing
            setPlaybackPreparer(AudioPlayBackPreparer())
            //help to navigate different playback items
            setQueueNavigator(MediaQueueNavigator(mediaSession))
            setPlayer(exoplayer)
        }

        mediaPlayerNotificationManager.showNotification(exoplayer)
    }


    //below two method helps to provide a client connection
    //server is mediaBrowserService and client is mainActivity
    //when we want to send data back to mainActivity this two methods use to perform connection
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        //method provide access to this service
        return BrowserRoot(Konstant.MEDIA_ROOT_ID, null)
    }


    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){
            Konstant.MEDIA_ROOT_ID -> {
                val resultSent = mediaSource.whenReady {isInitialized->
                    if (isInitialized){
                        result.sendResult(mediaSource.asMediaItem())
                    }else{
                        result.sendResult(null)
                    }

                }
                if (!resultSent){
                    result.detach()
                }
            }
            else -> Unit
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        when(action){
            Konstant.START_MEDIA_PLAY_ACTION -> {
                mediaPlayerNotificationManager.showNotification(exoplayer)
            }
            Konstant.REFRESH_MEDIA_PLAY_ACTION -> {
                mediaSource.refresh()
                notifyChildrenChanged(Konstant.MEDIA_ROOT_ID)
            }
            else -> Unit
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoplayer.stop()
        exoplayer.clearMediaItems()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoplayer.release()

    }

    inner class PlayerNotificationListener: PlayerNotificationManager.NotificationListener{

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForeground = false
            stopSelf()
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForeground){
                ContextCompat.startForegroundService(applicationContext,
                    Intent(applicationContext,this@MediaPlayerService.javaClass))
                startForeground(notificationId,notification)
                isForeground = true
            }
        }
    }

    inner class AudioPlayBackPreparer: MediaSessionConnector.PlaybackPreparer{
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            return false
        }

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        override fun onPrepare(playWhenReady: Boolean) = Unit

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {

            mediaSource.whenReady {
                val itemToPlay = mediaSource.audioMediaMetaData.find{
                    it.description.mediaId == mediaId
                }

                currentPlayingMedia = itemToPlay
                preparePlayer(mediaMetadata = mediaSource.audioMediaMetaData,
                    itemToPlay = itemToPlay,
                    playWhenReady = playWhenReady)
            }
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit
    }

    private fun preparePlayer(
        mediaMetadata: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playWhenReady: Boolean
    ){
        val indexToPlay = if (currentPlayingMedia == null) 0
        else mediaMetadata.indexOf(itemToPlay)

        exoplayer.addListener(PlayerEventListener())
        exoplayer.setMediaSource(mediaSource.asMediaSource(dataSourceFactory))
        exoplayer.prepare()
        exoplayer.seekTo(indexToPlay, 0)
        exoplayer.playWhenReady = playWhenReady
    }

    private inner class PlayerEventListener: Player.Listener{
        override fun onPlaybackStateChanged(playbackState: Int) {
            when(playbackState){
                Player.STATE_BUFFERING,
                    Player.STATE_READY -> {
                        mediaPlayerNotificationManager.showNotification(exoplayer)
                    }
                else ->
                    mediaPlayerNotificationManager.hideNotification()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            currentDuration = player.duration
        }

        override fun onPlayerError(error: PlaybackException) {
            var message = R.string.generic_error

            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND){
                message = R.string.stringerror_media_not_found
            }
            Toast.makeText(this@MediaPlayerService,message,Toast.LENGTH_SHORT)
        }
    }

    inner class MediaQueueNavigator(mediaSession: MediaSessionCompat): TimelineQueueNavigator(mediaSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            if (windowIndex < mediaSource.audioMediaMetaData.size){
                return mediaSource.audioMediaMetaData[windowIndex].description
            }
            return MediaDescriptionCompat.Builder().build()
        }

    }

}