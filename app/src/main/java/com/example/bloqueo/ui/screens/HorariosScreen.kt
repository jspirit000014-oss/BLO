package com.example.bloqueo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(
    repository: AppRepository,
    onBack: () -> Unit
) {
    var inicio by remember { mutableStateOf("08:00") }
    var fin by remember { mutableStateOf("17:00") }
    var dias by remember { mutableStateOf("Lunes a Viernes") }
    var diaCompleto by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(repository.getAppData()) }

    LaunchedEffect(Unit) { data = repository.getAppData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("⏰ Horarios", color = PurpleAccent) },
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text("← Volver", color = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = inicio,
                onValueChange = { inicio = it },
                label = { Text("Hora inicio (HH:MM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentTeal
                )
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = fin,
                onValueChange = { fin = it },
                label = { Text("Hora fin (HH:MM)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentTeal
                )
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = diaCompleto, onCheckedChange = { diaCompleto = it }, colors = CheckboxDefaults.colors(checkedColor = AccentTeal))
                Text("Día completo (bloquear todo el día)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = dias,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Días") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("Lunes a Viernes", "Todos los días", "Fines de semana", "Personalizado").forEach {
                        DropdownMenuItem(
                            text = { Text(it, color = TextPrimary) },
                            onClick = { dias = it; expanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (inicio.isNotBlank() && fin.isNotBlank()) {
                        repository.agregarHorario(inicio, fin, dias, diaCompleto)
                        data = repository.getAppData()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Text("+ Agregar Horario")
            }
            Spacer(Modifier.height(24.dp))
            Text("Horarios configurados:", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            data.horariosBloqueo.forEach { h ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (h.diaCompleto) "Día completo: ${h.dias}" else "⏰ ${h.inicio} - ${h.fin}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(h.dias, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        TextButton(onClick = {
                            repository.eliminarHorario(h)
                            data = repository.getAppData()
                        }) {
                            Text("Eliminar", color = AccentTeal)
                        }
                    }
                }
            }
        }
    }
}
