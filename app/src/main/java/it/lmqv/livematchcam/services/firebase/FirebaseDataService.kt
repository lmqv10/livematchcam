package it.lmqv.livematchcam.services.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.functions
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.services.firebase.listeners.OverlaysValueEventListener
import it.lmqv.livematchcam.services.firebase.listeners.OverlaysValueListener
import it.lmqv.livematchcam.services.firebase.listeners.SchedulesValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseDataService {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var matchKeyRef: DatabaseReference? = null
    private var matchValueEventListener: ValueEventListener? = null
    private var eventInfoKeyRef: DatabaseReference? = null
    private var eventInfoValueEventListener: ValueEventListener? = null

    private val schedulesValueEventListener = SchedulesValueEventListener()
    private val overlaysValueEventListener = OverlaysValueEventListener()

    private var isAdministrator: Boolean = false
    private var isAuthorizedUser: Boolean = false
    private var currentAccountKey: String = ""

    private var cachedAccountName: String? = null
    private var cachedAccountKey: String? = null

    private val isValidAccount: Boolean get() =
        (isAdministrator || isAuthorizedUser)
        && currentAccountKey.isNotEmpty()

    init {
        schedulesValueEventListener.initialize(database)
        overlaysValueEventListener.initialize(database)
    }

    fun authenticateAccount(accountName: String,
                            accountKey: String,
                            successCallback: (FirebaseAccount, ) -> Unit,
                            failureCallback: () -> Unit) {
        if (accountKey.isNotEmpty()) {
            //Logd("authenticateAccount:: $accountKey")
            database.getReference("accounts/$accountKey")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val firebaseAccount = snapshot.getValue(FirebaseAccount::class.java)

                        if (firebaseAccount != null) {
                            //Logd("account:: $account")
                            isAdministrator = firebaseAccount.admin == accountName
                            isAuthorizedUser = firebaseAccount.users.contains(accountName)

                            //Logd("isAdministrator $isAdministrator")
                            //Logd("isAuthorizedUser $isAuthorizedUser")

                            if (isAdministrator || isAuthorizedUser) {
                                currentAccountKey = accountKey

                                // Optimization: check if we already have an active session for the same credentials
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val isColdStartCacheResume = currentUser != null && cachedAccountKey == null
                                val isSameSession = currentUser != null && cachedAccountKey == accountKey && cachedAccountName == accountName

                                if (isColdStartCacheResume || isSameSession) {
                                    Logd("FirebaseDataService :: Using cached Auth session. Skipping getCustomToken.")
                                    cachedAccountKey = accountKey
                                    cachedAccountName = accountName
                                    successCallback(firebaseAccount)
                                } else {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val data = hashMapOf(
                                                "accountId" to accountKey,
                                                "userName" to accountName
                                            )
                                            Logd("FirebaseDataService :: Request Token in progress...")
                                            val result = Firebase.functions
                                                .getHttpsCallable("getCustomToken")
                                                .call(data).await()

                                            val resultData = result.getData()
                                            val responseMap = resultData as? Map<*, *>
                                            val tokenStr = responseMap?.get("token") as? String

                                            if (!tokenStr.isNullOrEmpty()) {
                                                //Logd("FirebaseDataService :: Token ottenuto con successo. Autenticazione in corso...")

                                                val authResult = FirebaseAuth.getInstance()
                                                    .signInWithCustomToken(tokenStr).await()

                                                if (authResult.user?.uid.isNullOrEmpty()) {
                                                    Loge("FirebaseDataService :: Auth With token Failed.")
                                                    currentAccountKey = ""
                                                    failureCallback()
                                                } else {
                                                    Logd("FirebaseDataService :: Auth With token Succeeded! User ID: ${authResult.user?.uid}")
                                                    cachedAccountKey = accountKey
                                                    cachedAccountName = accountName
                                                    successCallback(firebaseAccount)
                                                }
                                            } else {
                                                //Loge("FirebaseDataService :: La function non ha restituito un campo 'token' valido. Dati ricevuti: ${resultData}")
                                                currentAccountKey = ""
                                                failureCallback()
                                            }
                                        } catch (e: Exception) {
                                            // Cattura eventuali errori (ad es. assenza di rete, errori REST, permessi FirebaseAuth, token invalidi, ecc.)
                                            e.printStackTrace()
                                            Loge("FirebaseDataService :: Errore nel flusso di login Firebase: ${e.message.toString()}")
                                            currentAccountKey = ""
                                            failureCallback()
                                        }
                                    }
                                }
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
                        if (isValidAccount) {
                            if (snapshot.exists()) {
                                val match = snapshot.getValue(Match::class.java)
                                if (match != null) {
                                    //Logd("FirebaseDataService::onMatchChangeCallback $currentKey")
                                    onMatchChangeCallback(match)
                                }
                            } else {
                                //Logd("FirebaseDataService::updateMatchValue $currentKey")
                                updateMatchValue(Match())
                            }
                        } else {
                            //Logd("FirebaseDataService::matchValueEventListener NOT valid account")
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
                    if (isValidAccount) {
                        if (snapshot.exists()) {
                            val eventInfoData = snapshot.getValue(EventInfoData::class.java)
                            if (eventInfoData != null) {
                                val score = ScoreFactory.buildByType(eventInfoData.sport, snapshot)
                                val sport: Sports =
                                    enumValues<Sports>().find { it.name == eventInfoData.sport }
                                        ?: Sports.SOCCER
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
                    } else {
                        //Logd("FirebaseDataService::eventInfoValueEventListener NOT valid account")
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

    fun attachSchedulesValueEventListener(currentKey: String, onChangeSchedules: (List<Schedule>) -> Unit) {
        schedulesValueEventListener.setOnChangeListener(onChangeSchedules)
        schedulesValueEventListener.attach(this.currentAccountKey, currentKey)
    }

    fun detachSchedulesValueEventListener () {
        schedulesValueEventListener.detach()
        schedulesValueEventListener.setOnChangeListener({ })
    }

    fun attachOverlaysValueEventListener(currentKey: String, overlaysValueListener: OverlaysValueListener) {
        overlaysValueEventListener.setOnChangeListener(overlaysValueListener)
        overlaysValueEventListener.attach(this.currentAccountKey, currentKey)
    }

    fun detachOverlaysValueEventListener () {
        overlaysValueEventListener.detach()
        overlaysValueEventListener.setOnChangeListener(null)
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

    fun updateFilterValue(filter: FilterOverlayEvent) {
        if (this.isValidAccount) {
            overlaysValueEventListener.updateFilter(filter)
        }
    }

    fun updateScoreboardValue(scoreboard: ScoreboardOverlay) {
        if (this.isValidAccount) {
            overlaysValueEventListener.updateScoreboard(scoreboard)
        }
    }

    /*fun initialize(accountKey: String) : FirebaseDataService{
        //if (!this.initialized) {
            //Logd("FirebaseDataService:: init")
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

    /*fun saveAccount(accountId: String, account: FirebaseAccount, callback: (Boolean) -> Unit) {
        accountsRef.child(accountId).setValue(account)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }*/
}