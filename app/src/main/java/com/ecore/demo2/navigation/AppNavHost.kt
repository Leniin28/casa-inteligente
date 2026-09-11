package com.ecore.demo2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.ecore.demo2.feature.alerts.AlertsRoute
import com.ecore.demo2.feature.assistant.AssistantRoute
import com.ecore.demo2.feature.auth.forgot.ForgotPasswordRoute
import com.ecore.demo2.feature.auth.login.LoginRoute
import com.ecore.demo2.feature.auth.profile.ProfileRoute
import com.ecore.demo2.feature.auth.register.RegisterRoute
import com.ecore.demo2.feature.auth.splash.SplashScreen
import com.ecore.demo2.feature.budget.BudgetRoute
import com.ecore.demo2.feature.dashboard.DashboardRoute
import com.ecore.demo2.feature.devices.DevicesRoute
import com.ecore.demo2.feature.electricity.ElectricityRoute
import com.ecore.demo2.feature.history.HistoryRoute
import com.ecore.demo2.feature.menu.DataMenuScreen
import com.ecore.demo2.feature.menu.MoreMenuScreen
import com.ecore.demo2.feature.settings.HouseSettingsRoute
import com.ecore.demo2.feature.settings.NotificationSettingsRoute
import com.ecore.demo2.feature.settings.SettingsRoute
import com.ecore.demo2.feature.settings.about.AboutRoute
import com.ecore.demo2.feature.settings.status.SystemStatusRoute
import com.ecore.demo2.feature.water.WaterRoute

/** Grafo de navegación completo. Las pantallas solo reciben lambdas, nunca el NavController. */
@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val back: () -> Unit = { navController.popBackStack() }

    NavHost(navController = navController, startDestination = Routes.Splash, modifier = modifier) {
        composable<Routes.Splash> { SplashScreen() }

        composable<Routes.Login> {
            LoginRoute(
                onRegister = { navController.navigate(Routes.Register) },
                onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable<Routes.Register> { RegisterRoute(onBack = back) }
        composable<Routes.ForgotPassword> { ForgotPasswordRoute(onBack = back) }

        navigation<Routes.HomeGraph>(startDestination = Routes.Dashboard) {
            composable<Routes.Dashboard> {
                DashboardRoute(
                    onOpenElectricity = { navController.navigate(Routes.Electricity) },
                    onOpenWater = { navController.navigate(Routes.Water) },
                    onOpenDevices = { navController.navigate(Routes.Devices) },
                    onOpenAlerts = { navController.navigate(Routes.Alerts) },
                    onOpenBudgets = { navController.navigate(Routes.Budgets) },
                    onOpenSystemStatus = { navController.navigate(Routes.SystemStatus) },
                )
            }
        }

        navigation<Routes.DataGraph>(startDestination = Routes.DataMenu) {
            composable<Routes.DataMenu> {
                DataMenuScreen(
                    onOpenElectricity = { navController.navigate(Routes.Electricity) },
                    onOpenWater = { navController.navigate(Routes.Water) },
                    onOpenDevices = { navController.navigate(Routes.Devices) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                )
            }
            composable<Routes.Electricity> {
                ElectricityRoute(onOpenHistory = { navController.navigate(Routes.History) }, onBack = back)
            }
            composable<Routes.Water> {
                WaterRoute(onOpenHistory = { navController.navigate(Routes.History) }, onBack = back)
            }
            composable<Routes.Devices> { DevicesRoute(onBack = back) }
            composable<Routes.History> { HistoryRoute(onBack = back) }
        }

        navigation<Routes.AssistantGraph>(startDestination = Routes.Assistant) {
            composable<Routes.Assistant> { AssistantRoute() }
        }

        navigation<Routes.AlertsGraph>(startDestination = Routes.Alerts) {
            composable<Routes.Alerts> { AlertsRoute() }
        }

        navigation<Routes.MoreGraph>(startDestination = Routes.MoreMenu) {
            composable<Routes.MoreMenu> {
                MoreMenuScreen(
                    onOpenBudgets = { navController.navigate(Routes.Budgets) },
                    onOpenProfile = { navController.navigate(Routes.Profile) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenSystemStatus = { navController.navigate(Routes.SystemStatus) },
                    onOpenAbout = { navController.navigate(Routes.About) },
                )
            }
            composable<Routes.Budgets> { BudgetRoute(onBack = back) }
            composable<Routes.Profile> { ProfileRoute(onBack = back) }
            composable<Routes.Settings> {
                SettingsRoute(
                    onOpenHouseSettings = { navController.navigate(Routes.HouseSettings) },
                    onOpenNotificationSettings = { navController.navigate(Routes.NotificationSettings) },
                    onBack = back,
                )
            }
            composable<Routes.HouseSettings> { HouseSettingsRoute(onBack = back) }
            composable<Routes.NotificationSettings> { NotificationSettingsRoute(onBack = back) }
            composable<Routes.SystemStatus> { SystemStatusRoute(onBack = back) }
            composable<Routes.About> { AboutRoute(onBack = back) }
        }
    }
}
