package net.mezzdev.gradle.spotless

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
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
			→private final boolean flag = true;

			→int value() {
			→→return flag ? 1 : 2;
			→}
			}
			""".trimIndent()
				.replace("→", "\t") + "\n"
		)
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
