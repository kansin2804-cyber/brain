package com.shiftsleep.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.SleepPlan
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class HomeViewModel(
    private val repository: ShiftRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeWeek().collect { shifts ->
                refresh(shifts)
            }
        }
    }

    private suspend fun refresh(shifts: List<DayShift>) {
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
        _state.value = HomeUiState(
            loading = false,
            todayLabel = "${today.monthValue}/${today.dayOfMonth} " +
                today.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
            shiftLabel = day?.let { Templates.labelKo(it.type) } ?: "근무 미입력",
            plan = plan,
            week = week,
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
