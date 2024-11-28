package it.lmqv.livematchcam.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardBinding
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.viewmodels.AwayScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.HomeScoreBoardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SoccerScoreBoardFragment : Fragment(), IScoreBoardFragment {

    companion object {
        fun newInstance() = SoccerScoreBoardFragment()
    }

    private var onUpdateCallback: IScoreBoardFragment.OnUpdateCallback? = null
    override fun setOnUpdate(callback: IScoreBoardFragment.OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    private val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    private val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

    private var _binding: FragmentSoccerScoreBoardBinding? = null
    private val binding get() = _binding!!

    private var currentPeriod = "1T"
    private var timeElapsedInSeconds = 0
    private var job: Job? = null
    private var isInPause = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoccerScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        homeTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }
        awayTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.homeLogo.setShirtByColor(color)
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.awayLogo.setShirtByColor(color)
        }
    }

    override fun startTime() {
        this.isInPause = false
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    binding.matchTime.text = formatTime(timeElapsedInSeconds)
                    delay(1000)
                    if (!isInPause) {
                        timeElapsedInSeconds++
                        onUpdateCallback?.refresh()
                    }
                }
            }
        }
    }

    override fun pauseTime() {
        isInPause = true
    }

    override fun resetTime() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
        isInPause = false
        binding.matchTime.text = formatTime(0)
        onUpdateCallback?.refresh()
    }

    override fun isInPause() : Boolean {
        return this.isInPause
    }

    override fun togglePeriod() {
        currentPeriod = if (currentPeriod == "1T") "2T" else "1T"
        binding.matchPeriod.text = currentPeriod
        onUpdateCallback?.refresh()
    }

    override fun getBitmapView(callback: (Bitmap) -> Unit) {
        val view = this.view
        view?.post {
            val width = view.width
            val height = view.height
            /*
            var aspectRatio = width.toDouble() / height.toDouble()
            var factorFragment = width.toDouble() / height.toDouble()

            val screenWidth = ScreenUtils.getScreenWidth(requireContext())
            val screenHeight = ScreenUtils.getScreenHeight(requireContext())
            var factor = screenWidth.toDouble() / screenHeight.toDouble()

            //var factorHeight = 720
            //var factorWidth =  factorHeight * factor

            var scaleX = 15f //(scaleY * factor).toFloat();
            var scaleY = 15f
            */
            val scoreBoardBitmap : Bitmap
            if (width > 0 && height > 0) {
                scoreBoardBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            } else {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_launcher_background)!!
                scoreBoardBitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }
            val canvas = Canvas(scoreBoardBitmap)
            view.draw(canvas)

            callback(scoreBoardBitmap)
        }

    }
}