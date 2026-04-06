package com.example.bloqueo.util

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.bloqueo.AppBlockerService
import androidx.core.content.ContextCompat
import android.Manifest
import com.example.bloqueo.receiver.BloqueoDeviceAdminReceiver

/**
 * Comprueba y solicita los permisos necesarios para que la app funcione como Stay Focused.
 */
object PermissionHelper {

    /**
     * Permiso de uso (PACKAGE_USAGE_STATS): necesario para ver qué app está en primer plano
     * y para estadísticas de uso reales.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Abre la pantalla de configuración para conceder "Acceso de uso".
     */
    fun openUsageAccessSettings(context: Context): Intent {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    /**
     * Permiso de dibujar sobre otras apps: necesario para mostrar la ventana de bloqueo.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Abre la pantalla de información de esta app (acceso directo).
     * Ahí el usuario puede activar "Mostrar sobre otras apps", notificaciones, etc., según el dispositivo.
     */
    fun openAppDetailSettings(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Servicio de accesibilidad: detecta app en primer plano (método más fiable, como Stay Focused).
     */
    fun hasAccessibilityService(context: Context): Boolean {
        val expected = "${context.packageName}/${AppBlockerService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.trim().equals(expected, ignoreCase = true) }
    }

    /**
     * Abre la pantalla de Accesibilidad para activar el servicio (detectar app en primer plano).
     */
    fun openAccessibilitySettings(context: Context): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Abre la pantalla para conceder "Mostrar sobre otras apps".
     */
    fun openOverlaySettings(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }
    }

    /**
     * Notificaciones (Android 13+): para el canal del servicio en primer plano.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Ignorar optimización de batería: para que el servicio no se cierre.
     */
    fun openBatteryOptimizationSettings(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }
    }

    // ---------- Administrador del dispositivo (protección contra desinstalación, tipo Stay Focused) ----------

    /**
     * Componente de nuestro Device Admin (para comprobar estado y desactivar).
     */
    fun getDeviceAdminComponent(context: Context): ComponentName {
        return ComponentName(context, BloqueoDeviceAdminReceiver::class.java)
    }

    /**
     * True si la app está registrada como administrador del dispositivo (no se puede desinstalar hasta desactivar).
     */
    fun hasDeviceAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.isAdminActive(getDeviceAdminComponent(context))
    }

    /**
     * Abre la pantalla del sistema para que el usuario active esta app como administrador del dispositivo.
     * Mientras esté activo, la app no se puede desinstalar.
     */
    fun getAddDeviceAdminIntent(context: Context): Intent {
        val component = getDeviceAdminComponent(context)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Stay Focused necesita ser administrador del dispositivo para impedir que se desinstale la app sin tu contraseña. Así proteges la configuración de bloqueo (como en Stay Focused)."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * Abre la lista de administradores del dispositivo (plan B si "Activar administrador" no abre la ventana directa).
     * Usamos la acción del sistema directamente porque ACTION_DEVICE_ADMIN_SETTINGS no está en todas las versiones del SDK.
     */
    fun getDeviceAdminListIntent(context: Context): Intent {
        return Intent("android.settings.DEVICE_ADMIN_SETTINGS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Desactiva el administrador del dispositivo para esta app. Después de esto, el usuario puede desinstalar la app.
     * Solo debe llamarse tras verificar la contraseña en la app.
     */
    fun removeDeviceAdmin(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return try {
            dpm.removeActiveAdmin(getDeviceAdminComponent(context))
            true
        } catch (_: Exception) {
            false
        }
    }
}
