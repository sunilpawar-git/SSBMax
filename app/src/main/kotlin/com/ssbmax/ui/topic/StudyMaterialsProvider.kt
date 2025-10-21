package com.ssbmax.ui.topic

/**
 * Provides comprehensive study materials for all SSB topics
 * Separated to keep files under 300 lines
 */
object StudyMaterialsProvider {
    
    fun getStudyMaterials(testType: String): List<StudyMaterialItem> {
        return when (testType.uppercase()) {
            "OIR" -> getOIRMaterials()
            "PPDT" -> getPPDTMaterials()
            "PIQ_FORM" -> getPIQFormMaterials()
            "PSYCHOLOGY" -> getPsychologyMaterials()
            "GTO" -> getGTOMaterials()
            "INTERVIEW" -> getInterviewMaterials()
            "CONFERENCE" -> getConferenceMaterials()
            "MEDICALS" -> getMedicalsMaterials()
            "SSB_OVERVIEW" -> getSSBOverviewMaterials()
            else -> emptyList()
        }
    }
    
    private fun getOIRMaterials() = listOf(
        StudyMaterialItem(
            id = "oir_1",
            title = "Understanding OIR Test Pattern",
            duration = "8 min read",
            isPremium = false
        ),
        StudyMaterialItem(
            id = "oir_2",
            title = "Verbal Reasoning Mastery",
            duration = "12 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "oir_3",
            title = "Non-Verbal Reasoning Strategies",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "oir_4",
            title = "Time Management in OIR",
            duration = "6 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "oir_5",
            title = "Common Mistakes to Avoid",
            duration = "10 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "oir_6",
            title = "Practice Sets with Solutions",
            duration = "30 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "oir_7",
            title = "Mental Math Shortcuts",
            duration = "10 min read",
            isPremium = false,
        )
    )
    
    private fun getPPDTMaterials() = listOf(
        StudyMaterialItem(
            id = "ppdt_1",
            title = "PPDT Test Overview",
            duration = "7 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "ppdt_2",
            title = "Story Writing Techniques",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "ppdt_3",
            title = "Group Discussion Strategies",
            duration = "12 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "ppdt_4",
            title = "Character Perception Skills",
            duration = "10 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "ppdt_5",
            title = "Sample PPDT Stories",
            duration = "20 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "ppdt_6",
            title = "Common PPDT Mistakes",
            duration = "8 min read",
            isPremium = false,
        )
    )
    
    private fun getPsychologyMaterials() = listOf(
        StudyMaterialItem(
            id = "psy_1",
            title = "Psychology Tests Overview",
            duration = "10 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "psy_2",
            title = "TAT Mastery Guide",
            duration = "18 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "psy_3",
            title = "WAT Response Strategies",
            duration = "12 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "psy_4",
            title = "SRT Situation Analysis",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "psy_5",
            title = "Self Description Writing",
            duration = "14 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "psy_6",
            title = "Officer Like Qualities Explained",
            duration = "20 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "psy_7",
            title = "Psychology Test Practice Sets",
            duration = "45 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "psy_8",
            title = "Psychological Mindset Development",
            duration = "16 min read",
            isPremium = false,
        )
    )
    
    private fun getGTOMaterials() = listOf(
        StudyMaterialItem(
            id = "gto_1",
            title = "GTO Tasks Overview",
            duration = "12 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "gto_2",
            title = "Group Discussion Mastery",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "gto_3",
            title = "Progressive Group Task Tips",
            duration = "18 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "gto_4",
            title = "Half Group Task Techniques",
            duration = "14 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "gto_5",
            title = "Lecturette Preparation",
            duration = "10 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "gto_6",
            title = "Command Task Leadership",
            duration = "12 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "gto_7",
            title = "Snake Race & FGT Strategies",
            duration = "16 min read",
            isPremium = true,
        )
    )
    
    private fun getInterviewMaterials() = listOf(
        StudyMaterialItem(
            id = "int_1",
            title = "SSB Interview Process",
            duration = "10 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "int_2",
            title = "Personal Interview Preparation",
            duration = "20 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "int_3",
            title = "Current Affairs Mastery",
            duration = "25 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "int_4",
            title = "Military Knowledge Basics",
            duration = "30 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "int_5",
            title = "Interview Body Language",
            duration = "8 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "int_6",
            title = "Common Interview Questions",
            duration = "18 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "int_7",
            title = "Mock Interview Scenarios",
            duration = "40 min read",
            isPremium = true,
        )
    )
    
    private fun getConferenceMaterials() = listOf(
        StudyMaterialItem(
            id = "conf_1",
            title = "Conference Process Explained",
            duration = "8 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "conf_2",
            title = "Final Assessment Criteria",
            duration = "12 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "conf_3",
            title = "Conference Etiquette",
            duration = "6 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "conf_4",
            title = "Handling Results",
            duration = "10 min read",
            isPremium = false,
        )
    )
    
    private fun getMedicalsMaterials() = listOf(
        StudyMaterialItem(
            id = "med_1",
            title = "Medical Standards Overview",
            duration = "10 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "med_2",
            title = "Vision Requirements",
            duration = "8 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "med_3",
            title = "Physical Fitness Standards",
            duration = "12 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "med_4",
            title = "Medical Examination Process",
            duration = "15 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "med_5",
            title = "Common Medical Rejections",
            duration = "10 min read",
            isPremium = true,
        )
    )
    
    private fun getPIQFormMaterials() = listOf(
        StudyMaterialItem(
            id = "piq_1",
            title = "PIQ Form Guide",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "piq_2",
            title = "Self-Consistency Tips",
            duration = "10 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "piq_3",
            title = "Common PIQ Mistakes",
            duration = "8 min read",
            isPremium = true,
        )
    )
    
    private fun getSSBOverviewMaterials() = listOf(
        StudyMaterialItem(
            id = "ssb_1",
            title = "Complete SSB Process",
            duration = "20 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "ssb_2",
            title = "Preparation Roadmap",
            duration = "15 min read",
            isPremium = false,
        ),
        StudyMaterialItem(
            id = "ssb_3",
            title = "Success Stories",
            duration = "25 min read",
            isPremium = true,
        ),
        StudyMaterialItem(
            id = "ssb_4",
            title = "Myths vs Reality",
            duration = "10 min read",
            isPremium = false,
        )
    )
}

