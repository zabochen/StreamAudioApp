package ua.ck.streamaudioapp

import android.animation.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_main.*
import ua.ck.streamaudioapp.manager.StreamManager

class MainActivity : AppCompatActivity() {

    private lateinit var streamManager: StreamManager

    private lateinit var objectAnimatorBig: ObjectAnimator
    private var bigCurrentPlayTime: Long = -1

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

        // Init Views
        initViews()

        // Init Actions
        initActions()
    }

    private fun initViews() {
        // Cover Image
        initCoverImage()
    }

    private fun initCoverImage() {
        Glide.with(this)
            .load("https://c8.radioboss.fm/w/artwork/125.png")
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {

                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {

                    Log.i("MainActivity", "onResourceReady")

                    resource?.let { workWithPalette(it) }

                    return false
                }
            })
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(activityMain_imageView_cover)
    }

    private fun workWithPalette(drawable: Drawable) {
        // https://developer.android.com/reference/androidx/palette/graphics/Palette.html
        // https://developer.android.com/training/material/palette-colors
        // https://stackoverflow.com/questions/8471236/finding-the-dominant-color-of-an-image-in-an-android-drawable

        val bitmap: Bitmap = (drawable as BitmapDrawable).bitmap
        //val bitmap = BitmapFactory.decodeResource(resources, drawable)

        Palette.from(bitmap).generate {
            it?.let {

                // RGB
                val dominantColor = it.getDominantColor(
                    ContextCompat.getColor(this, R.color.colorAccent)
                )

                val vibrant = it.lightMutedSwatch

                activityMain_constraintLayout_parent.setBackgroundColor(
                    vibrant?.rgb ?: ContextCompat.getColor(this, R.color.colorAccent)
                )
            }
        }
    }

    private fun initActions() {
        // Button "Play Stream"
        initActionButtonPlayStream()

        // Button "Pause Stream"
        initActionButtonPauseStream()

        // Button "Play"
        initActionButtonPlay()
    }

    private fun initActionButtonPlayStream() {
        activityMain_button_playStream.setOnClickListener {
            //this.streamManager.playOrPause("http://c8.radioboss.fm:8125/stream")
            loadAnimation()
        }
    }

    private fun initActionButtonPauseStream() {
        activityMain_button_pauseStream.setOnClickListener {
            this.bigCurrentPlayTime = this.objectAnimatorBig.currentPlayTime
            this.objectAnimatorBig.cancel()
        }
    }

    private fun initActionButtonPlay() {
        activityMain_imageView_buttonPlay.setOnClickListener {
            loadAnimation()
        }
    }

    private fun loadAnimation() {

        Log.i("MainActivity", "${activityMain_imageView_ellipseBig.rotation}")


        val ellipseBigStartAnimation = ObjectAnimator
            .ofFloat(
                activityMain_imageView_ellipseBig,
                "rotation",
                activityMain_imageView_ellipseBig.rotation,
                90f

            ).apply {
                duration = 1500
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)

                        Log.i("MainActivity", "onAnimationEnd")

                        ObjectAnimator
                            .ofFloat(
                                activityMain_imageView_ellipseBig,
                                "rotation",
                                activityMain_imageView_ellipseBig.rotation,
                                315f
                            ).apply {
                                duration = 1500
                            }
                    }
                })
                //start()
            }

        if (bigCurrentPlayTime > 0) {

            Log.i("MainActivity", "currentPlayTime_1")


            this.objectAnimatorBig.apply {
                currentPlayTime = bigCurrentPlayTime
                start()
            }
        } else {

            Log.i("MainActivity", "currentPlayTime_2")
            this.objectAnimatorBig = ObjectAnimator
                .ofFloat(
                    activityMain_imageView_ellipseBig,
                    "rotation",
                    activityMain_imageView_ellipseBig.rotation,
                    90f
                ).apply {
                    duration = 1500
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                    start()
                }
        }


        val anim2 = ObjectAnimator
            .ofFloat(
                activityMain_imageView_ellipseBig,
                "rotation",
                activityMain_imageView_ellipseBig.rotation,
                315f
            ).apply {
                duration = 1500
            }


//        with(AnimationUtils.loadAnimation(this, R.anim.rotate_start)){
//            activityMain_imageView_ellipseBig.startAnimation(this)
//            setAnimationListener(object : Animation.AnimationListener{
//                override fun onAnimationRepeat(animation: Animation?) {
//                }
//
//                override fun onAnimationEnd(animation: Animation?) {
//                    with(AnimationUtils.loadAnimation(applicationContext, R.anim.rotate_back)){
//                        activityMain_imageView_ellipseBig.startAnimation(this)
//                    }
//                }
//
//                override fun onAnimationStart(animation: Animation?) {
//                }
//            })
//        }


    }
}
