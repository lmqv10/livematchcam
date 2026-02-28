package it.lmqv.livematchcam.fragments.sports.banners

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.launch
import it.lmqv.livematchcam.databinding.FragmentBannersControlSlimBinding
import it.lmqv.livematchcam.fragments.sports.BaseFiltersControlFragment

class FiltersControlSlimFragment() : BaseFiltersControlFragment() {
    companion object {
        fun newInstance() = FiltersControlSlimFragment()
    }

    private var _binding: FragmentBannersControlSlimBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBannersControlSlimBinding.inflate(inflater, container, false)
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
                    binding.spotBannerSwitch.visibility = View.VISIBLE
                } else {
                    binding.spotBannerSwitch.visibility = View.GONE
                }
            }
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
            MatchRepository.mainBannerURL.collect { mainBannerURL ->
                if (mainBannerURL.isNotEmpty()) {
                    binding.mainBannerSwitch.visibility = View.VISIBLE
                } else {
                    binding.mainBannerSwitch.visibility = View.GONE
                }
            }
        }
        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setMainBannerVisible(isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}