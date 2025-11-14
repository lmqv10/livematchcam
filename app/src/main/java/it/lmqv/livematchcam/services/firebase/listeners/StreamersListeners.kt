//package it.lmqv.livematchcam.services.firebase.listeners
//
//import android.graphics.Bitmap
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.GenericTypeIndicator
//import com.google.firebase.database.ValueEventListener
//import it.lmqv.livematchcam.extensions.Logd
//import it.lmqv.livematchcam.services.firebase.Stream
//
//object StreamersValueEventListener {
//    private lateinit var database: FirebaseDatabase
//    private lateinit var accountKey: String
//    private var keyRef: DatabaseReference? = null
//    private var valueEventListener: ValueEventListener? = null
//
//    private var listener: StreamersItemsListener? = null
//    interface StreamersItemsListener {
//        fun onChange(items: List<Stream>)
//    }
//    fun setListener(listener: StreamersItemsListener) {
//        this.listener = listener
//    }
//
//    fun initialize(
//        database: FirebaseDatabase,
//        accountKey: String)
//    {
//        this.database = database
//        this.accountKey = accountKey
//
//        handleValueEventListener()
//    }
//
//    fun destroy() {
//        this.accountKey = ""
//        handleValueEventListener()
//    }
//
//    private fun handleValueEventListener() {
//
//        Logd("StreamersListeners::handleValueEventListener ${this.accountKey}")
//        if (this.valueEventListener != null) {
//            Logd("StreamersListeners::removeEventListener")
//            this.keyRef?.removeEventListener(this.valueEventListener!!)
//        }
//
//        if (this.accountKey.isNotEmpty()) {
//            this.keyRef = database.getReference("accounts/${this.accountKey}/streamers")
//            this.valueEventListener =
//                this.keyRef?.addValueEventListener(object : ValueEventListener {
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        if (snapshot.exists()) {
//                            val typeIndicator = object : GenericTypeIndicator<List<Stream>>() {}
//                            val streamers = snapshot.getValue(typeIndicator)
//                            if (streamers != null) {
//                                listener?.onChange(streamers)
//                            }
////                            val streamers = mutableListOf<Stream>()
////                            for (child in snapshot.children) {
////                                val streamer = child.getValue(Stream::class.java)
////                                if (streamer != null) {
////                                    streamers.add(streamer)
////                                }
////                            }
////                            listener?.onChange(streamers)
//                        } else {
//                            listener?.onChange(emptyList())
//                        }
//                    }
//
//                    override fun onCancelled(error: DatabaseError) {
//                    }
//                })
//        } else {
//            listener?.onChange(emptyList())
//        }
//    }
//
//}