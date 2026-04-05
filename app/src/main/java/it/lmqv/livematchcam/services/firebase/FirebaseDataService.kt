package it.lmqv.livematchcam.services.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.services.firebase.listeners.OverlaysValueEventListener
import it.lmqv.livematchcam.services.firebase.listeners.OverlaysValueListener
import it.lmqv.livematchcam.services.firebase.listeners.SchedulesValueEventListener


object FirebaseDataService {
    //private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var matchKeyRef: DatabaseReference? = null
    private var matchValueEventListener: ValueEventListener? = null
    private var eventInfoKeyRef: DatabaseReference? = null
    private var eventInfoValueEventListener: ValueEventListener? = null

    private val schedulesValueEventListener = SchedulesValueEventListener()
    private val overlaysValueEventListener = OverlaysValueEventListener()

//    private var isAdministrator: Boolean = false
//    private var isAuthorizedUser: Boolean = false
//    private var currentAccountKey: String = ""
//
//    private var cachedAccountName: String? = null
//    private var cachedAccountKey: String? = null
//    private var cachedAccountData: FirebaseAccount? = null
//
//    private val isValidAccount: Boolean get() =
//        FirebaseAuth.getInstance().currentUser != null && currentAccountKey.isNotEmpty()

    init {
        schedulesValueEventListener.initialize(FirebaseManager.database)
        overlaysValueEventListener.initialize(FirebaseManager.database)
    }

//    fun authenticateAccount(accountName: String,
//                            accountKey: String,
//                            successCallback: (FirebaseAccount, ) -> Unit,
//                            failureCallback: () -> Unit) {
//        if (accountKey.isNotEmpty()) {
//
//            // Optimization: Inversion of Cache Control.
//            // Controlliamo in RAM prima di fare la costosa chiamata `addListenerForSingleValueEvent`
//            // che scaricherebbe tutto l'account inutilmente.
//            val currentUser = FirebaseAuth.getInstance().currentUser
//            val isColdStartCacheResume = currentUser != null && cachedAccountKey == null && cachedAccountData != null
//            val isSameSession = currentUser != null && cachedAccountKey == accountKey && cachedAccountName == accountName && cachedAccountData != null
//
//            if (isColdStartCacheResume || isSameSession) {
//                // Abbiamo già scaricato l'account almeno una volta, e FirebaseAuth conferma che il token è vivo!
//                Logd("FirebaseDataService :: cached account data ")
//                cachedAccountKey = accountKey
//                cachedAccountName = accountName
//                currentAccountKey = accountKey
//                successCallback(cachedAccountData!!)
//            } else {
//                Logd("FirebaseDataService :: addListenerForSingleValueEvent account")
//                database.getReference("accounts/$accountKey")
//                    .addListenerForSingleValueEvent(object : ValueEventListener {
//                        override fun onDataChange(snapshot: DataSnapshot) {
//                            val firebaseAccount = snapshot.getValue(FirebaseAccount::class.java)
//
//                            if (firebaseAccount != null) {
//                                isAdministrator = firebaseAccount.admin == accountName
//                                isAuthorizedUser = firebaseAccount.users.contains(accountName)
//
//                                if (isAdministrator || isAuthorizedUser) {
//                                    currentAccountKey = accountKey
//
//                                    CoroutineScope(Dispatchers.IO).launch {
//                                        try {
//                                            val data = hashMapOf(
//                                                "accountId" to accountKey,
//                                                "userName" to accountName
//                                            )
//                                            val result = Firebase.functions
//                                                .getHttpsCallable("getCustomToken")
//                                                .call(data).await()
//
//                                            val resultData = result.getData()
//                                            val responseMap = resultData as? Map<*, *>
//                                            val tokenStr = responseMap?.get("token") as? String
//
//                                            if (!tokenStr.isNullOrEmpty()) {
//                                                val authResult = FirebaseAuth.getInstance()
//                                                    .signInWithCustomToken(tokenStr).await()
//
//                                                if (authResult.user?.uid.isNullOrEmpty()) {
//                                                    currentAccountKey = ""
//                                                    failureCallback()
//                                                } else {
//                                                    cachedAccountKey = accountKey
//                                                    cachedAccountName = accountName
//                                                    cachedAccountData = firebaseAccount
//                                                    successCallback(firebaseAccount)
//                                                }
//                                            } else {
//                                                currentAccountKey = ""
//                                                failureCallback()
//                                            }
//                                        } catch (e: Exception) {
//                                            e.printStackTrace()
//                                            Loge("FirebaseDataService :: Errore nel flusso di login Firebase: ${e.message.toString()}")
//                                            currentAccountKey = ""
//                                            failureCallback()
//                                        }
//                                    }
//                                } else {
//                                    currentAccountKey = ""
//                                    failureCallback()
//                                }
//                            } else {
//                                currentAccountKey = ""
//                                failureCallback()
//                            }
//                        }
//                        override fun onCancelled(error: DatabaseError) {
//                            currentAccountKey = ""
//                            failureCallback()
//                        }
//                    })
//            }
//        } else {
//            currentAccountKey = ""
//            failureCallback()
//        }
//    }

    fun attachMatchValueEventListener(currentKey: String?, onMatchChangeCallback: (Match) -> Unit) {
        val isValidKey = !currentKey.isNullOrEmpty()
        //Logd("FirebaseDataService::attachMatchValueEventListener $currentKey")
        if (isValidKey) {
            detachMatchValueEventListener()

            var currentAccountKey = FirebaseAuthService.currentAccountKey
            this.matchKeyRef = FirebaseManager.database.getReference("accounts/$currentAccountKey/matches/$currentKey/match")
            this.matchValueEventListener =
                matchKeyRef?.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (FirebaseAuthService.isValidAccount) {
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

            var currentAccountKey = FirebaseAuthService.currentAccountKey
            this.eventInfoKeyRef = FirebaseManager.database.getReference("accounts/$currentAccountKey/matches/$currentKey/eventInfo")
            this.eventInfoValueEventListener = this.eventInfoKeyRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (FirebaseAuthService.isValidAccount) {
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
        var currentAccountKey = FirebaseAuthService.currentAccountKey
        schedulesValueEventListener.attach(currentAccountKey, currentKey)
    }

    fun detachSchedulesValueEventListener () {
        schedulesValueEventListener.detach()
        schedulesValueEventListener.setOnChangeListener({ })
    }

    fun attachOverlaysValueEventListener(currentKey: String, overlaysValueListener: OverlaysValueListener) {
        overlaysValueEventListener.setOnChangeListener(overlaysValueListener)
        var currentAccountKey = FirebaseAuthService.currentAccountKey
        overlaysValueEventListener.attach(currentAccountKey, currentKey)
    }

    fun detachOverlaysValueEventListener () {
        overlaysValueEventListener.detach()
        overlaysValueEventListener.setOnChangeListener(null)
    }


    fun updateMatchValue(match: Match?) {
        if (FirebaseAuthService.isValidAccount) {
            this.matchKeyRef?.setValue(match)
        }
    }

    fun updateEventInfoValue(eventInfoData: EventInfoData) {
        if (FirebaseAuthService.isValidAccount) {
            this.eventInfoKeyRef?.setValue(eventInfoData)
        }
    }

    fun updateFilterValue(filter: FilterOverlayEvent) {
        if (FirebaseAuthService.isValidAccount) {
            overlaysValueEventListener.updateFilter(filter)
        }
    }

    fun updateScoreboardValue(scoreboard: ScoreboardOverlay) {
        if (FirebaseAuthService.isValidAccount) {
            overlaysValueEventListener.updateScoreboard(scoreboard)
        }
    }

//    fun logout() {
//        cachedAccountKey = null
//        cachedAccountName = null
//        currentAccountKey = ""
//        isAdministrator = false
//        isAuthorizedUser = false
//        FirebaseAuth.getInstance().signOut()
//    }
}