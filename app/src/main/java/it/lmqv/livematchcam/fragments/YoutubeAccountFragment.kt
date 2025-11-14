package it.lmqv.livematchcam.fragments

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.BroadcastsAdapter
import it.lmqv.livematchcam.adapters.LiveBroadcastItem
import it.lmqv.livematchcam.databinding.FragmentYoutubeAccountBinding
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.databinding.FragmentYoutubeBinding
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.showQRCode
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.viewmodels.GoogleAccountViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.max

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

        binding.ytSignIn.setOnClickListener { _ ->
            signInLauncher.launch(googleAccountViewModel.getSignInIntent())
        }

        binding.ytSignOut.setOnClickListener { _ ->

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
                binding.ytSignIn.isVisible = !isLogged
                binding.ytSignOut.isVisible = isLogged
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}