"""Asistente de consumo.

Por ahora responde con reglas por palabras clave (igual que el modo demo de Android).
Para conectar la IA local de la laptop, sustituir `generate_reply` por una llamada al
modelo (p. ej. un servidor local tipo Ollama/llama.cpp) pasándole `context` como
información del sistema. El contrato POST /api/assistant/chat no cambia.
"""

from app.schemas import BudgetOut, ChatMessageIn


def generate_reply(
    message: str,
    history: list[ChatMessageIn],
    electricity: dict,
    water: dict,
    budgets: list[BudgetOut],
) -> str:
    q = message.lower()

    def has(*words: str) -> bool:
        return any(word in q for word in words)

    if has("hola", "buenas"):
        return "¡Hola! Puedo contarte cómo va tu consumo de electricidad, de agua o tus presupuestos."
    if has("agua", "fuga", "grifo", "litro"):
        flow = water["flow_liters_per_minute"]
        state = "Hay un consumo de agua activo en este momento." if flow > 0 else "No hay consumo de agua en este momento."
        return f"Ahora mismo el caudal es de {flow:.2f} L/min y hoy llevas {water['liters_today']:.1f} L. {state}"
    if has("presupuesto", "límite", "limite", "mes"):
        parts = []
        for budget in budgets:
            name = "electricidad" if budget.resource_type == "electricity" else "agua"
            unit = "kWh" if budget.resource_type == "electricity" else "L"
            projection = "superarás el límite" if budget.estimated_final_usage > budget.limit else "te mantendrás por debajo"
            parts.append(
                f"En {name} llevas {budget.current_usage:.2f} de {budget.limit:.2f} {unit}; "
                f"al ritmo actual {projection} ({budget.estimated_final_usage:.2f} {unit})."
            )
        return " ".join(parts)
    if has("ahorr", "consejo", "reduc"):
        return (
            "Consejos: apaga el ventilador cuando no haya nadie, evita dejar cargadores conectados "
            "de noche y revisa si hay flujo de agua cuando todo debería estar cerrado."
        )
    if has("luz", "electric", "energ", "potencia", "consum", "watt", "vatio"):
        return (
            f"Ahora mismo la casa consume {electricity['power']:.1f} W a {electricity['voltage']:.2f} V. "
            f"Hoy llevas {electricity['energy_today_kwh']:.3f} kWh."
        )
    return "Soy un asistente de demostración. Pregúntame por electricidad, agua, presupuestos o consejos de ahorro."
