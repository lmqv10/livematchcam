package it.lmqv.livematchcam.services.firebase

import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().also {
            it.setPersistenceEnabled(true) // garantito prima di tutto
        }
    }
}