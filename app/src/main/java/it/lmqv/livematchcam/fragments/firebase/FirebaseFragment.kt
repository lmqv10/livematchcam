package it.lmqv.livematchcam.fragments.firebase

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.BaseStreamItem
import it.lmqv.livematchcam.adapters.StreamsAdapter
import it.lmqv.livematchcam.databinding.FragmentFirebaseBinding
import it.lmqv.livematchcam.extensions.launchOnCreated
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.FirebaseAccountViewModel
import it.lmqv.livematchcam.viewmodels.FirebaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.math.max

class FirebaseFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FirebaseFragment()
    }

    private val firebaseViewModel: FirebaseViewModel by activityViewModels()
    private val firebaseAccountViewModel: FirebaseAccountViewModel by activityViewModels()

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

        launchOnCreated {
            combine(
                MatchRepository.firebaseAccountData,
                firebaseAccountViewModel.firebaseAccountKey,
                firebaseViewModel.currentKey
            ) { firebaseAccountData, firebaseAccountKey, currentKey -> Triple(firebaseAccountData, firebaseAccountKey, currentKey) }
                .collect { (firebaseAccountData, firebaseAccountKey, currentKey) ->

                var sourceLogoUrl = firebaseAccountData.logoURL
                if (sourceLogoUrl.isEmpty()) {
                    binding.accountLogo.setImageResource(R.drawable.ic_link)
                } else {
                    binding.accountLogo.load(sourceLogoUrl) {
                        placeholder(R.drawable.refresh)
                        error(R.drawable.ic_link)
                        allowHardware(false)
                    }
                }

                binding.accountName.text = firebaseAccountViewModel.accountName() ?: ""

                var streams = firebaseAccountData.streams
                var streamItems = streams
                    .map { x -> BaseStreamItem.MatchStream(x.logo, x.description, x.server, x.key) }

                binding.spinnerStreams.adapter = StreamsAdapter(requireActivity(), streamItems)
                binding.spinnerStreams.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        lifecycleScope.launch {
                            val selectedStream =
                                parent.getItemAtPosition(position) as BaseStreamItem.MatchStream
                            firebaseViewModel.setCurrentServer(selectedStream.server)
                            firebaseViewModel.setCurrentKey(selectedStream.key)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }

                if (currentKey != null) {
                    var adapter = binding.spinnerStreams.adapter
                    val itemsList = List(adapter.count) { index ->
                        adapter.getItem(index)!! as BaseStreamItem.MatchStream
                    }
                    var selectedPosition = max(0, itemsList.indexOfFirst { it.key == currentKey })
                    binding.spinnerStreams.setSelection(selectedPosition)
                }
            }
        }

        binding.accountName.setOnClickListener {
            val accountName = binding.accountName.text.toString()
            DialogHandler.editText(DialogContext(this, binding.accountName, R.string.account, accountName)) {
                binding.accountName.text = it

                if (it.isNotEmpty()) {
                    firebaseAccountViewModel.signIn(it)
                    toast(getString(R.string.logged_in, it))
                } else {
                    firebaseAccountViewModel.signOut {
                        CoroutineScope(Dispatchers.Main).launch {
                            toast(getString(R.string.logged_out))
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}