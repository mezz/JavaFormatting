package net.mezzdev.gradle.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpotlessFormattingPluginTest {
	@TempDir
	lateinit var tempDir: Path

	@Test
	fun `applying plugin applies spotless and creates java formatting extension`() {
		val project = newProject()

		project.plugins.apply(SpotlessFormattingPlugin::class.java)

		assertNotNull(project.extensions.findByType(SpotlessExtension::class.java))
		assertNotNull(project.extensions.findByType(JavaFormattingExtension::class.java))
	}

	@Test
	fun `project extension can configure every rule`() {
		val project = newProject()
		project.plugins.apply(SpotlessFormattingPlugin::class.java)
		val javaFormatting = project.extensions.getByType(JavaFormattingExtension::class.java)

		javaFormatting.target("src/generated/java/**/*.java")
		javaFormatting.targetJavaSources()
		javaFormatting.baseSpotlessRules()
		javaFormatting.eclipseFormatter()
		javaFormatting.leadingTabs()
		javaFormatting.singleExpressionLambdaCalls()
		javaFormatting.mixinAnnotationArguments()
		javaFormatting.multilineControlStatementConditions()
		javaFormatting.noTernaryOperators()
		javaFormatting.javadocIndentationFixes()
		javaFormatting.customRules()
		javaFormatting.all()

		evaluate(project)

		assertNotNull(project.extensions.findByType(SpotlessExtension::class.java))
	}

	@Test
	fun `spotless extension can configure every rule`() {
		val project = newProject()
		project.plugins.apply(SpotlessFormattingPlugin::class.java)
		val spotless = project.extensions.getByType(SpotlessExtension::class.java)

		spotless.javaFormatting(project) {
			target("src/generated/java/**/*.java")
			targetJavaSources()
			baseSpotlessRules()
			eclipseFormatter()
			leadingTabs()
			singleExpressionLambdaCalls()
			mixinAnnotationArguments()
			multilineControlStatementConditions()
			noTernaryOperators()
			javadocIndentationFixes()
			customRules()
			all()
		}

		evaluate(project)

		assertNotNull(project.extensions.findByType(SpotlessExtension::class.java))
	}

	@Test
	fun `project spotless config file is preferred over bundled config file`() {
		val project = newProject()
		val spotlessConfigDir = project.projectDir.resolve("config/spotless")
		Files.createDirectories(spotlessConfigDir.toPath())
		Files.writeString(spotlessConfigDir.resolve("eclipse-java.properties").toPath(), "custom=true\n")
		project.plugins.apply(SpotlessFormattingPlugin::class.java)
		val javaFormatting = project.extensions.getByType(JavaFormattingExtension::class.java)

		javaFormatting.eclipseFormatter()

		evaluate(project)

		assertTrue(spotlessConfigDir.resolve("eclipse-java.properties").isFile)
	}

	@Test
	fun `rules are safe to configure more than once on the same spotless java block`() {
		val project = newProject()
		project.plugins.apply(SpotlessFormattingPlugin::class.java)
		val spotless = project.extensions.getByType(SpotlessExtension::class.java)

		spotless.java {
			noTernaryOperators()
			noTernaryOperators()
			baseSpotlessRules()
			baseSpotlessRules()
		}

		evaluate(project)
	}

	private fun newProject(): Project {
		val projectDir = Files.createTempDirectory(tempDir, "project").toFile()
		return ProjectBuilder.builder()
			.withProjectDir(projectDir)
			.build()
			.also { project ->
				project.plugins.apply("java")
			}
	}

	private fun evaluate(project: Project) {
		(project as ProjectInternal).evaluate()
	}
}
