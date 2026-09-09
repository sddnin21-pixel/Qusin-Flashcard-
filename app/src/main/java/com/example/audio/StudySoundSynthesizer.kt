package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

enum class StudySound(val displayName: String, val icon: String) {
    LO_FI("Lo-fi Chords", "music_note"),
    RAIN("Rainfall", "water_drop"),
    OCEAN("Ocean Waves", "waves"),
    CAFE("Cafe Chatter", "local_cafe"),
    FOREST("Forest Chimes", "forest"),
    FIREPLACE("Fireplace", "local_fire_department"),
    PIANO("Soft Piano", "piano"),
    AMBIENT("Ambient Drone", "grain"),
    WHITE_NOISE("White Noise", "air")
}

class StudySoundSynthesizer(private val scope: CoroutineScope) {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var timerJob: Job? = null

    private val sampleRate = 22050
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(4096)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSound = MutableStateFlow(StudySound.LO_FI)
    val currentSound: StateFlow<StudySound> = _currentSound.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft: StateFlow<Int?> = _sleepTimerSecondsLeft.asStateFlow()

    fun setSound(sound: StudySound) {
        _currentSound.value = sound
        if (_isPlaying.value) {
            play(sound)
        }
    }

    fun setVolume(vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        audioTrack?.setVolume(_volume.value)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            stop()
        } else {
            play(_currentSound.value)
        }
    }

    fun setSleepTimer(minutes: Int) {
        timerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSecondsLeft.value = null
            return
        }
        _sleepTimerSecondsLeft.value = minutes * 60
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && (_sleepTimerSecondsLeft.value ?: 0) > 0) {
                delay(1000)
                val current = _sleepTimerSecondsLeft.value ?: 0
                if (current <= 1) {
                    _sleepTimerSecondsLeft.value = null
                    stop()
                    break
                } else {
                    _sleepTimerSecondsLeft.value = current - 1
                }
            }
        }
    }

    fun play(sound: StudySound = _currentSound.value) {
        stopAudioTrack()
        _currentSound.value = sound
        _isPlaying.value = true

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(_volume.value)
            audioTrack?.play()

            playbackJob = scope.launch(Dispatchers.Default) {
                generateAudioLoop(sound)
            }
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun stop() {
        _isPlaying.value = false
        stopAudioTrack()
    }

    fun release() {
        stop()
    }

    private fun stopAudioTrack() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun generateAudioLoop(sound: StudySound) {
        val buffer = ShortArray(1024)
        val random = Random()
        var phase = 0.0
        var phase2 = 0.0
        var phase3 = 0.0
        var wavePhase = 0.0
        var step = 0
        var lastOut = 0.0

        // Lo-fi chord sequence frequencies (Cmaj7 -> Am7 -> Dm7 -> G7)
        val lofiChords = listOf(
            doubleArrayOf(261.63, 329.63, 392.00, 493.88), // Cmaj7
            doubleArrayOf(220.00, 261.63, 329.63, 392.00), // Am7
            doubleArrayOf(146.83, 174.61, 220.00, 261.63), // Dm7
            doubleArrayOf(196.00, 246.94, 293.66, 349.23)  // G7
        )

        while (scope.isActive && _isPlaying.value) {
            for (i in buffer.indices) {
                var sample = 0.0

                when (sound) {
                    StudySound.WHITE_NOISE -> {
                        sample = (random.nextDouble() * 2.0 - 1.0) * 0.18
                    }

                    StudySound.RAIN -> {
                        // Pink/Brown noise + random drop spikes
                        val white = random.nextDouble() * 2.0 - 1.0
                        lastOut = (lastOut * 0.88) + (white * 0.12)
                        var drop = 0.0
                        if (random.nextInt(350) == 0) {
                            drop = (random.nextDouble() * 0.35)
                        }
                        sample = (lastOut * 0.25) + drop
                    }

                    StudySound.OCEAN -> {
                        // Slow rolling swell modulation
                        wavePhase += 2.0 * PI * 0.12 / sampleRate
                        val waveMod = (sin(wavePhase) + 1.0) * 0.5
                        val white = random.nextDouble() * 2.0 - 1.0
                        lastOut = (lastOut * 0.94) + (white * 0.06)
                        sample = lastOut * (0.05 + waveMod * 0.28)
                    }

                    StudySound.AMBIENT -> {
                        // 432Hz deep meditative drone with gentle warm harmonics
                        phase += 2.0 * PI * 108.0 / sampleRate
                        phase2 += 2.0 * PI * 216.0 / sampleRate
                        phase3 += 2.0 * PI * 324.0 / sampleRate
                        sample = (sin(phase) * 0.25) + (sin(phase2) * 0.12) + (sin(phase3) * 0.06)
                    }

                    StudySound.LO_FI -> {
                        // Soft warm chord pad cycling every ~3 seconds
                        val chordIndex = (step / (sampleRate * 3)) % lofiChords.size
                        val currentNotes = lofiChords[chordIndex]
                        var chordSample = 0.0
                        for (f in currentNotes) {
                            phase += 2.0 * PI * f / sampleRate
                            chordSample += sin(phase) * 0.08
                        }
                        // Add gentle vinyl static
                        val crackle = if (random.nextInt(800) == 0) (random.nextDouble() * 0.1) else 0.0
                        sample = (chordSample * 0.7) + crackle
                    }

                    StudySound.PIANO -> {
                        // Gentle arpeggiated melodic tone
                        val pianoNotes = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 493.88, 392.00)
                        val noteIndex = (step / (sampleRate / 2)) % pianoNotes.size
                        val freq = pianoNotes[noteIndex]
                        phase += 2.0 * PI * freq / sampleRate
                        val noteProgress = (step % (sampleRate / 2)).toDouble() / (sampleRate / 2)
                        val envelope = (1.0 - noteProgress) * (1.0 - noteProgress)
                        sample = (sin(phase) * 0.25 + sin(phase * 2.0) * 0.08) * envelope
                    }

                    StudySound.FIREPLACE -> {
                        // Warm low rumble + crackles
                        val rumble = sin(phase) * 0.08
                        phase += 2.0 * PI * 65.0 / sampleRate
                        val pop = if (random.nextInt(400) == 0) (random.nextDouble() * 0.4 - 0.2) else 0.0
                        sample = rumble + pop
                    }

                    StudySound.CAFE -> {
                        // Low-pass filtered murmur
                        val white = random.nextDouble() * 2.0 - 1.0
                        lastOut = (lastOut * 0.92) + (white * 0.08)
                        sample = lastOut * 0.22
                    }

                    StudySound.FOREST -> {
                        // Gentle wind noise with occasional melodic bird whistle
                        val white = random.nextDouble() * 2.0 - 1.0
                        lastOut = (lastOut * 0.95) + (white * 0.05)
                        var chime = 0.0
                        if ((step / sampleRate) % 5 == 0 && (step % sampleRate) < 4000) {
                            phase2 += 2.0 * PI * 1560.0 / sampleRate
                            chime = sin(phase2) * 0.15
                        }
                        sample = (lastOut * 0.18) + chime
                    }
                }

                step++
                val shortVal = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
                buffer[i] = shortVal
            }

            audioTrack?.write(buffer, 0, buffer.size)
        }
    }
}
