package com.ssbmax.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ssbmax.data.local.entity.TATStoryAssessmentEntity

@Dao
interface TATStoryAssessmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: TATStoryAssessmentEntity)

    @Query("SELECT * FROM tat_story_assessments WHERE submissionId = :submissionId ORDER BY storyIndex ASC")
    suspend fun getBySubmissionId(submissionId: String): List<TATStoryAssessmentEntity>

    @Query("SELECT * FROM tat_story_assessments WHERE submissionId = :submissionId AND questionId = :questionId LIMIT 1")
    suspend fun getByQuestionId(submissionId: String, questionId: String): TATStoryAssessmentEntity?

    @Query("DELETE FROM tat_story_assessments WHERE submissionId = :submissionId")
    suspend fun deleteBySubmissionId(submissionId: String)
}
