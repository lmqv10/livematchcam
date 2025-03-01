package it.lmqv.livematchcam.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.extensions.Logd

// Firebase Data Manager
class FirebaseDataManager {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    //private var accountsKeyRef: DatabaseReference? = null
    private var matchKeyRef: DatabaseReference? = null
    private var matchValueEventListener: ValueEventListener? = null

    //private var accountKey: String? = null
    //private var initialized: Boolean = false

    companion object {
        @Volatile
        private var INSTANCE: FirebaseDataManager? = null

        fun getInstance(): FirebaseDataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseDataManager().also { INSTANCE = it }
            }
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

    /*fun authenticateAccount(accountId: String,
                            accountName: String,
                            successCallback: (Account) -> Unit,
                            failureCallback: () -> Unit) {
        //if (this.initialized) {
            accountsKeyRef
                ?.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val account = snapshot.getValue(Account::class.java)

                    if (account != null) {
                        val isAdmin = account.admin == accountName
                        val isAuthorizedUser = account.users.contains(accountName)

                        if (isAdmin || isAuthorizedUser) {
                            for (match in account.matches) {
                                var scoreSnapshot = snapshot.child("matches").child(match.key);
                                match.value.score = ScoreFactory.getInstance()
                                    .buildByType(match.value.type, scoreSnapshot)
                            }
                        }
                        successCallback(account)
                    } else {
                        failureCallback()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    failureCallback()
                }
            })
        /*} else {
            failureCallback()
        }*/
    }*/

    fun attachMatchValueEventListener(accountKey: String, currentKey: String, onChangeCallback: (Match) -> Unit) {
        //if (this.initialized && this.matchValueEventListener == null) {
            //Logd("FirebaseDataManager::addMatchValueEventListener")
            this.matchKeyRef = database.getReference("accounts/$accountKey/matches/$currentKey")

            if (this.matchValueEventListener != null) {
                this.matchKeyRef?.removeEventListener(this.matchValueEventListener!!)
            }

            this.matchValueEventListener = matchKeyRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val match = snapshot.getValue(Match::class.java)
                    if (match != null) {
                        onChangeCallback(match)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        //}
    }

    fun updateMatchValue(match: Match?) {
        //if (this.initialized && this.matchKeyRef != null) {
        if (this.matchKeyRef != null) {
            //!currentKey.isNullOrEmpty() && match != null) {
            //Logd("FirebaseDataManager::updateMatchValue")
            /*accountsKeyRef
                ?.child("/matches/$currentKey")
                ?.setValue(match)*/
            this.matchKeyRef?.setValue(match)
        }
    }

    /*fun removeMatchValueEventListener() {
        //if (this.initialized && this.matchValueEventListener != null && this.matchKeyRef != null) {
        if (this.matchValueEventListener != null && this.matchKeyRef != null) {
            Logd("FirebaseDataManager::removeMatchValueEventListener")
            this.matchKeyRef?.removeEventListener(this.matchValueEventListener!!)
            this.matchKeyRef = null
            this.matchValueEventListener = null
            //this.initialized = false
        }
    }*/

    /*fun saveAccount(accountId: String, account: Account, callback: (Boolean) -> Unit) {
        accountsRef.child(accountId).setValue(account)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }*/
}