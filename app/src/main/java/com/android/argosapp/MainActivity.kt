package com.android.argosapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.argosapp.auth.manager.FirebaseAuthManager
import com.android.argosapp.auth.manager.ArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.data.api.FirebaseRepository
import com.android.argosapp.ui.navigation.ArgosNavHost
import com.android.argosapp.ui.theme.ArgosappTheme

class MainActivity : ComponentActivity() {

    private var passwordResetCode by mutableStateOf<String?>(null)

    private val firebaseAuthManager by lazy {
        FirebaseAuthManager(FirebaseRepository())
    }
    private val argosApiAuthManager by lazy {
        ArgosApiAuthManager(
            argosApiRepository = ArgosApiRepository(homologation = true),
            userSessionStorage = tokenStorage,
        )
    }
    private val tokenStorage by lazy {
        UserSessionStorage(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        passwordResetCode = firebaseAuthManager.extractPasswordResetCode(intent)
        enableEdgeToEdge()

        setContent {
            ArgosappTheme {
                ArgosNavHost(
                    firebaseAuthManager = firebaseAuthManager,
                    argosApiAuthManager = argosApiAuthManager,
                    passwordResetCode = passwordResetCode,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        passwordResetCode = firebaseAuthManager.extractPasswordResetCode(intent)
    }

    override fun onDestroy() {
        if (!isChangingConfigurations && !argosApiAuthManager.isRememberMeEnabled()) {
            argosApiAuthManager.clearSession()
        }

        super.onDestroy()
    }
}
