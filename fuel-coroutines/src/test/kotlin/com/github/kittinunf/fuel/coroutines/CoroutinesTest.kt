import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.ResponseDeserializable
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CoroutinesTest {

    init {
        FuelManager.instance.basePath = "https://httpbin.org"

        Fuel.testMode {
            timeout = 30000
        }
    }

    @Test
    fun testAwaitStringSuccess() = runBlocking {
        try {
            Fuel.get("/uuid").awaitString().third
                    .fold({ data ->
                        assertTrue(data.isNotEmpty())
                        assertTrue(data.contains("uuid"))
                    }, { error ->
                        fail("This test should pass but got an error: ${error.message}")
                    })
        } catch (exception: Exception) {
            fail("When using awaitString errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitStringErrorDueToNetwork() = runBlocking {
        try {
            Fuel.get("/not/found/address").awaitString().third.fold({
                fail("This test should fail due to HTTP status code.")
            }, { error ->
                assertTrue(error.exception is HttpException)
                assertTrue(error.message.orEmpty().contains("HTTP Exception 404"))
            })
        } catch (exception: HttpException) {
            fail("When using awaitString errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitResponseSuccess() = runBlocking {
        try {
            Fuel.get("/ip").awaitResponse().third
                    .fold({ data ->
                        assertTrue(data.isNotEmpty())
                    }, { error ->
                        fail("This test should pass but got an error: ${error.message}")
                    })
        } catch (exception: Exception) {
            fail("When using awaitResponse errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitResponseErrorDueToNetwork() = runBlocking {
        try {
            Fuel.get("/invalid/url").awaitResponse().third.fold({
                fail("This test should fail due to HTTP status code.")
            }, { error ->
                assertTrue(error.exception is HttpException)
                assertTrue(error.message!!.contains("HTTP Exception 404"))
            })
        } catch (exception: HttpException) {
            fail("When using awaitResponse errors should be folded instead of thrown.")
        }
    }

    private data class UUIDResponse(val uuid: String)

    private object UUIDResponseDeserializer : ResponseDeserializable<UUIDResponse> {
        override fun deserialize(content: String) =
                jacksonObjectMapper().readValue<UUIDResponse>(content)
    }

    @Test
    fun testAwaitObjectSuccess() = runBlocking {
        try {
            Fuel.get("/uuid").awaitObject(UUIDResponseDeserializer).third
                    .fold({ data ->
                        assertTrue(data.uuid.isNotEmpty())
                    }, { error ->
                        fail("This test should pass but got an error: ${error.message}")
                    })
        } catch (exception: HttpException) {
            fail("When using awaitObject network errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitObjectErrorDueToNetwork() = runBlocking {
        try {
            Fuel.get("/not/uuid/endpoint").awaitObject(UUIDResponseDeserializer).third.fold({
                fail("This test should fail due to HTTP status code.")
            }, { error ->
                assertTrue(error.exception is HttpException)
                assertTrue(error.message!!.contains("HTTP Exception 404"))
            })
        } catch (exception: HttpException) {
            fail("When using awaitObject errors should be folded instead of thrown.")
        }
    }

    private data class UUIDIntResponse(val uuid: Int)

    private object UUIDIntResponseDeserializer : ResponseDeserializable<UUIDIntResponse> {
        override fun deserialize(content: String) =
                jacksonObjectMapper().readValue<UUIDIntResponse>(content)
    }

    @Test
    fun testAwaitObjectDueToDeserialization() = runBlocking {
        try {
            Fuel.get("/uuid").awaitObject(UUIDIntResponseDeserializer).third.fold({
                fail("This test should fail because uuid property should be a String.")
            }, {
                fail("When using awaitObject serialization/deserialization errors are thrown.")
            })
        } catch (exception: JsonMappingException) {
            assertNotNull(exception)
        }
    }

    @Test
    fun testAwaitStringResultSuccess() = runBlocking {
        try {
            val data = Fuel.get("/uuid").awaitStringResult()
            assertTrue(data.contains("uuid"))
        } catch (exception: Exception) {
            fail("This test should pass but got an exception: ${exception.message}")
        }
    }

    @Test
    fun testAwaitResponseResultSuccess() = runBlocking {
        try {
            val data = Fuel.get("/uuid").awaitResponseResult()
            assertTrue(data.isNotEmpty())
        } catch (exception: Exception) {
            fail("This test should pass but got an exception: ${exception.message}")
        }
    }

    @Test
    fun testAwaitObjectResultSuccess() = runBlocking {
        try {
            val data = Fuel.get("/uuid").awaitObjectResult(UUIDResponseDeserializer)
            assertTrue(data.uuid.isNotEmpty())
        } catch (exception: Exception) {
            fail("This test should pass but got an exception: ${exception.message}")
        }
    }

    @Test
    fun testAwaitObjectResultExceptionDueToNetwork() = runBlocking {
        try {
            Fuel.get("/some/invalid/path").awaitObjectResult(UUIDResponseDeserializer)
            fail("This test should raise an exception due to invalid URL")
        } catch (exception: HttpException) {
            assertNotNull(exception)
            assertTrue(exception.message.orEmpty().contains("404"))
        }
    }

    @Test
    fun testAwaitObjectResultExceptionDueToDeserialization() = runBlocking {
        try {
            Fuel.get("/uuid").awaitObjectResult(UUIDIntResponseDeserializer)
            fail("This test should fail because uuid property should be a String.")
        } catch (exception: JsonMappingException) {
            assertNotNull(exception)
        }
    }

    @Test
    fun testItCanAwaitForStringResultCanThrowException() = runBlocking {
        try {
            Fuel.get("/error/404").awaitStringResult()
            fail("This test should fail due to status code 404")
        } catch (exception: HttpException) {
            assertNotNull(exception)
        }
    }

    @Test
    fun testAwaitSafelyObjectResultSuccess() = runBlocking {
        try {
            Fuel.get("/uuid").awaitSafelyObjectResult(UUIDResponseDeserializer)
                    .fold({ data ->
                        assertTrue(data.uuid.isNotEmpty())
                    }, { error ->
                        fail("This test should pass but got an error: ${error.message}")
                    })
        } catch (exception: Exception) {
            fail("When using awaitSafelyObjectResult errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitSafelyObjectResultErrorDueToNetwork() = runBlocking {
        try {
            Fuel.get("/error/404").awaitSafelyObjectResult(UUIDResponseDeserializer)
                    .fold({
                        fail("This test should fail due to HTTP status code.")
                    }, { error ->
                        assertTrue(error.exception is HttpException)
                    })
        } catch (exception: Exception) {
            fail("When using awaitSafelyObjectResult errors should be folded instead of thrown.")
        }
    }

    @Test
    fun testAwaitSafelyObjectResultErrorDueToDeserialization() = runBlocking {
        try {
            Fuel.get("/ip").awaitSafelyObjectResult(UUIDResponseDeserializer)
                    .fold({
                        fail("This test should fail due to HTTP status code.")
                    }, { error ->
                        assertNotNull(error)
                        assertTrue(error.exception is JsonMappingException)
                    })
        } catch (exception: Exception) {
            fail("When using awaitSafelyObjectResult errors should be folded instead of thrown.")
        }
    }
}
