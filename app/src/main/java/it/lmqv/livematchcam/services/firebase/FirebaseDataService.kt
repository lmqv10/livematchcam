package it.lmqv.livematchcam.services.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.factories.Sports

object FirebaseDataService {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var matchKeyRef: DatabaseReference? = null
    private var matchValueEventListener: ValueEventListener? = null
    private var eventInfoKeyRef: DatabaseReference? = null
    private var eventInfoValueEventListener: ValueEventListener? = null

    private var isAdministrator: Boolean = false
    private var isAuthorizedUser: Boolean = false
    private var currentAccountKey: String = ""
    private val isValidAccount: Boolean get() =
        (isAdministrator || isAuthorizedUser)
        && currentAccountKey.isNotEmpty()

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

    fun attachMatchValueEventListener(currentKey: String?, onMatchChangeCallback: (Match) -> Unit) {
        val isValidKey = !currentKey.isNullOrEmpty()
        //Logd("FirebaseDataService::attachMatchValueEventListener $currentKey")
        if (isValidKey) {
            detachMatchValueEventListener()

            this.matchKeyRef = database.getReference("accounts/$currentAccountKey/matches/$currentKey/match")
            this.matchValueEventListener =
                matchKeyRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val match = snapshot.getValue(Match::class.java)
                            if (match != null) {
                                onMatchChangeCallback(match)
                            }
                        } else {
                            updateMatchValue(Match())
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
        }
    }

    fun detachMatchValueEventListener () {
        if (this.matchValueEventListener != null) {
            //Logd("FirebaseDataService::detachMatchValueEventListener $matchKeyRef")
            this.matchKeyRef?.removeEventListener(this.matchValueEventListener!!)
        }
    }

    fun attachEventInfoValueEventListener(currentKey: String?, selectedSport : Sports, onScoreChangeCallback: (EventInfo) -> Unit) {
        val isValidKey = !currentKey.isNullOrEmpty()
        //Logd("FirebaseDataService::attachMatchValueEventListener $currentKey")
        if (isValidKey) {
            this.detachEventInfoValueEventListener()

            this.eventInfoKeyRef = database.getReference("accounts/$currentAccountKey/matches/$currentKey/eventInfo")
            this.eventInfoValueEventListener = this.eventInfoKeyRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val eventInfoData = snapshot.getValue(EventInfoData::class.java)
                        if (eventInfoData != null) {
                            val score = ScoreFactory.buildByType(eventInfoData.sport, snapshot)
                            val sport: Sports = enumValues<Sports>().find { it.name == eventInfoData.sport } ?: Sports.SOCCER
                            var eventInfo = EventInfo(sport, score)
                            //Logd("FirebaseDataService::onDataChange::$eventInfo")
                            onScoreChangeCallback(eventInfo)
                        }
                    } else {
                        //Logd("FirebaseDataService::onDataCreate::$selectedSport")

                        var defaultScore = ScoreFactory.getInitialScore(selectedSport)
                        val scoreMap = ScoreFactory.buildMap(defaultScore)
                        val eventInfoData = EventInfoData(selectedSport.name, scoreMap)
                        updateEventInfoValue(eventInfoData)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    fun detachEventInfoValueEventListener () {
        if (this.eventInfoValueEventListener != null) {
            //Logd("FirebaseDataService::detachEventInfoValueEventListener $eventInfoKeyRef")
            this.eventInfoKeyRef?.removeEventListener(this.eventInfoValueEventListener!!)
        }
    }

    fun updateMatchValue(match: Match?) {
        if (this.isValidAccount) {
            this.matchKeyRef?.setValue(match)
        }
    }

    fun updateEventInfoValue(eventInfoData: EventInfoData) {
        if (this.isValidAccount) {
            this.eventInfoKeyRef?.setValue(eventInfoData)
        }
    }

    /*fun initialize(accountKey: String) : FirebaseDataService{
        //if (!this.initialized) {
            Logd("FirebaseDataService:: init")
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