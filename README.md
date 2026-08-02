# Java Formatting

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
	id("net.mezzdev.java-formatting") version "0.1.0"
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
- `mixinAnnotationArguments()`
- `multilineControlStatementConditions()`
- `noTernaryOperators()`

Run formatting from a consuming build with the normal Spotless tasks:

```shell
./gradlew spotlessApply
./gradlew spotlessCheck
```
