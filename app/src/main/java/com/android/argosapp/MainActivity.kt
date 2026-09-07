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
import com.android.argosapp.auth.manager.MockedArgosApiAuthManager
import com.android.argosapp.auth.session.UserSessionStorage
import com.android.argosapp.ui.navigation.ArgosNavHost
import com.android.argosapp.ui.theme.ArgosappTheme

class MainActivity : ComponentActivity() {

    private var passwordResetCode by mutableStateOf<String?>(null)

    private val firebaseAuthManager by lazy {
        FirebaseAuthManager()
    }
    private val argosApiAuthManager by lazy {
        MockedArgosApiAuthManager()
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
                    tokenStorage = tokenStorage,
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
        if (!isChangingConfigurations && !tokenStorage.isRememberMeEnabled()) {
            tokenStorage.clearSession()
        }

        super.onDestroy()
    }
}
