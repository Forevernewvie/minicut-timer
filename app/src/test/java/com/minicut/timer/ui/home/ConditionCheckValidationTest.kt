package com.minicut.timer.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionCheckValidationTest {

    @Test
    fun validateConditionCheckInput_rejectsZeroOnlyInputs() {
        val result =
            validateConditionCheckInput(
                bodyWeightText = "0",
                proteinText = "0",
                resistanceSetsText = "0",
            )

        assertFalse(result.isValid)
        assertEquals("체중은 0보다 큰 값으로 입력해주세요.", result.errorMessage)
    }

    @Test
    fun validateConditionCheckInput_rejectsBlankAllInputs() {
        val result =
            validateConditionCheckInput(
                bodyWeightText = "   ",
                proteinText = "",
                resistanceSetsText = "",
            )

        assertFalse(result.isValid)
        assertEquals("체중/단백질/세트 중 최소 1개는 입력해야 저장됩니다.", result.errorMessage)
    }

    @Test
    fun validateConditionCheckInput_acceptsPositiveNumbers() {
        val result =
            validateConditionCheckInput(
                bodyWeightText = "79.4",
                proteinText = "160",
                resistanceSetsText = "12",
            )

        assertTrue(result.isValid)
        assertEquals(79.4f, result.bodyWeightKg)
        assertEquals(160, result.proteinGrams)
        assertEquals(12, result.resistanceSets)
    }
}
