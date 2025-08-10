package it.lmqv.livematchcam.viewmodels

import android.accounts.Account
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.auth.AuthResult
import it.lmqv.livematchcam.auth.GoogleAuthManager
import it.lmqv.livematchcam.repositories.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class AccountViewModel(private val application: Application) : AndroidViewModel(application)  {

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)
    private val authManager = GoogleAuthManager(application)

    val authState: StateFlow<AuthResult> = authManager.authState

    fun getSignInIntent(): Intent = authManager.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        authManager.handleSignInResult(data)
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        authManager.signOut(onComplete)
    }

    //private val _account = MutableStateFlow<Account?>(null)
    //val account: StateFlow<Account?> = _account
    /*fun setAccount(account: Account?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_account.value != account) {
                _account.value = account
                firebaseAccountRepository.setAccountGoogle(account?.name)
            }
        }
    }*/

    private val _firebaseAccountKey = MutableStateFlow<String?>(null)
    val firebaseAccountKey: StateFlow<String?> = _firebaseAccountKey.asStateFlow()
    fun setAccountKey(accountKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_firebaseAccountKey.value != accountKey) {
                firebaseAccountRepository.setAccountKey(accountKey)
            }
        }
    }

    fun isLogged() : Boolean
    {
        val state = authState.value
        return when (state) {
            is AuthResult.Authenticated -> true
            is AuthResult.Unauthenticated, is AuthResult.Error -> false
        }
    }

    fun accountDesc() : String?
    {
        val state = authState.value
        return when (state) {
            is AuthResult.Authenticated -> state.account.account?.name ?: state.account.email
            is AuthResult.Unauthenticated, is AuthResult.Error -> application.getString(R.string.google_sign_in)
        }
    }

    fun accountName() : String?
    {
        val state = authState.value
        return when (state) {
            is AuthResult.Authenticated -> state.account.email
            is AuthResult.Unauthenticated, is AuthResult.Error -> null
        }
    }

    init {
        viewModelScope.launch {
            authManager.authState.collectLatest { state ->
                when (state) {
                    is AuthResult.Authenticated -> {
                        var account = state.account.account
                        val name = account?.name
                        //_account.value = account
                        firebaseAccountRepository.setAccountGoogle(name)
                    }
                    is AuthResult.Unauthenticated -> {
                        //_account.value = null
                        firebaseAccountRepository.setAccountGoogle(null)
                    }
                    is AuthResult.Error -> {
                        //_account.value = null
                        firebaseAccountRepository.setAccountGoogle(null)
                    }
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