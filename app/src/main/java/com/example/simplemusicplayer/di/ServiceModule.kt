package com.example.simplemusicplayer.di

import android.content.Context
import com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import java.io.File

@Module
@InstallIn(ServiceComponent::class)
class ServiceModule {

    //audio attributes for exoplayer initialization
    @Provides
    @ServiceScoped
    fun provideAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(USAGE_MEDIA)
        .build()

    @Provides
    @ServiceScoped
    fun provideExoplayer(@ApplicationContext context: Context,
                         audioAttributes: AudioAttributes
    ):ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(audioAttributes,true)
        setHandleAudioBecomingNoisy(true)
    }

    //to read the data from different sources(For ExoPlayer)
    @Provides
    @ServiceScoped
    fun provideDataSourceFactory(@ApplicationContext context: Context) = DefaultDataSource.Factory(context)

    //to cache the data (ExoPlayer)
    //can use to get the data required for exoplayer
    //will fulfil the request for the data request if not than from upstream
    @Provides
    @ServiceScoped
    fun provideCacheDataSourceFactory(@ApplicationContext context: Context,
                                      dataSource: DefaultDataSource.Factory
    ): CacheDataSource.Factory {
        val cacheDir = File(context.cacheDir,"media")
        val databaseProvider =  StandaloneDatabaseProvider(context)
        val cache = SimpleCache(cacheDir,NoOpCacheEvictor(),databaseProvider)
        return CacheDataSource.Factory().apply {
            setCache(cache)
            setUpstreamDataSourceFactory(dataSource)
        }
    }
}