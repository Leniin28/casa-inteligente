package com.ecore.demo2.core.repository.demo

import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.model.ElectricalReading
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.model.WaterReading
import java.util.Locale

/** Respuestas simuladas por palabras clave. Sustituye a la IA local mientras no exista. */
object DemoAssistant {

    fun reply(
        question: String,
        electricity: ElectricalReading,
        water: WaterReading,
        budgets: List<Budget>,
    ): String {
        val q = question.lowercase(Locale.ROOT)
        return when {
            q.containsAny("hola", "buenas") ->
                "¡Hola! Puedo contarte cómo va tu consumo de electricidad, de agua o tus presupuestos."

            q.containsAny("agua", "fuga", "grifo", "litro") ->
                "Ahora mismo el caudal es de ${water.flowLitersPerMinute.fmt(2)} L/min y hoy llevas " +
                    "${water.litersToday.fmt(1)} L. " +
                    if (water.flowLitersPerMinute > 0) "Hay un consumo de agua activo en este momento."
                    else "No hay consumo de agua en este momento."

            q.containsAny("presupuesto", "límite", "limite", "mes") -> budgets.joinToString(" ") { budget ->
                val name = if (budget.resourceType == ResourceType.ELECTRICITY) "electricidad" else "agua"
                val unit = budget.resourceType.unit
                val projection = if (budget.isProjectedOverLimit) "superarás el límite" else "te mantendrás por debajo"
                "En $name llevas ${budget.currentUsage.fmt(2)} de ${budget.limit.fmt(2)} $unit; " +
                    "al ritmo actual $projection (${budget.estimatedFinalUsage.fmt(2)} $unit)."
            }

            q.containsAny("ahorr", "consejo", "reduc") ->
                "Consejos: apaga el ventilador cuando no haya nadie, evita dejar cargadores conectados " +
                    "de noche y revisa si hay flujo de agua cuando todo debería estar cerrado."

            q.containsAny("luz", "electric", "energ", "potencia", "consum", "watt", "vatio") ->
                "Ahora mismo la casa consume ${electricity.power.fmt(1)} W a ${electricity.voltage.fmt(2)} V. " +
                    "Hoy llevas ${electricity.energyTodayKwh.fmt(3)} kWh."

            else ->
                "Soy un asistente de demostración. Pregúntame por electricidad, agua, presupuestos " +
                    "o consejos de ahorro."
        }
    }

    private fun String.containsAny(vararg words: String) = words.any { it in this }

    private fun Double.fmt(decimals: Int) = String.format(Locale.ROOT, "%.${decimals}f", this)
}
