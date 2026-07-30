package net.mezzdev.gradle.spotless.formatter

object JavaSourceScanner {
	data class DelimiterDepth(
		var parentheses: Int = 0,
		var braces: Int = 0,
		var brackets: Int = 0
	) {
		fun isTopLevel(): Boolean {
			return parentheses == 0 && braces == 0 && brackets == 0
		}
	}

	fun splitTopLevelArguments(line: String): List<String> {
		val arguments = mutableListOf<String>()
		var argumentStart = 0
		val depth = DelimiterDepth()
		var state = ScanState.CODE
		var escaped = false
		for ((index, char) in line.withIndex()) {
			val result = scanChar(char, state, escaped, depth)
			state = result.state
			escaped = result.escaped
			if (state == ScanState.CODE && char == ',' && depth.isTopLevel()) {
				arguments.add(line.substring(argumentStart, index).trimEnd())
				argumentStart = index + 1
				while (argumentStart < line.length && line[argumentStart] == ' ') {
					argumentStart++
				}
			}
		}
		arguments.add(line.substring(argumentStart).trimEnd())
		return arguments
	}

	fun parenthesisDelta(line: String): Int {
		var state = ScanState.CODE
		var escaped = false
		var delta = 0
		for (char in line) {
			if (escaped) {
				escaped = false
				continue
			}
			when (state) {
				ScanState.CODE -> {
					when (char) {
						'"' -> state = ScanState.STRING
						'\'' -> state = ScanState.CHARACTER
						'(' -> delta++
						')' -> delta--
					}
				}
				ScanState.STRING -> {
					when (char) {
						'\\' -> escaped = true
						'"' -> state = ScanState.CODE
					}
				}
				ScanState.CHARACTER -> {
					when (char) {
						'\\' -> escaped = true
						'\'' -> state = ScanState.CODE
					}
				}
			}
		}
		return delta
	}

	fun unmatchedOpenParenIndex(line: String): Int? {
		val stack = mutableListOf<Int>()
		var state = ScanState.CODE
		var escaped = false
		for ((index, char) in line.withIndex()) {
			if (escaped) {
				escaped = false
				continue
			}
			when (state) {
				ScanState.CODE -> {
					when (char) {
						'"' -> state = ScanState.STRING
						'\'' -> state = ScanState.CHARACTER
						'(' -> stack.add(index)
						')' -> {
							if (stack.isNotEmpty()) {
								stack.removeLast()
							} else {
								return null
							}
						}
					}
				}
				ScanState.STRING -> {
					when (char) {
						'\\' -> escaped = true
						'"' -> state = ScanState.CODE
					}
				}
				ScanState.CHARACTER -> {
					when (char) {
						'\\' -> escaped = true
						'\'' -> state = ScanState.CODE
					}
				}
			}
		}
		return stack.singleOrNull()
	}

	fun nextWord(source: String, startIndex: Int): String {
		var index = startIndex
		while (index < source.length && source[index].isWhitespace()) {
			index++
		}
		val wordStart = index
		while (
			index < source.length &&
			(source[index].isLetterOrDigit() || source[index] == '_' || source[index] == '$')
		) {
			index++
		}
		return source.substring(wordStart, index)
	}

	fun nextNonWhitespaceChar(source: String, startIndex: Int): Char? {
		var index = startIndex
		while (index < source.length && source[index].isWhitespace()) {
			index++
		}
		return source.getOrNull(index)
	}

	fun lineNumber(source: String, index: Int): Int {
		return source.take(index).count { it == '\n' } + 1
	}

	private fun scanChar(
		char: Char,
		state: ScanState,
		escaped: Boolean,
		depth: DelimiterDepth,
		onParen: (openParen: Boolean) -> Unit = {}
	): ScanResult {
		if (escaped) {
			return ScanResult(state, false)
		}
		return when (state) {
			ScanState.CODE -> {
				when (char) {
					'"' -> ScanResult(ScanState.STRING, false)
					'\'' -> ScanResult(ScanState.CHARACTER, false)
					'(' -> {
						depth.parentheses++
						onParen(true)
						ScanResult(state, false)
					}
					')' -> {
						depth.parentheses = (depth.parentheses - 1).coerceAtLeast(0)
						onParen(false)
						ScanResult(state, false)
					}
					'{' -> {
						depth.braces++
						ScanResult(state, false)
					}
					'}' -> {
						depth.braces = (depth.braces - 1).coerceAtLeast(0)
						ScanResult(state, false)
					}
					'[' -> {
						depth.brackets++
						ScanResult(state, false)
					}
					']' -> {
						depth.brackets = (depth.brackets - 1).coerceAtLeast(0)
						ScanResult(state, false)
					}
					else -> ScanResult(state, false)
				}
			}
			ScanState.STRING -> {
				when (char) {
					'\\' -> ScanResult(state, true)
					'"' -> ScanResult(ScanState.CODE, false)
					else -> ScanResult(state, false)
				}
			}
			ScanState.CHARACTER -> {
				when (char) {
					'\\' -> ScanResult(state, true)
					'\'' -> ScanResult(ScanState.CODE, false)
					else -> ScanResult(state, false)
				}
			}
		}
	}

	private data class ScanResult(
		val state: ScanState,
		val escaped: Boolean
	)

	private enum class ScanState {
		CODE,
		STRING,
		CHARACTER
	}
}
