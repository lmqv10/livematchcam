package it.lmqv.livematchcam.services.auth

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

interface IAuthService {
    val authState: StateFlow<AuthResult>
    fun updateLastSignedInAccount()
    fun getSignInIntent(): Intent
    fun handleSignInResult(data: Intent?)
    fun signOut(onComplete: (() -> Unit)? = null)
}
