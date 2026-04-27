package com.example.bloqueo.ui.screens

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository

@Composable
fun ContenidoAdultoScreen(repository: AppRepository) {
    val context = LocalContext.current
    var appData by remember { mutableStateOf(repository.getAppData()) }
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var inputPassword by remember { mutableStateOf("") }
    var inputPasswordConfirm by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var nuevoDominio by remember { mutableStateOf("") }

    val pm = context.packageManager
    val browsersInstalados = remember {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://example.com")
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { pkg ->
                try { pm.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                catch (e: Exception) { false }
            }
    }

    if (showSetPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPasswordDialog = false
                inputPassword = ""
                inputPasswordConfirm = ""
                passwordError = ""
            },
            title = { Text("Establecer contrasena") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esta contrasena se pedira para desactivar el bloqueo.")
                    OutlinedTextField(
                        value = inputPassword,
                        onValueChange = { inputPassword = it; passwordError = "" },
                        label = { Text("Contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputPasswordConfirm,
                        onValueChange = { inputPasswordConfirm = it; passwordError = "" },
                        label = { Text("Confirmar contrasena") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    if (passwordError.isNotEmpty()) {
                        Text(text = passwordError, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when {
                        inputPassword.length < 4 -> passwordError = "Minimo 4 caracteres."
                        inputPassword != inputPasswordConfirm -> passwordError = "Las contrasenas no coinciden."
                        else -> {
                            repository.setNivelEstrictitudPorno("bloqueo")
                            repository.setPasswordBloqueoPorno(inputPassword)
                            appData = repository.getAppData()
                            showSetPasswordDialog = false
                            inputPassword = ""
                            inputPasswordConfirm = ""
                            passwordError = ""
                        }
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSetPasswordDialog = false
                    inputPassword = ""
                    inputPasswordConfirm = ""
                    passwordError = ""
                }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NIVEL DE ESTRICTITUD", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val nivel = appData.configuracion.nivelEstrictitudPorno
                    OutlinedCard(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (nivel == "normal") MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo normal", fontWeight = FontWeight.SemiBold)
                                Text("Sin restricciones activas.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (nivel != "normal") {
                                TextButton(onClick = {
                                    repository.setNivelEstrictitudPorno("normal")
                                    appData = repository.getAppData()
                                }) { Text("Activar") }
                            } else {
                                Text("Activo", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedCard(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (nivel == "bloqueo") MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo bloqueo", fontWeight = FontWeight.SemiBold)
                                Text("Requiere contrasena para desactivar.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (nivel != "bloqueo") {
                                TextButton(onClick = { showSetPasswordDialog = true }) { Text("Activar") }
                            } else {
                                Text("Activo", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedCard(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (nivel == "estricto") MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo estricto", fontWeight = FontWeight.SemiBold)
                                Text("Evita cambios. Sin forma de revertir.", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (nivel != "estricto") {
                                TextButton(onClick = {
                                    repository.setNivelEstrictitudPorno("estricto")
                                    appData = repository.getAppData()
                                }) { Text("Activar") }
                            } else {
                                Text("Activo", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NAVEGADORES A BLOQUEAR", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (browsersInstalados.isEmpty()) {
                        Text("No se encontraron navegadores instalados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        browsersInstalados.forEach { pkg ->
                            val checked = appData.configuracion.pornoBrowsersBloqueados.contains(pkg)
                            val appName = try {
                                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                            } catch (e: Exception) { pkg }
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = { isChecked ->
                                    if (isChecked) repository.agregarPornoBrowser(pkg)
                                    else repository.quitarPornoBrowser(pkg)
                                    appData = repository.getAppData()
                                })
                                Text(text = appName, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DOMINIOS A BLOQUEAR", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = nuevoDominio, onValueChange = { nuevoDominio = it },
                            label = { Text("ejemplo.com") }, singleLine = true, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            val dominio = nuevoDominio.trim().lowercase()
                            if (dominio.isNotEmpty()) {
                                repository.agregarPornoDominio(dominio)
                                appData = repository.getAppData()
                                nuevoDominio = ""
                            }
                        }, enabled = nuevoDominio.isNotBlank()) { Text("Agregar") }
                    }
                    val dominios = appData.configuracion.pornoDominios
                    if (dominios.isEmpty()) {
                        Text("No hay dominios bloqueados.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        dominios.forEach { dominio ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = dominio, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    repository.quitarPornoDominio(dominio)
                                    appData = repository.getAppData()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}
