package com.example.bloqueo

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.blockerx.complete.BlockerEngine
import com.blockerx.complete.BlockerPreferences
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.data.Horario
import com.example.bloqueo.util.PermissionHelper
import com.example.bloqueo.util.UsageStatsHelper
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class AppBlockerService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: AppRepository
    private var overlayView: View? = null
    private var isOverlayShowing: Boolean = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bgExecutor = Executors.newSingleThreadExecutor()

    // Cache ligera para detección de navegadores (fallback 18+ sin VPN)
    @Volatile
    private var browserCacheTimeMs: Long = 0L
    @Volatile
    private var cachedBrowserPackages: Set<String> = emptySet()

    private var pendingBrowserUrlCheck: Runnable? = null
    private val urlCheckDelayMs = 300L

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        repository = AppRepository(applicationContext)

        // Configuramos el servicio: cambios de ventana + contenido. La lectura de URL en navegador se
        // hace cuando abres/cambias a un navegador o cuando cambia el contenido de la ventana.
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return

        // Nunca bloquear nuestra propia app
        if (pkg == packageName) {
            hideOverlay()
            return
        }

        // Sin permiso de overlay, no podemos mostrar nada
        if (!PermissionHelper.hasOverlayPermission(this)) {
            hideOverlay()
            return
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 1) Integración BlockerX: si se acaba de bloquear una página vía VPN, mostramos overlay de "Página bloqueada"
            if (handleBlockerXPageOverlay(pkg)) return

            // 2) 18+ sin VPN: si "Bloquea contenido para adultos" está activo y VPN apagada, leemos la URL del navegador y bloqueamos solo esas páginas (como Blocker).
            if (handleBlockerXBrowserFallback(pkg)) return

            // 3) Lógica Stay Focused: perfiles, horarios, apps globales
            handleStayFocusedBlocking(pkg)
        } else {
            // Para cambios de contenido, solo nos interesa el bloqueo 18+ en navegadores.
            handleBlockerXBrowserFallback(pkg)
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        try {
            hideOverlay()
            bgExecutor.shutdown()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    // ---------- LÓGICA PRINCIPAL STAY FOCUSED (perfiles + horarios) ----------

    private fun handleStayFocusedBlocking(foregroundPkg: String) {
        val data = try {
            repository.getAppData()
        } catch (_: Exception) {
            hideOverlay()
            return
        }

        if (!data.configuracion.stayFocusedActivo) {
            hideOverlay()
            return
        }

        val nombresMapGlobal = data.nombresAppsBloqueadas ?: emptyMap()

        // Bloqueos globales
        val globalDesactivado = data.appsBloqueoDesactivado ?: emptyList()
        val globalBlocked = (data.aplicacionesBloqueadas - globalDesactivado).toSet()
        val globalHorarios = data.horariosBloqueo

        // Bloqueos por perfiles (solo si perfil tiene horarios)
        val activeProfileIds = repository.getPerfilesActivos()
        val profileIdsToUse = if (activeProfileIds.isNotEmpty()) {
            activeProfileIds
        } else {
            data.perfiles.mapNotNull { it.id }
        }

        val profileBlocked = mutableSetOf<String>()
        val profileHorarios = mutableListOf<Horario>()
        val profileNames = mutableMapOf<String, String>()

        for (pid in profileIdsToUse) {
            val p = data.perfiles.find { it.id == pid } ?: continue
            if (p.horarios.isEmpty()) continue

            val desactivado = p.appsBloqueoDesactivado ?: emptyList()
            val apps = (p.appsBloqueadas - desactivado)
            if (apps.isEmpty()) continue

            profileBlocked.addAll(apps)
            profileHorarios.addAll(p.horarios)
            profileNames.putAll(p.nombresAppsBloqueadas ?: emptyMap())
        }

        val mergedNames = (nombresMapGlobal + profileNames)

        val shouldBlock = when {
            foregroundPkg in globalBlocked && (globalHorarios.isEmpty() || isWithinAnySchedule(globalHorarios)) -> true
            foregroundPkg in profileBlocked && profileHorarios.isNotEmpty() && isWithinAnySchedule(profileHorarios) -> true
            else -> false
        }

        if (!shouldBlock) {
            hideOverlay()
            return
        }

        val displayName = mergedNames[foregroundPkg]
            ?: try {
                UsageStatsHelper.getAppDisplayName(this, foregroundPkg)
            } catch (_: Exception) {
                "App bloqueada"
            }

        showOverlay(
            title = "App bloqueada",
            message = "Mantente enfocado.\nLa aplicación ($displayName) está bloqueada en este momento.",
            isPageBlocked = false
        )
        // Expulsar al usuario de la app bloqueada hacia el inicio
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {
        }
    }

    private fun isWithinAnySchedule(horarios: List<Horario>): Boolean {
        if (horarios.isEmpty()) return true
        return try {
            val cal = Calendar.getInstance(Locale.getDefault())
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Lunes"
                Calendar.TUESDAY -> "Martes"
                Calendar.WEDNESDAY -> "Miércoles"
                Calendar.THURSDAY -> "Jueves"
                Calendar.FRIDAY -> "Viernes"
                Calendar.SATURDAY -> "Sábado"
                Calendar.SUNDAY -> "Domingo"
                else -> ""
            }
            for (h in horarios) {
                if (!h.activo) continue
                val inDays = when (h.dias) {
                    "Todos los días" -> true
                    "Fines de semana" -> dayName == "Sábado" || dayName == "Domingo"
                    "Lunes a Viernes" -> dayName in listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes")
                    else -> true
                }
                if (!inDays) continue
                if (h.diaCompleto) return true
                val (sh, sm) = parseTime(h.inicio)
                val (eh, em) = parseTime(h.fin)
                val start = sh * 60 + sm
                val end = eh * 60 + em
                val inTime = if (end > start) currentMinutes in start..end else (currentMinutes >= start || currentMinutes <= end)
                if (inTime) return true
            }
            false
        } catch (_: Exception) {
            true
        }
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val parts = time.split(":", ".")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    // ---------- INTEGRACIÓN BLOCKERX (18+) ----------

    /**
     * Integración BlockerX (18+): cuando la VPN ha bloqueado una página (dominios en código BlockerX
     * o personalizados), mostramos overlay y sacamos al usuario de esa página abriendo el inicio del
     * navegador o Google en el mismo navegador (como hace Android Blocker).
     */
    private fun handleBlockerXPageOverlay(foregroundPkg: String): Boolean {
        return try {
            val blockPrefs = getSharedPreferences("blockerx_overlay", Context.MODE_PRIVATE)
            val lastBlocked = blockPrefs.getLong("last_blocked_time", 0L)
            val lastDomain = blockPrefs.getString("last_blocked_domain", "") ?: ""
            if (lastBlocked > 0 &&
                lastDomain.isNotBlank() &&
                (System.currentTimeMillis() - lastBlocked) < PAGE_BLOCK_OVERLAY_MS
            ) {
                // Primero sacamos al usuario de la página bloqueada: abrir inicio del navegador o Google.
                redirectBrowserToSafeStart(foregroundPkg)
                // Luego mostramos el overlay "Página bloqueada".
                showOverlay(
                    title = "Página bloqueada",
                    message = "Esta página ($lastDomain) está bloqueada.\nTómate un descanso.",
                    isPageBlocked = true
                )
                try {
                    blockPrefs.edit()
                        .putLong("last_blocked_time", 0L)
                        .putString("last_blocked_domain", "")
                        .apply()
                } catch (_: Exception) {
                }
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Bloqueo 18+ por accesibilidad (sin VPN): cuando "Bloquear Contenido Para Adultos" está activo,
     * detectamos la URL del navegador por accesibilidad y bloqueamos páginas adultas.
     * Usa la MISMA ventana emergente de bloqueo que "App bloqueada".
     */
    private fun handleBlockerXBrowserFallback(foregroundPkg: String): Boolean {
        return try {
            val bx = getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)
            val adultEnabled = bx.getBoolean("adult_enabled", false)
            if (!adultEnabled) {
                pendingBrowserUrlCheck?.let { mainHandler.removeCallbacks(it) }
                pendingBrowserUrlCheck = null
                return false
            }
            val browserPkgs = getBrowserPackagesCached()
            if (foregroundPkg !in browserPkgs) {
                pendingBrowserUrlCheck?.let { mainHandler.removeCallbacks(it) }
                pendingBrowserUrlCheck = null
                return false
            }
            pendingBrowserUrlCheck?.let { mainHandler.removeCallbacks(it) }
            pendingBrowserUrlCheck = Runnable {
                pendingBrowserUrlCheck = null
                tryCheckBrowserUrlAndBlock(foregroundPkg)
            }
            mainHandler.postDelayed(pendingBrowserUrlCheck!!, urlCheckDelayMs)
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun tryCheckBrowserUrlAndBlock(browserPkg: String) {
        try {
            val root = rootInActiveWindow ?: return
            val url = extractBrowserUrl(root, browserPkg) ?: return
            if (url.isBlank()) return
            val prefs = loadBlockerPrefsFromSync()
            val engine = BlockerEngine(prefs)
            val result = engine.analyze(url)
            if (!result.blocked) {
                hideOverlay()
                return
            }
            redirectBrowserToSafeStart(browserPkg)
            showOverlay(
                title = "Página bloqueada",
                message = "Esta página (${result.domain}) está bloqueada.\nTómate un descanso.",
                isPageBlocked = true
            )
        } catch (_: Exception) {
        }
    }

    private fun loadBlockerPrefsFromSync(): BlockerPreferences {
        return try {
            val sp = getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)
            val customDomains = (sp.getString("custom_domains", "") ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            val customKeywords = (sp.getString("custom_keywords", "") ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() }
            val whitelist = (sp.getString("whitelist", "") ?: "").split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            BlockerPreferences(
                adultContentEnabled = true,
                keywordsEnabled = sp.getBoolean("keywords_enabled", false),
                safeSearchEnabled = sp.getBoolean("safe_search", true),
                reelsEnabled = sp.getBoolean("reels_enabled", true),
                customDomains = customDomains,
                customKeywords = customKeywords,
                whitelist = whitelist
            )
        } catch (_: Exception) {
            BlockerPreferences(adultContentEnabled = true)
        }
    }

    /** IDs de la barra de direcciones por navegador (Chrome, Brave, Opera, Firefox, etc.) */
    private fun getUrlBarIds(browserPkg: String): List<String> = when (browserPkg) {
        "com.android.chrome" -> listOf("com.android.chrome:id/url_bar", "com.android.chrome:id/search_box_text")
        "com.brave.browser" -> listOf("com.brave.browser:id/url_bar", "com.brave.browser:id/omnibox_input", "com.brave.browser:id/url_bar_title", "com.brave.browser:id/brave_address_bar")
        "com.opera.browser", "com.opera.mini.native", "com.opera.gx" -> listOf("$browserPkg:id/url_bar", "$browserPkg:id/search_bar", "$browserPkg:id/url_bar_title", "$browserPkg:id/omnibox_input")
        "com.microsoft.emmx" -> listOf("com.microsoft.emmx:id/url_bar", "com.microsoft.emmx:id/address_bar_url_text")
        "org.mozilla.firefox", "org.mozilla.firefox_nightly" -> listOf("org.mozilla.firefox:id/url_bar_title", "org.mozilla.firefox:id/url_bar")
        "org.mozilla.fenix", "org.mozilla.fennec_aurora" -> listOf("org.mozilla.fenix:id/mozac_browser_toolbar_url_view", "org.mozilla.fenix:id/url_bar")
        "com.sec.android.app.sbrowser" -> listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text")
        "com.duckduckgo.mobile.android" -> listOf("com.duckduckgo.mobile.android:id/omnibarTextInput", "com.duckduckgo.mobile.android:id/url_bar")
        else -> listOf("$browserPkg:id/url_bar", "$browserPkg:id/url", "$browserPkg:id/address_bar", "$browserPkg:id/search_box_text")
    }

    /** Obtiene la URL actual de la barra de direcciones vía accesibilidad. */
    private fun extractBrowserUrl(root: AccessibilityNodeInfo?, browserPkg: String): String? {
        if (root == null) return null
        try {
            for (viewId in getUrlBarIds(browserPkg)) {
                val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                for (node in nodes) {
                    try {
                        val text = node.text?.toString()?.trim()
                        if (!text.isNullOrBlank() && looksLikeUrl(text)) return normalizeUrl(text)
                        node.contentDescription?.toString()?.trim()?.let { if (looksLikeUrl(it)) return normalizeUrl(it) }
                    } finally {
                        try { node.recycle() } catch (_: Exception) { }
                    }
                }
            }
            return findUrlInNode(root, 0, 20)
        } catch (_: Exception) {
            return findUrlInNode(root, 0, 20)
        } finally {
            try { root.recycle() } catch (_: Exception) { }
        }
    }

    private fun looksLikeUrl(text: String): Boolean =
        text.startsWith("http://") || text.startsWith("https://") ||
            text.matches(Regex("^[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}.*"))

    private fun normalizeUrl(text: String): String =
        if (text.startsWith("http")) text else "https://$text"

    private fun findUrlInNode(node: AccessibilityNodeInfo?, depth: Int, maxDepth: Int): String? {
        if (node == null || depth > maxDepth) return null
        try {
            val text = node.text?.toString()?.trim()
            if (!text.isNullOrBlank() && looksLikeUrl(text)) return normalizeUrl(text)
            node.contentDescription?.toString()?.trim()?.let { if (looksLikeUrl(it)) return normalizeUrl(it) }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    findUrlInNode(child, depth + 1, maxDepth)?.let { return it }
                } finally {
                    try { child.recycle() } catch (_: Exception) { }
                }
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Saca al usuario de la página bloqueada: abre el navegador en una página segura (inicio o Google),
     * como hace la aplicación Android Blocker. Si la app en primer plano es un navegador, abre ahí
     * Google; si no, intenta abrir el navegador por defecto en Google; si falla, va al inicio.
     */
    private fun redirectBrowserToSafeStart(foregroundPkg: String) {
        val browserPkgs = try {
            getBrowserPackagesCached()
        } catch (_: Exception) {
            emptySet()
        }

        val safeUrl = "https://www.google.com"

        // Si la app en primer plano es un navegador, abrimos Google en ese mismo navegador (sale de la página bloqueada).
        if (foregroundPkg in browserPkgs) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                    setPackage(foregroundPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }

        // Fallback: abrir el navegador por defecto en Google (Chrome, Brave, Opera, etc.).
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        } catch (_: Exception) {
        }

        // Último recurso: ir al inicio del dispositivo.
        try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (_: Exception) {
        }
    }

    private fun getBrowserPackagesCached(): Set<String> {
        val now = System.currentTimeMillis()
        if (browserCacheTimeMs > 0L && (now - browserCacheTimeMs) < BROWSER_CACHE_VALID_MS) {
            return cachedBrowserPackages
        }
        browserCacheTimeMs = now
        val pm = packageManager ?: return cachedBrowserPackages
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val results = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        } catch (_: Exception) {
            emptyList()
        }
        cachedBrowserPackages = results.mapNotNull { it.activityInfo?.packageName }.toSet()
        return cachedBrowserPackages
    }

    // ---------- UI DEL OVERLAY (TODO EN ESTE ARCHIVO) ----------

    private fun showOverlay(title: String, message: String, isPageBlocked: Boolean) {
        // Si ya se muestra, solo actualizamos textos
        if (isOverlayShowing && overlayView != null) {
            try {
                overlayView?.findViewById<TextView>(ID_TITLE)?.text = title
                overlayView?.findViewById<TextView>(ID_MESSAGE)?.text = message
            } catch (_: Exception) {
            }
            return
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // Bloquea interacción con lo de atrás y captura el botón Atrás
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                // Consumimos el botón Atrás para que no se use como trampa
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }

        // Imagen de fondo (si el usuario la configuró). Debe cubrir toda la pantalla detrás del texto.
        val backgroundImageView = ImageView(this).apply {
            id = ID_CUSTOM_IMAGE
            visibility = View.GONE
            scaleType = ImageView.ScaleType.CENTER_CROP
            this.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Fondo por defecto cuando no hay imagen.
        val gradientBackground = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#141E30"), Color.parseColor("#243B55"))
        )
        root.background = gradientBackground

        // Capa oscura para que el texto sea legible sobre la imagen.
        val dimLayer = View(this).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            this.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Contenido centrado (texto / icono)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
            this.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Candado principal
        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_secure)
            this.layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                setMargins(0, 0, 0, 48)
            }
        }

        // Título
        val titleText = TextView(this).apply {
            id = ID_TITLE
            text = title
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            this.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        // Mensaje
        val messageText = TextView(this).apply {
            id = ID_MESSAGE
            text = message
            textSize = 16f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            setLineSpacing(0f, 1.2f)
            this.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 32) }
        }

        content.addView(icon)
        content.addView(titleText)
        content.addView(messageText)

        root.addView(backgroundImageView)
        root.addView(dimLayer)
        root.addView(content)

        overlayView = root

        try {
            windowManager.addView(root, layoutParams)
            isOverlayShowing = true
        } catch (_: Exception) {
            overlayView = null
            isOverlayShowing = false
            return
        }

        // Cargar la imagen que ya configuraste en la app (si existe)
        loadCustomImageAsync(backgroundImageView)
    }

    private fun hideOverlay() {
        if (!isOverlayShowing) return
        val view = overlayView
        overlayView = null
        isOverlayShowing = false
        if (view != null) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }
    }

    private fun loadCustomImageAsync(imageView: ImageView) {
        val config = try {
            repository.getAppData().configuracion
        } catch (_: Exception) {
            null
        } ?: return

        val uriString = config.imagenOverlayBloqueoUri.takeIf { it.isNotBlank() } ?: return
        val scale = if (config.imagenOverlayScale > 0f) config.imagenOverlayScale else 1f
        val offsetX = config.imagenOverlayOffsetX
        val offsetY = config.imagenOverlayOffsetY

        bgExecutor.execute {
            var bitmap: Bitmap? = null
            try {
                val uri = Uri.parse(uriString)
                contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    bitmap = BitmapFactory.decodeStream(input, null, opts)
                }
            } catch (_: Exception) {
            }

            mainHandler.post {
                if (!isOverlayShowing || overlayView == null) return@post
                bitmap?.let {
                    try {
                        imageView.setImageBitmap(it)
                        imageView.visibility = View.VISIBLE
                        imageView.scaleX = scale
                        imageView.scaleY = scale
                        imageView.translationX = offsetX
                        imageView.translationY = offsetY
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    // Acción para expulsar al usuario al menú principal
    private fun goToHomeScreen() {
        try {
            val startMain = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(startMain)
        } catch (_: Exception) {
        }
        // Al volver al launcher, el siguiente evento de accesibilidad ocultará el overlay si ya no hace falta.
    }

    companion object {
        private const val PAGE_BLOCK_OVERLAY_MS = 2_500L
        private const val BROWSER_CACHE_VALID_MS = 20_000L

        // IDs artificiales para las vistas creadas por código
        private const val ID_TITLE = 0x1001
        private const val ID_MESSAGE = 0x1002
        private const val ID_CUSTOM_IMAGE = 0x1003
    }
}

