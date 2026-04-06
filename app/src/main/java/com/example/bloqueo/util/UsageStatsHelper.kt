package com.example.bloqueo.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.concurrent.TimeUnit

data class AppUsageInfo(
    val packageName: String,
    val displayName: String,
    val minutesUsed: Long,
    val lastUsed: Long
)

/**
 * Obtiene lista de apps instaladas y tiempo de uso (requiere permiso de uso).
 */
object UsageStatsHelper {

    private const val OUR_PACKAGE = "com.example.bloqueo"

    /**
     * Todas las apps instaladas en el dispositivo.
     * En Android 11+ con QUERY_ALL_PACKAGES: lista completa de todas las apps.
     * Ejecutar en segundo plano (Dispatchers.IO).
     */
    fun getAllLauncherApps(context: Context): List<AppUsageInfo> {
        return try {
            val pm = context.packageManager
            val usageMap = getUsageStatsMap(context) ?: emptyMap()

            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getAllInstalledPackagesApi30(pm)
            } else {
                getLauncherPackagesLegacy(pm)
            }

            packages
                .filter { it != OUR_PACKAGE }
                .mapNotNull { pkg ->
                    try {
                        val ai = pm.getApplicationInfo(pkg, 0)
                        val label = pm.getApplicationLabel(ai).toString()
                        val usage = usageMap[pkg]
                        AppUsageInfo(
                            packageName = pkg,
                            displayName = if (label.isNotBlank()) label else pkg,
                            minutesUsed = usage?.let { TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground) } ?: 0L,
                            lastUsed = usage?.lastTimeUsed ?: 0L
                        )
                    } catch (_: Exception) { null }
                }
                .sortedWith(compareByDescending<AppUsageInfo> { it.minutesUsed }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Android 11+: obtiene TODAS las apps instaladas (requiere QUERY_ALL_PACKAGES en manifest). */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getAllInstalledPackagesApi30(pm: PackageManager): List<String> {
        return try {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.map { it.packageName }.distinct()
        } catch (_: Exception) {
            getLauncherPackagesLegacy(pm)
        }
    }

    /** Método legacy: solo apps con ícono de lanzador (para Android < 11 o fallback). */
    private fun getLauncherPackagesLegacy(pm: PackageManager): List<String> {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getInstalledAppsWithUsage(context: Context): List<AppUsageInfo> {
        return getAllLauncherApps(context)
    }

    private fun isLauncherApp(pm: PackageManager, packageName: String): Boolean {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfo = pm.queryIntentActivities(intent, 0)
        return resolveInfo.any { it.activityInfo.packageName == packageName }
    }

    /**
     * Estadísticas de uso de las últimas 24 horas (para "hoy").
     */
    private fun getUsageStatsMap(context: Context): Map<String, UsageStats>? {
        if (!PermissionHelper.hasUsageAccess(context)) return null
        val um = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)
        val stats = um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return null
        return stats.associateBy { it.packageName }
    }

    /**
     * Devuelve el paquete de la app que está en primer plano ahora.
     * Usa UsageEvents (más fiable) y, si falla, queryUsageStats con ventana amplia.
     * Ventana de 60 segundos para detectar apps aunque llevemos un rato usándolas.
     */
    fun getForegroundPackage(context: Context): String? {
        if (!PermissionHelper.hasUsageAccess(context)) return null
        val um = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.SECONDS.toMillis(60)
        // 1) Intentar con UsageEvents (eventos MOVE_TO_FOREGROUND = app actual)
        try {
            val events = um.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var lastForegroundPkg: String? = null
            while (events.hasNextEvent()) {
                if (events.getNextEvent(event) && event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundPkg = event.packageName
                }
            }
            if (!lastForegroundPkg.isNullOrBlank()) return lastForegroundPkg
        } catch (_: Exception) { }
        // 2) Fallback: app con lastTimeUsed más reciente (la que está en primer plano)
        val stats = um.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime) ?: return null
        return stats
            .filter { it.lastTimeUsed > 0 && it.packageName != OUR_PACKAGE }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    /**
     * Tiempo de uso por app hoy (en minutos) para estadísticas reales.
     * Key = packageName, Value = minutos (para cruzar con nombres en UI).
     */
    fun getTodayUsageByPackage(context: Context): Map<String, Long> {
        if (!PermissionHelper.hasUsageAccess(context)) return emptyMap()
        val um = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val endTime = System.currentTimeMillis()
        val startOfDay = endTime - TimeUnit.DAYS.toMillis(1)
        val stats = um.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOfDay, endTime) ?: return emptyMap()
        return stats
            .filter { it.totalTimeInForeground > 0 && it.packageName != OUR_PACKAGE }
            .associate { it.packageName to TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground) }
    }

    /** Nombre visible de una app por su packageName. */
    fun getAppDisplayName(context: Context, packageName: String): String {
        return try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
