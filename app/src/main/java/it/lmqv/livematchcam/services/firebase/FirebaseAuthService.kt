package it.lmqv.livematchcam.services.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.functions
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseAuthService {
    internal var currentAccountKey: String = ""

    //private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var isAdministrator: Boolean = false
    private var isAuthorizedUser: Boolean = false

    private var cachedAccountName: String? = null
    private var cachedAccountKey: String? = null
    private var cachedAccountData: FirebaseAccount? = null

    internal val isValidAccount: Boolean get() =
        FirebaseAuth.getInstance().currentUser != null && currentAccountKey.isNotEmpty()

    suspend fun authenticateAccount(accountName: String, accountKey: String): Result<FirebaseAccount> {
        if (accountKey.isEmpty()) {
            currentAccountKey = ""
            return Result.failure(Exception("Chiave account vuota"))
        }

        // Optimization: Inversion of Cache Control.
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isColdStartCacheResume = currentUser != null && cachedAccountKey == null && cachedAccountData != null
        val isSameSession = currentUser != null && cachedAccountKey == accountKey && cachedAccountName == accountName && cachedAccountData != null

        if (isColdStartCacheResume || isSameSession) {
            // Abbiamo già scaricato l'account almeno una volta, e FirebaseAuth conferma che il token è vivo!
            Logd("FirebaseDataService :: cached account data ")
            cachedAccountKey = accountKey
            cachedAccountName = accountName
            currentAccountKey = accountKey
            return Result.success(cachedAccountData!!)
        }

        return try {
            Logd("FirebaseDataService :: fetching account data via await()")
            val snapshot = FirebaseManager.database.getReference("accounts/$accountKey").get().await()
            val firebaseAccount = snapshot.getValue(FirebaseAccount::class.java)

            if (firebaseAccount != null) {
                isAdministrator = firebaseAccount.admin == accountName
                isAuthorizedUser = firebaseAccount.users.contains(accountName)

                if (isAdministrator || isAuthorizedUser) {
                    currentAccountKey = accountKey

                    val data = hashMapOf(
                        "accountId" to accountKey,
                        "userName" to accountName
                    )
                    
                    val result = Firebase.functions
                        .getHttpsCallable("getCustomToken")
                        .call(data).await()

                    val resultData = result.getData()
                    val responseMap = resultData as? Map<*, *>
                    val tokenStr = responseMap?.get("token") as? String

                    if (!tokenStr.isNullOrEmpty()) {
                        val authResult = FirebaseAuth.getInstance()
                            .signInWithCustomToken(tokenStr).await()

                        if (authResult.user?.uid.isNullOrEmpty()) {
                            currentAccountKey = ""
                            Result.failure(Exception("Autenticazione con token Firebase fallita"))
                        } else {
                            cachedAccountKey = accountKey
                            cachedAccountName = accountName
                            cachedAccountData = firebaseAccount
                            Result.success(firebaseAccount)
                        }
                    } else {
                        currentAccountKey = ""
                        Result.failure(Exception("Nessun token restituito dalla funzione cloud"))
                    }
                } else {
                    currentAccountKey = ""
                    Result.failure(Exception("Utente non autorizzato come admin o user"))
                }
            } else {
                currentAccountKey = ""
                Result.failure(Exception("Account inesistente nel database"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("FirebaseDataService :: Errore nel flusso di login Firebase: ${e.message}")
            currentAccountKey = ""
            Result.failure(e)
        }
    }

    fun logout() {
        cachedAccountKey = null
        cachedAccountName = null
        currentAccountKey = ""
        isAdministrator = false
        isAuthorizedUser = false
        FirebaseAuth.getInstance().signOut()
    }
}