package it.lmqv.livematchcam.fragments.youtube

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.databinding.FragmentYoutubeAccountBinding
import it.lmqv.livematchcam.viewmodels.GoogleAccountViewModel
import kotlinx.coroutines.launch
import kotlin.getValue

class YoutubeAccountFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = YoutubeAccountFragment()
    }

//    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
//        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
//    }
    private val googleAccountViewModel: GoogleAccountViewModel by activityViewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            googleAccountViewModel.handleSignInResult(result.data)
        }
    }

    private var _binding: FragmentYoutubeAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ytSignIn.btnYouTubeConnectSmall.setOnClickListener { _ ->
            signInLauncher.launch(googleAccountViewModel.getSignInIntent())
        }

        binding.ytSignOut.btnYouTubeDisconnectSmall.setOnClickListener { _ ->
            googleAccountViewModel.signOut()
        }

        lifecycleScope.launch {
            /*combine(
                googleAccountViewModel.authState,
                googleAccountViewModel.firebaseAccountKey
            ) { state, accountKey -> Pair(state, accountKey) }*/
            googleAccountViewModel.authState.collect { state ->
                val isLogged = googleAccountViewModel.isLogged()
                val accountDesc = googleAccountViewModel.accountDesc()

                binding.ytAccountName.text = accountDesc
                binding.ytSignIn.btnYouTubeConnectSmall.isVisible = !isLogged
                binding.ytSignOut.btnYouTubeDisconnectSmall.isVisible = isLogged
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}