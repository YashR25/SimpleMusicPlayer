package com.example.simplemusicplayer.ui.audio

import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.simplemusicplayer.R
import com.example.simplemusicplayer.data.model.Audio
import com.example.simplemusicplayer.ui.theme.SimpleMusicPlayerTheme
import java.util.concurrent.TimeUnit
import kotlin.math.floor

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    isAudioPlaying: Boolean,
    audioList:List<Audio>,
    currentlyPlayingAudio:Audio?,
    onStart: (Audio) -> Unit,
    onItemClick: (Audio) -> Unit,
    onNext: () -> Unit
){

    val scaffoldState = rememberBottomSheetScaffoldState()
    val animateHeight by animateDpAsState(
        targetValue = if (currentlyPlayingAudio == null) 0.dp
        else BottomSheetScaffoldDefaults.SheetPeekHeight
    )
    BottomSheetScaffold(
        sheetContent = {
            currentlyPlayingAudio?.let{currentPlayingAudio ->
                BottomBarPlayer(
                    progress = progress,
                    onProgressChange = onProgressChange,
                    audio = currentPlayingAudio,
                    isAudioPlaying = isAudioPlaying,
                    onStart = { onStart.invoke(currentPlayingAudio)},
                    onNext = {onNext.invoke()}
                )
            }
        },
        scaffoldState = scaffoldState,
        sheetPeekHeight = animateHeight
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 56.dp)){
            items(audioList){audio:Audio->
                AudioItem(audio = audio, onItemClick = {onItemClick.invoke(audio)})

            }
        }
    }
}

@Composable
fun AudioItem(audio: Audio, onItemClick: (id:Long) -> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                onItemClick.invoke(audio.id)
            },
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = .5f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier
                .weight(1f)
                .padding(8.dp)) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(text = audio.displayName, style = MaterialTheme.typography.h6, overflow = TextOverflow.Clip, maxLines = 1)
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = audio.artist,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = MaterialTheme.colors.onSurface.copy(alpha = .5f)
                )
                
            }
            Text(text = timeStampSuration(audio.duration.toLong()))
            Spacer(modifier = Modifier.size(8.dp))
            
        }
    }
}

private fun timeStampSuration(position: Long):String{
    val totalSeconds = floor(position/1E3).toInt()
    val minutes = totalSeconds/60
    val remainingSeconds = totalSeconds - (minutes * 60)
    return if (position < 0) "--:--"
    else "%d:%02d".format(minutes,remainingSeconds)
}

@Composable
fun BottomBarPlayer(
    progress:Float,
    onProgressChange:(Float) -> Unit,
    audio: Audio,
    isAudioPlaying:Boolean,
    onStart: () -> Unit,
    onNext: () -> Unit
){
    Column {
        Row(modifier = Modifier
            .height(56.dp)
            .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            ArtistInfo(audio = audio, modifier = Modifier.weight(1f))
            MediaPlayerController(isAudioPlaying = isAudioPlaying, onStart = { onStart.invoke() },onNext = {onNext.invoke()})

        }
        Slider(value = progress, onValueChange = {onProgressChange.invoke(it)},
            valueRange = 0f..100f)
    }
}

@Composable
fun ArtistInfo(
    modifier: Modifier = Modifier,
    audio: Audio
){
    Row(modifier = modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        PlayerIconItem(icon = ImageVector.vectorResource(id = R.drawable.ic_baseline_music_note_24),
            border = BorderStroke(width = 1.dp,color = MaterialTheme.colors.onSurface),
        ) {}
        Spacer(modifier = Modifier.size(4.dp))
        Column {
            Text(text = audio.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6, overflow = TextOverflow.Clip,modifier = Modifier.weight(1f), maxLines = 1)
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = audio.artist, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.subtitle1, overflow = TextOverflow.Clip, maxLines = 1)

        }
    }
}

@Composable
fun PlayerIconItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    border: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colors.surface,
    color: Color = MaterialTheme.colors.onSurface,
    onClick: () -> Unit ){
    Surface(
        shape = CircleShape,
        border = border,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClick.invoke()
            },
        contentColor = color,
        color = backgroundColor
    ){
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ){
            Icon(imageVector = icon, contentDescription = null)
        }

    }

}

@Composable
fun MediaPlayerController(isAudioPlaying: Boolean,onStart: () -> Unit,onNext: () -> Unit){
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(56.dp)
            .padding(4.dp)
    ) {
        PlayerIconItem(
            icon = if (isAudioPlaying) ImageVector.vectorResource(id = R.drawable.ic_baseline_pause_24) else ImageVector.vectorResource(
                id = R.drawable.ic_baseline_play_arrow_24
            ),
            backgroundColor = MaterialTheme.colors.primary
        ) {
            onStart.invoke()
        }
        Spacer(modifier = Modifier.size(8.dp))
        Icon(imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_skip_next_24), contentDescription = null, modifier = Modifier.clickable {
            onNext.invoke()
        })
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBarPrev(){
    SimpleMusicPlayerTheme {
        BottomBarPlayer(
            progress = 50f,
            onProgressChange = {},
            audio = Audio(Uri.parse(""),"",1,"","",1,""),
            isAudioPlaying = true,
            onStart = { /*TODO*/ }) {

        }

    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPrev(){
    SimpleMusicPlayerTheme {
        HomeScreen(
            progress = 50f,
            onProgressChange = {},
            isAudioPlaying = true,
            audioList = emptyList(),
            currentlyPlayingAudio = Audio(Uri.parse(""),"",1,"","",1,""),
            onStart = {},
            onItemClick = {}
        ) {
            
        }
        
    }
}