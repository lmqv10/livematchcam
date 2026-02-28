package it.lmqv.livematchcam.services.firebase.listeners

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.services.firebase.FilterOverlay
import it.lmqv.livematchcam.services.firebase.FilterOverlayEvent
import it.lmqv.livematchcam.services.firebase.OverlaysModel
import it.lmqv.livematchcam.services.firebase.ScoreboardOverlay

interface OverlaysValueListener {
    fun onChangeScoreboard(scoreboard: ScoreboardOverlay)
    fun onChangeFilters(filters: List<FilterOverlayEvent>)
    //fun onChangeFilter(filter: FilterOverlayEvent)
}

internal class OverlaysValueEventListener {
    private var _database: FirebaseDatabase? = null
    private val database get() = _database!!
    private lateinit var accountKey: String
    private lateinit var key: String

    private var overlaysRef: DatabaseReference? = null
    private var scoreboardRef: DatabaseReference? = null
    private var filtersRef: DatabaseReference? = null

    private var scoreboardListener: ValueEventListener? = null
    private var filtersChildAddedListener: ChildEventListener? = null

    // Stato locale
    //private var scoreboard: ScoreboardOverlay? = null
    private var filterEvents: MutableList<FilterOverlayEvent> = mutableListOf()

//    private var onChangeScoreboardListener: (ScoreboardOverlay) -> Unit = { }
//    fun setOnChangeScoreboardListener(onChangeListener: (ScoreboardOverlay) -> Unit) {
//        this.onChangeScoreboardListener = onChangeListener
//    }
//    private var onChangeFilterListener: (Int, FilterOverlay?) -> Unit = { index, filter -> }
//    fun setOnChangeFilterListener(onChangeListener: (Int, FilterOverlay?) -> Unit) {
//        this.onChangeFilterListener = onChangeListener
//    }
//    private var onChangeFiltersListener: (List<FilterOverlay>) -> Unit = { }
//    fun setOnChangeFiltersListener(onChangeListener: (List<FilterOverlay>) -> Unit) {
//        this.onChangeFiltersListener = onChangeListener
//    }
    private var onChangeListener: OverlaysValueListener? = null
    fun setOnChangeListener(onChangeListener: OverlaysValueListener?) {
        this.onChangeListener = onChangeListener
    }

    fun initialize(database: FirebaseDatabase) {
        this._database = database
    }

    fun attach(accountKey: String, key: String) {
        if (this._database == null) {
            throw IllegalStateException("OverlaysValueEventListener:: not initialized")
        } else {
            this.accountKey = accountKey
            this.key = key

            handleValueEventListener()
        }
    }

    fun detach() {
        this.accountKey = ""
        this.key = ""
        handleValueEventListener()
    }

    private fun handleValueEventListener() {
        Logd("OverlaysValueEventListener::handleValueEventListener ${this.accountKey} ${this.key}")

        scoreboardListener?.let { scoreboardRef?.removeEventListener(it) }
        filtersChildAddedListener?.let { filtersRef?.removeEventListener(it) }

        if (this.accountKey.isNotEmpty() && this.key.isNotEmpty()) {
            this.overlaysRef = database.getReference("accounts/${accountKey}/matches/${key}/overlays")

            this.overlaysRef!!.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val overlaysModel = snapshot.getValue(OverlaysModel::class.java) ?: OverlaysModel()
                    var scoreboard = overlaysModel.scoreboard

                    Logd("OverlaysValueEventListener::get scoreboard $scoreboard")
                    onChangeListener?.onChangeScoreboard(scoreboard)

                    var filters = overlaysModel.filters.toMutableList()
                    this.filterEvents = filters.map { x -> FilterOverlayEvent(x.position, x) }.toMutableList()
                    Logd("OverlaysValueEventListener::get filterEvents $filterEvents")
                    onChangeListener?.onChangeFilters(filterEvents)
                }

                scoreboardListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var scoreboard = snapshot.getValue(ScoreboardOverlay::class.java)
                        onChangeListener?.onChangeScoreboard(scoreboard!!)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                }
                this.scoreboardRef = overlaysRef!!.child("scoreboard")
                this.scoreboardRef!!.addValueEventListener(scoreboardListener!!)

                this.filtersChildAddedListener = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val index = snapshot.key?.toIntOrNull() ?: return
                        val filter = snapshot.getValue(FilterOverlay::class.java) ?: return
                        val filterEvent = FilterOverlayEvent(filter.position, filter)
                        if (index >= filterEvents.size) filterEvents.add(filterEvent)
                        else filterEvents[index] = filterEvent
                        onChangeListener?.onChangeFilters(filterEvents)
                    }
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        val index = snapshot.key?.toIntOrNull() ?: return
                        val filter = snapshot.getValue(FilterOverlay::class.java) ?: return
                        val filterEvent = FilterOverlayEvent(filter.position, filter)
                        if (index < filterEvents.size) filterEvents[index] = filterEvent
                        onChangeListener?.onChangeFilters(filterEvents)
                    }
                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        val index = snapshot.key?.toIntOrNull() ?: return
                        if (index < filterEvents.size) filterEvents.removeAt(index)
                        onChangeListener?.onChangeFilters(filterEvents)
                    }
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                }
                this.filtersRef = overlaysRef!!.child("filters")
                filtersRef?.addChildEventListener(filtersChildAddedListener!!)
            }
        } else {
            onChangeListener?.onChangeScoreboard(ScoreboardOverlay())
            onChangeListener?.onChangeFilters(listOf())
        }
    }

    fun updateFilter(filter: FilterOverlayEvent) {
        val index = filterEvents.indexOfFirst { it.position == filter.position }
        if (index >= 0 && filtersRef != null) {
            filtersRef!!.child(index.toString()).setValue(filter.filter)
        }
    }

    fun updateScoreboard(scoreboard: ScoreboardOverlay) {
        scoreboardRef?.setValue(scoreboard)
    }
}