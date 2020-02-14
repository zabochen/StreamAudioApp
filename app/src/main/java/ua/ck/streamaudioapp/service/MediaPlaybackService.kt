package ua.ck.streamaudioapp.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

private const val TAG = "media_playback_service"

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"

// https://code.tutsplus.com/tutorials/background-audio-in-android-with-mediasessioncompat--cms-27030
// https://github.com/tutsplus/background-audio-in-android-with-mediasessioncompat
class MediaPlaybackService : MediaBrowserServiceCompat() {

    //    private var mediaSession: MediaSessionCompat? = null
    //    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    //    override fun onCreate() {
    //        super.onCreate()
    //
    //        // Create Media Session
    //        this.mediaSession = MediaSessionCompat(baseContext, TAG).apply {
    //
    //            // Enable callback from "Media Buttons"
    //            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
    //
    //            // Set PlaybackState with ACTION_PLAY
    //            stateBuilder = PlaybackStateCompat.Builder()
    //                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
    //            setPlaybackState(stateBuilder.build())
    //
    //            // Handle callback from a Media Controller
    //            //setCallback()
    //
    //            // Session's token
    //            setSessionToken(sessionToken)
    //        }
    //
    //
    //    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    private var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                }
            }
        }
    }

    private var mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            super.onPlay()
            Log.i("MediaPlaybackService", "mediaSessionCallback: onPlay()")
        }

        override fun onPause() {
            super.onPause()
            Log.i("MediaPlaybackService", "mediaSessionCallback: onPause()")
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            Log.i("MediaPlaybackService", "mediaSessionCallback: onPlayFromMediaId()")
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            super.onPlayFromUri(uri, extras)
            Log.i("MediaPlaybackService", "mediaSessionCallback: onPlayFromUri()")
        }
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>()
        if (MY_MEDIA_ROOT_ID == parentId) {
        }

        result.sendResult(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        initMediaPlayer()
        initMediaSession()
        initNoisyReceiver()
    }

    private fun initMediaPlayer() {
        this.mediaPlayer = MediaPlayer()
        mediaPlayer?.apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setVolume(1f, 1f)
        }
    }

    private fun initMediaSession() {
        val mediaButtonReceiver = ComponentName(applicationContext, MediaButtonReceiver::class.java)
        this.mediaSession = MediaSessionCompat(applicationContext, "Tag", mediaButtonReceiver, null)
        mediaSession?.apply {
            setCallback(mediaSessionCallback)
        }
    }

    private fun initNoisyReceiver() {

    }

}