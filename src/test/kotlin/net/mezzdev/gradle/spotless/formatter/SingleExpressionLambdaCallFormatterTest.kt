package net.mezzdev.gradle.spotless.formatter

import kotlin.test.Test
import kotlin.test.assertEquals

class SingleExpressionLambdaCallFormatterTest {
	@Test
	fun `single-expression lambda closing parentheses are collapsed when short enough`() {
		val source = java(
			"""
			class Test {
			→Object test() {
			→→return ingredientManager.getIngredientTypeForUid(link.ingredientTypeUid())
			→→→.flatMap(ingredientType -> resolveTypedIngredient(ingredientType, link.ingredientUid(), ingredientManager)
			→→→);
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→Object test() {
			→→return ingredientManager.getIngredientTypeForUid(link.ingredientTypeUid())
			→→→.flatMap(ingredientType -> resolveTypedIngredient(ingredientType, link.ingredientUid(), ingredientManager));
			→}
			}
			"""
		)

		assertEquals(expected, SingleExpressionLambdaCallFormatter.apply(source))
	}

	@Test
	fun `continued stream-chain lambda calls are unchanged`() {
		val source = java(
			"""
			class Test {
			→Object test() {
			→→return getSlots().stream()
			→→→.filter(slot -> slot.getSlotName().map(slotName::equals).orElse(false)
			→→→)
			→→→.findFirst();
			→}
			}
			"""
		)

		assertEquals(source, SingleExpressionLambdaCallFormatter.apply(source))
	}

	@Test
	fun `long single-expression lambda calls are split after the call opening parenthesis`() {
		val source = java(
			"""
			class Test {
			→Object test() {
			→→return ingredientManager.getIngredientTypeForUid(link.ingredientTypeUid()).flatMap(ingredientType -> resolveTypedIngredient(ingredientType, link.ingredientUid(), ingredientManager, firstFallbackIngredient, secondFallbackIngredient, thirdFallbackIngredient)
			→→);
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→Object test() {
			→→return ingredientManager.getIngredientTypeForUid(link.ingredientTypeUid()).flatMap(
			→→→ingredientType -> resolveTypedIngredient(ingredientType, link.ingredientUid(), ingredientManager, firstFallbackIngredient, secondFallbackIngredient, thirdFallbackIngredient)
			→→);
			→}
			}
			"""
		)

		assertEquals(expected, SingleExpressionLambdaCallFormatter.apply(source))
	}

	@Test
	fun `non-lambda call closing parentheses are unchanged`() {
		val source = java(
			"""
			class Test {
			→Object test() {
			→→return ingredientManager.getIngredientTypeForUid(link.ingredientTypeUid())
			→→→.flatMap(resolveTypedIngredient(ingredientType, link.ingredientUid(), ingredientManager)
			→→→);
			→}
			}
			"""
		)

		assertEquals(source, SingleExpressionLambdaCallFormatter.apply(source))
	}
}
