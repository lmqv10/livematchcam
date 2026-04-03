package it.lmqv.livematchcam.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.RemoteScoreActivity
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FloatingAction(
    @IdRes val id: Int,
    val iconRes: Int,
    val contentDescription: String,
    val onClick: () -> Unit
)

class FloatingActionsViewModel : ViewModel() {
    private val _actions = MutableStateFlow<List<FloatingAction>>(emptyList())
    val actions: StateFlow<List<FloatingAction>> = _actions

    private var navigator : INavigateDrawerActivity? = null

    private var uvcEnabled = false
    private var remoteScoreAvailability = false
    private var firebaseAccountData : FirebaseAccountDataContract = FirebaseAccountDataContract()
    private @IdRes var currentMenuItemId : Int = 0

    fun setNavigator(navigator : INavigateDrawerActivity?) {
        //Logd("setNavigator:: ${navigator}")
        this.navigator = navigator
    }

    fun setFirebaseAccountData(firebaseAccountData: FirebaseAccountDataContract) {
        //Logd("setFirebaseAccountData:: firebaseAccountData ${firebaseAccountData}")
        this.firebaseAccountData = firebaseAccountData
        var settings = firebaseAccountData.settings
        uvcEnabled = settings.uvcEnabled
        remoteScoreAvailability = firebaseAccountData.remoteScoreAvailable
        handleActions()
    }

    fun setMenuItem(@IdRes currentMenuItemId: Int) {
        //Logd("setMenuItem:: currentMenuItemId ${currentMenuItemId}")
        this.currentMenuItemId = currentMenuItemId
        handleActions()
    }

    fun handleActions() {
        _actions.value = getActions()
    }

    private fun getActions() : List<FloatingAction> {
//        Logd("getActions:: this.uvcEnabled ${this.uvcEnabled}")
//        Logd("getActions:: this.remoteScoreAvailability ${this.remoteScoreAvailability}")
//        Logd("getActions:: this.firebaseAccountData.streams.isNotEmpty() ${this.firebaseAccountData.streams.isNotEmpty()}")
//        Logd("getActions:: this.currentMenuItemId ${currentMenuItemId}")
//        Logd("getActions:: R.id.firebaseConfigurationFragment ${R.id.firebaseConfigurationFragment}")
//        Logd("getActions:: R.id.youtubeStreamFragment ${R.id.youtubeStreamFragment}")
//        Logd("getActions:: this.currentMenuItemId == R.id.firebaseConfigurationFragment ${this.currentMenuItemId == R.id.firebaseConfigurationFragment}")
//        Logd("getActions:: this.currentMenuItemId != R.id.youtubeStreamFragment ${this.currentMenuItemId != R.id.youtubeStreamFragment}")

        return mutableListOf(FloatingAction(
                id = R.id.action_remote_control,
                iconRes = R.drawable.remote_controller,
                contentDescription = "remote",
                onClick = {
                    navigator?.navigateAsStartActivity(RemoteScoreActivity::class.java)
                }
            ), FloatingAction(
                id = R.id.action_camera_stream,
                iconRes = R.drawable.play_stream,
                contentDescription = "play",
                onClick = {
                    navigator?.navigateAsStartActivity(StreamActivity::class.java)
                }
            ))
            //.filter { x -> this.firebaseAccountData.guid.isNotEmpty() }
            .filter { x -> this.remoteScoreAvailability || x.id != R.id.action_remote_control }
            .filter { x -> this.currentMenuItemId != R.id.firebaseConfigurationFragment ||
                        (this.currentMenuItemId == R.id.firebaseConfigurationFragment &&
                         this.firebaseAccountData.streams.isNotEmpty()) }
            .filter { x -> this.currentMenuItemId != R.id.youtubeStreamFragment}
    }
}
