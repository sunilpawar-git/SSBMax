package com.ssbmax.ui.study

import android.content.Context
import java.io.IOException

/**
 * Provides comprehensive study material content for all SSB topics
 * Separated from ViewModel to keep files under 300 lines
 * 
 * Content is organized progressively from foundational to advanced concepts
 * All materials are researched from authentic SSB sources and structured educationally
 */
object StudyMaterialContentProvider {
    
    fun getMaterial(materialId: String): StudyMaterialContent {
        return when (materialId) {
            // PIQ Form HTML document
            "piq_form_reference" -> getPIQFormHTML()
            
            // PIQ Materials (placeholder content)
            "piq_1" -> getPIQ1()
            "piq_2" -> getPIQ2()
            "piq_3" -> getPIQ3()
            
            // OIR Materials (7 materials)
            "oir_1" -> getOir1()
            "oir_2" -> getOir2()
            "oir_3" -> OIRMaterialContent.getOir3()
            "oir_4" -> OIRMaterialContent.getOir4()
            "oir_5" -> OIRMaterialContent2.getOir5()
            "oir_6" -> OIRMaterialContent2.getOir6()
            "oir_7" -> OIRMaterialContent2.getOir7()
            
            // PPDT Materials (6 materials)
            "ppdt_1" -> PPDTMaterialContent.getPpdt1()
            "ppdt_2" -> PPDTMaterialContent.getPpdt2()
            "ppdt_3" -> PPDTMaterialContent2.getPpdt3()
            "ppdt_4" -> PPDTMaterialContent2.getPpdt4()
            "ppdt_5" -> PPDTMaterialContent2.getPpdt5()
            "ppdt_6" -> PPDTMaterialContent2.getPpdt6()
            
            // Psychology Materials (8 materials)
            "psy_1" -> PsychologyMaterialContent.getPsy1()
            "psy_2" -> PsychologyMaterialContent.getPsy2()
            "psy_3" -> PsychologyMaterialContent.getPsy3()
            "psy_4" -> PsychologyMaterialContent.getPsy4()
            "psy_5" -> PsychologyMaterialContent2.getPsy5()
            "psy_6" -> PsychologyMaterialContent2.getPsy6()
            "psy_7" -> PsychologyMaterialContent2.getPsy7()
            "psy_8" -> PsychologyMaterialContent2.getPsy8()
            
            // GTO Materials (7 materials)
            "gto_1" -> GTOMaterialContent.getGto1()
            "gto_2" -> GTOMaterialContent.getGto2()
            "gto_3" -> GTOMaterialContent.getGto3()
            "gto_4" -> GTOMaterialContent.getGto4()
            "gto_5" -> GTOMaterialContent2.getGto5()
            "gto_6" -> GTOMaterialContent2.getGto6()
            "gto_7" -> GTOMaterialContent2.getGto7()
            
            // Interview Materials (7 materials)
            "int_1" -> InterviewMaterialContent.getInt1()
            "int_2" -> InterviewMaterialContent.getInt2()
            "int_3" -> InterviewMaterialContent.getInt3()
            "int_4" -> InterviewMaterialContent.getInt4()
            "int_5" -> InterviewMaterialContent2.getInt5()
            "int_6" -> InterviewMaterialContent2.getInt6()
            "int_7" -> InterviewMaterialContent2.getInt7()
            
            // Default fallback
            else -> getDefaultMaterial(materialId)
        }
    }
    
    // OIR Materials
    private fun getOir1() = StudyMaterialContent(
        id = "oir_1",
        title = "Understanding OIR Test Pattern",
        category = "OIR Preparation",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "8 min read",
        content = """
# Understanding OIR Test Pattern

The Officer Intelligence Rating (OIR) test is the first major hurdle in the SSB selection process, conducted on Day 1 as part of the screening stage. Understanding its pattern and purpose is crucial for your success in the armed forces selection journey.

## What is OIR Test?

The OIR test is a comprehensive intelligence assessment designed to evaluate your cognitive abilities, logical reasoning, and problem-solving skills. Unlike academic exams, this test measures your mental alertness, ability to think under pressure, and aptitude for military leadership roles.

**Key Purpose**: The test determines whether you possess the intellectual capacity required for officer-level responsibilities in the Indian Armed Forces.

## Test Structure and Format

**Duration**: 30-40 minutes (varies by selection center)

**Total Questions**: 40-50 questions

**Question Format**: Multiple choice questions (MCQs) with 4-5 options

**Scoring**: Based on correct answers; negative marking may apply at some centers

## Major Sections Covered

### 1. Verbal Reasoning (35-40% weightage)
Tests your ability to understand and analyze written information:
- Analogies and relationships between words
- Synonyms and antonyms
- Sentence completion
- Coding-decoding patterns
- Series completion with words/letters

### 2. Non-Verbal Reasoning (30-35% weightage)
Assesses spatial intelligence and pattern recognition:
- Figure series and patterns
- Mirror and water images
- Embedded figures
- Paper folding and cutting
- Cube and dice problems

### 3. Numerical Ability (20-25% weightage)
Evaluates mathematical and logical reasoning:
- Number series
- Arithmetic operations
- Percentages and ratios
- Data interpretation
- Speed, time, and distance problems

### 4. General Intelligence (10-15% weightage)
Measures overall mental agility:
- Logical deductions
- Blood relations
- Direction sense
- Ranking and ordering
- Statement and conclusions

## Difficulty Level

The OIR test is designed to be **moderately challenging**. Questions range from easy to difficult, with most falling in the medium difficulty category. The test is not about testing your academic knowledge but your **thinking speed and accuracy**.

## What Makes OIR Different?

**Time Pressure**: You get approximately 45-60 seconds per question, making speed crucial

**Variety**: Questions span multiple cognitive domains, testing different aspects of intelligence

**Adaptive Difficulty**: Questions may progressively become harder to differentiate between candidates

**No Subject Expertise Required**: Unlike academic tests, OIR doesn't require specialized subject knowledge

## Scoring and Cut-off

While exact cut-off scores are not publicly disclosed, generally:
- **Excellent Performance**: 35+ correct answers
- **Good Performance**: 28-34 correct answers
- **Borderline**: 22-27 correct answers
- **Below Average**: Less than 22 correct answers

**Important**: Your OIR score is combined with your PPDT performance for screening. A strong OIR score can compensate for an average PPDT performance.

## Common Question Distribution

Based on recent SSB patterns:
- Verbal Reasoning: 18-20 questions
- Non-Verbal Reasoning: 15-18 questions
- Numerical Ability: 10-12 questions
- General Intelligence: 5-8 questions

## Key Success Factors

**Speed with Accuracy**: Don't sacrifice accuracy for speed; find the right balance

**Strategic Skipping**: If stuck on a question for more than 1 minute, move on and return later

**Pattern Recognition**: Develop the ability to quickly identify question types

**Mental Stamina**: Maintain concentration throughout the test duration

## Preparation Timeline

**Beginners**: 2-3 months of consistent practice

**Intermediate**: 4-6 weeks of focused preparation

**Advanced**: 2-3 weeks of revision and mock tests

## What OIR Reveals About You

The test assesses:
- Mental agility and processing speed
- Ability to handle stress and time pressure
- Logical thinking capabilities
- Problem-solving approach
- Attention to detail

## Conclusion

Understanding the OIR test pattern is your first step toward mastering it. The test is designed to be challenging but fair, rewarding those who prepare systematically. Focus on building speed through consistent practice while maintaining accuracy.

**Next Step**: Move on to mastering verbal reasoning techniques to build a strong foundation in the most weighted section of the OIR test.
        """.trimIndent(),
        isPremium = false,
        tags = listOf("OIR", "Test Pattern", "SSB Screening", "Intelligence Test"),
        relatedMaterials = listOf(
            RelatedMaterial("oir_2", "Verbal Reasoning Mastery"),
            RelatedMaterial("oir_3", "Non-Verbal Reasoning Strategies")
        )
    )
    
    private fun getOir2() = StudyMaterialContent(
        id = "oir_2",
        title = "Verbal Reasoning Mastery",
        category = "OIR Preparation",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "12 min read",
        content = """
# Verbal Reasoning Mastery

Verbal reasoning forms the backbone of the OIR test, accounting for 35-40% of all questions. Mastering this section can significantly boost your overall OIR score and increase your chances of clearing the screening stage.

## Why Verbal Reasoning Matters

Verbal reasoning isn't just about vocabulary—it tests your ability to:
- Understand relationships between concepts
- Identify logical patterns in language
- Make quick decisions based on linguistic information
- Process and analyze written information rapidly

**For Officers**: Strong verbal reasoning indicates effective communication skills, critical thinking, and the ability to understand complex orders and situations.

## Major Question Types

### 1. Analogies (High Priority)

**Format**: CAT : KITTEN :: DOG : ?

**What it tests**: Relationship recognition between words

**Approach**:
- Identify the exact relationship (animal : offspring)
- Apply the same relationship to find the answer (PUPPY)
- Common relationships: synonyms, antonyms, part-to-whole, cause-effect

**Practice Tip**: Create mental categories of relationships and practice 50+ analogies daily

### 2. Synonyms and Antonyms

**Format**: Find the synonym/antonym of "BRAVE"

**Strategy**:
- Build a strong vocabulary foundation (500-1000 words)
- Learn words in context, not isolation
- Use word roots, prefixes, and suffixes
- Practice with previous year questions

**High-Frequency Words**: Courage, valor, audacity, tenacity, resilience, diligence, meticulous, pragmatic, etc.

### 3. Sentence Completion

**Format**: "Despite the _____, the soldier continued his mission."

**Approach**:
- Read the complete sentence to understand context
- Look for clue words (despite, although, because, therefore)
- Identify if the blank requires a contrasting or supporting word
- Choose the option that maintains logical flow

**Common Clue Words**:
- Contrast: despite, although, however, yet
- Support: because, therefore, thus, hence
- Addition: moreover, furthermore, additionally

### 4. Coding-Decoding

**Format**: If ARMY is coded as BSNZ, how is NAVY coded?

**Pattern Recognition**:
- Letter shift (+1 in this example: A→B, R→S, M→N, Y→Z)
- Reverse coding
- Position-based coding
- Mixed patterns

**Quick Technique**: Write the alphabet and shifted alphabet below it for reference

### 5. Letter/Word Series

**Format**: AC, FH, KM, ?

**Approach**:
- Identify the pattern (gap of 3, gap of 4, gap of 5...)
- Check for alternating patterns
- Look for vowel-consonant patterns
- Consider reverse sequences

**Common Patterns**:
- Arithmetic progression in letter positions
- Alternating sequences
- Prime number gaps
- Fibonacci-like progressions

## Proven Strategies for Success

### Time Management
- Allocate 45-50 seconds per question
- Start with question types you're strongest in
- Mark difficult questions and return later
- Don't get stuck on any single question

### Vocabulary Building
- Learn 20-30 new words daily
- Use flashcards or mobile apps
- Read newspapers, especially editorials
- Create word associations for better retention

### Pattern Recognition
- Practice identifying patterns quickly
- Create a mental library of common patterns
- Time yourself during practice
- Review wrong answers to understand patterns you missed

### Elimination Technique
- Cross out obviously wrong answers
- Narrow down to 2-3 options
- Use logic to select the best fit
- Trust your instinct when genuinely confused

## Common Mistakes to Avoid

**Overthinking**: Your first instinct is often correct; don't second-guess unnecessarily

**Ignoring Context**: Always read the complete question before attempting

**Time Mismanagement**: Don't spend 3 minutes on one question while rushing through others

**Vocabulary Cramming**: Focus on understanding word usage, not just meanings

**Skipping Practice**: Verbal reasoning improves only with consistent practice

## Daily Practice Routine

**Week 1-2**: Foundation building
- 50 analogies
- 30 synonyms/antonyms
- 20 sentence completions
- Learn 20 new words daily

**Week 3-4**: Speed building
- Timed practice (45 seconds per question)
- Mixed question sets
- Identify weak areas
- Review and revise

**Week 5+**: Mastery and refinement
- Full-length mock tests
- Time-bound practice
- Analyze mistakes
- Build confidence

## Recommended Resources

- Previous year OIR questions
- Standard reasoning books (R.S. Aggarwal, Arihant)
- Mobile apps for daily practice
- SSB-specific study materials

## Quick Tips for Test Day

- Read each question completely before attempting
- Don't get emotional about difficult questions
- Use the elimination method when confused
- Keep track of time every 10 questions
- Stay calm and focused throughout

## Measuring Progress

**Week 1**: 40-50% accuracy, 60-70 seconds per question
**Week 2-3**: 60-70% accuracy, 50-60 seconds per question
**Week 4+**: 75-85% accuracy, 40-50 seconds per question

## Conclusion

Verbal reasoning mastery comes from consistent practice, strategic learning, and pattern recognition. Build your vocabulary, practice daily, and focus on understanding the logic behind each question type. Remember, the goal is not just to answer correctly but to do so quickly and confidently.

**Next Step**: Move to non-verbal reasoning to build a complete skillset for OIR success.
        """.trimIndent(),
        isPremium = false,
        tags = listOf("Verbal Reasoning", "OIR Preparation", "Vocabulary", "Analogies"),
        relatedMaterials = listOf(
            RelatedMaterial("oir_1", "Understanding OIR Test Pattern"),
            RelatedMaterial("oir_3", "Non-Verbal Reasoning Strategies")
        )
    )
    
    private fun getDefaultMaterial(id: String) = StudyMaterialContent(
        id = id,
        title = "Study Material",
        category = "SSB Preparation",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "10 min read",
        content = "Content for this material is being prepared. Please check back soon!",
        isPremium = false,
        tags = listOf("SSB", "Preparation"),
        relatedMaterials = emptyList()
    )
    
    /**
     * Load PIQ form HTML from assets
     */
    private fun getPIQFormHTML(): StudyMaterialContent {
        // HTML content will be loaded from assets in ViewModel
        // Return a placeholder that indicates HTML content
        return StudyMaterialContent(
            id = "piq_form_reference",
            title = "SSB PIQ Form (Reference)",
            category = "PIQ Form",
            author = "SSB",
            publishedDate = "2025",
            readTime = "5 min read",
            content = "<!DOCTYPE html>", // Marker to indicate HTML content
            isPremium = false,
            tags = listOf("PIQ", "Form", "Reference"),
            relatedMaterials = emptyList()
        )
    }
    
    /**
     * Load HTML content from assets file
     */
    fun loadHTMLFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            "<html><body><p>Error loading HTML content</p></body></html>"
        }
    }
    
    // PIQ Materials (placeholder content until full content is added)
    private fun getPIQ1() = StudyMaterialContent(
        id = "piq_1",
        title = "PIQ Form Guide",
        category = "PIQ Form",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "15 min read",
        content = """
# PIQ Form Guide

This comprehensive guide will help you understand and complete the Personal Information Questionnaire (PIQ) form correctly for your SSB interview.

## Coming Soon

Detailed content for this material is being prepared and will be available soon.

**Key Points to Remember**:
- Fill the PIQ form honestly and accurately
- Maintain consistency across all sections
- Be prepared to explain every detail
- Know your PIQ thoroughly for the interview
        """.trimIndent(),
        isPremium = false,
        tags = listOf("PIQ", "Form", "Guide"),
        relatedMaterials = emptyList()
    )
    
    private fun getPIQ2() = StudyMaterialContent(
        id = "piq_2",
        title = "Self-Consistency Tips",
        category = "PIQ Form",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "10 min read",
        content = """
# Self-Consistency Tips for PIQ

Maintaining consistency in your PIQ form is crucial for SSB success.

## Coming Soon

Detailed content for this material is being prepared and will be available soon.
        """.trimIndent(),
        isPremium = false,
        tags = listOf("PIQ", "Consistency"),
        relatedMaterials = emptyList()
    )
    
    private fun getPIQ3() = StudyMaterialContent(
        id = "piq_3",
        title = "Common PIQ Mistakes",
        category = "PIQ Form",
        author = "SSB Expert",
        publishedDate = "Oct 22, 2025",
        readTime = "8 min read",
        content = """
# Common PIQ Mistakes

Avoid these common mistakes when filling out your PIQ form.

## Coming Soon

Detailed content for this material is being prepared and will be available soon.
        """.trimIndent(),
        isPremium = false,
        tags = listOf("PIQ", "Mistakes"),
        relatedMaterials = emptyList()
    )
}

