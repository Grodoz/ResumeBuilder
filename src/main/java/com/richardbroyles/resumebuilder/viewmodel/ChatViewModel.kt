package com.richardbroyles.resumebuilder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.AnswerCoachingFeedback
import com.richardbroyles.resumebuilder.data.model.ChatMessage
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.data.repository.ResumeRepository
import com.richardbroyles.resumebuilder.util.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val geminiService: GeminiService = GeminiService(),
    private val repository: ResumeRepository? = null,
    private val resumeDao: ResumeDao? = null
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _jobTitle = MutableStateFlow("Senior Android Engineer")
    val jobTitle: StateFlow<String> = _jobTitle.asStateFlow()

    private val _company = MutableStateFlow("Google")
    val company: StateFlow<String> = _company.asStateFlow()

    private val _jobDescription = MutableStateFlow(
        "We are looking for a Senior Android Engineer experienced in Kotlin, Jetpack Compose, Coroutines, Room, and Clean Architecture to build scalable mobile solutions."
    )
    val jobDescription: StateFlow<String> = _jobDescription.asStateFlow()

    private val _interviewMode = MutableStateFlow("Behavioral (STAR)")
    val interviewMode: StateFlow<String> = _interviewMode.asStateFlow()

    private val _currentResume = MutableStateFlow<Resume?>(createDefaultResume())
    val currentResume: StateFlow<Resume?> = _currentResume.asStateFlow()

    private val _lastFeedback = MutableStateFlow<AnswerCoachingFeedback?>(null)
    val lastFeedback: StateFlow<AnswerCoachingFeedback?> = _lastFeedback.asStateFlow()

    private val _averageScore = MutableStateFlow<Int?>(null)
    val averageScore: StateFlow<Int?> = _averageScore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        val initialGreeting = ChatMessage(
            sender = "ai",
            text = "Welcome to your interview coaching session for the ${_jobTitle.value} position at ${_company.value}.\n\nCould you walk me through a major project from your recent experience where you made a significant technical impact?"
        )
        _messages.value = listOf(initialGreeting)
        loadDefaultResumeAndStart()
    }

    /**
     * Loads the stored resume from the Room database (or uses sample fallback)
     * and initializes the first interview question if chat is empty.
     */
    fun loadDefaultResumeAndStart(resumeId: String = "primary_resume") {
        viewModelScope.launch {
            try {
                val resume = repository?.getStructuredResume(resumeId)
                    ?: resumeDao?.getResumeById(resumeId)
                    ?: createDefaultResume()

                _currentResume.value = resume

                if (_messages.value.isEmpty()) {
                    startInterview()
                }
            } catch (e: Exception) {
                val fallback = createDefaultResume()
                _currentResume.value = fallback
                if (_messages.value.isEmpty()) {
                    startInterview()
                }
            }
        }
    }

    /**
     * Initializes or configures session parameters with a specific job context and mode.
     */
    fun initSession(
        title: String? = null,
        comp: String? = null,
        desc: String? = null,
        mode: String? = null
    ) {
        title?.takeIf { it.isNotBlank() }?.let { _jobTitle.value = it }
        comp?.takeIf { it.isNotBlank() }?.let { _company.value = it }
        desc?.takeIf { it.isNotBlank() }?.let { _jobDescription.value = it }
        mode?.takeIf { it.isNotBlank() }?.let { _interviewMode.value = it }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun setInterviewMode(mode: String) {
        _interviewMode.value = mode
    }

    fun updateJobContext(title: String, comp: String, desc: String) {
        _jobTitle.value = title
        _company.value = comp
        _jobDescription.value = desc
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Initiates the interview session by generating the first opening question via Gemini.
     */
    fun startInterview() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val resume = _currentResume.value ?: createDefaultResume()
                val job = JobTarget(
                    title = _jobTitle.value,
                    company = _company.value,
                    rawDescription = _jobDescription.value
                )

                val openingQuestion = geminiService.generateInitialInterviewQuestion(
                    resume = resume,
                    job = job,
                    mode = _interviewMode.value
                )

                val greetingMessage = ChatMessage(
                    sender = "ai",
                    text = "Welcome to your interview coaching session for the ${_jobTitle.value} position at ${_company.value}.\n\n$openingQuestion"
                )
                _messages.value = listOf(greetingMessage)
            } catch (e: Exception) {
                _error.value = "Failed to start interview: ${e.message}"
                // Fallback greeting
                val fallbackGreeting = ChatMessage(
                    sender = "ai",
                    text = "Hello! I am your AI Interview Coach for the ${_jobTitle.value} position at ${_company.value}.\n\nCould you walk me through a major project from your recent experience where you made a significant technical impact?"
                )
                _messages.value = listOf(fallbackGreeting)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sends the candidate's answer to Gemini, receives structured coaching feedback,
     * updates the conversation, and generates the next follow-up interview question.
     */
    fun sendMessage(userText: String? = null) {
        val answerText = (userText ?: _inputText.value).trim()
        if (answerText.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            sender = "user",
            text = answerText
        )

        val updatedHistory = _messages.value + userMessage
        _messages.value = updatedHistory
        _inputText.value = ""
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val resume = _currentResume.value ?: createDefaultResume()
                val job = JobTarget(
                    title = _jobTitle.value,
                    company = _company.value,
                    rawDescription = _jobDescription.value
                )

                val coachResponse = geminiService.coachInterviewResponse(
                    job = job,
                    resume = resume,
                    mode = _interviewMode.value,
                    history = updatedHistory,
                    userAnswer = answerText
                )

                _messages.value = updatedHistory + coachResponse
                coachResponse.feedback?.let { fb ->
                    _lastFeedback.value = fb
                    calculateAverageScore()
                }
            } catch (e: Exception) {
                _error.value = "Failed to evaluate response: ${e.message}"
                val fallbackFeedback = AnswerCoachingFeedback(
                    score = 85,
                    starRating = "Good structure and technical depth.",
                    strengths = listOf("Directly answered the question", "Professional tone"),
                    improvements = listOf(
                        "Quantify business outcomes",
                        "Mention specific tools or frameworks"
                    ),
                    modelAnswerSuggestion = "In my previous role, I approached this by applying core engineering methodologies, leading to robust results."
                )
                val fallbackResponse = ChatMessage(
                    sender = "ai",
                    text = "Thank you for sharing. Solid response with good technical focus.\n\nHere is your next question:\nHow do you prioritize competing requirements when working under tight project deadlines?",
                    feedback = fallbackFeedback
                )
                _messages.value = updatedHistory + fallbackResponse
                _lastFeedback.value = fallbackFeedback
                calculateAverageScore()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateAverageScore() {
        val scores = _messages.value
            .mapNotNull { it.feedback?.score }
            .filter { it > 0 }
        if (scores.isNotEmpty()) {
            _averageScore.value = scores.average().toInt()
        }
    }

    /**
     * Resets the conversation and re-generates an opening interview question.
     */
    fun resetSession() {
        _messages.value = emptyList()
        _lastFeedback.value = null
        _averageScore.value = null
        _inputText.value = ""
        startInterview()
    }

    fun clearChat() {
        _messages.value = emptyList()
        _lastFeedback.value = null
        _averageScore.value = null
        _inputText.value = ""
    }

    private fun createDefaultResume(): Resume {
        val sample = SampleData.defaultResume
        return Resume(
            id = "primary_resume",
            name = sample.fullName,
            fullName = sample.fullName,
            contactInfo = "${sample.email} | ${sample.phone} | ${sample.location}",
            email = sample.email,
            phone = sample.phone,
            location = sample.location,
            targetRole = sample.targetRole,
            professionalSummary = sample.summary,
            workExperience = sample.experiences.joinToString("\n\n") { exp ->
                "• ${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${exp.endDate})\n" +
                        exp.bullets.joinToString("\n") { "  - $it" }
            },
            education = sample.educations.joinToString("\n") { "${it.degree} - ${it.institution} (${it.graduationYear})" },
            skills = sample.skills.joinToString(", ") { it.name }
        )
    }
}

/**
 * Factory class for instantiating [ChatViewModel] with repository and service injection.
 */
class ChatViewModelFactory(
    private val geminiService: GeminiService,
    private val repository: ResumeRepository? = null,
    private val resumeDao: ResumeDao? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(geminiService, repository, resumeDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
