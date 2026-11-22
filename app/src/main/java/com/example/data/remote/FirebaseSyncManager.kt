package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.ReviewEntity
import com.example.data.model.UserAnimeEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.WatchStatus
import com.example.data.model.WatchlistEntity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?
)

data class CloudUserSnapshot(
    val profile: UserProfileEntity,
    val tutorialCompleted: Boolean,
    val library: List<UserAnimeEntity>,
    val reviews: List<ReviewEntity>,
    val watchlists: List<Pair<WatchlistEntity, List<String>>>
)

@Singleton
class FirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FirebaseSyncManager"
    }

    private val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    private val auth: FirebaseAuth?
        get() = if (isFirebaseInitialized) FirebaseAuth.getInstance() else null

    private val db: FirebaseFirestore?
        get() = if (isFirebaseInitialized) {
            val databaseId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(databaseId)
        } else {
            null
        }

    private val _authUser = MutableStateFlow<AuthUser?>(null)
    val authUser: StateFlow<AuthUser?> = _authUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _authUser.value = firebaseAuth.currentUser?.toAuthUser()
    }

    init {
        auth?.let { firebaseAuth ->
            _authUser.value = firebaseAuth.currentUser?.toAuthUser()
            firebaseAuth.addAuthStateListener(authListener)
        }
    }

    suspend fun attemptSilentGoogleSignIn(activityContext: Context): Boolean {
        val firebaseAuth = auth ?: return false
        if (firebaseAuth.currentUser != null) {
            _authUser.value = firebaseAuth.currentUser?.toAuthUser()
            return true
        }
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(true)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCred).await()
                _authUser.value = authResult.user?.toAuthUser()
                authResult.user != null
            } else {
                false
            }
        } catch (e: NoCredentialException) {
            Log.d(TAG, "No cached Google credential for silent sign-in")
            false
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Silent sign-in cancelled", e)
            false
        } catch (e: Exception) {
            Log.w(TAG, "Silent Google sign-in skipped: ${e.message}")
            false
        }
    }

    suspend fun signInWithGoogleInteractive(activityContext: Context): Result<AuthUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase not initialized"))
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val credentialManager = CredentialManager.create(activityContext)
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleId = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(firebaseCred).await()
                val user = authResult.user?.toAuthUser()
                    ?: return Result.failure(IllegalStateException("Google authentication succeeded without user"))
                _authUser.value = user
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unsupported credential type returned"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(activityContext: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(activityContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "clearCredentialState warning: ${e.message}")
            }
            try {
                auth?.signOut()
            } catch (e: Exception) {
                Log.w(TAG, "FirebaseAuth signOut warning: ${e.message}")
            }
            _authUser.value = null
        }
    }

    suspend fun ensureUserAndFetchSnapshot(user: AuthUser): CloudUserSnapshot? {
        val firestore = db ?: return null
        return try {
            val userRef = firestore.collection("users").document(user.uid)
            val userDoc = userRef.get().await()

            val defaultName = user.displayName.ifBlank { "Anime Fan" }.take(100)
            val defaultHandle = buildHandle(user.email, defaultName)

            val profile: UserProfileEntity
            val tutorialCompleted: Boolean

            if (!userDoc.exists()) {
                val createPayload = mapOf(
                    "uid" to user.uid.take(128),
                    "displayName" to defaultName,
                    "handle" to defaultHandle,
                    "bio" to "",
                    "favoriteGenres" to listOf("Action", "Fantasy"),
                    "tutorialCompleted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(createPayload).await()
                profile = UserProfileEntity(
                    id = 1,
                    displayName = defaultName,
                    handle = defaultHandle,
                    bio = "",
                    favoriteGenresCsv = "Action,Fantasy"
                )
                tutorialCompleted = false
            } else {
                val name = (userDoc.getString("displayName") ?: defaultName).ifBlank { defaultName }
                val handle = userDoc.getString("handle") ?: defaultHandle
                val bio = userDoc.getString("bio") ?: ""
                @Suppress("UNCHECKED_CAST")
                val genres = (userDoc.get("favoriteGenres") as? List<String>).orEmpty()
                profile = UserProfileEntity(
                    id = 1,
                    displayName = name,
                    handle = handle,
                    bio = bio,
                    favoriteGenresCsv = genres.joinToString(",")
                )
                tutorialCompleted = userDoc.getBoolean("tutorialCompleted") ?: false
            }

            val librarySnap = userRef.collection("library").get().await()
            val libraryList = librarySnap.documents.mapNotNull { doc ->
                val animeId = doc.getString("animeId") ?: doc.id
                val statusStr = doc.getString("status") ?: WatchStatus.PLANNED.name
                val status = WatchStatus.entries.firstOrNull { it.name == statusStr } ?: WatchStatus.PLANNED
                UserAnimeEntity(
                    animeId = animeId,
                    status = status,
                    watchedEpisodes = (doc.getLong("watchedEpisodes") ?: 0L).toInt().coerceIn(0, 5000),
                    currentSeason = (doc.getLong("currentSeason") ?: 1L).toInt().coerceIn(1, 50),
                    inLibrary = doc.getBoolean("inLibrary") ?: true,
                    isFavorite = doc.getBoolean("isFavorite") ?: false,
                    reminderEnabled = doc.getBoolean("reminderEnabled") ?: false,
                    personalNotes = doc.getString("personalNotes") ?: "",
                    addedAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            val reviewsSnap = userRef.collection("reviews").get().await()
            val reviewList = reviewsSnap.documents.mapNotNull { doc ->
                val animeId = doc.getString("animeId") ?: doc.id
                ReviewEntity(
                    animeId = animeId,
                    overallRating = (doc.getDouble("overallRating") ?: 8.0).toFloat().coerceIn(1f, 10f),
                    storyScore = (doc.getLong("storyScore") ?: 8L).toInt().coerceIn(1, 10),
                    animationScore = (doc.getLong("animationScore") ?: 8L).toInt().coerceIn(1, 10),
                    charactersScore = (doc.getLong("charactersScore") ?: 8L).toInt().coerceIn(1, 10),
                    soundtrackScore = (doc.getLong("soundtrackScore") ?: 8L).toInt().coerceIn(1, 10),
                    reviewText = doc.getString("reviewText") ?: "",
                    containsSpoilers = doc.getBoolean("containsSpoilers") ?: false,
                    updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                )
            }

            val wlSnap = userRef.collection("watchlists").get().await()
            val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val watchlists = wlSnap.documents.mapNotNull { doc ->
                val wlId = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: return@mapNotNull null
                val updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
                @Suppress("UNCHECKED_CAST")
                val animeIds = (doc.get("animeIds") as? List<String>).orEmpty()
                WatchlistEntity(
                    id = wlId,
                    name = name,
                    description = doc.getString("description") ?: "",
                    coverKey = doc.getString("coverKey") ?: "",
                    updatedDateLabel = "Updated ${dateFmt.format(Date(updatedAtMs))}",
                    updatedAt = updatedAtMs
                ) to animeIds
            }

            CloudUserSnapshot(
                profile = profile,
                tutorialCompleted = tutorialCompleted,
                library = libraryList,
                reviews = reviewList,
                watchlists = watchlists
            )
        } catch (e: Exception) {
            handleFirestoreError(e, "ensureUserAndFetchSnapshot(${user.uid})")
            null
        }
    }

    suspend fun pushProfile(profile: UserProfileEntity, tutorialCompleted: Boolean) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        try {
            val ref = firestore.collection("users").document(uid)
            val existing = ref.get().await()
            val safeName = profile.displayName.trim().ifBlank { "Anime Fan" }.take(100)
            val safeHandle = profile.handle.trim().take(60)
            val safeBio = profile.bio.trim().take(500)
            val genres = profile.favoriteGenres.take(20)
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "displayName" to safeName,
                        "handle" to safeHandle,
                        "bio" to safeBio,
                        "favoriteGenres" to genres,
                        "tutorialCompleted" to tutorialCompleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "uid" to uid.take(128),
                        "displayName" to safeName,
                        "handle" to safeHandle,
                        "bio" to safeBio,
                        "favoriteGenres" to genres,
                        "tutorialCompleted" to tutorialCompleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, "pushProfile($uid)")
        }
    }

    suspend fun pushTutorialCompleted(completed: Boolean) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        try {
            val ref = firestore.collection("users").document(uid)
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "tutorialCompleted" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, "pushTutorialCompleted($uid)")
        }
    }

    suspend fun pushLibraryEntry(
        tracking: UserAnimeEntity,
        title: String,
        posterUrl: String
    ) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val docId = tracking.animeId.take(80)
        try {
            val ref = firestore.collection("users").document(uid).collection("library").document(docId)
            val existing = ref.get().await()
            val safeTitle = title.trim().ifBlank { docId }.take(200)
            val safePoster = posterUrl.trim().take(500)
            val safeNotes = tracking.personalNotes.trim().take(2000)
            val watched = tracking.watchedEpisodes.coerceIn(0, 5000)
            val season = tracking.currentSeason.coerceIn(1, 50)

            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "title" to safeTitle,
                        "posterUrl" to safePoster,
                        "status" to tracking.status.name,
                        "watchedEpisodes" to watched,
                        "currentSeason" to season,
                        "inLibrary" to tracking.inLibrary,
                        "isFavorite" to tracking.isFavorite,
                        "reminderEnabled" to tracking.reminderEnabled,
                        "personalNotes" to safeNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "animeId" to docId,
                        "title" to safeTitle,
                        "posterUrl" to safePoster,
                        "status" to tracking.status.name,
                        "watchedEpisodes" to watched,
                        "currentSeason" to season,
                        "inLibrary" to tracking.inLibrary,
                        "isFavorite" to tracking.isFavorite,
                        "reminderEnabled" to tracking.reminderEnabled,
                        "personalNotes" to safeNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, "pushLibraryEntry($docId)")
        }
    }

    suspend fun pushReview(review: ReviewEntity) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val docId = review.animeId.take(80)
        try {
            val ref = firestore.collection("users").document(uid).collection("reviews").document(docId)
            val existing = ref.get().await()
            val overall = review.overallRating.toDouble().coerceIn(1.0, 10.0)
            val story = review.storyScore.coerceIn(1, 10)
            val anim = review.animationScore.coerceIn(1, 10)
            val chars = review.charactersScore.coerceIn(1, 10)
            val ost = review.soundtrackScore.coerceIn(1, 10)
            val text = review.reviewText.trim().take(3000)

            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "overallRating" to overall,
                        "storyScore" to story,
                        "animationScore" to anim,
                        "charactersScore" to chars,
                        "soundtrackScore" to ost,
                        "reviewText" to text,
                        "containsSpoilers" to review.containsSpoilers,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "animeId" to docId,
                        "overallRating" to overall,
                        "storyScore" to story,
                        "animationScore" to anim,
                        "charactersScore" to chars,
                        "soundtrackScore" to ost,
                        "reviewText" to text,
                        "containsSpoilers" to review.containsSpoilers,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, "pushReview($docId)")
        }
    }

    suspend fun deleteReview(animeId: String) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        try {
            firestore.collection("users").document(uid).collection("reviews")
                .document(animeId.take(80)).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, "deleteReview($animeId)")
        }
    }

    suspend fun pushWatchlist(watchlist: WatchlistEntity, animeIds: List<String>) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        val docId = watchlist.id.take(80)
        try {
            val ref = firestore.collection("users").document(uid).collection("watchlists").document(docId)
            val existing = ref.get().await()
            val safeName = watchlist.name.trim().ifBlank { "Untitled Watchlist" }.take(120)
            val safeDesc = watchlist.description.trim().take(1000)
            val safeCover = watchlist.coverKey.trim().take(500)
            val safeIds = animeIds.distinct().take(200)

            if (existing.exists()) {
                ref.update(
                    mapOf(
                        "name" to safeName,
                        "description" to safeDesc,
                        "coverKey" to safeCover,
                        "animeIds" to safeIds,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "id" to docId,
                        "name" to safeName,
                        "description" to safeDesc,
                        "coverKey" to safeCover,
                        "animeIds" to safeIds,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, "pushWatchlist($docId)")
        }
    }

    suspend fun deleteWatchlist(watchlistId: String) {
        val uid = auth?.currentUser?.uid ?: return
        val firestore = db ?: return
        try {
            firestore.collection("users").document(uid).collection("watchlists")
                .document(watchlistId.take(80)).delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, "deleteWatchlist($watchlistId)")
        }
    }

    private fun handleFirestoreError(e: Exception, contextLabel: String) {
        if (e is FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    Log.e(TAG, "Permission denied at $contextLabel", e)
                }
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                    Log.w(TAG, "Transient Firestore error at $contextLabel: ${e.code}")
                }
                else -> {
                    Log.e(TAG, "Firestore error at $contextLabel: ${e.code}", e)
                }
            }
        } else {
            Log.e(TAG, "Unexpected Firestore error at $contextLabel", e)
        }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName?.takeIf { it.isNotBlank() } ?: email?.substringBefore('@') ?: "Anime Fan",
        email = email,
        photoUrl = photoUrl?.toString()
    )

    private fun buildHandle(email: String?, fallbackName: String): String {
        val base = email?.substringBefore('@')
            ?: fallbackName.lowercase(Locale.US).replace(Regex("[^a-z0-9_]"), "_")
        return "@${base.ifBlank { "anitrack" }}".take(60)
    }
}
