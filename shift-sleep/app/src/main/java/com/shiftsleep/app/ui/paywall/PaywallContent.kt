package com.shiftsleep.app.ui.paywall

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shiftsleep.app.BuildConfig
import com.shiftsleep.app.billing.BillingManager
import com.shiftsleep.app.billing.BillingProducts
import com.shiftsleep.app.billing.EntitlementStatus
import com.shiftsleep.app.data.ShiftRepository
import com.shiftsleep.app.data.UserPrefsEntity
import com.shiftsleep.app.notify.NotificationScheduler
import kotlinx.coroutines.launch

@Composable
fun PaywallContent(
    prefs: UserPrefsEntity,
    entitlement: EntitlementStatus,
    repository: ShiftRepository,
    scheduler: NotificationScheduler,
    billing: BillingManager,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val available by billing.available.collectAsState()
    val products by billing.products.collectAsState()
    val message by billing.message.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.padding(4.dp),
    ) {
        Text("프로로 전체 기능", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                entitlement.isPro -> "프로가 활성화되어 있습니다."
                entitlement.inTrial -> "체험 ${entitlement.trialDaysLeft}일 남음 · 알림 전체·패턴 채우기 사용 중"
                else -> "체험이 끝났습니다. 무료는 취침 알림만, 프로는 기상·카페인·패턴 채우기까지."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text("· 월/연 구독 (Play Console 가격 기준)", style = MaterialTheme.typography.bodyMedium)
        Text("· 7일 체험 후 구독", style = MaterialTheme.typography.bodyMedium)
        Text("· 의료·진단 앱 아님", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (message != null) {
            Text(message!!, color = MaterialTheme.colorScheme.secondary)
            TextButton(onClick = { billing.clearMessage() }) { Text("닫기") }
            Spacer(Modifier.height(8.dp))
        }

        if (!entitlement.isPro) {
            if (available && products.isNotEmpty()) {
                Button(
                    onClick = {
                        activity?.let { billing.launchPurchase(it, BillingProducts.MONTHLY) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("월간 구독") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        activity?.let { billing.launchPurchase(it, BillingProducts.YEARLY) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("연간 구독") }
            } else {
                Text(
                    "Play 구독 상품이 아직 없습니다. Console에 " +
                        BillingProducts.MONTHLY + " / " + BillingProducts.YEARLY +
                        " 등록 후 라이선스 테스터로 확인하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            billing.queryProducts()
                            billing.refreshPurchases()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("상품 다시 불러오기") }
            }
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val next = prefs.copy(proUnlocked = true)
                            repository.savePrefs(next)
                            scheduler.rescheduleAll(repository, next)
                            onClose()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("개발용 프로 해제 (디버그)") }
            }
        } else if (BuildConfig.DEBUG) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val next = prefs.copy(proUnlocked = false, subscriptionActive = false)
                        repository.savePrefs(next)
                        scheduler.rescheduleAll(repository, next)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("프로 잠금 (테스트)") }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text("닫기")
        }
    }
}

fun entitlementLabel(status: EntitlementStatus): String = when {
    status.isPro -> "프로"
    status.inTrial -> "체험 ${status.trialDaysLeft}일"
    else -> "무료"
}
