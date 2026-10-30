package com.android.argosapp.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.argosapp.ArgosApplication
import com.android.argosapp.data.cache.storage.MonthApplicationsStorage
import com.android.argosapp.data.cache.storage.TokenStorage
import com.android.argosapp.data.cache.storage.DeviceStorage
import com.android.argosapp.data.cache.storage.UserStorage
import com.android.argosapp.data.dto.DeviceRegisterDto
import com.android.argosapp.data.repository.FirebaseRepository
import com.android.argosapp.model.ApplicationWithdrawal
import com.android.argosapp.util.logging.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val monthApplicationsStorage = MonthApplicationsStorage(application)
    private val tokenStorage = TokenStorage(application)
    private val deviceStorage = DeviceStorage(application)
    private val userStorage = UserStorage(application)
    private val repository = (application as ArgosApplication).argosApiRepository
    private val firebase = FirebaseRepository()

    private var requestedMonth: YearMonth? = null

    fun loadMonthThrough(
        month: YearMonth,
        throughDate: LocalDate,
        onLoading: (Boolean) -> Unit,
        onApplicationsLoaded: (List<ApplicationWithdrawal>) -> Unit,
        onError: (Exception) -> Unit = {},
    ) {
        val coverageEnd = minOf(throughDate, month.atEndOfMonth())
        if (YearMonth.from(coverageEnd) != month || requestedMonth == month) return
        requestedMonth = month

        viewModelScope.launch {
            try {
                onLoading(true)
                val cached = monthApplicationsStorage.getMonth(month)
                val cachedApplications = cached?.applications.orEmpty()
                onApplicationsLoaded(cachedApplications)
                val coveredThrough = cached?.coveredThrough
                    ?.takeIf { YearMonth.from(it) == month }

                if (coveredThrough != null && coveredThrough >= coverageEnd) {
                    return@launch
                }

                val bearerToken = tokenStorage.getArgosApiToken()
                if (bearerToken.isNullOrBlank()) {
                    AppLogger.error(
                        AppLogger.REPOSITORY_TAG,
                        "home_application_withdrawal_window_skipped",
                        SecurityException("Bearer token is unavailable."),
                    )
                    onError(SecurityException("Bearer token is unavailable."))
                    return@launch
                }

                val missingStart = coveredThrough?.plusDays(1) ?: month.atDay(1)
                if (missingStart > coverageEnd) {
                    return@launch
                }

                val missingDays = ChronoUnit.DAYS.between(missingStart, coverageEnd).toInt()
                val newApplications = repository.getApplicationWithdrawalWindow(
                    bearerToken = bearerToken,
                    referenceDate = missingStart,
                    daysBefore = 0,
                    daysAfter = missingDays,
                )
                val mergedApplications = monthApplicationsStorage.appendMonth(
                    month = month,
                    coveredThrough = coverageEnd,
                    newApplications = newApplications,
                )
                onApplicationsLoaded(mergedApplications)
            } catch (error: CancellationException) {
                throw error

            } catch (error: Exception) {
                AppLogger.error(
                    AppLogger.REPOSITORY_TAG,
                    "home_application_withdrawal_window_load_failed",
                    error,
                    "endpoint" to "application/withdrawal-window",
                    "month" to month,
                )
                onError(error)

            } finally {
                requestedMonth = null
                onLoading(false)
            }
        }

    }

    fun registerDevice() {
        viewModelScope.launch {
            try {
                val bearerToken = requireNotNull(tokenStorage.getArgosApiToken()?.takeIf { it.isNotBlank() }) {
                    "Bearer token is unavailable."
                }
                val pushToken = firebase.getMessagingToken()
                val userId = requireNotNull(userStorage.getUser()?.userId) { "User is unavailable." }.toString()
                val deviceId = repository.registerDevice(
                    bearerToken = bearerToken,
                    data = DeviceRegisterDto(
                        deviceId = deviceStorage.getDeviceId(userId),
                        pushToken = pushToken,
                        platform = "ANDROID",
                        model = Build.MODEL.take(60),
                    ),
                )
                deviceStorage.saveDeviceId(userId, deviceId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLogger.error(AppLogger.REPOSITORY_TAG, "home_device_registration_failed", error)
            }
        }
    }
}
