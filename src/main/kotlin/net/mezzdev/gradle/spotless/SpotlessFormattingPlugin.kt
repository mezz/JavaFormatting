package net.mezzdev.gradle.spotless

import com.diffplug.gradle.spotless.JavaExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import net.mezzdev.gradle.spotless.formatter.ControlStatementConditionFormatter
import net.mezzdev.gradle.spotless.formatter.MixinAnnotationArgumentFormatter
import net.mezzdev.gradle.spotless.formatter.NoTernaryOperatorFormatter
import net.mezzdev.gradle.spotless.formatter.SingleExpressionLambdaCallFormatter
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.Collections
import java.util.WeakHashMap
import javax.inject.Inject

class SpotlessFormattingPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.pluginManager.apply("com.diffplug.spotless")
		project.extensions.create("javaFormatting", JavaFormattingExtension::class.java, project)
	}
}

fun SpotlessExtension.javaFormatting(project: Project, configure: JavaFormattingSpotlessExtension.() -> Unit) {
	java(object : Action<JavaExtension> {
		override fun execute(javaExtension: JavaExtension) {
			javaExtension.target(DEFAULT_JAVA_TARGET)
			javaExtension.javaFormatting(project, configure)
		}
	})
}

fun JavaExtension.javaFormatting(project: Project, configure: JavaFormattingSpotlessExtension.() -> Unit) {
	JavaFormattingSpotlessExtension(project, this).configure()
}

fun JavaExtension.all(project: Project) {
	baseSpotlessRules()
	eclipseFormatter(project)
	leadingTabs()
	customRules()
	javadocIndentationFixes()
}

fun JavaExtension.baseSpotlessRules() {
	applyRule("baseSpotlessRules") {
		endWithNewline()
		trimTrailingWhitespace()
		removeUnusedImports()
		forbidWildcardImports()
	}
}

fun JavaExtension.eclipseFormatter(project: Project) {
	applyRule("eclipseFormatter") {
		eclipse().configFile(eclipseConfigFile(project))
	}
}

fun JavaExtension.leadingTabs() {
	applyRule("leadingTabs") {
		leadingSpacesToTabs(4)
	}
}

fun JavaExtension.customRules() {
	singleExpressionLambdaCalls()
	mixinAnnotationArguments()
	multilineControlStatementConditions()
	noTernaryOperators()
}

fun JavaExtension.singleExpressionLambdaCalls() {
	applyCustomRule("singleExpressionLambdaCalls") {
		custom(
			"format single-expression lambda call closing parentheses",
			SingleExpressionLambdaCallFormatter
		)
	}
}

fun JavaExtension.mixinAnnotationArguments() {
	applyCustomRule("mixinAnnotationArguments") {
		custom(
			"format mixin annotation arguments",
			MixinAnnotationArgumentFormatter
		)
	}
}

fun JavaExtension.multilineControlStatementConditions() {
	applyCustomRule("multilineControlStatementConditions") {
		custom(
			"format multiline control statement conditions",
			ControlStatementConditionFormatter
		)
	}
}

fun JavaExtension.noTernaryOperators() {
	applyCustomRule("noTernaryOperators") {
		custom(
			"ban ternary operators",
			NoTernaryOperatorFormatter
		)
	}
}

fun JavaExtension.javadocIndentationFixes() {
	applyRule("javadocIndentationFixes") {
		replaceRegex("class-level javadoc indentation fix", "^\\*", " *")
		replaceRegex("method-level javadoc indentation fix", "\t\\*", "\t *")
	}
}

open class JavaFormattingSpotlessExtension(
	private val project: Project,
	private val javaExtension: JavaExtension
) {
	fun all() {
		javaExtension.all(project)
	}

	fun target(vararg targets: Any) {
		javaExtension.target(*targets)
	}

	fun targetJavaSources() {
		javaExtension.target(DEFAULT_JAVA_TARGET)
	}

	fun baseSpotlessRules() {
		javaExtension.baseSpotlessRules()
	}

	fun eclipseFormatter() {
		javaExtension.eclipseFormatter(project)
	}

	fun leadingTabs() {
		javaExtension.leadingTabs()
	}

	fun customRules() {
		javaExtension.customRules()
	}

	fun singleExpressionLambdaCalls() {
		javaExtension.singleExpressionLambdaCalls()
	}

	fun mixinAnnotationArguments() {
		javaExtension.mixinAnnotationArguments()
	}

	fun multilineControlStatementConditions() {
		javaExtension.multilineControlStatementConditions()
	}

	fun noTernaryOperators() {
		javaExtension.noTernaryOperators()
	}

	fun javadocIndentationFixes() {
		javaExtension.javadocIndentationFixes()
	}
}

open class JavaFormattingExtension @Inject constructor(
	private val project: Project
) {
	private var javaTargetConfigured = false

	fun all() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.all(project)
		}
	}

	fun target(vararg targets: Any) {
		javaTargetConfigured = true
		configureJava { javaExtension ->
			javaExtension.target(*targets)
		}
	}

	fun targetJavaSources() {
		if (javaTargetConfigured) {
			return
		}
		javaTargetConfigured = true
		configureJava { javaExtension ->
			javaExtension.target(DEFAULT_JAVA_TARGET)
		}
	}

	fun baseSpotlessRules() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.baseSpotlessRules()
		}
	}

	fun eclipseFormatter() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.eclipseFormatter(project)
		}
	}

	fun leadingTabs() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.leadingTabs()
		}
	}

	fun customRules() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.customRules()
		}
	}

	fun singleExpressionLambdaCalls() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.singleExpressionLambdaCalls()
		}
	}

	fun mixinAnnotationArguments() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.mixinAnnotationArguments()
		}
	}

	fun multilineControlStatementConditions() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.multilineControlStatementConditions()
		}
	}

	fun noTernaryOperators() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.noTernaryOperators()
		}
	}

	fun javadocIndentationFixes() {
		targetJavaSources()
		configureJava { javaExtension ->
			javaExtension.javadocIndentationFixes()
		}
	}

	private fun configureJava(configure: (JavaExtension) -> Unit) {
		project.extensions.configure(SpotlessExtension::class.java, object : Action<SpotlessExtension> {
			override fun execute(spotless: SpotlessExtension) {
				spotless.java(object : Action<JavaExtension> {
					override fun execute(javaExtension: JavaExtension) {
						configure(javaExtension)
					}
				})
			}
		})
	}

}

private const val DEFAULT_JAVA_TARGET = "**/src/*/java/**/*.java"

private val appliedSpotlessBlockRules = Collections.synchronizedMap(WeakHashMap<JavaExtension, MutableSet<String>>())

private fun JavaExtension.applyCustomRule(name: String, configure: JavaExtension.() -> Unit) {
	applyRule(name) {
		configure()
		bumpThisNumberIfACustomStepChanges(8)
	}
}

private fun JavaExtension.applyRule(name: String, configure: JavaExtension.() -> Unit) {
	val appliedRules = appliedSpotlessBlockRules.getOrPut(this) { mutableSetOf() }
	if (appliedRules.add(name)) {
		configure()
	}
}

private fun eclipseConfigFile(project: Project): File {
	val projectConfig = project.rootProject.file("config/spotless/eclipse-java.properties")
	if (projectConfig.isFile) {
		return projectConfig
	}
	return bundledEclipseConfigFile(project)
}

private fun bundledEclipseConfigFile(project: Project): File {
	val resourceName = "net/mezzdev/gradle/spotless/eclipse-java.properties"
	val resourceBytes = JavaFormattingSpotlessExtension::class.java.classLoader.getResourceAsStream(resourceName)
		?.use { it.readBytes() }
		?: error("Missing bundled resource: $resourceName")

	val configFile = project.rootProject.file(".gradle/mezz-spotless-formatting/eclipse-java.properties")
	if (!configFile.isFile || !configFile.readBytes().contentEquals(resourceBytes)) {
		configFile.parentFile.mkdirs()
		configFile.writeBytes(resourceBytes)
	}
	return configFile
}
