package it.lmqv.livematchcam.fragments.sports.banners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import it.lmqv.livematchcam.databinding.FragmentBannersControlBinding
import it.lmqv.livematchcam.fragments.sports.BaseBannersControlFragment
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler

class BannersControlFragment() : BaseBannersControlFragment() {
    companion object {
        fun newInstance() = BannersControlFragment()
    }

    private var _binding: FragmentBannersControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBannersControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            MatchRepository.spotBannerVisible.collect { isVisible ->
                binding.spotBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            MatchRepository.spotBannerURL.collect { spotBannerURL ->
                if (spotBannerURL.isNotEmpty()) {
                    binding.spotBannerPreview.load(spotBannerURL) {
                        placeholder(R.drawable.preview_missing)
                        error(R.drawable.preview_missing)
                        allowHardware(false)
                        listener(
                            onError = { _, error ->
                                binding.spotBannerSwitch.isEnabled = false
                                binding.spotBannerTrash.visibility = View.GONE
                            },
                            onSuccess = { _, result ->
                                binding.spotBannerSwitch.isEnabled = true
                                binding.spotBannerTrash.visibility = View.VISIBLE
                            }
                        )
                    }
                } else {
                    binding.spotBannerSwitch.isEnabled = false
                    binding.spotBannerTrash.visibility = View.GONE
                    val drawable =
                        ContextCompat.getDrawable(requireContext(), R.drawable.preview_missing)
                    binding.spotBannerPreview.setImageDrawable(drawable)
                }
            }
        }

        binding.spotBannerTrash.setOnClickListener {
            MatchRepository.setSpotBannerURL("")
        }

        binding.spotBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setSpotBannerVisible(isChecked)
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerVisible.collect { isVisible ->
                binding.mainBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerURL.collect { spotBannerURL ->
                if (spotBannerURL.isNotEmpty()) {
                    binding.mainBannerPreview.load(spotBannerURL) {
                        placeholder(R.drawable.preview_missing)
                        error(R.drawable.preview_missing)
                        allowHardware(false)
                        listener(
                            onError = { _, error ->
                                binding.mainBannerSwitch.isEnabled = false
                                binding.mainBannerTrash.visibility = View.GONE
                            },
                            onSuccess = { _, result ->
                                binding.mainBannerSwitch.isEnabled = true
                                binding.mainBannerTrash.visibility = View.VISIBLE
                            }
                        )
                    }
                } else {
                    binding.mainBannerSwitch.isEnabled = false
                    binding.mainBannerTrash.visibility = View.GONE
                    val drawable =
                        ContextCompat.getDrawable(requireContext(), R.drawable.preview_missing)
                    binding.mainBannerPreview.setImageDrawable(drawable)
                }
            }
        }

        binding.mainBannerTrash.setOnClickListener {
            MatchRepository.setMainBannerURL("")
        }

        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setMainBannerVisible(isChecked)
        }

        binding.spotBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                var banner = MatchRepository.spotBannerURL.first()
                DialogHandler.editText(DialogContext(this@BannersControlFragment, binding.spotBannerPreview, R.string.spot_banner_placeholder, banner)) {
                    MatchRepository.setSpotBannerURL(it)
                }
            }
        }

        binding.mainBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                var mainBannerURL = MatchRepository.mainBannerURL.first()
                DialogHandler.editText(DialogContext(this@BannersControlFragment, binding.mainBannerPreview, R.string.main_banner_placeholder, mainBannerURL)) {
                    MatchRepository.setMainBannerURL(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}