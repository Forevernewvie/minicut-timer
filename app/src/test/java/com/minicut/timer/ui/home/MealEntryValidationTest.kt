package com.minicut.timer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealEntryValidationTest {

    @Test
    fun validateMealEntryInput_rejectsBlankFoodName() {
        val result = validateMealEntryInput(foodName = "   ", caloriesText = "320")

        assertFalse(result.isValid)
        assertEquals("무엇을 먹었는지 적어주세요", result.foodNameError)
        assertEquals("무엇을 먹었는지 적어주세요", result.firstErrorMessage)
    }

    @Test
    fun validateMealEntryInput_rejectsNonPositiveCalories() {
        val zeroCalories = validateMealEntryInput(foodName = "닭가슴살", caloriesText = "0")
        val invalidCalories = validateMealEntryInput(foodName = "닭가슴살", caloriesText = "abc")

        assertFalse(zeroCalories.isValid)
        assertEquals("칼로리는 1 이상 숫자로 입력해주세요", zeroCalories.caloriesError)

        assertFalse(invalidCalories.isValid)
        assertEquals("칼로리는 1 이상 숫자로 입력해주세요", invalidCalories.caloriesError)
    }

    @Test
    fun validateMealEntryInput_acceptsTrimmedValidInput() {
        val result = validateMealEntryInput(foodName = "  닭가슴살 샐러드  ", caloriesText = " 420 ")

        assertTrue(result.isValid)
        assertEquals(null, result.foodNameError)
        assertEquals(null, result.caloriesError)
        assertEquals(null, result.firstErrorMessage)
    }
}
