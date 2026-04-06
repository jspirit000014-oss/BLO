package com.example.bloqueo.util

import android.content.Context
import com.blockerx.complete.PrefKeys
import com.blockerx.complete.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

object BlockerXIntegration {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Sincroniza la lista de "Sitios bloqueados" (Stay Focused) con BlockerX para que
     * el motor VPN pueda bloquearlos realmente.
     *
     * Nota: BlockerX VPN lee preferencias desde "blockerx_sync" (SharedPreferences) y
     * la UI de BlockerX lee desde DataStore. Por eso se escriben ambos.
     */
    fun syncDomainsFromStayFocused(context: Context, sitios: List<String>) {
        val appContext = context.applicationContext
        val normalized = sitios
            .asSequence()
            .map { it.trim().lowercase() }
            .map { s ->
                s.removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .split("/", " ").firstOrNull().orEmpty()
            }
            .filter { it.isNotBlank() }
            .toSet()

        val joined = normalized.joinToString(",")

        try {
            appContext.getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)
                .edit()
                .putString("custom_domains", joined)
                .apply()
        } catch (_: Exception) {}

        scope.launch {
            try {
                appContext.dataStore.edit { prefs ->
                    prefs[PrefKeys.CUSTOM_DOMAINS] = joined
                }
            } catch (_: Exception) {}
        }
    }
}

