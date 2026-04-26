package com.example.bloqueo.data

import android.content.Context
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("stay_focused", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getAppData(): AppData {
        val json = prefs.getString(KEY_APP_DATA, null) ?: return AppData()
        return try {
            var data = gson.fromJson(json, AppData::class.java)
            data = data.copy(
                nombresAppsBloqueadas = data.nombresAppsBloqueadas ?: emptyMap(),
                sitiosBloqueados = data.sitiosBloqueados ?: emptyList(),
                appsBloqueoDesactivado = data.appsBloqueoDesactivado ?: emptyList()
            )
            // Compatibilidad: si faltan campos nuevos en JSON, Gson puede dejarlos en 0f.
            // La imagen debe cubrir pantalla: escala mínima 1f.
            run {
                val c = data.configuracion
                val fixedScale = if (c.imagenOverlayScale <= 0f) 1f else c.imagenOverlayScale
                val fixedOffsetX = if (c.imagenOverlayOffsetX.isNaN() || c.imagenOverlayOffsetX.isInfinite()) 0f else c.imagenOverlayOffsetX
                val fixedOffsetY = if (c.imagenOverlayOffsetY.isNaN() || c.imagenOverlayOffsetY.isInfinite()) 0f else c.imagenOverlayOffsetY
                if (fixedScale != c.imagenOverlayScale || fixedOffsetX != c.imagenOverlayOffsetX || fixedOffsetY != c.imagenOverlayOffsetY) {
                    data = data.copy(
                        configuracion = c.copy(
                            imagenOverlayScale = fixedScale,
                            imagenOverlayOffsetX = fixedOffsetX,
                            imagenOverlayOffsetY = fixedOffsetY
                        )
                    )
                    saveAppData(data)
                }
            }
            if (data.perfiles.any { it.id.isNullOrEmpty() }) {
                data = data.copy(perfiles = data.perfiles.mapIndexed { i, p ->
                    p.copy(
                        id = p.id?.takeIf { it.isNotBlank() } ?: "gen_${System.currentTimeMillis()}_$i",
                        nombresAppsBloqueadas = p.nombresAppsBloqueadas ?: emptyMap(),
                        appsBloqueoDesactivado = p.appsBloqueoDesactivado ?: emptyList()
                    )
                })
                saveAppData(data)
            } else {
                data = data.copy(perfiles = data.perfiles.map { p ->
                    p.copy(
                        nombresAppsBloqueadas = p.nombresAppsBloqueadas ?: emptyMap(),
                        appsBloqueoDesactivado = p.appsBloqueoDesactivado ?: emptyList()
                    )
                })
            }
            // Migración: si hay perfilActivoId pero perfilActivoIds vacío, usar el único activo
            data = if (data.perfilActivoIds.isEmpty() && data.perfilActivoId != null) {
                data.copy(perfilActivoIds = listOf(data.perfilActivoId!!)).also { saveAppData(it) }
            } else data
            data
        } catch (e: Exception) {
            AppData()
        }
    }

    fun saveAppData(data: AppData) {
        prefs.edit().putString(KEY_APP_DATA, gson.toJson(data)).apply()
    }

    private fun isStrictModeActive(): Boolean {
        val c = getAppData().configuracion
        return c.modoActual == "estricto" && c.modoEstricto == true
    }

    /** Añade bloqueo por package name (y nombre visible para la UI). */
    fun agregarAppBloqueada(packageName: String, displayName: String = packageName, limiteDiario: Int = 0) {
        if (isStrictModeActive()) return
        val data = getAppData()
        if (packageName in data.aplicacionesBloqueadas) return
        val nuevasApps = data.aplicacionesBloqueadas + packageName
        val nuevosNombres = (data.nombresAppsBloqueadas ?: emptyMap()) + (packageName to displayName)
        val nuevosLimites = if (limiteDiario > 0) {
            data.limitesDiarios + (packageName to limiteDiario)
        } else data.limitesDiarios
        saveAppData(data.copy(
            aplicacionesBloqueadas = nuevasApps,
            nombresAppsBloqueadas = nuevosNombres,
            limitesDiarios = nuevosLimites
        ))
    }

    fun quitarAppBloqueada(packageName: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val desactivado = (data.appsBloqueoDesactivado ?: emptyList()) - packageName
        saveAppData(data.copy(
            aplicacionesBloqueadas = data.aplicacionesBloqueadas - packageName,
            appsBloqueoDesactivado = desactivado,
            nombresAppsBloqueadas = (data.nombresAppsBloqueadas ?: emptyMap()) - packageName,
            limitesDiarios = data.limitesDiarios - packageName
        ))
    }

    /** Activa o desactiva el bloqueo de una app (sigue en la lista pero no se bloquea si está desactivado). */
    fun setAppBloqueoActivo(packageName: String, activo: Boolean) {
        if (isStrictModeActive()) return
        val data = getAppData()
        if (packageName !in data.aplicacionesBloqueadas) return
        val desactivado = (data.appsBloqueoDesactivado ?: emptyList()).toMutableList()
        if (activo) desactivado.remove(packageName) else if (packageName !in desactivado) desactivado.add(packageName)
        saveAppData(data.copy(appsBloqueoDesactivado = desactivado))
    }

    fun agregarHorario(inicio: String, fin: String, dias: String, diaCompleto: Boolean = false) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val nuevosHorarios = data.horariosBloqueo + Horario(inicio = inicio, fin = fin, dias = dias, diaCompleto = diaCompleto)
        saveAppData(data.copy(horariosBloqueo = nuevosHorarios))
    }

    fun eliminarHorario(horario: Horario) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(horariosBloqueo = data.horariosBloqueo - horario))
    }

    fun agregarSitioBloqueado(url: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val list = data.sitiosBloqueados ?: emptyList()
        if (url.isNotBlank() && url !in list) {
            val newList = list + url
            saveAppData(data.copy(sitiosBloqueados = newList))
            try {
                com.example.bloqueo.util.BlockerXIntegration.syncDomainsFromStayFocused(appContext, newList)
            } catch (_: Exception) {}
        }
    }

    fun quitarSitioBloqueado(url: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val newList = (data.sitiosBloqueados ?: emptyList()) - url
        saveAppData(data.copy(sitiosBloqueados = newList))
        try {
            com.example.bloqueo.util.BlockerXIntegration.syncDomainsFromStayFocused(appContext, newList)
        } catch (_: Exception) {}
    }

    fun actualizarObjetivoDiario(minutos: Int) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(perfil = data.perfil.copy(objetivoDiario = minutos)))
    }

    /**
     * Actualiza la configuración. En modo estricto solo se permite la desactivación (QR);
     * ningún otro cambio de configuración tiene efecto hasta desactivar el modo estricto.
     */
    fun actualizarConfiguracion(block: (Configuracion) -> Configuracion) {
        val data = getAppData()
        val current = data.configuracion
        val isStrictMode = current.modoActual == "estricto" && current.modoEstricto == true
        val updated = block(current)
        if (isStrictMode) {
            val isDeactivation = updated.modoActual == "normal" && updated.modoEstricto == false
            if (!isDeactivation) return
        }
        saveAppData(data.copy(configuracion = updated))
    }

    fun agregarPerfil(nombre: String): String {
        if (isStrictModeActive()) return ""
        val id = UUID.randomUUID().toString()
        val data = getAppData()
        saveAppData(data.copy(
            perfiles = data.perfiles + PerfilConfig(id = id, nombre = nombre, creado = dateFormat.format(Date()))
        ))
        return id
    }

    fun getPerfil(id: String): PerfilConfig? = getAppData().perfiles.find { it.id == id }

    fun actualizarPerfilNombre(id: String, nombre: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(
            perfiles = data.perfiles.map { if (it.id == id) it.copy(nombre = nombre) else it }
        ))
    }

    fun actualizarPerfilApps(id: String, apps: List<String>, nombres: Map<String, String>) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val perfil = data.perfiles.find { it.id == id } ?: return
        val desactivado = (perfil.appsBloqueoDesactivado ?: emptyList()).filter { it in apps }
        saveAppData(data.copy(
            perfiles = data.perfiles.map { if (it.id == id) it.copy(appsBloqueadas = apps, nombresAppsBloqueadas = nombres, appsBloqueoDesactivado = desactivado) else it }
        ))
    }

    /** Activa o desactiva el bloqueo de una app dentro de un perfil. */
    fun setAppBloqueoActivoEnPerfil(perfilId: String, packageName: String, activo: Boolean) {
        if (isStrictModeActive()) return
        val p = getPerfil(perfilId) ?: return
        if (packageName !in p.appsBloqueadas) return
        val data = getAppData()
        val desactivado = (p.appsBloqueoDesactivado ?: emptyList()).toMutableList()
        if (activo) desactivado.remove(packageName) else if (packageName !in desactivado) desactivado.add(packageName)
        saveAppData(data.copy(
            perfiles = data.perfiles.map { if (it.id == perfilId) it.copy(appsBloqueoDesactivado = desactivado) else it }
        ))
    }

    fun agregarAppEnPerfil(perfilId: String, packageName: String, displayName: String) {
        val p = getPerfil(perfilId) ?: return
        if (packageName in p.appsBloqueadas) return
        actualizarPerfilApps(
            perfilId,
            p.appsBloqueadas + packageName,
            (p.nombresAppsBloqueadas ?: emptyMap()) + (packageName to displayName)
        )
    }

    fun quitarAppDePerfil(perfilId: String, packageName: String) {
        val p = getPerfil(perfilId) ?: return
        actualizarPerfilApps(
            perfilId,
            p.appsBloqueadas - packageName,
            (p.nombresAppsBloqueadas ?: emptyMap()) - packageName
        )
    }

    fun actualizarPerfilHorarios(id: String, horarios: List<Horario>) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(
            perfiles = data.perfiles.map { if (it.id == id) it.copy(horarios = horarios) else it }
        ))
    }

    fun agregarHorarioEnPerfil(perfilId: String, horario: Horario) {
        val p = getPerfil(perfilId) ?: return
        actualizarPerfilHorarios(perfilId, p.horarios + horario)
    }

    fun eliminarHorarioDePerfil(perfilId: String, horario: Horario) {
        val p = getPerfil(perfilId) ?: return
        actualizarPerfilHorarios(perfilId, p.horarios - horario)
    }

    fun eliminarPerfil(id: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(
            perfiles = data.perfiles.filter { it.id != id },
            perfilActivoId = if (data.perfilActivoId == id) null else data.perfilActivoId,
            perfilActivoIds = data.perfilActivoIds.filter { it != id }
        ))
    }

    fun duplicarPerfil(id: String): String? {
        if (isStrictModeActive()) return null
        val p = getPerfil(id) ?: return null
        val newId = UUID.randomUUID().toString()
        val data = getAppData()
        saveAppData(data.copy(
            perfiles = data.perfiles + p.copy(
                id = newId,
                nombre = p.nombre + " (copia)",
                creado = dateFormat.format(Date()),
                nombresAppsBloqueadas = p.nombresAppsBloqueadas ?: emptyMap()
            )
        ))
        return newId
    }

    /** Activa solo este perfil (reemplaza la lista de activos por uno). Mantenido por compatibilidad. */
    fun setPerfilActivo(id: String?) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(
            perfilActivoId = id,
            perfilActivoIds = if (id != null) listOf(id) else emptyList()
        ))
    }

    /** Devuelve la lista de ids de perfiles actualmente activos (puede ser varios). */
    fun getPerfilesActivos(): List<String> {
        val data = getAppData()
        return if (data.perfilActivoIds.isNotEmpty()) data.perfilActivoIds
        else data.perfilActivoId?.let { listOf(it) } ?: emptyList()
    }

    /** Añade o quita un perfil de los activos; varios perfiles pueden estar activos a la vez. */
    fun togglePerfilActivo(profileId: String) {
        if (isStrictModeActive()) return
        val data = getAppData()
        val current = if (data.perfilActivoIds.isNotEmpty()) data.perfilActivoIds
            else data.perfilActivoId?.let { listOf(it) } ?: emptyList()
        val newIds = if (profileId in current) current.filter { it != profileId }
            else current + profileId
        saveAppData(data.copy(
            perfilActivoId = newIds.firstOrNull(),
            perfilActivoIds = newIds
        ))
    }

    /** Indica si un perfil está entre los activos. */
    fun isPerfilActivo(profileId: String): Boolean = profileId in getPerfilesActivos()

    /** Indica si hay apps bloqueadas (perfiles activos o lista global). Usado para mostrar avisos de permisos. */
    fun hasBlockedApps(): Boolean {
        val data = getAppData()
        val globalDesactivado = data.appsBloqueoDesactivado ?: emptyList()
        val globalHas = (data.aplicacionesBloqueadas - globalDesactivado).isNotEmpty()
        val profilesHave = data.perfiles.any { p ->
            val des = p.appsBloqueoDesactivado ?: emptyList()
            (p.appsBloqueadas - des).isNotEmpty()
        }
        return globalHas || profilesHave
    }

    fun obtenerEstadisticasHoy(): Map<String, Int> {
        val hoy = dateOnlyFormat.format(Date())
        return getAppData().estadisticasUso
            .filter { it.fecha.startsWith(hoy) }
            .groupBy { it.app }
            .mapValues { it.value.sumOf { r -> r.minutos } }
    }

    fun agregarEstadistica(app: String, minutos: Int) {
        if (isStrictModeActive()) return
        val data = getAppData()
        saveAppData(data.copy(
            estadisticasUso = data.estadisticasUso + UsoRegistro(
                fecha = dateFormat.format(Date()),
                app = app,
                minutos = minutos
            )
        ))
    }

    fun agregarPornoBrowser(packageName: String) {
        val data = getAppData()
        if (packageName in data.configuracion.pornoBrowsersBloqueados) return
        saveAppData(data.copy(configuracion = data.configuracion.copy(
            pornoBrowsersBloqueados = data.configuracion.pornoBrowsersBloqueados + packageName
        )))
    }

    fun quitarPornoBrowser(packageName: String) {
        val data = getAppData()
        saveAppData(data.copy(configuracion = data.configuracion.copy(
            pornoBrowsersBloqueados = data.configuracion.pornoBrowsersBloqueados - packageName
        )))
    }

    fun agregarPornoDominio(dominio: String) {
        val data = getAppData()
        val d = dominio.trim().lowercase()
        if (d.isBlank() || d in data.configuracion.pornoDominios) return
        saveAppData(data.copy(configuracion = data.configuracion.copy(
            pornoDominios = data.configuracion.pornoDominios + d
        )))
    }

    fun quitarPornoDominio(dominio: String) {
        val data = getAppData()
        saveAppData(data.copy(configuracion = data.configuracion.copy(
            pornoDominios = data.configuracion.pornoDominios - dominio
        )))
    }

    companion object {
        private const val KEY_APP_DATA = "app_data"
    }
}
