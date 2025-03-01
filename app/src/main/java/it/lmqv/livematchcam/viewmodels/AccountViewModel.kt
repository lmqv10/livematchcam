package it.lmqv.livematchcam.viewmodels

import android.accounts.Account
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.lmqv.livematchcam.repositories.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class GoogleViewModel(application: Application) : AndroidViewModel(application)  {

    private var firebaseAccountRepository: AccountRepository = AccountRepository(application)

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()
    fun setAccount(account: Account?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_account.value != account) {
                _account.value = account
                firebaseAccountRepository.setAccountGoogle(account?.name)
            }
        }
    }

    private val _firebaseAccountKey = MutableStateFlow<String?>(null)
    val firebaseAccountKey: StateFlow<String?> = _firebaseAccountKey.asStateFlow()
    fun setAccountKey(accountKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_firebaseAccountKey.value != accountKey) {
                firebaseAccountRepository.setAccountKey(accountKey)
            }
        }
    }

    init {
        viewModelScope.launch {
            firebaseAccountRepository.accountKey.collect { accountKey ->
                if (_firebaseAccountKey.value != accountKey) {
                    _firebaseAccountKey.value = accountKey
                }
            }
        }
    }

}