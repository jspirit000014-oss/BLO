package com.example.bloqueo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*
import com.example.bloqueo.util.PermissionHelper
import com.example.bloqueo.util.UsageStatsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadisticasScreen(
    repository: AppRepository,
    context: Context,
    onBack: () -> Unit
) {
    var data by remember { mutableStateOf(repository.getAppData()) }
    val hasUsageAccess = PermissionHelper.hasUsageAccess(context)
    val stats = remember(hasUsageAccess, data) {
        if (hasUsageAccess) {
            UsageStatsHelper.getTodayUsageByPackage(context).toList()
                .map { (pkg, mins) ->
                    val name = (data.nombresAppsBloqueadas ?: emptyMap())[pkg] ?: UsageStatsHelper.getAppDisplayName(context, pkg)
                    name to mins
                }
                .sortedByDescending { it.second }
        } else {
            repository.obtenerEstadisticasHoy().map { (k, v) -> k to v.toLong() }.sortedByDescending { it.second }
        }
    }

    LaunchedEffect(Unit) { data = repository.getAppData() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        TopAppBar(
            title = { Text("📊 Estadísticas", color = PurpleAccent) },
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
            if (!hasUsageAccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleButton.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Permiso de uso", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("Para ver estadísticas reales de uso, concede \"Acceso de uso\" en configuración.", color = TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { context.startActivity(com.example.bloqueo.util.PermissionHelper.openUsageAccessSettings(context)) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                        ) {
                            Text("Abrir configuración")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (stats.isNotEmpty()) {
                val total = stats.sumOf { it.second }.toInt()
                val objetivo = data.perfil.objetivoDiario
                val pct = if (objetivo > 0) (total.toFloat() / objetivo).coerceIn(0f, 1f) else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PurpleButton),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tiempo Total Hoy", color = TextPrimary)
                        Text("$total minutos", style = MaterialTheme.typography.headlineMedium, color = AccentTeal)
                        LinearProgressIndicator(
                            progress = pct,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = AccentTeal,
                            trackColor = CardBackground
                        )
                        Text("Objetivo: $objetivo min (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Apps más usadas hoy:", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                stats.forEach { (app, mins) ->
                    val pkg = (data.nombresAppsBloqueadas ?: emptyMap()).entries.find { it.value == app }?.key
                        ?: data.aplicacionesBloqueadas.find { UsageStatsHelper.getAppDisplayName(context, it) == app }
                    val limite = if (pkg != null) data.limitesDiarios[pkg] else null
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(app, style = MaterialTheme.typography.titleSmall, color = PurpleAccent)
                            Text("Tiempo: ${mins.toInt()} minutos", color = TextPrimary)
                            if (limite != null && limite > 0) {
                                LinearProgressIndicator(
                                    progress = (mins.toFloat() / limite).coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = AccentTeal,
                                    trackColor = CardBackground
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "No hay estadísticas disponibles. Concede permiso de uso para ver el tiempo real en cada app.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
    }
}
