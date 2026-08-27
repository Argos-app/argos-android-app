package com.android.argosapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.ui.navigation.routes.LoginRoute

private object AppRoute {
    const val Login = "login"
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
            )
        }

        composable(AppRoute.Home) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        }
    }
}
