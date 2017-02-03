/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:kotlin.jvm.JvmName("IntrinsicsJvmKt")
@file:kotlin.jvm.JvmVersion
package kotlin.coroutines.experimental.intrinsics
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.processBareContinuationResume
import kotlin.coroutines.experimental.jvm.internal.CoroutineImpl
import kotlin.coroutines.experimental.jvm.internal.interceptContinuationIfNeeded

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any? = (this as Function1<Continuation<T>, Any?>).invoke(completion)

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any? = (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)

@SinceKotlin("1.1")
fun <T> (suspend () -> T).createCoroutineUnchecked(
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function1<Continuation<T>, Any?>).invoke(completion)
            }
        else
            (this.create(completion) as CoroutineImpl).facade

@SinceKotlin("1.1")
fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> =
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
            }
        else
            ((this as CoroutineImpl).create(receiver, completion) as CoroutineImpl).facade

// INTERNAL DEFINITIONS

private inline fun <T> buildContinuationByInvokeCall(
        completion: Continuation<T>,
        crossinline block: () -> Any?
): Continuation<Unit> {
    val continuation =
            object : Continuation<Unit> {
                override val context: CoroutineContext
                    get() = completion.context

                override fun resume(value: Unit) {
                    processBareContinuationResume(completion, block)
                }

                override fun resumeWithException(exception: Throwable) {
                    completion.resumeWithException(exception)
                }
            }

    return completion.context.interceptContinuationIfNeeded(continuation)
}
