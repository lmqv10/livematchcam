package it.lmqv.livematchcam.services.firebase.listeners

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.services.firebase.Schedule

internal class SchedulesValueEventListener {
    private var _database: FirebaseDatabase? = null
    private val database get() = _database!!
    private lateinit var accountKey: String
    private lateinit var key: String

    private var keyRef: DatabaseReference? = null
    private var valueEventListener: ValueEventListener? = null

    private var onChangeListener: (List<Schedule>) -> Unit = { }
    fun setOnChangeListener(onChangeListener: (List<Schedule>) -> Unit) {
        this.onChangeListener = onChangeListener
    }

    fun initialize(database: FirebaseDatabase) {
        this._database = database
    }

    fun attach(accountKey: String, key: String) {
        if (this._database == null) {
            throw IllegalStateException("SchedulesSingleValueEvent:: not initialized")
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
        Logd("SchedulesSingleValueEvent::handleValueEventListener ${this.accountKey} ${this.key}")

        if (this.valueEventListener != null) {
            Logd("SchedulesSingleValueEvent::removeEventListener")
            this.keyRef?.removeEventListener(this.valueEventListener!!)
        }

        if (this.accountKey.isNotEmpty() && this.key.isNotEmpty()) {
            Logd("SchedulesSingleValueEvent::addValueEventListener")
            this.keyRef = database.getReference("accounts/${accountKey}/matches/${key}/schedule")
            this.valueEventListener = this.keyRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val scheduleList = snapshot.children.mapNotNull {
                            it.getValue(Schedule::class.java)
                        }
                        onChangeListener.invoke(scheduleList)
                    } else {
                        onChangeListener.invoke(emptyList())
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    onChangeListener.invoke(emptyList())
                }
            })
        } else {
            onChangeListener.invoke(emptyList())
        }
    }
}