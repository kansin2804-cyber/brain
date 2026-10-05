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
import com.shiftsleep.app.analytics.AnalyticsEvents
import com.shiftsleep.app.analytics.AppAnalytics
import com.shiftsleep.app.billing.BillingManager
import com.shiftsleep.app.billing.Entitlement
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.notify.NotificationScheduler
import com.shiftsleep.app.ui.onboarding.TutorialReplaySheet
import com.shiftsleep.app.ui.paywall.PaywallContent
import com.shiftsleep.app.ui.paywall.entitlementLabel
import com.shiftsleep.plan.TemplatePreset
import com.shiftsleep.plan.Templates
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
    billing: BillingManager,
) {
    val prefs by repository.observePrefs().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var showTutorial by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    if (prefs == null) return
    val entitlement = Entitlement.evaluate(prefs!!)

    if (showTutorial) {
        AlertDialog(
            onDismissRequest = { showTutorial = false },
            confirmButton = {},
            text = { TutorialReplaySheet(onClose = { showTutorial = false }) },
        )
    }
    if (showPaywall) {
        AlertDialog(
            onDismissRequest = { showPaywall = false },
            confirmButton = {},
            text = {
                PaywallContent(
                    prefs = prefs!!,
                    entitlement = entitlement,
                    repository = repository,
                    scheduler = scheduler,
                    billing = billing,
                    onClose = { showPaywall = false },
                )
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

        Text("구독 · ${entitlementLabel(entitlement)}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                entitlement.isPro -> "전체 알림·패턴 채우기 사용 중"
                entitlement.inTrial -> "체험 ${entitlement.trialDaysLeft}일 남음"
                else -> "무료: 취침 알림만 · 프로로 기상·카페인·패턴 채우기"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                showPaywall = true
                AppAnalytics.event(AnalyticsEvents.PAYWALL_OPEN)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (entitlement.isPro) "구독 관리" else "프로 구독 / 체험 안내")
        }

        Spacer(Modifier.height(24.dp))
        Text("도움말", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                showTutorial = true
                AppAnalytics.event(AnalyticsEvents.TUTORIAL_REPLAY)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("사용 튜토리얼 다시 보기") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.savePrefs(prefs!!.copy(homeTipDismissed = false))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("홈 첫 안내 다시 표시") }

        Spacer(Modifier.height(24.dp))
        Text("알림", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (!entitlement.canUseFullNotifications) {
            Text(
                "체험 종료 후 취침 알림만 동작합니다. 기상·카페인·준비는 프로.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(8.dp))
        }
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
            "버전 0.4.0 · Analytics 스캐폴딩 + Billing",
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
