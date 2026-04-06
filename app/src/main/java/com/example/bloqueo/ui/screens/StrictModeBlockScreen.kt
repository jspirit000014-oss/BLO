package com.example.bloqueo.ui.screens

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bloqueo.MainActivity
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.theme.*

/**
 * Pantalla que se muestra cuando el modo estricto está activo.
 * Muestra el estado bloqueado y permite desactivar escaneando el código QR.
 */
@Composable
fun StrictModeBlockScreen(
    repository: AppRepository,
    context: Context,
    onStrictModeDeactivated: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mainActivity = context as? MainActivity

    LaunchedEffect(Unit) {
        mainActivity?.let { activity ->
            activity.strictModeQrScanCallback = { scannedContent ->
                try {
                    val data = repository.getAppData()
                    val token = data.configuracion.estrictoQrToken
                    if (!token.isNullOrBlank() && scannedContent.trim() == token.trim()) {
                        repository.actualizarConfiguracion {
                            it.copy(modoActual = "normal", modoEstricto = false, estrictoQrToken = null)
                        }
                        errorMessage = null
                        onStrictModeDeactivated()
                    } else {
                        errorMessage = "Código QR incorrecto. Escanea el QR que generaste al activar el modo estricto."
                    }
                } catch (_: Exception) {
                    errorMessage = "Error al verificar. Inténtalo de nuevo."
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mainActivity?.clearStrictModeQrScanCallback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp)
            .statusBarsPadding()
    ) {
        // Cabecera: Modo estricto habilitado (y volver si se abrió desde Desactivar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                }
            }
            Text(
                text = "📱",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                "Modo estricto habilitado",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        // Tarjeta: Bloqueado / Restringir Todo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BlockedCardBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Bloqueado",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Restringir Todo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sección Código QR (única forma de desactivar)
        Text(
            "Código QR",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Escanea el código QR para desactivar. Es la única forma de volver al modo normal.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                errorMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = RedStrict,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        // Botón Desactivar
        Button(
            onClick = {
                errorMessage = null
                mainActivity?.startQRScanForStrictMode()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "Desactivar",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
