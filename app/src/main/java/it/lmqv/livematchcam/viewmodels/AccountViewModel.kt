package it.lmqv.livematchcam.viewmodels

import android.accounts.Account
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleViewModel : ViewModel() {
    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account
    fun setAccount(account: Account?) {
        if (_account.value != account) {
            _account.value = account
        }
    }
}