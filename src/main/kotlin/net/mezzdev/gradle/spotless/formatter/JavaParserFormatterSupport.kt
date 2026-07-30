package net.mezzdev.gradle.spotless.formatter

import com.github.javaparser.JavaParser
import com.github.javaparser.JavaToken
import com.github.javaparser.ParseResult
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Position
import com.github.javaparser.ast.CompilationUnit

object JavaParserFormatterSupport {
	private fun parser(): JavaParser {
		val parserConfiguration = ParserConfiguration()
			.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
			.setStoreTokens(true)
			.setTabSize(1)
		return JavaParser(parserConfiguration)
	}

	fun parseCompilationUnit(source: String): ParseResult<CompilationUnit> {
		return parser().parse(source)
	}

	fun parseOrNull(source: String): CompilationUnit? {
		val parseResult = parseCompilationUnit(source)
		if (!parseResult.isSuccessful) {
			return null
		}
		return parseResult.result.orElse(null)
	}

	fun parseOrThrow(source: String, action: String): CompilationUnit {
		val parseResult = parseCompilationUnit(source)
		if (parseResult.isSuccessful) {
			return parseResult.result.orElseThrow()
		}
		val problems = parseResult.problems.joinToString("; ") { it.message }
		throw IllegalArgumentException("Unable to parse Java source while $action: $problems")
	}

	fun significantTokens(tokens: Iterable<JavaToken>): List<JavaToken> {
		return tokens.filter { !it.category.isWhitespaceOrComment }
	}

	class SourceDocument(
		val source: String
	) {
		val lines: List<String> = source.removeSuffix("\n").split('\n')
		private val lineStartOffsets: IntArray = buildLineStartOffsets(source)

		fun offset(position: Position): Int {
			val lineIndex = position.line - 1
			require(lineIndex in lineStartOffsets.indices) {
				"Position line ${position.line} is outside the source document"
			}
			return lineStartOffsets[lineIndex] + position.column - 1
		}

		private fun buildLineStartOffsets(source: String): IntArray {
			val offsets = mutableListOf(0)
			source.forEachIndexed { index, char ->
				if (char == '\n' && index + 1 < source.length) {
					offsets.add(index + 1)
				}
			}
			return offsets.toIntArray()
		}
	}
}

fun JavaToken.requiredRange() = range.orElseThrow {
	IllegalStateException("JavaParser did not provide source ranges for token: $text")
}
