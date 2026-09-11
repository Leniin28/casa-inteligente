package com.ecore.demo2.core.repository

import com.ecore.demo2.core.datastore.SettingsRepository
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.network.ApiSmartHomeDataSource
import com.ecore.demo2.core.repository.demo.DemoSmartHomeDataSource
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Devuelve la fuente de datos activa (Demo o API) según lo guardado en Ajustes. */
@Singleton
class DataSourceProvider @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val demo: DemoSmartHomeDataSource,
    private val api: ApiSmartHomeDataSource,
) {
    suspend fun current(): SmartHomeDataSource = when (currentType()) {
        DataSourceType.DEMO -> demo
        DataSourceType.API -> api
    }

    suspend fun currentType(): DataSourceType = settingsRepository.settings.first().dataSource
}
