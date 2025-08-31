package it.lmqv.livematchcam.fragments

import android.os.Bundle
import android.text.InputFilter
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentServerBinding
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.ServerViewModel

class ServerFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ServerFragment()
    }

    private val serverViewModel: ServerViewModel by activityViewModels()

    private var _binding: FragmentServerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            serverViewModel.serverURI.collect { serverURI ->
                MatchRepository.setRTMPServer(serverURI)
            }
        }

        launchOnStarted {
            serverViewModel.currentServer.collect { currentServer ->
                if (currentServer != null && binding.textServer.text.toString() != currentServer) {
                    binding.textServer.text = currentServer
                }
            }
        }

        launchOnStarted {
            serverViewModel.currentKey.collect { currentKey ->
                if (currentKey != null && binding.textKey.text.toString() != currentKey) {
                    binding.textKey.text = currentKey
                }
            }
        }

        binding.textServer.setOnClickListener {
            val sourceServer = binding.textServer.text.toString()
            requireContext().showEditStringDialog(R.string.rtmp_server, sourceServer, arrayOf<InputFilter>()) { updatedServer ->
                serverViewModel.setCurrentServer(updatedServer)
                requireActivity().hideSystemUI()
            }
        }

        binding.textKey.setOnClickListener {
            val sourceKey = binding.textKey.text.toString()
            requireContext().showEditStringDialog(R.string.stream_key, sourceKey, arrayOf<InputFilter>()) { updatedKey ->
                serverViewModel.setCurrentKey(updatedKey)
                requireActivity().hideSystemUI()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}