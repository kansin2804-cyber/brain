package com.shiftsleep.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.data.UserPrefsEntity
import com.shiftsleep.app.notify.NotificationScheduler
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
    onFinished: (TemplatePreset) -> Unit,
) {
    var selected by remember { mutableStateOf(TemplatePreset.HOSPITAL_3SHIFT) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "교대수면",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "나이트 끝나고, 몇 시에 누울지",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "근무만 넣으면 취침·기상·카페인 컷오프를 알려드립니다. 근무표 앱을 대체하지 않습니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "교대 유형",
                    style = MaterialTheme.typography.titleLarge,
                )
                TemplatePreset.entries.forEach { preset ->
                    val selectedBorder = if (selected == preset) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, selectedBorder, RoundedCornerShape(16.dp))
                            .clickable { selected = preset }
                            .padding(16.dp),
                    ) {
                        Text(
                            Templates.presetLabelKo(preset),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            when (preset) {
                                TemplatePreset.HOSPITAL_3SHIFT -> "데이 / 이브닝 / 나이트"
                                TemplatePreset.FACTORY_12H -> "주간 12시간 / 야간 12시간"
                                TemplatePreset.CUSTOM -> "시작·종료 시각을 직접 입력"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column {
                Text(
                    text = "의료·진단·치료가 아닌 개인 가이드입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val prefs = repository.ensurePrefs().copy(
                                onboardingDone = true,
                                templatePreset = selected.name,
                            )
                            repository.savePrefs(prefs)
                            repository.seedWeekIfEmpty(selected)
                            scheduler.rescheduleAll(repository, prefs)
                            onFinished(selected)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("시작하기")
                }
            }
        }
    }
}
