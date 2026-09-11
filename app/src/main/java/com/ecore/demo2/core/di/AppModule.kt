package com.ecore.demo2.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.ecore.demo2.BuildConfig
import com.ecore.demo2.core.database.SmartHomeDatabase
import com.ecore.demo2.core.database.dao.AlertDao
import com.ecore.demo2.core.database.dao.HistoryDao
import com.ecore.demo2.core.database.dao.ReadingDao
import com.ecore.demo2.core.datastore.SessionPreferences
import com.ecore.demo2.core.datastore.SettingsPreferences
import com.ecore.demo2.core.network.AuthInterceptor
import com.ecore.demo2.core.network.NetworkJson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Reloj inyectable: en los tests se usa un Clock fijo para obtener datos demo reproducibles. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SettingsPreferences
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    @SessionPreferences
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartHomeDatabase =
        Room.databaseBuilder(context, SmartHomeDatabase::class.java, SmartHomeDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideReadingDao(db: SmartHomeDatabase): ReadingDao = db.readingDao()

    @Provides
    fun provideHistoryDao(db: SmartHomeDatabase): HistoryDao = db.historyDao()

    @Provides
    fun provideAlertDao(db: SmartHomeDatabase): AlertDao = db.alertDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = NetworkJson

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            // La IA local puede tardar en responder.
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BASIC no registra cabeceras, así el token no aparece en Logcat.
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
                },
            )
            .build()
}
