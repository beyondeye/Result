package com.github.beyondeye.result

import com.github.beyondeye.result.NoException
import com.github.beyondeye.result.Result
import nl.komponents.kovenant.async
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by Dario on 11/19/2015.
 */
class ResultKovenantTests
{
    @Test
    fun testKoventSuccess() {
        val someValue=123

        var promiseResultOnSuccess:Int=0
        var resultOnSuccess:Int=0

        val promise=async { someValue }
        val kresult = Result.of {someValue}

        val promiseResult=promise.get()
        val result = kresult.get()

        promise success { promiseResultOnSuccess=someValue}
        kresult success {resultOnSuccess =someValue}


        assert(promiseResult==result)
        assert(promiseResultOnSuccess==resultOnSuccess)
    }
}