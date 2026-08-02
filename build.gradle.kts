import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	`kotlin-dsl`
	jacoco
	`maven-publish`
}

group = "net.mezzdev.gradle"
version = providers.gradleProperty("VERSION")
	.map { versionName ->
		versionName.removePrefix("v")
	}
	.orElse(providers.environmentVariable("TAG_NAME").map { tagName ->
		tagName.removePrefix("v")
	})
	.orElse("0.1.0-SNAPSHOT")
	.get()

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(17))
	}
}

kotlin {
	jvmToolchain(17)
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_17)
	}
}

dependencies {
	implementation("com.diffplug.spotless:spotless-plugin-gradle:8.8.0")
	implementation("com.github.javaparser:javaparser-core:3.27.1")
	testImplementation(gradleTestKit())
	testImplementation(kotlin("test-junit5"))
	testRuntimeOnly("org.jacoco:org.jacoco.agent:${jacoco.toolVersion}:runtime")
}

tasks.test {
	useJUnitPlatform()
	doFirst {
		delete(layout.buildDirectory.dir("jacoco/functional"))
	}
}

fun jacocoExecutionData() = fileTree(layout.buildDirectory.dir("jacoco")) {
	include("*.exec")
	include("functional/*.exec")
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	executionData(jacocoExecutionData())
	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(false)
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	executionData(jacocoExecutionData())
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}

tasks.check {
	dependsOn(tasks.jacocoTestReport)
	dependsOn(tasks.jacocoTestCoverageVerification)
}

gradlePlugin {
	plugins {
		create("javaFormatting") {
			id = "net.mezzdev.java-formatting"
			implementationClass = "net.mezzdev.gradle.spotless.SpotlessFormattingPlugin"
			displayName = "Mezz Java Formatting"
			description = "Opt-in reusable Spotless Java formatting rules for Gradle projects."
		}
	}
}

publishing {
	publications.withType<MavenPublication>().configureEach {
		pom {
			name.set("Mezz Java Formatting")
			description.set("Opt-in reusable Spotless Java formatting rules for Gradle projects.")
			url.set("https://github.com/mezz/JavaFormatting")
			licenses {
				license {
					name.set("MIT License")
					url.set("https://opensource.org/license/mit/")
				}
			}
			scm {
				url.set("https://github.com/mezz/JavaFormatting")
				connection.set("scm:git:https://github.com/mezz/JavaFormatting.git")
				developerConnection.set("scm:git:git@github.com:mezz/JavaFormatting.git")
			}
		}
	}
	repositories {
		val deployRepositoryUrl = providers.gradleProperty("DEPLOY_DIR")
			.orElse(providers.environmentVariable("local_maven_url"))
			.orElse(providers.environmentVariable("MAVEN_DEPLOY_DIR"))
			.orElse(providers.environmentVariable("local_maven"))
		if (deployRepositoryUrl.isPresent) {
			maven {
				name = "BlameJared"
				url = uri(deployRepositoryUrl.get())
			}
		}
	}
}
