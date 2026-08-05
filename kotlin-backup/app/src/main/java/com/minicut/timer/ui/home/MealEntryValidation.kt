package com.minicut.timer.ui.home

data class MealEntryValidationResult(
    val foodNameError: String? = null,
    val caloriesError: String? = null,
) {
    val isValid: Boolean
        get() = foodNameError == null && caloriesError == null

    val firstErrorMessage: String?
        get() = foodNameError ?: caloriesError
}

fun validateMealEntryInput(
    foodName: String,
    caloriesText: String,
): MealEntryValidationResult {
    val trimmedFoodName = foodName.trim()
    val calories = caloriesText.trim().toIntOrNull()

    return MealEntryValidationResult(
        foodNameError = if (trimmedFoodName.isBlank()) "무엇을 먹었는지 적어주세요" else null,
        caloriesError = if (calories == null || calories <= 0) "칼로리는 1 이상 숫자로 입력해주세요" else null,
    )
}
