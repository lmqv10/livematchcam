package it.lmqv.livematchcam.views
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceView

class SwipeSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : SurfaceView(context, attrs, defStyleAttr), GestureDetector.OnGestureListener {

     interface OnSwipeGesture {
        fun swipeUp()
        fun swipeDown()
        fun swipeLeft()
        fun swipeRight()
    }

    init {
        // Additional initialization if needed
    }

    private var gestureCallback : OnSwipeGesture? = null
    private val gestureDetector = GestureDetector(context, this)

    fun setCallbackListener(callback: OnSwipeGesture) {
        this.gestureCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass the event to the gesture detector
        return gestureDetector.onTouchEvent(event)
    }

    override fun onDown(event: MotionEvent): Boolean {
        return true // Return true to indicate that we've handled the event
    }

    override fun onShowPress(event: MotionEvent) {
        // Optional: handle show press event
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        // Optional: handle single tap
        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent,
        distanceX: Float, distanceY: Float
    ): Boolean {
        // Optional: handle scroll event
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        // Optional: handle long press
    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent,
        velocityX: Float, velocityY: Float
    ): Boolean {
        if (e1 != null) {
            val deltaX = e2.x - e1.x
            val deltaY = e2.y - e1.y
            val threshold = 100 // Minimum swipe distance
            val velocityThreshold = 1000 // Minimum swipe speed

            if (Math.abs(deltaX) > threshold && Math.abs(velocityX) > velocityThreshold) {
                if (deltaX > 0) {
                    // Right swipe
                    this.gestureCallback?.swipeRight()
                    //Toast.makeText(context, "Swiped Right", Toast.LENGTH_SHORT).show()
                } else {
                    // Left swipe
                    this.gestureCallback?.swipeLeft()
                    //Toast.makeText(context, "Swiped Left", Toast.LENGTH_SHORT).show()
                }
                return true
            } else if (Math.abs(deltaY) > threshold && Math.abs(velocityY) > velocityThreshold) {
                if (deltaY > 0) {
                    // Down swipe
                    this.gestureCallback?.swipeDown()
                    //Toast.makeText(context, "Swiped Down", Toast.LENGTH_SHORT).show()
                } else {
                    // Up swipe
                    this.gestureCallback?.swipeUp()
                    //Toast.makeText(context, "Swiped Up", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        }
        return false
    }
}
