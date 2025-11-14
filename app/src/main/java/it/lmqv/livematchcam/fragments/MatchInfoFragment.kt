package it.lmqv.livematchcam.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentMatchInfoBinding
import it.lmqv.livematchcam.extensions.launchOnStarted
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.repositories.MatchRepository

class MatchInfoFragment : Fragment() {

    companion object {
        fun newInstance() = MatchInfoFragment()
    }

    private val sportInfoFragment: SportInfoFragment = SportInfoFragment.newInstance()

    private var _binding: FragmentMatchInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.sportsContainer, sportInfoFragment)
            .commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMatchInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("UseKtx")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MatchRepository.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeamControl.setTeamName(homeTeam)
        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            binding.guestTeamControl.setTeamName(guestTeam)
        }

        launchOnStarted {
            MatchRepository.homeLogo.collect { logoUrl ->
                binding.homeTeamControl.setLogoUrl(logoUrl)
            }
        }
        launchOnStarted {
            MatchRepository.guestLogo.collect { logoUrl ->
                binding.guestTeamControl.setLogoUrl(logoUrl)
            }
        }

        launchOnStarted {
            MatchRepository.homePrimaryColorHex.collect { primaryColor ->
                binding.homeTeamControl.setPrimaryColor(primaryColor.toColorInt())
            }
        }
        launchOnStarted {
            MatchRepository.guestPrimaryColorHex.collect { primaryColor ->
                binding.guestTeamControl.setPrimaryColor(primaryColor.toColorInt())
            }
        }

        binding.homeTeamControl.onTeamNameChanged = { updatedTeamName ->
            MatchRepository.setHomeTeam(updatedTeamName)
        }
        binding.homeTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setHomeLogo(updatedLogoUrl)
        }
        binding.homeTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setHomePrimaryColorHex(updatedColor)
        }

        binding.guestTeamControl.onTeamNameChanged = { updatedTeamName ->
            MatchRepository.setGuestTeam(updatedTeamName)
        }
        binding.guestTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setGuestLogo(updatedLogoUrl)
        }
        binding.guestTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setGuestPrimaryColorHex(updatedColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

