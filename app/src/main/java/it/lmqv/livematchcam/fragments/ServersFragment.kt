package it.lmqv.livematchcam.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.databinding.FragmentServersBinding
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.utils.getItemPositionByKey
import it.lmqv.livematchcam.viewmodels.StreamersViewModel
import kotlinx.coroutines.launch

interface IServersFragment {
    fun getServerURI() : String
}

class ServersFragment : Fragment(), IServersFragment {

    companion object {
        fun newInstance() = ServersFragment()
    }

    private val streamersViewModel: StreamersViewModel by viewModels()
    private var _binding: FragmentServersBinding? = null
    private val binding get() = _binding!!

    override fun getServerURI() : String {
        return streamersViewModel.getServerURI()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            streamersViewModel.servers.collect { servers ->
                val adapterServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servers)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerServers.adapter = adapterServer
                binding.spinnerServers.setSelection(0)

                val currentServer = streamersViewModel.getCurrentServer()
                binding.edittextServer.text = Editable.Factory.getInstance().newEditable(currentServer)
            }
        }

        lifecycleScope.launch {
            streamersViewModel.keys.collect { keys ->
                val adapterServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, keys)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerKeys.adapter = adapterServer
            }
        }

        lifecycleScope.launch {
            streamersViewModel.currentKey.collect { currentKey ->
                val selectedPosition = binding.spinnerKeys.adapter.getItemPositionByKey(currentKey)
                binding.spinnerKeys.setSelection(selectedPosition)
                if (binding.edittextKey.text.toString() != currentKey) {
                    binding.edittextKey.text = Editable.Factory.getInstance().newEditable(currentKey)
                }
            }
        }

        binding.spinnerServers.isEnabled = false
        binding.spinnerServers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    //val selectedServer = (parent.getItemAtPosition(position) as KeyValue<String>).key
                    //streamersViewModel.setCurrentServer(selectedServer)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        binding.spinnerKeys.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    val selectedKey = (parent.getItemAtPosition(position) as KeyValue<String>).key
                    if (position > 0) {
                        streamersViewModel.setCurrentKey(selectedKey)
                        binding.edittextKey.text = Editable.Factory.getInstance().newEditable(selectedKey)
                    }
                    binding.edittextKey.isEnabled = position == 0
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        binding.edittextKey.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.edittextKey.post { binding.edittextKey.selectAll() }
            }
        }
        binding.edittextKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(newKey: CharSequence, start: Int, before: Int, count: Int) {
                lifecycleScope.launch {
                    streamersViewModel.setCurrentKey(newKey.toString())
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