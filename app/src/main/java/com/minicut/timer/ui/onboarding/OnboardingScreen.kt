package com.minicut.timer.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minicut.timer.R
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutBottomActionBar
import com.minicut.timer.ui.components.MiniCutCardShape
import com.minicut.timer.ui.components.MiniCutGlassCard
import com.minicut.timer.ui.components.MiniCutPanelShape
import com.minicut.timer.ui.components.MiniCutPillShape
import com.minicut.timer.ui.components.MiniCutScreenHorizontalPadding
import com.minicut.timer.ui.components.MiniCutSectionHeader
import com.minicut.timer.ui.components.MiniCutSignalPill

@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
) {
    MiniCutBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                MiniCutBottomActionBar {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onStart,
                        shape = MiniCutPillShape,
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("플랜 만들고 시작하기", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "3분 안에 기간·목표·안전 가드레일을 정하고 바로 기록을 시작합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = MiniCutScreenHorizontalPadding,
                    end = MiniCutScreenHorizontalPadding,
                    top = 20.dp,
                    bottom = 18.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = "MINICUT SPRINT",
                        title = "짧게 끝내는 감량 컨트롤룸",
                        subtitle = "2~6주 동안 기준을 잠그고, 오늘 행동만 선명하게 실행하는 방식입니다.",
                    )
                }
                item { OnboardingHeroCard() }
                item { OnboardingScopeCard() }
                item {
                    OnboardingCard(
                        index = "01",
                        title = "목표는 ‘빠른 종료’",
                        body = "장기 다이어트처럼 버티는 화면이 아니라 시작일, 집중 기간, 하루 기준을 먼저 고정하는 플랜 중심 경험입니다.",
                    )
                }
                item {
                    OnboardingCard(
                        index = "02",
                        title = "매일 한 줄씩 누적",
                        body = "식사명과 칼로리만 빠르게 남기고, 홈에서는 남음/초과만 크게 확인합니다. 세부 복기는 캘린더로 넘겨 피로를 줄입니다.",
                    )
                }
                item {
                    OnboardingCard(
                        index = "03",
                        title = "무리하면 저장부터 막기",
                        body = "체중과 활동 수준을 입력하면 결핍 강도를 점검합니다. 강도가 높으면 목표를 조정해 안전하게 시작하도록 유도합니다.",
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroCard() {
    MiniCutGlassCard(accent = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniCutSignalPill("SAFE SPRINT")
                Text(
                    text = "플랜 → 기록 → 복기",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "의지력 앱이 아니라 판단을 덜어주는 체크포인트입니다. 오늘 할 일만 작게 보여주고, 집중 기간의 리듬을 선명하게 유지합니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "미니컷 스프린트 아이콘",
                modifier = Modifier.size(104.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OnboardingPill("2~6주")
            OnboardingPill("일일 기준")
            OnboardingPill("안전 가드레일")
        }
    }
}

@Composable
private fun OnboardingScopeCard() {
    MiniCutGlassCard(accent = MaterialTheme.colorScheme.tertiary) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniCutSignalPill("MINICUT SCOPE", accent = MaterialTheme.colorScheme.tertiary)
            Text(
                text = "미니컷이 남긴 것",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "짧은 기간, 빠른 기록, 회복 신호, 기기 안 기록에 집중합니다. 복잡한 자동화와 끝없는 점수화는 덜어냈어요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OnboardingScopeRow("유지", "2~6주 집중 · 하루 기준 · 남음/초과")
            OnboardingScopeRow("보강", "체중 속도 · 회복 대기 · 리듬 복기")
            OnboardingScopeRow("덜어냄", "복잡한 자동화 · 커뮤니티 순위 · 장기 점수화")
        }
    }
}

@Composable
private fun OnboardingScopeRow(
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MiniCutPanelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OnboardingPill(label: String) {
    Surface(
        shape = MiniCutPillShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OnboardingCard(
    index: String,
    title: String,
    body: String,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = MiniCutPanelShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(
                    text = index,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
