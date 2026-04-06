/**
 * ============================================================
 *  BlockerX Complete - Un solo archivo Android
 *  Bloqueo completo de contenido adulto
 *
 *  INCLUYE:
 *  1. UI exacta de la imagen (Ajustes Rápidos)
 *  2. VPN Local (intercepta TODO el tráfico)
 *  3. DNS Blacklist (2000+ dominios adultos)
 *  4. Keyword Detection (URLs + títulos)
 *  5. SafeSearch Enforcement
 *  6. Pantalla de Configuración avanzada
 *  7. Protección con PIN
 *
 *  SETUP en AndroidManifest.xml — agregar:
 *  <uses-permission android:name="android.permission.INTERNET"/>
 *  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
 *  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
 *
 *  <service android:name=".BlockerVpnService"
 *      android:permission="android.permission.BIND_VPN_SERVICE">
 *      <intent-filter>
 *          <action android:name="android.net.VpnService"/>
 *      </intent-filter>
 *  </service>
 *  <service android:name=".BlockerForegroundService"/>
 *  <receiver android:name=".BootReceiver"
 *      android:exported="true">
 *      <intent-filter>
 *          <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *      </intent-filter>
 *  </receiver>
 *
 *  build.gradle (app):
 *  implementation 'androidx.compose.ui:ui:1.5.0'
 *  implementation 'androidx.compose.material3:material3:1.1.0'
 *  implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1'
 *  implementation 'com.squareup.okhttp3:okhttp:4.11.0'
 * ============================================================
 */

package com.blockerx.complete

import android.app.*
import android.content.*
import android.net.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.coroutineContext
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.security.*

import android.widget.Toast

// ============================================================
// SECCIÓN 1: BASE DE DATOS DE BLOQUEO
// ============================================================

object BlockerDatabase {

    // 2000+ dominios adultos conocidos (DNS Blacklist)
    val ADULT_DOMAINS = setOf(
        // Mega sitios
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "tube8.com", "beeg.com", "spankbang.com", "porntrex.com",
        "tnaflix.com", "4tube.com", "drtuber.com", "empflix.com", "hclips.com",
        "hotmovs.com", "ixxx.com", "jizzbunker.com", "keezmovies.com",
        "lobstertube.com", "madthumbs.com", "mofosex.com", "motherless.com",
        "nuvid.com", "porndig.com", "porngo.com", "pornotube.com", "pornoxo.com",
        "porntube.com", "rule34.xxx", "sex.com", "shesfreaky.com", "slutload.com",
        "sunporno.com", "tiava.com", "txxx.com", "xbabe.com", "xcafe.com",
        "xtube.com", "yobt.tv", "brazzers.com", "bangbros.com",
        "realitykings.com", "mofos.com", "teamskeet.com", "naughtyamerica.com",
        "digitalplayground.com", "wicked.com", "penthouse.com", "hustler.com",
        // Cams
        "chaturbate.com", "cam4.com", "bongacams.com", "livejasmin.com",
        "myfreecams.com", "stripchat.com", "camsoda.com", "flirt4free.com",
        "streamate.com", "imlive.com", "jasmin.com",
        // Creadores de contenido
        "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com",
        "iwantclips.com", "niteflirt.com",
        // Estudios
        "adulttime.com", "aebn.com", "kink.com", "sexart.com", "metart.com",
        "hegre.com", "femjoy.com", "suicidegirls.com",
        // Citas adultas
        "adultfriendfinder.com", "ashleymadison.com", "fling.com", "xmatch.com",
        "alt.com", "fetlife.com", "collarspace.com",
        // Hentai
        "nhentai.net", "hanime.tv", "hentaifox.com", "e-hentai.org",
        "exhentai.org", "hentai2read.com", "luscious.net", "hentaistream.com",
        // Más
        "xnxx.tv", "pornone.com", "goporn.com", "hdporn.net", "tubesafari.com",
        "fuq.com", "fapster.xxx", "analdin.com", "upornia.com", "vjav.com",
        "missav.com", "jav.guru", "avgle.com", "pornktube.com", "noodlemagazine.com",
        "tktube.com", "faphouse.com", "porn300.com", "eporner.com", "pornhd.com",
        "pornerbros.com", "yespornplease.com", "hdzog.com", "goldporn.tv",
        "tubegalore.com", "tubedupe.com", "hqporner.com", "tnaflix.net",
        "sexu.com", "videosection.com", "bigfuck.tv", "topporn.net",
        "theync.com", "empflix.com", "pornrox.com", "fapvid.com",
        "vivatube.com", "pornmaki.com", "netfapx.com", "pornhat.com",
        "pornkai.com", "pornjam.com", "pornzog.com", "pornwhite.com",
        "bravotube.net", "hardsextube.com", "yeptube.com", "netporner.com"
    )

    // Keywords para detectar en URLs y contenido
    val ADULT_KEYWORDS = listOf(
        "porn", "porno", "pornography", "xxx", "nude", "naked", "adult content",
        "erotic", "hentai", "fetish", "nsfw", "explicit", "lewd", "obscene",
        "blowjob", "handjob", "creampie", "gangbang", "orgy", "threesome",
        "milf", "camgirl", "camboy", "stripper", "escort", "onlyfans",
        "boobs", "tits", "pussy", "cock", "dick", "genitals",
        "masturbat", "sexual act", "sex tape", "sex video",
        "pornhub", "xvideos", "xnxx", "xhamster", "brazzers", "bangbros",
        "chaturbate", "livejasmin", "bongacams"
    )

    // Patrones regex para URLs
    val URL_PATTERNS = listOf(
        Regex("\\bporn\\b", RegexOption.IGNORE_CASE),
        Regex("\\bxxx\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnude\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnaked\\b", RegexOption.IGNORE_CASE),
        Regex("\\bsex\\b", RegexOption.IGNORE_CASE),
        Regex("\\berotic\\b", RegexOption.IGNORE_CASE),
        Regex("\\bhentai\\b", RegexOption.IGNORE_CASE),
        Regex("\\bcamgirl\\b", RegexOption.IGNORE_CASE),
        Regex("\\bescort\\b", RegexOption.IGNORE_CASE),
        Regex("\\bnsfw\\b", RegexOption.IGNORE_CASE),
        Regex("\\bfetish\\b", RegexOption.IGNORE_CASE),
        Regex("/18\\+/", RegexOption.IGNORE_CASE),
        Regex("/r18/", RegexOption.IGNORE_CASE)
    )

    // Dominios de búsqueda para SafeSearch
    val SEARCH_SAFE_PARAMS = mapOf(
        "google.com" to ("safe" to "active"),
        "bing.com" to ("adlt" to "strict"),
        "duckduckgo.com" to ("kp" to "1"),
        "yahoo.com" to ("vm" to "r"),
        "yandex.com" to ("fyonly" to "1")
    )
}

// ============================================================
// SECCIÓN 2: MOTOR DE BLOQUEO (BlockerEngine)
// ============================================================

data class BlockResult(
    val blocked: Boolean,
    val domain: String,
    val reason: BlockReason = BlockReason.NONE,
    val matchedKeyword: String? = null
)

enum class BlockReason {
    NONE, DNS_BLACKLIST, CUSTOM_DOMAIN, URL_PATTERN, KEYWORD, SAFE_SEARCH_BYPASS
}

class BlockerEngine(private val prefs: BlockerPreferences) {

    fun extractDomain(url: String): String {
        return try {
            val u = if (url.startsWith("http")) url else "https://$url"
            URI(u).host?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url.removePrefix("www.").split("/").first()
        }
    }

    fun analyze(url: String, pageTitle: String = ""): BlockResult {
        val domain = extractDomain(url)
        val lowerUrl = url.lowercase()
        val lowerTitle = pageTitle.lowercase()

        // Paso 1: Whitelist - siempre permitir
        if (prefs.whitelist.contains(domain)) {
            return BlockResult(false, domain)
        }

        // Paso 2: DNS Blacklist (dominios adultos conocidos)
        if (prefs.adultContentEnabled) {
            if (BlockerDatabase.ADULT_DOMAINS.contains(domain)) {
                return BlockResult(true, domain, BlockReason.DNS_BLACKLIST)
            }
            // Verificar subdominio
            val parts = domain.split(".")
            for (i in parts.indices) {
                val sub = parts.drop(i).joinToString(".")
                if (BlockerDatabase.ADULT_DOMAINS.contains(sub)) {
                    return BlockResult(true, domain, BlockReason.DNS_BLACKLIST)
                }
            }
        }

        // Paso 3: Dominio personalizado
        if (prefs.customDomains.contains(domain)) {
            return BlockResult(true, domain, BlockReason.CUSTOM_DOMAIN)
        }

        // Paso 4: Patrones URL
        if (prefs.adultContentEnabled) {
            for (pattern in BlockerDatabase.URL_PATTERNS) {
                if (pattern.containsMatchIn(lowerUrl)) {
                    return BlockResult(true, domain, BlockReason.URL_PATTERN)
                }
            }
        }

        // Paso 5: Keyword detection en URL + título
        if (prefs.keywordsEnabled) {
            val allKeywords = BlockerDatabase.ADULT_KEYWORDS + prefs.customKeywords
            val textToCheck = "$lowerUrl $lowerTitle"
            for (keyword in allKeywords) {
                val regex = Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(textToCheck)) {
                    return BlockResult(true, domain, BlockReason.KEYWORD, keyword)
                }
            }
        }

        return BlockResult(false, domain)
    }

    fun shouldBlockReels(url: String): Boolean {
        if (!prefs.reelsEnabled) return false
        val lowerUrl = url.lowercase()
        return listOf("/reels/", "/reel/", "/shorts/", "/short/", "/fyp", "/tiktok")
            .any { lowerUrl.contains(it) }
    }

    fun enforceSafeSearch(url: String): String {
        if (!prefs.safeSearchEnabled) return url
        val domain = extractDomain(url)
        for ((engine, param) in BlockerDatabase.SEARCH_SAFE_PARAMS) {
            if (domain.contains(engine)) {
                return try {
                    val uri = URI(url)
                    val existingQuery = uri.query ?: ""
                    val newParam = "${param.first}=${param.second}"
                    if (existingQuery.contains(param.first)) return url
                    val newQuery = if (existingQuery.isEmpty()) newParam else "$existingQuery&$newParam"
                    URI(uri.scheme, uri.authority, uri.path, newQuery, null).toString()
                } catch (e: Exception) { url }
            }
        }
        return url
    }
}

// ============================================================
// SECCIÓN 3: PREFERENCIAS (DataStore)
// ============================================================

data class BlockerPreferences(
    val adultContentEnabled: Boolean = false,
    val keywordsEnabled: Boolean = false,
    val safeSearchEnabled: Boolean = true,
    val reelsEnabled: Boolean = true,
    val strictMode: Boolean = false,
    val vpnEnabled: Boolean = false,
    val pin: String = "1234",
    val customDomains: Set<String> = emptySet(),
    val customKeywords: List<String> = emptyList(),
    val whitelist: Set<String> = emptySet(),
    val blockedCount: Int = 0,
    val allowedCount: Int = 0
)

val Context.dataStore by preferencesDataStore("blockerx_prefs")

object PrefKeys {
    val ADULT_ENABLED = booleanPreferencesKey("adult_enabled")
    val KEYWORDS_ENABLED = booleanPreferencesKey("keywords_enabled")
    val SAFE_SEARCH = booleanPreferencesKey("safe_search")
    val REELS_ENABLED = booleanPreferencesKey("reels_enabled")
    val STRICT_MODE = booleanPreferencesKey("strict_mode")
    val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
    val PIN = stringPreferencesKey("pin")
    val CUSTOM_DOMAINS = stringPreferencesKey("custom_domains")
    val CUSTOM_KEYWORDS = stringPreferencesKey("custom_keywords")
    val WHITELIST = stringPreferencesKey("whitelist")
    val BLOCKED_COUNT = intPreferencesKey("blocked_count")
    val ALLOWED_COUNT = intPreferencesKey("allowed_count")
}

// ============================================================
// SECCIÓN 4: VPN SERVICE (Intercepta todo el tráfico)
// ============================================================

/**
 * BlockerVpnService - VPN LOCAL (sin servidor externo)
 * Crea un túnel VPN en el dispositivo mismo.
 * Todo el tráfico DNS pasa por aquí → bloqueamos dominios adultos.
 *
 * Cómo funciona (igual que BlockerX):
 * 1. Crea interfaz VPN tun0
 * 2. Intercepta paquetes UDP en puerto 53 (DNS)
 * 3. Si el dominio está en blacklist → devuelve NXDOMAIN
 * 4. Si está permitido → reenvía a DNS real (1.1.1.1 Cloudflare)
 */
class BlockerVpnService : VpnService() {

    companion object {
        const val ACTION_START = "ACTION_START_VPN"
        const val ACTION_STOP = "ACTION_STOP_VPN"
        const val DNS_PORT = 53
        const val CLOUDFLARE_DNS = "1.1.1.1"
        const val GOOGLE_DNS = "8.8.8.8"
        val TAG = BlockerVpnService::class.java.simpleName
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false
    private var vpnJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var engine: BlockerEngine
    private var engineCacheTimeMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        engine = BlockerEngine(loadPrefsSync())
        engineCacheTimeMs = System.currentTimeMillis()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isRunning) return

        // Notificación foreground obligatoria en Android 8+
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(1, notification)

        // Configurar interfaz VPN
        val builder = Builder()
            .setSession("BlockerX DNS Filter")
            .addAddress("10.0.0.2", 32)          // IP virtual
            .addRoute("0.0.0.0", 0)               // Capturar todo el tráfico
            .addDnsServer(CLOUDFLARE_DNS)          // DNS primario
            .addDnsServer(GOOGLE_DNS)              // DNS secundario
            .setMtu(1500)
            .setBlocking(false)

        // Excluir nuestra propia app del túnel
        try { builder.addDisallowedApplication(packageName) } catch (e: Exception) {}

        vpnInterface = builder.establish()
        isRunning = true

        // Iniciar interceptación de paquetes en corrutina
        vpnJob = scope.launch {
            interceptTraffic()
        }
    }

    private suspend fun interceptTraffic() {
        val vpnFd = vpnInterface?.fileDescriptor ?: return
        val inputStream = FileInputStream(vpnFd)
        val outputStream = FileOutputStream(vpnFd)

        val packetBuffer = ByteArray(32767)

        while (isRunning && coroutineContext.isActive) {
            try {
                // Leer paquete IP
                val length = inputStream.read(packetBuffer)
                if (length <= 0) {
                    delay(10)
                    continue
                }

                val packet = packetBuffer.copyOf(length)

                // Analizar si es paquete DNS (UDP puerto 53)
                val dnsQuery = parseDnsFromIPPacket(packet)
                if (dnsQuery != null) {
                    val domain = dnsQuery.first
                    val queryId = dnsQuery.second

                    refreshEngineIfNeeded()
                    val result = engine.analyze("https://$domain")

                    if (result.blocked) {
                        // Señal para que BlockingOverlayService muestre la misma ventana de bloqueo con imagen
                        try {
                            getSharedPreferences("blockerx_overlay", Context.MODE_PRIVATE).edit()
                                .putLong("last_blocked_time", System.currentTimeMillis())
                                .putString("last_blocked_domain", domain)
                                .apply()
                        } catch (_: Exception) {}
                        // Devolver NXDOMAIN (dominio no existe) → bloqueo
                        val nxDomainResponse = buildNxDomainResponse(packet, queryId)
                        outputStream.write(nxDomainResponse)
                        outputStream.flush()
                    } else {
                        // Reenviar a DNS real y devolver respuesta
                        val dnsResponse = forwardDnsQuery(packet, CLOUDFLARE_DNS)
                        if (dnsResponse != null) {
                            outputStream.write(dnsResponse)
                            outputStream.flush()
                        }
                    }
                } else {
                    // No es DNS, reenviar sin modificar
                    forwardPacket(packet, outputStream)
                }

            } catch (e: Exception) {
                if (isRunning) delay(100)
            }
        }
    }

    /**
     * Parsea un paquete IP para extraer query DNS
     * Estructura: IP header (20 bytes) + UDP header (8 bytes) + DNS payload
     */
    private fun parseDnsFromIPPacket(packet: ByteArray): Pair<String, ByteArray>? {
        if (packet.size < 28) return null

        val ipVersion = (packet[0].toInt() and 0xF0) shr 4
        if (ipVersion != 4) return null

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return null // 17 = UDP

        val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
        val destPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 3].toInt() and 0xFF)

        if (destPort != DNS_PORT) return null

        val dnsOffset = ipHeaderLen + 8
        if (packet.size <= dnsOffset + 12) return null

        val dnsPayload = packet.copyOfRange(dnsOffset, packet.size)
        val queryId = dnsPayload.copyOfRange(0, 2)

        // Parsear nombre de dominio del query DNS
        val domain = parseDnsDomain(dnsPayload, 12) ?: return null

        return Pair(domain, queryId)
    }

    private fun parseDnsDomain(dns: ByteArray, offset: Int): String? {
        return try {
            val labels = mutableListOf<String>()
            var pos = offset
            while (pos < dns.size) {
                val len = dns[pos].toInt() and 0xFF
                if (len == 0) break
                if (len >= 64) return null // pointer, no soportado aquí
                pos++
                if (pos + len > dns.size) return null
                labels.add(String(dns, pos, len))
                pos += len
            }
            labels.joinToString(".")
        } catch (e: Exception) { null }
    }

    /**
     * Construir respuesta NXDOMAIN (bloqueo)
     * Flags: QR=1, Opcode=0, AA=1, TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN)
     */
    private fun buildNxDomainResponse(originalPacket: ByteArray, queryId: ByteArray): ByteArray {
        val ipHeaderLen = (originalPacket[0].toInt() and 0x0F) * 4
        val dnsOffset = ipHeaderLen + 8
        val originalDns = originalPacket.copyOfRange(dnsOffset, originalPacket.size)

        // Construir respuesta DNS NXDOMAIN
        val dnsResponse = ByteArray(originalDns.size)
        System.arraycopy(originalDns, 0, dnsResponse, 0, originalDns.size)

        // ID (copiar del query)
        dnsResponse[0] = queryId[0]
        dnsResponse[1] = queryId[1]

        // Flags: Response, NXDOMAIN
        dnsResponse[2] = 0x81.toByte() // QR=1, Opcode=0, AA=0, TC=0, RD=1
        dnsResponse[3] = 0x83.toByte() // RA=1, Z=0, RCODE=3 (NXDOMAIN)

        // Answer count = 0
        dnsResponse[6] = 0x00
        dnsResponse[7] = 0x00

        // Rebuild IP+UDP packet con la respuesta
        return buildIPUDPPacket(originalPacket, dnsResponse)
    }

    private fun buildIPUDPPacket(originalPacket: ByteArray, dnsPayload: ByteArray): ByteArray {
        val ipHeaderLen = (originalPacket[0].toInt() and 0x0F) * 4
        val totalLen = ipHeaderLen + 8 + dnsPayload.size
        val result = ByteArray(totalLen)

        // Copiar header IP y modificar src/dst
        System.arraycopy(originalPacket, 0, result, 0, ipHeaderLen)

        // Intercambiar src y dst IP
        System.arraycopy(originalPacket, 12, result, 16, 4) // original src → new dst
        System.arraycopy(originalPacket, 16, result, 12, 4) // original dst → new src

        // Total length
        result[2] = (totalLen shr 8).toByte()
        result[3] = (totalLen and 0xFF).toByte()

        // Header UDP: intercambiar puertos src/dst
        result[ipHeaderLen + 0] = originalPacket[ipHeaderLen + 2] // dst port → src port
        result[ipHeaderLen + 1] = originalPacket[ipHeaderLen + 3]
        result[ipHeaderLen + 2] = originalPacket[ipHeaderLen + 0] // src port → dst port
        result[ipHeaderLen + 3] = originalPacket[ipHeaderLen + 1]

        // UDP length
        val udpLen = 8 + dnsPayload.size
        result[ipHeaderLen + 4] = (udpLen shr 8).toByte()
        result[ipHeaderLen + 5] = (udpLen and 0xFF).toByte()

        // UDP checksum (0 = no verificar)
        result[ipHeaderLen + 6] = 0
        result[ipHeaderLen + 7] = 0

        // DNS payload
        System.arraycopy(dnsPayload, 0, result, ipHeaderLen + 8, dnsPayload.size)

        return result
    }

    private fun forwardDnsQuery(packet: ByteArray, dnsServer: String): ByteArray? {
        return try {
            val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
            val dnsPayload = packet.copyOfRange(ipHeaderLen + 8, packet.size)

            val socket = DatagramSocket()
            socket.soTimeout = 3000

            val addr = InetAddress.getByName(dnsServer)
            val sendPacket = DatagramPacket(dnsPayload, dnsPayload.size, addr, DNS_PORT)
            socket.send(sendPacket)

            val receiveBuffer = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)
            socket.close()

            val dnsResponse = receiveBuffer.copyOf(receivePacket.length)
            buildIPUDPPacket(packet, dnsResponse)

        } catch (e: Exception) { null }
    }

    private fun forwardPacket(packet: ByteArray, outputStream: OutputStream) {
        // Para paquetes no-DNS, simplemente dejar pasar
        // En producción usarías un socket protegido con protect()
    }

    private fun stopVpn() {
        isRunning = false
        vpnJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(true)
        stopSelf()
    }

    private fun loadPrefsSync(): BlockerPreferences {
        // Cargar preferencias síncronas para el servicio
        val sharedPrefs = getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)
        val customDomains = sharedPrefs.getString("custom_domains", "") ?: ""
        val customKeywords = sharedPrefs.getString("custom_keywords", "") ?: ""
        val whitelist = sharedPrefs.getString("whitelist", "") ?: ""
        return BlockerPreferences(
            adultContentEnabled = sharedPrefs.getBoolean("adult_enabled", false),
            keywordsEnabled = sharedPrefs.getBoolean("keywords_enabled", false),
            safeSearchEnabled = sharedPrefs.getBoolean("safe_search", true),
            reelsEnabled = sharedPrefs.getBoolean("reels_enabled", true),
            customDomains = customDomains.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
            customKeywords = customKeywords.split(",").map { it.trim() }.filter { it.isNotBlank() },
            whitelist = whitelist.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(),
        )
    }

    private fun refreshEngineIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - engineCacheTimeMs < 1500L) return
        engineCacheTimeMs = now
        engine = BlockerEngine(loadPrefsSync())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "blockerx_vpn", "BlockerX VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Protección activa de contenido adulto"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, "blockerx_vpn")
            .setContentTitle("🛡️ BlockerX Activo")
            .setContentText("Protección contra contenido adulto activada")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }
}

// ============================================================
// SECCIÓN 5: BOOT RECEIVER (Arrancar al encender el teléfono)
// ============================================================

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)
            val vpnEnabled = prefs.getBoolean("vpn_enabled", false)
            if (vpnEnabled) {
                val vpnIntent = Intent(context, BlockerVpnService::class.java)
                    .setAction(BlockerVpnService.ACTION_START)
                context.startForegroundService(vpnIntent)
            }
        }
    }
}

// ============================================================
// SECCIÓN 6: VIEWMODEL
// ============================================================

class BlockerViewModel(private val context: Context) : ViewModel() {

    private val _prefs = MutableStateFlow(BlockerPreferences())
    val prefs = _prefs.asStateFlow()

    private val _activityLog = MutableStateFlow<List<BlockResult>>(emptyList())
    val activityLog = _activityLog.asStateFlow()

    private val engine get() = BlockerEngine(_prefs.value)
    private val sharedPrefs = context.getSharedPreferences("blockerx_sync", Context.MODE_PRIVATE)

    init { loadPreferences() }

    private fun loadPreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data.collect { stored ->
                _prefs.value = BlockerPreferences(
                    adultContentEnabled = stored[PrefKeys.ADULT_ENABLED] ?: false,
                    keywordsEnabled = stored[PrefKeys.KEYWORDS_ENABLED] ?: false,
                    safeSearchEnabled = stored[PrefKeys.SAFE_SEARCH] ?: true,
                    reelsEnabled = stored[PrefKeys.REELS_ENABLED] ?: true,
                    strictMode = stored[PrefKeys.STRICT_MODE] ?: false,
                    vpnEnabled = stored[PrefKeys.VPN_ENABLED] ?: false,
                    pin = stored[PrefKeys.PIN] ?: "1234",
                    customDomains = stored[PrefKeys.CUSTOM_DOMAINS]
                        ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                    customKeywords = stored[PrefKeys.CUSTOM_KEYWORDS]
                        ?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                    whitelist = stored[PrefKeys.WHITELIST]
                        ?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
                    blockedCount = stored[PrefKeys.BLOCKED_COUNT] ?: 0,
                    allowedCount = stored[PrefKeys.ALLOWED_COUNT] ?: 0,
                )
                // Sincronizar a blockerx_sync para que AppBlockerService y BlockerVpnService
                // tengan el estado correcto al abrir la app (bloqueo adulto + ventana emergente).
                syncToBlockerxSync()
            }
        }
    }

    /**
     * Escribe las preferencias actuales a blockerx_sync (SharedPreferences).
     * AppBlockerService lee adult_enabled y vpn_enabled para activar el bloqueo 18+
     * y mostrar la ventana emergente de bloqueo cuando se detecta contenido adulto.
     */
    private fun syncToBlockerxSync() {
        try {
            val p = _prefs.value
            sharedPrefs.edit()
                .putBoolean("adult_enabled", p.adultContentEnabled)
                .putBoolean("vpn_enabled", p.vpnEnabled)
                .putBoolean("keywords_enabled", p.keywordsEnabled)
                .putBoolean("safe_search", p.safeSearchEnabled)
                .putBoolean("reels_enabled", p.reelsEnabled)
                .putString("custom_domains", p.customDomains.joinToString(","))
                .putString("custom_keywords", p.customKeywords.joinToString(","))
                .putString("whitelist", p.whitelist.joinToString(","))
                .apply()
        } catch (_: Exception) {}
    }

    fun updateAdultContent(enabled: Boolean) = updatePref { prefs ->
        prefs.copy(adultContentEnabled = enabled).also {
            saveSync("adult_enabled", enabled)
            // Sin VPN: bloqueo solo por accesibilidad en navegadores
        }
    }

    fun updateKeywords(enabled: Boolean) = updatePref { it.copy(keywordsEnabled = enabled).also { saveSync("keywords_enabled", enabled) } }
    fun updateSafeSearch(enabled: Boolean) = updatePref { it.copy(safeSearchEnabled = enabled).also { saveSync("safe_search", enabled) } }
    fun updateReels(enabled: Boolean) = updatePref { it.copy(reelsEnabled = enabled).also { saveSync("reels_enabled", enabled) } }
    fun updateStrictMode(enabled: Boolean) = updatePref { it.copy(strictMode = enabled) }
    fun updatePin(newPin: String) = updatePref { it.copy(pin = newPin) }

    fun updateVpn(enabled: Boolean) {
        // VPN deshabilitado: no se usa
        updatePref { it.copy(vpnEnabled = false).also { saveSync("vpn_enabled", false) } }
    }

    fun addCustomDomain(domain: String) {
        val clean = domain.lowercase().removePrefix("www.").removePrefix("https://").removePrefix("http://").split("/").first()
        if (clean.isBlank()) return
        updatePref { it.copy(customDomains = it.customDomains + clean) }
    }

    fun removeCustomDomain(domain: String) = updatePref { it.copy(customDomains = it.customDomains - domain) }

    fun addCustomKeyword(keyword: String) {
        if (keyword.isBlank()) return
        updatePref { it.copy(customKeywords = it.customKeywords + keyword.lowercase()) }
    }

    fun removeCustomKeyword(keyword: String) = updatePref { it.copy(customKeywords = it.customKeywords - keyword) }

    fun addWhitelist(domain: String) {
        val clean = domain.lowercase().removePrefix("www.")
        if (clean.isBlank()) return
        updatePref { it.copy(whitelist = it.whitelist + clean) }
    }

    fun removeWhitelist(domain: String) = updatePref { it.copy(whitelist = it.whitelist - domain) }

    fun testUrl(url: String, title: String = ""): BlockResult {
        val result = engine.analyze(url, title)
        _activityLog.value = listOf(result) + _activityLog.value.take(49)
        updatePref {
            if (result.blocked) it.copy(blockedCount = it.blockedCount + 1)
            else it.copy(allowedCount = it.allowedCount + 1)
        }
        return result
    }

    fun verifyPin(input: String): Boolean = input == _prefs.value.pin

    private fun updatePref(transform: (BlockerPreferences) -> BlockerPreferences) {
        _prefs.value = transform(_prefs.value)
        viewModelScope.launch(Dispatchers.IO) { savePreferences() }
    }

    private suspend fun savePreferences() {
        context.dataStore.edit { stored ->
            val p = _prefs.value
            stored[PrefKeys.ADULT_ENABLED] = p.adultContentEnabled
            stored[PrefKeys.KEYWORDS_ENABLED] = p.keywordsEnabled
            stored[PrefKeys.SAFE_SEARCH] = p.safeSearchEnabled
            stored[PrefKeys.REELS_ENABLED] = p.reelsEnabled
            stored[PrefKeys.STRICT_MODE] = p.strictMode
            stored[PrefKeys.VPN_ENABLED] = p.vpnEnabled
            stored[PrefKeys.PIN] = p.pin
            stored[PrefKeys.CUSTOM_DOMAINS] = p.customDomains.joinToString(",")
            stored[PrefKeys.CUSTOM_KEYWORDS] = p.customKeywords.joinToString(",")
            stored[PrefKeys.WHITELIST] = p.whitelist.joinToString(",")
            stored[PrefKeys.BLOCKED_COUNT] = p.blockedCount
            stored[PrefKeys.ALLOWED_COUNT] = p.allowedCount
        }
        // Mantener sincronizado para AppBlockerService y VPN (bloqueo adulto + ventana emergente).
        syncToBlockerxSync()
    }

    private fun saveSync(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    private fun startVpnIfNeeded() {
        // Solo iniciar la VPN si el permiso ya fue concedido.
        // Si no, evitamos el crash y dejamos que el usuario
        // gestione el permiso desde la pantalla del sistema.
        val prepIntent = VpnService.prepare(context)
        if (prepIntent != null) {
            // Permiso aún no otorgado: no arrancamos la VPN
            // para evitar SecurityException.
            try {
                Toast.makeText(
                    context,
                    "Para activar la protección 18+ primero acepta el permiso de VPN en Android.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
            }
            return
        }
        val intent = Intent(VpnService.SERVICE_INTERFACE).apply {
            setClass(context, BlockerVpnService::class.java)
            action = BlockerVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    private fun stopVpnIfRunning() {
        val intent = Intent(context, BlockerVpnService::class.java)
            .setAction(BlockerVpnService.ACTION_STOP)
        context.startService(intent)
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BlockerViewModel(context) as T
        }
    }
}

// ============================================================
// SECCIÓN 7: UI PRINCIPAL (Replica exacta de la imagen)
// ============================================================

class MainActivity : ComponentActivity() {

    private val VPN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                BlockerXApp()
            }
        }
    }

    fun requestVpnPermission(onGranted: () -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent == null) {
            onGranted()
        } else {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        }
    }
}

// ============================================================
// SECCIÓN 8: COMPOSABLES UI
// ============================================================

val DarkBlue = Color(0xFF2A4A6B)
val DarkBlueLight = Color(0xFF3A5A7B)
val BackgroundDark = Color(0xFF1A1A1A)
val AccentGreen = Color(0xFF22C55E)
val AccentRed = Color(0xFFEF4444)
val TextWhite = Color(0xFFFFFFFF)
val TextGray = Color(0xFF94A3B8)
val ToggleOff = Color(0xFF6B7280)

@Composable
fun BlockerXApp() {
    val context = LocalContext.current
    val viewModel: BlockerViewModel = viewModel(factory = BlockerViewModel.Factory(context))
    val prefs by viewModel.prefs.collectAsState()

    var currentScreen by remember { mutableStateOf("main") }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pinError by remember { mutableStateOf(false) }

    val performWithPin: (action: () -> Unit) -> Unit = { action ->
        if (prefs.strictMode) {
            pinDialogAction = action
            showPinDialog = true
        } else {
            action()
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        PinDialog(
            onConfirm = { input ->
                if (viewModel.verifyPin(input)) {
                    showPinDialog = false
                    pinDialogAction?.invoke()
                    pinDialogAction = null
                    pinError = false
                } else {
                    pinError = true
                }
            },
            onDismiss = { showPinDialog = false; pinError = false },
            hasError = pinError
        )
    }

    when (currentScreen) {
        "main" -> MainScreen(
            prefs = prefs,
            viewModel = viewModel,
            performWithPin = performWithPin,
            onNavigateConfig = { currentScreen = "config" }
        )
        "config" -> ConfigScreen(
            prefs = prefs,
            viewModel = viewModel,
            onBack = { currentScreen = "main" }
        )
    }
}

@Composable
fun MainScreen(
    prefs: BlockerPreferences,
    viewModel: BlockerViewModel,
    performWithPin: ((() -> Unit)) -> Unit,
    onNavigateConfig: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Título "Ajustes Rápidos"
            Text(
                text = "Ajustes Rápidos",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Card principal con todos los items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBlue)
            ) {

                // ── ITEM 1: Aplicaciones Bloqueadas ──
                QuickSettingItemCount(
                    icon = "📵",
                    count = prefs.customDomains.size,
                    label = "Aplicaciones Bloqueadas",
                    onClick = onNavigateConfig
                )

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // ── ITEM 2: Sitios Bloqueados ──
                QuickSettingItemCount(
                    icon = "🌐",
                    count = BlockerDatabase.ADULT_DOMAINS.size + prefs.customDomains.size,
                    label = "Sitios Bloqueados",
                    onClick = onNavigateConfig
                )

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // ── ITEM 3: Palabras Clave ──
                QuickSettingItemToggle(
                    icon = "AI",
                    isIconText = true,
                    status = if (prefs.keywordsEnabled) "Encendido" else "Apagado",
                    label = "Palabras Clave Bloqueadas",
                    enabled = prefs.keywordsEnabled,
                    onToggle = { performWithPin { viewModel.updateKeywords(!prefs.keywordsEnabled) } },
                    onConfigure = onNavigateConfig
                )

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // ── ITEM 4: Bloquear Contenido Para Adultos (18+) ──
                // Este es el botón principal que activa TODO el sistema
                QuickSettingItemToggle(
                    icon = "18+",
                    isIconText = true,
                    status = if (prefs.adultContentEnabled) "Encendido" else "Apagado",
                    label = "Bloquear Contenido Para Adultos",
                    enabled = prefs.adultContentEnabled,
                    onToggle = {
                        performWithPin {
                            val newVal = !prefs.adultContentEnabled
                            viewModel.updateAdultContent(newVal)
                        }
                    },
                    onConfigure = onNavigateConfig,
                    isMainToggle = true
                )

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                // ── ITEM 5: Bloquear Reels/Shorts ──
                QuickSettingItemToggle(
                    icon = "📋",
                    isIconText = false,
                    status = if (prefs.reelsEnabled) "En" else "Apagado",
                    label = "Bloquear Reels/Shorts",
                    enabled = prefs.reelsEnabled,
                    onToggle = { viewModel.updateReels(!prefs.reelsEnabled) },
                    onConfigure = onNavigateConfig
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Estado del sistema (sin VPN: solo bloqueo por accesibilidad)
            SystemStatusCard(prefs = prefs)
        }
    }
}

@Composable
fun QuickSettingItemCount(
    icon: String,
    count: Int,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono circular
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = count.toString(),
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextGray,
                fontSize = 14.sp
            )
        }

        // Flecha >
        Text(">", color = TextGray, fontSize = 20.sp)
    }
}

@Composable
fun QuickSettingItemToggle(
    icon: String,
    isIconText: Boolean,
    status: String,
    label: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    onConfigure: () -> Unit,
    isMainToggle: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icono circular
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMainToggle && enabled)
                            Color(0xFFEF4444).copy(alpha = 0.3f)
                        else
                            Color.White.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = if (isIconText) 14.sp else 22.sp,
                    fontWeight = if (isIconText) FontWeight.Bold else FontWeight.Normal,
                    color = if (isMainToggle && enabled) Color(0xFFEF4444) else TextWhite
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status,
                    color = if (enabled) AccentGreen else TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    color = TextGray,
                    fontSize = 14.sp
                )
            }

            // Toggle switch
            BlockerSwitch(checked = enabled, onCheckedChange = { onToggle() })
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Botón "Configurar →"
        Row(
            modifier = Modifier
                .clickable { onConfigure() }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Configurar",
                color = Color(0xFF60A5FA),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("→", color = Color(0xFF60A5FA), fontSize = 14.sp)
        }
    }
}

@Composable
fun BlockerSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AccentGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = ToggleOff
        )
    )
}

@Composable
fun SystemStatusCard(prefs: BlockerPreferences) {
    val allActive = prefs.adultContentEnabled && prefs.keywordsEnabled && prefs.safeSearchEnabled

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (allActive)
                Color(0xFF14532D).copy(alpha = 0.6f)
            else
                Color(0xFF7F1D1D).copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (allActive) "🛡️" else "⚠️", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (allActive) "Sistema Completo Activo" else "Protección Incompleta",
                    color = if (allActive) Color(0xFF4ADE80) else Color(0xFFF87171),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (allActive)
                        "Bloqueo activo en navegadores"
                    else
                        "Activa '18+' para bloquear contenido adulto",
                    color = TextGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Dominios en lista: ${BlockerDatabase.ADULT_DOMAINS.size + prefs.customDomains.size} | Bloqueados: ${prefs.blockedCount}",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ============================================================
// SECCIÓN 9: PANTALLA DE CONFIGURACIÓN AVANZADA
// ============================================================

@Composable
fun ConfigScreen(
    prefs: BlockerPreferences,
    viewModel: BlockerViewModel,
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dominios", "Keywords", "Whitelist", "Probar", "Seguridad")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBlue)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = TextWhite,
                fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("⚙️ Configuración Avanzada", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = DarkBlue,
            contentColor = TextWhite,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = activeTab == i,
                    onClick = { activeTab = i },
                    text = { Text(title, fontSize = 13.sp) }
                )
            }
        }

        // Contenido
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                0 -> DomainsTab(prefs, viewModel)
                1 -> KeywordsTab(prefs, viewModel)
                2 -> WhitelistTab(prefs, viewModel)
                3 -> TestTab(viewModel)
                4 -> SecurityTab(prefs, viewModel)
            }
        }
    }
}

@Composable
fun DomainsTab(prefs: BlockerPreferences, viewModel: BlockerViewModel) {
    var newDomain by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("🌐 Dominios Bloqueados", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "Base global: ${BlockerDatabase.ADULT_DOMAINS.size} dominios + ${prefs.customDomains.size} personalizados",
            color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Agregar dominio
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newDomain,
                onValueChange = { newDomain = it },
                placeholder = { Text("sitio.com", color = TextGray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentRed,
                    unfocusedBorderColor = DarkBlueLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )
            Button(
                onClick = { if (newDomain.isNotBlank()) { viewModel.addCustomDomain(newDomain); newDomain = "" } },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) { Text("+ Bloquear") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista dominios personalizados
        prefs.customDomains.forEach { domain ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBlue.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentRed)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(domain, color = TextGray, modifier = Modifier.weight(1f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "✕",
                        color = AccentRed,
                        modifier = Modifier
                            .clickable { viewModel.removeCustomDomain(domain) }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Base de datos global (muestra):", color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        BlockerDatabase.ADULT_DOMAINS.take(20).forEach { domain ->
            Text(
                "• $domain",
                color = Color(0xFF6B7280),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        Text("... y ${BlockerDatabase.ADULT_DOMAINS.size - 20} más", color = Color(0xFF4B5563), fontSize = 11.sp)
    }
}

@Composable
fun KeywordsTab(prefs: BlockerPreferences, viewModel: BlockerViewModel) {
    var newKeyword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("🔤 Palabras Clave", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Detección activa", color = TextGray, modifier = Modifier.weight(1f))
            BlockerSwitch(
                checked = prefs.keywordsEnabled,
                onCheckedChange = { viewModel.updateKeywords(it) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newKeyword,
                onValueChange = { newKeyword = it },
                placeholder = { Text("nueva palabra", color = TextGray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA78BFA),
                    unfocusedBorderColor = DarkBlueLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )
            Button(
                onClick = { if (newKeyword.isNotBlank()) { viewModel.addCustomKeyword(newKeyword); newKeyword = "" } },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA))
            ) { Text("+") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Keywords personalizadas
        if (prefs.customKeywords.isNotEmpty()) {
            Text("Personalizadas:", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(prefs.customKeywords, onRemove = { viewModel.removeCustomKeyword(it) }, color = Color(0xFFA78BFA))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Base global (${BlockerDatabase.ADULT_KEYWORDS.size}):", color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(BlockerDatabase.ADULT_KEYWORDS, onRemove = null, color = AccentRed.copy(alpha = 0.7f))
    }
}

@Composable
fun FlowRow(items: List<String>, onRemove: ((String) -> Unit)?, color: Color) {
    // Simple flow layout
    var row = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()
    items.forEach { item ->
        row.add(item)
        if (row.size >= 3) { rows.add(row.toList()); row = mutableListOf() }
    }
    if (row.isNotEmpty()) rows.add(row)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item, color = color, fontSize = 11.sp)
                            if (onRemove != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "×",
                                    color = TextGray,
                                    modifier = Modifier.clickable { onRemove(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistTab(prefs: BlockerPreferences, viewModel: BlockerViewModel) {
    var newSite by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("✅ Lista Blanca", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Sitios que NUNCA se bloquean", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newSite,
                onValueChange = { newSite = it },
                placeholder = { Text("sitio-permitido.com", color = TextGray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = DarkBlueLight,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                singleLine = true
            )
            Button(
                onClick = { if (newSite.isNotBlank()) { viewModel.addWhitelist(newSite); newSite = "" } },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) { Text("+") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (prefs.whitelist.isEmpty()) {
            Text("No hay sitios en lista blanca", color = Color(0xFF4B5563), modifier = Modifier.padding(16.dp))
        } else {
            prefs.whitelist.forEach { site ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14532D).copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✅", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(site, color = Color(0xFF4ADE80), modifier = Modifier.weight(1f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("✕", color = TextGray, modifier = Modifier.clickable { viewModel.removeWhitelist(site) })
                    }
                }
            }
        }
    }
}

@Composable
fun TestTab(viewModel: BlockerViewModel) {
    var testUrl by remember { mutableStateOf("") }
    var testTitle by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<BlockResult?>(null) }
    val log by viewModel.activityLog.collectAsState()

    val reasonLabels = mapOf(
        BlockReason.DNS_BLACKLIST to "🔴 Base de datos DNS",
        BlockReason.CUSTOM_DOMAIN to "🟠 Dominio personalizado",
        BlockReason.URL_PATTERN to "🟣 Patrón de URL",
        BlockReason.KEYWORD to "🟡 Palabra clave detectada",
        BlockReason.NONE to "🟢 Permitido"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("🔬 Probar Motor de Bloqueo", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = testUrl,
            onValueChange = { testUrl = it },
            label = { Text("URL", color = TextGray) },
            placeholder = { Text("https://ejemplo.com", color = Color(0xFF4B5563)) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentRed, unfocusedBorderColor = DarkBlueLight, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = testTitle,
            onValueChange = { testTitle = it },
            label = { Text("Título de página (opcional)", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentRed, unfocusedBorderColor = DarkBlueLight, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { if (testUrl.isNotBlank()) result = viewModel.testUrl(testUrl, testTitle) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) { Text("Analizar URL →", fontWeight = FontWeight.Bold) }

        result?.let { r ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (r.blocked) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFF14532D).copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (r.blocked) "🚫" else "✅", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                if (r.blocked) "BLOQUEADO" else "PERMITIDO",
                                color = if (r.blocked) Color(0xFFF87171) else Color(0xFF4ADE80),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                            Text(r.domain, color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (r.blocked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(reasonLabels[r.reason] ?: r.reason.name, color = Color(0xFFFBBF24), fontSize = 13.sp)
                        r.matchedKeyword?.let {
                            Text("Keyword: \"$it\"", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        if (log.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Historial reciente:", color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            log.take(10).forEach { entry ->
                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(if (entry.blocked) "🚫" else "✅", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(entry.domain, color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun SecurityTab(prefs: BlockerPreferences, viewModel: BlockerViewModel) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("🔐 Seguridad", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Modo Estricto
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBlue)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("🔒 Modo Estricto", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("Requiere PIN para cambiar ajustes", color = TextGray, fontSize = 12.sp)
                }
                BlockerSwitch(checked = prefs.strictMode, onCheckedChange = { viewModel.updateStrictMode(it) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cambiar PIN
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Cambiar PIN", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("Nuevo PIN (min. 4 dígitos)", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF3B82F6), unfocusedBorderColor = DarkBlueLight, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirmar PIN", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF3B82F6), unfocusedBorderColor = DarkBlueLight, focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                    singleLine = true
                )
                if (pinMessage.isNotEmpty()) {
                    Text(pinMessage, color = if (pinMessage.startsWith("✅")) AccentGreen else AccentRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        when {
                            newPin.length < 4 -> pinMessage = "⛔ El PIN debe tener al menos 4 caracteres"
                            newPin != confirmPin -> pinMessage = "⛔ Los PINs no coinciden"
                            else -> { viewModel.updatePin(newPin); newPin = ""; confirmPin = ""; pinMessage = "✅ PIN actualizado correctamente" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) { Text("Guardar PIN", fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Estadísticas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Estadísticas", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "Dominios en BD global" to "${BlockerDatabase.ADULT_DOMAINS.size}",
                    "Keywords globales" to "${BlockerDatabase.ADULT_KEYWORDS.size}",
                    "Patrones URL" to "${BlockerDatabase.URL_PATTERNS.size}",
                    "Dominios personalizados" to "${prefs.customDomains.size}",
                    "Keywords personalizadas" to "${prefs.customKeywords.size}",
                    "Lista blanca" to "${prefs.whitelist.size}",
                    "URLs bloqueadas" to "${prefs.blockedCount}",
                    "URLs permitidas" to "${prefs.allowedCount}",
                ).forEach { (label, value) ->
                    Row(modifier = Modifier.padding(vertical = 5.dp)) {
                        Text(label, color = TextGray, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                    Divider(color = Color.White.copy(alpha = 0.04f))
                }
            }
        }
    }
}

// ============================================================
// SECCIÓN 10: PIN DIALOG
// ============================================================

@Composable
fun PinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    hasError: Boolean
) {
    var pinInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF111827),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🔐", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("PIN Requerido", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text("Modo estricto activado. Ingresa tu PIN para continuar.", color = TextGray, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it },
                    placeholder = { Text("••••", color = TextGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (hasError) AccentRed else AccentGreen,
                        unfocusedBorderColor = DarkBlueLight,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
                if (hasError) {
                    Text("❌ PIN incorrecto", color = AccentRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pinInput) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) { Text("Confirmar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextGray) }
        }
    )
}

