package com.minicut.timer.domain.rules

object RelapsePreventionCatalog {
    const val TRIGGER_LATE_NIGHT = "야식"
    const val TRIGGER_STRESS = "스트레스"
    const val TRIGGER_SOCIAL = "회식"
    const val TRIGGER_SLEEP_DEBT = "수면부족"

    const val ACTION_WALK = "산책"
    const val ACTION_BRUSH = "양치"
    const val ACTION_PROTEIN_SNACK = "단백질 간식"
    const val ACTION_WATER = "물 500ml"

    val triggerOptions = listOf(
        TRIGGER_LATE_NIGHT,
        TRIGGER_STRESS,
        TRIGGER_SOCIAL,
        TRIGGER_SLEEP_DEBT,
    )

    val copingActionOptions = listOf(
        ACTION_WALK,
        ACTION_BRUSH,
        ACTION_PROTEIN_SNACK,
        ACTION_WATER,
    )

    fun recommendedActionFor(trigger: String): String =
        when (trigger) {
            TRIGGER_LATE_NIGHT -> "양치 + 단백질 간식으로 마감 루틴 만들기"
            TRIGGER_STRESS -> "10분 산책 + 물 500ml 후 결정하기"
            TRIGGER_SOCIAL -> "식전 단백질 선행 + 첫 접시 고정하기"
            TRIGGER_SLEEP_DEBT -> "오늘은 유지 칼로리 또는 미니 브레이크 우선 고려하기"
            else -> "반복 트리거가 오는 시간대에 미리 대체 행동을 정해두기"
        }
}
