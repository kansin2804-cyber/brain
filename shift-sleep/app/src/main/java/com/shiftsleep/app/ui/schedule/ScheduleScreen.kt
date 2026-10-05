package com.shiftsleep.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.shiftsleep.app.billing.Entitlement
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.notify.NotificationScheduler
import com.shiftsleep.plan.DayShift
import com.shiftsleep.plan.ShiftType
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ScheduleScreen(
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
) {
    val week by repository.observeWeek().collectAsState(initial = emptyList())
    val prefs by repository.observePrefs().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val iso = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val start = remember { LocalDate.now().with(java.time.DayOfWeek.MONDAY) }
    val byDate = week.associateBy { it.date }
    val preset = prefs?.templatePreset?.let {
        runCatching { TemplatePreset.valueOf(it) }.getOrDefault(TemplatePreset.HOSPITAL_3SHIFT)
    } ?: TemplatePreset.HOSPITAL_3SHIFT
    val entitlement = prefs?.let { Entitlement.evaluate(it) }
    var showGate by remember { mutableStateOf(false) }

    if (showGate) {
        AlertDialog(
            onDismissRequest = { showGate = false },
            title = { Text("프로 기능") },
            text = { Text("빠른 채우기는 체험/프로에서 사용할 수 있습니다. 설정에서 구독을 확인하세요.") },
            confirmButton = {
                TextButton(onClick = { showGate = false }) { Text("확인") }
            },
        )
    }

    val types = when (preset) {
        TemplatePreset.FACTORY_12H -> listOf(
            ShiftType.DAY, ShiftType.NIGHT, ShiftType.OFF, ShiftType.CUSTOM,
        )
        else -> listOf(
            ShiftType.DAY, ShiftType.EVENING, ShiftType.NIGHT, ShiftType.OFF, ShiftType.CUSTOM,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("근무표", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "날짜를 눌러 교대를 바꿉니다. 수면 플랜과 알림이 다시 계산됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text("빠른 채우기", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "이번 주(월~일)를 한 번에 채웁니다. 나중에 날짜별로 수정하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val patterns = listOf(
                "기본" to repository.defaultPattern(preset),
                "D-D-N-Off" to listOf(
                    ShiftType.DAY, ShiftType.DAY, ShiftType.NIGHT, ShiftType.OFF,
                    ShiftType.DAY, ShiftType.EVENING, ShiftType.OFF,
                ),
                "나이트 연속" to listOf(
                    ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.NIGHT, ShiftType.OFF,
                    ShiftType.OFF, ShiftType.DAY, ShiftType.DAY,
                ),
            )
            patterns.forEach { (label, pattern) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        if (entitlement?.canUsePatternFill != true) {
                            showGate = true
                            return@FilterChip
                        }
                        scope.launch {
                            repository.applyWeekPattern(pattern, preset)
                            scheduler.rescheduleAll(repository, repository.ensurePrefs())
                        }
                    },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        (0..6).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            val dateStr = date.format(iso)
            val current = byDate[dateStr]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${date.monthValue}/${date.dayOfMonth} " +
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        current?.let { Templates.labelKo(it.type) } ?: "미입력",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { type ->
                        FilterChip(
                            selected = current?.type == type,
                            onClick = {
                                scope.launch {
                                    val hours = Templates.defaultHours(type, preset)
                                    repository.upsertShift(DayShift(dateStr, type, hours))
                                    val p = repository.ensurePrefs()
                                    scheduler.rescheduleAll(repository, p)
                                }
                            },
                            label = { Text(Templates.labelKo(type)) },
                        )
                    }
                }
                if (current != null) {
                    TextButton(onClick = {
                        scope.launch {
                            repository.deleteShift(dateStr)
                            scheduler.rescheduleAll(repository, repository.ensurePrefs())
                        }
                    }) {
                        Text("삭제")
                    }
                }
            }
        }
    }
}
