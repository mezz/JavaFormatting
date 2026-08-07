package net.mezzdev.gradle.spotless.formatter

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.TextBlockLiteralExpr

internal object WrappedChainedCallArgumentFormatter {
	fun format(source: String, compilationUnit: CompilationUnit): String {
		val document = JavaParserFormatterSupport.SourceDocument(source)
		val candidates = compilationUnit.findAll(MethodCallExpr::class.java)
			.mapNotNull { methodCall ->
				createEdit(source, document, methodCall)
			}
		val edits = candidates.filter { candidate ->
			candidates.count { it.overlaps(candidate) } == 1
		}
		return applySourceEdits(source, edits)
	}

	private fun createEdit(
		source: String,
		document: JavaParserFormatterSupport.SourceDocument,
		methodCall: MethodCallExpr
	): SourceEdit? {
		if (methodCall.arguments.size < 2) {
			return null
		}
		val parentCall = methodCall.parentNode.orElse(null) as? MethodCallExpr ?: return null
		if (parentCall.scope.orElse(null) !== methodCall) {
			return null
		}

		val argumentRanges = methodCall.arguments.map { argument ->
			if (argument.findAll(TextBlockLiteralExpr::class.java).isNotEmpty()) {
				return null
			}
			argument.range.orElse(null) ?: return null
		}
		if (argumentRanges.none { range -> range.begin.line < range.end.line }) {
			return null
		}
		if (argumentRanges.zipWithNext().none { (first, second) -> first.end.line == second.begin.line }) {
			return null
		}

		val methodCallRange = methodCall.range.orElse(null) ?: return null
		val methodNameRange = methodCall.name.range.orElse(null) ?: return null
		val openParenthesisOffset = document.offset(methodNameRange.end) + 1
		val closeParenthesisOffset = document.offset(methodCallRange.end)
		if (source.getOrNull(openParenthesisOffset) != '(' || source.getOrNull(closeParenthesisOffset) != ')') {
			return null
		}

		val argumentOffsets = argumentRanges.map { range ->
			SourceRange(
				startOffset = document.offset(range.begin),
				endOffsetExclusive = document.offset(range.end) + 1
			)
		}
		if (!source.substring(openParenthesisOffset + 1, argumentOffsets.first().startOffset).isBlank()) {
			return null
		}
		for ((first, second) in argumentOffsets.zipWithNext()) {
			val separator = source.substring(first.endOffsetExclusive, second.startOffset)
			if (!separator.matches(Regex(",\\s*"))) {
				return null
			}
		}
		if (!source.substring(argumentOffsets.last().endOffsetExclusive, closeParenthesisOffset).isBlank()) {
			return null
		}

		val lineStartOffset = source.lastIndexOf('\n', openParenthesisOffset - 1) + 1
		val callIndent = source.substring(lineStartOffset, openParenthesisOffset)
			.takeWhile { it == '\t' || it == ' ' }
		val argumentIndent = "$callIndent\t"
		val arguments = argumentOffsets.map { range ->
			val argumentSource = source.substring(range.startOffset, range.endOffsetExclusive)
			reindentArgument(argumentSource, callIndent, argumentIndent) ?: return null
		}
		val replacement = buildString {
			append("(\n")
			arguments.forEachIndexed { index, argument ->
				append(argument)
				if (index < arguments.lastIndex) {
					append(',')
				}
				append('\n')
			}
			append(callIndent)
			append(')')
		}
		return SourceEdit(
			startOffset = openParenthesisOffset,
			endOffsetExclusive = closeParenthesisOffset + 1,
			text = replacement
		)
	}

	private fun reindentArgument(argumentSource: String, callIndent: String, argumentIndent: String): String? {
		val lines = argumentSource.split('\n')
		return buildList {
			add("$argumentIndent${lines.first().trimStart()}")
			for (line in lines.drop(1)) {
				if (line.isBlank()) {
					add("")
				} else {
					if (!line.startsWith(callIndent)) {
						return null
					}
					add(argumentIndent + line.substring(callIndent.length))
				}
			}
		}.joinToString("\n")
	}

	private data class SourceRange(
		val startOffset: Int,
		val endOffsetExclusive: Int
	)
}
