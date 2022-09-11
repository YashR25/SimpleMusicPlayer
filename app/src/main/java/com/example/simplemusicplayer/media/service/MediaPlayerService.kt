package com.example.simplemusicplayer.media.service

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService
import com.example.simplemusicplayer.media.constants.Konstant
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaPlayerService: MediaBrowserService() {

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(Konstant.MEDIA_ROOT_ID,null)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        TODO("Not yet implemented")
    }
}