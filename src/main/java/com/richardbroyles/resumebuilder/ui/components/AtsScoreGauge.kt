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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreHigh
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreMedium
import com.richardbroyles.resumebuilder.ui.theme.AtsScoreLow
import kotlin.math.cos
import kotlin.math.sin

data class AtsOptimizationTier(
    val title: String,
    val subtitle: String,
    val color: Color,
    val icon: ImageVector,
    val recommendation: String
)

fun getAtsOptimizationTier(score: Int): AtsOptimizationTier {
    return when {
        score >= 85 -> AtsOptimizationTier(
            title = "Optimal / Top Tier",
            subtitle = "Top 5% Candidate Screening Match",
            color = AtsScoreHigh,
            icon = Icons.Default.CheckCircle,
            recommendation = "Resume clears automatic ATS parsers and ranks high in recruiter searches."
        )
        score >= 70 -> AtsOptimizationTier(
            title = "Competitive Match",
            subtitle = "Passes Initial Recruiter Screening",
            color = Color(0xFF0D9488),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            recommendation = "Strong alignment. Minor keyword additions can push this into the top percentile."
        )
        score >= 50 -> AtsOptimizationTier(
            title = "Moderate Optimization",
            subtitle = "Needs Targeted Keyword & Metric Enhancements",
            color = AtsScoreMedium,
            icon = Icons.Default.Warning,
            recommendation = "Missing essential role keywords or quantifiable impact metrics (XYZ formula)."
        )
        else -> AtsOptimizationTier(
            title = "Critical / High Filter Risk",
            subtitle = "High Probability of ATS Filter Drop",
            color = AtsScoreLow,
            icon = Icons.Default.ErrorOutline,
            recommendation = "Resume lacks primary skills and formatted sections required by automated ATS engines."
        )
    }
}

@Composable
fun AtsScoreGauge(
    score: Int,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 130.dp,
    strokeWidth: Dp = 11.dp,
    showTierBadge: Boolean = true,
    animated: Boolean = true
) {
    val tier = getAtsOptimizationTier(score)
    val clampedScore = score.coerceIn(0, 100)
    val targetProgress = clampedScore / 100f
    val progress by animateFloatAsState(
        targetValue = if (animated) targetProgress else targetProgress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ats_gauge_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val gaugeColor = tier.color
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.testTag("ats_score_gauge_component")
    ) {
        Box(
            modifier = Modifier
                .size(gaugeSize)
                .testTag("ats_gauge_dial_box"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(gaugeSize)) {
                val strokePx = strokeWidth.toPx()
                val diameter = this.size.minDimension
                val arcRadius = (diameter - strokePx) / 2f
                val centerOffset = Offset(this.size.width / 2f, this.size.height / 2)
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
                    brush = if(clampedScore > 50) gradientBrush else Brush.linearGradient(listOf(gaugeColor, gaugeColor)),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * progress,
                    useCenter = false,
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(diameter - strokePx, diameter - strokePx),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                val benchmarkAngle = Math.toRadians((startAngle + sweepAngle * 0.75))
                val dotX = centerOffset.x + arcRadius * cos(benchmarkAngle).toFloat()
                val dotY = centerOffset.y + arcRadius * sin(benchmarkAngle).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = strokePx * 0.28f,
                    center = Offset(dotX, dotY)
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = (gaugeSize.value * 0.24f).sp,
                        color = gaugeColor
                    ),
                    modifier = Modifier.testTag("ats_gauge_score_value")
                )
                Text(
                    text = "ATS SCORE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (gaugeSize.value * 0.07).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                )
            }
        }
        if (showTierBadge) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tier.color.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, tier.color.copy(alpha = 0.35f)),
                modifier = Modifier.testTag("ats_gauge_tier_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = tier.icon,
                        contentDescription = null,
                        tint = tier.color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tier.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = tier.color
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AtsScoreProgressBar(
    score: Int,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    benchMarkScore: Int = 75,
    showThresholdMarkers: Boolean = true
) {
    val tier = getAtsOptimizationTier(score)
    val clampedScore = score.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedScore / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "linear_ats_progress"
    )
    Column(modifier = modifier.fillMaxWidth().testTag("ats_score_progress_bar")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ATS Match Level",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = tier.color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tier.title,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = tier.color
                        )
                    )
                }
            }
            Text(
                text = "$score / 100",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = tier.color
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress.coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(height / 2))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                AtsScoreLow,
                                AtsScoreMedium,
                                tier.color
                            )
                        )
                    )
            )
            if (showThresholdMarkers) {
                val benchmarkFraction = (benchMarkScore / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = 0.dp)
                )
            }
        }
        if (showThresholdMarkers) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 (Needs Work)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                )
                Text(
                    text = "▲ $benchMarkScore% Target Recruiter Cutoff",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = "100 (Optimal)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                )
            }
        }
    }
}

@Composable
fun AtsScoreOptimizationCard(
    score: Int,
    modifier: Modifier = Modifier,
    title: String = "Resume ATS Optimization",
    subtitle: String? = "Automated Applicant Tracking System Readability",
    keywordScore: Int? = null,
    impactScore: Int? = null,
    formatScore: Int? = null,
    verbScore: Int? = null,
    onImproveClick: (() -> Unit)? = null
) {
    var displayGaugeMode by remember { mutableStateOf(true) }
    val tier = getAtsOptimizationTier(score)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ats_score_optimization_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clickable { displayGaugeMode = !displayGaugeMode }
                        .testTag("toggle_gauge_style_btn")
                ) {
                     Row(
                         modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                         verticalAlignment = Alignment.CenterVertically
                     ) {
                         Icon(
                             imageVector = Icons.Default.Tune,
                             contentDescription = "Switch display size",
                             tint = MaterialTheme.colorScheme.primary,
                             modifier = Modifier.size(13.dp)
                         )
                         Spacer(modifier = Modifier.width(4.dp))
                         Text(
                             text = if(displayGaugeMode) "Dial View" else "Bar View",
                             style = MaterialTheme.typography.labelSmall.copy(
                                 fontWeight = FontWeight.SemiBold,
                                 color = MaterialTheme.colorScheme.primary
                             )
                         )
                     }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            if (displayGaugeMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AtsScoreGauge(
                        score = score,
                        gaugeSize = 125.dp,
                        strokeWidth = 10.dp,
                        showTierBadge = false,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tier.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, tier.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tier.icon,
                                    contentDescription = null,
                                    tint = tier.color,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tier.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = tier.color
                                    )
                                )
                            }
                        }
                        Text(
                            text = tier.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tier.recommendation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            } else {
                AtsScoreProgressBar(
                    score = score,
                    height = 12.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = tier.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tier.recommendation,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            if (keywordScore != null || impactScore != null || formatScore != null || verbScore != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Core Optimization Dimensions",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    keywordScore?.let {
                        SubScoreRow(label = "keyword Density & Role Terms", score = it)
                    }
                    impactScore?.let {
                        SubScoreRow(label = "STAR / Quantified Metrics", score = it)
                    }
                    formatScore?.let {
                        SubScoreRow(label = "Parser Format & Layout Readiness", score = it)
                    }
                    verbScore?.let {
                        SubScoreRow(label = "Active Action Verb Strength", score = it)
                    }
                }
            }
            if (onImproveClick != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onImproveClick() }
                        .testTag("ats_improve_score_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Optimize Resume with Gemini AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScoreRow(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val barColor = when {
        score >= 80 -> AtsScoreHigh
        score >= 65 -> AtsScoreMedium
        else -> AtsScoreLow
    }
    Column(modifier = modifier.fillMaxWidth()) {
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
                text = "$score%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score / 100f).coerceIn(0.04f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}
