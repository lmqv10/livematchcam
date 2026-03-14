package it.lmqv.livematchcam.services.replay

enum class ReplayState {
    /** Not streaming, replay not available */
    IDLE,
    /** Streaming and recording rolling buffer */
    RECORDING,
    /** User is choosing replay start point via seekbar */
    SEEKING,
    /** Replay video is being transmitted */
    REPLAYING
}

data class ReplayMetadata(
    /** Path to the recorded MP4 file */
    val filePath: String,
    /** Total duration of the recorded file in milliseconds */
    val durationMs: Long,
    val filePlaybackOffsetMs: Long = 0L
)
