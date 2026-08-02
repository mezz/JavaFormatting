package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import java.io.Serial
import java.io.Serializable

object FluentMethodChainClosingParenthesesFormatter : FormatterFunc, Serializable {
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
					"(" -> openParentheses.add(ParenthesisToken(token.requiredRange().begin.line - 1))
					")" -> {
						val openParenthesis = openParentheses.removeLastOrNull() ?: return@forEach
						val closeParenthesisRange = token.requiredRange()
						if (closeParenthesisRange.begin.line == closeParenthesisRange.end.line) {
							add(
								Candidate(
									openLineIndex = openParenthesis.lineIndex,
									closeLineIndex = closeParenthesisRange.begin.line - 1
								)
							)
						}
					}
				}
			}
		}

		candidates.forEach { candidate ->
			formatCandidate(lines, candidate)
		}

		return lines.joinToString("\n") + if (hasTrailingNewline) "\n" else ""
	}

	private fun formatCandidate(lines: MutableList<String>, candidate: Candidate) {
		if (candidate.openLineIndex >= candidate.closeLineIndex) {
			return
		}

		val closingLine = lines.getOrNull(candidate.closeLineIndex) ?: return
		if (closingLine.trim() != ")") {
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

	private fun String.leadingWhitespace(): String {
		return takeWhile { it == '\t' || it == ' ' }
	}

	private data class ParenthesisToken(
		val lineIndex: Int
	)

	private data class Candidate(
		val openLineIndex: Int,
		val closeLineIndex: Int
	)
}
