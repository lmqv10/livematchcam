package it.lmqv.livematchcam

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.utils.FontScaleHelper

class LiveMatchApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.initializeApp(this) == null) {
            Logd("Firebase failed to initialize!")
        } else {
            Logd("Firebase initialized successfully.")
        }

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Loge("Uncaught exception in thread ${thread.name}: ${throwable.message}")
            throwable.printStackTrace()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        //Logd("LiveMatchApp::MatchRepository.init()")
        //MatchRepository.init(this)
    }

    override fun attachBaseContext(base: Context) {
        //Logd("LiveMatchApp::attachBaseContext()")
        super.attachBaseContext(FontScaleHelper.applyLimit(base))
    }
}
