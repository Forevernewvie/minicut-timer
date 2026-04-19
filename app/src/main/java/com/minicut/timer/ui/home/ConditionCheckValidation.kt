package com.minicut.timer.ui.home

data class ConditionCheckValidationResult(
    val bodyWeightKg: Float? = null,
    val proteinGrams: Int? = null,
    val resistanceSets: Int? = null,
    val errorMessage: String? = null,
) {
    val isValid: Boolean
        get() = errorMessage == null
}

fun validateConditionCheckInput(
    bodyWeightText: String,
    proteinText: String,
    resistanceSetsText: String,
): ConditionCheckValidationResult {
    val trimmedWeight = bodyWeightText.trim()
    val trimmedProtein = proteinText.trim()
    val trimmedSets = resistanceSetsText.trim()

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

    if (parsedWeight == null && parsedProtein == null && parsedSets == null) {
        return ConditionCheckValidationResult(errorMessage = "체중/단백질/세트 중 최소 1개는 입력해야 저장됩니다.")
    }

    return ConditionCheckValidationResult(
        bodyWeightKg = parsedWeight,
        proteinGrams = parsedProtein,
        resistanceSets = parsedSets,
    )
}
