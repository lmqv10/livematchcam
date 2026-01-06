package it.lmqv.livematchcam.fragments.firebase

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.viewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentFirebaseAccountBinding
import it.lmqv.livematchcam.extensions.launchOnCreated
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.viewmodels.FirebaseAccountViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.getValue

class FirebaseAccountFragment : Fragment() {

    private var _binding: FragmentFirebaseAccountBinding? = null
    private val binding get() = _binding!!

    private val firebaseAccountViewModel: FirebaseAccountViewModel by viewModels()

    companion object {
        @JvmStatic
        fun newInstance() = FirebaseAccountFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirebaseAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.accountName.setOnClickListener {
            val sourceName = binding.accountName.text.toString()
            requireContext().showEditStringDialog(R.string.account, sourceName, arrayOf()) { updatedAccountName ->
                binding.accountName.text = updatedAccountName

                if (updatedAccountName.isNotEmpty()) {
                    firebaseAccountViewModel.signIn(updatedAccountName)
                    toast(getString(R.string.logged_in, updatedAccountName))
                } else {
                    firebaseAccountViewModel.signOut {
                        CoroutineScope(Dispatchers.Main).launch {
                            toast(getString(R.string.logged_out))
                        }
                    }
                }
            }
        }

        binding.accountKey.setOnClickListener {
            val sourceKey = binding.accountKey.text.toString()
            requireContext().showEditStringDialog(R.string.account_key, sourceKey, arrayOf()) { updatedAccountKey ->
                binding.accountKey.text = updatedAccountKey
                firebaseAccountViewModel.setAccountKey(updatedAccountKey)
            }
        }

        launchOnCreated {
            combine(
                firebaseAccountViewModel.authState,
                firebaseAccountViewModel.firebaseAccountKey
            ) { state, accountKey -> Pair(state, accountKey) }
                .collect { (state, accountKey) ->
                    val accountName = firebaseAccountViewModel.accountName()

                    binding.accountName.text = accountName ?: ""
                    binding.accountKey.text = accountKey ?: ""

                    val isConnected =
                        !accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()

                    val resIcon = if (isConnected) R.drawable.cloud_check else R.drawable.cloud_cross
                    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(binding.accountKey, resIcon, 0, 0, 0)
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}