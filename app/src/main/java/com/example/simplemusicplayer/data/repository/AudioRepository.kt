package com.example.simplemusicplayer.data.repository

import com.example.simplemusicplayer.data.ContentResolverHelper
import com.example.simplemusicplayer.data.model.Audio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AudioRepository @Inject constructor(private val contentResolverHelper: ContentResolverHelper) {

    suspend fun getAudioData(): List<Audio> = withContext(Dispatchers.IO){
        contentResolverHelper.getAudioData()
    }
}