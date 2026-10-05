package com.shiftsleep.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.ShiftType
import com.shiftsleep.plan.SleepPlan
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

data class HomeUiState(
    val loading: Boolean = true,
    val todayLabel: String = "",
    val shiftLabel: String = "근무 없음",
    val plan: SleepPlan? = null,
    val week: List<Pair<DayShift?, SleepPlan?>> = emptyList(),
    val showTip: Boolean = false,
    val transitionHint: String? = null,
)

class HomeViewModel(
    private val repository: ShiftRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeWeek(),
                repository.observePrefs(),
            ) { shifts, prefs -> shifts to prefs }
                .collect { (shifts, prefs) ->
                    refresh(shifts, prefs.homeTipDismissed)
                }
        }
    }

    fun dismissTip() {
        viewModelScope.launch {
            val prefs = repository.ensurePrefs().copy(homeTipDismissed = true)
            repository.savePrefs(prefs)
        }
    }

    private suspend fun refresh(shifts: List<DayShift>, tipDismissed: Boolean) {
        val today = LocalDate.now()
        val iso = DateTimeFormatter.ISO_LOCAL_DATE
        val todayStr = today.format(iso)
        val plan = repository.planForDate(todayStr)
        val weekPlans = repository.planWeek()
        val start = today.with(java.time.DayOfWeek.MONDAY)
        val byDate = shifts.associateBy { it.date }
        val planByDate = weekPlans.associateBy { it.date }
        val week = (0..6).map { offset ->
            val date = start.plusDays(offset.toLong()).format(iso)
            byDate[date] to planByDate[date]
        }
        val day = byDate[todayStr]
        val yesterday = byDate[today.minusDays(1).format(iso)]
        val tomorrow = byDate[today.plusDays(1).format(iso)]
        val transition = when {
            yesterday?.type == ShiftType.NIGHT && day?.type == ShiftType.OFF ->
                "전환일 · 나이트→오프: 낮 리듬 복귀, 긴 저녁 낮잠은 피하세요."
            day?.type == ShiftType.OFF && tomorrow?.type == ShiftType.DAY ->
                "전환 전날 · 내일 데이: 오늘 취침을 맞춰 두면 기상 부담이 줄어듭니다."
            yesterday?.type == ShiftType.OFF && day?.type == ShiftType.NIGHT ->
                "전환일 · 오프→나이트: 카페인 컷오프를 근무 중반 이전으로 맞춰 보세요."
            else -> null
        }
        _state.value = HomeUiState(
            loading = false,
            todayLabel = "${today.monthValue}/${today.dayOfMonth} " +
                today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            shiftLabel = day?.let { Templates.labelKo(it.type) } ?: "근무 미입력",
            plan = plan,
            week = week,
            showTip = !tipDismissed,
            transitionHint = transition,
        )
    }

    companion object {
        fun factory(repository: ShiftRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
