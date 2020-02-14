package ua.ck.streamaudioapp.manager

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import ua.ck.streamaudioapp.service.StreamService

class StreamManager(
    private val context: Context
) {

    private var streamService: StreamService? = null
    private var serviceBound: Boolean = false

    fun bind() {
        context.apply {
            val intentStartStreamService = Intent(this, StreamService::class.java)
            bindService(intentStartStreamService, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        // TODO: Stream Service Status
        streamService?.let {
            // Service Status
            Log.i("StreamManager", "streamService status: ???")
        }
    }

    fun unBind() {
        context.unbindService(serviceConnection)
    }

    fun playOrPause(streamUrl: String) {
        streamService?.playOrPause(
            streamUrl = streamUrl
        )
    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            Log.i("StreamManager", "onServiceConnected")
            streamService = (iBinder as StreamService.LocalBinder).getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i("StreamManager", "onServiceDisconnected")
            serviceBound = false
        }
    }


}