package com.ecore.demo2.core.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dataSource: DataSourceType = DataSourceType.DEMO,
    val backendUrl: String,
    val notificationsEnabled: Boolean = true,
    val houseName: String = DEFAULT_HOUSE_NAME,
) {
    companion object {
        const val DEFAULT_HOUSE_NAME = "Mi casa"
    }
}
