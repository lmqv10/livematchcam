package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.services.auth.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PiccoAccountViewModel(private val application: Application) : AndroidViewModel(application)  {

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Unauthenticated)
    val authState: StateFlow<AuthResult> = _authState

    private val _firebaseAccountKey = MutableStateFlow<String?>(null)
    val firebaseAccountKey: StateFlow<String?> = _firebaseAccountKey.asStateFlow()
//    fun setAccountKey(accountKey: String?) {
//        viewModelScope.launch(Dispatchers.IO) {
//            if (_firebaseAccountKey.value != accountKey) {
//                firebaseAccountRepository.setAccountKey(accountKey)
//            }
//        }
//    }

    fun updateLastSignedInAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAccountRepository.accountName.collect { accountName ->
                if (accountName.isNotEmpty()) {
                    _authState.value = AuthResult.Authenticated(Account(accountName))
                } else {
                    _authState.value = AuthResult.Unauthenticated
                }
            }
        }
    }

    fun signIn(accountName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAccountRepository.setAccountName(accountName)
            _authState.value = AuthResult.Authenticated(Account(accountName))
        }
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAccountRepository.setAccountName(null)
            _authState.value = AuthResult.Unauthenticated
            onComplete?.invoke()
        }
    }

    fun isLogged() : Boolean
    {
        return authState.value is AuthResult.Authenticated
    }

    fun accountDesc() : String?
    {
        val state = authState.value
        return when (state) {
            is AuthResult.Authenticated -> state.account.name
            is AuthResult.Unauthenticated, is AuthResult.Error -> application.getString(R.string.account_sign_in)
        }
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
        viewModelScope.launch {
            firebaseAccountRepository.accountName.collect { accountName ->
                if (accountName.isNotEmpty()) {
                    _authState.value = AuthResult.Authenticated(Account(accountName))
                } else {
                    _authState.value = AuthResult.Unauthenticated
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
    }

}