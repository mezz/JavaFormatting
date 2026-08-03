# Java Formatting

[![Build Status](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fci.blamejared.com%2Fjob%2Fmezz%2Fjob%2FJavaFormatting%2Fjob%2Fmain%2F&label=build)](https://ci.blamejared.com/job/mezz/job/JavaFormatting/job/main/)
[![Latest Version](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.blamejared.com%2Fnet%2Fmezzdev%2Fjava-formatting%2Fnet.mezzdev.java-formatting.gradle.plugin%2Fmaven-metadata.xml&label=version)](https://maven.blamejared.com/net/mezzdev/java-formatting/net.mezzdev.java-formatting.gradle.plugin/maven-metadata.xml)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://adoptium.net/temurin/releases/?version=17)
[![License](https://img.shields.io/github/license/mezz/JavaFormatting)](LICENSE)

Reusable Java formatting rules for Gradle projects.

This plugin provides the Java formatting conventions used by mezz projects. It
wraps Spotless and adds a few project-specific Java source formatters.

Requires Java 17 or newer.

## Usage

Resolve the plugin from Jared's Maven:

```kotlin
pluginManagement {
	repositories {
		maven("https://maven.blamejared.com/")
		mavenCentral()
	}
}
```

Apply the plugin in your project's `build.gradle.kts`:

```kotlin
plugins {
	id("net.mezzdev.java-formatting") version "0.3.2"
}
```

## Configuring rules

Applying the plugin does not apply formatting rules automatically. It applies
Spotless and adds the `javaFormatting` extension.

Apply everything:

```kotlin
javaFormatting {
	all()
}
```

Or apply only the parts you want:

```kotlin
javaFormatting {
	baseSpotlessRules()
	eclipseFormatter()
	fluentMethodChainClosingParentheses()
	mixinAnnotationArguments()
	noTernaryOperators()
}
```

The `javaFormatting { ... }` form targets `**/src/*/java/**/*.java` by default.
To use a different target, call `target(...)` before enabling rules:

```kotlin
javaFormatting {
	target("src/main/java/**/*.java", "src/test/java/**/*.java")
	noTernaryOperators()
}
```

You can also configure these rules inside a Spotless block:

```kotlin
import net.mezzdev.gradle.spotless.javaFormatting

spotless {
	javaFormatting(project) {
		all()
	}
}
```

Or add individual rules directly to a normal Spotless Java block:

```kotlin
import net.mezzdev.gradle.spotless.noTernaryOperators

spotless {
	java {
		target("src/main/java/**/*.java")
		noTernaryOperators()
	}
}
```

## Formatter rules

`all()` enables:

- `baseSpotlessRules()`
- `eclipseFormatter()`
- `leadingTabs()`
- `customRules()`
- `javadocIndentationFixes()`

`baseSpotlessRules()`:

- ends files with a newline
- trims trailing whitespace
- removes unused imports
- forbids wildcard imports

`eclipseFormatter()` uses the bundled
`net/mezzdev/gradle/spotless/eclipse-java.properties` by default. A consuming
root project can override it with `config/spotless/eclipse-java.properties`.

`leadingTabs()` converts leading groups of 4 spaces to tabs.

`javadocIndentationFixes()` normalizes selected Javadoc indentation cases.

`customRules()` enables:

- `singleExpressionLambdaCalls()`
- `fluentMethodChainClosingParentheses()`
- `mixinAnnotationArguments()`
- `multilineControlStatementConditions()`
- `noTernaryOperators()`

Run formatting from a consuming build with the normal Spotless tasks:

```shell
./gradlew spotlessApply
./gradlew spotlessCheck
```
