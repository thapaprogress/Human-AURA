package com.example.natureaura.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.natureaura.model.FrequencyBand
import com.example.natureaura.model.SleepTimerOption
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared Real-time Healing Frequency Audio Synthesis Engine.
 *
 * Generates smooth stereo PCM sine waves and binaural beats using Android AudioTrack:
 * - Aum (136.1 Hz fundamental + 272.2 Hz & 408.3 Hz harmonic overtones + breath LFO)
 * - Alpha (10.0 Hz binaural beat: 200 Hz L / 210 Hz R)
 * - Beta (18.0 Hz binaural beat: 220 Hz L / 238 Hz R)
 * - Gamma (40.0 Hz binaural beat: 240 Hz L / 280 Hz R)
 *
 * Includes Sleep Timer auto-stop and can be driven by TherapyForegroundService for
 * screen-off playback.
 */
object FrequencyTherapyEngine {

    private const val SAMPLE_RATE = 44100
    private const val TWO_PI = 2.0 * PI

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playbackJob: Job? = null

    private val _activeBand = MutableStateFlow(FrequencyBand.AUM)
    val activeBand: StateFlow<FrequencyBand> = _activeBand.asStateFlow()

    private val _secondaryBand = MutableStateFlow<FrequencyBand?>(null)
    val secondaryBand: StateFlow<FrequencyBand?> = _secondaryBand.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _volume = MutableStateFlow(0.65f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _sleepTimerOption = MutableStateFlow(SleepTimerOption.CONTINUOUS)
    val sleepTimerOption: StateFlow<SleepTimerOption> = _sleepTimerOption.asStateFlow()

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
    }

    fun setSleepTimer(option: SleepTimerOption) {
        _sleepTimerOption.value = option
    }

    fun play(primary: FrequencyBand, secondary: FrequencyBand? = null) {
        _activeBand.value = primary
        _secondaryBand.value = secondary
        startPlaybackLoop()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            stop()
        } else {
            startPlaybackLoop()
        }
    }

    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        _isPlaying.value = true
        _elapsedSeconds.value = 0

        playbackJob = scope.launch {
            val minBufferBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBufferBytes * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: run {
                _isPlaying.value = false
                return@launch
            }

            val chunkFrames = 1024
            val pcmBuffer = ShortArray(chunkFrames * 2)

            var phaseL1 = 0.0
            var phaseR1 = 0.0
            var phaseHarmonic2 = 0.0
            var phaseHarmonic3 = 0.0
            var phaseLfo = 0.0
            var phaseSecL = 0.0
            var phaseSecR = 0.0

            var totalFrames = 0L
            var currentGain = 0.0

            try {
                track.play()
                while (isActive && _isPlaying.value) {
                    val elapsedSec = (totalFrames / SAMPLE_RATE).toInt()
                    _elapsedSeconds.value = elapsedSec

                    val timerLimit = _sleepTimerOption.value.durationSeconds
                    if (timerLimit != null && elapsedSec >= timerLimit) {
                        _isPlaying.value = false
                        break
                    }

                    // Smooth 5-second fade-out before Sleep Timer ends
                    val fadeOutMultiplier = if (timerLimit != null && timerLimit - elapsedSec <= 5) {
                        ((timerLimit - elapsedSec).coerceAtLeast(0) / 5.0)
                    } else {
                        1.0
                    }

                    val primary = _activeBand.value
                    val secondary = _secondaryBand.value
                    val targetGain = _volume.value.toDouble() * 0.42 * fadeOutMultiplier

                    val leftFreq = primary.carrierHz
                    val rightFreq = primary.carrierHz + primary.beatHz
                    val incL1 = TWO_PI * leftFreq / SAMPLE_RATE
                    val incR1 = TWO_PI * rightFreq / SAMPLE_RATE
                    val incH2 = TWO_PI * (primary.carrierHz * 2.0) / SAMPLE_RATE
                    val incH3 = TWO_PI * (primary.carrierHz * 3.0) / SAMPLE_RATE
                    val incLfo = TWO_PI * 0.14 / SAMPLE_RATE

                    val secLeftInc = secondary?.let { TWO_PI * it.carrierHz / SAMPLE_RATE } ?: 0.0
                    val secRightInc = secondary?.let { TWO_PI * (it.carrierHz + it.beatHz) / SAMPLE_RATE } ?: 0.0

                    for (i in 0 until chunkFrames) {
                        currentGain += (targetGain - currentGain) * 0.002
                        val breathLfo = 0.86 + 0.14 * sin(phaseLfo)

                        var sampleL: Double
                        var sampleR: Double

                        if (primary == FrequencyBand.AUM) {
                            val drone = sin(phaseL1) * 0.68 +
                                sin(phaseHarmonic2) * 0.22 +
                                sin(phaseHarmonic3) * 0.10
                            sampleL = drone * breathLfo
                            sampleR = drone * breathLfo
                        } else {
                            val pad = sin(phaseHarmonic2 * 0.25) * 0.18
                            sampleL = (sin(phaseL1) * 0.82 + pad) * breathLfo
                            sampleR = (sin(phaseR1) * 0.82 + pad) * breathLfo
                        }

                        if (secondary != null) {
                            val secL = sin(phaseSecL) * 0.28
                            val secR = sin(phaseSecR) * 0.28
                            sampleL = sampleL * 0.78 + secL
                            sampleR = sampleR * 0.78 + secR
                        }

                        val outL = (sampleL * currentGain * Short.MAX_VALUE)
                            .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        val outR = (sampleR * currentGain * Short.MAX_VALUE)
                            .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                        pcmBuffer[i * 2] = outL
                        pcmBuffer[i * 2 + 1] = outR

                        phaseL1 = (phaseL1 + incL1) % TWO_PI
                        phaseR1 = (phaseR1 + incR1) % TWO_PI
                        phaseHarmonic2 = (phaseHarmonic2 + incH2) % TWO_PI
                        phaseHarmonic3 = (phaseHarmonic3 + incH3) % TWO_PI
                        phaseLfo = (phaseLfo + incLfo) % TWO_PI
                        if (secondary != null) {
                            phaseSecL = (phaseSecL + secLeftInc) % TWO_PI
                            phaseSecR = (phaseSecR + secRightInc) % TWO_PI
                        }
                    }

                    track.write(pcmBuffer, 0, pcmBuffer.size)
                    totalFrames += chunkFrames
                }
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
            }
        }
    }
}

class FrequencyTherapyService {
    val activeBand = FrequencyTherapyEngine.activeBand
    val secondaryBand = FrequencyTherapyEngine.secondaryBand
    val isPlaying = FrequencyTherapyEngine.isPlaying
    val volume = FrequencyTherapyEngine.volume
    val elapsedSeconds = FrequencyTherapyEngine.elapsedSeconds
    val sleepTimerOption = FrequencyTherapyEngine.sleepTimerOption

    fun setVolume(newVolume: Float) = FrequencyTherapyEngine.setVolume(newVolume)
    fun setSleepTimer(option: SleepTimerOption) = FrequencyTherapyEngine.setSleepTimer(option)
    fun play(primary: FrequencyBand, secondary: FrequencyBand? = null) =
        FrequencyTherapyEngine.play(primary, secondary)
    fun togglePlayPause() = FrequencyTherapyEngine.togglePlayPause()
    fun stop() = FrequencyTherapyEngine.stop()
    fun release() = FrequencyTherapyEngine.stop()
}
