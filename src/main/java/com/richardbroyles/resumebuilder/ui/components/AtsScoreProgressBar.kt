package com.richardbroyles.resumebuilder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreHigh
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreLow
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreMedium

/**
 * Rating tier classification for an ATS Resume Score.
 */
enum class AtsScoreRatingTier(
    val title: String,
    val badgeText: String,
    val recommendation: String,
    val primaryColor: Color,
    val gradientColors: List<Color>,
    val icon: ImageVector
) {
    TOP_TIER(
        title = "Top 5% Candidate Tier",
        badgeText = "Top Tier Match",
        recommendation = "Resume clears automated ATS filters and ranks in the top percentile of recruiter pipelines.",
        primaryColor = AtsScoreHigh,
        gradientColors = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)),
        icon = Icons.Default.CheckCircle
    ),
    STRONG(
        title = "Strong Interview Match",
        badgeText = "Strong Match",
        recommendation = "Passes standard recruiter screening. Minor keyword alignment will guarantee top visibility.",
        primaryColor = Color(0xFF0D9488), // Refined Teal
        gradientColors = listOf(Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFF2DD4BF)),
        icon = Icons.AutoMirrored.Filled.TrendingUp
    ),
    MODERATE(
        title = "Moderate Screening Match",
        badgeText = "Moderate Match",
        recommendation = "At risk of being filtered out. Add missing high-priority keywords and quantify achievements.",
        primaryColor = AtsScoreMedium,
        gradientColors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24)),
        icon = Icons.Default.Warning
    ),
    RISK(
        title = "High ATS Filtering Risk",
        badgeText = "Action Needed",
        recommendation = "Likely to be rejected by ATS algorithms before human review. Critical keyword and format overhaul required.",
        primaryColor = AtsScoreLow,
        gradientColors = listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171)),
        icon = Icons.Default.ErrorOutline
    );

    companion object {
        fun fromScore(score: Int): AtsScoreRatingTier {
            val clamped = score.coerceIn(0, 100)
            return when {
                clamped >= 85 -> TOP_TIER
                clamped >= 72 -> STRONG
                clamped >= 55 -> MODERATE
                else -> RISK
            }
        }
    }
}

/**
 * Visual Progress Bar Component displaying the current ATS score rating and percentage match
 * for a resume against a target job description.
 *
 * @param score The ATS compatibility score (0 - 100).
 * @param modifier Layout modifier.
 * @param jobTitle Optional title of target job description.
 * @param companyName Optional hiring organization name.
 * @param barHeight Thickness of the progress bar track.
 * @param showPercentage Whether to show the percentage text callout.
 * @param showRatingBadge Whether to show the rating tier badge.
 * @param showThresholdMarkers Whether to display 50%, 70%, and 85% screening threshold markers.
 * @param showRecommendation Whether to display actionable recruiter guidance text below the bar.
 * @param animated Whether to animate the progress fill on appearance or value changes.
 */
@Composable
fun AtsScoreProgressBar(
    score: Int,
    modifier: Modifier = Modifier,
    jobTitle: String? = null,
    companyName: String? = null,
    barHeight: Dp = 14.dp,
    showPercentage: Boolean = true,
    showRatingBadge: Boolean = true,
    showThresholdMarkers: Boolean = true,
    showRecommendation: Boolean = true,
    animated: Boolean = true
) {
    val clampedScore = score.coerceIn(0, 100)
    val progressFraction = clampedScore / 100f
    val tier = AtsScoreRatingTier.fromScore(clampedScore)

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = if (animated) {
            tween(durationMillis = 900, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = 0)
        },
        label = "ats_score_progress_bar"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ats_score_progress_bar")
            .semantics {
                contentDescription = "ATS resume score $clampedScore percent match. Rating: ${tier.title}"
            }
    ) {
        // --- Header Row: Percentage Match & Rating Badge ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showPercentage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("ats_score_percentage_container")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = tier.primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$clampedScore%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("ats_score_percentage_text")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Match",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (showRatingBadge) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tier.primaryColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, tier.primaryColor.copy(alpha = 0.35f)),
                    modifier = Modifier.testTag("ats_score_rating_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tier.icon,
                            contentDescription = null,
                            tint = tier.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tier.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = tier.primaryColor
                            )
                        )
                    }
                }
            }
        }

        // --- Target Job Description Context (if provided) ---
        if (!jobTitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ats_score_job_context"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                val targetText = if (!companyName.isNullOrBlank()) "$jobTitle • $companyName" else jobTitle
                Text(
                    text = targetText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Progress Track with Gradient Fill and Milestone Markers ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ats_score_progress_track_container")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // The Main Progress Bar Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(barHeight / 2)
                        )
                        .testTag("ats_score_progress_track"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Animated Gradient Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(barHeight / 2))
                            .background(
                                Brush.horizontalGradient(tier.gradientColors)
                            )
                            .testTag("ats_score_progress_fill")
                    )

                    // Glowing leading indicator dot / pill at progress tip
                    if (animatedProgress > 0.04f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 2.dp)
                                    .size(barHeight - 4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .shadow(2.dp, CircleShape)
                            )
                        }
                    }

                    // Milestone Threshold Vertical Tick Marks
                    if (showThresholdMarkers) {
                        // 50% Threshold Mark
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.50f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .fillMaxHeight(0.6f)
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .testTag("ats_threshold_tick_50")
                            )
                        }

                        // 70% Threshold Mark
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .fillMaxHeight(0.6f)
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .testTag("ats_threshold_tick_70")
                            )
                        }

                        // 85% Threshold Mark
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(1.5.dp)
                                    .fillMaxHeight(0.6f)
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .testTag("ats_threshold_tick_85")
                            )
                        }
                    }
                }

                // Threshold Labels Under Track
                if (showThresholdMarkers) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            ThresholdBadge(
                                label = "50% Min",
                                isReached = clampedScore >= 50,
                                activeColor = AtsScoreMedium,
                                testTag = "ats_threshold_marker_50"
                            )
                            ThresholdBadge(
                                label = "70% Strong",
                                isReached = clampedScore >= 70,
                                activeColor = Color(0xFF0D9488),
                                testTag = "ats_threshold_marker_70"
                            )
                            ThresholdBadge(
                                label = "85% Top Tier",
                                isReached = clampedScore >= 85,
                                activeColor = AtsScoreHigh,
                                testTag = "ats_threshold_marker_85"
                            )
                        }
                        Text(
                            text = "100%",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // --- Recruiter Impact / Guidance Callout ---
        if (showRecommendation) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ats_score_recommendation_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = tier.primaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tier.recommendation,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("ats_score_recommendation_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdBadge(
    label: String,
    isReached: Boolean,
    activeColor: Color,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    if (isReached) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isReached) activeColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/**
 * An elevated card presentation wrapping the ATS Score Progress Bar component,
 * ideal for dashboards, job matching overviews, and scan summary screens.
 *
 * @param score The ATS compatibility score (0 - 100).
 * @param jobTitle Target job position title.
 * @param companyName Target company name.
 * @param modifier Card modifier.
 * @param keywordMatchPercent Optional keyword match sub-score.
 * @param impactScorePercent Optional impact & metrics sub-score.
 * @param formatScorePercent Optional formatting best practices sub-score.
 */
@Composable
fun AtsScoreProgressCard(
    score: Int,
    jobTitle: String,
    companyName: String,
    modifier: Modifier = Modifier,
    keywordMatchPercent: Int? = null,
    impactScorePercent: Int? = null,
    formatScorePercent: Int? = null
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ats_score_progress_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AtsScoreProgressBar(
                score = score,
                jobTitle = jobTitle,
                companyName = companyName,
                barHeight = 16.dp,
                showPercentage = true,
                showRatingBadge = true,
                showThresholdMarkers = true,
                showRecommendation = true
            )

            // Optional Sub-Metric Breakdown Row
            if (keywordMatchPercent != null || impactScorePercent != null || formatScorePercent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    keywordMatchPercent?.let { kw ->
                        SubScorePill(
                            label = "Keywords",
                            score = kw,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    impactScorePercent?.let { imp ->
                        SubScorePill(
                            label = "Impact Metrics",
                            score = imp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    formatScorePercent?.let { fmt ->
                        SubScorePill(
                            label = "Formatting",
                            score = fmt,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScorePill(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> AtsScoreHigh
        score >= 65 -> AtsScoreMedium
        else -> AtsScoreLow
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.testTag("subscore_pill_${label.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact single-line progress bar representation for dense lists or bottom navigation bars.
 */
@Composable
fun CompactAtsScoreProgressBar(
    score: Int,
    modifier: Modifier = Modifier,
    label: String = "ATS Match"
) {
    val clamped = score.coerceIn(0, 100)
    val tier = AtsScoreRatingTier.fromScore(clamped)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("compact_ats_score_progress_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$clamped% $label",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tier.primaryColor,
            modifier = Modifier.testTag("compact_ats_score_text")
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(tier.gradientColors))
            )
        }
    }
}
