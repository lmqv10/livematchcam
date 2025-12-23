package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardLightBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.loadDrawableOffscreen
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.SoccerScore
import it.lmqv.livematchcam.services.counter.CounterServiceConnector
import it.lmqv.livematchcam.services.counter.ICounterListener
import it.lmqv.livematchcam.viewmodels.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoccerScoreboardViewFilterRender(
    applicationContext: Context,
    filterDescriptor: FilterDescriptor = FilterDescriptor()
) : ScoreboardViewFilterRender<FragmentSoccerScoreBoardLightBinding>(applicationContext, filterDescriptor), ICounterListener {

    private var previousHomeLogo: String? = null
    private var previousAwayLogo: String? = null

    private val serviceConnector: CounterServiceConnector

    init {
        val inflater = LayoutInflater.from(applicationContext)
        _binding = FragmentSoccerScoreBoardLightBinding.inflate(inflater)

        serviceConnector = CounterServiceConnector(applicationContext)
        serviceConnector.setCounterListener(this)
    }

    override fun onTick(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
        render()
    }

    override fun release() {
        super.release()
        //Logd("SoccerScoreboardViewRenderer::release")
        serviceConnector.unbind(applicationContext)
        _binding = null
    }

    @Synchronized
    override fun match(match: Match) {
        try {
            //Logd("SoccerScoreboardViewRenderer::match $match")
            this.handleHomeTeam(match)
            this.handleAwayTeam(match)
            render()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("SoccerScoreboardViewRenderer::match Exception:: ${e.message.toString()}")
        }
    }

    @Synchronized
    override fun score(score: IScore) {
        try {
            //Logd("SoccerScoreboardViewRenderer::score $score")
            this.handleScore(score as SoccerScore)
            this.handleCommand(score.command)

            render()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("SoccerScoreboardViewRenderer::score Exception:: ${e.message.toString()}")
        }
    }

    private fun handleHomeTeam(match: Match) {
        binding.homeTeam.text = match.homeTeam

        var homeColorHex = match.homePrimaryColorHex.toColorInt()
        binding.homeColorBar.setBackgroundColor(homeColorHex)

        if (match.homeLogo != this.previousHomeLogo) {
            //Logd("SoccerScoreboardViewRenderer::match.homeLogo ${match.homeLogo}")
            this.previousHomeLogo = match.homeLogo

            if (match.homeLogo.isNotEmpty()) {
                binding.homeLogo.visibility = View.VISIBLE
                binding.homeColorBar.visibility = View.VISIBLE
                binding.homeShirt.visibility = View.INVISIBLE

                CoroutineScope(Dispatchers.Default).launch {
                    val drawable =
                        loadDrawableOffscreen(applicationContext, match.homeLogo, R.drawable.shield)
                    binding.homeLogo.setImageDrawable(drawable)
                    render()
                }
            } else {
                binding.homeLogo.visibility = View.GONE
                binding.homeColorBar.visibility = View.GONE
                binding.homeShirt.visibility = View.VISIBLE
                binding.homeShirt.setShirtByColor(homeColorHex)
            }
        }
    }

    private fun handleAwayTeam(match: Match) {
        binding.awayTeam.text = match.guestTeam

        var awayColorHex = match.guestPrimaryColorHex.toColorInt()
        binding.awayColorBar.setBackgroundColor(awayColorHex)

        if (match.guestLogo != this.previousAwayLogo) {
            //Logd("SoccerScoreboardViewRenderer::match.guestLogo ${match.guestLogo}")
            this.previousAwayLogo = match.guestLogo

            if (match.guestLogo.isNotEmpty()) {
                binding.awayLogo.visibility = View.VISIBLE
                binding.awayColorBar.visibility = View.VISIBLE
                binding.awayShirt.visibility = View.INVISIBLE

                CoroutineScope(Dispatchers.Default).launch {
                    val drawable = loadDrawableOffscreen(
                        applicationContext,
                        match.guestLogo,
                        R.drawable.shield
                    )
                    binding.awayLogo.setImageDrawable(drawable)
                    render()
                }
            } else {
                binding.awayLogo.visibility = View.GONE
                binding.awayColorBar.visibility = View.GONE
                binding.awayShirt.visibility = View.VISIBLE
                binding.awayShirt.setShirtByColor(awayColorHex)
            }
        }
    }

    private fun handleScore(score: SoccerScore) {
        binding.homeScore.text = score.home.toString()
        binding.awayScore.text = score.away.toString()
        binding.matchPeriod.text = score.period
    }

    private fun handleCommand(command: String) {
        if (command == Command.START_TIME.toString()) {
            if (!serviceConnector.isRunning()) {
                serviceConnector.startCounter()
            }
        }
        if (command == Command.PAUSE.toString()) {
            if (serviceConnector.isRunning()) {
                serviceConnector.pauseCounter()
            }
        }
        if (command == Command.RESET_TIME.toString()) {
            if (!serviceConnector.isRunning()) {
                serviceConnector.setCounter(0)
            }
        }
    }
}

/*
open class ScoreBoardFilterRender(
    var scoreBoardFragment: IScoreBoardFragment<BaseScoreBoardFragment>,
    val filterDescriptor: FilterDescriptor = FilterDescriptor()
) : OverlayObjectFilterRender(),
    IScoreBoardFragment.OnUpdateCallback {

    //private var previewVideoStreamData : IVideoStreamData? = null

    init {
        streamObject = ImageStreamObject()
        scoreBoardFragment.setOnUpdate(this)
    }

    override fun drawFilter() {
        super.drawFilter()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, streamObjectTextureId[0])
        GLES20.glUniform1f(uAlphaHandle, if (streamObjectTextureId[0] == -1) 0f else alpha)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
//        if (previewVideoStreamData == null
//            || previewVideoStreamData?.width != videoStreamData.width
//            || previewVideoStreamData?.height != videoStreamData.height) {
//            previewVideoStreamData = videoStreamData

            Logd("ScoreBoardFilterRender :: setVideoStreamData ${videoStreamData.width}x${videoStreamData.height}")
            loadBitmapFromView { bitmap ->
                val defaultScaleX = (bitmap.width * 100 / videoStreamData.width).toFloat()
                val defaultScaleY = (bitmap.height * 100 / videoStreamData.height).toFloat()

                val factorX = filterDescriptor.maxFactor / defaultScaleX
                val scaleX = factorX * defaultScaleX
                val scaleY = factorX * defaultScaleY
                setImage(bitmap)
                setScale(scaleX, scaleY)
                //val position = filterDescriptor.position
                //setPosition(position.x, position.y)
                setPosition(filterDescriptor.translateTo)
            }
//        } else {
//            Logd("ScoreBoardFilterRender :: setVideoStreamData no changes")
//        }
    }

    override fun refresh() {
        Logd("ScoreBoardFilterRender:: refresh()")
        loadBitmapFromView { bitmap ->
            setImage(bitmap)
        }
    }

    private fun loadBitmapFromView(callback: (Bitmap) -> Unit) {
        val view = (this.scoreBoardFragment as Fragment).view
        view?.post {
            val width = view.width
            val height = view.height
            val scoreBoardBitmap : Bitmap
            if (width > 0 && height > 0) {
                scoreBoardBitmap = createBitmap(view.width, view.height)
            } else {
                val drawable = ContextCompat.getDrawable(view.context, R.drawable.preview_missing)!!
                scoreBoardBitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            }
            val canvas = Canvas(scoreBoardBitmap)
            view.draw(canvas)

            callback(scoreBoardBitmap)
        }
    }
}
*/