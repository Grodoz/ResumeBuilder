package com.richardbroyles.resumebuilder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.model.AtsAnalysisResult
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.repository.ResumeImpactAnalysisRepository
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreHigh
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreLow
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreMedium
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class GeminiAtsOptimizationTier(
    val title: String,
    val subtitle: String,
    val color: Color,
    val icon: ImageVector,
    val guidance: String
)

fun getGeminiAtsTier(score: Int): GeminiAtsOptimizationTier {
    return when {
        score >= 85 -> GeminiAtsOptimizationTier(
            title = "Optimal / Top Tier",
            subtitle = "Top 5% Candidate Screening Match",
            color = AtsScoreHigh,
            icon = Icons.Default.CheckCircle,
            guidance = "Your resume seamlessly clears automated ATS scanners and is highly ranked for hiring manager review."
        )
        score >= 70 -> GeminiAtsOptimizationTier(
            title = "Competitive Match",
            subtitle = "Passes initial recruiter screening.",
            color = Color(0xFF0D9488),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            guidance = "Solid alignment with target role. Incorporate suggested missing keywords to reach top tier."
        )
        else -> GeminiAtsOptimizationTier(
            title = "Needs Optimization",
            subtitle = "High ATS Rejection Risk",
            color = Color(0xFFDC2626),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            guidance = "Add core skills and quantifiable metrics to pass automated keyword screening."
        )
    }
}

@Composable
fun GeminiAtsImpactGaugeCard(
    repository: ResumeImpactAnalysisRepository,
    resume: ResumeProfile,
    jobDescription: String = "Senior Software Engineer with modern architecture and cloud expertise.",
    modifier: Modifier = Modifier,
    onOptimizeClick: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val analysisResult by repository.latestImpactAnalysis.collectAsState()
    var isFetching by remember { mutableStateOf(false) }

    fun fetchAnalysis() {
        coroutineScope.launch {
            isFetching = true
            try {
                repository.analyzeResumeImpact(
                    resume = resume,
                    jobDescription = jobDescription,
                    jobTitle = resume.targetRole.ifBlank { "Target Role" },
                    company = "Target Employer"
                )
            } finally {
                isFetching = false
            }
        }
    }

    GeminiAtsImpactGaugeContent(
        analysisResult = analysisResult,
        isAnalyzing = isFetching,
        targetRole = resume.targetRole.ifBlank { "Target Role" },
        onRefreshAnalysis = { fetchAnalysis() },
        onOptimizeClick = onOptimizeClick,
        modifier = modifier
    )
}

@Composable
fun GeminiAtsImpactGaugeCard(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    onOptimizeClick: (() -> Unit)? = null
) {
    val activeResume by viewModel.resumeState.collectAsState()
    val selectedJob by viewModel.selectedJob.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingAts.collectAsState()
    val latestAnalysis by viewModel.impactRepository.latestImpactAnalysis.collectAsState()
    val globalAnalysis by viewModel.atsAnalysis.collectAsState()
    val effectiveAnalysis = latestAnalysis ?: globalAnalysis
    GeminiAtsImpactGaugeContent(
        analysisResult = effectiveAnalysis,
        isAnalyzing = isAnalyzing,
        targetRole = activeResume.targetRole.ifBlank { selectedJob.title.ifBlank { "Target Role" }},
        onRefreshAnalysis = { viewModel.runGeminiImpactAnalysis(selectedJob.rawDescription.ifBlank { "Software Engineer" }) },
        onOptimizeClick = onOptimizeClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GeminiAtsImpactGaugeContent(
    analysisResult: AtsAnalysisResult?,
    isAnalyzing: Boolean,
    targetRole: String,
    onRefreshAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    onOptimizeClick: (() -> Unit)? = null
) {
    val score = analysisResult?.overallScore ?: 78
    val tier = getGeminiAtsTier(score)
    var showDetails by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf("dial") }
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_ats_gauge_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Gemini AI Powered",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gemini ATS Score",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "AI Impact",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }
                        }
                        Text(
                            text = "Target: $targetRole",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshAnalysis,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("gemini_ats_fetch_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("gemini_ats_loading_indicator"),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Gemini ATS analysis",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Analyzing resume impact & ATS score.with Gemini...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GeminiAtsDialGauge(
                    score = score,
                    tier = tier,
                    modifier = Modifier.testTag("gemini_ats_gauge_canvas")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tier.color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, tier.color.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().testTag("gemini_ats_tier_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tier.icon,
                        contentDescription = "Optimization status",
                        tint = tier.color,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tier.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = tier.color
                            )
                        )
                        Text(
                            text = tier.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tier.guidance,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATS Optimization Dimensions",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (showDetails) "Collapse dimensions" else "Expand dimensions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val keywordScore = analysisResult?.keywordScore ?: 84
                    val impactScore = analysisResult?.impactScore ?: 72
                    val formatScore = analysisResult?.formatScore ?: 92
                    val verbScore = analysisResult?.verbStrengthScore ?: 78
                    DimensionProgressBar(
                        label = "Keyword Density & Role Terms",
                        score = keywordScore,
                        tag = "gemini_ats_subscore_keywords"
                    )
                    DimensionProgressBar(
                        label = "STAR Impact & Quantifiable Metrics",
                        score = impactScore,
                        tag = "gemini_ats_subscore_impact"
                    )
                    DimensionProgressBar(
                        label = "Parser Format & Layout Readiness",
                        score = formatScore,
                        tag = "gemini_ats_subscore_format"
                    )
                    DimensionProgressBar(
                        label = "Active Action Verb Strength",
                        score = verbScore,
                        tag = "gemini_ats_subscore_verbs"
                    )
                    val matched = analysisResult?.matchedKeywords ?: listOf(
                        "Kotlin",
                        "Jetpack Compose",
                        "Coroutines",
                        "Room",
                        "MVVM"
                    )
                    if (matched.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matched Core Keywords",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AtsScoreHigh
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("gemini_ats_matched_keywords")
                        ) {
                            matched.take(8).forEach { kw ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AtsScoreHigh.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, AtsScoreHigh.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = AtsScoreHigh,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = kw,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                color = AtsScoreHigh
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val missing = analysisResult?.missingKeywords ?: listOf("CI/CD", "Performance Profiling", "System Architecture")
                    if (missing.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Missing High-Priority Keywords",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AtsScoreMedium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().testTag("gemini_ats_missing_keywords")
                        ) {
                            missing.take(6).forEach { kw ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AtsScoreMedium.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, AtsScoreMedium.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AtsScoreMedium,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+ $kw",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                color = AtsScoreMedium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val topBullet = analysisResult?.bulletImprovements?.firstOrNull()
                    if (topBullet != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_ats_bullet_preview"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Gemini STAR Bullet Optimization",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = topBullet.improvedStar,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (topBullet.metricsAdded.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Metric Added: ${topBullet.metricsAdded}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = AtsScoreHigh
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (onOptimizeClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onOptimizeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("gemini_ats_optimize_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Optimize Resume with Gemini AI",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun GeminiAtsDialGauge(
    score: Int,
    tier: GeminiAtsOptimizationTier,
    modifier: Modifier = Modifier,
    dialSize: Dp = 150.dp,
    strokeWidth: Dp = 13.dp
) {
    val clampedScore = score.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedScore / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "gemini_ats_gauge_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gaugeColor = tier.color
    Box(
        modifier = modifier
            .size(dialSize)
            .semantics {
                contentDescription = "ATS Score gauge showing $score out of 100, level ${tier.title}"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(dialSize)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension
            val arcRadius = (diameter - strokePx) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val startAngle = 135f
            val sweepAngle = 270f
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(diameter - strokePx, diameter - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val gradientBrush = Brush.sweepGradient(
                colors = listOf(
                    AtsScoreLow,
                    AtsScoreMedium,
                    tier.color
                ),
                center = centerOffset
            )
            drawArc(
                brush = if (clampedScore > 50) gradientBrush else Brush.linearGradient(listOf(gaugeColor, gaugeColor)),
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedProgress,
                useCenter = false,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size(diameter - strokePx, diameter - strokePx),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            val cutoffAngle = Math.toRadians((startAngle + sweepAngle * 0.75))
            val dotX = centerOffset.x + arcRadius * cos(cutoffAngle).toFloat()
            val dotY = centerOffset.y + arcRadius * sin(cutoffAngle).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = strokePx * 0.32f,
                center = Offset(dotX, dotY)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$clampedScore",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (dialSize.value * 0.26).sp,
                    color = gaugeColor
                ),
                modifier = Modifier.testTag("gemini_ats_score_text")
            )
            Text(
                text = "ATS SCORE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    letterSpacing = 1.2.sp
                )
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = tier.color.copy(alpha = 0.15f),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${clampedScore}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tier.color
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

    }
}

@Composable
private fun DimensionProgressBar(
    label: String,
    score: Int,
    tag: String,
    modifier: Modifier = Modifier
) {
    val clampedScore = score.coerceIn(0, 100)
    val barColor = when {
        clampedScore >= 80 -> AtsScoreHigh
        clampedScore >= 65 -> AtsScoreMedium
        else -> AtsScoreLow
    }
    val animateWidth by animateFloatAsState(
        targetValue = clampedScore / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "dimension_progress"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Text(
                text = "$clampedScore",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = barColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animateWidth.coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}