package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.services.auth.Account
import it.lmqv.livematchcam.services.firebase.FirebaseAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FirebaseAccountViewModel(private val application: Application) : AndroidViewModel(application)  {

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Unauthenticated)
    val authState: StateFlow<AuthResult> = _authState

//    private val _logoURL = MutableStateFlow<String>("")
//    val logoURL: StateFlow<String> = _logoURL
//    fun setLogo(updatedLogoUrl: String) {
//        if (_logoURL.value != updatedLogoUrl) {
//            _logoURL.value = updatedLogoUrl
//        }
//    }

//    private val _title = MutableStateFlow<String>("")
//    val title: StateFlow<String> = _title

    private val _firebaseAccountKey = MutableStateFlow<String?>(null)
    val firebaseAccountKey: StateFlow<String?> = _firebaseAccountKey.asStateFlow()

    private val _savedAccountName = MutableStateFlow<String?>(null)
    val savedAccountName: StateFlow<String?> = _savedAccountName.asStateFlow()

    fun setAccountKey(accountKey: String?) {
        // Obsolete auto-save method, now handled by validateAndApplyCredentials
    }

    fun validateAndApplyCredentials(name: String, key: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = FirebaseAuthService.authenticateAccount(name, key)
            
            result.onSuccess {
                firebaseAccountRepository.setAccountName(name)
                firebaseAccountRepository.setAccountKey(key)
                _authState.value = AuthResult.Authenticated(Account(name))
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
            
            result.onFailure {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    fun updateLastSignedInAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            val accountName = firebaseAccountRepository.accountName.first()
            if (isLogged() && accountName.isNotEmpty()) {
                _authState.value = AuthResult.Authenticated(Account(accountName))
            } else {
                _authState.value = AuthResult.Unauthenticated
            }
        }
    }

    fun signIn(accountName: String) {
        // Obsolete auto-save method, now handled by validateAndApplyCredentials
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAccountRepository.setAccountName("")
            FirebaseAuthService.logout()
            _authState.value = AuthResult.Unauthenticated
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    fun isLogged() : Boolean
    {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    }

    fun accountName() : String?
    {
        val state = authState.value
        return when (state) {
            is AuthResult.Authenticated -> state.account.name
            is AuthResult.Unauthenticated, is AuthResult.Error -> null
        }
    }

    init {
        // Expose raw offline data for UI fields auto-completion
        viewModelScope.launch {
            firebaseAccountRepository.accountName.collect { accountName ->
                if (_savedAccountName.value != accountName) {
                    _savedAccountName.value = accountName
                }
            }
        }
        viewModelScope.launch {
            firebaseAccountRepository.accountKey.collect { accountKey ->
                if (_firebaseAccountKey.value != accountKey) {
                    _firebaseAccountKey.value = accountKey
                }
            }
        }

        // Single Source Of Truth per l'auth state
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val user = auth.currentUser
            viewModelScope.launch {
                if (user != null) {
                    val savedName = firebaseAccountRepository.accountName.first()
                    if (savedName.isNotEmpty()) {
                        _authState.value = AuthResult.Authenticated(Account(savedName))
                    } else {
                        // User logged in ma nessun nome salvato?
                        _authState.value = AuthResult.Unauthenticated
                    }
                } else {
                    _authState.value = AuthResult.Unauthenticated
                }
            }
        }
    }

}