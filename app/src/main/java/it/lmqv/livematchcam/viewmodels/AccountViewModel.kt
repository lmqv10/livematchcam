package it.lmqv.livematchcam.viewmodels

import android.accounts.Account
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.extensions.Logd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GoogleViewModel : ViewModel() {
    fun isLoggedIn() : Boolean {
        return _account.value != null
    }

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account
    fun setAccount(account: Account?) {
        if (_account.value != account) {
            Logd("GoogleViewModel.account: Set ${account?.name}")
            _account.value = account
        }
    }
}