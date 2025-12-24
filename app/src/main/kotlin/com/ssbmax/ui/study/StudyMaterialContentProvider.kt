package com.ssbmax.ui.study

import android.content.Context
import java.io.IOException

/**
 * LEGACY FALLBACK PROVIDER (Phase 1: Firestore Migration Complete)
 * 
 * This provider is now a MINIMAL fallback for edge cases only.
 * All study materials are loaded from Firestore via StudyMaterialDetailViewModel.
 * 
 * Only Purpose:
 * - Load PIQ HTML form from assets (not stored in Firestore)
 * - Provide generic "content unavailable" message for edge cases
 * 
 * TODO Phase 2: Remove this file entirely once PIQ form is migrated to Firestore
 */
object StudyMaterialContentProvider {
    
    /**
     * Minimal fallback for study materials
     * Should rarely be called - ViewModel loads from Firestore first
     */
    fun getMaterial(materialId: String): StudyMaterialContent {
        return when (materialId) {
            // PIQ Form HTML document (only asset-based content remaining)
            "piq_form_reference" -> getPIQFormHTML()
            
            // All other materials should be in Firestore
            // This fallback ensures app doesn't crash if Firestore fetch fails
            else -> getContentUnavailable(materialId)
        }
    }
    
    /**
     * PIQ Form HTML (loaded from assets/piq_form.html)
     * TODO: Migrate to Firestore in Phase 2 to fully eliminate this file
     */
    private fun getPIQFormHTML(): StudyMaterialContent {
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
     * Used by ViewModel to load PIQ form HTML
     */
    fun loadHTMLFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            "<html><body><p>Error loading HTML content: ${e.message}</p></body></html>"
        }
    }
    
    /**
     * Fallback for when Firestore content is unavailable
     * This should rarely be shown - indicates network/cache issue
     */
    private fun getContentUnavailable(id: String) = StudyMaterialContent(
        id = id,
        title = "Content Unavailable",
        category = "SSB Preparation",
        author = "SSB Expert",
        publishedDate = "2025",
        readTime = "N/A",
        content = """
# Content Currently Unavailable

This study material is stored in our cloud database. Please check:

- **Internet Connection**: Ensure you're connected to the internet
- **App Update**: Make sure you have the latest version of SSBMax
- **Try Again**: Pull down to refresh or close and reopen the material

All study materials are available offline once loaded (cached for 7 days).

If the problem persists, please contact support@ssbmax.com.
        """.trimIndent(),
        isPremium = false,
        tags = listOf("Error"),
        relatedMaterials = emptyList()
    )
}
