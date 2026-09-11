package com.ecore.demo2.core.repository.demo

import com.ecore.demo2.core.model.Alert
import com.ecore.demo2.core.model.AlertSeverity
import com.ecore.demo2.core.model.AlertType
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.Device
import com.ecore.demo2.core.model.DeviceStatus
import com.ecore.demo2.core.model.DeviceType
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.HistoryRecord
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.WaterReading
import com.ecore.demo2.core.repository.BudgetCalculator
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.pow
import kotlin.math.round

/**
 * Simulación determinista de la maqueta (12 V DC, algunos dispositivos y un caudalímetro).
 *
 * Todo se calcula a partir del instante recibido: el mismo instante devuelve siempre
 * los mismos valores, así que los datos son reproducibles y coherentes entre pantallas
 * (el consumo de hoy es la integral de la potencia, el presupuesto suma el historial, etc.).
 */
class DemoSimulator(private val zone: ZoneId) {

    private data class DemoDevice(
        val id: String,
        val name: String,
        val type: DeviceType,
        val watts: Double,
    )

    private val catalog = listOf(
        DemoDevice("router", "Router + ESP32", DeviceType.ELECTRONICS, 2.5),
        DemoDevice("lights", "Luces LED sala", DeviceType.LIGHTING, 4.8),
        DemoDevice("fan", "Ventilador", DeviceType.FAN, 12.0),
        DemoDevice("charger", "Cargador USB", DeviceType.ELECTRONICS, 7.5),
        DemoDevice("pump", "Bomba de agua", DeviceType.PUMP, 6.0),
    )

    // ---------------------------------------------------------------- Electricidad

    fun electricalReading(at: Instant): ElectricalReading {
        val power = power(at)
        val voltage = voltageFor(power, at)
        return ElectricalReading(
            timestamp = at,
            voltage = voltage.round(2),
            current = (power / voltage).round(3),
            power = power.round(2),
            energyTodayKwh = energyKwh(startOfDay(at), at).round(3),
        )
    }

    fun power(at: Instant): Double {
        val devicesPower = catalog.filter { isOn(it, at) }.sumOf { it.watts }
        val jitter = 1.0 + (noise(at.epochSecond / 60, 7) - 0.5) * 0.06
        return IDLE_WATTS + devicesPower * jitter
    }

    fun energyKwh(from: Instant, to: Instant): Double {
        var wattHours = 0.0
        var t = from
        while (t < to) {
            val next = minOf(t.plusSeconds(ENERGY_STEP_SECONDS), to)
            wattHours += power(t) * Duration.between(t, next).seconds / 3600.0
            t = next
        }
        return wattHours / 1000.0
    }

    private fun voltageFor(power: Double, at: Instant): Double =
        12.2 - power * 0.004 + (noise(at.epochSecond / 60, 11) - 0.5) * 0.04

    // ---------------------------------------------------------------- Agua

    fun waterReading(at: Instant): WaterReading = WaterReading(
        timestamp = at,
        flowLitersPerMinute = waterFlow(at).round(2),
        litersToday = liters(startOfDay(at), at).round(1),
    )

    /** Caudal constante durante bloques de 10 minutos; más probable en horas de uso típico. */
    fun waterFlow(at: Instant): Double {
        val slot = at.epochSecond / WATER_SLOT_SECONDS
        val probability = when (at.atZone(zone).hour) {
            in 6..8 -> 0.35
            in 12..13 -> 0.25
            in 19..21 -> 0.30
            in 0..4 -> 0.02
            else -> 0.08
        }
        return if (noise(slot, 101) < probability) 0.6 + noise(slot, 202) * 1.4 else 0.0
    }

    fun liters(from: Instant, to: Instant): Double {
        var liters = 0.0
        var t = from
        while (t < to) {
            val slotEnd = Instant.ofEpochSecond((t.epochSecond / WATER_SLOT_SECONDS + 1) * WATER_SLOT_SECONDS)
            val next = minOf(slotEnd, to)
            liters += waterFlow(t) * Duration.between(t, next).seconds / 60.0
            t = next
        }
        return liters
    }

    // ---------------------------------------------------------------- Dispositivos

    fun devices(at: Instant): List<Device> = catalog.map { device ->
        val on = isOn(device, at)
        Device(
            id = device.id,
            name = device.name,
            type = device.type,
            estimatedPowerWatts = device.watts,
            status = when {
                device.id == "router" -> DeviceStatus.ACTIVE
                on -> DeviceStatus.PROBABLY_ACTIVE
                else -> DeviceStatus.INACTIVE
            },
            confidence = (0.7 + noise(device.id.hashCode().toLong(), 303) * 0.25).round(2),
        )
    }

    private fun isOn(device: DemoDevice, at: Instant): Boolean {
        val hour = at.atZone(zone).hour
        val n = noise(at.epochSecond / DEVICE_SLOT_SECONDS, device.id.hashCode().toLong())
        return when (device.id) {
            "router" -> true
            "lights" -> (hour in 18..22 || hour in 6..7) && n < 0.9
            "fan" -> hour in 12..17 && n < 0.7
            "charger" -> (hour >= 21 || hour <= 1) && n < 0.8
            "pump" -> waterFlow(at) > 0.0
            else -> false
        }
    }

    // ---------------------------------------------------------------- Historial

    fun history(period: HistoryPeriod, now: Instant): List<HistoryRecord> = when (period) {
        HistoryPeriod.DAY -> {
            val currentHour = now.truncatedTo(ChronoUnit.HOURS)
            (23L downTo 0L).map { hoursAgo ->
                val start = currentHour.minus(hoursAgo, ChronoUnit.HOURS)
                record(start, minOf(start.plus(1, ChronoUnit.HOURS), now))
            }
        }
        HistoryPeriod.WEEK -> dailyRecords(days = 7, now = now)
        HistoryPeriod.MONTH -> dailyRecords(days = 30, now = now)
    }

    /** Muestras de electricidad/agua en los mismos instantes que [history]. */
    fun electricalHistory(period: HistoryPeriod, now: Instant): List<ElectricalReading> =
        history(period, now).map { electricalReading(it.timestamp) }

    fun waterHistory(period: HistoryPeriod, now: Instant): List<WaterReading> =
        history(period, now).map { waterReading(it.timestamp) }

    private fun dailyRecords(days: Long, now: Instant): List<HistoryRecord> {
        val today = now.atZone(zone).toLocalDate()
        return (days - 1 downTo 0L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            val start = date.atStartOfDay(zone).toInstant()
            val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), now)
            record(start, end)
        }
    }

    private fun record(start: Instant, end: Instant): HistoryRecord {
        val kwh = energyKwh(start, end)
        val liters = liters(start, end)
        return HistoryRecord(
            timestamp = start,
            electricityKwh = kwh.round(3),
            waterLiters = liters.round(1),
            estimatedCost = (kwh * ELECTRICITY_PRICE_PER_KWH + liters * WATER_PRICE_PER_LITER).round(2),
        )
    }

    // ---------------------------------------------------------------- Presupuestos

    fun budget(resourceType: ResourceType, limit: Double, now: Instant): Budget {
        val today = now.atZone(zone).toLocalDate()
        val periodStart = today.withDayOfMonth(1)
        val periodEnd = today.withDayOfMonth(today.lengthOfMonth())
        val from = periodStart.atStartOfDay(zone).toInstant()
        val usage = when (resourceType) {
            ResourceType.ELECTRICITY -> energyKwh(from, now).round(3)
            ResourceType.WATER -> liters(from, now).round(1)
        }
        return BudgetCalculator.project(
            id = resourceType.name.lowercase(),
            resourceType = resourceType,
            limit = limit,
            currentUsage = usage,
            periodStart = periodStart,
            periodEnd = periodEnd,
            today = today,
        )
    }

    // ---------------------------------------------------------------- Alertas

    /** Alertas fijas con marcas de tiempo relativas a la hora actual (estables durante una hora). */
    fun alerts(now: Instant, readIds: Set<String>): List<Alert> {
        val hour = now.truncatedTo(ChronoUnit.HOURS)
        fun alert(id: String, type: AlertType, severity: AlertSeverity, title: String, message: String, hoursAgo: Long) =
            Alert(id, type, severity, title, message, hour.minus(hoursAgo, ChronoUnit.HOURS), id in readIds)

        return listOf(
            alert(
                "alert-high-power", AlertType.HIGH_CONSUMPTION, AlertSeverity.WARNING,
                "Consumo elevado", "La potencia superó 25 W durante más de 10 minutos.", 1,
            ),
            alert(
                "alert-water-night", AlertType.WATER_LEAK, AlertSeverity.CRITICAL,
                "Posible fuga de agua", "Se detectó flujo de agua continuo durante la madrugada.", 5,
            ),
            alert(
                "alert-budget-electricity", AlertType.BUDGET, AlertSeverity.WARNING,
                "Presupuesto eléctrico", "Al ritmo actual superarás el límite mensual de electricidad.", 26,
            ),
            alert(
                "alert-device-new", AlertType.DEVICE, AlertSeverity.INFO,
                "Nuevo dispositivo detectado", "Se detectó un patrón compatible con un cargador USB.", 50,
            ),
            alert(
                "alert-system-reconnected", AlertType.SYSTEM, AlertSeverity.INFO,
                "ESP32 reconectado", "El nodo de sensores volvió a enviar datos.", 74,
            ),
        )
    }

    // ---------------------------------------------------------------- Utilidades

    private fun startOfDay(at: Instant): Instant = at.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    /** Pseudoaleatorio determinista en [0, 1) (SplitMix64). */
    private fun noise(a: Long, b: Long): Double {
        var z = a * -7046029254386353131L + b + SEED
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        z = z xor (z ushr 31)
        return (z ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    private fun Double.round(decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return round(this * factor) / factor
    }

    fun today(now: Instant): LocalDate = now.atZone(zone).toLocalDate()

    companion object {
        const val DEFAULT_ELECTRICITY_LIMIT_KWH = 5.0
        const val DEFAULT_WATER_LIMIT_LITERS = 7000.0
        const val ELECTRICITY_PRICE_PER_KWH = 0.15
        const val WATER_PRICE_PER_LITER = 0.002

        private const val SEED = 42L
        private const val IDLE_WATTS = 0.3
        private const val ENERGY_STEP_SECONDS = 300L
        private const val DEVICE_SLOT_SECONDS = 1200L
        private const val WATER_SLOT_SECONDS = 600L
    }
}
