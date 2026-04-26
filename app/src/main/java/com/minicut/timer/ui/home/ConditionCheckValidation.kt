package com.minicut.timer.ui.home

data class ConditionCheckValidationResult(
    val bodyWeightKg: Float? = null,
    val proteinGrams: Int? = null,
    val resistanceSets: Int? = null,
    val mainLiftKg: Float? = null,
    val relapseTrigger: String? = null,
    val copingAction: String? = null,
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

private data class ParsedConditionField<T>(
    val value: T?,
    val errorMessage: String? = null,
)

fun validateConditionCheckInput(
    bodyWeightText: String,
    proteinText: String,
    resistanceSetsText: String,
    mainLiftKgText: String,
    relapseTrigger: String?,
    copingAction: String?,
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

    val parsedWeight =
        parsePositiveFloat(
            trimmedValue = trimmedWeight,
            invalidMessage = "체중은 숫자(예: 72.4)로 입력해주세요.",
            nonPositiveMessage = "체중은 0보다 큰 값으로 입력해주세요.",
        )
    parsedWeight.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedProtein =
        parsePositiveInt(
            trimmedValue = trimmedProtein,
            invalidMessage = "단백질은 숫자로 입력해주세요.",
            nonPositiveMessage = "단백질은 0보다 큰 값으로 입력해주세요.",
        )
    parsedProtein.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedSets =
        parsePositiveInt(
            trimmedValue = trimmedSets,
            invalidMessage = "세트 수는 숫자로 입력해주세요.",
            nonPositiveMessage = "세트 수는 1 이상으로 입력해주세요.",
        )
    parsedSets.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedMainLift =
        parsePositiveFloat(
            trimmedValue = trimmedMainLift,
            invalidMessage = "핵심 리프트는 숫자(예: 100)로 입력해주세요.",
            nonPositiveMessage = "핵심 리프트는 0보다 큰 값으로 입력해주세요.",
        )
    parsedMainLift.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedSleepHours = parseSleepHours(trimmedSleepHours)
    parsedSleepHours.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedFatigue = parseRecoveryScore(trimmedFatigue, label = "피로 점수")
    parsedFatigue.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedHunger = parseRecoveryScore(trimmedHunger, label = "허기 점수")
    parsedHunger.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedMood = parseRecoveryScore(trimmedMood, label = "기분 점수")
    parsedMood.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val parsedPerformance = parseRecoveryScore(trimmedPerformance, label = "수행감 점수")
    parsedPerformance.errorMessage?.let { return ConditionCheckValidationResult(errorMessage = it) }

    val hasAnyValue =
        parsedWeight.value != null ||
            parsedProtein.value != null ||
            parsedSets.value != null ||
            parsedMainLift.value != null ||
            !relapseTrigger.isNullOrBlank() ||
            !copingAction.isNullOrBlank() ||
            parsedSleepHours.value != null ||
            parsedFatigue.value != null ||
            parsedHunger.value != null ||
            parsedMood.value != null ||
            parsedPerformance.value != null
    if (!hasAnyValue) {
        return ConditionCheckValidationResult(errorMessage = "체중/단백질/세트/회복지표 중 최소 1개는 입력해야 저장됩니다.")
    }

    return ConditionCheckValidationResult(
        bodyWeightKg = parsedWeight.value,
        proteinGrams = parsedProtein.value,
        resistanceSets = parsedSets.value,
        mainLiftKg = parsedMainLift.value,
        relapseTrigger = relapseTrigger?.trim()?.takeIf { it.isNotEmpty() },
        copingAction = copingAction?.trim()?.takeIf { it.isNotEmpty() },
        sleepHours = parsedSleepHours.value,
        fatigueScore = parsedFatigue.value,
        hungerScore = parsedHunger.value,
        moodScore = parsedMood.value,
        workoutPerformanceScore = parsedPerformance.value,
    )
}

private fun parsePositiveFloat(
    trimmedValue: String,
    invalidMessage: String,
    nonPositiveMessage: String,
): ParsedConditionField<Float> {
    if (trimmedValue.isBlank()) return ParsedConditionField(null)

    val parsedValue = trimmedValue.toFloatOrNull()
        ?: return ParsedConditionField(null, invalidMessage)
    return if (parsedValue > 0f) {
        ParsedConditionField(parsedValue)
    } else {
        ParsedConditionField(null, nonPositiveMessage)
    }
}

private fun parsePositiveInt(
    trimmedValue: String,
    invalidMessage: String,
    nonPositiveMessage: String,
): ParsedConditionField<Int> {
    if (trimmedValue.isBlank()) return ParsedConditionField(null)

    val parsedValue = trimmedValue.toIntOrNull()
        ?: return ParsedConditionField(null, invalidMessage)
    return if (parsedValue > 0) {
        ParsedConditionField(parsedValue)
    } else {
        ParsedConditionField(null, nonPositiveMessage)
    }
}

private fun parseSleepHours(trimmedValue: String): ParsedConditionField<Float> {
    if (trimmedValue.isBlank()) return ParsedConditionField(null)

    val parsedValue = trimmedValue.toFloatOrNull()
        ?: return ParsedConditionField(null, "수면 시간은 숫자(예: 6.5)로 입력해주세요.")
    return if (parsedValue in 0.5f..24f) {
        ParsedConditionField(parsedValue)
    } else {
        ParsedConditionField(null, "수면 시간은 0.5~24시간 범위로 입력해주세요.")
    }
}

private fun parseRecoveryScore(
    trimmedValue: String,
    label: String,
): ParsedConditionField<Int> {
    if (trimmedValue.isBlank()) return ParsedConditionField(null)

    val parsedValue = trimmedValue.toIntOrNull()
        ?: return ParsedConditionField(null, "${label}는 1~5 사이 숫자로 입력해주세요.")
    return if (parsedValue in 1..5) {
        ParsedConditionField(parsedValue)
    } else {
        ParsedConditionField(null, "${label}는 1~5 범위로 입력해주세요.")
    }
}
