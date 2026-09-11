package com.ecore.demo2.core.di

import com.ecore.demo2.core.datastore.DataStoreSessionStore
import com.ecore.demo2.core.datastore.DataStoreSettingsRepository
import com.ecore.demo2.core.datastore.SessionStore
import com.ecore.demo2.core.datastore.SettingsRepository
import com.ecore.demo2.core.repository.AssistantRepository
import com.ecore.demo2.core.repository.AuthRepository
import com.ecore.demo2.core.repository.DefaultAssistantRepository
import com.ecore.demo2.core.repository.DefaultAuthRepository
import com.ecore.demo2.core.repository.DefaultSmartHomeRepository
import com.ecore.demo2.core.repository.SmartHomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindSessionStore(impl: DataStoreSessionStore): SessionStore

    @Binds
    abstract fun bindSmartHomeRepository(impl: DefaultSmartHomeRepository): SmartHomeRepository

    @Binds
    abstract fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository

    @Binds
    abstract fun bindAssistantRepository(impl: DefaultAssistantRepository): AssistantRepository
}
