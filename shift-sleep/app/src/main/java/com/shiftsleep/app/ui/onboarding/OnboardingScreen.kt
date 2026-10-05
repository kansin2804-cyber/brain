package com.shiftsleep.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.shiftsleep.app.notify.NotificationScheduler
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import com.shiftsleep.app.analytics.AnalyticsEvents
import com.shiftsleep.app.analytics.AnalyticsParams
import com.shiftsleep.app.analytics.AppAnalytics
import kotlinx.coroutines.launch

private data class TutorialPage(
    val title: String,
    val body: String,
)

private val tutorialPages = listOf(
    TutorialPage(
        title = "근무표 앱이 아닙니다",
        body = "마이듀티·캘린더로 근무를 관리하세요. 교대수면은 그 옆에서 취침·기상·카페인 시각만 알려줍니다.",
    ),
    TutorialPage(
        title = "3가지만 보면 됩니다",
        body = "① 오늘 취침 ② 기상 ③ 카페인 끊기. 알림은 취침·기상이 기본이고, 나머지는 설정에서 켤 수 있습니다.",
    ),
    TutorialPage(
        title = "근무만 바꾸면 플랜이 바뀝니다",
        body = "하단 ‘근무표’에서 데이·이브닝·나이트·오프를 누르면, 오늘 플랜과 알림이 다시 계산됩니다.",
    ),
    TutorialPage(
        title = "의료 앱이 아닙니다",
        body = "진단·치료·불면증 완치를 하지 않습니다. 교대에 맞춘 개인 일정 가이드입니다. 불편하면 전문의와 상담하세요.",
    ),
)

@Composable
fun OnboardingScreen(
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
    onFinished: (TemplatePreset) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) } // 0..3 tutorial, 4 template
    var selected by remember { mutableStateOf(TemplatePreset.HOSPITAL_3SHIFT) }
    val scope = rememberCoroutineScope()
    val totalTutorial = tutorialPages.size

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
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "교대수면",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                if (step < totalTutorial) {
                    Text(
                        text = "튜토리얼 ${step + 1}/$totalTutorial",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Text(
                        text = "교대 유형 선택",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            if (step < totalTutorial) {
                val page = tutorialPages[step]
                Column {
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(totalTutorial) { i ->
                            Box(
                                modifier = Modifier
                                    .size(if (i == step) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (i == step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                    ),
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "나중에 설정에서 바꿀 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }

            Column {
                Text(
                    text = "의료·진단·치료가 아닌 개인 가이드입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (step > 0) {
                        OutlinedButton(
                            onClick = { step -= 1 },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("이전") }
                    } else {
                        TextButton(
                            onClick = { step = totalTutorial },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text("건너뛰기") }
                    }
                    Button(
                        onClick = {
                            if (step < totalTutorial) {
                                step += 1
                            } else {
                                scope.launch {
                                    val prefs = repository.ensurePrefs().copy(
                                        onboardingDone = true,
                                        templatePreset = selected.name,
                                        homeTipDismissed = false,
                                        replayTutorial = false,
                                    )
                                    repository.savePrefs(prefs)
                                    repository.startTrialIfNeeded()
                                    AppAnalytics.event(
                                        AnalyticsEvents.ONBOARDING_COMPLETE,
                                        mapOf(AnalyticsParams.PRESET to selected.name),
                                    )
                                    AppAnalytics.event(AnalyticsEvents.TRIAL_STARTED)
                                    repository.seedWeekIfEmpty(selected)
                                    scheduler.rescheduleAll(repository, repository.ensurePrefs())
                                    onFinished(selected)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(if (step < totalTutorial) "다음" else "시작하기")
                    }
                }
            }
        }
    }
}

/** Short in-app tutorial sheet reused from Settings. */
@Composable
fun TutorialReplaySheet(
    onClose: () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        Text("사용 방법", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(tutorialPages[step].title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            tutorialPages[step].body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) {
                OutlinedButton(onClick = { step -= 1 }) { Text("이전") }
            }
            Button(
                onClick = {
                    if (step < tutorialPages.lastIndex) step += 1 else onClose()
                },
            ) {
                Text(if (step < tutorialPages.lastIndex) "다음" else "닫기")
            }
        }
    }
}
