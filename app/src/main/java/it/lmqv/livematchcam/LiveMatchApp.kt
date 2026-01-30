package it.lmqv.livematchcam

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.utils.FontScaleHelper

class LiveMatchApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.initializeApp(this) == null) {
            Loge("Firebase failed to initialize!")
        } else {
            Loge("Firebase initialized successfully.")
        }

        //Logd("LiveMatchApp::MatchRepository.init()")
        //MatchRepository.init(this)
    }

    override fun attachBaseContext(base: Context) {
        //Logd("LiveMatchApp::attachBaseContext()")
        super.attachBaseContext(FontScaleHelper.applyLimit(base))
    }
}
