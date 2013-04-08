package databench.runner

import databench.properties
import databench.util.Logging
import databench.task.Generator
import java.lang.System.{ currentTimeMillis => now }
import scala.collection.immutable.Seq
import scala.annotation.tailrec

case class Benchmark(
    subjects: Seq[BankSubject],
    generatorsPercentages: Seq[(Int, Generator)],
    numberOfTasks: Int,
    numberOfAccounts: Int,
    memory: Int,
    reporter: Reporter)
        extends Logging {

    info(s"Benchmark start for subjects: $subjects")
    info(properties.toString)

    val start = now

    val tasks =
        Generator.genTasksFor(
            numberOfTasks,
            numberOfAccounts,
            generatorsPercentages)

    benchmark(numberOfVMs = properties.vmsStart)

    @tailrec private def benchmark(numberOfVMs: Int): Unit = {
        val candidates =
            subjects.filter(_.acceptMultipleVMs || numberOfVMs == 1)
        if (candidates.nonEmpty) {
            Round(
                candidates,
                tasks,
                numberOfAccounts,
                memory,
                numberOfVMs,
                reporter)
            benchmark(numberOfVMs + properties.vmsStep)
        }
    }

    info("Benchmark end.")

}

