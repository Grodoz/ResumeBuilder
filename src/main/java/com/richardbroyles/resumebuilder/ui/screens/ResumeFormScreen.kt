package com.richardbroyles.resumebuilder.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardbroyles.resumebuilder.data.local.entity.Resume
import com.richardbroyles.resumebuilder.data.model.SkillItem
import com.richardbroyles.resumebuilder.viewmodel.ResumeViewModel
import kotlinx.coroutines.launch

@Composable
fun ResumeFormScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val structuredResume by viewModel.structuredResume.collectAsState()
    val activeProfile by viewModel.resumeState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var portfolioUrl by remember { mutableStateOf("") }
    var professionalSummary by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }

    var isSavedNotificationVisible by remember { mutableStateOf(false) }
    var activeStepIndex by remember { mutableIntStateOf(0) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Sync form values from Room database entity or fallback to active profile
    LaunchedEffect(structuredResume, activeProfile) {
        val entity = structuredResume
        if (entity != null) {
            fullName = entity.fullName
            targetRole = entity.targetRole
            email = entity.email
            phone = entity.phone
            location = entity.location
            linkedinUrl = entity.linkedinUrl
            portfolioUrl = entity.portfolioUrl
            professionalSummary = entity.professionalSummary
            experience = entity.experience
            education = entity.education
            skills = entity.skills
        } else {
            fullName = activeProfile.fullName
            targetRole = activeProfile.targetRole
            email = activeProfile.email
            phone = activeProfile.phone
            location = activeProfile.location
            linkedinUrl = activeProfile.linkedinUrl
            portfolioUrl = activeProfile.portfolioUrl
            professionalSummary = activeProfile.summary
            experience = activeProfile.experiences.joinToString("\n\n") { exp ->
                "${exp.jobTitle} at ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}, ${exp.location})\n" +
                        exp.bullets.joinToString("\n") { "• $it" }
            }
            education = activeProfile.educations.joinToString("\n\n") { edu ->
                "${edu.degree} in ${edu.fieldOfStudy} - ${edu.institution} (${edu.graduationYear}, ${edu.gpaOrHonors})"
            }
            skills = activeProfile.skills.joinToString(", ") { it.name }
        }
    }

    // Calculate step completion progress
    val isContactComplete = fullName.isNotBlank() && (email.isNotBlank() || phone.isNotBlank())
    val isSummaryComplete = professionalSummary.isNotBlank()
    val isExperienceComplete = experience.isNotBlank()
    val isEducationComplete = education.isNotBlank()
    val isSkillsComplete = skills.isNotBlank()

    val completedStepsCount = listOf(
        isContactComplete,
        isSummaryComplete,
        isExperienceComplete,
        isEducationComplete
    ).count { it }

    val progressFraction = (completedStepsCount / 4f).coerceIn(0.05f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "progress")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Card with Back Action and Stepper Status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_header_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Column {
                            Text(
                                text = "Resume Builder",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Multi-step form with Room database persistence",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${(animatedProgress * 100).toInt()}% Done",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .testTag("multistep_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Horizontal Stepper Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("multistep_stepper_bar"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepChip(
                        stepNumber = 1,
                        label = "Contact",
                        icon = Icons.Default.Person,
                        isComplete = isContactComplete,
                        isActive = activeStepIndex == 0,
                        tag = "multistep_step_tab_1",
                        onClick = {
                            activeStepIndex = 0
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        }
                    )
                    StepChip(
                        stepNumber = 2,
                        label = "Summary",
                        icon = Icons.Default.Description,
                        isComplete = isSummaryComplete,
                        isActive = activeStepIndex == 1,
                        tag = "multistep_step_tab_2",
                        onClick = {
                            activeStepIndex = 1
                            coroutineScope.launch { scrollState.animateScrollTo(600) }
                        }
                    )
                    StepChip(
                        stepNumber = 3,
                        label = "Experience",
                        icon = Icons.Default.Work,
                        isComplete = isExperienceComplete,
                        isActive = activeStepIndex == 2,
                        tag = "multistep_step_tab_3",
                        onClick = {
                            activeStepIndex = 2
                            coroutineScope.launch { scrollState.animateScrollTo(1200) }
                        }
                    )
                    StepChip(
                        stepNumber = 4,
                        label = "Education",
                        icon = Icons.Default.School,
                        isComplete = isEducationComplete,
                        isActive = activeStepIndex == 3,
                        tag = "multistep_step_tab_4",
                        onClick = {
                            activeStepIndex = 3
                            coroutineScope.launch { scrollState.animateScrollTo(1800) }
                        }
                    )
                    StepChip(
                        stepNumber = 5,
                        label = "Skills & Save",
                        icon = Icons.Default.Save,
                        isComplete = isSkillsComplete,
                        isActive = activeStepIndex == 4,
                        tag = "multistep_step_tab_5",
                        onClick = {
                            activeStepIndex = 4
                            coroutineScope.launch { scrollState.animateScrollTo(2400) }
                        }
                    )
                }

                if (isSavedNotificationVisible) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resume entity saved to Room database!",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // STEP 1: USER CONTACT INFO
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_profile_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepCardHeader(
                    stepNumber = 1,
                    title = "Contact Information",
                    subtitle = "Personal identity, target position, and communication channels",
                    isComplete = isContactComplete,
                    icon = Icons.Default.Person
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_name_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetRole,
                    onValueChange = { targetRole = it },
                    label = { Text("Target Role / Professional Title") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp))
                    },
                    placeholder = { Text("e.g. Senior Software Architect") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_role_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_form_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_form_phone_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        placeholder = { Text("City, State") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_form_location_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = linkedinUrl,
                        onValueChange = { linkedinUrl = it },
                        label = { Text("LinkedIn URL") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        placeholder = { Text("linkedin.com/in/...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_form_linkedin_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = portfolioUrl,
                    onValueChange = { portfolioUrl = it },
                    label = { Text("Portfolio / GitHub URL") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    placeholder = { Text("github.com/... or portfolio site") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_portfolio_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Step 1 Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            activeStepIndex = 1
                            coroutineScope.launch { scrollState.animateScrollTo(600) }
                        },
                        modifier = Modifier.testTag("step_next_btn_1")
                    ) {
                        Text("Continue to Summary")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // STEP 2: PROFESSIONAL SUMMARY
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_summary_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepCardHeader(
                    stepNumber = 2,
                    title = "Professional Summary",
                    subtitle = "A compelling 2-4 sentence executive overview of your career impact",
                    isComplete = isSummaryComplete,
                    icon = Icons.Default.Description
                )

                // Quick Inspiration Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            if (professionalSummary.isBlank()) {
                                professionalSummary = "Dynamic and results-driven $targetRole with extensive experience designing resilient distributed systems, mentoring high-velocity engineering teams, and optimizing production latency."
                            }
                        },
                        label = { Text("Insert Executive Template", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    )
                    AssistChip(
                        onClick = {
                            if (professionalSummary.isBlank()) {
                                professionalSummary = "Innovative technical leader with deep expertise across modern Android architecture, Jetpack Compose, Kotlin coroutines, and cloud synchronization."
                            }
                        },
                        label = { Text("Insert Mobile Architect Template", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                OutlinedTextField(
                    value = professionalSummary,
                    onValueChange = { professionalSummary = it },
                    placeholder = {
                        Text("Enter 2-4 sentences highlighting your years of experience, core domains, and major career achievements...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_summary_input"),
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )

                // Step 2 Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeStepIndex = 0
                            coroutineScope.launch { scrollState.animateScrollTo(0) }
                        },
                        modifier = Modifier.testTag("step_prev_btn_2")
                    ) {
                        Text("Back to Contact")
                    }

                    Button(
                        onClick = {
                            activeStepIndex = 2
                            coroutineScope.launch { scrollState.animateScrollTo(1200) }
                        },
                        modifier = Modifier.testTag("step_next_btn_2")
                    ) {
                        Text("Continue to Experience")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // STEP 3: WORK EXPERIENCE
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_experience_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepCardHeader(
                    stepNumber = 3,
                    title = "Work Experience",
                    subtitle = "Employment history formatted with quantifiable STAR method achievements",
                    isComplete = isExperienceComplete,
                    icon = Icons.Default.Work
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Format Tip: Role at Company (Dates, Location) followed by bulleted STAR accomplishments (Situation, Task, Action, Result).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = experience,
                    onValueChange = { experience = it },
                    placeholder = {
                        Text(
                            "Role at Company (Dates, Location)\n" +
                                    "• Accomplished [X], measured by [Y], by doing [Z]\n" +
                                    "• Architected scalable Room database caching, reducing latency by 45%\n" +
                                    "• Spearheaded CI/CD pipelines increasing test coverage to 92%"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_experience_input"),
                    minLines = 6,
                    maxLines = 14,
                    shape = RoundedCornerShape(12.dp)
                )

                // Step 3 Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeStepIndex = 1
                            coroutineScope.launch { scrollState.animateScrollTo(600) }
                        },
                        modifier = Modifier.testTag("step_prev_btn_3")
                    ) {
                        Text("Back to Summary")
                    }

                    Button(
                        onClick = {
                            activeStepIndex = 3
                            coroutineScope.launch { scrollState.animateScrollTo(1800) }
                        },
                        modifier = Modifier.testTag("step_next_btn_3")
                    ) {
                        Text("Continue to Education")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // STEP 4: EDUCATIONAL BACKGROUND
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_education_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepCardHeader(
                    stepNumber = 4,
                    title = "Educational Background",
                    subtitle = "Degrees, universities, academic honors, and professional certifications",
                    isComplete = isEducationComplete,
                    icon = Icons.Default.School
                )

                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    placeholder = {
                        Text(
                            "Degree in Field of Study - University (Graduation Year, GPA/Honors)\n" +
                                    "Certifications: AWS Solutions Architect, Google Cloud Professional..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_education_input"),
                    minLines = 3,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )

                // Step 4 Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            activeStepIndex = 2
                            coroutineScope.launch { scrollState.animateScrollTo(1200) }
                        },
                        modifier = Modifier.testTag("step_prev_btn_4")
                    ) {
                        Text("Back to Experience")
                    }

                    Button(
                        onClick = {
                            activeStepIndex = 4
                            coroutineScope.launch { scrollState.animateScrollTo(2400) }
                        },
                        modifier = Modifier.testTag("step_next_btn_4")
                    ) {
                        Text("Continue to Skills & Save")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ==========================================
        // STEP 5: SKILLS & DATABASE PERSISTENCE
        // ==========================================
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("resume_form_skills_card"),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StepCardHeader(
                    stepNumber = 5,
                    title = "Skills & Room Database Persistence",
                    subtitle = "Core proficiencies and save to local on-device Room storage",
                    isComplete = isSkillsComplete,
                    icon = Icons.Default.Save
                )

                OutlinedTextField(
                    value = skills,
                    onValueChange = { skills = it },
                    label = { Text("Skills & Technologies (comma separated)") },
                    placeholder = {
                        Text("Kotlin, Jetpack Compose, Room Database, Coroutines, Flow, Hilt, MVVM, Git...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_form_skills_input"),
                    minLines = 2,
                    maxLines = 6,
                    shape = RoundedCornerShape(10.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Primary Save to Room Database Action
                Button(
                    onClick = {
                        val contactInfoStr = listOfNotNull(
                            email.takeIf { it.isNotBlank() },
                            phone.takeIf { it.isNotBlank() },
                            location.takeIf { it.isNotBlank() }
                        ).joinToString(" | ")

                        val resumeEntity = Resume(
                            id = "primary_resume",
                            name = fullName.ifBlank { "Professional Candidate" },
                            fullName = fullName.ifBlank { "Professional Candidate" },
                            contactInfo = contactInfoStr,
                            email = email,
                            phone = phone,
                            location = location,
                            linkedinUrl = linkedinUrl,
                            portfolioUrl = portfolioUrl,
                            targetRole = targetRole,
                            professionalSummary = professionalSummary,
                            workExperience = experience,
                            experience = experience,
                            education = education,
                            skills = skills,
                            lastUpdated = System.currentTimeMillis()
                        )

                        // Save directly into Room database
                        viewModel.saveStructuredResume(resumeEntity)

                        // Also update active profile state for instant sync
                        val parsedSkills = skills.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .map {
                                SkillItem(
                                    name = it,
                                    category = "Technical",
                                    yearsExperience = 3
                                )
                            }

                        val updatedProfile = activeProfile.copy(
                            fullName = fullName.ifBlank { activeProfile.fullName },
                            email = email,
                            phone = phone,
                            location = location,
                            linkedinUrl = linkedinUrl,
                            portfolioUrl = portfolioUrl,
                            targetRole = targetRole,
                            summary = professionalSummary,
                            skills = if (parsedSkills.isNotEmpty()) parsedSkills else activeProfile.skills
                        )
                        viewModel.updateResume(updatedProfile)
                        isSavedNotificationVisible = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("resume_form_save_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Resume to Room Database",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Reusable header for each step card showing step badge, title, subtitle, and completion status.
 */
@Composable
private fun StepCardHeader(
    stepNumber: Int,
    title: String,
    subtitle: String,
    isComplete: Boolean,
    icon: ImageVector
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
                color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isComplete) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * Interactive step chip in the horizontal stepper bar.
 */
@Composable
private fun StepChip(
    stepNumber: Int,
    label: String,
    icon: ImageVector,
    isComplete: Boolean,
    isActive: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary
            isComplete -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "chipBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.onPrimary
            isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "chipText"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "$stepNumber.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal),
                color = contentColor
            )
        }
    }
}
