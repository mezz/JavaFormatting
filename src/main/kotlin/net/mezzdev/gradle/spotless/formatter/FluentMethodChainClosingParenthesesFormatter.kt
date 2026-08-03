package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import java.io.Serial
import java.io.Serializable

object FluentMethodChainClosingParenthesesFormatter : FormatterFunc, Serializable {
	private const val MAX_COLLAPSED_LINE_LENGTH = 160

	@Serial
	private fun readResolve(): Any = FluentMethodChainClosingParenthesesFormatter

	override fun apply(source: String): String {
		val compilationUnit = JavaParserFormatterSupport.parseOrNull(source) ?: return source
		val tokenRange = compilationUnit.tokenRange.orElse(null) ?: return source
		val hasTrailingNewline = source.endsWith('\n')
		val lines = source.removeSuffix("\n").split('\n').toMutableList()
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
}
