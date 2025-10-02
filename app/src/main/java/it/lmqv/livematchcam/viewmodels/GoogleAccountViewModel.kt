package it.lmqv.livematchcam.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.repositories.AccountRepository
import it.lmqv.livematchcam.services.auth.GoogleAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/*class AccountViewModelFactory(
    private val application: Application,
    private val authService: IAuthService
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GoogleAccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GoogleAccountViewModel(application, authService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}*/

class GoogleAccountViewModel(private val application: Application) : AndroidViewModel(application)  {

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)
    private val authService = GoogleAuthService(application)

    val authState: StateFlow<AuthResult> = authService.authState

    fun updateLastSignedInAccount() {
        authService.updateLastSignedInAccount()
    }

    fun getSignInIntent(): Intent = authService.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        authService.handleSignInResult(data)
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        authService.signOut(onComplete)
    }

    //private val _account = MutableStateFlow<FirebaseAccount?>(null)
    //val account: StateFlow<FirebaseAccount?> = _account
    /*fun setAccount(account: FirebaseAccount?) {
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
            is AuthResult.Authenticated -> state.account.name
            is AuthResult.Unauthenticated, is AuthResult.Error -> application.getString(R.string.google_sign_in)
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
            authService.authState.collectLatest { state ->
                when (state) {
                    is AuthResult.Authenticated -> {
                        var name = state.account.name
                        //val name = account?.name
                        //_account.value = account
                        firebaseAccountRepository.setAccountName(name)
                    }
                    is AuthResult.Unauthenticated -> {
                        //_account.value = null
                        firebaseAccountRepository.setAccountName(null)
                    }
                    is AuthResult.Error -> {
                        //_account.value = null
                        firebaseAccountRepository.setAccountName(null)
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