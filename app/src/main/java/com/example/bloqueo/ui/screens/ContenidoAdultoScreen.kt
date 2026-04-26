package com.example.bloqueo.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blockerx.complete.BlockerXApp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContenidoAdultoScreen(
    repository: AppRepository,
    context: Context,
    onBack: () -> Unit
) {
    var appData by remember { mutableStateOf(repository.getAppData()) }
    var nuevoDominioPorno by remember { mutableStateOf("") }
    var browsersInstalados by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        appData = repository.getAppData()
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com"))
        val pm = context.packageManager
        val resolvers = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
        else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)
        browsersInstalados = resolvers.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            val nombre = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            pkg to nombre
        }.distinctBy { it.first }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloquear Contenido Adulto", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atras", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            BlockerXApp()
            Spacer(Modifier.height(24.dp))
            Divider(color = CardBackground)
            Spacer(Modifier.height(24.dp))
            Text("NAVEGADORES A BLOQUEAR", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            if (browsersInstalados.isEmpty()) {
                Text("No se encontraron navegadores instalados.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                browsersInstalados.forEach { (packageName, nombre) ->
                    val bloqueado = appData.configuracion.pornoBrowsersBloqueados.contains(packageName)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (bloqueado) repository.quitarPornoBrowser(packageName)
                            else repository.agregarPornoBrowser(packageName)
                            appData = repository.getAppData()
                        }.padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = bloqueado,
                            onCheckedChange = { checked ->
                                if (checked) repository.agregarPornoBrowser(packageName)
                                else repository.quitarPornoBrowser(packageName)
                                appData = repository.getAppData()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(nombre, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("DOMINIOS A BLOQUEAR", style = MaterialTheme.typography.labelLarge, color = AccentTeal)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nuevoDominioPorno,
                    onValueChange = { nuevoDominioPorno = it },
                    placeholder = { Text("ej: pornhub.com") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val dominio = nuevoDominioPorno.trim()
                        if (dominio.isNotEmpty()) {
                            repository.agregarPornoDominio(dominio)
                            nuevoDominioPorno = ""
                            appData = repository.getAppData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                ) { Text("Agregar") }
            }
            Spacer(Modifier.height(8.dp))
            if (appData.configuracion.pornoDominios.isEmpty()) {
                Text("Sin dominios agregados.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            } else {
                appData.configuracion.pornoDominios.forEach { dominio ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(dominio, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            repository.quitarPornoDominio(dominio)
                            appData = repository.getAppData()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
