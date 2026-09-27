package com.android.argosapp

import android.app.Application
import com.android.argosapp.data.api.ArgosApiRepository
import com.android.argosapp.data.mock.MockedArgosApiRepository
import com.android.argosapp.util.AppLogger
import timber.log.Timber

class ArgosApplication : Application() {
    val runMode: ArgosRunMode =
        if (BuildConfig.DEBUG) ArgosRunMode.RUN_MOCKED_WITH_FIRST_ACCESS
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
}
