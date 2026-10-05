package com.shiftsleep.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shiftsleep.app.R
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.notify.NotificationScheduler
import com.shiftsleep.app.ui.onboarding.TutorialReplaySheet
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
) {
    val prefs by repository.observePrefs().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showTutorial by remember { mutableStateOf(false) }
    if (prefs == null) return

    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            confirmButton = {},
            text = {
                TutorialReplaySheet(onClose = { showTutorial = false })
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        Text("도움말", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showTutorial = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("사용 튜토리얼 다시 보기")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.savePrefs(prefs!!.copy(homeTipDismissed = false))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("홈 첫 안내 다시 표시")
        }

        Spacer(Modifier.height(24.dp))
        Text("알림", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        ToggleRow("취침 알림", prefs!!.notifySleep) { checked ->
            scope.launch {
                val next = prefs!!.copy(notifySleep = checked)
                repository.savePrefs(next)
                scheduler.rescheduleAll(repository, next)
            }
        }
        ToggleRow("기상 알림", prefs!!.notifyWake) { checked ->
            scope.launch {
                val next = prefs!!.copy(notifyWake = checked)
                repository.savePrefs(next)
                scheduler.rescheduleAll(repository, next)
            }
        }
        ToggleRow("카페인 컷오프 (선택)", prefs!!.notifyCaffeine) { checked ->
            scope.launch {
                val next = prefs!!.copy(notifyCaffeine = checked)
                repository.savePrefs(next)
                scheduler.rescheduleAll(repository, next)
            }
        }
        ToggleRow("취침 준비 30분 전", prefs!!.notifyWindDown) { checked ->
            scope.launch {
                val next = prefs!!.copy(notifyWindDown = checked)
                repository.savePrefs(next)
                scheduler.rescheduleAll(repository, next)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("교대 템플릿", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            Templates.presetLabelKo(
                runCatching { TemplatePreset.valueOf(prefs!!.templatePreset) }
                    .getOrDefault(TemplatePreset.HOSPITAL_3SHIFT),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        TemplatePreset.entries.forEach { preset ->
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val next = prefs!!.copy(templatePreset = preset.name)
                        repository.savePrefs(next)
                        scheduler.rescheduleAll(repository, next)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                Text(Templates.presetLabelKo(preset))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("구독 (MVP 뼈대)", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            if (prefs!!.proUnlocked) {
                "프로 해제됨 (개발용)"
            } else {
                "무료: 미리보기 · 프로: 무제한 플랜·알림 (Play Billing 연동 예정)"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    repository.savePrefs(prefs!!.copy(proUnlocked = !prefs!!.proUnlocked))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (prefs!!.proUnlocked) "프로 잠금 (테스트)" else "프로 체험 토글 (테스트)")
        }

        Spacer(Modifier.height(28.dp))
        Text("면책", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.disclaimer_long),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "버전 0.2.0 · 튜토리얼·전환일·패턴 채우기",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
