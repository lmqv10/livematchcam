package it.lmqv.livematchcam.services.auth

data class Account(val name: String)

@Suppress("DEPRECATION")
sealed class AuthResult {
    data object Unauthenticated : AuthResult()
    data class Authenticated(val account: Account) : AuthResult()
    data class Error(val exception: Exception) : AuthResult()
}