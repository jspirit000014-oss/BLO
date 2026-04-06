package com.example.bloqueo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.PermissionHelper
import com.example.bloqueo.util.UsageStatsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    repository: AppRepository,
    context: Context,
    onNavigateBloqueo: () -> Unit,
    onNavigateHorarios: () -> Unit,
    onNavigateEstadisticas: () -> Unit,
    onNavigateConfiguracion: () -> Unit,
    onNavigateModoEstricto: () -> Unit = {},
    onNavigateDesactivarModoEstricto: () -> Unit = {},
    onNavigatePerfil: (String) -> Unit = {},
    onNavigateBlockerX: () -> Unit = {}
) {
    var data by remember { mutableStateOf(repository.getAppData()) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isStrictMode = data.configuracion.modoActual == "estricto" && data.configuracion.modoEstricto == true
    val stats = remember(data) {
        if (PermissionHelper.hasUsageAccess(context)) {
            UsageStatsHelper.getTodayUsageByPackage(context).toList()
                .map { (pkg, mins) ->
                    Triple(
                        (data.nombresAppsBloqueadas ?: emptyMap())[pkg] ?: UsageStatsHelper.getAppDisplayName(context, pkg),
                        pkg,
                        mins
                    )
                }
                .sortedByDescending { it.third }
                .take(5)
        } else {
            repository.obtenerEstadisticasHoy().map { (app, mins) -> Triple(app, "", mins.toLong()) }.take(5)
        }
    }

    LaunchedEffect(Unit) {
        data = repository.getAppData()
    }

    fun refreshAndUpdateService() {
        data = repository.getAppData()
    }

    val hasBlockedApps = repository.hasBlockedApps()
    val needsAccessibility = hasBlockedApps && !PermissionHelper.hasAccessibilityService(context)
    val needsUsage = hasBlockedApps && !PermissionHelper.hasUsageAccess(context)
    val needsOverlay = hasBlockedApps && !PermissionHelper.hasOverlayPermission(context)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
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
                    "Modo estricto activo. No puedes modificar la configuración hasta desactivarlo (código QR).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        if (needsAccessibility || needsUsage || needsOverlay) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = RedStrict.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Permisos necesarios para bloquear apps", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Abre la configuración de la app en un toque para activar todo.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Button(
                        onClick = { context.startActivity(PermissionHelper.openAppDetailSettings(context)) },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Abrir configuración de la app (acceso directo)") }
                    Spacer(Modifier.height(8.dp))
                    if (needsAccessibility) {
                        Text("• Servicio de accesibilidad (recomendado): detecta apps en tiempo real.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openAccessibilitySettings(context)) },
                            modifier = Modifier.padding(top = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Activar servicio de accesibilidad") }
                    }
                    if (needsUsage) {
                        Text("• Acceso de uso: para detectar qué app estás usando.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openUsageAccessSettings(context)) },
                            modifier = Modifier.padding(top = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Conceder acceso de uso") }
                    }
                    if (needsOverlay) {
                        Spacer(Modifier.height(if (needsUsage) 12.dp else 0.dp))
                        Text("• Mostrar sobre otras apps: para mostrar la pantalla de bloqueo.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openOverlaySettings(context)) },
                            modifier = Modifier.padding(top = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Permitir mostrar sobre otras apps") }
                    }
                }
            }
        }
        Text(
            "Tómate Un Descanso",
            style = MaterialTheme.typography.headlineMedium,
            color = AccentTeal
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tómate un descanso de tu teléfono y concéntrate en lo que realmente importa.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(24.dp))

        Text("Nivel de estrictitud", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                ModoItem("Modo normal", "Puedes cambiar la configuración libremente.",
                    data.configuracion.modoActual == "normal") {
                    if (isStrictMode) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "No puedes cambiar el modo. Desactiva el modo estricto primero (escanea el código QR).",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        repository.actualizarConfiguracion { it.copy(modoActual = "normal") }
                        refreshAndUpdateService()
                    }
                }
                Spacer(Modifier.height(12.dp))
                ModoItem("Modo de bloqueo", "Contraseña para bloquear la configuración.",
                    data.configuracion.modoActual == "bloqueo") {
                    if (isStrictMode) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "No puedes cambiar el modo. Desactiva el modo estricto primero (escanea el código QR).",
                                duration = SnackbarDuration.Short
                            )
                        }
                    } else {
                        repository.actualizarConfiguracion { it.copy(modoActual = "bloqueo") }
                        refreshAndUpdateService()
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (isStrictMode) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Activo", color = AccentTeal, style = MaterialTheme.typography.labelSmall)
                            Text("Modo estricto", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            Text("Escanea el código QR para volver al modo normal.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Button(
                            onClick = onNavigateDesactivarModoEstricto,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Desactivar", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    ModoItem("Modo estricto", "Evita cambios y desinstalación.",
                        data.configuracion.modoActual == "estricto") {
                        onNavigateModoEstricto()
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("Ajustes Rápidos", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        AjusteContador("Aplicaciones Bloqueadas", run {
            val global = (data.aplicacionesBloqueadas - (data.appsBloqueoDesactivado ?: emptyList())).toSet()
            val activeIds = if (data.perfilActivoIds.isNotEmpty()) data.perfilActivoIds else data.perfilActivoId?.let { listOf(it) } ?: emptyList()
            val profileIdsToUse = if (activeIds.isNotEmpty()) {
                activeIds
            } else {
                data.perfiles.mapNotNull { it.id }
            }
            val profiles = profileIdsToUse
                .mapNotNull { pid -> data.perfiles.find { it.id == pid } }
                .filter { it.horarios.isNotEmpty() }
                .flatMap { p -> p.appsBloqueadas - (p.appsBloqueoDesactivado ?: emptyList()) }
                .toSet()
            (global + profiles).size
        })
        var showSitiosDialog by remember { mutableStateOf(false) }
        AjusteContador(
            "Sitios Bloqueados",
            (data.sitiosBloqueados ?: emptyList()).size,
            onClick = { showSitiosDialog = true }
        )
        if (showSitiosDialog) {
            SitiosBloqueadosDialog(
                sitios = data.sitiosBloqueados ?: emptyList(),
                onDismiss = { showSitiosDialog = false },
                onAdd = { url -> repository.agregarSitioBloqueado(url); data = repository.getAppData() },
                onRemove = { url -> repository.quitarSitioBloqueado(url); data = repository.getAppData() }
            )
        }
        Spacer(Modifier.height(8.dp))
        // Acceso visible al bloqueo de contenido adulto (BlockerX) dentro de Ajustes Rápidos
        AccionRapida("🛡️ Bloqueo contenido adulto (BlockerX)", onNavigateBlockerX)
        Spacer(Modifier.height(24.dp))

        Text("Perfiles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Button(
            onClick = { showProfileDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
        ) {
            Text("AGREGAR UN NUEVO PERFIL")
        }
        data.perfiles.forEach { pr ->
            val pid = pr.id ?: return@forEach
            val isActive = pid in (if (data.perfilActivoIds.isNotEmpty()) data.perfilActivoIds else data.perfilActivoId?.let { listOf(it) } ?: emptyList())
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onNavigatePerfil(pid) },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pr.nombre, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("${pr.appsBloqueadas.size} apps · ${pr.horarios.size} horarios", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    if (isActive) {
                        Text("Activo", color = AccentTeal, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("⚡ Acceso Rápido", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        AccionRapida("🚫 Bloquear Apps", onNavigateBloqueo)
        AccionRapida("⏰ Crear Horario", onNavigateHorarios)
        AccionRapida("📊 Ver Estadísticas", onNavigateEstadisticas)
        AccionRapida("⚙️ Configuración", onNavigateConfiguracion)
        Spacer(Modifier.height(24.dp))

        Text("📈 Resumen del Día", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        for ((displayName, pkg, mins) in stats) {
            val limite = if (pkg.isNotEmpty()) data.limitesDiarios[pkg] else null
            TarjetaAppItem(displayName, mins.toInt(), limite)
        }
        if (stats.isEmpty()) {
            Text("No hay estadísticas para hoy. Concede permiso de uso en Configuración para ver uso real.", color = TextSecondary)
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Nuevo perfil", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Nombre del perfil") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
            },
                confirmButton = {
                TextButton(onClick = {
                    if (newProfileName.isNotBlank()) {
                        val id = repository.agregarPerfil(newProfileName.trim())
                        data = repository.getAppData()
                        newProfileName = ""
                        showProfileDialog = false
                        onNavigatePerfil(id)
                    }
                }) {
                    Text("Crear", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false; newProfileName = "" }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }
    }
}

@Composable
private fun ModoItem(titulo: String, desc: String, activo: Boolean, onActivar: () -> Unit) {
    Column {
        if (activo) Text("Activo", color = AccentTeal, style = MaterialTheme.typography.labelSmall)
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = if (activo) TextPrimary else TextSecondary)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        if (!activo) {
            Button(onClick = onActivar, colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                modifier = Modifier.height(36.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                Text("Activar", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AjusteContador(titulo: String, valor: Int, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, color = TextPrimary)
            Text("$valor", color = AccentTeal, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun SitiosBloqueadosDialog(
    sitios: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newUrl by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sitios bloqueados", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    label = { Text("URL o dominio (ej. facebook.com)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
                Button(
                    onClick = { onAdd(newUrl.trim()); newUrl = "" },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) {
                    Text("Agregar")
                }
                Spacer(Modifier.height(16.dp))
                Text("Lista:", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                sitios.forEach { url ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(url, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onRemove(url) }) {
                            Text("Quitar", color = AccentTeal)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        },
        containerColor = CardBackground
    )
}

@Composable
private fun AccionRapida(texto: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
    ) {
        Text(texto)
    }
}

@Composable
private fun TarjetaAppItem(app: String, minutos: Int, limite: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(app, style = MaterialTheme.typography.titleSmall, color = PurpleAccent)
            Text("Tiempo: $minutos minutos", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            if (limite != null && limite > 0) {
                LinearProgressIndicator(
                    progress = (minutos.toFloat() / limite).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = AccentTeal,
                    trackColor = CardBackground
                )
            } else {
                Text("Sin límite", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}
