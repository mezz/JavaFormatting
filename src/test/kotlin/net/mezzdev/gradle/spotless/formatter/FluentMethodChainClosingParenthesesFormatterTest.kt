package net.mezzdev.gradle.spotless.formatter

import kotlin.test.Test
import kotlin.test.assertEquals

class FluentMethodChainClosingParenthesesFormatterTest {
	@Test
	fun `short fluent chain receiver call is collapsed and selector is moved to continuation line`() {
		val source = java(
			"""
			class Test {
				void test() {
					lookupHistoryEnabled = lookups.addBoolean(
						"enabled",
						false
					).setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
				void test() {
					lookupHistoryEnabled = lookups.addBoolean("enabled", false)
						.setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `short fluent chain receiver call is collapsed when selector is already on continuation line`() {
		val source = java(
			"""
			class Test {
				void test() {
					lookupHistoryEnabled = lookups.addBoolean(
						"enabled",
						false
					)
						.setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
				void test() {
					lookupHistoryEnabled = lookups.addBoolean("enabled", false)
						.setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `multiline fluent chain call body is indented to match selector continuation`() {
		val source = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
						"addBookmarksToFrontEnabled",
						BookmarkAddPosition.END,
						enumSerializer(BookmarkAddPosition.class, Map.of(
							"false", BookmarkAddPosition.END,
							"true", BookmarkAddPosition.FRONT
						))
					)
						.build();
				}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
							"addBookmarksToFrontEnabled",
							BookmarkAddPosition.END,
							enumSerializer(BookmarkAddPosition.class, Map.of(
								"false", BookmarkAddPosition.END,
								"true", BookmarkAddPosition.FRONT
							))
						)
						.build();
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `multiline attached fluent chain call body is split and indented to match selector continuation`() {
		val source = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
						"addBookmarksToFrontEnabled",
						BookmarkAddPosition.END,
						enumSerializer(BookmarkAddPosition.class, Map.of(
							"false", BookmarkAddPosition.END,
							"true", BookmarkAddPosition.FRONT
						))
					).build();
				}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
							"addBookmarksToFrontEnabled",
							BookmarkAddPosition.END,
							enumSerializer(BookmarkAddPosition.class, Map.of(
								"false", BookmarkAddPosition.END,
								"true", BookmarkAddPosition.FRONT
							))
						)
						.build();
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `attached selector after multiline lambda argument is split and indented to match selector continuation`() {
		val source = java(
			"""
			class Test {
				void test() {
					Comparator<String> minecraftCraftingFirst = Comparator.comparing((String s) -> {
						String vanillaCrafting = RecipeTypes.CRAFTING.getUid().toString();
						return s.equals(vanillaCrafting);
					}).reversed();
				}
			}
			"""
		)

		val expected = java(
			"""
			class Test {
				void test() {
					Comparator<String> minecraftCraftingFirst = Comparator.comparing((String s) -> {
							String vanillaCrafting = RecipeTypes.CRAFTING.getUid().toString();
							return s.equals(vanillaCrafting);
						})
						.reversed();
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `already aligned fluent chain call body is unchanged`() {
		val source = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
							"addBookmarksToFrontEnabled",
							BookmarkAddPosition.END,
							enumSerializer(BookmarkAddPosition.class, Map.of(
								"false", BookmarkAddPosition.END,
								"true", BookmarkAddPosition.FRONT
							))
						)
						.build();
				}
			}
			"""
		)

		assertEquals(source, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `non-chained multiline calls are unchanged`() {
		val source = java(
			"""
			class Test {
				void test() {
					bookmarkAddPosition = bookmarks.addValue(
						"addBookmarksToFrontEnabled",
						BookmarkAddPosition.END
					);
				}
			}
			"""
		)

		assertEquals(source, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}

	@Test
	fun `text block contents are unchanged`() {
		val source = java(
			"""
			class Test {
				String test() {
					return ${"\"\"\""}
					)
						.build();
					${"\"\"\""};
				}
			}
			"""
		)

		assertEquals(source, FluentMethodChainClosingParenthesesFormatter.apply(source))
	}
}
