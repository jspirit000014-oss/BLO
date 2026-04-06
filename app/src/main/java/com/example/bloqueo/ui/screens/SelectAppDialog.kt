package com.example.bloqueo.ui.screens

import android.content.Context
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.AppUsageInfo
import com.example.bloqueo.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Diálogo compartido para seleccionar una app a bloquear.
 * Se usa tanto en la sección "Bloquear Apps" como en el detalle de cada Perfil,
 * para que la ventana emergente sea la misma en ambos sitios.
 */
@Composable
fun SelectAppDialog(
    context: Context,
    title: String,
    blockedPackages: List<String>,
    onDismiss: () -> Unit,
    onSelect: (packageName: String, displayName: String) -> Unit
) {
    var apps by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        apps = try {
            withContext(Dispatchers.IO) {
                UsageStatsHelper.getAllLauncherApps(context)
            }
        } catch (_: Exception) {
            emptyList()
        }
        loading = false
    }

    val filtered = remember(apps, search) {
        if (search.isBlank()) apps
        else apps.filter {
            it.displayName.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Buscar") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentTeal
                    )
                )
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentTeal)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filtered) { app ->
                            if (app.packageName in blockedPackages) return@items
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(app.packageName, app.displayName) },
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SelectAppDialogIcon(context, app.packageName, Modifier.size(40.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(app.displayName, color = TextPrimary, modifier = Modifier.weight(1f))
                                    if (app.minutesUsed > 0) {
                                        Text("${app.minutesUsed} min", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
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
fun SelectAppDialogIcon(context: Context, packageName: String, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = {
            ImageView(it).apply {
                try {
                    setImageDrawable(context.packageManager.getApplicationIcon(packageName))
                } catch (_: Exception) { }
            }
        },
        update = { view ->
            try {
                view.setImageDrawable(context.packageManager.getApplicationIcon(packageName))
            } catch (_: Exception) { }
        }
    )
}
