package net.mezzdev.gradle.spotless.formatter

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertSame

class FormatterSerializationTest {
	@Test
	fun `formatter singletons preserve identity after java serialization`() {
		assertSame(ControlStatementConditionFormatter, roundTrip(ControlStatementConditionFormatter))
		assertSame(MixinAnnotationArgumentFormatter, roundTrip(MixinAnnotationArgumentFormatter))
		assertSame(NoTernaryOperatorFormatter, roundTrip(NoTernaryOperatorFormatter))
		assertSame(SingleExpressionLambdaCallFormatter, roundTrip(SingleExpressionLambdaCallFormatter))
	}

	private fun <T : Serializable> roundTrip(value: T): Any {
		val bytes = ByteArrayOutputStream()
		ObjectOutputStream(bytes).use { output ->
			output.writeObject(value)
		}
		return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { input ->
			input.readObject()
		}
	}
}
