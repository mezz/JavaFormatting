package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import java.io.Serial
import java.io.Serializable

object MixinAnnotationArgumentFormatter : FormatterFunc, Serializable {
	private val MIXIN_ANNOTATIONS = listOf("@Inject(", "@ModifyVariable(")

	@Serial
	private fun readResolve(): Any = MixinAnnotationArgumentFormatter

	override fun apply(source: String): String {
		val hasTrailingNewline = source.endsWith('\n')
		val lines = source.removeSuffix("\n").split('\n')
		val result = mutableListOf<String>()
		var annotationDepth = 0
		for (line in lines) {
			val trimmedLine = line.trimStart()
			val startsMixinAnnotation = MIXIN_ANNOTATIONS.any { trimmedLine.startsWith(it) }
			if (annotationDepth == 0 && startsMixinAnnotation) {
				val expandedAnnotation = expandSingleLineMixinAnnotation(line)
				if (expandedAnnotation != null) {
					result.addAll(expandedAnnotation)
					continue
				}
			}

			if (annotationDepth > 0) {
				result.addAll(splitAnnotationArgumentLine(line))
			} else {
				result.add(line)
			}

			if (startsMixinAnnotation && annotationDepth == 0) {
				annotationDepth = JavaSourceScanner.parenthesisDelta(trimmedLine)
			} else if (annotationDepth > 0) {
				annotationDepth += JavaSourceScanner.parenthesisDelta(trimmedLine)
			}
			if (annotationDepth < 0) {
				annotationDepth = 0
			}
		}
		return result.joinToString("\n") + if (hasTrailingNewline) "\n" else ""
	}

	private fun expandSingleLineMixinAnnotation(line: String): List<String>? {
		val indent = line.takeWhile { it == '\t' || it == ' ' }
		val trimmedLine = line.substring(indent.length).trimEnd()
		val annotationStart = MIXIN_ANNOTATIONS.firstOrNull { trimmedLine.startsWith(it) } ?: return null
		if (trimmedLine == annotationStart || !trimmedLine.endsWith(")")) {
			return null
		}
		val arguments = JavaSourceScanner.splitTopLevelArguments(trimmedLine.substring(annotationStart.length, trimmedLine.length - 1))
		if (arguments.size < 2) {
			return null
		}
		return buildList {
			add("$indent$annotationStart")
			arguments.forEachIndexed { index, argument ->
				val separator = if (index < arguments.lastIndex) "," else ""
				add("$indent\t$argument$separator")
			}
			add("$indent)")
		}
	}

	private fun splitAnnotationArgumentLine(line: String): List<String> {
		val indent = line.takeWhile { it == '\t' || it == ' ' }
		val arguments = JavaSourceScanner.splitTopLevelArguments(line.substring(indent.length))
		if (arguments.size < 2) {
			return listOf(line)
		}
		return arguments.mapIndexed { index, argument ->
			val separator = if (index < arguments.lastIndex) "," else ""
			"$indent$argument$separator"
		}
	}
}
