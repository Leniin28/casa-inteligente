package com.ecore.demo2.feature.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ecore.demo2.core.model.Budget
import com.ecore.demo2.core.ui.components.SectionCard
import com.ecore.demo2.core.ui.components.ValueRow
import com.ecore.demo2.core.ui.format.Format
import com.ecore.demo2.core.ui.format.label

@Composable
fun BudgetItem(
    budget: Budget,
    isEditing: Boolean,
    limitInput: String,
    isSaving: Boolean,
    editError: String?,
    onEdit: () -> Unit,
    onLimitInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier, title = budget.resourceType.label) {
        ValueRow("Límite", Format.usage(budget.limit, budget.resourceType))
        ValueRow("Consumo actual", Format.usage(budget.currentUsage, budget.resourceType))
        ValueRow("Estimado a fin de periodo", Format.usage(budget.estimatedFinalUsage, budget.resourceType))
        ValueRow(
            "Fecha estimada del límite",
            budget.estimatedLimitDate?.let(Format::date) ?: "No se alcanzará",
        )
        LinearProgressIndicator(
            progress = { budget.usedFraction.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${Format.percent(budget.usedFraction)} usado")
        if (budget.isProjectedOverLimit) {
            Text("Al ritmo actual superarás el límite.", color = MaterialTheme.colorScheme.error)
        }

        if (isEditing) {
            OutlinedTextField(
                value = limitInput,
                onValueChange = onLimitInputChange,
                label = { Text("Nuevo límite (${budget.resourceType.unit})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !isSaving) { Text(if (isSaving) "Guardando..." else "Guardar") }
                TextButton(onClick = onCancel) { Text("Cancelar") }
            }
        } else {
            TextButton(onClick = onEdit) { Text("Editar límite") }
        }
    }
}
