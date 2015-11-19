package plenseesim.utils

import com.github.beyondeye.result.NoException
import com.github.beyondeye.result.Result
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by Dario on 11/15/2015.
 * remove SideResourceAccessEnabler for restoring original code for Result Tests
 */
class ResultTestCopy {
    @Test
    fun testANDandORforPair() {
        val (one, two) = Result.of(null) or 1 and { Result.of(null) or 2 }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
    }
    @Test
    fun testANDforPair() {
        val (one, two) = Result.of(1) and { Result.of(2)  }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
    }
    @Test
    fun testANDandORforTriple() {
        val (one, two, three) = Result.of(null) or 1 and { Result.of(null) or 2 } and { Result.of(null) or 3 }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
        assertTrue(three?.value==3)
    }
    @Test
    fun testANDforTriple() {
        val (one, two, three) = Result.of(1) and { Result.of(2)  } and { Result.of(3) }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
        assertTrue(three?.value==3)
    }
    @Test
    fun testANDandORforQuad() {
        val (one, two, three,four) = Result.of(null) or 1 and { Result.of(null) or 2 } and { Result.of(null) or 3 } and { Result.of(null) or 4 }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
        assertTrue(three?.value==3)
        assertTrue(four?.value==4)
    }
    @Test
    fun testANDforQuad() {
        val (one, two, three,four) = Result.of(1) and { Result.of(2)  } and { Result.of(3) } and { Result.of(4) }
        assertTrue(one.value==1)
        assertTrue(two?.value ==2)
        assertTrue(three?.value==3)
        assertTrue(four?.value==4)
    }


    @Test
    fun testCreateValue() {
        val v = Result.of(1)

        assertNotNull(v, "Result is created successfully")
        assertTrue(v is Result.Success, "v is Result.Success type")
    }

    @Test
    fun testCreateError() {
        val e = Result.of(RuntimeException())

        assertNotNull(e, "Result is created successfully")
        assertTrue(e is Result.Fail, "v is Result.Success type")
    }

    @Test
    fun testCreateOptionalValue() {
        val value1: String? = null
        val value2: String? = "1"

        val result1 = Result.of(value1) { UnsupportedOperationException("value is null") }
        val result2 = Result.of(value2) { IllegalStateException("value is null") }

        assertTrue(result1 is Result.Fail, "result1 is Result.Fail type")
        assertTrue(result2 is Result.Success, "result2 is Result.Success type")
    }

    @Test
    fun testCreateFromLambda() {
        val f1 = { "foo" }
        val f2 = {
            val v = arrayListOf<Int>()
            v[1]
        }

        val f3 = {
            val s: String?
            s = null
            s
        }

        val result1 = Result.of(f1)
        val result2 = Result.of(f2)
        val result3 = Result.of(f3())

        assertTrue(result1 is Result.Success, "result1 is Result.Success type")
        assertTrue(result2 is Result.Fail, "result2 is Result.Fail type")
        assertTrue(result3 is Result.Fail, "result2 is Result.Fail type")
    }

    @Test
    fun testGet() {
        val f1 = { true }
        val f2 = { File("not_found_file").readText() }

        val result1 = Result.of(f1)
        val result2 = Result.of(f2)

        assertTrue(result1.get(), "result1 is true")
        assertTrue("result2 expecting to throw FileNotFoundException") {
            var result = false
            try {
                result2.get()
            } catch(e: FileNotFoundException) {
                result = true
            }
            result
        }
    }

    @Test
    fun testGetValue() {
        val result1 = Result.of(22)
        val result2 = Result.of(KotlinNullPointerException())

        val v1: Int = result1.getAs<Int>()!!
        val (v2, err) = result2

        assertTrue { v1 == 22 }
        assertTrue { err is KotlinNullPointerException }
    }

    @Test
    fun testFold() {
        val success = Result.of("success")
        val failure = Result.of(RuntimeException("failure"))

        val v1 = success.fold({ 1 }, { 0 })
        val v2 = failure.fold({ 1 }, { 0 })

        assertTrue { v1 == 1 }
        assertTrue { v2 == 0 }
    }

    //helper
    fun Nothing.count() = 0
    fun Nothing.getMessage() = ""

    @Test
    fun testMap() {
        val success = Result.of("success")
        val failure = Result.of(RuntimeException("failure"))

        val v1 = success.map { it.count() }
        val v2 = failure.map { it.count() }

        assertTrue { v1.getAs<Int>() == 7 }
        assertTrue { v2.getAs<Int>() ?: 0 == 0 }
    }

    @Test
    fun testFlatMap() {
        val success = Result.of("success")
        val failure = Result.of(RuntimeException("failure"))

        val v1 = success.bind { Result.of(it.last()) }
        val v2 = failure.bind { Result.of(it.count()) }

        assertTrue { v1.getAs<Char>() == 's' }
        assertTrue { v2.getAs<Char>() ?: "" == "" }
    }

    @Test
    fun testMapError() {
        val success = Result.of("success")
        val failure = Result.of(Exception("failure"))

        val v1 = success.mapError { InstantiationException(it.message) }
        val v2 = failure.mapError { InstantiationException(it.message) }

        assertTrue { v1.value == "success" && v1.error == null }
        assertTrue {
            val (value, error) = v2
            error is InstantiationException && error.message == "failure"
        }
    }

    @Test
    fun testFlatMapError() {
        val success = Result.of("success")
        val failure = Result.of(Exception("failure"))

        val v1 = success.bindError { Result.of(IllegalArgumentException()) }
        val v2 = failure.bindError { Result.of(IllegalArgumentException()) }

        assertTrue { v1.getAs<String>() == "success" }
        assertTrue { v2.error is IllegalArgumentException }
    }

    @Test
    fun testComposableFunctions1() {
        val foo = { readFromAssetFileName("foo.txt") }
        val bar = { readFromAssetFileName("bar.txt") }

        val notFound = { readFromAssetFileName("fooo.txt") }

        val (value1, error1) = Result.of(foo).map { it.count() }.mapError { IllegalStateException() }
        val (value2, error2) = Result.of(notFound).map { bar }.mapError { IllegalStateException() }

        assertTrue { value1 == 574 && error1 == null }
        assertTrue { value2 == null && error2 is IllegalStateException }
    }

    @Test
    fun testComposableFunctions2() {
        val r1 = Result.of(functionThatCanReturnNull(false)).bind { resultReadFromAssetFileName("bar.txt") }.mapError { Exception("this should not happen") }
        val r2 = Result.of(functionThatCanReturnNull(true)).map { it.rangeTo(Int.MAX_VALUE) }.mapError { KotlinNullPointerException() }

        assertTrue { r1 is Result.Success }
        assertTrue { r2 is Result.Fail }
    }

    @Test
    fun testNoException() {
        val r = concat("1", "2")
        assertTrue { r is Result.Success }
    }

    // helper
    fun readFromAssetFileName(name: String): String {
        val dir = System.getProperty("user.dir")
        val assetsDir = File(dir, "src/test/assets/")
        return File(assetsDir, name).readText()
    }

    fun resultReadFromAssetFileName(name: String): Result<String, Exception> {
        val operation = { readFromAssetFileName(name) }
        return Result.of(operation)
    }

    fun functionThatCanReturnNull(nullEnabled: Boolean): Int? = if (nullEnabled) null else Int.MIN_VALUE

    fun concat(a: String, b: String): Result<String, NoException> = Result.Success(a + b)

}