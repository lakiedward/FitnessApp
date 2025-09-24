package com.example.fitnessapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.CyclingFtpResponse
import com.example.fitnessapp.model.RunningFtpResponse
import com.example.fitnessapp.model.SwimmingPaceResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class PerformanceViewModel(
    private val api: ApiService = RetrofitClient.retrofit.create(ApiService::class.java)
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerformanceUiState())
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    fun refreshAll(token: String?) {
        if (token.isNullOrBlank()) {
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    globalError = "Autentificare necesar\u0103 pentru a \u00EEnc\u0103rca metricile."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                val loadingMetrics = state.metrics.mapValues { (_, metricState) ->
                    metricState.copy(isRefreshing = true, error = null)
                }
                state.copy(isLoading = true, globalError = null, metrics = loadingMetrics)
            }

            MetricType.values().forEach { metric ->
                refreshMetricInternal(metric, token)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshMetric(metric: MetricType, token: String?) {
        if (token.isNullOrBlank()) {
            updateMetric(metric) { state ->
                state.copy(
                    isRefreshing = false,
                    status = MetricStatus.ERROR,
                    error = "Autentificare necesar\u0103."
                )
            }
            return
        }

        viewModelScope.launch {
            updateMetric(metric) { it.copy(isRefreshing = true, error = null) }
            refreshMetricInternal(metric, token)
        }
    }

    fun recalc(metric: MetricType, token: String?) {
        if (token.isNullOrBlank()) {
            updateMetric(metric) { state ->
                state.copy(
                    isRecalculating = false,
                    status = MetricStatus.ERROR,
                    error = "Autentificare necesar\u0103."
                )
            }
            return
        }

        viewModelScope.launch {
            updateMetric(metric) {
                it.copy(
                    isRecalculating = true,
                    status = MetricStatus.RECALCULATING,
                    statusMessage = "Proces\u0103m recalcularea...",
                    error = null
                )
            }

            try {
                val response = api.recalc("Bearer $token", metric.apiName)
                if (response.isSuccessful) {
                    val body = response.body()
                    val jobId = body?.resolvedJobId ?: response.headers()["X-Job-Id"]
                    if (jobId.isNullOrBlank()) {
                        updateMetric(metric) {
                            it.copy(
                                isRecalculating = false,
                                status = MetricStatus.ERROR,
                                error = body?.message ?: "Job ID indisponibil.",
                                statusMessage = body?.message
                            )
                        }
                        return@launch
                    }

                    updateMetric(metric) {
                        it.copy(
                            pendingJobId = jobId,
                            statusMessage = body?.message
                        )
                    }
                    pollRecalcStatus(metric, token, jobId)
                } else {
                    val message = response.errorMessage("Recalcularea nu a putut fi ini\u021Biat\u0103.")
                    val targetStatus = if (response.code() == 429) MetricStatus.COOLDOWN else MetricStatus.ERROR
                    updateMetric(metric) {
                        it.copy(
                            isRecalculating = false,
                            status = targetStatus,
                            statusMessage = if (targetStatus == MetricStatus.COOLDOWN) message else it.statusMessage,
                            error = if (targetStatus == MetricStatus.ERROR) message else it.error,
                            pendingJobId = null
                        )
                    }
                }
            } catch (io: IOException) {
                updateMetric(metric) {
                    it.copy(
                        isRecalculating = false,
                        status = MetricStatus.ERROR,
                        error = "Conexiunea a e\u0219uat. \u00CEncerc\u0103 din nou.",
                        pendingJobId = null
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                updateMetric(metric) {
                    it.copy(
                        isRecalculating = false,
                        status = MetricStatus.ERROR,
                        error = t.localizedMessage ?: "Eroare necunoscut\u0103.",
                        pendingJobId = null
                    )
                }
            }
        }
    }

    private suspend fun pollRecalcStatus(metric: MetricType, token: String, jobId: String) {
        val maxAttempts = 15
        repeat(maxAttempts) { attempt ->
            if (attempt > 0) {
                delay(2000)
            }

            val response = try {
                api.getRecalcStatus("Bearer $token", metric.apiName, jobId)
            } catch (io: IOException) {
                updateMetric(metric) {
                    it.copy(
                        isRecalculating = false,
                        status = MetricStatus.ERROR,
                        error = "Nu am putut verifica statusul recalcul\u0103rii.",
                        pendingJobId = null
                    )
                }
                return
            }

            if (!response.isSuccessful) {
                val message = response.errorMessage("Nu am putut verifica statusul recalcul\u0103rii.")
                updateMetric(metric) {
                    it.copy(
                        isRecalculating = false,
                        status = MetricStatus.ERROR,
                        error = message,
                        statusMessage = message,
                        pendingJobId = null
                    )
                }
                return
            }

            val body = response.body()
            val statusValue = body?.status?.lowercase(Locale.ROOT)
            when (statusValue) {
                "queued", "running" -> {
                    updateMetric(metric) {
                        it.copy(
                            status = MetricStatus.RECALCULATING,
                            statusMessage = body?.message ?: "\u00CEn recalculare...",
                            lastUpdated = formatTimestamp(body?.resolvedLastComputedAt) ?: it.lastUpdated,
                            source = body?.source ?: it.source,
                            cooldownUntil = body?.resolvedCooldownUntil,
                            isRecalculating = true
                        )
                    }
                }
                "cooldown" -> {
                    updateMetric(metric) {
                        it.copy(
                            status = MetricStatus.COOLDOWN,
                            statusMessage = body?.message,
                            cooldownUntil = body?.resolvedCooldownUntil,
                            isRecalculating = false,
                            pendingJobId = null
                        )
                    }
                    return
                }
                "done" -> {
                    updateMetric(metric) {
                        it.copy(
                            lastUpdated = formatTimestamp(body?.resolvedLastComputedAt) ?: it.lastUpdated,
                            source = body?.source ?: it.source,
                            statusMessage = body?.message,
                            cooldownUntil = body?.resolvedCooldownUntil
                        )
                    }
                    refreshMetricInternal(metric, token)
                    updateMetric(metric) {
                        it.copy(
                            isRecalculating = false,
                            status = MetricStatus.OK,
                            statusMessage = body?.message,
                            cooldownUntil = body?.resolvedCooldownUntil,
                            pendingJobId = null
                        )
                    }
                    return
                }
                "error" -> {
                    val message = body?.message ?: "Recalcularea a e\u0219uat."
                    updateMetric(metric) {
                        it.copy(
                            isRecalculating = false,
                            status = MetricStatus.ERROR,
                            error = message,
                            statusMessage = message,
                            pendingJobId = null
                        )
                    }
                    return
                }
                else -> {
                    updateMetric(metric) {
                        it.copy(
                            statusMessage = body?.message ?: it.statusMessage,
                            lastUpdated = formatTimestamp(body?.resolvedLastComputedAt) ?: it.lastUpdated,
                            source = body?.source ?: it.source
                        )
                    }
                }
            }
        }

        updateMetric(metric) {
            it.copy(
                isRecalculating = false,
                status = MetricStatus.ERROR,
                error = "Recalcularea dureaz\u0103 prea mult. Verific\u0103 mai t\u00E2rziu.",
                statusMessage = "Recalcularea dureaz\u0103 prea mult.",
                pendingJobId = null
            )
        }
    }

    private suspend fun refreshMetricInternal(metric: MetricType, token: String) {
        try {
            when (metric) {
                MetricType.CYCLING_FTP -> handleCyclingResponse(api.getCyclingFtp("Bearer $token"))
                MetricType.RUNNING_FTP -> handleRunningResponse(api.getRunningFtp("Bearer $token"))
                MetricType.SWIM_CSS -> handleSwimResponse(api.getSwimmingPace("Bearer $token"))
            }
        } catch (io: IOException) {
            applyNetworkError(metric)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            updateMetric(metric) {
                it.copy(
                    isRefreshing = false,
                    isRecalculating = false,
                    status = MetricStatus.ERROR,
                    error = t.localizedMessage ?: "Eroare necunoscut\u0103."
                )
            }
        }
    }

    private fun handleCyclingResponse(response: Response<CyclingFtpResponse>) {
        if (response.isSuccessful) {
            val ftp = response.body()?.cyclingFtp ?: 0
            updateMetric(MetricType.CYCLING_FTP) { state ->
                val hasValue = ftp > 0
                val targetStatus = if (hasValue) MetricStatus.OK else MetricStatus.INSUFFICIENT
                state.copy(
                    value = if (hasValue) ftp.toString() else "--",
                    status = if (state.isRecalculating && state.status == MetricStatus.RECALCULATING) state.status else targetStatus,
                    statusMessage = if (hasValue) null else "Nu avem suficiente date pentru FTP.",
                    isRefreshing = false,
                    error = if (hasValue) null else state.error
                )
            }
        } else {
            val message = response.errorMessage("Nu am putut ob\u021Bine FTP-ul de ciclism.")
            applyError(MetricType.CYCLING_FTP, message, response.code())
        }
    }

    private fun handleRunningResponse(response: Response<RunningFtpResponse>) {
        if (response.isSuccessful) {
            val pace = response.body()?.runningFtp.orEmpty()
            updateMetric(MetricType.RUNNING_FTP) { state ->
                val hasValue = pace.isNotBlank()
                val targetStatus = if (hasValue) MetricStatus.OK else MetricStatus.INSUFFICIENT
                state.copy(
                    value = if (hasValue) pace else "--",
                    status = if (state.isRecalculating && state.status == MetricStatus.RECALCULATING) state.status else targetStatus,
                    statusMessage = if (hasValue) null else "Nu avem suficiente date pentru pace.",
                    isRefreshing = false,
                    error = if (hasValue) null else state.error
                )
            }
        } else {
            val message = response.errorMessage("Nu am putut ob\u021Bine pace-ul de alergare.")
            applyError(MetricType.RUNNING_FTP, message, response.code())
        }
    }

    private fun handleSwimResponse(response: Response<SwimmingPaceResponse>) {
        if (response.isSuccessful) {
            val pace = response.body()?.pace100m.orEmpty()
            updateMetric(MetricType.SWIM_CSS) { state ->
                val hasValue = pace.isNotBlank()
                val targetStatus = if (hasValue) MetricStatus.OK else MetricStatus.INSUFFICIENT
                state.copy(
                    value = if (hasValue) pace else "--",
                    status = if (state.isRecalculating && state.status == MetricStatus.RECALCULATING) state.status else targetStatus,
                    statusMessage = if (hasValue) null else "Nu avem suficiente date pentru CSS.",
                    isRefreshing = false,
                    error = if (hasValue) null else state.error
                )
            }
        } else {
            val message = response.errorMessage("Nu am putut ob\u021Bine pace-ul de \u00EEnot.")
            applyError(MetricType.SWIM_CSS, message, response.code())
        }
    }

    private fun applyNetworkError(metric: MetricType) {
        val message = "Nu am putut comunica cu serverul. Verific\u0103 conexiunea."
        updateMetric(metric) {
            it.copy(
                isRefreshing = false,
                isRecalculating = false,
                status = MetricStatus.ERROR,
                error = message,
                pendingJobId = null
            )
        }
        _uiState.update { state ->
            state.copy(globalError = message)
        }
    }

    private fun applyError(metric: MetricType, message: String, code: Int?) {
        val status = if (code == 429) MetricStatus.COOLDOWN else MetricStatus.ERROR
        updateMetric(metric) {
            it.copy(
                isRefreshing = false,
                isRecalculating = false,
                status = status,
                statusMessage = if (status == MetricStatus.COOLDOWN) message else it.statusMessage,
                error = if (status == MetricStatus.ERROR) message else it.error,
                pendingJobId = null
            )
        }
    }

    private fun updateMetric(metric: MetricType, transform: (MetricCardState) -> MetricCardState) {
        _uiState.update { state ->
            val current = state.metrics[metric] ?: MetricCardState(metric)
            state.copy(metrics = state.metrics + (metric to transform(current)))
        }
    }

    private fun formatTimestamp(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val instant = Instant.parse(raw)
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }.getOrElse { raw }
    }

    private fun <T> Response<T>.errorMessage(fallback: String): String {
        return try {
            errorBody()?.string()?.takeIf { it.isNotBlank() } ?: fallback
        } catch (t: Throwable) {
            fallback
        }
    }
}

data class PerformanceUiState(
    val isLoading: Boolean = false,
    val metrics: Map<MetricType, MetricCardState> = MetricType.values().associateWith { MetricCardState(it) },
    val globalError: String? = null
)

data class MetricCardState(
    val metric: MetricType,
    val label: String = metric.displayName,
    val unit: String = metric.unit,
    val value: String? = null,
    val lastUpdated: String? = null,
    val source: String? = null,
    val status: MetricStatus = MetricStatus.UNKNOWN,
    val statusMessage: String? = null,
    val isRefreshing: Boolean = false,
    val isRecalculating: Boolean = false,
    val error: String? = null,
    val cooldownUntil: String? = null,
    val pendingJobId: String? = null
)

enum class MetricStatus(val displayText: String) {
    OK("OK"),
    RECALCULATING("\u00CEn recalculare"),
    INSUFFICIENT("Insuficiente date"),
    COOLDOWN("\u00CEn a\u0219teptare"),
    ERROR("Eroare"),
    UNKNOWN("--")
}

enum class MetricType(
    val apiName: String,
    val displayName: String,
    val unit: String
) {
    CYCLING_FTP("cycling_ftp", "Cycling FTP", "W"),
    RUNNING_FTP("running_ftp", "Running threshold pace", "min/km"),
    SWIM_CSS("swim_css", "Swim CSS pace", "min/100m");
}


