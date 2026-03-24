package com.devil.phoenixproject.data.sync.talos

import com.russhwolf.settings.Settings

/**
 * Talos VPS configuration.
 * Reads device token from persistent settings (set during pairing).
 */
class TalosConfig(private val settings: Settings) {
    val vpsUrl: String = "https://talos.muzidube.com"

    private companion object {
        const val KEY_DEVICE_TOKEN = "talos_vps_device_token"
        const val KEY_VPS_PAIRED = "talos_vps_paired"
    }

    var deviceToken: String?
        get() = settings.getStringOrNull(KEY_DEVICE_TOKEN)
        set(value) {
            if (value != null) {
                settings.putString(KEY_DEVICE_TOKEN, value)
                settings.putBoolean(KEY_VPS_PAIRED, true)
            } else {
                settings.remove(KEY_DEVICE_TOKEN)
                settings.putBoolean(KEY_VPS_PAIRED, false)
            }
        }

    val isPaired: Boolean
        get() = settings.getBoolean(KEY_VPS_PAIRED, false) && !deviceToken.isNullOrBlank()

    fun disconnect() {
        deviceToken = null
    }
}
