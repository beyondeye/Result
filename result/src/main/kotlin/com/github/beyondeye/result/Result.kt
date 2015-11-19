package com.github.beyondeye.result

//TODO implement some common method for all classes derived from resultList
interface ResultList {
    /**
     * number of [Result] objects in [ResultList]
     */
    val size:Int
    fun isSuccess():Boolean
    /**
     * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#isdone]]
     */
    fun isDone(): Boolean {return true }
    fun isFailure():Boolean {return !isSuccess()}
}
public class NoException private constructor()


/**
 * Created by Dario Elyasy on 11/15/15
 * based on code by Kittinun Vantasin [Result library][https://github.com/beyondeye/Result]
 * and code by Mark Platvoet [Kovenant library][https://github.com/mplatvoet/kovenant]
 *
 * Emulate kovenant API for synchronous code. The use case is handling errors in cases where kotlin support
 * for nullable types (overhead free Option type) is not enough, i.e. we need to propagate some information about
 * the cause of the error to the caller. This is the case where usually exceptions are used, so the code support
 * factory methods for integrating with code that uses exception based error handling
 */
sealed public class Result<out V : Any, out E> private constructor(val value: V?, val error: E?) :ResultList{

    override val size:Int get()=1
    override fun isSuccess():Boolean =  this is Success

    operator public fun component1(): V? = this.value
    operator public fun component2(): E? = this.error

    public class Success<V : Any>(value: V) : Result<V, Nothing>(value, null)

    public class Failure<E>(error: E) : Result<Nothing, E>(null, error)

    companion object {
        /**
         *
         * Factory method
         * For integration with code with error handling based on Exceptions
         */
        public fun <V: Any> ofTry(f: Function0<V>): Result<V, Exception> {
            return try {
                Result.ofSuccess(f())
            } catch(ex: Exception) {
                Result.ofFail(ex)
            }
        }

        /**
         * Factory method
         */
        public  fun <V : Any> of(value: V?,  fail: (() -> Exception)? = null) =
                value?.let { Success(it) } ?: Failure(fail?.invoke() ?: Exception())


        /**
         * Factory method
         * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#of]
         */
        public inline fun <V : Any> of(valueFn: ()->V?) =
                valueFn()?.let { Success(it) } ?: Failure( Exception())

        /**
         * Factory method
         * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#of]
         */
        public  fun <V:Any> ofSuccess(value: V)=Success(value)
        /**
         * Factory method
         * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#of]
         */
        public  fun <E> ofFail(error: E)= Failure(error)


    }


    /**
     * provide a fallback value in case of failure
     *
     * It is the equivalent of the elvis operator ?: for nullable types
     *
     * Example:
     *
     * val vecsize= Result.of(vec.size() or 0)
     *
     */
    public inline infix fun <reified V : Any> or(fallbackValue:V):Result<V,E> {
        return when(this) {
            is Success -> Success(this.value as V)
            is Failure -> Success(fallbackValue)
        }
    }


    /*
    *Safe cast actual value of Result to the requested type
    *
     */
    public inline fun <reified X> getAs(): X? {
        @Suppress("unchecked_cast")
        return when (this) {
            is Success -> this.value as? X
            is Failure -> this.error as? X
        }
    }

    /**
     * get the value of the result or throw an exception if result is of type error.
     *
     * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#get]
     */
    public fun  get():V {
        when (this) {
            is Success -> return this.value as V
            is Failure -> throw this.error as? Throwable ?: Exception(this.error.toString())
        }
    }


    /**
     * execute the given function if the result is of type [Success].
     *
     * this is similar to [then]/[map].
     * the difference is that the returned result is the original object, and the [onSuccess] function does not return anything
     *
     * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#callbacks]
     * @return 'this' for allowing chained calls
     * **/
    public infix fun success(onSuccess:(V)->Unit): Result<V, E> {
        when (this) {
            is Success ->  onSuccess(this.value!!)
        }
        return this
    }
    /**
     * execute the given function if result is of type [Failure]
     *
     * This is similar to [mapError].
     * The difference is that the returned result is the original object, and the [onFailure] function does not return anything
     *
     * see [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#callbacks]
     * @return 'this' for allowing chained calls
     **/
    public infix fun fail(onFailure:(E)->Unit): Result<V, E> {
        when (this) {
            is Failure ->  onFailure(this.error!!)
        }
        return this
    }

    /**
     * execute the given function, both if the object is of type
     * [Success] or if of type [Failure].
     *
     * see [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#callbacks]
     * This function is pretty pointless in the context of the [Result] class. It exists in kovenant because the
     * function is called only when the promise complete
     * @return 'this' for allowing chained calls
     * **/
    public infix fun always(onAlways:()->Unit): Result<V, E> {
        onAlways()
        return this
    }


    public fun <X> fold(success: (V) -> X, failure: (E) -> X): X {
        return when (this) {
            is Success -> success(this.value!!)
            is Failure -> failure(this.error!!)
        }
    }
    /**
     * process the success value of a [Result] and returns a new [Result] with the transformed value.
     *
     * This function is exaclty the same as [map]  but with infix syntax.
     *
     * see [map] documentation for more details
     * @param transform the transform function.
     * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#then] **/
    public infix  fun <U : Any> then(transform: (V) -> U) =
            fold({ Result.Success(transform(it)) }, { Result.Failure(it) })

    /**
     *  map the success value of a [Result] and returns a new [Result] with the transformed value.
     *
     *  This function is exactly the same as [then]
     *
     * *This function is similar to [bind] except for the signature of the [transform] function
     *  that here should return a value while in [bind] it returns a [Result] object
     *
     *  Extended description:
     *
     * Transforms Result A to Result B. If Result A resolves successful then [transform] is executed
     *  and returns Result B. If [transform] is successful, then Result B resolves successful, failed otherwise.
     * If Result A fails with error E, Result B will fail with error E too.

     * see also [kovenant docs][http://kovenant.komponents.nl/api/functional_usage/#map]
     * @param transform the transform function.
     * **/
    public fun <U : Any> map(transform: (V) -> U) =
            fold({ Result.Success(transform(it)) }, { Result.Failure(it) })

    /**
     * process the success value of a [Result] and returns a new [Result] with the transformed value.
     *
     * This function is similar to [then]/[map] but the [transform] function is an extension function of the value class for the result
     *
     * See [map] for extended documentation
     *
     * see also [kovenant docs][http://kovenant.komponents.nl/api/core_usage/#then-use] **/
    public infix  fun <U : Any> thenUse(transform: V.() -> U) =
            fold({ Result.Success(it.transform()) }, { Result.Failure(it) })

    /**
     * process the success value of a [Result] and returns a new [Result] with the transformed value.
     *
     *
     * *This function is similar to [then]/[map] except for the signature of the [transform] function
     *  that here should return a [Result] object, while in [then]/[map] it returns the success value itself
     *
     * Extended description:
     *
     * Transforms Result A to Result B with a bind function that returns Result B.
     * If Result A resolves successful then [transform] is executed  and returns Result B.
     * If [transform] is successful, then Result B resolves successful, failed otherwise.
     *
     * If Promise A fails with error E, Promise B will fail with error E too.
     *
     * see also [kovenant docs][http://kovenant.komponents.nl/api/functional_usage/#bind]
     * @param bind the transform function.
     */
    public infix fun <U : Any, E > bind(transform: (V) -> Result<U, E>) =
            fold({ transform(it) }, { Result.Failure(it) })


    /**
     * process the error value of a [Result] and returns a new [Result] with the transformed value.
     *
     * Basically it is the same as [then]/[map] but apply [errorTransform] function on the error (if Result is of type [Failure]), and do nothing if it is of
     * type [Success]
     */
    public fun <E2> mapError(errorTransform: (E) -> E2) =
            fold({ Result.Success(it) }, { Result.Failure(errorTransform(it)) })


    /**
     * process the error value of a [Result] and returns a new [Result] with the transformed value.
     *
     * Basically it is the same as [bind] but apply [errorTransform] function on the error (if Result is of type [Failure]]), and do nothing if it is of
     * type [Success]
     */
    public infix fun <V : Any, E2> bindError(errorTransform: (E) -> Result<V, E2>) =
            fold({ Result.Success(it) }, { errorTransform(it) })

    override fun toString() = fold({ "[Success: $it]" }, { "[Failure: $it]" })


    public infix fun <V1 : Any, E1>and(f: () -> Result<V1, E1>): ResultTuple2<V, E, V1, E1> {
        return ResultTuple2(this,
                if (this is Success) f() else null)
    }

    /**
     * a list of 4 [Result] objects
     */
    data class ResultTuple4<out V :Any,out E,out V2:Any,out E2,out V3:Any,out E3,out V4:Any,out E4>(
            val first:Result<V, E>,
            val second:Result<V2,E2>?,
            val third:Result<V3,E3>?,
            val fourth:Result<V4,E4>?):ResultList {
        override val size:Int get()=4
        override fun isSuccess():Boolean = this.first is Success && this.second is Success && this.third is Success && this.fourth is Success

    }
    /**
     * a triple of [Result] objects
     */
    data class ResultTuple3<out V :Any,out E,out V2:Any,out E2,out V3:Any,out E3>(
            val first:Result<V, E>,
            val second:Result<V2,E2>?,
            val third:Result<V3,E3>?) :ResultList {
        override val size:Int get()=3
        override fun isSuccess():Boolean = this.first is Success && this.second is Success && this.third is Success

        public infix fun <V4 : Any, E4 > and(f: () -> Result<V4, E4>): ResultTuple4<V, E, V2, E2, V3, E3,V4,E4> {
            return ResultTuple4(
                    this.first,
                    this.second,
                    this.third,
                    if (isSuccess()) f() else null
            )
        }
    }

    /**
     * a pair of [Result] objects
     */
    data class ResultTuple2<out V : Any, out E, out V2 : Any, out E2 >(val first: Result<V, E>, val second: Result<V2, E2>?) : ResultList {
        override val size:Int get()=2
        override fun isSuccess():Boolean = this.first is Success && this.second is Success
        public infix fun <V3 : Any, E3 >and(f: () -> Result<V3, E3>): ResultTuple3<V, E, V2, E2, V3, E3> {
            return ResultTuple3(
                    this.first,
                    this.second,
                    if (isSuccess()) f() else null
            )
        }
    }


}