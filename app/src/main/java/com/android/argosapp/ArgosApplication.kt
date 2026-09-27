package com.android.argosapp

import android.app.Application
import com.android.argosapp.data.repository.ArgosApiRepository
import com.android.argosapp.data.repository.MockedArgosApiRepository
import com.android.argosapp.util.logging.AppLogger
import timber.log.Timber

class ArgosApplication : Application() {
    val runMode: ArgosRunMode =
        if (BuildConfig.DEBUG) ArgosRunMode.RUN_MOCKED
        else ArgosRunMode.RUN_HOMOLOGATION

    val argosApiRepository: ArgosApiRepository by lazy {
        when (runMode) {
            ArgosRunMode.RUN_HOMOLOGATION -> ArgosApiRepository(homologation = true)
            ArgosRunMode.RUN_PRODUCTION -> ArgosApiRepository(homologation = false)
            ArgosRunMode.RUN_MOCKED -> MockedArgosApiRepository()
            ArgosRunMode.RUN_MOCKED_WITH_FIRST_ACCESS -> MockedArgosApiRepository(firstAccess = true)
            ArgosRunMode.RUN_MOCKED_WITH_NOT_FOUND -> MockedArgosApiRepository(notFound = true)
        }
    }

    override fun onCreate() {
        super.onCreate()

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
