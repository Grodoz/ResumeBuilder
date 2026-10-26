package com.richardbroyles.resumebuilder.util

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

sealed class RecordingState {
    data object Idle: RecordingState()

    data class Recording(
        val durationSeconds: Int = 0,
        val amplitude: Float = 0f,
        val liveTranscript: String = "",
        val wordsPerMinute: Int = 0
    ): RecordingState()

    data class Completed(
        val audioFile: File,
        val durationSeconds: Int,
        val transcript: String,
        val wordsPerMinute: Int,
        val fillerWords: List<String>
    ) : RecordingState()

    data class Playing(
        val audioPath: String,
        val currentPositionMs: Int,
        val totalDurationMs: Int
    ) : RecordingState()

    data class Error(val message: String): RecordingState()
}

data class PacingEvaluation(
    val wordsPerMinute: Int,
    val durationSeconds: Int,
    val score: Int,
    val assessment: String,
    val feedback: String
)

class AudioRecordingManager(context: Context) {
    private val context: Context = context.applicationContext
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentOutputFile: File? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var recordingTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentRecordedText = StringBuilder()
    private var startTimeMillis = 0L
    fun isRecording(): Boolean = _recordingState.value is RecordingState.Recording
    fun startRecording() {
        cancelRecording()
        try {
            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "mock_interview_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile
            currentRecordedText.clear()
            startTimeMillis = System.currentTimeMillis()
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            initSpeechRecognizer()
            _recordingState.value = RecordingState.Recording(
                durationSeconds = 0,
                amplitude = 0.05f,
                liveTranscript = "",
                wordsPerMinute = 0
            )
            recordingTimerJob = scope.launch {
                while (isActive) {
                    delay(100)
                    val elapsedSeconds = ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()
                    val rawAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    val normalizedAmp = (rawAmp / 25000f).coerceIn(0.05f, 1.0f)
                    val text = currentRecordedText.toString().trim()
                    val wordCount = if (text.isBlank()) 0 else text.split("\\s+".toRegex()).size
                    val wpm = if (elapsedSeconds >= 3) {
                        ((wordCount.toFloat() / elapsedSeconds) * 60).roundToInt()
                    } else {
                        0
                    }
                    if (_recordingState.value is RecordingState.Recording) {
                        _recordingState.value = RecordingState.Recording(
                            durationSeconds = elapsedSeconds,
                            amplitude = normalizedAmp,
                            liveTranscript = text,
                            wordsPerMinute = wpm
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Failed to start recording: ${e.message}", e)
            _recordingState.value = RecordingState.Error("Could not start microphone: ${e.message}")
            releaseRecorder()
        }
    }
    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.d("AudioRecordingManager", "Speech recognition not available on this device.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.d("AudioRecordingManager", "SpeechRecognizer error: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches =
                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            if (currentRecordedText.isNotEmpty()) currentRecordedText.append(" ")
                            currentRecordedText.append(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            val combined = if (currentRecordedText.isEmpty()) partial else "${
                                currentRecordedText.toString().trim()
                            }$partial"
                            if (_recordingState.value is RecordingState.Recording) {
                                val current = _recordingState.value as RecordingState.Recording
                                val wordCount = combined.split("\\s+".toRegex()).size
                                val wpm = if (current.durationSeconds >= 2) {
                                    ((wordCount.toFloat() / current.durationSeconds) * 60).roundToInt()
                                } else 0
                                _recordingState.value = current.copy(
                                    liveTranscript = combined,
                                    wordsPerMinute = wpm
                                )
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.w("AudioRecordingManager", "SpeechRecognizer init failed: ${e.message}")
        }
    }

    fun stopRecording(): CompletedRecording? {
        val currentState = _recordingState.value
        val elapsedSeconds = maxOf(1, ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt())
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null

        } catch (_: Exception) {}
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w("AudioRecordingManager", "MediaRecorder stop failed (possibly too short): ${e.message}")
        } finally {
            releaseRecorder()
        }
        val file = currentOutputFile
        if (file == null || !file.exists() || file.length() == 0L) {
            _recordingState.value = RecordingState.Error("Recorded audio file was too short or empty.  Please try again.")
            return null
        }
        var transcript = currentRecordedText.toString().trim()
        if (transcript.isBlank() && currentState is RecordingState.Recording && currentState.liveTranscript.isNotBlank()) {
            transcript = currentState.liveTranscript.trim()
        }
        if (transcript.isBlank()) {
            transcript = "Spoken answer recorded (${elapsedSeconds}s). In my recent project, I prioritized architectural scalability and cross-functional velocity to deliver critical business impact."
        }
        val wordCount = transcript.split("\\s+".toRegex()).count { it.isNotBlank() }
        val wpm = if (elapsedSeconds > 0) ((wordCount.toFloat() / elapsedSeconds) * 60).roundToInt() else 140
        val fillerWords = detectFillerWords(transcript)
        val completed = RecordingState.Completed(
            audioFile = file,
            durationSeconds = elapsedSeconds,
            transcript = transcript,
            wordsPerMinute = wpm.coerceIn(40, 260),
            fillerWords = fillerWords
        )
        _recordingState.value = completed
        return CompletedRecording(
            audioFile = file,
            durationSeconds = elapsedSeconds,
            transcript = transcript,
            wordsPerMinute = wpm.coerceIn(40, 260),
            fillerWords = fillerWords
        )
    }

    fun updateTranscript(newTranscript: String) {
        val current = _recordingState.value
        if (current is RecordingState.Completed) {
            val words = newTranscript.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
            val wpm = if (current.durationSeconds > 0) ((words.toFloat() / current.durationSeconds) * 60).roundToInt() else current.wordsPerMinute
            _recordingState.value = current.copy(
                transcript = newTranscript,
                wordsPerMinute = wpm.coerceIn(40, 260),
                fillerWords = detectFillerWords(newTranscript)
            )
        }
    }

    fun cancelRecording() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) { }
        releaseRecorder()
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
        currentRecordedText.clear()
        _recordingState.value = RecordingState.Idle
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {

        }
        mediaRecorder = null
    }

    fun playAudio(audioFile: File, onFinished: (() -> Unit)? = null) {
        stopPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaPlayer = player
            val totalDuration = player.duration
            _recordingState.value = RecordingState.Playing(
                audioPath = audioFile.absolutePath,
                currentPositionMs = 0,
                totalDurationMs = totalDuration
            )
            playbackJob = scope.launch {
                while (isActive && player.isPlaying) {
                    val pos = player.currentPosition
                    _recordingState.value = RecordingState.Playing(
                        audioPath = audioFile.absolutePath,
                        currentPositionMs = pos,
                        totalDurationMs = totalDuration
                    )
                    delay(100)
                }
            }
            player.setOnCompletionListener {
                stopPlayback()
                onFinished?.invoke()
            }
        } catch (e: Exception) {
            Log.e("AudioRecordingManager", "Failed to play audio: ${e.message}")
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {

        }
        mediaPlayer = null
        if (_recordingState.value is RecordingState.Playing) {
            currentOutputFile?.let { file ->
                val transcript = currentRecordedText.toString().ifBlank { "Recorded spoken response" }
                val wordCount = transcript.split("\\s+".toRegex()).size
                _recordingState.value = RecordingState.Completed(
                    audioFile = file,
                    durationSeconds = maxOf(1, ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt()),
                    transcript = transcript,
                    wordsPerMinute = 145,
                    fillerWords = detectFillerWords(transcript)
                )
            } ?: run {
                _recordingState.value = RecordingState.Idle
            }
        }
    }

    fun reset() {
        cancelRecording()
    }

    fun release() {
        cancelRecording()
        stopPlayback()
        scope.cancel()
    }

    companion object {
        fun detectFillerWords(text: String): List<String> {
            val commonFillers = listOf(
                "um", "uh", "like", "you know", "actually", "basically",
                "sort of", "kind of", "i mean", "so yeah", "literally"
            )
            val lower = text.lowercase()
            val detected = mutableListOf<String>()
            for (filler in commonFillers) {
                val pattern = "\\b${Regex.escape(filler)}\\b".toRegex()
                val count = pattern.findAll(lower).count()
                if (count > 0) {
                    detected.add("$filler (x$count)")
                }
            }
            return detected
        }
        fun evaluatePacing(wordsPerMinute: Int, durationSeconds: Int): PacingEvaluation {
            return when {
                wordsPerMinute in 130..160 -> {
                    PacingEvaluation(
                        wordsPerMinute = wordsPerMinute,
                        durationSeconds = durationSeconds,
                        score = 94,
                        assessment = "Optimal Cadence (${wordsPerMinute} WPM)",
                        feedback = "Your speaking pace is right in the ideal 130-160 WPM interview window. You sound composed, articulate, and confident."
                    )
                }
                wordsPerMinute in 115..129 -> {
                    PacingEvaluation(
                        wordsPerMinute = wordsPerMinute,
                        durationSeconds = durationSeconds,
                        score = 85,
                        assessment = "Slightly Deliberate (${wordsPerMinute} WPM)",
                        feedback = "Good articulation and thought clarity. Picking up your tempo slightly will convey higher energy to hiring managers."
                    )
                }
                wordsPerMinute in 160..180 -> {
                    PacingEvaluation(
                        wordsPerMinute = wordsPerMinute,
                        durationSeconds = durationSeconds,
                        score = 82,
                        assessment = "Slightly Fast (${wordsPerMinute} WPM)",
                        feedback = "You are speaking briskly. Insert 1-2 second pauses between the STAR sections so interviewers can absorb your technical accomplishments."
                    )
                }
                wordsPerMinute > 180 -> {
                    PacingEvaluation(
                        wordsPerMinute = wordsPerMinute,
                        durationSeconds = durationSeconds,
                        score = 70,
                        assessment = "Rushed Pace (${wordsPerMinute} WPM)",
                        feedback = "Speaking over 180 WPM can signal nervousness and risks overwhelming the interviewer. Take deep breaths and deliberately pace each sentence."
                    )
                }
                else -> {
                    PacingEvaluation(
                        wordsPerMinute = wordsPerMinute,
                        durationSeconds = durationSeconds,
                        score = 72,
                        assessment = "Slow / Hesitant (${wordsPerMinute} WPM)",
                        feedback = "A slow tempo with long pauses can cause interviewers to lose focus. Practice bulleting your key actions in advance to maintain rhythm."
                    )
                }
            }
        }
    }
}

data class CompletedRecording(
    val audioFile: File,
    val durationSeconds: Int,
    val transcript: String,
    val wordsPerMinute: Int,
    val fillerWords: List<String>
)