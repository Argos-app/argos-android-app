package com.android.argosapp

import android.app.Application
import com.android.argosapp.data.repository.ArgosApiRepository
import com.android.argosapp.data.repository.AiChatApiRepository
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.data.repository.MockedArgosApiRepository
import com.android.argosapp.data.repository.RemoteArgosApiRepository
import com.android.argosapp.util.logging.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class ArgosApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val runMode: ArgosRunMode = ArgosRunMode.RUN_MOCKED // Aqui foi setado explicitamente para run em etapa de desenvolvimento, caso queria mude o modo de run aqui

    val argosApiRepository: ArgosApiRepository by lazy {
        when (runMode) {
            ArgosRunMode.RUN_HOMOLOGATION -> RemoteArgosApiRepository(homologation = true)
            ArgosRunMode.RUN_PRODUCTION -> RemoteArgosApiRepository(homologation = false)
            ArgosRunMode.RUN_MOCKED -> MockedArgosApiRepository()
            ArgosRunMode.RUN_MOCKED_WITH_FIRST_ACCESS -> MockedArgosApiRepository(firstAccess = true)
            ArgosRunMode.RUN_MOCKED_WITH_NOT_FOUND -> MockedArgosApiRepository(notFound = true)
        }
    }

    val firebaseRepository: FirebaseRepository by lazy { FirebaseRepository() }
    val aiChatApiRepository: AiChatApiRepository by lazy {
        AiChatApiRepository.newClient(firebaseRepository)
    }

    override fun onCreate() {
        super.onCreate()

        var observedFirebaseUid = FirebaseAuth.getInstance().currentUser?.uid
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUid = auth.currentUser?.uid
            if (currentUid != observedFirebaseUid) {
                observedFirebaseUid = currentUid
                applicationScope.launch { aiChatApiRepository.clearCachedToken() }
            }
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        AppLogger.info(
            AppLogger.AUTH_TAG,
            "application_run_mode_selected",
            "mode" to runMode.name,
        )
    }

    enum class ArgosRunMode {
        RUN_HOMOLOGATION,
        RUN_PRODUCTION,
        RUN_MOCKED,
        RUN_MOCKED_WITH_FIRST_ACCESS,
        RUN_MOCKED_WITH_NOT_FOUND,
    }
}
