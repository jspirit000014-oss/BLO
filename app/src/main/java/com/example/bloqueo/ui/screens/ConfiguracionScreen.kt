package com.example.bloqueo.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.PermissionHelper
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    repository: AppRepository,
    context: Context,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf(repository.getAppData()) }
    var showPasswordCheck by remember { mutableStateOf(false) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var passwordToCheck by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPasswordConfirm by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showDisableAdminDialog by remember { mutableStateOf(false) }
    var passwordToDisableAdmin by remember { mutableStateOf("") }
    var disableAdminMessage by remember { mutableStateOf<String?>(null) }
    var showAdjustImageDialog by remember { mutableStateOf(false) }
    var showSetPasswordPornoDialog by remember { mutableStateOf(false) }
    var showImportExportMsg by remember { mutableStateOf<String?>(null) }
    var newPasswordPorno by remember { mutableStateOf("") }
    var newPasswordPornoConfirm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { data = repository.getAppData() }

    fun refresh() {
        data = repository.getAppData()
    }

    fun requirePasswordThen(action: () -> Unit) {
        if (data.configuracion.modoActual == "normal") {
            action()
            refresh()
            return
        }
        if (data.configuracion.passwordModoBloqueo.isEmpty()) {
            action()
            refresh()
            return
        }
        pendingAction = action
        passwordToCheck = ""
        showPasswordCheck = true
    }

    fun performPendingAction() {
        val pass = data.configuracion.passwordModoBloqueo
        if (pass.isEmpty() || passwordToCheck == pass) {
            pendingAction?.invoke()
            refresh()
        }
        pendingAction = null
        showPasswordCheck = false
        passwordToCheck = ""
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (json != null) {
                    val imported = Gson().fromJson(json, com.example.bloqueo.data.AppData::class.java)
                    repository.saveAppData(imported)
                    refresh()
                    showImportExportMsg = "Importación exitosa ✓"
                }
            } catch (e: Exception) {
                showImportExportMsg = "Error al importar: ${e.message}"
            }
        }
    }

    val isStrictMode = data.configuracion.modoActual == "estricto" && data.configuracion.modoEstricto == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("⚙️ Configuración", color = PurpleAccent) },
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
                        "Modo estricto activo. No puedes modificar la configuración hasta desactivarlo (código QR en Inicio).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Text("🔐 PERMISOS NECESARIOS", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            Text("Para que la app funcione correctamente y vea todas tus aplicaciones, concede estos permisos:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Acceso directo", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Abre la configuración de esta app en un solo toque (permisos, mostrar sobre otras apps, etc.).", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Button(
                        onClick = { context.startActivity(PermissionHelper.openAppDetailSettings(context)) },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Abrir configuración de la app") }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (!PermissionHelper.hasUsageAccess(context)) {
                PermissionCard(
                    title = "Acceso de uso",
                    description = "Obligatorio. Permite detectar qué app usas y mostrar estadísticas reales.",
                    granted = false,
                    onGrant = { context.startActivity(PermissionHelper.openUsageAccessSettings(context)) }
                )
            } else {
                PermissionCard(title = "Acceso de uso", description = "Concedido ✓", granted = true, onGrant = {})
            }

            if (!PermissionHelper.hasAccessibilityService(context)) {
                PermissionCard(
                    title = "Servicio de accesibilidad (Recomendado)",
                    description = "Detecta qué app usas en tiempo real. Sin esto, el bloqueo puede no funcionar en algunos dispositivos. Igual que Stay Focused.",
                    granted = false,
                    onGrant = { context.startActivity(PermissionHelper.openAccessibilitySettings(context)) }
                )
            } else {
                PermissionCard(title = "Servicio de accesibilidad", description = "Concedido ✓", granted = true, onGrant = {})
            }

            if (!PermissionHelper.hasOverlayPermission(context)) {
                PermissionCard(
                    title = "Mostrar sobre otras apps",
                    description = "Necesario para mostrar la pantalla de bloqueo cuando abres una app bloqueada.",
                    granted = false,
                    onGrant = { context.startActivity(PermissionHelper.openOverlaySettings(context)) }
                )
            } else {
                PermissionCard(title = "Mostrar sobre otras apps", description = "Concedido ✓", granted = true, onGrant = {})
            }

            if (!PermissionHelper.hasNotificationPermission(context)) {
                PermissionCard(
                    title = "Notificaciones",
                    description = "Para el servicio de bloqueo en segundo plano (Android 13+).",
                    granted = false,
                    onGrant = {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                )
            } else {
                PermissionCard(title = "Notificaciones", description = "Concedido ✓", granted = true, onGrant = {})
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Optimización de batería", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Recomendado. Evita que el sistema cierre el servicio de bloqueo.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Button(
                        onClick = { context.startActivity(PermissionHelper.openBatteryOptimizationSettings(context)) },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("No optimizar batería") }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (!PermissionHelper.hasOverlayPermission(context) && repository.hasBlockedApps()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleButton.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permiso de superposición", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Para bloquear apps necesitas permitir \"Mostrar sobre otras aplicaciones\".", color = TextSecondary)
                        Button(
                            onClick = { context.startActivity(PermissionHelper.openOverlaySettings(context)) },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Abrir configuración")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Text("IMPORTAR / EXPORTAR", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            if (showImportExportMsg != null) {
                Text(showImportExportMsg!!, color = AccentTeal, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Exportar configuración", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        "Guarda todas tus apps bloqueadas, perfiles, horarios y configuración en un archivo .json.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Button(
                        onClick = {
                            try {
                                val json = Gson().toJson(repository.getAppData())
                                val file = File(context.cacheDir, "stayfocused_backup.json")
                                file.writeText(json)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                })
                                showImportExportMsg = "Exportando..."
                            } catch (e: Exception) {
                                showImportExportMsg = "Error al exportar: ${e.message}"
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Exportar") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Importar configuración", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(
                        "Restaura todo desde un archivo .json exportado anteriormente.",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Importar") }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("GENERAL", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            SwitchRow("Stay Focused activo (bloqueo de apps)", data.configuracion.stayFocusedActivo) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(stayFocusedActivo = v) }
                }
            }
            SwitchRow("Permitir Rotación", data.configuracion.rotacionPantalla) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(rotacionPantalla = v) }
                }
            }
            NivelEstrictitudPornoSection(
                nivelActual = data.configuracion.nivelEstrictitudPorno,
                onActivarBloqueo = {
                    requirePasswordThen { showSetPasswordPornoDialog = true }
                },
                onActivarEstricto = {
                    requirePasswordThen {
                        repository.actualizarConfiguracion { it.copy(nivelEstrictitudPorno = "estricto") }
                        refresh()
                    }
                },
                onDesactivar = {
                    requirePasswordThen {
                        repository.actualizarConfiguracion { it.copy(nivelEstrictitudPorno = "normal") }
                        refresh()
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
            Text("PANTALLA DE BLOQUEO", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            SwitchRow("Cita Motivacional", data.configuracion.citaMotivacional) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(citaMotivacional = v) }
                }
            }
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { }
                    // Al elegir imagen, reinicia ajustes para que cubra pantalla por defecto.
                    repository.actualizarConfiguracion {
                        it.copy(
                            imagenOverlayBloqueoUri = uri.toString(),
                            imagenOverlayScale = 1f,
                            imagenOverlayOffsetX = 0f,
                            imagenOverlayOffsetY = 0f
                        )
                    }
                    refresh()
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Imagen personalizada (pantalla de bloqueo)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("Muestra una imagen en la ventana cuando se bloquea una app.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                requirePasswordThen {
                                    imagePickerLauncher.launch(arrayOf("image/*"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Elegir imagen") }
                        if (data.configuracion.imagenOverlayBloqueoUri.isNotBlank()) {
                            OutlinedButton(
                                onClick = { showAdjustImageDialog = true }
                            ) { Text("Ajustar imagen") }
                            OutlinedButton(
                                onClick = {
                                    requirePasswordThen {
                                        repository.actualizarConfiguracion {
                                            it.copy(
                                                imagenOverlayBloqueoUri = "",
                                                imagenOverlayScale = 1f,
                                                imagenOverlayOffsetX = 0f,
                                                imagenOverlayOffsetY = 0f
                                            )
                                        }
                                    }
                                }
                            ) { Text("Quitar imagen") }
                        }
                    }
                    if (data.configuracion.imagenOverlayBloqueoUri.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "La imagen se muestra como fondo y cubre toda la pantalla. Puedes ajustar tamaño y posición en “Ajustar imagen”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            if (data.configuracion.modoActual != "normal") {
                Button(
                    onClick = { showSetPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleButton)
                ) {
                    Text(if (data.configuracion.passwordModoBloqueo.isEmpty()) "Establecer contraseña" else "Cambiar contraseña")
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("POMODORO", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            SwitchRow("Activar Pomodoro", data.configuracion.pomodoroActivo) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(pomodoroActivo = v) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("AVANZADO", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            SwitchRow("Bloquear Navegadores", data.configuracion.bloquearNavegadores) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(bloquearNavegadores = v) }
                }
            }
            SwitchRow("Bloquear Pantalla Dividida", data.configuracion.bloquearPantallaDividida) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(bloquearPantallaDividida = v) }
                }
            }
            SwitchRow("Bloquear Apagado", data.configuracion.bloquearApagado) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(bloquearApagado = v) }
                }
            }
            SwitchRow("Bloquear Apps Recientes", data.configuracion.bloquearAppsRecientes) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(bloquearAppsRecientes = v) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("INTERFAZ", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            SwitchRow("Modo Oscuro", data.configuracion.modoOscuroActivo) { v ->
                requirePasswordThen {
                    repository.actualizarConfiguracion { it.copy(modoOscuroActivo = v) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("PROTECCIÓN CONTRA DESINSTALACIÓN", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            Text("Como Stay Focused: impide desinstalar la app hasta que desactives el administrador con tu contraseña.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            if (!PermissionHelper.hasDeviceAdmin(context)) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Protección desactivada", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("La app se puede desinstalar normalmente. Activa el administrador del dispositivo para impedir la desinstalación hasta que introduzcas tu contraseña en Configuración.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(PermissionHelper.getAddDeviceAdminIntent(context))
                                } catch (_: Exception) {
                                    context.startActivity(PermissionHelper.getDeviceAdminListIntent(context))
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Activar administrador del dispositivo") }
                        TextButton(
                            onClick = { context.startActivity(PermissionHelper.getDeviceAdminListIntent(context)) },
                            modifier = Modifier.padding(top = 4.dp)
                        ) { Text("Si no se abre: ver lista de administradores") }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Protección activa ✓", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("La app no se puede desinstalar hasta que desactives el administrador. Para permitir desinstalar: introduce tu contraseña abajo y desactiva.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        if (disableAdminMessage != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(disableAdminMessage!!, style = MaterialTheme.typography.bodySmall, color = RedStrict)
                        }
                        OutlinedButton(
                            onClick = {
                                passwordToDisableAdmin = ""
                                disableAdminMessage = null
                                showDisableAdminDialog = true
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text("Desactivar protección (permitir desinstalar)") }
                    }
                }
            }
        }
    }

    if (showPasswordCheck) {
        AlertDialog(
            onDismissRequest = { showPasswordCheck = false; pendingAction = null },
            title = { Text("Contraseña", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = passwordToCheck,
                    onValueChange = { passwordToCheck = it },
                    label = { Text("Introduce la contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { performPendingAction() }) {
                    Text("Aceptar", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordCheck = false; pendingAction = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }

    if (showDisableAdminDialog) {
        AlertDialog(
            onDismissRequest = { showDisableAdminDialog = false; passwordToDisableAdmin = ""; disableAdminMessage = null },
            title = { Text("Desactivar protección contra desinstalación", color = TextPrimary) },
            text = {
                Column {
                    Text("Introduce tu contraseña para permitir desinstalar la app.", color = TextSecondary)
                    if (data.configuracion.passwordModoBloqueo.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Primero establece una contraseña en \"Modo bloqueo\" (arriba).", color = RedStrict, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = passwordToDisableAdmin,
                            onValueChange = { passwordToDisableAdmin = it },
                            label = { Text("Contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentTeal
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (data.configuracion.passwordModoBloqueo.isEmpty()) {
                        showDisableAdminDialog = false
                        disableAdminMessage = "Establece una contraseña en Modo bloqueo antes de desactivar la protección."
                        return@TextButton
                    }
                    if (passwordToDisableAdmin != data.configuracion.passwordModoBloqueo) {
                        disableAdminMessage = "Contraseña incorrecta."
                        showDisableAdminDialog = false
                        passwordToDisableAdmin = ""
                        return@TextButton
                    }
                    if (PermissionHelper.removeDeviceAdmin(context)) {
                        showDisableAdminDialog = false
                        passwordToDisableAdmin = ""
                        disableAdminMessage = null
                        refresh()
                    } else {
                        disableAdminMessage = "No se pudo desactivar. Inténtalo de nuevo."
                        showDisableAdminDialog = false
                    }
                }) {
                    Text("Desactivar protección", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableAdminDialog = false; passwordToDisableAdmin = ""; disableAdminMessage = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }

    if (showSetPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showSetPasswordDialog = false; newPassword = ""; newPasswordConfirm = "" },
            title = { Text("Contraseña del modo bloqueo", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTeal
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasswordConfirm,
                        onValueChange = { newPasswordConfirm = it },
                        label = { Text("Repetir contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTeal
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPassword.isNotBlank() && newPassword == newPasswordConfirm) {
                        repository.actualizarConfiguracion { it.copy(passwordModoBloqueo = newPassword) }
                        data = repository.getAppData()
                        showSetPasswordDialog = false
                        newPassword = ""
                        newPasswordConfirm = ""
                    }
                }) {
                    Text("Guardar", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasswordDialog = false; newPassword = ""; newPasswordConfirm = "" }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }

    if (showAdjustImageDialog) {
        val density = LocalDensity.current
        val uriString = data.configuracion.imagenOverlayBloqueoUri

        var tmpScale by remember(showAdjustImageDialog) {
            mutableStateOf(if (data.configuracion.imagenOverlayScale > 0f) data.configuracion.imagenOverlayScale else 1f)
        }
        var tmpOffsetXDp by remember(showAdjustImageDialog) {
            mutableStateOf(with(density) { (data.configuracion.imagenOverlayOffsetX / density.density).coerceIn(-250f, 250f) })
        }
        var tmpOffsetYDp by remember(showAdjustImageDialog) {
            mutableStateOf(with(density) { (data.configuracion.imagenOverlayOffsetY / density.density).coerceIn(-250f, 250f) })
        }

        AlertDialog(
            onDismissRequest = { showAdjustImageDialog = false },
            title = { Text("Ajustar imagen", color = TextPrimary) },
            text = {
                Column {
                    // Vista previa
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = {
                                    ImageView(it).apply {
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                        try { setImageURI(Uri.parse(uriString)) } catch (_: Exception) { }
                                    }
                                },
                                update = { iv ->
                                    try { iv.setImageURI(Uri.parse(uriString)) } catch (_: Exception) { }
                                    iv.scaleX = tmpScale
                                    iv.scaleY = tmpScale
                                    iv.translationX = tmpOffsetXDp * density.density
                                    iv.translationY = tmpOffsetYDp * density.density
                                }
                            )
                            // Capa suave para que se lea en la preview, similar al overlay real
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(BackgroundDark.copy(alpha = 0.25f))
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Text("Tamaño", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tmpScale.coerceIn(1f, 3f),
                        onValueChange = { tmpScale = it.coerceIn(1f, 3f) },
                        valueRange = 1f..3f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Mover horizontal", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tmpOffsetXDp,
                        onValueChange = { tmpOffsetXDp = it.coerceIn(-250f, 250f) },
                        valueRange = -250f..250f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )

                    Spacer(Modifier.height(8.dp))
                    Text("Mover vertical", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = tmpOffsetYDp,
                        onValueChange = { tmpOffsetYDp = it.coerceIn(-250f, 250f) },
                        valueRange = -250f..250f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal)
                    )

                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            tmpScale = 1f
                            tmpOffsetXDp = 0f
                            tmpOffsetYDp = 0f
                        }
                    ) { Text("Restablecer", color = TextSecondary) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    requirePasswordThen {
                        val offsetXPx = tmpOffsetXDp * density.density
                        val offsetYPx = tmpOffsetYDp * density.density
                        repository.actualizarConfiguracion {
                            it.copy(
                                imagenOverlayScale = tmpScale.coerceIn(1f, 3f),
                                imagenOverlayOffsetX = offsetXPx,
                                imagenOverlayOffsetY = offsetYPx
                            )
                        }
                    }
                    refresh()
                    showAdjustImageDialog = false
                }) {
                    Text("Guardar", color = AccentTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustImageDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            containerColor = CardBackground
        )
    }
}


    if (showSetPasswordPornoDialog) {
        AlertDialog(
            onDismissRequest = { showSetPasswordPornoDialog = false; newPasswordPorno = ""; newPasswordPornoConfirm = "" },
            title = { Text("Contraseña bloqueo de contenido", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPasswordPorno,
                        onValueChange = { newPasswordPorno = it },
                        label = { Text("Nueva contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTeal
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasswordPornoConfirm,
                        onValueChange = { newPasswordPornoConfirm = it },
                        label = { Text("Repetir contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentTeal
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPasswordPorno.isNotBlank() && newPasswordPorno == newPasswordPornoConfirm) {
                        repository.actualizarConfiguracion {
                            it.copy(nivelEstrictitudPorno = "bloqueo", passwordBloqueoPorno = newPasswordPorno)
                        }
                        data = repository.getAppData()
                        showSetPasswordPornoDialog = false
                        newPasswordPorno = ""
                        newPasswordPornoConfirm = ""
                        refresh()
                    }
                }) { Text("Guardar", color = AccentTeal) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPasswordPornoDialog = false
                    newPasswordPorno = ""
                    newPasswordPornoConfirm = ""
                }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = CardBackground
        )
    }
@Composable
private fun PermissionCard(title: String, description: String, granted: Boolean, onGrant: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) AccentTeal.copy(alpha = 0.15f) else CardBackground
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            if (!granted) {
                Button(
                    onClick = onGrant,
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) { Text("Conceder permiso") }
            }
        }
    }
}

@Composable
private fun SwitchRow(text: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text, color = TextPrimary)
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentTeal,
                    checkedTrackColor = AccentTeal.copy(alpha = 0.5f)
                )
            )
        }
    }
}


@Composable
private fun NivelEstrictitudPornoSection(
    nivelActual: String,
    onActivarBloqueo: () -> Unit,
    onActivarEstricto: () -> Unit,
    onDesactivar: () -> Unit
) {
    Text(
        "NIVEL DE ESTRICTITUD (CONTENIDO)",
        style = MaterialTheme.typography.labelLarge,
        color = AccentTeal
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Column(Modifier.padding(bottom = 12.dp)) {
                if (nivelActual == "normal") {
                    Text("Activo", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                }
                Text("Modo normal", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    "Puedes cambiar la configuración libremente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (nivelActual != "normal") {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onDesactivar,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                        shape = RoundedCornerShape(50)
                    ) { Text("Activar") }
                }
            }
            HorizontalDivider(color = BackgroundDark.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(bottom = 12.dp)) {
                if (nivelActual == "bloqueo") {
                    Text("Activo", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                }
                Text("Modo de bloqueo", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    "Contraseña para bloquear la configuración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (nivelActual != "bloqueo") {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onActivarBloqueo,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                        shape = RoundedCornerShape(50)
                    ) { Text("Activar") }
                }
            }
            HorizontalDivider(color = BackgroundDark.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
            Column {
                if (nivelActual == "estricto") {
                    Text("Activo", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                }
                Text("Modo estricto", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(
                    "Evita cambios y desinstalación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (nivelActual != "estricto") {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onActivarEstricto,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleButton),
                        shape = RoundedCornerShape(50)
                    ) { Text("Activar") }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}
