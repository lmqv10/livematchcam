package it.lmqv.livematchcam.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AuthResult {
    data object Unauthenticated : AuthResult()
    data class Authenticated(val account: GoogleSignInAccount) : AuthResult()
    data class Error(val exception: Exception) : AuthResult()
}

interface IAuthManager {
    val authState: StateFlow<AuthResult>
    fun getSignInIntent(): Intent
    fun handleSignInResult(data: Intent?)
    fun signOut(onComplete: (() -> Unit)? = null)
}

class GoogleAuthManager(context: Context): IAuthManager {

    private val googleSignInClient: GoogleSignInClient

    private val GOOGLE_APIS_AUTH_YOUTUBE : String = "https://www.googleapis.com/auth/youtube"
    private val CLIENT_ID : String = "54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com"

    private val _authState = MutableStateFlow<AuthResult>(AuthResult.Unauthenticated)
    override val authState: StateFlow<AuthResult> = _authState

    init {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(GOOGLE_APIS_AUTH_YOUTUBE))
            .requestServerAuthCode(CLIENT_ID, true)
            .build()

        googleSignInClient = GoogleSignIn.getClient(context.applicationContext, googleSignInOptions)

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _authState.value = AuthResult.Authenticated(account)
        }
    }

    override fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    override fun handleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            _authState.value = AuthResult.Authenticated(account)
        } catch (e: ApiException) {
            _authState.value = AuthResult.Error(e)
        }
    }

    override fun signOut(onComplete: (() -> Unit)?) {
        googleSignInClient.signOut().addOnCompleteListener {
            _authState.value = AuthResult.Unauthenticated
            onComplete?.invoke()
        }
    }

}
