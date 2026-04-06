package com.example.bloqueo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.navigation.AppNavigation
import com.example.bloqueo.ui.theme.BloqueoTheme
import com.example.bloqueo.ui.theme.BackgroundDark
import com.example.bloqueo.util.PermissionHelper
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private fun seedDataIfEmpty(repository: AppRepository) {
    val data = repository.getAppData()
    if (data.estadisticasUso.isEmpty()) {
        listOf("Instagram" to 45, "YouTube" to 120, "WhatsApp" to 30, "Facebook" to 25, "TikTok" to 90)
            .forEach { (app, mins) -> repository.agregarEstadistica(app, mins) }
    }
}

class MainActivity : ComponentActivity() {

    var strictModeQrScanCallback: ((String) -> Unit)? = null

    fun clearStrictModeQrScanCallback() {
        strictModeQrScanCallback = null
    }

    fun startQRScanForStrictMode() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Escanea el código QR para desactivar modo estricto")
            setBeepEnabled(true)
            setBarcodeImageEnabled(false)
        }
        qrScanLauncher.launch(options)
    }

    private val qrScanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.let { contents ->
            strictModeQrScanCallback?.invoke(contents)
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Resultado ignorado: el usuario puede conceder desde Configuración */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !PermissionHelper.hasNotificationPermission(applicationContext)) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val repository = AppRepository(applicationContext)
        seedDataIfEmpty(repository)
        setContent {
            BloqueoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AppNavigation(
                        repository = repository,
                        context = this
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
