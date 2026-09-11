package com.ecore.demo2.core.ui.format

import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.ResourceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Formato de valores para mostrar en pantalla. Cambiar aquí unidades/decimales afecta a toda la app. */
object Format {

    fun number(value: Double, decimals: Int): String =
        String.format(Locale.getDefault(), "%.${decimals}f", value)

    fun watts(value: Double) = "${number(value, 1)} W"
    fun volts(value: Double) = "${number(value, 2)} V"
    fun amps(value: Double) = "${number(value, 2)} A"
    fun kwh(value: Double) = "${number(value, 3)} kWh"
    fun liters(value: Double) = "${number(value, 1)} L"
    fun flow(value: Double) = "${number(value, 2)} L/min"
    fun cost(value: Double) = "$ ${number(value, 2)}"
    fun percent(fraction: Double) = "${(fraction * 100).roundToInt()} %"

    fun usage(value: Double, resourceType: ResourceType) = when (resourceType) {
        ResourceType.ELECTRICITY -> kwh(value)
        ResourceType.WATER -> liters(value)
    }

    fun dateTime(instant: Instant): String = DATE_TIME.format(instant.atZone(ZoneId.systemDefault()))
    fun time(instant: Instant): String = TIME.format(instant.atZone(ZoneId.systemDefault()))
    fun date(date: LocalDate): String = DATE.format(date)

    /** Etiqueta de un registro del historial: hora para "Día", fecha para "Semana"/"Mes". */
    fun historyLabel(instant: Instant, period: HistoryPeriod): String = when (period) {
        HistoryPeriod.DAY -> time(instant)
        HistoryPeriod.WEEK, HistoryPeriod.MONTH -> DAY_OF_WEEK.format(instant.atZone(ZoneId.systemDefault()))
    }

    private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
    private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val DAY_OF_WEEK = DateTimeFormatter.ofPattern("EEE dd/MM")
}
