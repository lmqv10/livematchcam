package it.lmqv.livematchcam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.KeyDescriptionAdapter
import it.lmqv.livematchcam.databinding.FragmentFirebaseBinding
import it.lmqv.livematchcam.extensions.getItemPositionByKey
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.repositories.KeyDescription
import it.lmqv.livematchcam.viewmodels.FirebaseViewModel
import kotlinx.coroutines.launch

class FirebaseFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FirebaseFragment()
    }

    private val firebaseViewModel: FirebaseViewModel by activityViewModels()

    private var _binding: FragmentFirebaseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirebaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        launchOnStarted {
//            firebaseViewModel.serverURI.collect { serverURI ->
//                MatchRepository.setRTMPServer(serverURI)
//            }
//        }

        launchOnStarted {
            firebaseViewModel.servers.collect { servers ->
                //Logd("$servers")
                binding.spinnerServers.adapter = KeyDescriptionAdapter(requireContext(), servers, R.drawable.ic_server)
            }
        }

        launchOnStarted {
            firebaseViewModel.keys.collect { keys ->
                //Logd("$keys")
                binding.spinnerKeys.adapter = KeyDescriptionAdapter(requireContext(), keys, R.drawable.ic_key)
            }
        }

        launchOnStarted {
            firebaseViewModel.currentServer.collect { currentServer ->
                if (currentServer != null) {
                    //Logd("$currentServer")
                    val selectedPosition = binding.spinnerServers.adapter.getItemPositionByKey(currentServer)
                    binding.spinnerServers.setSelection(selectedPosition)
                }
            }
        }

        launchOnStarted {
            firebaseViewModel.currentKey.collect { currentKey ->
                if (currentKey != null) {
                    //Logd("$currentKey")
                    val selectedPosition = binding.spinnerKeys.adapter.getItemPositionByKey(currentKey)
                    binding.spinnerKeys.setSelection(selectedPosition)
                }
            }
        }

        binding.spinnerServers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //Logd("spinnerServers $position")
                lifecycleScope.launch {
                    val selectedServer = (parent.getItemAtPosition(position) as KeyDescription).key
                    firebaseViewModel.setCurrentServer(selectedServer)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        binding.spinnerKeys.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //Logd("spinnerKeys $position")
                lifecycleScope.launch {
                    val selectedKey = (parent.getItemAtPosition(position) as KeyDescription).key
                    firebaseViewModel.setCurrentKey(selectedKey)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}