package it.lmqv.livematchcam.services.stream

interface IVideoStreamData {
    var width: Int
    var height: Int
    var fps: Int
    var bitrate: Int
    val rotation: Int
}

data class VideoCaptureFormat(
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 0)

data class CameraVideoStreamData(
    override var width: Int = 1920,
    override var height: Int = 1080,
    override var fps: Int = 30,
    override var bitrate: Int = 6000 * 1000,
    override val rotation: Int = 0,
) : IVideoStreamData

//data class UVCCameraVideoStreamData(
//    override var width: Int = 1280,
//    override var height: Int = 720,
//    override var fps: Int = 30,
//    override val bitrate: Int = 6000 * 1000,
//    override val rotation: Int = 0,
//) : IVideoStreamData

interface IAudioStreamData {
    val sampleRate: Int
    val bitrate: Int
    val isStereo: Boolean
    val echoCanceler: Boolean
    val noiseSuppressor: Boolean
}

data class AudioStreamData(
    override val sampleRate: Int = 44100,
    override val bitrate: Int = 128 * 1000,
    override val isStereo: Boolean = true,
    override val echoCanceler: Boolean = false,
    override val noiseSuppressor: Boolean = false,
) : IAudioStreamData


//enum class StreamingState {
//    IDLE, STREAMING
//}
//enum class MicrophoneState {
//    UNKNOWN, MUTE, UNMUTE
//}