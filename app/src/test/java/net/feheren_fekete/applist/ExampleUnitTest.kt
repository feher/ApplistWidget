package net.feheren_fekete.applist

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.junit.Assert
import org.junit.Test
import kotlin.coroutines.coroutineContext

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        fun foo(): Flow<Int> = flow {
            for (i in 1..10) {
//                delay(100)
                if (!coroutineContext.isActive) {
                    return@flow
                }
                Thread.sleep(100)
                println("Emitting $i")
                emit(i)
            }
        }

        runBlocking {
            val job = launch(Dispatchers.IO) { // Timeout after 250ms
                foo().collect {
                    println("fuck")
                }
            }
            //delay(200)
            job.cancelAndJoin()
            println("Done")
        }
    }
}