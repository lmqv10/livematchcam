package it.lmqv.livematchcam.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.RemoteScoreActivity
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.UVCStreamActivity
import it.lmqv.livematchcam.services.firebase.FirebaseAccountDataContract
import it.lmqv.livematchcam.services.firebase.Settings
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

    private var uvcEnabled = false
    private var remoteScoreAvailability = false

    fun setFirebaseAccountData(firebaseAccountData: FirebaseAccountDataContract, navigator : INavigateDrawerActivity?) {
        var settings = firebaseAccountData.settings
        uvcEnabled = settings.uvcEnabled
        remoteScoreAvailability = firebaseAccountData.remoteScoreAvailable
        handleActions(navigator)
    }

    fun handleActions(navigator : INavigateDrawerActivity?) {
        _actions.value = getActions(navigator)
    }

    fun setNoActions() {
        _actions.value = listOf<FloatingAction>()
    }

//    fun setOnlyStreamActions(navigator : INavigateDrawerActivity?) {
//        _actions.value = getActions(navigator)
//    }

//    fun setWithRemoteScoreActions(navigator : INavigateDrawerActivity?) {
//
//        var withRemoteScoreActions = getBaseActions(navigator)
//            .toMutableList().apply {
//                add(0, FloatingAction(
//                    id = R.id.action_remote_control,
//                    iconRes = R.drawable.remote_controller,
//                    contentDescription = "remote",
//                    onClick = {
//                        navigator?.navigateAsStartActivity(RemoteScoreActivity::class.java)
//                    }
//                ))
//            }
//        _actions.value = withRemoteScoreActions
//    }

    private fun getActions(navigator : INavigateDrawerActivity?) : List<FloatingAction> {
        return mutableListOf(FloatingAction(
                id = R.id.action_remote_control,
                iconRes = R.drawable.remote_controller,
                contentDescription = "remote",
                onClick = {
                    navigator?.navigateAsStartActivity(RemoteScoreActivity::class.java)
                }
            ),  FloatingAction(
                id = R.id.action_uvc_stream,
                iconRes = R.drawable.ic_uvc_camera,
                contentDescription = "usb",
                onClick = {
                    navigator?.navigateAsStartActivity(UVCStreamActivity::class.java)
                }
            ), FloatingAction(
                id = R.id.action_camera_stream,
                iconRes = R.drawable.play_stream,
                contentDescription = "play",
                onClick = {
                    navigator?.navigateAsStartActivity(StreamActivity::class.java)
                }
            ))
            .filter { x -> uvcEnabled || x.id != R.id.action_uvc_stream }
            .filter { x -> remoteScoreAvailability || x.id != R.id.action_remote_control }
    }
}
