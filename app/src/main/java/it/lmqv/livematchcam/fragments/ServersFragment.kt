package it.lmqv.livematchcam.fragments

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.GlobalDataManager
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentServersBinding
import it.lmqv.livematchcam.databinding.FragmentStatusBinding
import it.lmqv.livematchcam.extensions.bitrateFormat
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.settings.SettingsRepository
import it.lmqv.livematchcam.settings.StreamSettingsRepository
import it.lmqv.livematchcam.utils.KeyValue
import kotlinx.coroutines.launch

class ServersFragment : Fragment() {

    companion object {
        fun newInstance() = ServersFragment()
    }

    private lateinit var streamSettingsRepository: StreamSettingsRepository

    private val serversViewModel: ServersViewModel by viewModels()
    private var _binding: FragmentServersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.streamSettingsRepository = StreamSettingsRepository(requireContext())

        _binding = FragmentServersBinding.inflate(inflater, container, false)

        this.launchOnStarted {
            streamSettingsRepository.getServers.collect { servers ->
                serversViewModel.setServers(servers)
            }
        }

        this.launchOnStarted {
            streamSettingsRepository.getKeys.collect { keys ->
                serversViewModel.setKeys(keys)
            }
        }

        this.launchOnStarted {
            streamSettingsRepository.getCurrentServer.collect { server ->
                //if (binding.edittextServer.text.toString() != server) {
                    binding.edittextServer.text = Editable.Factory.getInstance().newEditable(server)
                //}
            }
        }

        this.launchOnStarted {
            streamSettingsRepository.getCurrentKey.collect { key ->
                //if (binding.edittextKey.text.toString() != key) {
                    binding.edittextKey.text = Editable.Factory.getInstance().newEditable(key)
                //}
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       super.onViewCreated(view, savedInstanceState)

        serversViewModel.servers.observe(viewLifecycleOwner, Observer { servers ->
            val adapterServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, servers)
            adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerServers.adapter = adapterServer
        })

        serversViewModel.keys.observe(viewLifecycleOwner, Observer { keys ->
            val adapterServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, keys)
            adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerKeys.adapter = adapterServer
        })

        binding.spinnerServers.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    val selectedServer = parent.getItemAtPosition(position) as KeyValue<String>
                    streamSettingsRepository.setCurrentServer(selectedServer.key)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        binding.spinnerKeys.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    val selectedKey = parent.getItemAtPosition(position) as KeyValue<String>
                    streamSettingsRepository.setCurrentKey(selectedKey.key)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        binding.edittextServer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                lifecycleScope.launch {
                    val newServer = s.toString()
                    streamSettingsRepository.setCurrentServer(newServer)
                }
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        binding.edittextKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                lifecycleScope.launch {
                    val newKey = s.toString()
                    streamSettingsRepository.setCurrentKey(newKey)
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