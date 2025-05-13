package com.compiler.server

import com.compiler.server.base.BaseExecutorTest
import org.junit.jupiter.api.Test

class CoroutinesFlowTest : BaseExecutorTest() {

  @Test
  fun `flow api basic test`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlinx.coroutines.flow.*\n\nfun main() = runBlocking {\n    // Create a flow of integers from 1 to 3\n    val flow = flow {\n        for (i in 1..3) {\n            delay(100) // Pretend we're doing something useful\n            emit(i) // Emit the next value\n        }\n    }\n    \n    // Collect the flow\n    flow.collect { value ->\n        println(\"Received: \$value\")\n    }\n}",
      contains = "Received: 1\nReceived: 2\nReceived: 3\n"
    )
  }

  @Test
  fun `flow api transformation test`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlinx.coroutines.flow.*\n\nfun main() = runBlocking {\n    // Create a flow and apply transformations\n    (1..3).asFlow()\n        .map { it * it } // Square the numbers\n        .filter { it > 1 } // Filter out 1\n        .collect { value ->\n            println(\"Processed value: \$value\")\n        }\n}",
      contains = "Processed value: 4\nProcessed value: 9\n"
    )
  }

  @Test
  fun `flow api terminal operators test`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlinx.coroutines.flow.*\n\nfun main() = runBlocking {\n    val sum = (1..5).asFlow()\n        .map { it * it }\n        .reduce { a, b -> a + b }\n    \n    println(\"Sum of squares: \$sum\")\n    \n    val numbers = (1..5).asFlow()\n    val count = numbers.count()\n    println(\"Count: \$count\")\n}",
      contains = "Sum of squares: 55\nCount: 5\n"
    )
  }

  @Test
  fun `channels basic test`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlinx.coroutines.channels.*\n\nfun main() = runBlocking {\n    val channel = Channel<Int>()\n    \n    launch {\n        // Send 5 numbers\n        for (x in 1..5) {\n            channel.send(x)\n        }\n        channel.close() // Close the channel\n    }\n    \n    // Receive and print all numbers\n    for (y in channel) {\n        println(\"Received: \$y\")\n    }\n    \n    println(\"Done!\")\n}",
      contains = "Received: 1\nReceived: 2\nReceived: 3\nReceived: 4\nReceived: 5\nDone!\n"
    )
  }

  @Test
  fun `select expression test`() {
    run(
      code = "import kotlinx.coroutines.*\nimport kotlinx.coroutines.channels.*\nimport kotlinx.coroutines.selects.*\n\nfun main() = runBlocking {\n    val channel1 = Channel<String>()\n    val channel2 = Channel<String>()\n    \n    launch {\n        delay(100)\n        channel1.send(\"from channel1\")\n    }\n    \n    launch {\n        delay(50)\n        channel2.send(\"from channel2\")\n    }\n    \n    // Select from the first channel that becomes available\n    val result = select<String> {\n        channel1.onReceive { it }\n        channel2.onReceive { it }\n    }\n    \n    println(\"Result: \$result\")\n    \n    // Clean up\n    coroutineContext.cancelChildren()\n}",
      contains = "Result: from channel2\n"
    )
  }

  @Test
  fun `supervisor scope test`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\n    supervisorScope {\n        val job1 = launch {\n            try {\n                println(\"Child 1 is running\")\n                delay(500)\n                println(\"Child 1 completed\")\n            } catch (e: CancellationException) {\n                println(\"Child 1 was cancelled\")\n            }\n        }\n        \n        val job2 = launch {\n            try {\n                println(\"Child 2 is running\")\n                throw RuntimeException(\"Child 2 failed\")\n            } catch (e: RuntimeException) {\n                println(\"Child 2 caught exception: \${e.message}\")\n            }\n        }\n        \n        // Wait for all children\n        try {\n            job1.join()\n            job2.join()\n            println(\"All children completed or failed\")\n        } catch (e: Exception) {\n            println(\"Parent caught: \${e.message}\")\n        }\n    }\n}",
      contains = "Child 1 is running\nChild 2 is running\nChild 2 caught exception: Child 2 failed\nChild 1 completed\nAll children completed or failed\n"
    )
  }

  @Test
  fun `coroutine exception handler test`() {
    run(
      code = "import kotlinx.coroutines.*\n\nfun main() = runBlocking {\n    val handler = CoroutineExceptionHandler { _, exception ->\n        println(\"Caught exception: \${exception.message}\")\n    }\n    \n    val job = GlobalScope.launch(handler) {\n        launch {\n            // This will be cancelled when child 2 fails\n            try {\n                delay(1000)\n                println(\"Child 1 completed\")\n            } catch (e: CancellationException) {\n                println(\"Child 1 was cancelled\")\n            }\n        }\n        \n        launch {\n            // This will fail and trigger the handler\n            delay(100)\n            throw RuntimeException(\"Child 2 failed\")\n        }\n    }\n    \n    job.join()\n    println(\"Done\")\n}",
      contains = "Caught exception: Child 2 failed\nDone\n"
    )
  }
}