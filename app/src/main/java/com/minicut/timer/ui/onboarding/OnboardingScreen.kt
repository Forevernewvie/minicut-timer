package com.minicut.timer.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minicut.timer.ui.components.MiniCutBackdrop
import com.minicut.timer.ui.components.MiniCutCardShape
import com.minicut.timer.ui.components.MiniCutSectionHeader

@Composable
fun OnboardingScreen(
    onStart: () -> Unit,
) {
    MiniCutBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onStart,
                    ) {
                        Text("미니컷 시작하기")
                    }
                    Text(
                        text = "온보딩은 한 번만 표시되고 이후 바로 홈 화면으로 이동합니다.",
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
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MiniCutSectionHeader(
                        kicker = "미니컷 101",
                        title = "미니컷이 뭔가요?",
                        subtitle = "짧은 기간(보통 2~6주) 동안 집중적으로 체지방을 관리하고, 이후 유지 단계로 전환하는 전략입니다.",
                    )
                }
                item {
                    OnboardingCard(
                        title = "핵심 포인트",
                        body = "길게 버티는 다이어트보다 짧고 명확하게 끝내는 것이 목적이에요. 그래서 이 앱도 기간과 하루 목표를 먼저 설정하도록 설계돼 있습니다.",
                    )
                }
                item {
                    OnboardingCard(
                        title = "이 앱에서 하는 일",
                        body = "① 시작일/기간/목표 칼로리 설정\n② 음식 기록으로 섭취량 누적\n③ 남음/초과를 즉시 확인\n④ 달력에서 날짜별 흐름 복기",
                    )
                }
                item {
                    OnboardingCard(
                        title = "안전한 사용 팁",
                        body = "무리하게 장기 지속하지 말고, 피로가 커지기 전에 종료 후 유지 단계로 전환하세요. 컨디션/수면/운동 수행능력도 함께 체크하면 더 좋습니다.",
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingCard(
    title: String,
    body: String,
) {
    Card(
        shape = MiniCutCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
