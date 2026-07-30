plugins {
	`kotlin-dsl`
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

dependencies {
	implementation("com.diffplug.spotless:spotless-plugin-gradle:8.8.0")
	implementation("com.github.javaparser:javaparser-core:3.27.1")
	testImplementation(gradleTestKit())
	testImplementation(kotlin("test-junit5"))
}

tasks.test {
	useJUnitPlatform()
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
