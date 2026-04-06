package com.example.bloqueo.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Administrador del dispositivo: mientras esté activo, la app no se puede desinstalar
 * hasta que el usuario la desactive (con contraseña) desde Configuración.
 */
class BloqueoDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Protección contra desinstalación activada", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Protección desactivada. Ya puedes desinstalar la app.", Toast.LENGTH_LONG).show()
    }

    /**
     * Mensaje que ve el usuario si intenta desactivar el administrador desde Ajustes del sistema.
     * Se le indica que use la app y la contraseña para desactivar.
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Para desactivar la protección, abre Stay Focused, ve a Configuración → Protección contra desinstalación e introduce tu contraseña para \"Desactivar protección\"."
    }
}
