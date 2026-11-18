package com.example.natureaura.connectivity

import com.example.natureaura.model.BiometricPayload
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WearPayloadBus {
    private val _incomingWearPayloads = MutableSharedFlow<BiometricPayload>(extraBufferCapacity = 32)
    val incomingWearPayloads: SharedFlow<BiometricPayload> = _incomingWearPayloads.asSharedFlow()

    fun emit(payload: BiometricPayload) {
        _incomingWearPayloads.tryEmit(payload)
    }
}

/**
 * Background WearableListenerService receiving Wear OS Data Layer messages
 * on `/nature_aura/biometrics` even when the Wearables tab is not foregrounded.
 */
class NatureAuraWearListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearableBiometricBridge.PATH_BIOMETRICS) {
            val rawJson = String(messageEvent.data, StandardCharsets.UTF_8)
            runCatching { BiometricPayload.fromJsonString(rawJson) }
                .onSuccess { payload -> WearPayloadBus.emit(payload) }
        }
    }
}
