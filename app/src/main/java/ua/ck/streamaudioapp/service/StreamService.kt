package ua.ck.streamaudioapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

// https://habr.com/ru/post/339416/
// https://exoplayer.dev/hello-world.html
class StreamService : Service(), MetadataOutput {

    private val iBinder: Binder = LocalBinder()

    private var streamUrl: String? = null

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var audioManagerCompat: AudioManager
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var transportControlsCompat: MediaControllerCompat.TransportControls

    override fun onCreate() {
        super.onCreate()

        Log.i("StreamService", "StreamService: onCreate()")

        // Audio Manager
        this.audioManagerCompat = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Media Session
        this.mediaSessionCompat = MediaSessionCompat(this, "StreamService")

        // Transport Controls
        this.transportControlsCompat = mediaSessionCompat.controller.transportControls

        // Media Session Set
        mediaSessionCompat.isActive = true
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSessionCompat.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "Artist?")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "Album?")
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Title?")
                .build()
        )
        mediaSessionCompat.setCallback(object : MediaSessionCompat.Callback() {

            override fun onPlay() {
                super.onPlay()

            }

            override fun onPause() {
                super.onPause()
            }

            override fun onStop() {
                super.onStop()
            }

        })

        val bandWithMeter = DefaultBandwidthMeter()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandWithMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)

        this.simpleExoPlayer = SimpleExoPlayer.Builder(applicationContext).build()
        //this.simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext, trackSelector)
        simpleExoPlayer.addMetadataOutput {
            Log.i("StreamService", "addMetadataOutput")
        }


    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.i("StreamService", "onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return iBinder
    }

    fun play(streamUrl: String) {

        Log.i("StreamService", "play(streamUrl: String): $streamUrl")

        this.streamUrl = streamUrl

        val dataSourceFactory =
            DefaultDataSourceFactory(applicationContext, Util.getUserAgent(applicationContext, "StreamService"))

        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(streamUrl))

        simpleExoPlayer?.prepare(mediaSource)
        //simpleExoPlayer.playWhenReady = true
    }

    fun pause() {

    }

    fun stop() {

    }

    fun playOrPause(streamUrl: String) {
        play(streamUrl = streamUrl)
    }

    inner class LocalBinder : Binder() {
        fun getService(): StreamService {
            return this@StreamService
        }
    }

    override fun onMetadata(metadata: Metadata) {
        Log.i("StreamService", "${metadata.length()}")
    }
}