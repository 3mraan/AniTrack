package com.example

import android.app.Application
import com.example.data.repository.AniTrackRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AniTrackApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var repository: AniTrackRepository

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.ensureSeeded()
            // Non-blocking background refresh of official AniList artwork & discover feed
            repository.refreshSeededArtworkIfNeeded()
            repository.refreshDiscoverFromAniList()
        }
    }
}
