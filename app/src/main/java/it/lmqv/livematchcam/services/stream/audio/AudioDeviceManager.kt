package it.lmqv.livematchcam.services.stream.audio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * Utility for enumerating and classifying audio devices relevant to
 * the streaming workflow (headphone outputs and USB audio inputs).
 */
object AudioDeviceManager {

    /** Device types we consider "headphone-like" outputs. */
    private val OUTPUT_HEADPHONE_TYPES = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER
    )

    /** Device types that represent USB audio inputs (HDMI capture, USB mics). */
    private val USB_INPUT_TYPES = setOf(
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_ACCESSORY
    )

    /**
     * Returns all connected audio output devices that can be used
     * for headphone monitoring (wired, USB, Bluetooth).
     */
    fun getOutputHeadphones(context: Context): List<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.type in OUTPUT_HEADPHONE_TYPES }
    }

    /**
     * Returns all connected USB audio input devices.
     * These typically correspond to HDMI capture cards or USB microphones.
     */
    fun getUsbAudioInputDevices(context: Context): List<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.type in USB_INPUT_TYPES }
    }

    /**
     * Returns a human-readable label for a given [AudioDeviceInfo].
     */
    fun getDeviceDisplayName(device: AudioDeviceInfo): String {
        val productName = device.productName?.toString()?.takeIf { it.isNotBlank() }
        val typeName = when (device.type) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET      -> "Cuffie cablate"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES    -> "Headphones cablate"
            AudioDeviceInfo.TYPE_USB_HEADSET         -> "USB Headset"
            AudioDeviceInfo.TYPE_USB_DEVICE          -> "USB Audio"
            AudioDeviceInfo.TYPE_USB_ACCESSORY       -> "USB Accessory"
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP      -> "Bluetooth"
            AudioDeviceInfo.TYPE_BLE_HEADSET         -> "BLE Headset"
            AudioDeviceInfo.TYPE_BLE_SPEAKER         -> "BLE Speaker"
            else                                     -> "Audio Device"
        }
        return if (productName != null) "$typeName ($productName)" else typeName
    }
}
