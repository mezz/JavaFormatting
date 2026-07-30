package net.mezzdev.gradle.spotless.formatter

import kotlin.test.Test
import kotlin.test.assertEquals

class MixinAnnotationArgumentFormatterTest {
	@Test
	fun `single-line inject annotations are expanded`() {
		val source = java(
			"""
			class Test {
			→@Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
			→void handleUpdateRecipes() {}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→@Inject(
			→→method = "handleUpdateRecipes",
			→→at = @At("RETURN")
			→)
			→void handleUpdateRecipes() {}
			}
			"""
		)

		assertEquals(expected, MixinAnnotationArgumentFormatter.apply(source))
	}

	@Test
	fun `single-line inject annotations with cancellable argument are expanded`() {
		val source = java(
			"""
			class Test {
			→@Inject(method = "componentHoverEffect", at = @At("HEAD"), cancellable = true)
			→private void jei${'$'}componentHoverEffect() {}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→@Inject(
			→→method = "componentHoverEffect",
			→→at = @At("HEAD"),
			→→cancellable = true
			→)
			→private void jei${'$'}componentHoverEffect() {}
			}
			"""
		)

		assertEquals(expected, MixinAnnotationArgumentFormatter.apply(source))
	}

	@Test
	fun `single-line modify variable annotations are expanded`() {
		val source = java(
			"""
			class Test {
			→@ModifyVariable(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V", name = "maxWidth", at = @At("STORE"))
			→public int modifyEffectWidth(int maxWidth) {
			→→return maxWidth;
			→}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→@ModifyVariable(
			→→method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
			→→name = "maxWidth",
			→→at = @At("STORE")
			→)
			→public int modifyEffectWidth(int maxWidth) {
			→→return maxWidth;
			→}
			}
			"""
		)

		assertEquals(expected, MixinAnnotationArgumentFormatter.apply(source))
	}

	@Test
	fun `mixin annotation arguments are split recursively across nested annotation lines`() {
		val source = java(
			"""
			class Test {
			→@Inject(
			→→method = "methodName", at = @At(
			→→→value = "INVOKE", target = "Target.method()V"
			→→), require = 0
			→)
			→void methodName() {}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
			→@Inject(
			→→method = "methodName",
			→→at = @At(
			→→→value = "INVOKE",
			→→→target = "Target.method()V"
			→→),
			→→require = 0
			→)
			→void methodName() {}
			}
			"""
		)

		assertEquals(expected, MixinAnnotationArgumentFormatter.apply(source))
	}

	@Test
	fun `mixin annotations with one argument are unchanged`() {
		val source = java(
			"""
			class Test {
			→@Inject(method = "methodName")
			→void methodName() {}
			}
			"""
		)

		assertEquals(source, MixinAnnotationArgumentFormatter.apply(source))
	}

	@Test
	fun `non-mixin annotations are unchanged`() {
		val source = java(
			"""
			class Test {
			→@Deprecated(forRemoval = true, since = "1.0")
			→void methodName() {}
			}
			"""
		)

		assertEquals(source, MixinAnnotationArgumentFormatter.apply(source))
	}
}
