package net.mezzdev.gradle.spotless

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotlessFormattingPluginFunctionalTest {
	@TempDir
	lateinit var projectDir: Path

	@Test
	fun `applying the plugin does not apply java formatting rules by default`() {
		writeConsumerBuild(
			"""
			plugins {
				java
				id("net.mezzdev.java-formatting")
			}
			"""
		)
		writeTernarySource()

		val result = runGradle("spotlessCheck")

		assertTrue(result.task(":spotlessCheck")?.outcome in setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE))
	}

	@Test
	fun `all applies every java formatting rule`() {
		writeConsumerBuild(
			"""
			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			javaFormatting {
				all()
			}
			"""
		)
		writeTernarySource()

		val result = runGradleAndFail("spotlessCheck")

		assertContains(result.output, "Ternary operators are banned")
	}

	@Test
	fun `individual custom rules can be applied without all rules`() {
		writeConsumerBuild(
			"""
			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			javaFormatting {
				noTernaryOperators()
			}
			"""
		)
		writeTernarySource()

		val result = runGradleAndFail("spotlessCheck")

		assertContains(result.output, "Ternary operators are banned")
	}

	@Test
	fun `custom rules can be applied from the spotless java block`() {
		writeConsumerBuild(
			"""
			import net.mezzdev.gradle.spotless.noTernaryOperators

			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			spotless {
				java {
					noTernaryOperators()
				}
			}
			"""
		)
		writeTernarySource()

		val result = runGradleAndFail("spotlessCheck")

		assertContains(result.output, "Ternary operators are banned")
	}

	@Test
	fun `custom rules can be applied from the spotless block`() {
		writeConsumerBuild(
			"""
			import net.mezzdev.gradle.spotless.javaFormatting

			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			spotless {
				javaFormatting(project) {
					noTernaryOperators()
				}
			}
			"""
		)
		writeTernarySource()

		val result = runGradleAndFail("spotlessCheck")

		assertContains(result.output, "Ternary operators are banned")
	}

	@Test
	fun `rules are deduped across project extension and spotless block configuration`() {
		writeConsumerBuild(
			"""
			import net.mezzdev.gradle.spotless.noTernaryOperators

			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			javaFormatting {
				noTernaryOperators()
			}

			spotless {
				java {
					noTernaryOperators()
				}
			}
			"""
		)
		writeTernarySource()

		val result = runGradleAndFail("spotlessCheck")

		assertContains(result.output, "Ternary operators are banned")
	}

	@Test
	fun `bundled eclipse formatter survives clean and spotless check in one invocation`() {
		writeConsumerBuild(
			"""
			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			javaFormatting {
				eclipseFormatter()
			}
			"""
		)
		writeTernarySource()

		runGradle("spotlessApply")
		val result = runGradle("clean", "spotlessCheck")

		assertTrue(result.task(":spotlessCheck")?.outcome in setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE))
		assertTrue(Files.isRegularFile(projectDir.resolve(".gradle/mezz-spotless-formatting/eclipse-java.properties")))
		assertTrue(Files.notExists(projectDir.resolve("build/mezz-spotless-formatting/eclipse-java.properties")))
	}

	@Test
	fun `all aligns multiline fluent chain closing parentheses with selector continuation`() {
		writeConsumerBuild(
			"""
			plugins {
				java
				id("net.mezzdev.java-formatting")
			}

			javaFormatting {
				all()
			}
			"""
		)
		val sourceFile = writeFluentChainSource()

		runGradle("spotlessApply")

		val expected = """
			import java.util.Map;

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
						.addValue(7)
						.setTrusted()
						.build();
					lookupHistoryEnabled = lookups.addBoolean("enabled", false)
						.setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			""".trimIndent() + "\n"
		assertEquals(expected, Files.readString(sourceFile))
	}

	private fun writeConsumerBuild(buildFile: String) {
		Files.writeString(
			projectDir.resolve("settings.gradle.kts"),
			"""
			pluginManagement {
				repositories {
					gradlePluginPortal()
					mavenCentral()
				}
			}

			dependencyResolutionManagement {
				repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
				repositories {
					mavenCentral()
				}
			}

			rootProject.name = "consumer"
			""".trimIndent()
		)
		Files.writeString(
			projectDir.resolve("build.gradle.kts"),
			buildFile.trimIndent()
		)
	}

	private fun writeTernarySource() {
		val sourceDir = projectDir.resolve("src/main/java")
		Files.createDirectories(sourceDir)
		Files.writeString(
			sourceDir.resolve("Test.java"),
			"""
			class Test {
				private final boolean flag = true;

				int value() {
					return flag ? 1 : 2;
				}
			}
			""".trimIndent() + "\n"
		)
	}

	private fun writeFluentChainSource(): Path {
		val sourceDir = projectDir.resolve("src/main/java")
		Files.createDirectories(sourceDir)
		val sourceFile = sourceDir.resolve("Test.java")
		Files.writeString(
			sourceFile,
			"""
			import java.util.Map;

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
						.addValue(7)
						.setTrusted()
						.build();
					lookupHistoryEnabled = lookups.addBoolean(
						"enabled",
						false
					).setEditMode(ConfigValueEditMode.IMMEDIATE)
						.build();
				}
			}
			""".trimIndent() + "\n"
		)
		return sourceFile
	}

	private fun runGradle(vararg arguments: String) =
		gradleRunner(arguments.toList()).build()

	private fun runGradleAndFail(vararg arguments: String) =
		gradleRunner(arguments.toList()).buildAndFail()

	private fun gradleRunner(arguments: List<String>): GradleRunner =
		GradleRunner.create()
			.withProjectDir(projectDir.toFile())
			.withArguments(arguments + "--stacktrace")
			.withPluginClasspath()
}
