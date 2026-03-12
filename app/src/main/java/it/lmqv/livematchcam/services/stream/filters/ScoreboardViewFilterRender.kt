package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLES20
import androidx.core.graphics.createBitmap
import androidx.viewbinding.ViewBinding
import com.pedro.encoder.utils.gl.ImageStreamObject
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.wrapLayout
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.min

abstract class ScoreboardViewFilterRender<T>(val applicationContext: Context)
    : OverlayObjectFilterRender() where T : ViewBinding {

    internal var _binding: T? = null
    internal val binding get() = _binding!!

    private var width: Int = 0
    private var height: Int = 0
    private var scaleFactor: Float = 0f
    private var translateTo: FilterPosition = FilterPosition.TOP_LEFT

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var scoreboardRepositoryJob : Job? = null
    private var matchRepositoryJob : Job? = null
    private var scoreRepositoryJob : Job? = null

    private var isVisible: Boolean = false
    @Volatile
    private var isUpdating: Boolean = false

    private var minimalWidth: Int = Int.MAX_VALUE
    private var minimalHeight: Int = Int.MAX_VALUE

    init {
        streamObject = ImageStreamObject()
    }

    override fun release() {
        super.release()

        this.matchRepositoryJob?.cancel()
        this.scoreRepositoryJob?.cancel()
        this.scoreboardRepositoryJob?.cancel()

        _binding = null
    }

    override fun drawFilter() {
        if (!this.isUpdating) {
            super.drawFilter()
            if (streamObjectTextureId[0] == -1) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            }
            var targetAlpha = if (!isVisible || streamObjectTextureId[0] == -1) { 0f } else { 1f }
            GLES20.glUniform1f(uAlphaHandle, targetAlpha)
        }
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        if (_binding != null) {
            // Logd("ScoreboardViewFilterRender::setVideoStreamData $videoStreamData")
            this.width = videoStreamData.width
            this.height = videoStreamData.height

            binding.root.wrapLayout(width, height)
            minimalWidth = min(minimalWidth, binding.root.measuredWidth)
            minimalHeight = min(minimalHeight, binding.root.measuredHeight)

            this.initializeCollectors()
        }
    }

    private fun initializeCollectors() {
        //Logd("ScoreboardViewFilterRender initializeCollectors")
        this.matchRepositoryJob?.cancel()
        this.matchRepositoryJob = this.coroutineScope.launch {
            MatchRepository.match.collect { match ->
                //Logd("ScoreboardViewFilterRender :: MatchRepository.match.collect :: $match")
                match(match)
            }
        }

        this.scoreRepositoryJob?.cancel()
        this.scoreRepositoryJob = this.coroutineScope.launch {
            MatchRepository.score.collect { score ->
                //Logd("ScoreboardViewFilterRender :: MatchRepository.score.collect :: $score")
                score(score)
            }
        }

        this.scoreboardRepositoryJob?.cancel()
        this.scoreboardRepositoryJob = this.coroutineScope.launch {
            MatchRepository.scoreboard.collect { scoreboard ->
                //Logd("ScoreboardViewFilterRender collect.scoreboard $scoreboard")
                this@ScoreboardViewFilterRender.isVisible = scoreboard.visible
                
                if (isVisible) {
                    this@ScoreboardViewFilterRender.scaleFactor = scoreboard.size.toFloat()
                    this@ScoreboardViewFilterRender.translateTo = scoreboard.position
                    this@ScoreboardViewFilterRender.updateLayout()
                }
            }
        }
    }

    @Synchronized
    open fun updateLayout() {
        //Logd("ScoreboardViewFilterRender::updateLayout")
        this.isUpdating = true
        binding.root.wrapLayout(width, height)
        scaleSprite()
        translateSprite()
        render()
        this.isUpdating = false
    }

    @Synchronized
    open fun updateContentView() {
        //Logd("ScoreboardViewFilterRender::updateContentView")
        this.isUpdating = true
        render()
        this.isUpdating = false
    }

    @Synchronized
    private fun scaleSprite() {
        try {
            //Logd("ScoreboardViewFilterRender::scaleSprite")
            if (_binding != null && minimalWidth > 0 && binding.root.measuredHeight > 0 && isVisible) {
                val streamAspectRatio = width.toFloat() / height.toFloat()

                val viewWidth = binding.root.measuredWidth.toFloat()
                val viewHeight = binding.root.measuredHeight.toFloat()
                val viewAspectRatio = viewWidth / viewHeight

                val adaptedFactorWidth = this.scaleFactor * (viewWidth / minimalWidth)
                val adaptedFactorHeight = adaptedFactorWidth * (streamAspectRatio / viewAspectRatio)

                //Logd("ScoreboardViewFilterRender::scaleSprite -> X: $adaptedFactorWidth, Y: $adaptedFactorHeight")
                setScale(adaptedFactorWidth, adaptedFactorHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("ScoreboardViewRenderer::scaleSprite Exception:: ${e.message.toString()}")
        }
    }

    @Synchronized
    private fun translateSprite() {
        try {
            //Logd("ScoreboardViewFilterRender::translateSprite")
            if (isVisible) {
                var adaptedFactorWidth = scale.x
                var adaptedFactorHeight = scale.y

                when (this.translateTo) {
                    FilterPosition.TOP_LEFT -> setPosition(0f, 0f)
                    FilterPosition.TOP -> setPosition(50f - adaptedFactorWidth / 2f, 0f)
                    FilterPosition.TOP_RIGHT -> setPosition(100f - adaptedFactorWidth, 0f)
                    FilterPosition.CENTER -> setPosition(50f - adaptedFactorWidth / 2f, 50f - adaptedFactorHeight / 2f)
                    FilterPosition.BOTTOM_LEFT -> setPosition(0f, 100f - adaptedFactorHeight)
                    FilterPosition.BOTTOM -> setPosition(50f - adaptedFactorWidth / 2f, 100f - adaptedFactorHeight)
                    FilterPosition.BOTTOM_RIGHT -> setPosition(100f - adaptedFactorWidth, 100f - adaptedFactorHeight)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("ScoreboardViewRenderer::translateSprite Exception:: ${e.message.toString()}")
        }
    }

    @Synchronized
    private fun render() {
        try {
            if (_binding != null && binding.root.measuredWidth > 0 && binding.root.measuredHeight > 0 && isVisible) {
                var bitmap = createBitmap(binding.root.measuredWidth, binding.root.measuredHeight)
                var canvas = Canvas(bitmap)
                binding.root.draw(canvas)
                //Logd("ScoreboardViewFilterRender::render bitmap ${bitmap.width}x${bitmap.height}")
                setImage(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("ScoreboardViewRenderer::render Exception:: ${e.message.toString()}")
        }
    }

    abstract fun match(match: Match)
    abstract fun score(score: IScore)
}
