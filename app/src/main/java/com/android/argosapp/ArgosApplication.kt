package com.android.argosapp

import android.app.Application
import com.android.argosapp.data.repository.ArgosApiRepository
import com.android.argosapp.data.repository.MedicationStockRepository
import com.android.argosapp.data.repository.MockedArgosApiRepository
import com.android.argosapp.data.repository.MockedMedicationStockRepository
import com.android.argosapp.data.repository.RemoteArgosApiRepository
import com.android.argosapp.data.repository.RemoteMedicationStockRepository
import com.android.argosapp.util.logging.AppLogger
import timber.log.Timber

class ArgosApplication : Application() {
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

    val medicationStockRepository: MedicationStockRepository by lazy {
        when (runMode) {
            ArgosRunMode.RUN_HOMOLOGATION -> RemoteMedicationStockRepository(homologation = true)
            ArgosRunMode.RUN_PRODUCTION -> RemoteMedicationStockRepository(homologation = false)
            ArgosRunMode.RUN_MOCKED,
            ArgosRunMode.RUN_MOCKED_WITH_FIRST_ACCESS,
            ArgosRunMode.RUN_MOCKED_WITH_NOT_FOUND -> MockedMedicationStockRepository()
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
