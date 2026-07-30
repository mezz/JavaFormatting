package net.mezzdev.gradle.spotless.formatter

internal fun java(source: String): String {
	return source.trimIndent()
		.replace("→", "\t") + "\n"
}
