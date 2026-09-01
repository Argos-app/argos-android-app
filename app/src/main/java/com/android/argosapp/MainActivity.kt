package com.android.argosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.ui.navigation.ArgosNavHost
import com.android.argosapp.ui.theme.ArgosappTheme

class MainActivity : ComponentActivity() {

    private val firebaseAuthManager by lazy {
        FirebaseAuthManager()
    }
    private val argosApiAuthManager by lazy {
        MockedArgosApiAuthManager(
            firstAccess = true // força fluxo de primeiro acesso
        )
    }
    private val tokenStorage by lazy {
        UserSessionStorage(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArgosappTheme {
                ArgosNavHost(
                    firebaseAuthManager = firebaseAuthManager,
                    argosApiAuthManager = argosApiAuthManager,
                    tokenStorage = tokenStorage,
                )
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && !tokenStorage.isRememberMeEnabled()) {
            tokenStorage.clearSession()
        }

        super.onDestroy()
    }
}
