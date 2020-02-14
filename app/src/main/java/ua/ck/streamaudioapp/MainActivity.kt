package ua.ck.streamaudioapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import ua.ck.streamaudioapp.manager.StreamManager

class MainActivity : AppCompatActivity() {

    private lateinit var streamManager: StreamManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUI()

        this.streamManager = StreamManager(applicationContext)
    }

    override fun onResume() {
        super.onResume()
        this.streamManager.bind()
    }

    override fun onDestroy() {
        this.streamManager.unBind()
        super.onDestroy()
    }

    private fun setUI() {
        // Layout
        setContentView(R.layout.activity_main)

        // Init Actions
        initActions()
    }

    private fun initActions() {
        // Button "Play Stream"
        initActionButtonPlayStream()

        // Button "Pause Stream"
        initActionButtonPauseStream()
    }

    private fun initActionButtonPlayStream() {
        activityMain_button_playStream.setOnClickListener {
            this.streamManager.playOrPause("http://c8.radioboss.fm:8125/stream")
        }
    }

    private fun initActionButtonPauseStream() {
        activityMain_button_pauseStream.setOnClickListener {

        }
    }
}
