package net.mezzdev.gradle.spotless.formatter

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NoTernaryOperatorFormatterTest {
	@Test
	fun `ternary operators are banned`() {
		val exception = assertFailsWith<IllegalArgumentException> {
			NoTernaryOperatorFormatter.apply(java("class Test { int value = flag ? 1 : 2; }"))
		}

		assertContains(exception.message ?: "", "Ternary operators are banned")
	}

	@Test
	fun `ternary operator error reports source line`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→int color = filterEmpty.getAsBoolean() ? 0xFFFF0000 : 0xFFFFFFFF;
			→}
			}
			"""
		)

		val exception = assertFailsWith<IllegalArgumentException> {
			NoTernaryOperatorFormatter.apply(source)
		}

		assertContains(exception.message ?: "", "Line: 3")
	}

	@Test
	fun `ternary operator check rejects real-world JEI ternaries`() {
		val source = java(
			"""
			class Test {
			→void test() {
			→→String brewingStepsString = brewingSteps < Integer.MAX_VALUE ? Integer.toString(brewingSteps) : "?";
			→→Level logLevel = Services.PLATFORM.getModHelper().isInDev() ? Level.WARN : Level.DEBUG;
			→}
			}
			"""
		)

		val exception = assertFailsWith<IllegalArgumentException> {
			NoTernaryOperatorFormatter.apply(source)
		}

		assertContains(exception.message ?: "", "Ternary operators are banned")
	}

	@Test
	fun `ternary operator check ignores wildcards comments strings characters and method references`() {
		val source = java(
			"""
			class Test {
			→List<?> unboundedWildcard;
			→List<? extends String> extendsWildcard;
			→Map<String, ? super Object> superWildcard;
			→Function<?, ?> functionWildcard;
			→String text = "? :";
			→char question = '?';
			→// flag ? 1 : 2
			→/* flag ? 1 : 2 */
			→Runnable runnable = Test::run;
			}
			"""
		)

		assertEquals(source, NoTernaryOperatorFormatter.apply(source))
	}

	@Test
	fun `ternary operator check ignores text blocks`() {
		val source = java(
			"""
			class Test {
			→String text = ${'"'}${'"'}${'"'}
			→→flag ? 1 : 2
			→${'"'}${'"'}${'"'};
			}
			"""
		)

		assertEquals(source, NoTernaryOperatorFormatter.apply(source))
	}
}
