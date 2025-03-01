package it.lmqv.livematchcam.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.factories.Sports

class FirebaseDataManager {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var matchKeyRef: DatabaseReference? = null
    private var matchValueEventListener: ValueEventListener? = null

    private var isAdministrator: Boolean = false
    private var isAuthorizedUser: Boolean = false
    private var currentAccountKey: String = ""
    private val isValidAccount: Boolean get() =
        (isAdministrator || isAuthorizedUser)
        && currentAccountKey.isNotEmpty()

    companion object {
        @Volatile
        private var INSTANCE: FirebaseDataManager? = null

        fun getInstance(): FirebaseDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseDataManager().also { INSTANCE = it }
            }
        }
    }


    fun authenticateAccount(accountGoogle: String,
                            accountKey: String,
                            successCallback: (Account) -> Unit,
                            failureCallback: () -> Unit) {
        if (accountKey.isNotEmpty()) {
            //Logd("authenticateAccount:: $accountKey")
            database.getReference("accounts/$accountKey")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val account = snapshot.getValue(Account::class.java)

                        if (account != null) {
                            //Logd("account:: $account")
                            isAdministrator = account.admin == accountGoogle
                            isAuthorizedUser = account.users.contains(accountGoogle)

                            //Logd("isAdministrator $isAdministrator")
                            //Logd("isAuthorizedUser $isAuthorizedUser")

                            if (isAdministrator || isAuthorizedUser) {
                                currentAccountKey = accountKey
                                successCallback(account)
                            } else {
                                currentAccountKey = ""
                                failureCallback()
                            }
                        } else {
                            currentAccountKey = ""
                            failureCallback()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        currentAccountKey = ""
                        failureCallback()
                    }
                })
        } else {
            currentAccountKey = ""
            failureCallback()
        }
    }

    fun attachMatchValueEventListener(currentKey: String?,
                                      sport: Sports,
                                      onChangeCallback: (Match, IScore) -> Unit) {
        val isValidKey = !currentKey.isNullOrEmpty()
        //Logd("attachMatchValueEventListener $isValidKey")
        if (isValidKey) {
            this.matchKeyRef = database.getReference("accounts/$currentAccountKey/matches/$currentKey")

            if (this.matchValueEventListener != null) {
                this.matchKeyRef?.removeEventListener(this.matchValueEventListener!!)
            }

            this.matchValueEventListener = matchKeyRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val match = snapshot.getValue(Match::class.java)
                        if (match != null) {
                            val score = ScoreFactory.getInstance().buildByType(match.type, snapshot)
                            onChangeCallback(match, score)
                        }
                    } else {
                        updateMatchValue(Match(type = sport.name))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    fun updateMatchValue(match: Match?) {
        if (this.isValidAccount) {
            this.matchKeyRef?.setValue(match)
        }
    }

    fun updateScoreValue(score: Map<String, Any?>?) {
        if (this.isValidAccount) {
            this.matchKeyRef?.child("score")?.setValue(score)
        }
    }

    /*fun initialize(accountKey: String) : FirebaseDataManager{
        //if (!this.initialized) {
            Logd("FirebaseDataManager:: init")
            //this.accountKey = accountKey
            //this.accountsKeyRef = database.getReference("accounts/$accountKey")
            //this.initialized = true
        //}
        return this
    }*/

    /*fun fetchAccounts(callback: (Accounts?) -> Unit) {
        accountsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val accountsMap = snapshot.getValue(Accounts::class.java)
                callback(accountsMap)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }*/


    /*fun saveAccount(accountId: String, account: Account, callback: (Boolean) -> Unit) {
        accountsRef.child(accountId).setValue(account)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }*/
}