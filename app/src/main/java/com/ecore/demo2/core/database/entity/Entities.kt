package com.ecore.demo2.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tablas de caché local. Guardan tipos simples (millis, String) para no necesitar TypeConverters.

@Entity(tableName = "electrical_readings")
data class ElectricalReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val voltage: Double,
    val current: Double,
    val power: Double,
    val energyTodayKwh: Double,
)

@Entity(tableName = "water_readings")
data class WaterReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val flowLitersPerMinute: Double,
    val litersToday: Double,
)

@Entity(tableName = "history_records", primaryKeys = ["period", "timestampMillis"])
data class HistoryRecordEntity(
    val period: String,
    val timestampMillis: Long,
    val electricityKwh: Double,
    val waterLiters: Double,
    val estimatedCost: Double?,
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val message: String,
    val timestampMillis: Long,
    val isRead: Boolean,
)
