package it.lmqv.livematchcam.viewmodels

import androidx.lifecycle.ViewModel
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
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

    fun setMatchInfoActions(navigator : INavigateDrawerActivity?) {
        var actions = listOf(
            FloatingAction(
                id = R.id.serversFragment.toString(),
                iconRes = R.drawable.arrow_right,
                contentDescription = "server",
                onClick = {
                    navigator?.navigateAsDrawerSelection(R.id.serversFragment)
                }
            )
        )
        _actions.value = actions
    }

    fun setStreamActions(navigator : INavigateDrawerActivity?) {
        var actions = listOf(
            FloatingAction(
                id = R.id.matchInfoFragment.toString(),
                iconRes = R.drawable.arrow_left,
                contentDescription = "matchInfo",
                onClick = {
                    navigator?.navigateAsDrawerSelection(R.id.matchInfoFragment)
                }
            ),
            FloatingAction(
                id = R.id.activity_usb.toString(),
                iconRes = R.drawable.usb_cam,
                contentDescription = "usb",
                onClick = {
                    navigator?.navigateAsStartActivity(UVCStreamActivity::class.java)
                }
            ),
            FloatingAction(
                id = R.id.activity_Live.toString(),
                iconRes = R.drawable.play_stream,
                contentDescription = "play",
                onClick = {
                    navigator?.navigateAsStartActivity(StreamActivity::class.java)
                }
            )
        )
        _actions.value = actions
    }

    /*fun setActions(actions: List<FloatingAction>) {
        _actions.value = actions
    }*/
}
