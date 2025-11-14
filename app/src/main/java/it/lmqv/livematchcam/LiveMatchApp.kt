package it.lmqv.livematchcam

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.repositories.MatchRepository

class LiveMatchApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.initializeApp(this) == null) {
            Log.e("FirebaseInit", "Firebase failed to initialize!")
        } else {
            Log.d("FirebaseInit", "Firebase initialized successfully.")
        }

        Logd("LiveMatchApp::MatchRepository.init()")
        MatchRepository.init()
        //MatchRepository.init(this)
    }
}