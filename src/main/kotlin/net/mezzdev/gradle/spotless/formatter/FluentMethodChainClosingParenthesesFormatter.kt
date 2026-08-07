package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.TextBlockLiteralExpr
import java.io.Serial
import java.io.Serializable

object FluentMethodChainClosingParenthesesFormatter : FormatterFunc, Serializable {
	private const val MAX_COLLAPSED_LINE_LENGTH = 160

	@Serial
	private fun readResolve(): Any = FluentMethodChainClosingParenthesesFormatter

	override fun apply(source: String): String {
		val originalCompilationUnit = JavaParserFormatterSupport.parseOrNull(source) ?: return source
		val expandedSource = expandWrappedChainedCallArguments(source, originalCompilationUnit)
		val compilationUnit = if (expandedSource == source) {
			originalCompilationUnit
		} else {
			JavaParserFormatterSupport.parseOrNull(expandedSource) ?: return source
		}
		val tokenRange = compilationUnit.tokenRange.orElse(null) ?: return source
		val hasTrailingNewline = expandedSource.endsWith('\n')
		val lines = expandedSource.removeSuffix("\n").split('\n').toMutableList()
		val candidates = buildList {
			val openParentheses = mutableListOf<ParenthesisToken>()
			JavaParserFormatterSupport.significantTokens(tokenRange).forEach { token ->
				when (token.text) {
					"(" -> {
						val openParenthesisRange = token.requiredRange()
						openParentheses.add(
							ParenthesisToken(
								lineIndex = openParenthesisRange.begin.line - 1,
								columnIndex = openParenthesisRange.begin.column - 1
							)
						)
					}
					")" -> {
						val openParenthesis = openParentheses.removeLastOrNull() ?: return@forEach
						val closeParenthesisRange = token.requiredRange()
						if (closeParenthesisRange.begin.line == closeParenthesisRange.end.line) {
							add(
								Candidate(
									openLineIndex = openParenthesis.lineIndex,
									openColumnIndex = openParenthesis.columnIndex,
									closeLineIndex = closeParenthesisRange.begin.line - 1,
									closeColumnIndex = closeParenthesisRange.begin.column - 1
								)
							)
						}
					}
				}
			}
		}

		candidates.asReversed().forEach { candidate ->
			formatCandidate(lines, candidate)
		}

		return lines.joinToString("\n") + if (hasTrailingNewline) "\n" else ""
	}

	private fun expandWrappedChainedCallArguments(source: String, compilationUnit: CompilationUnit): String {
		val document = JavaParserFormatterSupport.SourceDocument(source)
		val candidates = compilationUnit.findAll(MethodCallExpr::class.java)
			.mapNotNull { methodCall ->
				wrappedChainedCallReplacement(source, document, methodCall)
			}
		val replacements = candidates.filterIndexed { index, candidate ->
			candidates.withIndex().none { (otherIndex, other) ->
				index != otherIndex && candidate.overlaps(other)
			}
		}
		if (replacements.isEmpty()) {
			return source
		}

		return buildString(source.length) {
			var sourceIndex = 0
			for (replacement in replacements.sortedBy { it.startOffset }) {
				append(source, sourceIndex, replacement.startOffset)
				append(replacement.text)
				sourceIndex = replacement.endOffsetExclusive
			}
			append(source, sourceIndex, source.length)
		}
	}

	private fun wrappedChainedCallReplacement(
		source: String,
		document: JavaParserFormatterSupport.SourceDocument,
		methodCall: MethodCallExpr
	): SourceReplacement? {
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
		return SourceReplacement(
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

	private fun formatCandidate(lines: MutableList<String>, candidate: Candidate) {
		if (candidate.openLineIndex >= candidate.closeLineIndex) {
			return
		}

		if (collapseShortChainedCall(lines, candidate)) {
			return
		}

		val closingLine = splitAttachedSelector(lines, candidate) ?: return
		if (!closingLine.isStandaloneClosingParenthesisLine()) {
			return
		}

		val nextLine = lines.getOrNull(candidate.closeLineIndex + 1) ?: return
		if (!nextLine.trimStart().startsWith(".")) {
			return
		}

		val closingIndent = closingLine.leadingWhitespace()
		val nextLineIndent = nextLine.leadingWhitespace()
		if (nextLineIndent == closingIndent || !nextLineIndent.startsWith(closingIndent)) {
			return
		}

		val firstLineToIndent = candidate.openLineIndex + 1
		for (lineIndex in firstLineToIndent..candidate.closeLineIndex) {
			val line = lines[lineIndex]
			if (line.isNotBlank() && !line.startsWith(closingIndent)) {
				return
			}
		}

		for (lineIndex in firstLineToIndent..candidate.closeLineIndex) {
			val line = lines[lineIndex]
			if (line.isNotBlank()) {
				lines[lineIndex] = nextLineIndent + line.substring(closingIndent.length)
			}
		}
	}

	private fun splitAttachedSelector(lines: MutableList<String>, candidate: Candidate): String? {
		val closingLine = lines.getOrNull(candidate.closeLineIndex) ?: return null
		if (candidate.closeColumnIndex >= closingLine.length) {
			return null
		}

		val suffixAfterCloseParenthesis = closingLine.substring(candidate.closeColumnIndex + 1)
		if (suffixAfterCloseParenthesis.isBlank()) {
			return closingLine
		}

		val trimmedSuffix = suffixAfterCloseParenthesis.trimStart()
		if (!trimmedSuffix.startsWith(".")) {
			return closingLine
		}

		val splitClosingLine = closingLine.substring(0, candidate.closeColumnIndex + 1)
		lines[candidate.closeLineIndex] = splitClosingLine
		lines.add(candidate.closeLineIndex + 1, "${closingLine.leadingWhitespace()}\t$trimmedSuffix")
		return splitClosingLine
	}

	private fun collapseShortChainedCall(lines: MutableList<String>, candidate: Candidate): Boolean {
		val openLine = lines.getOrNull(candidate.openLineIndex) ?: return false
		val closingLine = lines.getOrNull(candidate.closeLineIndex) ?: return false
		if (candidate.openColumnIndex >= openLine.length || candidate.closeColumnIndex >= closingLine.length) {
			return false
		}

		val suffixAfterCloseParenthesis = closingLine.substring(candidate.closeColumnIndex + 1)
		val chainLine = if (suffixAfterCloseParenthesis.isNotBlank()) {
			val trimmedSuffix = suffixAfterCloseParenthesis.trimStart()
			if (!trimmedSuffix.startsWith(".")) {
				return false
			}
			"${openLine.leadingWhitespace()}\t$trimmedSuffix"
		} else {
			val nextLine = lines.getOrNull(candidate.closeLineIndex + 1) ?: return false
			if (!nextLine.trimStart().startsWith(".")) {
				return false
			}
			null
		}

		val collapsedArguments = collapsedArguments(lines, candidate) ?: return false
		val collapsedCall = buildString {
			append(openLine.substring(0, candidate.openColumnIndex + 1).trimEnd())
			append(collapsedArguments)
			append(")")
		}
		if (collapsedCall.length > MAX_COLLAPSED_LINE_LENGTH) {
			return false
		}

		lines[candidate.openLineIndex] = collapsedCall
		repeat(candidate.closeLineIndex - candidate.openLineIndex) {
			lines.removeAt(candidate.openLineIndex + 1)
		}
		if (chainLine != null) {
			lines.add(candidate.openLineIndex + 1, chainLine)
		}
		return true
	}

	private fun collapsedArguments(lines: List<String>, candidate: Candidate): String? {
		val argumentLines = buildList {
			val openLine = lines[candidate.openLineIndex]
			val firstArgumentPart = openLine.substring(candidate.openColumnIndex + 1).trim()
			if (firstArgumentPart.isNotEmpty()) {
				add(firstArgumentPart)
			}

			for (lineIndex in candidate.openLineIndex + 1 until candidate.closeLineIndex) {
				val argumentLine = lines[lineIndex].trim()
				if (argumentLine.isEmpty()) {
					return null
				}
				add(argumentLine)
			}

			val closingLine = lines[candidate.closeLineIndex]
			val lastArgumentPart = closingLine.substring(0, candidate.closeColumnIndex).trim()
			if (lastArgumentPart.isNotEmpty()) {
				add(lastArgumentPart)
			}
		}
		if (argumentLines.any { it.containsLineCommentOrBlockComment() }) {
			return null
		}

		val arguments = argumentLines.joinToString(" ")
		if (arguments.any { it == '(' || it == ')' || it == '{' || it == '}' || it == ';' }) {
			return null
		}
		return arguments
	}

	private fun String.leadingWhitespace(): String {
		return takeWhile { it == '\t' || it == ' ' }
	}

	private fun String.containsLineCommentOrBlockComment(): Boolean {
		return contains("//") || contains("/*") || contains("*/")
	}

	private fun String.isStandaloneClosingParenthesisLine(): Boolean {
		val trimmed = trim()
		return trimmed == ")" ||
			(trimmed.endsWith(")") && trimmed.dropLast(1).all { it == '}' })
	}

	private data class ParenthesisToken(
		val lineIndex: Int,
		val columnIndex: Int
	)

	private data class Candidate(
		val openLineIndex: Int,
		val openColumnIndex: Int,
		val closeLineIndex: Int,
		val closeColumnIndex: Int
	)

	private data class SourceRange(
		val startOffset: Int,
		val endOffsetExclusive: Int
	)

	private data class SourceReplacement(
		val startOffset: Int,
		val endOffsetExclusive: Int,
		val text: String
	) {
		fun overlaps(other: SourceReplacement): Boolean {
			return startOffset < other.endOffsetExclusive && other.startOffset < endOffsetExclusive
		}
	}
}
