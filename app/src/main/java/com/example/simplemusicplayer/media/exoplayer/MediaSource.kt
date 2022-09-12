package com.example.simplemusicplayer.media.exoplayer

import android.media.browse.MediaBrowser
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.example.simplemusicplayer.data.repository.AudioRepository
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import javax.inject.Inject

class MediaSource @Inject constructor(private val repository: AudioRepository) {
    private val onReadyListeners: MutableList<OnReadyListener> = mutableListOf()
    var audioMediaMetaData: List<MediaMetadataCompat> = emptyList()

    //to check whether our source is fully loaded whenever needed and depending on the state the callbacks are called
    private var state: AudioSourceState = AudioSourceState.STATE_CREATED
    set(value){
        if (value == AudioSourceState.STATE_CREATED || value == AudioSourceState.STATE_ERROR){
            //to keep the stat async across multiple thread use synchronize
            synchronized(onReadyListeners){
                field = value
                onReadyListeners.forEach{listener:OnReadyListener ->
                    listener.invoke(isReady)
                }
            }
        }else{
            field = value
        }


    }
    private val isReady: Boolean
        get() = state == AudioSourceState.STATE_INITIALIZED

    //this function is used in mediaService class to fill the onReadyListener lambda function
    fun whenReady(listener: OnReadyListener): Boolean{
        return if (state == AudioSourceState.STATE_INITIALIZING || state == AudioSourceState.STATE_CREATED){
            onReadyListeners += listener
            false
        }else{
            listener.invoke(isReady)
            true
        }
    }


    suspend fun load(){
        state = AudioSourceState.STATE_INITIALIZING
        //this data does not recognized by exoplayer and mediaPlayerService so transform it to metadata
        val data = repository.getAudioData()
        audioMediaMetaData = data.map {audio ->  
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,audio.id.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,audio.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,audio.uri.toString())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,audio.title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,audio.displayName)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,audio.duration.toLong())
                .build()
        }
        state = AudioSourceState.STATE_INITIALIZED
    }


    //for exoplayer to play
    fun asMediaSource(
        dataSourceFactory: CacheDataSource.Factory
    ): ConcatenatingMediaSource{
        val concatenatingMediaSource = ConcatenatingMediaSource()
        audioMediaMetaData.forEach{mediaMetadataCompat ->  
            val mediaItem = MediaItem.fromUri(mediaMetadataCompat.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
            val mediaSource = ProgressiveMediaSource
                .Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    //for client to display
    fun asMediaItem() = audioMediaMetaData.map {metadata->
        val description = MediaDescriptionCompat.Builder()
            .setTitle(metadata.description.title)
            .setMediaId(metadata.description.mediaId)
            .setSubtitle(metadata.description.subtitle)
            .setMediaUri(metadata.description.mediaUri)
            .build()
        MediaBrowserCompat.MediaItem(description,FLAG_PLAYABLE)
    }.toMutableList()


    fun refresh(){
        onReadyListeners.clear()
        state = AudioSourceState.STATE_CREATED
    }
}

enum class AudioSourceState{
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}
//just the name for the lambda function
typealias OnReadyListener = (Boolean) -> Unit