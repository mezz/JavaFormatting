package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import com.github.javaparser.ast.expr.ConditionalExpr
import java.io.Serial
import java.io.Serializable

object NoTernaryOperatorFormatter : FormatterFunc, Serializable {
	@Serial
	private fun readResolve(): Any = NoTernaryOperatorFormatter

	override fun apply(source: String): String {
		val compilationUnit = JavaParserFormatterSupport.parseOrThrow(source, "checking for ternary operators")
		val ternaryOperator = compilationUnit.findAll(ConditionalExpr::class.java).firstOrNull()
			?: return source
		val line = ternaryOperator.range
			.map { it.begin.line }
			.orElse(1)
		throw IllegalArgumentException("Ternary operators are banned. Use if/else or a helper method. Line: $line")
	}
}
