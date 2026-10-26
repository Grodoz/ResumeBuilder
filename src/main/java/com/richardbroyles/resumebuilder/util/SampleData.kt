package com.richardbroyles.resumebuilder.util

import com.richardbroyles.resumebuilder.data.model.EducationItem
import com.richardbroyles.resumebuilder.data.model.ExperienceItem
import com.richardbroyles.resumebuilder.data.model.JobTarget
import com.richardbroyles.resumebuilder.data.model.ProjectItem
import com.richardbroyles.resumebuilder.data.model.ResumeProfile
import com.richardbroyles.resumebuilder.data.model.SkillItem

object SampleData {
    val defaultResume = ResumeProfile(
        id = "primary_resume",
        title = "Full-Stack Tech Lead Resume",
        fullName = "Alex Morgan",
        email = "alex.morgan.dev@gmail.com",
        phone = "(415) 555-0192",
        location = "San Francisco, CA",
        linkedinUrl = "linkedin.com/in/alexmorgandev",
        portfolioUrl = "alexmorgan.tech",
        targetRole = "Senior Full-Stack & Mobile Engineer",
        summary = "Results-driven Senior Software Engineer with 6+ years of experience architecting high-scale cloud platforms, responsive Android mobile apps, and distributed microservices. Proven track record of improving latency by 45%, leading cross-functional squads of 8 engineers, and deploying mission-critical AI-driven workflows.",
        experiences = listOf(
            ExperienceItem(
                id = "exp_1",
                jobTitle = "Senior Software Engineer",
                company = "Nexus Cloud Technologies",
                location = "San Francisco, CA",
                startDate = "Jan 2022",
                endDate = "Present",
                isCurrent = true,
                bullets = listOf(
                    "Architected high-throughput Kotlin and GraphQL microservices serving 4.2M daily active users with 99.99% uptime.",
                    "Reduced p99 API response latency by 42% through distributed Redis caching and query batching.",
                    "Led a cross-functional agile team of 7 engineers delivering real-time telemetry streaming in Jetpack Compose.",
                    "Automated CI/CD build deployment pipelines with GitHub Actions, slashing release cycle duration from 4 days to 45 minutes."
                )
            ),
            ExperienceItem(
                id = "exp_2",
                jobTitle = "Mobile & Full-Stack Engineer",
                company = "Apex Digital Labs",
                location = "Austin, TX",
                startDate = "Jun 2019",
                endDate = "Dec 2021",
                isCurrent = false,
                bullets = listOf(
                    "Developed customer-facing Android e-commerce application scaled to 500k+ downloads on Google Play.",
                    "Implemented Room offline-first sync engine and dynamic UI with Jetpack Compose and Coroutines.",
                    "Partnered with product managers and UX designers to increase in-app checkout conversion rate by 18.5%.",
                    "Integrated Stripe SDK and Google Pay tokenization, maintaining strict PCI-DSS compliance."
                )
            ),
            ExperienceItem(
                id = "exp_3",
                jobTitle = "Junior Software Developer",
                company = "Vanguard Innovations",
                location = "San Jose, CA",
                startDate = "Aug 2017",
                endDate = "May 2019",
                isCurrent = false,
                bullets = listOf(
                    "Maintained and enhanced core REST APIs in Java/Spring Boot and PostgreSQL database schemas.",
                    "Wrote unit and integration tests with JUnit, raising test code coverage from 62% to 88%."
                )
            )
        ),
        skills = listOf(
            SkillItem(name = "Kotlin", category = "Technical", yearsExperience = 6),
            SkillItem(name = "Jetpack Compose", category = "Technical", yearsExperience = 4),
            SkillItem(
                name = "Android SDK & Coroutines",
                category = "Technical",
                yearsExperience = 6
            ),
            SkillItem(name = "TypeScript & React", category = "Technical", yearsExperience = 4),
            SkillItem(name = "Python & FastApi", category = "Technical", yearsExperience = 3),
            SkillItem(
                name = "System Architecture & Microservices",
                category = "Technical",
                yearsExperience = 5
            ),
            SkillItem(
                name = "Google Cloud Platform (GCP)",
                category = "Technical",
                yearsExperience = 4
            ),
            SkillItem(name = "Docker & Kubernetes", category = "Technical", yearsExperience = 3),
            SkillItem(name = "CI/CD & DevOps Automation", category = "Tool", yearsExperience = 5),
            SkillItem(
                name = "Agile Squad Leadership & Mentorship",
                category = "Leadership",
                yearsExperience = 3
            ),
            SkillItem(name = "REST & GraphQL APIs", category = "Technical", yearsExperience = 5),
            SkillItem(name = "SQL & PostgreSQL", category = "Technical", yearsExperience = 5)
        ),
        educations = listOf(
            EducationItem(
                id = "edu_1",
                institution = "University of California, Berkeley",
                degree = "Bachelor of Science",
                fieldOfStudy = "Computer Science",
                graduationYear = "2017",
                gpaOrHonors = "Honors, Dean's Honor List"
            )
        ),
        projects = listOf(
            ProjectItem(
                id = "proj_1",
                title = "Distributed Realtime Cache Sync",
                role = "Lead Architect",
                description = "Open source cache replication library for distributed multi-region databases handling 50k ops/sec.",
                technologies = listOf("Kotlin", "gRPC", "Redis", "Docker"),
                url = "github.com/alexmorgan/distributed-cache"
            )
        ),
        certifications = listOf(
            "Google Cloud Certified Professional Cloud Architect",
            "Certified Kubernetes Administrator (CKA)"
        )
    )

    val sampleArchitectResume = ResumeProfile(
        id = "resume_architect",
        title = "Staff Android & Mobile Architect",
        fullName = "Alex Morgan",
        email = "alex.morgan.dev@gmail.com",
        phone = "(415) 555-0192",
        location = "San Francisco, CA",
        linkedinUrl = "linkedin.com/in/alexmorgandev",
        portfolioUrl = "alexmorgan.tech",
        targetRole = "Staff Android Solutions Architect",
        summary = "Principal & Staff Mobile Engineer with deep specialization in modern Android OS architecture, Jetpack Compose UI toolkits, high-performance offline data sync engines, and cross-functional engineering leadership.",
        experiences = listOf(
            ExperienceItem(
                id = "exp_arch_1",
                jobTitle = "Staff Android Architect",
                company = "Global Velocity Labs",
                location = "San Francisco, CA",
                startDate = "Mar 2021",
                endDate = "Present",
                isCurrent = true,
                bullets = listOf(
                    "Standardized Jetpack Compose architecture across 5 distributed client apps serving 10M+ users.",
                    "Improved app cold start time by 52% and reduced crash rate to 0.01% with strict coroutine scoping.",
                    "Authored developer guidelines adopted across 45 Android engineers."
                )
            ),
            ExperienceItem(
                id = "exp_arch_2",
                jobTitle = "Senior Android Engineer",
                company = "Aura Mobile Systems",
                location = "San Jose, CA",
                startDate = "Jan 2018",
                endDate = "Feb 2021",
                isCurrent = false,
                bullets = listOf(
                    "Architected offline-first Room database synchronization with optimistic conflict resolution.",
                    "Delivered modularized dynamic feature architecture cutting APK size by 35%."
                )
            )
        ),
        skills = listOf(
            SkillItem(name = "Kotlin & Coroutines", category = "Technical", yearsExperience = 7),
            SkillItem(name = "Jetpack Compose & M3", category = "Technical", yearsExperience = 4),
            SkillItem(name = "Android Performance & Profiling", category = "Technical", yearsExperience = 6),
            SkillItem(name = "Room & Local Persistence", category = "Technical", yearsExperience = 5),
            SkillItem(name = "Clean Architecture & Modularization", category = "Technical", yearsExperience = 6),
            SkillItem(name = "CI/CD & Gradle Automation", category = "Tool", yearsExperience = 5),
            SkillItem(name = "Engineering Mentorship", category = "Leadership", yearsExperience = 4)
        ),
        educations = listOf(
            EducationItem(
                id = "edu_arch_1",
                institution = "University of California, Berkeley",
                degree = "Bachelor of Science",
                fieldOfStudy = "Computer Science",
                graduationYear = "2017",
                gpaOrHonors = "Honors"
            )
        ),
        projects = listOf(
            ProjectItem(
                id = "proj_arch_1",
                title = "Compose Multiplatform Canvas Toolkit",
                role = "Creator",
                description = "High-performance vector rendering library for Jetpack Compose.",
                technologies = listOf("Kotlin", "Compose", "Android NDK")
            )
        ),
        certifications = listOf(
            "Associate Android Developer Certification",
            "Google Cloud Certified Professional Cloud Architect"
        )
    )

    val sampleJobs = listOf(
        JobTarget(
            id = "job_1",
            title = "Senior Android / Mobile Solutions Architect",
            company = "Starlight Global Systems",
            location = "San Francisco, CA (Hybrid)",
            rawDescription = """
Starlight Global Systems is seeking a Senior Android / Mobile Solutions Architect to drive next-generation connected mobile experiences.

Responsibilities:
- Architect, build, and deploy mission-critical Android applications using modern Kotlin, Jetpack Compose, and M3 standards.
- Collaborate with cloud teams to design high-throughput REST and GraphQL APIs.
- Optimize application performance, frame rates, and memory footprint.
- Mentor engineers, lead code reviews, and champion engineering best practices.
- Lead CI/CD automation pipelines and test-driven development (TDD).

Requirements:
- 5+ years building production Android applications in Kotlin.
- Deep expertise in Jetpack Compose, Coroutines, Flow, and Android Jetpack libraries.
- Strong background in scalable system design, offline-first data sync, and Room.
- Experience with cloud platforms (GCP or AWS) and Docker containerization.
- Demonstrated leadership in mentoring engineers and driving cross-functional projects.
- Experience with CI/CD automation and test coverage (JUnit, Robolectric).
            """.trimIndent(),
            requiredQualifications = listOf(
                "5+ years Android development in Kotlin",
                "Jetpack Compose and M3 design systems",
                "Kotlin Coroutines and Flow concurrency",
                "Offline-first synchronization & Room database",
                "System architecture & scalable REST/GraphQL APIs",
                "Cloud infrastructure (GCP/AWS)",
                "Agile technical leadership and mentoring junior engineers"
            ),
            preferredQualifications = listOf(
                "Experience with Google Drive API / OAuth integration",
                "AI/LLM prompt engineering and mobile integration",
                "Kubernetes & Docker container deployment"
            )
        ),
        JobTarget(
            id = "job_2",
            title = "Lead Full-Stack Software Engineer",
            company = "Vanguard AI Solutions",
            location = "Remote",
            rawDescription = """
Vanguard AI Solutions is looking for a Lead Full-Stack Software Engineer to build intelligent workflow products.
Must have strong background in Kotlin or Java, TypeScript, cloud services, and leading technical initiatives.
Experience in optimizing query performance, latency reduction, and designing intuitive enterprise UIs is essential.
            """.trimIndent(),
            requiredQualifications = listOf(
                "6+ years full-stack software development",
                "Kotlin, Java, or Python microservices",
                "Distributed caching and API latency optimization",
                "CI/CD pipelines and automated testing",
                "Cross-functional team leadership"
            ),
            preferredQualifications = listOf(
                "Experience with Generative AI / LLM APIs",
                "Docker and Kubernetes cluster orchestration"
            )
        )
    )
}

