package com.example.bloqueo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.data.Horario
import com.example.bloqueo.data.PerfilConfig
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.UsageStatsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilDetailScreen(
    repository: AppRepository,
    context: Context,
    profileId: String,
    onBack: () -> Unit
) {
    var perfil by remember { mutableStateOf<PerfilConfig?>(repository.getPerfil(profileId)) }
    var showRename by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddSchedule by remember { mutableStateOf(false) }
    var showSelectApp by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(perfil?.nombre ?: "") }

    LaunchedEffect(profileId) { perfil = repository.getPerfil(profileId) }
    LaunchedEffect(perfil) { newName = perfil?.nombre ?: "" }

    fun refresh() {
        perfil = repository.getPerfil(profileId)
    }

    val data = repository.getAppData()
    val isActive = repository.isPerfilActivo(profileId)

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text(perfil?.nombre ?: "Perfil", color = PurpleAccent) },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("← Volver", color = TextPrimary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (perfil == null) {
                Text("Perfil no encontrado", color = TextSecondary)
                return@Column
            }
            val p = perfil!!

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isActive) {
                    Button(
                        onClick = {
                            repository.togglePerfilActivo(profileId)
                            refresh()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) {
                        Text("Activar perfil")
                    }
                } else {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Perfil activo", modifier = Modifier.padding(12.dp), color = AccentTeal)
                    }
                    OutlinedButton(
                        onClick = {
                            repository.togglePerfilActivo(profileId)
                            refresh()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedStrict)
                    ) {
                        Text("Desactivar")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showRename = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentTeal)
                ) { Text("Renombrar") }
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedStrict)
                ) { Text("Eliminar") }
                OutlinedButton(
                    onClick = {
                        repository.duplicarPerfil(profileId)
                        onBack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent)
                ) { Text("Duplicar") }
            }
            Spacer(Modifier.height(24.dp))

            Text("Apps bloqueadas", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Button(
                onClick = { showSelectApp = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
            ) { Text("➕ Agregar app") }
            val isStrictMode = data.configuracion.modoActual == "estricto" && data.configuracion.modoEstricto == true
            p.appsBloqueadas.forEach { pkg ->
                val name = (p.nombresAppsBloqueadas ?: emptyMap())[pkg] ?: UsageStatsHelper.getAppDisplayName(context, pkg)
                val bloqueoActivo = pkg !in (p.appsBloqueoDesactivado ?: emptyList())
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectAppDialogIcon(context, pkg, Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (bloqueoActivo) "🔒 $name" else "⏸ $name", color = TextPrimary)
                            Text(
                                if (bloqueoActivo) "Bloqueo activo" else "Bloqueo desactivado",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (!isStrictMode) {
                            TextButton(onClick = {
                                repository.setAppBloqueoActivoEnPerfil(profileId, pkg, !bloqueoActivo)
                                refresh()
                            }) { Text(if (bloqueoActivo) "Desactivar" else "Activar", color = AccentTeal) }
                            TextButton(onClick = {
                                repository.quitarAppDePerfil(profileId, pkg)
                                refresh()
                            }) { Text("Quitar", color = RedStrict) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Text("Horarios", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Button(
                onClick = { showAddSchedule = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) { Text("➕ Agregar horario") }
            p.horarios.forEach { h ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (h.diaCompleto) "Día completo: ${h.dias}" else "${h.inicio} - ${h.fin}",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(h.dias, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            repository.eliminarHorarioDePerfil(profileId, h)
                            refresh()
                        }) { Text("Eliminar", color = AccentTeal) }
                    }
                }
            }
        }
    }

    if (showRename && perfil != null) {
        var name by remember(showRename) { mutableStateOf(perfil!!.nombre) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Renombrar perfil", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        repository.actualizarPerfilNombre(profileId, name.trim())
                        perfil = repository.getPerfil(profileId)
                        showRename = false
                    }
                }) { Text("Guardar", color = AccentTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = CardBackground
        )
    }

    if (showDeleteConfirm && perfil != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar perfil", color = TextPrimary) },
            text = { Text("¿Eliminar \"${perfil!!.nombre}\"?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    repository.eliminarPerfil(profileId)
                    showDeleteConfirm = false
                    onBack()
                }) { Text("Eliminar", color = RedStrict) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = CardBackground
        )
    }

    if (showAddSchedule) {
        var inicio by remember { mutableStateOf("08:00") }
        var fin by remember { mutableStateOf("17:00") }
        var dias by remember { mutableStateOf("Lunes a Viernes") }
        var diaCompleto by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddSchedule = false },
            title = { Text("Nuevo horario", color = TextPrimary) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = diaCompleto, onCheckedChange = { diaCompleto = it }, colors = CheckboxDefaults.colors(checkedColor = AccentTeal))
                        Text("Día completo (bloquear todo el día)", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!diaCompleto) {
                        OutlinedTextField(value = inicio, onValueChange = { inicio = it }, label = { Text("Inicio (HH:MM)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = AccentTeal))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = fin, onValueChange = { fin = it }, label = { Text("Fin (HH:MM)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = AccentTeal))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = dias, onValueChange = { dias = it }, label = { Text("Días") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = AccentTeal))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repository.agregarHorarioEnPerfil(profileId, Horario(inicio = inicio, fin = fin, dias = dias, diaCompleto = diaCompleto))
                    refresh()
                    showAddSchedule = false
                }) { Text("Agregar", color = AccentTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showAddSchedule = false }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = CardBackground
        )
    }

    if (showSelectApp && perfil != null) {
        SelectAppDialog(
            context = context,
            title = "Seleccionar app a bloquear",
            blockedPackages = perfil!!.appsBloqueadas,
            onDismiss = { showSelectApp = false },
            onSelect = { pkg, displayName ->
                repository.agregarAppEnPerfil(profileId, pkg, displayName)
                refresh()
                showSelectApp = false
            }
        )
    }
}
