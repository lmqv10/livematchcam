package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.RemoteScoreActivity
import it.lmqv.livematchcam.StreamActivity
import it.lmqv.livematchcam.UVCStreamActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class FloatingAction(
    val id: String,
    val iconRes: Int,
    val contentDescription: String,
    val onClick: () -> Unit
)

class FloatingActionsViewModel : ViewModel() {
    private val _actions = MutableStateFlow<List<FloatingAction>>(emptyList())
    val actions: StateFlow<List<FloatingAction>> = _actions

    fun clear() {
        _actions.value = emptyList()
    }

    fun setOnlyStreamActions(navigator : INavigateDrawerActivity?) {
        var actions = getBaseActions(navigator)
        _actions.value = actions
    }

    fun setWithRemoteScoreActions(navigator : INavigateDrawerActivity?) {
        var actions = getBaseActions(navigator)
        var withRemoteScoreActions = actions.toMutableList().apply {
            add(0, FloatingAction(
                id = "RemoteContolActivity",
                iconRes = R.drawable.remote_controller,
                contentDescription = "remote",
                onClick = {
                    navigator?.navigateAsStartActivity(RemoteScoreActivity::class.java)
                }
            ))
        }
        _actions.value = withRemoteScoreActions
    }


    fun setEmptyActions() {
        _actions.value = listOf<FloatingAction>()
    }

    private fun getBaseActions(navigator : INavigateDrawerActivity?) : List<FloatingAction>{
        return listOf(
            FloatingAction(
                id = "UVCStreamActivity",
                iconRes = R.drawable.usb_cam,
                contentDescription = "usb",
                onClick = {
                    navigator?.navigateAsStartActivity(UVCStreamActivity::class.java)
                }
            ),
            FloatingAction(
                id = "StreamActivity",
                iconRes = R.drawable.play_stream,
                contentDescription = "play",
                onClick = {
                    navigator?.navigateAsStartActivity(StreamActivity::class.java)
                }
            )
        )
    }
}
