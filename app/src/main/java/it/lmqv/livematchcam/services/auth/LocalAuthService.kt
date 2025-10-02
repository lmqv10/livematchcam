/*
package it.lmqv.livematchcam.services.auth

import android.app.Application
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("DEPRECATION")
class LocalAuthService(private val context: Application): IAuthService {

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Unauthenticated)
    override val authState: StateFlow<AuthResult> = _authState

    init {
        this.updateLastSignedInAccount()
    }

    override fun updateLastSignedInAccount() {
        //Logd("===================== GoogleAuthService::updateLastSignedInAccount")
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _authState.value = AuthResult.Authenticated(account)
        } else {
            _authState.value = AuthResult.Unauthenticated
        }
    }

    override fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    override fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            //Logd("===================== GoogleAuthService::handleSignInResult")
            val account = task.getResult(ApiException::class.java)
            _authState.value = AuthResult.Authenticated(account.account.name)
        } catch (e: ApiException) {
            _authState.value = AuthResult.Error(e)
        }
    }

    override fun signOut(onComplete: (() -> Unit)?) {
        Logd("===================== LocalAuthService::signOut")
        _authState.value = AuthResult.Unauthenticated
        onComplete?.invoke()
    }

}
*/