package net.mezzdev.gradle.spotless.formatter

internal const val MAX_COLLAPSED_LINE_LENGTH = 160

internal data class SourceEdit(
	val startOffset: Int,
	val endOffsetExclusive: Int,
	val text: String
) {
	fun overlaps(other: SourceEdit): Boolean {
		return startOffset < other.endOffsetExclusive && other.startOffset < endOffsetExclusive
	}
}

internal fun applySourceEdits(source: String, edits: Collection<SourceEdit>): String {
	val sortedEdits = edits.sortedByDescending { it.startOffset }
	for (edit in sortedEdits) {
		require(edit.startOffset in 0..edit.endOffsetExclusive)
		require(edit.endOffsetExclusive <= source.length)
	}
	for ((laterEdit, earlierEdit) in sortedEdits.zipWithNext()) {
		require(earlierEdit.endOffsetExclusive <= laterEdit.startOffset) {
			"Source edits must not overlap"
		}
	}
	return sortedEdits.fold(source) { formattedSource, edit ->
		formattedSource.replaceRange(edit.startOffset, edit.endOffsetExclusive, edit.text)
	}
}
