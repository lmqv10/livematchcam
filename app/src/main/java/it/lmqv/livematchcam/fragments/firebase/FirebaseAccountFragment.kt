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
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
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
            val currentName = binding.accountName.text.toString()
            DialogHandler.editText(DialogContext(this, binding.accountName, R.string.account, currentName)) {
                binding.accountName.text = it
            }
        }

        binding.accountKey.setOnClickListener {
            val currentKey = binding.accountKey.text.toString()
            DialogHandler.editText(DialogContext(this, binding.accountName, R.string.account_key, currentKey)) {
                binding.accountKey.text = it
            }
        }

        binding.btnLogin.setOnClickListener {
            val accountName = binding.accountName.text.toString().trim()
            val accountKey = binding.accountKey.text.toString().trim()

            if (accountName.isEmpty() || accountKey.isEmpty()) {
                binding.errorMessage.visibility = View.VISIBLE
                binding.errorMessage.text = "Inserisci nome e chiave per connetterti."
                return@setOnClickListener
            }

            // Show Loader and hide error
            binding.loaderOverlay.visibility = View.VISIBLE
            binding.errorMessage.visibility = View.GONE

            firebaseAccountViewModel.validateAndApplyCredentials(accountName, accountKey,
                onSuccess = {
                    binding.loaderOverlay.visibility = View.GONE
                    toast(getString(R.string.logged_in, accountName))
                },
                onError = {
                    binding.loaderOverlay.visibility = View.GONE
                    binding.errorMessage.visibility = View.VISIBLE
                    binding.errorMessage.text = "Autenticazione fallita. Controlla le credenziali."
                }
            )
        }

        binding.btnLogout.setOnClickListener {
            firebaseAccountViewModel.signOut {
                toast(getString(R.string.logged_out))
            }
        }

        launchOnCreated {
            combine(
                firebaseAccountViewModel.authState,
                firebaseAccountViewModel.firebaseAccountKey,
                firebaseAccountViewModel.savedAccountName
            ) { state, accountKey, savedName -> Triple(state, accountKey, savedName) }
                .collect { (state, accountKey, savedName) ->
                    val isConnected = state is it.lmqv.livematchcam.services.auth.AuthResult.Authenticated

                    // Update UI from ViewModel only if we are connected (loading saved data)
                    // or if it's completely empty (startup) to not overwrite user typing
                    if (isConnected || (binding.accountName.text.isEmpty() && !savedName.isNullOrEmpty())) {
                        binding.accountName.text = savedName ?: ""
                    }
                    if (isConnected || (binding.accountKey.text.isEmpty() && !accountKey.isNullOrEmpty())) {
                        binding.accountKey.text = accountKey ?: ""
                    }

                    if (isConnected) {
                        // Logged in UI state
                        binding.accountName.isEnabled = false
                        binding.accountKey.isEnabled = false
                        binding.btnLogin.visibility = View.GONE
                        binding.errorMessage.visibility = View.GONE
                        binding.btnLogout.visibility = View.VISIBLE

                        val resIcon = R.drawable.cloud_check
                        binding.accountKey.setCompoundDrawablesRelativeWithIntrinsicBounds(resIcon, 0, 0, 0)
                    } else {
                        // Unauthenticated UI state
                        binding.accountName.isEnabled = true
                        binding.accountKey.isEnabled = true
                        binding.btnLogin.visibility = View.VISIBLE
                        binding.btnLogout.visibility = View.GONE

                        val resIcon = R.drawable.cloud_cross
                        binding.accountKey.setCompoundDrawablesRelativeWithIntrinsicBounds(resIcon, 0, 0, 0)
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}