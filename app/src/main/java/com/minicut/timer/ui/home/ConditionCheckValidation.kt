package com.minicut.timer.ui.home

data class ConditionCheckValidationResult(
    val bodyWeightKg: Float? = null,
    val proteinGrams: Int? = null,
    val resistanceSets: Int? = null,
    val mainLiftKg: Float? = null,
    val sleepHours: Float? = null,
    val fatigueScore: Int? = null,
    val hungerScore: Int? = null,
    val moodScore: Int? = null,
    val workoutPerformanceScore: Int? = null,
    val errorMessage: String? = null,
) {
    val isValid: Boolean
        get() = errorMessage == null
}

fun validateConditionCheckInput(
    bodyWeightText: String,
    proteinText: String,
    resistanceSetsText: String,
    mainLiftKgText: String,
    sleepHoursText: String,
    fatigueScoreText: String,
    hungerScoreText: String,
    moodScoreText: String,
    workoutPerformanceScoreText: String,
): ConditionCheckValidationResult {
    val trimmedWeight = bodyWeightText.trim()
    val trimmedProtein = proteinText.trim()
    val trimmedSets = resistanceSetsText.trim()
    val trimmedMainLift = mainLiftKgText.trim()
    val trimmedSleepHours = sleepHoursText.trim()
    val trimmedFatigue = fatigueScoreText.trim()
    val trimmedHunger = hungerScoreText.trim()
    val trimmedMood = moodScoreText.trim()
    val trimmedPerformance = workoutPerformanceScoreText.trim()

    val parsedWeight = trimmedWeight.takeIf { it.isNotBlank() }?.toFloatOrNull()
    if (trimmedWeight.isNotBlank() && parsedWeight == null) {
        return ConditionCheckValidationResult(errorMessage = "체중은 숫자(예: 72.4)로 입력해주세요.")
    }
    if (parsedWeight != null && parsedWeight <= 0f) {
        return ConditionCheckValidationResult(errorMessage = "체중은 0보다 큰 값으로 입력해주세요.")
    }

    val parsedProtein = trimmedProtein.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedProtein.isNotBlank() && parsedProtein == null) {
        return ConditionCheckValidationResult(errorMessage = "단백질은 숫자로 입력해주세요.")
    }
    if (parsedProtein != null && parsedProtein <= 0) {
        return ConditionCheckValidationResult(errorMessage = "단백질은 0보다 큰 값으로 입력해주세요.")
    }

    val parsedSets = trimmedSets.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedSets.isNotBlank() && parsedSets == null) {
        return ConditionCheckValidationResult(errorMessage = "세트 수는 숫자로 입력해주세요.")
    }
    if (parsedSets != null && parsedSets <= 0) {
        return ConditionCheckValidationResult(errorMessage = "세트 수는 1 이상으로 입력해주세요.")
    }

    val parsedMainLift = trimmedMainLift.takeIf { it.isNotBlank() }?.toFloatOrNull()
    if (trimmedMainLift.isNotBlank() && parsedMainLift == null) {
        return ConditionCheckValidationResult(errorMessage = "핵심 리프트는 숫자(예: 100)로 입력해주세요.")
    }
    if (parsedMainLift != null && parsedMainLift <= 0f) {
        return ConditionCheckValidationResult(errorMessage = "핵심 리프트는 0보다 큰 값으로 입력해주세요.")
    }

    val parsedSleepHours = trimmedSleepHours.takeIf { it.isNotBlank() }?.toFloatOrNull()
    if (trimmedSleepHours.isNotBlank() && parsedSleepHours == null) {
        return ConditionCheckValidationResult(errorMessage = "수면 시간은 숫자(예: 6.5)로 입력해주세요.")
    }
    if (parsedSleepHours != null && parsedSleepHours !in 0.5f..24f) {
        return ConditionCheckValidationResult(errorMessage = "수면 시간은 0.5~24시간 범위로 입력해주세요.")
    }

    val parsedFatigue = trimmedFatigue.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedFatigue.isNotBlank() && parsedFatigue == null) {
        return ConditionCheckValidationResult(errorMessage = "피로 점수는 1~5 사이 숫자로 입력해주세요.")
    }
    if (parsedFatigue != null && parsedFatigue !in 1..5) {
        return ConditionCheckValidationResult(errorMessage = "피로 점수는 1~5 범위로 입력해주세요.")
    }

    val parsedHunger = trimmedHunger.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedHunger.isNotBlank() && parsedHunger == null) {
        return ConditionCheckValidationResult(errorMessage = "허기 점수는 1~5 사이 숫자로 입력해주세요.")
    }
    if (parsedHunger != null && parsedHunger !in 1..5) {
        return ConditionCheckValidationResult(errorMessage = "허기 점수는 1~5 범위로 입력해주세요.")
    }

    val parsedMood = trimmedMood.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedMood.isNotBlank() && parsedMood == null) {
        return ConditionCheckValidationResult(errorMessage = "기분 점수는 1~5 사이 숫자로 입력해주세요.")
    }
    if (parsedMood != null && parsedMood !in 1..5) {
        return ConditionCheckValidationResult(errorMessage = "기분 점수는 1~5 범위로 입력해주세요.")
    }

    val parsedPerformance = trimmedPerformance.takeIf { it.isNotBlank() }?.toIntOrNull()
    if (trimmedPerformance.isNotBlank() && parsedPerformance == null) {
        return ConditionCheckValidationResult(errorMessage = "수행감 점수는 1~5 사이 숫자로 입력해주세요.")
    }
    if (parsedPerformance != null && parsedPerformance !in 1..5) {
        return ConditionCheckValidationResult(errorMessage = "수행감 점수는 1~5 범위로 입력해주세요.")
    }

    val hasAnyValue =
            parsedWeight != null ||
            parsedProtein != null ||
            parsedSets != null ||
            parsedMainLift != null ||
            parsedSleepHours != null ||
            parsedFatigue != null ||
            parsedHunger != null ||
            parsedMood != null ||
            parsedPerformance != null
    if (!hasAnyValue) {
        return ConditionCheckValidationResult(errorMessage = "체중/단백질/세트/회복지표 중 최소 1개는 입력해야 저장됩니다.")
    }

    return ConditionCheckValidationResult(
        bodyWeightKg = parsedWeight,
        proteinGrams = parsedProtein,
        resistanceSets = parsedSets,
        mainLiftKg = parsedMainLift,
        sleepHours = parsedSleepHours,
        fatigueScore = parsedFatigue,
        hungerScore = parsedHunger,
        moodScore = parsedMood,
        workoutPerformanceScore = parsedPerformance,
    )
}
