package net.mezzdev.gradle.spotless.formatter

import com.diffplug.spotless.FormatterFunc
import com.github.javaparser.JavaToken
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.SwitchExpr
import com.github.javaparser.ast.stmt.CatchClause
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.SwitchStmt
import com.github.javaparser.ast.stmt.SynchronizedStmt
import com.github.javaparser.ast.stmt.TryStmt
import com.github.javaparser.ast.stmt.WhileStmt
import java.io.Serial
import java.io.Serializable

object ControlStatementConditionFormatter : FormatterFunc, Serializable {
	@Serial
	private fun readResolve(): Any = ControlStatementConditionFormatter

	override fun apply(source: String): String {
		val compilationUnit = JavaParserFormatterSupport.parseOrNull(source) ?: return source
		val document = JavaParserFormatterSupport.SourceDocument(source)
		val replacements = controlStatementHeaders(compilationUnit)
			.mapNotNull { header ->
				formatControlStatement(document, header)
			}
			.sortedByDescending { it.startOffset }

		return replacements.fold(source) { formattedSource, replacement ->
			formattedSource.replaceRange(replacement.startOffset, replacement.endOffset, replacement.text)
		}
	}

	private fun controlStatementHeaders(root: Node): List<ControlStatementHeader> {
		return buildList {
			root.findAll(IfStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "if") }
			root.findAll(ForStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "for") }
			root.findAll(ForEachStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "for") }
			root.findAll(WhileStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "while") }
			root.findAll(DoStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "while", searchLastKeyword = true) }
			root.findAll(SwitchStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "switch") }
			root.findAll(SwitchExpr::class.java)
				.mapNotNullTo(this) { findHeader(it, "switch") }
			root.findAll(CatchClause::class.java)
				.mapNotNullTo(this) { findHeader(it, "catch") }
			root.findAll(SynchronizedStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "synchronized") }
			root.findAll(TryStmt::class.java)
				.mapNotNullTo(this) { findHeader(it, "try") }
		}
	}

	private fun findHeader(
		node: Node,
		keyword: String,
		searchLastKeyword: Boolean = false
	): ControlStatementHeader? {
		val tokenRange = node.tokenRange.orElse(null) ?: return null
		val significantTokens = JavaParserFormatterSupport.significantTokens(tokenRange)
		val keywordTokenIndex = if (searchLastKeyword) {
			significantTokens.indexOfLast { it.text == keyword }
		} else {
			significantTokens.indexOfFirst { it.text == keyword }
		}
		if (keywordTokenIndex < 0) {
			return null
		}

		val openParenthesisTokenIndex = keywordTokenIndex + 1
		val openParenthesis = significantTokens.getOrNull(openParenthesisTokenIndex) ?: return null
		if (openParenthesis.text != "(") {
			return null
		}
		if (openParenthesis.requiredRange().begin.line != significantTokens[keywordTokenIndex].requiredRange().begin.line) {
			return null
		}

		val closeParenthesis = matchingCloseParenthesis(significantTokens, openParenthesisTokenIndex) ?: return null
		if (hasNestedMultilineExpression(significantTokens, openParenthesisTokenIndex, closeParenthesis)) {
			return null
		}
		return ControlStatementHeader(significantTokens[keywordTokenIndex], openParenthesis, closeParenthesis)
	}

	private fun matchingCloseParenthesis(tokens: List<JavaToken>, openParenthesisTokenIndex: Int): JavaToken? {
		var depth = 0
		for (tokenIndex in openParenthesisTokenIndex until tokens.size) {
			when (tokens[tokenIndex].text) {
				"(" -> depth++
				")" -> {
					depth--
					if (depth == 0) {
						return tokens[tokenIndex]
					}
					if (depth < 0) {
						return null
					}
				}
			}
		}
		return null
	}

	private fun hasNestedMultilineExpression(
		tokens: List<JavaToken>,
		openParenthesisTokenIndex: Int,
		closeParenthesis: JavaToken
	): Boolean {
		var currentLine = tokens[openParenthesisTokenIndex].requiredRange().begin.line
		var depth = 0
		for (tokenIndex in openParenthesisTokenIndex until tokens.size) {
			val token = tokens[tokenIndex]
			val line = token.requiredRange().begin.line
			if (line > currentLine) {
				if (depth != 1) {
					return true
				}
				currentLine = line
			}
			when (token.text) {
				"(" -> depth++
				")" -> {
					depth--
					if (token == closeParenthesis) {
						return false
					}
					if (depth < 0) {
						return true
					}
				}
			}
		}
		return true
	}

	private fun formatControlStatement(
		document: JavaParserFormatterSupport.SourceDocument,
		header: ControlStatementHeader
	): Replacement? {
		val lines = document.lines
		val openParenthesisRange = header.openParenthesis.requiredRange()
		val closeParenthesisRange = header.closeParenthesis.requiredRange()
		val startLineIndex = openParenthesisRange.begin.line - 1
		val closeLineIndex = closeParenthesisRange.begin.line - 1
		if (closeLineIndex == startLineIndex) {
			return null
		}

		val openParenthesisIndex = openParenthesisRange.begin.column - 1
		val closeParenthesisIndex = closeParenthesisRange.begin.column - 1
		val firstLine = lines[startLineIndex]
		if (!isSupportedHeaderPrefix(firstLine, header)) {
			return null
		}
		if (isElseWhileHeader(firstLine, header)) {
			return null
		}
		val indent = firstLine.takeWhile { it == '\t' || it == ' ' }
		val conditionLines = buildConditionLines(lines, startLineIndex, openParenthesisIndex, closeLineIndex, closeParenthesisIndex)
		if (conditionLines.size < 2) {
			return null
		}

		val outputLines = mutableListOf(
			firstLine.substring(0, openParenthesisIndex + 1).trimEnd() + conditionLines.first()
		)
		conditionLines.drop(1)
			.forEach { conditionLine ->
				outputLines.add("$indent\t$conditionLine")
			}

		val closingLine = lines[closeLineIndex]
		val suffix = closingLine.substring(closeParenthesisIndex + 1)
		val trimmedSuffix = suffix.trimStart()
		if (trimmedSuffix.startsWith("{")) {
			outputLines.add("$indent) $trimmedSuffix")
			return Replacement(
				document.offset(openParenthesisRange.begin.withColumn(1)),
				document.offset(closeParenthesisRange.begin.withColumn(closingLine.length + 1)),
				outputLines.joinToString("\n")
			)
		}

		val nextLine = lines.getOrNull(closeLineIndex + 1)
		if (suffix.isBlank() && nextLine != null && nextLine.trimStart().startsWith("{")) {
			outputLines.add("$indent) ${nextLine.trimStart()}")
			return Replacement(
				document.offset(openParenthesisRange.begin.withColumn(1)),
				document.offset(closeParenthesisRange.begin.withLine(closeLineIndex + 2).withColumn(nextLine.length + 1)),
				outputLines.joinToString("\n")
			)
		}

		val finalConditionLineIndex = outputLines.lastIndex
		outputLines[finalConditionLineIndex] += ")$suffix"
		return Replacement(
			document.offset(openParenthesisRange.begin.withColumn(1)),
			document.offset(closeParenthesisRange.begin.withColumn(closingLine.length + 1)),
			outputLines.joinToString("\n")
		)
	}

	private fun isSupportedHeaderPrefix(line: String, header: ControlStatementHeader): Boolean {
		val keywordColumn = header.keyword.requiredRange().begin.column - 1
		val prefix = line.substring(0, keywordColumn).trim()
		return when {
			prefix.isEmpty() -> true
			header.keyword.text == "if" && prefix == "} else" -> true
			header.keyword.text == "while" && prefix == "}" -> true
			else -> false
		}
	}

	private fun isElseWhileHeader(line: String, header: ControlStatementHeader): Boolean {
		if (header.keyword.text != "while") {
			return false
		}
		val keywordColumn = header.keyword.requiredRange().begin.column - 1
		return Regex("""\belse\b""").containsMatchIn(line.substring(0, keywordColumn))
	}

	private fun buildConditionLines(
		lines: List<String>,
		startLineIndex: Int,
		openParenthesisIndex: Int,
		closeLineIndex: Int,
		closeParenthesisIndex: Int
	): List<String> {
		val conditionLines = mutableListOf<String>()
		val firstLine = lines[startLineIndex]
			.substring(openParenthesisIndex + 1)
			.trimStart()
		if (firstLine.isNotBlank()) {
			conditionLines.add(firstLine)
		}

		for (lineIndex in startLineIndex + 1 until closeLineIndex) {
			val conditionLine = lines[lineIndex].trimStart()
			if (conditionLine.isNotBlank()) {
				conditionLines.add(conditionLine)
			}
		}

		val finalLine = lines[closeLineIndex]
			.substring(0, closeParenthesisIndex)
			.trimStart()
		if (finalLine.isNotBlank()) {
			conditionLines.add(finalLine)
		}
		return conditionLines
	}

	private data class ControlStatementHeader(
		val keyword: JavaToken,
		val openParenthesis: JavaToken,
		val closeParenthesis: JavaToken
	)

	private data class Replacement(
		val startOffset: Int,
		val endOffset: Int,
		val text: String
	)
}
