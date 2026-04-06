package com.example.bloqueo.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import com.example.bloqueo.util.QrHelper
import java.io.File
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivarModoEstrictoScreen(
    repository: AppRepository,
    context: Context,
    onBack: () -> Unit,
    onActivated: () -> Unit
) {
    var data by remember { mutableStateOf(repository.getAppData()) }
    var expanded1 by remember { mutableStateOf(true) }
    var expanded2 by remember { mutableStateOf(false) }
    var expanded3 by remember { mutableStateOf(false) }
    var expanded4 by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { data = repository.getAppData() }
    LaunchedEffect(showQrDialog) {
        if (showQrDialog && (data.configuracion.estrictoQrToken.isNullOrBlank())) {
            val token = QrHelper.generateToken()
            repository.actualizarConfiguracion { it.copy(estrictoQrToken = token) }
            data = repository.getAppData()
        }
    }

    fun refresh() {
        data = repository.getAppData()
    }

    fun updateConfig(block: (com.example.bloqueo.data.Configuracion) -> com.example.bloqueo.data.Configuracion) {
        repository.actualizarConfiguracion(block)
        refresh()
    }

    val config = data.configuracion
    val restriccion = config.estrictoRestriccion ?: "todo"
    val bloquearDesinstalacion = config.estrictoBloquearDesinstalacion != false
    val bloquearConfiguracion = config.estrictoBloquearConfiguracion != false
    val metodoDesactivacion = config.estrictoMetodoDesactivacion ?: "expiracion"
    val canActivate = PermissionHelper.hasDeviceAdmin(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("Activar modo estricto", color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("✕", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
                }
            },
            actions = {
                Text("⚡", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 8.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 1. Edición de restricciones
            EstrictoSection(
                number = 1,
                title = "Edición de restricciones",
                subtitle = "Evita cambios en tus restricciones.",
                expanded = expanded1,
                onToggle = { expanded1 = !expanded1 }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    EstrictoRadio(
                        title = "Restringir todo",
                        desc = "No se permitirá editar, eliminar ni suavizar ninguna restricción.",
                        selected = restriccion == "todo"
                    ) {
                        updateConfig { it.copy(estrictoRestriccion = "todo") }
                    }
                    Spacer(Modifier.height(12.dp))
                    EstrictoRadio(
                        title = "Restringir específicas",
                        desc = "Elige qué restricciones no se pueden editar, eliminar ni suavizar.",
                        selected = restriccion == "especificas"
                    ) {
                        updateConfig { it.copy(estrictoRestriccion = "especificas") }
                    }
                    Button(
                        onClick = { expanded1 = false; expanded2 = true },
                        modifier = Modifier.padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Siguiente") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 2. Restricciones adicionales
            EstrictoSection(
                number = 2,
                title = "Restricciones adicionales",
                subtitle = "Añade protecciones adicionales para mantener el control.",
                expanded = expanded2,
                onToggle = { expanded2 = !expanded2 }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    EstrictoToggle(
                        title = "Desinstalación de aplicaciones",
                        desc = "No podrás desinstalar ninguna aplicación, incluyendo Stay Focused.",
                        checked = bloquearDesinstalacion
                    ) { nuevo ->
                        updateConfig { c -> c.copy(estrictoBloquearDesinstalacion = nuevo) }
                    }
                    Spacer(Modifier.height(12.dp))
                    EstrictoToggle(
                        title = "Configuración del teléfono",
                        desc = "No podrás acceder a la configuración del teléfono, lo que ayuda a evitar la evasión de Stay Focused.",
                        checked = bloquearConfiguracion
                    ) { nuevo ->
                        updateConfig { c -> c.copy(estrictoBloquearConfiguracion = nuevo) }
                    }
                    Button(
                        onClick = { expanded2 = false; expanded3 = true },
                        modifier = Modifier.padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Siguiente") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3. Método de desactivación
            EstrictoSection(
                number = 3,
                title = "Método de desactivación",
                subtitle = "Elige cómo se puede desactivar el modo estricto.",
                expanded = expanded3,
                onToggle = { expanded3 = !expanded3 }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    EstrictoOptionRow(
                        title = "Tiempo de expiración",
                        desc = "El modo estricto se desactivará automáticamente en el tiempo establecido.",
                        selected = metodoDesactivacion == "expiracion"
                    ) { updateConfig { it.copy(estrictoMetodoDesactivacion = "expiracion") } }
                    EstrictoOptionRow(
                        title = "Código QR",
                        desc = "Escanea el código QR para desbloquear el modo estricto.",
                        selected = metodoDesactivacion == "qr"
                    ) {
                        updateConfig { it.copy(estrictoMetodoDesactivacion = "qr") }
                        showQrDialog = true
                    }
                    EstrictoOptionRow(
                        title = "Programado",
                        desc = "El modo estricto se activará en el horario seleccionado.",
                        selected = metodoDesactivacion == "programado"
                    ) { updateConfig { it.copy(estrictoMetodoDesactivacion = "programado") } }
                    EstrictoOptionRow(
                        title = "Texto aleatorio",
                        desc = "Introduce el texto generado aleatoriamente para desactivar el modo estricto.",
                        selected = metodoDesactivacion == "texto_aleatorio"
                    ) { updateConfig { it.copy(estrictoMetodoDesactivacion = "texto_aleatorio") } }
                    Button(
                        onClick = { expanded3 = false; expanded4 = true },
                        modifier = Modifier.padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                    ) { Text("Siguiente") }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4. Activar administrador del dispositivo
            EstrictoSection(
                number = 4,
                title = "Activar administrador del dispositivo",
                subtitle = "Concede a Stay Focused el permiso necesario para aplicar restricciones.",
                expanded = expanded4,
                onToggle = { expanded4 = !expanded4 }
            ) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    if (PermissionHelper.hasDeviceAdmin(context)) {
                        Text("Administrador activo ✓", color = AccentTeal, style = MaterialTheme.typography.titleSmall)
                    } else {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(PermissionHelper.getAddDeviceAdminIntent(context))
                                } catch (_: Exception) {
                                    context.startActivity(PermissionHelper.getDeviceAdminListIntent(context))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) { Text("Activar administrador del dispositivo") }
                        TextButton(onClick = { context.startActivity(PermissionHelper.getDeviceAdminListIntent(context)) }) {
                            Text("Ver lista de administradores", color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    repository.actualizarConfiguracion { cfg ->
                        val withToken = if ((cfg.estrictoMetodoDesactivacion ?: "") == "qr" && cfg.estrictoQrToken.isNullOrBlank())
                            cfg.copy(estrictoQrToken = QrHelper.generateToken()) else cfg
                        withToken.copy(modoActual = "estricto", modoEstricto = true)
                    }
                    refresh()
                    onActivated()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canActivate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    disabledContainerColor = CardBackground
                )
            ) {
                Text(if (canActivate) "Activar" else "Activa el administrador del dispositivo primero")
            }
            if (metodoDesactivacion == "qr") {
                OutlinedButton(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("Configurar código QR") }
            }
        }
    }

    if (showQrDialog) {
        CodigoQrDialog(
            token = data.configuracion.estrictoQrToken ?: "",
            onGenerarNuevo = {
                val token = QrHelper.generateToken()
                repository.actualizarConfiguracion { it.copy(estrictoQrToken = token) }
                data = repository.getAppData()
            },
            onImprimirCompartir = {
                val token = data.configuracion.estrictoQrToken ?: return@CodigoQrDialog
                val bitmap = QrHelper.encodeToQrBitmap(token, 512)
                if (bitmap != null) {
                    try {
                        val file = File(context.cacheDir, "qr_estricto.png")
                        file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        context.startActivity(Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        })
                    } catch (_: Exception) { }
                }
            },
            onHecho = { showQrDialog = false }
        )
    }
}

@Composable
private fun CodigoQrDialog(
    token: String,
    onGenerarNuevo: () -> Unit,
    onImprimirCompartir: () -> Unit,
    onHecho: () -> Unit
) {
    val bitmap = remember(token) { QrHelper.encodeToQrBitmap(token, 400) }
    AlertDialog(
        onDismissRequest = onHecho,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Código QR", color = TextPrimary)
                TextButton(onClick = onHecho) { Text("✕", color = TextPrimary) }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Código QR",
                        modifier = Modifier.size(280.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        Text("Generando...", color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onGenerarNuevo,
                        modifier = Modifier.weight(1f)
                    ) { Text("GENERAR NUEVO") }
                    OutlinedButton(
                        onClick = onImprimirCompartir,
                        modifier = Modifier.weight(1f)
                    ) { Text("IMPRIMIR O COMPARTIR") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onHecho,
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) { Text("HECHO") }
        },
        containerColor = CardBackground
    )
}

@Composable
private fun EstrictoSection(
    number: Int,
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$number.", color = AccentTeal, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = if (expanded) "▼" else "▶",
                    color = TextSecondary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                content()
            }
        }
    }
}

@Composable
private fun EstrictoRadio(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AccentTeal)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EstrictoToggle(
    title: String,
    desc: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentTeal, checkedTrackColor = AccentTeal.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun EstrictoOptionRow(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "▶",
            color = if (selected) AccentTeal else TextSecondary,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
