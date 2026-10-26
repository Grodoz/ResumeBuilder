package com.richardbroyles.resumebuilder.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.richardbroyles.resumebuilder.data.local.AppDatabase
import com.richardbroyles.resumebuilder.data.local.dao.ResumeDao
import com.richardbroyles.resumebuilder.data.local.entity.ContactsEntity
import com.richardbroyles.resumebuilder.data.local.entity.EducationEntity
import com.richardbroyles.resumebuilder.data.local.entity.ExperienceEntity
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSession
import com.richardbroyles.resumebuilder.data.local.entity.InterviewPracticeSessionWithQuestions
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.local.entity.ResumeEntity
import com.richardbroyles.resumebuilder.data.local.entity.ResumeVersion
import com.richardbroyles.resumebuilder.data.model.AtsAnalysisResult
import com.richardbroyles.resumebuilder.data.model.ChatMessage
import com.richardbroyles.resumebuilder.data.model.CoverLetter
import com.richardbroyles.resumebuilder.data.model.DriveSyncState
import com.richardbroyles.resumebuilder.data.model.InterviewSession
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.model.SimulatedInterviewQuestion
import com.richardbroyles.resumebuilder.data.model.SkillsMatrixReport
import com.richardbroyles.resumebuilder.data.remote.GeminiService
import com.richardbroyles.resumebuilder.data.remote.GoogleDriveService
import com.richardbroyles.resumebuilder.data.service.AtsEvaluationService
import com.richardbroyles.resumebuilder.data.repository.ResumeRepository
import com.richardbroyles.resumebuilder.util.AtsScoreCalculator
import com.richardbroyles.resumebuilder.util.AtsScoreResult
import com.richardbroyles.resumebuilder.util.PdfGenerator
import com.richardbroyles.resumebuilder.util.SampleData
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

// ============================================================================
// Feature UI State Data Classes (Consolidated State Management UDF)
// ============================================================================

data class SavedResumesUiState(
    val isLoading: Boolean = false,
    val selectedEntity: ResumeEntity? = null,
    val searchQuery: String = "",
    val savedList: List<ResumeEntity> = emptyList(),
    val filteredList: List<ResumeEntity> = emptyList()
)

data class AtsAnalysisUiState(
    val isAnalyzingRaw: Boolean = false,
    val isCalculatingCompatibility: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isComparingSkills: Boolean = false,
    val rawAnalysis: AtsAnalysisResult? = null,
    val compatibilityReport: AtsEvaluationService.AtsCompatibilityReport? = null,
    val analysisResult: AtsAnalysisResult? = null,
    val detailedReport: AtsEvaluationService.AtsEvaluationReport? = null,
    val skillsMatrix: SkillsMatrixReport? = null
)

data class InterviewUiState(
    val session: InterviewSession = InterviewSession(
        jobTitle = SampleData.sampleJobs.first().title,
        company = SampleData.sampleJobs.first().company,
        mode = "Behavioral (STAR)",
        messages = listOf(
            ChatMessage(
                sender = "ai",
                text = "Welcome to your interview practice! I'm your AI Interview Coach for the ${SampleData.sampleJobs.first().title} role at ${SampleData.sampleJobs.first().company}.\n\nLet's start with a classic behavioral question: 'Tell me about a time you had to lead a critical technical initiative under a tight deadline. How did you structure your approach, and what was the outcome?'"
            )
        )
    ),
    val isResponding: Boolean = false,
    val simulatedQuestions: List<SimulatedInterviewQuestion> = emptyList(),
    val isSimulatingQuestions: Boolean = false,
    val selectedSimulatedQuestion: SimulatedInterviewQuestion? = null
)

class ResumeViewModel(
    val repository: ResumeRepository,
    val resumeDao: ResumeDao = repository.resumeDao
) : ViewModel() {

    /**
     * Secondary constructor allowing initialization with [AppDatabase].
     */
    constructor(appDatabase: AppDatabase, context: Context) : this(
        repository = ResumeRepository(
            database = appDatabase,
            geminiService = GeminiService(),
            driveService = GoogleDriveService(context),
            context = context
        ),
        resumeDao = appDatabase.resumeDao()
    )

    /**
     * Secondary constructor allowing initialization with [ResumeRepository].
     */
    constructor(repository: ResumeRepository) : this(
        repository = repository,
        resumeDao = repository.resumeDao
    )

    val impactRepository = repository.impactAnalysisRepository

    // ========================================================================
    // Consolidated UI States & Delegated Backward-Compatible State Flows
    // ========================================================================

    private val _savedResumesUiState = MutableStateFlow(SavedResumesUiState())
    val savedResumesUiState: StateFlow<SavedResumesUiState> = _savedResumesUiState.asStateFlow()

    private val _atsUiState = MutableStateFlow(AtsAnalysisUiState())
    val atsUiState: StateFlow<AtsAnalysisUiState> = _atsUiState.asStateFlow()

    private val _interviewUiState = MutableStateFlow(InterviewUiState())
    val interviewUiState: StateFlow<InterviewUiState> = _interviewUiState.asStateFlow()

    // Backward-compatible individual getters delegating to UI states via flow operators
    val isLoadingResumes: StateFlow<Boolean> = savedResumesUiState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedResumeEntity: StateFlow<ResumeEntity?> = savedResumesUiState
        .map { it.selectedEntity }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val resumeSearchQuery: StateFlow<String> = savedResumesUiState
        .map { it.searchQuery }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val rawAtsAnalysis: StateFlow<AtsAnalysisResult?> = atsUiState
        .map { it.rawAnalysis }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAnalyzingRawAts: StateFlow<Boolean> = atsUiState
        .map { it.isAnalyzingRaw }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val atsCompatibilityReport: StateFlow<AtsEvaluationService.AtsCompatibilityReport?> = atsUiState
        .map { it.compatibilityReport }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isCalculatingAtsCompatibility: StateFlow<Boolean> = atsUiState
        .map { it.isCalculatingCompatibility }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val atsAnalysis: StateFlow<AtsAnalysisResult?> = atsUiState
        .map { it.analysisResult }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAnalyzingAts: StateFlow<Boolean> = atsUiState
        .map { it.isAnalyzing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _atsDetailedReport = MutableStateFlow<AtsEvaluationService.AtsEvaluationReport?>(null)
    val atsDetailedReport: StateFlow<AtsEvaluationService.AtsEvaluationReport?> = _atsDetailedReport.asStateFlow()

    private val _isParsingResume = MutableStateFlow(false)
    val isParsingResume: StateFlow<Boolean> = _isParsingResume.asStateFlow()

    val skillsMatrix: StateFlow<SkillsMatrixReport?> = atsUiState
        .map { it.skillsMatrix }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isComparingSkills: StateFlow<Boolean> = atsUiState
        .map { it.isComparingSkills }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val interviewSession: StateFlow<InterviewSession> = interviewUiState
        .map { it.session }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InterviewUiState().session)

    val isInterviewResponding: StateFlow<Boolean> = interviewUiState
        .map { it.isResponding }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val simulatedQuestions: StateFlow<List<SimulatedInterviewQuestion>> = interviewUiState
        .map { it.simulatedQuestions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSimulatingQuestions: StateFlow<Boolean> = interviewUiState
        .map { it.isSimulatingQuestions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedSimulatedQuestion: StateFlow<SimulatedInterviewQuestion?> = interviewUiState
        .map { it.selectedSimulatedQuestion }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ========================================================================
    // ResumeDao State Management for Saved Resumes (ResumeEntity)
    // ========================================================================

    /**
     * Reactive StateFlow of all saved ResumeEntity records ordered by last modification timestamp.
     */
    val savedResumes: StateFlow<List<ResumeEntity>> = resumeDao.getAllResumesByLastModified()
        .catch { e ->
            Log.e("ResumeViewModel", "Error collecting saved resumes from ResumeDao: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedResumeEntities: StateFlow<List<ResumeEntity>> = savedResumes

    /**
     * StateFlow filtered by current resumeSearchQuery.
     */
    val filteredSavedResumes: StateFlow<List<ResumeEntity>> = combine(
        savedResumes,
        resumeSearchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter { entity ->
                entity.title.contains(query, ignoreCase = true) ||
                        entity.rawContent.contains(query, ignoreCase = true) ||
                        entity.id.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Fetches / refreshes saved resumes and sets selected resume if needed.
     */
    fun fetchSavedResumes() {
        viewModelScope.launch {
            _savedResumesUiState.value = _savedResumesUiState.value.copy(isLoading = true)
            try {
                val list = resumeDao.getAllResumesByLastModifiedSync()
                val currentSelected = _savedResumesUiState.value.selectedEntity
                val newSelected = if (currentSelected == null && list.isNotEmpty()) list.first() else currentSelected
                _savedResumesUiState.value = _savedResumesUiState.value.copy(
                    isLoading = false,
                    selectedEntity = newSelected,
                    savedList = list
                )
            } catch (e: Exception) {
                Log.e("ResumeViewModel", "Failed to fetch saved resumes: ${e.message}", e)
                _savedResumesUiState.value = _savedResumesUiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * Selects a saved ResumeEntity.
     */
    fun selectSavedResume(entity: ResumeEntity?) {
        _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = entity)
    }

    /**
     * Selects a saved resume by its id.
     */
    fun selectSavedResumeById(id: String) {
        viewModelScope.launch {
            val entity = resumeDao.getResumeEntityById(id)
            _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = entity)
        }
    }

    /**
     * Searches saved resumes by keyword query.
     */
    fun searchSavedResumes(query: String) {
        _savedResumesUiState.value = _savedResumesUiState.value.copy(searchQuery = query)
    }

    // ========================================================================
    // Streamlined CRUD Operations (Using Default Parameters)
    // ========================================================================

    fun insertResume(
        entity: ResumeEntity,
        onComplete: ((Long) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val insertedId = resumeDao.insertResumeEntity(entity)
            _events.emit("Resume '${entity.title.ifBlank { entity.id }}' saved")
            onComplete?.invoke(insertedId)
        }
    }

    fun insertResume(
        title: String,
        rawContent: String,
        id: String = UUID.randomUUID().toString(),
        onComplete: ((Long) -> Unit)? = null
    ) {
        val entity = ResumeEntity(
            id = id,
            title = title,
            rawContent = rawContent,
            lastModified = System.currentTimeMillis()
        )
        insertResume(entity, onComplete)
    }

    fun insertResume(resume: Resume) {
        viewModelScope.launch {
            resumeDao.insertStructuredResume(resume)
            _events.emit("Resume '${resume.name}' saved")
        }
    }

    suspend fun insertResumeEntity(entity: ResumeEntity): Long {
        return resumeDao.insertResumeEntity(entity)
    }

    fun updateResume(
        entity: ResumeEntity,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            resumeDao.updateResumeEntity(entity)
            if (_savedResumesUiState.value.selectedEntity?.id == entity.id) {
                _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = entity)
            }
            _events.emit("Resume '${entity.title.ifBlank { entity.id }}' updated")
            onComplete?.invoke()
        }
    }

    fun updateResume(
        id: String,
        title: String,
        rawContent: String,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            resumeDao.updateResumeTitleAndContent(id, title, rawContent, now)
            val updated = resumeDao.getResumeEntityById(id)
            if (updated != null && _savedResumesUiState.value.selectedEntity?.id == id) {
                _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = updated)
            }
            _events.emit("Resume updated")
            onComplete?.invoke()
        }
    }

    fun updateResume(resume: Resume) {
        viewModelScope.launch {
            resumeDao.update(resume)
            _events.emit("Resume updated")
        }
    }

    suspend fun updateResumeEntity(entity: ResumeEntity) {
        resumeDao.updateResumeEntity(entity)
    }

    fun deleteResume(
        entity: ResumeEntity,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            resumeDao.deleteResumeEntity(entity)
            if (_savedResumesUiState.value.selectedEntity?.id == entity.id) {
                _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = null)
            }
            _events.emit("Resume '${entity.title.ifBlank { entity.id }}' deleted")
            onComplete?.invoke()
        }
    }

    fun deleteResume(
        id: String,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            resumeDao.deleteResume(id)
            if (_savedResumesUiState.value.selectedEntity?.id == id) {
                _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = null)
            }
            _events.emit("Resume deleted")
            onComplete?.invoke()
        }
    }

    suspend fun deleteResumeById(id: String) {
        resumeDao.deleteResume(id)
        if (_savedResumesUiState.value.selectedEntity?.id == id) {
            _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = null)
        }
    }

    fun deleteAllSavedResumes() {
        viewModelScope.launch {
            resumeDao.deleteAllResumeEntities()
            _savedResumesUiState.value = _savedResumesUiState.value.copy(selectedEntity = null)
            _events.emit("All saved resumes cleared")
        }
    }

    suspend fun getResumeById(id: String): ResumeEntity? {
        return resumeDao.getResumeEntityById(id)
    }

    private val _resumeState = MutableStateFlow(SampleData.defaultResume)
    val resumeState: StateFlow<ResumeProfile> = _resumeState.asStateFlow()

    private val _allResumesState = MutableStateFlow<List<ResumeProfile>>(listOf(SampleData.defaultResume, SampleData.sampleArchitectResume))
    val allResumesState: StateFlow<List<ResumeProfile>> = _allResumesState.asStateFlow()

    private val _jobsState = MutableStateFlow<List<JobTarget>>(SampleData.sampleJobs)
    val jobsState: StateFlow<List<JobTarget>> = _jobsState.asStateFlow()

    private val _selectedJob = MutableStateFlow<JobTarget>(SampleData.sampleJobs.first())
    val selectedJob: StateFlow<JobTarget> = _selectedJob.asStateFlow()

    private val _activeCoverLetter = MutableStateFlow<CoverLetter?>(null)
    val activeCoverLetter: StateFlow<CoverLetter?> = _activeCoverLetter.asStateFlow()
    val isGeneratingCoverLetter = MutableStateFlow(false)

    private val _coverLetterEditorText = MutableStateFlow("")
    val coverLetterEditorText: StateFlow<String> = _coverLetterEditorText.asStateFlow()

    private val _driveSyncState = MutableStateFlow(repository.getDriveSyncState())
    val driveSyncState: StateFlow<DriveSyncState> = _driveSyncState.asStateFlow()

    private val _structuredResume = MutableStateFlow<Resume?>(null)
    val structuredResume: StateFlow<Resume?> = _structuredResume.asStateFlow()

    private val _contactModule = MutableStateFlow<ContactsEntity?>(null)
    val contactModule: StateFlow<ContactsEntity?> = _contactModule.asStateFlow()

    private val _experiencesModule = MutableStateFlow<List<ExperienceEntity>>(emptyList())
    val experiencesModule: StateFlow<List<ExperienceEntity>> = _experiencesModule.asStateFlow()

    private val _educationsModule = MutableStateFlow<List<EducationEntity>>(emptyList())
    val educationsModule: StateFlow<List<EducationEntity>> = _educationsModule.asStateFlow()

    private val _themeMode = MutableStateFlow("system") // "system", "dark", "light"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val pastPracticeSessions: StateFlow<List<InterviewPracticeSession>> =
        repository.getPastInterviewPracticeSessions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val pastPracticeSessionsWithQuestions: StateFlow<List<InterviewPracticeSessionWithQuestions>> =
        repository.getPastInterviewPracticeSessionsWithQuestions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun deletePracticeSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteInterviewPracticeSession(sessionId)
            _events.emit("Practice session removed from history")
        }
    }

    fun clearAllPracticeSessions() {
        viewModelScope.launch {
            repository.clearAllInterviewPracticeSessions()
            _events.emit("All practice history cleared")
        }
    }

    val resumeVersions: StateFlow<List<ResumeVersion>> =
        repository.getResumeVersions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun createResumeSnapshot(
        versionName: String = "",
        changeNotes: String = ""
    ) {
        viewModelScope.launch {
            val currentResume = _resumeState.value
            val currentAts = _atsUiState.value.analysisResult?.overallScore
            val savedVersion = repository.createResumeVersion(
                resume = currentResume,
                versionName = versionName,
                changeNotes = changeNotes,
                atsScore = currentAts,
                targetCompany = _selectedJob.value.company
            )
            _events.emit("Created snapshot: ${savedVersion.versionName}")
        }
    }

    fun revertToVersion(version: ResumeVersion) {
        viewModelScope.launch {
            val restored = repository.revertToResumeVersion(version.id)
            if (restored != null) {
                _resumeState.value = restored
                runAtsAnalysis()
                runSkillsMatrixComparison()
                _events.emit("Reverted document to: ${version.versionName}")
            } else {
                _events.emit("Failed to revert: snapshot could not be parsed")
            }
        }
    }

    fun deleteResumeVersion(versionId: Long) {
        viewModelScope.launch {
            repository.deleteResumeVersion(versionId)
            _events.emit("Resume version deleted")
        }
    }

    fun saveStructuredResume(resume: Resume) {
        viewModelScope.launch {
            _structuredResume.value = resume
            repository.saveStructuredResume(resume)
            _events.emit("Resume entity saved successfully to Room database!")
        }
    }

    fun saveContactModule(contact: ContactsEntity) {
        viewModelScope.launch {
            _contactModule.value = contact
            repository.cacheContactInfo(contact)
            _events.emit("Contact details cached locally")
        }
    }

    fun saveExperienceModule(experience: ExperienceEntity) {
        viewModelScope.launch {
            repository.cacheExperience(experience)
            _events.emit("Experience entry cached locally")
        }
    }

    fun saveEducationModule(education: EducationEntity) {
        viewModelScope.launch {
            repository.cacheEducation(education)
            _events.emit("Education entry cached locally")
        }
    }

    fun deleteExperienceModule(id: String) {
        viewModelScope.launch {
            repository.deleteExperience(id)
            _events.emit("Experience entry removed")
        }
    }

    fun deleteEducationModule(id: String) {
        viewModelScope.launch {
            repository.deleteEducation(id)
            _events.emit("Education entry removed")
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.initializeDefaultsIfNeeded()
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Init defaults error: ${e.message}", e)
            }
            launch {
                repository.getAllResumesFlow()
                    .catch { e -> Log.w("ResumeViewModel", "getAllResumesFlow error: ${e.message}", e) }
                    .collect { list ->
                        if (list.isNotEmpty()) {
                            _allResumesState.value = list
                            if (list.none { it.id == _resumeState.value.id }) {
                                _resumeState.value = list.first()
                            }
                        }
                    }
            }
            launch {
                repository.getResumeProfile()
                    .catch { e -> Log.w("ResumeViewModel", "getResumeProfile error: ${e.message}", e) }
                    .collect { profile ->
                        _resumeState.value = profile
                    }
            }
            launch {
                repository.getStructuredResumeFlow()
                    .catch { e -> Log.w("ResumeViewModel", "getStructuredResumeFlow error: ${e.message}", e) }
                    .collect { structured ->
                        _structuredResume.value = structured
                    }
            }
            launch {
                repository.getJobs()
                    .catch { e -> Log.w("ResumeViewModel", "getJobs error: ${e.message}", e) }
                    .collect { jobs ->
                        if (jobs.isNotEmpty()) {
                            _jobsState.value = jobs
                            if (_selectedJob.value.id !in jobs.map { it.id }) {
                                _selectedJob.value = jobs.first()
                            }
                        }
                    }
            }
            launch {
                repository.getContactInfoFlow()
                    ?.catch { e -> Log.w("ResumeViewModel", "getContactInfoFlow error: ${e.message}", e) }
                    ?.collect { contact ->
                        _contactModule.value = contact
                    }
            }
            launch {
                repository.getExperiencesFlow()
                    ?.catch { e -> Log.w("ResumeViewModel", "getExperiencesFlow error: ${e.message}", e) }
                    ?.collect { exps ->
                        _experiencesModule.value = exps
                    }
            }
            launch {
                repository.getEducationsFlow()
                    ?.catch { e -> Log.w("ResumeViewModel", "getEducationsFlow error: ${e.message}", e) }
                    ?.collect { edus ->
                        _educationsModule.value = edus
                    }
            }
            try {
                runAtsAnalysis()
                runSkillsMatrixComparison()
                simulateQuestionsForTargetJob()
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Initial analysis error: ${e.message}", e)
            }
        }
    }

    fun cancelAllCoroutines() {
        viewModelScope.coroutineContext[Job]?.cancelChildren()
    }

    fun updateResume(updated: ResumeProfile) {
        viewModelScope.launch {
            _resumeState.value = updated
            repository.saveResumeProfile(updated)
            _events.emit("Resume profile updated")
        }
    }

    fun selectActiveResume(resume: ResumeProfile) {
        _resumeState.value = resume
        runAtsAnalysis()
        runSkillsMatrixComparison()
        simulateQuestionsForTargetJob(resume = resume)
        viewModelScope.launch {
            _events.emit("Switched active resume to: ${resume.title.ifEmpty { resume.targetRole.ifEmpty { resume.fullName } }}")
        }
    }

    fun createNewResume(
        title: String,
        fullName: String,
        targetRole: String,
        email: String = "",
        phone: String = "",
        location: String = "",
        summary: String = ""
    ) {
        val newId = "resume_${System.currentTimeMillis()}"
        val newProfile = ResumeProfile(
            id = newId,
            title = title.ifBlank { "$targetRole Resume" },
            fullName = fullName.ifBlank { "Professional Candidate" },
            targetRole = targetRole.ifBlank { "Software Engineer" },
            email = email.ifBlank { _resumeState.value.email },
            phone = phone.ifBlank { _resumeState.value.phone },
            location = location.ifBlank { _resumeState.value.location },
            summary = summary.ifBlank { "Dedicated $targetRole with expertise in delivering scalable high-impact solutions." },
            skills = _resumeState.value.skills.take(6),
            experiences = emptyList(),
            educations = _resumeState.value.educations.take(1)
        )
        viewModelScope.launch {
            repository.saveResumeProfile(newProfile)
            _resumeState.value = newProfile
            _events.emit("Created new resume: ${newProfile.title}")
        }
    }

    fun duplicateResume(original: ResumeProfile) {
        val copyId = "resume_${System.currentTimeMillis()}"
        val copyTitle = "${original.title.ifEmpty { original.targetRole.ifEmpty { "Resume" } }} (Tailored Copy)"
        val duplicated = original.copy(
            id = copyId,
            title = copyTitle,
            lastUpdated = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.saveResumeProfile(duplicated)
            _events.emit("Duplicated resume: $copyTitle")
        }
    }

    fun deleteResume(id: String) {
        viewModelScope.launch {
            repository.deleteResume(id)
            _events.emit("Resume deleted from Room database")
            val currentList = repository.getAllResumes()
            if (currentList.isNotEmpty()) {
                if (_resumeState.value.id == id) {
                    _resumeState.value = currentList.first()
                    runAtsAnalysis()
                }
            } else {
                repository.saveResumeProfile(SampleData.defaultResume)
                _resumeState.value = SampleData.defaultResume
            }
        }
    }

    fun analyzeResumeAgainstJob(
        resumeText: String,
        jobTitle: String,
        companyName: String,
        jobDescription: String
    ) {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(
                isAnalyzingRaw = true,
                isCalculatingCompatibility = true
            )
            try {
                val compatibilityReport = repository.calculateAtsCompatibility(
                    resumeText = resumeText,
                    jobDescription = jobDescription
                )
                val result = repository.analyzeRawResume(
                    resumeText = resumeText,
                    jobTitle = jobTitle,
                    companyName = companyName,
                    jobDescription = jobDescription
                )
                _atsUiState.value = _atsUiState.value.copy(
                    compatibilityReport = compatibilityReport,
                    rawAnalysis = result
                )
                _events.emit("ATS Compatibility Score: ${compatibilityReport.atsCompatibilityScore}% calculated!")
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Raw ATS analysis error: ${e.message}", e)
                try {
                    val fallbackReport = repository.calculateAtsCompatibility(resumeText, jobDescription)
                    _atsUiState.value = _atsUiState.value.copy(compatibilityReport = fallbackReport)
                } catch (fallbackEx: Exception) {
                    Log.w("ResumeViewModel", "Fallback compatibility error: ${fallbackEx.message}", fallbackEx)
                }
                _events.emit("ATS scan completed with local recruiter heuristics")
            } finally {
                _atsUiState.value = _atsUiState.value.copy(
                    isAnalyzingRaw = false,
                    isCalculatingCompatibility = false
                )
            }
        }
    }

    fun calculateAtsCompatibility(resumeText: String, jobDescription: String) {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(isCalculatingCompatibility = true)
            try {
                val report = repository.calculateAtsCompatibility(resumeText, jobDescription)
                _atsUiState.value = _atsUiState.value.copy(compatibilityReport = report)
                _events.emit("ATS Compatibility Score calculated: ${report.atsCompatibilityScore}/100")
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "ATS compatibility calculation error: ${e.message}", e)
                _events.emit("Calculation error: ${e.message}")
            } finally {
                _atsUiState.value = _atsUiState.value.copy(isCalculatingCompatibility = false)
            }
        }
    }

    fun clearRawAtsAnalysis() {
        _atsUiState.value = _atsUiState.value.copy(
            rawAnalysis = null,
            compatibilityReport = null
        )
    }

    fun selectJob(job: JobTarget) {
        _selectedJob.value = job
        runAtsAnalysis()
        runSkillsMatrixComparison()
        simulateQuestionsForTargetJob(job = job)
        resetInterview(job = job, mode = _interviewUiState.value.session.mode)
    }

    fun addJobTarget(title: String, company: String, description: String, qualifications: List<String>) {
        val newJob = JobTarget(
            title = title.ifEmpty { "Target Role" },
            company = company.ifEmpty { "Target Company" },
            rawDescription = description,
            requiredQualifications = qualifications.ifEmpty {
                description.lines().filter { it.contains("experience", true) || it.contains("skill", true) || it.contains("require", true) }
            }
        )
        viewModelScope.launch {
            repository.saveJobTarget(newJob)
            selectJob(newJob)
            _events.emit("Added new job: ${newJob.title}")
        }
    }

    fun runAtsAnalysis() {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(isAnalyzing = true)
            try {
                val report = repository.evaluateAtsDetailed(_resumeState.value, _selectedJob.value)
                _atsDetailedReport.value = report
                _atsUiState.value = _atsUiState.value.copy(analysisResult = report.analysisResult)
                try {
                    impactRepository.analyzeResumeImpact(
                        _resumeState.value,
                        _selectedJob.value.rawDescription.ifBlank { "Senior Software Engineer job target" }
                    )
                } catch (ignored: Exception) {
                    Log.w("ResumeViewModel", "Impact analysis warning: ${ignored.message}", ignored)
                }
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Detailed ATS analysis error: ${e.message}", e)
                try {
                    val result = repository.analyzeAtsAndImpact(_resumeState.value, _selectedJob.value)
                    _atsUiState.value = _atsUiState.value.copy(analysisResult = result)
                } catch (fallbackEx: Exception) {
                    Log.w("ResumeViewModel", "Fallback ATS analysis error: ${fallbackEx.message}", fallbackEx)
                    _events.emit("Analysis notice: using on-device assessment")
                }
            } finally {
                _atsUiState.value = _atsUiState.value.copy(isAnalyzing = false)
            }
        }
    }

    fun runGeminiImpactAnalysis(jobDescription: String? = null) {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(isAnalyzing = true)
            try {
                val targetDesc = jobDescription ?: _selectedJob.value.rawDescription.ifBlank { "Senior Software Engineer" }
                val result = impactRepository.analyzeResumeImpact(_resumeState.value, targetDesc)
                result.getOrNull()?.let { analysis ->
                    _atsUiState.value = _atsUiState.value.copy(analysisResult = analysis)
                }
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Gemini impact analysis error: ${e.message}", e)
            } finally {
                _atsUiState.value = _atsUiState.value.copy(isAnalyzing = false)
            }
        }
    }

    fun calculateAtsScoreForJob(jobDescription: String): AtsScoreResult {
        val structured = _structuredResume.value
        return if (structured != null) {
            repository.calculateAtsScore(structured, jobDescription)
        } else {
            AtsScoreCalculator.calculate(_resumeState.value, jobDescription)
        }
    }

    fun parseAndImportResume(
        rawText: String,
        onComplete: ((ResumeProfile) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isParsingResume.value = true
            try {
                val parsed = repository.parseAndPopulateDatabaseFromResumeText(rawText)
                _resumeState.value = parsed
                runAtsAnalysis()
                runSkillsMatrixComparison()
                _events.emit("Extracted resume for ${parsed.fullName} & saved to database!")
                onComplete?.invoke(parsed)
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Parse and import error: ${e.message}", e)
                _events.emit("Failed to parse resume: ${e.message}")
            } finally {
                _isParsingResume.value = false
            }
        }
    }

    fun runSkillsMatrixComparison() {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(isComparingSkills = true)
            try {
                val report = repository.compareSkillsMatrix(_resumeState.value, _selectedJob.value)
                _atsUiState.value = _atsUiState.value.copy(skillsMatrix = report)
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Skills matrix comparison error: ${e.message}", e)
                _events.emit("Matrix updated with local comparisons")
            } finally {
                _atsUiState.value = _atsUiState.value.copy(isComparingSkills = false)
            }
        }
    }

    fun analyzePastedJobSkillsGap(
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        onResult: (SkillsMatrixReport) -> Unit
    ) {
        viewModelScope.launch {
            _atsUiState.value = _atsUiState.value.copy(isComparingSkills = true)
            try {
                val customJob = JobTarget(
                    title = jobTitle.ifBlank { "Target Role" },
                    company = companyName.ifBlank { "Hiring Organization" },
                    rawDescription = jobDescription
                )
                val report = repository.compareSkillsMatrix(_resumeState.value, customJob)
                _atsUiState.value = _atsUiState.value.copy(skillsMatrix = report)
                _events.emit("Skills gap analysis complete!")
                onResult(report)
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Skills gap analysis error: ${e.message}", e)
                _events.emit("Analysis note: ${e.message}")
            } finally {
                _atsUiState.value = _atsUiState.value.copy(isComparingSkills = false)
            }
        }
    }

    /**
     * Unified cover letter generation supporting optional custom parameters with clean defaults.
     */
    fun generatePersonalizedCoverLetter(
        jobDescription: String = _selectedJob.value.rawDescription.ifBlank { "Senior Software Engineer" },
        resume: ResumeProfile? = null,
        jobTitle: String = _selectedJob.value.title.ifBlank { "Target Role" },
        companyName: String = _selectedJob.value.company.ifBlank { "Target Company" },
        tone: String = "Professional & Results-Oriented",
        onComplete: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            isGeneratingCoverLetter.value = true
            try {
                val resumeToUse = resume ?: _resumeState.value
                val customJob = JobTarget(
                    title = jobTitle,
                    company = companyName,
                    rawDescription = jobDescription
                )
                val letter = repository.generateCoverLetter(resumeToUse, customJob, tone)
                _activeCoverLetter.value = letter
                _coverLetterEditorText.value = letter.content
                repository.saveCoverLetter(letter)
                _events.emit("Personalized cover letter generated for ${customJob.company}!")
                onComplete?.invoke(letter.content)
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Cover letter generation error: ${e.message}", e)
                _events.emit("Error generating cover letter: ${e.message}")
            } finally {
                isGeneratingCoverLetter.value = false
            }
        }
    }

    fun generateCustomCoverLetter(
        jobTitle: String,
        companyName: String,
        jobDescription: String,
        tone: String = "Professional & Results-Oriented",
        resume: ResumeProfile? = null,
        onSuccess: ((CoverLetter) -> Unit)? = null
    ) {
        generatePersonalizedCoverLetter(
            jobDescription = jobDescription,
            resume = resume,
            jobTitle = jobTitle,
            companyName = companyName,
            tone = tone,
            onComplete = { _ ->
                _activeCoverLetter.value?.let { onSuccess?.invoke(it) }
            }
        )
    }

    fun updateCoverLetterEditorText(newText: String) {
        _coverLetterEditorText.value = newText
        updateCoverLetterContent(newText)
    }

    fun updateCoverLetterContent(newContent: String) {
        _activeCoverLetter.value?.let { current ->
            val updated = current.copy(content = newContent)
            _activeCoverLetter.value = updated
            _coverLetterEditorText.value = newContent
            viewModelScope.launch {
                repository.saveCoverLetter(updated)
            }
        } ?: run {
            _coverLetterEditorText.value = newContent
        }
    }

    fun saveCoverLetter(letter: CoverLetter) {
        viewModelScope.launch {
            repository.saveCoverLetter(letter)
            _activeCoverLetter.value = letter
            _events.emit("Cover letter saved!")
        }
    }

    fun sendInterviewAnswer(
        userAnswer: String,
        audioPath: String? = null,
        durationSeconds: Int = 0,
        wordsPerMinute: Int = 0
    ) {
        if (userAnswer.isBlank()) return
        val currentSession = _interviewUiState.value.session
        val userMsg = ChatMessage(
            sender = "user",
            text = userAnswer.trim(),
            audioPath = audioPath,
            audioDurationSeconds = durationSeconds,
            wordsPerMinute = wordsPerMinute
        )
        val updatedHistory = currentSession.messages + userMsg

        _interviewUiState.value = _interviewUiState.value.copy(
            session = currentSession.copy(messages = updatedHistory)
        )

        viewModelScope.launch {
            _interviewUiState.value = _interviewUiState.value.copy(isResponding = true)
            try {
                val aiReply = repository.coachInterview(
                    job = _selectedJob.value,
                    resume = _resumeState.value,
                    mode = currentSession.mode,
                    history = updatedHistory,
                    userAnswer = userAnswer,
                    durationSeconds = durationSeconds,
                    wordsPerMinute = wordsPerMinute,
                    isAudioRecorded = audioPath != null
                )
                val finalHistory = updatedHistory + aiReply
                val finalSession = currentSession.copy(messages = finalHistory)
                _interviewUiState.value = _interviewUiState.value.copy(session = finalSession)
                repository.saveInterviewSession(finalSession)
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Interview coach error: ${e.message}", e)
                _events.emit("Interview coach error: ${e.message}")
            } finally {
                _interviewUiState.value = _interviewUiState.value.copy(isResponding = false)
            }
        }
    }

    fun simulateQuestionsForTargetJob(
        resume: ResumeProfile? = null,
        job: JobTarget? = null,
        mode: String = "All"
    ) {
        viewModelScope.launch {
            _interviewUiState.value = _interviewUiState.value.copy(isSimulatingQuestions = true)
            try {
                val targetResume = resume ?: _resumeState.value
                val targetJob = job ?: _selectedJob.value
                val questions = repository.simulateInterviewQuestions(targetResume, targetJob, mode)
                _interviewUiState.value = _interviewUiState.value.copy(simulatedQuestions = questions)
                if (questions.isNotEmpty()) {
                    _events.emit("Simulated ${questions.size} interview questions tailored to ${targetJob.company}")
                }
            } catch (e: Exception) {
                Log.w("ResumeViewModel", "Simulate questions error: ${e.message}", e)
                _events.emit("Error generating simulated questions: ${e.message}")
            } finally {
                _interviewUiState.value = _interviewUiState.value.copy(isSimulatingQuestions = false)
            }
        }
    }

    fun startInterviewWithQuestion(
        question: SimulatedInterviewQuestion,
        job: JobTarget = _selectedJob.value
    ) {
        _interviewUiState.value = _interviewUiState.value.copy(selectedSimulatedQuestion = question)
        val introText = buildString {
            append("Welcome to your tailored interview coaching session for ${job.title} at ${job.company}!\n\n")
            if (question.rationale.isNotBlank()) {
                append("🎯 Question Context (Tailored to your resume):\n${question.rationale}\n\n")
            }
            append("Category: ${question.category} | Difficulty: ${question.difficulty}\n")
            if (question.keySkillsTested.isNotEmpty()) {
                append("Key Focus: ${question.keySkillsTested.joinToString(", ")}\n")
            }
            append("\nQuestion:\n${question.question}\n\n")
            if (question.sampleTip.isNotBlank()) {
                append("💡 Coaching Tip: ${question.sampleTip}")
            }
        }

        val newSession = InterviewSession(
            jobTitle = job.title,
            company = job.company,
            mode = question.category,
            messages = listOf(
                ChatMessage(
                    sender = "ai",
                    text = introText
                )
            )
        )
        _interviewUiState.value = _interviewUiState.value.copy(session = newSession)
        viewModelScope.launch {
            repository.saveInterviewSession(newSession)
            _events.emit("Ready! Practice your response below.")
        }
    }

    fun resetInterview(job: JobTarget = _selectedJob.value, mode: String = "Behavioral (STAR)") {
        val matchingSimulated = _interviewUiState.value.simulatedQuestions.firstOrNull {
            it.category.contains(mode.substringBefore(" "), ignoreCase = true) ||
                    mode.contains(it.category.substringBefore(" "), ignoreCase = true)
        }

        if (matchingSimulated != null) {
            startInterviewWithQuestion(matchingSimulated, job)
            return
        }

        val firstQuestion = when (mode) {
            "Technical" -> "Tell me how you would design an offline-first data sync engine in Android to minimize battery usage and avoid race conditions."
            "Situational" -> "Imagine a critical production bug is detected 20 minutes before a holiday release freeze. How do you triage, communicate, and decide whether to rollback or hotfix?"
            else -> "Tell me about a time you had to lead a critical technical initiative under a tight deadline. How did you structure your approach, and what was the outcome?"
        }

        val newSession = InterviewSession(
            jobTitle = job.title,
            company = job.company,
            mode = mode,
            messages = listOf(
                ChatMessage(
                    sender = "ai",
                    text = "Welcome to your interview practice! I'm your AI Interview Coach for the ${job.title} role at ${job.company}.\n\nPractice Mode: $mode\n\nQuestion 1:\n$firstQuestion"
                )
            )
        )
        _interviewUiState.value = _interviewUiState.value.copy(session = newSession)
        viewModelScope.launch {
            repository.saveInterviewSession(newSession)
        }
    }

    // ========================================================================
    // Context-Free Local File Export Operations (Context-dependent operations
    // delegated to PdfGenerator / DocumentExportService from the UI layer)
    // ========================================================================

    fun exportResumeToLocalFile(
        resume: ResumeProfile,
        file: File,
        onResult: (PdfGenerator.ExportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = PdfGenerator.exportResumeToLocalFile(resume, file)
            onResult(result)
        }
    }

    fun exportResumeToLocalFile(
        resumeEntity: Resume,
        file: File,
        onResult: (PdfGenerator.ExportResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = PdfGenerator.exportResumeToLocalFile(resumeEntity, file)
            onResult(result)
        }
    }

    fun syncWithGoogleDrive() {
        viewModelScope.launch {
            _driveSyncState.value = _driveSyncState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing resume data to Google Drive..."
            )
            val result = repository.syncWithDrive(_resumeState.value)
            result.onSuccess { updatedState ->
                _driveSyncState.value = updatedState
                _events.emit("Synced successfully with Google Drive!")
            }.onFailure { err ->
                _driveSyncState.value = _driveSyncState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "Sync failed: ${err.message}"
                )
                _events.emit("Google Drive sync error")
            }
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }
}

class ResumeViewModelFactory : ViewModelProvider.Factory {
    private var repository: ResumeRepository? = null
    private var appDatabase: AppDatabase? = null
    private var context: Context? = null

    constructor(repository: ResumeRepository) {
        this.repository = repository
    }

    constructor(appDatabase: AppDatabase, context: Context) {
        this.appDatabase = appDatabase
        this.context = context
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = appDatabase
        val ctx = context
        val repo = repository
        if (modelClass.isAssignableFrom(ResumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return when {
                db != null && ctx != null -> ResumeViewModel(db, ctx) as T
                repo != null -> ResumeViewModel(repo) as T
                else -> throw IllegalArgumentException("Either repository or appDatabase must be provided")
            }
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
