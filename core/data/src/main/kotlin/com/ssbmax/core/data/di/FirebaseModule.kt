package com.ssbmax.core.data.di

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for providing Firebase services.
 */
val firebaseModule = module {
    single<FirebaseAuth> { Firebase.auth }
    single<FirebaseFirestore> {
        Firebase.firestore
        // Note: Offline persistence is enabled by default in Firestore SDK
    }
    single<FirebaseStorage> { Firebase.storage }
    single<FirebaseMessaging> { Firebase.messaging }
    single<FirebaseAnalytics> { provideFirebaseAnalytics(androidContext()) }
}

// Permissions declared in app module's AndroidManifest.xml
@SuppressLint("MissingPermission")
private fun provideFirebaseAnalytics(context: Context): FirebaseAnalytics =
    FirebaseAnalytics.getInstance(context)
