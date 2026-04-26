package com.example.bloqueo.data

data class AppData(
    val perfil: Perfil = Perfil(),
    /** Lista de package names de apps bloqueadas. */
    val aplicacionesBloqueadas: List<String> = emptyList(),
    /** Apps de la lista que tienen el bloqueo desactivado (siguen en la lista pero no se bloquean). */
    val appsBloqueoDesactivado: List<String>? = null,
    /** Nombre visible por package (para la UI y el overlay). */
    val nombresAppsBloqueadas: Map<String, String>? = null,
    val horariosBloqueo: List<Horario> = emptyList(),
    val estadisticasUso: List<UsoRegistro> = emptyList(),
    val limitesDiarios: Map<String, Int> = emptyMap(),
    val perfiles: List<PerfilConfig> = emptyList(),
    /** Id del perfil activo para bloqueo; si null se usa aplicacionesBloqueadas/horariosBloqueo globales. (Legacy: si perfilActivoIds está vacío se usa este.) */
    val perfilActivoId: String? = null,
    /** Lista de ids de perfiles activos; si está vacía se usa perfilActivoId o datos globales. Varios perfiles pueden estar activos a la vez. */
    val perfilActivoIds: List<String> = emptyList(),
    val configuracion: Configuracion = Configuracion(),
    /** URLs o dominios bloqueados (ej. para recordatorio; bloqueo real depende del navegador). */
    val sitiosBloqueados: List<String>? = null
)

data class Perfil(
    val nombre: String = "Usuario",
    val edad: Int = 25,
    val objetivoDiario: Int = 120
)

data class Horario(
    val inicio: String = "08:00",
    val fin: String = "17:00",
    val dias: String = "Lunes a Viernes",
    val activo: Boolean = true,
    /** Si true, bloquea el día completo en los días indicados (se ignora inicio/fin). */
    val diaCompleto: Boolean = false
)

data class UsoRegistro(
    val fecha: String,
    val app: String,
    val minutos: Int
)

data class PerfilConfig(
    val id: String? = null,
    val nombre: String,
    val appsBloqueadas: List<String> = emptyList(),
    /** Apps del perfil con bloqueo desactivado (siguen en la lista pero no se bloquean). */
    val appsBloqueoDesactivado: List<String>? = null,
    val nombresAppsBloqueadas: Map<String, String>? = null,
    val horarios: List<Horario> = emptyList(),
    val creado: String
)

data class Configuracion(
    val modoActual: String = "normal",
    val passwordModoBloqueo: String = "",
    val modoEstricto: Boolean = false,
    val stayFocusedActivo: Boolean = true,
    val rotacionPantalla: Boolean = false,
    val idioma: String = "Español",
    val citaMotivacional: Boolean = false,
    val pomodoroActivo: Boolean = false,
    val pomodoroTrabajo: Int = 25,
    val pomodoroDescanso: Int = 5,
    val modoOscuroActivo: Boolean = false,
    val bloquearNavegadores: Boolean = false,
    val bloquearPantallaDividida: Boolean = false,
    val bloquearApagado: Boolean = false,
    val bloquearAppsRecientes: Boolean = false,
    /** URI (content) de imagen personalizada para la pantalla de bloqueo; vacío = sin imagen. */
    val imagenOverlayBloqueoUri: String = "",
    /** Escala de la imagen de pantalla de bloqueo (1f = cubre pantalla). */
    val imagenOverlayScale: Float = 1f,
    /** Desplazamiento X de la imagen en px (para ajustar posición). */
    val imagenOverlayOffsetX: Float = 0f,
    /** Desplazamiento Y de la imagen en px (para ajustar posición). */
    val imagenOverlayOffsetY: Float = 0f,
    // Modo estricto (pantalla "Activar modo estricto") — nullable para compatibilidad con datos antiguos
    /** "todo" = Restringir todo, "especificas" = Restringir específicas */
    val estrictoRestriccion: String? = "todo",
    /** No desinstalar apps (incl. esta app) */
    val estrictoBloquearDesinstalacion: Boolean? = true,
    /** Bloquear acceso a configuración del teléfono */
    val estrictoBloquearConfiguracion: Boolean? = true,
    /** Método desactivación: "expiracion" | "qr" | "programado" | "texto_aleatorio" */
    val estrictoMetodoDesactivacion: String? = "expiracion",
    /** Minutos para expiración (si método = expiracion) */
    val estrictoExpiracionMinutos: Int? = 60,
    /** Token secreto codificado en el QR; al escanearlo se desactiva el modo estricto. */
    val estrictoQrToken: String? = null
)
