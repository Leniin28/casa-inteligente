package com.ecore.demo2.core.database

import com.ecore.demo2.core.database.entity.AlertEntity
import com.ecore.demo2.core.database.entity.ElectricalReadingEntity
import com.ecore.demo2.core.database.entity.HistoryRecordEntity
import com.ecore.demo2.core.database.entity.WaterReadingEntity
import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AlertSeverity
import com.ecore.demo2.core.model.AlertType
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.util.enumValueOrNull
import java.time.Instant

fun ElectricalReading.toEntity() = ElectricalReadingEntity(
    timestampMillis = timestamp.toEpochMilli(),
    voltage = voltage,
    current = current,
    power = power,
    energyTodayKwh = energyTodayKwh,
)

fun ElectricalReadingEntity.toDomain() = ElectricalReading(
    timestamp = Instant.ofEpochMilli(timestampMillis),
    voltage = voltage,
    current = current,
    power = power,
    energyTodayKwh = energyTodayKwh,
)

fun WaterReading.toEntity() = WaterReadingEntity(
    timestampMillis = timestamp.toEpochMilli(),
    flowLitersPerMinute = flowLitersPerMinute,
    litersToday = litersToday,
)

fun WaterReadingEntity.toDomain() = WaterReading(
    timestamp = Instant.ofEpochMilli(timestampMillis),
    flowLitersPerMinute = flowLitersPerMinute,
    litersToday = litersToday,
)

fun HistoryRecord.toEntity(period: HistoryPeriod) = HistoryRecordEntity(
    period = period.name,
    timestampMillis = timestamp.toEpochMilli(),
    electricityKwh = electricityKwh,
    waterLiters = waterLiters,
    estimatedCost = estimatedCost,
)

fun HistoryRecordEntity.toDomain() = HistoryRecord(
    timestamp = Instant.ofEpochMilli(timestampMillis),
    electricityKwh = electricityKwh,
    waterLiters = waterLiters,
    estimatedCost = estimatedCost,
)

fun Alert.toEntity() = AlertEntity(
    id = id,
    type = type.name,
    severity = severity.name,
    title = title,
    message = message,
    timestampMillis = timestamp.toEpochMilli(),
    isRead = read,
)

fun AlertEntity.toDomain() = Alert(
    id = id,
    type = enumValueOrNull<AlertType>(type) ?: AlertType.OTHER,
    severity = enumValueOrNull<AlertSeverity>(severity) ?: AlertSeverity.INFO,
    title = title,
    message = message,
    timestamp = Instant.ofEpochMilli(timestampMillis),
    read = isRead,
)
