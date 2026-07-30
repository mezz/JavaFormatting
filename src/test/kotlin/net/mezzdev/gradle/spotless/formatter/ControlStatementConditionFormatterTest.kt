package net.mezzdev.gradle.spotless.formatter

import kotlin.test.Test
import kotlin.test.assertEquals

class ControlStatementConditionFormatterTest {
	@Test
	fun `multiline control statement conditions keep the condition start on the control line`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader()) {
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `multiline control statement condition formatting is idempotent`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(source, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `previous multiline control statement condition style is normalized`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (
			→→→!serverConnection.isJeiOnServer() &&
			→→→→serverConnection.isSameModLoader()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `previous brace-only line style is normalized`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader())
			→→{
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if (!serverConnection.isJeiOnServer() &&
			→→→serverConnection.isSameModLoader()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `real-world JEI compound conditions are normalized`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (serverConfig.isCheatModeEnabledForCreative() &&
			→→→sender.isCreative()) {
			→→→return true;
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if (serverConfig.isCheatModeEnabledForCreative() &&
			→→→sender.isCreative()
			→→) {
			→→→return true;
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `real-world JEI instanceof conditions are normalized from previous style`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (
			→→→menu instanceof AbstractCraftingMenu craftingMenu &&
			→→→→recipe.recipe() instanceof RecipeHolder<?> recipeHolder &&
			→→→→recipeHolder.value() instanceof CraftingRecipe
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if (menu instanceof AbstractCraftingMenu craftingMenu &&
			→→→recipe.recipe() instanceof RecipeHolder<?> recipeHolder &&
			→→→recipeHolder.value() instanceof CraftingRecipe
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `other multiline control statements use the same condition style`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→for (int i = 0;
			→→→i < values.size();
			→→→i++) {
			→→→run(i);
			→→}
			→→do {
			→→→run();
			→→} while (keepRunning() &&
			→→→hasWork());
			→→try (Reader reader = openReader();
			→→→Writer writer = openWriter()) {
			→→→run(reader, writer);
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→for (int i = 0;
			→→→i < values.size();
			→→→i++
			→→) {
			→→→run(i);
			→→}
			→→do {
			→→→run();
			→→} while (keepRunning() &&
			→→→hasWork());
			→→try (Reader reader = openReader();
			→→→Writer writer = openWriter()
			→→) {
			→→→run(reader, writer);
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `single-line control statement conditions are unchanged`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (isEnabled()) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(source, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `parentheses in strings characters and comments do not affect condition matching`() {
		val source = java(
			"""
			class Test {
			→void test(String value) {
			→→if (value.equals("(") &&
			→→→value.indexOf(')') >= 0 && // ignored )
			→→→value.endsWith(")")) {
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test(String value) {
			→→if (value.equals("(") &&
			→→→value.indexOf(')') >= 0 && // ignored )
			→→→value.endsWith(")")
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `block comments between control keyword and parenthesis are supported`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if /* comment with ) */ (firstCondition() &&
			→→→secondCondition()) {
			→→→run();
			→→}
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→void test() {
			→→if /* comment with ) */ (firstCondition() &&
			→→→secondCondition()
			→→) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(expected, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `nested multiline calls in conditions are left unchanged`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (firstCondition(
			→→→value
			→→) && secondCondition()) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(source, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `else while statements are left unchanged`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→if (hasWork()) {
			→→→run();
			→→} else while (keepRunning() &&
			→→→hasWork()) {
			→→→run();
			→→}
			→}
			}
			"""
		)

		assertEquals(source, ControlStatementConditionFormatter.apply(source))
	}

	@Test
	fun `try blocks without resources are not mistaken for control headers`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→try { run(firstCondition() &&
			→→→secondCondition()); }
			→}
			}
			"""
		)

		assertEquals(source, ControlStatementConditionFormatter.apply(source))
	}
}
