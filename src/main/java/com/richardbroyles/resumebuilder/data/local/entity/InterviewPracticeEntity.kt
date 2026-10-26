package com.richardbroyles.resumebuilder.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "interview_practice_sessions")
data class InterviewPracticeSession(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    @ColumnInfo(name = "job_title")
    val jobTitle: String = "",
    @ColumnInfo(name = "company_name")
    val companyName: String = "",
    @ColumnInfo(name = "interview_mode")
    val interviewMode:String = "Behavioral (STAR)",
    @ColumnInfo(name = "questions_asked")
    val questionsAsked: String = "",
    @ColumnInfo(name = "candidate_response")
    val candidateResponse: String = "",
    @ColumnInfo(name = "ai_feedback")
    val aiFeedback: String = "",
    @ColumnInfo(name ="strengths")
    val strengths: String = "",
    @ColumnInfo(name = "improvements")
    val improvements: String = "",
    @ColumnInfo(name = "model_answers")
    val modelAnswers: String = "",
    @ColumnInfo(name = "overall_score")
    val overallScore: Int = 0,
    @ColumnInfo(name = "question_count")
    val questionCount: Int = 0,
    @ColumnInfo(name = "raw_messages_json")
    val rawMessagesJson: String = "",
    @ColumnInfo(name = "timestamp")
    val timestamp:Long = System.currentTimeMillis(),
    @ColumnInfo(name = "clarity_score")
    val clarityScore: Int = 0,
    @ColumnInfo(name = "pacing_score")
    val pacingScore: Int = 0,
    @ColumnInfo(name = "pacing_wpm")
    val pacingWpm: Int = 0,
    @ColumnInfo(name = "clarity_feedback")
    val clarityFeedback: String = "",
    @ColumnInfo(name = "pacing_feedback")
    val pacingFeedback: String = ""
)

@Entity(
    tableName = "interview_question_feedbacks",
    foreignKeys = [
        ForeignKey(
            entity = InterviewPracticeSession::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["session_id"])]
)
data class InterviewQuestionFeedback (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "question_number")
    val questionNumber: Int = 1,
    @ColumnInfo(name = "question_text")
    val questionText: String = "",
    @ColumnInfo(name = "candidate_answer")
    val candidateAnswer: String = "",
    @ColumnInfo(name = "ai_feedback")
    val aiFeedback: String = "",
    @ColumnInfo(name = "score")
    val score: Int = 0,
    @ColumnInfo(name = "star_rating")
    val starRating: String = "",
    @ColumnInfo(name = "strengths")
    val strengths: String = "",
    @ColumnInfo(name = "improvements")
    val improvements: String = "",
    @ColumnInfo(name = "model_answer_suggestion")
    val modelAnswerSuggestion: String = "",
    @ColumnInfo(name = "clarity_score")
    val clarityScore: Int = 0,
    @ColumnInfo(name = "pacing_score")
    val pacingScore: Int = 0,
    @ColumnInfo(name = "pacing_wpm")
    val pacingWpm: Int = 0,
    @ColumnInfo(name = "clarity_feedback")
    val clarityFeedback: String = "",
    @ColumnInfo(name = "pacing_feedback")
    val pacingFeedback: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

data class InterviewPracticeSessionWithQuestions(
    @Embedded
    val session: InterviewPracticeSession,
    @Relation(
        parentColumn = "id",
        entityColumn = "session_id"
    )
    val questions: List<InterviewQuestionFeedback> = emptyList()
)