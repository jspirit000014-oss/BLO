package com.example.bloqueo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.PermissionHelper
import com.example.bloqueo.util.UsageStatsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloqueoScreen(
    repository: AppRepository,
    context: Context,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf(repository.getAppData()) }
    var showSelectAppDialog by remember { mutableStateOf(false) }
    var showUsagePermissionDialog by remember { mutableStateOf(false) }
    var limiteDiario by remember { mutableStateOf("30") }

    LaunchedEffect(Unit) { data = repository.getAppData() }

    fun refresh() {
        data = repository.getAppData()
    }

    val isStrictMode = data.configuracion.modoActual == "estricto" && data.configuracion.modoEstricto == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("🚫 Bloquear Apps", color = PurpleAccent) },
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
            if (isStrictMode) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = RedStrict.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Modo estricto activo. No puedes modificar hasta desactivarlo (código QR en Inicio).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (!PermissionHelper.hasUsageAccess(context)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RedStrict.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permiso de uso necesario", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Para bloquear aplicaciones y ver estadísticas reales, concede acceso de uso.", color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openUsageAccessSettings(context)) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Abrir configuración")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (data.aplicacionesBloqueadas.isNotEmpty() && !PermissionHelper.hasOverlayPermission(context)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleButton.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permiso de superposición necesario", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Para mostrar la pantalla de bloqueo sobre otras apps, permite \"Mostrar sobre otras aplicaciones\".", color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openOverlaySettings(context)) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Abrir configuración")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("Límite diario opcional (minutos, 0 = sin límite)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            OutlinedTextField(
                value = limiteDiario,
                onValueChange = { limiteDiario = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentTeal
                )
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!PermissionHelper.hasUsageAccess(context)) showUsagePermissionDialog = true
                    showSelectAppDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedStrict)
            ) {
                Text("➕ Seleccionar aplicación a bloquear")
            }

            Spacer(Modifier.height(24.dp))
            Text("Apps bloqueadas:", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            for (pkg in data.aplicacionesBloqueadas) {
                val displayName = (data.nombresAppsBloqueadas ?: emptyMap())[pkg] ?: UsageStatsHelper.getAppDisplayName(context, pkg)
                val bloqueoActivo = pkg !in (data.appsBloqueoDesactivado ?: emptyList())
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(context, pkg, Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (bloqueoActivo) "🔒 $displayName" else "⏸ $displayName", color = TextPrimary)
                            Text(
                                if (bloqueoActivo) "Bloqueo activo" else "Bloqueo desactivado",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        if (!isStrictMode) {
                            TextButton(
                                onClick = {
                                    repository.setAppBloqueoActivo(pkg, !bloqueoActivo)
                                    refresh()
                                }
                            ) {
                                Text(if (bloqueoActivo) "Desactivar" else "Activar", color = AccentTeal)
                            }
                            TextButton(
                                onClick = {
                                    repository.quitarAppBloqueada(pkg)
                                    refresh()
                                }
                            ) {
                                Text("Quitar", color = RedStrict)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUsagePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog = false },
            title = { Text("Permiso de uso", color = TextPrimary) },
            text = {
                Text(
                    "Stay Focused necesita acceso de uso para:\n• Detectar qué aplicación estás usando\n• Bloquear las apps que configures\n• Mostrar estadísticas reales de uso\n\nSe abrirá la configuración. Busca esta app y activa \"Permitir acceso de uso\".",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(PermissionHelper.openUsageAccessSettings(context))
                    showUsagePermissionDialog = false
                }) {
                    Text("Abrir configuración", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsagePermissionDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }

    if (showSelectAppDialog) {
        SelectAppDialog(
            context = context,
            title = "Seleccionar app a bloquear",
            blockedPackages = data.aplicacionesBloqueadas,
            onDismiss = { showSelectAppDialog = false },
            onSelect = { pkg, displayName ->
                repository.agregarAppBloqueada(pkg, displayName, limiteDiario.toIntOrNull() ?: 0)
                refresh()
                showSelectAppDialog = false
            }
        )
    }
}

@Composable
private fun AppIcon(context: Context, packageName: String, modifier: Modifier) {
    SelectAppDialogIcon(context, packageName, modifier)
}
