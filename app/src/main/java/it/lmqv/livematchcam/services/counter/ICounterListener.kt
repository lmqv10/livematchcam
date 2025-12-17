package it.lmqv.livematchcam.services.counter

interface ICounterListener {
    fun onTick(timeElapsedInSeconds: Int)
}