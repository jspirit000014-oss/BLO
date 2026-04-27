package com.example.bloqueo.ui.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bloqueo.data.AppRepository
import com.example.bloqueo.ui.screens.*
import com.blockerx.complete.BlockerXApp

sealed class Screen(val route: String) {
    object Inicio : Screen("inicio")
    object Bloqueo : Screen("bloqueo")
    object Horarios : Screen("horarios")
    object Estadisticas : Screen("estadisticas")
    object Configuracion : Screen("configuracion")
    object ActivarModoEstricto : Screen("activar_modo_estricto")
    object DesactivarModoEstricto : Screen("desactivar_modo_estricto")
    object BlockerX : Screen("blockerx")
    object PerfilDetail : Screen("perfil/{profileId}") {
        fun route(profileId: String) = "perfil/$profileId"
    }
}

@Composable
fun AppNavigation(
    repository: AppRepository,
    context: Context,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Inicio.route
    ) {
        composable(Screen.Inicio.route) {
            InicioScreen(
                repository = repository,
                context = context,
                onNavigateBloqueo = { navController.navigate(Screen.Bloqueo.route) },
                onNavigateHorarios = { navController.navigate(Screen.Horarios.route) },
                onNavigateEstadisticas = { navController.navigate(Screen.Estadisticas.route) },
                onNavigateConfiguracion = { navController.navigate(Screen.Configuracion.route) },
                onNavigateModoEstricto = { navController.navigate(Screen.ActivarModoEstricto.route) },
                onNavigateDesactivarModoEstricto = { navController.navigate(Screen.DesactivarModoEstricto.route) },
                onNavigatePerfil = { id -> navController.navigate(Screen.PerfilDetail.route(id)) },
                onNavigateBlockerX = { navController.navigate(Screen.BlockerX.route) }
            )
        }
        composable(Screen.DesactivarModoEstricto.route) {
            StrictModeBlockScreen(
                repository = repository,
                context = context,
                onStrictModeDeactivated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ActivarModoEstricto.route) {
            ActivarModoEstrictoScreen(
                repository = repository,
                context = context,
                onBack = { navController.popBackStack() },
                onActivated = { navController.popBackStack() }
            )
        }
        composable(Screen.Bloqueo.route) {
            BloqueoScreen(
                repository = repository,
                context = context,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Horarios.route) {
            HorariosScreen(
                repository = repository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Estadisticas.route) {
            EstadisticasScreen(
                repository = repository,
                context = context,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(
                repository = repository,
                context = context,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.BlockerX.route) {
            ContenidoAdultoScreen(
                repository = repository
            )
        }
        composable(Screen.PerfilDetail.route) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            if (profileId.isNotEmpty()) {
                PerfilDetailScreen(
                    repository = repository,
                    context = context,
                    profileId = profileId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
