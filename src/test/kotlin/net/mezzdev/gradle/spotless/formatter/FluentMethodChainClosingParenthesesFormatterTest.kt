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
	fun `wrapped chained call arguments containing lambdas are put on separate lines`() {
		val source = java(
			"""
			class Test {
				Object test(Object instance) {
					return instance.group(Codec.STRING.optionalFieldOf("group", "").forGetter((shapedRecipe) -> {
						return shapedRecipe.group;
					}), CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((shapedRecipe) -> {
						return shapedRecipe.category;
					}), ShapedRecipePattern.MAP_CODEC.forGetter((shapedRecipe) -> {
						return shapedRecipe.pattern;
					}), Codec.list(SlotDisplay.CODEC).fieldOf("display").forGetter((shapedRecipe) -> {
						return shapedRecipe.displays;
					}), ItemStackTemplate.CODEC.fieldOf("result").forGetter((shapedRecipe) -> {
						return shapedRecipe.result;
					}))
						.apply(instance, JeiShapedRecipe::new);
				}
			}
			"""
		)
		val expected = java(
			"""
			class Test {
				Object test(Object instance) {
					return instance.group(
							Codec.STRING.optionalFieldOf("group", "").forGetter((shapedRecipe) -> {
								return shapedRecipe.group;
							}),
							CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter((shapedRecipe) -> {
								return shapedRecipe.category;
							}),
							ShapedRecipePattern.MAP_CODEC.forGetter((shapedRecipe) -> {
								return shapedRecipe.pattern;
							}),
							Codec.list(SlotDisplay.CODEC).fieldOf("display").forGetter((shapedRecipe) -> {
								return shapedRecipe.displays;
							}),
							ItemStackTemplate.CODEC.fieldOf("result").forGetter((shapedRecipe) -> {
								return shapedRecipe.result;
							})
						)
						.apply(instance, JeiShapedRecipe::new);
				}
			}
			"""
		)

		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(source))
		assertEquals(expected, FluentMethodChainClosingParenthesesFormatter.apply(expected))
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
