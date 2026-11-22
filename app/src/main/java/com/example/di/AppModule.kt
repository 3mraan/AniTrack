package com.example.di

import android.content.Context
import com.example.data.local.AniTrackDao
import com.example.data.local.AniTrackDatabase
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.AniListApiService
import com.example.data.remote.AniListRemoteDataSource
import com.example.data.repository.AniTrackRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val ANILIST_BASE_URL = "https://graphql.anilist.co/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideAniListApiService(
        okHttpClient: OkHttpClient,
        json: Json
    ): AniListApiService {
        return Retrofit.Builder()
            .baseUrl(ANILIST_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AniListApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AniTrackDatabase {
        return AniTrackDatabase.getInstance(context)
    }

    @Provides
    fun provideAniTrackDao(database: AniTrackDatabase): AniTrackDao {
        return database.aniTrackDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideAniTrackRepository(
        dao: AniTrackDao,
        settingsDataStore: SettingsDataStore,
        remoteDataSource: AniListRemoteDataSource,
        json: Json,
        firebaseSyncManager: com.example.data.remote.FirebaseSyncManager
    ): AniTrackRepository {
        return AniTrackRepository(
            dao = dao,
            settingsDataStore = settingsDataStore,
            remoteDataSource = remoteDataSource,
            json = json,
            firebaseSyncManager = firebaseSyncManager
        )
    }
}
