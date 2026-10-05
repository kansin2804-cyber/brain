package com.shiftsleep.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.shiftsleep.plan.Templates

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val state by vm.state.collectAsState()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "교대수면",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "오늘 · ${state.todayLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.showTip) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(14.dp),
            ) {
                Text("처음이세요?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    "1) 아래 플랜 확인 → 2) ‘근무표’에서 실제 듀티로 수정 → 3) 알림은 설정에서 조절. 마이듀티를 대체하지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { vm.dismissTip() }) {
                    Text("알겠어요")
                }
            }
        }

        state.transitionHint?.let { hint ->
            Spacer(Modifier.height(14.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        val plan = state.plan
        if (plan == null) {
            Text(
                "이번 주 근무를 입력하면 오늘의 취침·기상 플랜이 나타납니다.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "하단 ‘근무표’ 탭에서 오늘 듀티를 눌러 주세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = state.shiftLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "오늘의 플랜",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(16.dp))
            PlanRow("취침", plan.sleepStart.format(), plan.sleepStartDate)
            Spacer(Modifier.height(10.dp))
            PlanRow("기상", plan.sleepEnd.format(), plan.sleepEndDate)
            Spacer(Modifier.height(10.dp))
            PlanRow("카페인 끊기", plan.caffeineCutoff.format(), plan.caffeineCutoffDate)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "왜 이 시간인가요?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = plan.reason,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!plan.allowEveningNap) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "긴 저녁 낮잠은 피하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("이번 주", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        state.week.forEach { (shift, weekPlan) ->
            val label = shift?.let { Templates.labelKo(it.type) } ?: "—"
            val sleep = weekPlan?.let { "${it.sleepStart.format()}→${it.sleepEnd.format()}" } ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    shift?.date?.takeLast(5) ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    sleep,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "의료·진단이 아닌 개인 가이드입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlanRow(label: String, time: String, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                date.takeLast(5),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            time,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
