package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import java.io.Serial
import java.io.Serializable

object SingleExpressionLambdaCallFormatter : FormatterFunc, Serializable {
	private const val MAX_COLLAPSED_LINE_LENGTH = 160

	@Serial
	private fun readResolve(): Any = SingleExpressionLambdaCallFormatter

	override fun apply(source: String): String {
		val hasTrailingNewline = source.endsWith('\n')
		val lines = source.removeSuffix("\n").split('\n')
		return buildList {
			var index = 0
			while (index < lines.size) {
				val line = lines[index]
				val nextLine = lines.getOrNull(index + 1)
				val unmatchedOpenParenIndex = JavaSourceScanner.unmatchedOpenParenIndex(line)
				if (
					nextLine?.trim() == ");" &&
					line.contains("->") &&
					line.trimEnd().endsWith(")") &&
					unmatchedOpenParenIndex != null
				) {
					val collapsedLine = "${line});"
					if (collapsedLine.length <= MAX_COLLAPSED_LINE_LENGTH) {
						add(collapsedLine)
					} else {
						val indent = line.takeWhile { it == '\t' || it == ' ' }
						add(line.substring(0, unmatchedOpenParenIndex + 1))
						add("$indent\t${line.substring(unmatchedOpenParenIndex + 1).trimStart()}")
						add("$indent);")
					}
					index += 2
				} else {
					add(line)
					index++
				}
			}
		}.joinToString("\n") + if (hasTrailingNewline) "\n" else ""
	}
}
