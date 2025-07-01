package it.lmqv.livematchcam.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.databinding.FragmentServersBinding
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.viewmodels.StreamersViewModel
import kotlinx.coroutines.launch

interface IServersFragment { }

class ServersFragment : Fragment() , IServersFragment {

    companion object {
        fun newInstance() = ServersFragment()
    }

    private val streamersViewModel: StreamersViewModel by activityViewModels()
    private var _binding: FragmentServersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchOnStarted {
            streamersViewModel.currentServer.collect { currentServer ->
                if (currentServer != null && binding.edittextServer.text.toString() != currentServer) {
                    binding.edittextServer.text = Editable.Factory.getInstance().newEditable(currentServer)
                }
            }
        }

        launchOnStarted {
            streamersViewModel.currentKey.collect { currentKey ->
                if (currentKey != null && binding.edittextKey.text.toString() != currentKey) {
                    binding.edittextKey.text = Editable.Factory.getInstance().newEditable(currentKey)
                }
            }
        }

        binding.edittextServer.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            try {
                if (hasFocus && _binding != null) {
                    binding.edittextServer.post { binding.edittextServer.selectAll() }
                }
            } catch (_: Exception) { }
        }

        binding.edittextServer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(newServer: CharSequence, start: Int, before: Int, count: Int) {
                lifecycleScope.launch {
                    val updatedServer = newServer.toString()
                    streamersViewModel.setCurrentServer(updatedServer)
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        binding.edittextKey.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.edittextKey.post { binding.edittextKey.selectAll() }
            }
        }
        binding.edittextKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(newKey: CharSequence, start: Int, before: Int, count: Int) {
                lifecycleScope.launch {
                    val updatedKey = newKey.toString()
                    if (binding.edittextKey.text.toString() != updatedKey) {
                        streamersViewModel.setCurrentKey(updatedKey)
                    }
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}