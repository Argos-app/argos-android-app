package com.android.argosapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.ui.navigation.route.FirstAccessChangePasswordRoute
import com.android.argosapp.ui.navigation.route.LoginRoute
import com.android.argosapp.ui.navigation.route.RedefinePasswordRoute
import com.android.argosapp.ui.screen.HomeScreen

private object AppRoute {
    const val Login = "login"
    const val FirstAccessChangePassword = "first_access_change_password"
    const val RedefinePassword = "redefine_password"
    const val Home = "home"
}

@Composable
fun ArgosNavHost(
    firebaseAuthManager: FirebaseAuthManager,
    argosApiAuthManager: MockedArgosApiAuthManager,
    tokenStorage: UserSessionStorage,
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Login,
        modifier = modifier,
    ) {
        composable(AppRoute.Login) {
            LoginRoute(
                firebaseAuthManager = firebaseAuthManager,
                argosApiAuthManager = argosApiAuthManager,
                tokenStorage = tokenStorage,
                onNavigateToHome = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.Login) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToFirstAccess = {
                    navController.navigate(AppRoute.FirstAccessChangePassword) {
                        popUpTo(AppRoute.Login) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToRedefinePassword = {
                    navController.navigate(AppRoute.RedefinePassword) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoute.RedefinePassword) {
            RedefinePasswordRoute(
                firebaseAuthManager = firebaseAuthManager,
                onNavigateToLogin = {
                    navController.popBackStack(AppRoute.Login, inclusive = false)
                },
            )
        }

        composable(AppRoute.FirstAccessChangePassword) {
            FirstAccessChangePasswordRoute(
                firebaseAuthManager = firebaseAuthManager,
                argosApiAuthManager = argosApiAuthManager,
                tokenStorage = tokenStorage,
                onNavigateToHome = {
                    navController.navigate(AppRoute.Home) {
                        popUpTo(AppRoute.FirstAccessChangePassword) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    tokenStorage.clearSession()
                    firebaseAuthManager.signOut()
                    navController.navigate(AppRoute.Login) {
                        popUpTo(AppRoute.FirstAccessChangePassword) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(AppRoute.Home) {
            HomeScreen(
                user = tokenStorage.getUser(),
                bearerToken = tokenStorage.getBearerToken(),
            )
        }
    }
}
