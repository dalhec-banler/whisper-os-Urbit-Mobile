package io.nativeplanet.launcher.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.nativeplanet.launcher.domain.IdentityMode
import io.nativeplanet.launcher.ui.home.RuntimeStatusScreen
import io.nativeplanet.launcher.ui.onboarding.CometScreen
import io.nativeplanet.launcher.ui.onboarding.ImportScreen
import io.nativeplanet.launcher.ui.onboarding.OnboardingViewModel
import io.nativeplanet.launcher.ui.onboarding.PairScreen
import io.nativeplanet.launcher.ui.onboarding.RevealScreen
import io.nativeplanet.launcher.ui.onboarding.WelcomeScreen
import io.nativeplanet.launcher.ui.settings.IdentitySettingsScreen

object Routes {
    const val WELCOME = "welcome"
    const val PAIR = "pair"
    const val IMPORT = "import"
    const val COMET = "comet"
    const val REVEAL = "reveal/{shipName}/{parentName}/{identityMode}"
    const val HOME = "home"
    const val SETTINGS_IDENTITY = "settings/identity"

    fun reveal(shipName: String, parentName: String?, mode: IdentityMode) =
        "reveal/${shipName}/${parentName ?: "none"}/${mode.name}"
}

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onPairWithPlanet = { navController.navigate(Routes.PAIR) },
                onImportShip = { navController.navigate(Routes.IMPORT) },
                onStartComet = { navController.navigate(Routes.COMET) }
            )
        }

        composable(Routes.IMPORT) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            ImportScreen(
                onImportComplete = { shipName, parentName ->
                    navController.navigate(Routes.reveal(shipName, parentName, IdentityMode.IMPORTED))
                },
                onBack = { navController.popBackStack() },
                onProvisionMoon = viewModel::provisionMoon
            )
        }

        composable(Routes.COMET) {
            CometScreen(
                onBootComplete = { cometName ->
                    navController.navigate(Routes.reveal(cometName, null, IdentityMode.COMET))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PAIR) {
            val viewModel: OnboardingViewModel = hiltViewModel()
            PairScreen(
                onPairWithPlanet = viewModel::pairWithPlanet,
                onPairComplete = { shipName, parentName ->
                    navController.navigate(Routes.reveal(shipName, parentName, IdentityMode.PAIRED_MOON))
                },
                onImportManually = { navController.navigate(Routes.IMPORT) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.REVEAL) { backStackEntry ->
            val viewModel: OnboardingViewModel = hiltViewModel()
            val shipName = backStackEntry.arguments?.getString("shipName") ?: "~zod"
            val parentName = backStackEntry.arguments?.getString("parentName")?.takeIf { it != "none" && it.isNotEmpty() }
            val identityMode = backStackEntry.arguments?.getString("identityMode")?.let {
                try { IdentityMode.valueOf(it) } catch (e: Exception) { IdentityMode.NONE }
            } ?: IdentityMode.NONE

            RevealScreen(
                shipName = shipName,
                parentName = parentName,
                onContinue = {
                    when (identityMode) {
                        IdentityMode.COMET -> viewModel.completeComet(shipName)
                        IdentityMode.PAIRED_MOON -> viewModel.completePairing(shipName, parentName ?: "")
                        IdentityMode.IMPORTED -> viewModel.completeImport(shipName, parentName)
                        else -> {}
                    }
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            RuntimeStatusScreen(
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS_IDENTITY) },
                onNavigateToOnboarding = { navController.navigate(Routes.WELCOME) }
            )
        }

        composable(Routes.SETTINGS_IDENTITY) {
            IdentitySettingsScreen(
                onBack = { navController.popBackStack() },
                onAddIdentity = { navController.navigate(Routes.WELCOME) }
            )
        }
    }
}
