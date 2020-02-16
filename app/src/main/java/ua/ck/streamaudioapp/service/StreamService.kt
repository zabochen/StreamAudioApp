package ua.ck.streamaudioapp.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.MetadataOutput
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

// https://habr.com/ru/post/339416/
// https://exoplayer.dev/hello-world.html
class StreamService : Service(), MetadataOutput, Player.EventListener, OnAudioFocusChangeListener {

    val ACTION_PLAY = "com.mcakir.radio.player.ACTION_PLAY"
    val ACTION_PAUSE = "com.mcakir.radio.player.ACTION_PAUSE"
    val ACTION_STOP = "com.mcakir.radio.player.ACTION_STOP"

    private val iBinder: IBinder = LocalBinder()

    private var handler: Handler? = null
    private val BANDWIDTH_METER = DefaultBandwidthMeter()
    private var exoPlayer: SimpleExoPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var transportControls: MediaControllerCompat.TransportControls? = null

    private var onGoingCall = false
    private var telephonyManager: TelephonyManager? = null

    private var wifiLock: WifiLock? = null

    private var audioManager: AudioManager? = null

    //private var notificationManager: MediaNotificationManager? = null

    private var status: String? = null

    private var strAppName: String? = null
    private var strLiveBroadcast: String? = null
    private var streamUrl: String? = null

    inner class LocalBinder: Binder() {
        fun getService(): StreamService{
         return this@StreamService
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pause()
        }
    }

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK
                || state == TelephonyManager.CALL_STATE_RINGING
            ) {
                if (!isPlaying()) return
                onGoingCall = true
                stop()
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                if (!onGoingCall) return
                onGoingCall = false
                resume()
            }
        }
    }

    private val mediasSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPause() {
                super.onPause()
                pause()
            }

            override fun onStop() {
                super.onStop()
                stop()
                //notificationManager.cancelNotify()
            }

            override fun onPlay() {
                super.onPlay()
                resume()
            }
        }

    override fun onBind(intent: Intent?): IBinder? {
        return iBinder
    }

    override fun onCreate() {
        super.onCreate()
        strAppName = "App Name"
        strLiveBroadcast = "Broadcast Name"
        onGoingCall = false
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        //notificationManager = MediaNotificationManager(this)
        wifiLock =
            (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mcScPAmpLock")
        mediaSession = MediaSessionCompat(this, javaClass.simpleName)
        transportControls = mediaSession!!.controller.transportControls
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession!!.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "...")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, strAppName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, strLiveBroadcast)
                .build()
        )
        mediaSession!!.setCallback(mediasSessionCallback)
        telephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager!!.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        handler = Handler()
        val bandwidthMeter = DefaultBandwidthMeter()
        val trackSelectionFactory =
            AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        exoPlayer = ExoPlayerFactory.newSimpleInstance(applicationContext, trackSelector)
        exoPlayer!!.addListener(this)
        registerReceiver(
            becomingNoisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        )
        //status = PlaybackStatus.IDLE
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (TextUtils.isEmpty(action)) return START_NOT_STICKY
        val result = audioManager!!.requestAudioFocus(
            this,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stop()
            return START_NOT_STICKY
        }
        if (action.equals(ACTION_PLAY, ignoreCase = true)) {
            transportControls!!.play()
        } else if (action.equals(ACTION_PAUSE, ignoreCase = true)) {
            transportControls!!.pause()
        } else if (action.equals(ACTION_STOP, ignoreCase = true)) {
            transportControls!!.stop()
        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        //if (status == PlaybackStatus.IDLE) stopSelf()
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {}

    override fun onDestroy() {
        pause()
        exoPlayer!!.release()
        exoPlayer!!.removeListener(this)
        if (telephonyManager != null) telephonyManager!!.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_NONE
        )
        //notificationManager.cancelNotify()
        mediaSession!!.release()
        unregisterReceiver(becomingNoisyReceiver)
        super.onDestroy()
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                exoPlayer!!.volume = 0.8f
                resume()
            }
            AudioManager.AUDIOFOCUS_LOSS -> stop()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> if (isPlaying()) pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> if (isPlaying()) exoPlayer!!.volume =
                0.1f
        }
    }
//
//    fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
//        status = when (playbackState) {
//            Player.STATE_BUFFERING -> PlaybackStatus.LOADING
//            Player.STATE_ENDED -> PlaybackStatus.STOPPED
//            Player.STATE_IDLE -> PlaybackStatus.IDLE
//            Player.STATE_READY -> if (playWhenReady) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
//            else -> PlaybackStatus.IDLE
//        }
//        if (status != PlaybackStatus.IDLE) notificationManager.startNotify(status)
//        EventBus.getDefault().post(status)
//    }

    override fun onLoadingChanged(isLoading: Boolean) {}

//    fun onPlayerError(error: ExoPlaybackException?) {
//        EventBus.getDefault().post(PlaybackStatus.ERROR)
//    }

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {}
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    }

    override fun onSeekProcessed() {}
    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
    }

    fun play(streamUrl: String?) {
        this.streamUrl = streamUrl
        if (wifiLock != null && !wifiLock!!.isHeld) {
            wifiLock!!.acquire()
        }
        //        DefaultHttpDataSourceFactory dataSourceFactory = new DefaultHttpDataSourceFactory(getUserAgent());
        val dataSourceFactory =
            DefaultDataSourceFactory(this, getUserAgent(), BANDWIDTH_METER)
        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .setExtractorsFactory(DefaultExtractorsFactory())
            .createMediaSource(Uri.parse(streamUrl))
        exoPlayer!!.prepare(mediaSource)
        exoPlayer!!.playWhenReady = true
    }

    fun resume() {
        if (streamUrl != null) play(streamUrl)
    }

    fun pause() {
        exoPlayer!!.playWhenReady = false
        audioManager!!.abandonAudioFocus(this)
        wifiLockRelease()
    }

    fun stop() {
        exoPlayer!!.stop()
        audioManager!!.abandonAudioFocus(this)
        wifiLockRelease()
    }

    fun playOrPause(url: String) {
        if (streamUrl != null && streamUrl == url) {
            if (!isPlaying()) {
                play(streamUrl)
            } else {
                pause()
            }
        } else {
            if (isPlaying()) {
                pause()
            }
            play(url)
        }
    }

    fun getStatus(): String? {
        return status
    }

    fun getMediaSession(): MediaSessionCompat? {
        return mediaSession
    }

    fun isPlaying(): Boolean {
        return true
    }

    private fun wifiLockRelease() {
        if (wifiLock != null && wifiLock!!.isHeld) {
            wifiLock!!.release()
        }
    }

    private fun getUserAgent(): String? {
        return Util.getUserAgent(this, javaClass.simpleName)
    }

    override fun onMetadata(metadata: Metadata) {

    }
}